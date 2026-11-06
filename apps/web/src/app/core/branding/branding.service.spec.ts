import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { DOCUMENT } from '@angular/core';
import { TestBed } from '@angular/core/testing';

import { environment } from '../../../environments/environment';
import { TenantIdentityResponse, TenantTheme } from '../../features/admin/admin.types';
import { ThemeService } from '../theme/theme.service';
import { BrandingService } from './branding.service';

/** Ce que le service pose sur `<html>` : classes et propriétés en ligne. */
interface FauxHtml {
  classes: Set<string>;
  styles: Map<string, string>;
}

function faireDocument(): { doc: Document; html: FauxHtml } {
  const html: FauxHtml = { classes: new Set<string>(), styles: new Map<string, string>() };
  const documentElement = {
    classList: {
      add: (c: string) => html.classes.add(c),
      remove: (...cs: string[]) => cs.forEach(c => html.classes.delete(c))
    },
    style: {
      setProperty: (k: string, v: string) => html.styles.set(k, v),
      removeProperty: (k: string) => html.styles.delete(k)
    }
  };
  return { doc: { documentElement } as unknown as Document, html };
}

function marque(partiel: Partial<TenantIdentityResponse> = {}): TenantIdentityResponse {
  return {
    slogan: null, legalName: null, legalForm: null, registrationNumber: null,
    vatNumber: null, addressLine1: null, addressLine2: null, postalCode: null,
    city: null, countryCode: null, contactEmail: null, contactPhone: null,
    primaryColor: null, secondaryColor: null,
    themeCode: 'AIRY_LIGHT' as TenantTheme, logoAvailable: false,
    ...partiel
  };
}

/**
 * La marque d'un client, posée sur l'application.
 *
 * <p>Trois exigences s'y croisent : le thème choisi par le client doit se voir,
 * les couleurs choisies doivent rester lisibles, et rien de tout cela ne doit
 * pouvoir empêcher l'application de s'afficher.
 */
