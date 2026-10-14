import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { ActivatedRoute, Router, convertToParamMap } from '@angular/router';
import { of, throwError } from 'rxjs';

import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { CapaDirectoryService, DirectoryChoices } from '../../capa-directory.service';
import { CapaService } from '../../capa.service';
import {
  CapaActionResponse, CapaCaseResponse, CapaEvidence, CapaSourceType
} from '../../capa.types';
import { CapaCreateDialogComponent, CapaCreateDialogData } from '../capa-create-dialog/capa-create-dialog.component';
import { CapaDetailComponent } from './capa-detail.component';
import {
  CapaRevisionImpactComponent
} from '../capa-revision-impact/capa-revision-impact.component';
import { ProductsService } from '../../../products/products.service';
import { AuthzService } from '../../../../core/authz/authz.service';

/**
 * L'action unique d'une fiche CAPA (ADR 0088, ADR 0052 pour la preuve).
 *
 * Une CAPA EST son action, quelle que soit sa source : la fiche la montre et la
 * fait avancer, sans liste à remplir, sans « Ajouter une action » ni
 * suggestion. Un dossier sans action propose de définir la sienne dans LA
 * fenêtre de création ; un ancien dossier à plusieurs actions les montre
 * toutes, sans permettre d'en ajouter. Le pilote se lit par son nom.
 */
