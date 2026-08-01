import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { environment } from '../../../environments/environment';
import { WebhooksService } from './webhooks.service';
import {
  CreatedWebhookSubscription, WebhookDelivery, WebhookPage, WebhookSubscription
} from './webhooks.types';

/**
 * `/api/v1/webhooks` (9 routes) n'avait aucun consommateur : la promesse §13.2
 * « webhooks sortants souscriptibles » n'était pilotable qu'en appelant l'API à la main.
 */
describe('WebhooksService', () => {
  let service: WebhooksService;
  let http: HttpTestingController;

  const base = `${environment.apiBaseUrl}/api/v1/webhooks`;

  const subscription = (over: Partial<WebhookSubscription> = {}): WebhookSubscription => ({
    id: 's-1', tenantId: 't-1', name: 'ERP', endpointUrl: 'https://erp.example/hook',
    eventTypes: ['capa.case.opened'], status: 'ACTIVE', maxRetries: 5, consecutiveFailures: 0,
    lastTriggeredAt: null, lastSuccessAt: null, createdBy: 'u-1',
    createdAt: null, updatedAt: null, ...over
  });

  const delivery = (over: Partial<WebhookDelivery> = {}): WebhookDelivery => ({
    id: 'd-1', tenantId: 't-1', subscriptionId: 's-1', eventId: 'e-1',
    eventType: 'capa.case.opened', payload: '{}', status: 'FAILED', attemptCount: 2,
    lastAttemptAt: null, nextRetryAt: null, responseStatusCode: 500, responseBody: null,
    errorMessage: 'boom', createdAt: null, updatedAt: null, ...over
  });

  const page = <T>(content: T[]): WebhookPage<T> => ({
    content, totalElements: content.length, totalPages: 1, number: 0, size: 20
  });

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()]
    });
    service = TestBed.inject(WebhooksService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  // ---- Abonnements -----------------------------------------------------------

  it('liste les abonnements avec la pagination demandée', (done) => {
    service.listSubscriptions(2, 10).subscribe(p => {
      expect(p.content.length).toBe(1);
      expect(p.content[0].name).toBe('ERP');
      done();
    });
    const req = http.expectOne(r => r.url === `${base}/subscriptions`);
    expect(req.request.params.get('page')).toBe('2');
    expect(req.request.params.get('size')).toBe('10');
    req.flush(page([subscription()]));
  });

  it('remonte l\'erreur HTTP de la liste sans la masquer', (done) => {
    service.listSubscriptions(0, 20).subscribe({
      next: () => fail('ne doit pas émettre'),
      error: err => { expect(err.status).toBe(403); done(); }
    });
    http.expectOne(r => r.url === `${base}/subscriptions`)
      .flush({}, { status: 403, statusText: 'Forbidden' });
  });

  it('crée un abonnement et récupère le secret retourné une seule fois', (done) => {
    const created: CreatedWebhookSubscription = { subscription: subscription(), secret: 'a'.repeat(64) };
    service.createSubscription({
      name: 'ERP', endpointUrl: 'https://erp.example/hook',
      eventTypes: ['capa.case.opened'], secret: 'a'.repeat(64), maxRetries: 5, createdBy: 'u-1'
    }).subscribe(r => {
      expect(r.secret).toBe('a'.repeat(64));
      expect(r.subscription.id).toBe('s-1');
      done();
    });
    const req = http.expectOne(`${base}/subscriptions`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({
      name: 'ERP', endpointUrl: 'https://erp.example/hook',
      eventTypes: ['capa.case.opened'], secret: 'a'.repeat(64), maxRetries: 5, createdBy: 'u-1'
    });
    req.flush(created);
  });

  it('met à jour partiellement un abonnement (PATCH)', (done) => {
    service.updateSubscription('s-1', { status: 'PAUSED' }).subscribe(s => {
      expect(s.status).toBe('PAUSED');
      done();
    });
    const req = http.expectOne(`${base}/subscriptions/s-1`);
    expect(req.request.method).toBe('PATCH');
    expect(req.request.body).toEqual({ status: 'PAUSED' });
    req.flush(subscription({ status: 'PAUSED' }));
  });

  it('supprime un abonnement', (done) => {
    service.deleteSubscription('s-1').subscribe(() => done());
    const req = http.expectOne(`${base}/subscriptions/s-1`);
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
  });

  it('envoie un ping de test et restitue le résultat synchrone', (done) => {
    service.testPing('s-1').subscribe(r => {
      expect(r.status).toBe('SUCCESS');
      expect(r.responseStatusCode).toBe(200);
      done();
    });
    const req = http.expectOne(`${base}/subscriptions/s-1/test`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({});
    req.flush({ deliveryId: 'd-9', status: 'SUCCESS', responseStatusCode: 200, errorMessage: null });
  });

  // ---- Livraisons ------------------------------------------------------------

  it('liste les livraisons sans filtre', (done) => {
    service.listDeliveries({ subscriptionId: null, status: null, page: 0, size: 25 })
      .subscribe(p => { expect(p.content.length).toBe(1); done(); });
    const req = http.expectOne(r => r.url === `${base}/deliveries`);
    expect(req.request.params.get('page')).toBe('0');
    expect(req.request.params.get('size')).toBe('25');
    expect(req.request.params.has('subscriptionId')).toBeFalse();
    expect(req.request.params.has('status')).toBeFalse();
    req.flush(page([delivery()]));
  });

  it('filtre les livraisons par statut quand aucun abonnement n\'est ciblé', (done) => {
    service.listDeliveries({ subscriptionId: null, status: 'DEAD_LETTER', page: 0, size: 25 })
      .subscribe(() => done());
    const req = http.expectOne(r => r.url === `${base}/deliveries`);
    expect(req.request.params.get('status')).toBe('DEAD_LETTER');
    expect(req.request.params.has('subscriptionId')).toBeFalse();
    req.flush(page([delivery({ status: 'DEAD_LETTER' })]));
  });

  it('n\'envoie jamais statut et abonnement ensemble : le serveur ignorerait le statut', (done) => {
    service.listDeliveries({ subscriptionId: 's-1', status: 'FAILED', page: 1, size: 10 })
      .subscribe(() => done());
    const req = http.expectOne(r => r.url === `${base}/deliveries`);
    expect(req.request.params.get('subscriptionId')).toBe('s-1');
    expect(req.request.params.has('status')).toBeFalse();
    expect(req.request.params.get('page')).toBe('1');
    req.flush(page([delivery()]));
  });

  it('rejoue une livraison', (done) => {
    service.retryDelivery('d-1').subscribe(d => {
      expect(d.status).toBe('SUCCESS');
      done();
    });
    const req = http.expectOne(`${base}/deliveries/d-1/retry`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({});
    req.flush(delivery({ status: 'SUCCESS', responseStatusCode: 200 }));
  });

  // ---- Secret ----------------------------------------------------------------

  it('génère un secret hexadécimal de 64 caractères, dans la fenêtre 16..128 du serveur', () => {
    const secret = service.generateSecret();
    expect(secret.length).toBe(64);
    expect(secret).toMatch(/^[0-9a-f]{64}$/);
  });

  it('génère un secret différent à chaque appel', () => {
    expect(service.generateSecret()).not.toBe(service.generateSecret());
  });
});
