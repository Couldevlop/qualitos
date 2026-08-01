import { Component, OnInit } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ActivatedRoute, Router } from '@angular/router';
import type { EChartsCoreOption } from 'echarts/core';
import { BehaviorSubject, Observable, combineLatest, of } from 'rxjs';
import { catchError, finalize, shareReplay, switchMap, tap } from 'rxjs/operators';

import { AuthService } from '../../../../core/auth/auth.service';
import { safeErrorMessage } from '../../../../core/http/error-message';
import { deferredView } from '../../../../core/rx/deferred-view';
import { ConfirmDialogComponent } from '../../../../shared/ui/confirm-dialog/confirm-dialog.component';
import {
  Tone, ageLabel, criticityLabel, criticityTone, healthLabel, healthTone, protocolLabel,
  statusLabel, statusTone, typeLabel
} from '../../iot.labels';
import { IotService } from '../../iot.service';
import {
  CapaCriticity, DeviceDetail, DeviceHealth, IotDeviceStatus, IotDeviceType, IotProtocol,
  TelemetryResponse, TelemetryWindow, ThresholdResponse
} from '../../iot.types';
import { IotDeviceDialogComponent } from '../iot-device-dialog/iot-device-dialog.component';
import {
  IotTelemetryDialogComponent
} from '../iot-telemetry-dialog/iot-telemetry-dialog.component';
import {
  IotThresholdDialogComponent
} from '../iot-threshold-dialog/iot-threshold-dialog.component';

/** Rôles autorisés à supprimer une ressource qualité (SecurityConfig, règle DELETE). */
const DELETE_ROLES = ['quality_manager', 'admin_tenant', 'admin', 'super_admin'];

/**
 * Seuil de silence appliqué sur la fiche : une heure sans signal suffit à alerter
 * sur un capteur censé émettre en continu. La liste laisse l'utilisateur ajuster ce
 * seuil ; ici l'horodatage exact est affiché juste à côté, ce qui lève l'ambiguïté.
 */
const DETAIL_SILENCE_MS = 3_600_000;

/** Amplitude des fenêtres d'analyse, en millisecondes. */
const WINDOW_MS: Record<Exclude<TelemetryWindow, 'RECENT'>, number> = {
  H24: 86_400_000,
  D7: 604_800_000,
  D30: 2_592_000_000
};

/** Requête de courbe pilotée par les deux sélecteurs du panneau télémétrie. */
interface ChartQuery {
  metric: string | null;
  window: TelemetryWindow;
}

/** Courbe prête à afficher, options ECharts comprises. */
export interface ChartView {
  metric: string;
  metrics: string[];
  window: TelemetryWindow;
  /** Points de la métrique, dans l'ordre chronologique. */
  points: TelemetryResponse[];
  /** Volume total côté serveur pour ce périmètre (fenêtre ou équipement). */
  total: number;
  truncated: boolean;
  option: EChartsCoreOption;
}

/**
 * Parc IoT — fiche d'un équipement (§9.6, §9.7).
 *
 * Trois niveaux de lecture empilés : la santé du capteur (a-t-il parlé récemment ?),
 * sa télémétrie (que dit-il ?), ses seuils (que déclenche-t-il ?). Les actions
 * proposées sont exactement celles que le serveur accepte dans l'état courant — un
 * bouton affiché est un bouton qui aboutit :
 *  - pas de relevé sur un équipement qui n'est pas EN SERVICE (409 côté serveur) ;
 *  - une seule transition d'état proposée à la fois ;
 *  - pas de suppression d'un équipement porteur d'historique tant qu'il n'est pas
 *    décommissionné ;
 *  - rien de modifiable sur un équipement décommissionné (état terminal).
 */
@Component({
  selector: 'qos-iot-device-detail',
  templateUrl: './iot-device-detail.component.html',
  styleUrls: ['./iot-device-detail.component.scss'],
  standalone: false
})
export class IotDeviceDetailComponent implements OnInit {

  readonly telemetryColumns = ['recordedAt', 'metric', 'value', 'unit', 'source'];
  readonly thresholdColumns = ['metric', 'bounds', 'criticity', 'scope', 'state', 'actions'];

