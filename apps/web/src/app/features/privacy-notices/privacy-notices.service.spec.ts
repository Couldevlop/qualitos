import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { PrivacyNoticesService } from './privacy-notices.service';
import {
  CreatePrivacyNoticeRequest,
  EditPrivacyNoticeRequest,
  PrivacyNoticeView
} from './privacy-notices.types';

/**
 * Mentions d'information (RGPD art. 13 et 14).
 *
 * Le service porte deux implémentations du même contrat : un magasin en mémoire
 * (démo sans backend) et les appels HTTP réels. Le magasin rejoue le cycle
 * DRAFT → PUBLISHED → ARCHIVED et surtout l'invariant qui fait la valeur
 * probatoire du registre : une seule mention publiée à la fois par couple
 * référence + langue, la précédente étant archivée avec sa date de fin d'effet.
 * Les deux modes sont testés.
 */
describe('PrivacyNoticesService', () => {

  const AUTHOR = 'demo-user';
  const BASE = `${environment.apiBaseUrl}/api/v1/gdpr/privacy-notices`;

  const createReq = (over: Partial<CreatePrivacyNoticeRequest> = {}): CreatePrivacyNoticeRequest => ({
    reference: 'PUBLIC_WEB_NOTICE',
    version: '2026.2',
    language: 'fr',
    title: 'Politique de confidentialité — révision 2026.2',
    createdByUserId: AUTHOR,
    ...over
  });

  const editReq = (over: Partial<EditPrivacyNoticeRequest> = {}): EditPrivacyNoticeRequest => ({
    title: 'Mention RH — Recrutement (candidats) — v2',
    ...over
  });

  // ------------------------------------------------------------------------
  // Magasin en mémoire
  // ------------------------------------------------------------------------
  describe('en mode démo (magasin en mémoire)', () => {
    let service: PrivacyNoticesService;
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
      service = TestBed.inject(PrivacyNoticesService);
      http = TestBed.inject(HttpTestingController);
    });

    afterEach(() => {
      environment.useMockApi = prevMock;
      // Le mode démo ne doit émettre AUCUNE requête réseau.
      http.verify();
    });

    // ---- Lectures ----------------------------------------------------------

    it('liste les mentions pré-chargées et sait les filtrer par statut', fakeAsync(() => {
      expect(run(service.list()).length).toBe(3);

      expect(run(service.list('DRAFT')).map(n => n.id)).toEqual(['pn-3']);
      expect(run(service.list('ARCHIVED'))).toEqual([]);
    }));

    it('regroupe les versions d\'une même référence, toutes langues confondues', fakeAsync(() => {
      expect(run(service.versions('PUBLIC_WEB_NOTICE')).map(n => n.language)).toEqual(['fr', 'en']);
      expect(run(service.versions('REFERENCE-ABSENTE'))).toEqual([]);
    }));

    it('résout une mention par identifiant, avec repli sur la première si inconnue', fakeAsync(() => {
      expect(run(service.get('pn-2')).language).toBe('en');
      // Repli assumé du mode démo : les écrans restent utilisables sans backend.
      expect(run(service.get('pn-inexistante')).id).toBe('pn-1');
    }));

    // ---- Mention publiée en vigueur ----------------------------------------

    it('ne rend en vigueur que la mention publiée de la langue demandée', fakeAsync(() => {
      expect(run(service.findPublished('PUBLIC_WEB_NOTICE', 'fr'))?.id).toBe('pn-1');
      expect(run(service.findPublished('PUBLIC_WEB_NOTICE', 'en'))?.id).toBe('pn-2');
    }));

    it('ne rend rien quand aucune mention publiée ne correspond', fakeAsync(() => {
      // langue absente, référence absente, et référence encore à l'état de brouillon
      expect(run(service.findPublished('PUBLIC_WEB_NOTICE', 'es'))).toBeNull();
      expect(run(service.findPublished('REFERENCE-ABSENTE', 'fr'))).toBeNull();
      expect(run(service.findPublished('HR_RECRUITMENT_NOTICE', 'fr'))).toBeNull();
    }));

    // ---- Création ----------------------------------------------------------

    it('crée un brouillon en tête de liste', fakeAsync(() => {
      const created = run(service.create(createReq()));

      expect(created.status).toBe('DRAFT');
      expect(created.publishedAt).toBeUndefined();
      expect(run(service.list())[0].version).toBe('2026.2');
    }));

    it('normalise en tableau vide les traitements liés non fournis', fakeAsync(() => {
      expect(run(service.create(createReq())).linkedProcessingActivityIds).toEqual([]);

      expect(run(service.create(createReq({
        version: '2026.3', linkedProcessingActivityIds: ['ropa-1']
      }))).linkedProcessingActivityIds).toEqual(['ropa-1']);
    }));

    // ---- Édition -----------------------------------------------------------

    it('remplace les champs de la mention éditée', fakeAsync(() => {
      const edited = run(service.edit('pn-3', editReq({
        summary: 'Mention révisée après avis du délégué.',
        contactEmail: 'dpo@qualitos.io'
      })));

      expect(edited.title).toBe('Mention RH — Recrutement (candidats) — v2');
      expect(edited.summary).toBe('Mention révisée après avis du délégué.');
    }));

    it('remet à vide les traitements liés absents de l\'édition', fakeAsync(() => {
      run(service.edit('pn-3', editReq({ linkedProcessingActivityIds: ['ropa-9'] })));
      expect(run(service.get('pn-3')).linkedProcessingActivityIds).toEqual(['ropa-9']);

      run(service.edit('pn-3', editReq()));
      expect(run(service.get('pn-3')).linkedProcessingActivityIds).toEqual([]);
    }));

    it('édite sans effet de bord quand la mention visée n\'existe pas', fakeAsync(() => {
      const before = run(service.get('pn-1')).title;

      run(service.edit('pn-inexistante', editReq()));

      expect(run(service.get('pn-1')).title).toBe(before);
    }));

    // ---- Publication -------------------------------------------------------

    it('publie la mention et ouvre sa période d\'effet', fakeAsync(() => {
      const published = run(service.publish('pn-3', { publishedByUserId: AUTHOR }));

      expect(published.status).toBe('PUBLISHED');
      expect(published.publishedByUserId).toBe(AUTHOR);
      expect(published.publishedAt).toBeTruthy();
      expect(published.effectiveFrom).toBeTruthy();
    }));

    it('archive la version précédente de la même langue — une seule en vigueur', fakeAsync(() => {
      const next = run(service.create(createReq({ version: '2026.2', language: 'fr' })));

      run(service.publish(next.id, { publishedByUserId: AUTHOR }));

      const previous = run(service.get('pn-1'));
      expect(previous.status).toBe('ARCHIVED');
      expect(previous.effectiveTo).toBeTruthy();
      expect(run(service.findPublished('PUBLIC_WEB_NOTICE', 'fr'))?.id).toBe(next.id);
    }));

    it('ne touche pas aux autres langues ni aux autres références', fakeAsync(() => {
      const next = run(service.create(createReq({ version: '2026.2', language: 'fr' })));

      run(service.publish(next.id, { publishedByUserId: AUTHOR }));

      // La version anglaise reste en vigueur : chaque locale a son cycle propre.
      expect(run(service.get('pn-2')).status).toBe('PUBLISHED');
    }));

    it('publie sans effet de bord quand la mention visée n\'existe pas', fakeAsync(() => {
      run(service.publish('pn-inexistante', { publishedByUserId: AUTHOR }));

      expect(run(service.get('pn-3')).status).toBe('DRAFT');
    }));

    // ---- Archivage / suppression -------------------------------------------

    it('archive une mention et date sa fin d\'effet', fakeAsync(() => {
      const archived = run(service.archive('pn-1'));

      expect(archived.status).toBe('ARCHIVED');
      expect(archived.effectiveTo).toBeTruthy();
      expect(run(service.findPublished('PUBLIC_WEB_NOTICE', 'fr'))).toBeNull();
    }));

    it('archive sans effet de bord quand la mention visée n\'existe pas', fakeAsync(() => {
      run(service.archive('pn-inexistante'));

      expect(run(service.get('pn-1')).status).toBe('PUBLISHED');
    }));

    it('supprime une mention, et ignore une suppression inconnue', fakeAsync(() => {
      run(service.delete('pn-3'));
      expect(run(service.list()).length).toBe(2);

      run(service.delete('pn-inexistante'));
      expect(run(service.list()).length).toBe(2);
    }));
  });

  // ------------------------------------------------------------------------
  // Appels HTTP réels
  // ------------------------------------------------------------------------
  describe('en mode connecté (HTTP)', () => {
    let service: PrivacyNoticesService;
    let http: HttpTestingController;
    let prevMock: boolean;

    beforeEach(() => {
      prevMock = environment.useMockApi;
      environment.useMockApi = false;
      TestBed.configureTestingModule({
        providers: [provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()]
      });
      service = TestBed.inject(PrivacyNoticesService);
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

      service.list('PUBLISHED').subscribe();
      const filtered = http.expectOne(r => r.url === BASE);
      expect(filtered.request.params.get('status')).toBe('PUBLISHED');
      filtered.flush([]);
    });

    it('lit une fiche par identifiant', () => {
      service.get('n-1').subscribe();

      const req = http.expectOne(`${BASE}/n-1`);
      expect(req.request.method).toBe('GET');
      req.flush({} as PrivacyNoticeView);
    });

    it('demande les versions et la mention en vigueur avec leurs paramètres', () => {
      service.versions('PUBLIC_WEB_NOTICE').subscribe();
      const versions = http.expectOne(r => r.url === `${BASE}/versions`);
      expect(versions.request.params.get('reference')).toBe('PUBLIC_WEB_NOTICE');
      versions.flush([]);

      service.findPublished('PUBLIC_WEB_NOTICE', 'fr').subscribe();
      const published = http.expectOne(r => r.url === `${BASE}/published`);
      expect(published.request.params.get('reference')).toBe('PUBLIC_WEB_NOTICE');
      expect(published.request.params.get('language')).toBe('fr');
      published.flush(null);
    });

    it('crée en POST sur la collection et édite en PUT sur la ressource', () => {
      const body = createReq();
      service.create(body).subscribe();
      const post = http.expectOne(BASE);
      expect(post.request.method).toBe('POST');
      expect(post.request.body).toEqual(body);
      post.flush({} as PrivacyNoticeView);

      const edit = editReq();
      service.edit('n-1', edit).subscribe();
      const put = http.expectOne(`${BASE}/n-1`);
      expect(put.request.method).toBe('PUT');
      expect(put.request.body).toEqual(edit);
      put.flush({} as PrivacyNoticeView);
    });

    it('poste la publication avec son auteur, et l\'archivage sans corps', () => {
      service.publish('n-1', { publishedByUserId: AUTHOR }).subscribe();
      const publish = http.expectOne(`${BASE}/n-1/publish`);
      expect(publish.request.method).toBe('POST');
      expect(publish.request.body).toEqual({ publishedByUserId: AUTHOR });
      publish.flush({} as PrivacyNoticeView);

      service.archive('n-1').subscribe();
      const archive = http.expectOne(`${BASE}/n-1/archive`);
      expect(archive.request.method).toBe('POST');
      expect(archive.request.body).toEqual({});
      archive.flush({} as PrivacyNoticeView);
    });

    it('supprime en DELETE sur la ressource', () => {
      service.delete('n-1').subscribe();

      const req = http.expectOne(`${BASE}/n-1`);
      expect(req.request.method).toBe('DELETE');
      req.flush(null);
    });
  });
});
