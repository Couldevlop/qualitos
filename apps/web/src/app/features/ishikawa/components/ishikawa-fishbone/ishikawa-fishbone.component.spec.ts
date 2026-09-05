import { Directionality } from '@angular/cdk/bidi';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Subject } from 'rxjs';

import { SharedModule } from '../../../../shared/shared.module';
import { IshikawaFishboneComponent } from './ishikawa-fishbone.component';
import { FishboneBranchInput } from './ishikawa-fishbone.layout';

/**
 * Le test ne juge pas l'allure de l'arête — aucun assert DOM ne dira qu'elle
 * ressemble à un poisson. Il vérifie qu'elle rend compte de la configuration
 * réelle du diagramme (6M, 7M, 8M), qu'elle avoue les branches vides, et
 * qu'elle reste annoncée à qui ne la voit pas.
 */
describe('IshikawaFishboneComponent', () => {

  let fixture: ComponentFixture<IshikawaFishboneComponent>;
  let component: IshikawaFishboneComponent;

  const branch = (n: number, causes = 2, descendants = 0): FishboneBranchInput => ({
    key: `b-${n}`,
    label: `Branche ${n}`,
    causes: Array.from({ length: causes }, (_, i) => ({
      id: `c-${n}-${i}`, label: `Cause ${n}.${i}`, descendants
    }))
  });

  const branchesOf = (n: number, causes = 2) =>
    Array.from({ length: n }, (_, i) => branch(i + 1, causes));

  function setup(dir: 'ltr' | 'rtl' | null = 'ltr'): void {
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      declarations: [IshikawaFishboneComponent],
      imports: [SharedModule],
      providers: [
        {
          provide: Directionality,
          useValue: dir ? { value: dir, change: new Subject<void>() } : null
        }
      ]
    });
    fixture = TestBed.createComponent(IshikawaFishboneComponent);
    component = fixture.componentInstance;
  }

  /** `setInput` passe par le cycle de vie réel : c'est `ngOnChanges` qui trace. */
  function render(problem: string, branches: readonly FishboneBranchInput[] | undefined): void {
    fixture.componentRef.setInput('problem', problem);
    fixture.componentRef.setInput('branches', branches);
    fixture.detectChanges();
  }

  const host = () => fixture.nativeElement as HTMLElement;
  const svg = () => host().querySelector('svg');
  const bones = () => host().querySelectorAll('.fishbone__bone');

  beforeEach(() => setup());

  // --- le nombre de catégories n'est pas figé ------------------------------------

  it('ne dessine rien sans aucune catégorie', () => {
    render('Rebuts en hausse', []);

    expect(svg()).toBeNull();
    expect(component.hasDiagram).toBeFalse();
  });

  it('trace six branches en 6M', () => {
    render('Rebuts en hausse', branchesOf(6));

    expect(bones().length).toBe(6);
  });

  it('trace sept branches en 7M', () => {
    render('Rebuts en hausse', branchesOf(7));

    expect(bones().length).toBe(7);
  });

  it('trace huit branches en 8M et élargit le dessin en conséquence', () => {
    render('Rebuts en hausse', branchesOf(6));
    const largeurSix = component.layout.width;

    render('Rebuts en hausse', branchesOf(8));

    expect(bones().length).toBe(8);
    expect(component.layout.width).toBeGreaterThan(largeurSix);
  });

  it('alterne les branches de part et d\'autre de l\'épine', () => {
    render('P', branchesOf(6));

    expect(host().querySelectorAll('.side-top').length).toBe(3);
    expect(host().querySelectorAll('.side-bottom').length).toBe(3);
  });

  // --- causes -----------------------------------------------------------------------

  it('accroche un trait par cause de premier niveau', () => {
    render('P', [branch(1, 3), branch(2, 1)]);

    expect(host().querySelectorAll('.fishbone__tick').length).toBe(4);
  });

  it('affiche les libellés des causes et de leurs catégories', () => {
    render('P', [branch(1, 1)]);

    const texte = host().textContent ?? '';
    expect(texte).toContain('Branche 1');
    expect(texte).toContain('Cause 1.0');
  });

  it('signale les sous-causes par un compteur, sans les tracer', () => {
    render('P', [branch(1, 1, 3)]);

    // Trois niveaux d'obliques deviennent illisibles : la hiérarchie complète
    // reste dépliée dans les cartes de branche sous le diagramme.
    expect(host().querySelectorAll('.fishbone__sub').length).toBe(1);
    expect(host().querySelector('.fishbone__sub')!.textContent).toContain('+3');
    expect(host().querySelectorAll('.fishbone__tick').length).toBe(1);
  });

  it('n\'ajoute pas de compteur à une cause feuille', () => {
    render('P', [branch(1, 2, 0)]);

    expect(host().querySelectorAll('.fishbone__sub').length).toBe(0);
  });

  it('marque une branche encore vide au lieu de la masquer', () => {
    render('P', [branch(1, 0), branch(2, 2)]);

    expect(host().querySelectorAll('.is-empty').length).toBe(1);
    expect(bones().length).toBe(2);
  });

  // --- tête -----------------------------------------------------------------------

  it('porte l\'énoncé du problème dans la tête', () => {
    render('Taux de rebut en hausse', branchesOf(6));

    const tete = host().querySelector('.fishbone__head')!;
    expect(tete.textContent).toContain('Taux de rebut');
  });

  // --- accessibilité ------------------------------------------------------------------

  it('expose le dessin comme un groupe nommé et décrit', () => {
    // `group` et NON `img` : une image est un tout opaque pour les technologies
    // d'assistance, qui en ignorent le contenu. Les causes étant devenues des
    // boutons, `img` les rendrait inatteignables au clavier tout en les laissant
    // cliquables — le pire des deux.
    render('Rebuts en hausse', branchesOf(7));

    const el = svg()!;
    expect(el.getAttribute('role')).toBe('group');
    expect(el.getAttribute('aria-labelledby')).toBe(`${component.titleId} ${component.descId}`);
    expect(el.querySelector('title')!.getAttribute('id')).toBe(component.titleId);

    const desc = el.querySelector('desc')!.textContent ?? '';
    expect(desc).toContain('7');        // familles de causes
    expect(desc).toContain('14');       // causes de premier niveau
    expect(desc).toContain('sous-pourquoi');   // ce qu'une cause activée permet
  });

  it('laisse le focus entrer dans le dessin', () => {
    // `focusable="false"` a été retiré : il empêchait le clavier d'atteindre les
    // causes, qui sont désormais le seul moyen d'ajouter un sous-pourquoi.
    render('P', branchesOf(6));

    expect(svg()!.getAttribute('focusable')).toBeNull();
  });

  // --- une cause s'active ---------------------------------------------------------------

  it('rend chaque cause focusable et actionnable', () => {
    render('P', branchesOf(6, 1));

    const causes = host().querySelectorAll('.fishbone__cause-group');
    expect(causes.length).toBe(6);
    expect(causes[0].getAttribute('role')).toBe('button');
    expect(causes[0].getAttribute('tabindex')).toBe('0');
  });

  it('annonce le libellé ENTIER de la cause, là où le dessin le tronque', () => {
    // Une cause coupée au milieu d'un mot ne s'identifie pas — et c'est
    // précisément sur elle qu'on va cliquer.
    render('P', [{
      key: 'b-1', label: 'Méthodes',
      causes: [{
        id: 'c-1', descendants: 0,
        label: 'Gamme de fabrication obsolète depuis la révision de mars'
      }]
    }]);

    const nom = host().querySelector('.fishbone__cause-group')!.getAttribute('aria-label') ?? '';
    expect(nom).toContain('Gamme de fabrication obsolète depuis la révision de mars');
  });

  it('émet l’identifiant de la cause activée, sans décider de la suite', () => {
    // Le diagramme ne connaît ni le dialogue ni l'arbre complet : il dit
    // seulement LAQUELLE. C'est ce qui le garde réutilisable ailleurs.
    render('P', branchesOf(1, 1));
    const emis: string[] = [];
    component.causeActivate.subscribe(id => emis.push(id));

    const cause = host().querySelector('.fishbone__cause-group') as HTMLElement;
    cause.dispatchEvent(new MouseEvent('click', { bubbles: true }));
    cause.dispatchEvent(new KeyboardEvent('keydown', { key: 'Enter', bubbles: true }));
    cause.dispatchEvent(new KeyboardEvent('keydown', { key: ' ', bubbles: true }));

    // Clic, « Entrée » et barre d'espace : un bouton répond aux trois.
    expect(emis.length).toBe(3);
    expect(emis.every(id => id === emis[0])).toBeTrue();
  });

  // --- le score se lit sur l'arête ------------------------------------------------------

  it('écrit le score de cause racine sur le dessin, avec sa bande de lecture', () => {
    // Depuis le retrait des cartes de branche, l'arête est le seul endroit où
    // on lit le score : une cause pesée à 80 % ne doit pas se confondre avec une
    // hypothèse de passage.
    render('P', [{
      key: 'b-1', label: 'Méthodes',
      causes: [
        { id: 'c-1', label: 'Forte', descendants: 0, score: 0.8 },
        { id: 'c-2', label: 'Moyenne', descendants: 0, score: 0.5 },
        { id: 'c-3', label: 'Faible', descendants: 0, score: 0.1 }
      ]
    }]);

    const scores = host().querySelectorAll('.fishbone__score');
    expect(scores.length).toBe(3);
    expect(scores[0].getAttribute('class')).toContain('fishbone__score--high');
    expect(scores[1].getAttribute('class')).toContain('fishbone__score--mid');
    expect(scores[2].getAttribute('class')).toContain('fishbone__score--low');
  });

  it('n’invente pas de score là où il n’a pas été renseigné', () => {
    // Un « 0 % » affiché par défaut se lirait comme une cause écartée.
    render('P', branchesOf(1, 1));

    expect(host().querySelectorAll('.fishbone__score').length).toBe(0);
  });

  it('donne des identifiants distincts à deux arêtes de la même page', () => {
    const autre = TestBed.createComponent(IshikawaFishboneComponent).componentInstance;

    expect(autre.titleId).not.toBe(component.titleId);
    expect(autre.descId).not.toBe(component.descId);
    expect(autre.arrowId).not.toBe(component.arrowId);
  });

  // --- sens de lecture -----------------------------------------------------------------

  it('bascule la tête à gauche en lecture de droite à gauche', () => {
    setup('rtl');
    render('Rebuts en hausse', branchesOf(6));

    expect(component.isRtl).toBeTrue();
    expect(component.layout.head.x).toBeLessThan(component.layout.spineX1);
    expect(component.layout.head.anchor).toBe('end');
  });

  it('laisse la tête à droite quand aucun service de direction n\'est fourni', () => {
    setup(null);
    render('Rebuts en hausse', branchesOf(6));

    expect(component.isRtl).toBeFalse();
    expect(component.layout.head.x).toBeGreaterThan(component.layout.spineX1);
  });

  // --- divers --------------------------------------------------------------------------

  it('tolère une entrée absente sans planter le tracé', () => {
    render('P', undefined);

    expect(component.hasDiagram).toBeFalse();
    expect(svg()).toBeNull();
  });

  it('suit branches et causes par leur clé', () => {
    expect(component.trackByKey(0, { key: 'METHODS' })).toBe('METHODS');
    expect(component.subCountLabel(4)).toBe('+4');
    expect(component.diagramTitle.length).toBeGreaterThan(0);
  });
});
