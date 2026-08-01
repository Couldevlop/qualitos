import { Component, OnInit } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { PageEvent } from '@angular/material/paginator';
import { MatSnackBar } from '@angular/material/snack-bar';
import { BehaviorSubject, Observable, of } from 'rxjs';
import { catchError, finalize, map, shareReplay, switchMap, tap } from 'rxjs/operators';

import { safeErrorMessage } from '../../../../core/http/error-message';
import { deferredView } from '../../../../core/rx/deferred-view';
import { ConfirmDialogComponent } from '../../../../shared/ui/confirm-dialog/confirm-dialog.component';
import { WebhooksService } from '../../webhooks.service';
import {
  WebhookDelivery, WebhookDeliveryQuery, WebhookDeliveryStatus, WebhookPage,
  WebhookSubscription, WebhookSubscriptionStatus
} from '../../webhooks.types';
import {
  WebhookDeliveryDialogComponent, WebhookDeliveryDialogData
} from '../webhook-delivery-dialog/webhook-delivery-dialog.component';
import {
  WebhookFormDialogComponent, WebhookFormDialogData, WebhookFormDialogResult
} from '../webhook-form-dialog/webhook-form-dialog.component';
import {
  WebhookSecretDialogComponent, WebhookSecretDialogData
} from '../webhook-secret-dialog/webhook-secret-dialog.component';

type Tone = 'neutral' | 'success' | 'warn' | 'danger';

/** Vue des abonnements : la page serveur, plus ce que l'écran en déduit. */
interface SubscriptionsView {
  rows: WebhookSubscription[];
  total: number;
  /** Vrai quand la page contient TOUT le jeu : condition pour afficher des compteurs honnêtes. */
  complete: boolean;
  activeCount: number;
  pausedCount: number;
  disabledCount: number;
  failingCount: number;
}

/** Option du filtre de statut des livraisons. */
interface DeliveryStatusOption {
  value: WebhookDeliveryStatus | null;
  label: string;
}

const EMPTY_PAGE: WebhookPage<never> = {
  content: [], totalElements: 0, totalPages: 0, number: 0, size: 0
};

/**
 * Console d'administration — webhooks sortants (§13.2).
 *
 * L'API `/api/v1/webhooks` (9 routes) n'avait aucun consommateur : configurer un
 * abonnement, le tester ou comprendre pourquoi un consommateur ne reçoit plus rien
 * imposait de parler HTTP à la main.
 *
 * Deux plans distincts sur un même écran : la CONFIGURATION (abonnements, ce que l'admin
 * décide) et l'EXPLOITATION (livraisons, ce que la plateforme a réellement fait). Le lien
 * entre les deux est le bouton « Voir les livraisons » qui pré-filtre le second plan.
 */
@Component({
  selector: 'qos-webhooks-list',
  templateUrl: './webhooks-list.component.html',
  styleUrls: ['./webhooks-list.component.scss'],
  standalone: false
})
export class WebhooksListComponent implements OnInit {

  readonly subscriptionColumns = ['name', 'events', 'status', 'health', 'actions'];
  readonly deliveryColumns = ['event', 'subscription', 'status', 'attempts', 'lastAttempt', 'actions'];

  readonly subscriptionPageSizes = [10, 20, 50];
  readonly deliveryPageSizes = [10, 25, 50];

  subscriptions$!: Observable<SubscriptionsView>;
  deliveries$!: Observable<WebhookPage<WebhookDelivery>>;

  // Chargement ET erreur passent par `deferredView` : les deux sont poussés depuis les
  // opérateurs du flux (tap/catchError), donc pendant la passe de détection courante —
  // les livrer en macrotâche évite NG0100, y compris quand la bannière d'erreur est
  // rendue AVANT le tableau qui déclenche la requête.
  private readonly subsLoadingState$ = new BehaviorSubject<boolean>(false);
  readonly subsLoading$ = deferredView(this.subsLoadingState$);
  private readonly subsErrorState$ = new BehaviorSubject<string | null>(null);
  readonly subsError$ = deferredView(this.subsErrorState$);

  private readonly deliveriesLoadingState$ = new BehaviorSubject<boolean>(false);
  readonly deliveriesLoading$ = deferredView(this.deliveriesLoadingState$);
  private readonly deliveriesErrorState$ = new BehaviorSubject<string | null>(null);
  readonly deliveriesError$ = deferredView(this.deliveriesErrorState$);

