import { Component, OnInit } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ActivatedRoute, Router } from '@angular/router';
import { BehaviorSubject, Observable, of } from 'rxjs';
import { catchError, switchMap, tap } from 'rxjs/operators';

import { AuthService } from '../../../../core/auth/auth.service';
import { safeErrorMessage } from '../../../../core/http/error-message';
import { SubjectRequestsService } from '../../subject-requests.service';
import {
  SubjectRequestStatus,
  SubjectRequestType,
  SubjectRequestView
} from '../../subject-requests.types';
import { SrCompleteDialogComponent } from '../sr-complete-dialog/sr-complete-dialog.component';
import { SrExtendDialogComponent } from '../sr-extend-dialog/sr-extend-dialog.component';
import { SrRejectDialogComponent } from '../sr-reject-dialog/sr-reject-dialog.component';

const UUID_REGEX = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;
const MAX_EXTENSION_DAYS = 90; // Art. 12§3 — 3 mois max

@Component({
  selector: 'qos-sr-detail',
  templateUrl: './sr-detail.component.html',
  styleUrls: ['./sr-detail.component.scss'],
  standalone: false
})
export class SrDetailComponent implements OnInit {

  request$!: Observable<SubjectRequestView | null>;
  loading$ = new BehaviorSubject<boolean>(false);
  error$   = new BehaviorSubject<string | null>(null);

  private readonly refresh$ = new BehaviorSubject<void>(undefined);

  constructor(
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly svc: SubjectRequestsService,
    private readonly auth: AuthService,
    private readonly dialog: MatDialog,
    private readonly snack: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.request$ = this.route.paramMap.pipe(
      tap(() => { this.error$.next(null); queueMicrotask(() => this.loading$.next(true)); }),
      switchMap(p => {
        const id = p.get('id') ?? '';
        // OWASP A03 — UUID regex on path id (mock-id fallback).
        if (!UUID_REGEX.test(id) && !id.startsWith('sr-')) {
          this.error$.next($localize`:@@common.invalid-id:Identifiant invalide.`);
          this.loading$.next(false);
          return of(null);
        }
        return this.refresh$.pipe(
          switchMap(() => this.svc.get(id).pipe(
            catchError(err => {
              // eslint-disable-next-line no-console
              console.warn('[sr-detail] failed', err?.status, err?.error?.title);
              this.error$.next(safeErrorMessage(err, $localize`:@@common.error-loading:Erreur lors du chargement.`));
              return of(null);
            }),
            tap(() => this.loading$.next(false))
          ))
        );
      })
    );
  }

  start(r: SubjectRequestView): void {
    const handledByUserId = this.auth.snapshot()?.userId;
    if (!handledByUserId) {
      this.snack.open($localize`:@@common.session-expired:Session expirée — veuillez vous reconnecter.`, $localize`:@@common.ok:OK`, { duration: 4000 });
      return;
    }
    this.svc.start(r.id, { handledByUserId }).subscribe({
      next: () => { this.snack.open($localize`:@@subject-requests.detail.started:Demande en cours de traitement.`, $localize`:@@common.ok:OK`, { duration: 2200 }); this.refresh$.next(); },
      error: err => this.fail(err, $localize`:@@subject-requests.detail.start-failed:Démarrage impossible.`)
    });
  }

  openComplete(r: SubjectRequestView): void {
    const ref = this.dialog.open(SrCompleteDialogComponent, {
      data: { requestId: r.id }, panelClass: 'qos-dialog-panel',
      autoFocus: 'first-tabbable', restoreFocus: true
    });
    ref.afterClosed().subscribe(updated => { if (updated) this.refresh$.next(); });
  }

  openReject(r: SubjectRequestView): void {
    const ref = this.dialog.open(SrRejectDialogComponent, {
      data: { requestId: r.id }, panelClass: 'qos-dialog-panel',
      autoFocus: 'first-tabbable', restoreFocus: true
    });
    ref.afterClosed().subscribe(updated => { if (updated) this.refresh$.next(); });
  }

  openExtend(r: SubjectRequestView): void {
    const received = new Date(r.receivedAt);
    const max = new Date(received.getTime() + MAX_EXTENSION_DAYS * 86400000);
    const ref = this.dialog.open(SrExtendDialogComponent, {
      data: { request: r, maxDeadlineIso: max.toISOString() },
      panelClass: 'qos-dialog-panel',
      autoFocus: 'first-tabbable', restoreFocus: true
    });
    ref.afterClosed().subscribe(updated => { if (updated) this.refresh$.next(); });
  }

  typeLabel(t: SubjectRequestType): string {
    return ({
      ACCESS: $localize`:@@subject-requests.type.access:Accès aux données (Art. 15)`,
      ERASURE: $localize`:@@subject-requests.type.erasure:Effacement / droit à l'oubli (Art. 17)`,
      PORTABILITY: $localize`:@@subject-requests.type.portability:Portabilité (Art. 20)`,
      RECTIFICATION: $localize`:@@subject-requests.type.rectification:Rectification (Art. 16)`,
      RESTRICTION: $localize`:@@subject-requests.type.restriction:Limitation du traitement (Art. 18)`,
      OBJECTION: $localize`:@@subject-requests.type.objection:Opposition (Art. 21)`
    })[t];
  }
  typeBadge(t: SubjectRequestType): string { return 'tbadge tbadge-' + t.toLowerCase(); }
  statusBadge(s: SubjectRequestStatus): string { return 'badge badge-' + s.toLowerCase(); }

  isTerminal(s: SubjectRequestStatus): boolean { return s === 'COMPLETED' || s === 'REJECTED'; }

  private fail(err: unknown, fallback: string): void {
    // eslint-disable-next-line no-console
    console.warn('[sr-detail] action failed', (err as any)?.status, (err as any)?.error?.title);
    this.snack.open(safeErrorMessage(err, fallback), $localize`:@@common.ok:OK`, { duration: 4000 });
  }
}
