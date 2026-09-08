import { Component, Inject } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';

import { ApqpDeliverable, ApqpDeliverableRequest } from '../../apqp.types';
import { nonBlank } from '../../apqp.validators';

/** La phase concernée, et le livrable à reformuler s'il en existe un. */
export interface ApqpDeliverableDialogData {
  phaseTitle: string;
  deliverable?: ApqpDeliverable;
}

/**
 * Ajouter ou reformuler un livrable.
 *
 * <p>Le titre rappelle la PHASE : sur un cycle de cinq à six phases portant
 * chacune une dizaine de livrables, un dialogue qui ne dirait pas où l'on écrit
 * laisserait un doute au moment de valider.
 */
@Component({
  selector: 'qos-apqp-deliverable-dialog',
  templateUrl: './apqp-deliverable-dialog.component.html',
  standalone: false
})
export class ApqpDeliverableDialogComponent {

  readonly form = this.fb.nonNullable.group({
    label: ['', [Validators.required, nonBlank, Validators.maxLength(500)]]
  });

  readonly editing: boolean;

  constructor(
    private readonly fb: FormBuilder,
    private readonly dialogRef:
      MatDialogRef<ApqpDeliverableDialogComponent, ApqpDeliverableRequest>,
    @Inject(MAT_DIALOG_DATA) readonly data: ApqpDeliverableDialogData
  ) {
    this.editing = !!data?.deliverable;
    if (data?.deliverable) {
      this.form.patchValue({ label: data.deliverable.label });
    }
  }

  get dialogTitle(): string {
    return this.editing
      ? $localize`:@@apqp.deliverable-dialog.title-edit:Reformuler un livrable`
      : $localize`:@@apqp.deliverable-dialog.title-create:Ajouter un livrable`;
  }

  get submitLabel(): string {
    return this.editing
      ? $localize`:@@common.save:Enregistrer`
      : $localize`:@@common.add:Ajouter`;
  }

  get blockedReason(): string | undefined {
    return this.form.controls.label.invalid
      ? $localize`:@@apqp.deliverable-dialog.blocked-label:Nommez le livrable attendu.`
      : undefined;
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.dialogRef.close({ label: this.form.getRawValue().label.trim() });
  }

  cancel(): void {
    this.dialogRef.close();
  }
}
