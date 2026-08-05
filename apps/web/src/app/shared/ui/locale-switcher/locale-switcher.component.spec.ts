import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { UiModule } from '../ui.module';
import { LocaleSwitcherComponent } from './locale-switcher.component';

/**
 * Changer de langue n'était possible qu'en modifiant l'URL à la main : chaque
 * locale est un build distinct servi sous son propre préfixe (`/fr/`, `/en/`…),
 * et le serveur ne devine la langue qu'une seule fois, à la racine. Un
 * utilisateur arrivé en français y restait, sans le moindre moyen d'en sortir.
 */
describe('LocaleSwitcherComponent', () => {
  let component: LocaleSwitcherComponent;
  let fixture: ComponentFixture<LocaleSwitcherComponent>;

  /** Emplacement simulé : le composant ne doit jamais recharger la page en test. */
  let navigatedTo: string | null;

  beforeEach(async () => {
    navigatedTo = null;

    await TestBed.configureTestingModule({
      imports: [UiModule, NoopAnimationsModule]
    }).compileComponents();

    fixture = TestBed.createComponent(LocaleSwitcherComponent);
    component = fixture.componentInstance;
    component.navigate = (url: string) => { navigatedTo = url; };
  });

  it('propose les six langues servies par la plateforme', () => {
    expect(component.locales.map(l => l.code))
      .toEqual(['fr', 'en', 'es', 'ar', 'ja', 'zh']);
  });

  it('reconnaît la langue courante depuis le préfixe de l’URL', () => {
    component.currentPath = '/en/standards/iso-9001';
    fixture.detectChanges();

    expect(component.currentCode).toBe('en');
  });

  it('retombe sur la langue source quand l’URL ne porte aucun préfixe connu', () => {
    component.currentPath = '/standards';
    fixture.detectChanges();

    expect(component.currentCode).toBe('fr');
  });

  it('conserve la page consultée en changeant de langue', () => {
    component.currentPath = '/fr/standards/iso-9001';

    component.switchTo('en');

    expect(navigatedTo).toBe('/en/standards/iso-9001');
  });

  it('conserve les paramètres de requête et l’ancre', () => {
    component.currentPath = '/fr/kpis';
    component.currentSearch = '?tri=nom';
    component.currentHash = '#section';

    component.switchTo('ja');

    expect(navigatedTo).toBe('/ja/kpis?tri=nom#section');
  });

  it('ajoute le préfixe quand l’URL n’en avait pas', () => {
    component.currentPath = '/audits';

    component.switchTo('es');

    expect(navigatedTo).toBe('/es/audits');
  });

  it('ne recharge rien si la langue choisie est déjà la langue courante', () => {
    // Un rechargement complet pour rien coûte une reconstruction de toute la SPA.
    component.currentPath = '/fr/home';

    component.switchTo('fr');

    expect(navigatedTo).toBeNull();
  });

  it('mène à la racine de la langue quand on est déjà à sa racine', () => {
    component.currentPath = '/fr/';

    component.switchTo('en');

    expect(navigatedTo).toBe('/en/');
  });
});
