import { Component, Inject } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Observable } from 'rxjs';
import { finalize } from 'rxjs/operators';

import { AuthService } from '../../../../core/auth/auth.service';
import { safeErrorMessage } from '../../../../core/http/error-message';
import { protocolLabel, typeLabel } from '../../iot.labels';
import { IotService } from '../../iot.service';
import {
  DEVICE_TYPES, DeviceResponse, IotDeviceType, IotProtocol, PROTOCOLS
} from '../../iot.types';

export interface IotDeviceDialogData {
  /** `null` = enregistrement d'un nouvel équipement ; sinon édition. */
  device: DeviceResponse | null;
}

/**
 * Enregistrement / modification d'un équipement IoT (§9.6).
 *
 * Le code technique n'est proposé qu'à la création : la route PATCH ne l'accepte
 * pas (unicité par tenant garantie côté serveur), on n'affiche donc pas un champ
 * dont la saisie serait perdue.
 */
@Component({
  selector: 'qos-iot-device-dialog',
  templateUrl: './iot-device-dialog.component.html',
  styleUrls: ['./iot-device-dialog.component.scss'],
  standalone: false
})
export class IotDeviceDialogComponent {

  readonly deviceTypes = DEVICE_TYPES;
  readonly protocols = PROTOCOLS;

  submitting = false;

  readonly isEdit: boolean;
  readonly title: string;
  readonly submitLabel: string;

  /**
   * Le motif reprend celui du serveur (`IotDto.CreateDeviceRequest`) : un refus
   * doit se voir dans le formulaire, pas revenir en 400 après l'envoi.
   */
  readonly form = this.fb.nonNullable.group({
    code: ['', [
      Validators.required, Validators.maxLength(120),
      Validators.pattern(/^[A-Za-z0-9][A-Za-z0-9._\-]{0,119}$/)
    ]],
    name: ['', [Validators.required, Validators.maxLength(200)]],
    deviceType: ['SENSOR_GENERIC' as IotDeviceType, [Validators.required]],
    protocol: ['MQTT' as IotProtocol, [Validators.required]],
    location: ['', [Validators.maxLength(500)]],
    description: ['', [Validators.maxLength(1000)]]
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly svc: IotService,
    private readonly auth: AuthService,
    private readonly snack: MatSnackBar,
    private readonly dialogRef: MatDialogRef<IotDeviceDialogComponent, DeviceResponse>,
    @Inject(MAT_DIALOG_DATA) public readonly data: IotDeviceDialogData
  ) {
    this.isEdit = !!data.device;
    this.title = this.isEdit
      ? $localize`:@@iot.device-dialog.edit-title:Modifier l'équipement`
      : $localize`:@@iot.device-dialog.create-title:Nouvel équipement IoT`;
    this.submitLabel = this.isEdit
      ? $localize`:@@common.save:Enregistrer`
      : $localize`:@@common.create:Créer`;

    if (data.device) {
      const d = data.device;
      this.form.patchValue({
        code: d.code,
        name: d.name,
        deviceType: d.deviceType,
        protocol: d.protocol,
        location: d.location ?? '',
        description: d.description ?? ''
      });
      this.form.controls.code.disable();
    }
  }

  submit(): void {
    if (this.form.invalid || this.submitting) { this.form.markAllAsTouched(); return; }
    const v = this.form.getRawValue();

    let call$: Observable<DeviceResponse>;
    if (this.data.device) {
      call$ = this.svc.updateDevice(this.data.device.id, {
        name: v.name.trim(),
        deviceType: v.deviceType,
        protocol: v.protocol,
        location: v.location.trim() || undefined,
        description: v.description.trim() || undefined
      });
    } else {
      // Le serveur exige un `createdBy` : sans session valide, l'appel partirait en 400.
      const createdBy = this.auth.snapshot()?.userId;
      if (!createdBy) {
        this.snack.open(
          $localize`:@@common.session-expired:Session expirée — veuillez vous reconnecter.`,
          $localize`:@@common.ok:OK`, { duration: 4000 });
        return;
      }
      call$ = this.svc.createDevice({
        code: v.code.trim(),
        name: v.name.trim(),
        deviceType: v.deviceType,
        protocol: v.protocol,
        location: v.location.trim() || undefined,
        description: v.description.trim() || undefined,
        createdBy
      });
    }

    this.submitting = true;
    call$.pipe(finalize(() => { this.submitting = false; })).subscribe({
      next: d => this.dialogRef.close(d),
      error: err => {
        // eslint-disable-next-line no-console
        console.warn('[iot-device-dialog] failed', (err as { status?: number })?.status);
        this.snack.open(safeErrorMessage(err,
          $localize`:@@iot.save-error:Erreur lors de l'enregistrement.`),
          $localize`:@@common.ok:OK`, { duration: 4000 });
      }
    });
  }

  cancel(): void { this.dialogRef.close(); }

  typeLabel(type: IotDeviceType): string { return typeLabel(type); }
  protocolLabel(protocol: IotProtocol): string { return protocolLabel(protocol); }
}
