import { TestBed } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { ActivatedRoute } from '@angular/router';
import { of, throwError } from 'rxjs';

import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { ProductsService } from '../../products.service';
import {
  ProductComponentResponse,
  ProductOperationResponse,
  ProductResponse
} from '../../products.types';
import { ComponentDialogComponent } from '../component-dialog/component-dialog.component';
import { OperationDialogComponent } from '../operation-dialog/operation-dialog.component';
import { ProductDetailComponent } from './product-detail.component';

/**
 * La fiche produit : ce qu'elle charge, ce qu'elle rouvre, et ce qu'elle fait
 * d'une panne.
 *
 * <p>Ce qui se vérifie ici tient en une phrase : la fiche reste LISIBLE quand
 * une de ses requêtes tombe. Nomenclature, gamme et compteur de révisions sont
 * trois appels distincts ; laisser l'un d'eux vider l'écran ou bloquer le
 * voyant de chargement rendrait la fiche inutilisable pour une panne qui ne la
 * concerne qu'à moitié.
 */
describe('ProductDetailComponent', () => {

  const produit: ProductResponse = {
    id: 'p-1', code: 'REF-4471', designation: 'Support moteur', status: 'ACTIVE',
    createdAt: '2026-08-19T08:00:00Z', updatedAt: '2026-08-19T08:00:00Z'
  } as ProductResponse;
  const obsolete: ProductResponse = { ...produit, status: 'OBSOLETE' } as ProductResponse;

  const piece = {
    id: 'c-1', productId: 'p-1', sequenceNo: 10, reference: 'V-12',
    label: 'Vis', quantity: 4
  } as ProductComponentResponse;
  const operation = {
    id: 'o-1', productId: 'p-1', sequenceNo: 20, code: 'OP20', label: 'Perçage'
  } as ProductOperationResponse;

  let component: ProductDetailComponent;
  let service: jasmine.SpyObj<ProductsService>;
  let snack: jasmine.SpyObj<MatSnackBar>;
  let dialog: jasmine.SpyObj<MatDialog>;
  /** Ce que le prochain dialogue rendra à sa fermeture. */
  let ferme: unknown;

  beforeEach(async () => {
    service = jasmine.createSpyObj<ProductsService>('ProductsService', [
      'get', 'components', 'operations', 'revisionRequests',
      'activate', 'markObsolete', 'deleteComponent', 'deleteOperation'
    ]);
    service.get.and.returnValue(of(produit));
    service.components.and.returnValue(of([piece]));
    service.operations.and.returnValue(of([operation]));
    service.revisionRequests.and.returnValue(of([{ id: 'r-1' }]) as never);

    snack = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);
    ferme = true;
    dialog = jasmine.createSpyObj<MatDialog>('MatDialog', ['open']);
    dialog.open.and.callFake(() => ({ afterClosed: () => of(ferme) }) as never);

    await TestBed.configureTestingModule({
      declarations: [ProductDetailComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        { provide: ProductsService, useValue: service },
        { provide: MatSnackBar, useValue: snack },
        { provide: MatDialog, useValue: dialog },
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: { get: () => 'p-1' } } }
        }
      ]
    }).compileComponents();

    component = TestBed.createComponent(ProductDetailComponent).componentInstance;
  });

  it('charge la fiche, sa nomenclature, sa gamme et son compteur de révisions', () => {
    component.ngOnInit();

    expect(component.productId).toBe('p-1');
    expect(component.product).toEqual(produit);
    expect(component.components).toEqual([piece]);
    expect(component.operations).toEqual([operation]);
    expect(component.pendingRevisions).toBe(1);
    // Le voyant s'éteint : laissé allumé, la fiche resterait grisée sur des
    // données pourtant arrivées.
    expect(component.loading).toBeFalse();
  });

  it('éteint le voyant et le dit quand le produit est introuvable', () => {
    service.get.and.returnValue(throwError(() => new Error('404')));

    component.ngOnInit();

    expect(component.loading).toBeFalse();
    expect(snack.open).toHaveBeenCalled();
    // Les listes ne sont PAS demandées : elles porteraient sur un produit
    // inexistant, et leurs deux échecs enterreraient le vrai message.
    expect(service.components).not.toHaveBeenCalled();
  });

  it('remet le compteur de révisions à zéro plutôt que de masquer la fiche', () => {
    // Le compteur est un confort ; une pastille absente vaut mieux qu'un écran
    // vide parce qu'un appel secondaire est tombé.
    service.revisionRequests.and.returnValue(throwError(() => new Error('500')));

    component.ngOnInit();

    expect(component.pendingRevisions).toBe(0);
    expect(component.product).toEqual(produit);
  });

  it('signale une nomenclature ou une gamme indisponible sans perdre la fiche', () => {
    service.components.and.returnValue(throwError(() => new Error('500')));
    service.operations.and.returnValue(throwError(() => new Error('500')));

    component.ngOnInit();

    expect(component.product).toEqual(produit);
    expect(snack.open).toHaveBeenCalledTimes(2);
  });

  it('ferme la fiche à l’édition quand le produit est obsolète', () => {
    // Un produit obsolète se consulte, ne se modifie plus : c'est ce booléen qui
    // désactive les commandes du gabarit.
    component.product = produit;
    expect(component.editable).toBeTrue();

    component.product = obsolete;
    expect(component.editable).toBeFalse();
  });

  it('remplace le produit affiché après activation et mise en obsolescence', () => {
    component.productId = 'p-1';
    service.activate.and.returnValue(of(obsolete));
    service.markObsolete.and.returnValue(of(obsolete));

    component.activate();
    expect(component.product).toEqual(obsolete);

    component.product = produit;
    component.markObsolete();
    expect(component.product).toEqual(obsolete);
  });

  it('laisse le produit en place quand le changement d’état échoue', () => {
    component.product = produit;
    component.productId = 'p-1';
    service.activate.and.returnValue(throwError(() => new Error('409')));
    service.markObsolete.and.returnValue(throwError(() => new Error('409')));

    component.activate();
    component.markObsolete();

    // Afficher un état non enregistré ferait croire l'inverse de la vérité.
    expect(component.product).toEqual(produit);
    expect(snack.open).toHaveBeenCalledTimes(2);
  });

  it('recharge la nomenclature après un dialogue de composant enregistré', () => {
    component.productId = 'p-1';

    component.addComponent();
    expect(dialog.open).toHaveBeenCalledWith(ComponentDialogComponent, jasmine.objectContaining({
      data: { productId: 'p-1', component: undefined }
    }));
    expect(service.components).toHaveBeenCalledTimes(1);

    component.editComponent(piece);
    expect(dialog.open).toHaveBeenCalledWith(ComponentDialogComponent, jasmine.objectContaining({
      data: { productId: 'p-1', component: piece }
    }));
    expect(service.components).toHaveBeenCalledTimes(2);
  });

  it('ne recharge rien quand le dialogue est abandonné', () => {
    // Un rechargement sur annulation coûterait deux requêtes par échappement.
    ferme = undefined;
    component.productId = 'p-1';

    component.addComponent();
    component.addOperation();

    expect(service.components).not.toHaveBeenCalled();
    expect(service.operations).not.toHaveBeenCalled();
  });

  it('recharge la gamme après un dialogue d’opération enregistré', () => {
    component.productId = 'p-1';

    component.addOperation();
    expect(dialog.open).toHaveBeenCalledWith(OperationDialogComponent, jasmine.objectContaining({
      data: { productId: 'p-1', operation: undefined }
    }));

    component.editOperation(operation);
    expect(dialog.open).toHaveBeenCalledWith(OperationDialogComponent, jasmine.objectContaining({
      data: { productId: 'p-1', operation }
    }));
    expect(service.operations).toHaveBeenCalledTimes(2);
  });

  it('relit la liste après une suppression, et le dit quand elle est refusée', () => {
    component.productId = 'p-1';
    service.deleteComponent.and.returnValue(of(void 0));
    service.deleteOperation.and.returnValue(throwError(() => new Error('409')));

    component.deleteComponent(piece);
    expect(service.deleteComponent).toHaveBeenCalledWith('p-1', 'c-1');
    expect(service.components).toHaveBeenCalledTimes(1);

    component.deleteOperation(operation);
    expect(service.deleteOperation).toHaveBeenCalledWith('p-1', 'o-1');
    // La gamme n'est PAS relue : rien n'a changé côté serveur.
    expect(service.operations).not.toHaveBeenCalled();
    expect(snack.open).toHaveBeenCalled();
  });
});
