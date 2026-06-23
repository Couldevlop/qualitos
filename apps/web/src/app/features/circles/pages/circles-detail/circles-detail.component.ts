import { Component, OnInit } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ActivatedRoute, Router } from '@angular/router';
import { BehaviorSubject, Observable, of, Subject } from 'rxjs';
import { catchError, finalize, shareReplay, switchMap, tap } from 'rxjs/operators';

import { deferredView } from '../../../../core/rx/deferred-view';
import { safeErrorMessage } from '../../../../core/http/error-message';
import { ConfirmDialogComponent, ConfirmDialogData } from '../../../../shared/ui/confirm-dialog/confirm-dialog.component';
import { CirclesService } from '../../circles.service';
import {
  CirclesEditDialogComponent,
  CirclesEditDialogData
} from '../circles-edit-dialog/circles-edit-dialog.component';
import {
  CirclesMeetingDialogComponent,
  CirclesMeetingDialogData
} from '../circles-meeting-dialog/circles-meeting-dialog.component';
import {
  CirclesProposalDialogComponent,
  CirclesProposalDialogData
} from '../circles-proposal-dialog/circles-proposal-dialog.component';
import { CircleResponse, CircleStatus } from '../../circles.types';
import {
  CirclesMemberDialogComponent,
  CirclesMemberDialogData
} from '../circles-member-dialog/circles-member-dialog.component';

const UUID_RE = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

@Component({
  selector: 'qos-circles-detail',
  templateUrl: './circles-detail.component.html',
  styleUrls: ['./circles-detail.component.scss'],
  standalone: false
})
export class CirclesDetailComponent implements OnInit {

  readonly circleNotFoundLabel = $localize`:@@circles.detail.not-found:Cercle introuvable`;

  readonly memberColumns = ['role', 'userId', 'joinedAt'];
  readonly meetingColumns = ['title', 'scheduledAt', 'status', 'location'];
  readonly proposalColumns = ['title', 'status', 'proposedBy'];

  circle$!: Observable<CircleResponse | null>;
  private readonly loadingState$ = new BehaviorSubject<boolean>(false);
  readonly loading$ = deferredView(this.loadingState$);
  private readonly errorState$ = new BehaviorSubject<string | null>(null);
  readonly error$ = deferredView(this.errorState$);
  acting$ = new BehaviorSubject<boolean>(false);

  private circleId = '';
  private readonly reload$ = new BehaviorSubject<void>(undefined);
  private isMockId(s: string): boolean { return /^c[0-9-]/.test(s); }

  constructor(
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly circles: CirclesService,
    private readonly dialog: MatDialog,
    private readonly snack: MatSnackBar
  ) {}

  ngOnInit(): void {
    const raw = this.route.snapshot.paramMap.get('id') ?? '';
    if (!UUID_RE.test(raw) && !this.isMockId(raw)) {
      this.snack.open($localize`:@@circles.detail.invalid-id:Identifiant invalide.`, $localize`:@@common.ok:OK`, { duration: 3000 });
      this.router.navigate(['/circles']);
      return;
    }
    this.circleId = raw;
    this.circle$ = this.reload$.pipe(
      tap(() => { this.errorState$.next(null); this.loadingState$.next(true); }),
      switchMap(() => this.circles.getCircle(this.circleId).pipe(
        catchError(err => {
          // eslint-disable-next-line no-console
          console.warn('[circles-detail] getCircle failed', err?.status, err?.error?.title);
          this.errorState$.next(safeErrorMessage(err, $localize`:@@circles.detail.load-error:Cercle introuvable.`));
          return of(null);
        }),
        finalize(() => this.loadingState$.next(false))
      )),
      shareReplay({ bufferSize: 1, refCount: true })
    );
    this.reload$.next();
  }

  goBack(): void { this.router.navigate(['/circles']); }

  openEdit(c: CircleResponse): void {
    const data: CirclesEditDialogData = { circle: c };
    this.dialog
      .open(CirclesEditDialogComponent, {
        data, autoFocus: 'first-tabbable', restoreFocus: true,
        panelClass: 'qos-dialog-panel'
      })
      .afterClosed()
      .subscribe(updated => { if (updated) this.reload$.next(); });
  }

