import { Component, OnInit } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ActivatedRoute, Router } from '@angular/router';
import { BehaviorSubject, Observable, of } from 'rxjs';
import { catchError, switchMap, tap } from 'rxjs/operators';

import { ConfirmDialogComponent } from '../../../../shared/ui/confirm-dialog/confirm-dialog.component';
import { safeErrorMessage } from '../../../../core/http/error-message';
import { RopaService } from '../../ropa.service';
import {
  LawfulBasis,
  ProcessingActivityStatus,
  ProcessingActivityView
} from '../../ropa.types';
import { RopaDialogComponent } from '../ropa-dialog/ropa-dialog.component';

const UUID_REGEX = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

@Component({
  selector: 'qos-ropa-detail',
  templateUrl: './ropa-detail.component.html',
  styleUrls: ['./ropa-detail.component.scss'],
  standalone: false
})
export class RopaDetailComponent implements OnInit {

  activity$!: Observable<ProcessingActivityView | null>;
  loading$ = new BehaviorSubject<boolean>(false);
  error$   = new BehaviorSubject<string | null>(null);

  private readonly refresh$ = new BehaviorSubject<void>(undefined);

  constructor(
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly svc: RopaService,
    private readonly dialog: MatDialog,
    private readonly snack: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.activity$ = this.route.paramMap.pipe(
      tap(() => { this.loading$.next(true); this.error$.next(null); }),
      switchMap(p => {
        const id = p.get('id') ?? '';
        // OWASP A03 — UUID v4 regex on path id (mock-id fallback for demo data).
        if (!UUID_REGEX.test(id) && !id.startsWith('ropa-')) {
          this.error$.next('Identifiant invalide.');
          this.loading$.next(false);
          return of(null);
        }
        return this.refresh$.pipe(
          switchMap(() => this.svc.get(id).pipe(
            catchError(err => {
              // eslint-disable-next-line no-console
              console.warn('[ropa-detail] failed', err?.status, err?.error?.title);
              this.error$.next(safeErrorMessage(err, 'Erreur lors du chargement.'));
              return of(null);
            }),
            tap(() => this.loading$.next(false))
          ))
        );
      })
    );
  }

  openEdit(a: ProcessingActivityView): void {
    // OWASP A04 — ACTIVE is immutable (matches backend invariant).
    if (a.status !== 'DRAFT') {
      this.snack.open('Seul un brouillon peut être édité. Archivez pour modifier ensuite.', 'OK', { duration: 4000 });
      return;
    }
    const ref = this.dialog.open(RopaDialogComponent, {
      data: { activity: a }, panelClass: 'qos-dialog-panel',
      autoFocus: 'first-tabbable', restoreFocus: true
    });
    ref.afterClosed().subscribe(updated => { if (updated) this.refresh$.next(); });
  }

  activate(a: ProcessingActivityView): void {
    const ref = this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Activer cette activité de traitement ?',
        message: 'Une fois ACTIVE, la fiche devient immutable pour préserver la traçabilité (Art. 30 RGPD). Toute évolution future devra passer par un nouveau brouillon.',
        confirmLabel: 'Activer', cancelLabel: 'Annuler', danger: false
      }
    });
    ref.afterClosed().subscribe(ok => {
      if (!ok) return;
      this.svc.activate(a.id).subscribe({
        next: () => { this.snack.open('Activité activée.', 'OK', { duration: 2200 }); this.refresh$.next(); },
        error: err => this.fail(err, 'Activation impossible.')
      });
    });
  }

  archive(a: ProcessingActivityView): void {
    const ref = this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Archiver cette activité ?',
        message: '« ' + a.name + ' » sera marquée ARCHIVED. La fiche reste consultable pour les contrôles CNIL/EDPB.',
        confirmLabel: 'Archiver', cancelLabel: 'Annuler', danger: true
      }
    });
    ref.afterClosed().subscribe(ok => {
      if (!ok) return;
      this.svc.archive(a.id).subscribe({
        next: () => { this.snack.open('Activité archivée.', 'OK', { duration: 2200 }); this.refresh$.next(); },
        error: err => this.fail(err, 'Archivage impossible.')
      });
    });
  }

  remove(a: ProcessingActivityView): void {
    // OWASP A04 — backend refuse de supprimer ACTIVE ; on coupe en amont aussi.
    if (a.status === 'ACTIVE') {
      this.snack.open('Impossible de supprimer une activité ACTIVE — archivez-la d\'abord.', 'OK', { duration: 4000 });
      return;
    }
    const ref = this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Supprimer cette activité ?',
        message: 'Suppression définitive de « ' + a.name + ' ». Cette action est irréversible et ne devrait être utilisée que pour un brouillon créé par erreur.',
        confirmLabel: 'Supprimer', cancelLabel: 'Annuler', danger: true
      }
    });
    ref.afterClosed().subscribe(ok => {
      if (!ok) return;
      this.svc.delete(a.id).subscribe({
        next: () => { this.snack.open('Activité supprimée.', 'OK', { duration: 2200 }); this.router.navigate(['/ropa']); },
        error: err => this.fail(err, 'Suppression impossible.')
      });
    });
  }

  basisLabel(b: LawfulBasis): string {
    return ({
      CONSENT: 'Consentement (Art. 6.1.a)',
      CONTRACT: 'Exécution d\'un contrat (Art. 6.1.b)',
      LEGAL_OBLIGATION: 'Obligation légale (Art. 6.1.c)',
      VITAL_INTERESTS: 'Intérêts vitaux (Art. 6.1.d)',
      PUBLIC_TASK: 'Mission de service public (Art. 6.1.e)',
      LEGITIMATE_INTERESTS: 'Intérêt légitime (Art. 6.1.f)'
    })[b];
  }
  basisBadge(b: LawfulBasis): string  { return 'lb lb-' + b.toLowerCase(); }
  statusBadge(s: ProcessingActivityStatus): string { return 'badge badge-' + s.toLowerCase(); }

  private fail(err: unknown, fallback: string): void {
    // eslint-disable-next-line no-console
    console.warn('[ropa-detail] action failed', (err as any)?.status, (err as any)?.error?.title);
    this.snack.open(safeErrorMessage(err, fallback), 'OK', { duration: 4000 });
  }
}
