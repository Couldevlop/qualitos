import { Component } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { finalize } from 'rxjs/operators';

import { AuthService } from '../../../../core/auth/auth.service';
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
    { value: 'NON_CONFORMITY', label: 'Non-conformité' },
    { value: 'AUDIT',          label: 'Audit' },
    { value: 'COMPLAINT',      label: 'Réclamation client' },
    { value: 'INTERNAL',       label: 'Détection interne' },
    { value: 'IOT_ALERT',      label: 'Alerte IoT' },
    { value: 'OTHER',          label: 'Autre' }
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
      this.snack.open('Session expirée — veuillez vous reconnecter.', 'OK', { duration: 4000 });
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
          this.snack.open('Cas CAPA créé.', 'OK', { duration: 2500 });
          this.dialogRef.close(c);
        },
        error: err => {
          this.snack.open(
            err?.error?.message ?? err?.message ?? 'Erreur lors de la création',
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
