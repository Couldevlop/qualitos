import { HttpErrorResponse } from '@angular/common/http';
import { Component } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';

import { AuthzService } from '../../../../core/authz/authz.service';
import { DirectoryService } from '../../../../core/directory/directory.service';
import { CapaActionPanelModule } from '../../capa-action-panel.module';
import { CapaService } from '../../capa.service';
import {
  CapaActionResponse, CapaActionStatus, CapaCaseResponse, CapaStatus
} from '../../capa.types';
import { CapaActionPanelComponent } from './capa-action-panel.component';

function action(overrides: Partial<CapaActionResponse> = {}): CapaActionResponse {
  return {
    id: 'act-1',
    capaId: 'capa-1',
    title: 'Remplacer le joint du presse-étoupe',
    status: 'PENDING',
    actionType: 'CORRECTIVE',
    assigneeId: 'u-pilote',
    assigneeName: 'Marie Durand',
    ...overrides
  };
}

function dossier(overrides: Partial<CapaCaseResponse> = {}): CapaCaseResponse {
  return {
    id: 'capa-1',
    tenantId: 't-1',
    title: 'Fuite au presse-étoupe',
    type: 'CORRECTIVE',
    criticity: 'HIGH',
    status: 'IN_PROGRESS',
    sourceType: 'NON_CONFORMITY',
    ownerId: 'u-pilote',
    ownerName: 'Marie Durand',
    dueDate: '2026-12-15',
    createdAt: '2026-11-01T08:00:00Z',
    updatedAt: '2026-11-01T08:00:00Z',
    actions: [action()],
    ...overrides
  };
}

@Component({
  template: `<qos-capa-action-panel [capaCaseId]="id"
                                    (changed)="changements = changements + 1"></qos-capa-action-panel>`,
  standalone: false
})
class HoteComponent {
  id = 'capa-1';
  changements = 0;
}

/**
 * L'action du dossier CAPA, lue et avancée hors de la fiche CAPA.
 *
 * <p>Deux promesses tiennent ici. Le dossier porte <b>plusieurs actions</b>
 * (ADR 0115) : le tableau les montre toutes, une ligne s'ouvre pour se corriger,
 * et le bouton d'ajout en ajoute une. Et une <b>panne se dit</b> : un panneau
 * vide passerait pour « aucune action », ce qui est l'information opposée.
 */
