import { Component, OnInit } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ActivatedRoute, Router } from '@angular/router';
import { BehaviorSubject, Observable, of } from 'rxjs';
import { catchError, switchMap, tap } from 'rxjs/operators';

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
  loading$ = new BehaviorSubject<boolean>(false);
  error$   = new BehaviorSubject<string | null>(null);

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
      tap(() => { this.error$.next(null); queueMicrotask(() => this.loading$.next(true)); }),
      switchMap(p => {
        const id = p.get('id') ?? '';
        if (!UUID_REGEX.test(id) && !id.startsWith('qms-')) {
          this.error$.next('Identifiant invalide.');
          this.loading$.next(false);
          return of(null);
        }
        return this.refresh$.pipe(
          switchMap(() => this.svc.get(id).pipe(
            catchError(err => {
              this.error$.next(safeErrorMessage(err, 'Erreur lors du chargement.'));
              return of(null);
            }),
            tap(() => this.loading$.next(false))
          ))
        );
      })
    );
  }

  openEdit(q: AiQmsView): void {
    if (q.status !== 'DRAFT') {
      this.snack.open('Seul un DRAFT peut être édité.', 'OK', { duration: 4000 });
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
        title: 'Mettre en vigueur ?',
        message: 'Toute version IN_FORCE précédente pour la référence « ' + q.reference + ' » sera automatiquement marquée SUPERSEDED.',
        confirmLabel: 'Mettre en vigueur', cancelLabel: 'Annuler', danger: false
      }
    });
    ref.afterClosed().subscribe(ok => {
      if (!ok) return;
      this.svc.putInForce(q.id).subscribe({
        next: () => { this.snack.open('QMS en vigueur.', 'OK', { duration: 2200 }); this.refresh$.next(); },
        error: err => this.fail(err, 'Mise en vigueur impossible.')
      });
    });
  }

  archive(q: AiQmsView): void {
    // Simple confirm avec reason — pour rester compact, reason demandée via prompt côté UI minimaliste
    const ref = this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Archiver ce QMS ?',
        message: 'L\'archivage est terminal. Saisissez le motif au prochain prompt.',
        confirmLabel: 'Archiver', cancelLabel: 'Annuler', danger: true
      }
    });
    ref.afterClosed().subscribe(ok => {
      if (!ok) return;
      const reason = prompt('Motif d\'archivage (obligatoire, ≤ 2000 chars) :');
      if (!reason || !reason.trim()) {
        this.snack.open('Motif obligatoire — archivage annulé.', 'OK', { duration: 3000 });
        return;
      }
      this.svc.archive(q.id, { reason: reason.trim() }).subscribe({
        next: () => { this.snack.open('QMS archivé.', 'OK', { duration: 2200 }); this.refresh$.next(); },
        error: err => this.fail(err, 'Archivage impossible.')
      });
    });
  }

  remove(q: AiQmsView): void {
    if (q.status !== 'DRAFT') {
      this.snack.open('Seul un DRAFT peut être supprimé.', 'OK', { duration: 4000 });
      return;
    }
    const ref = this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Supprimer le brouillon ?',
        message: 'Suppression définitive de « ' + q.reference + ' v' + q.version + ' ».',
        confirmLabel: 'Supprimer', cancelLabel: 'Annuler', danger: true
      }
    });
    ref.afterClosed().subscribe(ok => {
      if (!ok) return;
      this.svc.delete(q.id).subscribe({
        next: () => { this.snack.open('Brouillon supprimé.', 'OK', { duration: 2200 }); this.router.navigate(['/ai-qms']); },
        error: err => this.fail(err, 'Suppression impossible.')
      });
    });
  }

  statusBadge(s: AiQmsStatus): string { return 'badge badge-' + s.toLowerCase(); }

  private fail(err: unknown, fallback: string): void {
    // eslint-disable-next-line no-console
    console.warn('[ai-qms-detail] action failed', (err as any)?.status, (err as any)?.error?.title);
    this.snack.open(safeErrorMessage(err, fallback), 'OK', { duration: 4000 });
  }
}
