import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { environment } from '../../../environments/environment';
import { AiSystemsService } from './ai-systems.service';
import { AiSystemPayload, AiSystemView } from './ai-systems.types';

/**
 * `/api/v1/ai-act/systems` (11 routes) n'avait aucun consommateur : impossible de
 * déclarer un système d'IA depuis l'interface, alors que tous les autres écrans
 * AI Act en référencent un.
 */
describe('AiSystemsService', () => {
  let service: AiSystemsService;
  let http: HttpTestingController;

  const base = `${environment.apiBaseUrl}/api/v1/ai-act/systems`;

  const view = (over: Partial<AiSystemView> = {}): AiSystemView => ({
    id: '11111111-1111-4111-8111-111111111111',
    tenantId: 't-1',
    reference: 'AISYS-A',
    name: 'Triage urgences',
    description: null,
    providerName: null,
    intendedPurpose: 'Priorisation des passages aux urgences',
    riskClassification: 'HIGH',
    role: 'DEPLOYER',
    generalPurpose: false,
    status: 'DRAFT',
    conformityAssessmentEvidenceUrl: null,
    ceMarkingNumber: null,
    humanOversightDescription: null,
    transparencyMeasures: null,
    dataGovernanceNotes: null,
    linkedDpiaId: null,
    linkedProcessingActivityIds: [],
    linkedAutomatedDecisionIds: [],
    effectiveFrom: null,
    effectiveTo: null,
    withdrawalReason: null,
    createdByUserId: 'u-1',
    createdAt: '2026-07-01T09:00:00Z',
    updatedAt: '2026-07-01T09:00:00Z',
    prohibited: false,
    requiresConformityAssessment: true,
    requiresTransparency: true,
    ...over
  });

  const payload = (over: Partial<AiSystemPayload> = {}): AiSystemPayload => ({
    name: 'Triage urgences',
    description: null,
    providerName: null,
    intendedPurpose: 'Priorisation des passages aux urgences',
    riskClassification: 'HIGH',
    role: 'DEPLOYER',
    generalPurpose: false,
    conformityAssessmentEvidenceUrl: null,
    ceMarkingNumber: null,
    humanOversightDescription: null,
    transparencyMeasures: null,
    dataGovernanceNotes: null,
    linkedDpiaId: null,
    linkedProcessingActivityIds: [],
    linkedAutomatedDecisionIds: [],
    ...over
  });

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()]
    });
    service = TestBed.inject(AiSystemsService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  // ---- Routes unitaires ------------------------------------------------------

  it('liste le registre sans filtre de statut', (done) => {
    service.list().subscribe(rows => {
      expect(rows.length).toBe(1);
      done();
    });
    const req = http.expectOne(r => r.url === base);
    expect(req.request.params.has('status')).toBeFalse();
    req.flush([view()]);
  });

  it('transmet le statut au serveur plutôt que de filtrer localement', (done) => {
    service.list('IN_USE').subscribe(() => done());
    const req = http.expectOne(r => r.url === base);
    expect(req.request.params.get('status')).toBe('IN_USE');
    req.flush([]);
  });

  it('interroge la route par classification de risque', (done) => {
    service.listByRisk('UNACCEPTABLE').subscribe(() => done());
    const req = http.expectOne(r => r.url === `${base}/by-risk`);
    expect(req.request.params.get('classification')).toBe('UNACCEPTABLE');
    req.flush([]);
  });

  it('lit une fiche par identifiant', (done) => {
    service.get('id-1').subscribe(s => {
      expect(s.reference).toBe('AISYS-A');
      done();
    });
    http.expectOne(`${base}/id-1`).flush(view());
  });

  it('lit une fiche par référence lisible', (done) => {
    service.getByReference('AISYS-A').subscribe(() => done());
    const req = http.expectOne(r => r.url === `${base}/by-reference`);
    expect(req.request.params.get('reference')).toBe('AISYS-A');
    req.flush(view());
  });

  it('crée un brouillon en POST avec sa référence et son auteur', (done) => {
    service.draft({ ...payload(), reference: 'AISYS-A', createdByUserId: 'u-1' })
      .subscribe(() => done());
    const req = http.expectOne(base);
    expect(req.request.method).toBe('POST');
    expect(req.request.body.reference).toBe('AISYS-A');
    expect(req.request.body.createdByUserId).toBe('u-1');
    req.flush(view());
  });

  it('modifie une fiche en PUT sans jamais renvoyer la référence', (done) => {
    service.edit('id-1', payload({ name: 'Nouveau nom' })).subscribe(() => done());
    const req = http.expectOne(`${base}/id-1`);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body.name).toBe('Nouveau nom');
    expect(req.request.body.reference).toBeUndefined();
    req.flush(view({ name: 'Nouveau nom' }));
  });

  it('enchaîne les transitions de cycle de vie', (done) => {
    const seen: string[] = [];

    service.register('id-1').subscribe(s => seen.push(s.status));
    const register = http.expectOne(`${base}/id-1/register`);
    expect(register.request.method).toBe('POST');
    register.flush(view({ status: 'REGISTERED' }));

    service.putInUse('id-1').subscribe(s => seen.push(s.status));
    http.expectOne(`${base}/id-1/put-in-use`).flush(view({ status: 'IN_USE' }));

    service.decommission('id-1').subscribe(s => {
      seen.push(s.status);
      expect(seen).toEqual(['REGISTERED', 'IN_USE', 'DECOMMISSIONED']);
      done();
    });
    http.expectOne(`${base}/id-1/decommission`).flush(view({ status: 'DECOMMISSIONED' }));
  });

  it('abandonne un système avec son motif', (done) => {
    service.withdraw('id-1', { reason: 'Projet arrêté' }).subscribe(() => done());
    const req = http.expectOne(`${base}/id-1/withdraw`);
    expect(req.request.body).toEqual({ reason: 'Projet arrêté' });
    req.flush(view({ status: 'WITHDRAWN', withdrawalReason: 'Projet arrêté' }));
  });

  it('supprime un brouillon', (done) => {
    service.delete('id-1').subscribe(() => done());
    const req = http.expectOne(`${base}/id-1`);
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
  });

  // ---- Vue de l'écran de liste ------------------------------------------------

  it('sans filtre, n\'appelle le serveur qu\'une fois et sert le même jeu deux fois', (done) => {
    service.registry({ status: null, risk: null }).subscribe(({ all, rows }) => {
      expect(all.length).toBe(2);
      expect(rows).toBe(all);
      done();
    });
    http.expectOne(r => r.url === base).flush([
      view({ id: 'a', reference: 'AISYS-B' }),
      view({ id: 'b', reference: 'AISYS-A' })
    ]);
  });

  it('trie par sévérité décroissante puis par référence', (done) => {
    service.registry({ status: null, risk: null }).subscribe(({ rows }) => {
      expect(rows.map(s => s.reference))
        .toEqual(['AISYS-INTERDIT', 'AISYS-A', 'AISYS-Z', 'AISYS-CHAT', 'AISYS-MIN']);
      done();
    });
    http.expectOne(r => r.url === base).flush([
      view({ reference: 'AISYS-MIN', riskClassification: 'MINIMAL_OR_NO' }),
      view({ reference: 'AISYS-Z', riskClassification: 'HIGH' }),
      view({ reference: 'AISYS-CHAT', riskClassification: 'LIMITED' }),
      view({ reference: 'AISYS-INTERDIT', riskClassification: 'UNACCEPTABLE' }),
      view({ reference: 'AISYS-A', riskClassification: 'HIGH' })
    ]);
  });

  it('garde des compteurs honnêtes : le registre entier reste chargé quand on filtre', (done) => {
    service.registry({ status: 'DRAFT', risk: null }).subscribe(({ all, rows }) => {
      expect(all.length).toBe(3);
      expect(rows.length).toBe(1);
      done();
    });
    // La première requête est celle du registre complet, la seconde celle du filtre.
    const requests = http.match(r => r.url === base);
    expect(requests.length).toBe(2);
    expect(requests[0].request.params.has('status')).toBeFalse();
    expect(requests[1].request.params.get('status')).toBe('DRAFT');
    requests[0].flush([view({ id: '1' }), view({ id: '2' }), view({ id: '3' })]);
    requests[1].flush([view({ id: '1' })]);
  });

  it('croise classification et statut côté client, faute de route combinée', (done) => {
    service.registry({ status: 'REGISTERED', risk: 'HIGH' }).subscribe(({ rows }) => {
      expect(rows.map(s => s.id)).toEqual(['keep']);
      done();
    });
    http.expectOne(r => r.url === base).flush([]);
    const byRisk = http.expectOne(r => r.url === `${base}/by-risk`);
    expect(byRisk.request.params.get('classification')).toBe('HIGH');
    byRisk.flush([
      view({ id: 'keep', status: 'REGISTERED' }),
      view({ id: 'drop', status: 'DRAFT' })
    ]);
  });

  // ---- Résolution d'un paramètre de route ------------------------------------

  it('résout un UUID par la route par identifiant', (done) => {
    service.resolve('11111111-1111-4111-8111-111111111111').subscribe(s => {
      expect(s?.reference).toBe('AISYS-A');
      done();
    });
    http.expectOne(`${base}/11111111-1111-4111-8111-111111111111`).flush(view());
  });

  it('résout une référence lisible par la route dédiée', (done) => {
    service.resolve(' AISYS-A ').subscribe(s => {
      expect(s?.id).toBeDefined();
      done();
    });
    const req = http.expectOne(r => r.url === `${base}/by-reference`);
    expect(req.request.params.get('reference')).toBe('AISYS-A');
    req.flush(view());
  });

  it('n\'interroge pas le serveur pour un paramètre que le contrôleur rejetterait', (done) => {
    service.resolve('pas-une-reference').subscribe(s => {
      expect(s).toBeNull();
      done();
    });
    http.expectNone(() => true);
  });
});
