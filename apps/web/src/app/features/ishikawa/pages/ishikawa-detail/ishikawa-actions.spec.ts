import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { ActivatedRoute, Router } from '@angular/router';
import { of, throwError } from 'rxjs';

import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { IshikawaService } from '../../ishikawa.service';
import { IshikawaActionResponse, IshikawaDiagramResponse } from '../../ishikawa.types';
import { IshikawaDetailComponent } from './ishikawa-detail.component';

/**
 * Plan d'actions du diagramme.
 *
 * <p>Un diagramme s'arrêtait aux causes : les décisions prises devant lui — qui
 * fait quoi, décidé quand — vivaient dans un compte rendu, un tableur, une
 * mémoire. Ce tableau les ramène là où elles ont été prises, et se modifie
 * cellule par cellule : on corrige un libellé sans rouvrir un formulaire.
 */
describe('IshikawaDetailComponent (plan d’actions)', () => {
  let component: IshikawaDetailComponent;
  let fixture: ComponentFixture<IshikawaDetailComponent>;
  let svc: jasmine.SpyObj<IshikawaService>;
  let snack: jasmine.SpyObj<MatSnackBar>;

  const DIAGRAM = '11111111-1111-1111-1111-111111111111';

  const diagram: IshikawaDiagramResponse = {
    id: DIAGRAM, tenantId: 't1', problemStatement: 'Rebuts en hausse',
    description: null, mode: 'SIX_M', status: 'DRAFT', ownerId: 'u1',
    causes: [], createdAt: '2026-08-06T08:00:00Z', updatedAt: '2026-08-06T08:00:00Z'
  } as unknown as IshikawaDiagramResponse;

  const action = (over: Partial<IshikawaActionResponse> = {}): IshikawaActionResponse => ({
    id: 'a-1', diagramId: DIAGRAM, label: 'Refaire le réglage', responsible: 'Karim',
    decidedOn: '2026-08-06', status: 'TODO',
    createdAt: '2026-08-06T08:00:00Z', updatedAt: '2026-08-06T08:00:00Z', ...over
  });

  beforeEach(async () => {
    svc = jasmine.createSpyObj<IshikawaService>('IshikawaService',
      ['getDiagram', 'listActions', 'addAction', 'updateAction', 'deleteAction',
       'suggestCauses', 'convertToPdca', 'deleteDiagram', 'addCause']);
    svc.getDiagram.and.returnValue(of(diagram));
    svc.listActions.and.returnValue(of([action()]));
    snack = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);

    await TestBed.configureTestingModule({
      declarations: [IshikawaDetailComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        { provide: IshikawaService, useValue: svc },
        { provide: MatSnackBar, useValue: snack },
        { provide: MatDialog, useValue: jasmine.createSpyObj('MatDialog', ['open']) },
        { provide: Router, useValue: jasmine.createSpyObj('Router', ['navigate']) },
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: new Map([['id', DIAGRAM]]) as never } }
        }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(IshikawaDetailComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('charge le plan d’actions du diagramme', () => {
    expect(svc.listActions).toHaveBeenCalledWith(DIAGRAM);
    expect(component.actions.length).toBe(1);
  });

  it('ajoute une action décidée', () => {
    svc.addAction.and.returnValue(of(action({ id: 'a-2', label: 'Contrôler le lot' })));

    component.newActionLabel = 'Contrôler le lot';
    component.addAction();

    expect(svc.addAction).toHaveBeenCalled();
    expect(component.actions.length).toBe(2);
    // Le champ se vide : sans cela, un double clic recréerait la même action.
    expect(component.newActionLabel).toBe('');
  });

  it('refuse d’ajouter une action sans intitulé', () => {
    component.newActionLabel = '   ';
    component.addAction();

    expect(svc.addAction).not.toHaveBeenCalled();
  });

  it('modifie le libellé dans la cellule, et rien d’autre', () => {
    svc.updateAction.and.returnValue(of(action({ label: 'Nouveau libellé' })));

    component.startEdit(component.actions[0]);
    component.editLabel = 'Nouveau libellé';
    component.commitEdit(component.actions[0]);

    expect(svc.updateAction).toHaveBeenCalledWith('a-1', { label: 'Nouveau libellé' });
    expect(component.editingId).toBeNull();
  });

  it('abandonne l’édition sans rien envoyer', () => {
    component.startEdit(component.actions[0]);
    component.editLabel = 'saisie abandonnée';
    component.cancelEdit();

    expect(svc.updateAction).not.toHaveBeenCalled();
    expect(component.editingId).toBeNull();
  });

  it('ne renvoie rien si le libellé n’a pas changé', () => {
    // Un clic hors de la cellule ne doit pas produire un appel inutile.
    component.startEdit(component.actions[0]);
    component.commitEdit(component.actions[0]);

    expect(svc.updateAction).not.toHaveBeenCalled();
  });

  it('refuse de valider une cellule vidée', () => {
    component.startEdit(component.actions[0]);
    component.editLabel = '';
    component.commitEdit(component.actions[0]);

    expect(svc.updateAction).not.toHaveBeenCalled();
    expect(snack.open).toHaveBeenCalled();
  });

  it('change le statut depuis la liste déroulante', () => {
    svc.updateAction.and.returnValue(of(action({ status: 'DONE' })));

    component.changeStatus(component.actions[0], 'DONE');

    expect(svc.updateAction).toHaveBeenCalledWith('a-1', { status: 'DONE' });
    expect(component.actions[0].status).toBe('DONE');
  });

  it('remet la ligne dans son état si le serveur refuse', () => {
    svc.updateAction.and.returnValue(throwError(() => new Error('403')));

    component.changeStatus(component.actions[0], 'DONE');

    // Laisser « fait » à l'écran alors que rien n'est enregistré serait pire que
    // l'erreur elle-même : l'utilisateur croirait son action close.
    expect(component.actions[0].status).toBe('TODO');
    expect(snack.open).toHaveBeenCalled();
  });

  it('supprime une action du plan', () => {
    svc.deleteAction.and.returnValue(of(undefined));

    component.deleteAction(component.actions[0]);

    expect(svc.deleteAction).toHaveBeenCalledWith('a-1');
    expect(component.actions.length).toBe(0);
  });
});
