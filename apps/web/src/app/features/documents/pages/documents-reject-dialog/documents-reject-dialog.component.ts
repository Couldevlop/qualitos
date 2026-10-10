import { Component, Inject } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { finalize } from 'rxjs/operators';

import { safeErrorMessage } from '../../../../core/http/error-message';
import { DocumentsService } from '../../documents.service';
import { DocumentVersionResponse } from '../../documents.types';

export interface DocumentsRejectDialogData {
  version: DocumentVersionResponse;
}

export const REASON_MAX = 1000;

/**
 * Refuser une version en revue (ADR 0080) : la raison est obligatoire, parce
 * que l'auteur la lira en retrouvant sa version en brouillon.
 */
@Component({
  selector: 'qos-documents-reject-dialog',
  templateUrl: './documents-reject-dialog.component.html',
  styleUrls: ['./documents-reject-dialog.component.scss'],
  standalone: false
})
export class DocumentsRejectDialogComponent {

  readonly reasonMax = REASON_MAX;
  submitting = false;

  readonly form = this.fb.nonNullable.group({
    reason: ['', [Validators.required, Validators.maxLength(REASON_MAX), Validators.pattern(/\S/)]]
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly svc: DocumentsService,
    private readonly snack: MatSnackBar,
    private readonly dialogRef: MatDialogRef<DocumentsRejectDialogComponent, DocumentVersionResponse>,
    @Inject(MAT_DIALOG_DATA) public readonly data: DocumentsRejectDialogData
  ) {}

  get title(): string {
    return $localize`:@@documents.reject.title:Refuser la version v${this.data.version.versionNumber}:number:`;
  }

  submit(): void {
    if (this.form.invalid || this.submitting) {
      this.form.markAllAsTouched();
      return;
    }
    this.submitting = true;
    const v = this.data.version;
    this.svc.reject(v.documentId, v.id, this.form.getRawValue().reason.trim())
      .pipe(finalize(() => (this.submitting = false)))
      .subscribe({
        next: refusee => {
          this.snack.open($localize`:@@documents.reject.done:Version refusée : elle revient en brouillon chez son auteur.`,
            $localize`:@@common.ok:OK`, { duration: 3000 });
          this.dialogRef.close(refusee);
        },
        error: err => this.snack.open(
          safeErrorMessage(err, $localize`:@@documents.reject.failed:Refus impossible.`),
          $localize`:@@common.ok:OK`, { duration: 4000 })
      });
  }

  cancel(): void {
    this.dialogRef.close();
  }
}
