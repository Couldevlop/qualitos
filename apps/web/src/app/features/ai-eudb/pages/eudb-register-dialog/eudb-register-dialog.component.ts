import { Component, Inject } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { finalize } from 'rxjs/operators';

import { safeErrorMessage } from '../../../../core/http/error-message';
import { EudbService } from '../../eudb.service';
import { EudbView } from '../../eudb.types';

export interface EudbRegisterDialogData { id: string; }

@Component({
  selector: 'qos-eudb-register-dialog',
  templateUrl: './eudb-register-dialog.component.html',
  styleUrls: ['./eudb-register-dialog.component.scss'],
  standalone: false
})
export class EudbRegisterDialogComponent {

  submitting = false;

  readonly form = this.fb.nonNullable.group({
    eudbId: ['', [Validators.required, Validators.maxLength(64),
                  Validators.pattern(/^EUDB-AI-[A-Z0-9]{6,32}$/)]],
    registrationDate: ['', [Validators.required]]
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly svc: EudbService,
    private readonly snack: MatSnackBar,
    private readonly dialogRef: MatDialogRef<EudbRegisterDialogComponent, EudbView>,
    @Inject(MAT_DIALOG_DATA) public readonly data: EudbRegisterDialogData
  ) {}

  submit(): void {
    if (this.form.invalid || this.submitting) { this.form.markAllAsTouched(); return; }
    const v = this.form.getRawValue();
    if (new Date(v.registrationDate).getTime() > Date.now()) {
      this.snack.open('La date d\'enregistrement ne peut pas être dans le futur.', 'OK', { duration: 4000 });
      return;
    }
    this.submitting = true;
    this.svc.markRegistered(this.data.id, {
      eudbId: v.eudbId.trim(),
      registrationDate: new Date(v.registrationDate).toISOString()
    })
      .pipe(finalize(() => (this.submitting = false)))
      .subscribe({
        next: r => { this.snack.open('Enregistrement validé.', 'OK', { duration: 2200 }); this.dialogRef.close(r); },
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[eudb-register] failed', err?.status, err?.error?.title);
          this.snack.open(safeErrorMessage(err, 'Marquage impossible.'), 'OK', { duration: 4000 });
        }
      });
  }

  cancel(): void { this.dialogRef.close(); }
}
