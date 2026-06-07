import { Component, OnInit } from '@angular/core';
import { FormControl } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { Router } from '@angular/router';
import { BehaviorSubject, Observable, combineLatest, of } from 'rxjs';
import { catchError, finalize, shareReplay, startWith, switchMap, tap } from 'rxjs/operators';

import { deferredView } from '../../../../core/rx/deferred-view';
import { safeErrorMessage } from '../../../../core/http/error-message';
import { RopaService } from '../../ropa.service';
import {
  LawfulBasis,
  ProcessingActivityStatus,
  ProcessingActivityView
} from '../../ropa.types';
import { RopaDialogComponent } from '../ropa-dialog/ropa-dialog.component';

@Component({
  selector: 'qos-ropa-list',
  templateUrl: './ropa-list.component.html',
  styleUrls: ['./ropa-list.component.scss'],
  standalone: false
})
export class RopaListComponent implements OnInit {

  readonly displayedColumns = ['reference', 'name', 'lawfulBasis', 'controller', 'special', 'status'];

  readonly statuses: ProcessingActivityStatus[] = ['DRAFT', 'ACTIVE', 'ARCHIVED'];
  readonly statusFilter = new FormControl<ProcessingActivityStatus | ''>('ACTIVE');

  activities$!: Observable<ProcessingActivityView[]>;
  private readonly loadingState$ = new BehaviorSubject<boolean>(false);
  readonly loading$ = deferredView(this.loadingState$);
  private readonly errorState$ = new BehaviorSubject<string | null>(null);
  readonly error$ = deferredView(this.errorState$);

  private readonly refresh$ = new BehaviorSubject<void>(undefined);

  constructor(
    private readonly svc: RopaService,
    private readonly dialog: MatDialog,
    private readonly router: Router
  ) {}

  ngOnInit(): void {
    this.activities$ = combineLatest([
      this.statusFilter.valueChanges.pipe(startWith(this.statusFilter.value)),
      this.refresh$
    ]).pipe(
      tap(() => { this.errorState$.next(null); this.loadingState$.next(true); }),
      switchMap(([status]) =>
        this.svc.list(status || undefined).pipe(
          catchError(err => {
            // eslint-disable-next-line no-console
            console.warn('[ropa-list] failed', err?.status, err?.error?.title);
            this.errorState$.next(safeErrorMessage(err, $localize`:@@common.error-loading:Erreur lors du chargement.`));
            return of([] as ProcessingActivityView[]);
          }),
          finalize(() => this.loadingState$.next(false))
        )
      ),
      shareReplay({ bufferSize: 1, refCount: true })
    );
  }

  openCreate(): void {
    const ref = this.dialog.open(RopaDialogComponent, {
      autoFocus: 'first-tabbable', restoreFocus: true,
      panelClass: 'qos-dialog-panel'
    });
    ref.afterClosed().subscribe(a => { if (a) this.refresh$.next(); });
  }

  open(a: ProcessingActivityView): void { this.router.navigate(['/ropa', a.id]); }

  basisLabel(b: LawfulBasis): string {
    return ({
      CONSENT: $localize`:@@ropa.list.basis-consent:Consentement (6.1.a)`,
      CONTRACT: $localize`:@@ropa.list.basis-contract:Contrat (6.1.b)`,
      LEGAL_OBLIGATION: $localize`:@@ropa.list.basis-legal-obligation:Obligation légale (6.1.c)`,
      VITAL_INTERESTS: $localize`:@@ropa.list.basis-vital-interests:Intérêts vitaux (6.1.d)`,
      PUBLIC_TASK: $localize`:@@ropa.list.basis-public-task:Mission de service public (6.1.e)`,
      LEGITIMATE_INTERESTS: $localize`:@@ropa.list.basis-legitimate-interests:Intérêt légitime (6.1.f)`
    })[b];
  }
  basisBadge(b: LawfulBasis): string  { return 'lb lb-' + b.toLowerCase(); }
  statusBadge(s: ProcessingActivityStatus): string { return 'badge badge-' + s.toLowerCase(); }
}
