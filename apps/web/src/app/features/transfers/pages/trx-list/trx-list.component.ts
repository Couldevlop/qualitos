import { Component, OnInit } from '@angular/core';
import { FormControl } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { Router } from '@angular/router';
import { BehaviorSubject, Observable, combineLatest, of } from 'rxjs';
import { catchError, finalize, shareReplay, startWith, switchMap, tap } from 'rxjs/operators';

import { deferredView } from '../../../../core/rx/deferred-view';
import { safeErrorMessage } from '../../../../core/http/error-message';
import { TransfersService } from '../../transfers.service';
import { TransferMechanism, TransferStatus, TransferView } from '../../transfers.types';
import { TrxDialogComponent } from '../trx-dialog/trx-dialog.component';

@Component({
  selector: 'qos-trx-list',
  templateUrl: './trx-list.component.html',
  styleUrls: ['./trx-list.component.scss'],
  standalone: false
})
export class TrxListComponent implements OnInit {

  readonly displayedColumns = ['reference', 'recipient', 'countries', 'mechanism', 'status'];

  readonly statuses: TransferStatus[] = ['DRAFT', 'ACTIVE', 'SUSPENDED', 'TERMINATED'];
  readonly statusFilter = new FormControl<TransferStatus | ''>('ACTIVE');

  rows$!: Observable<TransferView[]>;
  private readonly loadingState$ = new BehaviorSubject<boolean>(false);
  readonly loading$ = deferredView(this.loadingState$);
  private readonly errorState$ = new BehaviorSubject<string | null>(null);
  readonly error$ = deferredView(this.errorState$);

  private readonly refresh$ = new BehaviorSubject<void>(undefined);

  constructor(
    private readonly svc: TransfersService,
    private readonly dialog: MatDialog,
    private readonly router: Router
  ) {}

  ngOnInit(): void {
    this.rows$ = combineLatest([
      this.statusFilter.valueChanges.pipe(startWith(this.statusFilter.value)),
      this.refresh$
    ]).pipe(
      tap(() => { this.errorState$.next(null); this.loadingState$.next(true); }),
      switchMap(([status]) =>
        this.svc.list(status || undefined).pipe(
          catchError(err => {
            // eslint-disable-next-line no-console
            console.warn('[trx-list] failed', err?.status, err?.error?.title);
            this.errorState$.next(safeErrorMessage(err, $localize`:@@common.error-loading:Erreur lors du chargement.`));
            return of([] as TransferView[]);
          }),
          finalize(() => this.loadingState$.next(false))
        )
      ),
      shareReplay({ bufferSize: 1, refCount: true })
    );
  }

  openCreate(): void {
    const ref = this.dialog.open(TrxDialogComponent, {
      autoFocus: 'first-tabbable', restoreFocus: true,
      panelClass: 'qos-dialog-panel'
    });
    ref.afterClosed().subscribe(t => { if (t) this.router.navigate(['/cross-border', t.id]); });
  }

  open(t: TransferView): void { this.router.navigate(['/cross-border', t.id]); }

  mechLabel(m: TransferMechanism): string {
    return ({
      ADEQUACY_DECISION: $localize`:@@transfers.mech-short.adequacy:Adéquation (Art. 45)`,
      STANDARD_CONTRACTUAL_CLAUSES: $localize`:@@transfers.mech-short.scc:SCC (Art. 46.2)`,
      BINDING_CORPORATE_RULES: $localize`:@@transfers.mech-short.bcr:BCR (Art. 47)`,
      CODE_OF_CONDUCT: $localize`:@@transfers.mech-short.coc:Code de conduite (Art. 46.2.e)`,
      CERTIFICATION: $localize`:@@transfers.mech-short.certification:Certification (Art. 46.2.f)`,
      DEROGATION_ART49: $localize`:@@transfers.mech-short.derogation:Dérogation (Art. 49)`
    })[m];
  }
  mechBadge(m: TransferMechanism): string { return 'mech mech-' + m.toLowerCase(); }
  statusBadge(s: TransferStatus): string { return 'badge badge-' + s.toLowerCase(); }
}