  /** Action en cours : évite le double-clic et signale l'attente sur la bonne ligne. */
  pendingSubscriptionId: string | null = null;
  pendingDeliveryId: string | null = null;

  /** Critères courants du second plan ; exposés au template pour piloter les sélecteurs. */
  deliveryQuery: WebhookDeliveryQuery = {
    subscriptionId: null, status: null, page: 0, size: 25
  };

  readonly deliveryStatusOptions: DeliveryStatusOption[] = [
    { value: null, label: $localize`:@@common.all:Tous` },
    { value: 'FAILED', label: $localize`:@@admin.webhooks.delivery-status-failed:Échec` },
    { value: 'RETRYING', label: $localize`:@@admin.webhooks.delivery-status-retrying:Renvoi programmé` },
    { value: 'DEAD_LETTER', label: $localize`:@@admin.webhooks.delivery-status-dead:Abandonné` },
    { value: 'PENDING', label: $localize`:@@admin.webhooks.delivery-status-pending:En attente` },
    { value: 'SUCCESS', label: $localize`:@@admin.webhooks.delivery-status-success:Succès` }
  ];

  /** Liste servant à la fois le sélecteur de filtre et la résolution des noms d'abonnement. */
  private loadedSubscriptions: WebhookSubscription[] = [];

  private readonly subsQuery$ = new BehaviorSubject<{ page: number; size: number }>({ page: 0, size: 20 });
  private readonly deliveryQuery$ = new BehaviorSubject<WebhookDeliveryQuery>(this.deliveryQuery);

  constructor(
    private readonly svc: WebhooksService,
    private readonly dialog: MatDialog,
    private readonly snack: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.subscriptions$ = this.subsQuery$.pipe(
      tap(() => { this.subsErrorState$.next(null); this.subsLoadingState$.next(true); }),
      switchMap(q => this.svc.listSubscriptions(q.page, q.size).pipe(
        catchError(err => {
          this.subsErrorState$.next(safeErrorMessage(err,
            $localize`:@@admin.webhooks.load-error:Impossible de charger les abonnements webhook.`));
          return of(EMPTY_PAGE as WebhookPage<WebhookSubscription>);
        }),
        finalize(() => this.subsLoadingState$.next(false))
      )),
      map(page => this.toSubscriptionsView(page)),
      // refCount:false : le tableau, les compteurs, le paginateur et le sélecteur de
      // filtre s'abonnent séparément — un abonnement tardif ne doit pas relancer la requête.
      shareReplay({ bufferSize: 1, refCount: false })
    );

    this.deliveries$ = this.deliveryQuery$.pipe(
      tap(() => { this.deliveriesErrorState$.next(null); this.deliveriesLoadingState$.next(true); }),
      switchMap(q => this.svc.listDeliveries(q).pipe(
        catchError(err => {
          this.deliveriesErrorState$.next(safeErrorMessage(err,
            $localize`:@@admin.webhooks.deliveries-load-error:Impossible de charger les livraisons.`));
          return of(EMPTY_PAGE as WebhookPage<WebhookDelivery>);
        }),
        finalize(() => this.deliveriesLoadingState$.next(false))
      )),
      shareReplay({ bufferSize: 1, refCount: false })
    );
  }

  // ---- Abonnements : cycle de vie -------------------------------------------

  create(): void {
    this.openForm(null);
  }

  edit(sub: WebhookSubscription): void {
    this.openForm(sub);
  }

  /** ACTIVE → PAUSED : la plateforme cesse d'appeler l'endpoint sans perdre la configuration. */
  pause(sub: WebhookSubscription): void {
    this.runOnSubscription(sub, () => this.svc.updateSubscription(sub.id, { status: 'PAUSED' }),
      $localize`:@@admin.webhooks.paused:Abonnement mis en pause.`);
  }

  /**
   * PAUSED / DISABLED_ON_ERRORS → ACTIVE. Le serveur remet le compteur d'échecs
   * consécutifs à zéro quand il sortait d'une désactivation automatique.
   */
  resume(sub: WebhookSubscription): void {
    this.runOnSubscription(sub, () => this.svc.updateSubscription(sub.id, { status: 'ACTIVE' }),
      $localize`:@@admin.webhooks.resumed:Abonnement réactivé.`);
  }

