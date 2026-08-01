import { Component, OnInit } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { BehaviorSubject, Observable, Subject, of } from 'rxjs';
import { catchError, finalize, map, shareReplay, startWith, switchMap, tap } from 'rxjs/operators';

import { safeErrorMessage } from '../../../../core/http/error-message';
import { deferredView } from '../../../../core/rx/deferred-view';
import {
  ConfirmDialogComponent, ConfirmDialogData
} from '../../../../shared/ui/confirm-dialog/confirm-dialog.component';
import { ApiKeysService } from '../../api-keys.service';
import {
  API_KEY_EXPIRING_SOON_DAYS, ApiKeyCounts, ApiKeyStatus, ApiKeyView, IssuedApiKey,
  MissingActorError
} from '../../api-keys.types';
import { ApiKeyCreateDialogComponent } from '../api-key-create-dialog/api-key-create-dialog.component';
import {
  ApiKeyRevealDialogComponent, ApiKeyRevealDialogData
} from '../api-key-reveal-dialog/api-key-reveal-dialog.component';

/** Ligne de synthèse affichée en tête d'écran. */
interface SummaryTile {
  label: string;
  value: number;
  tone: 'neutral' | 'success' | 'warn' | 'danger';
}

/**
 * Console d'administration — clés d'API du tenant (§13.1).
 *
 * L'API `/api/v1/api-keys` existait sans écran : émettre ou révoquer une clé imposait
 * un appel HTTP manuel, ce qui rendait de fait impossible la révocation d'urgence.
 *
 * Deux partis pris dictés par le modèle serveur :
 *  - le secret n'est renvoyé qu'à la création et à la rotation ; il est donc affiché
 *    dans un dialogue non fermable par mégarde, jamais dans le tableau ;
 *  - une clé non ACTIVE n'accepte plus aucune opération (`ApiKey.revoke` lève sur un
 *    statut autre) : les actions correspondantes ne sont pas affichées du tout, plutôt
 *    que proposées puis refusées par le serveur.
 */
@Component({
  selector: 'qos-api-keys-list',
  templateUrl: './api-keys-list.component.html',
  styleUrls: ['./api-keys-list.component.scss'],
  standalone: false
})
export class ApiKeysListComponent implements OnInit {

  readonly displayedColumns = ['name', 'scopes', 'status', 'created', 'lastUsed', 'expiry', 'actions'];

  readonly expiringSoonDays = API_KEY_EXPIRING_SOON_DAYS;

  keys$!: Observable<ApiKeyView[]>;
  tiles$!: Observable<SummaryTile[]>;
  /** Fiche complète de la clé sélectionnée : le tableau ne peut pas montrer 12 champs. */
  detail$!: Observable<ApiKeyView | null>;

  private readonly loadingState$ = new BehaviorSubject<boolean>(false);
  readonly loading$ = deferredView(this.loadingState$);
  private readonly errorState$ = new BehaviorSubject<string | null>(null);
  readonly error$ = this.errorState$.asObservable();
  private readonly detailLoadingState$ = new BehaviorSubject<boolean>(false);
  readonly detailLoading$ = deferredView(this.detailLoadingState$);
  detailError: string | null = null;

  /** Action en cours sur une ligne : évite le double-clic et signale l'attente. */
  pendingKeyId: string | null = null;
  /** Traitement global (expiration des clés échues) en cours. */
  maintenanceRunning = false;
  selectedId: string | null = null;

  private readonly reload$ = new Subject<void>();
  private readonly selection$ = new BehaviorSubject<string | null>(null);

  constructor(
    private readonly svc: ApiKeysService,
    private readonly dialog: MatDialog,
    private readonly snack: MatSnackBar
  ) {}

