import { Component, OnInit } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Router } from '@angular/router';
import { forkJoin, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';

import { ProductsService } from '../../products.service';
import { ProductResponse, ProductStatus } from '../../products.types';
import { ProductFormDialogComponent } from '../product-form-dialog/product-form-dialog.component';

/** Une ligne du tableau : le produit et ce qui, autour de lui, demande une décision. */
export interface ProductRow {
  product: ProductResponse;
  /** Nombre de propositions de révision en attente. Zéro n'affiche rien. */
  pendingRevisions: number;
}

/**
 * Liste des produits.
 *
 * <p>La colonne « À réviser » est le point d'entrée de toute la boucle : elle dit
 * d'un coup d'œil quels produits ont dérivé depuis la dernière revue. Un compteur
 * à zéro n'affiche rien — un badge « 0 » attire l'œil pour rien, et l'œil finit
 * par ne plus voir les autres.
 */
@Component({
  selector: 'qos-products-list',
  templateUrl: './products-list.component.html',
  styleUrls: ['./products-list.component.scss'],
  standalone: false
})
export class ProductsListComponent implements OnInit {

  readonly columns = ['code', 'designation', 'family', 'status', 'revisions', 'actions'];
  readonly statuses: ProductStatus[] = ['DRAFT', 'ACTIVE', 'OBSOLETE'];

  rows: ProductRow[] = [];
  loading = false;
  statusFilter: ProductStatus | '' = '';

  constructor(
    private readonly service: ProductsService,
    private readonly dialog: MatDialog,
    private readonly router: Router,
    private readonly snack: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.reload();
  }

  /** Le filtre travaille sur la liste déjà chargée : aucune requête par frappe. */
  get visibleRows(): ProductRow[] {
    return this.statusFilter
      ? this.rows.filter(row => row.product.status === this.statusFilter)
      : this.rows;
  }

  reload(): void {
    // `loading` est un champ simple, posé AVANT l'abonnement : le patron
    // queueMicrotask ne vaut que pour un Subject poussé depuis une souscription.
    // Ici il produirait l'inverse — un flux synchrone rendrait la main avant la
    // micro-tâche, qui rallumerait le voyant sur une liste déjà chargée.
    this.loading = true;
    this.service.list().subscribe({
      next: products => this.loadRevisionCounts(products),
      error: () => this.fail($localize`:@@product.list-failed:Impossible de charger les produits.`)
    });
  }

  create(): void {
    this.dialog.open(ProductFormDialogComponent, { panelClass: 'qos-dialog-panel' })
      .afterClosed()
      .subscribe(created => { if (created) this.reload(); });
  }

  edit(row: ProductRow, event: MouseEvent): void {
    event.stopPropagation();
    this.dialog.open(ProductFormDialogComponent, {
      panelClass: 'qos-dialog-panel',
      data: { product: row.product }
    }).afterClosed().subscribe(updated => { if (updated) this.reload(); });
  }

  open(row: ProductRow): void {
    this.router.navigate(['/products', row.product.id]);
  }

  /** Un produit obsolète ne se modifie plus : le bouton d'édition disparaît. */
  isEditable(row: ProductRow): boolean {
    return row.product.status !== 'OBSOLETE';
  }

  private loadRevisionCounts(products: ProductResponse[]): void {
    if (products.length === 0) {
      this.rows = [];
      this.loading = false;
      return;
    }
    forkJoin(products.map(product => this.service.revisionRequests(product.id).pipe(
      map(requests => ({ product, pendingRevisions: requests.length })),
      // Le compteur est un confort : s'il tombe, la liste reste utilisable.
      catchError(() => of({ product, pendingRevisions: 0 }))
    ))).subscribe({
      next: rows => { this.rows = rows; this.loading = false; },
      error: () => this.fail($localize`:@@product.list-failed:Impossible de charger les produits.`)
    });
  }

  private fail(message: string): void {
    this.loading = false;
    this.snack.open(message, $localize`:@@common.ok:OK`, { duration: 4000 });
  }
}
