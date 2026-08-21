import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { ActivatedRoute, Router, convertToParamMap } from '@angular/router';
import { of } from 'rxjs';

import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { CapaService } from '../../capa.service';
import { CapaActionResponse, CapaCaseResponse, ClosureBlocker } from '../../capa.types';
import { CapaDetailComponent } from './capa-detail.component';
import {
  CapaRevisionImpactComponent
} from '../capa-revision-impact/capa-revision-impact.component';
import { ProductsService } from '../../../products/products.service';

/**
 * Ce qui s'oppose à la clôture, DIT avant le clic (§4.2).
 *
 * Auparavant, le refus n'arrivait qu'après : l'utilisateur cliquait sur
 * « Efficace — clôturer », recevait un 409 portant une phrase anglaise, et
 * devait instruire lui-même ce qu'il lui restait à faire. Ces tests portent sur
 * la promesse inverse — l'écran énonce l'obstacle avant qu'on s'y heurte.
 */
describe('CapaDetailComponent — motifs de blocage de la clôture', () => {

  const CASE_ID = '77777777-7777-7777-7777-777777777777';

  let fixture: ComponentFixture<CapaDetailComponent>;
  let component: CapaDetailComponent;
  let capa: jasmine.SpyObj<CapaService>;

  const action = (over: Partial<CapaActionResponse> = {}): CapaActionResponse => ({
    id: 'a1', capaId: CASE_ID, title: 'Trier le lot', status: 'DONE',
    actionType: 'CONTAINMENT', ...over
  });

  const dossier = (blockers?: ClosureBlocker[]): CapaCaseResponse => ({
    id: CASE_ID, tenantId: 't1', title: 'Étiquetage manquant', type: 'CORRECTIVE',
    criticity: 'HIGH', status: 'RESOLVED', sourceType: 'INTERNAL', ownerId: 'u1',
    createdAt: '2026-08-01T00:00:00Z', updatedAt: '2026-08-01T00:00:00Z',
    actions: [action()], closureBlockers: blockers
  });

  function setup(blockers?: ClosureBlocker[]): void {
    capa.getCase.and.returnValue(of(dossier(blockers)));
    fixture = TestBed.createComponent(CapaDetailComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  /** Le bouton qui promet la clôture — celui dont l'état est en jeu. */
  function closeButton(): HTMLButtonElement | null {
    const buttons = Array.from(
      (fixture.nativeElement as HTMLElement).querySelectorAll('.effectiveness-actions button'));
    return (buttons.find(b => (b.textContent ?? '').includes('clôturer')) as HTMLButtonElement) ?? null;
  }

  beforeEach(async () => {
    capa = jasmine.createSpyObj<CapaService>('CapaService',
      ['getCase', 'listEvidences', 'uploadEvidence', 'deleteEvidence',
       'listActionEvidences', 'uploadActionEvidence', 'deleteActionEvidence',
       'updateAction', 'addAction', 'deleteCase', 'suggestActions',
       'verifyEffectiveness', 'startCase', 'resolveCase', 'rejectCase']);
    capa.listEvidences.and.returnValue(of([]));
    capa.listActionEvidences.and.returnValue(of([]));

    TestBed.resetTestingModule();
    await TestBed.configureTestingModule({
      declarations: [CapaDetailComponent, CapaRevisionImpactComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        { provide: CapaService, useValue: capa },
        // L'encart d'impact vit dans la fiche : sans ce doublon, il irait
        // chercher un HttpClient que ce banc de test ne fournit pas.
        { provide: ProductsService, useValue: { revisionRequestsForTrigger: () => of([]) } },
        { provide: MatSnackBar, useValue: jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']) },
        { provide: MatDialog, useValue: jasmine.createSpyObj<MatDialog>('MatDialog', ['open']) },
        { provide: Router, useValue: jasmine.createSpyObj<Router>('Router', ['navigate']) },
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: convertToParamMap({ id: CASE_ID }) } } }
      ]
    }).compileComponents();
  });

  // --- affichage ------------------------------------------------------------

  it('énonce les obstacles avant le clic plutôt qu\'après le refus', () => {
    setup([{ code: 'CONTAINMENT_ONLY', count: 2 }]);

    const banner = (fixture.nativeElement as HTMLElement).querySelector('.blockers-block');
    expect(banner).not.toBeNull();
    expect(banner!.textContent).toContain('endiguement');
  });

  it('liste TOUS les obstacles, pas seulement le premier', () => {
    // N'en montrer qu'un enverrait l'utilisateur le corriger pour en découvrir
    // un autre — deux allers-retours là où un seul écran suffit.
    setup([
      { code: 'ACTIONS_NOT_DONE', count: 2 },
      { code: 'OPEN_NON_CONFORMITIES', count: 1 }
    ]);

    expect((fixture.nativeElement as HTMLElement)
      .querySelectorAll('.blockers-list li').length).toBe(2);
  });

  it('n\'affiche aucun bandeau quand rien ne s\'oppose à la clôture', () => {
    setup([]);

    expect((fixture.nativeElement as HTMLElement).querySelector('.blockers-block')).toBeNull();
  });

  it('n\'affiche aucun bandeau quand le serveur n\'a rien calculé', () => {
    // `undefined` (liste paginée) doit se comporter comme « pas d'information »,
    // jamais comme « obstacle inconnu ».
    setup(undefined);

    expect((fixture.nativeElement as HTMLElement).querySelector('.blockers-block')).toBeNull();
  });

  // --- effet sur le bouton --------------------------------------------------

  it('éteint le bouton de clôture tant qu\'un obstacle demeure', () => {
    setup([{ code: 'ACTIONS_NOT_DONE', count: 1 }]);

    expect(closeButton()?.disabled).toBeTrue();
  });

  it('allume le bouton quand la liste d\'obstacles est vide', () => {
    setup([]);

    expect(closeButton()?.disabled).toBeFalse();
  });

  it('laisse « Non efficace » accessible même bloqué', () => {
    // Constater un échec ne clôt rien : l'interdire empêcherait de consigner que
    // les actions n'ont pas produit leur effet.
    setup([{ code: 'CONTAINMENT_ONLY', count: 1 }]);

    const buttons = Array.from((fixture.nativeElement as HTMLElement)
      .querySelectorAll('.effectiveness-actions button')) as HTMLButtonElement[];
    const notEffective = buttons.find(b => (b.textContent ?? '').includes('Non efficace'));
    expect(notEffective?.disabled).toBeFalse();
  });

  // --- libellés -------------------------------------------------------------

  it('accorde le singulier et le pluriel sur le décompte', () => {
    setup([]);

    expect(component.blockerLabel({ code: 'ACTIONS_NOT_DONE', count: 1 })).toContain('1 action');
    expect(component.blockerLabel({ code: 'ACTIONS_NOT_DONE', count: 4 })).toContain('4 actions');
    expect(component.blockerLabel({ code: 'OPEN_NON_CONFORMITIES', count: 1 }))
      .toContain('1 non-conformité liée');
    expect(component.blockerLabel({ code: 'OPEN_NON_CONFORMITIES', count: 3 }))
      .toContain('3 non-conformités');
  });

  it('reste compréhensible face à un code que l\'écran ne connaît pas', () => {
    // Serveur plus récent que le bundle : mieux vaut une phrase générique qu'un
    // bouton actif dont on ignore pourquoi il échouera.
    setup([]);

    const label = component.blockerLabel({ code: 'PAS_UN_CODE' as never, count: 0 });
    expect(label).toBeTruthy();
    expect(label).not.toContain('PAS_UN_CODE');
  });

  it('résume tous les motifs dans l\'infobulle du bouton éteint', () => {
    setup([{ code: 'NO_ACTION', count: 0 }, { code: 'OPEN_NON_CONFORMITIES', count: 2 }]);

    const tip = component.closureTooltip(dossier([
      { code: 'NO_ACTION', count: 0 }, { code: 'OPEN_NON_CONFORMITIES', count: 2 }
    ]));
    expect(tip).toContain('Aucune action');
    expect(tip).toContain('2 non-conformités');
  });

  it('n\'attache aucune infobulle quand rien ne bloque', () => {
    // Une infobulle qui répète le libellé du bouton n'apprend rien et gêne le survol.
    setup([]);

    expect(component.closureTooltip(dossier([]))).toBe('');
  });

  // --- nature des actions ---------------------------------------------------

  it('affiche la nature de chaque action et distingue l\'endiguement', () => {
    setup([]);

    const cell = (fixture.nativeElement as HTMLElement).querySelector('.action-type');
    expect(cell).not.toBeNull();
    expect(cell!.textContent!.trim()).toBe('Endiguement');
    expect(cell!.classList).toContain('action-type--containment');
  });

  it('retombe sur le code brut pour une nature inconnue', () => {
    setup([]);

    expect(component.actionTypeLabel('AUTRE' as never)).toBe('AUTRE');
  });
});
