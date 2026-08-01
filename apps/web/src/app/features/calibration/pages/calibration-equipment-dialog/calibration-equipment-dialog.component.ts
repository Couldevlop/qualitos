import { Component, Inject } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Observable } from 'rxjs';
import { finalize } from 'rxjs/operators';

import { AuthService } from '../../../../core/auth/auth.service';
import { safeErrorMessage } from '../../../../core/http/error-message';
import { CalibrationService } from '../../calibration.service';
import { EquipmentResponse } from '../../calibration.types';

export interface CalibrationEquipmentDialogData {
  /** `null` = création ; sinon édition de l'instrument fourni. */
  equipment: EquipmentResponse | null;
}

/**
 * Création / modification d'un équipement (§4.10).
 *
 * Le code technique n'est modifiable qu'à la création : la route PATCH ne l'accepte
 * pas (unicité par tenant garantie côté serveur), donc on ne propose pas un champ
 * qui ne serait pas enregistré.
 */
@Component({
  selector: 'qos-calibration-equipment-dialog',
  templateUrl: './calibration-equipment-dialog.component.html',
  styleUrls: ['./calibration-equipment-dialog.component.scss'],
  standalone: false
})
export class CalibrationEquipmentDialogComponent {

  submitting = false;

  readonly isEdit: boolean;

  /** Le gabarit de dialogue reçoit un titre déjà localisé (entrée, pas contenu). */
  readonly title: string;
  readonly submitLabel: string;

  readonly form = this.fb.nonNullable.group({
    code: ['', [
      Validators.required, Validators.maxLength(120), Validators.minLength(2),
      Validators.pattern(/^[A-Za-z0-9][A-Za-z0-9._\-]{1,119}$/)
    ]],
    name: ['', [Validators.required, Validators.maxLength(250)]],
    manufacturer: ['', [Validators.maxLength(200)]],
    model: ['', [Validators.maxLength(200)]],
    serialNumber: ['', [Validators.maxLength(200)]],
    location: ['', [Validators.maxLength(500)]],
    critical: [false]
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly svc: CalibrationService,
    private readonly auth: AuthService,
    private readonly snack: MatSnackBar,
    private readonly dialogRef:
      MatDialogRef<CalibrationEquipmentDialogComponent, EquipmentResponse>,
    @Inject(MAT_DIALOG_DATA) public readonly data: CalibrationEquipmentDialogData
  ) {
    this.isEdit = !!data.equipment;
    this.title = this.isEdit
      ? $localize`:@@calibration.equipment.edit-title:Modifier l'équipement`
      : $localize`:@@calibration.equipment.create-title:Nouvel équipement`;
    this.submitLabel = this.isEdit
      ? $localize`:@@common.save:Enregistrer`
      : $localize`:@@common.create:Créer`;
    if (data.equipment) {
      const e = data.equipment;
      this.form.patchValue({
        code: e.code,
        name: e.name,
        manufacturer: e.manufacturer ?? '',
        model: e.model ?? '',
        serialNumber: e.serialNumber ?? '',
        location: e.location ?? '',
        critical: e.critical
      });
      this.form.controls.code.disable();
    }
  }

  submit(): void {
    if (this.form.invalid || this.submitting) { this.form.markAllAsTouched(); return; }
    const v = this.form.getRawValue();

    let call$: Observable<EquipmentResponse>;
    if (this.data.equipment) {
      call$ = this.svc.updateEquipment(this.data.equipment.id, {
        name: v.name.trim(),
        manufacturer: v.manufacturer.trim() || undefined,
        model: v.model.trim() || undefined,
        serialNumber: v.serialNumber.trim() || undefined,
        location: v.location.trim() || undefined,
        critical: v.critical
      });
    } else {
      // Le serveur exige un `createdBy` : sans session valide, l'appel échouerait en 400.
      const createdBy = this.auth.snapshot()?.userId;
      if (!createdBy) {
        this.snack.open(
          $localize`:@@common.session-expired:Session expirée — veuillez vous reconnecter.`,
          $localize`:@@common.ok:OK`, { duration: 4000 });
        return;
      }
      call$ = this.svc.createEquipment({
        code: v.code.trim(),
        name: v.name.trim(),
        manufacturer: v.manufacturer.trim() || undefined,
        model: v.model.trim() || undefined,
        serialNumber: v.serialNumber.trim() || undefined,
        location: v.location.trim() || undefined,
        critical: v.critical,
        createdBy
      });
    }

    this.submitting = true;
    call$.pipe(finalize(() => { this.submitting = false; })).subscribe({
      next: e => this.dialogRef.close(e),
      error: err => {
        // eslint-disable-next-line no-console
        console.warn('[calibration-equipment] failed', (err as { status?: number })?.status);
        this.snack.open(safeErrorMessage(err,
          $localize`:@@calibration.save-error:Erreur lors de l'enregistrement.`),
          $localize`:@@common.ok:OK`, { duration: 4000 });
      }
    });
  }

  cancel(): void { this.dialogRef.close(); }
}
