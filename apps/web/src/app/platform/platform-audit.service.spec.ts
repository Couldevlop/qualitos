import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';

import { environment } from '../../environments/environment';
import { PlatformAuditService } from './platform-audit.service';
import { PlatformAuditFilter } from './platform-audit.types';

describe('PlatformAuditService', () => {

  const base = `${environment.apiBaseUrl}/api/v1/platform-audit`;
  let service: PlatformAuditService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [PlatformAuditService, provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(PlatformAuditService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('n’envoie que les critères renseignés', () => {
    service.list({ targetTenantId: 'c0ffee00-0000-4000-8000-000000000001' }, 0, 50).subscribe();

    const requete = http.expectOne(
      r => r.url === base && r.params.get('targetTenantId') === 'c0ffee00-0000-4000-8000-000000000001');
    expect(requete.request.params.get('page')).toBe('0');
    expect(requete.request.params.get('size')).toBe('50');
    // Un paramètre vide n'est pas « neutre » : `action=` partirait au serveur et
    // son convertisseur d'énumération le refuserait en 400.
    expect(requete.request.params.has('action')).toBeFalse();
    expect(requete.request.params.has('actorUserId')).toBeFalse();
    requete.flush({ content: [], totalElements: 0, totalPages: 0, number: 0, size: 50 });
  });

  it('n’envoie jamais de paramètre de tri', () => {
    // L'ordre est figé côté dépôt (`sequenceNo DESC`) : un tri non mappé
    // partirait en 400 sur un écran qui n'offre pourtant aucun tri.
    service.list({}, 1, 25).subscribe();

    const requete = http.expectOne(r => r.url === base);
    expect(requete.request.params.has('sort')).toBeFalse();
    requete.flush({ content: [], totalElements: 0, totalPages: 0, number: 1, size: 25 });
  });

  it('vérifie toute la chaîne quand aucune borne n’est donnée', () => {
    // Pas de bornes = le serveur couvre tout le journal. Envoyer des bornes par
    // défaut depuis le client rendrait un « intact » sur une fenêtre arbitraire.
    service.verify().subscribe();

    const requete = http.expectOne(`${base}/verify`);
    expect(requete.request.params.keys().length).toBe(0);
    requete.flush({ fromSeq: 1, toSeq: 12, examined: 12, intact: true, breaks: [] });
  });

  it('transmet les bornes de vérification quand elles sont données', () => {
    service.verify(10, 20).subscribe();

    const requete = http.expectOne(r => r.url === `${base}/verify`);
    expect(requete.request.params.get('fromSeq')).toBe('10');
    expect(requete.request.params.get('toSeq')).toBe('20');
    requete.flush({ fromSeq: 10, toSeq: 20, examined: 11, intact: true, breaks: [] });
  });

  it('demande le dossier d’audit en lecture', () => {
    service.export().subscribe();

    const requete = http.expectOne(`${base}/export`);
    expect(requete.request.method).toBe('GET');
    requete.flush({
      generatedAt: '2026-11-08T09:30:00Z', fromSeq: 1, toSeq: 3, count: 3,
      seal: 'a'.repeat(64),
      verification: { fromSeq: 1, toSeq: 3, examined: 3, intact: true, breaks: [] },
      events: []
    });
  });

  describe('critère réellement appliqué', () => {

    // Le serveur applique UN seul critère, dans cet ordre. Si cette cascade
    // change côté serveur, c'est ce bloc qui doit la suivre — sans quoi l'écran
    // annoncerait un filtre que le serveur n'a pas retenu, ce qui est pire que
    // de ne rien annoncer.
    const cas: ReadonlyArray<[string, PlatformAuditFilter, string]> = [
      ['le client passe avant tout', { targetTenantId: 'x', action: 'TENANT_CREATED' }, 'tenant'],
      ['puis l’acte', { action: 'TENANT_CREATED', actorUserId: 'y' }, 'action'],
      ['puis l’auteur', { actorUserId: 'y', from: 'a', to: 'b' }, 'actor'],
      ['la période exige ses DEUX bornes', { from: 'a', to: 'b' }, 'period'],
      ['une seule borne ne filtre rien', { from: 'a' }, 'none'],
      ['aucun critère', {}, 'none']
    ];

    for (const [nom, filtre, attendu] of cas) {
      it(nom, () => {
        expect(service.effectiveCriterion(filtre)).toBe(attendu as never);
      });
    }
  });

  it('n’expose aucune méthode d’écriture', () => {
    // Une surface d'écriture, même réservée à l'éditeur, permettrait de
    // FABRIQUER un acte qui n'a pas eu lieu. Le serveur n'en offre aucune ; ce
    // banc interdit qu'on en ajoute une ici par commodité.
    const api = Object.getOwnPropertyNames(PlatformAuditService.prototype);
    expect(api.sort()).toEqual(['constructor', 'effectiveCriterion', 'export', 'list', 'verify']);
  });
});
