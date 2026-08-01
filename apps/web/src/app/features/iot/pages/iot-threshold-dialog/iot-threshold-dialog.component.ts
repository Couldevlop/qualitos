import { Component, Inject } from '@angular/core';
import { AbstractControl, FormBuilder, ValidationErrors, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Observable } from 'rxjs';
import { finalize } from 'rxjs/operators';

import { AuthService } from '../../../../core/auth/auth.service';
import { safeErrorMessage } from '../../../../core/http/error-message';
import { criticityLabel } from '../../iot.labels';
import { IotService } from '../../iot.service';
import { CapaCriticity, ThresholdResponse } from '../../iot.types';

export interface IotThresholdDialogData {
  deviceId: string;
  deviceName: string;
  /** `null` = création d'un seuil pour cet équipement ; sinon édition. */
  threshold: ThresholdResponse | null;
  /** Métriques déjà relevées sur l'équipement, proposées en raccourci. */
  knownMetrics: string[];
}

/** UUID v1-v5 tel qu'attendu par le serveur pour la fiche FMEA liée. */
const UUID_PATTERN = /^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-5][0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$/;

export const CRITICITIES: CapaCriticity[] = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'];

/**
 * Seuil de surveillance d'une métrique (§9.7, §9.9).
 *
 * Le seuil est le pont entre le terrain et la qualité : son franchissement ouvre
 * une CAPA à l'ingestion de la mesure, et un cycle PDCA si l'option est cochée.
 * Le dialogue est toujours rattaché à UN équipement — les seuils portant sur tout
 * le tenant ne se modifient pas depuis la fiche d'un capteur en particulier.
 */
@Component({
  selector: 'qos-iot-threshold-dialog',
  templateUrl: './iot-threshold-dialog.component.html',
  styleUrls: ['./iot-threshold-dialog.component.scss'],
  standalone: false
})
export class IotThresholdDialogComponent {

  readonly criticities = CRITICITIES;

  submitting = false;

  readonly isEdit: boolean;
  readonly title: string;
  readonly submitLabel: string;

  readonly form = this.fb.group({
    metric: this.fb.nonNullable.control('',
      [Validators.required, Validators.maxLength(100)]),
    minValue: this.fb.control<number | null>(null),
    maxValue: this.fb.control<number | null>(null),
    capaCriticity: this.fb.nonNullable.control<CapaCriticity>('MEDIUM', [Validators.required]),
    enabled: this.fb.nonNullable.control(true),
    openPdcaCycle: this.fb.nonNullable.control(false),
    fmeaItemId: this.fb.nonNullable.control('', [Validators.pattern(UUID_PATTERN)])
  }, { validators: boundsConsistent });

  constructor(
    private readonly fb: FormBuilder,
    private readonly svc: IotService,
    private readonly auth: AuthService,
    private readonly snack: MatSnackBar,
    private readonly dialogRef: MatDialogRef<IotThresholdDialogComponent, ThresholdResponse>,
    @Inject(MAT_DIALOG_DATA) public readonly data: IotThresholdDialogData
  ) {
    this.isEdit = !!data.threshold;
    this.title = this.isEdit
      ? $localize`:@@iot.threshold-dialog.edit-title:Modifier le seuil`
      : $localize`:@@iot.threshold-dialog.create-title:Nouveau seuil de surveillance`;
    this.submitLabel = this.isEdit
      ? $localize`:@@common.save:Enregistrer`
      : $localize`:@@common.create:Créer`;

    if (data.threshold) {
      const t = data.threshold;
      this.form.patchValue({
        metric: t.metric,
        minValue: t.minValue,
        maxValue: t.maxValue,
        capaCriticity: t.capaCriticity,
        enabled: t.enabled,
        openPdcaCycle: t.openPdcaCycle,
        fmeaItemId: t.fmeaItemId ?? ''
      });
    }
  }

  submit(): void {
    if (this.form.invalid || this.submitting) { this.form.markAllAsTouched(); return; }
    const v = this.form.getRawValue();

    // En édition, le propriétaire de la CAPA reste celui qui a été désigné : le
    // réattribuer à l'utilisateur courant ferait changer de responsable à chaque
    // retouche de borne.
    const capaOwnerId = this.data.threshold?.capaOwnerId ?? this.auth.snapshot()?.userId;
    if (!capaOwnerId) {
      this.snack.open(
        $localize`:@@common.session-expired:Session expirée — veuillez vous reconnecter.`,
        $localize`:@@common.ok:OK`, { duration: 4000 });
      return;
    }

    const payload = {
      deviceId: this.data.deviceId,
      metric: v.metric.trim(),
      minValue: v.minValue,
      maxValue: v.maxValue,
      capaCriticity: v.capaCriticity,
      capaOwnerId,
      enabled: v.enabled,
      fmeaItemId: v.fmeaItemId.trim() || null,
      openPdcaCycle: v.openPdcaCycle
    };

    const call$: Observable<ThresholdResponse> = this.data.threshold
      ? this.svc.updateThreshold(this.data.threshold.id, payload)
      : this.svc.createThreshold(payload);

    this.submitting = true;
    call$.pipe(finalize(() => { this.submitting = false; })).subscribe({
      next: t => this.dialogRef.close(t),
      error: err => {
        // eslint-disable-next-line no-console
        console.warn('[iot-threshold-dialog] failed', (err as { status?: number })?.status);
        this.snack.open(safeErrorMessage(err,
          $localize`:@@iot.threshold-dialog.error:Le seuil a été refusé par le serveur.`),
          $localize`:@@common.ok:OK`, { duration: 4000 });
      }
    });
  }

  cancel(): void { this.dialogRef.close(); }

  useMetric(metric: string): void {
    this.form.controls.metric.setValue(metric);
    this.form.controls.metric.markAsDirty();
  }

  criticityLabel(criticity: CapaCriticity): string { return criticityLabel(criticity); }
}

/**
 * Reprend les deux `@AssertTrue` du serveur : au moins une borne, et un minimum
 * inférieur ou égal au maximum. Sans ça, la validation ne remonterait qu'en 400
 * après l'envoi, sans indiquer quel champ corriger.
 */
export function boundsConsistent(group: AbstractControl): ValidationErrors | null {
  const min = (group.get('minValue')?.value ?? null) as number | null;
  const max = (group.get('maxValue')?.value ?? null) as number | null;
  if (min === null && max === null) return { boundsRequired: true };
  if (min !== null && max !== null && min > max) return { boundsInverted: true };
  return null;
}
