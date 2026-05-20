import { Component, Inject } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { finalize } from 'rxjs/operators';

import { AuthService } from '../../../../core/auth/auth.service';
import { safeErrorMessage } from '../../../../core/http/error-message';
import { CirclesService } from '../../circles.service';
import { CircleProposalResponse } from '../../circles.types';

export interface CirclesProposalDialogData {
  circleId: string;
}

@Component({
  selector: 'qos-circles-proposal-dialog',
  templateUrl: './circles-proposal-dialog.component.html',
  styleUrls: ['./circles-proposal-dialog.component.scss'],
  standalone: false
})
export class CirclesProposalDialogComponent {
  submitting = false;

  readonly form = this.fb.nonNullable.group({
    title: ['', [Validators.required, Validators.maxLength(255)]],
    description: ['']
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly circles: CirclesService,
    private readonly auth: AuthService,
    private readonly snack: MatSnackBar,
    private readonly dialogRef: MatDialogRef<CirclesProposalDialogComponent, CircleProposalResponse>,
    @Inject(MAT_DIALOG_DATA) public readonly data: CirclesProposalDialogData
  ) {}

  submit(): void {
    if (this.form.invalid || this.submitting) {
      this.form.markAllAsTouched();
      return;
    }
    const proposedBy = this.auth.snapshot()?.userId;
    if (!proposedBy) {
      this.snack.open('Session expirée — veuillez vous reconnecter.', 'OK', { duration: 4000 });
      return;
    }
    this.submitting = true;
    const v = this.form.getRawValue();
    this.circles
      .addProposal(this.data.circleId, {
        title: v.title.trim(),
        description: v.description?.trim() || undefined,
        proposedBy
      })
      .pipe(finalize(() => (this.submitting = false)))
      .subscribe({
        next: p => {
          this.snack.open('Proposition enregistrée.', 'OK', { duration: 2500 });
          this.dialogRef.close(p);
        },
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[circles-proposal-dialog] failed', err?.status, err?.error?.title);
          this.snack.open(
            safeErrorMessage(err, 'Erreur lors de l\'enregistrement.'),
            'OK', { duration: 4000 }
          );
        }
      });
  }

  cancel(): void { this.dialogRef.close(); }
}
