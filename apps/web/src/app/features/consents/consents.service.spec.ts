import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { ConsentsService } from './consents.service';
import { ConsentView, GrantConsentRequest } from './consents.types';

/**
 * Registre des consentements (RGPD art. 7).
 *
 * Le service porte deux implémentations du même contrat : un magasin en mémoire
 * (démo sans backend) et les appels HTTP réels. Deux invariants portent la valeur
 * probatoire du registre et sont vérifiés ici : l'identifiant de la personne n'est
 * jamais conservé en clair — seule son empreinte l'est — et un consentement
 * retiré ou échu cesse immédiatement d'être opposable. Les deux modes sont testés.
 */
describe('ConsentsService', () => {

  const AUTHOR = 'demo-user';
  const BASE = `${environment.apiBaseUrl}/api/v1/gdpr/consents`;
  const DAY = 86400000;
  const inDays = (d: number) => new Date(Date.now() + d * DAY).toISOString();

  const grantReq = (over: Partial<GrantConsentRequest> = {}): GrantConsentRequest => ({
    subjectIdentifier: 'carol@example.fr',
    purposeCode: 'newsletter.marketing',
    purposeVersion: '2026.1',
    source: 'WEB_FORM',
    grantedByUserId: AUTHOR,
    ...over
  });

  // ------------------------------------------------------------------------
  // Magasin en mémoire
  // ------------------------------------------------------------------------
  describe('en mode démo (magasin en mémoire)', () => {
    let service: ConsentsService;
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
      service = TestBed.inject(ConsentsService);
      http = TestBed.inject(HttpTestingController);
    });

    afterEach(() => {
      environment.useMockApi = prevMock;
      // Le mode démo ne doit émettre AUCUNE requête réseau.
      http.verify();
    });

    // ---- Identifiant jamais conservé en clair --------------------------------

    it('ne conserve qu\'une empreinte de l\'identifiant, jamais sa valeur', fakeAsync(() => {
      const granted = run(service.grant(grantReq()));

      // L'adresse ne doit apparaître dans aucun champ conservé : le registre
      // doit rester consultable sans exposer l'identité des personnes.
      expect(JSON.stringify(granted)).not.toContain('carol@example.fr');
      expect(granted.subjectIdentifierHash.startsWith('fnv1a:')).toBeTrue();
    }));

    it('donne la même empreinte au même identifiant, une autre à un autre', fakeAsync(() => {
      const premier = run(service.grant(grantReq({ purposeCode: 'a' })));
      const second = run(service.grant(grantReq({ purposeCode: 'b' })));
      const autre = run(service.grant(grantReq({
        subjectIdentifier: 'dave@example.fr', purposeCode: 'a'
      })));

      // Sans stabilité de l'empreinte, retrouver les consentements d'une
      // personne — donc honorer un retrait — deviendrait impossible.
      expect(second.subjectIdentifierHash).toBe(premier.subjectIdentifierHash);
      expect(autre.subjectIdentifierHash).not.toBe(premier.subjectIdentifierHash);
    }));

    // ---- Recherches ------------------------------------------------------------

    it('retrouve tous les consentements d\'une personne par son identifiant', fakeAsync(() => {
      const rows = run(service.searchBySubject('alice@example.fr'));

      expect(rows.map(c => c.id).sort()).toEqual(['cons-1', 'cons-2']);
      expect(run(service.searchBySubject('inconnu@example.fr'))).toEqual([]);
    }));

    it('retrouve les consentements par finalité', fakeAsync(() => {
      expect(run(service.searchByPurpose('telemedicine.session')).map(c => c.id))
        .toEqual(['cons-3']);
      expect(run(service.searchByPurpose('finalite.inconnue'))).toEqual([]);
    }));

    it('résout un consentement par identifiant, avec repli si inconnu', fakeAsync(() => {
      expect(run(service.get('cons-3')).purposeCode).toBe('telemedicine.session');
      // Repli assumé du mode démo : les écrans restent utilisables sans backend.
      expect(run(service.get('cons-inexistant')).id).toBe('cons-1');
    }));

    // ---- Consentement opposable --------------------------------------------------

    it('ne rend opposable qu\'un consentement actif pour la finalité demandée', fakeAsync(() => {
      expect(run(service.active('alice@example.fr', 'newsletter.marketing'))?.id).toBe('cons-1');
    }));

    it('ne rend rien quand le consentement a été retiré', fakeAsync(() => {
      // cons-2 est WITHDRAWN : il ne fonde plus aucun traitement.
      expect(run(service.active('alice@example.fr', 'analytics.optional'))).toBeNull();
    }));

    it('ne rend rien pour une personne ou une finalité sans consentement', fakeAsync(() => {
      expect(run(service.active('inconnu@example.fr', 'newsletter.marketing'))).toBeNull();
      expect(run(service.active('alice@example.fr', 'finalite.inconnue'))).toBeNull();
    }));

    // ---- Recueil -----------------------------------------------------------------

    it('recueille un consentement actif, en tête de registre', fakeAsync(() => {
      const granted = run(service.grant(grantReq({ evidenceUrl: 'https://preuve/1.pdf' })));

      expect(granted.status).toBe('GRANTED');
      expect(granted.active).toBeTrue();
      expect(granted.grantedAt).toBeTruthy();
      expect(granted.evidenceUrl).toBe('https://preuve/1.pdf');
      expect(run(service.searchByPurpose('newsletter.marketing'))[0].id).toBe(granted.id);
    }));

    it('reste actif sans échéance, et déjà inactif si l\'échéance est passée', fakeAsync(() => {
      expect(run(service.grant(grantReq())).active).toBeTrue();
      expect(run(service.grant(grantReq({
        purposeCode: 'p2', expiresAt: inDays(30)
      }))).active).toBeTrue();

      // Un consentement recueilli avec une échéance déjà dépassée n'a jamais
      // été opposable : il ne doit pas être présenté comme actif.
      expect(run(service.grant(grantReq({
        purposeCode: 'p3', expiresAt: inDays(-1)
      }))).active).toBeFalse();
    }));

    // ---- Retrait -------------------------------------------------------------------

    it('retire un consentement et le rend immédiatement inopposable', fakeAsync(() => {
      const withdrawn = run(service.withdraw('cons-1', {
        actorUserId: AUTHOR, reason: 'Désabonnement depuis le lien de l\'email.'
      }));

      expect(withdrawn.status).toBe('WITHDRAWN');
      expect(withdrawn.active).toBeFalse();
      expect(withdrawn.withdrawnAt).toBeTruthy();
      expect(withdrawn.withdrawnByUserId).toBe(AUTHOR);
      expect(withdrawn.withdrawalReason).toContain('Désabonnement');
      // Le retrait doit produire son effet tout de suite : l'article 7§3
      // impose qu'il soit aussi simple que le recueil.
      expect(run(service.active('alice@example.fr', 'newsletter.marketing'))).toBeNull();
    }));

    it('retire sans effet de bord quand le consentement visé n\'existe pas', fakeAsync(() => {
      run(service.withdraw('cons-inexistant', { actorUserId: AUTHOR }));

      expect(run(service.get('cons-3')).status).toBe('GRANTED');
    }));

    // ---- Balayage des échéances -------------------------------------------------------

    it('fait expirer les consentements échus et rend leur nombre', fakeAsync(() => {
      run(service.grant(grantReq({ purposeCode: 'p1', expiresAt: inDays(-1) })));
      run(service.grant(grantReq({ purposeCode: 'p2', expiresAt: inDays(-2) })));

      expect(run(service.expireDue()).expired).toBe(2);
      expect(run(service.searchByPurpose('p1'))[0].active).toBeFalse();
    }));

    it('ne compte pas deux fois un consentement déjà expiré', fakeAsync(() => {
      run(service.grant(grantReq({ purposeCode: 'p1', expiresAt: inDays(-1) })));

      expect(run(service.expireDue()).expired).toBe(1);
      expect(run(service.expireDue()).expired).toBe(0);
    }));

    it('borne le nombre de consentements traités par balayage', fakeAsync(() => {
      run(service.grant(grantReq({ purposeCode: 'p1', expiresAt: inDays(-1) })));
      run(service.grant(grantReq({ purposeCode: 'p2', expiresAt: inDays(-2) })));

      expect(run(service.expireDue(1)).expired).toBe(1);
    }));

    it('ignore les consentements déjà retirés et ceux sans échéance', fakeAsync(() => {
      // cons-2 est retiré, cons-1 et cons-3 ont des échéances futures.
      expect(run(service.expireDue()).expired).toBe(0);
    }));
  });

  // ------------------------------------------------------------------------
  // Appels HTTP réels
  // ------------------------------------------------------------------------
  describe('en mode connecté (HTTP)', () => {
    let service: ConsentsService;
    let http: HttpTestingController;
    let prevMock: boolean;

    beforeEach(() => {
      prevMock = environment.useMockApi;
      environment.useMockApi = false;
      TestBed.configureTestingModule({
        providers: [provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()]
      });
      service = TestBed.inject(ConsentsService);
      http = TestBed.inject(HttpTestingController);
    });

    afterEach(() => {
      environment.useMockApi = prevMock;
      http.verify();
    });

    it('lit une fiche par identifiant', () => {
      service.get('c-1').subscribe();

      const req = http.expectOne(`${BASE}/c-1`);
      expect(req.request.method).toBe('GET');
      req.flush({} as ConsentView);
    });

    it('interroge chaque recherche avec son paramètre', () => {
      service.searchBySubject('alice@example.fr').subscribe();
      const bySubject = http.expectOne(r => r.url === `${BASE}/search`);
      expect(bySubject.request.params.get('subjectIdentifier')).toBe('alice@example.fr');
      bySubject.flush([]);

      service.searchByPurpose('newsletter.marketing').subscribe();
      const byPurpose = http.expectOne(r => r.url === `${BASE}/by-purpose`);
      expect(byPurpose.request.params.get('purposeCode')).toBe('newsletter.marketing');
      byPurpose.flush([]);
    });

    it('demande le consentement opposable sur les deux critères', () => {
      service.active('alice@example.fr', 'newsletter.marketing').subscribe();

      const req = http.expectOne(r => r.url === `${BASE}/active`);
      expect(req.request.params.get('subjectIdentifier')).toBe('alice@example.fr');
      expect(req.request.params.get('purposeCode')).toBe('newsletter.marketing');
      req.flush(null);
    });

    it('recueille en POST sur la collection et retire sur un sous-chemin', () => {
      const body = grantReq();
      service.grant(body).subscribe();
      const post = http.expectOne(BASE);
      expect(post.request.method).toBe('POST');
      expect(post.request.body).toEqual(body);
      post.flush({} as ConsentView);

      service.withdraw('c-1', { actorUserId: AUTHOR, reason: 'r' }).subscribe();
      const withdraw = http.expectOne(`${BASE}/c-1/withdraw`);
      expect(withdraw.request.method).toBe('POST');
      expect(withdraw.request.body).toEqual({ actorUserId: AUTHOR, reason: 'r' });
      withdraw.flush({} as ConsentView);
    });

    it('déclenche le balayage des échéances avec sa borne', () => {
      service.expireDue().subscribe();
      const byDefault = http.expectOne(r => r.url === `${BASE}/expire-due`);
      expect(byDefault.request.method).toBe('POST');
      expect(byDefault.request.params.get('limit')).toBe('200');
      byDefault.flush({ expired: 0 });

      service.expireDue(50).subscribe();
      const bounded = http.expectOne(r => r.url === `${BASE}/expire-due`);
      expect(bounded.request.params.get('limit')).toBe('50');
      bounded.flush({ expired: 0 });
    });
  });
});
