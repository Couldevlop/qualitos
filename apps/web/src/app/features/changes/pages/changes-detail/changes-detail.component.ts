import { Component, OnInit } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ActivatedRoute, Router } from '@angular/router';
import { BehaviorSubject, Observable, forkJoin, of } from 'rxjs';
import { catchError, switchMap, tap } from 'rxjs/operators';

import { ConfirmDialogComponent } from '../../../../shared/ui/confirm-dialog/confirm-dialog.component';
import { safeErrorMessage } from '../../../../core/http/error-message';
import { ChangesService } from '../../changes.service';
import {
  ApprovalDecision,
  ApprovalResponse,
  ChangeImpactTargetType,
  ChangeRequestPriority,
  ChangeRequestStatus,
  ChangeRequestType,
  ChangeResponse,
  ChangeSummary,
  ImpactResponse
} from '../../changes.types';
import { ChangesApproverDialogComponent } from '../changes-approver-dialog/changes-approver-dialog.component';
import { ChangesCreateDialogComponent } from '../changes-create-dialog/changes-create-dialog.component';
import { ChangesDecisionDialogComponent } from '../changes-decision-dialog/changes-decision-dialog.component';
import { ChangesImpactDialogComponent } from '../changes-impact-dialog/changes-impact-dialog.component';
import { ChangesImplementDialogComponent } from '../changes-implement-dialog/changes-implement-dialog.component';

const UUID_REGEX = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

@Component({
  selector: 'qos-changes-detail',
  templateUrl: './changes-detail.component.html',
  styleUrls: ['./changes-detail.component.scss'],
  standalone: false
})
export class ChangesDetailComponent implements OnInit {

  readonly approvalColumns = ['approverUserId', 'approvalLevel', 'decision', 'comment', 'decidedAt', 'actions'];
  readonly impactColumns   = ['targetType', 'targetId', 'notes', 'createdAt', 'actions'];

  change$!: Observable<ChangeResponse | null>;
  loading$ = new BehaviorSubject<boolean>(false);
  error$   = new BehaviorSubject<string | null>(null);

  summary: ChangeSummary | null = null;
  approvals: ApprovalResponse[] = [];
  impacts: ImpactResponse[] = [];

  private readonly refresh$ = new BehaviorSubject<void>(undefined);
  private changeId = '';

  constructor(
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly svc: ChangesService,
    private readonly dialog: MatDialog,
    private readonly snack: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.change$ = this.route.paramMap.pipe(
      tap(() => { this.error$.next(null); queueMicrotask(() => this.loading$.next(true)); }),
      switchMap(p => {
        const id = p.get('id') ?? '';
        if (!UUID_REGEX.test(id) && !id.startsWith('chg-')) {
          this.error$.next($localize`:@@common.invalid-id:Identifiant invalide.`);
          this.loading$.next(false);
          return of(null);
        }
        this.changeId = id;
        return this.refresh$.pipe(
          switchMap(() => forkJoin({
            change: this.svc.get(id).pipe(catchError(err => {
              this.error$.next(safeErrorMessage(err, $localize`:@@common.error-loading:Erreur lors du chargement.`));
              return of(null);
            })),
            summary: this.svc.summary(id).pipe(catchError(() => of(null))),
            approvals: this.svc.listApprovals(id).pipe(catchError(() => of([]))),
            impacts:  this.svc.listImpacts(id).pipe(catchError(() => of([])))
          })),
          tap(({ summary, approvals, impacts }) => {
            this.loading$.next(false);
            this.summary = summary;
            this.approvals = approvals;
            this.impacts = impacts;
          }),
          switchMap(({ change }) => of(change))
        );
      })
    );
  }

  openEdit(c: ChangeResponse): void {
    const ref = this.dialog.open(ChangesCreateDialogComponent, {
      data: { change: c }, panelClass: 'qos-dialog-panel',
      autoFocus: 'first-tabbable', restoreFocus: true
    });
    ref.afterClosed().subscribe(updated => { if (updated) this.refresh$.next(); });
  }

  submitForReview(c: ChangeResponse): void {
    this.svc.submit(c.id).subscribe({
      next: () => { this.snack.open($localize`:@@changes.detail.submitted:Demande soumise.`, $localize`:@@common.ok:OK`, { duration: 2200 }); this.refresh$.next(); },
      error: err => this.fail(err, $localize`:@@changes.detail.submit-failed:Soumission impossible.`)
    });
  }

