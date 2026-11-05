import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { SharedModule } from '../../shared.module';
import { UiModule } from '../ui.module';
import { KpiCardComponent } from './kpi-card.component';

/**
 * Carte KPI — composant partagé, réutilisé sur les trois niveaux de tableaux de
 * bord (§7.1).
 *
 * Le point sensible n'est pas l'affichage mais le SENS de la tendance : une
 * flèche verte vers le haut est juste pour un taux de conformité, fausse pour un
 * taux de rebut. `trendInvertedIsGood` porte cette distinction, et une erreur à
 * cet endroit ferait lire une dégradation comme une amélioration.
 */
describe('KpiCardComponent', () => {
  let fixture: ComponentFixture<KpiCardComponent>;
  let component: KpiCardComponent;

  function el(): HTMLElement {
    return fixture.nativeElement as HTMLElement;
  }

  /**
   * Le composant est en `OnPush` : affecter une entrée directement sur
   * l'instance ne le marque pas à recalculer, et les rendus suivants seraient
   * sans effet. `setInput` reproduit exactement ce que fait Angular quand
   * l'entrée est liée depuis un parent — y compris le marquage.
   */
  function render(inputs: Record<string, unknown> = {}): void {
    Object.entries(inputs).forEach(([name, value]) => fixture.componentRef.setInput(name, value));
    fixture.detectChanges();
  }

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SharedModule, UiModule, NoopAnimationsModule]
    }).compileComponents();

    fixture = TestBed.createComponent(KpiCardComponent);
    component = fixture.componentInstance;
  });

  // ---- Sens de la tendance ---------------------------------------------------

  it('lit une hausse comme une amélioration par défaut', () => {
    component.trend = 2.5;

    expect(component.trendClass()).toBe('qos-kpi__trend--up');
    expect(component.trendIcon()).toBe('trending_up');
  });

  it('lit une baisse comme une dégradation par défaut', () => {
    component.trend = -2.5;

    expect(component.trendClass()).toBe('qos-kpi__trend--down');
    expect(component.trendIcon()).toBe('trending_down');
  });

  it('inverse la lecture pour un indicateur où baisser est bon', () => {
    // Rebuts, délai de clôture CAPA, DPMO : la baisse est la bonne nouvelle.
    component.trendInvertedIsGood = true;

    component.trend = -2.5;
    expect(component.trendClass()).toBe('qos-kpi__trend--up');
    // La flèche, elle, garde le sens du mouvement : c'est la COULEUR qui porte
    // le jugement, pas l'icône, sous peine d'afficher une flèche montante sur
    // une valeur qui descend.
    expect(component.trendIcon()).toBe('trending_down');

    component.trend = 2.5;
    expect(component.trendClass()).toBe('qos-kpi__trend--down');
  });

  it('reste neutre sur une tendance nulle ou absente, quel que soit le sens', () => {
    component.trend = 0;
    expect(component.trendClass()).toBe('qos-kpi__trend--flat');
    expect(component.trendIcon()).toBe('remove');

    component.trend = undefined;
    expect(component.trendClass()).toBe('qos-kpi__trend--flat');
    expect(component.trendIcon()).toBe('remove');

    // Une stagnation n'est ni bonne ni mauvaise, même sur un indicateur inversé.
    component.trendInvertedIsGood = true;
    component.trend = 0;
    expect(component.trendClass()).toBe('qos-kpi__trend--flat');
  });

  // ---- Mise en forme ----------------------------------------------------------

  it('signe explicitement une tendance positive et l\'arrondit au dixième', () => {
    component.trend = 2.46;
    expect(component.formattedTrend()).toBe('+2.5%');

    component.trend = -2.44;
    expect(component.formattedTrend()).toBe('-2.4%');

    component.trend = 0;
    expect(component.formattedTrend()).toBe('0.0%');
  });

  it('n\'affiche rien plutôt qu\'un zéro trompeur quand la tendance est absente', () => {
    component.trend = undefined;

    expect(component.formattedTrend()).toBe('');
  });

  // ---- Rendu -------------------------------------------------------------------

  it('affiche le libellé, la valeur et l\'unité', () => {
    render({ label: 'Coût d\'obtention de la qualité', value: 2.8, unit: '% CA' });

    expect(el().textContent).toContain('Coût d\'obtention de la qualité');
    expect(el().querySelector('.qos-kpi__value')?.textContent).toContain('2.8');
    expect(el().querySelector('.qos-kpi__unit')?.textContent).toContain('% CA');
  });

  it('porte un libellé accessible qui reprend la mesure complète', () => {
    render({ label: 'DPMO', value: 3.4, unit: 'ppm' });

    // Un lecteur d'écran doit entendre la mesure, pas seulement le titre.
    expect(el().querySelector('article')?.getAttribute('aria-label'))
      .toBe('DPMO: 3.4 ppm');
  });

  it('n\'affiche la tendance et la cible que si elles existent', () => {
    render({ value: 12 });
    expect(el().querySelector('.qos-kpi__trend')).toBeNull();
    expect(el().querySelector('.qos-kpi__target')).toBeNull();

    render({ trend: 1.2 });
    expect(el().querySelector('.qos-kpi__trend')).not.toBeNull();
    expect(el().querySelector('.qos-kpi__target')).toBeNull();

    render({ target: 10 });
    expect(el().querySelector('.qos-kpi__target')?.textContent).toContain('10');
  });

  it('affiche une tendance nulle plutôt que de masquer la ligne', () => {
    // 0 est une information — la stagnation — que `*ngIf="trend != null"` doit
    // laisser passer, contrairement à ce que ferait un test de véracité.
    render({ value: 12, trend: 0 });

    expect(el().querySelector('.qos-kpi__trend')).not.toBeNull();
  });

  it('affiche l\'icône et la description seulement si elles sont fournies', () => {
    render({ value: 12 });
    expect(el().querySelector('.qos-kpi__icon')).toBeNull();
    expect(el().querySelector('.qos-kpi__desc')).toBeNull();

    render({ icon: 'paid', description: 'Prévention + détection + défaillances.' });
    expect(el().querySelector('.qos-kpi__icon')?.textContent).toContain('paid');
    expect(el().querySelector('.qos-kpi__desc')?.textContent).toContain('Prévention');
  });

  // ---- États -------------------------------------------------------------------

  it('remplace la valeur par un squelette pendant le chargement', () => {
    render({ value: 42, loading: true });

    expect(el().querySelector('.qos-kpi__skeleton')).not.toBeNull();
    expect(el().querySelector('.qos-kpi__value')).toBeNull();
    // L'assistance vocale doit savoir que la carte n'est pas encore renseignée.
    expect(el().querySelector('article')?.getAttribute('aria-busy')).toBe('true');
  });

  it('traduit l\'état et la taille en classes de la carte', () => {
    render({ state: 'bad', size: 'lg' });

    const card = el().querySelector('article')!;
    expect(card.classList).toContain('qos-kpi--bad');
    expect(card.classList).toContain('qos-kpi--lg');
    expect(card.classList).not.toContain('qos-kpi--sm');
  });

  it('n\'ajoute aucune classe d\'état pour une carte neutre de taille moyenne', () => {
    render();

    const card = el().querySelector('article')!;
    expect(card.classList).not.toContain('qos-kpi--good');
    expect(card.classList).not.toContain('qos-kpi--warn');
    expect(card.classList).not.toContain('qos-kpi--bad');
    expect(card.classList).not.toContain('qos-kpi--sm');
    expect(card.classList).not.toContain('qos-kpi--lg');
  });

  it('accepte une valeur textuelle, pour les indicateurs non numériques', () => {
    render({ label: 'Niveau Sigma', value: 'σ 4,2' });

    expect(el().querySelector('.qos-kpi__value')?.textContent).toContain('σ 4,2');
  });

  // ---- Règle de mesure ---------------------------------------------------------
  //
  // C'est le dispositif qui rend la tuile lisible : la valeur se juge CONTRE sa
  // cible, pas dans l'absolu. Les positions doivent donc rester fidèles aux
  // seules données affichées — aucune échelle inventée.

  it('dessine la règle dès qu\'il y a une valeur chiffrée et une cible', () => {
    render({ value: 2.8, target: 3.2, unit: '% CA' });

    expect(component.hasRail).toBeTrue();
    expect(el().querySelector('.qos-kpi__rail')).not.toBeNull();
    expect(el().querySelector('.qos-kpi__rail-target')).not.toBeNull();
    expect(el().querySelector('.qos-kpi__rail-dot')).not.toBeNull();
  });

  it('place la valeur avant la cible quand elle lui est inférieure', () => {
    render({ value: 2.8, target: 3.2 });

    expect(component.valuePercent).toBeLessThan(component.targetPercent);
  });

  it('place la valeur après la cible quand elle la dépasse', () => {
    // Un indicateur au-dessus de sa cible doit SE VOIR au-delà du repère : le
    // dépassement est précisément ce qu'on vient lire.
    render({ value: 10, target: 5 });

    expect(component.valuePercent).toBeGreaterThan(component.targetPercent);
  });

  it('laisse de l\'air au bout de la règle plutôt que de coller au bord', () => {
    // Un point collé à l'extrémité se lit comme un maximum atteint, ce que la
    // donnée ne dit pas.
    render({ value: 100, target: 80 });

    expect(component.valuePercent).toBeLessThan(100);
    expect(component.valuePercent).toBeGreaterThan(70);
  });

  it('ramène une valeur négative au début de la règle sans sortir du cadre', () => {
    render({ value: -5, target: 10 });

    expect(component.valuePercent).toBe(0);
  });

  it('renonce à la règle sans cible, et retombe sur le liseré d\'état', () => {
    render({ value: 42, state: 'bad' });

    expect(component.hasRail).toBeFalse();
    expect(el().querySelector('.qos-kpi__rail')).toBeNull();
    expect(el().querySelector('article')!.classList).toContain('qos-kpi--no-rail');
  });

  it('renonce à la règle sur une valeur textuelle, qu\'elle ne sait pas situer', () => {
    render({ value: 'σ 4,2', target: 5 });

    expect(component.hasRail).toBeFalse();
    expect(el().querySelector('.qos-kpi__rail')).toBeNull();
  });

  it('renonce à la règle pendant le chargement', () => {
    render({ value: 2.8, target: 3.2, loading: true });

    expect(component.hasRail).toBeFalse();
  });

  it('énonce la cible dans le libellé accessible, puisque la règle est muette', () => {
    // La règle porte `aria-hidden` : un graphique ne se lit pas à voix haute.
    // Son unique information doit donc être dite en mots, sinon elle est perdue.
    render({ label: 'COQ', value: 2.8, unit: '% CA', target: 3.2 });

    const label = el().querySelector('article')!.getAttribute('aria-label')!;
    expect(label).toContain('COQ: 2.8 % CA');
    expect(label).toContain('3.2');
    expect(el().querySelector('.qos-kpi__rail')!.getAttribute('aria-hidden')).toBe('true');
  });
});
