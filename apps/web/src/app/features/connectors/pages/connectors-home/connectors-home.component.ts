import { ComponentType } from '@angular/cdk/portal';
import { Component, OnInit } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { PageEvent } from '@angular/material/paginator';
import { MatSnackBar } from '@angular/material/snack-bar';
import { BehaviorSubject, Observable, of } from 'rxjs';
import { catchError, finalize, map, shareReplay, switchMap, tap } from 'rxjs/operators';

import { safeErrorMessage } from '../../../../core/http/error-message';
import { deferredView } from '../../../../core/rx/deferred-view';
import { ConfirmDialogComponent } from '../../../../shared/ui/confirm-dialog/confirm-dialog.component';
import {
  ConnectorTone, commProviderLabel, connectorStatusLabel, connectorStatusTone,
  ehrAuthModeLabel, ehrProviderLabel, erpProviderLabel
} from '../../connectors.labels';
import { ConnectorsService } from '../../connectors.service';
import {
  CONNECTOR_MAX_PAGE_SIZE,
  CommConnection, CommTestResult,
  ConnectorPage, ConnectorRow, ConnectorStatus,
  EhrConnection, EhrSyncReport,
  ErpConnection, ErpSyncReport
} from '../../connectors.types';
import {
  CommConnectionDialogComponent, CommConnectionDialogData
} from '../comm-connection-dialog/comm-connection-dialog.component';
import {
  EhrConnectionDialogComponent, EhrConnectionDialogData
} from '../ehr-connection-dialog/ehr-connection-dialog.component';
import {
  ErpConnectionDialogComponent, ErpConnectionDialogData
} from '../erp-connection-dialog/erp-connection-dialog.component';

/** Vue d'une famille : la page serveur, plus ce que l'écran en déduit honnêtement. */
interface FamilyView<T extends ConnectorRow> {
  rows: T[];
  total: number;
  /** Vrai quand la page couvre TOUT le jeu : condition pour afficher des compteurs justes. */
  complete: boolean;
  activeCount: number;
  disabledCount: number;
  failingCount: number;
}

/** Résultat de la dernière synchronisation ERP, rattaché à sa connexion. */
interface ErpOutcome {
  connectionName: string;
  report: ErpSyncReport;
}

interface EhrOutcome {
  connectionName: string;
  report: EhrSyncReport;
}

interface CommOutcome {
  connectionName: string;
  result: CommTestResult;
}

interface PageQuery {
  page: number;
  size: number;
}

const EMPTY_PAGE = { content: [], totalElements: 0, totalPages: 0, number: 0, size: 0 };

/**
 * Configuration des connecteurs tiers — ERP, EHR/FHIR et Communication (§13.3).
 *
 * Trois contrôleurs, un seul écran : ce sont trois déclinaisons de la même décision
 * d'administration (« à quel système tiers la plateforme parle-t-elle, avec quel
 * secret, et cela fonctionne-t-il encore ? »). Les séparer en trois entrées de menu
 * aurait dispersé une configuration qu'on relit toujours d'un bloc.
 *
 * Les trois familles sont chargées dès l'ouverture, et non à la sélection d'onglet :
 * les compteurs affichés sur les onglets doivent être justes avant qu'on les ouvre,
 * sans quoi ils n'aideraient pas à choisir où regarder.
 *
 * Ce que l'écran ne promet PAS : il n'existe pas de test de connexion pour l'ERP ni
 * pour le FHIR côté serveur — seule la synchronisation existe, et elle importe
 * réellement des données. Seule la famille Communication expose un test (envoi d'un
 * message réel dans le salon).
 */
@Component({
  selector: 'qos-connectors-home',
  templateUrl: './connectors-home.component.html',
  styleUrls: ['./connectors-home.component.scss'],
  standalone: false
})
export class ConnectorsHomeComponent implements OnInit {

  readonly erpColumns = ['name', 'endpoint', 'status', 'sync', 'actions'];
  readonly ehrColumns = ['name', 'endpoint', 'status', 'sync', 'actions'];
  readonly commColumns = ['name', 'channel', 'status', 'activity', 'actions'];

  /** Bornes alignées sur le plafond serveur : au-delà, la page serait rabotée en silence. */
  readonly pageSizes = [10, 20, 50, CONNECTOR_MAX_PAGE_SIZE];

  erp$!: Observable<FamilyView<ErpConnection>>;
  ehr$!: Observable<FamilyView<EhrConnection>>;
  comm$!: Observable<FamilyView<CommConnection>>;

