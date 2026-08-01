import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { ActivatedRoute, Router, convertToParamMap, provideRouter } from '@angular/router';
import { Observable, of, throwError } from 'rxjs';

import { AuthService } from '../../../../core/auth/auth.service';
import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { IotService } from '../../iot.service';
import {
  DeviceDetail, DeviceResponse, TelemetryPage, TelemetryResponse, ThresholdResponse
} from '../../iot.types';
import { IotDeviceDetailComponent } from './iot-device-detail.component';

describe('IotDeviceDetailComponent', () => {
  let fixture: ComponentFixture<IotDeviceDetailComponent>;
  let component: IotDeviceDetailComponent;
  let svc: jasmine.SpyObj<IotService>;
  let router: Router;
  /** Valeur renvoyée par le prochain dialogue (confirmation ou formulaire). */
  let dialogResult: unknown;
  let roles: string[];

  const device = (over: Partial<DeviceResponse> = {}): DeviceResponse => ({
    id: 'd1', tenantId: 't1', code: 'FRIGO-01', name: 'Réfrigérateur pharmacie',
    deviceType: 'SENSOR_TEMPERATURE', protocol: 'MQTT', status: 'ACTIVE',
    location: 'Pharmacie — réserve froide', description: 'Chaîne du froid vaccins',
    metadataJson: null, lastSeenAt: '2026-07-31T10:00:00.000Z', telemetryCount: 340,
    createdBy: 'u1', createdAt: '2026-01-01T00:00:00Z', updatedAt: '2026-01-01T00:00:00Z',
    ...over
  });

  const event = (id: string, over: Partial<TelemetryResponse> = {}): TelemetryResponse => ({
    id, tenantId: 't1', deviceId: 'd1', metric: 'temperature', valueNumeric: 4.2,
    valueText: null, unit: '°C', source: 'MQTT',
    recordedAt: '2026-07-31T10:00:00.000Z', ingestedAt: '2026-07-31T10:00:01.000Z', ...over
  });

  const threshold = (id: string, over: Partial<ThresholdResponse> = {}): ThresholdResponse => ({
    id, tenantId: 't1', deviceId: 'd1', metric: 'temperature', minValue: 2, maxValue: 8,
    capaCriticity: 'HIGH', capaOwnerId: 'u1', enabled: true, fmeaItemId: null,
    openPdcaCycle: true, createdAt: '2026-01-01T00:00:00Z', ...over
  });

  const detail = (over: Partial<DeviceDetail> = {}): DeviceDetail => ({
    device: device(),
    health: 'LIVE',
    ageMs: 120_000,
    telemetry: [
      event('e1'),
      event('e2', { metric: 'humidity', valueNumeric: 55, unit: '%' }),
      event('e3', { valueNumeric: null, valueText: 'PORTE_OUVERTE', unit: null })
    ],
    telemetryTotal: 340,
    metrics: ['humidity', 'temperature'],
    thresholds: [threshold('t1'), threshold('t2', { deviceId: null, enabled: false })],
    ...over
  });

  const telemetryPage = (content: TelemetryResponse[], total = content.length): TelemetryPage =>
    ({ content, totalElements: total, totalPages: 1, number: 0, size: 100 });

  function build(result: Observable<DeviceDetail | null> = of(detail())): void {
    svc.detail.and.returnValue(result as Observable<DeviceDetail>);
    fixture = TestBed.createComponent(IotDeviceDetailComponent);
    component = fixture.componentInstance;
    router = TestBed.inject(Router);
    fixture.detectChanges();
  }

  async function settle(): Promise<void> {
    await new Promise<void>(resolve => setTimeout(resolve));
    fixture.detectChanges();
  }

  function textOf(selector: string): string {
    return ((fixture.nativeElement as HTMLElement).querySelector(selector)?.textContent ?? '').trim();
  }

  function buttonWith(label: string): HTMLButtonElement | null {
    const buttons = Array.from(
      (fixture.nativeElement as HTMLElement).querySelectorAll('button')) as HTMLButtonElement[];
    return buttons.find(b => (b.textContent ?? '').includes(label)) ?? null;
  }

  beforeEach(async () => {
    svc = jasmine.createSpyObj<IotService>('IotService', [
      'detail', 'telemetryRange', 'activateDevice', 'suspendDevice',
      'decommissionDevice', 'deleteDevice', 'deleteThreshold'
    ]);
    svc.telemetryRange.and.returnValue(of(telemetryPage([event('e1')], 900)));
    svc.activateDevice.and.returnValue(of(device({ status: 'ACTIVE' })));
    svc.suspendDevice.and.returnValue(of(device({ status: 'SUSPENDED' })));
    svc.decommissionDevice.and.returnValue(of(device({ status: 'DECOMMISSIONED' })));
    svc.deleteDevice.and.returnValue(of(void 0));
    svc.deleteThreshold.and.returnValue(of(void 0));
    dialogResult = true;
    roles = ['quality_manager'];

    await TestBed.configureTestingModule({
      declarations: [IotDeviceDetailComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        { provide: IotService, useValue: svc },
        { provide: MatDialog, useValue: { open: () => ({ afterClosed: () => of(dialogResult) }) } },
        { provide: MatSnackBar, useValue: jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']) },
        {
          provide: AuthService,
          useValue: { snapshot: () => ({ userId: 'u1', tenantId: 't1', displayName: 'D', roles }) }
        },
        provideRouter([]),
        // Après `provideRouter` : celui-ci fournit aussi ActivatedRoute (racine, sans
        // paramètre) et écraserait le double si l'ordre était inversé.
        { provide: ActivatedRoute, useValue: { paramMap: of(convertToParamMap({ id: 'd1' })) } }
      ]
    }).compileComponents();
  });

  // ---- Rendu ---------------------------------------------------------------------

  it('charge la fiche de l’équipement de la route', () => {
    build();
    expect(svc.detail).toHaveBeenCalledWith('d1', 3_600_000);
    expect(component.deviceId).toBe('d1');
    expect(textOf('h1')).toBe('Réfrigérateur pharmacie');
  });

  it('met la santé du capteur en tête de fiche', () => {
    build();
    const el: HTMLElement = fixture.nativeElement;
    expect(el.querySelector('.head__meta')?.textContent).toContain('En ligne');
    expect(el.querySelectorAll('.tile').length).toBe(4);
    expect(el.querySelectorAll('.tile')[0].textContent).toContain('2 min');
  });

  it('annonce explicitement un capteur qui n’a jamais émis', () => {
    build(of(detail({
      device: device({ lastSeenAt: null, telemetryCount: 0 }),
      health: 'NEVER_SEEN', ageMs: null, telemetry: [], telemetryTotal: 0, metrics: []
    })));
    const el: HTMLElement = fixture.nativeElement;
    expect(el.textContent).toContain('Jamais vu');
    expect(el.textContent).toContain('Aucune mesure reçue à ce jour');
  });

  it('journalise les mesures, valeur textuelle comprise', () => {
    build();
    const rows = (fixture.nativeElement as HTMLElement).querySelectorAll('.panel tbody tr');
    // 3 mesures + 2 seuils
    expect(rows.length).toBe(5);
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('PORTE_OUVERTE');
  });

  it('distingue les seuils de l’équipement de ceux du tenant', () => {
    build();
    const el: HTMLElement = fixture.nativeElement;
    expect(el.textContent).toContain('Cet équipement');
    expect(el.textContent).toContain('Tous les équipements du tenant');
    expect(el.querySelectorAll('tr.row--off').length).toBe(1);
  });

  it('affiche l’état vide des seuils', () => {
    build(of(detail({ thresholds: [] })));
    expect((fixture.nativeElement as HTMLElement).textContent)
      .toContain('n\'ouvrira de CAPA automatiquement');
  });

  it('affiche l’état vide de la télémétrie', () => {
    build(of(detail({ telemetry: [], telemetryTotal: 0, metrics: [] })));
    expect((fixture.nativeElement as HTMLElement).textContent)
      .toContain('Aucune mesure reçue.');
  });

  it('affiche un bandeau d’erreur si la fiche est introuvable', async () => {
    build(throwError(() => ({ status: 404 })));
    await settle();
    expect((fixture.nativeElement as HTMLElement).querySelector('.banner-error')?.textContent)
      .toContain('Équipement introuvable');
  });

  // ---- Courbe ----------------------------------------------------------------------

  it('trace la première métrique disponible sans fenêtre serveur', (done) => {
    build();
    component.chart$.subscribe(c => {
      expect(c?.metric).toBe('humidity');
      expect(c?.window).toBe('RECENT');
      expect(c?.points.length).toBe(1);
      expect(c?.truncated).toBeTrue();   // 340 mesures au total, 3 rapatriées
      expect(svc.telemetryRange).not.toHaveBeenCalled();
      done();
    });
  });

  it('interroge le serveur quand une fenêtre temporelle est demandée', () => {
    build();
    component.onMetricChange('temperature');
    component.onWindowChange('H24');
    expect(svc.telemetryRange).toHaveBeenCalled();
    const [id, metric] = svc.telemetryRange.calls.mostRecent().args;
    expect(id).toBe('d1');
    expect(metric).toBe('temperature');
  });

  it('annonce une fenêtre tronquée plutôt que de laisser croire au silence', (done) => {
    build();
    component.onWindowChange('D7');
    component.chart$.subscribe(c => {
      expect(c?.truncated).toBeTrue();
      expect(c?.total).toBe(900);
      expect(c?.points.length).toBe(1);
      done();
    });
  });

  it('signale l’échec du chargement d’une fenêtre sans casser la fiche', async () => {
    build();
    svc.telemetryRange.and.returnValue(throwError(() => ({ status: 500 })));
    component.onWindowChange('D30');
    await settle();
    expect((fixture.nativeElement as HTMLElement).textContent)
      .toContain('Erreur serveur');
    expect(textOf('h1')).toBe('Réfrigérateur pharmacie');
  });

  it('retombe sur une métrique existante si celle demandée a disparu', (done) => {
    build();
    component.onMetricChange('pressure');
    component.chart$.subscribe(c => {
      expect(c?.metric).toBe('humidity');
      done();
    });
  });

  it('libelle les fenêtres d’analyse', () => {
    build();
    expect(component.windowLabel('RECENT')).toBe('Dernières mesures');
    expect(component.windowLabel('H24')).toBe('24 heures');
    expect(component.windowLabel('D7')).toBe('7 jours');
    expect(component.windowLabel('D30')).toBe('30 jours');
  });

  // ---- Actions contextuelles ----------------------------------------------------------

  it('n’offre que les actions valides sur un équipement en service', () => {
    build();
    expect(buttonWith('Relever une mesure')).toBeTruthy();
    expect(buttonWith('Suspendre')).toBeTruthy();
    expect(buttonWith('Décommissionner')).toBeTruthy();
    expect(buttonWith('Mettre en service')).toBeNull();
    // 340 mesures ingérées : le serveur refuserait la suppression.
    expect(buttonWith('Supprimer')).toBeNull();
  });

  it('propose la mise en service d’un équipement provisionné', () => {
    build(of(detail({
      device: device({ status: 'PROVISIONED', telemetryCount: 0, lastSeenAt: null }),
      health: 'INACTIVE', ageMs: null
    })));
    expect(buttonWith('Mettre en service')).toBeTruthy();
    expect(buttonWith('Relever une mesure')).toBeNull();
    // Sans historique, la suppression aboutit côté serveur.
    expect(buttonWith('Supprimer')).toBeTruthy();
  });

  it('fige la fiche d’un équipement décommissionné', () => {
    build(of(detail({
      device: device({ status: 'DECOMMISSIONED' }), health: 'INACTIVE', ageMs: 9_000_000
    })));
    const el: HTMLElement = fixture.nativeElement;
    expect(el.textContent).toContain('Équipement décommissionné');
    expect(buttonWith('Modifier')).toBeNull();
    expect(buttonWith('Décommissionner')).toBeNull();
    expect(buttonWith('Nouveau seuil')).toBeNull();
    // État terminal : la suppression redevient possible malgré l'historique.
    expect(buttonWith('Supprimer')).toBeTruthy();
  });

  it('masque les suppressions à un rôle qui n’y a pas droit', () => {
    roles = ['user'];
    build(of(detail({ device: device({ telemetryCount: 0 }) })));
    expect(buttonWith('Supprimer')).toBeNull();
  });

  it('exécute les transitions d’état après confirmation', () => {
    build();
    const d = detail();
    component.suspend(d);
    expect(svc.suspendDevice).toHaveBeenCalledWith('d1');
    component.decommission(d);
    expect(svc.decommissionDevice).toHaveBeenCalledWith('d1');
    component.activate(d);
    expect(svc.activateDevice).toHaveBeenCalledWith('d1');
    expect(component.pending).toBeFalse();
  });

  it('n’exécute rien si la confirmation est refusée', () => {
    dialogResult = false;
    build();
    component.decommission(detail());
    expect(svc.decommissionDevice).not.toHaveBeenCalled();
  });

  it('revient au parc après suppression de l’équipement', () => {
    build();
    const nav = spyOn(router, 'navigate');
    component.remove(detail({ device: device({ telemetryCount: 0 }) }));
    expect(svc.deleteDevice).toHaveBeenCalledWith('d1');
    expect(nav).toHaveBeenCalledWith(['/iot']);
  });

  it('supprime un seuil et recharge la fiche', () => {
    build();
    const calls = svc.detail.calls.count();
    component.removeThreshold(threshold('t1'));
    expect(svc.deleteThreshold).toHaveBeenCalledWith('t1');
    expect(svc.detail.calls.count()).toBe(calls + 1);
  });

  // ---- Règles d'affichage -----------------------------------------------------------------

  it('n’autorise la retouche que des seuils portés par l’équipement', () => {
    build();
    const d = detail();
    expect(component.canEditThreshold(d, threshold('t1'))).toBeTrue();
    expect(component.canEditThreshold(d, threshold('t2', { deviceId: null }))).toBeFalse();
    expect(component.canRemoveThreshold(d, threshold('t1'))).toBeTrue();
    expect(component.isDeviceScoped(threshold('t2', { deviceId: null }))).toBeFalse();
  });

  it('refuse la suppression d’un équipement porteur d’historique non décommissionné', () => {
    build();
    expect(component.canRemove(detail())).toBeFalse();
    expect(component.canRemove(detail({ device: device({ telemetryCount: 0 }) }))).toBeTrue();
    expect(component.canRemove(detail({ device: device({ status: 'DECOMMISSIONED' }) }))).toBeTrue();
  });

  // ---- Présentation --------------------------------------------------------------------------

  it('formule les bornes d’un seuil selon celles qui sont définies', () => {
    build();
    expect(component.boundsLabel(threshold('t1'))).toBe('2 … 8');
    expect(component.boundsLabel(threshold('t1', { maxValue: null }))).toBe('≥ 2');
    expect(component.boundsLabel(threshold('t1', { minValue: null }))).toBe('≤ 8');
  });

  it('affiche la valeur numérique, puis la valeur textuelle en secours', () => {
    build();
    expect(component.valueLabel(event('e1'))).toBe('4.2');
    expect(component.valueLabel(event('e2', { valueNumeric: null, valueText: 'OK' }))).toBe('OK');
    expect(component.valueLabel(event('e3', { valueNumeric: null, valueText: null }))).toBe('—');
  });

  it('mappe la criticité CAPA sur un libellé et une tonalité', () => {
    build();
    expect(component.criticityLabel('CRITICAL')).toBe('Critique');
    expect(component.criticityTone('CRITICAL')).toBe('danger');
    expect(component.criticityTone('HIGH')).toBe('danger');
    expect(component.criticityTone('MEDIUM')).toBe('warn');
    expect(component.criticityTone('LOW')).toBe('neutral');
  });

  it('identifie mesures et seuils par leur identifiant', () => {
    build();
    expect(component.trackTelemetry(0, event('e7'))).toBe('e7');
    expect(component.trackThreshold(0, threshold('t7'))).toBe('t7');
  });
});
