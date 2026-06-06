import { Component, OnInit } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ActivatedRoute, Router } from '@angular/router';
import { BehaviorSubject, Observable, of } from 'rxjs';
import { catchError, switchMap, tap } from 'rxjs/operators';

import { AuthService } from '../../../../core/auth/auth.service';
import { ConfirmDialogComponent } from '../../../../shared/ui/confirm-dialog/confirm-dialog.component';
import { safeErrorMessage } from '../../../../core/http/error-message';
import { DpiaService } from '../../dpia.service';
import { DpiaStatus, DpiaView, RiskLevel } from '../../dpia.types';
import { DpiaEditDialogComponent } from '../dpia-edit-dialog/dpia-edit-dialog.component';
import { DpiaOpinionDialogComponent } from '../dpia-opinion-dialog/dpia-opinion-dialog.component';

const UUID_REGEX = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

@Component({
  selector: 'qos-dpia-detail',
  templateUrl: './dpia-detail.component.html',
  styleUrls: ['./dpia-detail.component.scss'],
  standalone: false
})
export class DpiaDetailComponent implements OnInit {

  dpia$!: Observable<DpiaView | null>;
  loading$ = new BehaviorSubject<boolean>(false);
  error$   = new BehaviorSubject<string | null>(null);

  readonly editTooltip       = $localize`:@@common.edit:Modifier`;
  readonly editTooltipLocked = $localize`:@@dpia.detail.edit-tooltip-locked:Non éditable dans cet état`;
  readonly deleteTooltipDraft     = $localize`:@@dpia.detail.delete-tooltip-draft:Supprimer le brouillon`;
  readonly deleteTooltipDraftOnly = $localize`:@@dpia.detail.delete-tooltip-draft-only:Seul un brouillon peut être supprimé`;
  readonly opinionFavorable   = $localize`:@@dpia.detail.opinion-favorable:(favorable)`;
  readonly opinionUnfavorable = $localize`:@@dpia.detail.opinion-unfavorable:(défavorable)`;

  private readonly refresh$ = new BehaviorSubject<void>(undefined);

  constructor(
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly svc: DpiaService,
    private readonly auth: AuthService,
    private readonly dialog: MatDialog,
    private readonly snack: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.dpia$ = this.route.paramMap.pipe(
      tap(() => { this.error$.next(null); queueMicrotask(() => this.loading$.next(true)); }),
      switchMap(p => {
        const id = p.get('id') ?? '';
        // OWASP A03 — UUID v4 regex on path id (mock-id fallback).
        if (!UUID_REGEX.test(id) && !id.startsWith('dpia-')) {
          this.error$.next($localize`:@@common.invalid-id:Identifiant invalide.`);
          this.loading$.next(false);
          return of(null);
        }
        return this.refresh$.pipe(
          switchMap(() => this.svc.get(id).pipe(
            catchError(err => {
              // eslint-disable-next-line no-console
              console.warn('[dpia-detail] failed', err?.status, err?.error?.title);
              this.error$.next(safeErrorMessage(err, $localize`:@@common.error-loading:Erreur lors du chargement.`));
              return of(null);
            }),
            tap(() => this.loading$.next(false))
          ))
        );
      })
    );
  }

  openEdit(d: DpiaView): void {
    // OWASP A04 — édition limitée aux statuts mutables (mirror backend).
    if (d.status !== 'DRAFT' && d.status !== 'IN_PROGRESS') {
      this.snack.open($localize`:@@dpia.detail.only-draft-progress-editable:La fiche n'est éditable qu'en DRAFT ou IN_PROGRESS.`, $localize`:@@common.ok:OK`, { duration: 4000 });
      return;
    }
    const ref = this.dialog.open(DpiaEditDialogComponent, {
      data: { dpia: d }, panelClass: 'qos-dialog-panel',
      autoFocus: 'first-tabbable', restoreFocus: true
    });
    ref.afterClosed().subscribe(updated => { if (updated) this.refresh$.next(); });
  }

  start(d: DpiaView): void {
    const handledByUserId = this.auth.snapshot()?.userId;
    if (!handledByUserId) {
      this.snack.open($localize`:@@common.session-expired:Session expirée — veuillez vous reconnecter.`, $localize`:@@common.ok:OK`, { duration: 4000 });
      return;
    }
    this.svc.start(d.id, { handledByUserId }).subscribe({
      next: () => { this.snack.open($localize`:@@dpia.detail.started:Analyse démarrée.`, $localize`:@@common.ok:OK`, { duration: 2200 }); this.refresh$.next(); },
      error: err => this.fail(err, $localize`:@@dpia.detail.start-failed:Démarrage impossible.`)
    });
  }

