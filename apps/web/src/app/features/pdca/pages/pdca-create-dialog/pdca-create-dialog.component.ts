import { Component } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { finalize } from 'rxjs/operators';

import { AuthService } from '../../../../core/auth/auth.service';
import { safeErrorMessage } from '../../../../core/http/error-message';
import { PdcaService } from '../../pdca.service';
import { PdcaCycleResponse } from '../../pdca.types';

@Component({
  selector: 'qos-pdca-create-dialog',
  templateUrl: './pdca-create-dialog.component.html',
  styleUrls: ['./pdca-create-dialog.component.scss'],
  standalone: false
})
export class PdcaCreateDialogComponent {

  submitting = false;

  readonly form = this.fb.nonNullable.group({
    title: ['', [Validators.required, Validators.maxLength(255)]],
    description: ['']
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly pdca: PdcaService,
    private readonly auth: AuthService,
    private readonly snack: MatSnackBar,
    private readonly dialogRef: MatDialogRef<PdcaCreateDialogComponent, PdcaCycleResponse>
  ) {}

  submit(): void {
    if (this.form.invalid || this.submitting) {
      this.form.markAllAsTouched();
      return;
    }
    const ownerId = this.auth.snapshot()?.userId;
    if (!ownerId) {
      this.snack.open($localize`:@@common.session-expired:Session expirée — veuillez vous reconnecter.`, 'OK', { duration: 4000 });
      return;
    }
    this.submitting = true;
    const { title, description } = this.form.getRawValue();
    this.pdca
      .createCycle({ title: title.trim(), description: description?.trim() || undefined, ownerId })
      .pipe(finalize(() => (this.submitting = false)))
      .subscribe({
        next: cycle => {
          this.snack.open($localize`:@@pdca.create.success:Cycle PDCA créé.`, 'OK', { duration: 2500 });
          this.dialogRef.close(cycle);
        },
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[pdca-create] failed', err?.status, err?.error?.title);
          this.snack.open(
            safeErrorMessage(err, $localize`:@@common.error-create:Erreur lors de la création.`),
            'OK',
            { duration: 4000 }
          );
        }
      });
  }

  cancel(): void {
    this.dialogRef.close();
  }
}
