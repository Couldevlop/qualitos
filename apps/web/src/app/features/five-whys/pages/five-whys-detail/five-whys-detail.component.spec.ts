import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { ActivatedRoute, Router, convertToParamMap } from '@angular/router';
import { of, throwError } from 'rxjs';

import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { FiveWhysService } from '../../five-whys.service';
import { FiveWhysAnalysis, FiveWhysStep } from '../../five-whys.types';
import { FiveWhysDetailComponent } from './five-whys-detail.component';

/**
 * Déroulé d'une analyse des 5 Pourquoi.
 *
 * <p>L'écran ne décore pas la méthode, il la tient. Trois règles s'y voient : on
 * ne conclut pas la cause racine avant le troisième pourquoi, la chaîne s'arrête
 * à sept, et seul le dernier maillon peut être retiré. Le serveur les applique
 * de son côté ; ce qui se teste ici, c'est qu'elles soient ÉNONCÉES à l'écran au
 * lieu d'être découvertes par un refus — une règle qu'on ne rencontre qu'en la
 * heurtant passe pour un bug.
 */
describe('FiveWhysDetailComponent', () => {

  let fixture: ComponentFixture<FiveWhysDetailComponent>;
  let component: FiveWhysDetailComponent;
  let service: jasmine.SpyObj<FiveWhysService>;
  let router: jasmine.SpyObj<Router>;
  let snack: jasmine.SpyObj<MatSnackBar>;

  const step = (position: number, answer = `Réponse ${position}`): FiveWhysStep => ({
    id: `s-${position}`, position, answer,
    createdAt: '2026-08-06T10:00:00Z', updatedAt: '2026-08-06T10:00:00Z'
  });

  const analysis = (steps: FiveWhysStep[], rootCause: string | null = null): FiveWhysAnalysis => ({
    id: 'a-1',
    ncId: 'nc-1',
    ncReference: 'NC-2026-014',
    problem: 'Arrêt de ligne récurrent',
    rootCause,
    steps,
    createdAt: '2026-08-06T10:00:00Z',
    updatedAt: '2026-08-06T10:00:00Z'
  });

  function setup(loaded: FiveWhysAnalysis | null, failLoad = false): void {
    service.get.and.returnValue(
      failLoad ? throwError(() => new Error('404')) : of(loaded as FiveWhysAnalysis));

    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      declarations: [FiveWhysDetailComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        { provide: FiveWhysService, useValue: service },
        { provide: Router, useValue: router },
        { provide: MatSnackBar, useValue: snack },
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: convertToParamMap({ id: 'a-1' }) } } }
      ]
    });
    fixture = TestBed.createComponent(FiveWhysDetailComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  function texte(): string {
    return (fixture.nativeElement as HTMLElement).textContent ?? '';
  }

  beforeEach(() => {
    service = jasmine.createSpyObj<FiveWhysService>('FiveWhysService',
      ['get', 'addStep', 'updateStep', 'deleteStep', 'setRootCause']);
    router = jasmine.createSpyObj<Router>('Router', ['navigate']);
    snack = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);
  });

  // --- chargement -------------------------------------------------------------

  it('charge l\'analyse désignée par la route et pré-remplit la conclusion existante', () => {
    setup(analysis([step(1), step(2), step(3)], 'Presse mal réglée'));

    expect(service.get).toHaveBeenCalledWith('a-1');
    expect(component.rootCauseDraft).toBe('Presse mal réglée');
    expect(component.loading).toBeFalse();
  });

  it('renvoie à la liste quand l\'analyse est introuvable', () => {
    setup(null, true);

    expect(snack.open).toHaveBeenCalled();
    expect(router.navigate).toHaveBeenCalledWith(['/five-whys']);
    expect(component.loading).toBeFalse();
  });

  // --- règle : pas de conclusion avant le 3e pourquoi ---------------------------

  it('verrouille la conclusion sous trois pourquoi, EN DISANT pourquoi', () => {
    setup(analysis([step(1), step(2)]));

    expect(component.canConcludeRootCause).toBeFalse();
    expect(texte()).toContain('symptôme');
    // Le champ n'est pas seulement désactivé : il n'est pas proposé.
    expect((fixture.nativeElement as HTMLElement)
      .querySelector('.root-cause__field')).toBeNull();
  });

  it('ouvre la conclusion dès le troisième pourquoi', () => {
    setup(analysis([step(1), step(2), step(3)]));

    expect(component.canConcludeRootCause).toBeTrue();
    expect((fixture.nativeElement as HTMLElement)
      .querySelector('.root-cause__field')).not.toBeNull();
  });

  it('refuse de conclure tant que la chaîne est trop courte, même appelé de force', () => {
    setup(analysis([step(1), step(2)]));
    component.rootCauseDraft = 'Un symptôme déguisé';

    component.concludeRootCause();

    expect(service.setRootCause).not.toHaveBeenCalled();
  });

  it('garde la réponse du serveur après conclusion plutôt que son propre état', () => {
    setup(analysis([step(1), step(2), step(3)]));
    const conclu = analysis([step(1), step(2), step(3), step(4)], 'Presse mal réglée');
    service.setRootCause.and.returnValue(of(conclu));
    component.rootCauseDraft = '  Presse mal réglée  ';

    component.concludeRootCause();

    expect(service.setRootCause).toHaveBeenCalledWith('a-1', 'Presse mal réglée');
    // Le serveur peut avoir plus à jour que nous : c'est sa version qui fait foi.
    expect(component.analysis).toBe(conclu);
    expect(component.steps.length).toBe(4);
    expect(component.saving).toBeFalse();
  });

  it('signale une conclusion refusée sans laisser le bouton bloqué', () => {
    setup(analysis([step(1), step(2), step(3)]));
    service.setRootCause.and.returnValue(throwError(() => new Error('409')));
    component.rootCauseDraft = 'Cause racine';

    component.concludeRootCause();

    expect(snack.open).toHaveBeenCalled();
    expect(component.saving).toBeFalse();
  });

  // --- règle : la chaîne s'arrête à sept ---------------------------------------

  it('propose le pourquoi suivant avec son rang', () => {
    setup(analysis([step(1), step(2)]));

    expect(component.depth).toBe(2);
    expect(component.nextQuestion).toContain('(3)');
  });

  it('retire le bloc d\'ajout au septième pourquoi et énonce la limite', () => {
    setup(analysis([1, 2, 3, 4, 5, 6, 7].map(n => step(n))));

    expect(component.canAddWhy).toBeFalse();
    expect((fixture.nativeElement as HTMLElement).querySelector('.add-why')).toBeNull();
    expect(texte()).toContain('Sept pourquoi');
  });

  it('n\'ajoute rien au-delà de la limite, même appelé de force', () => {
    setup(analysis([1, 2, 3, 4, 5, 6, 7].map(n => step(n))));
    component.newAnswer = 'Un huitième pourquoi';

    component.addWhy();

    expect(service.addStep).not.toHaveBeenCalled();
  });

  it('ajoute un pourquoi à la suite et vide la saisie', () => {
    setup(analysis([step(1)]));
    service.addStep.and.returnValue(of(step(2, 'Le convoyeur a dérivé')));
    component.newAnswer = '  Le convoyeur a dérivé  ';

    component.addWhy();

    expect(service.addStep).toHaveBeenCalledWith('a-1', 'Le convoyeur a dérivé');
    expect(component.steps.map(s => s.position)).toEqual([1, 2]);
    expect(component.newAnswer).toBe('');
    expect(component.saving).toBeFalse();
  });

  it('ignore une réponse vide', () => {
    setup(analysis([step(1)]));
    component.newAnswer = '   ';

    component.addWhy();

    expect(service.addStep).not.toHaveBeenCalled();
  });

  it('signale un ajout refusé par le serveur', () => {
    setup(analysis([step(1)]));
    service.addStep.and.returnValue(throwError(() => new Error('409')));
    component.newAnswer = 'Une réponse';

    component.addWhy();

    expect(snack.open).toHaveBeenCalled();
    expect(component.saving).toBeFalse();
  });

  // --- règle : seul le dernier maillon peut partir ------------------------------

  it('ne propose le retrait que sur le dernier pourquoi', () => {
    setup(analysis([step(1), step(2), step(3)]));

    const retraits = (fixture.nativeElement as HTMLElement)
      .querySelectorAll('button[aria-label^="Retirer le pourquoi"]');
    expect(retraits.length).toBe(1);
    expect(retraits[0].getAttribute('aria-label')).toContain('3');
  });

  it('retire le dernier maillon et laisse la chaîne continue', () => {
    setup(analysis([step(1), step(2), step(3)]));
    service.deleteStep.and.returnValue(of(void 0));

    component.removeLastWhy();

    expect(service.deleteStep).toHaveBeenCalledWith('s-3');
    expect(component.steps.map(s => s.position)).toEqual([1, 2]);
  });

  it('ne tente rien quand il n\'y a aucun maillon à retirer', () => {
    setup(analysis([]));

    component.removeLastWhy();

    expect(service.deleteStep).not.toHaveBeenCalled();
  });

  it('signale un retrait refusé et garde la chaîne intacte', () => {
    setup(analysis([step(1), step(2)]));
    service.deleteStep.and.returnValue(throwError(() => new Error('409')));

    component.removeLastWhy();

    expect(snack.open).toHaveBeenCalled();
    expect(component.steps.length).toBe(2);
  });

  // --- correction d'un pourquoi -------------------------------------------------

  it('ouvre la correction sur le maillon visé, une seule à la fois', () => {
    setup(analysis([step(1), step(2)]));

    component.startEdit('s-2', 'Réponse 2');
    fixture.detectChanges();

    expect(component.editingStepId).toBe('s-2');
    expect(component.editAnswer).toBe('Réponse 2');
    expect((fixture.nativeElement as HTMLElement)
      .querySelectorAll('.why__input').length).toBe(1);
  });

  it('enregistre la correction et remplace le maillon en place', () => {
    setup(analysis([step(1), step(2)]));
    service.updateStep.and.returnValue(of(step(2, 'Formulation corrigée')));
    component.startEdit('s-2', 'Réponse 2');
    component.editAnswer = 'Formulation corrigée';

    component.commitEdit('s-2', 'Réponse 2');

    expect(service.updateStep).toHaveBeenCalledWith('s-2', 'Formulation corrigée');
    expect(component.steps[1].answer).toBe('Formulation corrigée');
    expect(component.editingStepId).toBeNull();
  });

  it('n\'appelle pas le serveur quand la réponse n\'a pas bougé', () => {
    // Un clic à côté ne doit pas produire un aller-retour ni une trace d'audit.
    setup(analysis([step(1)]));
    component.startEdit('s-1', 'Réponse 1');
    component.editAnswer = 'Réponse 1';

    component.commitEdit('s-1', 'Réponse 1');

    expect(service.updateStep).not.toHaveBeenCalled();
    expect(component.editingStepId).toBeNull();
  });

  it('refuse une correction vide en le disant', () => {
    setup(analysis([step(1)]));
    component.startEdit('s-1', 'Réponse 1');
    component.editAnswer = '   ';

    component.commitEdit('s-1', 'Réponse 1');

    expect(service.updateStep).not.toHaveBeenCalled();
    expect(snack.open).toHaveBeenCalled();
    // La correction reste ouverte : on ne perd pas ce qui était en cours.
    expect(component.editingStepId).toBe('s-1');
  });

  it('referme la correction et signale l\'échec quand le serveur refuse', () => {
    setup(analysis([step(1)]));
    service.updateStep.and.returnValue(throwError(() => new Error('409')));
    component.startEdit('s-1', 'Réponse 1');
    component.editAnswer = 'Autre formulation';

    component.commitEdit('s-1', 'Réponse 1');

    expect(snack.open).toHaveBeenCalled();
    expect(component.editingStepId).toBeNull();
  });

  it('annule une correction sans rien changer', () => {
    setup(analysis([step(1)]));
    component.startEdit('s-1', 'Réponse 1');

    component.cancelEdit();

    expect(component.editingStepId).toBeNull();
    expect(component.editAnswer).toBe('');
    expect(service.updateStep).not.toHaveBeenCalled();
  });

  it('revient à la liste', () => {
    setup(analysis([step(1)]));

    component.goBack();

    expect(router.navigate).toHaveBeenCalledWith(['/five-whys']);
  });

  it('renvoie à l\'écart constaté d\'où part l\'analyse', () => {
    // Sans ce retour, l'utilisateur venu de la non-conformité devrait repasser
    // par la liste des analyses pour retrouver son point de départ.
    setup(analysis([step(1)]));

    expect((fixture.nativeElement as HTMLElement).querySelector('.open-nc')).not.toBeNull();

    component.openNc();

    expect(router.navigate).toHaveBeenCalledWith(['/nc', 'nc-1']);
  });

  it('ne propose pas le retour à la NC tant que rien n\'est chargé', () => {
    setup(null, true);

    component.openNc();

    expect(router.navigate).not.toHaveBeenCalledWith(['/nc', jasmine.anything()]);
  });
});
