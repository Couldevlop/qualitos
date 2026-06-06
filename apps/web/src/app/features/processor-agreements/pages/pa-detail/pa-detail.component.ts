import { Component, OnInit } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ActivatedRoute, Router } from '@angular/router';
import { BehaviorSubject, Observable, of } from 'rxjs';
import { catchError, switchMap, tap } from 'rxjs/operators';

import { ConfirmDialogComponent } from '../../../../shared/ui/confirm-dialog/confirm-dialog.component';
import { safeErrorMessage } from '../../../../core/http/error-message';
import { PaService } from '../../pa.service';
import { PaStatus, PaView } from '../../pa.types';
import { PaDialogComponent } from '../pa-dialog/pa-dialog.component';
import { PaTerminateDialogComponent } from '../pa-terminate-dialog/pa-terminate-dialog.component';

const UUID_REGEX = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

@Component({
  selector: 'qos-pa-detail',
  templateUrl: './pa-detail.component.html',
  styleUrls: ['./pa-detail.component.scss'],
  standalone: false
})
export class PaDetailComponent implements OnInit {

  pa$!: Observable<PaView | null>;
  loading$ = new BehaviorSubject<boolean>(false);
  error$   = new BehaviorSubject<string | null>(null);

  private readonly refresh$ = new BehaviorSubject<void>(undefined);

  constructor(
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly svc: PaService,
    private readonly dialog: MatDialog,
    private readonly snack: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.pa$ = this.route.paramMap.pipe(
      tap(() => { this.error$.next(null); queueMicrotask(() => this.loading$.next(true)); }),
      switchMap(p => {
        const id = p.get('id') ?? '';
        if (!UUID_REGEX.test(id) && !id.startsWith('pa-')) {
          this.error$.next($localize`:@@common.invalid-id:Identifiant invalide.`);
          this.loading$.next(false);
          return of(null);
        }
        return this.refresh$.pipe(
          switchMap(() => this.svc.get(id).pipe(
            catchError(err => {
              // eslint-disable-next-line no-console
              console.warn('[pa-detail] failed', err?.status, err?.error?.title);
              this.error$.next(safeErrorMessage(err, $localize`:@@common.error-loading:Erreur lors du chargement.`));
              return of(null);
            }),
            tap(() => this.loading$.next(false))
          ))
        );
      })
    );
  }

  openEdit(p: PaView): void {
    if (p.status === 'TERMINATED' || p.status === 'EXPIRED') {
      this.snack.open($localize`:@@dpa.detail.edit-terminated-blocked:Un DPA terminé ne peut plus être modifié.`, $localize`:@@common.ok:OK`, { duration: 4000 });
      return;
    }
    const ref = this.dialog.open(PaDialogComponent, {
      data: { agreement: p }, panelClass: 'qos-dialog-panel',
      autoFocus: 'first-tabbable', restoreFocus: true
    });
    ref.afterClosed().subscribe(updated => { if (updated) this.refresh$.next(); });
  }

  activate(p: PaView): void {
    this.svc.activate(p.id).subscribe({
      next: () => { this.snack.open($localize`:@@dpa.detail.activated:DPA activé.`, $localize`:@@common.ok:OK`, { duration: 2200 }); this.refresh$.next(); },
      error: err => this.fail(err, $localize`:@@dpa.detail.activate-failed:Activation impossible.`)
    });
  }

  terminate(p: PaView): void {
    const ref = this.dialog.open(PaTerminateDialogComponent, {
      data: { agreementId: p.id }, panelClass: 'qos-dialog-panel',
      autoFocus: 'first-tabbable', restoreFocus: true
    });
    ref.afterClosed().subscribe(updated => { if (updated) this.refresh$.next(); });
  }

  remove(p: PaView): void {
    if (p.status !== 'DRAFT') {
      this.snack.open($localize`:@@dpa.detail.only-draft-deletable:Seul un brouillon peut être supprimé.`, $localize`:@@common.ok:OK`, { duration: 4000 });
      return;
    }
    const ref = this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: $localize`:@@dpa.detail.delete-confirm-title:Supprimer le brouillon ?`,
        message: 'Suppression définitive de « ' + p.reference + ' ».',
        confirmLabel: $localize`:@@common.delete:Supprimer`, cancelLabel: $localize`:@@common.cancel:Annuler`, danger: true
      }
    });
    ref.afterClosed().subscribe(ok => {
      if (!ok) return;
      this.svc.delete(p.id).subscribe({
        next: () => { this.snack.open($localize`:@@dpa.detail.deleted:Brouillon supprimé.`, $localize`:@@common.ok:OK`, { duration: 2200 }); this.router.navigate(['/processor-agreements']); },
        error: err => this.fail(err, $localize`:@@common.delete-failed:Suppression impossible.`)
      });
    });
  }

  statusBadge(s: PaStatus): string { return 'badge badge-' + s.toLowerCase(); }

  daysUntilExpiration(p: PaView): number | null {
    if (!p.expirationDate) return null;
    return Math.round((new Date(p.expirationDate).getTime() - Date.now()) / 86400000);
  }

  private fail(err: unknown, fallback: string): void {
    // eslint-disable-next-line no-console
    console.warn('[pa-detail] action failed', (err as any)?.status, (err as any)?.error?.title);
    this.snack.open(safeErrorMessage(err, fallback), 'OK', { duration: 4000 });
  }
}
