import { Directionality } from '@angular/cdk/bidi';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Subject } from 'rxjs';

import { SharedModule } from '../../../../shared/shared.module';
import { FiveWhysStep } from '../../five-whys.types';
import { FiveWhysCascadeComponent } from './five-whys-cascade.component';

/**
 * Ce qui se teste sur un dessin, ce n'est pas son esthétique — aucun test DOM
 * ne dira qu'une flèche est jolie. C'est qu'il PORTE la méthode : autant
 * d'encarts que de pourquoi réellement posés, une progression de teinte du
 * symptôme vers la racine, et un équivalent accessible pour qui ne voit pas
 * l'image.
 */
describe('FiveWhysCascadeComponent', () => {

  let fixture: ComponentFixture<FiveWhysCascadeComponent>;
  let component: FiveWhysCascadeComponent;

  const step = (position: number, answer = `Réponse ${position}`): FiveWhysStep => ({
    id: `s-${position}`, position, answer,
    createdAt: '2026-08-07T10:00:00Z', updatedAt: '2026-08-07T10:00:00Z'
  });

  const chainOf = (n: number) => Array.from({ length: n }, (_, i) => step(i + 1));

  function setup(dir: 'ltr' | 'rtl' | null = 'ltr'): void {
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      declarations: [FiveWhysCascadeComponent],
      imports: [SharedModule],
      providers: [
        {
          provide: Directionality,
          useValue: dir ? { value: dir, change: new Subject<void>() } : null
        }
      ]
    });
    fixture = TestBed.createComponent(FiveWhysCascadeComponent);
    component = fixture.componentInstance;
  }

  /** `setInput` passe par le cycle de vie réel : c'est `ngOnChanges` qui trace. */
  function render(steps: readonly FiveWhysStep[] | undefined, rootCause: string | null = null): void {
    fixture.componentRef.setInput('steps', steps);
    fixture.componentRef.setInput('rootCause', rootCause);
    fixture.detectChanges();
  }

  const host = () => fixture.nativeElement as HTMLElement;
  const svg = () => host().querySelector('svg');
  const cards = () => host().querySelectorAll('.cascade__card');

  beforeEach(() => setup());

  // --- adaptation au nombre réel de pourquoi ------------------------------------

  it('ne dessine rien tant qu\'aucun pourquoi n\'est posé', () => {
    render([]);

    expect(svg()).toBeNull();
    expect(component.hasDiagram).toBeFalse();
  });

  it('trace exactement autant d\'encarts que de pourquoi — trois', () => {
    render(chainOf(3));

    expect(cards().length).toBe(3);
    expect(host().querySelectorAll('.cascade__link').length).toBe(2);
  });

  it('trace exactement autant d\'encarts que de pourquoi — sept', () => {
    render(chainOf(7));

    expect(cards().length).toBe(7);
    expect(host().querySelectorAll('.cascade__link').length).toBe(6);
  });

  it('redessine quand la chaîne s\'allonge', () => {
    render(chainOf(3));
    const largeurTrois = component.layout.width;

    render(chainOf(6));

    expect(cards().length).toBe(6);
    expect(component.layout.width).toBeGreaterThan(largeurTrois);
  });

  it('part du premier cran de teinte et finit au dernier, sans supposer cinq', () => {
    render(chainOf(4));

    const classes = Array.from(cards()).map(c => c.getAttribute('class') ?? '');
    expect(classes[0]).toContain('tone-0');
    expect(classes[classes.length - 1]).toContain('tone-3');
  });

  it('affiche la réponse et le rang de chaque pourquoi', () => {
    render([step(1, 'Le convoyeur a dérivé')]);

    const texte = host().textContent ?? '';
    expect(texte).toContain('Le convoyeur a dérivé');
    expect(texte).toContain('1');
  });

  // --- cause racine ---------------------------------------------------------------

  it('ferme la descente par un encart de cause racine quand elle est conclue', () => {
    render(chainOf(3), 'Presse mal réglée');

    expect(cards().length).toBe(4);
    expect(host().querySelectorAll('.is-root').length).toBe(1);
    expect(host().textContent).toContain('Presse mal réglée');
  });

  it('ne montre pas d\'encart terminal tant que rien n\'est conclu', () => {
    render(chainOf(3), null);

    expect(host().querySelectorAll('.is-root').length).toBe(0);
  });

  // --- accessibilité ---------------------------------------------------------------

  it('expose le dessin comme une image nommée et décrite', () => {
    render(chainOf(5));

    const el = svg()!;
    expect(el.getAttribute('role')).toBe('img');
    expect(el.getAttribute('aria-labelledby')).toBe(`${component.titleId} ${component.descId}`);
    expect(el.querySelector('title')!.getAttribute('id')).toBe(component.titleId);
    expect(el.querySelector('desc')!.getAttribute('id')).toBe(component.descId);
    // La description annonce la profondeur et renvoie à la liste, qui porte le
    // texte intégral : le dessin illustre, il ne remplace pas.
    expect(el.querySelector('desc')!.textContent).toContain('5');
    expect(el.querySelector('desc')!.textContent).toContain('liste');
  });

  it('dit dans sa description si la cause racine est conclue', () => {
    render(chainOf(3), 'Presse mal réglée');

    expect(host().querySelector('desc')!.textContent).toContain('racine');
  });

  it('retire le dessin du parcours de tabulation', () => {
    render(chainOf(3));

    // Certains moteurs rendent un `svg` focusable par défaut : ce serait un
    // arrêt clavier qui ne mène à rien, puisque tout est opérable dans la liste.
    expect(svg()!.getAttribute('focusable')).toBe('false');
  });

  it('donne des identifiants distincts à deux cascades de la même page', () => {
    const autre = TestBed.createComponent(FiveWhysCascadeComponent).componentInstance;

    expect(autre.titleId).not.toBe(component.titleId);
    expect(autre.descId).not.toBe(component.descId);
    expect(autre.arrowIdPrefix).not.toBe(component.arrowIdPrefix);
  });

  // --- sens de lecture ---------------------------------------------------------------

  it('descend vers la gauche en lecture de droite à gauche', () => {
    setup('rtl');
    render(chainOf(4));

    expect(component.isRtl).toBeTrue();
    const xs = component.layout.cards.map(c => c.x);
    expect(xs[3]).toBeLessThan(xs[0]);
    component.layout.cards.forEach(c => expect(c.anchor).toBe('end'));
  });

  it('descend vers la droite quand aucun service de direction n\'est fourni', () => {
    setup(null);
    render(chainOf(3));

    expect(component.isRtl).toBeFalse();
    expect(component.layout.cards[2].x).toBeGreaterThan(component.layout.cards[0].x);
  });

  // --- divers ------------------------------------------------------------------------

  it('suit les encarts par leur clé et non par leur rang', () => {
    // Un suivi par index redessinerait tout à chaque ajout de pourquoi.
    expect(component.trackByKey(0, { key: 's-2' })).toBe('s-2');
  });

  it('tolère une entrée absente sans planter le tracé', () => {
    render(undefined);

    expect(component.hasDiagram).toBeFalse();
    expect(svg()).toBeNull();
  });

  it('nomme la légende de chaque encart avec son rang', () => {
    expect(component.caption(4)).toContain('4');
    expect(component.rootCaption.length).toBeGreaterThan(0);
    expect(component.diagramTitle.length).toBeGreaterThan(0);
  });
});
