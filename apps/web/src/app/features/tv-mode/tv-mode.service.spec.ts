import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { environment } from '../../../environments/environment';
import { DashboardService } from '../dashboard/dashboard.service';
import {
  AiPrediction, KpiCard, QualityTrendPoint, TopRisk
} from '../dashboard/dashboard.types';
import { TvModeService, TvSlideLabels } from './tv-mode.service';

/** Libellés factices — le service est volontairement agnostique de l'i18n. */
const LABELS: TvSlideLabels = {
  kpisTitle: 'KPI', kpisSubtitle: 'sub',
  risksTitle: 'Risks', risksSubtitle: 'sub',
  predictionsTitle: 'AI', predictionsSubtitle: 'sub',
  trendTitle: 'Trend', trendSubtitle: 'sub',
  emptyTitle: 'Empty', emptySubtitle: 'sub',
  trendSeriesName: 'Quality', trendTargetName: 'Target'
};

const KPI: KpiCard = {
  id: 'k', label: 'l', value: 1, unit: '%', description: 'd', icon: 'i', state: 'good'
};
const RISK: TopRisk = { id: 'r', title: 't', source: 's', sourceType: 'FMEA', severity: 'high' };
const PRED: AiPrediction = {
  id: 'p', kind: 'drift', title: 't', detail: 'd', confidence: 0.8, horizon: '7j', state: 'bad'
};
const TREND: QualityTrendPoint = { month: 'M', value: 90, target: 95 };

describe('TvModeService', () => {
  let service: TvModeService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        // DashboardService interroge désormais l'agrégat exécutif du serveur.
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
        TvModeService, DashboardService
      ]
    });
    service = TestBed.inject(TvModeService);
  });

  it('assemble une slide par source non vide, dans l’ordre attendu', () => {
    const slides = service.assemble(LABELS, [KPI], [RISK], [PRED], [TREND]);
    expect(slides.map(s => s.kind)).toEqual(['kpis', 'trend', 'risks', 'predictions']);
  });

  it('omet les slides dont la source est vide', () => {
    const slides = service.assemble(LABELS, [KPI], [], [], []);
    expect(slides.length).toBe(1);
    expect(slides[0].kind).toBe('kpis');
  });

  it('produit une slide de repli quand toutes les sources sont vides', () => {
    const slides = service.assemble(LABELS, [], [], [], []);
    expect(slides.length).toBe(1);
    expect(slides[0].kind).toBe('empty');
    expect(slides[0].title).toBe('Empty');
  });

  it('plafonne les KPIs à 8 pour rester lisible à distance', () => {
    const many = Array.from({ length: 20 }, (_v, i) => ({ ...KPI, id: 'k' + i }));
    const slides = service.assemble(LABELS, many, [], [], []);
    const kpiSlide = slides.find(s => s.kind === 'kpis');
    expect(kpiSlide && kpiSlide.kind === 'kpis' && kpiSlide.kpis.length).toBe(8);
  });

  it('buildSlides agrège les données réelles du DashboardService', (done) => {
    service.buildSlides(LABELS).subscribe(slides => {
      expect(slides.length).toBeGreaterThan(0);
      expect(slides.some(s => s.kind === 'kpis')).toBeTrue();
      expect(slides.some(s => s.kind === 'empty')).toBeFalse();
      done();
    });

    // Le Mode TV projette ce que sert l'agrégat exécutif : plus aucune donnée de
    // démonstration ne circule ici (c'était le cas tant que DashboardService
    // renvoyait des constantes).
    TestBed.inject(HttpTestingController)
      .expectOne(`${environment.apiBaseUrl}/api/v1/dashboards/executive`)
      .flush({
        kpis: [{
          kpiId: 'k1', code: 'FPY', name: 'First Pass Yield', description: null,
          category: 'quality', unit: '%', direction: 'HIGHER_IS_BETTER',
          value: 94.2, targetValue: 95, trendDelta: 2.1, health: 'OK',
          latestPeriodStart: null, latestPeriodEnd: null
        }],
        qualityTrend: [], defectsByCategory: [], topRisks: [], alignment: [],
        generatedAt: '2026-05-15T00:00:00Z'
      });
  });

  it('affiche une slide « aucune donnée » pour un tenant sans indicateur', (done) => {
    service.buildSlides(LABELS).subscribe(slides => {
      // Un mur d'écran vide serait pire qu'un message : on dégrade explicitement.
      expect(slides.length).toBe(1);
      expect(slides[0].kind).toBe('empty');
      done();
    });

    TestBed.inject(HttpTestingController)
      .expectOne(`${environment.apiBaseUrl}/api/v1/dashboards/executive`)
      .flush({
        kpis: [], qualityTrend: [], defectsByCategory: [], topRisks: [],
        alignment: [], generatedAt: '2026-05-15T00:00:00Z'
      });
  });
});