  private readonly erpLoadingState$ = new BehaviorSubject<boolean>(false);
  readonly erpLoading$ = deferredView(this.erpLoadingState$);
  private readonly erpErrorState$ = new BehaviorSubject<string | null>(null);
  readonly erpError$ = deferredView(this.erpErrorState$);

  private readonly ehrLoadingState$ = new BehaviorSubject<boolean>(false);
  readonly ehrLoading$ = deferredView(this.ehrLoadingState$);
  private readonly ehrErrorState$ = new BehaviorSubject<string | null>(null);
  readonly ehrError$ = deferredView(this.ehrErrorState$);

  private readonly commLoadingState$ = new BehaviorSubject<boolean>(false);
  readonly commLoading$ = deferredView(this.commLoadingState$);
  private readonly commErrorState$ = new BehaviorSubject<string | null>(null);
  readonly commError$ = deferredView(this.commErrorState$);

  /** Action en vol : évite le double-clic et signale l'attente sur la bonne ligne. */
  pendingId: string | null = null;

  /** Dernier compte rendu, affiché tant que l'administrateur ne l'a pas écarté. */
  erpOutcome: ErpOutcome | null = null;
  ehrOutcome: EhrOutcome | null = null;
  commOutcome: CommOutcome | null = null;

  private readonly erpQuery$ = new BehaviorSubject<PageQuery>({ page: 0, size: 20 });
  private readonly ehrQuery$ = new BehaviorSubject<PageQuery>({ page: 0, size: 20 });
  private readonly commQuery$ = new BehaviorSubject<PageQuery>({ page: 0, size: 20 });

  constructor(
    private readonly svc: ConnectorsService,
    private readonly dialog: MatDialog,
    private readonly snack: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.erp$ = this.erpQuery$.pipe(
      tap(() => { this.erpErrorState$.next(null); this.erpLoadingState$.next(true); }),
      switchMap(q => this.svc.listErp(q.page, q.size).pipe(
        catchError(err => {
          this.erpErrorState$.next(safeErrorMessage(err,
            $localize`:@@connectors.erp.load-error:Impossible de charger les connexions ERP.`));
          return of(EMPTY_PAGE as ConnectorPage<ErpConnection>);
        }),
        finalize(() => this.erpLoadingState$.next(false))
      )),
      map(page => this.toView(page)),
      // refCount:false : le compteur de l'onglet, les tuiles, le tableau et le paginateur
      // s'abonnent séparément — un abonnement tardif ne doit pas relancer la requête.
      shareReplay({ bufferSize: 1, refCount: false })
    );

    this.ehr$ = this.ehrQuery$.pipe(
      tap(() => { this.ehrErrorState$.next(null); this.ehrLoadingState$.next(true); }),
      switchMap(q => this.svc.listEhr(q.page, q.size).pipe(
        catchError(err => {
          this.ehrErrorState$.next(safeErrorMessage(err,
            $localize`:@@connectors.ehr.load-error:Impossible de charger les connexions FHIR.`));
          return of(EMPTY_PAGE as ConnectorPage<EhrConnection>);
        }),
        finalize(() => this.ehrLoadingState$.next(false))
      )),
      map(page => this.toView(page)),
      shareReplay({ bufferSize: 1, refCount: false })
    );

    this.comm$ = this.commQuery$.pipe(
      tap(() => { this.commErrorState$.next(null); this.commLoadingState$.next(true); }),
      switchMap(q => this.svc.listComm(q.page, q.size).pipe(
        catchError(err => {
          this.commErrorState$.next(safeErrorMessage(err,
            $localize`:@@connectors.comm.load-error:Impossible de charger les destinations de notification.`));
          return of(EMPTY_PAGE as ConnectorPage<CommConnection>);
        }),
        finalize(() => this.commLoadingState$.next(false))
      )),
      map(page => this.toView(page)),
      shareReplay({ bufferSize: 1, refCount: false })
    );
  }

  // ---- ERP -------------------------------------------------------------------

  createErp(): void {
    const data: ErpConnectionDialogData = { connection: null };
    this.openDialog(ErpConnectionDialogComponent, data, () => {
      this.erpQuery$.next({ page: 0, size: this.erpQuery$.value.size });
      this.notify($localize`:@@connectors.created:Connexion enregistrée.`);
    });
  }

  editErp(connection: ErpConnection): void {
    if (this.pendingId) return;
    const data: ErpConnectionDialogData = { connection };
    this.openDialog(ErpConnectionDialogComponent, data, () => {
      this.reloadErp();
      this.notify($localize`:@@connectors.updated:Connexion mise à jour.`);
    });
  }

