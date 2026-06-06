import { Component } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { finalize } from 'rxjs/operators';

import { AuthService } from '../../../../core/auth/auth.service';
import { safeErrorMessage } from '../../../../core/http/error-message';
import { NcService } from '../../nc.service';
import { NcCategory, NcResponse, NcSeverity } from '../../nc.types';

@Component({
  selector: 'qos-nc-create-dialog',
  templateUrl: './nc-create-dialog.component.html',
  styleUrls: ['./nc-create-dialog.component.scss'],
  standalone: false
})
export class NcCreateDialogComponent {

  submitting = false;
  locating = false;

  readonly categories: NcCategory[] = ['PRODUCT', 'PROCESS', 'DOCUMENTATION', 'SUPPLIER', 'SAFETY', 'ENVIRONMENT', 'OTHER'];
  readonly severities: NcSeverity[] = ['MINOR', 'MAJOR', 'CRITICAL'];

  readonly form = this.fb.nonNullable.group({
    title: ['', [Validators.required, Validators.maxLength(255)]],
    category: ['PROCESS' as NcCategory, [Validators.required]],
    severity: ['MAJOR' as NcSeverity, [Validators.required]],
    zone: ['', [Validators.maxLength(255)]],
    description: [''],
    geoLat: [null as number | null],
    geoLng: [null as number | null],
    photoUrls: ['']
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly nc: NcService,
    private readonly auth: AuthService,
    private readonly snack: MatSnackBar,
    private readonly dialogRef: MatDialogRef<NcCreateDialogComponent, NcResponse>
  ) {}

  /** Terrain : récupère la position GPS et la pose dans le formulaire (affichée). */
  useMyLocation(): void {
    if (this.locating) return;
    if (typeof navigator === 'undefined' || !navigator.geolocation) {
      this.snack.open(
        $localize`:@@nc.create.geo-unsupported:La géolocalisation n'est pas disponible sur cet appareil.`,
        $localize`:@@common.ok:OK`, { duration: 4000 });
      return;
    }
    this.locating = true;
    navigator.geolocation.getCurrentPosition(
      pos => {
        this.form.patchValue({
          geoLat: Math.round(pos.coords.latitude * 1e6) / 1e6,
          geoLng: Math.round(pos.coords.longitude * 1e6) / 1e6
        });
        this.locating = false;
      },
      () => {
        this.locating = false;
        this.snack.open(
          $localize`:@@nc.create.geo-error:Impossible de récupérer la position.`,
          $localize`:@@common.ok:OK`, { duration: 4000 });
      },
      { enableHighAccuracy: true, timeout: 10000 }
    );
  }

  clearLocation(): void {
    this.form.patchValue({ geoLat: null, geoLng: null });
  }

  submit(): void {
    if (this.form.invalid || this.submitting) {
      this.form.markAllAsTouched();
      return;
    }
    // reporterId facultatif : la session courante si disponible (terrain non bloquant).
    const reporterId = this.auth.snapshot()?.userId;
    this.submitting = true;
    const { title, category, severity, zone, description, geoLat, geoLng, photoUrls } = this.form.getRawValue();
    const cleanedPhotos = this.cleanPhotoUrls(photoUrls);
    this.nc
      .createNc({
        title: title.trim(),
        category,
        severity,
        zone: zone?.trim() || undefined,
        description: description?.trim() || undefined,
        geoLat: geoLat ?? undefined,
        geoLng: geoLng ?? undefined,
        photoUrls: cleanedPhotos || undefined,
        detectedAt: new Date().toISOString(),
        reporterId: reporterId || undefined
      })
      .pipe(finalize(() => (this.submitting = false)))
      .subscribe({
        next: created => {
          const msg = created.pendingSync
            ? $localize`:@@nc.create.success-offline:NC enregistrée hors-ligne — elle sera synchronisée au retour du réseau.`
            : $localize`:@@nc.create.success:Non-conformité déclarée.`;
          this.snack.open(msg, $localize`:@@common.ok:OK`, { duration: 3000 });
          this.dialogRef.close(created);
        },
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[nc-create] failed', err?.status, err?.error?.title);
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

  /** Normalise le textarea : une URL par ligne, espaces et lignes vides retirés. */
  private cleanPhotoUrls(raw: string): string {
    return raw
      .split(/\r?\n/)
      .map(s => s.trim())
      .filter(Boolean)
      .join('\n');
  }
}
