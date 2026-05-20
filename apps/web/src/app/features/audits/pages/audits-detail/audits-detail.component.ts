import { Component, OnInit } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ActivatedRoute, Router } from '@angular/router';
import { BehaviorSubject, Observable, of, Subject } from 'rxjs';
import { catchError, finalize, switchMap, tap } from 'rxjs/operators';

import { safeErrorMessage } from '../../../../core/http/error-message';
import { ConfirmDialogComponent, ConfirmDialogData } from '../../../../shared/ui/confirm-dialog/confirm-dialog.component';
import { AuditsService } from '../../audits.service';
import { AuditPlanResponse, AuditStatus, ChecklistItemResponse } from '../../audits.types';
import {
  AuditsChecklistDialogComponent,
  AuditsChecklistDialogData
} from '../audits-checklist-dialog/audits-checklist-dialog.component';
import {
  AuditsResponseDialogComponent,
  AuditsResponseDialogData
} from '../audits-response-dialog/audits-response-dialog.component';

const UUID_RE = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

@Component({
  selector: 'qos-audits-detail',
  templateUrl: './audits-detail.component.html',
  styleUrls: ['./audits-detail.component.scss'],
  standalone: false
})
export class AuditsDetailComponent implements OnInit {

  readonly checklistColumns = ['orderIndex', 'question', 'clauseRef', 'weight', 'response', 'actions'];

  plan$!: Observable<AuditPlanResponse | null>;
  loading$ = new BehaviorSubject<boolean>(false);
  error$ = new BehaviorSubject<string | null>(null);
  acting$ = new BehaviorSubject<boolean>(false);

  private planId = '';
  private readonly reload$ = new Subject<void>();
  private isMockId(s: string): boolean { return /^a-?[a-z0-9-]+$/i.test(s) && /^a[0-9-]/i.test(s); }

  constructor(
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly audits: AuditsService,
    private readonly dialog: MatDialog,
    private readonly snack: MatSnackBar
  ) {}

  ngOnInit(): void {
    const raw = this.route.snapshot.paramMap.get('id') ?? '';
    if (!UUID_RE.test(raw) && !this.isMockId(raw)) {
      this.snack.open('Identifiant invalide.', 'OK', { duration: 3000 });
      this.router.navigate(['/audits']);
      return;
    }
    this.planId = raw;
    this.plan$ = this.reload$.pipe(
      tap(() => { this.loading$.next(true); this.error$.next(null); }),
      switchMap(() => this.audits.getPlan(this.planId).pipe(
        catchError(err => {
          // eslint-disable-next-line no-console
          console.warn('[audits-detail] getPlan failed', err?.status, err?.error?.title);
          this.error$.next(safeErrorMessage(err, 'Plan d\'audit introuvable.'));
          return of(null);
        }),
        finalize(() => this.loading$.next(false))
      ))
    );
    this.reload$.next();
  }

  goBack(): void { this.router.navigate(['/audits']); }

  /** OWASP A04 — destructive action gated by a confirm dialog. */
  deletePlan(title: string): void {
    this.dialog.open(ConfirmDialogComponent, {
      data: <ConfirmDialogData>{
        title: 'Supprimer ce plan ?',
        message: `« ${title} », sa checklist et ses findings seront supprimés définitivement.`,
        confirmLabel: 'Supprimer',
        destructive: true
      },
      autoFocus: false,
      restoreFocus: true
    }).afterClosed().subscribe(confirmed => {
      if (!confirmed) return;
      this.audits.deletePlan(this.planId).subscribe({
        next: () => {
          this.snack.open('Plan supprimé.', 'OK', { duration: 2000 });
          this.router.navigate(['/audits']);
        },
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[audits-detail] delete failed', err?.status, err?.error?.title);
          this.snack.open(
            safeErrorMessage(err, 'Erreur lors de la suppression.'),
            'OK', { duration: 4000 }
          );
        }
      });
    });
  }

  openResponse(item: ChecklistItemResponse): void {
    const data: AuditsResponseDialogData = {
      planId: this.planId,
      itemId: item.id,
      question: item.question,
      clauseRef: item.clauseRef,
      initialResponse: item.response,
      initialConformant: item.conformant
    };
    this.dialog
      .open(AuditsResponseDialogComponent, {
        data,
        autoFocus: 'first-tabbable',
        restoreFocus: true,
        panelClass: 'qos-dialog-panel'
      })
      .afterClosed()
      .subscribe(updated => {
        if (updated) this.reload$.next();
      });
  }

  openAddChecklist(): void {
    const data: AuditsChecklistDialogData = { planId: this.planId };
    this.dialog
      .open(AuditsChecklistDialogComponent, {
        data,
        autoFocus: 'first-tabbable',
        restoreFocus: true,
        panelClass: 'qos-dialog-panel'
      })
      .afterClosed()
      .subscribe(item => {
        if (item) this.reload$.next();
      });
  }

  start(): void { this.transition('start'); }
  complete(): void { this.transition('complete'); }
  cancel(): void { this.transition('cancel'); }

  private transition(action: 'start' | 'complete' | 'cancel'): void {
    if (this.acting$.value) return;
    this.acting$.next(true);
    const call =
      action === 'start' ? this.audits.startPlan(this.planId)
      : action === 'complete' ? this.audits.completePlan(this.planId)
      : this.audits.cancelPlan(this.planId);
    call.pipe(finalize(() => this.acting$.next(false))).subscribe({
      next: () => {
        const label = action === 'start' ? 'démarré' : action === 'complete' ? 'clôturé' : 'annulé';
        this.snack.open(`Audit ${label}.`, 'OK', { duration: 2000 });
        this.reload$.next();
      },
      error: err => {
        // eslint-disable-next-line no-console
        console.warn('[audits-detail] transition failed', action, err?.status, err?.error?.title);
        this.snack.open(
          safeErrorMessage(err, 'Erreur lors de la transition.'),
          'OK', { duration: 4000 }
        );
      }
    });
  }

  canStart(s: AuditStatus): boolean { return s === 'PLANNED'; }
  canComplete(s: AuditStatus): boolean { return s === 'IN_PROGRESS'; }
  isTerminal(s: AuditStatus): boolean { return s === 'COMPLETED' || s === 'CANCELLED'; }

  statusBadge(s: AuditStatus): string { return 'badge badge-' + s.toLowerCase(); }
  scoreClass(s?: number): string {
    if (s == null) return 'score';
    if (s >= 85) return 'score score-high';
    if (s >= 70) return 'score score-mid';
    return 'score score-low';
  }
}
