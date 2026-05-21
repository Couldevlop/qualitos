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
      tap(() => { this.loading$.next(true); this.error$.next(null); }),
      switchMap(p => {
        const id = p.get('id') ?? '';
        if (!UUID_REGEX.test(id) && !id.startsWith('chg-')) {
          this.error$.next('Identifiant invalide.');
          this.loading$.next(false);
          return of(null);
        }
        this.changeId = id;
        return this.refresh$.pipe(
          switchMap(() => forkJoin({
            change: this.svc.get(id).pipe(catchError(err => {
              this.error$.next(safeErrorMessage(err, 'Erreur lors du chargement.'));
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
      next: () => { this.snack.open('Demande soumise.', 'OK', { duration: 2200 }); this.refresh$.next(); },
      error: err => this.fail(err, 'Soumission impossible.')
    });
  }

  cancel(c: ChangeResponse): void {
    const ref = this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Annuler la demande ?',
        message: 'La demande « ' + c.title + ' » sera marquée CANCELLED. Transition terminale.',
        confirmLabel: 'Annuler la demande', cancelLabel: 'Conserver', danger: true
      }
    });
    ref.afterClosed().subscribe(ok => {
      if (!ok) return;
      this.svc.cancel(c.id).subscribe({
        next: () => { this.snack.open('Demande annulée.', 'OK', { duration: 2200 }); this.refresh$.next(); },
        error: err => this.fail(err, 'Annulation impossible.')
      });
    });
  }

  remove(c: ChangeResponse): void {
    const ref = this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Supprimer la demande ?',
        message: 'Suppression définitive de « ' + c.title + ' » avec son historique d\'approbations et d\'impacts.',
        confirmLabel: 'Supprimer', cancelLabel: 'Annuler', danger: true
      }
    });
    ref.afterClosed().subscribe(ok => {
      if (!ok) return;
      this.svc.delete(c.id).subscribe({
        next: () => { this.snack.open('Demande supprimée.', 'OK', { duration: 2200 }); this.router.navigate(['/changes']); },
        error: err => this.fail(err, 'Suppression impossible.')
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
        title: 'Retirer l\'approbateur ?',
        message: 'L\'approbateur ' + a.approverUserId.slice(0, 8) + '… sera retiré de ce changement.',
        confirmLabel: 'Retirer', cancelLabel: 'Annuler', danger: true
      }
    });
    ref.afterClosed().subscribe(ok => {
      if (!ok) return;
      this.svc.removeApprover(c.id, a.approverUserId).subscribe({
        next: () => { this.approvals = this.approvals.filter(x => x.id !== a.id); this.refresh$.next(); },
        error: err => this.fail(err, 'Retrait impossible.')
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
        title: 'Retirer l\'impact ?',
        message: 'L\'entité ' + this.targetLabel(im.targetType) + ' ' + im.targetId.slice(0, 8) + '… sera détachée.',
        confirmLabel: 'Retirer', cancelLabel: 'Annuler', danger: true
      }
    });
    ref.afterClosed().subscribe(ok => {
      if (!ok) return;
      this.svc.removeImpact(c.id, im.id).subscribe({
        next: () => { this.impacts = this.impacts.filter(x => x.id !== im.id); this.refresh$.next(); },
        error: err => this.fail(err, 'Retrait impossible.')
      });
    });
  }

  statusBadge(s: ChangeRequestStatus): string   { return 'badge badge-' + s.toLowerCase(); }
  priorityBadge(p: ChangeRequestPriority): string { return 'prio prio-' + p.toLowerCase(); }
  typeLabel(t: ChangeRequestType): string {
    return ({
      DOCUMENT: 'Document', PROCESS: 'Processus', EQUIPMENT: 'Équipement',
      SUPPLIER: 'Fournisseur', IT_SYSTEM: 'Système IT', ORGANIZATIONAL: 'Organisationnel',
      OTHER: 'Autre'
    })[t];
  }
  targetLabel(t: ChangeImpactTargetType): string {
    return ({
      DOCUMENT: 'Document', TRAINING_PATH: 'Parcours', SUPPLIER: 'Fournisseur',
      IOT_DEVICE: 'IoT', FMEA_PROJECT: 'FMEA', PDCA_CYCLE: 'PDCA',
      STANDARD: 'Norme', OTHER: 'Autre'
    })[t];
  }
  decisionBadge(d: ApprovalDecision): string { return 'dbadge dbadge-' + d.toLowerCase(); }

  private fail(err: unknown, fallback: string): void {
    // eslint-disable-next-line no-console
    console.warn('[changes-detail] action failed', (err as any)?.status, (err as any)?.error?.title);
    this.snack.open(safeErrorMessage(err, fallback), 'OK', { duration: 4000 });
  }
}
