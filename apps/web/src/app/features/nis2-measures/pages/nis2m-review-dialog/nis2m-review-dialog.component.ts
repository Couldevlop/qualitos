import { Component, Inject } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { finalize } from 'rxjs/operators';
import { Observable } from 'rxjs';

import { AuthService } from '../../../../core/auth/auth.service';
import { safeErrorMessage } from '../../../../core/http/error-message';
import { Nis2MeasuresService } from '../../nis2m.service';
import { Nis2MeasureView } from '../../nis2m.types';

export interface Nis2mReviewDialogData {
  id: string;
  mode: 'VERIFY' | 'REVIEW';
}

@Component({
  selector: 'qos-nis2m-review-dialog',
  templateUrl: './nis2m-review-dialog.component.html',
  styleUrls: ['./nis2m-review-dialog.component.scss'],
  standalone: false
})
export class Nis2mReviewDialogComponent {

  submitting = false;

  readonly form = this.fb.nonNullable.group({
    reviewedAt: [new Date().toISOString().slice(0, 16), [Validators.required]]
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly svc: Nis2MeasuresService,
    private readonly auth: AuthService,
    private readonly snack: MatSnackBar,
    private readonly dialogRef: MatDialogRef<Nis2mReviewDialogComponent, Nis2MeasureView>,
    @Inject(MAT_DIALOG_DATA) public readonly data: Nis2mReviewDialogData
  ) {}

  get title(): string {
    return this.data.mode === 'VERIFY'
      ? 'Vérifier la mesure (passe en VERIFIED)'
      : 'Effectuer une revue périodique';
  }

  get hint(): string {
    return this.data.mode === 'VERIFY'
      ? 'Confirme que la mesure a été testée et est effective. Démarre le cycle de revues périodiques.'
      : 'Enregistre une revue périodique. La prochaine échéance est recalculée automatiquement.';
  }

  submit(): void {
    if (this.form.invalid || this.submitting) { this.form.markAllAsTouched(); return; }
    const v = this.form.getRawValue();
    if (new Date(v.reviewedAt).getTime() > Date.now()) {
      this.snack.open('La date de revue ne peut pas être dans le futur.', 'OK', { duration: 4000 });
      return;
    }
    const userId = this.auth.snapshot()?.userId;
    if (!userId) {
      this.snack.open('Session expirée — veuillez vous reconnecter.', 'OK', { duration: 4000 });
      return;
    }
    this.submitting = true;
    const payload = { reviewedByUserId: userId, reviewedAt: new Date(v.reviewedAt).toISOString() };
    const op$: Observable<Nis2MeasureView> = this.data.mode === 'VERIFY'
      ? this.svc.verify(this.data.id, payload)
      : this.svc.review(this.data.id, payload);
    op$
      .pipe(finalize(() => (this.submitting = false)))
      .subscribe({
        next: m => { this.snack.open(this.data.mode === 'VERIFY' ? 'Mesure vérifiée.' : 'Revue enregistrée.',
                                     'OK', { duration: 2200 }); this.dialogRef.close(m); },
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[nis2m-review] failed', err?.status, err?.error?.title);
          this.snack.open(safeErrorMessage(err, 'Opération impossible.'), 'OK', { duration: 4000 });
        }
      });
  }

  cancel(): void { this.dialogRef.close(); }
}
