import { Component, OnInit } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ActivatedRoute, Router } from '@angular/router';
import { BehaviorSubject, Observable, Subject, of } from 'rxjs';
import { catchError, finalize, map, shareReplay, startWith, switchMap, tap } from 'rxjs/operators';

import { AuthService } from '../../../../core/auth/auth.service';
import { safeErrorMessage } from '../../../../core/http/error-message';
import { deferredView } from '../../../../core/rx/deferred-view';
import { ConfirmDialogComponent } from '../../../../shared/ui/confirm-dialog/confirm-dialog.component';
import { ComplaintsService } from '../../complaints.service';
import {
  COMPLAINT_CATEGORY_LABEL, COMPLAINT_CHANNEL_LABEL, COMPLAINT_SEVERITY_LABEL,
  COMPLAINT_STATUS_LABEL, Complaint, ComplaintDetail, ComplaintResponseEntry, ComplaintSeverity,
  ComplaintStatus, UUID_PATTERN, awaitingFirstResponse, canAssignComplaint, canCloseComplaint,
  canDeleteComplaint, canEditComplaint, canRateComplaint, canRejectComplaint, canReopenComplaint,
  canResolveComplaint, canRespondToComplaint
} from '../../complaints.types';
import { ComplaintActionDialogComponent, ComplaintActionMode } from '../complaint-action-dialog/complaint-action-dialog.component';
import { ComplaintEditDialogComponent } from '../complaint-edit-dialog/complaint-edit-dialog.component';
import { ComplaintResponseDialogComponent } from '../complaint-response-dialog/complaint-response-dialog.component';

/**
 * Rôles autorisés à supprimer une ressource qualité (SecurityConfig : DELETE
 * /api/v1/** réservé à ADMIN / ADMIN_TENANT / SUPER_ADMIN / QUALITY_MANAGER).
 * Afficher le bouton à un opérateur ne lui vaudrait qu'un 403.
 */
const DELETE_ROLES = ['admin', 'admin_tenant', 'super_admin', 'quality_manager'];

/**
 * Réclamation client — dossier complet et cycle de vie (§4.9).
 *
 * Chaque action n'est proposée que si le serveur l'accepterait dans l'état
 * courant (cf. règles de cycle de vie dans complaints.types) : un bouton visible
 * doit aboutir. Le cas « résolution bloquée faute de réponse au client » est
 * expliqué à l'écran plutôt que masqué, parce que c'est un blocage qu'on lève
 * en une action.
 */
@Component({
  selector: 'qos-complaint-detail',
  templateUrl: './complaint-detail.component.html',
  styleUrls: ['./complaint-detail.component.scss'],
  standalone: false
})
export class ComplaintDetailComponent implements OnInit {

  detail$!: Observable<ComplaintDetail | null>;

  private readonly loadingState$ = new BehaviorSubject<boolean>(false);
  readonly loading$ = deferredView(this.loadingState$);
  private readonly errorState$ = new BehaviorSubject<string | null>(null);
  readonly error$ = deferredView(this.errorState$);

  /** Action en cours : évite le double-clic et signale l'attente à l'utilisateur. */
  pending = false;

  readonly statusLabel = COMPLAINT_STATUS_LABEL;
  readonly severityLabel = COMPLAINT_SEVERITY_LABEL;
  readonly categoryLabel = COMPLAINT_CATEGORY_LABEL;
  readonly channelLabel = COMPLAINT_CHANNEL_LABEL;

  private readonly reload$ = new Subject<void>();

  constructor(
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly svc: ComplaintsService,
    private readonly auth: AuthService,
    private readonly dialog: MatDialog,
    private readonly snack: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.detail$ = this.route.paramMap.pipe(
      map(p => p.get('id') ?? ''),
      switchMap(id => {
        if (!UUID_PATTERN.test(id)) {
          this.errorState$.next($localize`:@@common.invalid-id:Identifiant invalide.`);
          return of(null);
        }
        return this.reload$.pipe(
          startWith(undefined),
          tap(() => { this.errorState$.next(null); this.loadingState$.next(true); }),
          switchMap(() => this.svc.detail(id).pipe(
            catchError(err => {
              this.errorState$.next(safeErrorMessage(err,
                $localize`:@@complaints.detail.load-error:Réclamation introuvable.`));
              return of(null);
            }),
            finalize(() => this.loadingState$.next(false))
          ))
        );
      }),
      // refCount:false : l'en-tête, la barre d'actions, les cartes et le fil de
      // discussion s'abonnent séparément, certains derrière un *ngIf.
      shareReplay({ bufferSize: 1, refCount: false })
    );
  }

