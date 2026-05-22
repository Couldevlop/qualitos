import { Component, Inject } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { finalize } from 'rxjs/operators';

import { AuthService } from '../../../../core/auth/auth.service';
import { safeErrorMessage } from '../../../../core/http/error-message';
import { SubjectRequestsService } from '../../subject-requests.service';
import { SubjectRequestView } from '../../subject-requests.types';

export interface SrCompleteDialogData { requestId: string; }

@Component({
  selector: 'qos-sr-complete-dialog',
  templateUrl: './sr-complete-dialog.component.html',
  styleUrls: ['./sr-complete-dialog.component.scss'],
  standalone: false
})
export class SrCompleteDialogComponent {

  submitting = false;

  // OWASP A03 — resolutionNotes obligatoire pour la traçabilité;
  // evidenceUrl restreint à https:// quand renseigné (blocque http/js/data).
  readonly form = this.fb.nonNullable.group({
    resolutionNotes: ['', [Validators.required, Validators.maxLength(4000)]],
    evidenceUrl:     ['', [Validators.maxLength(1024), Validators.pattern(/^https:\/\/.+/)]]
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly svc: SubjectRequestsService,
    private readonly auth: AuthService,
    private readonly snack: MatSnackBar,
    private readonly dialogRef: MatDialogRef<SrCompleteDialogComponent, SubjectRequestView>,
    @Inject(MAT_DIALOG_DATA) public readonly data: SrCompleteDialogData
  ) {
    // Pattern only applies when value is non-empty.
    this.form.controls.evidenceUrl.valueChanges.subscribe(v => {
      if (!v) this.form.controls.evidenceUrl.setErrors(null);
    });
  }

  submit(): void {
    if (this.form.invalid || this.submitting) { this.form.markAllAsTouched(); return; }
    this.submitting = true;
    const v = this.form.getRawValue();
    this.svc.complete(this.data.requestId, {
      resolutionNotes: v.resolutionNotes.trim(),
      evidenceUrl: v.evidenceUrl?.trim() || undefined,
      handledByUserId: this.auth.snapshot()?.userId
    })
      .pipe(finalize(() => (this.submitting = false)))
      .subscribe({
        next: r => { this.snack.open('Demande clôturée.', 'OK', { duration: 2500 }); this.dialogRef.close(r); },
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[sr-complete] failed', err?.status, err?.error?.title);
          this.snack.open(safeErrorMessage(err, 'Clôture impossible.'), 'OK', { duration: 4000 });
        }
      });
  }

  cancel(): void { this.dialogRef.close(); }
}
