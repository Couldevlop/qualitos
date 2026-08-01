import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { SuppliersService } from './suppliers.service';
import {
  AuditResponse,
  CertificateResponse,
  CreateAuditRequest,
  CreateCertificateRequest,
  CreateNonConformityRequest,
  CreateSupplierRequest,
  NonConformityResponse,
  SupplierResponse
} from './suppliers.types';

/**
 * Qualité fournisseurs (§4.6).
 *
 * Le service porte deux implémentations du même contrat : un magasin en mémoire
 * (démo sans backend) et les appels HTTP réels. Le magasin rejoue ce qui donne sa
 * valeur au module : le score fournisseur n'est pas saisi mais RECALCULÉ à chaque
 * audit par une moyenne mobile pondérée vers le plus récent, la péremption d'un
 * certificat se déduit de sa date d'échéance, et l'agrégat de la fiche compte les
 * non-conformités encore ouvertes. Les deux modes sont testés.
 */
describe('SuppliersService', () => {

  const AUTHOR = 'demo-user';
  const BASE = `${environment.apiBaseUrl}/api/v1/suppliers`;
  const DAY = 86400000;
  const iso = (offsetDays: number) =>
    new Date(Date.now() + offsetDays * DAY).toISOString().slice(0, 10);

  const supplierReq = (over: Partial<CreateSupplierRequest> = {}): CreateSupplierRequest => ({
    code: 'PACK-ES-007',
    name: 'EmbalajesIberia SL',
    supplierType: 'COMPONENT',
    createdBy: AUTHOR,
    ...over
  });

  const auditReq = (over: Partial<CreateAuditRequest> = {}): CreateAuditRequest => ({
    auditedOn: iso(0),
    score: 80,
    ...over
  });

  const ncReq = (over: Partial<CreateNonConformityRequest> = {}): CreateNonConformityRequest => ({
    description: 'Lot hors tolérance dimensionnelle',
    severity: 'MAJOR',
    detectedOn: iso(0),
    ...over
  });

  const certReq = (over: Partial<CreateCertificateRequest> = {}): CreateCertificateRequest => ({
    standardCode: 'iso-9001',
    issuedOn: iso(-30),
    expiresOn: iso(365),
    ...over
  });

  // ------------------------------------------------------------------------
  // Magasin en mémoire
  // ------------------------------------------------------------------------
  describe('en mode démo (magasin en mémoire)', () => {
    let service: SuppliersService;
    let http: HttpTestingController;
    let prevMock: boolean;

    /** Les réponses simulées sont différées (`delay`) : on déroule le temps virtuel. */
    function run<T>(source: Observable<T>): T {
      let value: T | undefined;
      source.subscribe(v => (value = v));
      tick(300);
      return value as T;
    }

    beforeEach(() => {
      prevMock = environment.useMockApi;
      environment.useMockApi = true;
      TestBed.configureTestingModule({
        providers: [provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()]
      });
      service = TestBed.inject(SuppliersService);
      http = TestBed.inject(HttpTestingController);
    });

    afterEach(() => {
      environment.useMockApi = prevMock;
      // Le mode démo ne doit émettre AUCUNE requête réseau.
      http.verify();
    });

    // ---- Fournisseurs : lectures --------------------------------------------

    it('liste les fournisseurs pré-chargés', fakeAsync(() => {
      const page = run(service.list());

      expect(page.totalElements).toBe(3);
      expect(page.content.map(s => s.code))
        .toEqual(['STEEL-FR-001', 'EMS-DE-014', 'CLOUD-IE-002']);
    }));

    it('filtre par statut et par type, y compris conjointement', fakeAsync(() => {
      expect(run(service.list(0, 50, 'CONDITIONAL')).content.map(s => s.code))
        .toEqual(['EMS-DE-014']);
      expect(run(service.list(0, 50, undefined, 'SOFTWARE')).content.map(s => s.code))
        .toEqual(['CLOUD-IE-002']);

      expect(run(service.list(0, 50, 'APPROVED', 'RAW_MATERIAL')).content.map(s => s.code))
        .toEqual(['STEEL-FR-001']);
      expect(run(service.list(0, 50, 'APPROVED', 'SOFTWARE')).content).toEqual([]);
    }));

    it('résout un fournisseur par identifiant, avec repli sur le premier si inconnu', fakeAsync(() => {
      expect(run(service.get('sup-2')).code).toBe('EMS-DE-014');
      // Repli assumé du mode démo : les écrans restent utilisables sans backend.
      expect(run(service.get('sup-inexistant')).id).toBe('sup-1');
    }));

    // ---- Fournisseurs : écritures -------------------------------------------

    it('crée un prospect sans score et sans historique', fakeAsync(() => {
      const created = run(service.create(supplierReq()));

      expect(created.status).toBe('PROSPECT');
      expect(created.score).toBe(0);
      expect(run(service.list()).content[0].code).toBe('PACK-ES-007');
      // Les trois collections rattachées existent d'emblée : une fiche neuve ne
      // doit pas se comporter différemment d'une fiche ancienne.
      expect(run(service.listAudits(created.id)).content).toEqual([]);
      expect(run(service.listNcs(created.id)).content).toEqual([]);
      expect(run(service.listCerts(created.id)).content).toEqual([]);
    }));

    it('met à jour la fiche et ignore une mise à jour inconnue', fakeAsync(() => {
      const updated = run(service.update('sup-3', {
        name: 'CloudHostIE Ltd — EMEA', contactEmail: 'quality@cloudhost.ie'
      }));
      expect(updated.name).toBe('CloudHostIE Ltd — EMEA');
      expect(updated.contactEmail).toBe('quality@cloudhost.ie');

      const before = run(service.get('sup-1')).name;
      run(service.update('sup-inexistant', { name: 'x' }));
      expect(run(service.get('sup-1')).name).toBe(before);
    }));

    it('supprime un fournisseur, et ignore une suppression inconnue', fakeAsync(() => {
      run(service.delete('sup-3'));
      expect(run(service.list()).totalElements).toBe(2);

      run(service.delete('sup-inexistant'));
      expect(run(service.list()).totalElements).toBe(2);
    }));

    // ---- Changement de statut ------------------------------------------------

    it('trace l\'approbation avec son auteur et sa date', fakeAsync(() => {
      const approved = run(service.changeStatus('sup-3', 'APPROVED', { actorUserId: 'acheteur-1' }));

      expect(approved.status).toBe('APPROVED');
      expect(approved.approvedBy).toBe('acheteur-1');
      expect(approved.approvedAt).toBeTruthy();
    }));

    it('change de statut sans tracer d\'approbation pour les autres transitions', fakeAsync(() => {
      const suspended = run(service.changeStatus('sup-3', 'SUSPENDED', {
        actorUserId: 'acheteur-1', reason: 'Audit en retard.'
      }));

      expect(suspended.status).toBe('SUSPENDED');
      // Une suspension n'est pas une approbation : ne pas horodater l'une comme
      // l'autre, sous peine de fausser la traçabilité du référencement.
      expect(suspended.approvedBy).toBeUndefined();
    }));

    it('laisse le magasin intact quand le fournisseur visé n\'existe pas', fakeAsync(() => {
      run(service.changeStatus('sup-inexistant', 'BLACKLISTED', { actorUserId: AUTHOR }));

      expect(run(service.list()).content.map(s => s.status))
        .toEqual(['APPROVED', 'CONDITIONAL', 'PROSPECT']);
    }));

    // ---- Audits et recalcul du score -----------------------------------------

    it('liste les audits d\'un fournisseur, et rien pour un fournisseur sans audit', fakeAsync(() => {
      expect(run(service.listAudits('sup-1')).content.map(a => a.score)).toEqual([92]);
      expect(run(service.listAudits('sup-3')).content).toEqual([]);
      expect(run(service.listAudits('fournisseur-inconnu')).content).toEqual([]);
    }));

    it('recalcule le score en pondérant le dernier audit, sans le recopier', fakeAsync(() => {
      // sup-1 est à 92 ; un audit à 60 ne fait pas tomber le score à 60 : la
      // moyenne mobile amortit, sans quoi un audit isolé effacerait l'historique.
      run(service.addAudit('sup-1', auditReq({ score: 60 })));

      expect(run(service.get('sup-1')).score).toBe(Math.round(92 * 0.4 + 60 * 0.6));
    }));

    it('date le dernier audit sur la fiche fournisseur', fakeAsync(() => {
      run(service.addAudit('sup-1', auditReq({ auditedOn: '2026-07-20' })));

      expect(run(service.get('sup-1')).lastAuditAt).toBe('2026-07-20');
    }));

    it('empile les audits du plus récent au plus ancien', fakeAsync(() => {
      run(service.addAudit('sup-1', auditReq({ score: 75 })));

      const audits = run(service.listAudits('sup-1')).content;
      expect(audits.length).toBe(2);
      expect(audits[0].score).toBe(75);
    }));

    it('compte à zéro les constats non renseignés', fakeAsync(() => {
      const created = run(service.addAudit('sup-3', auditReq()));

      // Un constat non saisi vaut zéro, jamais « inconnu » : un compteur absent
      // fausserait toute agrégation de findings.
      expect(created.criticalFindingsCount).toBe(0);
      expect(created.majorFindingsCount).toBe(0);
      expect(created.minorFindingsCount).toBe(0);
    }));

    it('retient les constats fournis', fakeAsync(() => {
      const created = run(service.addAudit('sup-3', auditReq({
        criticalFindingsCount: 1, majorFindingsCount: 2, minorFindingsCount: 5
      })));

      expect(created.criticalFindingsCount).toBe(1);
      expect(created.majorFindingsCount).toBe(2);
      expect(created.minorFindingsCount).toBe(5);
    }));

    it('enregistre l\'audit d\'un fournisseur inconnu sans recalculer de score', fakeAsync(() => {
      const created = run(service.addAudit('fournisseur-inconnu', auditReq()));

      expect(created.supplierId).toBe('fournisseur-inconnu');
      expect(run(service.get('sup-1')).score).toBe(92);
    }));

    // ---- Non-conformités ------------------------------------------------------

    it('ouvre une non-conformité et l\'empile en tête', fakeAsync(() => {
      const created = run(service.addNc('sup-1', ncReq()));

      expect(created.status).toBe('OPEN');
      expect(created.severity).toBe('MAJOR');
      expect(run(service.listNcs('sup-1')).content[0].id).toBe(created.id);
    }));

    it('met à jour une non-conformité existante', fakeAsync(() => {
      const created = run(service.addNc('sup-1', ncReq()));

      const resolved = run(service.updateNc('sup-1', created.id, {
        status: 'RESOLVED', resolvedOn: iso(2), resolution: 'Tri à 100 % + avoir fournisseur.'
      }));

      expect(resolved.status).toBe('RESOLVED');
      expect(resolved.resolution).toContain('avoir fournisseur');
    }));

    it('ne modifie rien quand la non-conformité visée n\'existe pas', fakeAsync(() => {
      const created = run(service.addNc('sup-1', ncReq()));

      run(service.updateNc('sup-1', 'nc-inexistante', { status: 'REJECTED' }));

      expect(run(service.listNcs('sup-1')).content[0].status).toBe('OPEN');
      expect(created.status).toBe('OPEN');
    }));

    it('rend une liste vide pour un fournisseur sans non-conformité', fakeAsync(() => {
      expect(run(service.listNcs('sup-3')).content).toEqual([]);
      expect(run(service.listNcs('fournisseur-inconnu')).content).toEqual([]);
    }));

    // ---- Certificats -----------------------------------------------------------

    it('déduit la péremption de la date d\'échéance, pas d\'une saisie', fakeAsync(() => {
      const valide = run(service.addCert('sup-3', certReq({ expiresOn: iso(200) })));
      const perime = run(service.addCert('sup-3', certReq({
        standardCode: 'iso-14001', expiresOn: iso(-1)
      })));

      expect(valide.expired).toBeFalse();
      expect(perime.expired).toBeTrue();
    }));

    it('liste les certificats du plus récent, et rien pour un fournisseur sans certificat', fakeAsync(() => {
      expect(run(service.listCerts('sup-2')).content.map(c => c.standardCode))
        .toEqual(['iatf-16949']);
      expect(run(service.listCerts('sup-3')).content).toEqual([]);
      expect(run(service.listCerts('fournisseur-inconnu')).content).toEqual([]);
    }));

    it('supprime un certificat, et ignore une suppression inconnue', fakeAsync(() => {
      run(service.deleteCert('sup-1', 'sc-1'));
      expect(run(service.listCerts('sup-1')).content).toEqual([]);

      run(service.deleteCert('sup-1', 'sc-inexistant'));
      run(service.deleteCert('fournisseur-inconnu', 'sc-1'));
      expect(run(service.listCerts('sup-1')).content).toEqual([]);
    }));

    // ---- Agrégat de la fiche ----------------------------------------------------

    it('compte les non-conformités encore ouvertes et les certificats périmés', fakeAsync(() => {
      const stats = run(service.statistics('sup-2'));

      expect(stats.score).toBe(68);
      expect(stats.status).toBe('CONDITIONAL');
      expect(stats.expiredCertificates).toBe(1);
      expect(stats.lastAuditAt).toBe('2026-02-08');
    }));

    it('range en cours de traitement une non-conformité en revue', fakeAsync(() => {
      const nc = run(service.addNc('sup-1', ncReq()));
      run(service.updateNc('sup-1', nc.id, { status: 'IN_REVIEW' }));

      // Une NC en revue n'est pas soldée : elle reste comptée comme ouverte.
      expect(run(service.statistics('sup-1')).openNonConformities).toBe(1);
    }));

    it('sort une non-conformité résolue du compteur des ouvertes', fakeAsync(() => {
      const nc = run(service.addNc('sup-1', ncReq()));
      run(service.updateNc('sup-1', nc.id, { status: 'RESOLVED' }));

      const stats = run(service.statistics('sup-1'));
      expect(stats.openNonConformities).toBe(0);
      expect(stats.resolvedNonConformitiesRecent).toBe(1);
    }));

    it('rend un agrégat neutre pour un fournisseur inconnu', fakeAsync(() => {
      const stats = run(service.statistics('fournisseur-inconnu'));

      expect(stats.supplierId).toBe('fournisseur-inconnu');
      expect(stats.score).toBe(0);
      expect(stats.status).toBe('PROSPECT');
      expect(stats.openNonConformities).toBe(0);
      expect(stats.lastAuditAt).toBeUndefined();
    }));
  });

  // ------------------------------------------------------------------------
  // Appels HTTP réels
  // ------------------------------------------------------------------------
  describe('en mode connecté (HTTP)', () => {
    let service: SuppliersService;
    let http: HttpTestingController;
    let prevMock: boolean;

    beforeEach(() => {
      prevMock = environment.useMockApi;
      environment.useMockApi = false;
      TestBed.configureTestingModule({
        providers: [provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()]
      });
      service = TestBed.inject(SuppliersService);
      http = TestBed.inject(HttpTestingController);
    });

    afterEach(() => {
      environment.useMockApi = prevMock;
      http.verify();
    });

    it('pagine la liste et n\'ajoute les filtres que s\'ils sont fournis', () => {
      service.list().subscribe();
      const plain = http.expectOne(r => r.url === BASE);
      expect(plain.request.params.get('page')).toBe('0');
      expect(plain.request.params.get('size')).toBe('50');
      expect(plain.request.params.has('status')).toBeFalse();
      expect(plain.request.params.has('type')).toBeFalse();
      plain.flush({ content: [], totalElements: 0, totalPages: 0, number: 0, size: 0 });

      service.list(2, 10, 'SUSPENDED', 'LOGISTICS').subscribe();
      const filtered = http.expectOne(r => r.url === BASE);
      expect(filtered.request.params.get('page')).toBe('2');
      expect(filtered.request.params.get('size')).toBe('10');
      expect(filtered.request.params.get('status')).toBe('SUSPENDED');
      expect(filtered.request.params.get('type')).toBe('LOGISTICS');
      filtered.flush({ content: [], totalElements: 0, totalPages: 0, number: 0, size: 0 });
    });

    it('lit une fiche et son agrégat', () => {
      service.get('s-1').subscribe();
      const one = http.expectOne(`${BASE}/s-1`);
      expect(one.request.method).toBe('GET');
      one.flush({} as SupplierResponse);

      service.statistics('s-1').subscribe();
      const stats = http.expectOne(`${BASE}/s-1/statistics`);
      expect(stats.request.method).toBe('GET');
      stats.flush({
        supplierId: 's-1', score: 0, status: 'PROSPECT',
        openNonConformities: 0, resolvedNonConformitiesRecent: 0, expiredCertificates: 0
      });
    });

    it('crée en POST et met à jour en PATCH — mise à jour partielle', () => {
      const body = supplierReq();
      service.create(body).subscribe();
      const post = http.expectOne(BASE);
      expect(post.request.method).toBe('POST');
      expect(post.request.body).toEqual(body);
      post.flush({} as SupplierResponse);

      service.update('s-1', { name: 'n' }).subscribe();
      const patch = http.expectOne(`${BASE}/s-1`);
      expect(patch.request.method).toBe('PATCH');
      expect(patch.request.body).toEqual({ name: 'n' });
      patch.flush({} as SupplierResponse);
    });

    it('poste le changement de statut sur un chemin qui porte la cible', () => {
      const body = { actorUserId: AUTHOR, reason: 'Audit favorable.' };

      service.changeStatus('s-1', 'APPROVED', body).subscribe();

      const req = http.expectOne(`${BASE}/s-1/status/APPROVED`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(body);
      req.flush({} as SupplierResponse);
    });

    it('gère les audits sous la ressource du fournisseur', () => {
      service.listAudits('s-1').subscribe();
      const list = http.expectOne(r => r.url === `${BASE}/s-1/audits`);
      expect(list.request.params.get('page')).toBe('0');
      expect(list.request.params.get('size')).toBe('50');
      list.flush({ content: [], totalElements: 0, totalPages: 0, number: 0, size: 0 });

      const body = auditReq();
      service.addAudit('s-1', body).subscribe();
      const add = http.expectOne(`${BASE}/s-1/audits`);
      expect(add.request.method).toBe('POST');
      expect(add.request.body).toEqual(body);
      add.flush({} as AuditResponse);
    });

    it('gère les non-conformités sous la ressource du fournisseur', () => {
      service.listNcs('s-1', 1, 20).subscribe();
      const list = http.expectOne(r => r.url === `${BASE}/s-1/non-conformities`);
      expect(list.request.params.get('page')).toBe('1');
      expect(list.request.params.get('size')).toBe('20');
      list.flush({ content: [], totalElements: 0, totalPages: 0, number: 0, size: 0 });

      const body = ncReq();
      service.addNc('s-1', body).subscribe();
      const add = http.expectOne(`${BASE}/s-1/non-conformities`);
      expect(add.request.method).toBe('POST');
      expect(add.request.body).toEqual(body);
      add.flush({} as NonConformityResponse);

      service.updateNc('s-1', 'n-1', { status: 'RESOLVED' }).subscribe();
      const patch = http.expectOne(`${BASE}/s-1/non-conformities/n-1`);
      expect(patch.request.method).toBe('PATCH');
      expect(patch.request.body).toEqual({ status: 'RESOLVED' });
      patch.flush({} as NonConformityResponse);
    });

    it('gère les certificats sous la ressource du fournisseur', () => {
      service.listCerts('s-1').subscribe();
      const list = http.expectOne(r => r.url === `${BASE}/s-1/certificates`);
      expect(list.request.params.get('page')).toBe('0');
      list.flush({ content: [], totalElements: 0, totalPages: 0, number: 0, size: 0 });

      const body = certReq();
      service.addCert('s-1', body).subscribe();
      const add = http.expectOne(`${BASE}/s-1/certificates`);
      expect(add.request.method).toBe('POST');
      expect(add.request.body).toEqual(body);
      add.flush({} as CertificateResponse);

      service.deleteCert('s-1', 'c-1').subscribe();
      const del = http.expectOne(`${BASE}/s-1/certificates/c-1`);
      expect(del.request.method).toBe('DELETE');
      del.flush(null);
    });

    it('supprime un fournisseur en DELETE sur la ressource', () => {
      service.delete('s-1').subscribe();

      const req = http.expectOne(`${BASE}/s-1`);
      expect(req.request.method).toBe('DELETE');
      req.flush(null);
    });
  });
});
