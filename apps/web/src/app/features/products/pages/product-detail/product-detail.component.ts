import { Component, OnInit } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ActivatedRoute } from '@angular/router';

import { ProductsService } from '../../products.service';
import {
  ProductComponentResponse,
  ProductOperationResponse,
  ProductResponse
} from '../../products.types';
import { ComponentDialogComponent } from '../component-dialog/component-dialog.component';
import { OperationDialogComponent } from '../operation-dialog/operation-dialog.component';

/**
 * Fiche produit : la synthèse, ce qui le compose, comment il est fabriqué, et
 * les trois documents qui l'analysent.
 *
 * <p>Les onglets d'analyse ne se chargent qu'à l'ouverture : afficher six
 * tableaux d'un coup ferait six requêtes pour cinq écrans que personne ne regarde.
 */
@Component({
  selector: 'qos-product-detail',
  templateUrl: './product-detail.component.html',
  styleUrls: ['./product-detail.component.scss'],
  standalone: false
})
export class ProductDetailComponent implements OnInit {

  productId = '';
  product?: ProductResponse;
  components: ProductComponentResponse[] = [];
  operations: ProductOperationResponse[] = [];
  pendingRevisions = 0;
  loading = false;

  readonly componentColumns = ['sequenceNo', 'reference', 'label', 'quantity', 'actions'];
  readonly operationColumns = ['sequenceNo', 'code', 'label', 'workstation', 'actions'];

  constructor(
    private readonly service: ProductsService,
    private readonly route: ActivatedRoute,
    private readonly dialog: MatDialog,
    private readonly snack: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.productId = this.route.snapshot.paramMap.get('id') ?? '';
    // `loading` est un champ simple, posé AVANT l'abonnement : le patron
    // queueMicrotask ne vaut que pour un Subject poussé depuis une souscription.
    // Ici il produirait l'inverse — un flux synchrone rendrait la main avant la
    // micro-tâche, qui rallumerait le voyant sur une liste déjà chargée.
    this.loading = true;
    this.service.get(this.productId).subscribe({
      next: product => { this.product = product; this.loading = false; this.loadLists(); },
      error: () => this.fail($localize`:@@product.detail-failed:Produit introuvable.`)
    });
    this.refreshRevisionCount();
  }

  get editable(): boolean {
    return this.product?.status !== 'OBSOLETE';
  }

  refreshRevisionCount(): void {
    this.service.revisionRequests(this.productId).subscribe({
      next: requests => (this.pendingRevisions = requests.length),
      // Le compteur est un confort : son échec ne doit pas masquer la fiche.
      error: () => (this.pendingRevisions = 0)
    });
  }

  activate(): void {
    this.service.activate(this.productId).subscribe({
      next: product => (this.product = product),
      error: () => this.fail($localize`:@@product.save-failed:Enregistrement impossible.`)
    });
  }

  markObsolete(): void {
    this.service.markObsolete(this.productId).subscribe({
      next: product => (this.product = product),
      error: () => this.fail($localize`:@@product.save-failed:Enregistrement impossible.`)
    });
  }

  addComponent(): void {
    this.openComponentDialog();
  }

  editComponent(component: ProductComponentResponse): void {
    this.openComponentDialog(component);
  }

  deleteComponent(component: ProductComponentResponse): void {
    this.service.deleteComponent(this.productId, component.id).subscribe({
      next: () => this.loadComponents(),
      error: () => this.fail($localize`:@@product.delete-failed:Suppression impossible.`)
    });
  }

  addOperation(): void {
    this.openOperationDialog();
  }

  editOperation(operation: ProductOperationResponse): void {
    this.openOperationDialog(operation);
  }

  deleteOperation(operation: ProductOperationResponse): void {
    this.service.deleteOperation(this.productId, operation.id).subscribe({
      next: () => this.loadOperations(),
      error: () => this.fail($localize`:@@product.delete-failed:Suppression impossible.`)
    });
  }

  private openComponentDialog(component?: ProductComponentResponse): void {
    this.dialog.open(ComponentDialogComponent, {
      panelClass: 'qos-dialog-panel',
      data: { productId: this.productId, component }
    }).afterClosed().subscribe(saved => { if (saved) this.loadComponents(); });
  }

  private openOperationDialog(operation?: ProductOperationResponse): void {
    this.dialog.open(OperationDialogComponent, {
      panelClass: 'qos-dialog-panel',
      data: { productId: this.productId, operation }
    }).afterClosed().subscribe(saved => { if (saved) this.loadOperations(); });
  }

  private loadLists(): void {
    this.loadComponents();
    this.loadOperations();
  }

  private loadComponents(): void {
    this.service.components(this.productId).subscribe({
      next: components => (this.components = components),
      error: () => this.fail($localize`:@@product.bom-failed:Nomenclature indisponible.`)
    });
  }

  private loadOperations(): void {
    this.service.operations(this.productId).subscribe({
      next: operations => (this.operations = operations),
      error: () => this.fail($localize`:@@product.routing-failed:Gamme indisponible.`)
    });
  }

  private fail(message: string): void {
    this.loading = false;
    this.snack.open(message, $localize`:@@common.ok:OK`, { duration: 4000 });
  }
}
