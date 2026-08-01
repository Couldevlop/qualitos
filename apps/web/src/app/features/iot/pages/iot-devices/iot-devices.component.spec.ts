import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { Router, provideRouter } from '@angular/router';
import { Observable, of, throwError } from 'rxjs';

import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { IotService } from '../../iot.service';
import {
  DeviceResponse, DeviceRow, DevicesOverview, FleetHealth
} from '../../iot.types';
import { IotDevicesComponent } from './iot-devices.component';

describe('IotDevicesComponent', () => {
  let fixture: ComponentFixture<IotDevicesComponent>;
  let component: IotDevicesComponent;
  let svc: jasmine.SpyObj<IotService>;
  let dialogResult: DeviceResponse | undefined;
  let router: Router;

  const HOUR_MS = 3_600_000;

  const device = (id: string, over: Partial<DeviceResponse> = {}): DeviceResponse => ({
    id, tenantId: 't1', code: 'DEV-' + id, name: 'Capteur ' + id,
    deviceType: 'SENSOR_TEMPERATURE', protocol: 'MQTT', status: 'ACTIVE',
    location: 'Atelier A', description: null, metadataJson: null,
    lastSeenAt: '2026-07-31T10:00:00.000Z', telemetryCount: 12, createdBy: 'u1',
    createdAt: '2026-01-01T00:00:00Z', updatedAt: '2026-01-01T00:00:00Z', ...over
  });

  const fleet = (over: Partial<FleetHealth> = {}): FleetHealth => ({
    scanned: 4, total: 4, truncated: false, live: 1, aging: 0, silent: 1, neverSeen: 1, ...over
  });

  const overview = (rows: DeviceRow[], f: FleetHealth = fleet()): DevicesOverview => ({
    page: {
      content: rows.map(r => r.device), totalElements: rows.length,
      totalPages: 1, number: 0, size: 25
    },
    rows,
    fleet: f
  });

  const defaultRows: DeviceRow[] = [
    { device: device('d1', { code: 'C-01' }), health: 'SILENT', ageMs: 5 * HOUR_MS },
    { device: device('d2', { code: 'C-02', lastSeenAt: null }), health: 'NEVER_SEEN', ageMs: null },
    { device: device('d3', { code: 'C-03', status: 'SUSPENDED', location: null }),
      health: 'INACTIVE', ageMs: 9 * HOUR_MS },
    { device: device('d4', { code: 'C-04', status: 'PROVISIONED' }), health: 'INACTIVE', ageMs: null }
  ];

  function build(result: Observable<DevicesOverview> = of(overview(defaultRows))): void {
    svc.overview.and.returnValue(result);
    fixture = TestBed.createComponent(IotDevicesComponent);
    component = fixture.componentInstance;
    router = TestBed.inject(Router);
    fixture.detectChanges();
  }

  /**
   * `deferredView` livre les états de chargement/erreur via `asyncScheduler`
   * (macrotâche) : il faut laisser passer un tour de boucle avant de les lire.
   */
  async function settle(): Promise<void> {
    await new Promise<void>(resolve => setTimeout(resolve));
    fixture.detectChanges();
  }

  function buttonWith(label: string): HTMLButtonElement | null {
    const buttons = Array.from(
      (fixture.nativeElement as HTMLElement).querySelectorAll('button')) as HTMLButtonElement[];
    return buttons.find(b => (b.textContent ?? '').includes(label)) ?? null;
  }

  beforeEach(async () => {
    svc = jasmine.createSpyObj<IotService>('IotService',
      ['overview', 'activateDevice', 'suspendDevice']);
    svc.activateDevice.and.returnValue(of(device('d1')));
    svc.suspendDevice.and.returnValue(of(device('d1', { status: 'SUSPENDED' })));
    dialogResult = undefined;

    await TestBed.configureTestingModule({
      declarations: [IotDevicesComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        { provide: IotService, useValue: svc },
        { provide: MatDialog, useValue: { open: () => ({ afterClosed: () => of(dialogResult) }) } },
        { provide: MatSnackBar, useValue: jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']) },
        provideRouter([])
      ]
    }).compileComponents();
  });

  // ---- Rendu ------------------------------------------------------------------

  it('rend une ligne par équipement et les cinq tuiles de santé', () => {
    build();
    const el: HTMLElement = fixture.nativeElement;
    expect(el.querySelectorAll('tbody tr').length).toBe(4);
    expect(el.querySelectorAll('.tile').length).toBe(5);
  });

  it('signale visuellement les capteurs qui ne parlent plus', () => {
    build();
    const el: HTMLElement = fixture.nativeElement;
    // Muet ET jamais vu : les deux sont des capteurs dont on n'a plus de nouvelles.
    expect(el.querySelectorAll('tr.row--silent').length).toBe(2);
    expect(el.querySelectorAll('tr.row--inactive').length).toBe(2);
    expect(el.textContent).toContain('Muet');
    expect(el.textContent).toContain('Jamais vu');
  });

  it('affiche l’âge du dernier signal et « Jamais » sans signal', () => {
    build();
    const ages = Array.from(
      (fixture.nativeElement as HTMLElement).querySelectorAll('.health-cell__age'))
      .map(e => (e.textContent ?? '').trim());
    expect(ages[0]).toBe('5 h');
    expect(ages[1]).toBe('Jamais');
  });

  it('avertit quand les compteurs ne portent que sur un échantillon', async () => {
    build(of(overview(defaultRows, fleet({ truncated: true, scanned: 100, total: 420 }))));
    await settle();
    expect((fixture.nativeElement as HTMLElement).querySelector('.scope-note')?.textContent)
      .toContain('échantillon');
  });

  it('affiche l’état vide quand aucun équipement ne correspond au filtre', async () => {
    build(of(overview([])));
    await settle();
    const el: HTMLElement = fixture.nativeElement;
    expect(el.querySelector('.empty')).toBeTruthy();
    expect(el.querySelectorAll('tbody tr').length).toBe(0);
  });

  it('affiche un bandeau d’erreur si le chargement échoue', async () => {
    build(throwError(() => ({ status: 500 })));
    await settle();
    const banner = (fixture.nativeElement as HTMLElement).querySelector('.banner-error');
    expect(banner).toBeTruthy();
    expect(banner?.textContent).toContain('Erreur serveur');
  });

  // ---- Filtres et pagination ------------------------------------------------------

  it('recharge à la première page quand le statut change', () => {
    build();
    component.pageIndex = 3;
    component.onStatusChange('SUSPENDED');
    expect(component.pageIndex).toBe(0);
    expect(svc.overview).toHaveBeenCalledWith('SUSPENDED', null, 0, 25, HOUR_MS);
  });

  it('vide le type quand un statut est choisi : le serveur ignorerait le second critère', () => {
    build();
    component.onTypeChange('CAMERA');
    expect(svc.overview).toHaveBeenCalledWith(null, 'CAMERA', 0, 25, HOUR_MS);
    component.onStatusChange('ACTIVE');
    expect(component.type).toBe('');
    expect(svc.overview).toHaveBeenCalledWith('ACTIVE', null, 0, 25, HOUR_MS);
  });

  it('vide le statut quand un type est choisi', () => {
    build();
    component.onStatusChange('ACTIVE');
    component.onTypeChange('GATEWAY');
    expect(component.status).toBe('');
    expect(svc.overview).toHaveBeenCalledWith(null, 'GATEWAY', 0, 25, HOUR_MS);
  });

  it('recharge avec le nouveau seuil de silence', () => {
    build();
    component.onSilenceChange(1440);
    expect(component.silenceMinutes).toBe(1440);
    expect(svc.overview).toHaveBeenCalledWith(null, null, 0, 25, 1440 * 60_000);
  });

  it('recharge à la pagination', () => {
    build();
    component.onPage({ pageIndex: 2, pageSize: 50, length: 120 });
    expect(svc.overview).toHaveBeenCalledWith(null, null, 2, 50, HOUR_MS);
  });

  it('rafraîchit la vue à la demande', () => {
    build();
    const calls = svc.overview.calls.count();
    component.refresh();
    expect(svc.overview.calls.count()).toBe(calls + 1);
  });

  // ---- Actions --------------------------------------------------------------------

  it('n’expose que la transition valide dans l’état courant', () => {
    build();
    expect(component.canActivate(defaultRows[3])).toBeTrue();   // PROVISIONED
    expect(component.canSuspend(defaultRows[3])).toBeFalse();
    expect(component.canActivate(defaultRows[0])).toBeFalse();  // ACTIVE
    expect(component.canSuspend(defaultRows[0])).toBeTrue();
    expect(buttonWith('Mettre en service')).toBeTruthy();
    expect(buttonWith('Suspendre')).toBeTruthy();
  });

  it('met en service et recharge la vue complète', () => {
    build();
    const calls = svc.overview.calls.count();
    component.activate(defaultRows[3]);
    expect(svc.activateDevice).toHaveBeenCalledWith('d4');
    expect(svc.overview.calls.count()).toBe(calls + 1);
    expect(component.pendingDeviceId).toBeNull();
  });

  it('suspend un équipement en service', () => {
    build();
    component.suspend(defaultRows[0]);
    expect(svc.suspendDevice).toHaveBeenCalledWith('d1');
  });

  it('affiche un bandeau quand la transition est refusée', async () => {
    build();
    svc.activateDevice.and.returnValue(throwError(() => ({ status: 409 })));
    component.activate(defaultRows[3]);
    await settle();
    expect((fixture.nativeElement as HTMLElement).querySelector('.banner-error')?.textContent)
      .toContain('État incompatible');
  });

  it('ouvre la fiche du nouvel équipement après création', () => {
    build();
    dialogResult = device('d9');
    const nav = spyOn(router, 'navigate');
    component.create();
    expect(nav).toHaveBeenCalledWith(['/iot', 'd9']);
  });

  it('ne navigue pas si la création est annulée', () => {
    build();
    dialogResult = undefined;
    const nav = spyOn(router, 'navigate');
    component.create();
    expect(nav).not.toHaveBeenCalled();
  });

  // ---- Présentation ------------------------------------------------------------------

  it('mappe les états de santé sur un libellé et une tonalité', () => {
    build();
    expect(component.healthTone('LIVE')).toBe('success');
    expect(component.healthTone('AGING')).toBe('warn');
    expect(component.healthTone('SILENT')).toBe('danger');
    expect(component.healthTone('NEVER_SEEN')).toBe('danger');
    expect(component.healthTone('INACTIVE')).toBe('neutral');
    expect(component.healthLabel('SILENT')).toBe('Muet');
  });

  it('mappe les statuts d’équipement sur un libellé et une tonalité', () => {
    build();
    expect(component.statusLabel('PROVISIONED')).toBe('Provisionné');
    expect(component.statusLabel('DECOMMISSIONED')).toBe('Décommissionné');
    expect(component.statusTone('ACTIVE')).toBe('success');
    expect(component.statusTone('SUSPENDED')).toBe('warn');
    expect(component.statusTone('DECOMMISSIONED')).toBe('neutral');
  });

  it('abrège l’âge du signal par palier', () => {
    build();
    expect(component.ageLabel(null)).toBe('Jamais');
    expect(component.ageLabel(30_000)).toBe('À l\'instant');
    expect(component.ageLabel(12 * 60_000)).toBe('12 min');
    expect(component.ageLabel(3 * HOUR_MS)).toBe('3 h');
    expect(component.ageLabel(5 * 86_400_000)).toBe('5 j');
  });

  it('conserve la typographie des protocoles et traduit le relevé manuel', () => {
    build();
    expect(component.protocolLabel('OPC_UA')).toBe('OPC-UA');
    expect(component.protocolLabel('LORAWAN')).toBe('LoRaWAN');
    expect(component.protocolLabel('MANUAL')).toBe('Relevé manuel');
  });

  it('identifie les lignes par l’identifiant d’équipement', () => {
    build();
    expect(component.trackById(0, defaultRows[0])).toBe('d1');
    expect(component.typeLabel('GATEWAY')).toBe('Passerelle Edge');
  });
});
