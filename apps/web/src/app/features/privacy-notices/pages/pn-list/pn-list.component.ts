import { Component, OnInit } from '@angular/core';
import { FormControl } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { Router } from '@angular/router';
import { BehaviorSubject, Observable, combineLatest, of } from 'rxjs';
import { catchError, finalize, startWith, switchMap, tap } from 'rxjs/operators';

import { safeErrorMessage } from '../../../../core/http/error-message';
import { PrivacyNoticesService } from '../../privacy-notices.service';
import { PrivacyNoticeStatus, PrivacyNoticeView } from '../../privacy-notices.types';
import { PnDialogComponent } from '../pn-dialog/pn-dialog.component';

@Component({
  selector: 'qos-pn-list',
  templateUrl: './pn-list.component.html',
  styleUrls: ['./pn-list.component.scss'],
  standalone: false
})
export class PnListComponent implements OnInit {

  readonly displayedColumns = ['reference', 'title', 'language', 'version', 'status', 'effectiveFrom'];

  readonly statuses: PrivacyNoticeStatus[] = ['DRAFT', 'PUBLISHED', 'ARCHIVED'];
  readonly statusFilter = new FormControl<PrivacyNoticeStatus | ''>('PUBLISHED');

  notices$!: Observable<PrivacyNoticeView[]>;
  loading$ = new BehaviorSubject<boolean>(false);
  error$   = new BehaviorSubject<string | null>(null);

  private readonly refresh$ = new BehaviorSubject<void>(undefined);

  constructor(
    private readonly svc: PrivacyNoticesService,
    private readonly dialog: MatDialog,
    private readonly router: Router
  ) {}

  ngOnInit(): void {
    this.notices$ = combineLatest([
      this.statusFilter.valueChanges.pipe(startWith(this.statusFilter.value)),
      this.refresh$
    ]).pipe(
      tap(() => { this.error$.next(null); queueMicrotask(() => this.loading$.next(true)); }),
      switchMap(([status]) =>
        this.svc.list(status || undefined).pipe(
          catchError(err => {
            // eslint-disable-next-line no-console
            console.warn('[pn-list] failed', err?.status, err?.error?.title);
            this.error$.next(safeErrorMessage(err, 'Erreur lors du chargement.'));
            return of([] as PrivacyNoticeView[]);
          }),
          finalize(() => this.loading$.next(false))
        )
      )
    );
  }

  openCreate(): void {
    const ref = this.dialog.open(PnDialogComponent, {
      autoFocus: 'first-tabbable', restoreFocus: true,
      panelClass: 'qos-dialog-panel'
    });
    ref.afterClosed().subscribe(n => { if (n) this.router.navigate(['/privacy-notices', n.id]); });
  }

  open(n: PrivacyNoticeView): void { this.router.navigate(['/privacy-notices', n.id]); }

  statusBadge(s: PrivacyNoticeStatus): string { return 'badge badge-' + s.toLowerCase(); }
}
