import { Component, Inject } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { finalize } from 'rxjs/operators';

import { safeErrorMessage } from '../../../../core/http/error-message';
import { BreachService } from '../../breach.service';
import { BreachView } from '../../breach.types';

export interface BreachSubjectsDialogData { id: string; }

@Component({
  selector: 'qos-breach-subjects-dialog',
  templateUrl: './breach-subjects-dialog.component.html',
  styleUrls: ['./breach-subjects-dialog.component.scss'],
  standalone: false
})
export class BreachSubjectsDialogComponent {

  submitting = false;

  readonly form = this.fb.nonNullable.group({
    notifiedAt: [new Date().toISOString().slice(0, 16), [Validators.required]],
    channel:    ['', [Validators.required, Validators.maxLength(250)]]
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly svc: BreachService,
    private readonly snack: MatSnackBar,
    private readonly dialogRef: MatDialogRef<BreachSubjectsDialogComponent, BreachView>,
    @Inject(MAT_DIALOG_DATA) public readonly data: BreachSubjectsDialogData
  ) {}

  submit(): void {
    if (this.form.invalid || this.submitting) { this.form.markAllAsTouched(); return; }
    const v = this.form.getRawValue();
    if (new Date(v.notifiedAt).getTime() > Date.now()) {
      this.snack.open('La date de notification ne peut pas être dans le futur.', 'OK', { duration: 4000 });
      return;
    }
    this.submitting = true;
    this.svc.notifySubjects(this.data.id, {
      notifiedAt: new Date(v.notifiedAt).toISOString(),
      channel: v.channel.trim()
    })
      .pipe(finalize(() => (this.submitting = false)))
      .subscribe({
        next: b => { this.snack.open('Personnes notifiées.', 'OK', { duration: 2200 }); this.dialogRef.close(b); },
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[breach-subj] failed', err?.status, err?.error?.title);
          this.snack.open(safeErrorMessage(err, 'Notification impossible.'), 'OK', { duration: 4000 });
        }
      });
  }

  cancel(): void { this.dialogRef.close(); }
}
