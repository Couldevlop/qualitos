import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { EhsService } from './ehs.service';
import { IncidentView, ReportRequest, Statistics } from './ehs.types';

/**
 * Incidents environnement, hygiène et sécurité (§4.11 — ISO 14001 / ISO 45001).
 *
 * Le service porte deux implémentations du même contrat : un magasin en mémoire
 * (démo sans backend) et les appels HTTP réels. Le magasin rejoue le cycle
 * REPORTED → INVESTIGATING → MITIGATED → CLOSED, la sortie CANCELLED, le
 * rattachement au référentiel transverse (CAPA, non-conformité) et l'agrégat qui
 * alimente le tableau de bord EHS. Les deux modes sont testés.
 */
describe('EhsService', () => {

  const AUTHOR = 'demo-user';
  const BASE = `${environment.apiBaseUrl}/api/v1/ehs/incidents`;

  const reportReq = (over: Partial<ReportRequest> = {}): ReportRequest => ({
    code: 'EHS-2026-900',
    title: 'Coupure légère lors d\'un changement de lame',
    type: 'INJURY',
    reportedBy: AUTHOR,
    ...over
  });

  // ------------------------------------------------------------------------
  // Magasin en mémoire
  // ------------------------------------------------------------------------
  describe('en mode démo (magasin en mémoire)', () => {
    let service: EhsService;
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
      service = TestBed.inject(EhsService);
      http = TestBed.inject(HttpTestingController);
    });

    afterEach(() => {
      environment.useMockApi = prevMock;
      // Le mode démo ne doit émettre AUCUNE requête réseau.
      http.verify();
    });

    // ---- Lectures ----------------------------------------------------------

    it('liste les incidents pré-chargés', fakeAsync(() => {
      const page = run(service.list());

      expect(page.totalElements).toBe(3);
      expect(page.content.map(i => i.code))
        .toEqual(['EHS-2026-014', 'EHS-2026-015', 'EHS-2026-016']);
    }));

    it('filtre par statut, par type et par gravité', fakeAsync(() => {
      expect(run(service.list(0, 50, 'MITIGATED')).content.map(i => i.code)).toEqual(['EHS-2026-015']);
      expect(run(service.list(0, 50, undefined, 'ENVIRONMENTAL')).content.map(i => i.code))
        .toEqual(['EHS-2026-016']);
      expect(run(service.list(0, 50, undefined, undefined, 'HIGH')).content.map(i => i.code))
        .toEqual(['EHS-2026-015']);
    }));

    it('combine les filtres, et rend une page vide quand ils s\'excluent', fakeAsync(() => {
      expect(run(service.list(0, 50, 'INVESTIGATING', 'INJURY', 'MEDIUM')).content.map(i => i.code))
        .toEqual(['EHS-2026-014']);
      expect(run(service.list(0, 50, 'CLOSED', 'INJURY')).content).toEqual([]);
    }));

    it('résout un incident par identifiant, avec repli sur le premier si inconnu', fakeAsync(() => {
      expect(run(service.get('ehs-2')).code).toBe('EHS-2026-015');
      // Repli assumé du mode démo : les écrans restent utilisables sans backend.
      expect(run(service.get('ehs-inexistant')).id).toBe('ehs-1');
    }));

    // ---- Déclaration -------------------------------------------------------

    it('déclare un incident en tête de liste et l\'horodate', fakeAsync(() => {
      const reported = run(service.report(reportReq()));

      expect(reported.status).toBe('REPORTED');
      expect(reported.reportedAt).toBeTruthy();
      expect(run(service.list()).content[0].code).toBe('EHS-2026-900');
    }));

    it('classe en gravité moyenne à défaut de cotation', fakeAsync(() => {
      // Le terrain déclare vite : ne pas coter ne doit pas produire un incident
      // sans gravité, ce qui le ferait disparaître des filtres.
      expect(run(service.report(reportReq())).severity).toBe('MEDIUM');
      expect(run(service.report(reportReq({ code: 'EHS-2026-901', severity: 'CRITICAL' }))).severity)
        .toBe('CRITICAL');
    }));

    // ---- Édition -----------------------------------------------------------

    it('met à jour l\'incident, champ par champ', fakeAsync(() => {
      const edited = run(service.edit('ehs-1', {
        severity: 'HIGH', personsInvolved: '1 opérateur, arrêt 3 jours'
      }));

      expect(edited.severity).toBe('HIGH');
      expect(edited.personsInvolved).toContain('arrêt 3 jours');
      // Mise à jour partielle : le titre non transmis est conservé.
      expect(edited.title).toContain('Chute de plain-pied');
    }));

    it('édite sans effet de bord quand l\'incident visé n\'existe pas', fakeAsync(() => {
      const before = run(service.get('ehs-1')).severity;

      run(service.edit('ehs-inexistant', { severity: 'CRITICAL' }));

      expect(run(service.get('ehs-1')).severity).toBe(before);
    }));

    // ---- Cycle de vie ------------------------------------------------------

    it('ouvre l\'investigation et affecte un pilote quand il est fourni', fakeAsync(() => {
      const withOwner = run(service.investigate('ehs-3', { ownerUserId: 'pilote-hse' }));
      expect(withOwner.status).toBe('INVESTIGATING');
      expect(withOwner.ownerUserId).toBe('pilote-hse');

      // Sans pilote transmis, celui déjà en place est conservé.
      const kept = run(service.investigate('ehs-3', {}));
      expect(kept.ownerUserId).toBe('pilote-hse');
    }));

    it('consigne la cause racine et les actions correctives à la maîtrise', fakeAsync(() => {
      const mitigated = run(service.mitigate('ehs-1', {
        rootCause: 'Fuite non détectée de la pompe de refroidissement.',
        correctiveActions: 'Ronde de contrôle quotidienne + joint remplacé.'
      }));

      expect(mitigated.status).toBe('MITIGATED');
      expect(mitigated.rootCause).toContain('Fuite non détectée');
      expect(mitigated.correctiveActions).toContain('Ronde de contrôle');
      expect(mitigated.mitigatedAt).toBeTruthy();
    }));

    it('clôt et annule un incident', fakeAsync(() => {
      const closed = run(service.close('ehs-2'));
      expect(closed.status).toBe('CLOSED');
      expect(closed.closedAt).toBeTruthy();

      expect(run(service.cancel('ehs-1')).status).toBe('CANCELLED');
    }));

    it('laisse le magasin intact quand une transition vise un incident inconnu', fakeAsync(() => {
      run(service.investigate('ehs-inexistant', {}));
      run(service.mitigate('ehs-inexistant', { rootCause: 'r', correctiveActions: 'a' }));
      run(service.close('ehs-inexistant'));
      run(service.cancel('ehs-inexistant'));

      expect(run(service.list()).content.map(i => i.status))
        .toEqual(['INVESTIGATING', 'MITIGATED', 'CLOSED']);
    }));

    // ---- Référentiel transverse ---------------------------------------------

    it('rattache l\'incident à une CAPA et à une non-conformité', fakeAsync(() => {
      // C'est le référentiel commun du §3.6 : un incident EHS alimente les mêmes
      // objets que le reste de la plateforme, il ne vit pas en silo.
      expect(run(service.linkCapa('ehs-1', { capaCaseId: 'capa-42' })).capaCaseId).toBe('capa-42');
      expect(run(service.linkNc('ehs-1', { ncId: 'nc-7' })).ncId).toBe('nc-7');
    }));

    it('rattache sans effet de bord quand l\'incident visé n\'existe pas', fakeAsync(() => {
      run(service.linkCapa('ehs-inexistant', { capaCaseId: 'capa-42' }));
      run(service.linkNc('ehs-inexistant', { ncId: 'nc-7' }));

      expect(run(service.get('ehs-1')).capaCaseId).toBeUndefined();
      expect(run(service.get('ehs-1')).ncId).toBeUndefined();
    }));

    // ---- Suppression ---------------------------------------------------------

    it('supprime un incident, et ignore une suppression inconnue', fakeAsync(() => {
      run(service.delete('ehs-3'));
      expect(run(service.list()).totalElements).toBe(2);

      run(service.delete('ehs-inexistant'));
      expect(run(service.list()).totalElements).toBe(2);
    }));

    // ---- Statistiques --------------------------------------------------------

    it('compte les incidents par statut et par type', fakeAsync(() => {
      const stats = run(service.statistics());

      expect(stats.investigating).toBe(1);
      expect(stats.mitigated).toBe(1);
      expect(stats.closed).toBe(1);
      expect(stats.reported).toBe(0);
      expect(stats.cancelled).toBe(0);

      expect(stats.injuries).toBe(1);
      expect(stats.nearMisses).toBe(1);
      expect(stats.environmental).toBe(1);
      expect(stats.security).toBe(0);
      expect(stats.propertyDamage).toBe(0);
      expect(stats.other).toBe(0);
    }));

    it('suit les transitions dans l\'agrégat', fakeAsync(() => {
      run(service.close('ehs-1'));
      run(service.cancel('ehs-2'));

      const stats = run(service.statistics());
      expect(stats.closed).toBe(2);
      expect(stats.cancelled).toBe(1);
      expect(stats.investigating).toBe(0);
    }));
  });

  // ------------------------------------------------------------------------
  // Appels HTTP réels
  // ------------------------------------------------------------------------
  describe('en mode connecté (HTTP)', () => {
    let service: EhsService;
    let http: HttpTestingController;
    let prevMock: boolean;

    beforeEach(() => {
      prevMock = environment.useMockApi;
      environment.useMockApi = false;
      TestBed.configureTestingModule({
        providers: [provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()]
      });
      service = TestBed.inject(EhsService);
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
      expect(plain.request.params.has('severity')).toBeFalse();
      plain.flush({ content: [], totalElements: 0, totalPages: 0, number: 0, size: 0 });

      service.list(1, 25, 'CLOSED', 'NEAR_MISS', 'HIGH').subscribe();
      const filtered = http.expectOne(r => r.url === BASE);
      expect(filtered.request.params.get('page')).toBe('1');
      expect(filtered.request.params.get('size')).toBe('25');
      expect(filtered.request.params.get('status')).toBe('CLOSED');
      expect(filtered.request.params.get('type')).toBe('NEAR_MISS');
      expect(filtered.request.params.get('severity')).toBe('HIGH');
      filtered.flush({ content: [], totalElements: 0, totalPages: 0, number: 0, size: 0 });
    });

    it('lit une fiche et l\'agrégat', () => {
      service.get('i-1').subscribe();
      const one = http.expectOne(`${BASE}/i-1`);
      expect(one.request.method).toBe('GET');
      one.flush({} as IncidentView);

      service.statistics().subscribe();
      const stats = http.expectOne(`${BASE}/statistics`);
      expect(stats.request.method).toBe('GET');
      stats.flush({} as Statistics);
    });

    it('déclare en POST et édite en PATCH — mise à jour partielle', () => {
      const body = reportReq();
      service.report(body).subscribe();
      const post = http.expectOne(BASE);
      expect(post.request.method).toBe('POST');
      expect(post.request.body).toEqual(body);
      post.flush({} as IncidentView);

      service.edit('i-1', { severity: 'HIGH' }).subscribe();
      const patch = http.expectOne(`${BASE}/i-1`);
      expect(patch.request.method).toBe('PATCH');
      expect(patch.request.body).toEqual({ severity: 'HIGH' });
      patch.flush({} as IncidentView);
    });

    it('poste chaque transition sur son propre sous-chemin', () => {
      const mitigation = { rootCause: 'r', correctiveActions: 'a' };
      const transitions: Array<[string, () => void, unknown]> = [
        ['investigate',
          () => service.investigate('i-1', { ownerUserId: AUTHOR }).subscribe(),
          { ownerUserId: AUTHOR }],
        ['mitigate', () => service.mitigate('i-1', mitigation).subscribe(), mitigation],
        // Clôture et annulation n'ont pas de corps métier : objet vide plutôt que
        // `null`, un POST sans corps déclenchant des 415 sur certains serveurs.
        ['close', () => service.close('i-1').subscribe(), {}],
        ['cancel', () => service.cancel('i-1').subscribe(), {}],
        ['link-capa',
          () => service.linkCapa('i-1', { capaCaseId: 'c-1' }).subscribe(),
          { capaCaseId: 'c-1' }],
        ['link-nc',
          () => service.linkNc('i-1', { ncId: 'n-1' }).subscribe(),
          { ncId: 'n-1' }]
      ];

      transitions.forEach(([path, call, body]) => {
        call();
        const req = http.expectOne(`${BASE}/i-1/${path}`);
        expect(req.request.method).withContext(path).toBe('POST');
        expect(req.request.body).withContext(path).toEqual(body);
        req.flush({} as IncidentView);
      });
    });

    it('supprime en DELETE sur la ressource', () => {
      service.delete('i-1').subscribe();

      const req = http.expectOne(`${BASE}/i-1`);
      expect(req.request.method).toBe('DELETE');
      req.flush(null);
    });
  });
});
