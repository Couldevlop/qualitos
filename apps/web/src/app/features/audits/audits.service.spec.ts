import { HttpErrorResponse, provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import { Observable, Subject } from 'rxjs';

import { environment } from '../../../environments/environment';
import { ConnectivityService } from '../../core/offline/connectivity.service';
import { InMemoryQueueStore, OfflineQueueStore } from '../../core/offline/offline-queue.store';
import { AuditsService } from './audits.service';
import {
  AddFindingRequest,
  AuditPlanResponse,
  ChecklistItemResponse,
  CreateAuditPlanRequest,
  FindingResponse
} from './audits.types';

/**
 * Gestion des audits (§4.4).
 *
 * Le service porte deux implémentations du même contrat — magasin en mémoire et
 * appels HTTP réels — plus un troisième mode qui lui est propre : le terrain
 * hors réseau (§15.2-15.3). Un auditeur en zone blanche doit pouvoir soulever un
 * constat et répondre à une checklist ; ces écritures sont mises en file et
 * rejouées à la resynchronisation, avec une réponse optimiste marquée
 * `pendingSync` pour que l'écran ne mente pas sur ce qui est réellement parti.
 * Les trois modes sont testés.
 */
describe('AuditsService', () => {

  const BASE = `${environment.apiBaseUrl}/api/v1/audits/plans`;
  const PLANNING = `${environment.apiBaseUrl}/api/v1/audits/planning`;

  /** Connectivité pilotable (navigator.onLine est en lecture seule). */
  class FakeConnectivity {
    online = true;
    private readonly subject = new Subject<boolean>();
    readonly online$ = this.subject.asObservable();
    isOnline(): boolean { return this.online; }
  }

  const planReq = (over: Partial<CreateAuditPlanRequest> = {}): CreateAuditPlanRequest => ({
    title: 'Audit interne ISO 14001',
    type: 'INTERNAL',
    leadAuditorId: 'u1',
    ...over
  });

  const findingReq = (over: Partial<AddFindingRequest> = {}): AddFindingRequest => ({
    type: 'MINOR_NC',
    description: 'Procédure documentée non à jour.',
    raisedBy: 'u1',
    ...over
  });

  function configure(connectivity: FakeConnectivity): void {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
        { provide: OfflineQueueStore, useClass: InMemoryQueueStore },
        { provide: ConnectivityService, useValue: connectivity }
      ]
    });
  }

  // ------------------------------------------------------------------------
  // Magasin en mémoire
  // ------------------------------------------------------------------------
  describe('en mode démo (magasin en mémoire)', () => {
    let service: AuditsService;
    let http: HttpTestingController;
    let prevMock: boolean;

    /** Les réponses simulées sont différées (`delay`) : on déroule le temps virtuel. */
    function run<T>(source: Observable<T>): T {
      let value: T | undefined;
      source.subscribe(v => (value = v));
      tick(800);
      return value as T;
    }

    beforeEach(() => {
      prevMock = environment.useMockApi;
      environment.useMockApi = true;
      configure(new FakeConnectivity());
      service = TestBed.inject(AuditsService);
      http = TestBed.inject(HttpTestingController);
    });

    afterEach(() => {
      environment.useMockApi = prevMock;
      // Le mode démo ne doit émettre AUCUNE requête réseau.
      http.verify();
    });

    // ---- Lectures ----------------------------------------------------------

    it('liste les plans pré-chargés et sait les filtrer par statut', fakeAsync(() => {
      expect(run(service.listPlans()).totalElements).toBe(3);

      expect(run(service.listPlans(0, 50, 'PLANNED')).content.map(p => p.id)).toEqual(['a3']);
      expect(run(service.listPlans(0, 50, 'CANCELLED')).content).toEqual([]);
    }));

    it('produit un planning trié du plus urgent au plus lointain', fakeAsync(() => {
      const entries = run(service.listPlanning());

      expect(entries.length).toBeGreaterThan(0);
      const days = entries.map(e => e.daysUntil);
      expect(days).toEqual([...days].sort((a, b) => a - b));
      expect(entries[0].overdue).toBeTrue();
    }));

    it('restreint le planning simulé au type et à l’horizon demandés', fakeAsync(() => {
      expect(run(service.listPlanning('SUPPLIER')).every(e => e.type === 'SUPPLIER')).toBeTrue();
      expect(run(service.listPlanning(undefined, 10)).every(e => e.daysUntil <= 10)).toBeTrue();
    }));

    it('résout un plan par identifiant, avec repli sur le premier si inconnu', fakeAsync(() => {
      expect(run(service.getPlan('a2')).title).toContain('Acme Forge');
      // Repli assumé du mode démo : les écrans restent utilisables sans backend.
      expect(run(service.getPlan('plan-inconnu')).id).toBe('a1');
    }));

    // ---- Écritures -----------------------------------------------------------

    it('crée un plan à l\'état planifié, sans checklist ni constat', fakeAsync(() => {
      const created = run(service.createPlan(planReq()));

      expect(created.status).toBe('PLANNED');
      expect(created.checklist).toEqual([]);
      expect(created.findings).toEqual([]);
      expect(run(service.listPlans()).content[0].title).toBe('Audit interne ISO 14001');
    }));

    it('ne met à jour que les champs transmis', fakeAsync(() => {
      const updated = run(service.updatePlan('a3', { scope: 'Périmètre siège uniquement' }));

      expect(updated.scope).toBe('Périmètre siège uniquement');
      // Le titre n'était pas transmis : il ne doit pas être effacé.
      expect(updated.title).toContain('Pré-audit certification');
    }));

    it('met à jour sans effet de bord quand le plan visé n\'existe pas', fakeAsync(() => {
      const before = run(service.getPlan('a1')).title;

      run(service.updatePlan('plan-inconnu', { title: 'usurpé' }));

      expect(run(service.getPlan('a1')).title).toBe(before);
    }));

    it('supprime un plan, et ignore une suppression inconnue', fakeAsync(() => {
      run(service.deletePlan('a3'));
      expect(run(service.listPlans()).totalElements).toBe(2);

      run(service.deletePlan('plan-inconnu'));
      expect(run(service.listPlans()).totalElements).toBe(2);
    }));

    // ---- Constats et checklist ------------------------------------------------

    it('rattache un constat au plan', fakeAsync(() => {
      const finding = run(service.addFinding('a2', findingReq({ clauseRef: '7.5' })));

      expect(finding.planId).toBe('a2');
      expect(finding.clauseRef).toBe('7.5');
      expect(finding.raisedAt).toBeTruthy();
      expect(run(service.getPlan('a2')).findings?.map(f => f.id)).toEqual([finding.id]);
    }));

    it('rattache un item de checklist au plan', fakeAsync(() => {
      const item = run(service.addChecklistItem('a2', {
        question: 'Les enregistrements sont-ils conservés ?', clauseRef: '7.5.3'
      }));

      expect(item.planId).toBe('a2');
      expect(run(service.getPlan('a2')).checklist?.map(i => i.id)).toEqual([item.id]);
    }));

    it('rend l\'objet créé même quand le plan visé n\'existe pas', fakeAsync(() => {
      expect(run(service.addFinding('plan-inconnu', findingReq())).description)
        .toBe('Procédure documentée non à jour.');
      expect(run(service.addChecklistItem('plan-inconnu', { question: 'q' })).question).toBe('q');
    }));

    // ---- Score de conformité ---------------------------------------------------

    it('calcule le score sur les seuls items répondus', fakeAsync(() => {
      const a = run(service.addChecklistItem('a3', { question: 'q1' }));
      const b = run(service.addChecklistItem('a3', { question: 'q2' }));
      run(service.addChecklistItem('a3', { question: 'q3' }));

      run(service.respondChecklistItem('a3', a.id, { response: 'oui', conformant: true }));
      run(service.respondChecklistItem('a3', b.id, { response: 'non', conformant: false }));

      // 1 conforme sur 2 RÉPONDUS : l'item sans réponse ne compte pas comme
      // non conforme, sinon un audit à peine commencé afficherait un score
      // catastrophique.
      expect(run(service.getPlan('a3')).conformityScore).toBe(50);
    }));

    it('met le score à 100 quand tous les items répondus sont conformes', fakeAsync(() => {
      const a = run(service.addChecklistItem('a3', { question: 'q1' }));

      run(service.respondChecklistItem('a3', a.id, { response: 'oui', conformant: true }));

      expect(run(service.getPlan('a3')).conformityScore).toBe(100);
    }));

    it('rend un item de repli quand le plan ou l\'item est inconnu', fakeAsync(() => {
      const surPlanInconnu = run(service.respondChecklistItem('plan-inconnu', 'i-1', {
        response: 'oui', conformant: true
      }));
      expect(surPlanInconnu.id).toBe('i-1');

      const itemInconnu = run(service.respondChecklistItem('a3', 'item-inconnu', {
        response: 'oui', conformant: true
      }));
      expect(itemInconnu.id).toBe('item-inconnu');
    }));

    // ---- Cycle de vie ------------------------------------------------------------

    it('démarre le plan et l\'horodate', fakeAsync(() => {
      const started = run(service.startPlan('a3'));

      expect(started.status).toBe('IN_PROGRESS');
      expect(started.startedAt).toBeTruthy();
    }));

    it('clôt le plan avec ou sans synthèse de rapport', fakeAsync(() => {
      const withSummary = run(service.completePlan('a2', 'Périmètre couvert, 1 NC mineure.'));
      expect(withSummary.status).toBe('COMPLETED');
      expect(withSummary.completedAt).toBeTruthy();
      expect(withSummary.reportSummary).toContain('NC mineure');

      expect(run(service.completePlan('a3')).reportSummary).toBeUndefined();
    }));

    it('annule le plan', fakeAsync(() => {
      expect(run(service.cancelPlan('a3')).status).toBe('CANCELLED');
    }));

    it('laisse le magasin intact quand une transition vise un plan inconnu', fakeAsync(() => {
      run(service.startPlan('plan-inconnu'));
      run(service.completePlan('plan-inconnu'));
      run(service.cancelPlan('plan-inconnu'));

      expect(run(service.listPlans()).content.map(p => p.status))
        .toEqual(['COMPLETED', 'IN_PROGRESS', 'PLANNED']);
    }));

    it('génère une synthèse de rapport sur le plan', fakeAsync(() => {
      const withReport = run(service.generateReport('a2'));

      expect(withReport.reportSummary).toBeTruthy();
      expect(run(service.getPlan('a2')).reportSummary).toBe(withReport.reportSummary!);
    }));
  });

  // ------------------------------------------------------------------------
  // Appels HTTP réels
  // ------------------------------------------------------------------------
  describe('en mode connecté (HTTP)', () => {
    let service: AuditsService;
    let http: HttpTestingController;
    let prevMock: boolean;

    beforeEach(() => {
      prevMock = environment.useMockApi;
      environment.useMockApi = false;
      configure(new FakeConnectivity());
      service = TestBed.inject(AuditsService);
      http = TestBed.inject(HttpTestingController);
    });

    afterEach(() => {
      environment.useMockApi = prevMock;
      http.verify();
    });

    it('pagine la liste et n\'ajoute le statut que s\'il est fourni', () => {
      service.listPlans().subscribe();
      const plain = http.expectOne(r => r.url === BASE);
      expect(plain.request.params.get('page')).toBe('0');
      expect(plain.request.params.get('size')).toBe('50');
      expect(plain.request.params.has('status')).toBeFalse();
      plain.flush({ content: [], totalElements: 0, totalPages: 0, number: 0, size: 0 });

      service.listPlans(1, 25, 'IN_PROGRESS').subscribe();
      const filtered = http.expectOne(r => r.url === BASE);
      expect(filtered.request.params.get('page')).toBe('1');
      expect(filtered.request.params.get('status')).toBe('IN_PROGRESS');
      filtered.flush({ content: [], totalElements: 0, totalPages: 0, number: 0, size: 0 });
    });

    it('interroge l’endpoint dédié du planning, jamais la liste paginée', () => {
      // Endpoint distinct : la liste pagine et charge checklists et constats, dont
      // un planning n'a que faire — et le décompte doit venir du serveur.
      service.listPlanning().subscribe();
      const plain = http.expectOne(r => r.url === PLANNING);
      expect(plain.request.method).toBe('GET');
      expect(plain.request.params.has('type')).toBeFalse();
      expect(plain.request.params.has('horizonDays')).toBeFalse();
      plain.flush([]);

      service.listPlanning('INTERNAL', 30).subscribe();
      const filtered = http.expectOne(r => r.url === PLANNING);
      expect(filtered.request.params.get('type')).toBe('INTERNAL');
      expect(filtered.request.params.get('horizonDays')).toBe('30');
      filtered.flush([]);
    });

    it('transmet le destinataire du rappel tel quel à la création', () => {
      service.createPlan(planReq({ reminderEmail: 'qualite@exemple.test' })).subscribe();
      const post = http.expectOne(BASE);
      expect(post.request.body.reminderEmail).toBe('qualite@exemple.test');
      post.flush({} as AuditPlanResponse);
    });

    it('crée en POST, lit en GET, met à jour en PATCH et supprime en DELETE', () => {
      const body = planReq();
      service.createPlan(body).subscribe();
      const post = http.expectOne(BASE);
      expect(post.request.method).toBe('POST');
      expect(post.request.body).toEqual(body);
      post.flush({} as AuditPlanResponse);

      service.getPlan('p-1').subscribe();
      const get = http.expectOne(`${BASE}/p-1`);
      expect(get.request.method).toBe('GET');
      get.flush({} as AuditPlanResponse);

      service.updatePlan('p-1', { title: 't' }).subscribe();
      const patch = http.expectOne(`${BASE}/p-1`);
      expect(patch.request.method).toBe('PATCH');
      expect(patch.request.body).toEqual({ title: 't' });
      patch.flush({} as AuditPlanResponse);

      service.deletePlan('p-1').subscribe();
      const del = http.expectOne(`${BASE}/p-1`);
      expect(del.request.method).toBe('DELETE');
      del.flush(null);
    });

    it('poste constats et items sous la ressource du plan', () => {
      const finding = findingReq();
      service.addFinding('p-1', finding).subscribe();
      const findingHttp = http.expectOne(`${BASE}/p-1/findings`);
      expect(findingHttp.request.method).toBe('POST');
      expect(findingHttp.request.body).toEqual(finding);
      findingHttp.flush({} as FindingResponse);

      service.addChecklistItem('p-1', { question: 'q' }).subscribe();
      const itemHttp = http.expectOne(`${BASE}/p-1/checklist`);
      expect(itemHttp.request.method).toBe('POST');
      itemHttp.flush({} as ChecklistItemResponse);
    });

    it('enregistre une réponse de checklist en PUT sur son propre chemin', () => {
      service.respondChecklistItem('p-1', 'i-1', { response: 'oui', conformant: true }).subscribe();

      const req = http.expectOne(`${BASE}/p-1/checklist/i-1/response`);
      expect(req.request.method).toBe('PUT');
      expect(req.request.body).toEqual({ response: 'oui', conformant: true });
      req.flush({} as ChecklistItemResponse);
    });

    it('fait transiter le plan en PATCH sur son propre sous-chemin', () => {
      service.startPlan('p-1').subscribe();
      const start = http.expectOne(`${BASE}/p-1/start`);
      expect(start.request.method).toBe('PATCH');
      expect(start.request.body).toEqual({});
      start.flush({} as AuditPlanResponse);

      service.cancelPlan('p-1').subscribe();
      const cancel = http.expectOne(`${BASE}/p-1/cancel`);
      expect(cancel.request.body).toEqual({});
      cancel.flush({} as AuditPlanResponse);
    });

    it('n\'envoie la synthèse à la clôture que si elle est fournie', () => {
      service.completePlan('p-1', 'synthèse').subscribe();
      const withSummary = http.expectOne(`${BASE}/p-1/complete`);
      expect(withSummary.request.body).toEqual({ reportSummary: 'synthèse' });
      withSummary.flush({} as AuditPlanResponse);

      service.completePlan('p-1').subscribe();
      const without = http.expectOne(`${BASE}/p-1/complete`);
      expect(without.request.body).toEqual({});
      without.flush({} as AuditPlanResponse);
    });

    it('demande la génération du rapport sur son propre chemin', () => {
      service.generateReport('p-1').subscribe();

      const req = http.expectOne(`${BASE}/p-1/report/generate`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({});
      req.flush({} as AuditPlanResponse);
    });
  });

  // ------------------------------------------------------------------------
  // Terrain hors réseau
  // ------------------------------------------------------------------------
  describe('sur le terrain, sans réseau', () => {
    let service: AuditsService;
    let http: HttpTestingController;
    let connectivity: FakeConnectivity;
    let prevMock: boolean;

    beforeEach(() => {
      prevMock = environment.useMockApi;
      environment.useMockApi = false;
      connectivity = new FakeConnectivity();
      configure(connectivity);
      service = TestBed.inject(AuditsService);
      http = TestBed.inject(HttpTestingController);
    });

    afterEach(() => {
      environment.useMockApi = prevMock;
      http.verify();
    });

    it('met le constat en file sans tenter d\'appel quand la connexion est coupée', fakeAsync(() => {
      connectivity.online = false;
      let finding: FindingResponse | undefined;

      service.addFinding('p-1', findingReq()).subscribe(f => (finding = f));
      // La file s'appuie sur un magasin à promesses : on déroule les microtâches.
      tick();

      // Aucune requête ne part : inutile d'attendre un échec réseau connu.
      http.expectNone(`${BASE}/p-1/findings`);
      expect(finding?.pendingSync).toBeTrue();
      expect(finding?.id.startsWith('offline-')).toBeTrue();
      // La saisie de l'auditeur est restituée telle quelle, pour que l'écran
      // affiche ce qu'il vient d'enregistrer.
      expect(finding?.description).toBe('Procédure documentée non à jour.');
    }));

    it('met en file un constat dont l\'envoi n\'a pas atteint le serveur', fakeAsync(() => {
      let finding: FindingResponse | undefined;

      service.addFinding('p-1', findingReq()).subscribe(f => (finding = f));

      // Coupure PENDANT l'envoi : statut 0, la requête n'a jamais abouti.
      http.expectOne(`${BASE}/p-1/findings`)
        .error(new ProgressEvent('error'), { status: 0, statusText: 'Unknown Error' });
      tick();

      expect(finding?.pendingSync).toBeTrue();
    }));

    it('laisse remonter une erreur serveur, qui n\'est pas une coupure', () => {
      let error: HttpErrorResponse | undefined;

      service.addFinding('p-1', findingReq()).subscribe({
        error: e => (error = e)
      });

      // 422 : le serveur a bien répondu et a refusé. Mettre cela en file
      // rejouerait indéfiniment une requête que le serveur refusera toujours.
      http.expectOne(`${BASE}/p-1/findings`)
        .flush({ title: 'invalide' }, { status: 422, statusText: 'Unprocessable Entity' });

      expect(error?.status).toBe(422);
    });

    it('met en file la réponse de checklist et la rend marquée à synchroniser', fakeAsync(() => {
      connectivity.online = false;
      let item: ChecklistItemResponse | undefined;

      service.respondChecklistItem('p-1', 'i-1', { response: 'oui', conformant: true })
        .subscribe(i => (item = i));
      tick();

      http.expectNone(`${BASE}/p-1/checklist/i-1/response`);
      expect(item?.pendingSync).toBeTrue();
      expect(item?.id).toBe('i-1');
      expect(item?.conformant).toBeTrue();
    }));

    it('met en file une réponse de checklist dont l\'envoi n\'a pas abouti', fakeAsync(() => {
      let item: ChecklistItemResponse | undefined;

      service.respondChecklistItem('p-1', 'i-1', { response: 'non', conformant: false })
        .subscribe(i => (item = i));

      http.expectOne(`${BASE}/p-1/checklist/i-1/response`)
        .error(new ProgressEvent('error'), { status: 0, statusText: 'Unknown Error' });
      tick();

      expect(item?.pendingSync).toBeTrue();
      expect(item?.conformant).toBeFalse();
    }));

    it('laisse remonter un refus serveur sur une réponse de checklist', () => {
      let error: HttpErrorResponse | undefined;

      service.respondChecklistItem('p-1', 'i-1', { response: 'oui', conformant: true })
        .subscribe({ error: e => (error = e) });

      http.expectOne(`${BASE}/p-1/checklist/i-1/response`)
        .flush({ title: 'conflit' }, { status: 409, statusText: 'Conflict' });

      expect(error?.status).toBe(409);
    });
  });
});