  /**
   * Le serveur refuse de synchroniser une connexion non ACTIVE (il renvoie un rapport
   * vide assorti d'un message) : on ne propose donc l'action que quand elle agit.
   */
  canSync(connection: ConnectorRow): boolean {
    return connection.status === 'ACTIVE';
  }

  syncErp(connection: ErpConnection): void {
    if (this.pendingId) return;
    this.pendingId = connection.id;
    this.svc.syncErp(connection.id)
      .pipe(finalize(() => { this.pendingId = null; }))
      .subscribe({
        next: report => {
          this.erpOutcome = { connectionName: connection.name, report };
          // La sync déplace lastSyncAt, le compteur d'échecs et parfois le statut :
          // seul un rechargement dit l'état réel de la connexion.
          this.reloadErp();
        },
        error: err => this.fail(err,
          $localize`:@@connectors.erp.sync-error:La synchronisation ERP a échoué.`)
      });
  }

  toggleErpStatus(connection: ErpConnection): void {
    const next = connection.status === 'ACTIVE' ? 'DISABLED' : 'ACTIVE';
    this.run(connection.id, () => this.svc.updateErp(connection.id, { status: next }),
      () => this.reloadErp(), next === 'ACTIVE');
  }

  removeErp(connection: ErpConnection): void {
    this.confirmDelete(connection, () => {
      this.pendingId = connection.id;
      this.svc.deleteErp(connection.id)
        .pipe(finalize(() => { this.pendingId = null; }))
        .subscribe({
          next: () => {
            if (this.erpOutcome?.report.connectionId === connection.id) this.erpOutcome = null;
            this.notify($localize`:@@connectors.deleted:Connexion supprimée.`);
            this.reloadErp();
          },
          error: err => this.fail(err, $localize`:@@common.error-delete:Erreur lors de la suppression.`)
        });
    });
  }

  onErpPage(event: PageEvent): void {
    this.erpQuery$.next({ page: event.pageIndex, size: event.pageSize });
  }

  get erpPageIndex(): number { return this.erpQuery$.value.page; }
  get erpPageSize(): number { return this.erpQuery$.value.size; }

  erpProvider(connection: ErpConnection): string {
    return erpProviderLabel(connection.provider);
  }

  /**
   * Le serveur renvoie 200 même quand la synchronisation a échoué : c'est le message
   * d'erreur du rapport, pas le code HTTP, qui distingue succès et échec.
   */
  erpOutcomeTone(outcome: ErpOutcome): ConnectorTone {
    return outcome.report.errorMessage ? 'danger' : 'success';
  }

  dismissErpOutcome(): void { this.erpOutcome = null; }

  // ---- EHR -------------------------------------------------------------------

  createEhr(): void {
    const data: EhrConnectionDialogData = { connection: null };
    this.openDialog(EhrConnectionDialogComponent, data, () => {
      this.ehrQuery$.next({ page: 0, size: this.ehrQuery$.value.size });
      this.notify($localize`:@@connectors.created:Connexion enregistrée.`);
    });
  }

  editEhr(connection: EhrConnection): void {
    if (this.pendingId) return;
    const data: EhrConnectionDialogData = { connection };
    this.openDialog(EhrConnectionDialogComponent, data, () => {
      this.reloadEhr();
      this.notify($localize`:@@connectors.updated:Connexion mise à jour.`);
    });
  }

  syncEhr(connection: EhrConnection): void {
    if (this.pendingId) return;
    this.pendingId = connection.id;
    this.svc.syncEhr(connection.id)
      .pipe(finalize(() => { this.pendingId = null; }))
      .subscribe({
        next: report => {
          this.ehrOutcome = { connectionName: connection.name, report };
          this.reloadEhr();
        },
        error: err => this.fail(err,
          $localize`:@@connectors.ehr.sync-error:La synchronisation FHIR a échoué.`)
      });
  }

  toggleEhrStatus(connection: EhrConnection): void {
    const next = connection.status === 'ACTIVE' ? 'DISABLED' : 'ACTIVE';
    this.run(connection.id, () => this.svc.updateEhr(connection.id, { status: next }),
      () => this.reloadEhr(), next === 'ACTIVE');
  }

