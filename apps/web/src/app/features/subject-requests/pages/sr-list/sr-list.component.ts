import { Component, OnInit } from '@angular/core';
import { FormControl } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { Router } from '@angular/router';
import { BehaviorSubject, Observable, combineLatest, of } from 'rxjs';
import { catchError, finalize, shareReplay, startWith, switchMap, tap } from 'rxjs/operators';

import { deferredView } from '../../../../core/rx/deferred-view';
import { safeErrorMessage } from '../../../../core/http/error-message';
import { SubjectRequestsService } from '../../subject-requests.service';
import {
  SubjectRequestStatus,
  SubjectRequestType,
  SubjectRequestView
} from '../../subject-requests.types';
import { SrReceiveDialogComponent } from '../sr-receive-dialog/sr-receive-dialog.component';

type Mode = 'all' | 'overdue';

@Component({
  selector: 'qos-sr-list',
  templateUrl: './sr-list.component.html',
  styleUrls: ['./sr-list.component.scss'],
  standalone: false
})
export class SrListComponent implements OnInit {

  readonly displayedColumns = ['type', 'subject', 'status', 'receivedAt', 'deadlineAt', 'flags'];

  readonly statuses: SubjectRequestStatus[] = ['RECEIVED', 'IN_PROGRESS', 'COMPLETED', 'REJECTED'];
  readonly statusFilter = new FormControl<SubjectRequestStatus | ''>('');
  readonly modeFilter   = new FormControl<Mode>('all');

  readonly modes: { value: Mode; label: string }[] = [
    { value: 'all',     label: $localize`:@@subject-requests.list.mode-all:Toutes les demandes` },
    { value: 'overdue', label: $localize`:@@subject-requests.list.mode-overdue:En retard uniquement` }
  ];

  rows$!: Observable<SubjectRequestView[]>;
  private readonly loadingState$ = new BehaviorSubject<boolean>(false);
  readonly loading$ = deferredView(this.loadingState$);
  private readonly errorState$ = new BehaviorSubject<string | null>(null);
  readonly error$ = deferredView(this.errorState$);

  private readonly refresh$ = new BehaviorSubject<void>(undefined);

  constructor(
    private readonly svc: SubjectRequestsService,
    private readonly dialog: MatDialog,
    private readonly router: Router
  ) {}

  ngOnInit(): void {
    this.rows$ = combineLatest([
      this.modeFilter.valueChanges.pipe(startWith(this.modeFilter.value)),
      this.statusFilter.valueChanges.pipe(startWith(this.statusFilter.value)),
      this.refresh$
    ]).pipe(
      // loading différé hors de la passe de détection de changements courante :
      // évite NG0100 (le banner *ngIf="loading$ | async" est évalué avant que la
      // souscription synchrone de rows$ ne bascule loading$ à true).
      tap(() => { this.errorState$.next(null); this.loadingState$.next(true); }),
      switchMap(([mode, status]) => {
        const op$ = mode === 'overdue'
          ? this.svc.overdue(200)
          : this.svc.list(status || undefined);
        return op$.pipe(
          catchError(err => {
            // eslint-disable-next-line no-console
            console.warn('[sr-list] failed', err?.status, err?.error?.title);
            this.errorState$.next(safeErrorMessage(err, $localize`:@@common.error-loading:Erreur lors du chargement.`));
            return of([] as SubjectRequestView[]);
          }),
          finalize(() => this.loadingState$.next(false))
        );
      }),
      shareReplay({ bufferSize: 1, refCount: true })
    );
  }

  openReceive(): void {
    const ref = this.dialog.open(SrReceiveDialogComponent, {
      autoFocus: 'first-tabbable', restoreFocus: true,
      panelClass: 'qos-dialog-panel'
    });
    ref.afterClosed().subscribe(r => { if (r) this.router.navigate(['/subject-requests', r.id]); });
  }

  open(r: SubjectRequestView): void { this.router.navigate(['/subject-requests', r.id]); }

  typeLabel(t: SubjectRequestType): string {
    return ({
      ACCESS: $localize`:@@subject-requests.type-short.access:Accès (Art. 15)`,
      ERASURE: $localize`:@@subject-requests.type-short.erasure:Effacement (Art. 17)`,
      PORTABILITY: $localize`:@@subject-requests.type-short.portability:Portabilité (Art. 20)`,
      RECTIFICATION: $localize`:@@subject-requests.type-short.rectification:Rectification (Art. 16)`,
      RESTRICTION: $localize`:@@subject-requests.type-short.restriction:Limitation (Art. 18)`,
      OBJECTION: $localize`:@@subject-requests.type-short.objection:Opposition (Art. 21)`
    })[t];
  }
  typeBadge(t: SubjectRequestType): string { return 'tbadge tbadge-' + t.toLowerCase(); }
  statusBadge(s: SubjectRequestStatus): string { return 'badge badge-' + s.toLowerCase(); }

  isOverdueMode(): boolean { return this.modeFilter.value === 'overdue'; }
}
