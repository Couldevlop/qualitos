import { Component } from '@angular/core';
import { AbstractControl, FormBuilder, ValidationErrors, ValidatorFn, Validators } from '@angular/forms';
import { MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { finalize } from 'rxjs/operators';

import { AuthService } from '../../../../core/auth/auth.service';
import { safeErrorMessage } from '../../../../core/http/error-message';
import { CyberIncidentsService } from '../../cyi.service';
import { CyiSeverity, CyiType, CyiView, TYPE_LABEL } from '../../cyi.types';

const UUID_REGEX = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

function linesValidator(re: RegExp, msg: string, allowEmpty = true): ValidatorFn {
  return (c: AbstractControl): ValidationErrors | null => {
    const v = (c.value ?? '').toString().trim();
    if (!v) return allowEmpty ? null : { required: msg };
    for (const l of v.split(/\r?\n/).map((s: string) => s.trim()).filter((s: string) => s)) {
      if (!re.test(l)) return { lines: msg };
    }
    return null;
  };
}

@Component({
  selector: 'qos-cyi-detect-dialog',
  templateUrl: './cyi-detect-dialog.component.html',
  styleUrls: ['./cyi-detect-dialog.component.scss'],
  standalone: false
})
export class CyiDetectDialogComponent {

  submitting = false;
  readonly typeLabel = TYPE_LABEL;
  readonly types: CyiType[] = Object.keys(TYPE_LABEL) as CyiType[];
  readonly severities: CyiSeverity[] = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'];

  readonly form = this.fb.nonNullable.group({
    reference:   ['', [Validators.required, Validators.maxLength(64), Validators.pattern(/^[A-Z][A-Z0-9_-]{1,63}$/)]],
    title:       ['', [Validators.required, Validators.maxLength(250)]],
    description: ['', [Validators.maxLength(4000)]],
    incidentType: ['MALWARE' as CyiType, [Validators.required]],
    severity:     ['MEDIUM' as CyiSeverity, [Validators.required]],
    detectedAt:   [new Date().toISOString().slice(0, 16), [Validators.required]],
    occurredAt:   [new Date().toISOString().slice(0, 16), []],
    estimatedAffectedUsers: [0, [Validators.required, Validators.min(0)]],
    affectedAssets:   ['', [Validators.maxLength(4000)]],
    affectedServices: ['', [Validators.maxLength(4000)]],
    linkedBreachId:   ['', [Validators.pattern(new RegExp(`^$|${UUID_REGEX.source}`, 'i'))]]
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly svc: CyberIncidentsService,
    private readonly auth: AuthService,
    private readonly snack: MatSnackBar,
    private readonly dialogRef: MatDialogRef<CyiDetectDialogComponent, CyiView>
  ) {}

  isSignificant(): boolean {
    const s = this.form.controls.severity.value;
    return s === 'HIGH' || s === 'CRITICAL';
  }

  submit(): void {
    if (this.form.invalid || this.submitting) { this.form.markAllAsTouched(); return; }
    const v = this.form.getRawValue();
    const now = Date.now();
    const detectedMs = new Date(v.detectedAt).getTime();
    if (detectedMs > now) {
      this.snack.open('La date de détection ne peut pas être dans le futur.', 'OK', { duration: 4000 });
      return;
    }
    if (v.occurredAt) {
      const occMs = new Date(v.occurredAt).getTime();
      if (occMs > detectedMs) {
        this.snack.open('La date de survenue doit être ≤ à la date de détection.', 'OK', { duration: 4000 });
        return;
      }
    }
    const userId = this.auth.snapshot()?.userId;
    if (!userId) {
      this.snack.open('Session expirée — veuillez vous reconnecter.', 'OK', { duration: 4000 });
      return;
    }
    this.submitting = true;
    this.svc.detect({
      reference: v.reference.trim(),
      title: v.title.trim(),
      description: v.description?.trim() || undefined,
      detectedAt: new Date(v.detectedAt).toISOString(),
      occurredAt: v.occurredAt ? new Date(v.occurredAt).toISOString() : undefined,
      incidentType: v.incidentType, severity: v.severity,
      estimatedAffectedUsers: v.estimatedAffectedUsers,
      affectedAssets:   this.lines(v.affectedAssets),
      affectedServices: this.lines(v.affectedServices),
      linkedBreachId: v.linkedBreachId?.trim() || undefined,
      reportedByUserId: userId
    })
      .pipe(finalize(() => (this.submitting = false)))
      .subscribe({
        next: i => this.dialogRef.close(i),
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[cyi-detect] failed', err?.status, err?.error?.title);
          this.snack.open(safeErrorMessage(err, 'Enregistrement impossible.'), 'OK', { duration: 4000 });
        }
      });
  }

  cancel(): void { this.dialogRef.close(); }

  private lines(raw: string): string[] {
    return (raw ?? '').split(/\r?\n/).map(s => s.trim()).filter(s => s);
  }
}
