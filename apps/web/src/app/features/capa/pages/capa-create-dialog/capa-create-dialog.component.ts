import { Component } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { finalize } from 'rxjs/operators';

import { AuthService } from '../../../../core/auth/auth.service';
import { safeErrorMessage } from '../../../../core/http/error-message';
import { CapaService } from '../../capa.service';
import {
  CapaCaseResponse,
  CapaCriticity,
  CapaSourceType,
  CapaType
} from '../../capa.types';

@Component({
  selector: 'qos-capa-create-dialog',
  templateUrl: './capa-create-dialog.component.html',
  styleUrls: ['./capa-create-dialog.component.scss'],
  standalone: false
})
export class CapaCreateDialogComponent {

  submitting = false;

  readonly types: CapaType[] = ['CORRECTIVE', 'PREVENTIVE'];
  readonly criticities: CapaCriticity[] = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'];
  readonly sourceTypes: { value: CapaSourceType; label: string }[] = [
    { value: 'NON_CONFORMITY', label: $localize`:@@capa.source.non-conformity:Non-conformité` },
    { value: 'AUDIT',          label: $localize`:@@capa.source.audit:Audit` },
    { value: 'COMPLAINT',      label: $localize`:@@capa.source.complaint:Réclamation client` },
    { value: 'INTERNAL',       label: $localize`:@@capa.source.internal:Détection interne` },
    { value: 'IOT_ALERT',      label: $localize`:@@capa.source.iot-alert:Alerte IoT` },
    { value: 'OTHER',          label: $localize`:@@capa.source.other:Autre` }
  ];

  readonly form = this.fb.nonNullable.group({
    title: ['', [Validators.required, Validators.maxLength(255)]],
    description: [''],
    type: ['CORRECTIVE' as CapaType, [Validators.required]],
    criticity: ['MEDIUM' as CapaCriticity, [Validators.required]],
    sourceType: ['INTERNAL' as CapaSourceType, [Validators.required]],
    sourceRef: ['', [Validators.maxLength(255)]],
    dueDate: ['']
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly capa: CapaService,
    private readonly auth: AuthService,
    private readonly snack: MatSnackBar,
    private readonly dialogRef: MatDialogRef<CapaCreateDialogComponent, CapaCaseResponse>
  ) {}

  submit(): void {
    if (this.form.invalid || this.submitting) {
      this.form.markAllAsTouched();
      return;
    }
    const ownerId = this.auth.snapshot()?.userId;
    if (!ownerId) {
      this.snack.open($localize`:@@common.session-expired:Session expirée — veuillez vous reconnecter.`, $localize`:@@common.ok:OK`, { duration: 4000 });
      return;
    }
    this.submitting = true;
    const v = this.form.getRawValue();
    this.capa
      .createCase({
        title: v.title.trim(),
        description: v.description?.trim() || undefined,
        type: v.type,
        criticity: v.criticity,
        sourceType: v.sourceType,
        sourceRef: v.sourceRef?.trim() || undefined,
        dueDate: v.dueDate || undefined,
        ownerId
      })
      .pipe(finalize(() => (this.submitting = false)))
      .subscribe({
        next: c => {
          this.snack.open($localize`:@@capa.create.success:Cas CAPA créé.`, $localize`:@@common.ok:OK`, { duration: 2500 });
          this.dialogRef.close(c);
        },
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[capa-create] failed', err?.status, err?.error?.title);
          this.snack.open(
            safeErrorMessage(err, $localize`:@@common.error-create:Erreur lors de la création.`),
            $localize`:@@common.ok:OK`,
            { duration: 4000 }
          );
        }
      });
  }

  cancel(): void {
    this.dialogRef.close();
  }
}
