import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { RouterTestingModule } from '@angular/router/testing';
import { of, throwError } from 'rxjs';

import { ConfirmDialogComponent } from '../../../../shared/ui/confirm-dialog/confirm-dialog.component';
import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { FmeaService } from '../../../fmea/fmea.service';
import {
  FmeaItemDialogComponent
} from '../../../fmea/pages/fmea-item-dialog/fmea-item-dialog.component';
import { FmeaItemResponse, FmeaProjectResponse } from '../../../fmea/fmea.types';
import { ProductsService } from '../../products.service';
import { RevisionRequestView } from '../../products.types';
import { ProductPfmeaTabComponent } from './product-pfmea-tab.component';

/**
 * L'onglet PFMEA de la fiche produit.
 *
 * <p>Le tri est le point qui compte : par priorité d'action PUIS par RPN. Trier
 * par le seul RPN reproduirait le défaut que l'AP corrige, et la ligne la plus
 * grave se retrouverait sous une ligne bénigne au même produit de notes.
 */
describe('ProductPfmeaTabComponent', () => {

  const project: FmeaProjectResponse = {
    id: 'pf-1', tenantId: 't', code: 'PF-4471', name: 'Assemblage',
    type: 'PROCESS_FMEA', status: 'ACTIVE', criticalRpnThreshold: 100, revision: 2,
    createdBy: 'u', createdAt: '2026-08-19T08:00:00Z', updatedAt: '2026-08-19T08:00:00Z'
  };

  const item = (over: Partial<FmeaItemResponse>): FmeaItemResponse => ({
    id: 'i-1', tenantId: 't', projectId: 'pf-1', sequenceNo: 10,
    severity: 5, occurrence: 5, detection: 5, rpn: 125, critical: true,
    createdAt: '2026-08-19T08:00:00Z', updatedAt: '2026-08-19T08:00:00Z', ...over
  });

  let fixture: ComponentFixture<ProductPfmeaTabComponent>;
  let component: ProductPfmeaTabComponent;
  let fmea: jasmine.SpyObj<FmeaService>;
  let products: jasmine.SpyObj<ProductsService>;
  let dialog: jasmine.SpyObj<MatDialog>;
  let snack: { open: jasmine.Spy };

  /** Fait répondre au prochain `dialog.open` ce que l'utilisateur aurait rendu. */
  function dialogRend(valeur: unknown): void {
    dialog.open.and.returnValue({ afterClosed: () => of(valeur) } as never);
  }

  beforeEach(async () => {
    fmea = jasmine.createSpyObj<FmeaService>('FmeaService', ['list', 'listItems', 'deleteItem']);
    products = jasmine.createSpyObj<ProductsService>('ProductsService', ['revisionRequests']);
    dialog = jasmine.createSpyObj<MatDialog>('MatDialog', ['open']);
    snack = { open: jasmine.createSpy('open') };

    await TestBed.configureTestingModule({
      declarations: [ProductPfmeaTabComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule, RouterTestingModule],
      providers: [
        { provide: FmeaService, useValue: fmea },
        { provide: ProductsService, useValue: products },
        { provide: MatDialog, useValue: dialog },
        { provide: MatSnackBar, useValue: snack }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ProductPfmeaTabComponent);
    component = fixture.componentInstance;
    component.productId = 'p-1';
  });

  function page(content: FmeaProjectResponse[]) {
    return of({ content, totalElements: content.length, totalPages: 1, number: 0, size: 50 });
  }

  function itemsPage(content: FmeaItemResponse[]) {
    return of({ content, totalElements: content.length, totalPages: 1, number: 0, size: 100 });
  }

  it('trie par priorité d’action puis par RPN, jamais par le seul RPN', fakeAsync(() => {
    fmea.list.and.returnValue(page([project]));
    fmea.listItems.and.returnValue(itemsPage([
      item({ id: 'low', rpn: 300, actionPriority: 'LOW' }),
      item({ id: 'medium-1', rpn: 40, actionPriority: 'MEDIUM' }),
      item({ id: 'high', rpn: 12, actionPriority: 'HIGH' }),
      item({ id: 'medium-2', rpn: 90, actionPriority: 'MEDIUM' })
    ]));
    products.revisionRequests.and.returnValue(of([]));

    fixture.detectChanges();
    tick();

    expect(component.items.map(i => i.id)).toEqual(['high', 'medium-2', 'medium-1', 'low']);
  }));

  it('relègue une ligne non cotée en fin de tableau', fakeAsync(() => {
    fmea.list.and.returnValue(page([project]));
    fmea.listItems.and.returnValue(itemsPage([
      item({ id: 'unrated', rpn: 0 }),
      item({ id: 'low', rpn: 10, actionPriority: 'LOW' })
    ]));
    products.revisionRequests.and.returnValue(of([]));

    fixture.detectChanges();
    tick();

    expect(component.items.map(i => i.id)).toEqual(['low', 'unrated']);
  }));

  it('marque visiblement une ligne visée par une proposition en attente', fakeAsync(() => {
    fmea.list.and.returnValue(page([project]));
    fmea.listItems.and.returnValue(itemsPage([item({ id: 'i-1', actionPriority: 'HIGH' })]));
    products.revisionRequests.and.returnValue(of([{
      id: 'r-1', productId: 'p-1', targetType: 'PFMEA_ITEM', targetId: 'i-1',
      triggerType: 'NC_CREATED', triggerRefId: 'nc-1', triggerRefLabel: 'NC-2026-0143',
      rationale: '3 NC en 12 mois', status: 'PENDING',
      createdAt: '2026-08-19T08:00:00Z', updatedAt: '2026-08-19T08:00:00Z'
    } as RevisionRequestView]));

    fixture.detectChanges();
    tick();
    fixture.detectChanges();

    expect(component.isFlagged(component.items[0])).toBeTrue();
    expect(fixture.nativeElement.querySelector('tr.flagged')).toBeTruthy();
  }));

  it('affiche un état vide explicite quand aucun PFMEA ne couvre le produit', fakeAsync(() => {
    fmea.list.and.returnValue(page([]));
    products.revisionRequests.and.returnValue(of([]));

    fixture.detectChanges();
    tick();
    fixture.detectChanges();

    expect(component.project).toBeUndefined();
    expect(fixture.nativeElement.textContent).toContain('Aucun PFMEA');
    expect(fmea.listItems).not.toHaveBeenCalled();
  }));

  it('donne le ton rouge à une priorité haute et neutre à l’absence de cotation', () => {
    expect(component.tone('HIGH')).toBe('danger');
    expect(component.tone('MEDIUM')).toBe('warning');
    expect(component.tone('LOW')).toBe('neutral');
    expect(component.tone(undefined)).toBe('neutral');
  });

  // ---------------------------------------------------------------------------
  // La saisie. L'onglet était en lecture seule : corriger une note obligeait à
  // quitter la fiche produit, retrouver le projet dans le module FMEA, et
  // revenir. Les bancs ci-dessous tiennent les deux promesses de ce changement :
  // on édite SANS quitter l'écran, et on édite avec le dialogue du module FMEA
  // et non une copie — sans quoi les deux écrans finiraient par coter
  // différemment la même saisie.
  // ---------------------------------------------------------------------------

  /** Monte l'onglet sur un projet et ses lignes, et rend la main une fois chargé. */
  function charge(projet: FmeaProjectResponse = project,
                  lignes: FmeaItemResponse[] = [item({})]): void {
    fmea.list.and.returnValue(page([projet]));
    fmea.listItems.and.returnValue(itemsPage(lignes));
    products.revisionRequests.and.returnValue(of([]));
    fixture.detectChanges();
    tick();
    fixture.detectChanges();
  }

  it('ajoute une ligne avec le dialogue du module FMEA, et recharge', fakeAsync(() => {
    charge();
    dialogRend(item({ id: 'nouvelle' }));

    component.openAddItem();
    tick();

    const [composant, config] = dialog.open.calls.mostRecent().args as [unknown, { data: unknown }];
    expect(composant).toBe(FmeaItemDialogComponent);
    expect(config.data).toEqual({ projectId: 'pf-1', criticalRpnThreshold: 100 });
    // Deux fois : le chargement initial, puis le rechargement après l'ajout.
    expect(fmea.list).toHaveBeenCalledTimes(2);
  }));

  it('ne recharge rien quand le dialogue d’ajout est annulé', fakeAsync(() => {
    charge();
    dialogRend(undefined);

    component.openAddItem();
    tick();

    expect(fmea.list).toHaveBeenCalledTimes(1);
  }));

  it('passe la ligne au dialogue pour la modifier, et non un formulaire vide', fakeAsync(() => {
    charge();
    const ligne = item({ id: 'i-9', sequenceNo: 90 });
    dialogRend(ligne);

    component.openEditItem(ligne);
    tick();

    const config = dialog.open.calls.mostRecent().args[1] as { data: { item: FmeaItemResponse } };
    expect(config.data.item).toBe(ligne);
    expect(fmea.list).toHaveBeenCalledTimes(2);
  }));

  it('supprime une ligne après confirmation, puis recharge depuis le serveur', fakeAsync(() => {
    charge();
    const ligne = component.items[0];
    dialogRend(true);
    fmea.deleteItem.and.returnValue(of(void 0));

    component.removeItem(ligne);
    tick();

    expect(dialog.open.calls.mostRecent().args[0]).toBe(ConfirmDialogComponent);
    expect(fmea.deleteItem).toHaveBeenCalledWith('pf-1', ligne.id);
    // On recharge au lieu de retirer la ligne du tableau : les rangs et le
    // marquage de révision viennent du serveur.
    expect(fmea.list).toHaveBeenCalledTimes(2);
  }));

  it('ne supprime rien quand la confirmation est refusée', fakeAsync(() => {
    charge();
    dialogRend(false);

    component.removeItem(component.items[0]);
    tick();

    expect(fmea.deleteItem).not.toHaveBeenCalled();
  }));

  it('dit que la suppression a échoué au lieu de laisser croire qu’elle a eu lieu', fakeAsync(() => {
    charge();
    dialogRend(true);
    fmea.deleteItem.and.returnValue(throwError(() => ({ status: 409 })));

    component.removeItem(component.items[0]);
    tick();

    expect(snack.open).toHaveBeenCalled();
    expect(fmea.list).toHaveBeenCalledTimes(1);
  }));

  it('ferme la saisie sur un PFMEA archivé — il se lit, il ne se modifie plus', fakeAsync(() => {
    charge({ ...project, status: 'ARCHIVED' });

    expect(component.editable).toBeFalse();
    const ajouter: HTMLButtonElement =
      fixture.nativeElement.querySelector('[data-test="pfmea-nouvel-item"]');
    const modifier: HTMLButtonElement =
      fixture.nativeElement.querySelector('[data-test="pfmea-modifier-item"]');
    expect(ajouter.disabled).toBeTrue();
    expect(modifier.disabled).toBeTrue();
  }));

  it('n’ouvre rien tant qu’aucun PFMEA ne couvre le produit', fakeAsync(() => {
    fmea.list.and.returnValue(page([]));
    products.revisionRequests.and.returnValue(of([]));
    fixture.detectChanges();
    tick();

    component.openAddItem();
    component.openEditItem(item({}));
    component.removeItem(item({}));

    expect(dialog.open).not.toHaveBeenCalled();
    expect(component.editable).toBeFalse();
  }));

  it('relit la criticité sur le seuil du projet, jamais sur le drapeau de la ligne',
    fakeAsync(() => {
      // `critical` a été calculé à l'écriture ; le seuil du projet a pu bouger
      // depuis. Une ligne à 40 sous un seuil de 100 n'est pas critique, quoi
      // qu'en dise son drapeau.
      charge(project, [item({ id: 'perimee', rpn: 40, critical: true })]);

      expect(component.isCritical(component.items[0])).toBeFalse();
      expect(fixture.nativeElement.querySelector('[data-test="pfmea-creer-risque"]')).toBeNull();
    }));

  it('propose de créer un risque depuis une ligne au-delà du seuil', fakeAsync(() => {
    charge(project, [item({ id: 'grave', rpn: 240, critical: false })]);

    expect(component.isCritical(component.items[0])).toBeTrue();
    expect(fixture.nativeElement.querySelector('[data-test="pfmea-creer-risque"]')).toBeTruthy();
  }));
});