  returnToDraft(d: DpiaView): void {
    this.svc.returnToDraft(d.id).subscribe({
      next: () => { this.snack.open($localize`:@@dpia.detail.returned-to-draft:Renvoyée en DRAFT.`, $localize`:@@common.ok:OK`, { duration: 2200 }); this.refresh$.next(); },
      error: err => this.fail(err, $localize`:@@dpia.detail.return-draft-failed:Retour DRAFT impossible.`)
    });
  }

  submitToDpo(d: DpiaView): void {
    const ref = this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: $localize`:@@dpia.detail.submit-confirm-title:Soumettre au DPO ?`,
        message: $localize`:@@dpia.detail.submit-confirm-message:La DPIA passera en DPO_REVIEW. Pendant cette phase, le DPO rend un avis motivé (Art. 39§1.c).`,
        confirmLabel: $localize`:@@dpia.detail.submit-to-dpo:Soumettre au DPO`, cancelLabel: $localize`:@@common.cancel:Annuler`, danger: false
      }
    });
    ref.afterClosed().subscribe(ok => {
      if (!ok) return;
      this.svc.submitToDpo(d.id).subscribe({
        next: () => { this.snack.open($localize`:@@dpia.detail.submitted:Soumise au DPO.`, $localize`:@@common.ok:OK`, { duration: 2200 }); this.refresh$.next(); },
        error: err => this.fail(err, $localize`:@@dpia.detail.submit-failed:Soumission impossible.`)
      });
    });
  }

  decide(d: DpiaView, decision: 'APPROVED' | 'REJECTED'): void {
    const ref = this.dialog.open(DpiaOpinionDialogComponent, {
      data: { dpiaId: d.id, decision }, panelClass: 'qos-dialog-panel',
      autoFocus: 'first-tabbable', restoreFocus: true
    });
    ref.afterClosed().subscribe(updated => { if (updated) this.refresh$.next(); });
  }

  archive(d: DpiaView): void {
    const ref = this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: $localize`:@@dpia.detail.archive-confirm-title:Archiver la DPIA ?`,
        message: '« ' + d.title + ' » sera marquée ARCHIVED. Conservée pour la traçabilité historique.',
        confirmLabel: $localize`:@@dpia.detail.archive:Archiver`, cancelLabel: $localize`:@@common.cancel:Annuler`, danger: true
      }
    });
    ref.afterClosed().subscribe(ok => {
      if (!ok) return;
      this.svc.archive(d.id).subscribe({
        next: () => { this.snack.open($localize`:@@dpia.detail.archived:DPIA archivée.`, $localize`:@@common.ok:OK`, { duration: 2200 }); this.refresh$.next(); },
        error: err => this.fail(err, $localize`:@@dpia.detail.archive-failed:Archivage impossible.`)
      });
    });
  }

  remove(d: DpiaView): void {
    // OWASP A04 — only DRAFT can be deleted (mirror backend).
    if (d.status !== 'DRAFT') {
      this.snack.open($localize`:@@dpia.detail.only-draft-deletable:Seule une DPIA en DRAFT peut être supprimée.`, $localize`:@@common.ok:OK`, { duration: 4000 });
      return;
    }
    const ref = this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: $localize`:@@dpia.detail.delete-confirm-title:Supprimer le brouillon ?`,
        message: 'Suppression définitive du brouillon « ' + d.title + ' ».',
        confirmLabel: $localize`:@@common.delete:Supprimer`, cancelLabel: $localize`:@@common.cancel:Annuler`, danger: true
      }
    });
    ref.afterClosed().subscribe(ok => {
      if (!ok) return;
      this.svc.delete(d.id).subscribe({
        next: () => { this.snack.open($localize`:@@dpia.detail.draft-deleted:Brouillon supprimé.`, $localize`:@@common.ok:OK`, { duration: 2200 }); this.router.navigate(['/dpia']); },
        error: err => this.fail(err, $localize`:@@common.delete-failed:Suppression impossible.`)
      });
    });
  }

  riskBadge(r: RiskLevel): string    { return 'risk risk-' + r.toLowerCase(); }
  statusBadge(s: DpiaStatus): string { return 'badge badge-' + s.toLowerCase(); }

  isLive(s: DpiaStatus): boolean { return s === 'DRAFT' || s === 'IN_PROGRESS' || s === 'DPO_REVIEW'; }

  private fail(err: unknown, fallback: string): void {
    // eslint-disable-next-line no-console
    console.warn('[dpia-detail] action failed', (err as any)?.status, (err as any)?.error?.title);
    this.snack.open(safeErrorMessage(err, fallback), $localize`:@@common.ok:OK`, { duration: 4000 });
  }
}
