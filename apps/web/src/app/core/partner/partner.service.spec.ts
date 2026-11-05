import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { environment } from '../../../environments/environment';
import { PartnerService } from './partner.service';

/**
 * Les trois chemins du domaine partenaire.
 *
 * <p>Ce que ce banc tient : <b>aucun identifiant de client ne voyage dans une
 * URL ni dans un corps</b>, sauf la proposition de l'éditeur, qui désigne
 * explicitement le client visé parce que c'est son objet. Partout ailleurs le
 * client vient du jeton (`CLAUDE.md` §18.2.2) — et si quelqu'un ajoute un jour
 * un paramètre `tenantId` « pour déboguer », c'est ici que ça tombe.
 */
describe('PartnerService', () => {
  const firms = `${environment.apiBaseUrl}/api/v1/partner-firms`;
  const access = `${environment.apiBaseUrl}/api/v1/partner-access`;
  const portfolio = `${environment.apiBaseUrl}/api/v1/partner/portfolio`;

  let service: PartnerService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()]
    });
    service = TestBed.inject(PartnerService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  // ---------- côté éditeur ----------

  it('liste les cabinets par page', () => {
    service.listFirms().subscribe();
    let req = http.expectOne(r => r.url === firms);
    expect(req.request.params.get('page')).toBe('0');
    expect(req.request.params.get('size')).toBe('50');
    req.flush({ content: [], totalElements: 0 });

    service.listFirms(3, 10).subscribe();
    req = http.expectOne(r => r.url === firms);
    expect(req.request.params.get('page')).toBe('3');
    expect(req.request.params.get('size')).toBe('10');
    req.flush({ content: [], totalElements: 0 });
  });

  it('crée et modifie un cabinet', () => {
    const corps = {
      slug: 'qualiconseil', name: 'QualiConseil',
      registrationNumber: null, contactEmail: null, contactPhone: null
    };

    service.createFirm(corps).subscribe();
    let req = http.expectOne(firms);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(corps);
    req.flush({});

    service.updateFirm('f-1', corps).subscribe();
    req = http.expectOne(`${firms}/f-1`);
    expect(req.request.method).toBe('PUT');
    req.flush({});
  });

  it('bascule l’état d’un cabinet par une route dédiée', () => {
    service.setFirmActive('f-1', false).subscribe();
    const req = http.expectOne(`${firms}/f-1/active`);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual({ active: false });
    req.flush({});
  });

  it('inscrit et bascule un consultant SOUS son cabinet', () => {
    service.addConsultant('f-1', { displayName: 'Claire', email: 'claire@x.fr' }).subscribe();
    let req = http.expectOne(`${firms}/f-1/consultants`);
    expect(req.request.method).toBe('POST');
    req.flush({});

    // Le cabinet est dans le chemin : le serveur vérifie ainsi que la fiche
    // appartient bien au cabinet qu'on nomme, plutôt que de faire confiance à
    // l'identifiant seul.
    service.setConsultantActive('f-1', 'c-1', true).subscribe();
    req = http.expectOne(`${firms}/f-1/consultants/c-1/active`);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual({ active: true });
    req.flush({});
  });

  it('lit le portefeuille d’un cabinet, et y propose un client nommé', () => {
    service.portfolioOf('f-1').subscribe();
    let req = http.expectOne(`${firms}/f-1/portfolio`);
    expect(req.request.method).toBe('GET');
    req.flush([]);

    // La seule route où un identifiant de client voyage : c'est son objet.
    service.propose('f-1', { tenantId: 't-1', scope: 'READ', purpose: null }).subscribe();
    req = http.expectOne(`${firms}/f-1/portfolio`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ tenantId: 't-1', scope: 'READ', purpose: null });
    req.flush({});
  });

  // ---------- côté client ----------

  it('lit ses accords sans nommer son client', () => {
    service.myEngagements().subscribe();
    const req = http.expectOne(access);
    expect(req.request.method).toBe('GET');
    // Ni paramètre, ni corps : le client vient du jeton.
    expect(req.request.params.keys().length).toBe(0);
    req.flush([]);
  });

  it('accorde, refuse et reprend par trois routes distinctes', () => {
    service.grant('e-1', { scope: 'INTERVENE', endsOn: '2027-01-31', purpose: null }).subscribe();
    let req = http.expectOne(`${access}/e-1/grant`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ scope: 'INTERVENE', endsOn: '2027-01-31', purpose: null });
    req.flush({});

    // Refuser et reprendre sont deux routes, pas un drapeau : le premier n'a
    // jamais rien ouvert, le second ferme un accès qui existait.
    service.decline('e-1').subscribe();
    req = http.expectOne(`${access}/e-1/decline`);
    expect(req.request.method).toBe('POST');
    req.flush({});

    service.revoke('e-1').subscribe();
    req = http.expectOne(`${access}/e-1/revoke`);
    expect(req.request.method).toBe('POST');
    req.flush({});
  });

  // ---------- côté consultant ----------

  it('lit son portefeuille et se rattache sans rien dire de son cabinet', () => {
    service.myPortfolio().subscribe();
    let req = http.expectOne(portfolio);
    expect(req.request.method).toBe('GET');
    req.flush([]);

    // Le courriel n'est pas envoyé : le serveur le lit dans le jeton. L'envoyer
    // laisserait s'inscrire dans un cabinet sous le courriel d'autrui.
    service.linkAccount().subscribe();
    req = http.expectOne(`${portfolio}/link`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({});
    req.flush(null);
  });

  it('rend null quand aucune fiche n’attend le compte', () => {
    let recu: unknown = 'inchangé';
    service.linkAccount().subscribe(v => recu = v);
    // 204 : le serveur ne renvoie pas de corps, ce n'est pas une panne.
    http.expectOne(`${portfolio}/link`).flush(null, { status: 204, statusText: 'No Content' });

    expect(recu).toBeNull();
  });
});
