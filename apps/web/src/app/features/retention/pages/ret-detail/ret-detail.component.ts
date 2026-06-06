import { Component, OnInit } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ActivatedRoute, Router } from '@angular/router';
import { BehaviorSubject, Observable, of } from 'rxjs';
import { catchError, switchMap, tap } from 'rxjs/operators';

import { ConfirmDialogComponent } from '../../../../shared/ui/confirm-dialog/confirm-dialog.component';
import { safeErrorMessage } from '../../../../core/http/error-message';
import { describeDuration, RetentionService } from '../../retention.service';
import { RetentionRuleStatus, RetentionRuleView } from '../../retention.types';
import { RetRuleDialogComponent } from '../ret-rule-dialog/ret-rule-dialog.component';

const UUID_REGEX = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

@Component({
  selector: 'qos-ret-detail',
  templateUrl: './ret-detail.component.html',
  styleUrls: ['./ret-detail.component.scss'],
  standalone: false
})
export class RetDetailComponent implements OnInit {

  rule$!: Observable<RetentionRuleView | null>;
  loading$ = new BehaviorSubject<boolean>(false);
  error$   = new BehaviorSubject<string | null>(null);

  private readonly refresh$ = new BehaviorSubject<void>(undefined);

  describe = describeDuration;

  constructor(
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly svc: RetentionService,
    private readonly dialog: MatDialog,
    private readonly snack: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.rule$ = this.route.paramMap.pipe(
      tap(() => { this.error$.next(null); queueMicrotask(() => this.loading$.next(true)); }),
      switchMap(p => {
        const id = p.get('id') ?? '';
        if (!UUID_REGEX.test(id) && !id.startsWith('ret-')) {
          this.error$.next($localize`:@@common.invalid-id:Identifiant invalide.`);
          this.loading$.next(false);
          return of(null);
        }
        return this.refresh$.pipe(
          switchMap(() => this.svc.get(id).pipe(
            catchError(err => {
              // eslint-disable-next-line no-console
              console.warn('[ret-detail] failed', err?.status, err?.error?.title);
              this.error$.next(safeErrorMessage(err, $localize`:@@common.error-loading:Erreur lors du chargement.`));
              return of(null);
            }),
            tap(() => this.loading$.next(false))
          ))
        );
      })
    );
  }

  openEdit(r: RetentionRuleView): void {
    // OWASP A04 — ACTIVE est immutable (mirror backend RetentionRuleStateException).
    if (r.status !== 'DRAFT') {
      this.snack.open($localize`:@@retention.detail.edit-active-blocked:Seule une règle DRAFT peut être modifiée. Créez une nouvelle règle pour changer la durée.`, $localize`:@@common.ok:OK`, { duration: 5000 });
      return;
    }
    const ref = this.dialog.open(RetRuleDialogComponent, {
      data: { rule: r }, panelClass: 'qos-dialog-panel',
      autoFocus: 'first-tabbable', restoreFocus: true
    });
    ref.afterClosed().subscribe(updated => { if (updated) this.refresh$.next(); });
  }

  activate(r: RetentionRuleView): void {
    const ref = this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: $localize`:@@retention.detail.activate-confirm-title:Activer cette règle ?`,
        message: 'La règle deviendra ACTIVE et immutable. Toute autre règle ACTIVE pour la catégorie « ' + r.dataCategoryCode + ' » sera automatiquement archivée.',
        confirmLabel: $localize`:@@retention.detail.activate:Activer`, cancelLabel: $localize`:@@common.cancel:Annuler`, danger: false
      }
    });
    ref.afterClosed().subscribe(ok => {
      if (!ok) return;
      this.svc.activate(r.id).subscribe({
        next: () => { this.snack.open($localize`:@@retention.detail.activated:Règle activée.`, $localize`:@@common.ok:OK`, { duration: 2200 }); this.refresh$.next(); },
        error: err => this.fail(err, $localize`:@@retention.detail.activate-failed:Activation impossible.`)
      });
    });
  }

  archive(r: RetentionRuleView): void {
    const ref = this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: $localize`:@@retention.detail.archive-confirm-title:Archiver cette règle ?`,
        message: $localize`:@@retention.detail.archive-confirm-message:La règle sera ARCHIVED. Conservée pour la traçabilité historique des effacements passés.`,
        confirmLabel: $localize`:@@retention.detail.archive:Archiver`, cancelLabel: $localize`:@@common.cancel:Annuler`, danger: true
      }
    });
    ref.afterClosed().subscribe(ok => {
      if (!ok) return;
      this.svc.archive(r.id).subscribe({
        next: () => { this.snack.open($localize`:@@retention.detail.archived:Règle archivée.`, $localize`:@@common.ok:OK`, { duration: 2200 }); this.refresh$.next(); },
        error: err => this.fail(err, $localize`:@@retention.detail.archive-failed:Archivage impossible.`)
      });
    });
  }

  remove(r: RetentionRuleView): void {
    if (r.status !== 'DRAFT') {
      this.snack.open($localize`:@@retention.detail.only-draft-deletable:Seul un brouillon peut être supprimé.`, $localize`:@@common.ok:OK`, { duration: 4000 });
      return;
    }
    const ref = this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: $localize`:@@retention.detail.delete-confirm-title:Supprimer le brouillon ?`,
        message: 'Suppression définitive du brouillon « ' + r.dataCategoryCode + ' ».',
        confirmLabel: $localize`:@@common.delete:Supprimer`, cancelLabel: $localize`:@@common.cancel:Annuler`, danger: true
      }
    });
    ref.afterClosed().subscribe(ok => {
      if (!ok) return;
      this.svc.delete(r.id).subscribe({
        next: () => { this.snack.open($localize`:@@retention.detail.deleted:Brouillon supprimé.`, $localize`:@@common.ok:OK`, { duration: 2200 }); this.router.navigate(['/retention']); },
        error: err => this.fail(err, $localize`:@@common.delete-failed:Suppression impossible.`)
      });
    });
  }

  statusBadge(s: RetentionRuleStatus): string { return 'badge badge-' + s.toLowerCase(); }

  private fail(err: unknown, fallback: string): void {
    // eslint-disable-next-line no-console
    console.warn('[ret-detail] action failed', (err as any)?.status, (err as any)?.error?.title);
    this.snack.open(safeErrorMessage(err, fallback), 'OK', { duration: 4000 });
  }
}
