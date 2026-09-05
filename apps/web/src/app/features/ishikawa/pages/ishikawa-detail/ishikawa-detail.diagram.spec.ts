import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { ActivatedRoute, Router } from '@angular/router';
import { of } from 'rxjs';

import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { IshikawaFishboneComponent } from '../../components/ishikawa-fishbone/ishikawa-fishbone.component';
import { IshikawaService } from '../../ishikawa.service';
import {
  IshikawaCauseResponse,
  IshikawaDiagramResponse,
  IshikawaMode
} from '../../ishikawa.types';
import { IshikawaDetailComponent } from './ishikawa-detail.component';

/**
 * L'arête de poisson sur l'écran du diagramme.
 *
 * <p>Des cartes par branche RANGEAIENT les causes ; elles ne montraient pas
 * qu'elles convergent toutes vers un même effet — or c'est tout le propos
 * d'Ishikawa. Elles ont donc été retirées, et la figure a repris ce qu'elles
 * seules portaient : le score, et l'ajout d'un sous-pourquoi. Ce qui se vérifie
 * ici : que la figure suive la configuration réelle du diagramme (6M, 7M, 8M),
 * et qu'activer une cause dessus ouvre bien le dialogue de sous-cause.
 */
describe('IshikawaDetailComponent (arête de poisson)', () => {

  let component: IshikawaDetailComponent;
  let fixture: ComponentFixture<IshikawaDetailComponent>;
  let svc: jasmine.SpyObj<IshikawaService>;

  const DIAGRAM = '22222222-2222-2222-2222-222222222222';

  const cause = (over: Partial<IshikawaCauseResponse>): IshikawaCauseResponse => ({
    id: 'c-0', diagramId: DIAGRAM, category: 'METHODS', label: 'Cause',
    createdAt: '2026-08-07T08:00:00Z', updatedAt: '2026-08-07T08:00:00Z', ...over
  });

  const diagram = (
    mode: IshikawaMode,
    causes: IshikawaCauseResponse[] = []
  ): IshikawaDiagramResponse => ({
    id: DIAGRAM, tenantId: 't1', problemStatement: 'Rebuts en hausse sur la ligne 3',
    description: null, mode, status: 'DRAFT', ownerId: 'u1', causes,
    createdAt: '2026-08-07T08:00:00Z', updatedAt: '2026-08-07T08:00:00Z'
  } as unknown as IshikawaDiagramResponse);

  function setup(d: IshikawaDiagramResponse): void {
    svc = jasmine.createSpyObj<IshikawaService>('IshikawaService',
      ['getDiagram', 'listActions', 'addAction', 'updateAction', 'deleteAction',
       'suggestCauses', 'convertToPdca', 'deleteDiagram', 'addCause']);
    svc.getDiagram.and.returnValue(of(d));
    svc.listActions.and.returnValue(of([]));

    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      declarations: [IshikawaDetailComponent, IshikawaFishboneComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        { provide: IshikawaService, useValue: svc },
        { provide: MatSnackBar, useValue: jasmine.createSpyObj('MatSnackBar', ['open']) },
        { provide: MatDialog, useValue: jasmine.createSpyObj('MatDialog', ['open']) },
        { provide: Router, useValue: jasmine.createSpyObj('Router', ['navigate']) },
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: new Map([['id', DIAGRAM]]) as never } }
        }
      ]
    });
    fixture = TestBed.createComponent(IshikawaDetailComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  const host = () => fixture.nativeElement as HTMLElement;

  // --- le mode commande le nombre de branches -----------------------------------

  it('trace six branches en 6M', () => {
    setup(diagram('SIX_M'));

    expect(host().querySelectorAll('.fishbone__bone').length).toBe(6);
  });

  it('trace sept branches en 7M', () => {
    setup(diagram('SEVEN_M'));

    expect(host().querySelectorAll('.fishbone__bone').length).toBe(7);
  });

  it('trace huit branches en 8M', () => {
    setup(diagram('EIGHT_M'));

    expect(host().querySelectorAll('.fishbone__bone').length).toBe(8);
  });

  // --- causes et sous-causes -------------------------------------------------------

  it('accroche les causes de premier niveau à leur branche', () => {
    setup(diagram('SIX_M', [
      cause({ id: 'c-1', category: 'METHODS', label: 'Gamme obsolète' }),
      cause({ id: 'c-2', category: 'MACHINES', label: 'Presse déréglée' })
    ]));

    expect(host().querySelectorAll('.fishbone__tick').length).toBe(2);
    expect(host().textContent).toContain('Gamme obsolète');
  });

  it('compte les sous-causes au lieu de les tracer', () => {
    setup(diagram('SIX_M', [
      cause({ id: 'c-1', category: 'METHODS', label: 'Gamme obsolète' }),
      cause({ id: 'c-2', category: 'METHODS', label: 'Non relue', parentId: 'c-1' }),
      cause({ id: 'c-3', category: 'METHODS', label: 'Pas de revue', parentId: 'c-2' })
    ]));

    // Un seul trait — la cause racine de la branche — et le compteur dit que
    // deux niveaux sont dépliés plus bas, dans la carte de branche.
    expect(host().querySelectorAll('.fishbone__tick').length).toBe(1);
    expect(host().querySelector('.fishbone__sub')!.textContent).toContain('+2');
  });

  it('a retiré les cartes de branche au profit du dessin', () => {
    setup(diagram('SIX_M', [cause({ id: 'c-1', category: 'METHODS', label: 'Gamme obsolète' })]));

    // Elles rangeaient les causes par famille sans jamais montrer la
    // convergence vers l'effet — et doublaient la figure à l'écran.
    expect(host().querySelectorAll('.branch-card').length).toBe(0);
    expect(host().querySelectorAll('.fishbone__cause-group').length).toBe(1);
  });

  it('ouvre le dialogue de sous-cause quand une cause est activée sur le dessin', () => {
    // C'était le rôle des cartes : depuis leur retrait, une figure qu'on ne peut
    // que regarder rendrait la méthode inutilisable au-delà du premier niveau.
    setup(diagram('SIX_M', [cause({ id: 'c-1', category: 'METHODS', label: 'Gamme obsolète' })]));
    const ouvrir = spyOn(component, 'openAddSubCause');

    (host().querySelector('.fishbone__cause-group') as HTMLElement)
      .dispatchEvent(new MouseEvent('click', { bubbles: true }));

    expect(ouvrir).toHaveBeenCalled();
    expect(ouvrir.calls.mostRecent().args[1].id).toBe('c-1');
  });

  it('ignore en silence une cause qui n’existe plus', () => {
    // Le seul cas réel est une activation qui arrive après un rechargement
    // l'ayant supprimée : ouvrir un dialogue sur une cause disparue, ou afficher
    // une erreur pour un clic devenu sans objet, serait pire que ne rien faire.
    const d = diagram('SIX_M', [cause({ id: 'c-1', category: 'METHODS', label: 'Gamme' })]);
    setup(d);
    const ouvrir = spyOn(component, 'openAddSubCause');

    component.onCauseActivate(d, 'c-disparue');

    expect(ouvrir).not.toHaveBeenCalled();
  });

  it('porte l\'énoncé du problème dans la tête du dessin', () => {
    setup(diagram('SIX_M'));

    expect(host().querySelector('.fishbone__head')!.textContent).toContain('Rebuts');
  });

  // --- coût de calcul -----------------------------------------------------------------

  it('ne recalcule pas l\'arbre des branches à chaque cycle de détection', () => {
    // Le gabarit appelle ces méthodes à chaque cycle ; sans mémorisation, la
    // moindre frappe dans le plan d'actions reconstruirait tout le tracé.
    setup(diagram('SIX_M', [cause({ id: 'c-1', category: 'METHODS', label: 'Gamme obsolète' })]));
    const d = diagram('SIX_M', [cause({ id: 'c-1', category: 'METHODS', label: 'Gamme obsolète' })]);

    expect(component.branches(d)).toBe(component.branches(d));
    expect(component.fishboneBranches(d)).toBe(component.fishboneBranches(d));
  });

  it('recalcule quand le diagramme rechargé est un autre objet', () => {
    setup(diagram('SIX_M'));
    const premier = component.fishboneBranches(diagram('SIX_M'));
    const second = component.fishboneBranches(diagram('EIGHT_M'));

    expect(second).not.toBe(premier);
    expect(second.length).toBe(8);
  });
});
