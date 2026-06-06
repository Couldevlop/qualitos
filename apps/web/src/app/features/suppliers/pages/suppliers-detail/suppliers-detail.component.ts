import { Component, OnInit } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ActivatedRoute, Router } from '@angular/router';
import { BehaviorSubject, Observable, forkJoin, of } from 'rxjs';
import { catchError, shareReplay, switchMap, tap } from 'rxjs/operators';

import { deferredView } from '../../../../core/rx/deferred-view';
import { ConfirmDialogComponent } from '../../../../shared/ui/confirm-dialog/confirm-dialog.component';
import { safeErrorMessage } from '../../../../core/http/error-message';
import { SuppliersService } from '../../suppliers.service';
import {
  AuditResponse,
  CertificateResponse,
  NonConformityResponse,
  NonConformitySeverity,
  NonConformityStatus,
  SupplierResponse,
  SupplierStatistics,
  SupplierStatus,
  SupplierType
} from '../../suppliers.types';
import { SuppliersAuditDialogComponent } from '../suppliers-audit-dialog/suppliers-audit-dialog.component';
import { SuppliersCertDialogComponent } from '../suppliers-cert-dialog/suppliers-cert-dialog.component';
import { SuppliersEditDialogComponent } from '../suppliers-edit-dialog/suppliers-edit-dialog.component';
import { SuppliersNcDialogComponent } from '../suppliers-nc-dialog/suppliers-nc-dialog.component';
import { SuppliersStatusDialogComponent } from '../suppliers-status-dialog/suppliers-status-dialog.component';

const UUID_REGEX = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

@Component({
  selector: 'qos-suppliers-detail',
  templateUrl: './suppliers-detail.component.html',
  styleUrls: ['./suppliers-detail.component.scss'],
  standalone: false
})
export class SuppliersDetailComponent implements OnInit {

  readonly auditColumns = ['auditedOn', 'score', 'critical', 'major', 'minor', 'findingsSummary'];
  readonly ncColumns    = ['detectedOn', 'severity', 'status', 'lotReference', 'description'];
  readonly certColumns  = ['standardCode', 'reference', 'issuedOn', 'expiresOn', 'actions'];

  supplier$!: Observable<SupplierResponse | null>;
  private readonly loadingState$ = new BehaviorSubject<boolean>(false);
  readonly loading$ = deferredView(this.loadingState$);
  private readonly errorState$ = new BehaviorSubject<string | null>(null);
  readonly error$ = deferredView(this.errorState$);

  stats: SupplierStatistics | null = null;
  audits: AuditResponse[] = [];
  ncs: NonConformityResponse[] = [];
  certs: CertificateResponse[] = [];

  private readonly refresh$ = new BehaviorSubject<void>(undefined);
  private supplierId = '';

  constructor(
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly svc: SuppliersService,
    private readonly dialog: MatDialog,
    private readonly snack: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.supplier$ = this.route.paramMap.pipe(
      tap(() => { this.errorState$.next(null); this.loadingState$.next(true); }),
      switchMap(p => {
        const id = p.get('id') ?? '';
        if (!UUID_REGEX.test(id) && !id.startsWith('sup-')) {
          this.errorState$.next('Identifiant invalide.');
          this.loadingState$.next(false);
          return of(null);
        }
        this.supplierId = id;
        return this.refresh$.pipe(
          switchMap(() => forkJoin({
            supplier: this.svc.get(id).pipe(catchError(err => {
              this.errorState$.next(safeErrorMessage(err, 'Erreur lors du chargement.'));
              return of(null);
            })),
            audits: this.svc.listAudits(id).pipe(catchError(() => of({
              content: [], totalElements: 0, totalPages: 0, number: 0, size: 0
            }))),
            ncs: this.svc.listNcs(id).pipe(catchError(() => of({
              content: [], totalElements: 0, totalPages: 0, number: 0, size: 0
            }))),
            certs: this.svc.listCerts(id).pipe(catchError(() => of({
              content: [], totalElements: 0, totalPages: 0, number: 0, size: 0
            }))),
            stats: this.svc.statistics(id).pipe(catchError(() => of(null)))
          })),
          tap(({ audits, ncs, certs, stats }) => {
            this.loadingState$.next(false);
            this.audits = audits.content;
            this.ncs    = ncs.content;
            this.certs  = certs.content;
            this.stats  = stats;
          }),
          switchMap(({ supplier }) => of(supplier))
        );
      }),
      shareReplay({ bufferSize: 1, refCount: true })
    );
  }

  openEdit(s: SupplierResponse): void {
    const ref = this.dialog.open(SuppliersEditDialogComponent, {
      data: { supplier: s }, autoFocus: 'first-tabbable', restoreFocus: true,
      panelClass: 'qos-dialog-panel'
    });
    ref.afterClosed().subscribe(updated => { if (updated) this.refresh$.next(); });
  }

