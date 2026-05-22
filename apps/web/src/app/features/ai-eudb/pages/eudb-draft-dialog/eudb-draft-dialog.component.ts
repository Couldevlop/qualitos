import { Component } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { finalize } from 'rxjs/operators';

import { AuthService } from '../../../../core/auth/auth.service';
import { safeErrorMessage } from '../../../../core/http/error-message';
import { EudbService } from '../../eudb.service';
import { EudbView } from '../../eudb.types';

const UUID_REGEX = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

@Component({
  selector: 'qos-eudb-draft-dialog',
  templateUrl: './eudb-draft-dialog.component.html',
  styleUrls: ['./eudb-draft-dialog.component.scss'],
  standalone: false
})
export class EudbDraftDialogComponent {

  submitting = false;

  readonly form = this.fb.nonNullable.group({
    reference:  ['', [Validators.required, Validators.maxLength(64), Validators.pattern(/^[A-Z][A-Z0-9_-]{1,63}$/)]],
    aiSystemId: ['', [Validators.required, Validators.pattern(UUID_REGEX)]],
    providerEntityName:              ['', [Validators.maxLength(250)]],
    providerEuRepresentative:        ['', [Validators.maxLength(250)]],
    memberStateOfReference:          ['', [Validators.pattern(/^$|^[A-Z]{2}$/)]],
    intendedPurposeSummary:          ['', [Validators.maxLength(4000)]],
    technicalDocumentationReference: ['', [Validators.maxLength(250)]]
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly svc: EudbService,
    private readonly auth: AuthService,
    private readonly snack: MatSnackBar,
    private readonly dialogRef: MatDialogRef<EudbDraftDialogComponent, EudbView>
  ) {}

  submit(): void {
    if (this.form.invalid || this.submitting) { this.form.markAllAsTouched(); return; }
    const userId = this.auth.snapshot()?.userId;
    if (!userId) {
      this.snack.open('Session expirée — veuillez vous reconnecter.', 'OK', { duration: 4000 });
      return;
    }
    this.submitting = true;
    const v = this.form.getRawValue();
    this.svc.draft({
      reference: v.reference.trim(),
      aiSystemId: v.aiSystemId.trim(),
      providerEntityName: v.providerEntityName?.trim() || undefined,
      providerEuRepresentative: v.providerEuRepresentative?.trim() || undefined,
      memberStateOfReference: v.memberStateOfReference?.trim().toUpperCase() || undefined,
      intendedPurposeSummary: v.intendedPurposeSummary?.trim() || undefined,
      technicalDocumentationReference: v.technicalDocumentationReference?.trim() || undefined,
      createdByUserId: userId
    })
      .pipe(finalize(() => (this.submitting = false)))
      .subscribe({
        next: r => this.dialogRef.close(r),
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[eudb-draft] failed', err?.status, err?.error?.title);
          this.snack.open(safeErrorMessage(err, 'Création impossible.'), 'OK', { duration: 4000 });
        }
      });
  }

  cancel(): void { this.dialogRef.close(); }
}
