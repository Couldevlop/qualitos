import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';

import { KpisService } from '../../../kpis/kpis.service';
import {
  KpiDirection, KpiResponse, MeasurementPage, MeasurementResponse
} from '../../../kpis/kpis.types';
import { ForecastService } from '../../forecast.service';
import { ForecastResponse } from '../../forecast.types';
import { ForecastSharedModule } from '../../forecast-shared.module';
import { KpiForecastPanelComponent, MIN_POINTS } from './kpi-forecast-panel.component';

/**
 * La prévision se lit dans la fiche de l'indicateur, sur SES mesures et SA cible.
 *
 * <p>Elle vivait dans une page d'essai à part : il fallait y retourner, rechoisir
 * l'indicateur, et comparer de tête avec sa fiche. Ce banc vérifie surtout que le
 * panneau ne reste jamais muet — série trop courte, cible absente ou service en
 * panne se disent, faute de quoi on lirait « cet indicateur n'a pas de tendance ».
 */
describe('KpiForecastPanelComponent', () => {

  function kpi(over: Partial<KpiResponse> = {}): KpiResponse {
    return {
      id: '11111111-1111-1111-1111-111111111111',
      tenantId: 't1', code: 'FPY', name: 'First Pass Yield',
      direction: 'HIGHER_IS_BETTER' as KpiDirection, targetValue: 95, unit: '%',
      status: 'ACTIVE', createdBy: 'moi', createdAt: '2026-01-01', updatedAt: '2026-01-01',
      ...over
    };
  }

  /** Une page de mesures, telle que la renvoie l'API des indicateurs. */
  function page(valeurs: number[]): MeasurementPage {
    const content = mesures(valeurs);
    return { content, totalElements: content.length, totalPages: 1, number: 0, size: 200 };
  }

  function mesures(valeurs: number[]): MeasurementResponse[] {
    return valeurs.map((value, i) => ({
      id: 'm' + i, tenantId: 't1', kpiId: 'k1',
      periodStart: `2026-0${(i % 9) + 1}-01`, periodEnd: `2026-0${(i % 9) + 1}-28`,
      value, health: 'OK', createdAt: '2026-01-01'
    } as MeasurementResponse));
  }

  const REPONSE: ForecastResponse = {
    n: 6, slope: 1.2, intercept: 90, residualSigma: 1.1, r2: 0.93,
    horizon: 6, target: 95, direction: 'at_least', probability: 0.82,
    confidence: 'high', model: 'holt_linear', seasonalPeriod: 0,
    points: [
      { step: 1, value: 91, low: 89, high: 93 },
      { step: 2, value: 92, low: 89, high: 95 }
    ]
  };

  let fixture: ComponentFixture<KpiForecastPanelComponent>;
  let component: KpiForecastPanelComponent;
  let kpis: jasmine.SpyObj<KpisService>;
  let forecast: jasmine.SpyObj<ForecastService>;

  function hote(): HTMLElement {
    return fixture.nativeElement as HTMLElement;
  }

  function texte(selecteur: string): string {
    return hote().querySelector(selecteur)?.textContent?.trim() ?? '';
  }

  beforeEach(async () => {
    kpis = jasmine.createSpyObj<KpisService>('KpisService', ['listMeasurements']);
    forecast = jasmine.createSpyObj<ForecastService>('ForecastService', ['forecast']);
    kpis.listMeasurements.and.returnValue(of(page([88, 89, 90, 91, 92, 93])));
    forecast.forecast.and.returnValue(of(REPONSE));

    await TestBed.configureTestingModule({
      imports: [ForecastSharedModule, NoopAnimationsModule],
      providers: [
        { provide: KpisService, useValue: kpis },
        { provide: ForecastService, useValue: forecast }
      ]
    }).compileComponents();
    fixture = TestBed.createComponent(KpiForecastPanelComponent);
    component = fixture.componentInstance;
  });

  /** Pose l'indicateur et laisse le cycle de vie faire son travail. */
  function poser(k: KpiResponse | null): void {
    fixture.componentRef.setInput('kpi', k);
    fixture.detectChanges();
  }

  // ---------- rien à projeter ----------

  it('ne demande rien tant qu’aucun indicateur n’est posé', () => {
    fixture.detectChanges();
    expect(kpis.listMeasurements).not.toHaveBeenCalled();
    expect(forecast.forecast).not.toHaveBeenCalled();
  });

  // Projeter une courbe que rien ne vient juger n'informe pas : sans cible,
  // « atteindra-t-il son objectif ? » n'a pas de réponse.
  it('dit que la cible manque au lieu de projeter une courbe sans juge', () => {
    poser(kpi({ targetValue: undefined }));
    expect(kpis.listMeasurements).not.toHaveBeenCalled();
    expect(forecast.forecast).not.toHaveBeenCalled();
    expect(texte('[data-test="forecast-unavailable"]')).toContain('cible');
  });

  it('dit combien de mesures manquent quand la série est trop courte', () => {
    kpis.listMeasurements.and.returnValue(of(page([88, 90])));
    poser(kpi());
    expect(forecast.forecast).not.toHaveBeenCalled();
    const dit = texte('[data-test="forecast-unavailable"]');
    expect(dit).toContain('2');
    expect(dit).toContain(String(MIN_POINTS));
  });

  // ---------- la projection ----------

  it('projette la série de l’indicateur sur sa cible et son sens', () => {
    poser(kpi());
    expect(kpis.listMeasurements).toHaveBeenCalledWith(kpi().id, 0, 200);
    expect(forecast.forecast).toHaveBeenCalledWith({
      values: [88, 89, 90, 91, 92, 93], target: 95, horizon: 6, direction: 'at_least'
    });
    expect(component.result).toEqual(REPONSE);
    expect(component.chartOption).not.toBeNull();
    expect(component.unavailable).toBeNull();
  });

  // Un indicateur à minimiser (taux de rebut) ne se juge pas « au moins » :
  // le sens de la fiche doit traverser jusqu'au moteur.
  it('traduit « plus bas = mieux » en « ne pas dépasser »', () => {
    poser(kpi({ direction: 'LOWER_IS_BETTER', targetValue: 2 }));
    expect(forecast.forecast).toHaveBeenCalledWith(
      jasmine.objectContaining({ direction: 'at_most', target: 2 }));
    expect(component.directionLabel()).toContain('dépasser');
  });

  it('affiche la probabilité, la confiance et le modèle', () => {
    poser(kpi());
    expect(texte('[data-test="forecast-probability"]')).toContain('82 %');
    expect(texte('[data-test="forecast-confidence"]')).toContain('high');
    expect(texte('[data-test="forecast-model"]')).toContain('Holt');
    // Les points projetés sont lisibles au chiffre près, pas seulement sur la courbe.
    expect(hote().querySelectorAll('.forecast-table tbody tr').length).toBe(2);
  });

  it('nomme le modèle saisonnier avec sa période', () => {
    expect(component.modelLabel('holt_winters_additive', 12)).toContain('12');
    expect(component.modelLabel('holt_linear', 0)).not.toContain('0');
  });

  it('classe la probabilité et la confiance en trois états', () => {
    expect(component.probabilityState(0.8)).toBe('good');
    expect(component.probabilityState(0.5)).toBe('warn');
    expect(component.probabilityState(0.2)).toBe('bad');
    expect(component.confidenceState('high')).toBe('good');
    expect(component.confidenceState('medium')).toBe('warn');
    expect(component.confidenceState('low')).toBe('neutral');
  });

  // ---------- les pannes se disent ----------

  it('dit que les mesures sont illisibles quand le registre ne répond pas', () => {
    kpis.listMeasurements.and.returnValue(throwError(() => new Error('503')));
    poser(kpi());
    expect(component.loading).toBeFalse();
    expect(texte('[data-test="forecast-unavailable"]')).toContain('mesures');
  });

  it('traduit chaque panne du service de prévision en une phrase', () => {
    const cas: Array<[number, string]> = [
      [0, 'injoignable'], [422, 'insuffisante'], [429, 'Débit'],
      [502, 'indisponible'], [503, 'indisponible'], [418, '418']
    ];
    for (const [status, attendu] of cas) {
      forecast.forecast.and.returnValue(throwError(() => new HttpErrorResponse({ status })));
      component.kpi = kpi();
      component.reload();
      expect(component.loading).toBeFalse();
      expect(component.unavailable).withContext('HTTP ' + status).toContain(attendu);
    }
  });

  // ---------- la relecture ----------

  it('relit les mesures à la demande, après un enregistrement', () => {
    poser(kpi());
    expect(kpis.listMeasurements).toHaveBeenCalledTimes(1);
    component.reload();
    expect(kpis.listMeasurements).toHaveBeenCalledTimes(2);
  });

  it('oublie le résultat précédent avant de reprojeter', () => {
    poser(kpi());
    expect(component.result).not.toBeNull();
    kpis.listMeasurements.and.returnValue(throwError(() => new Error('503')));
    component.reload();
    expect(component.result).toBeNull();
    expect(component.chartOption).toBeNull();
  });
});