  ngOnInit(): void {
    const overview$ = this.reload$.pipe(
      startWith(undefined),
      tap(() => { this.errorState$.next(null); this.loadingState$.next(true); }),
      switchMap(() => this.svc.overview().pipe(
        catchError(err => {
          this.errorState$.next(safeErrorMessage(err,
            $localize`:@@admin.api-keys.load-error:Impossible de charger les clés d'API.`));
          return [];
        }),
        finalize(() => this.loadingState$.next(false))
      )),
      // refCount:false : le tableau, les tuiles et l'état vide s'abonnent séparément et
      // certains sont derrière un *ngIf — un abonnement tardif ne doit pas relancer une
      // seconde requête.
      shareReplay({ bufferSize: 1, refCount: false })
    );

    this.keys$ = overview$.pipe(map(o => o.keys));
    this.tiles$ = overview$.pipe(map(o => this.toTiles(o.counts)));

    this.detail$ = this.selection$.pipe(
      tap(() => { this.detailError = null; }),
      switchMap(id => {
        if (!id) return of(null);
        // Relecture ciblée plutôt que réutilisation de la ligne : `lastUsedAt` bouge à
        // chaque appel d'API, la valeur du tableau est vite périmée.
        this.detailLoadingState$.next(true);
        return this.svc.get(id).pipe(
          catchError(err => {
            this.detailError = safeErrorMessage(err,
              $localize`:@@admin.api-keys.detail-error:Impossible de charger le détail de la clé.`);
            return of(null);
          }),
          finalize(() => this.detailLoadingState$.next(false))
        );
      }),
      shareReplay({ bufferSize: 1, refCount: false })
    );
  }

  // ---- Actions ---------------------------------------------------------------

  refresh(): void {
    this.reload$.next();
    if (this.selectedId) this.selection$.next(this.selectedId);
  }

  create(): void {
    const ref = this.dialog.open<ApiKeyCreateDialogComponent, void, IssuedApiKey>(
      ApiKeyCreateDialogComponent,
      { panelClass: 'qos-dialog-panel', autoFocus: 'first-tabbable', restoreFocus: true }
    );
    ref.afterClosed().subscribe(issued => {
      if (!issued) return;
      this.reveal(issued, false);
      this.refresh();
    });
  }

  /**
   * Rotation = révocation immédiate + nouvelle clé côté serveur. C'est irréversible
   * pour les appelants en production : on confirme avant.
   */
  rotate(key: ApiKeyView): void {
    if (!this.canRotate(key) || this.pendingKeyId) return;
    this.confirm({
      title: $localize`:@@admin.api-keys.rotate-confirm-title:Faire tourner cette clé ?`,
      message: $localize`:@@admin.api-keys.rotate-confirm-message:La clé actuelle est révoquée immédiatement et un nouveau secret est émis. Les intégrations qui utilisent encore l'ancienne clé cesseront de fonctionner.`,
      confirmLabel: $localize`:@@admin.api-keys.rotate:Faire tourner`,
      cancelLabel: $localize`:@@admin.api-keys.cancel:Annuler`,
      destructive: true
    }, () => {
      this.pendingKeyId = key.id;
      this.errorState$.next(null);
      this.svc.rotate(key.id)
        .pipe(finalize(() => { this.pendingKeyId = null; }))
        .subscribe({
          next: issued => { this.reveal(issued, true); this.refresh(); },
          error: err => this.fail(err,
            $localize`:@@admin.api-keys.rotate-error:La rotation de la clé a échoué.`)
        });
    });
  }

  revoke(key: ApiKeyView): void {
    if (!this.canRevoke(key) || this.pendingKeyId) return;
    this.confirm({
      title: $localize`:@@admin.api-keys.revoke-confirm-title:Révoquer cette clé ?`,
      message: $localize`:@@admin.api-keys.revoke-confirm-message:Action irréversible : la clé est rejetée dès la validation et ne peut pas être réactivée. Toute intégration qui l'utilise cessera de fonctionner.`,
      confirmLabel: $localize`:@@admin.api-keys.revoke:Révoquer`,
      cancelLabel: $localize`:@@admin.api-keys.cancel:Annuler`,
      destructive: true
    }, () => {
      this.pendingKeyId = key.id;
      this.errorState$.next(null);
      this.svc.revoke(key.id)
        .pipe(finalize(() => { this.pendingKeyId = null; }))
        .subscribe({
          next: () => {
            this.notify($localize`:@@admin.api-keys.revoked:Clé révoquée.`);
            this.refresh();
          },
          error: err => this.fail(err,
            $localize`:@@admin.api-keys.revoke-error:La révocation de la clé a échoué.`)
        });
    });
  }

  /** Maintenance : bascule en EXPIRED les clés actives dont l'échéance est passée. */
  expireDue(): void {
    if (this.maintenanceRunning) return;
    this.maintenanceRunning = true;
    this.errorState$.next(null);
    this.svc.expireDue()
      .pipe(finalize(() => { this.maintenanceRunning = false; }))
      .subscribe({
        next: count => {
          this.notify(count > 0
            ? $localize`:@@admin.api-keys.expire-due-done:Clés échues traitées : ${count}:COUNT:`
            : $localize`:@@admin.api-keys.expire-due-none:Aucune clé échue à traiter.`);
          this.refresh();
        },
        error: err => this.fail(err,
          $localize`:@@admin.api-keys.expire-due-error:Le traitement des clés échues a échoué.`)
      });
  }

