import { Component, Inject } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { finalize } from 'rxjs/operators';

import { AuthService } from '../../../../core/auth/auth.service';
import { safeErrorMessage } from '../../../../core/http/error-message';
import { DocumentsService } from '../../documents.service';
import { DocumentVersionResponse } from '../../documents.types';

export interface DocumentsVersionDialogData {
  documentId: string;
  nextVersionNumber: number;
}

@Component({
  selector: 'qos-documents-version-dialog',
  templateUrl: './documents-version-dialog.component.html',
  styleUrls: ['./documents-version-dialog.component.scss'],
  standalone: false
})
export class DocumentsVersionDialogComponent {

  submitting = false;

  readonly form = this.fb.nonNullable.group({
    changeNote: ['', [Validators.required, Validators.maxLength(255)]],
    content: [''],
    contentUri: ['', [Validators.maxLength(1024)]]
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly svc: DocumentsService,
    private readonly auth: AuthService,
    private readonly snack: MatSnackBar,
    private readonly dialogRef: MatDialogRef<DocumentsVersionDialogComponent, DocumentVersionResponse>,
    @Inject(MAT_DIALOG_DATA) public readonly data: DocumentsVersionDialogData
  ) {}

  submit(): void {
    if (this.form.invalid || this.submitting) {
      this.form.markAllAsTouched();
      return;
    }
    const authorId = this.auth.snapshot()?.userId;
    if (!authorId) {
      this.snack.open($localize`:@@common.session-expired:Session expirée — veuillez vous reconnecter.`, $localize`:@@common.ok:OK`, { duration: 4000 });
      return;
    }
    this.submitting = true;
    const v = this.form.getRawValue();
    this.svc.createVersion(this.data.documentId, {
      authorId,
      changeNote: v.changeNote.trim(),
      content:    v.content?.trim()    || undefined,
      contentUri: v.contentUri?.trim() || undefined
    })
      .pipe(finalize(() => (this.submitting = false)))
      .subscribe({
        next: ver => {
          this.snack.open($localize`:@@documents.version.created:Nouvelle version créée (DRAFT).`, $localize`:@@common.ok:OK`, { duration: 2500 });
          this.dialogRef.close(ver);
        },
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[documents-version] failed', err?.status, err?.error?.title);
          this.snack.open(safeErrorMessage(err, $localize`:@@common.error-create:Erreur lors de la création.`), $localize`:@@common.ok:OK`, { duration: 4000 });
        }
      });
  }

  cancel(): void { this.dialogRef.close(); }
}