  removeEhr(connection: EhrConnection): void {
    this.confirmDelete(connection, () => {
      this.pendingId = connection.id;
      this.svc.deleteEhr(connection.id)
        .pipe(finalize(() => { this.pendingId = null; }))
        .subscribe({
          next: () => {
            if (this.ehrOutcome?.report.connectionId === connection.id) this.ehrOutcome = null;
            this.notify($localize`:@@connectors.deleted:Connexion supprimée.`);
            this.reloadEhr();
          },
          error: err => this.fail(err, $localize`:@@common.error-delete:Erreur lors de la suppression.`)
        });
    });
  }

  onEhrPage(event: PageEvent): void {
    this.ehrQuery$.next({ page: event.pageIndex, size: event.pageSize });
  }

  get ehrPageIndex(): number { return this.ehrQuery$.value.page; }
  get ehrPageSize(): number { return this.ehrQuery$.value.size; }

  ehrProvider(connection: EhrConnection): string {
    return ehrProviderLabel(connection.provider);
  }

  ehrAuth(connection: EhrConnection): string {
    return ehrAuthModeLabel(connection.authMode);
  }

  ehrOutcomeTone(outcome: EhrOutcome): ConnectorTone {
    if (outcome.report.errorMessage) return 'danger';
    return outcome.report.errors > 0 ? 'warn' : 'success';
  }

  dismissEhrOutcome(): void { this.ehrOutcome = null; }

  // ---- Communication ---------------------------------------------------------

  createComm(): void {
    const data: CommConnectionDialogData = { connection: null };
    this.openDialog(CommConnectionDialogComponent, data, () => {
      this.commQuery$.next({ page: 0, size: this.commQuery$.value.size });
      this.notify($localize`:@@connectors.created:Connexion enregistrée.`);
    });
  }

  editComm(connection: CommConnection): void {
    if (this.pendingId) return;
    const data: CommConnectionDialogData = { connection };
    this.openDialog(CommConnectionDialogComponent, data, () => {
      this.reloadComm();
      this.notify($localize`:@@connectors.updated:Connexion mise à jour.`);
    });
  }

  /**
   * Envoie un VRAI message dans le salon : c'est le seul moyen de valider une URL de
   * webhook que le serveur ne relit jamais. Réservé aux connexions actives, comme la
   * synchronisation.
   */
  testComm(connection: CommConnection): void {
    if (this.pendingId) return;
    this.pendingId = connection.id;
    this.svc.testComm(connection.id)
      .pipe(finalize(() => { this.pendingId = null; }))
      .subscribe({
        next: result => {
          this.commOutcome = { connectionName: connection.name, result };
          // Un test réussi remet le compteur d'échecs à zéro, un test raté l'incrémente.
          this.reloadComm();
        },
        error: err => this.fail(err,
          $localize`:@@connectors.comm.test-error:L'envoi du message de test a échoué.`)
      });
  }

  toggleCommStatus(connection: CommConnection): void {
    const next = connection.status === 'ACTIVE' ? 'DISABLED' : 'ACTIVE';
    this.run(connection.id, () => this.svc.updateComm(connection.id, { status: next }),
      () => this.reloadComm(), next === 'ACTIVE');
  }

  removeComm(connection: CommConnection): void {
    this.confirmDelete(connection, () => {
      this.pendingId = connection.id;
      this.svc.deleteComm(connection.id)
        .pipe(finalize(() => { this.pendingId = null; }))
        .subscribe({
          next: () => {
            if (this.commOutcome?.result.connectionId === connection.id) this.commOutcome = null;
            this.notify($localize`:@@connectors.deleted:Connexion supprimée.`);
            this.reloadComm();
          },
          error: err => this.fail(err, $localize`:@@common.error-delete:Erreur lors de la suppression.`)
        });
    });
  }

  onCommPage(event: PageEvent): void {
    this.commQuery$.next({ page: event.pageIndex, size: event.pageSize });
  }

  get commPageIndex(): number { return this.commQuery$.value.page; }
  get commPageSize(): number { return this.commQuery$.value.size; }

  commProvider(connection: CommConnection): string {
    return commProviderLabel(connection.provider);
  }

  commOutcomeTone(outcome: CommOutcome): ConnectorTone {
    return outcome.result.success ? 'success' : 'danger';
  }

  dismissCommOutcome(): void { this.commOutcome = null; }

  // ---- Présentation commune --------------------------------------------------

  /**
   * Le bouton de bascule change de sens selon la ligne : son libellé accessible doit
   * suivre, sinon un lecteur d'écran annonce une action pour l'autre.
   */
  readonly disableTooltip = $localize`:@@connectors.action-disable:Désactiver la connexion`;
  readonly enableTooltip = $localize`:@@connectors.action-enable:Réactiver la connexion`;

