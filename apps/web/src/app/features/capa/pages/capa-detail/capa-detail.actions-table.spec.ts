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
import {
  CapaActionResponse, CapaCaseResponse, CapaEvidence, CapaStatus
} from '../../capa.types';
import { CapaDetailComponent } from './capa-detail.component';

/**
 * Tableau des actions d'une fiche CAPA (§4.2, ADR 0052).
 *
 * Ce qui se teste ici n'est pas « la colonne s'affiche-t-elle » — c'est que
 * chaque colonne dit ce qu'elle prétend dire, et qu'elle se tait quand elle
 * n'a rien à dire plutôt que d'inventer : la date est celle de la DÉCISION et
 * jamais celle de la saisie, le responsable est un nom et non un identifiant,
 * la non-conformité vient du dossier, et l'édition en ligne ne peut ni vider un
 * libellé ni se perdre au clavier.
 */
describe('CapaDetailComponent — tableau des actions', () => {

  const CASE_ID = '55555555-5555-5555-5555-555555555555';
  const ACTION_ID = 'a1111111-1111-1111-1111-111111111111';

  let fixture: ComponentFixture<CapaDetailComponent>;
  let component: CapaDetailComponent;
  let capa: jasmine.SpyObj<CapaService>;
  let snack: jasmine.SpyObj<MatSnackBar>;
  let dialog: jasmine.SpyObj<MatDialog>;

  const action = (over: Partial<CapaActionResponse> = {}): CapaActionResponse => ({
    id: ACTION_ID, capaId: CASE_ID, title: 'Réviser le plan de contrôle réception',
    status: 'PENDING', actionType: 'CORRECTIVE', ...over
  });

  const dossier = (over: Partial<CapaCaseResponse> = {}): CapaCaseResponse => ({
    id: CASE_ID, tenantId: 't1', title: 'Étiquetage manquant', type: 'CORRECTIVE',
    criticity: 'HIGH', status: 'IN_PROGRESS', sourceType: 'NON_CONFORMITY', ownerId: 'u1',
    createdAt: '2026-08-01T00:00:00Z', updatedAt: '2026-08-01T00:00:00Z',
    actions: [action()], ...over
  });

  const preuveAction = (over: Partial<CapaEvidence> = {}): CapaEvidence => ({
    id: 'evd-a1', capaId: CASE_ID, actionId: ACTION_ID, contentType: 'application/pdf',
    sizeBytes: 2048, originalFilename: 'constat.pdf', createdAt: '2026-08-09T10:00:00Z',
    url: 'https://stockage.example/p?sig=abc', ...over
  });

  function setup(c: CapaCaseResponse = dossier()): void {
    capa.getCase.and.returnValue(of(c));
    fixture = TestBed.createComponent(CapaDetailComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
    (component as unknown as { reload$: { next(v: void): void } }).reload$.next();
    fixture.detectChanges();
  }

  function texte(): string {
    return (fixture.nativeElement as HTMLElement).textContent ?? '';
  }

  function cellules(): string[] {
    return Array.from((fixture.nativeElement as HTMLElement)
      .querySelectorAll('.actions-table tbody td'))
      .map(td => (td.textContent ?? '').trim());
  }

  /**
   * Cellule d'une colonne NOMMÉE de la première ligne.
   *
   * Les index positionnels ont coûté une demi-douzaine de faux échecs le jour où
   * une colonne s'est insérée au milieu du tableau : chaque assertion pointait
   * alors sa voisine, et le test accusait la colonne qu'il ne testait pas.
   * Le nom, lui, ne bouge pas quand l'ordre change.
   */
  function cellule(colonne: string): string {
    const index = component.actionColumns.indexOf(colonne);
    expect(index).withContext(`colonne inconnue : ${colonne}`).toBeGreaterThanOrEqual(0);
    return cellules()[index];
  }

  function fichier(): Event {
    const file = new File(['%PDF-1.7'], 'constat.pdf', { type: 'application/pdf' });
    const input = document.createElement('input');
    Object.defineProperty(input, 'files', { value: [file] });
    return { target: input } as unknown as Event;
  }

  beforeEach(async () => {
    capa = jasmine.createSpyObj<CapaService>('CapaService',
      ['getCase', 'listEvidences', 'uploadEvidence', 'deleteEvidence',
       'listActionEvidences', 'uploadActionEvidence', 'deleteActionEvidence',
       'updateAction']);
    capa.listEvidences.and.returnValue(of([]));
    capa.listActionEvidences.and.returnValue(of([]));
    snack = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);
    dialog = jasmine.createSpyObj<MatDialog>('MatDialog', ['open']);
    dialog.open.and.returnValue({ afterClosed: () => of(true) } as never);

    TestBed.resetTestingModule();
    await TestBed.configureTestingModule({
      declarations: [CapaDetailComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        { provide: CapaService, useValue: capa },
        { provide: MatSnackBar, useValue: snack },
        { provide: MatDialog, useValue: dialog },
        { provide: Router, useValue: jasmine.createSpyObj('Router', ['navigate']) },
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: convertToParamMap({ id: CASE_ID }) } } }
      ]
    }).compileComponents();
  });

  // --- structure du tableau -----------------------------------------------

  it('porte les colonnes attendues, dans l\'ordre de lecture d\'un auditeur', () => {
    setup();

    // La NATURE suit immédiatement le libellé : c'est la première question que
    // pose un auditeur devant une action — a-t-elle contenu, ou corrigé ?
    expect(component.actionColumns).toEqual([
      'title', 'actionType', 'decidedOn', 'assignee', 'nonConformity',
      'evidence', 'status', 'dueDate', 'rowActions'
    ]);
    const entetes = Array.from((fixture.nativeElement as HTMLElement)
      .querySelectorAll('.actions-table th')).map(th => (th.textContent ?? '').trim());
    expect(entetes).toContain('Action');
    expect(entetes).toContain('Nature');
    expect(entetes).toContain('Date');
    expect(entetes).toContain('Responsable');
    expect(entetes).toContain('Non-conformité');
    expect(entetes).toContain('Preuve');
  });

  // --- colonne « Date » : la DÉCISION, pas la saisie -----------------------

  it('affiche la date de décision, pas la date de création', () => {
    setup(dossier({
      actions: [action({
        decidedOn: '2026-03-12',
        // Créée trois semaines après le comité : si la colonne affichait
        // createdAt, elle daterait la décision d'avril.
        completedAt: '2026-04-02T09:00:00Z'
      })]
    }));

    expect(cellule('decidedOn')).toContain('2026');
    expect(cellule('decidedOn')).toContain('12');
    expect(cellule('decidedOn')).not.toContain('avr');
  });

  it('n\'invente pas de date quand l\'action est antérieure à cette colonne', () => {
    setup(dossier({ actions: [action({ decidedOn: undefined })] }));

    // Recopier la date de création fabriquerait une décision jamais enregistrée.
    expect(cellule('decidedOn')).toBe('—');
  });

  // --- colonne « Responsable » : un nom, jamais un identifiant -------------

  it('affiche le NOM du porteur', () => {
    setup(dossier({ actions: [action({ assigneeName: 'Amina Dridi', assigneeId: 'u-42' })] }));

    expect(cellule('assignee')).toBe('Amina Dridi');
  });

  it('ne retombe pas sur l\'identifiant quand le nom manque', () => {
    setup(dossier({ actions: [action({ assigneeId: '9c1f2b7e-0000-4000-8000-000000000000' })] }));

    // Un UUID dans une colonne « Responsable » n'apprend rien à personne.
    expect(cellule('assignee')).toBe('—');
    expect(texte()).not.toContain('9c1f2b7e');
  });

  // --- colonne « Non-conformité » -----------------------------------------

  it('montre le nom de l\'écart d\'origine sur chaque ligne', () => {
    setup(dossier({
      sourceNonConformity: {
        id: 'nc-1', reference: 'NC-2026-0018', title: 'Étiquetage lot 4471 illisible'
      }
    }));

    expect(cellule('nonConformity')).toContain('Étiquetage lot 4471 illisible');
  });

  it('reste muet quand le dossier ne procède d\'aucun écart', () => {
    setup(dossier({ sourceType: 'AUDIT', sourceNonConformity: undefined }));

    expect(cellule('nonConformity')).toBe('—');
  });

  // --- colonne « Preuve » --------------------------------------------------

  it('range la pièce dans la ligne de son action', () => {
    capa.listActionEvidences.and.returnValue(of([preuveAction()]));

    setup();

    expect(component.actionEvidence(ACTION_ID)?.originalFilename).toBe('constat.pdf');
    const lien = (fixture.nativeElement as HTMLElement)
      .querySelector('.action-evidence') as HTMLAnchorElement;
    expect(lien.textContent).toContain('constat.pdf');
    // Une URL signée pointe hors application : sans noopener, la page ouverte
    // garde une prise sur la nôtre.
    expect(lien.getAttribute('rel')).toContain('noopener');
    expect(lien.getAttribute('target')).toBe('_blank');
  });

  it('propose de joindre quand l\'action n\'a pas encore de preuve', () => {
    setup();

    expect(component.canAttachActionEvidence(ACTION_ID, 'IN_PROGRESS')).toBeTrue();
    expect((fixture.nativeElement as HTMLElement).querySelector('.attach-btn')).not.toBeNull();
  });

  it('dépose la pièce sur l\'action visée et non sur la dernière ligne rendue', () => {
    setup(dossier({
      actions: [action(), action({ id: 'a2', title: 'Former les opérateurs' })]
    }));
    capa.uploadActionEvidence.and.returnValue(of(preuveAction()));

    const input = document.createElement('input');
    component.triggerActionEvidencePicker(input, ACTION_ID);
    component.onActionEvidenceSelected(fichier());

    expect(capa.uploadActionEvidence).toHaveBeenCalledWith(CASE_ID, ACTION_ID, jasmine.any(File));
    expect(component.actionEvidence(ACTION_ID)).toBeTruthy();
    expect(component.actionEvidence('a2')).toBeUndefined();
  });

  it('ne tente rien si aucun fichier n\'est choisi', () => {
    setup();

    const input = document.createElement('input');
    component.triggerActionEvidencePicker(input, ACTION_ID);
    component.onActionEvidenceSelected({ target: document.createElement('input') } as unknown as Event);

    expect(capa.uploadActionEvidence).not.toHaveBeenCalled();
  });

  it('distingue les refus de dépôt : trop lourd, format, action déjà pourvue', () => {
    setup();
    const messages: string[] = [];
    snack.open.and.callFake((m: string) => { messages.push(m); return {} as never; });

    [413, 400, 409].forEach(status => {
      capa.uploadActionEvidence.and.returnValue(
        throwError(() => new HttpErrorResponse({ status })));
      const input = document.createElement('input');
      component.triggerActionEvidencePicker(input, ACTION_ID);
      component.onActionEvidenceSelected(fichier());
    });

    expect(messages[0]).toContain('10 Mo');
    expect(messages[1]).toContain('Format refusé');
    expect(messages[2]).toContain('déjà sa preuve');
    expect(new Set(messages).size).toBe(3);
  });

  it('annonce le stockage coupé au lieu d\'un tableau sans preuve', () => {
    capa.listActionEvidences.and.returnValue(throwError(() => new HttpErrorResponse({
      status: 503, error: { type: 'https://qualitos.io/errors/storage-disabled' }
    })));

    setup();

    expect(component.evidenceStorageDisabled$.value).toBeTrue();
    // Proposer « Joindre » alors que rien ne peut être déposé enverrait
    // l'utilisateur droit dans un refus.
    expect(component.canAttachActionEvidence(ACTION_ID, 'IN_PROGRESS')).toBeFalse();
  });

  it('retire la pièce de l\'action après confirmation', () => {
    capa.listActionEvidences.and.returnValue(of([preuveAction()]));
    capa.deleteActionEvidence.and.returnValue(of(void 0));

    setup();
    component.removeActionEvidence(ACTION_ID, preuveAction());

    expect(dialog.open).toHaveBeenCalled();
    expect(capa.deleteActionEvidence).toHaveBeenCalledWith(CASE_ID, ACTION_ID, 'evd-a1');
    expect(component.actionEvidence(ACTION_ID)).toBeUndefined();
  });

  it('garde la pièce quand le retrait échoue, et le dit', () => {
    capa.listActionEvidences.and.returnValue(of([preuveAction()]));
    capa.deleteActionEvidence.and.returnValue(
      throwError(() => new HttpErrorResponse({ status: 409 })));

    setup();
    component.removeActionEvidence(ACTION_ID, preuveAction());

    // Retirer la ligne d'écran alors que le serveur a refusé ferait croire à
    // une preuve disparue.
    expect(component.actionEvidence(ACTION_ID)).toBeTruthy();
    expect(snack.open).toHaveBeenCalled();
    expect(component.busyEvidenceActionId$.value).toBeNull();
  });

  it('ne retire rien quand la confirmation est refusée', () => {
    capa.listActionEvidences.and.returnValue(of([preuveAction()]));
    dialog.open.and.returnValue({ afterClosed: () => of(false) } as never);

    setup();
    component.removeActionEvidence(ACTION_ID, preuveAction());

    expect(capa.deleteActionEvidence).not.toHaveBeenCalled();
    expect(component.actionEvidence(ACTION_ID)).toBeTruthy();
  });

  it('ignore un second geste tant que la cellule travaille', () => {
    setup();
    component.busyEvidenceActionId$.next(ACTION_ID);

    const input = document.createElement('input');
    spyOn(input, 'click');
    component.triggerActionEvidencePicker(input, ACTION_ID);
    component.removeActionEvidence(ACTION_ID, preuveAction());

    // Deux dépôts concurrents sur la même action buteraient sur la borne « une
    // pièce » et laisseraient l'utilisateur devant un refus qu'il n'a pas causé.
    expect(input.click).not.toHaveBeenCalled();
    expect(dialog.open).not.toHaveBeenCalled();
    expect(component.canAttachActionEvidence(ACTION_ID, 'IN_PROGRESS')).toBeFalse();
  });

  it('ne rouvre pas une édition ni n\'en démarre une pendant l\'enregistrement', () => {
    setup();
    component.savingEdit = true;

    component.startEdit(action());
    expect(component.isEditing(ACTION_ID)).toBeFalse();

    component.savingEdit = false;
    component.startEdit(action());
    component.savingEdit = true;
    component.cancelEdit();
    // Annuler pendant l'écriture laisserait une requête sans destinataire.
    expect(component.isEditing(ACTION_ID)).toBeTrue();
    component.savingEdit = false;
  });

  it('ne fait pas avancer une action déjà faite', () => {
    setup();

    component.advanceAction(action({ status: 'DONE' }));

    expect(capa.updateAction).not.toHaveBeenCalled();
    expect(component.nextActionStatus('DONE')).toBeNull();
  });

  it('verrouille la colonne sur un dossier clos', () => {
    capa.listActionEvidences.and.returnValue(of([preuveAction()]));

    setup(dossier({ status: 'CLOSED' }));

    expect(component.canAttachActionEvidence(ACTION_ID, 'CLOSED')).toBeFalse();
    // La pièce reste consultable : c'est elle qui explique la clôture.
    expect((fixture.nativeElement as HTMLElement).querySelector('.action-evidence')).not.toBeNull();
    expect((fixture.nativeElement as HTMLElement).querySelector('.evidence-remove')).toBeNull();
  });

  // --- édition en ligne ----------------------------------------------------

  it('bascule la ligne en édition sans ouvrir de dialogue', () => {
    setup();

    component.startEdit(action());
    fixture.detectChanges();

    expect(dialog.open).not.toHaveBeenCalled();
    expect(component.isEditing(ACTION_ID)).toBeTrue();
    const champ = (fixture.nativeElement as HTMLElement)
      .querySelector('.inline-field--title input') as HTMLInputElement;
    expect(champ).not.toBeNull();
    expect(champ.value).toBe('Réviser le plan de contrôle réception');
    // Le focus suit l'édition : sinon l'utilisateur au clavier se retrouve sur
    // un bouton disparu, et repart en tête de document.
    expect(document.activeElement).toBe(champ);
  });

  it('n\'envoie que les champs édités — date de décision et porteur restent intouchés', () => {
    setup();
    capa.updateAction.and.returnValue(of(action()));

    component.startEdit(action({ assigneeName: 'Amina Dridi', decidedOn: '2026-03-12' }));
    component.editForm.setValue({ title: '  Libellé corrigé  ', status: 'DONE', actionType: 'CORRECTIVE' });
    component.saveEdit();

    expect(capa.updateAction).toHaveBeenCalledWith(CASE_ID, ACTION_ID, {
      title: 'Libellé corrigé', status: 'DONE', actionType: 'CORRECTIVE'
    });
    expect(component.isEditing(ACTION_ID)).toBeFalse();
  });

  it('refuse d\'enregistrer un libellé vide, sans appeler le serveur', () => {
    setup();

    component.startEdit(action());
    component.editForm.setValue({ title: '   ', status: 'PENDING', actionType: 'CORRECTIVE' });
    component.saveEdit();

    // Required + trim côté formulaire : un espace ne doit pas devenir le
    // libellé d'une action dans un dossier d'audit.
    expect(capa.updateAction).not.toHaveBeenCalled();
    expect(component.isEditing(ACTION_ID)).toBeTrue();
  });

  it('annule sans rien écrire', () => {
    setup();

    component.startEdit(action());
    component.editForm.setValue({ title: 'saisie abandonnée', status: 'DONE', actionType: 'CORRECTIVE' });
    component.cancelEdit();

    expect(capa.updateAction).not.toHaveBeenCalled();
    expect(component.isEditing(ACTION_ID)).toBeFalse();
  });

  it('se pilote au clavier : Échap annule, Entrée enregistre', () => {
    setup();
    capa.updateAction.and.returnValue(of(action()));

    component.startEdit(action());
    component.onEditKeydown(new KeyboardEvent('keydown', { key: 'Escape' }));
    expect(component.isEditing(ACTION_ID)).toBeFalse();
    expect(capa.updateAction).not.toHaveBeenCalled();

    component.startEdit(action());
    component.onEditKeydown(new KeyboardEvent('keydown', { key: 'Enter' }));
    expect(capa.updateAction).toHaveBeenCalled();
  });

  it('laisse passer les autres touches — on tape dans le champ', () => {
    setup();

    component.startEdit(action());
    component.onEditKeydown(new KeyboardEvent('keydown', { key: 'a' }));

    expect(component.isEditing(ACTION_ID)).toBeTrue();
    expect(capa.updateAction).not.toHaveBeenCalled();
  });

  it('n\'enregistre rien quand aucune ligne n\'est en édition', () => {
    setup();

    component.saveEdit();

    expect(capa.updateAction).not.toHaveBeenCalled();
  });

  it('laisse la ligne ouverte quand l\'enregistrement échoue', () => {
    setup();
    capa.updateAction.and.returnValue(throwError(() => new HttpErrorResponse({ status: 400 })));

    component.startEdit(action());
    component.editForm.setValue({ title: 'Libellé corrigé', status: 'DONE', actionType: 'CORRECTIVE' });
    component.saveEdit();

    // Refermer effacerait la saisie que l'utilisateur doit justement corriger.
    expect(component.isEditing(ACTION_ID)).toBeTrue();
    expect(component.savingEdit).toBeFalse();
    expect(snack.open).toHaveBeenCalled();
  });

  it('ferme l\'édition sur un dossier clos', () => {
    setup(dossier({ status: 'CLOSED' }));

    expect(component.canEditAction('CLOSED')).toBeFalse();
    const bouton = (fixture.nativeElement as HTMLElement)
      .querySelector('.edit-btn') as HTMLButtonElement;
    expect(bouton.disabled).toBeTrue();
  });

  // --- avancement ----------------------------------------------------------

  it('fait avancer une action sans renvoyer son libellé', () => {
    setup();
    capa.updateAction.and.returnValue(of(action({ status: 'IN_PROGRESS' })));

    component.advanceAction(action());

    // Renvoyer le libellé écraserait une correction faite entre-temps ailleurs.
    expect(capa.updateAction).toHaveBeenCalledWith(CASE_ID, ACTION_ID, { status: 'IN_PROGRESS' });
  });

  it('nomme les statuts en toutes lettres plutôt qu\'en constantes serveur', () => {
    setup(dossier({ actions: [action({ status: 'IN_PROGRESS' })] }));

    // La colonne de statut du TABLEAU : la constante brute reste ailleurs sur
    // la fiche (badge d'état du dossier), ce n'est pas ce qu'on juge ici.
    expect(cellule('status')).toBe('En cours');
    expect(cellule('status')).not.toContain('IN_PROGRESS');
  });
});