  /** Ping synchrone : le seul moyen de valider endpoint + secret sans attendre un vrai évènement. */
  test(sub: WebhookSubscription): void {
    if (this.pendingSubscriptionId) return;
    this.pendingSubscriptionId = sub.id;
    this.svc.testPing(sub.id)
      .pipe(finalize(() => { this.pendingSubscriptionId = null; }))
      .subscribe({
        next: result => {
          const ok = result.status === 'SUCCESS';
          const message = ok
            ? $localize`:@@admin.webhooks.test-success:Ping de test accepté par le consommateur.`
            : $localize`:@@admin.webhooks.test-failed:Ping de test rejeté — consultez la livraison pour le détail.`;
          this.snack.open(message, $localize`:@@common.ok:OK`, { duration: ok ? 3000 : 5000 });
          // Le ping crée une livraison et bouge les compteurs : les deux plans changent.
          this.reloadSubscriptions();
          this.reloadDeliveries();
        },
        error: err => this.fail(err,
          $localize`:@@admin.webhooks.test-error:L'envoi du ping de test a échoué.`)
      });
  }

  /**
   * Rotation du secret : on en génère un neuf, on le pousse, puis on le montre une fois.
   * Confirmation obligatoire — l'ancien secret devient invalide immédiatement et tout
   * consommateur non mis à jour rejettera les appels suivants.
   */
  rotateSecret(sub: WebhookSubscription): void {
    if (this.pendingSubscriptionId) return;
    this.dialog.open(ConfirmDialogComponent, {
      panelClass: 'qos-dialog-panel', autoFocus: 'first-tabbable', restoreFocus: true,
      data: {
        title: $localize`:@@admin.webhooks.rotate-title:Renouveler le secret ?`,
        message: $localize`:@@admin.webhooks.rotate-message:Un nouveau secret HMAC sera généré et affiché une seule fois. L'ancien cessera immédiatement d'être valide.`,
        confirmLabel: $localize`:@@admin.webhooks.rotate-confirm:Renouveler`,
        cancelLabel: $localize`:@@common.cancel:Annuler`
      }
    }).afterClosed().subscribe(confirmed => {
      if (!confirmed) return;
      const secret = this.svc.generateSecret();
      this.pendingSubscriptionId = sub.id;
      this.svc.updateSubscription(sub.id, { secret })
        .pipe(finalize(() => { this.pendingSubscriptionId = null; }))
        .subscribe({
          next: updated => {
            this.showSecret(updated.name, secret, true);
            this.reloadSubscriptions();
          },
          error: err => this.fail(err,
            $localize`:@@common.error-update:Erreur lors de la mise à jour.`)
        });
    });
  }

  remove(sub: WebhookSubscription): void {
    if (this.pendingSubscriptionId) return;
    this.dialog.open(ConfirmDialogComponent, {
      panelClass: 'qos-dialog-panel', autoFocus: 'first-tabbable', restoreFocus: true,
      data: {
        title: $localize`:@@admin.webhooks.delete-title:Supprimer cet abonnement ?`,
        message: $localize`:@@admin.webhooks.delete-message:Les évènements ne seront plus transmis à cette destination. L'historique de livraison associé n'est plus consultable.`,
        confirmLabel: $localize`:@@common.delete:Supprimer`,
        cancelLabel: $localize`:@@common.cancel:Annuler`,
        destructive: true
      }
    }).afterClosed().subscribe(confirmed => {
      if (!confirmed) return;
      this.pendingSubscriptionId = sub.id;
      this.svc.deleteSubscription(sub.id)
        .pipe(finalize(() => { this.pendingSubscriptionId = null; }))
        .subscribe({
          next: () => {
            this.snack.open($localize`:@@admin.webhooks.deleted:Abonnement supprimé.`,
              $localize`:@@common.ok:OK`, { duration: 3000 });
            // Le filtre pointait peut-être sur l'abonnement disparu : on le relâche
            // pour ne pas laisser l'écran sur une liste vide inexplicable.
            if (this.deliveryQuery.subscriptionId === sub.id) {
              this.deliveryQuery = { ...this.deliveryQuery, subscriptionId: null, page: 0 };
            }
            this.reloadSubscriptions();
            this.reloadDeliveries();
          },
          error: err => this.fail(err,
            $localize`:@@common.error-delete:Erreur lors de la suppression.`)
        });
    });
  }

  // ---- Abonnements : présentation -------------------------------------------

