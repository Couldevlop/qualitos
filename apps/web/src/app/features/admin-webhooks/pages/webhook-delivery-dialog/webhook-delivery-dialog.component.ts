import { Component, Inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';

import { WebhookDelivery } from '../../webhooks.types';

export interface WebhookDeliveryDialogData {
  delivery: WebhookDelivery;
  /** Nom de l'abonnement destinataire, résolu par la liste (la livraison ne porte qu'un id). */
  subscriptionName: string | null;
}

/**
 * Inspection d'une livraison en échec (§13.2).
 *
 * Sans la charge utile ET la réponse du consommateur côte à côte, un échec de webhook est
 * indiagnosticable : on ne sait pas si c'est le contenu envoyé ou le récepteur qui est en
 * cause. Ce dialogue est en lecture seule ; le rejeu se déclenche depuis la liste.
 */
@Component({
  selector: 'qos-webhook-delivery-dialog',
  templateUrl: './webhook-delivery-dialog.component.html',
  styleUrls: ['./webhook-delivery-dialog.component.scss'],
  standalone: false
})
export class WebhookDeliveryDialogComponent {

  /** Charge utile mise en forme si elle est parsable, sinon telle quelle. */
  readonly prettyPayload: string;

  constructor(
    private readonly dialogRef: MatDialogRef<WebhookDeliveryDialogComponent, void>,
    @Inject(MAT_DIALOG_DATA) readonly data: WebhookDeliveryDialogData
  ) {
    this.prettyPayload = this.prettify(data.delivery.payload);
  }

  close(): void {
    this.dialogRef.close();
  }

  /**
   * Le payload est stocké en texte : s'il n'est pas du JSON valide (tronqué, encodage
   * exotique), on l'affiche brut plutôt que d'échouer — le diagnostic vaut mieux qu'un écran vide.
   */
  private prettify(raw: string | null): string {
    if (!raw) return '';
    try {
      return JSON.stringify(JSON.parse(raw), null, 2);
    } catch {
      return raw;
    }
  }
}
