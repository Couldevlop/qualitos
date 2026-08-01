import { Component } from '@angular/core';
import { AbstractControl, FormBuilder, ValidationErrors, Validators } from '@angular/forms';
import { MatDialogRef } from '@angular/material/dialog';
import { finalize } from 'rxjs/operators';

import { safeErrorMessage } from '../../../../core/http/error-message';
import { ApiKeysService } from '../../api-keys.service';
import {
  API_KEY_NAME_MAX_LENGTH, API_KEY_SCOPE_PATTERN, IssuedApiKey, MissingActorError, parseScopes
} from '../../api-keys.types';

/**
 * Émission d'une clé d'API (§13.1).
 *
 * Le dialogue porte l'appel et se ferme en renvoyant la clé émise : l'écran appelant
 * enchaîne alors sur l'affichage unique du secret. Les règles du serveur (format des
 * scopes, échéance future) sont rejouées ici pour expliquer un refus au moment de la
 * saisie, sans attendre un 400.
 */
@Component({
  selector: 'qos-api-key-create-dialog',
  templateUrl: './api-key-create-dialog.component.html',
  styleUrls: ['./api-key-create-dialog.component.scss'],
  standalone: false
})
export class ApiKeyCreateDialogComponent {

  readonly nameMaxLength = API_KEY_NAME_MAX_LENGTH;

  /** Borne basse du sélecteur de date : une échéance passée serait refusée. */
  readonly todayIso = new Date().toISOString().slice(0, 10);

  submitting = false;
  error: string | null = null;

  readonly form = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(API_KEY_NAME_MAX_LENGTH)]],
    scopes: ['', [scopesFormatValidator]],
    expiresAt: ['', [futureInstantValidator]]
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly svc: ApiKeysService,
    private readonly dialogRef: MatDialogRef<ApiKeyCreateDialogComponent, IssuedApiKey>
  ) {}

  /** Aperçu des scopes normalisés : l'utilisateur voit ce qui sera réellement envoyé. */
  get previewScopes(): string[] {
    return parseScopes(this.form.controls.scopes.value);
  }

  submit(): void {
    if (this.submitting) return;
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }

    const value = this.form.getRawValue();
    this.submitting = true;
    this.error = null;
    this.svc.create({
      name: value.name.trim(),
      scopes: parseScopes(value.scopes),
      expiresAt: toExpiryInstant(value.expiresAt)
    })
      .pipe(finalize(() => { this.submitting = false; }))
      .subscribe({
        next: issued => this.dialogRef.close(issued),
        error: err => {
          this.error = err instanceof MissingActorError
            ? $localize`:@@admin.api-keys.session-expired:Session expirée — reconnectez-vous pour agir sur les clés d'API.`
            : safeErrorMessage(err, $localize`:@@admin.api-keys.create-error:Émission de la clé impossible.`);
        }
      });
  }

  cancel(): void {
    this.dialogRef.close();
  }
}

/**
 * Convertit une date de calendrier en instant. On vise la FIN de journée UTC : une
 * clé saisie « valable jusqu'au 31 » doit rester utilisable pendant tout le 31.
 * Vide = clé sans échéance, cas légitime pour une intégration permanente.
 */
export function toExpiryInstant(dateValue: string): string | null {
  const raw = (dateValue ?? '').trim();
  if (!raw) return null;
  const ms = Date.parse(`${raw}T23:59:59.999Z`);
  return Number.isNaN(ms) ? null : new Date(ms).toISOString();
}

/** `ApiKey.issued` refuse une échéance non postérieure à l'instant courant. */
export function futureInstantValidator(control: AbstractControl): ValidationErrors | null {
  const raw = String(control.value ?? '').trim();
  if (!raw) return null;
  const ms = Date.parse(`${raw}T23:59:59.999Z`);
  if (Number.isNaN(ms)) return { dateFormat: true };
  return ms > Date.now() ? null : { pastDate: true };
}

/** Reflet de `ApiKeyService.normalizeScopes` : un scope hors format renverrait 400. */
export function scopesFormatValidator(control: AbstractControl): ValidationErrors | null {
  const invalid = parseScopes(String(control.value ?? ''))
    .filter(scope => !API_KEY_SCOPE_PATTERN.test(scope));
  return invalid.length ? { scopeFormat: invalid.join(', ') } : null;
}