describe('BrandingService', () => {

  const URL = `${environment.apiBaseUrl}/api/v1/branding`;

  let service: BrandingService;
  let http: HttpTestingController;
  let html: FauxHtml;
  let theme: jasmine.SpyObj<ThemeService>;

  beforeEach(() => {
    const ctx = faireDocument();
    html = ctx.html;
    theme = jasmine.createSpyObj<ThemeService>('ThemeService',
      ['applyOrganisationDefault', 'currentMode']);
    theme.currentMode.and.returnValue('light');

    TestBed.resetTestingModule().configureTestingModule({
      providers: [
        provideHttpClient(), provideHttpClientTesting(),
        { provide: DOCUMENT, useValue: ctx.doc },
        { provide: ThemeService, useValue: theme }
      ]
    });
    service = TestBed.inject(BrandingService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  // ---------- la lecture ----------

  it('lit la marque du client du JETON : aucun identifiant dans l URL', () => {
    service.load().subscribe();

    const requete = http.expectOne(URL);
    expect(requete.request.method).toBe('GET');
    // CLAUDE.md §18.2.2 : le client ne se lit ni du corps ni du chemin.
    expect(requete.request.urlWithParams).toBe(URL);
    requete.flush(marque());
  });

  it('ne rappelle pas le serveur à chaque écran', () => {
    service.load().subscribe();
    http.expectOne(URL).flush(marque({ slogan: 'Zéro défaut' }));

    let vu: string | null = 'rien';
    service.slogan().subscribe(s => (vu = s));

    http.expectNone(URL);
    expect(vu).toBe('Zéro défaut');
  });

  it('un serveur muet ne casse rien : on garde les jetons du socle', () => {
    let resultat: TenantIdentityResponse | null | undefined;
    service.load().subscribe(m => (resultat = m));

    http.expectOne(URL).flush('en panne', { status: 500, statusText: 'Server Error' });

    expect(resultat).toBeNull();
    expect(html.classes.size).toBe(0);
    expect(html.styles.size).toBe(0);
  });

  it('un slogan fait d espaces vaut pas de slogan', () => {
    service.load().subscribe();
    http.expectOne(URL).flush(marque({ slogan: '   ' }));

    let vu: string | null = 'rien';
    service.slogan().subscribe(s => (vu = s));

    expect(vu).toBeNull();
  });

  // ---------- le thème ----------

  it('pose la classe du thème choisi', () => {
    service.apply(marque({ themeCode: 'DENSE_LIGHT' as TenantTheme }));

    expect(html.classes.has('qos-client-dense')).toBeTrue();
  });

  it('un thème inconnu retombe sur le défaut au lieu de ne rien poser', () => {
    // Un code venu d'une version plus récente du serveur ne doit pas laisser
    // l'écran sans thème : le défaut est le comportement antérieur.
    service.apply(marque({ themeCode: 'ARC_EN_CIEL' as TenantTheme }));

    expect(html.classes.has('qos-client-airy')).toBeTrue();
  });

  it('change de thème sans empiler les classes', () => {
    service.apply(marque({ themeCode: 'DARK' as TenantTheme }));
    service.apply(marque({ themeCode: 'DENSE_LIGHT' as TenantTheme }));

    expect(html.classes.has('qos-client-dark')).toBeFalse();
    expect([...html.classes]).toEqual(['qos-client-dense']);
  });

  it('le thème sombre du client n est qu un DÉFAUT, pas un ordre', () => {
    service.apply(marque({ themeCode: 'DARK' as TenantTheme }));

    // C'est `ThemeService` qui tranche : lui seul sait si la personne a choisi.
    expect(theme.applyOrganisationDefault).toHaveBeenCalledWith('dark');
  });

  it('les trois thèmes clairs demandent le mode clair', () => {
    for (const code of ['AIRY_LIGHT', 'DENSE_LIGHT', 'HIGH_CONTRAST']) {
      service.apply(marque({ themeCode: code as TenantTheme }));
      expect(theme.applyOrganisationDefault).toHaveBeenCalledWith('light');
    }
  });

  // ---------- les couleurs ----------

  it('surcharge les jetons d accent avec la couleur du client', () => {
    service.apply(marque({ primaryColor: '#b31d1d' }));

    expect(html.styles.get('--qos-accent-bg')).toBe('#b31d1d');
    expect(html.styles.get('--qos-chart-1')).toBe('#b31d1d');
    expect(html.styles.get('--qos-accent-bg-soft')).toBe('rgba(179, 29, 29, 0.12)');
  });

  it('choisit un libellé LISIBLE sur la couleur, pas un blanc par défaut', () => {
    // Le serveur garantit que la couleur se lit sur la page ; il ne dit rien du
    // texte posé SUR elle. Un bouton jaune à libellé blanc passerait sa garde.
    service.apply(marque({ primaryColor: '#ffd400' }));

    expect(html.styles.get('--qos-accent-fg')).toBe('#11181b');
  });

  it('la couleur secondaire alimente la deuxième série des graphiques', () => {
    service.apply(marque({ secondaryColor: '#00806a' }));

    expect(html.styles.get('--qos-chart-2')).toBe('#00806a');
  });

  it('une couleur mal formée est ignorée, le reste est posé quand même', () => {
    service.apply(marque({ primaryColor: '#fff', secondaryColor: '#00806a' }));

    expect(html.styles.has('--qos-accent-bg')).toBeFalse();
    expect(html.styles.get('--qos-chart-2')).toBe('#00806a');
  });

  it('retire les surcharges précédentes avant d en poser de nouvelles', () => {
    service.apply(marque({ primaryColor: '#b31d1d', secondaryColor: '#00806a' }));

    service.apply(marque({ primaryColor: '#2563d6' }));

    expect(html.styles.get('--qos-accent-bg')).toBe('#2563d6');
    expect(html.styles.has('--qos-chart-2')).toBeFalse();
  });

  it('en mode sombre, le survol ÉCLAIRCIT au lieu d assombrir', () => {
    theme.currentMode.and.returnValue('dark');

    service.apply(marque({ themeCode: 'DARK' as TenantTheme, primaryColor: '#2563d6' }));

    // Assombrir sur une page sombre rendait le survol invisible.
    expect(html.styles.get('--qos-accent-bg-hover')).not.toBe('#1f54b4');
    expect(html.styles.get('--qos-accent-bg-soft')).toBe('rgba(37, 99, 214, 0.18)');
  });

  it('une marque absente rend l application à ses jetons', () => {
    service.apply(marque({ themeCode: 'DARK' as TenantTheme, primaryColor: '#b31d1d' }));

    service.apply(null);

    expect(html.classes.size).toBe(0);
    expect(html.styles.size).toBe(0);
  });
});