  readonly windows: TelemetryWindow[] = ['RECENT', 'H24', 'D7', 'D30'];

  detail$!: Observable<DeviceDetail | null>;
  chart$!: Observable<ChartView | null>;

  private readonly loadingState$ = new BehaviorSubject<boolean>(false);
  readonly loading$ = deferredView(this.loadingState$);
  private readonly errorState$ = new BehaviorSubject<string | null>(null);
  readonly error$ = deferredView(this.errorState$);

  private readonly chartLoadingState$ = new BehaviorSubject<boolean>(false);
  readonly chartLoading$ = deferredView(this.chartLoadingState$);
  private readonly chartErrorState$ = new BehaviorSubject<string | null>(null);
  readonly chartError$ = deferredView(this.chartErrorState$);

  /** Action en cours : évite le double-clic sur les transitions d'état. */
  pending = false;

  /** Les suppressions sont réservées au manager qualité ou plus (SecurityConfig). */
  readonly canDelete: boolean;

  deviceId = '';

  private readonly reload$ = new BehaviorSubject<void>(undefined);
  private readonly chartQuery$ =
    new BehaviorSubject<ChartQuery>({ metric: null, window: 'RECENT' });

  constructor(
    private readonly svc: IotService,
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly dialog: MatDialog,
    private readonly snack: MatSnackBar,
    private readonly auth: AuthService
  ) {
    const roles = this.auth.snapshot()?.roles ?? [];
    this.canDelete = roles.some(r => DELETE_ROLES.includes(r));
  }

  ngOnInit(): void {
    this.detail$ = combineLatest([this.route.paramMap, this.reload$]).pipe(
      tap(() => { this.errorState$.next(null); this.loadingState$.next(true); }),
      switchMap(([params]) => {
        this.deviceId = params.get('id') ?? '';
        return this.svc.detail(this.deviceId, DETAIL_SILENCE_MS).pipe(
          catchError(err => {
            this.errorState$.next(safeErrorMessage(err,
              $localize`:@@iot.detail.load-error:Équipement introuvable.`));
            return of(null);
          }),
          finalize(() => this.loadingState$.next(false))
        );
      }),
      // refCount:false : en-tête, panneau télémétrie, courbe et seuils s'abonnent
      // séparément — sans quoi chaque section relancerait les trois requêtes.
      shareReplay({ bufferSize: 1, refCount: false })
    );

    this.chart$ = combineLatest([this.detail$, this.chartQuery$]).pipe(
      tap(() => this.chartErrorState$.next(null)),
      switchMap(([d, q]) => this.buildChart(d, q)),
      shareReplay({ bufferSize: 1, refCount: false })
    );
  }

  // ---- Courbe de télémétrie ---------------------------------------------------------

  onMetricChange(metric: string): void {
    this.chartQuery$.next({ ...this.chartQuery$.getValue(), metric });
  }

  onWindowChange(window: TelemetryWindow): void {
    this.chartQuery$.next({ ...this.chartQuery$.getValue(), window });
  }

  windowLabel(window: TelemetryWindow): string {
    return ({
      RECENT: $localize`:@@iot.window.recent:Dernières mesures`,
      H24: $localize`:@@iot.window.h24:24 heures`,
      D7: $localize`:@@iot.window.d7:7 jours`,
      D30: $localize`:@@iot.window.d30:30 jours`
    })[window];
  }