  /** OWASP A04 — destructive action gated by a confirm dialog. */
  deleteCircle(name: string): void {
    this.dialog.open(ConfirmDialogComponent, {
      data: <ConfirmDialogData>{
        title: $localize`:@@circles.detail.delete-confirm-title:Supprimer ce cercle ?`,
        message: $localize`:@@circles.detail.delete-confirm-message:« ${name}:name: », ses membres, réunions et propositions seront supprimés définitivement.`,
        confirmLabel: $localize`:@@common.delete:Supprimer`,
        destructive: true
      },
      autoFocus: false,
      restoreFocus: true
    }).afterClosed().subscribe(confirmed => {
      if (!confirmed) return;
      this.circles.deleteCircle(this.circleId).subscribe({
        next: () => {
          this.snack.open($localize`:@@circles.detail.deleted:Cercle supprimé.`, $localize`:@@common.ok:OK`, { duration: 2000 });
          this.router.navigate(['/circles']);
        },
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[circles-detail] delete failed', err?.status, err?.error?.title);
          this.snack.open(
            safeErrorMessage(err, $localize`:@@circles.detail.delete-error:Erreur lors de la suppression.`),
            'OK', { duration: 4000 }
          );
        }
      });
    });
  }

  openAddMeeting(): void {
    const data: CirclesMeetingDialogData = { circleId: this.circleId };
    this.dialog
      .open(CirclesMeetingDialogComponent, {
        data, autoFocus: 'first-tabbable', restoreFocus: true,
        panelClass: 'qos-dialog-panel'
      })
      .afterClosed()
      .subscribe(m => { if (m) this.reload$.next(); });
  }

  openAddProposal(): void {
    const data: CirclesProposalDialogData = { circleId: this.circleId };
    this.dialog
      .open(CirclesProposalDialogComponent, {
        data, autoFocus: 'first-tabbable', restoreFocus: true,
        panelClass: 'qos-dialog-panel'
      })
      .afterClosed()
      .subscribe(p => { if (p) this.reload$.next(); });
  }

  proposalBadge(status: string): string {
    return 'proposal-badge proposal-' + status.toLowerCase();
  }

  meetingBadge(status: string): string {
    return 'meeting-badge meeting-' + status.toLowerCase();
  }

  openAddMember(): void {
    const data: CirclesMemberDialogData = { circleId: this.circleId };
    this.dialog
      .open(CirclesMemberDialogComponent, {
        data,
        autoFocus: 'first-tabbable',
        restoreFocus: true,
        panelClass: 'qos-dialog-panel'
      })
      .afterClosed()
      .subscribe(member => {
        if (member) this.reload$.next();
      });
  }

  pause(): void { this.transition('pause'); }
  resume(): void { this.transition('resume'); }
  archive(): void { this.transition('archive'); }

  private transition(action: 'pause' | 'resume' | 'archive'): void {
    if (this.acting$.value) return;
    this.acting$.next(true);
    const call =
      action === 'pause' ? this.circles.pauseCircle(this.circleId)
      : action === 'resume' ? this.circles.resumeCircle(this.circleId)
      : this.circles.archiveCircle(this.circleId);
    call.pipe(finalize(() => this.acting$.next(false))).subscribe({
      next: () => {
        const msg = action === 'pause'
          ? $localize`:@@circles.detail.paused:Cercle mis en pause.`
          : action === 'resume'
            ? $localize`:@@circles.detail.resumed:Cercle réactivé.`
            : $localize`:@@circles.detail.archived:Cercle archivé.`;
        this.snack.open(msg, $localize`:@@common.ok:OK`, { duration: 2000 });
        this.reload$.next();
      },
      error: err => {
        // eslint-disable-next-line no-console
        console.warn('[circles-detail] transition failed', action, err?.status, err?.error?.title);
        this.snack.open(
          safeErrorMessage(err, $localize`:@@circles.detail.transition-error:Erreur lors de la transition.`),
          'OK', { duration: 4000 }
        );
      }
    });
  }

  canPause(s: CircleStatus): boolean { return s === 'ACTIVE'; }
  canResume(s: CircleStatus): boolean { return s === 'PAUSED'; }
  canArchive(s: CircleStatus): boolean { return s !== 'ARCHIVED'; }
  isTerminal(s: CircleStatus): boolean { return s === 'ARCHIVED'; }

  statusBadge(s: CircleStatus): string { return 'badge badge-' + s.toLowerCase(); }
}
