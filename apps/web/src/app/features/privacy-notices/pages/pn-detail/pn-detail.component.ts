import { Component, OnInit } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ActivatedRoute, Router } from '@angular/router';
import { BehaviorSubject, Observable, of } from 'rxjs';
import { catchError, shareReplay, switchMap, tap } from 'rxjs/operators';

import { deferredView } from '../../../../core/rx/deferred-view';
import { AuthService } from '../../../../core/auth/auth.service';
import { ConfirmDialogComponent } from '../../../../shared/ui/confirm-dialog/confirm-dialog.component';
import { safeErrorMessage } from '../../../../core/http/error-message';
import { PrivacyNoticesService } from '../../privacy-notices.service';
import { PrivacyNoticeStatus, PrivacyNoticeView } from '../../privacy-notices.types';
import { PnDialogComponent } from '../pn-dialog/pn-dialog.component';

const UUID_REGEX = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

@Component({
  selector: 'qos-pn-detail',
  templateUrl: './pn-detail.component.html',
  styleUrls: ['./pn-detail.component.scss'],
  standalone: false
})
export class PnDetailComponent implements OnInit {

  notice$!: Observable<PrivacyNoticeView | null>;
  private readonly loadingState$ = new BehaviorSubject<boolean>(false);
  readonly loading$ = deferredView(this.loadingState$);
  private readonly errorState$ = new BehaviorSubject<string | null>(null);
  readonly error$ = deferredView(this.errorState$);

  versions: PrivacyNoticeView[] = [];

  readonly editTooltipImmutable = $localize`:@@privacy-notices.detail.edit-tooltip-immutable:Une mention PUBLISHED est immutable`;
  readonly editTooltipDraft     = $localize`:@@privacy-notices.detail.edit-tooltip-draft:Modifier le brouillon`;
  readonly deleteTooltip          = $localize`:@@common.delete:Supprimer`;
  readonly deleteTooltipDraftOnly = $localize`:@@privacy-notices.detail.delete-tooltip-draft-only:Seul un brouillon peut être supprimé`;

  private readonly refresh$ = new BehaviorSubject<void>(undefined);

  constructor(
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly svc: PrivacyNoticesService,
    private readonly auth: AuthService,
    private readonly dialog: MatDialog,
    private readonly snack: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.notice$ = this.route.paramMap.pipe(
      tap(() => { this.errorState$.next(null); this.loadingState$.next(true); }),
      switchMap(p => {
        const id = p.get('id') ?? '';
        if (!UUID_REGEX.test(id) && !id.startsWith('pn-')) {
          this.errorState$.next($localize`:@@common.invalid-id:Identifiant invalide.`);
          this.loadingState$.next(false);
          return of(null);
        }
        return this.refresh$.pipe(
          switchMap(() => this.svc.get(id).pipe(
            catchError(err => {
              this.errorState$.next(safeErrorMessage(err, $localize`:@@common.error-loading:Erreur lors du chargement.`));
              return of(null);
            }),
            tap(n => {
              this.loadingState$.next(false);
              if (n) {
                this.svc.versions(n.reference).subscribe({
                  next: arr => (this.versions = arr.sort((a, b) =>
                    b.createdAt.localeCompare(a.createdAt))),
                  error: () => (this.versions = [])
                });
              }
            })
          ))
        );
      }),
      shareReplay({ bufferSize: 1, refCount: true })
    );
  }

  openEdit(n: PrivacyNoticeView): void {
    // OWASP A04 — PUBLISHED est immutable (mirror backend invariant).
    if (n.status !== 'DRAFT') {
      this.snack.open($localize`:@@privacy-notices.detail.only-draft-editable:Seul un brouillon peut être édité.`, $localize`:@@common.ok:OK`, { duration: 4000 });
      return;
    }
    const ref = this.dialog.open(PnDialogComponent, {
      data: { notice: n }, panelClass: 'qos-dialog-panel',
      autoFocus: 'first-tabbable', restoreFocus: true
    });
    ref.afterClosed().subscribe(updated => { if (updated) this.refresh$.next(); });
  }

