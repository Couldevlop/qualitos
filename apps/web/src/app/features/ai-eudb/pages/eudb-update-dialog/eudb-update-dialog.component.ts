import { Component, Inject } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { finalize } from 'rxjs/operators';

import { safeErrorMessage } from '../../../../core/http/error-message';
import { EudbService } from '../../eudb.service';
import { EudbView } from '../../eudb.types';

export interface EudbUpdateDialogData { id: string; }

@Component({
  selector: 'qos-eudb-update-dialog',
  templateUrl: './eudb-update-dialog.component.html',
  styleUrls: ['./eudb-update-dialog.component.scss'],
  standalone: false
})
export class EudbUpdateDialogComponent {

  submitting = false;

  readonly form = this.fb.nonNullable.group({
    updateSummary: ['', [Validators.required, Validators.maxLength(4000)]],
    updateDate:    ['', [Validators.required]]
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly svc: EudbService,
    private readonly snack: MatSnackBar,
    private readonly dialogRef: MatDialogRef<EudbUpdateDialogComponent, EudbView>,
    @Inject(MAT_DIALOG_DATA) public readonly data: EudbUpdateDialogData
  ) {}

  submit(): void {
    if (this.form.invalid || this.submitting) { this.form.markAllAsTouched(); return; }
    const v = this.form.getRawValue();
    if (new Date(v.updateDate).getTime() > Date.now()) {
      this.snack.open('La date de mise à jour ne peut pas être dans le futur.', 'OK', { duration: 4000 });
      return;
    }
    this.submitting = true;
    this.svc.declareUpdate(this.data.id, {
      updateSummary: v.updateSummary.trim(),
      updateDate: new Date(v.updateDate).toISOString()
    })
      .pipe(finalize(() => (this.submitting = false)))
      .subscribe({
        next: r => { this.snack.open('Mise à jour déclarée.', 'OK', { duration: 2200 }); this.dialogRef.close(r); },
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[eudb-update] failed', err?.status, err?.error?.title);
          this.snack.open(safeErrorMessage(err, 'Déclaration impossible.'), 'OK', { duration: 4000 });
        }
      });
  }

  cancel(): void { this.dialogRef.close(); }
}