  /**
   * La métrique demandée peut ne plus exister après un rechargement : on retombe
   * alors sur la première disponible plutôt que d'afficher une courbe vide sans
   * expliquer pourquoi.
   */
  private buildChart(d: DeviceDetail | null, q: ChartQuery): Observable<ChartView | null> {
    if (!d) return of(null);
    const metric = q.metric && d.metrics.includes(q.metric) ? q.metric : (d.metrics[0] ?? null);
    if (!metric) return of(null);

    if (q.window === 'RECENT') {
      // Les mesures rapatriées sont triées du plus récent au plus ancien : la courbe
      // se lit dans l'autre sens.
      const points = d.telemetry.filter(e => e.metric === metric).slice().reverse();
      return of(this.toChartView(d, metric, q.window, points,
        d.telemetryTotal, d.telemetryTotal > d.telemetry.length));
    }

    const to = new Date();
    const from = new Date(to.getTime() - WINDOW_MS[q.window]);
    this.chartLoadingState$.next(true);
    return this.svc
      .telemetryRange(d.device.id, metric, from.toISOString(), to.toISOString())
      .pipe(
        // Le serveur trie la fenêtre par ordre chronologique croissant : la page 0
        // en renvoie le DÉBUT, ce que la vue annonce quand le total la dépasse.
        switchMap(page => of(this.toChartView(d, metric, q.window, page.content,
          page.totalElements, page.totalElements > page.content.length))),
        catchError(err => {
          this.chartErrorState$.next(safeErrorMessage(err,
            $localize`:@@iot.detail.chart-error:Impossible de charger la fenêtre de mesures.`));
          return of(null);
        }),
        finalize(() => this.chartLoadingState$.next(false))
      );
  }

  private toChartView(d: DeviceDetail, metric: string, window: TelemetryWindow,
                      points: TelemetryResponse[], total: number,
                      truncated: boolean): ChartView {
    return {
      metric,
      metrics: d.metrics,
      window,
      points,
      total,
      truncated,
      option: this.toOption(metric, points, this.thresholdFor(d, metric))
    };
  }

  /** Seuil actif le plus spécifique pour la métrique : celui du capteur d'abord. */
  private thresholdFor(d: DeviceDetail, metric: string): ThresholdResponse | null {
    const applicable = d.thresholds.filter(t => t.enabled && t.metric === metric);
    return applicable.find(t => t.deviceId === d.device.id) ?? applicable[0] ?? null;
  }

  /**
   * Les mesures textuelles n'ont pas de place sur une courbe : elles restent dans
   * le journal en dessous. Les bornes du seuil sont tracées en repère — c'est ce
   * franchissement qui ouvrira une CAPA à la prochaine mesure.
   */
  private toOption(metric: string, points: TelemetryResponse[],
                   threshold: ThresholdResponse | null): EChartsCoreOption {
    const numeric = points.filter(p => p.valueNumeric !== null);
    const marks: { yAxis: number; name: string }[] = [];
    if (threshold) {
      if (threshold.minValue !== null) marks.push({ yAxis: threshold.minValue, name: 'min' });
      if (threshold.maxValue !== null) marks.push({ yAxis: threshold.maxValue, name: 'max' });
    }
    return {
      xAxis: { type: 'category', data: numeric.map(p => formatInstant(p.recordedAt)) },
      yAxis: { type: 'value', name: numeric[0]?.unit ?? '' },
      series: [{
        type: 'line',
        name: metric,
        data: numeric.map(p => p.valueNumeric),
        showSymbol: numeric.length <= 60,
        markLine: marks.length
          ? { silent: true, symbol: 'none', data: marks }
          : undefined
      }]
    } as EChartsCoreOption;
  }

  // ---- Cycle de vie de l'équipement ---------------------------------------------------

  edit(d: DeviceDetail): void {
    const ref = this.dialog.open(IotDeviceDialogComponent, {
      data: { device: d.device },
      autoFocus: 'first-tabbable', restoreFocus: true, panelClass: 'qos-dialog-panel'
    });
    ref.afterClosed().subscribe(updated => { if (updated) this.reload$.next(); });
  }

  activate(d: DeviceDetail): void {
    this.runStatus(this.svc.activateDevice(d.device.id));
  }

  suspend(d: DeviceDetail): void {
    this.confirmThen(
      $localize`:@@iot.detail.suspend-title:Suspendre l'équipement ?`,
      $localize`:@@iot.detail.suspend-message:Le serveur cessera d'accepter sa télémétrie : aucune dérive ne sera plus détectée tant qu'il n'est pas remis en service.`,
      false,
      () => this.runStatus(this.svc.suspendDevice(d.device.id)));
  }