  statusLabel(status: ConnectorStatus): string {
    return connectorStatusLabel(status);
  }

  statusTone(status: ConnectorStatus): ConnectorTone {
    return connectorStatusTone(status);
  }

  /** Une connexion inactive se réactive ; une connexion active se met en pause. */
  isActive(connection: ConnectorRow): boolean {
    return connection.status === 'ACTIVE';
  }

  isFailing(connection: ConnectorRow): boolean {
    return connection.consecutiveFailures > 0;
  }

  trackById(_index: number, connection: ConnectorRow): string {
    return connection.id;
  }

  // ---- Interne ---------------------------------------------------------------

  /**
   * Ouvre un formulaire de connexion et n'agit que si le dialogue a réellement produit
   * une connexion (annulation = aucun rechargement, aucun message).
   *
   * Le typage est volontairement lâche sur la donnée d'entrée : les trois familles ont
   * des formulaires distincts, mais la mécanique d'ouverture est strictement la même.
   */
  private openDialog(
    component: ComponentType<unknown>,
    data: ErpConnectionDialogData | EhrConnectionDialogData | CommConnectionDialogData,
    onSaved: () => void
  ): void {
    this.dialog.open(component, {
      data, panelClass: 'qos-dialog-panel', autoFocus: 'first-tabbable', restoreFocus: true
    }).afterClosed().subscribe(saved => {
      if (saved) onSaved();
    });
  }

  /** Confirmation obligatoire : supprimer une connexion coupe un flux d'intégration en production. */
  private confirmDelete(connection: ConnectorRow, onConfirmed: () => void): void {
    if (this.pendingId) return;
    this.dialog.open(ConfirmDialogComponent, {
      panelClass: 'qos-dialog-panel', autoFocus: 'first-tabbable', restoreFocus: true,
      data: {
        title: $localize`:@@connectors.delete-title:Supprimer cette connexion ?`,
        message: $localize`:@@connectors.delete-message:Le secret associé est détruit et les échanges avec ce système cessent immédiatement. Cette action est irréversible.`,
        confirmLabel: $localize`:@@common.delete:Supprimer`,
        cancelLabel: $localize`:@@common.cancel:Annuler`,
        destructive: true
      }
    }).afterClosed().subscribe(confirmed => {
      if (confirmed) onConfirmed();
    });
  }

  /**
   * Exécute une transition d'état puis recharge la famille concernée.
   *
   * L'action est une FABRIQUE, pas un observable déjà construit : quand le garde-fou
   * anti-double-clic bloque, rien ne doit avoir été fabriqué ni envoyé.
   */
  private run(id: string, action: () => Observable<unknown>, reload: () => void, activating: boolean): void {
    if (this.pendingId) return;
    this.pendingId = id;
    action().pipe(finalize(() => { this.pendingId = null; })).subscribe({
      next: () => {
        this.notify(activating
          ? $localize`:@@connectors.enabled:Connexion réactivée.`
          : $localize`:@@connectors.disabled:Connexion désactivée.`);
        reload();
      },
      error: err => this.fail(err, $localize`:@@common.error-update:Erreur lors de la mise à jour.`)
    });
  }

  private reloadErp(): void { this.erpQuery$.next({ ...this.erpQuery$.value }); }
  private reloadEhr(): void { this.ehrQuery$.next({ ...this.ehrQuery$.value }); }
  private reloadComm(): void { this.commQuery$.next({ ...this.commQuery$.value }); }

  private notify(message: string): void {
    this.snack.open(message, $localize`:@@common.ok:OK`, { duration: 3000 });
  }

  private fail(err: unknown, fallback: string): void {
    this.snack.open(safeErrorMessage(err, fallback), $localize`:@@common.ok:OK`, { duration: 5000 });
  }

  private toView<T extends ConnectorRow>(page: ConnectorPage<T>): FamilyView<T> {
    const rows = page.content ?? [];
    const total = page.totalElements ?? rows.length;
    return {
      rows,
      total,
      // Les compteurs ne portent que sur ce qui est chargé : les afficher alors que la
      // page ne couvre pas tout le jeu ferait mentir l'écran.
      complete: rows.length === total,
      activeCount: rows.filter(r => r.status === 'ACTIVE').length,
      disabledCount: rows.filter(r => r.status !== 'ACTIVE').length,
      failingCount: rows.filter(r => r.consecutiveFailures > 0).length
    };
  }
}
