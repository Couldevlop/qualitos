import { Component, Inject } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { finalize } from 'rxjs/operators';

import { safeErrorMessage } from '../../../../core/http/error-message';
import { DocumentsService } from '../../documents.service';
import { DocumentResponse, DocumentType } from '../../documents.types';

export interface DocumentsEditDialogData {
  document: DocumentResponse;
}

@Component({
  selector: 'qos-documents-edit-dialog',
  templateUrl: './documents-edit-dialog.component.html',
  styleUrls: ['./documents-edit-dialog.component.scss'],
  standalone: false
})
export class DocumentsEditDialogComponent {

  submitting = false;

  readonly types: { value: DocumentType; label: string }[] = [
    { value: 'POLICY',           label: $localize`:@@documents.type.policy:Politique` },
    { value: 'PROCEDURE',        label: $localize`:@@documents.type.procedure:Procédure` },
    { value: 'WORK_INSTRUCTION', label: $localize`:@@documents.type.work-instruction:Mode opératoire` },
    { value: 'RECORD',           label: $localize`:@@documents.type.record:Enregistrement` },
    { value: 'FORM',             label: $localize`:@@documents.type.form:Formulaire` },
    { value: 'MANUAL',           label: $localize`:@@documents.type.manual:Manuel` },
    { value: 'OTHER',            label: $localize`:@@documents.type.other:Autre` }
  ];

  readonly form = this.fb.nonNullable.group({
    title: [this.data.document.title, [Validators.required, Validators.maxLength(255)]],
    description: [this.data.document.description ?? ''],
    type: [this.data.document.type, [Validators.required]],
    mandatoryRead: [this.data.document.mandatoryRead]
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly svc: DocumentsService,
    private readonly snack: MatSnackBar,
    private readonly dialogRef: MatDialogRef<DocumentsEditDialogComponent, DocumentResponse>,
    @Inject(MAT_DIALOG_DATA) public readonly data: DocumentsEditDialogData
  ) {}

  submit(): void {
    if (this.form.invalid || this.submitting) {
      this.form.markAllAsTouched();
      return;
    }
    this.submitting = true;
    const v = this.form.getRawValue();
    this.svc.update(this.data.document.id, {
      title: v.title.trim(),
      description: v.description?.trim() || undefined,
      type: v.type,
      mandatoryRead: v.mandatoryRead
    })
      .pipe(finalize(() => (this.submitting = false)))
      .subscribe({
        next: d => {
          this.snack.open($localize`:@@documents.edit.updated:Document mis à jour.`, $localize`:@@common.ok:OK`, { duration: 2500 });
          this.dialogRef.close(d);
        },
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[documents-edit] failed', err?.status, err?.error?.title);
          this.snack.open(safeErrorMessage(err, $localize`:@@common.error-update:Erreur lors de la mise à jour.`), $localize`:@@common.ok:OK`, { duration: 4000 });
        }
      });
  }

  cancel(): void { this.dialogRef.close(); }
}
