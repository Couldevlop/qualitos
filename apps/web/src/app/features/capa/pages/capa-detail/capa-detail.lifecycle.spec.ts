import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { ActivatedRoute, Router, convertToParamMap } from '@angular/router';
import { of, throwError } from 'rxjs';

import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { CapaService } from '../../capa.service';
import { CapaCaseResponse, CapaStatus } from '../../capa.types';
import { CapaDetailComponent } from './capa-detail.component';
import {
  CapaRevisionImpactComponent
} from '../capa-revision-impact/capa-revision-impact.component';
import { ProductsService } from '../../../products/products.service';

/**
 * Cycle de vie du dossier depuis la fiche (§4.2, ISO 9001 §10.2).
 *
 * Complète les deux autres fichiers de ce répertoire — preuves du dossier,
 * tableau des actions — sur ce qui restait sans filet : les transitions, la
 * vérification d'efficacité, la suppression, et le fait qu'un refus serveur ne
 * doit jamais laisser l'écran affirmer que le geste a eu lieu.
 */
describe('CapaDetailComponent — cycle de vie du dossier', () => {

  const CASE_ID = '66666666-6666-6666-6666-666666666666';

  let fixture: ComponentFixture<CapaDetailComponent>;
  let component: CapaDetailComponent;
  let capa: jasmine.SpyObj<CapaService>;
  let snack: jasmine.SpyObj<MatSnackBar>;
  let dialog: jasmine.SpyObj<MatDialog>;
  let router: jasmine.SpyObj<Router>;

  const dossier = (status: CapaStatus = 'OPEN'): CapaCaseResponse => ({
    id: CASE_ID, tenantId: 't1', title: 'Étiquetage manquant', type: 'CORRECTIVE',
    criticity: 'HIGH', status, sourceType: 'INTERNAL', ownerId: 'u1',
    createdAt: '2026-08-01T00:00:00Z', updatedAt: '2026-08-01T00:00:00Z', actions: []
  });

  function setup(status: CapaStatus = 'OPEN', routeId = CASE_ID): void {
    TestBed.overrideProvider(ActivatedRoute, {
      useValue: { snapshot: { paramMap: convertToParamMap({ id: routeId }) } }
    });
    capa.getCase.and.returnValue(of(dossier(status)));
    fixture = TestBed.createComponent(CapaDetailComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  beforeEach(async () => {
    capa = jasmine.createSpyObj<CapaService>('CapaService',
      ['getCase', 'listEvidences', 'uploadEvidence', 'deleteEvidence',
       'listActionEvidences', 'uploadActionEvidence', 'deleteActionEvidence',
       'updateAction', 'addAction', 'deleteCase', 'suggestActions',
       'verifyEffectiveness', 'startCase', 'resolveCase', 'rejectCase']);
    capa.listEvidences.and.returnValue(of([]));
    capa.listActionEvidences.and.returnValue(of([]));
    snack = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);
    dialog = jasmine.createSpyObj<MatDialog>('MatDialog', ['open']);
    dialog.open.and.returnValue({ afterClosed: () => of(true) } as never);
    router = jasmine.createSpyObj<Router>('Router', ['navigate']);

    TestBed.resetTestingModule();
    await TestBed.configureTestingModule({
      declarations: [CapaDetailComponent, CapaRevisionImpactComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        { provide: CapaService, useValue: capa },
        // L'encart d'impact vit dans la fiche : sans ce doublon, il irait
        // chercher un HttpClient que ce banc de test ne fournit pas.
        { provide: ProductsService, useValue: { revisionRequestsForTrigger: () => of([]) } },
        { provide: MatSnackBar, useValue: snack },
        { provide: MatDialog, useValue: dialog },
        { provide: Router, useValue: router },
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: convertToParamMap({ id: CASE_ID }) } } }
      ]
    }).compileComponents();
  });

  // --- garde d'entrée -------------------------------------------------------

  it('refuse un identifiant de route qui n\'en est pas un', () => {
    // Un préfixe suffisait autrefois : « capa-1/../../secrets » passait.
    setup('OPEN', 'capa-1/../../secrets');

    expect(capa.getCase).not.toHaveBeenCalled();
    expect(router.navigate).toHaveBeenCalledWith(['/capa']);
    expect(snack.open).toHaveBeenCalled();
  });

  it('accepte un identifiant de démonstration bien formé', () => {
    setup('OPEN', 'capa-1');

    expect(capa.getCase).toHaveBeenCalledWith('capa-1');
  });

  it('affiche un message quand le dossier est introuvable, sans page blanche muette', done => {
    TestBed.overrideProvider(ActivatedRoute, {
      useValue: { snapshot: { paramMap: convertToParamMap({ id: CASE_ID }) } }
    });
    capa.getCase.and.returnValue(throwError(() => new HttpErrorResponse({ status: 404 })));
    fixture = TestBed.createComponent(CapaDetailComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    component.error$.subscribe(msg => {
      if (msg) {
        expect(msg.length).toBeGreaterThan(0);
        done();
      }
    });
  });

  // --- transitions ----------------------------------------------------------

  it('enchaîne démarrage, résolution et rejet sur les bons appels', () => {
    setup('OPEN');
    capa.startCase.and.returnValue(of(dossier('IN_PROGRESS')));
    capa.resolveCase.and.returnValue(of(dossier('RESOLVED')));
    capa.rejectCase.and.returnValue(of(dossier('REJECTED')));

    component.start();
    component.resolve();
    component.reject();

    expect(capa.startCase).toHaveBeenCalledWith(CASE_ID);
    expect(capa.resolveCase).toHaveBeenCalledWith(CASE_ID);
    expect(capa.rejectCase).toHaveBeenCalledWith(CASE_ID);
    expect(component.acting$.value).toBeFalse();
  });

  it('dit pourquoi une transition a échoué au lieu de rester silencieuse', () => {
    setup('OPEN');
    capa.startCase.and.returnValue(throwError(() => new HttpErrorResponse({ status: 409 })));

    component.start();

    expect(snack.open).toHaveBeenCalled();
    expect(component.acting$.value).toBeFalse();
  });

  it('n\'enchaîne pas deux transitions concurrentes', () => {
    setup('OPEN');
    component.acting$.next(true);

    component.start();

    expect(capa.startCase).not.toHaveBeenCalled();
  });

  it('borne les transitions au statut qui les autorise', () => {
    setup('OPEN');

    expect(component.canStart('OPEN')).toBeTrue();
    expect(component.canStart('RESOLVED')).toBeFalse();
    expect(component.canResolve('IN_PROGRESS')).toBeTrue();
    expect(component.canReject('CLOSED')).toBeFalse();
    expect(component.isTerminal('REJECTED')).toBeTrue();
  });

  // --- vérification d'efficacité (ISO 9001 §10.2) ---------------------------

  it('clôture après confirmation d\'efficacité', () => {
    setup('RESOLVED');
    capa.verifyEffectiveness.and.returnValue(of(dossier('CLOSED')));

    component.verifyEffectiveness(true);

    expect(dialog.open).toHaveBeenCalled();
    expect(capa.verifyEffectiveness).toHaveBeenCalledWith(CASE_ID, true);
  });

  it('enregistre une non-efficacité comme un fait, pas comme un échec technique', () => {
    setup('RESOLVED');
    capa.verifyEffectiveness.and.returnValue(of(dossier('IN_PROGRESS')));

    component.verifyEffectiveness(false);

    expect(capa.verifyEffectiveness).toHaveBeenCalledWith(CASE_ID, false);
    expect(component.effectivenessLabel(false)).toContain('Non efficace');
    expect(component.effectivenessLabel(true)).toContain('Vérifiée');
  });

  it('ne vérifie rien si la confirmation est refusée', () => {
    setup('RESOLVED');
    dialog.open.and.returnValue({ afterClosed: () => of(false) } as never);

    component.verifyEffectiveness(true);

    expect(capa.verifyEffectiveness).not.toHaveBeenCalled();
  });

  it('remonte le refus du serveur sur la vérification', () => {
    setup('RESOLVED');
    capa.verifyEffectiveness.and.returnValue(
      throwError(() => new HttpErrorResponse({ status: 409 })));

    component.verifyEffectiveness(true);

    expect(snack.open).toHaveBeenCalled();
    expect(component.acting$.value).toBeFalse();
  });

  it('n\'ouvre la vérification que sur un dossier résolu', () => {
    setup('IN_PROGRESS');

    expect(component.canVerifyEffectiveness('RESOLVED')).toBeTrue();
    expect(component.canVerifyEffectiveness('IN_PROGRESS')).toBeFalse();

    component.acting$.next(true);
    component.verifyEffectiveness(true);
    expect(dialog.open).not.toHaveBeenCalled();
  });

  // --- suppression ----------------------------------------------------------

  it('supprime le dossier après confirmation et quitte la fiche', () => {
    setup('OPEN');
    capa.deleteCase.and.returnValue(of(void 0));

    component.deleteCase('Étiquetage manquant');

    expect(capa.deleteCase).toHaveBeenCalledWith(CASE_ID);
    expect(router.navigate).toHaveBeenCalledWith(['/capa']);
  });

  it('ne supprime rien sans confirmation', () => {
    setup('OPEN');
    dialog.open.and.returnValue({ afterClosed: () => of(false) } as never);

    component.deleteCase('Étiquetage manquant');

    expect(capa.deleteCase).not.toHaveBeenCalled();
  });

  it('reste sur la fiche quand la suppression échoue', () => {
    setup('OPEN');
    capa.deleteCase.and.returnValue(throwError(() => new HttpErrorResponse({ status: 409 })));

    component.deleteCase('Étiquetage manquant');

    expect(router.navigate).not.toHaveBeenCalled();
    expect(snack.open).toHaveBeenCalled();
  });

  // --- suggestions IA (§12) -------------------------------------------------

  it('affiche les actions suggérées sans rien persister', () => {
    setup('OPEN');
    capa.suggestActions.and.returnValue(of([{ title: 'Auditer le fournisseur' }]));

    component.suggestActions();

    expect(component.suggestions.length).toBe(1);
    expect(component.suggesting).toBeFalse();
    expect(capa.addAction).not.toHaveBeenCalled();
  });

  it('dit quand l\'IA ne rend rien d\'exploitable', () => {
    setup('OPEN');
    capa.suggestActions.and.returnValue(of([]));

    component.suggestActions();

    expect(snack.open).toHaveBeenCalled();
  });

  it('annonce l\'indisponibilité du service IA plutôt qu\'un silence', () => {
    setup('OPEN');
    capa.suggestActions.and.returnValue(
      throwError(() => new HttpErrorResponse({ status: 502 })));

    component.suggestActions();

    expect(component.suggesting).toBeFalse();
    expect(snack.open).toHaveBeenCalled();
  });

  it('ajoute une suggestion et la retire de la liste', () => {
    setup('OPEN');
    const suggestion = { title: 'Auditer le fournisseur' };
    capa.suggestActions.and.returnValue(of([suggestion]));
    capa.addAction.and.returnValue(of({
      id: 'a1', capaId: CASE_ID, title: suggestion.title, status: 'PENDING',
      actionType: 'CORRECTIVE'
    }));

    component.suggestActions();
    component.addSuggestion(component.suggestions[0]);

    expect(capa.addAction).toHaveBeenCalled();
    expect(component.suggestions.length).toBe(0);
    expect(component.addingKey).toBeNull();
  });

  it('garde la suggestion en liste quand son ajout échoue', () => {
    setup('OPEN');
    capa.suggestActions.and.returnValue(of([{ title: 'Auditer le fournisseur' }]));
    capa.addAction.and.returnValue(throwError(() => new HttpErrorResponse({ status: 409 })));

    component.suggestActions();
    component.addSuggestion(component.suggestions[0]);

    expect(component.suggestions.length).toBe(1);
    expect(snack.open).toHaveBeenCalled();
  });

  it('referme le bloc de suggestions à la demande', () => {
    setup('OPEN');
    capa.suggestActions.and.returnValue(of([{ title: 'Auditer le fournisseur' }]));

    component.suggestActions();
    component.dismissSuggestions();

    expect(component.suggestions).toEqual([]);
  });

  // --- dialogues et navigation ---------------------------------------------

  it('recharge la fiche après une édition confirmée', () => {
    setup('OPEN');
    capa.getCase.calls.reset();

    component.openEdit(dossier('OPEN'));

    expect(dialog.open).toHaveBeenCalled();
    expect(capa.getCase).toHaveBeenCalled();
  });

  it('recharge la fiche après l\'ajout d\'une action', () => {
    setup('OPEN');
    capa.getCase.calls.reset();

    component.openAddAction();

    expect(dialog.open).toHaveBeenCalled();
    expect(capa.getCase).toHaveBeenCalled();
  });

  it('ne recharge pas quand le dialogue est abandonné', () => {
    setup('OPEN');
    dialog.open.and.returnValue({ afterClosed: () => of(undefined) } as never);
    capa.getCase.calls.reset();

    component.openAddAction();

    expect(capa.getCase).not.toHaveBeenCalled();
  });

  it('revient à la liste', () => {
    setup('OPEN');

    component.goBack();

    expect(router.navigate).toHaveBeenCalledWith(['/capa']);
  });

  // --- avancement d'action --------------------------------------------------

  it('signale l\'échec d\'un avancement d\'action', () => {
    setup('IN_PROGRESS');
    capa.updateAction.and.returnValue(throwError(() => new HttpErrorResponse({ status: 409 })));

    component.advanceAction({
      id: 'a1', capaId: CASE_ID, title: 'x', status: 'PENDING', actionType: 'CORRECTIVE' });

    expect(snack.open).toHaveBeenCalled();
    expect(component.acting$.value).toBeFalse();
  });

  it('donne à chaque étape le verbe qui lui revient', () => {
    setup('IN_PROGRESS');

    expect(component.advanceActionLabel('PENDING')).toBe('Démarrer');
    expect(component.advanceActionLabel('IN_PROGRESS')).toBe('Terminer');
    expect(component.nextActionStatus('PENDING')).toBe('IN_PROGRESS');
  });

  // --- pièces du dossier : chemins non couverts ailleurs --------------------

  it('ne relance pas un dépôt tant que le précédent n\'a pas rendu la main', () => {
    setup('OPEN');
    component.uploadingEvidence$.next(true);
    const input = document.createElement('input');
    spyOn(input, 'click');

    component.triggerEvidencePicker(input);

    expect(input.click).not.toHaveBeenCalled();
  });

  it('n\'enchaîne pas deux retraits de preuve du dossier', () => {
    setup('OPEN');
    component.removingEvidenceId$.next('evd-1');

    component.removeEvidence({
      id: 'evd-2', capaId: CASE_ID, contentType: 'application/pdf',
      sizeBytes: 10, createdAt: '2026-08-01T00:00:00Z'
    });

    expect(dialog.open).not.toHaveBeenCalled();
  });

  // --- présentation ---------------------------------------------------------

  it('traduit les tailles en unités lisibles', () => {
    setup('OPEN');

    expect(component.formatSize(512)).toBe('512 o');
    expect(component.formatSize(2048)).toBe('2 Ko');
    expect(component.formatSize(3 * 1024 * 1024)).toBe('3,0 Mo');
  });

  it('donne à chaque famille de document son icône', () => {
    setup('OPEN');

    expect(component.evidenceIcon('application/pdf')).toBe('picture_as_pdf');
    expect(component.evidenceIcon('image/png')).toBe('image');
    expect(component.evidenceIcon(
      'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet')).toBe('table_chart');
    expect(component.evidenceIcon(
      'application/vnd.openxmlformats-officedocument.wordprocessingml.document')).toBe('description');
    expect(component.evidenceIcon('application/octet-stream')).toBe('attach_file');
  });

  it('dérive les classes de badge du statut et de la criticité', () => {
    setup('OPEN');

    expect(component.statusBadge('IN_PROGRESS')).toBe('badge badge-in_progress');
    expect(component.criticityBadge('CRITICAL')).toBe('crit crit-critical');
    expect(component.actionBadge('DONE')).toBe('badge badge-done');
  });

  it('compte les preuves du dossier face à leur borne', () => {
    setup('OPEN');

    expect(component.evidenceCountLabel()).toBe('0 / 10');
  });
});