  // ---- Actions ---------------------------------------------------------------

  edit(complaint: Complaint): void {
    this.dialog.open(ComplaintEditDialogComponent, {
      data: { complaint }, panelClass: 'qos-dialog-panel',
      autoFocus: 'first-tabbable', restoreFocus: true
    }).afterClosed().subscribe(updated => { if (updated) this.reload$.next(); });
  }

  respond(complaint: Complaint): void {
    this.dialog.open(ComplaintResponseDialogComponent, {
      data: { complaint }, panelClass: 'qos-dialog-panel',
      autoFocus: 'first-tabbable', restoreFocus: true
    }).afterClosed().subscribe(entry => { if (entry) this.reload$.next(); });
  }

  /** Assignation, rejet, résolution et satisfaction passent par le même dialogue. */
  runAction(complaint: Complaint, mode: ComplaintActionMode): void {
    this.dialog.open(ComplaintActionDialogComponent, {
      data: { complaint, mode }, panelClass: 'qos-dialog-panel',
      autoFocus: 'first-tabbable', restoreFocus: true
    }).afterClosed().subscribe(updated => { if (updated) this.reload$.next(); });
  }

  close(complaint: Complaint): void {
    this.confirm(
      $localize`:@@complaints.detail.close-title:Clôturer la réclamation ?`,
      $localize`:@@complaints.detail.close-message:Le dossier est archivé. Il restera réouvrable si le client revient vers vous.`,
      $localize`:@@complaints.detail.close:Clôturer`,
      false
    ).subscribe(ok => {
      if (!ok) return;
      this.run(this.svc.close(complaint.id),
        $localize`:@@complaints.detail.closed:Réclamation clôturée.`,
        $localize`:@@complaints.detail.close-failed:Clôture impossible.`);
    });
  }

  reopen(complaint: Complaint): void {
    this.confirm(
      $localize`:@@complaints.detail.reopen-title:Rouvrir la réclamation ?`,
      $localize`:@@complaints.detail.reopen-message:Le dossier repart en investigation ; les dates de résolution et de clôture sont effacées.`,
      $localize`:@@complaints.detail.reopen:Rouvrir`,
      false
    ).subscribe(ok => {
      if (!ok) return;
      this.run(this.svc.reopen(complaint.id),
        $localize`:@@complaints.detail.reopened:Réclamation rouverte.`,
        $localize`:@@complaints.detail.reopen-failed:Réouverture impossible.`);
    });
  }

  remove(complaint: Complaint): void {
    this.confirm(
      $localize`:@@complaints.detail.delete-title:Supprimer la réclamation ?`,
      $localize`:@@complaints.detail.delete-message:Suppression définitive du dossier et de son fil de messages.`,
      $localize`:@@common.delete:Supprimer`,
      true
    ).subscribe(ok => {
      if (!ok || this.pending) return;
      this.pending = true;
      this.svc.delete(complaint.id)
        .pipe(finalize(() => (this.pending = false)))
        .subscribe({
          next: () => {
            this.snack.open($localize`:@@complaints.detail.deleted:Réclamation supprimée.`,
              $localize`:@@common.ok:OK`, { duration: 2500 });
            this.router.navigate(['/complaints']);
          },
          error: err => this.fail(err, $localize`:@@common.delete-failed:Suppression impossible.`)
        });
    });
  }

  deleteResponse(complaint: Complaint, entry: ComplaintResponseEntry): void {
    this.confirm(
      $localize`:@@complaints.detail.delete-note-title:Supprimer cette note interne ?`,
      $localize`:@@complaints.detail.delete-note-message:Seules les notes internes sont supprimables ; les messages envoyés au client sont immuables.`,
      $localize`:@@common.delete:Supprimer`,
      true
    ).subscribe(ok => {
      if (!ok) return;
      this.run(this.svc.deleteResponse(complaint.id, entry.id),
        $localize`:@@complaints.detail.note-deleted:Note interne supprimée.`,
        $localize`:@@common.delete-failed:Suppression impossible.`);
    });
  }