  publish(n: PrivacyNoticeView): void {
    const ref = this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: $localize`:@@privacy-notices.detail.publish-confirm-title:Publier cette mention ?`,
        message: 'La version DRAFT « ' + n.reference + ' v' + n.version + ' [' + n.language.toUpperCase() + '] » deviendra PUBLISHED, donc immutable. Toute version PUBLISHED précédente pour cette même référence + langue sera automatiquement archivée.',
        confirmLabel: $localize`:@@privacy-notices.detail.publish:Publier`, cancelLabel: $localize`:@@common.cancel:Annuler`, danger: false
      }
    });
    ref.afterClosed().subscribe(ok => {
      if (!ok) return;
      const publishedByUserId = this.auth.snapshot()?.userId;
      if (!publishedByUserId) {
        this.snack.open($localize`:@@common.session-expired:Session expirée — veuillez vous reconnecter.`, $localize`:@@common.ok:OK`, { duration: 4000 });
        return;
      }
      this.svc.publish(n.id, { publishedByUserId }).subscribe({
        next: () => { this.snack.open($localize`:@@privacy-notices.detail.published:Mention publiée.`, $localize`:@@common.ok:OK`, { duration: 2200 }); this.refresh$.next(); },
        error: err => this.fail(err, $localize`:@@privacy-notices.detail.publish-failed:Publication impossible.`)
      });
    });
  }

  archive(n: PrivacyNoticeView): void {
    const ref = this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: $localize`:@@privacy-notices.detail.archive-confirm-title:Archiver cette mention ?`,
        message: '« ' + n.title + ' » sera marquée ARCHIVED. Conservée pour la traçabilité (preuve de ce qui a été affiché).',
        confirmLabel: $localize`:@@privacy-notices.detail.archive:Archiver`, cancelLabel: $localize`:@@common.cancel:Annuler`, danger: true
      }
    });
    ref.afterClosed().subscribe(ok => {
      if (!ok) return;
      this.svc.archive(n.id).subscribe({
        next: () => { this.snack.open($localize`:@@privacy-notices.detail.archived:Mention archivée.`, $localize`:@@common.ok:OK`, { duration: 2200 }); this.refresh$.next(); },
        error: err => this.fail(err, $localize`:@@privacy-notices.detail.archive-failed:Archivage impossible.`)
      });
    });
  }

  remove(n: PrivacyNoticeView): void {
    // OWASP A04 — only DRAFT can be deleted (mirror backend invariant).
    if (n.status !== 'DRAFT') {
      this.snack.open($localize`:@@privacy-notices.detail.cannot-delete-published:Une mention publiée ou archivée ne peut pas être supprimée.`, $localize`:@@common.ok:OK`, { duration: 4000 });
      return;
    }
    const ref = this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: $localize`:@@privacy-notices.detail.delete-confirm-title:Supprimer le brouillon ?`,
        message: 'Suppression définitive de « ' + n.title + ' » (statut DRAFT).',
        confirmLabel: $localize`:@@common.delete:Supprimer`, cancelLabel: $localize`:@@common.cancel:Annuler`, danger: true
      }
    });
    ref.afterClosed().subscribe(ok => {
      if (!ok) return;
      this.svc.delete(n.id).subscribe({
        next: () => { this.snack.open($localize`:@@privacy-notices.detail.draft-deleted:Brouillon supprimé.`, $localize`:@@common.ok:OK`, { duration: 2200 }); this.router.navigate(['/privacy-notices']); },
        error: err => this.fail(err, $localize`:@@common.delete-failed:Suppression impossible.`)
      });
    });
  }

  openVersion(v: PrivacyNoticeView): void { this.router.navigate(['/privacy-notices', v.id]); }

  statusBadge(s: PrivacyNoticeStatus): string { return 'badge badge-' + s.toLowerCase(); }

  private fail(err: unknown, fallback: string): void {
    // eslint-disable-next-line no-console
    console.warn('[pn-detail] action failed', (err as any)?.status, (err as any)?.error?.title);
    this.snack.open(safeErrorMessage(err, fallback), $localize`:@@common.ok:OK`, { duration: 4000 });
  }
}