  cancel(c: ChangeResponse): void {
    const ref = this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: $localize`:@@changes.detail.cancel-confirm-title:Annuler la demande ?`,
        message: 'La demande « ' + c.title + ' » sera marquée CANCELLED. Transition terminale.',
        confirmLabel: $localize`:@@changes.detail.cancel-confirm-label:Annuler la demande`, cancelLabel: $localize`:@@changes.detail.keep:Conserver`, danger: true
      }
    });
    ref.afterClosed().subscribe(ok => {
      if (!ok) return;
      this.svc.cancel(c.id).subscribe({
        next: () => { this.snack.open($localize`:@@changes.detail.cancelled:Demande annulée.`, $localize`:@@common.ok:OK`, { duration: 2200 }); this.refresh$.next(); },
        error: err => this.fail(err, $localize`:@@changes.detail.cancel-failed:Annulation impossible.`)
      });
    });
  }

  remove(c: ChangeResponse): void {
    const ref = this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: $localize`:@@changes.detail.delete-confirm-title:Supprimer la demande ?`,
        message: 'Suppression définitive de « ' + c.title + ' » avec son historique d\'approbations et d\'impacts.',
        confirmLabel: $localize`:@@common.delete:Supprimer`, cancelLabel: $localize`:@@common.cancel:Annuler`, danger: true
      }
    });
    ref.afterClosed().subscribe(ok => {
      if (!ok) return;
      this.svc.delete(c.id).subscribe({
        next: () => { this.snack.open($localize`:@@changes.detail.deleted:Demande supprimée.`, $localize`:@@common.ok:OK`, { duration: 2200 }); this.router.navigate(['/changes']); },
        error: err => this.fail(err, $localize`:@@common.delete-failed:Suppression impossible.`)
      });
    });
  }

  openImplement(c: ChangeResponse): void {
    const ref = this.dialog.open(ChangesImplementDialogComponent, {
      data: { changeId: c.id }, panelClass: 'qos-dialog-panel',
      autoFocus: 'first-tabbable', restoreFocus: true
    });
    ref.afterClosed().subscribe(updated => { if (updated) this.refresh$.next(); });
  }

  openAddApprover(c: ChangeResponse): void {
    const ref = this.dialog.open(ChangesApproverDialogComponent, {
      data: { changeId: c.id }, panelClass: 'qos-dialog-panel',
      autoFocus: 'first-tabbable', restoreFocus: true
    });
    ref.afterClosed().subscribe(a => { if (a) this.refresh$.next(); });
  }

  removeApprover(c: ChangeResponse, a: ApprovalResponse): void {
    const ref = this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: $localize`:@@changes.detail.remove-approver-title:Retirer l'approbateur ?`,
        message: 'L\'approbateur ' + a.approverUserId.slice(0, 8) + '… sera retiré de ce changement.',
        confirmLabel: $localize`:@@changes.detail.remove:Retirer`, cancelLabel: $localize`:@@common.cancel:Annuler`, danger: true
      }
    });
    ref.afterClosed().subscribe(ok => {
      if (!ok) return;
      this.svc.removeApprover(c.id, a.approverUserId).subscribe({
        next: () => { this.approvals = this.approvals.filter(x => x.id !== a.id); this.refresh$.next(); },
        error: err => this.fail(err, $localize`:@@changes.detail.remove-failed:Retrait impossible.`)
      });
    });
  }

  decide(c: ChangeResponse, decision: 'APPROVED' | 'REJECTED'): void {
    const ref = this.dialog.open(ChangesDecisionDialogComponent, {
      data: { changeId: c.id, decision }, panelClass: 'qos-dialog-panel',
      autoFocus: 'first-tabbable', restoreFocus: true
    });
    ref.afterClosed().subscribe(updated => { if (updated) this.refresh$.next(); });
  }

  openAddImpact(c: ChangeResponse): void {
    const ref = this.dialog.open(ChangesImpactDialogComponent, {
      data: { changeId: c.id }, panelClass: 'qos-dialog-panel',
      autoFocus: 'first-tabbable', restoreFocus: true
    });
    ref.afterClosed().subscribe(im => { if (im) this.refresh$.next(); });
  }

  removeImpact(c: ChangeResponse, im: ImpactResponse): void {
    const ref = this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: $localize`:@@changes.detail.remove-impact-title:Retirer l'impact ?`,
        message: 'L\'entité ' + this.targetLabel(im.targetType) + ' ' + im.targetId.slice(0, 8) + '… sera détachée.',
        confirmLabel: $localize`:@@changes.detail.remove:Retirer`, cancelLabel: $localize`:@@common.cancel:Annuler`, danger: true
      }
    });
    ref.afterClosed().subscribe(ok => {
      if (!ok) return;
      this.svc.removeImpact(c.id, im.id).subscribe({
        next: () => { this.impacts = this.impacts.filter(x => x.id !== im.id); this.refresh$.next(); },
        error: err => this.fail(err, $localize`:@@changes.detail.remove-failed:Retrait impossible.`)
      });
    });
  }

  statusBadge(s: ChangeRequestStatus): string   { return 'badge badge-' + s.toLowerCase(); }
  priorityBadge(p: ChangeRequestPriority): string { return 'prio prio-' + p.toLowerCase(); }
  typeLabel(t: ChangeRequestType): string {
    return ({
      DOCUMENT: $localize`:@@changes.type.document:Document`,
      PROCESS: $localize`:@@changes.type.process:Processus`,
      EQUIPMENT: $localize`:@@changes.type.equipment:Équipement`,
      SUPPLIER: $localize`:@@changes.type.supplier:Fournisseur`,
      IT_SYSTEM: $localize`:@@changes.type.it-system:Système IT`,
      ORGANIZATIONAL: $localize`:@@changes.type.organizational:Organisationnel`,
      OTHER: $localize`:@@changes.type.other:Autre`
    })[t];
  }
  targetLabel(t: ChangeImpactTargetType): string {
    return ({
      DOCUMENT: $localize`:@@changes.target.document:Document`,
      TRAINING_PATH: $localize`:@@changes.target.training-path:Parcours`,
      SUPPLIER: $localize`:@@changes.target.supplier:Fournisseur`,
      IOT_DEVICE: $localize`:@@changes.target.iot-device:IoT`,
      FMEA_PROJECT: $localize`:@@changes.target.fmea-project:FMEA`,
      PDCA_CYCLE: $localize`:@@changes.target.pdca-cycle:PDCA`,
      STANDARD: $localize`:@@changes.target.standard:Norme`,
      OTHER: $localize`:@@changes.target.other:Autre`
    })[t];
  }
  decisionBadge(d: ApprovalDecision): string { return 'dbadge dbadge-' + d.toLowerCase(); }

  private fail(err: unknown, fallback: string): void {
    // eslint-disable-next-line no-console
    console.warn('[changes-detail] action failed', (err as any)?.status, (err as any)?.error?.title);
    this.snack.open(safeErrorMessage(err, fallback), $localize`:@@common.ok:OK`, { duration: 4000 });
  }
}