  decommission(d: DeviceDetail): void {
    this.confirmThen(
      $localize`:@@iot.detail.decommission-title:Décommissionner l'équipement ?`,
      $localize`:@@iot.detail.decommission-message:L'état « décommissionné » est définitif : aucune remise en service, aucune nouvelle mesure. L'historique reste consultable.`,
      true,
      () => this.runStatus(this.svc.decommissionDevice(d.device.id)));
  }

  remove(d: DeviceDetail): void {
    this.confirmThen(
      $localize`:@@iot.detail.delete-title:Supprimer l'équipement ?`,
      $localize`:@@iot.detail.delete-message:Suppression définitive de l'équipement du registre. Les mesures déjà ingérées ne sont plus rattachées à aucun capteur.`,
      true,
      () => {
        if (this.pending) return;
        this.pending = true;
        this.svc.deleteDevice(d.device.id)
          .pipe(finalize(() => { this.pending = false; }))
          .subscribe({
            next: () => {
              this.snack.open($localize`:@@iot.detail.deleted:Équipement supprimé.`,
                $localize`:@@common.ok:OK`, { duration: 2500 });
              this.router.navigate(['/iot']);
            },
            error: err => this.fail(err,
              $localize`:@@iot.detail.delete-failed:Suppression impossible.`)
          });
      });
  }

  // ---- Télémétrie et seuils -------------------------------------------------------------

  addTelemetry(d: DeviceDetail): void {
    const ref = this.dialog.open(IotTelemetryDialogComponent, {
      data: {
        deviceId: d.device.id, deviceName: d.device.name, knownMetrics: d.metrics
      },
      autoFocus: 'first-tabbable', restoreFocus: true, panelClass: 'qos-dialog-panel'
    });
    ref.afterClosed().subscribe(saved => { if (saved) this.reload$.next(); });
  }

  addThreshold(d: DeviceDetail): void {
    this.openThresholdDialog(d, null);
  }

  editThreshold(d: DeviceDetail, threshold: ThresholdResponse): void {
    this.openThresholdDialog(d, threshold);
  }

  removeThreshold(threshold: ThresholdResponse): void {
    this.confirmThen(
      $localize`:@@iot.detail.threshold-delete-title:Supprimer ce seuil ?`,
      $localize`:@@iot.detail.threshold-delete-message:Les dépassements de cette métrique n'ouvriront plus de CAPA automatiquement.`,
      true,
      () => {
        if (this.pending) return;
        this.pending = true;
        this.svc.deleteThreshold(threshold.id)
          .pipe(finalize(() => { this.pending = false; }))
          .subscribe({
            next: () => {
              this.snack.open($localize`:@@iot.detail.threshold-deleted:Seuil supprimé.`,
                $localize`:@@common.ok:OK`, { duration: 2500 });
              this.reload$.next();
            },
            error: err => this.fail(err,
              $localize`:@@iot.detail.delete-failed:Suppression impossible.`)
          });
      });
  }

  private openThresholdDialog(d: DeviceDetail, threshold: ThresholdResponse | null): void {
    const ref = this.dialog.open(IotThresholdDialogComponent, {
      data: {
        deviceId: d.device.id, deviceName: d.device.name,
        threshold, knownMetrics: d.metrics
      },
      autoFocus: 'first-tabbable', restoreFocus: true, panelClass: 'qos-dialog-panel'
    });
    ref.afterClosed().subscribe(saved => { if (saved) this.reload$.next(); });
  }

  // ---- Règles d'affichage des actions -----------------------------------------------------

  /** État terminal côté serveur : plus aucune mutation n'aboutit. */
  isDecommissioned(d: DeviceDetail): boolean {
    return d.device.status === 'DECOMMISSIONED';
  }

  canActivate(d: DeviceDetail): boolean {
    return d.device.status === 'PROVISIONED' || d.device.status === 'SUSPENDED';
  }

  canSuspend(d: DeviceDetail): boolean {
    return d.device.status === 'ACTIVE';
  }

  /** Le serveur refuse (409) toute ingestion hors état ACTIVE. */
  canIngest(d: DeviceDetail): boolean {
    return d.device.status === 'ACTIVE';
  }

