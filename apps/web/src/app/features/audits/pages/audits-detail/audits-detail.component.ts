import { Component, OnInit } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ActivatedRoute, Router } from '@angular/router';
import { BehaviorSubject, Observable, of, Subject } from 'rxjs';
import { catchError, finalize, shareReplay, switchMap, tap } from 'rxjs/operators';

import { deferredView } from '../../../../core/rx/deferred-view';
import { safeErrorMessage } from '../../../../core/http/error-message';
import { ConfirmDialogComponent, ConfirmDialogData } from '../../../../shared/ui/confirm-dialog/confirm-dialog.component';
import { AuditsService } from '../../audits.service';
import {
  AuditsEditDialogComponent,
  AuditsEditDialogData
} from '../audits-edit-dialog/audits-edit-dialog.component';
import {
  AuditsFindingDialogComponent,
  AuditsFindingDialogData
} from '../audits-finding-dialog/audits-finding-dialog.component';
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
  readonly findingColumns = ['type', 'description', 'clauseRef', 'raisedAt'];

  readonly notFoundLabel = $localize`:@@audits.detail.not-found:Audit introuvable`;

  plan$!: Observable<AuditPlanResponse | null>;
  private readonly loadingState$ = new BehaviorSubject<boolean>(false);
  readonly loading$ = deferredView(this.loadingState$);
  private readonly errorState$ = new BehaviorSubject<string | null>(null);
  readonly error$ = deferredView(this.errorState$);
  acting$ = new BehaviorSubject<boolean>(false);

  private planId = '';
  private readonly reload$ = new BehaviorSubject<void>(undefined);
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
      this.snack.open($localize`:@@common.invalid-id:Identifiant invalide.`, $localize`:@@common.ok:OK`, { duration: 3000 });
      this.router.navigate(['/audits']);
      return;
    }
    this.planId = raw;
    this.plan$ = this.reload$.pipe(
      tap(() => { this.errorState$.next(null); this.loadingState$.next(true); }),
      switchMap(() => this.audits.getPlan(this.planId).pipe(
        catchError(err => {
          // eslint-disable-next-line no-console
          console.warn('[audits-detail] getPlan failed', err?.status, err?.error?.title);
          this.errorState$.next(safeErrorMessage(err, $localize`:@@audits.detail.plan-not-found:Plan d'audit introuvable.`));
          return of(null);
        }),
        finalize(() => this.loadingState$.next(false))
      )),
      shareReplay({ bufferSize: 1, refCount: false }) // refCount:false : gated sur loading, evite la boucle teardown
    );
    this.reload$.next();
  }

  goBack(): void { this.router.navigate(['/audits']); }

  openEdit(p: AuditPlanResponse): void {
    const data: AuditsEditDialogData = { plan: p };
    this.dialog
      .open(AuditsEditDialogComponent, {
        data, autoFocus: 'first-tabbable', restoreFocus: true,
        panelClass: 'qos-dialog-panel'
      })
      .afterClosed()
      .subscribe(updated => { if (updated) this.reload$.next(); });
  }

  /** OWASP A04 — destructive action gated by a confirm dialog. */
  deletePlan(title: string): void {
    this.dialog.open(ConfirmDialogComponent, {
      data: <ConfirmDialogData>{
        title: $localize`:@@audits.detail.delete-title:Supprimer ce plan ?`,
        message: $localize`:@@audits.detail.delete-message:« ${title}:title: », sa checklist et ses findings seront supprimés définitivement.`,
        confirmLabel: $localize`:@@common.delete:Supprimer`,
        destructive: true
      },
      autoFocus: false,
      restoreFocus: true
    }).afterClosed().subscribe(confirmed => {
      if (!confirmed) return;
      this.audits.deletePlan(this.planId).subscribe({
        next: () => {
          this.snack.open($localize`:@@audits.detail.deleted:Plan supprimé.`, $localize`:@@common.ok:OK`, { duration: 2000 });
          this.router.navigate(['/audits']);
        },
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[audits-detail] delete failed', err?.status, err?.error?.title);
          this.snack.open(
            safeErrorMessage(err, $localize`:@@common.error-delete:Erreur lors de la suppression.`),
            $localize`:@@common.ok:OK`, { duration: 4000 }
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

  openAddFinding(checklistItemId?: string): void {
    const data: AuditsFindingDialogData = { planId: this.planId, checklistItemId };
    this.dialog
      .open(AuditsFindingDialogComponent, {
        data, autoFocus: 'first-tabbable', restoreFocus: true,
        panelClass: 'qos-dialog-panel'
      })
      .afterClosed()
      .subscribe(finding => { if (finding) this.reload$.next(); });
  }

  findingBadge(type: string): string {
    return 'finding-badge finding-' + type.toLowerCase().replace('_', '-');
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
        const msg = action === 'start'
          ? $localize`:@@audits.detail.started-msg:Audit démarré.`
          : action === 'complete'
            ? $localize`:@@audits.detail.completed-msg:Audit clôturé.`
            : $localize`:@@audits.detail.cancelled-msg:Audit annulé.`;
        this.snack.open(msg, $localize`:@@common.ok:OK`, { duration: 2000 });
        this.reload$.next();
      },
      error: err => {
        // eslint-disable-next-line no-console
        console.warn('[audits-detail] transition failed', action, err?.status, err?.error?.title);
        this.snack.open(
          safeErrorMessage(err, $localize`:@@audits.detail.transition-error:Erreur lors de la transition.`),
          $localize`:@@common.ok:OK`, { duration: 4000 }
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
