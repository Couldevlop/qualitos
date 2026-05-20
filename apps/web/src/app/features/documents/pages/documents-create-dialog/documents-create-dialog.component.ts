import { Component } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { finalize } from 'rxjs/operators';

import { AuthService } from '../../../../core/auth/auth.service';
import { safeErrorMessage } from '../../../../core/http/error-message';
import { DocumentsService } from '../../documents.service';
import { DocumentResponse, DocumentType } from '../../documents.types';

@Component({
  selector: 'qos-documents-create-dialog',
  templateUrl: './documents-create-dialog.component.html',
  styleUrls: ['./documents-create-dialog.component.scss'],
  standalone: false
})
export class DocumentsCreateDialogComponent {

  submitting = false;

  readonly types: { value: DocumentType; label: string }[] = [
    { value: 'POLICY',           label: 'Politique' },
    { value: 'PROCEDURE',        label: 'Procédure' },
    { value: 'WORK_INSTRUCTION', label: 'Mode opératoire' },
    { value: 'RECORD',           label: 'Enregistrement' },
    { value: 'FORM',             label: 'Formulaire' },
    { value: 'MANUAL',           label: 'Manuel' },
    { value: 'OTHER',            label: 'Autre' }
  ];

  readonly form = this.fb.nonNullable.group({
    code:  ['', [Validators.required, Validators.maxLength(100), Validators.pattern(/^[A-Z0-9._\-]+$/)]],
    title: ['', [Validators.required, Validators.maxLength(255)]],
    description: [''],
    type: ['PROCEDURE' as DocumentType, [Validators.required]],
    mandatoryRead: [false],
    initialContent: [''],
    initialChangeNote: ['Création initiale']
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly svc: DocumentsService,
    private readonly auth: AuthService,
    private readonly snack: MatSnackBar,
    private readonly dialogRef: MatDialogRef<DocumentsCreateDialogComponent, DocumentResponse>
  ) {}

  submit(): void {
    if (this.form.invalid || this.submitting) {
      this.form.markAllAsTouched();
      return;
    }
    const ownerId = this.auth.snapshot()?.userId;
    if (!ownerId) {
      this.snack.open('Session expirée — veuillez vous reconnecter.', 'OK', { duration: 4000 });
      return;
    }
    this.submitting = true;
    const v = this.form.getRawValue();
    this.svc.create({
      code: v.code.trim(),
      title: v.title.trim(),
      description: v.description?.trim() || undefined,
      type: v.type,
      ownerId,
      mandatoryRead: v.mandatoryRead,
      initialContent:    v.initialContent?.trim() || undefined,
      initialChangeNote: v.initialChangeNote?.trim() || undefined
    })
      .pipe(finalize(() => (this.submitting = false)))
      .subscribe({
        next: d => {
          this.snack.open('Document créé.', 'OK', { duration: 2500 });
          this.dialogRef.close(d);
        },
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[documents-create] failed', err?.status, err?.error?.title);
          this.snack.open(safeErrorMessage(err, 'Erreur lors de la création.'), 'OK', { duration: 4000 });
        }
      });
  }

  cancel(): void { this.dialogRef.close(); }
}