  select(key: ApiKeyView): void {
    this.selectedId = key.id;
    this.selection$.next(key.id);
  }

  clearSelection(): void {
    this.selectedId = null;
    this.selection$.next(null);
  }

  // ---- Présentation ----------------------------------------------------------

  /** Seule une clé ACTIVE accepte encore une opération côté serveur. */
  /** Échéance dépassée, indépendamment du statut encore affiché par le serveur. */
  isExpired(key: ApiKeyView): boolean {
    return !!key.expiresAt && new Date(key.expiresAt).getTime() <= Date.now();
  }

  canRotate(key: ApiKeyView): boolean {
    // Une clé dont l'échéance est passée reste ACTIVE tant que le balayage
    // `expire-due` n'a pas tourné. Or la rotation d'une telle clé échoue côté
    // serveur au moment de réémettre (« expiresAt must be in the future »).
    // La proposer reviendrait à offrir un bouton qui ne peut que échouer.
    return key.status === 'ACTIVE' && !this.isExpired(key);
  }

  canRevoke(key: ApiKeyView): boolean {
    return key.status === 'ACTIVE';
  }

  statusTone(status: ApiKeyStatus): 'neutral' | 'success' | 'warn' | 'danger' {
    switch (status) {
      case 'ACTIVE': return 'success';
      case 'EXPIRED': return 'warn';
      case 'REVOKED': return 'danger';
      default: return 'neutral';
    }
  }

  /** Signale les clés à renouveler avant qu'elles ne coupent une intégration. */
  isExpiringSoon(key: ApiKeyView): boolean {
    if (key.status !== 'ACTIVE' || !key.expiresAt) return false;
    const due = Date.parse(key.expiresAt);
    if (Number.isNaN(due)) return false;
    return due <= Date.now() + API_KEY_EXPIRING_SOON_DAYS * 24 * 60 * 60 * 1000;
  }

  trackById(_index: number, key: ApiKeyView): string {
    return key.id;
  }

  // ---- Interne ---------------------------------------------------------------

  private toTiles(counts: ApiKeyCounts): SummaryTile[] {
    return [
      { label: $localize`:@@admin.api-keys.tile-active:Clés actives`, value: counts.active, tone: 'success' },
      { label: $localize`:@@admin.api-keys.tile-expiring:Expirent bientôt`, value: counts.expiringSoon, tone: 'warn' },
      { label: $localize`:@@admin.api-keys.tile-expired:Expirées`, value: counts.expired, tone: 'warn' },
      { label: $localize`:@@admin.api-keys.tile-revoked:Révoquées`, value: counts.revoked, tone: 'danger' },
      { label: $localize`:@@admin.api-keys.tile-total:Total`, value: counts.total, tone: 'neutral' }
    ];
  }

  /**
   * `disableClose` : le secret n'existe que dans ce dialogue et le serveur ne peut pas
   * le régénérer. Une fermeture par Échap ou clic hors cadre coûterait une rotation.
   */
  private reveal(issued: IssuedApiKey, rotated: boolean): void {
    this.dialog.open<ApiKeyRevealDialogComponent, ApiKeyRevealDialogData, void>(
      ApiKeyRevealDialogComponent,
      {
        data: { issued, rotated },
        panelClass: 'qos-dialog-panel',
        disableClose: true,
        autoFocus: 'first-tabbable',
        restoreFocus: true
      }
    );
  }

  private confirm(data: ConfirmDialogData, onConfirmed: () => void): void {
    this.dialog
      .open<ConfirmDialogComponent, ConfirmDialogData, boolean>(ConfirmDialogComponent, { data })
      .afterClosed()
      .subscribe(ok => { if (ok) onConfirmed(); });
  }

  private notify(message: string): void {
    this.snack.open(message, $localize`:@@admin.api-keys.ok:OK`, { duration: 3000 });
  }

  private fail(err: unknown, fallback: string): void {
    this.errorState$.next(err instanceof MissingActorError
      ? $localize`:@@admin.api-keys.session-expired:Session expirée — reconnectez-vous pour agir sur les clés d'API.`
      : safeErrorMessage(err, fallback));
  }
}
