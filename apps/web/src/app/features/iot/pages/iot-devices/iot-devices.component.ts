import { Component, OnInit } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { PageEvent } from '@angular/material/paginator';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Router } from '@angular/router';
import { BehaviorSubject, Observable } from 'rxjs';
import { catchError, finalize, map, shareReplay, switchMap, tap } from 'rxjs/operators';

import { safeErrorMessage } from '../../../../core/http/error-message';
import { deferredView } from '../../../../core/rx/deferred-view';
import {
  Tone, ageLabel, healthLabel, healthTone, protocolLabel, statusLabel, statusTone, typeLabel
} from '../../iot.labels';
import { IotService } from '../../iot.service';
import {
  DEVICE_TYPES, DeviceHealth, DeviceResponse, DeviceRow, FleetHealth, IotDeviceStatus,
  IotDeviceType, IotProtocol
} from '../../iot.types';
import { IotDeviceDialogComponent } from '../iot-device-dialog/iot-device-dialog.component';

/** Tuile de synthèse en tête d'écran. */
interface SummaryTile {
  label: string;
  value: number;
  tone: Tone;
}

/** Seuil au-delà duquel un équipement actif est déclaré muet. */
interface SilenceOption {
  minutes: number;
  label: string;
}

/**
 * Parc IoT et télémétrie — vue de flotte (§9).
 *
 * L'écran est organisé autour de la FRAÎCHEUR du signal et non de l'ordre
 * d'insertion : un capteur qui s'est tu est un capteur dont les dérives ne sont
 * plus détectées, donc une CAPA qui ne s'ouvrira jamais (§9.9). Les équipements
 * muets remontent en tête et portent un marqueur rouge ; protocole, type et
 * emplacement ne sont que du contexte.
 */
@Component({
  selector: 'qos-iot-devices',
  templateUrl: './iot-devices.component.html',
  styleUrls: ['./iot-devices.component.scss'],
  standalone: false
})
export class IotDevicesComponent implements OnInit {

  readonly displayedColumns =
    ['device', 'health', 'protocol', 'location', 'telemetry', 'status', 'actions'];
  readonly deviceTypes = DEVICE_TYPES;

  readonly silenceOptions: SilenceOption[] = [
    { minutes: 15, label: $localize`:@@iot.silence.15min:15 minutes` },
    { minutes: 60, label: $localize`:@@iot.silence.1h:1 heure` },
    { minutes: 360, label: $localize`:@@iot.silence.6h:6 heures` },
    { minutes: 1440, label: $localize`:@@iot.silence.24h:24 heures` }
  ];

  /** Chaîne vide = « tous » (le paramètre n'est alors pas envoyé au serveur). */
  status: IotDeviceStatus | '' = '';
  type: IotDeviceType | '' = '';
  silenceMinutes = 60;
  pageIndex = 0;
  pageSize = 25;

  rows$!: Observable<DeviceRow[]>;
  tiles$!: Observable<SummaryTile[]>;
  fleet$!: Observable<FleetHealth>;
  total$!: Observable<number>;

  private readonly loadingState$ = new BehaviorSubject<boolean>(false);
  readonly loading$ = deferredView(this.loadingState$);
  private readonly errorState$ = new BehaviorSubject<string | null>(null);
  readonly error$ = deferredView(this.errorState$);

  /** Équipement en cours de transition : évite le double-clic sur une ligne. */
  pendingDeviceId: string | null = null;

  private readonly reload$ = new BehaviorSubject<void>(undefined);

  constructor(
    private readonly svc: IotService,
    private readonly dialog: MatDialog,
    private readonly router: Router,
    private readonly snack: MatSnackBar
  ) {}

  ngOnInit(): void {
    const overview$ = this.reload$.pipe(
      tap(() => { this.errorState$.next(null); this.loadingState$.next(true); }),
      switchMap(() => this.svc
        .overview(this.status || null, this.type || null,
          this.pageIndex, this.pageSize, this.silenceMinutes * 60_000)
        .pipe(
          catchError(err => {
            this.errorState$.next(safeErrorMessage(err,
              $localize`:@@iot.devices.load-error:Impossible de charger le parc d'équipements.`));
            return [];
          }),
          finalize(() => this.loadingState$.next(false))
        )),
      // refCount:false : tuiles, tableau et paginateur s'abonnent séparément —
      // un abonnement tardif ne doit pas relancer les deux requêtes.
      shareReplay({ bufferSize: 1, refCount: false })
    );

    this.rows$ = overview$.pipe(map(o => o.rows));
    this.total$ = overview$.pipe(map(o => o.page.totalElements));
    this.fleet$ = overview$.pipe(map(o => o.fleet));
    this.tiles$ = overview$.pipe(map(o => this.toTiles(o.fleet, o.page.totalElements)));
  }

  // ---- Filtres et pagination -----------------------------------------------------

