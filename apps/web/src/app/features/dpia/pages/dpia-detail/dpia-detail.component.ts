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
      tap(() => { this.loading$.next(true); this.error$.next(null); }),
      switchMap(p => {
        const id = p.get('id') ?? '';
        // OWASP A03 — UUID v4 regex on path id (mock-id fallback).
        if (!UUID_REGEX.test(id) && !id.startsWith('dpia-')) {
          this.error$.next('Identifiant invalide.');
          this.loading$.next(false);
          return of(null);
        }
        return this.refresh$.pipe(
          switchMap(() => this.svc.get(id).pipe(
            catchError(err => {
              // eslint-disable-next-line no-console
              console.warn('[dpia-detail] failed', err?.status, err?.error?.title);
              this.error$.next(safeErrorMessage(err, 'Erreur lors du chargement.'));
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
      this.snack.open('La fiche n\'est éditable qu\'en DRAFT ou IN_PROGRESS.', 'OK', { duration: 4000 });
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
      this.snack.open('Session expirée — veuillez vous reconnecter.', 'OK', { duration: 4000 });
      return;
    }
    this.svc.start(d.id, { handledByUserId }).subscribe({
      next: () => { this.snack.open('Analyse démarrée.', 'OK', { duration: 2200 }); this.refresh$.next(); },
      error: err => this.fail(err, 'Démarrage impossible.')
    });
  }

  returnToDraft(d: DpiaView): void {
    this.svc.returnToDraft(d.id).subscribe({
      next: () => { this.snack.open('Renvoyée en DRAFT.', 'OK', { duration: 2200 }); this.refresh$.next(); },
      error: err => this.fail(err, 'Retour DRAFT impossible.')
    });
  }

  submitToDpo(d: DpiaView): void {
    const ref = this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Soumettre au DPO ?',
        message: 'La DPIA passera en DPO_REVIEW. Pendant cette phase, le DPO rend un avis motivé (Art. 39§1.c).',
        confirmLabel: 'Soumettre au DPO', cancelLabel: 'Annuler', danger: false
      }
    });
    ref.afterClosed().subscribe(ok => {
      if (!ok) return;
      this.svc.submitToDpo(d.id).subscribe({
        next: () => { this.snack.open('Soumise au DPO.', 'OK', { duration: 2200 }); this.refresh$.next(); },
        error: err => this.fail(err, 'Soumission impossible.')
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
        title: 'Archiver la DPIA ?',
        message: '« ' + d.title + ' » sera marquée ARCHIVED. Conservée pour la traçabilité historique.',
        confirmLabel: 'Archiver', cancelLabel: 'Annuler', danger: true
      }
    });
    ref.afterClosed().subscribe(ok => {
      if (!ok) return;
      this.svc.archive(d.id).subscribe({
        next: () => { this.snack.open('DPIA archivée.', 'OK', { duration: 2200 }); this.refresh$.next(); },
        error: err => this.fail(err, 'Archivage impossible.')
      });
    });
  }

  remove(d: DpiaView): void {
    // OWASP A04 — only DRAFT can be deleted (mirror backend).
    if (d.status !== 'DRAFT') {
      this.snack.open('Seule une DPIA en DRAFT peut être supprimée.', 'OK', { duration: 4000 });
      return;
    }
    const ref = this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Supprimer le brouillon ?',
        message: 'Suppression définitive du brouillon « ' + d.title + ' ».',
        confirmLabel: 'Supprimer', cancelLabel: 'Annuler', danger: true
      }
    });
    ref.afterClosed().subscribe(ok => {
      if (!ok) return;
      this.svc.delete(d.id).subscribe({
        next: () => { this.snack.open('Brouillon supprimé.', 'OK', { duration: 2200 }); this.router.navigate(['/dpia']); },
        error: err => this.fail(err, 'Suppression impossible.')
      });
    });
  }

  riskBadge(r: RiskLevel): string    { return 'risk risk-' + r.toLowerCase(); }
  statusBadge(s: DpiaStatus): string { return 'badge badge-' + s.toLowerCase(); }

  isLive(s: DpiaStatus): boolean { return s === 'DRAFT' || s === 'IN_PROGRESS' || s === 'DPO_REVIEW'; }

  private fail(err: unknown, fallback: string): void {
    // eslint-disable-next-line no-console
    console.warn('[dpia-detail] action failed', (err as any)?.status, (err as any)?.error?.title);
    this.snack.open(safeErrorMessage(err, fallback), 'OK', { duration: 4000 });
  }
}