  statusLabel(status: WebhookSubscriptionStatus): string {
    switch (status) {
      case 'ACTIVE': return $localize`:@@admin.webhooks.status-active:Actif`;
      case 'PAUSED': return $localize`:@@admin.webhooks.status-paused:En pause`;
      default: return $localize`:@@admin.webhooks.status-disabled:Désactivé sur erreurs`;
    }
  }

  statusTone(status: WebhookSubscriptionStatus): Tone {
    switch (status) {
      case 'ACTIVE': return 'success';
      case 'PAUSED': return 'warn';
      default: return 'danger';
    }
  }

  /** Un abonnement en pause ou désactivé se relance ; un abonnement actif se met en pause. */
  canResume(sub: WebhookSubscription): boolean {
    return sub.status !== 'ACTIVE';
  }

  /** Santé lisible : des échecs consécutifs annoncent la désactivation automatique. */
  isFailing(sub: WebhookSubscription): boolean {
    return sub.consecutiveFailures > 0;
  }

  trackBySubscription(_index: number, sub: WebhookSubscription): string {
    return sub.id;
  }

  // ---- Livraisons -----------------------------------------------------------

  /** Pré-filtre le plan exploitation sur un abonnement précis. */
  showDeliveriesFor(sub: WebhookSubscription): void {
    this.deliveryQuery = { ...this.deliveryQuery, subscriptionId: sub.id, page: 0 };
    this.reloadDeliveries();
  }

  onSubscriptionFilterChange(subscriptionId: string | null): void {
    this.deliveryQuery = { ...this.deliveryQuery, subscriptionId, page: 0 };
    this.reloadDeliveries();
  }

  onStatusFilterChange(status: WebhookDeliveryStatus | null): void {
    this.deliveryQuery = { ...this.deliveryQuery, status, page: 0 };
    this.reloadDeliveries();
  }

  /**
   * Le serveur applique le filtre d'abonnement OU le filtre de statut, jamais les deux.
   * On désactive donc le second quand le premier est posé, plutôt que d'afficher un
   * critère que l'API ignorerait silencieusement.
   */
  get statusFilterDisabled(): boolean {
    return this.deliveryQuery.subscriptionId !== null;
  }

  onDeliveryPage(event: PageEvent): void {
    this.deliveryQuery = { ...this.deliveryQuery, page: event.pageIndex, size: event.pageSize };
    this.reloadDeliveries();
  }

  onSubscriptionsPage(event: PageEvent): void {
    this.subsQuery$.next({ page: event.pageIndex, size: event.pageSize });
  }

  get subsPageIndex(): number {
    return this.subsQuery$.value.page;
  }

  get subsPageSize(): number {
    return this.subsQuery$.value.size;
  }

  /** Le rejeu est refusé par le serveur sur une livraison déjà réussie : on ne le propose pas. */
  canRetry(delivery: WebhookDelivery): boolean {
    return delivery.status !== 'SUCCESS';
  }

  retry(delivery: WebhookDelivery): void {
    if (this.pendingDeliveryId) return;
    this.pendingDeliveryId = delivery.id;
    this.svc.retryDelivery(delivery.id)
      .pipe(finalize(() => { this.pendingDeliveryId = null; }))
      .subscribe({
        next: updated => {
          const message = updated.status === 'SUCCESS'
            ? $localize`:@@admin.webhooks.retry-success:Livraison rejouée avec succès.`
            : $localize`:@@admin.webhooks.retry-failed:Nouvelle tentative échouée.`;
          this.snack.open(message, $localize`:@@common.ok:OK`, { duration: 3500 });
          this.reloadDeliveries();
          // Le rejeu modifie les compteurs d'échecs consécutifs de l'abonnement.
          this.reloadSubscriptions();
        },
        error: err => this.fail(err,
          $localize`:@@admin.webhooks.retry-error:Le rejeu de la livraison a échoué.`)
      });
  }

  openDelivery(delivery: WebhookDelivery): void {
    const data: WebhookDeliveryDialogData = {
      delivery,
      subscriptionName: this.subscriptionName(delivery.subscriptionId)
    };
    this.dialog.open(WebhookDeliveryDialogComponent, {
      data, panelClass: 'qos-dialog-panel', autoFocus: 'first-tabbable', restoreFocus: true
    });
  }