describe('CapaDetailComponent — action unique', () => {

  const CASE_ID = '55555555-5555-5555-5555-555555555555';
  const ACTION_ID = 'a1111111-1111-1111-1111-111111111111';

  let fixture: ComponentFixture<CapaDetailComponent>;
  let component: CapaDetailComponent;
  let capa: jasmine.SpyObj<CapaService>;
  let snack: jasmine.SpyObj<MatSnackBar>;
  let dialog: jasmine.SpyObj<MatDialog>;
  let annuaire: DirectoryChoices;

  const action = (over: Partial<CapaActionResponse> = {}): CapaActionResponse => ({
    id: ACTION_ID, capaId: CASE_ID, title: 'Réviser le plan de contrôle réception',
    status: 'PENDING', actionType: 'CORRECTIVE', ...over
  });

  const dossier = (over: Partial<CapaCaseResponse> = {}): CapaCaseResponse => ({
    id: CASE_ID, tenantId: 't1', title: 'Étiquetage manquant', type: 'CORRECTIVE',
    criticity: 'HIGH', status: 'IN_PROGRESS', sourceType: 'NON_CONFORMITY',
    ownerId: '9c1f2b7e-0000-4000-8000-000000000000',
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

  function hote(): HTMLElement {
    return fixture.nativeElement as HTMLElement;
  }

  function carte(): string {
    return hote().querySelector('[data-test="action-unique"]')?.textContent ?? '';
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
       'updateAction', 'addAction']);
    capa.listEvidences.and.returnValue(of([]));
    capa.listActionEvidences.and.returnValue(of([]));
    snack = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);
    dialog = jasmine.createSpyObj<MatDialog>('MatDialog', ['open']);
    dialog.open.and.returnValue({ afterClosed: () => of(true) } as never);
    annuaire = { members: [], available: true };

    TestBed.resetTestingModule();
    await TestBed.configureTestingModule({
      declarations: [CapaDetailComponent, CapaRevisionImpactComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        { provide: CapaService, useValue: capa },
        { provide: CapaDirectoryService, useValue: { choices: () => of(annuaire) } },
        // Tous les droits : ces bancs testent la fiche, pas la matrice (ADR 0078).
        { provide: AuthzService, useValue: { can: () => of(true) } },
        // L'encart d'impact vit dans la fiche : sans ce doublon, il irait
        // chercher un HttpClient que ce banc de test ne fournit pas.
        { provide: ProductsService, useValue: { revisionRequestsForTrigger: () => of([]) } },
        { provide: MatSnackBar, useValue: snack },
        { provide: MatDialog, useValue: dialog },
        { provide: Router, useValue: jasmine.createSpyObj('Router', ['navigate']) },
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: convertToParamMap({ id: CASE_ID }) } } }
      ]
    }).compileComponents();
  });

  // --- une action, quelle que soit la source --------------------------------

  const sources: CapaSourceType[] = ['NON_CONFORMITY', 'AUDIT', 'RISK', 'COMPLAINT', 'INTERNAL', 'SPC_ALERT'];
  sources.forEach(source => {
    it(`montre l'action seule, sans liste ni ajout ni suggestion — source ${source}`, () => {
      setup(dossier({
        sourceType: source,
        actions: [action({ assigneeName: 'A. Diallo', dueDate: '2026-12-01', actionType: 'PREVENTIVE' })]
      }));

      expect(hote().querySelector('[data-test="action-unique"]')).not.toBeNull();
      expect(hote().querySelector('.actions-card')).toBeNull();
      expect(hote().querySelector('.actions-table')).toBeNull();
      expect(hote().querySelector('.ia-btn')).toBeNull();
      expect(hote().textContent).not.toContain('Ajouter une action');
      expect(hote().textContent).not.toContain('Suggérer');
      expect(carte()).toContain('Réviser le plan de contrôle réception');
      expect(carte()).toContain('A. Diallo');
      expect(carte()).toContain('Préventive');
      expect(hote().querySelector('[data-test="definir-action"]')).toBeNull();
    });
  });

  it('fait avancer l\'action sans renvoyer son libellé', () => {
    setup();
    capa.updateAction.and.returnValue(of(action({ status: 'IN_PROGRESS' })));

    (hote().querySelector('[data-test="avancer-action"]') as HTMLButtonElement).click();

    // Renvoyer le libellé écraserait une correction faite entre-temps ailleurs.
    expect(capa.updateAction).toHaveBeenCalledWith(CASE_ID, ACTION_ID, { status: 'IN_PROGRESS' });
    expect(snack.open).toHaveBeenCalled();
  });

  it('une action terminée n\'offre plus de bouton d\'avancement', () => {
    setup(dossier({ actions: [action({ status: 'DONE', completedAt: '2026-08-02T00:00:00Z' })] }));
    expect(hote().querySelector('[data-test="avancer-action"]')).toBeNull();
    expect(carte()).toContain('Faite');
  });

  it('ne fait pas avancer une action déjà faite', () => {
    setup();
    component.advanceAction(action({ status: 'DONE' }));
    expect(capa.updateAction).not.toHaveBeenCalled();
    expect(component.nextActionStatus('DONE')).toBeNull();
  });

  it('nomme les statuts en toutes lettres plutôt qu\'en constantes serveur', () => {
    setup(dossier({ actions: [action({ status: 'IN_PROGRESS' })] }));
    expect(carte()).toContain('En cours');
    expect(carte()).not.toContain('IN_PROGRESS');
    expect(component.actionStatusLabel('PENDING')).toBe('À faire');
  });

  it('ne retombe pas sur un identifiant quand le nom du responsable manque', () => {
    setup(dossier({ actions: [action({ assigneeId: '9c1f2b7e-0000-4000-8000-000000000000' })] }));
    expect(carte()).toContain('—');
    expect(hote().textContent).not.toContain('9c1f2b7e');
  });

  // --- un dossier sans action -----------------------------------------------

  it('un dossier sans action propose de définir la sienne, dans la fenêtre commune', () => {
    const vide = dossier({ sourceType: 'SPC_ALERT', type: 'PREVENTIVE', dueDate: '2099-01-01', actions: [] });
    setup(vide);
    capa.getCase.calls.reset();

    expect(hote().querySelector('[data-test="action-unique-vide"]')).not.toBeNull();
    (hote().querySelector('[data-test="definir-action"]') as HTMLButtonElement).click();

    expect(dialog.open).toHaveBeenCalledWith(CapaCreateDialogComponent, jasmine.anything());
    const data = dialog.open.calls.mostRecent().args[1]!.data as CapaCreateDialogData;
    expect(data.mode).toBe('define-action');
    expect(data.caseId).toBe(CASE_ID);
    expect(data.prefill).toEqual(jasmine.objectContaining({
      title: 'Étiquetage manquant', type: 'PREVENTIVE', dueDate: '2099-01-01'
    }));
    expect(capa.getCase).toHaveBeenCalled();
  });

  it('un ancien dossier d\'endiguement propose une action corrective, pas un endiguement', () => {
    setup(dossier({ type: 'CONTAINMENT', actions: [] }));
    component.openDefineAction(dossier({ type: 'CONTAINMENT', actions: [] }));
    const data = dialog.open.calls.mostRecent().args[1]!.data as CapaCreateDialogData;
    expect(data.prefill?.type).toBe('CORRECTIVE');
  });

  it('ne propose pas de définir l\'action d\'un dossier clos', () => {
    setup(dossier({ status: 'CLOSED', actions: [] }));
    expect((hote().querySelector('[data-test="definir-action"]') as HTMLButtonElement).disabled).toBeTrue();
  });

  // --- les anciens dossiers à plusieurs actions -----------------------------

  it('montre toutes les actions d\'un ancien dossier, en lecture et avancement, sans en ajouter', () => {
    setup(dossier({
      actions: [action(), action({ id: 'a2', title: 'Former les opérateurs', status: 'DONE' })]
    }));

    expect(hote().querySelectorAll('[data-test="action-ligne"]').length).toBe(2);
    expect(carte()).toContain('Actions (2)');
    expect(carte()).toContain('Former les opérateurs');
    expect(hote().querySelectorAll('[data-test="avancer-action"]').length).toBe(1);
    expect(hote().querySelector('[data-test="definir-action"]')).toBeNull();
    expect(component.trackActionById(0, action())).toBe(ACTION_ID);
  });

  // --- le pilote, par son nom ------------------------------------------------

  it('nomme le pilote par le nom recopié à sa désignation', () => {
    setup(dossier({ ownerName: 'amina.dridi@client.test' }));
    expect(hote().querySelector('[data-test="capa-pilote"]')?.textContent?.trim()).toBe('amina.dridi@client.test');
  });

  it('nomme le pilote d\'un ancien dossier par l\'annuaire, jamais par son identifiant', () => {
    annuaire = { members: [{ userId: '9c1f2b7e-0000-4000-8000-000000000000', label: 'karim@client.test' }], available: true };
    setup(dossier());
    expect(hote().querySelector('[data-test="capa-pilote"]')?.textContent?.trim()).toBe('karim@client.test');
  });

  it('affiche « — » plutôt qu\'un identifiant quand le pilote est inconnu', () => {
    setup(dossier());
    expect(hote().querySelector('[data-test="capa-pilote"]')?.textContent?.trim()).toBe('—');
    expect(hote().textContent).not.toContain('9c1f2b7e');
  });

  it('dit la source en toutes lettres', () => {
    setup(dossier({ sourceType: 'RISK', sourceRef: 'R-014' }));
    const info = hote().querySelector('.info-card')?.textContent ?? '';
    expect(info).toContain('Risque');
    expect(info).toContain('R-014');
    expect(info).not.toContain('RISK');
  });

  it('garde l\'écart d\'origine visible, une fois, dans l\'en-tête du dossier', () => {
    setup(dossier({
      sourceNonConformity: { id: 'nc-1', reference: 'NC-2026-0018', title: 'Étiquetage lot 4471 illisible' }
    }));
    const info = hote().querySelector('.info-card');
    expect(info?.textContent).toContain('Étiquetage lot 4471 illisible');
  });

  // --- la preuve de l'action (ADR 0052) --------------------------------------

  it('range la pièce sous son action', () => {
    capa.listActionEvidences.and.returnValue(of([preuveAction()]));
    setup();

    expect(component.actionEvidence(ACTION_ID)?.originalFilename).toBe('constat.pdf');
    const lien = hote().querySelector('.action-evidence') as HTMLAnchorElement;
    expect(lien.textContent).toContain('constat.pdf');
    // Une URL signée pointe hors application : sans noopener, la page ouverte
    // garde une prise sur la nôtre.
    expect(lien.getAttribute('rel')).toContain('noopener');
    expect(lien.getAttribute('target')).toBe('_blank');
  });

  it('propose de joindre quand l\'action n\'a pas encore de preuve', () => {
    setup();
    expect(component.canAttachActionEvidence(ACTION_ID, 'IN_PROGRESS')).toBeTrue();
    expect(hote().querySelector('.attach-btn')).not.toBeNull();
  });

  it('dépose la pièce sur l\'action visée', () => {
    setup(dossier({ actions: [action(), action({ id: 'a2', title: 'Former les opérateurs' })] }));
    capa.uploadActionEvidence.and.returnValue(of(preuveAction()));

    component.triggerActionEvidencePicker(document.createElement('input'), ACTION_ID);
    component.onActionEvidenceSelected(fichier());

    expect(capa.uploadActionEvidence).toHaveBeenCalledWith(CASE_ID, ACTION_ID, jasmine.any(File));
    expect(component.actionEvidence(ACTION_ID)).toBeTruthy();
    expect(component.actionEvidence('a2')).toBeUndefined();
  });

  it('ne tente rien si aucun fichier n\'est choisi', () => {
    setup();
    component.triggerActionEvidencePicker(document.createElement('input'), ACTION_ID);
    component.onActionEvidenceSelected({ target: document.createElement('input') } as unknown as Event);
    expect(capa.uploadActionEvidence).not.toHaveBeenCalled();
  });

  it('distingue les refus de dépôt : trop lourd, format, action déjà pourvue, action disparue', () => {
    setup();
    const messages: string[] = [];
    snack.open.and.callFake((m: string) => { messages.push(m); return {} as never; });

    [413, 400, 409, 404].forEach(status => {
      capa.uploadActionEvidence.and.returnValue(throwError(() => new HttpErrorResponse({ status })));
      component.triggerActionEvidencePicker(document.createElement('input'), ACTION_ID);
      component.onActionEvidenceSelected(fichier());
    });

    expect(messages[0]).toContain('10 Mo');
    expect(messages[1]).toContain('Format refusé');
    expect(messages[2]).toContain('déjà sa preuve');
    expect(messages[3]).toContain('n\'existe plus');
    expect(new Set(messages).size).toBe(4);
  });

  it('annonce le stockage coupé au lieu de proposer un dépôt voué au refus', () => {
    capa.listActionEvidences.and.returnValue(throwError(() => new HttpErrorResponse({
      status: 503, error: { type: 'https://qualitos.io/errors/storage-disabled' }
    })));
    setup();
    expect(component.evidenceStorageDisabled$.value).toBeTrue();
    expect(component.canAttachActionEvidence(ACTION_ID, 'IN_PROGRESS')).toBeFalse();
  });

  it('retire la pièce de l\'action après confirmation', () => {
    capa.listActionEvidences.and.returnValue(of([preuveAction()]));
    capa.deleteActionEvidence.and.returnValue(of(void 0));
    setup();

    component.removeActionEvidence(ACTION_ID, preuveAction());

    expect(capa.deleteActionEvidence).toHaveBeenCalledWith(CASE_ID, ACTION_ID, 'evd-a1');
    expect(component.actionEvidence(ACTION_ID)).toBeUndefined();
  });

  it('garde la pièce quand le retrait échoue, et le dit', () => {
    capa.listActionEvidences.and.returnValue(of([preuveAction()]));
    capa.deleteActionEvidence.and.returnValue(throwError(() => new HttpErrorResponse({ status: 409 })));
    setup();

    component.removeActionEvidence(ACTION_ID, preuveAction());

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
  });

  it('ignore un second geste tant que la preuve travaille', () => {
    setup();
    component.busyEvidenceActionId$.next(ACTION_ID);
    const input = document.createElement('input');
    spyOn(input, 'click');

    component.triggerActionEvidencePicker(input, ACTION_ID);
    component.removeActionEvidence(ACTION_ID, preuveAction());

    expect(input.click).not.toHaveBeenCalled();
    expect(dialog.open).not.toHaveBeenCalled();
    expect(component.canAttachActionEvidence(ACTION_ID, 'IN_PROGRESS')).toBeFalse();
  });

  it('verrouille la preuve sur un dossier clos, sans la cacher', () => {
    capa.listActionEvidences.and.returnValue(of([preuveAction()]));
    setup(dossier({ status: 'CLOSED' }));

    expect(component.canAttachActionEvidence(ACTION_ID, 'CLOSED')).toBeFalse();
    expect(hote().querySelector('.action-evidence')).not.toBeNull();
    expect(hote().querySelector('.evidence-remove')).toBeNull();
  });

  it('nomme l\'action visée dans les libellés d\'accessibilité', () => {
    setup();
    expect(component.attachEvidenceAria('Former')).toContain('Former');
    expect(component.removeEvidenceAria('Former')).toContain('Former');
  });
});
