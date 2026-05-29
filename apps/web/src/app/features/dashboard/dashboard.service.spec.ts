import { TestBed } from '@angular/core/testing';

import { DashboardService } from './dashboard.service';

describe('DashboardService', () => {
  let service: DashboardService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(DashboardService);
  });

  it('exposes a capped set of executive KPI cards', (done) => {
    service.getExecutiveKpis().subscribe(cards => {
      expect(cards.length).toBeGreaterThan(0);
      // §6.1 anti-pattern: ne pas dépasser 8-12 KPIs au niveau exécutif.
      expect(cards.length).toBeLessThanOrEqual(12);
      expect(cards[0].id).toBeTruthy();
      expect(cards[0].label).toBeTruthy();
      done();
    });
  });

  it('returns a non-empty quality trend series', (done) => {
    service.getQualityTrend().subscribe(points => {
      expect(points.length).toBeGreaterThan(0);
      done();
    });
  });

  it('returns defects grouped by category', (done) => {
    service.getDefectsByCategory().subscribe(rows => {
      expect(rows.length).toBeGreaterThan(0);
      done();
    });
  });

  it('returns a compliance heatmap', (done) => {
    service.getComplianceHeatmap().subscribe(cells => {
      expect(cells.length).toBeGreaterThan(0);
      done();
    });
  });

  it('returns the top risks', (done) => {
    service.getTopRisks().subscribe(risks => {
      expect(risks.length).toBeGreaterThan(0);
      done();
    });
  });

  it('returns AI predictions', (done) => {
    service.getAiPredictions().subscribe(preds => {
      expect(preds.length).toBeGreaterThan(0);
      done();
    });
  });
});
