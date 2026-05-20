import { Component } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { finalize } from 'rxjs/operators';

import { AuthService } from '../../../../core/auth/auth.service';
import { safeErrorMessage } from '../../../../core/http/error-message';
import { FmeaService } from '../../fmea.service';
import { FmeaProjectResponse, FmeaType } from '../../fmea.types';

@Component({
  selector: 'qos-fmea-create-dialog',
  templateUrl: './fmea-create-dialog.component.html',
  styleUrls: ['./fmea-create-dialog.component.scss'],
  standalone: false
})
export class FmeaCreateDialogComponent {

  submitting = false;

  readonly types: { value: FmeaType; label: string }[] = [
    { value: 'PROCESS_FMEA', label: 'Processus (PFMEA)' },
    { value: 'DESIGN_FMEA',  label: 'Conception (DFMEA)' },
    { value: 'SYSTEM_FMEA',  label: 'Système' },
    { value: 'SERVICE_FMEA', label: 'Service' },
    { value: 'BOW_TIE',      label: 'Bow-tie (cyber/HSE)' }
  ];

  readonly form = this.fb.nonNullable.group({
    code: ['', [
      Validators.required, Validators.maxLength(120),
      Validators.pattern(/^[A-Za-z0-9][A-Za-z0-9._\-]{0,119}$/)
    ]],
    name: ['', [Validators.required, Validators.maxLength(250)]],
    scope: ['', [Validators.maxLength(1000)]],
    type: ['PROCESS_FMEA' as FmeaType, [Validators.required]],
    criticalRpnThreshold: [100, [Validators.required, Validators.min(1), Validators.max(1000)]]
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly svc: FmeaService,
    private readonly auth: AuthService,
    private readonly snack: MatSnackBar,
    private readonly dialogRef: MatDialogRef<FmeaCreateDialogComponent, FmeaProjectResponse>
  ) {}

  submit(): void {
    if (this.form.invalid || this.submitting) {
      this.form.markAllAsTouched();
      return;
    }
    const createdBy = this.auth.snapshot()?.userId;
    if (!createdBy) {
      this.snack.open('Session expirée — veuillez vous reconnecter.', 'OK', { duration: 4000 });
      return;
    }
    this.submitting = true;
    const v = this.form.getRawValue();
    this.svc.create({
      code: v.code.trim(),
      name: v.name.trim(),
      scope: v.scope?.trim() || undefined,
      type: v.type,
      criticalRpnThreshold: v.criticalRpnThreshold,
      createdBy
    })
      .pipe(finalize(() => (this.submitting = false)))
      .subscribe({
        next: p => {
          this.snack.open('Projet FMEA créé.', 'OK', { duration: 2500 });
          this.dialogRef.close(p);
        },
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[fmea-create] failed', err?.status, err?.error?.title);
          this.snack.open(safeErrorMessage(err, 'Erreur lors de la création.'), 'OK', { duration: 4000 });
        }
      });
  }

  cancel(): void { this.dialogRef.close(); }
}
