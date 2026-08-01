import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { ActivatedRoute, Router, convertToParamMap, provideRouter } from '@angular/router';
import { Subscription, of, throwError } from 'rxjs';

import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { KpisService } from '../../kpis.service';
import {
  KpiCurrentStatus, KpiResponse, KpiTrend, MeasurementResponse
} from '../../kpis.types';
import { KpisDetailComponent } from './kpis-detail.component';

describe('KpisDetailComponent', () => {
  let fixture: ComponentFixture<KpisDetailComponent>;
  let component: KpisDetailComponent;
  let svc: jasmine.SpyObj<KpisService>;
  let snack: jasmine.SpyObj<MatSnackBar>;
  /** Valeur renvoyée par le prochain dialogue (confirmation ou formulaire). */
  let dialogResult: unknown;
  let routeId: string;
  /** Souscription manuelle à `kpi$` — recréée à chaque test. */
  let subs: Subscription;
  /** État du « serveur » : les suppressions doivent y être reflétées. */
  let storedMeasurements: MeasurementResponse[];

  const kpi = (over: Partial<KpiResponse> = {}): KpiResponse => ({
    id: 'kpi-1', tenantId: 't1', code: 'first-pass-yield', name: 'First Pass Yield',
    description: 'Bons du premier coup', category: 'Qualité', unit: '%',
    direction: 'HIGHER_IS_BETTER', frequency: 'WEEKLY',
    targetValue: 98, thresholdWarning: 95, thresholdCritical: 90,
    status: 'ACTIVE', applicableIndustriesCsv: 'Manufacturing',
    ownerUserId: 'u1', createdBy: 'u1',
    createdAt: '2026-01-01T00:00:00Z', updatedAt: '2026-01-01T00:00:00Z', ...over
  });

  const status = (over: Partial<KpiCurrentStatus> = {}): KpiCurrentStatus => ({
    kpiId: 'kpi-1', code: 'first-pass-yield', name: 'First Pass Yield',
    definitionStatus: 'ACTIVE', direction: 'HIGHER_IS_BETTER',
    latestValue: 96.2, unit: '%',
    latestPeriodStart: '2026-06-01T00:00:00Z', latestPeriodEnd: '2026-06-30T00:00:00Z',
    health: 'OK', targetValue: 98, thresholdWarning: 95, thresholdCritical: 90, ...over
  });

  const measurement = (over: Partial<MeasurementResponse> = {}): MeasurementResponse => ({
    id: 'mes-1', tenantId: 't1', kpiId: 'kpi-1',
    periodStart: '2026-06-01T00:00:00Z', periodEnd: '2026-06-30T00:00:00Z',
    value: 96.2, unit: '%', source: 'COMPUTED', health: 'OK',
    createdAt: '2026-07-01T00:00:00Z', ...over
  });

  const trend = (values: number[]): KpiTrend => ({
    kpiId: 'kpi-1', code: 'first-pass-yield', sampleCount: values.length,
    points: values.map((v, i) => ({
      periodStart: `2026-0${i + 1}-01T00:00:00Z`, periodEnd: `2026-0${i + 1}-28T00:00:00Z`,
      value: v, health: 'OK' as const
    }))
  });

  const page = <T>(content: T[]) => ({
    content, totalElements: content.length, totalPages: 1, number: 0, size: content.length
  });

  function build(): void {
    fixture = TestBed.createComponent(KpisDetailComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
    subs.add(component.kpi$.subscribe());
  }

  /** `deferredView` livre ses états via `asyncScheduler` : un tour de boucle suffit. */
  async function settle(): Promise<void> {
    await new Promise<void>(resolve => setTimeout(resolve));
    fixture.detectChanges();
  }

  function text(): string { return (fixture.nativeElement as HTMLElement).textContent ?? ''; }

  beforeEach(async () => {
    svc = jasmine.createSpyObj<KpisService>('KpisService', [
      'get', 'currentStatus', 'trend', 'listMeasurements',
      'activate', 'reopen', 'archive', 'delete', 'deleteMeasurement'
    ]);
    svc.get.and.returnValue(of(kpi()));
    svc.currentStatus.and.returnValue(of(status()));
    svc.trend.and.returnValue(of(trend([94, 95, 96.2])));
    // Le composant recharge la liste après suppression (la tendance est recalculée
    // côté serveur) : le double doit donc se comporter comme un vrai serveur et
    // ne plus renvoyer la mesure supprimée, sinon il la ressuscite.
    storedMeasurements = [measurement()];
    svc.listMeasurements.and.callFake(() => of(page(storedMeasurements)));
    svc.activate.and.returnValue(of(kpi({ status: 'ACTIVE' })));
    svc.reopen.and.returnValue(of(kpi({ status: 'DRAFT' })));
    svc.archive.and.returnValue(of(kpi({ status: 'ARCHIVED' })));
    svc.delete.and.returnValue(of(void 0));
    svc.deleteMeasurement.and.callFake((_kpiId: string, mesId: string) => {
      storedMeasurements = storedMeasurements.filter(m => m.id !== mesId);
      return of(void 0);
    });
    snack = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);
    dialogResult = true;
    routeId = 'kpi-1';
    subs = new Subscription();

    await TestBed.configureTestingModule({
      declarations: [KpisDetailComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        { provide: KpisService, useValue: svc },
        { provide: MatDialog, useValue: { open: () => ({ afterClosed: () => of(dialogResult) }) } },
        { provide: MatSnackBar, useValue: snack },
        provideRouter([]),
        // Après `provideRouter` : celui-ci fournit aussi un ActivatedRoute racine
        // qui écraserait le double si l'ordre était inversé.
        { provide: ActivatedRoute, useValue: { paramMap: of(convertToParamMap({ get id() { return routeId; } })) } }
      ]
    }).compileComponents();
  });

  afterEach(() => subs.unsubscribe());

  // ---- Garde sur l'identifiant (OWASP A03) ----------------------------------

  it('refuse un identifiant qui n’est ni un UUID ni un identifiant de démo', async () => {
    routeId = 'kpi\'; DROP TABLE kpis;--';
    build();
    await settle();
    expect(svc.get).not.toHaveBeenCalled();
    expect(text()).toContain('Identifiant invalide');
  });

  it('accepte un UUID renvoyé par le backend', () => {
    routeId = '3f2504e0-4f89-11d3-9a0c-0305e82c3301';
    build();
    expect(svc.get).toHaveBeenCalledWith('3f2504e0-4f89-11d3-9a0c-0305e82c3301');
  });

  // ---- Chargement ------------------------------------------------------------

  it('charge la définition, l’état courant, la tendance et les mesures', () => {
    build();
    expect(svc.get).toHaveBeenCalledWith('kpi-1');
    expect(svc.currentStatus).toHaveBeenCalledWith('kpi-1');
    expect(svc.trend).toHaveBeenCalledWith('kpi-1');
    expect(svc.listMeasurements).toHaveBeenCalledWith('kpi-1');
    expect(component.measurements.length).toBe(1);
    expect(text()).toContain('First Pass Yield');
    expect(text()).toContain('96.2');
  });

  it('affiche l’état vide quand aucune mesure n’a été enregistrée', () => {
    svc.listMeasurements.and.returnValue(of(page([])));
    svc.trend.and.returnValue(of(trend([])));
    svc.currentStatus.and.returnValue(of(status({
      latestValue: undefined, latestPeriodEnd: undefined, health: 'UNKNOWN'
    })));
    build();
    expect(text()).toContain('Aucune mesure pour l\'instant');
    expect(text()).toContain('Données insuffisantes');
  });

  it('affiche un bandeau d’erreur quand le KPI est introuvable', async () => {
    svc.get.and.returnValue(throwError(() => ({ status: 404 })));
    build();
    await settle();
    expect((fixture.nativeElement as HTMLElement).querySelector('.banner-error')).toBeTruthy();
  });

  it('reste affichable si l’état courant et la tendance sont indisponibles', () => {
    svc.currentStatus.and.returnValue(throwError(() => ({ status: 500 })));
    svc.trend.and.returnValue(throwError(() => ({ status: 500 })));
    svc.listMeasurements.and.returnValue(throwError(() => ({ status: 500 })));
    build();
    expect(component.status).toBeNull();
    expect(component.trend).toBeNull();
    expect(component.measurements).toEqual([]);
    expect(text()).toContain('First Pass Yield');
  });

  // ---- Transitions de statut -------------------------------------------------

  it('active un KPI en brouillon puis recharge la fiche', () => {
    svc.get.and.returnValue(of(kpi({ status: 'DRAFT' })));
    build();
    const before = svc.get.calls.count();
    component.activate(kpi({ status: 'DRAFT' }));
    expect(svc.activate).toHaveBeenCalledWith('kpi-1');
    expect(svc.get.calls.count()).toBe(before + 1);
  });

  it('signale une activation refusée sans recharger', () => {
    build();
    svc.activate.and.returnValue(throwError(() => ({ status: 409 })));
    const before = svc.get.calls.count();
    component.activate(kpi());
    expect(snack.open).toHaveBeenCalledWith('État incompatible — rechargez la page.', 'OK', { duration: 4000 });
    expect(svc.get.calls.count()).toBe(before);
  });

  it('rouvre un KPI actif', () => {
    build();
    component.reopen(kpi());
    expect(svc.reopen).toHaveBeenCalledWith('kpi-1');
  });

  it('signale une réouverture refusée', () => {
    build();
    svc.reopen.and.returnValue(throwError(() => ({ status: 403 })));
    component.reopen(kpi());
    expect(snack.open)
      .toHaveBeenCalledWith('Vous n\'avez pas les droits pour cette action.', 'OK', { duration: 4000 });
  });

  it('archive seulement après confirmation', () => {
    build();
    dialogResult = false;
    component.archive(kpi());
    expect(svc.archive).not.toHaveBeenCalled();

    dialogResult = true;
    component.archive(kpi());
    expect(svc.archive).toHaveBeenCalledWith('kpi-1');
  });

  it('signale un archivage refusé', () => {
    build();
    svc.archive.and.returnValue(throwError(() => ({ status: 500 })));
    component.archive(kpi());
    expect(snack.open)
      .toHaveBeenCalledWith('Erreur serveur — réessayez dans un instant.', 'OK', { duration: 4000 });
  });

  // ---- Suppressions -----------------------------------------------------------

  it('supprime le KPI après confirmation puis revient au catalogue', () => {
    build();
    const nav = spyOn(TestBed.inject(Router), 'navigate');
    component.remove(kpi());
    expect(svc.delete).toHaveBeenCalledWith('kpi-1');
    expect(nav).toHaveBeenCalledWith(['/kpis']);
  });

  it('ne supprime rien si la confirmation est refusée', () => {
    build();
    dialogResult = false;
    component.remove(kpi());
    expect(svc.delete).not.toHaveBeenCalled();
  });

  it('reste sur la fiche quand la suppression échoue', () => {
    build();
    svc.delete.and.returnValue(throwError(() => ({ status: 409 })));
    const nav = spyOn(TestBed.inject(Router), 'navigate');
    component.remove(kpi());
    expect(nav).not.toHaveBeenCalled();
    expect(snack.open).toHaveBeenCalled();
  });

  it('retire la mesure de la tendance après confirmation', () => {
    build();
    component.removeMeasurement(kpi(), measurement());
    expect(svc.deleteMeasurement).toHaveBeenCalledWith('kpi-1', 'mes-1');
    expect(component.measurements.some(m => m.id === 'mes-1')).toBeFalse();
  });

  it('conserve la mesure si la confirmation est refusée ou si le serveur refuse', () => {
    build();
    dialogResult = false;
    component.removeMeasurement(kpi(), measurement());
    expect(svc.deleteMeasurement).not.toHaveBeenCalled();
    expect(component.measurements.length).toBe(1);

    dialogResult = true;
    svc.deleteMeasurement.and.returnValue(throwError(() => ({ status: 409 })));
    component.removeMeasurement(kpi(), measurement());
    expect(component.measurements.length).toBe(1);
    expect(snack.open).toHaveBeenCalled();
  });

  // ---- Dialogues ---------------------------------------------------------------

  it('recharge après une édition ou une mesure confirmées, pas après une annulation', () => {
    build();
    const before = svc.get.calls.count();
    component.openEdit(kpi());
    component.openRecord(kpi());
    expect(svc.get.calls.count()).toBe(before + 2);

    dialogResult = undefined;
    component.openEdit(kpi());
    expect(svc.get.calls.count()).toBe(before + 2);
  });

  // ---- Sparkline ----------------------------------------------------------------

  it('ne trace rien tant qu’il n’y a pas au moins deux points', () => {
    svc.trend.and.returnValue(of(trend([42])));
    build();
    expect(component.sparklinePath()).toBe('');
    expect(component.sparklineFill()).toBe('');
  });

  it('trace la courbe entre 0 et 100 en largeur et inverse l’axe vertical du SVG', () => {
    svc.trend.and.returnValue(of(trend([90, 100])));
    build();
    // Le minimum est en bas (y=30), le maximum en haut (y=0).
    expect(component.sparklinePath()).toBe('M0.00,30.00 L100.00,0.00');
    expect(component.sparklineFill()).toBe('M0.00,30.00 L100.00,0.00 L100,30 L0,30 Z');
  });

  it('reste traçable quand toutes les valeurs sont identiques (aucune division par zéro)', () => {
    svc.trend.and.returnValue(of(trend([50, 50, 50])));
    build();
    expect(component.sparklinePath()).toBe('M0.00,30.00 L50.00,30.00 L100.00,30.00');
    expect(component.sparklinePath()).not.toContain('NaN');
  });

  // ---- Présentation ---------------------------------------------------------------

  it('distingue les deux sens de lecture du KPI', () => {
    build();
    expect(component.directionLabel('HIGHER_IS_BETTER'))
      .not.toBe(component.directionLabel('LOWER_IS_BETTER'));
  });

  it('mappe statut et santé sur des classes de badge', () => {
    build();
    expect(component.statusBadge('ARCHIVED')).toBe('badge badge-archived');
    expect(component.healthBadge('CRITICAL')).toBe('health health-critical');
    expect(component.healthBadge('UNKNOWN')).toBe('health health-unknown');
  });
});
