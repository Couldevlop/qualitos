import { Component, OnInit } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ActivatedRoute, Router } from '@angular/router';
import { BehaviorSubject, Observable, forkJoin, of } from 'rxjs';
import { catchError, switchMap, tap } from 'rxjs/operators';

import { ConfirmDialogComponent } from '../../../../shared/ui/confirm-dialog/confirm-dialog.component';
import { safeErrorMessage } from '../../../../core/http/error-message';
import { ItsmService } from '../../itsm.service';
import {
  ConnectionResponse,
  ConnectionStatus,
  ItsmProvider,
  MappingResponse,
  SyncReport
} from '../../itsm.types';
import { ItsmConnectionDialogComponent } from '../itsm-connection-dialog/itsm-connection-dialog.component';

const UUID_REGEX = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

@Component({
  selector: 'qos-itsm-detail',
  templateUrl: './itsm-detail.component.html',
  styleUrls: ['./itsm-detail.component.scss'],
  standalone: false
})
export class ItsmDetailComponent implements OnInit {

  readonly mappingColumns = [
    'externalId', 'externalTitle', 'externalStatus', 'externalPriority',
    'internalEntity', 'lastSeenAt'
  ];

  connection$!: Observable<ConnectionResponse | null>;
  loading$ = new BehaviorSubject<boolean>(false);
  error$   = new BehaviorSubject<string | null>(null);

  mappings: MappingResponse[] = [];
  lastSync: SyncReport | null = null;
  syncing = false;

  private readonly refresh$ = new BehaviorSubject<void>(undefined);
  private connectionId = '';

  constructor(
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly svc: ItsmService,
    private readonly dialog: MatDialog,
    private readonly snack: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.connection$ = this.route.paramMap.pipe(
      tap(() => { this.loading$.next(true); this.error$.next(null); }),
      switchMap(p => {
        const id = p.get('id') ?? '';
        // OWASP A03 — UUID regex on path id before backend call
        if (!UUID_REGEX.test(id) && !id.startsWith('itsm-')) {
          this.error$.next('Identifiant invalide.');
          this.loading$.next(false);
          return of(null);
        }
        this.connectionId = id;
        return this.refresh$.pipe(
          switchMap(() => forkJoin({
            connection: this.svc.get(id).pipe(catchError(err => {
              this.error$.next(safeErrorMessage(err, 'Erreur lors du chargement.'));
              return of(null);
            })),
            mappings: this.svc.listMappings(id).pipe(catchError(() => of({
              content: [], totalElements: 0, totalPages: 0, number: 0, size: 0
            })))
          })),
          tap(({ mappings }) => {
            this.loading$.next(false);
            this.mappings = mappings.content;
          }),
          switchMap(({ connection }) => of(connection))
        );
      })
    );
  }

  openEdit(c: ConnectionResponse): void {
    const ref = this.dialog.open(ItsmConnectionDialogComponent, {
      data: { connection: c }, panelClass: 'qos-dialog-panel',
      autoFocus: 'first-tabbable', restoreFocus: true
    });
    ref.afterClosed().subscribe(updated => { if (updated) this.refresh$.next(); });
  }

  sync(c: ConnectionResponse): void {
    if (this.syncing) return;
    this.syncing = true;
    this.svc.sync(c.id).subscribe({
      next: r => {
        this.lastSync = r;
        this.syncing = false;
        if (r.errorMessage) {
          this.snack.open('Sync terminée avec erreur — voir détail.', 'OK', { duration: 4000 });
        } else {
          this.snack.open(`Sync OK — ${r.newImports} nouveau(x), ${r.alreadyKnown} déjà connu(s).`,
            'OK', { duration: 3000 });
        }
        this.refresh$.next();
      },
      error: err => {
        this.syncing = false;
        // eslint-disable-next-line no-console
        console.warn('[itsm-detail] sync failed', err?.status, err?.error?.title);
        this.snack.open(safeErrorMessage(err, 'Synchronisation impossible.'), 'OK', { duration: 4000 });
      }
    });
  }

  remove(c: ConnectionResponse): void {
    const ref = this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Supprimer la connexion ?',
        message: 'La connexion « ' + c.name + ' » et ses ' + this.mappings.length + ' mapping(s) seront supprimés. Le secret chiffré sera révoqué.',
        confirmLabel: 'Supprimer', cancelLabel: 'Annuler', danger: true
      }
    });
    ref.afterClosed().subscribe(ok => {
      if (!ok) return;
      this.svc.delete(c.id).subscribe({
        next: () => { this.snack.open('Connexion supprimée.', 'OK', { duration: 2200 }); this.router.navigate(['/itsm']); },
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[itsm-detail] delete failed', err?.status, err?.error?.title);
          this.snack.open(safeErrorMessage(err, 'Suppression impossible.'), 'OK', { duration: 4000 });
        }
      });
    });
  }

  providerLabel(p: ItsmProvider): string { return p === 'SERVICENOW' ? 'ServiceNow' : 'Jira SM'; }
  providerBadge(p: ItsmProvider): string { return 'pbadge pbadge-' + p.toLowerCase(); }
  statusBadge(s: ConnectionStatus): string { return 'badge badge-' + s.toLowerCase(); }
}
