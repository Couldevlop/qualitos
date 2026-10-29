import { Component, OnDestroy, OnInit } from '@angular/core';
import { FormBuilder } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { PageEvent } from '@angular/material/paginator';
import { ActivatedRoute } from '@angular/router';
import { BehaviorSubject, Subscription, combineLatest, of } from 'rxjs';
import { catchError, debounceTime, finalize, map, startWith, switchMap } from 'rxjs/operators';

import { safeErrorMessage } from '../../../../core/http/error-message';
import { SuppliersService } from '../../suppliers.service';
import {
  NC_SEVERITIES,
  NC_STATUSES,
  attachmentName,
  ncLockReason,
  ncSubject,
  saveBlob
} from '../../suppliers.labels';
import {
  NonConformityFilters,
  NonConformityResponse,
  NonConformitySeverity,
  NonConformityStatus,
  SupplierResponse,
  UpdateNonConformityRequest
} from '../../suppliers.types';
import {
  SupplierDocumentsDialogComponent,
  SupplierDocumentsDialogData
} from '../supplier-documents-dialog/supplier-documents-dialog.component';
import { SuppliersNcDialogComponent } from '../suppliers-nc-dialog/suppliers-nc-dialog.component';

export const NC_PAGE_SIZE = 25;

/**
 * Les non-conformités fournisseur en tableau : définition, statut, suivi du 8D,
 * pièces jointes et fiche PDF de chacune.
 *
 * <p>Une seule page pour deux entrées : sans fournisseur dans l'adresse, toutes
 * les NC du tenant avec un filtre par fournisseur ; avec, celles du fournisseur
 * seul (on y arrive depuis sa fiche). Statut et cases 8D se changent dans la
 * ligne : c'est là qu'on les suit au quotidien.
 */
@Component({
  selector: 'qos-supplier-nc-list',
  templateUrl: './supplier-nc-list.component.html',
  styleUrls: ['./supplier-nc-list.component.scss'],
  standalone: false
})
export class SupplierNcListComponent implements OnInit, OnDestroy {

  readonly statuses = NC_STATUSES;
  readonly severities = NC_SEVERITIES;
  readonly pageSize = NC_PAGE_SIZE;

  /** Le fournisseur de la page, quand elle lui est propre ; null pour la vue transverse. */
  supplier: SupplierResponse | null = null;
  scopedSupplierId: string | null = null;
  /** Les fournisseurs proposés au filtre de la vue transverse. */
  suppliers: SupplierResponse[] = [];

  readonly filters = this.fb.nonNullable.group({
    supplierId: [''],
    status: ['' as NonConformityStatus | ''],
    severity: ['' as NonConformitySeverity | '']
  });

  rows: NonConformityResponse[] = [];
  totalElements = 0;
  pageIndex = 0;
  loading = true;
  error: string | null = null;
  /** Les lignes dont une modification est en cours d'envoi. */
  readonly saving = new Set<string>();

  private readonly page$ = new BehaviorSubject<number>(0);
  private readonly refresh$ = new BehaviorSubject<void>(undefined);
  private readonly subs = new Subscription();

  constructor(
    private readonly route: ActivatedRoute,
    private readonly svc: SuppliersService,
    private readonly fb: FormBuilder,
    private readonly dialog: MatDialog,
    private readonly snack: MatSnackBar
  ) {}

  get columns(): string[] {
    const cols = ['detectedOn'];
    if (!this.scopedSupplierId) cols.push('supplier');
    return cols.concat(['part', 'quantity', 'definition', 'severity', 'status',
      'eightDRequested', 'eightDReceived', 'documents', 'report']);
  }