  /**
   * Le serveur refuse de supprimer un équipement porteur d'historique tant qu'il
   * n'est pas décommissionné. On n'affiche donc le bouton que dans les cas où il
   * aboutit réellement.
   */
  canRemove(d: DeviceDetail): boolean {
    return this.canDelete
      && (this.isDecommissioned(d) || d.device.telemetryCount === 0);
  }

  /** Un seuil global (sans équipement) ne se retouche pas depuis une fiche capteur. */
  isDeviceScoped(threshold: ThresholdResponse): boolean {
    return threshold.deviceId !== null;
  }

  canEditThreshold(d: DeviceDetail, threshold: ThresholdResponse): boolean {
    return this.isDeviceScoped(threshold) && !this.isDecommissioned(d);
  }

  canRemoveThreshold(d: DeviceDetail, threshold: ThresholdResponse): boolean {
    return this.canDelete && this.canEditThreshold(d, threshold);
  }

  // ---- Présentation -------------------------------------------------------------------------

  healthLabel(health: DeviceHealth): string { return healthLabel(health); }
  healthTone(health: DeviceHealth): Tone { return healthTone(health); }
  statusLabel(status: IotDeviceStatus): string { return statusLabel(status); }
  statusTone(status: IotDeviceStatus): Tone { return statusTone(status); }
  typeLabel(type: IotDeviceType): string { return typeLabel(type); }
  protocolLabel(protocol: IotProtocol): string { return protocolLabel(protocol); }
  criticityLabel(criticity: CapaCriticity): string { return criticityLabel(criticity); }
  criticityTone(criticity: CapaCriticity): Tone { return criticityTone(criticity); }
  ageLabel(ageMs: number | null): string { return ageLabel(ageMs); }

  /** Bornes lisibles d'un seuil : « ≥ 4 », « ≤ 8 » ou « 4 … 8 ». */
  boundsLabel(threshold: ThresholdResponse): string {
    if (threshold.minValue !== null && threshold.maxValue !== null) {
      return `${threshold.minValue} … ${threshold.maxValue}`;
    }
    if (threshold.minValue !== null) return `≥ ${threshold.minValue}`;
    return `≤ ${threshold.maxValue}`;
  }

  /** Valeur affichable d'une mesure : le numérique prime, le texte prend le relais. */
  valueLabel(event: TelemetryResponse): string {
    if (event.valueNumeric !== null) return String(event.valueNumeric);
    return event.valueText ?? '—';
  }

  trackTelemetry(_index: number, event: TelemetryResponse): string {
    return event.id;
  }

  trackThreshold(_index: number, threshold: ThresholdResponse): string {
    return threshold.id;
  }

  // ---- Interne ------------------------------------------------------------------------------

  private confirmThen(title: string, message: string, destructive: boolean,
                      action: () => void): void {
    const ref = this.dialog.open(ConfirmDialogComponent, {
      data: {
        title, message,
        confirmLabel: $localize`:@@common.confirm:Confirmer`,
        cancelLabel: $localize`:@@common.cancel:Annuler`,
        destructive
      }
    });
    ref.afterClosed().subscribe(ok => { if (ok) action(); });
  }

  private runStatus(action$: Observable<unknown>): void {
    if (this.pending) return;
    this.pending = true;
    action$.pipe(finalize(() => { this.pending = false; })).subscribe({
      next: () => {
        this.snack.open($localize`:@@iot.detail.status-changed:Statut mis à jour.`,
          $localize`:@@common.ok:OK`, { duration: 2500 });
        this.reload$.next();
      },
      error: err => this.fail(err,
        $localize`:@@iot.detail.status-failed:Changement de statut refusé.`)
    });
  }

  private fail(err: unknown, fallback: string): void {
    // eslint-disable-next-line no-console
    console.warn('[iot-device-detail] action failed', (err as { status?: number })?.status);
    this.snack.open(safeErrorMessage(err, fallback),
      $localize`:@@common.ok:OK`, { duration: 4000 });
  }
}

/** Horodatage court pour l'axe de la courbe, dans la locale du navigateur. */
export function formatInstant(iso: string): string {
  const d = new Date(iso);
  return Number.isNaN(d.getTime()) ? iso : d.toLocaleString();
}
