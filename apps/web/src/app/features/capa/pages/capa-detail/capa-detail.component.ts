import { Component, OnInit } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ActivatedRoute, Router } from '@angular/router';
import { BehaviorSubject, Observable, of, Subject } from 'rxjs';
import { catchError, finalize, switchMap, tap } from 'rxjs/operators';

import { safeErrorMessage } from '../../../../core/http/error-message';
import { CapaService } from '../../capa.service';
import { CapaCaseResponse, CapaCriticity, CapaStatus } from '../../capa.types';
import {
  CapaActionDialogComponent,
  CapaActionDialogData
} from '../capa-action-dialog/capa-action-dialog.component';

const UUID_RE = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

@Component({
  selector: 'qos-capa-detail',
  templateUrl: './capa-detail.component.html',
  styleUrls: ['./capa-detail.component.scss'],
  standalone: false
})
export class CapaDetailComponent implements OnInit {

  readonly actionColumns = ['title', 'status', 'dueDate', 'completedAt'];

  case$!: Observable<CapaCaseResponse | null>;
  loading$ = new BehaviorSubject<boolean>(false);
  error$ = new BehaviorSubject<string | null>(null);
  acting$ = new BehaviorSubject<boolean>(false);

  private caseId = '';
  private readonly reload$ = new Subject<void>();
  private isMockId(s: string): boolean { return /^capa-/.test(s); }

  constructor(
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly capa: CapaService,
    private readonly dialog: MatDialog,
    private readonly snack: MatSnackBar
  ) {}

  ngOnInit(): void {
    const raw = this.route.snapshot.paramMap.get('id') ?? '';
    if (!UUID_RE.test(raw) && !this.isMockId(raw)) {
      this.snack.open('Identifiant invalide.', 'OK', { duration: 3000 });
      this.router.navigate(['/capa']);
      return;
    }
    this.caseId = raw;
    this.case$ = this.reload$.pipe(
      tap(() => { this.loading$.next(true); this.error$.next(null); }),
      switchMap(() => this.capa.getCase(this.caseId).pipe(
        catchError(err => {
          // eslint-disable-next-line no-console
          console.warn('[capa-detail] getCase failed', err?.status, err?.error?.title);
          this.error$.next(safeErrorMessage(err, 'Cas CAPA introuvable.'));
          return of(null);
        }),
        finalize(() => this.loading$.next(false))
      ))
    );
    this.reload$.next();
  }

  goBack(): void {
    this.router.navigate(['/capa']);
  }

  openAddAction(): void {
    const data: CapaActionDialogData = { caseId: this.caseId };
    this.dialog
      .open(CapaActionDialogComponent, {
        data,
        autoFocus: 'first-tabbable',
        restoreFocus: true,
        panelClass: 'qos-dialog-panel'
      })
      .afterClosed()
      .subscribe(action => {
        if (action) this.reload$.next();
      });
  }

  start(): void { this.transition('start'); }
  resolve(): void { this.transition('resolve'); }
  reject(): void { this.transition('reject'); }

  private transition(action: 'start' | 'resolve' | 'reject'): void {
    if (this.acting$.value) return;
    this.acting$.next(true);
    const call =
      action === 'start' ? this.capa.startCase(this.caseId)
      : action === 'resolve' ? this.capa.resolveCase(this.caseId)
      : this.capa.rejectCase(this.caseId);
    call.pipe(finalize(() => this.acting$.next(false))).subscribe({
      next: () => {
        const label = action === 'start' ? 'démarré' : action === 'resolve' ? 'résolu' : 'rejeté';
        this.snack.open(`Cas ${label}.`, 'OK', { duration: 2000 });
        this.reload$.next();
      },
      error: err => {
        // eslint-disable-next-line no-console
        console.warn('[capa-detail] transition failed', action, err?.status, err?.error?.title);
        this.snack.open(
          safeErrorMessage(err, 'Erreur lors de la transition.'),
          'OK', { duration: 4000 }
        );
      }
    });
  }

  canStart(s: CapaStatus): boolean { return s === 'OPEN'; }
  canResolve(s: CapaStatus): boolean { return s === 'IN_PROGRESS'; }
  canReject(s: CapaStatus): boolean { return s === 'OPEN' || s === 'IN_PROGRESS'; }
  isTerminal(s: CapaStatus): boolean { return s === 'CLOSED' || s === 'REJECTED'; }

  statusBadge(s: CapaStatus): string { return 'badge badge-' + s.toLowerCase(); }
  criticityBadge(c: CapaCriticity): string { return 'crit crit-' + c.toLowerCase(); }
  actionBadge(s: 'PENDING' | 'IN_PROGRESS' | 'DONE'): string { return 'badge badge-' + s.toLowerCase(); }
}
