import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { environment } from '../../../environments/environment';
import { DashboardService } from './dashboard.service';
import { ExecutiveDashboardResponse } from './dashboard.types';

/**
 * Le dashboard exécutif était auparavant alimenté par des constantes codées en dur.
 * Ces tests verrouillent le comportement inverse : une seule requête réelle, projetée
 * sur les modèles de vue, et aucune donnée fabriquée quand l'API ne répond rien.
 */
describe('DashboardService', () => {
  let service: DashboardService;
  let http: HttpTestingController;

  const endpoint = `${environment.apiBaseUrl}/api/v1/dashboards/executive`;

  const payload: ExecutiveDashboardResponse = {
    kpis: [
      {
        kpiId: 'k1', code: 'FPY', name: 'First Pass Yield',
        description: 'Réussite en premier passage', category: 'quality', unit: '%',
        direction: 'HIGHER_IS_BETTER', value: 94.2, targetValue: 95, trendDelta: 2.1,
        health: 'WARNING',
        latestPeriodStart: '2026-05-01T00:00:00Z', latestPeriodEnd: '2026-05-31T00:00:00Z'
      },
      {
        kpiId: 'k2', code: 'COQ', name: 'Coût d’obtention de la qualité',
        description: null, category: 'cost', unit: '% CA',
        direction: 'LOWER_IS_BETTER', value: null, targetValue: null, trendDelta: null,
        health: 'UNKNOWN', latestPeriodStart: null, latestPeriodEnd: null
      }
    ],
    qualityTrend: [
      { periodStart: '2026-04-01T00:00:00Z', value: 92.1, targetValue: 95, health: 'WARNING' },
      { periodStart: '2026-05-01T00:00:00Z', value: 94.2, targetValue: 95, health: 'WARNING' }
    ],
    defectsByCategory: [{ category: 'PRODUCT', count: 42 }],
    topRisks: [
      {
        id: 'r1', title: 'Rupture de joint', source: 'FMEA', severity: 'CRITICAL',
        rpn: 240, dueDate: '2026-06-15T00:00:00Z'
      },
      {
        id: 'r2', title: 'CAPA en retard', source: 'CAPA', severity: 'HIGH',
        rpn: null, dueDate: null
      }
    ],
    alignment: [
      {
        adoptionId: 'a1', standardCode: 'iso-9001', standardName: 'ISO 9001:2015',
        score: 76.4, status: 'IN_PROGRESS',
        sections: [{ sectionCode: '4', score: 88.2 }, { sectionCode: '5', score: 61.7 }]
      }
    ],
    generatedAt: '2026-05-15T00:00:00Z'
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()]
    });
    service = TestBed.inject(DashboardService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  /** Sert la réponse à l'unique requête attendue. */
  function flush(body: ExecutiveDashboardResponse = payload): void {
    http.expectOne(endpoint).flush(body);
  }

  it('projette les cartes KPI du catalogue', (done) => {
    service.getExecutiveKpis().subscribe(cards => {
      expect(cards.length).toBe(2);
      expect(cards[0].id).toBe('k1');
      expect(cards[0].label).toBe('First Pass Yield');
      expect(cards[0].value).toBe(94.2);
      expect(cards[0].target).toBe(95);
      expect(cards[0].trend).toBe(2.1);
      expect(cards[0].state).toBe('warn');
      expect(cards[0].trendInvertedIsGood).toBeFalse();
      done();
    });
    flush();
  });

  it('affiche un KPI défini mais non mesuré sans inventer de valeur', (done) => {
    service.getExecutiveKpis().subscribe(cards => {
      const coq = cards[1];
      expect(coq.value).toBe('—');
      expect(coq.trend).toBeUndefined();
      expect(coq.target).toBeUndefined();
      expect(coq.state).toBe('neutral');
      // KPI où une baisse est une bonne nouvelle.
      expect(coq.trendInvertedIsGood).toBeTrue();
      done();
    });
    flush();
  });

  it('formate la tendance en périodes courtes triables', (done) => {
    service.getQualityTrend().subscribe(points => {
      expect(points.map(p => p.month)).toEqual(['2026-04', '2026-05']);
      expect(points[1].value).toBe(94.2);
      expect(points[1].target).toBe(95);
      done();
    });
    flush();
  });

  it('remonte les défauts par catégorie', (done) => {
    service.getDefectsByCategory().subscribe(rows => {
      expect(rows).toEqual([{ category: 'PRODUCT', count: 42 }]);
      done();
    });
    flush();
  });

  it('construit la heatmap de conformité à partir des sections de norme', (done) => {
    service.getComplianceHeatmap().subscribe(cells => {
      expect(cells.length).toBe(2);
      expect(cells[0]).toEqual({ norm: 'iso-9001', clause: '§4', score: 88 });
      expect(cells[1].clause).toBe('§5');
      done();
    });
    flush();
  });

  it('qualifie les risques et expose le RPN quand il existe', (done) => {
    service.getTopRisks().subscribe(risks => {
      expect(risks[0].severity).toBe('critical');
      expect(risks[0].source).toBe('FMEA · RPN 240');
      expect(risks[0].due).toBe('2026-06-15T00:00:00Z');
      expect(risks[1].severity).toBe('high');
      expect(risks[1].source).toBe('CAPA');
      expect(risks[1].due).toBeUndefined();
      done();
    });
    flush();
  });

  it('arrondit les scores d’alignement normatif', (done) => {
    service.getAlignmentBars().subscribe(bars => {
      expect(bars).toEqual([{
        standardCode: 'iso-9001', standardName: 'ISO 9001:2015',
        score: 76, status: 'IN_PROGRESS'
      }]);
      done();
    });
    flush();
  });

  it('ne fabrique aucune prédiction IA', (done) => {
    service.getAiPredictions().subscribe(predictions => {
      expect(predictions).toEqual([]);
      done();
    });
    // Aucune requête HTTP déclenchée : rien à flusher.
  });

  it('ne fabrique aucune sous-catégorie de défaut', () => {
    expect(service.getDefectSubcategoriesSync('PRODUCT')).toEqual([]);
  });

  it('partage une seule requête entre toutes les sections', (done) => {
    let received = 0;
    const tick = () => {
      if (++received === 3) {
        expect(received).toBe(3);
        done();
      }
    };
    service.getExecutiveKpis().subscribe(tick);
    service.getTopRisks().subscribe(tick);
    service.getAlignmentBars().subscribe(tick);
    // expectOne échouerait si chaque section déclenchait son propre appel.
    flush();
  });

  it('dégrade proprement quand l’API est indisponible', (done) => {
    service.getExecutiveKpis().subscribe(cards => {
      expect(cards).toEqual([]);
      done();
    });
    http.expectOne(endpoint).flush('boom', { status: 503, statusText: 'Service Unavailable' });
  });

  it('rejoue l’agrégation sur refresh()', (done) => {
    const emissions: number[] = [];
    service.getExecutiveKpis().subscribe(cards => {
      emissions.push(cards.length);
      if (emissions.length === 2) {
        // Première réponse servie, puis celle du refresh : le flux rejoue bien.
        expect(emissions).toEqual([2, 0]);
        done();
      }
    });

    flush();
    service.refresh();
    http.expectOne(endpoint).flush({ ...payload, kpis: [] });
  });
});
