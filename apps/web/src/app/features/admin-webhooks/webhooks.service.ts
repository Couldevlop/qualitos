import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import {
  CreateWebhookSubscriptionRequest, CreatedWebhookSubscription, UpdateWebhookSubscriptionRequest,
  WebhookDelivery, WebhookDeliveryQuery, WebhookPage, WebhookSubscription, WebhookTestPingResult
} from './webhooks.types';

/**
 * Webhooks sortants (§13.2) — client de `/api/v1/webhooks`.
 *
 * Le tenant est dérivé du JWT côté serveur (règle §18.2 #2) : aucune méthode ne le prend
 * en paramètre. Les routes sont réservées Admin Tenant / Super Admin (SecurityConfig),
 * un 403 remonte donc comme une erreur HTTP normale et non comme un état de l'écran.
 */
@Injectable({ providedIn: 'root' })
export class WebhooksService {

  private readonly endpoint = `${environment.apiBaseUrl}/api/v1/webhooks`;

  constructor(private readonly http: HttpClient) {}

  // ---- Abonnements -----------------------------------------------------------

  listSubscriptions(page: number, size: number): Observable<WebhookPage<WebhookSubscription>> {
    const params = new HttpParams().set('page', String(page)).set('size', String(size));
    return this.http.get<WebhookPage<WebhookSubscription>>(`${this.endpoint}/subscriptions`, { params });
  }

  /**
   * Crée un abonnement. Le secret HMAC est fourni par le client et renvoyé UNE seule fois
   * dans la réponse : les lectures ultérieures ne l'exposent plus (cf. `SubscriptionResponse`).
   */
  createSubscription(req: CreateWebhookSubscriptionRequest): Observable<CreatedWebhookSubscription> {
    return this.http.post<CreatedWebhookSubscription>(`${this.endpoint}/subscriptions`, req);
  }

  /** PATCH partiel : n'envoyer que les champs à modifier, le serveur ignore les absents. */
  updateSubscription(id: string, req: UpdateWebhookSubscriptionRequest): Observable<WebhookSubscription> {
    return this.http.patch<WebhookSubscription>(`${this.endpoint}/subscriptions/${id}`, req);
  }

  deleteSubscription(id: string): Observable<void> {
    return this.http.delete<void>(`${this.endpoint}/subscriptions/${id}`);
  }

  /** Envoi synchrone d'un `webhook.test.ping` : valide endpoint + secret sans attendre un vrai évènement. */
  testPing(id: string): Observable<WebhookTestPingResult> {
    return this.http.post<WebhookTestPingResult>(`${this.endpoint}/subscriptions/${id}/test`, {});
  }

  // ---- Livraisons ------------------------------------------------------------

  /**
   * Historique des livraisons.
   *
   * Attention au contrat serveur : le filtre par abonnement est PRIORITAIRE et exclusif —
   * quand `subscriptionId` est fourni, le statut n'est pas appliqué. On n'envoie donc pas
   * les deux à la fois, sinon l'écran afficherait un filtre que le serveur ignore.
   */
  listDeliveries(query: WebhookDeliveryQuery): Observable<WebhookPage<WebhookDelivery>> {
    let params = new HttpParams()
      .set('page', String(query.page))
      .set('size', String(query.size));
    if (query.subscriptionId) {
      params = params.set('subscriptionId', query.subscriptionId);
    } else if (query.status) {
      params = params.set('status', query.status);
    }
    return this.http.get<WebhookPage<WebhookDelivery>>(`${this.endpoint}/deliveries`, { params });
  }

  /** Rejoue une livraison ; le serveur refuse (409) si elle a déjà réussi. */
  retryDelivery(id: string): Observable<WebhookDelivery> {
    return this.http.post<WebhookDelivery>(`${this.endpoint}/deliveries/${id}/retry`, {});
  }

  // ---- Utilitaires -----------------------------------------------------------

  /**
   * Secret HMAC de 64 caractères hexadécimaux (256 bits d'entropie), dans la fenêtre
   * 16..128 imposée par le serveur.
   *
   * Pas de repli sur `Math.random()` : un générateur non cryptographique produirait une
   * clé de signature devinable, ce qui viderait de son sens la signature HMAC-SHA256 des
   * webhooks (§13.2). Mieux vaut échouer bruyamment que signer avec une clé faible.
   */
  generateSecret(): string {
    const bytes = new Uint8Array(32);
    crypto.getRandomValues(bytes);
    return Array.from(bytes, b => b.toString(16).padStart(2, '0')).join('');
  }
}
