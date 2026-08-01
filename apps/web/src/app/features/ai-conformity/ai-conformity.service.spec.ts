import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { AiConformityService } from './ai-conformity.service';
import { ConformityView, EditRequest, PlanRequest } from './ai-conformity.types';

/**
 * Évaluations de la conformité des systèmes d'IA (AI Act, art. 43).
 *
 * Le service porte deux implémentations du même contrat : un magasin en mémoire
 * (démo sans backend) et les appels HTTP réels. Le magasin rejoue le cycle
 * PLANNED → IN_PROGRESS → CERTIFIED, avec EXPIRED, REVOKED et FAILED en sorties,
 * et surtout la péremption : un certificat dont la date de validité est passée
 * bascule de lui-même à la lecture, sans attendre qu'on le marque. Les deux modes
 * sont testés.
 */
describe('AiConformityService', () => {

  const AUTHOR = 'demo-user';
  const BASE = `${environment.apiBaseUrl}/api/v1/ai-act/conformity-assessments`;
  const DAY = 86400000;
  const inDays = (d: number) => new Date(Date.now() + d * DAY).toISOString();

  const planReq = (over: Partial<PlanRequest> = {}): PlanRequest => ({
    reference: 'CA-NOUVELLE-900',
    aiSystemId: '00000000-0000-0000-0000-000000000009',
    procedure: 'INTERNAL_CONTROL',
    scope: 'Assistant de rédaction interne — auto-évaluation Annexe VI.',
    createdByUserId: AUTHOR,
    ...over
  });

  const editReq = (over: Partial<EditRequest> = {}): EditRequest => ({
    scope: 'Périmètre révisé — modèles V4.0 inclus.',
    ...over
  });

  // ------------------------------------------------------------------------
  // Magasin en mémoire
  // ------------------------------------------------------------------------
  describe('en mode démo (magasin en mémoire)', () => {
    let service: AiConformityService;
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
      service = TestBed.inject(AiConformityService);
      http = TestBed.inject(HttpTestingController);
    });

    afterEach(() => {
      environment.useMockApi = prevMock;
      // Le mode démo ne doit émettre AUCUNE requête réseau.
      http.verify();
    });

    // ---- Lectures ----------------------------------------------------------

    it('liste les évaluations pré-chargées et sait les filtrer par statut', fakeAsync(() => {
      expect(run(service.list()).length).toBe(3);

      expect(run(service.list('CERTIFIED')).map(c => c.reference)).toEqual(['CA-TELEMED-2026']);
      expect(run(service.list('REVOKED'))).toEqual([]);
    }));

    it('filtre par système d\'IA évalué', fakeAsync(() => {
      expect(run(service.listByAiSystem('00000000-0000-0000-0000-000000000002')).map(c => c.id))
        .toEqual(['ca-2']);
      expect(run(service.listByAiSystem('système-inconnu'))).toEqual([]);
    }));

    it('résout une évaluation par identifiant, avec repli sur la première si inconnue', fakeAsync(() => {
      expect(run(service.get('ca-3')).reference).toBe('CA-FRAUD-DETECT');
      // Repli assumé du mode démo : les écrans restent utilisables sans backend.
      expect(run(service.get('ca-inexistante')).id).toBe('ca-1');
    }));

    // ---- Péremption automatique ---------------------------------------------

    it('fait basculer d\'elle-même une certification dont la validité est passée', fakeAsync(() => {
      run(service.certify('ca-3', {
        certificateNumber: 'NB0987-AI-2026-0001',
        euDeclarationReference: 'EU-DECL-2026-FRAUD-001',
        validUntil: inDays(-1)
      }));

      // La bascule se fait à la lecture : aucun marquage manuel n'est requis.
      const listed = run(service.list()).find(c => c.id === 'ca-3')!;
      expect(listed.status).toBe('EXPIRED');
      expect(listed.expiredAt).toBeTruthy();
    }));

    it('laisse en vigueur un certificat encore valide', fakeAsync(() => {
      expect(run(service.list()).find(c => c.id === 'ca-1')?.status).toBe('CERTIFIED');
    }));

    // ---- Certificats arrivant à échéance ------------------------------------

    it('ne remonte à échéance que les certificats valides expirant sous 90 jours', fakeAsync(() => {
      // ca-1 est valide plus de deux ans : hors horizon.
      expect(run(service.listExpiring())).toEqual([]);

      run(service.certify('ca-3', {
        certificateNumber: 'NB0987-AI-2026-0002',
        euDeclarationReference: 'EU-DECL-2026-FRAUD-002',
        validUntil: inDays(30)
      }));

      expect(run(service.listExpiring()).map(c => c.id)).toEqual(['ca-3']);
    }));

    it('exclut de l\'échéancier un certificat déjà périmé', fakeAsync(() => {
      run(service.certify('ca-3', {
        certificateNumber: 'NB0987-AI-2026-0003',
        euDeclarationReference: 'EU-DECL-2026-FRAUD-003',
        validUntil: inDays(-1)
      }));

      // Périmé n'est pas « à renouveler sous 90 jours » : c'est déjà trop tard.
      expect(run(service.listExpiring())).toEqual([]);
    }));

    it('borne le nombre de certificats remontés', fakeAsync(() => {
      run(service.certify('ca-3', {
        certificateNumber: 'NB0987-AI-2026-0004',
        euDeclarationReference: 'EU-DECL-2026-FRAUD-004',
        validUntil: inDays(30)
      }));

      expect(run(service.listExpiring(0))).toEqual([]);
    }));

    // ---- Planification / édition --------------------------------------------

    it('planifie une évaluation en tête de liste', fakeAsync(() => {
      const planned = run(service.plan(planReq()));

      expect(planned.status).toBe('PLANNED');
      expect(planned.startedAt).toBeUndefined();
      expect(run(service.list())[0].reference).toBe('CA-NOUVELLE-900');
    }));

    it('retient l\'organisme notifié quand la procédure l\'exige', fakeAsync(() => {
      const planned = run(service.plan(planReq({
        procedure: 'NOTIFIED_BODY', notifiedBodyId: '0123', notifiedBodyName: 'BSI — UK0123'
      })));

      expect(planned.procedure).toBe('NOTIFIED_BODY');
      expect(planned.notifiedBodyId).toBe('0123');
    }));

    it('remplace les champs de l\'évaluation éditée', fakeAsync(() => {
      const edited = run(service.edit('ca-3', editReq({ qmsId: 'AIQMS-2026-003' })));

      expect(edited.scope).toContain('V4.0');
      expect(edited.qmsId).toBe('AIQMS-2026-003');
    }));

    it('édite sans effet de bord quand l\'évaluation visée n\'existe pas', fakeAsync(() => {
      const before = run(service.get('ca-1')).scope;

      run(service.edit('ca-inexistante', editReq()));

      expect(run(service.get('ca-1')).scope).toBe(before);
    }));

    // ---- Cycle de vie --------------------------------------------------------

    it('démarre l\'évaluation et l\'horodate', fakeAsync(() => {
      const started = run(service.start('ca-3'));

      expect(started.status).toBe('IN_PROGRESS');
      expect(started.startedAt).toBeTruthy();
    }));

    it('certifie avec son numéro, sa déclaration UE et sa validité', fakeAsync(() => {
      const certified = run(service.certify('ca-2', {
        certificateNumber: 'NB0123-AI-2026-0099',
        euDeclarationReference: 'EU-DECL-2026-CHATBOT-001',
        validUntil: inDays(900)
      }));

      expect(certified.status).toBe('CERTIFIED');
      expect(certified.certificateNumber).toBe('NB0123-AI-2026-0099');
      expect(certified.euDeclarationReference).toBe('EU-DECL-2026-CHATBOT-001');
      expect(certified.certifiedAt).toBeTruthy();
    }));

    it('marque une péremption à la demande', fakeAsync(() => {
      const expired = run(service.markExpired('ca-1'));

      expect(expired.status).toBe('EXPIRED');
      expect(expired.expiredAt).toBeTruthy();
    }));

    it('révoque un certificat en conservant le motif', fakeAsync(() => {
      const revoked = run(service.revoke('ca-1', {
        reason: 'Écart majeur constaté lors de l\'audit de surveillance.'
      }));

      expect(revoked.status).toBe('REVOKED');
      expect(revoked.revokeReason).toContain('Écart majeur');
      expect(revoked.revokedAt).toBeTruthy();
    }));

    it('solde une évaluation en échec en conservant le motif', fakeAsync(() => {
      const failed = run(service.fail('ca-2', {
        reason: 'Documentation technique incomplète — Annexe IV.'
      }));

      expect(failed.status).toBe('FAILED');
      expect(failed.failReason).toContain('Annexe IV');
      expect(failed.failedAt).toBeTruthy();
    }));

    it('laisse le magasin intact quand une transition vise une évaluation inconnue', fakeAsync(() => {
      run(service.start('ca-inexistante'));
      run(service.markExpired('ca-inexistante'));
      run(service.certify('ca-inexistante', {
        certificateNumber: 'x', euDeclarationReference: 'y', validUntil: inDays(10)
      }));
      run(service.revoke('ca-inexistante', { reason: 'r' }));
      run(service.fail('ca-inexistante', { reason: 'r' }));

      expect(run(service.list()).map(c => c.status))
        .toEqual(['CERTIFIED', 'IN_PROGRESS', 'PLANNED']);
    }));

    // ---- Suppression ---------------------------------------------------------

    it('supprime une évaluation, et ignore une suppression inconnue', fakeAsync(() => {
      run(service.delete('ca-3'));
      expect(run(service.list()).length).toBe(2);

      run(service.delete('ca-inexistante'));
      expect(run(service.list()).length).toBe(2);
    }));
  });

  // ------------------------------------------------------------------------
  // Appels HTTP réels
  // ------------------------------------------------------------------------
  describe('en mode connecté (HTTP)', () => {
    let service: AiConformityService;
    let http: HttpTestingController;
    let prevMock: boolean;

    beforeEach(() => {
      prevMock = environment.useMockApi;
      environment.useMockApi = false;
      TestBed.configureTestingModule({
        providers: [provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()]
      });
      service = TestBed.inject(AiConformityService);
      http = TestBed.inject(HttpTestingController);
    });

    afterEach(() => {
      environment.useMockApi = prevMock;
      http.verify();
    });

    it('liste sans paramètre, et ajoute le statut quand il est demandé', () => {
      service.list().subscribe();
      const plain = http.expectOne(BASE);
      expect(plain.request.method).toBe('GET');
      expect(plain.request.params.has('status')).toBeFalse();
      plain.flush([]);

      service.list('CERTIFIED').subscribe();
      const filtered = http.expectOne(r => r.url === BASE);
      expect(filtered.request.params.get('status')).toBe('CERTIFIED');
      filtered.flush([]);
    });

    it('interroge les vues dérivées avec leurs paramètres', () => {
      service.listByAiSystem('sys-1').subscribe();
      const bySystem = http.expectOne(r => r.url === `${BASE}/by-system`);
      expect(bySystem.request.params.get('aiSystemId')).toBe('sys-1');
      bySystem.flush([]);

      service.listExpiring().subscribe();
      const byDefault = http.expectOne(r => r.url === `${BASE}/expiring-certificates`);
      expect(byDefault.request.params.get('limit')).toBe('200');
      byDefault.flush([]);

      service.listExpiring(12).subscribe();
      const bounded = http.expectOne(r => r.url === `${BASE}/expiring-certificates`);
      expect(bounded.request.params.get('limit')).toBe('12');
      bounded.flush([]);
    });

    it('lit une fiche par identifiant', () => {
      service.get('c-1').subscribe();

      const req = http.expectOne(`${BASE}/c-1`);
      expect(req.request.method).toBe('GET');
      req.flush({} as ConformityView);
    });

    it('planifie en POST sur la collection et édite en PUT sur la ressource', () => {
      const body = planReq();
      service.plan(body).subscribe();
      const post = http.expectOne(BASE);
      expect(post.request.method).toBe('POST');
      expect(post.request.body).toEqual(body);
      post.flush({} as ConformityView);

      const edit = editReq();
      service.edit('c-1', edit).subscribe();
      const put = http.expectOne(`${BASE}/c-1`);
      expect(put.request.method).toBe('PUT');
      expect(put.request.body).toEqual(edit);
      put.flush({} as ConformityView);
    });

    it('poste chaque transition sur son propre sous-chemin', () => {
      const certif = {
        certificateNumber: 'n-1', euDeclarationReference: 'd-1', validUntil: '2028-01-01T00:00:00Z'
      };
      const transitions: Array<[string, () => void, unknown]> = [
        // Les transitions sans corps postent un objet vide plutôt que `null` :
        // un POST sans corps déclenche des 415 sur certains serveurs.
        ['start', () => service.start('c-1').subscribe(), {}],
        ['mark-expired', () => service.markExpired('c-1').subscribe(), {}],
        ['certify', () => service.certify('c-1', certif).subscribe(), certif],
        ['revoke', () => service.revoke('c-1', { reason: 'r' }).subscribe(), { reason: 'r' }],
        ['fail', () => service.fail('c-1', { reason: 'r' }).subscribe(), { reason: 'r' }]
      ];

      transitions.forEach(([path, call, body]) => {
        call();
        const req = http.expectOne(`${BASE}/c-1/${path}`);
        expect(req.request.method).withContext(path).toBe('POST');
        expect(req.request.body).withContext(path).toEqual(body);
        req.flush({} as ConformityView);
      });
    });

    it('supprime en DELETE sur la ressource', () => {
      service.delete('c-1').subscribe();

      const req = http.expectOne(`${BASE}/c-1`);
      expect(req.request.method).toBe('DELETE');
      req.flush(null);
    });
  });
});
