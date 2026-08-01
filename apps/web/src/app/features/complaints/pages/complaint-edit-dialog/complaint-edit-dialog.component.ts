import { Component, Inject } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { finalize } from 'rxjs/operators';

import { safeErrorMessage } from '../../../../core/http/error-message';
import { ComplaintsService } from '../../complaints.service';
import {
  COMPLAINT_CATEGORIES, COMPLAINT_CATEGORY_LABEL, COMPLAINT_SEVERITIES, COMPLAINT_SEVERITY_LABEL,
  Complaint, ComplaintCategory, ComplaintSeverity, UUID_PATTERN
} from '../../complaints.types';

export interface ComplaintEditDialogData {
  complaint: Complaint;
}

/**
 * Édition d'une réclamation en cours de traitement.
 *
 * Le serveur n'applique que les champs NON NULS : un champ laissé vide signifie
 * « inchangé », il n'efface pas la valeur existante. On envoie donc `null` pour
 * les champs vides et on le dit à l'utilisateur, plutôt que d'écrire une chaîne
 * vide qui écraserait silencieusement la donnée.
 */
@Component({
  selector: 'qos-complaint-edit-dialog',
  templateUrl: './complaint-edit-dialog.component.html',
  styleUrls: ['./complaint-edit-dialog.component.scss'],
  standalone: false
})
export class ComplaintEditDialogComponent {

  submitting = false;

  readonly categories = COMPLAINT_CATEGORIES;
  readonly severities = COMPLAINT_SEVERITIES;
  readonly categoryLabel = COMPLAINT_CATEGORY_LABEL;
  readonly severityLabel = COMPLAINT_SEVERITY_LABEL;

  readonly form = this.fb.nonNullable.group({
    subject: [this.data.complaint.subject, [Validators.required, Validators.maxLength(250)]],
    description: [this.data.complaint.description ?? '', Validators.maxLength(4000)],
    severity: [this.data.complaint.severity as ComplaintSeverity, Validators.required],
    category: [this.data.complaint.category as ComplaintCategory, Validators.required],
    customerName: [this.data.complaint.customerName ?? '', Validators.maxLength(250)],
    customerEmail: [this.data.complaint.customerEmail ?? '', [Validators.email, Validators.maxLength(320)]],
    customerExternalId: [this.data.complaint.customerExternalId ?? '', Validators.maxLength(200)],
    supplierId: [this.data.complaint.supplierId ?? '', Validators.pattern(UUID_PATTERN)],
    assignedToUserId: [this.data.complaint.assignedToUserId ?? '', Validators.pattern(UUID_PATTERN)]
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly svc: ComplaintsService,
    private readonly snack: MatSnackBar,
    private readonly dialogRef: MatDialogRef<ComplaintEditDialogComponent, Complaint>,
    @Inject(MAT_DIALOG_DATA) public readonly data: ComplaintEditDialogData
  ) {}

  submit(): void {
    if (this.form.invalid || this.submitting) {
      this.form.markAllAsTouched();
      return;
    }
    this.submitting = true;
    const v = this.form.getRawValue();
    this.svc.update(this.data.complaint.id, {
      subject: v.subject.trim(),
      description: blankToNull(v.description),
      severity: v.severity,
      category: v.category,
      customerName: blankToNull(v.customerName),
      customerEmail: blankToNull(v.customerEmail),
      customerExternalId: blankToNull(v.customerExternalId),
      supplierId: blankToNull(v.supplierId),
      assignedToUserId: blankToNull(v.assignedToUserId)
    })
      .pipe(finalize(() => (this.submitting = false)))
      .subscribe({
        next: updated => {
          this.snack.open($localize`:@@complaints.edit.success:Réclamation mise à jour.`,
            $localize`:@@common.ok:OK`, { duration: 2500 });
          this.dialogRef.close(updated);
        },
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[complaint-edit] failed', (err as { status?: number })?.status);
          this.snack.open(
            safeErrorMessage(err, $localize`:@@common.error-update:Erreur lors de la mise à jour.`),
            $localize`:@@common.ok:OK`, { duration: 4000 });
        }
      });
  }

  cancel(): void {
    this.dialogRef.close();
  }
}

/** Champ vide ⇒ `null` ⇒ « inchangé » côté serveur (mise à jour partielle). */
function blankToNull(value: string): string | null {
  const trimmed = value.trim();
  return trimmed.length > 0 ? trimmed : null;
}
