import { Component, Inject } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { finalize } from 'rxjs/operators';

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
    private readonly snack: MatSnackBar,
    private readonly dialogRef: MatDialogRef<AuditsFindingDialogComponent, FindingResponse>,
    @Inject(MAT_DIALOG_DATA) public readonly data: AuditsFindingDialogData
  ) {}

  submit(): void {
    if (this.form.invalid || this.submitting) {
      this.form.markAllAsTouched();
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
        // L'auteur n'est pas envoyé : le serveur le lit dans le jeton.
        checklistItemId: this.data.checklistItemId
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
          this.snack.open(this.messageDErreur(err), $localize`:@@common.ok:OK`, { duration: 6000 });
        }
      });
  }

  cancel(): void { this.dialogRef.close(); }

  /**
   * Le 409 dit ici une chose précise : l'audit n'est pas en cours. Le message
   * générique (« rechargez la page ») envoyait l'utilisateur recharger un écran
   * qui refusait toujours, sans lui dire qu'il fallait démarrer l'audit.
   */
  private messageDErreur(err: unknown): string {
    if ((err as { status?: number })?.status === 409) {
      return $localize`:@@audits.finding.not-in-progress:Un constat se relève pendant l'audit : démarrez l'audit pour en ajouter.`;
    }
    return safeErrorMessage(err, $localize`:@@audits.finding.save-error:Erreur lors de l'enregistrement.`);
  }
}
