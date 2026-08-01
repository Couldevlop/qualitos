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

  // ---- Traductions serveur → présentation ------------------------------------

  /** Fabrique un KPI ne différant du modèle que par la santé et la catégorie. */
  function kpiWith(health: string, category: string | null) {
    return {
      ...payload.kpis[0], kpiId: `k-${health}-${category}`, health, category
    } as ExecutiveDashboardResponse['kpis'][number];
  }

  it('traduit chaque santé serveur en état visuel, l\'inconnu restant neutre', (done) => {
    service.getExecutiveKpis().subscribe(cards => {
      expect(cards.map(c => c.state)).toEqual(['good', 'warn', 'bad', 'neutral', 'neutral']);
      done();
    });

    flush({
      ...payload,
      kpis: [
        kpiWith('OK', 'quality'),
        kpiWith('WARNING', 'quality'),
        kpiWith('CRITICAL', 'quality'),
        kpiWith('UNKNOWN', 'quality'),
        // Une santé que le serveur ajouterait demain ne doit pas être colorée
        // au hasard : neutre par défaut vaut mieux qu'un vert usurpé.
        kpiWith('SOMETHING_NEW', 'quality')
      ]
    });
  });

  it('déduit l\'icône de la catégorie du KPI, avec un repli générique', (done) => {
    service.getExecutiveKpis().subscribe(cards => {
      expect(cards.map(c => c.icon)).toEqual([
        'engineering', 'workspace_premium', 'fact_check',
        'local_shipping', 'warning', 'paid', 'monitoring', 'monitoring'
      ]);
      done();
    });

    flush({
      ...payload,
      kpis: [
        kpiWith('OK', 'capa-actions'),
        kpiWith('OK', 'compliance'),
        kpiWith('OK', 'audit'),
        kpiWith('OK', 'supplier'),
        kpiWith('OK', 'risk'),
        kpiWith('OK', 'cost'),
        kpiWith('OK', 'catégorie-inconnue'),
        // Le catalogue ne porte pas d'icône : une catégorie absente doit rester
        // affichable, pas casser la carte.
        kpiWith('OK', null)
      ]
    });
  });

  it('normalise la casse de la catégorie avant de choisir l\'icône', (done) => {
    service.getExecutiveKpis().subscribe(cards => {
      expect(cards[0].icon).toBe('paid');
      done();
    });

    flush({ ...payload, kpis: [kpiWith('OK', 'COST')] });
  });

  it('gradue la sévérité des risques, l\'inconnue restant moyenne', (done) => {
    service.getTopRisks().subscribe(risks => {
      expect(risks.map(r => r.severity)).toEqual(['critical', 'high', 'medium', 'medium']);
      done();
    });

    flush({
      ...payload,
      topRisks: [
        { id: 'r1', title: 'A', source: 'FMEA', severity: 'CRITICAL', rpn: 240, dueDate: null },
        { id: 'r2', title: 'B', source: 'FMEA', severity: 'HIGH', rpn: null, dueDate: null },
        { id: 'r3', title: 'C', source: 'FMEA', severity: 'MEDIUM', rpn: null, dueDate: null },
        // Une sévérité inattendue ne doit pas être promue « critique » par
        // accident : le repli est le niveau intermédiaire.
        { id: 'r4', title: 'D', source: 'FMEA', severity: 'INCONNUE', rpn: null, dueDate: null }
      ]
    });
  });

  it('ne range en CAPA que ce qui vient réellement des CAPA', (done) => {
    service.getTopRisks().subscribe(risks => {
      expect(risks.map(r => r.sourceType)).toEqual(['CAPA', 'FMEA']);
      done();
    });

    flush({
      ...payload,
      topRisks: [
        { id: 'r1', title: 'A', source: 'CAPA', severity: 'HIGH', rpn: null, dueDate: null },
        { id: 'r2', title: 'B', source: 'AUTRE', severity: 'HIGH', rpn: null, dueDate: null }
      ]
    });
  });

  it('tolère une norme dépourvue de sections dans la heatmap', (done) => {
    service.getComplianceHeatmap().subscribe(cells => {
      // Une norme adoptée dont le référentiel n'est pas encore chargé ne doit
      // pas faire disparaître les autres lignes de la heatmap.
      expect(cells.map(c => c.norm)).toEqual(['iso-9001']);
      done();
    });

    // Le contrat déclare `sections` obligatoire : la garde `?? []` du service
    // protège contre un serveur qui ne le respecterait pas. Éprouver cette
    // garde suppose donc de sortir volontairement du type — c'est tout l'objet
    // du test, et le cast le dit explicitement.
    flush({
      ...payload,
      alignment: [
        {
          adoptionId: 'a1', standardCode: 'iso-9001', standardName: 'ISO 9001',
          score: 70, status: 'IN_PROGRESS', sections: [{ sectionCode: '4', score: 88 }]
        },
        {
          adoptionId: 'a2', standardCode: 'iso-14001', standardName: 'ISO 14001',
          score: 0, status: 'PLANNED'
        }
      ] as unknown as ExecutiveDashboardResponse['alignment']
    });
  });
});
