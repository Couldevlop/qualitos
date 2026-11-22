import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { environment } from '../../../environments/environment';
import { DerogationsService } from './derogations.service';
import { DerogationPage } from './derogations.types';

const RACINE = `${environment.apiBaseUrl}/api/v1/derogations`;

describe('DerogationsService', () => {

  let service: DerogationsService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        DerogationsService,
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting()
      ]
    });
    service = TestBed.inject(DerogationsService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  const vide: DerogationPage = { content: [], total: 0, page: 0, size: 20 };

  // ---------- le registre ----------

  it('le registre pagine, et N\'ENVOIE PAS les filtres laissés vides', () => {
    service.search({ page: 2, size: 50 }).subscribe();

    const r = http.expectOne(q => q.url === RACINE);
    expect(r.request.params.get('page')).toBe('2');
    expect(r.request.params.get('size')).toBe('50');
    // Un filtre vide envoyé serait un filtre sur la chaîne vide : le serveur
    // ne rendrait rien, et l'écran paraîtrait cassé.
    expect(r.request.params.has('owner')).toBeFalse();
    expect(r.request.params.has('partReference')).toBeFalse();
    expect(r.request.params.has('customerName')).toBeFalse();
    expect(r.request.params.has('status')).toBeFalse();
    r.flush(vide);
  });

  it('les quatre filtres partent quand ils sont renseignés', () => {
    service.search({
      owner: 'kc-paul', partReference: 'P-1',
      customerName: 'ACME', status: 'PENDING_INTERNAL'
    }).subscribe();

    const r = http.expectOne(q => q.url === RACINE);
    expect(r.request.params.get('owner')).toBe('kc-paul');
    expect(r.request.params.get('partReference')).toBe('P-1');
    expect(r.request.params.get('customerName')).toBe('ACME');
    expect(r.request.params.get('status')).toBe('PENDING_INTERNAL');
    // La page et la taille gardent leur valeur par défaut.
    expect(r.request.params.get('page')).toBe('0');
    expect(r.request.params.get('size')).toBe('20');
    r.flush(vide);
  });

  it('« Mes validations » est une AUTRE route, qui ne prend aucun filtre', () => {
    service.myApprovals(1, 10).subscribe();

    // Le serveur y décide seul de qui attend quoi : laisser l'écran filtrer
    // lui permettrait de regarder les validations d'un autre.
    const r = http.expectOne(q => q.url === `${RACINE}/my-approvals`);
    expect(r.request.method).toBe('GET');
    expect(r.request.params.get('page')).toBe('1');
    expect(r.request.params.get('size')).toBe('10');
    expect(r.request.params.has('owner')).toBeFalse();
    r.flush(vide);
  });

  it('le compteur a sa propre route, sans corps de page', () => {
    service.myApprovalCount().subscribe(c => expect(c.count).toBe(3));

    const r = http.expectOne(`${RACINE}/my-approvals/count`);
    expect(r.request.method).toBe('GET');
    r.flush({ count: 3 });
  });

  // ---------- un geste, un appel ----------

  it('chaque geste a SA route et SON verbe', () => {
    const attendus: Array<[() => void, string, string]> = [
      [() => service.get('d-1').subscribe(), 'GET', `${RACINE}/d-1`],
      [() => service.create({} as never).subscribe(), 'POST', RACINE],
      [() => service.update('d-1', {} as never).subscribe(), 'PUT', `${RACINE}/d-1`],
      [() => service.withdraw('d-1').subscribe(), 'POST', `${RACINE}/d-1/withdraw`],
      [() => service.reopen('d-1').subscribe(), 'POST', `${RACINE}/d-1/reopen`]
    ];
    // Un « enregistrer » générique contournerait les règles : l'agrégat
    // vérifie l'état ET l'acteur à chaque transition, et ce ne sont pas les
    // mêmes d'un geste à l'autre.
    for (const [appel, verbe, url] of attendus) {
      appel();
      const r = http.expectOne(url);
      expect(r.request.method).toBe(verbe);
      r.flush({});
    }
  });

  it('la demande de validation porte le compte du manager, pas son nom', () => {
    service.requestApproval('d-1', 'kc-manager').subscribe();

    const r = http.expectOne(`${RACINE}/d-1/request-approval`);
    // C'est l'identité que portent les enregistrements qualité : un nom
    // s'écrit en double et ne désigne personne de façon sûre.
    expect(r.request.body).toEqual({ managerAccountId: 'kc-manager' });
    r.flush({});
  });

  it('le renvoi porte son motif', () => {
    service.sendBack('d-1', 'Quantité incohérente').subscribe();

    const r = http.expectOne(`${RACINE}/d-1/send-back`);
    expect(r.request.body).toEqual({ reason: 'Quantité incohérente' });
    r.flush({});
  });

  it('la signature porte le nom ET le tracé', () => {
    service.approve('d-1', {
      signedByName: 'Sophie Marchand',
      signaturePngBase64: 'data:image/png;base64,AAA'
    }).subscribe();

    const r = http.expectOne(`${RACINE}/d-1/approve`);
    expect(r.request.body).toEqual({
      signedByName: 'Sophie Marchand',
      signaturePngBase64: 'data:image/png;base64,AAA'
    });
    r.flush({});
  });

  it('l\'envoi au client porte l\'adresse, le commentaire et la langue', () => {
    service.sendToCustomer('d-1', {
      customerEmail: 'qualite@acme.test', comment: 'Urgence', language: 'EN'
    }).subscribe();

    const r = http.expectOne(`${RACINE}/d-1/send-to-customer`);
    expect(r.request.body).toEqual({
      customerEmail: 'qualite@acme.test', comment: 'Urgence', language: 'EN'
    });
    r.flush({});
  });

  it('la réponse du client porte la décision et sa date CIVILE', () => {
    service.recordCustomerDecision('d-1', {
      decision: 'ACCEPTED_WITH_CONDITIONS', decidedOn: '2026-11-25'
    }).subscribe();

    const r = http.expectOne(`${RACINE}/d-1/customer-decision`);
    // « aaaa-mm-jj » et non un instant ISO : c'est le jour où le client a
    // répondu, pas une heure, et le serveur attend une date civile.
    expect(r.request.body).toEqual({
      decision: 'ACCEPTED_WITH_CONDITIONS', decidedOn: '2026-11-25'
    });
    r.flush({});
  });

  // ---------- le document ----------

  it('le document est demandé en BLOB, pas en JSON', () => {
    service.pdf('d-1').subscribe(b => expect(b.type).toBe('application/pdf'));

    const r = http.expectOne(`${RACINE}/d-1/pdf`);
    // Demandé en JSON, le PDF serait décodé en texte et corrompu.
    expect(r.request.responseType).toBe('blob');
    r.flush(new Blob(['%PDF-1.4'], { type: 'application/pdf' }));
  });

  it('une erreur du serveur remonte telle quelle, sans être avalée', () => {
    let recu: unknown;
    service.get('d-9').subscribe({ error: e => (recu = e) });

    http.expectOne(`${RACINE}/d-9`)
      .flush({ detail: 'Introuvable' }, { status: 404, statusText: 'Not Found' });

    // Avaler l'erreur ici ferait croire l'écran à un dossier vide.
    expect((recu as { status: number }).status).toBe(404);
  });
});