  /**
   * Exécute une transition puis recharge le dossier.
   *
   * Le rechargement complet est volontaire : une transition modifie aussi les
   * horodatages et le fil de messages, et le serveur est seul juge de l'état
   * résultant. Recopier la réponse dans la vue donnerait un dossier à demi faux.
   */
  private run(action$: Observable<unknown>, success: string, failure: string): void {
    if (this.pending) return;
    this.pending = true;
    action$.pipe(finalize(() => (this.pending = false))).subscribe({
      next: () => {
        this.snack.open(success, $localize`:@@common.ok:OK`, { duration: 2500 });
        this.reload$.next();
      },
      error: err => this.fail(err, failure)
    });
  }

  private confirm(title: string, message: string, confirmLabel: string, destructive: boolean): Observable<boolean | undefined> {
    return this.dialog.open(ConfirmDialogComponent, {
      data: { title, message, confirmLabel, cancelLabel: $localize`:@@common.cancel:Annuler`, destructive },
      panelClass: 'qos-dialog-panel', autoFocus: 'first-tabbable', restoreFocus: true
    }).afterClosed();
  }

  private fail(err: unknown, fallback: string): void {
    // eslint-disable-next-line no-console
    console.warn('[complaint-detail] action failed', (err as { status?: number })?.status);
    this.snack.open(safeErrorMessage(err, fallback), $localize`:@@common.ok:OK`, { duration: 4000 });
  }

  // ---- Règles d'affichage ----------------------------------------------------

  canEdit(c: Complaint): boolean { return canEditComplaint(c.status); }
  canAssign(c: Complaint): boolean { return canAssignComplaint(c.status); }
  canRespond(c: Complaint): boolean { return canRespondToComplaint(c.status); }
  canReject(c: Complaint): boolean { return canRejectComplaint(c.status); }
  canResolve(c: Complaint): boolean { return canResolveComplaint(c); }
  canClose(c: Complaint): boolean { return canCloseComplaint(c.status); }
  canReopen(c: Complaint): boolean { return canReopenComplaint(c.status); }
  canRate(c: Complaint): boolean { return canRateComplaint(c.status); }
  awaitingResponse(c: Complaint): boolean { return awaitingFirstResponse(c); }

  /** Suppression : autorisée par l'état ET par le rôle porté par le JWT. */
  canDelete(c: Complaint): boolean {
    return canDeleteComplaint(c.status) && this.hasDeleteRole();
  }

  /** Une note interne n'est supprimable que par un profil habilité (403 sinon). */
  canDeleteNote(entry: ComplaintResponseEntry): boolean {
    return entry.internalNote && this.hasDeleteRole();
  }

  private hasDeleteRole(): boolean {
    const roles = this.auth.snapshot()?.roles ?? [];
    return roles.some(r => DELETE_ROLES.includes(r.toLowerCase()));
  }

  statusTone(status: ComplaintStatus): 'neutral' | 'info' | 'success' | 'warn' | 'danger' {
    switch (status) {
      case 'RECEIVED': return 'info';
      case 'UNDER_INVESTIGATION': return 'warn';
      case 'REOPENED': return 'warn';
      case 'RESPONDED': return 'info';
      case 'RESOLVED': return 'success';
      case 'CLOSED': return 'neutral';
      case 'REJECTED': return 'danger';
      default: return 'neutral';
    }
  }

  severityTone(severity: ComplaintSeverity): 'neutral' | 'info' | 'success' | 'warn' | 'danger' {
    switch (severity) {
      case 'LOW': return 'success';
      case 'MEDIUM': return 'info';
      case 'HIGH': return 'warn';
      case 'CRITICAL': return 'danger';
      default: return 'neutral';
    }
  }

  /**
   * Ton de la note de satisfaction, lu comme un NPS simplifié :
   * 9-10 promoteur, 7-8 passif, ≤ 6 détracteur.
   */
  satisfactionTone(score: number | null): 'neutral' | 'success' | 'warn' | 'danger' {
    if (score === null) return 'neutral';
    if (score >= 9) return 'success';
    if (score >= 7) return 'warn';
    return 'danger';
  }

  trackByEntry(_index: number, entry: ComplaintResponseEntry): string {
    return entry.id;
  }
}