  ngOnInit(): void {
    this.scopedSupplierId = this.route.snapshot.paramMap.get('id');
    if (this.scopedSupplierId) {
      this.subs.add(this.svc.get(this.scopedSupplierId).pipe(catchError(() => of(null)))
        .subscribe(s => (this.supplier = s)));
    } else {
      this.subs.add(this.svc.list(0, 500).pipe(catchError(() => of(null)))
        .subscribe(p => (this.suppliers = p?.content ?? [])));
    }

    const filters$ = this.filters.valueChanges.pipe(startWith(this.filters.getRawValue()), debounceTime(150));
    // Un filtre qui change ramène à la première page : la page 4 d'un autre filtre n'a pas de sens.
    this.subs.add(this.filters.valueChanges.subscribe(() => {
      if (this.page$.value !== 0) this.page$.next(0);
    }));

    this.subs.add(combineLatest([filters$, this.page$, this.refresh$]).pipe(
      map(([, page]) => ({ filters: this.currentFilters(), page })),
      switchMap(({ filters, page }) => {
        this.loading = true;
        this.error = null;
        this.pageIndex = page;
        return this.svc.listAllNcs(filters, page, this.pageSize).pipe(
          catchError(err => {
            this.error = safeErrorMessage(err, $localize`:@@suppliers.nc-list.load-error:Les non-conformités n'ont pas pu être chargées.`);
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

  get heading(): string {
    return this.scopedSupplierId
      ? $localize`:@@suppliers.nc-list.title-supplier:Non-conformités du fournisseur`
      : $localize`:@@suppliers.nc-list.title:Non-conformités fournisseurs`;
  }

  get hasFilters(): boolean {
    const f = this.filters.getRawValue();
    return !!(f.status || f.severity || f.supplierId);
  }

  clearFilters(): void { this.filters.reset(); }

  onPage(e: PageEvent): void { this.page$.next(e.pageIndex); }

  create(): void {
    if (!this.scopedSupplierId) return;
    this.dialog.open(SuppliersNcDialogComponent, {
      data: { supplierId: this.scopedSupplierId }, autoFocus: 'first-tabbable', restoreFocus: true,
      panelClass: 'qos-dialog-panel'
    }).afterClosed().subscribe(nc => { if (nc) this.refresh$.next(); });
  }

  /** Ouvre la NC pour la modifier : tous ses champs, statut compris. */
  edit(nc: NonConformityResponse): void {
    this.dialog.open(SuppliersNcDialogComponent, {
      data: { supplierId: nc.supplierId, nc }, autoFocus: 'first-tabbable', restoreFocus: true,
      panelClass: 'qos-dialog-panel'
    }).afterClosed().subscribe(saved => { if (saved) this.refresh$.next(); });
  }

  setStatus(nc: NonConformityResponse, status: NonConformityStatus): void {
    if (status === nc.status) return;
    this.patch(nc, { status }, { status: nc.status });
  }

  setEightD(nc: NonConformityResponse, field: 'eightDRequested' | 'eightDReceived', value: boolean): void {
    this.patch(nc, { [field]: value }, { [field]: nc[field] });
  }

  openDocuments(nc: NonConformityResponse): void {
    const data: SupplierDocumentsDialogData = {
      supplierId: nc.supplierId, ownerType: 'NON_CONFORMITY', ownerId: nc.id,
      subject: this.subject(nc), lockReason: ncLockReason(nc)
    };
    this.dialog.open(SupplierDocumentsDialogComponent, {
      data, autoFocus: 'first-tabbable', restoreFocus: true, panelClass: 'qos-dialog-panel'
    }).afterClosed().subscribe(count => { if (count !== undefined) nc.documentCount = count; });
  }

  downloadReport(nc: NonConformityResponse): void {
    this.svc.ncReport(nc.supplierId, nc.id).subscribe({
      next: res => { if (res.body) saveBlob(res.body, attachmentName(res, `nc-fournisseur-${nc.id}.pdf`)); },
      error: err => this.fail(err, $localize`:@@suppliers.nc.report-failed:La fiche NC n'a pas pu être générée.`)
    });
  }

  subject(nc: NonConformityResponse): string {
    const base = ncSubject(nc);
    return nc.supplierCode && !this.scopedSupplierId ? `${nc.supplierCode} · ${base}` : base;
  }

  severityBadge(s: NonConformitySeverity): string { return 'sev sev-' + s.toLowerCase(); }
  ncStatusBadge(s: NonConformityStatus): string { return 'ncs ncs-' + s.toLowerCase(); }
  trackById(_i: number, nc: NonConformityResponse): string { return nc.id; }

  documentsAria(nc: NonConformityResponse): string {
    const count = nc.documentCount;
    return $localize`:@@suppliers.documents.open-aria:Documents joints (${count}:count:)`;
  }

  /**
   * Modifie la ligne tout de suite, puis l'envoie ; un refus la remet comme
   * elle était et le dit.
   */
  private patch(nc: NonConformityResponse, change: UpdateNonConformityRequest,
                previous: Partial<NonConformityResponse>): void {
    Object.assign(nc, change);
    this.saving.add(nc.id);
    this.svc.updateNc(nc.supplierId, nc.id, change)
      .pipe(finalize(() => this.saving.delete(nc.id)))
      .subscribe({
        next: updated => {
          const i = this.rows.findIndex(r => r.id === nc.id);
          if (i >= 0) {
            // Le serveur ne renvoie le fournisseur que pour une liste ; on garde celui de la ligne.
            this.rows[i] = { ...updated, supplierCode: nc.supplierCode, supplierName: nc.supplierName };
            this.rows = [...this.rows];
          }
        },
        error: err => {
          Object.assign(nc, previous);
          this.rows = [...this.rows];
          this.fail(err, $localize`:@@suppliers.nc-list.update-failed:La modification n'a pas été enregistrée.`);
        }
      });
  }

  private currentFilters(): NonConformityFilters {
    const f = this.filters.getRawValue();
    return {
      supplierId: this.scopedSupplierId ?? (f.supplierId || undefined),
      status: f.status || undefined,
      severity: f.severity || undefined
    };
  }

  private fail(err: unknown, fallback: string): void {
    // eslint-disable-next-line no-console
    console.warn('[supplier-nc-list] action failed', (err as { status?: number })?.status);
    this.snack.open(safeErrorMessage(err, fallback), $localize`:@@common.ok:OK`, { duration: 4000 });
  }
}
