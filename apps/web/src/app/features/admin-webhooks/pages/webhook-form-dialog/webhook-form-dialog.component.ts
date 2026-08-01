import { Component, Inject } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { finalize } from 'rxjs/operators';

import { AuthService } from '../../../../core/auth/auth.service';
import { safeErrorMessage } from '../../../../core/http/error-message';
import { WebhooksService } from '../../webhooks.service';
import {
  WEBHOOK_EVENT_GROUPS, WebhookEventType, WebhookSubscription
} from '../../webhooks.types';

/** `null` = création ; sinon édition de l'abonnement fourni. */
export interface WebhookFormDialogData {
  subscription: WebhookSubscription | null;
}

/**
 * Résultat du dialogue. `secret` n'est renseigné qu'à la création : c'est la seule et
 * unique occasion de le montrer (le serveur ne le renvoie plus ensuite).
 */
export interface WebhookFormDialogResult {
  subscription: WebhookSubscription;
  secret: string | null;
}

/** Ensemble des évènements connus, pour filtrer ce que le serveur renvoie. */
const KNOWN_EVENT_TYPES: ReadonlySet<string> =
  new Set<string>(WEBHOOK_EVENT_GROUPS.flatMap(g => g.types as readonly string[]));

/**
 * Création / édition d'un abonnement webhook (§13.2).
 *
 * Le secret HMAC n'est saisi qu'à la création : la rotation se fait depuis la liste, via
 * une action dédiée, pour que l'admin ne le remplace pas par inadvertance en corrigeant
 * un simple libellé.
 */
@Component({
  selector: 'qos-webhook-form-dialog',
  templateUrl: './webhook-form-dialog.component.html',
  styleUrls: ['./webhook-form-dialog.component.scss'],
  standalone: false
})
export class WebhookFormDialogComponent {

  readonly groups = WEBHOOK_EVENT_GROUPS;
  readonly editing: boolean;

  submitting = false;
  error: string | null = null;

  /**
   * Évènements stockés côté serveur qu'on ne sait plus interpréter (référentiel modifié
   * depuis la souscription). On les signale car l'enregistrement les retirera : les
   * renvoyer tels quels ferait échouer la validation serveur.
   */
  readonly droppedEventTypes: string[] = [];

  readonly form = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(255)]],
    endpointUrl: ['', [Validators.required, Validators.maxLength(2048),
                       Validators.pattern(/^https?:\/\/.+/)]],
    eventTypes: [[] as WebhookEventType[], [Validators.required]],
    maxRetries: [5, [Validators.required, Validators.min(0), Validators.max(10)]],
    secret: ['', [Validators.required, Validators.minLength(16), Validators.maxLength(128)]]
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly svc: WebhooksService,
    private readonly auth: AuthService,
    private readonly dialogRef: MatDialogRef<WebhookFormDialogComponent, WebhookFormDialogResult>,
    @Inject(MAT_DIALOG_DATA) readonly data: WebhookFormDialogData
  ) {
    this.editing = !!data.subscription;
    if (data.subscription) {
      const known: WebhookEventType[] = [];
      for (const t of data.subscription.eventTypes) {
        if (KNOWN_EVENT_TYPES.has(t)) known.push(t as WebhookEventType);
        else if (t) this.droppedEventTypes.push(t);
      }
      this.form.patchValue({
        name: data.subscription.name,
        endpointUrl: data.subscription.endpointUrl,
        eventTypes: known,
        maxRetries: data.subscription.maxRetries
      });
      // Le secret n'est jamais relu : le champ n'a pas de sens en édition.
      this.form.controls.secret.disable();
    } else {
      this.form.controls.secret.setValue(this.svc.generateSecret());
    }
  }

  /** Regénère un secret : l'admin peut aussi coller le sien, le champ reste éditable. */
  regenerateSecret(): void {
    this.form.controls.secret.setValue(this.svc.generateSecret());
    this.form.controls.secret.markAsDirty();
  }

  /** Préfixe de domaine servant d'en-tête de groupe : c'est littéralement celui des ids listés. */
  groupLabel(domain: string): string {
    return `${domain}.*`;
  }

  submit(): void {
    if (this.submitting) return;
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    const v = this.form.getRawValue();

    this.error = null;
    this.submitting = true;

    if (this.data.subscription) {
      const id = this.data.subscription.id;
      this.svc.updateSubscription(id, {
        name: v.name.trim(),
        endpointUrl: v.endpointUrl.trim(),
        eventTypes: v.eventTypes,
        maxRetries: v.maxRetries
      })
        .pipe(finalize(() => { this.submitting = false; }))
        .subscribe({
          next: s => this.dialogRef.close({ subscription: s, secret: null }),
          error: err => { this.error = safeErrorMessage(err,
            $localize`:@@common.error-update:Erreur lors de la mise à jour.`); }
        });
      return;
    }

    // `createdBy` est exigé par le serveur (`@NotNull UUID`) : sans session utilisable,
    // inutile d'envoyer une requête qui sera rejetée en 400.
    const createdBy = this.auth.snapshot()?.userId;
    if (!createdBy) {
      this.submitting = false;
      this.error = $localize`:@@common.session-expired:Session expirée — veuillez vous reconnecter.`;
      return;
    }

    this.svc.createSubscription({
      name: v.name.trim(),
      endpointUrl: v.endpointUrl.trim(),
      eventTypes: v.eventTypes,
      secret: v.secret,
      maxRetries: v.maxRetries,
      createdBy
    })
      .pipe(finalize(() => { this.submitting = false; }))
      .subscribe({
        next: created => this.dialogRef.close({
          subscription: created.subscription,
          secret: created.secret
        }),
        error: err => { this.error = safeErrorMessage(err,
          $localize`:@@common.error-create:Erreur lors de la création.`); }
      });
  }

  cancel(): void {
    this.dialogRef.close();
  }
}
