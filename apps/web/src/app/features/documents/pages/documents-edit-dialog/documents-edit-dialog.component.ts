import { Component, Inject } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { finalize } from 'rxjs/operators';

import { safeErrorMessage } from '../../../../core/http/error-message';
import { FolderRow } from '../../documents.folders';
import { DOCUMENT_TYPE_LABELS, DOCUMENT_TYPES } from '../../documents.labels';
import { DocumentsService } from '../../documents.service';
import { DocumentProcess, DocumentResponse, UpdateDocumentRequest } from '../../documents.types';

export interface DocumentsEditDialogData {
  document: DocumentResponse;
  folders?: FolderRow[];
  processes?: DocumentProcess[];
}

@Component({
  selector: 'qos-documents-edit-dialog',
  templateUrl: './documents-edit-dialog.component.html',
  styleUrls: ['./documents-edit-dialog.component.scss'],
  standalone: false
})
export class DocumentsEditDialogComponent {

  submitting = false;

  readonly types = DOCUMENT_TYPES.map(value => ({ value, label: DOCUMENT_TYPE_LABELS[value] }));
  readonly folders: FolderRow[] = this.data.folders ?? [];
  readonly processes: DocumentProcess[] = this.data.processes ?? [];
  /** L'échéance de revue ne se règle qu'une fois le document publié. */
  readonly reviewEditable = (this.data.document.lifecycle ?? 'DRAFT') !== 'DRAFT';

  readonly form = this.fb.nonNullable.group({
    title: [this.data.document.title, [Validators.required, Validators.maxLength(255)]],
    description: [this.data.document.description ?? ''],
    type: [this.data.document.type, [Validators.required]],
    mandatoryRead: [this.data.document.mandatoryRead],
    folderId: [this.data.document.folderId ?? null],
    processId: [this.data.document.processId ?? null],
    ownerId: [this.data.document.ownerId as string | null, [Validators.required]],
    nextReviewDate: [this.data.document.nextReviewDate ?? '']
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
    const d = this.data.document;
    const body: UpdateDocumentRequest = {
      title: v.title.trim(),
      description: v.description?.trim() || undefined,
      type: v.type,
      mandatoryRead: v.mandatoryRead,
      ownerId: v.ownerId ?? undefined,
      folderId: v.folderId ?? undefined,
      processId: v.processId ?? undefined,
      // Vider un champ se dit explicitement : son absence veut dire « inchangé ».
      clearFolder: !v.folderId && !!d.folderId ? true : undefined,
      clearProcess: !v.processId && !!d.processId ? true : undefined
    };
    if (this.reviewEditable && v.nextReviewDate && v.nextReviewDate !== d.nextReviewDate) {
      body.nextReviewDate = v.nextReviewDate;
    }
    this.svc.update(d.id, body)
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
