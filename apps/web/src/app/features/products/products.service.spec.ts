import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { environment } from '../../../environments/environment';
import { ProductsService } from './products.service';

/**
 * Référentiel Produit (§3.6).
 *
 * <p>Le service n'a aucune règle métier à lui : elles sont tenues par le serveur.
 * Ce qu'il doit garantir, c'est le contrat — la bonne route, le bon verbe, et
 * surtout aucun `tenantId` transmis par le client (§18.2 #2). Les routes
 * imbriquées portent la chaîne complète : c'est le serveur qui la revérifie, mais
 * une URL tronquée côté client toucherait la mauvaise ressource.
 */
describe('ProductsService', () => {

  const BASE = `${environment.apiBaseUrl}/api/v1/products`;
  const PRODUCT = 'p-1';
  const PLAN = 'cp-1';

  let service: ProductsService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()]
    });
    service = TestBed.inject(ProductsService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('liste les produits sur la route exacte, sans paramètre superflu', () => {
    service.list().subscribe();

    const req = http.expectOne(BASE);
    expect(req.request.method).toBe('GET');
    expect(req.request.params.keys().length).toBe(0);
    req.flush([]);
  });

  it('crée un produit sans jamais transmettre de tenant', () => {
    service.create({ code: 'REF-4471', designation: 'Support moteur' }).subscribe();

    const req = http.expectOne(BASE);
    expect(req.request.method).toBe('POST');
    expect(Object.keys(req.request.body)).not.toContain('tenantId');
    req.flush({});
  });

  it('met à jour un composant sur la route imbriquée complète', () => {
    service.updateComponent(PRODUCT, 'c-9', {
      sequenceNo: 10, reference: 'CMP-1', label: 'Vis'
    }).subscribe();

    const req = http.expectOne(`${BASE}/${PRODUCT}/components/c-9`);
    expect(req.request.method).toBe('PUT');
    req.flush({});
  });

  it('supprime une opération sur la route imbriquée complète', () => {
    service.deleteOperation(PRODUCT, 'op-3').subscribe();

    const req = http.expectOne(`${BASE}/${PRODUCT}/operations/op-3`);
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
  });

  it('propage un 404 tel quel — le message revient au composant, pas au service', () => {
    let status = 0;
    service.get('inconnu').subscribe({ error: err => (status = err.status) });

    http.expectOne(`${BASE}/inconnu`).flush('nope', { status: 404, statusText: 'Not Found' });

    expect(status).toBe(404);
  });

  it('active et rend obsolète par POST, sans corps signifiant', () => {
    service.activate(PRODUCT).subscribe();
    const activate = http.expectOne(`${BASE}/${PRODUCT}/activate`);
    expect(activate.request.method).toBe('POST');
    activate.flush({});

    service.markObsolete(PRODUCT).subscribe();
    const obsolete = http.expectOne(`${BASE}/${PRODUCT}/obsolete`);
    expect(obsolete.request.method).toBe('POST');
    obsolete.flush({});
  });

  it('lit le détail d’un control plan sous son produit', () => {
    service.controlPlan(PRODUCT, PLAN).subscribe();

    const req = http.expectOne(`${BASE}/${PRODUCT}/control-plans/${PLAN}`);
    expect(req.request.method).toBe('GET');
    req.flush({ plan: {}, lines: [] });
  });

  it('ouvre une révision et approuve par deux routes distinctes', () => {
    service.openRevision(PRODUCT, PLAN).subscribe();
    http.expectOne(`${BASE}/${PRODUCT}/control-plans/${PLAN}/revision`).flush({});

    service.approveControlPlan(PRODUCT, PLAN).subscribe();
    http.expectOne(`${BASE}/${PRODUCT}/control-plans/${PLAN}/approve`).flush({});
  });

  it('ajoute et supprime une ligne de control plan sur la chaîne complète', () => {
    service.addLine(PRODUCT, PLAN, {
      sequenceNo: 10, characteristicLabel: 'Diamètre', characteristicType: 'PRODUCT'
    }).subscribe();
    const add = http.expectOne(`${BASE}/${PRODUCT}/control-plans/${PLAN}/lines`);
    expect(add.request.method).toBe('POST');
    add.flush({});

    service.deleteLine(PRODUCT, PLAN, 'l-1').subscribe();
    const remove = http.expectOne(`${BASE}/${PRODUCT}/control-plans/${PLAN}/lines/l-1`);
    expect(remove.request.method).toBe('DELETE');
    remove.flush(null);
  });

  it('lit les propositions d’un produit et celles d’un déclencheur sur deux routes', () => {
    service.revisionRequests(PRODUCT).subscribe();
    http.expectOne(`${BASE}/${PRODUCT}/revision-requests`).flush([]);

    service.revisionRequestsForTrigger('capa-7').subscribe();
    const byTrigger = http.expectOne(
      r => r.url === `${environment.apiBaseUrl}/api/v1/revision-requests`);
    expect(byTrigger.request.params.get('triggerRefId')).toBe('capa-7');
    byTrigger.flush([]);
  });

  it('refuse une proposition en transmettant la note, qui est obligatoire côté serveur', () => {
    service.rejectRevision('r-1', 'Cotation revue le 12/08').subscribe();

    const req = http.expectOne(`${environment.apiBaseUrl}/api/v1/revision-requests/r-1/reject`);
    expect(req.request.body).toEqual({ note: 'Cotation revue le 12/08' });
    req.flush({});
  });

  it('demande les suggestions de mode de défaillance avec le texte saisi', () => {
    service.failureModeSuggestions(PRODUCT, 'Bavure sur alésage').subscribe();

    const req = http.expectOne(r => r.url === `${BASE}/${PRODUCT}/failure-mode-suggestions`);
    expect(req.request.params.get('text')).toBe('Bavure sur alésage');
    req.flush([]);
  });

  it('modifie un produit sans toucher à sa référence', () => {
    service.update(PRODUCT, { designation: 'Support moteur v2' }).subscribe();

    const req = http.expectOne(`${BASE}/${PRODUCT}`);
    expect(req.request.method).toBe('PUT');
    expect(Object.keys(req.request.body)).not.toContain('code');
    expect(Object.keys(req.request.body)).not.toContain('tenantId');
    req.flush({});
  });

  it('supprime un produit sur sa route propre', () => {
    service.remove(PRODUCT).subscribe();

    const req = http.expectOne(`${BASE}/${PRODUCT}`);
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
  });

  it('lit la nomenclature sous le produit, jamais à plat', () => {
    service.components(PRODUCT).subscribe();

    const req = http.expectOne(`${BASE}/${PRODUCT}/components`);
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('ajoute une ligne de nomenclature sous son produit', () => {
    service.addComponent(PRODUCT, { sequenceNo: 10, reference: 'VIS-M6' }).subscribe();

    const req = http.expectOne(`${BASE}/${PRODUCT}/components`);
    expect(req.request.method).toBe('POST');
    expect(Object.keys(req.request.body)).not.toContain('tenantId');
    req.flush({});
  });

  it('supprime une ligne de nomenclature en citant la chaîne complète', () => {
    service.deleteComponent(PRODUCT, 'c-9').subscribe();

    const req = http.expectOne(`${BASE}/${PRODUCT}/components/c-9`);
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
  });

  it('lit la gamme sous le produit', () => {
    service.operations(PRODUCT).subscribe();

    const req = http.expectOne(`${BASE}/${PRODUCT}/operations`);
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('ajoute une opération de gamme sous son produit', () => {
    service.addOperation(PRODUCT, { sequenceNo: 10, code: 'OP10', label: 'Perçage' }).subscribe();

    const req = http.expectOne(`${BASE}/${PRODUCT}/operations`);
    expect(req.request.method).toBe('POST');
    req.flush({});
  });

  it('modifie une opération de gamme sur la route imbriquée complète', () => {
    service.updateOperation(PRODUCT, 'op-9', {
      sequenceNo: 20, code: 'OP20', label: 'Ébavurage'
    }).subscribe();

    const req = http.expectOne(`${BASE}/${PRODUCT}/operations/op-9`);
    expect(req.request.method).toBe('PUT');
    req.flush({});
  });

  it('liste les control plans sous le produit', () => {
    service.controlPlans(PRODUCT).subscribe();

    const req = http.expectOne(`${BASE}/${PRODUCT}/control-plans`);
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('crée un brouillon de control plan sous son produit', () => {
    service.createControlPlan(PRODUCT, { phase: 'PRODUCTION', code: 'CP-4471' }).subscribe();

    const req = http.expectOne(`${BASE}/${PRODUCT}/control-plans`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ phase: 'PRODUCTION', code: 'CP-4471' });
    req.flush({});
  });

  it('modifie une ligne de control plan en citant produit, plan et ligne', () => {
    service.updateLine(PRODUCT, PLAN, 'l-9', {
      sequenceNo: 10, characteristicLabel: 'Diamètre', characteristicType: 'PRODUCT'
    }).subscribe();

    const req = http.expectOne(`${BASE}/${PRODUCT}/control-plans/${PLAN}/lines/l-9`);
    expect(req.request.method).toBe('PUT');
    req.flush({});
  });

  it('accepte une proposition sans corps : la décision est dans la route', () => {
    service.acceptRevision('r-1').subscribe();

    const req = http.expectOne(`${environment.apiBaseUrl}/api/v1/revision-requests/r-1/accept`);
    expect(req.request.method).toBe('POST');
    req.flush({});
  });
});
