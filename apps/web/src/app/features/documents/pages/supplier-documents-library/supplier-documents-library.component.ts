import { Component, OnDestroy, OnInit } from '@angular/core';
import { FormBuilder } from '@angular/forms';
import { PageEvent } from '@angular/material/paginator';
import { BehaviorSubject, Subscription, combineLatest, of } from 'rxjs';
import { catchError, debounceTime, distinctUntilChanged, finalize, map, startWith, switchMap } from 'rxjs/operators';

import { safeErrorMessage } from '../../../../core/http/error-message';
import { SuppliersService } from '../../../suppliers/suppliers.service';
import {
  DOCUMENT_CATEGORIES,
  OWNER_TYPES,
  documentCategoryLabel,
  documentIcon,
  fileSize,
  ownerTypeLabel
} from '../../../suppliers/suppliers.labels';
import {
  SupplierDocumentCatalogItem,
  SupplierDocumentCategory,
  SupplierDocumentFilters,
  SupplierDocumentOwnerType,
  SupplierResponse
} from '../../../suppliers/suppliers.types';

export const SUPPLIER_DOCUMENTS_PAGE_SIZE = 25;

/**
 * La GED lit aussi les pièces des fournisseurs : 8D, preuves de CAPA, certificats.
 *
 * <p>Lecture seule. Ces pièces naissent et meurent avec la NC ou le certificat qui
 * les porte, et c'est là qu'on les joint ou les retire ; elles n'ont pas de cycle
 * de validation. Ici on les retrouve toutes au même endroit, filtrées, avec le
 * chemin vers leur porteur.
 */
@Component({
  selector: 'qos-supplier-documents-library',
  templateUrl: './supplier-documents-library.component.html',
  styleUrls: ['./supplier-documents-library.component.scss'],
  standalone: false
})
export class SupplierDocumentsLibraryComponent implements OnInit, OnDestroy {

  readonly categories = DOCUMENT_CATEGORIES;
  readonly ownerTypes = OWNER_TYPES;
  readonly pageSize = SUPPLIER_DOCUMENTS_PAGE_SIZE;
  readonly columns = ['name', 'category', 'supplier', 'owner', 'size', 'createdAt'];

  readonly filters = this.fb.nonNullable.group({
    q: [''],
    supplierId: [''],
    ownerType: ['' as SupplierDocumentOwnerType | ''],
    category: ['' as SupplierDocumentCategory | '']
  });

  suppliers: SupplierResponse[] = [];
  rows: SupplierDocumentCatalogItem[] = [];
  totalElements = 0;
  pageIndex = 0;
  loading = true;
  error: string | null = null;

  private readonly page$ = new BehaviorSubject<number>(0);
  private readonly refresh$ = new BehaviorSubject<void>(undefined);
  private readonly subs = new Subscription();

  constructor(
    private readonly svc: SuppliersService,
    private readonly fb: FormBuilder
  ) {}

  ngOnInit(): void {
    this.subs.add(this.svc.list(0, 500).pipe(catchError(() => of(null)))
      .subscribe(p => (this.suppliers = p?.content ?? [])));

    const filters$ = this.filters.valueChanges.pipe(
      startWith(this.filters.getRawValue()),
      debounceTime(250),
      map(() => this.currentFilters()),
      distinctUntilChanged((a, b) => JSON.stringify(a) === JSON.stringify(b))
    );
    // Un filtre qui change ramène à la première page.
    this.subs.add(this.filters.valueChanges.subscribe(() => {
      if (this.page$.value !== 0) this.page$.next(0);
    }));

    this.subs.add(combineLatest([filters$, this.page$, this.refresh$]).pipe(
      switchMap(([filters, page]) => {
        this.loading = true;
        this.error = null;
        this.pageIndex = page;
        return this.svc.listAllDocuments(filters, page, this.pageSize).pipe(
          catchError(err => {
            this.error = safeErrorMessage(err,
              $localize`:@@documents.suppliers.load-error:Les documents fournisseurs n'ont pas pu être chargés.`);
            return of(null);
          }),
          finalize(() => (this.loading = false))
        );
      })
    ).subscribe(page => {
      this.loading = false;
      this.rows = page?.content ?? [];
      this.totalElements = page?.totalElements ?? 0;
    }));
  }

  ngOnDestroy(): void { this.subs.unsubscribe(); }

  get hasFilters(): boolean {
    const f = this.filters.getRawValue();
    return !!(f.q.trim() || f.supplierId || f.ownerType || f.category);
  }

  clearFilters(): void { this.filters.reset(); }

  onPage(e: PageEvent): void { this.page$.next(e.pageIndex); }

  /** Les liens de lecture expirent après quelques minutes : on les redemande. */
  reload(): void { this.refresh$.next(); }

  /** La NC mène à la liste des NC de son fournisseur, le certificat à sa fiche. */
  ownerLink(d: SupplierDocumentCatalogItem): string[] {
    return d.ownerType === 'NON_CONFORMITY'
      ? ['/suppliers', d.supplierId, 'non-conformites']
      : ['/suppliers', d.supplierId];
  }

  categoryLabel(d: SupplierDocumentCatalogItem): string { return documentCategoryLabel(d.category); }
  ownerType(d: SupplierDocumentCatalogItem): string { return ownerTypeLabel(d.ownerType); }
  size(d: SupplierDocumentCatalogItem): string { return fileSize(d.sizeBytes); }
  icon(d: SupplierDocumentCatalogItem): string { return documentIcon(d.contentType); }
  name(d: SupplierDocumentCatalogItem): string {
    return d.originalFilename || $localize`:@@documents.suppliers.unnamed:Document sans nom`;
  }
  trackById(_i: number, d: SupplierDocumentCatalogItem): string { return d.id; }

  private currentFilters(): SupplierDocumentFilters {
    const f = this.filters.getRawValue();
    return {
      q: f.q.trim() || undefined,
      supplierId: f.supplierId || undefined,
      ownerType: f.ownerType || undefined,
      category: f.category || undefined
    };
  }
}