  changeStatus(s: SupplierResponse, target: SupplierStatus): void {
    const reasonRequired = target === 'SUSPENDED' || target === 'BLACKLISTED';
    const ref = this.dialog.open(SuppliersStatusDialogComponent, {
      data: { supplier: s, target, reasonRequired },
      autoFocus: 'first-tabbable', restoreFocus: true,
      panelClass: 'qos-dialog-panel'
    });
    ref.afterClosed().subscribe(updated => { if (updated) this.refresh$.next(); });
  }

  remove(s: SupplierResponse): void {
    const ref = this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: $localize`:@@suppliers.detail.delete-title:Supprimer le fournisseur ?`,
        message: $localize`:@@suppliers.detail.delete-message:Suppression définitive de « ${s.name}:name: » et de tout son historique (audits, NC, certificats).`,
        confirmLabel: $localize`:@@common.delete:Supprimer`, cancelLabel: $localize`:@@common.cancel:Annuler`, danger: true
      }
    });
    ref.afterClosed().subscribe(ok => {
      if (!ok) return;
      this.svc.delete(s.id).subscribe({
        next: () => {
          this.snack.open($localize`:@@suppliers.detail.deleted:Fournisseur supprimé.`, $localize`:@@common.ok:OK`, { duration: 2200 });
          this.router.navigate(['/suppliers']);
        },
        error: err => this.fail(err, $localize`:@@suppliers.detail.delete-failed:Suppression impossible.`)
      });
    });
  }

  openAudit(s: SupplierResponse): void {
    const ref = this.dialog.open(SuppliersAuditDialogComponent, {
      data: { supplierId: s.id }, autoFocus: 'first-tabbable', restoreFocus: true,
      panelClass: 'qos-dialog-panel'
    });
    ref.afterClosed().subscribe(a => { if (a) this.refresh$.next(); });
  }

  openNc(s: SupplierResponse): void {
    const ref = this.dialog.open(SuppliersNcDialogComponent, {
      data: { supplierId: s.id }, autoFocus: 'first-tabbable', restoreFocus: true,
      panelClass: 'qos-dialog-panel'
    });
    ref.afterClosed().subscribe(nc => { if (nc) this.refresh$.next(); });
  }

  openCert(s: SupplierResponse): void {
    const ref = this.dialog.open(SuppliersCertDialogComponent, {
      data: { supplierId: s.id }, autoFocus: 'first-tabbable', restoreFocus: true,
      panelClass: 'qos-dialog-panel'
    });
    ref.afterClosed().subscribe(c => { if (c) this.refresh$.next(); });
  }

  removeCert(s: SupplierResponse, c: CertificateResponse): void {
    const ref = this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: $localize`:@@suppliers.detail.delete-cert-title:Supprimer le certificat ?`,
        message: $localize`:@@suppliers.detail.delete-cert-message:Le certificat ${c.standardCode}:code: (${c.reference}:reference:) sera supprimé.`,
        confirmLabel: $localize`:@@common.delete:Supprimer`, cancelLabel: $localize`:@@common.cancel:Annuler`, danger: true
      }
    });
    ref.afterClosed().subscribe(ok => {
      if (!ok) return;
      this.svc.deleteCert(s.id, c.id).subscribe({
        next: () => { this.certs = this.certs.filter(x => x.id !== c.id); this.refresh$.next(); },
        error: err => this.fail(err, $localize`:@@suppliers.detail.delete-failed:Suppression impossible.`)
      });
    });
  }

  scoreClass(score: number, status: SupplierStatus): string {
    if (status === 'PROSPECT') return 'big-score score-na';
    if (score >= 85) return 'big-score score-good';
    if (score >= 65) return 'big-score score-warn';
    return 'big-score score-bad';
  }

  severityBadge(s: NonConformitySeverity): string { return 'sev sev-' + s.toLowerCase(); }
  ncStatusBadge(s: NonConformityStatus): string   { return 'ncs ncs-' + s.toLowerCase(); }
  statusBadge(s: SupplierStatus): string { return 'badge badge-' + s.toLowerCase(); }
  typeBadge(t: SupplierType): string     { return 'tbadge tbadge-' + t.toLowerCase(); }
  typeLabel(t: SupplierType): string {
    return ({
      RAW_MATERIAL: $localize`:@@suppliers.type.raw-material:Matière première`,
      COMPONENT: $localize`:@@suppliers.type.component:Composant`,
      SERVICE: $localize`:@@suppliers.type.service:Service`,
      CONTRACT_MANUFACTURER: $localize`:@@suppliers.type.contract-manufacturer:Sous-traitant`,
      SOFTWARE: $localize`:@@suppliers.type.software:Logiciel`,
      LOGISTICS: $localize`:@@suppliers.type.logistics:Logistique`,
      OTHER: $localize`:@@suppliers.type.other:Autre`
    })[t];
  }

  private fail(err: unknown, fallback: string): void {
    // eslint-disable-next-line no-console
    console.warn('[suppliers-detail] action failed', (err as any)?.status, (err as any)?.error?.title);
    this.snack.open(safeErrorMessage(err, fallback), $localize`:@@common.ok:OK`, { duration: 4000 });
  }
}