  /** Nom lisible d'un abonnement ; `null` si absent de la page chargée (on retombe sur l'id). */
  subscriptionName(subscriptionId: string): string | null {
    return this.loadedSubscriptions.find(s => s.id === subscriptionId)?.name ?? null;
  }

  /** Options du sélecteur d'abonnement du plan exploitation. */
  get subscriptionOptions(): WebhookSubscription[] {
    return this.loadedSubscriptions;
  }

  deliveryStatusLabel(status: WebhookDeliveryStatus): string {
    const found = this.deliveryStatusOptions.find(o => o.value === status);
    return found ? found.label : status;
  }

  deliveryStatusTone(status: WebhookDeliveryStatus): Tone {
    switch (status) {
      case 'SUCCESS': return 'success';
      case 'RETRYING': return 'warn';
      case 'PENDING': return 'neutral';
      default: return 'danger';
    }
  }

  trackByDelivery(_index: number, delivery: WebhookDelivery): string {
    return delivery.id;
  }

  // ---- Interne ---------------------------------------------------------------

  private openForm(subscription: WebhookSubscription | null): void {
    const data: WebhookFormDialogData = { subscription };
    this.dialog.open<WebhookFormDialogComponent, WebhookFormDialogData, WebhookFormDialogResult>(
      WebhookFormDialogComponent,
      { data, panelClass: 'qos-dialog-panel', autoFocus: 'first-tabbable', restoreFocus: true }
    ).afterClosed().subscribe(result => {
      if (!result) return;
      this.reloadSubscriptions();
      if (result.secret) {
        this.showSecret(result.subscription.name, result.secret, false);
      } else {
        this.snack.open($localize`:@@admin.webhooks.updated:Abonnement mis à jour.`,
          $localize`:@@common.ok:OK`, { duration: 3000 });
      }
    });
  }

  private showSecret(subscriptionName: string, secret: string, rotated: boolean): void {
    const data: WebhookSecretDialogData = { subscriptionName, secret, rotated };
    this.dialog.open(WebhookSecretDialogComponent, {
      data, panelClass: 'qos-dialog-panel', autoFocus: 'first-tabbable',
      restoreFocus: true, disableClose: true
    });
  }

  /**
   * Exécute une transition d'état sur un abonnement puis recharge la liste.
   *
   * Le rechargement complet est volontaire : le serveur ajuste aussi des compteurs
   * (échecs consécutifs remis à zéro à la réactivation) que la réponse d'une seule ligne
   * ne suffirait pas à refléter partout.
   *
   * L'action est une FABRIQUE, pas un observable déjà construit : quand le garde-fou
   * anti-double-clic bloque, rien ne doit avoir été fabriqué ni appelé.
   */
  private runOnSubscription(sub: WebhookSubscription, action: () => Observable<unknown>, success: string): void {
    if (this.pendingSubscriptionId) return;
    this.pendingSubscriptionId = sub.id;
    action().pipe(finalize(() => { this.pendingSubscriptionId = null; })).subscribe({
      next: () => {
        this.snack.open(success, $localize`:@@common.ok:OK`, { duration: 3000 });
        this.reloadSubscriptions();
      },
      error: err => this.fail(err,
        $localize`:@@common.error-update:Erreur lors de la mise à jour.`)
    });
  }

  private reloadSubscriptions(): void {
    this.subsQuery$.next({ ...this.subsQuery$.value });
  }

  private reloadDeliveries(): void {
    this.deliveryQuery$.next({ ...this.deliveryQuery });
  }

  private fail(err: unknown, fallback: string): void {
    this.snack.open(safeErrorMessage(err, fallback), $localize`:@@common.ok:OK`, { duration: 5000 });
  }

  private toSubscriptionsView(page: WebhookPage<WebhookSubscription>): SubscriptionsView {
    const rows = page.content ?? [];
    this.loadedSubscriptions = rows;
    const total = page.totalElements ?? rows.length;
    return {
      rows,
      total,
      // Les compteurs ne sont calculables que sur ce qui est chargé : on ne les affiche
      // que si la page couvre l'intégralité du jeu, sinon ils mentiraient.
      complete: rows.length === total,
      activeCount: rows.filter(s => s.status === 'ACTIVE').length,
      pausedCount: rows.filter(s => s.status === 'PAUSED').length,
      disabledCount: rows.filter(s => s.status === 'DISABLED_ON_ERRORS').length,
      failingCount: rows.filter(s => s.consecutiveFailures > 0).length
    };
  }
}
