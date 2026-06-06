import { Component, Inject } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { finalize } from 'rxjs/operators';

import { safeErrorMessage } from '../../../../core/http/error-message';
import { NcService } from '../../nc.service';
import { NcResponse } from '../../nc.types';

export interface NcResolveDialogData {
  ncId: string;
  reference: string;
}

@Component({
  selector: 'qos-nc-resolve-dialog',
  templateUrl: './nc-resolve-dialog.component.html',
  styleUrls: ['./nc-resolve-dialog.component.scss'],
  standalone: false
})
export class NcResolveDialogComponent {

  submitting = false;

  readonly form = this.fb.nonNullable.group({
    resolutionNote: ['', [Validators.required, Validators.maxLength(2000)]]
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly nc: NcService,
    private readonly snack: MatSnackBar,
    private readonly dialogRef: MatDialogRef<NcResolveDialogComponent, NcResponse>,
    @Inject(MAT_DIALOG_DATA) public readonly data: NcResolveDialogData
  ) {}

  submit(): void {
    if (this.form.invalid || this.submitting) {
      this.form.markAllAsTouched();
      return;
    }
    this.submitting = true;
    const { resolutionNote } = this.form.getRawValue();
    this.nc
      .resolve(this.data.ncId, { resolutionNote: resolutionNote.trim() })
      .pipe(finalize(() => (this.submitting = false)))
      .subscribe({
        next: resolved => {
          this.snack.open(
            $localize`:@@nc.resolve.success:Non-conformité résolue.`,
            $localize`:@@common.ok:OK`, { duration: 2500 });
          this.dialogRef.close(resolved);
        },
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[nc-resolve] failed', err?.status, err?.error?.title);
          this.snack.open(
            safeErrorMessage(err, $localize`:@@common.error-update:Erreur lors de la mise à jour.`),
            'OK', { duration: 4000 });
        }
      });
  }

  cancel(): void {
    this.dialogRef.close();
  }
}