  /**
   * Statut et type s'excluent : le serveur n'applique qu'un seul critère et ignore
   * silencieusement le second. Plutôt que d'envoyer un filtre sans effet, on remet
   * l'autre à zéro — l'utilisateur voit ce qu'il obtient.
   */
  onStatusChange(value: IotDeviceStatus | ''): void {
    this.status = value;
    if (value) this.type = '';
    this.pageIndex = 0;
    this.reload$.next();
  }

  onTypeChange(value: IotDeviceType | ''): void {
    this.type = value;
    if (value) this.status = '';
    this.pageIndex = 0;
    this.reload$.next();
  }

  onSilenceChange(minutes: number): void {
    this.silenceMinutes = minutes;
    this.reload$.next();
  }

  onPage(event: PageEvent): void {
    this.pageIndex = event.pageIndex;
    this.pageSize = event.pageSize;
    this.reload$.next();
  }

  refresh(): void {
    this.reload$.next();
  }

  // ---- Actions ---------------------------------------------------------------------

  /**
   * Après création on ouvre la fiche : un équipement fraîchement provisionné doit
   * encore être activé et outillé de seuils, ce qui se pilote depuis sa fiche.
   */
  create(): void {
    const ref = this.dialog.open(IotDeviceDialogComponent, {
      data: { device: null },
      autoFocus: 'first-tabbable', restoreFocus: true, panelClass: 'qos-dialog-panel'
    });
    ref.afterClosed().subscribe((created?: DeviceResponse) => {
      if (!created) return;
      this.snack.open(
        $localize`:@@iot.devices.created:Équipement enregistré.`,
        $localize`:@@common.ok:OK`, { duration: 2500 });
      this.router.navigate(['/iot', created.id]);
    });
  }

  /** Mise en service depuis la liste : PROVISIONED ou SUSPENDED → ACTIVE. */
  activate(row: DeviceRow): void {
    this.run(row, this.svc.activateDevice(row.device.id));
  }

  /** Suspension depuis la liste : le serveur cessera d'accepter sa télémétrie. */
  suspend(row: DeviceRow): void {
    this.run(row, this.svc.suspendDevice(row.device.id));
  }

  /**
   * Recharge la vue complète après une transition : le compteur de capteurs muets
   * dépend de l'état de TOUS les équipements actifs, pas seulement de la ligne
   * touchée. Recopier la réponse dans la ligne laisserait les tuiles fausses.
   */
  private run(row: DeviceRow, action$: Observable<unknown>): void {
    if (this.pendingDeviceId) return;
    this.pendingDeviceId = row.device.id;
    this.errorState$.next(null);
    action$.pipe(finalize(() => { this.pendingDeviceId = null; })).subscribe({
      next: () => this.reload$.next(),
      error: err => this.errorState$.next(safeErrorMessage(err,
        $localize`:@@iot.devices.action-error:La transition d'état a été refusée.`))
    });
  }

  // ---- Règles d'affichage des actions ------------------------------------------------

  canActivate(row: DeviceRow): boolean {
    return row.device.status === 'PROVISIONED' || row.device.status === 'SUSPENDED';
  }

  canSuspend(row: DeviceRow): boolean {
    return row.device.status === 'ACTIVE';
  }

  // ---- Présentation ------------------------------------------------------------------

  trackById(_index: number, row: DeviceRow): string {
    return row.device.id;
  }

  healthLabel(health: DeviceHealth): string { return healthLabel(health); }
  healthTone(health: DeviceHealth): Tone { return healthTone(health); }
  statusLabel(status: IotDeviceStatus): string { return statusLabel(status); }
  statusTone(status: IotDeviceStatus): Tone { return statusTone(status); }
  typeLabel(type: IotDeviceType): string { return typeLabel(type); }
  protocolLabel(protocol: IotProtocol): string { return protocolLabel(protocol); }
  ageLabel(ageMs: number | null): string { return ageLabel(ageMs); }

  /**
   * Horodatage exact du dernier signal, en infobulle. La colonne n'affiche qu'une
   * durée arrondie : sur un incident, c'est la date précise qu'on recoupe avec les
   * journaux de la passerelle.
   */
  lastSeenTooltip(row: DeviceRow): string {
    if (!row.device.lastSeenAt) return '';
    const at = new Date(row.device.lastSeenAt);
    return Number.isNaN(at.getTime()) ? '' : at.toLocaleString();
  }

  private toTiles(fleet: FleetHealth, listed: number): SummaryTile[] {
    return [
      { label: $localize`:@@iot.devices.tile-listed:Équipements listés`, value: listed, tone: 'neutral' },
      { label: $localize`:@@iot.devices.tile-live:En ligne`, value: fleet.live, tone: 'success' },
      { label: $localize`:@@iot.devices.tile-aging:Signal vieillissant`, value: fleet.aging, tone: 'warn' },
      { label: $localize`:@@iot.devices.tile-silent:Capteurs muets`, value: fleet.silent, tone: 'danger' },
      { label: $localize`:@@iot.devices.tile-never:Jamais vus`, value: fleet.neverSeen, tone: 'danger' }
    ];
  }
}
