import { Component, Inject } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { finalize } from 'rxjs/operators';

import { safeErrorMessage } from '../../../../core/http/error-message';
import { BreachService } from '../../breach.service';
import { BreachView } from '../../breach.types';

export interface BreachDpaDialogData { id: string; }

@Component({
  selector: 'qos-breach-dpa-dialog',
  templateUrl: './breach-dpa-dialog.component.html',
  styleUrls: ['./breach-dpa-dialog.component.scss'],
  standalone: false
})
export class BreachDpaDialogComponent {

  submitting = false;

  readonly form = this.fb.nonNullable.group({
    notifiedAt: [new Date().toISOString().slice(0, 16), [Validators.required]],
    reference:  ['', [Validators.required, Validators.maxLength(250)]]
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly svc: BreachService,
    private readonly snack: MatSnackBar,
    private readonly dialogRef: MatDialogRef<BreachDpaDialogComponent, BreachView>,
    @Inject(MAT_DIALOG_DATA) public readonly data: BreachDpaDialogData
  ) {}

  submit(): void {
    if (this.form.invalid || this.submitting) { this.form.markAllAsTouched(); return; }
    const v = this.form.getRawValue();
    if (new Date(v.notifiedAt).getTime() > Date.now()) {
      this.snack.open($localize`:@@breach.notif-date-future:La date de notification ne peut pas être dans le futur.`, $localize`:@@common.ok:OK`, { duration: 4000 });
      return;
    }
    this.submitting = true;
    this.svc.notifyDpa(this.data.id, {
      notifiedAt: new Date(v.notifiedAt).toISOString(),
      reference: v.reference.trim()
    })
      .pipe(finalize(() => (this.submitting = false)))
      .subscribe({
        next: b => { this.snack.open($localize`:@@breach.dpa.notified-toast:CNIL notifiée.`, $localize`:@@common.ok:OK`, { duration: 2200 }); this.dialogRef.close(b); },
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[breach-dpa] failed', err?.status, err?.error?.title);
          this.snack.open(safeErrorMessage(err, $localize`:@@breach.notification-failed:Notification impossible.`), $localize`:@@common.ok:OK`, { duration: 4000 });
        }
      });
  }

  cancel(): void { this.dialogRef.close(); }
}
