import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatDialog, MatDialogRef } from '@angular/material/dialog';
import { PageEvent } from '@angular/material/paginator';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { provideRouter, Router } from '@angular/router';
import { firstValueFrom, of } from 'rxjs';
import { filter, take } from 'rxjs/operators';

import { environment } from '../../../../../environments/environment';
import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { FiveSAuditResponse } from '../../fives.types';
import { FivesListComponent } from './fives-list.component';

function audit(over: Partial<FiveSAuditResponse> = {}): FiveSAuditResponse {
  return {
    id: '5s-1', tenantId: 't1', zone: 'Atelier A', status: 'COMPLETED', auditorId: 'u1',
    overallScore: 80, createdAt: '2026-06-01T00:00:00Z', updatedAt: '2026-06-15T00:00:00Z',
    items: [], ...over
  };
}

describe('FivesListComponent', () => {
  let component: FivesListComponent;
  let fixture: ComponentFixture<FivesListComponent>;
  let prevMock: boolean;

  beforeEach(async () => {
    prevMock = environment.useMockApi;
    environment.useMockApi = true;
    await TestBed.configureTestingModule({
      declarations: [FivesListComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
        provideRouter([])
      ]
    }).compileComponents();
    fixture = TestBed.createComponent(FivesListComponent);
    component = fixture.componentInstance;
  });

  afterEach(() => { environment.useMockApi = prevMock; });

  it('renders without throwing', () => {
    fixture.detectChanges();
    expect(component).toBeTruthy();
  });

  it('exposes the canonical 5S audit statuses and columns', () => {
    expect(component.statuses).toEqual(['DRAFT', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED']);
    expect(component.displayedColumns).toEqual(['zone', 'status', 'score', 'scheduledAt', 'updatedAt']);
  });

  it('builds the status badge class', () => {
    expect(component.badgeClass('IN_PROGRESS')).toBe('badge badge-in_progress');
  });

  it('maps the 5S score to high/mid/low buckets', () => {
    expect(component.scoreClass(undefined)).toBe('score');
    expect(component.scoreClass(80)).toBe('score score-high');
    expect(component.scoreClass(79)).toBe('score score-mid');
    expect(component.scoreClass(60)).toBe('score score-mid');
    expect(component.scoreClass(59)).toBe('score score-low');
  });

  it('clamps pagination on onPage', () => {
    component.onPage({ pageIndex: -1, pageSize: 777 } as PageEvent);
    expect(component.pageIndex).toBe(0);
    expect(component.pageSize).toBe(100);
  });

  it('navigates to the audit detail on openAudit', () => {
    const router = TestBed.inject(Router);
    const nav = spyOn(router, 'navigate');
    component.openAudit({ id: '5s-8' } as never);
    expect(nav).toHaveBeenCalledWith(['/fives', '5s-8']);
  });
});

/**
 * Heatmap zone × mois (§3.2) : c'est une fonction pure, donc entièrement
 * vérifiable — moyenne par cellule, tri, exclusion des audits non notés.
 */
describe('FivesListComponent — heatmap des scores par zone', () => {
  let component: FivesListComponent;
  let prevMock: boolean;

  beforeEach(async () => {
    prevMock = environment.useMockApi;
    environment.useMockApi = true;
    await TestBed.configureTestingModule({
      declarations: [FivesListComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
        provideRouter([])
      ]
    }).compileComponents();
    component = TestBed.createComponent(FivesListComponent).componentInstance;
  });

  afterEach(() => { environment.useMockApi = prevMock; });

  it('ne produit pas de heatmap sans aucun audit noté', () => {
    expect(component.buildHeatmapOption([])).toBeNull();
    expect(component.buildHeatmapOption([audit({ overallScore: undefined })])).toBeNull();
    expect(component.buildHeatmapOption([audit({ zone: '' })])).toBeNull();
  });

  it('moyenne les scores d\'une même zone sur un même mois', () => {
    const opt = component.buildHeatmapOption([
      audit({ id: 'a', zone: 'Atelier A', overallScore: 70, updatedAt: '2026-06-04T00:00:00Z' }),
      audit({ id: 'b', zone: 'Atelier A', overallScore: 81, updatedAt: '2026-06-28T00:00:00Z' })
    ]) as { series: { data: number[][] }[] };

    expect(opt.series[0].data.length).toBe(1);
    // (70 + 81) / 2 = 75,5 → arrondi à 76.
    expect(opt.series[0].data[0]).toEqual([0, 0, 76]);
  });

  it('trie les zones et les mois, et ne crée de cellule que là où il y a des données', () => {
    const opt = component.buildHeatmapOption([
      audit({ id: 'a', zone: 'Zone B', overallScore: 60, updatedAt: '2026-07-10T00:00:00Z' }),
      audit({ id: 'b', zone: 'Zone A', overallScore: 90, updatedAt: '2026-06-10T00:00:00Z' })
    ]) as { xAxis: { data: string[] }; yAxis: { data: string[] }; series: { data: number[][] }[] };

    expect(opt.yAxis.data).toEqual(['Zone A', 'Zone B']);
    expect(opt.xAxis.data).toEqual(['06/26', '07/26']);
    // 2 zones × 2 mois = 4 cellules possibles, mais seules 2 sont renseignées.
    expect(opt.series[0].data.length).toBe(2);
    expect(opt.series[0].data).toContain([0, 0, 90]);
    expect(opt.series[0].data).toContain([1, 1, 60]);
  });

  it('écarte les audits non notés du calcul sans écarter la zone entière', () => {
    const opt = component.buildHeatmapOption([
      audit({ id: 'a', zone: 'Atelier A', overallScore: 40, updatedAt: '2026-06-04T00:00:00Z' }),
      audit({ id: 'b', zone: 'Atelier A', overallScore: undefined, updatedAt: '2026-06-05T00:00:00Z' })
    ]) as { series: { data: number[][] }[] };

    expect(opt.series[0].data).toEqual([[0, 0, 40]]);
  });

  it('libelle l\'infobulle avec la zone, le mois et le score sur 100', () => {
    const opt = component.buildHeatmapOption([audit()]) as {
      tooltip: { formatter: (p: { value: number[] }) => string };
      series: { label: { formatter: (p: { value: number[] }) => string } }[];
    };

    const html = opt.tooltip.formatter({ value: [0, 0, 80] });
    expect(html).toContain('Atelier A');
    expect(html).toContain('06/26');
    expect(html).toContain('80/100');
    expect(opt.series[0].label.formatter({ value: [0, 0, 80] })).toBe('80');
  });
});

/**
 * Chargement contre l'API réelle : la table et la heatmap se chargent
 * indépendamment (la heatmap échantillonne 100 audits hors filtre).
 */
describe('FivesListComponent (chargement API)', () => {
  let component: FivesListComponent;
  let http: HttpTestingController;
  let prevMock: boolean;

  const base = `${environment.apiBaseUrl}/api/v1/fives/audits`;

  const page = (content: FiveSAuditResponse[], totalElements = content.length) =>
    ({ content, totalElements, totalPages: 1, number: 0, size: 20 });

  function stubDialog(result: unknown): jasmine.Spy {
    return spyOn(TestBed.inject(MatDialog), 'open').and.returnValue({
      afterClosed: () => of(result)
    } as MatDialogRef<unknown>);
  }

  /** Requêtes de la table (page/size pilotés par le paginateur). */
  const tableRequest = () => http.expectOne(r => r.url === base && r.params.get('size') !== '100');
  /** Requête d'échantillonnage de la heatmap (toujours 0/100). */
  const heatmapRequest = () => http.expectOne(r => r.url === base && r.params.get('size') === '100');

  beforeEach(async () => {
    prevMock = environment.useMockApi;
    environment.useMockApi = false;
    await TestBed.configureTestingModule({
      declarations: [FivesListComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
        provideRouter([])
      ]
    }).compileComponents();
    component = TestBed.createComponent(FivesListComponent).componentInstance;
    http = TestBed.inject(HttpTestingController);
    component.ngOnInit();
  });

  afterEach(() => {
    environment.useMockApi = prevMock;
    http.verify();
  });

  it('charge la première page sans filtre', (done) => {
    component.audits$.subscribe(list => {
      expect(list.length).toBe(1);
      expect(component.totalElements).toBe(9);
      done();
    });

    const req = tableRequest();
    expect(req.request.params.get('page')).toBe('0');
    expect(req.request.params.get('size')).toBe('20');
    expect(req.request.params.has('status')).toBeFalse();
    req.flush(page([audit()], 9));
  });

  it('relance la requête avec le statut choisi', () => {
    component.audits$.subscribe();
    tableRequest().flush(page([audit()]));

    component.statusFilter.setValue('DRAFT');
    const req = tableRequest();
    expect(req.request.params.get('status')).toBe('DRAFT');
    req.flush(page([]));
  });

  it('affiche un message générique quand le chargement échoue', async () => {
    component.audits$.subscribe();
    const errorPromise = firstValueFrom(component.error$.pipe(filter(Boolean), take(1)));

    tableRequest().flush({ detail: 'org.postgresql.util.PSQLException' },
                         { status: 500, statusText: 'Server Error' });

    const message = await errorPromise;
    expect(message).toContain('Erreur serveur');
    expect(message).not.toContain('PSQL');
    expect(await firstValueFrom(component.loading$)).toBeFalse();
  });

  it('échantillonne 100 audits pour la heatmap, indépendamment du filtre de la table', (done) => {
    component.heatmapOption$.subscribe(opt => {
      expect(opt).not.toBeNull();
      done();
    });

    const req = heatmapRequest();
    expect(req.request.params.get('page')).toBe('0');
    expect(req.request.params.has('status')).toBeFalse();
    req.flush(page([audit()]));
  });

  it('n\'affiche pas de heatmap quand son échantillon est en erreur (la table reste utilisable)', (done) => {
    component.heatmapOption$.subscribe(opt => {
      expect(opt).toBeNull();
      done();
    });
    heatmapRequest().flush({}, { status: 500, statusText: 'Server Error' });
  });

  it('revient en première page et rafraîchit table et heatmap après une création', () => {
    component.audits$.subscribe();
    component.heatmapOption$.subscribe();
    tableRequest().flush(page([]));
    heatmapRequest().flush(page([audit()]));

    component.onPage({ pageIndex: 2, pageSize: 20 } as PageEvent);
    tableRequest().flush(page([]));

    stubDialog(audit({ id: '5s-neuf' }));
    component.openCreate();
    expect(component.pageIndex).toBe(0);

    const pendingTable = http.match(r => r.url === base && r.params.get('size') !== '100')
      .filter(r => !r.cancelled);
    expect(pendingTable.length).toBe(1);
    expect(pendingTable[0].request.params.get('page')).toBe('0');
    pendingTable[0].flush(page([audit({ id: '5s-neuf' })]));

    // La heatmap se recharge aussi : un nouvel audit change la maille zone × mois.
    const pendingHeatmap = http.match(r => r.url === base && r.params.get('size') === '100')
      .filter(r => !r.cancelled);
    expect(pendingHeatmap.length).toBe(1);
    pendingHeatmap[0].flush(page([audit({ id: '5s-neuf' })]));
  });

  it('ne recharge rien quand la création est annulée', () => {
    component.audits$.subscribe();
    tableRequest().flush(page([]));

    stubDialog(undefined);
    component.openCreate();
    http.expectNone(r => r.url === base);
  });
});
