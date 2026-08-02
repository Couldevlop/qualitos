import { Component, OnInit } from '@angular/core';
import { FormControl } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { PageEvent } from '@angular/material/paginator';
import { Router } from '@angular/router';
import { BehaviorSubject, Observable, combineLatest, of } from 'rxjs';
import { catchError, finalize, map, shareReplay, startWith, switchMap, tap } from 'rxjs/operators';

import { deferredView } from '../../../../core/rx/deferred-view';
import { safeErrorMessage } from '../../../../core/http/error-message';
import { AuditsService } from '../../audits.service';
import { AuditPlanResponse, AuditStatus, AuditsPage } from '../../audits.types';
import { AuditsCreateDialogComponent } from '../audits-create-dialog/audits-create-dialog.component';

const PAGE_SIZE_OPTIONS = [10, 20, 50, 100];
const MAX_PAGE_SIZE = 100;

/** Page vide emise quand le chargement echoue : vide la table et remet le compteur a zero. */
const EMPTY_PAGE = { content: [], totalElements: 0, totalPages: 0, number: 0, size: 0 };

@Component({
  selector: 'qos-audits-list',
  templateUrl: './audits-list.component.html',
  styleUrls: ['./audits-list.component.scss'],
  standalone: false
})
export class AuditsListComponent implements OnInit {

  readonly displayedColumns = ['title', 'type', 'standard', 'status', 'score', 'scheduledDate'];
  readonly statusFilter = new FormControl<AuditStatus | ''>('');
  readonly statuses: AuditStatus[] = ['PLANNED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED'];

  readonly pageSizeOptions = PAGE_SIZE_OPTIONS;
  pageIndex = 0;
  pageSize = 20;
  totalElements = 0;

  plans$!: Observable<AuditPlanResponse[]>;
  private readonly loadingState$ = new BehaviorSubject<boolean>(false);
  readonly loading$ = deferredView(this.loadingState$);
  private readonly errorState$ = new BehaviorSubject<string | null>(null);
  readonly error$ = deferredView(this.errorState$);

  // `page$` amorce a lui seul le combineLatest et rejoue a chaque rechargement
  // voulu : un second sujet << refresh >> ferait partir DEUX requetes pour une
  // creation (course entre les deux reponses, la plus ancienne pouvant gagner).
  private readonly page$ = new BehaviorSubject<{ index: number; size: number }>({ index: 0, size: 20 });

  constructor(
    private readonly svc: AuditsService,
    private readonly dialog: MatDialog,
    private readonly router: Router
  ) {}

  ngOnInit(): void {
    this.plans$ = combineLatest([
      this.statusFilter.valueChanges.pipe(startWith(this.statusFilter.value)),
      this.page$
    ]).pipe(
      tap(() => { this.errorState$.next(null); this.loadingState$.next(true); }),
      switchMap(([s, p]) => this.svc.listPlans(p.index, p.size, s || undefined).pipe(
        catchError(err => {
          // eslint-disable-next-line no-console
          console.warn('[audits-list] listPlans failed', err?.status, err?.error?.title);
          this.errorState$.next(safeErrorMessage(err, $localize`:@@common.error-loading:Erreur lors du chargement.`));
          // `return []` renverrait un observable VIDE : RxJS convertit le tableau
          // en source, donc la liste n'emettrait RIEN et la table garderait les
          // lignes precedentes a cote de la banniere d'erreur. On emet une page
          // vide explicite, ce qui vide la table ET remet le compteur a zero.
          return of(EMPTY_PAGE as AuditsPage);
        }),
        finalize(() => this.loadingState$.next(false))
      )),
      map(page => {
        this.totalElements = page.totalElements;
        return page.content;
      }),
      shareReplay({ bufferSize: 1, refCount: false }) // refCount:false : evite la boucle de teardown quand *ngIf loading masque la table
    );
  }

  onPage(e: PageEvent): void {
    this.pageIndex = Math.max(0, e.pageIndex);
    this.pageSize = Math.min(MAX_PAGE_SIZE, Math.max(1, e.pageSize));
    this.page$.next({ index: this.pageIndex, size: this.pageSize });
  }

  openCreate(): void {
    const ref = this.dialog.open(AuditsCreateDialogComponent, {
      autoFocus: 'first-tabbable',
      restoreFocus: true,
      panelClass: 'qos-dialog-panel'
    });
    ref.afterClosed().subscribe(created => {
      if (created) {
        this.pageIndex = 0;
        this.page$.next({ index: 0, size: this.pageSize });
      }
    });
  }

  openPlan(p: AuditPlanResponse): void {
    this.router.navigate(['/audits', p.id]);
  }

  statusBadge(s: AuditStatus): string { return 'badge badge-' + s.toLowerCase(); }
  scoreClass(s?: number): string {
    if (s == null) return 'score';
    if (s >= 85) return 'score score-high';
    if (s >= 70) return 'score score-mid';
    return 'score score-low';
  }
}
