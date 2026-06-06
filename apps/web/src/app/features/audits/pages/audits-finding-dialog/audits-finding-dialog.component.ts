import { Component, Inject } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { finalize } from 'rxjs/operators';

import { AuthService } from '../../../../core/auth/auth.service';
import { safeErrorMessage } from '../../../../core/http/error-message';
import { AuditsService } from '../../audits.service';
import { FindingResponse, FindingType } from '../../audits.types';

export interface AuditsFindingDialogData {
  planId: string;
  /** Optional pre-selected checklist item id when raising a finding from a question. */
  checklistItemId?: string;
}

@Component({
  selector: 'qos-audits-finding-dialog',
  templateUrl: './audits-finding-dialog.component.html',
  styleUrls: ['./audits-finding-dialog.component.scss'],
  standalone: false
})
export class AuditsFindingDialogComponent {

  submitting = false;

  readonly types: { value: FindingType; label: string; hint: string }[] = [
    { value: 'MAJOR_NC',    label: $localize`:@@audits.finding.major-nc:Non-conformité majeure`, hint: $localize`:@@audits.finding.major-nc-hint:Défaillance système — corrective action obligatoire` },
    { value: 'MINOR_NC',    label: $localize`:@@audits.finding.minor-nc:Non-conformité mineure`, hint: $localize`:@@audits.finding.minor-nc-hint:Écart ponctuel — à corriger` },
    { value: 'OBSERVATION', label: $localize`:@@audits.finding.observation:Observation`,          hint: $localize`:@@audits.finding.observation-hint:Risque potentiel à surveiller` },
    { value: 'OPPORTUNITY', label: $localize`:@@audits.finding.opportunity:Opportunité`,          hint: $localize`:@@audits.finding.opportunity-hint:Amélioration suggérée` },
    { value: 'CONFORMITY',  label: $localize`:@@audits.finding.conformity:Conformité`,            hint: $localize`:@@audits.finding.conformity-hint:Bonne pratique relevée` }
  ];

  readonly form = this.fb.nonNullable.group({
    type: ['MINOR_NC' as FindingType, [Validators.required]],
    description: ['', [Validators.required]],
    clauseRef: ['', [Validators.maxLength(100)]],
    photoUrl: ['', [Validators.maxLength(1024)]]
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly audits: AuditsService,
    private readonly auth: AuthService,
    private readonly snack: MatSnackBar,
    private readonly dialogRef: MatDialogRef<AuditsFindingDialogComponent, FindingResponse>,
    @Inject(MAT_DIALOG_DATA) public readonly data: AuditsFindingDialogData
  ) {}

  submit(): void {
    if (this.form.invalid || this.submitting) {
      this.form.markAllAsTouched();
      return;
    }
    const raisedBy = this.auth.snapshot()?.userId;
    if (!raisedBy) {
      this.snack.open($localize`:@@common.session-expired:Session expirée — veuillez vous reconnecter.`, $localize`:@@common.ok:OK`, { duration: 4000 });
      return;
    }
    this.submitting = true;
    const v = this.form.getRawValue();
    this.audits
      .addFinding(this.data.planId, {
        type: v.type,
        description: v.description.trim(),
        clauseRef: v.clauseRef?.trim() || undefined,
        photoUrl: v.photoUrl?.trim() || undefined,
        checklistItemId: this.data.checklistItemId,
        raisedBy
      })
      .pipe(finalize(() => (this.submitting = false)))
      .subscribe({
        next: f => {
          this.snack.open($localize`:@@audits.finding.saved:Constat enregistré.`, $localize`:@@common.ok:OK`, { duration: 2500 });
          this.dialogRef.close(f);
        },
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[audits-finding-dialog] failed', err?.status, err?.error?.title);
          this.snack.open(
            safeErrorMessage(err, $localize`:@@audits.finding.save-error:Erreur lors de l'enregistrement.`),
            $localize`:@@common.ok:OK`, { duration: 4000 }
          );
        }
      });
  }

  cancel(): void { this.dialogRef.close(); }
}
