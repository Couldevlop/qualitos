import { Component, OnInit } from '@angular/core';
import { FormControl } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { Router } from '@angular/router';
import { BehaviorSubject, Observable, combineLatest, of } from 'rxjs';
import { catchError, finalize, startWith, switchMap, tap } from 'rxjs/operators';

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
  loading$ = new BehaviorSubject<boolean>(false);
  error$   = new BehaviorSubject<string | null>(null);

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
      tap(() => { this.loading$.next(true); this.error$.next(null); }),
      switchMap(([status]) =>
        this.svc.list(status || undefined).pipe(
          catchError(err => {
            // eslint-disable-next-line no-console
            console.warn('[trx-list] failed', err?.status, err?.error?.title);
            this.error$.next(safeErrorMessage(err, 'Erreur lors du chargement.'));
            return of([] as TransferView[]);
          }),
          finalize(() => this.loading$.next(false))
        )
      )
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
      ADEQUACY_DECISION: 'Adéquation (Art. 45)',
      STANDARD_CONTRACTUAL_CLAUSES: 'SCC (Art. 46.2)',
      BINDING_CORPORATE_RULES: 'BCR (Art. 47)',
      CODE_OF_CONDUCT: 'Code de conduite (Art. 46.2.e)',
      CERTIFICATION: 'Certification (Art. 46.2.f)',
      DEROGATION_ART49: 'Dérogation (Art. 49)'
    })[m];
  }
  mechBadge(m: TransferMechanism): string { return 'mech mech-' + m.toLowerCase(); }
  statusBadge(s: TransferStatus): string { return 'badge badge-' + s.toLowerCase(); }
}