describe('CapaActionPanelComponent', () => {

  let fixture: ComponentFixture<HoteComponent>;
  let hote: HoteComponent;
  let capa: jasmine.SpyObj<CapaService>;
  let dialog: jasmine.SpyObj<MatDialog>;
  let snack: jasmine.SpyObj<MatSnackBar>;
  /** Ce que rend la fenêtre en se refermant : un dossier, ou rien si on abandonne. */
  let ferme: unknown;

  const racine = (): HTMLElement => fixture.nativeElement as HTMLElement;
  const un = (sel: string) => racine().querySelector<HTMLElement>(`[data-test="${sel}"]`);
  const tous = (sel: string) => Array.from(racine().querySelectorAll(`[data-test="${sel}"]`));
  const panneau = (): CapaActionPanelComponent =>
    fixture.debugElement.children[0].componentInstance as CapaActionPanelComponent;

  async function setup(
    opts: { dossier?: CapaCaseResponse; droits?: boolean; panne?: boolean } = {}
  ): Promise<void> {
    TestBed.resetTestingModule();

    capa = jasmine.createSpyObj<CapaService>('CapaService', ['getCase', 'updateAction']);
    capa.getCase.and.returnValue(opts.panne
      ? throwError(() => new HttpErrorResponse({ status: 503 }))
      : of(opts.dossier ?? dossier()));
    capa.updateAction.and.returnValue(of(action({ status: 'IN_PROGRESS' })));

    snack = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);
    ferme = undefined;
    dialog = jasmine.createSpyObj<MatDialog>('MatDialog', ['open']);
    dialog.open.and.callFake((() => ({ afterClosed: () => of(ferme) })) as never);

    const annuaire = jasmine.createSpyObj<DirectoryService>('DirectoryService', ['search']);
    annuaire.search.and.returnValue(
      of({ content: [], number: 0, size: 20, totalElements: 0, totalPages: 0 }) as never);

    await TestBed.configureTestingModule({
      declarations: [HoteComponent],
      imports: [CapaActionPanelModule, NoopAnimationsModule],
      providers: [
        { provide: CapaService, useValue: capa },
        { provide: MatDialog, useValue: dialog },
        { provide: MatSnackBar, useValue: snack },
        { provide: DirectoryService, useValue: annuaire },
        // Tous les droits par défaut : ce banc teste le panneau, pas la matrice (ADR 0078).
        { provide: AuthzService, useValue: { can: () => of(opts.droits ?? true) } }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(HoteComponent);
    hote = fixture.componentInstance;
    fixture.detectChanges();
  }

  // ---------- lecture ----------

  it('lit le dossier dont on lui donne l’identifiant', async () => {
    await setup();

    expect(capa.getCase).toHaveBeenCalledWith('capa-1');
    expect(tous('capa-action-ligne').length).toBe(1);
    expect(un('capa-action-ligne')!.textContent).toContain('Remplacer le joint');
  });

  it('ne charge rien sans identifiant', async () => {
    await setup();
    capa.getCase.calls.reset();

    hote.id = '';
    fixture.detectChanges();

    expect(capa.getCase).not.toHaveBeenCalled();
    expect(tous('capa-action-ligne').length).toBe(0);
  });

  it('nomme la nature de l’action en clair, jamais son code', async () => {
    await setup();

    expect(un('capa-action-ligne')!.textContent).toContain('Corrective');
    expect(un('capa-action-ligne')!.textContent).not.toContain('CORRECTIVE');
  });

  // Le tableau porte des COLONNES : chaque action y dit son porteur et son
  // echeance, meme quand ils sont ceux du dossier. Une cellule laissee vide
  // selon la valeur du dossier rendrait la colonne illisible d'une ligne a
  // l'autre, et c'est precisement ce qu'on vient comparer.
  it('donne le responsable et l’échéance de chaque action', async () => {
    await setup({
      dossier: dossier({
        actions: [
          action(),
          action({ id: 'act-2', title: 'Former l’équipe de nuit', actionType: 'PREVENTIVE',
                   assigneeId: 'u-autre', assigneeName: 'Ali Benali', dueDate: '2026-11-30' })
        ]
      })
    });

    const lignes = tous('capa-action-ligne');
    expect(lignes.length).toBe(2);
    expect(lignes[0].textContent).toContain('Marie Durand');
    expect(lignes[1].textContent).toContain('Ali Benali');
    expect(lignes[1].textContent).toContain('Préventive');
    expect(un('capa-actions-table')!.textContent).toContain('Responsable');
  });

  it('affiche un tiret plutôt qu’un identifiant quand le nom du porteur manque', async () => {
    await setup({
      dossier: dossier({ actions: [action({ assigneeId: 'u-autre', assigneeName: undefined })] })
    });

    const texte = un('capa-action-ligne')!.textContent!;
    expect(texte).toContain('—');
    expect(texte).not.toContain('u-autre');
  });

  // ---------- faire avancer ----------

  it('fait avancer l’action d’un cran, statut seul', async () => {
    await setup();

    un('capa-action-avancer')!.click();
    fixture.detectChanges();

    // Seul le statut part : renvoyer le libellé écraserait une correction faite
    // entre-temps par quelqu'un d'autre.
    expect(capa.updateAction).toHaveBeenCalledWith('capa-1', 'act-1', { status: 'IN_PROGRESS' });
    expect(snack.open).toHaveBeenCalled();
  });

  it('propose de terminer une action déjà démarrée', async () => {
    await setup({ dossier: dossier({ actions: [action({ status: 'IN_PROGRESS' })] }) });

    un('capa-action-avancer')!.click();

    expect(capa.updateAction).toHaveBeenCalledWith('capa-1', 'act-1', { status: 'DONE' });
  });

  it('annonce le changement à l’écran qui l’héberge', async () => {
    await setup();

    un('capa-action-avancer')!.click();

    expect(hote.changements).toBe(1);
  });

  it('relit le dossier après avoir avancé', async () => {
    await setup();
    capa.getCase.calls.reset();

    un('capa-action-avancer')!.click();

    expect(capa.getCase).toHaveBeenCalledTimes(1);
  });

  it('dit l’échec d’une mise à jour', async () => {
    await setup();
    capa.updateAction.and.returnValue(throwError(() => new HttpErrorResponse({ status: 500 })));

    panneau().advance(action());

    expect(snack.open).toHaveBeenCalled();
    expect(hote.changements).toBe(0);
    // Le geste se rejoue : un panneau resté « en vol » après une panne se fige.
    expect(panneau().acting$.value).toBeFalse();
  });

  it('n’offre plus de geste sur une action faite', async () => {
    await setup({ dossier: dossier({ actions: [action({ status: 'DONE' })] }) });

    expect(un('capa-action-avancer')).toBeNull();
    expect(panneau().nextStatus('DONE')).toBeNull();
  });

  // Le serveur tient la même garde : proposer le geste serait proposer un refus.
  it('n’offre aucun geste sur un dossier clos ou rejeté', async () => {
    for (const status of ['CLOSED', 'REJECTED'] as CapaStatus[]) {
      await setup({ dossier: dossier({ status }) });

      expect(panneau().canAdvance(action())).withContext(status).toBeFalse();
      expect((un('capa-action-avancer') as HTMLButtonElement).disabled).withContext(status).toBeTrue();
    }
  });

  it('n’offre aucun geste sans le droit de faire avancer', async () => {
    await setup({ droits: false });

    expect(un('capa-action-avancer')).toBeNull();
    expect(tous('capa-action-ligne').length).toBe(1);
  });

  it('ne lance pas deux mises à jour de front', async () => {
    await setup();
    panneau().acting$.next(true);

    panneau().advance(action());

    expect(capa.updateAction).not.toHaveBeenCalled();
  });

  // ---------- ajouter une action, ouvrir une ligne ----------

  it('dit qu’un dossier sans action ne pourra pas être clôturé', async () => {
    await setup({ dossier: dossier({ actions: [] }) });

    expect(un('capa-action-vide')).not.toBeNull();
    expect(un('capa-action-vide')!.textContent).toContain('clôturé');
    expect(un('capa-action-definir')).not.toBeNull();
  });

  /**
   * Le pilote et l'echeance du dossier sont PROPOSES a l'action nouvelle :
   * c'est le cas le plus frequent, et les retaper serait du recopiage. La
   * fenetre ne recoit pas d'action : c'est ce qui en fait un ajout.
   */
  it('ajoute une action, en proposant le pilote et l’échéance du dossier', async () => {
    await setup({ dossier: dossier({ actions: [] }) });
    ferme = true;

    un('capa-action-definir')!.click();

    expect(dialog.open).toHaveBeenCalled();
    const data = (dialog.open.calls.mostRecent().args[1] as {
      data: { caseId: string; action?: unknown; ownerId?: string; dueDate?: string };
    }).data;
    expect(data.caseId).toBe('capa-1');
    expect(data.action).toBeUndefined();
    expect(data.ownerId).toBe('u-pilote');
    expect(data.dueDate).toBe('2026-12-15');
    expect(hote.changements).toBe(1);
  });

  it('propose d’ajouter une action même quand il y en a déjà une', async () => {
    await setup();

    expect(un('capa-action-ajouter')).not.toBeNull();
    (un('capa-action-ajouter') as HTMLButtonElement).click();

    expect(dialog.open).toHaveBeenCalled();
  });

  it('n’ajoute rien quand la fenêtre est abandonnée', async () => {
    await setup({ dossier: dossier({ actions: [] }) });
    capa.getCase.calls.reset();
    ferme = undefined;

    un('capa-action-definir')!.click();

    expect(hote.changements).toBe(0);
    expect(capa.getCase).not.toHaveBeenCalled();
  });

  it('n’ajoute pas d’action à un dossier clos', async () => {
    await setup({ dossier: dossier({ actions: [], status: 'CLOSED' }) });

    expect((un('capa-action-definir') as HTMLButtonElement).disabled).toBeTrue();
    panneau().addAction();
    expect(dialog.open).not.toHaveBeenCalled();
  });

  /**
   * Une ligne s'ouvre d'un clic : c'est la que l'action se corrige -- un
   * intitule a reformuler, un porteur parti, une echeance a decaler. Avant, il
   * fallait rouvrir un dossier pour changer une phrase.
   */
  it('ouvre l’action cliquée pour la corriger', async () => {
    await setup();
    ferme = true;

    un('capa-action-ligne')!.click();

    const data = (dialog.open.calls.mostRecent().args[1] as {
      data: { caseId: string; action: { id: string }; readOnly?: boolean };
    }).data;
    expect(data.caseId).toBe('capa-1');
    expect(data.action.id).toBe('act-1');
    expect(data.readOnly).toBeFalse();
    expect(hote.changements).toBe(1);
  });

  /**
   * Un dossier termine s'ouvre EN LECTURE SEULE plutot que de ne pas s'ouvrir :
   * relire ce qui a ete fait est legitime apres la cloture, et une ligne qui ne
   * reagit pas au clic passe pour une panne.
   */
  it('ouvre en lecture seule l’action d’un dossier clos', async () => {
    await setup({ dossier: dossier({ status: 'CLOSED' }) });

    un('capa-action-ligne')!.click();

    const data = (dialog.open.calls.mostRecent().args[1] as {
      data: { readOnly?: boolean };
    }).data;
    expect(data.readOnly).toBeTrue();
  });

  /** Avancer d'un cran et corriger l'action sont deux gestes : le bouton ne doit pas ouvrir la ligne. */
  it('ne confond pas l’avancement d’un cran avec l’ouverture de la ligne', async () => {
    await setup();

    un('capa-action-avancer')!.click();

    expect(capa.updateAction).toHaveBeenCalled();
    expect(dialog.open).not.toHaveBeenCalled();
  });

  // ---------- la panne ----------

  it('dit la panne au lieu de laisser croire qu’il n’y a pas d’action', async () => {
    await setup({ panne: true });

    expect(un('capa-action-erreur')).not.toBeNull();
    expect(un('capa-action-vide')).toBeNull();
    expect(tous('capa-action-ligne').length).toBe(0);
  });

  it('se relance après une panne', async () => {
    await setup({ panne: true });
    capa.getCase.and.returnValue(of(dossier()));

    un('capa-action-relancer')!.click();
    fixture.detectChanges();

    expect(un('capa-action-erreur')).toBeNull();
    expect(tous('capa-action-ligne').length).toBe(1);
  });

  // ---------- les anciens dossiers ----------

  // Les dossiers d'avant l'ADR 0088 portent plusieurs actions : on les montre
  // toutes, et on n'en ajoute pas pour autant.
  it('montre toutes les actions d’un ancien dossier', async () => {
    await setup({
      dossier: dossier({
        actions: [action(), action({ id: 'act-2', title: 'Revoir la gamme de montage' })]
      })
    });

    expect(tous('capa-action-ligne').length).toBe(2);
    expect(un('capa-action-definir')).toBeNull();
  });

  it('nomme chaque statut en clair', async () => {
    await setup();

    const libelles = (['PENDING', 'IN_PROGRESS', 'DONE'] as CapaActionStatus[])
      .map(s => panneau().actionStatusLabel(s));
    expect(new Set(libelles).size).toBe(3);
    expect(libelles.every(l => l.length > 0)).toBeTrue();
  });
});
