import { Component, OnInit } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ActivatedRoute, Router } from '@angular/router';
import { BehaviorSubject, Observable, of } from 'rxjs';
import { catchError, shareReplay, switchMap, tap } from 'rxjs/operators';

import { deferredView } from '../../../../core/rx/deferred-view';
import { ConfirmDialogComponent } from '../../../../shared/ui/confirm-dialog/confirm-dialog.component';
import { safeErrorMessage } from '../../../../core/http/error-message';
import { AiQmsService } from '../../ai-qms.service';
import { AiQmsStatus, AiQmsView } from '../../ai-qms.types';
import { AiQmsApproveDialogComponent } from '../ai-qms-approve-dialog/ai-qms-approve-dialog.component';
import { AiQmsDialogComponent } from '../ai-qms-dialog/ai-qms-dialog.component';

const UUID_REGEX = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

@Component({
  selector: 'qos-ai-qms-detail',
  templateUrl: './ai-qms-detail.component.html',
  styleUrls: ['./ai-qms-detail.component.scss'],
  standalone: false
})
export class AiQmsDetailComponent implements OnInit {

  qms$!: Observable<AiQmsView | null>;
  private readonly loadingState$ = new BehaviorSubject<boolean>(false);
  readonly loading$ = deferredView(this.loadingState$);
  private readonly errorState$ = new BehaviorSubject<string | null>(null);
  readonly error$ = deferredView(this.errorState$);

  private readonly refresh$ = new BehaviorSubject<void>(undefined);

  constructor(
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly svc: AiQmsService,
    private readonly dialog: MatDialog,
    private readonly snack: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.qms$ = this.route.paramMap.pipe(
      tap(() => { this.errorState$.next(null); this.loadingState$.next(true); }),
      switchMap(p => {
        const id = p.get('id') ?? '';
        if (!UUID_REGEX.test(id) && !id.startsWith('qms-')) {
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
            tap(() => this.loadingState$.next(false))
          ))
        );
      }),
      shareReplay({ bufferSize: 1, refCount: true })
    );
  }

  openEdit(q: AiQmsView): void {
    if (q.status !== 'DRAFT') {
      this.snack.open($localize`:@@ai-qms.detail.only-draft-edit:Seul un DRAFT peut être édité.`, $localize`:@@common.ok:OK`, { duration: 4000 });
      return;
    }
    const ref = this.dialog.open(AiQmsDialogComponent, {
      data: { qms: q }, panelClass: 'qos-dialog-panel',
      autoFocus: 'first-tabbable', restoreFocus: true
    });
    ref.afterClosed().subscribe(updated => { if (updated) this.refresh$.next(); });
  }

  approve(q: AiQmsView): void {
    const ref = this.dialog.open(AiQmsApproveDialogComponent, {
      data: { qmsId: q.id }, panelClass: 'qos-dialog-panel',
      autoFocus: 'first-tabbable', restoreFocus: true
    });
    ref.afterClosed().subscribe(u => { if (u) this.refresh$.next(); });
  }

  putInForce(q: AiQmsView): void {
    const ref = this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: $localize`:@@ai-qms.detail.put-in-force-title:Mettre en vigueur ?`,
        message: $localize`:@@ai-qms.detail.put-in-force-message:Toute version IN_FORCE précédente pour la référence « ${q.reference}:reference: » sera automatiquement marquée SUPERSEDED.`,
        confirmLabel: $localize`:@@ai-qms.detail.put-in-force:Mettre en vigueur`, cancelLabel: $localize`:@@common.cancel:Annuler`, danger: false
      }
    });
    ref.afterClosed().subscribe(ok => {
      if (!ok) return;
      this.svc.putInForce(q.id).subscribe({
        next: () => { this.snack.open($localize`:@@ai-qms.detail.in-force:QMS en vigueur.`, $localize`:@@common.ok:OK`, { duration: 2200 }); this.refresh$.next(); },
        error: err => this.fail(err, $localize`:@@ai-qms.detail.put-in-force-failed:Mise en vigueur impossible.`)
      });
    });
  }

  archive(q: AiQmsView): void {
    // Simple confirm avec reason — pour rester compact, reason demandée via prompt côté UI minimaliste
    const ref = this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: $localize`:@@ai-qms.detail.archive-title:Archiver ce QMS ?`,
        message: $localize`:@@ai-qms.detail.archive-message:L'archivage est terminal. Saisissez le motif au prochain prompt.`,
        confirmLabel: $localize`:@@ai-qms.detail.archive:Archiver`, cancelLabel: $localize`:@@common.cancel:Annuler`, danger: true
      }
    });
    ref.afterClosed().subscribe(ok => {
      if (!ok) return;
      const reason = prompt($localize`:@@ai-qms.detail.archive-prompt:Motif d'archivage (obligatoire, ≤ 2000 chars) :`);
      if (!reason || !reason.trim()) {
        this.snack.open($localize`:@@ai-qms.detail.reason-required:Motif obligatoire — archivage annulé.`, $localize`:@@common.ok:OK`, { duration: 3000 });
        return;
      }
      this.svc.archive(q.id, { reason: reason.trim() }).subscribe({
        next: () => { this.snack.open($localize`:@@ai-qms.detail.archived:QMS archivé.`, $localize`:@@common.ok:OK`, { duration: 2200 }); this.refresh$.next(); },
        error: err => this.fail(err, $localize`:@@ai-qms.detail.archive-failed:Archivage impossible.`)
      });
    });
  }

  remove(q: AiQmsView): void {
    if (q.status !== 'DRAFT') {
      this.snack.open($localize`:@@ai-qms.detail.only-draft-delete:Seul un DRAFT peut être supprimé.`, $localize`:@@common.ok:OK`, { duration: 4000 });
      return;
    }
    const ref = this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: $localize`:@@ai-qms.detail.delete-title:Supprimer le brouillon ?`,
        message: $localize`:@@ai-qms.detail.delete-message:Suppression définitive de « ${q.reference}:reference: v${q.version}:version: ».`,
        confirmLabel: $localize`:@@common.delete:Supprimer`, cancelLabel: $localize`:@@common.cancel:Annuler`, danger: true
      }
    });
    ref.afterClosed().subscribe(ok => {
      if (!ok) return;
      this.svc.delete(q.id).subscribe({
        next: () => { this.snack.open($localize`:@@ai-qms.detail.deleted:Brouillon supprimé.`, $localize`:@@common.ok:OK`, { duration: 2200 }); this.router.navigate(['/ai-qms']); },
        error: err => this.fail(err, $localize`:@@common.delete-failed:Suppression impossible.`)
      });
    });
  }

  statusBadge(s: AiQmsStatus): string { return 'badge badge-' + s.toLowerCase(); }

  private fail(err: unknown, fallback: string): void {
    // eslint-disable-next-line no-console
    console.warn('[ai-qms-detail] action failed', (err as any)?.status, (err as any)?.error?.title);
    this.snack.open(safeErrorMessage(err, fallback), $localize`:@@common.ok:OK`, { duration: 4000 });
  }
}
