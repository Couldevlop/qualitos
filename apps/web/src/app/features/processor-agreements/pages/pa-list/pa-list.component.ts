import { Component, OnInit } from '@angular/core';
import { FormControl } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Router } from '@angular/router';
import { BehaviorSubject, Observable, combineLatest, of } from 'rxjs';
import { catchError, finalize, shareReplay, startWith, switchMap, tap } from 'rxjs/operators';

import { deferredView } from '../../../../core/rx/deferred-view';
import { safeErrorMessage } from '../../../../core/http/error-message';
import { PaService } from '../../pa.service';
import { PaStatus, PaView } from '../../pa.types';
import { PaDialogComponent } from '../pa-dialog/pa-dialog.component';

@Component({
  selector: 'qos-pa-list',
  templateUrl: './pa-list.component.html',
  styleUrls: ['./pa-list.component.scss'],
  standalone: false
})
export class PaListComponent implements OnInit {

  readonly displayedColumns = ['reference', 'processor', 'country', 'breach', 'audit', 'expiration', 'status'];

  readonly statuses: PaStatus[] = ['DRAFT', 'ACTIVE', 'TERMINATED', 'EXPIRED'];
  readonly statusFilter = new FormControl<PaStatus | ''>('ACTIVE');

  rows$!: Observable<PaView[]>;
  private readonly loadingState$ = new BehaviorSubject<boolean>(false);
  readonly loading$ = deferredView(this.loadingState$);
  private readonly errorState$ = new BehaviorSubject<string | null>(null);
  readonly error$ = deferredView(this.errorState$);
  expiring = false;

  private readonly refresh$ = new BehaviorSubject<void>(undefined);

  constructor(
    private readonly svc: PaService,
    private readonly dialog: MatDialog,
    private readonly snack: MatSnackBar,
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
            console.warn('[pa-list] failed', err?.status, err?.error?.title);
            this.errorState$.next(safeErrorMessage(err, $localize`:@@common.error-loading:Erreur lors du chargement.`));
            return of([] as PaView[]);
          }),
          finalize(() => this.loadingState$.next(false))
        )
      ),
      shareReplay({ bufferSize: 1, refCount: true })
    );
  }

  openCreate(): void {
    const ref = this.dialog.open(PaDialogComponent, {
      autoFocus: 'first-tabbable', restoreFocus: true,
      panelClass: 'qos-dialog-panel'
    });
    ref.afterClosed().subscribe(p => { if (p) this.router.navigate(['/processor-agreements', p.id]); });
  }

  expireDue(): void {
    if (this.expiring) return;
    this.expiring = true;
    this.svc.expireDue(200).subscribe({
      next: r => {
        this.expiring = false;
        this.snack.open($localize`:@@dpa.list.maintenance-done:Maintenance : ${r.expired}:count: DPA expiré(s).`, $localize`:@@common.ok:OK`, { duration: 3500 });
        this.refresh$.next();
      },
      error: err => {
        this.expiring = false;
        this.snack.open(safeErrorMessage(err, $localize`:@@dpa.list.maintenance-failed:Maintenance impossible.`), $localize`:@@common.ok:OK`, { duration: 4000 });
      }
    });
  }

  open(p: PaView): void { this.router.navigate(['/processor-agreements', p.id]); }

  statusBadge(s: PaStatus): string { return 'badge badge-' + s.toLowerCase(); }
  isExpiringSoon(p: PaView): boolean {
    if (!p.expirationDate || p.status !== 'ACTIVE') return false;
    return new Date(p.expirationDate).getTime() - Date.now() < 60 * 86400000; // < 60j
  }
}
