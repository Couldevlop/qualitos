import { Component, Inject } from '@angular/core';
import { AbstractControl, FormBuilder, ValidationErrors, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { finalize } from 'rxjs/operators';

import { safeErrorMessage } from '../../../../core/http/error-message';
import { protocolLabel } from '../../iot.labels';
import { IotService } from '../../iot.service';
import { IotProtocol, PROTOCOLS, TelemetryResponse } from '../../iot.types';

export interface IotTelemetryDialogData {
  deviceId: string;
  deviceName: string;
  /** Métriques déjà vues sur l'équipement : évite d'en inventer une variante. */
  knownMetrics: string[];
}

/**
 * Relevé manuel d'une mesure (§9.4 — protocole MANUAL).
 *
 * Sert au mode dégradé : capteur dont le protocole natif n'est pas encore câblé,
 * relevé terrain, import ponctuel. Le serveur refuse (409) toute ingestion sur un
 * équipement non ACTIF — la fiche n'ouvre donc ce dialogue que dans cet état.
 */
@Component({
  selector: 'qos-iot-telemetry-dialog',
  templateUrl: './iot-telemetry-dialog.component.html',
  styleUrls: ['./iot-telemetry-dialog.component.scss'],
  standalone: false
})
export class IotTelemetryDialogComponent {

  readonly protocols = PROTOCOLS;

  submitting = false;

  readonly title = $localize`:@@iot.telemetry-dialog.title:Relever une mesure`;

  /**
   * `valueNumeric` reste nullable : un relevé peut être purement textuel (code
   * défaut, état d'un automate). Le serveur exige seulement qu'une des deux
   * valeurs soit renseignée — d'où le validateur de groupe.
   */
  readonly form = this.fb.group({
    metric: this.fb.nonNullable.control('',
      [Validators.required, Validators.maxLength(100)]),
    valueNumeric: this.fb.control<number | null>(null),
    valueText: this.fb.nonNullable.control('', [Validators.maxLength(500)]),
    unit: this.fb.nonNullable.control('', [Validators.maxLength(32)]),
    recordedAt: this.fb.nonNullable.control(''),
    source: this.fb.nonNullable.control<IotProtocol>('MANUAL')
  }, { validators: atLeastOneValue });

  constructor(
    private readonly fb: FormBuilder,
    private readonly svc: IotService,
    private readonly snack: MatSnackBar,
    private readonly dialogRef: MatDialogRef<IotTelemetryDialogComponent, TelemetryResponse>,
    @Inject(MAT_DIALOG_DATA) public readonly data: IotTelemetryDialogData
  ) {}

  submit(): void {
    if (this.form.invalid || this.submitting) { this.form.markAllAsTouched(); return; }
    const v = this.form.getRawValue();

    this.submitting = true;
    this.svc.ingestTelemetry(this.data.deviceId, {
      metric: v.metric.trim(),
      valueNumeric: v.valueNumeric ?? undefined,
      valueText: v.valueText.trim() || undefined,
      unit: v.unit.trim() || undefined,
      recordedAt: toInstant(v.recordedAt),
      source: v.source
    }).pipe(finalize(() => { this.submitting = false; })).subscribe({
      next: t => this.dialogRef.close(t),
      error: err => {
        // eslint-disable-next-line no-console
        console.warn('[iot-telemetry-dialog] failed', (err as { status?: number })?.status);
        this.snack.open(safeErrorMessage(err,
          $localize`:@@iot.telemetry-dialog.error:La mesure a été refusée par le serveur.`),
          $localize`:@@common.ok:OK`, { duration: 4000 });
      }
    });
  }

  cancel(): void { this.dialogRef.close(); }

  /** Recopie une métrique déjà connue : évite les doublons d'orthographe. */
  useMetric(metric: string): void {
    this.form.controls.metric.setValue(metric);
    this.form.controls.metric.markAsDirty();
  }

  protocolLabel(protocol: IotProtocol): string { return protocolLabel(protocol); }
}

/**
 * Le serveur rejette (409) une mesure sans aucune valeur. On bloque donc la
 * soumission côté formulaire plutôt que de laisser l'utilisateur essuyer l'erreur.
 */
export function atLeastOneValue(group: AbstractControl): ValidationErrors | null {
  const numeric = group.get('valueNumeric')?.value;
  const text = String(group.get('valueText')?.value ?? '').trim();
  const hasNumber = typeof numeric === 'number' && Number.isFinite(numeric);
  return hasNumber || text.length > 0 ? null : { valueRequired: true };
}

/**
 * `datetime-local` produit une heure LOCALE sans fuseau (« 2026-07-31T10:30 ») ;
 * le serveur attend un `Instant`. On repasse donc par `Date`, qui interprète la
 * saisie dans le fuseau du poste avant sérialisation en UTC.
 */
export function toInstant(localDateTime: string): string | undefined {
  if (!localDateTime) return undefined;
  const parsed = new Date(localDateTime);
  return Number.isNaN(parsed.getTime()) ? undefined : parsed.toISOString();
}
