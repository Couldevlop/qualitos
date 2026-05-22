import { Component } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { finalize } from 'rxjs/operators';

import { AuthService } from '../../../../core/auth/auth.service';
import { safeErrorMessage } from '../../../../core/http/error-message';
import { AiIncidentsService } from '../../ai-inc.service';
import { AiIncSeverity, AiIncView, SEVERITY_DEADLINE_DAYS } from '../../ai-inc.types';

const UUID_REGEX = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

@Component({
  selector: 'qos-inc-detect-dialog',
  templateUrl: './inc-detect-dialog.component.html',
  styleUrls: ['./inc-detect-dialog.component.scss'],
  standalone: false
})
export class IncDetectDialogComponent {

  submitting = false;

  readonly severities: { value: AiIncSeverity; label: string }[] = [
    { value: 'DEATH_OR_SERIOUS_HARM_TO_HEALTH',          label: 'Décès / atteinte grave santé (notif. 2 jours)' },
    { value: 'SERIOUS_INFRINGEMENT_FUNDAMENTAL_RIGHTS',  label: 'Atteinte grave droits fondamentaux (10 jours)' },
    { value: 'CRITICAL_INFRASTRUCTURE_DISRUPTION',       label: 'Disruption infrastructure critique (15 jours)' },
    { value: 'SERIOUS_PROPERTY_OR_ENVIRONMENTAL_DAMAGE', label: 'Dommage grave bien/environnement (15 jours)' }
  ];

  readonly form = this.fb.nonNullable.group({
    reference:  ['', [Validators.required, Validators.maxLength(64), Validators.pattern(/^[A-Z][A-Z0-9_-]{1,63}$/)]],
    aiSystemId: ['', [Validators.required, Validators.pattern(UUID_REGEX)]],
    severity:   ['SERIOUS_INFRINGEMENT_FUNDAMENTAL_RIGHTS' as AiIncSeverity, [Validators.required]],
    description:                ['', [Validators.required, Validators.maxLength(4000)]],
    affectedPersonsDescription: ['', [Validators.maxLength(4000)]],
    immediateActionsTaken:      ['', [Validators.maxLength(4000)]],
    occurredAt: ['', [Validators.required]],
    detectedAt: ['', [Validators.required]]
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly svc: AiIncidentsService,
    private readonly auth: AuthService,
    private readonly snack: MatSnackBar,
    private readonly dialogRef: MatDialogRef<IncDetectDialogComponent, AiIncView>
  ) {}

  deadlineDays(): number { return SEVERITY_DEADLINE_DAYS[this.form.controls.severity.value]; }

  submit(): void {
    if (this.form.invalid || this.submitting) { this.form.markAllAsTouched(); return; }
    const v = this.form.getRawValue();
    // OWASP A04 — detectedAt >= occurredAt
    if (new Date(v.detectedAt) < new Date(v.occurredAt)) {
      this.snack.open('La date de détection doit être ≥ à la date d\'occurrence.', 'OK', { duration: 4000 });
      return;
    }
    // detectedAt et occurredAt pas dans le futur
    const now = Date.now();
    if (new Date(v.detectedAt).getTime() > now || new Date(v.occurredAt).getTime() > now) {
      this.snack.open('Les dates ne peuvent pas être dans le futur.', 'OK', { duration: 4000 });
      return;
    }
    const createdByUserId = this.auth.snapshot()?.userId;
    if (!createdByUserId) {
      this.snack.open('Session expirée — veuillez vous reconnecter.', 'OK', { duration: 4000 });
      return;
    }
    this.submitting = true;
    this.svc.detect({
      reference: v.reference.trim(),
      aiSystemId: v.aiSystemId.trim(),
      severity: v.severity,
      description: v.description.trim(),
      affectedPersonsDescription: v.affectedPersonsDescription?.trim() || undefined,
      immediateActionsTaken: v.immediateActionsTaken?.trim() || undefined,
      occurredAt: new Date(v.occurredAt).toISOString(),
      detectedAt: new Date(v.detectedAt).toISOString(),
      createdByUserId
    })
      .pipe(finalize(() => (this.submitting = false)))
      .subscribe({
        next: i => { this.snack.open('Incident signalé.', 'OK', { duration: 2500 }); this.dialogRef.close(i); },
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[inc-detect] failed', err?.status, err?.error?.title);
          this.snack.open(safeErrorMessage(err, 'Erreur lors du signalement.'), 'OK', { duration: 4000 });
        }
      });
  }

  cancel(): void { this.dialogRef.close(); }
}
