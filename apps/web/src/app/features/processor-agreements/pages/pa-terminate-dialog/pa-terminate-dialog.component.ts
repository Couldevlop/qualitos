import { Component, Inject } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { finalize } from 'rxjs/operators';

import { safeErrorMessage } from '../../../../core/http/error-message';
import { PaService } from '../../pa.service';
import { PaView } from '../../pa.types';

export interface PaTerminateDialogData { agreementId: string; }

@Component({
  selector: 'qos-pa-terminate-dialog',
  templateUrl: './pa-terminate-dialog.component.html',
  styleUrls: ['./pa-terminate-dialog.component.scss'],
  standalone: false
})
export class PaTerminateDialogComponent {

  submitting = false;

  readonly form = this.fb.nonNullable.group({
    reason: ['', [Validators.required, Validators.maxLength(2000)]]
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly svc: PaService,
    private readonly snack: MatSnackBar,
    private readonly dialogRef: MatDialogRef<PaTerminateDialogComponent, PaView>,
    @Inject(MAT_DIALOG_DATA) public readonly data: PaTerminateDialogData
  ) {}

  submit(): void {
    if (this.form.invalid || this.submitting) { this.form.markAllAsTouched(); return; }
    this.submitting = true;
    const reason = this.form.getRawValue().reason.trim();
    this.svc.terminate(this.data.agreementId, { reason })
      .pipe(finalize(() => (this.submitting = false)))
      .subscribe({
        next: a => { this.snack.open($localize`:@@dpa.terminate-dialog.terminated:DPA résilié.`, $localize`:@@common.ok:OK`, { duration: 2500 }); this.dialogRef.close(a); },
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[pa-terminate] failed', err?.status, err?.error?.title);
          this.snack.open(safeErrorMessage(err, $localize`:@@dpa.terminate-dialog.terminate-failed:Résiliation impossible.`), $localize`:@@common.ok:OK`, { duration: 4000 });
        }
      });
  }

  cancel(): void { this.dialogRef.close(); }
}
