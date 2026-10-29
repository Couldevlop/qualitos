import { FormBuilder } from '@angular/forms';
import { of, throwError } from 'rxjs';

import { KpisService } from '../../../kpis/kpis.service';
import { KpiResponse, MeasurementResponse } from '../../../kpis/kpis.types';
import { AnomalyDetectComponent } from './anomaly-detect.component';
import { AnomalyService } from '../../anomaly.service';
import { AnomalyDetectRequest, AnomalyDetectResponse } from '../../anomaly.types';

/**
 * Ce qui se teste ici : le constat d'anomalie DÉBOUCHE, et ne promet rien qu'il
 * ne tienne. La boucle détection → action corrective s'arrêtait à l'écran ; elle
 * ne doit pas s'arrêter plus loin, dans un refus silencieux du serveur.
 */
describe('AnomalyDetectComponent — ouverture de CAPA', () => {

  let anomaly: jasmine.SpyObj<AnomalyService>;
  let kpiService: jasmine.SpyObj<KpisService>;
  let component: AnomalyDetectComponent;

  const response: AnomalyDetectResponse = {
    n: 3, nFeatures: 2, method: 'isolation_forest', contamination: 0.1,
    threshold: 0.7, anomalyCount: 1, hasAnomalies: true,
    points: [
      { index: 0, score: 0.4, isAnomaly: false, topFeature: null },
      { index: 1, score: 0.45, isAnomaly: false, topFeature: null },
      { index: 2, score: 0.9, isAnomaly: true, topFeature: null }
    ],
    capaId: null
  };

  beforeEach(() => {
    anomaly = jasmine.createSpyObj<AnomalyService>('AnomalyService', ['detect', 'explain']);
    kpiService = jasmine.createSpyObj<KpisService>('KpisService', ['list', 'listMeasurements']);
    anomaly.detect.and.returnValue(of(response));
    component = new AnomalyDetectComponent(new FormBuilder(), anomaly, kpiService);
    component.form.patchValue({ matrixText: '1, 2\n2, 4\n12, -8' });
  });

  function lastRequest(): AnomalyDetectRequest {
    return anomaly.detect.calls.mostRecent().args[0];
  }

  it("n'envoie ni sujet ni demande de CAPA par défaut", () => {
    component.detect();

    expect(lastRequest().subject).toBeUndefined();
    expect(lastRequest().openCapa).toBeUndefined();
  });

  it('transmet le sujet et la demande quand la case est cochée', () => {
    component.form.patchValue({ subject: '  presse-2 / ligne 3  ', openCapa: true });

    component.detect();

    expect(lastRequest().subject).toBe('presse-2 / ligne 3');
    expect(lastRequest().openCapa).toBeTrue();
  });

  it('refuse une demande de CAPA sans sujet, au lieu de la laisser échouer en silence', () => {
    component.form.patchValue({ subject: '   ', openCapa: true });

    component.detect();

    // Le serveur refuserait d'ouvrir un dossier sans sujet sans rien dire :
    // l'utilisateur croirait qu'une CAPA a été créée.
    expect(component.error).toBeTruthy();
    expect(anomaly.detect).not.toHaveBeenCalled();
  });

  it('expose la CAPA ouverte pour que l\'écran y renvoie', () => {
    anomaly.detect.and.returnValue(of({ ...response, capaId: 'capa-1' }));
    component.form.patchValue({ subject: 'presse-2', openCapa: true });

    component.detect();

    expect(component.result?.capaId).toBe('capa-1');
  });

  it('ne renvoie vers aucun dossier quand le serveur n\'en a pas ouvert', () => {
    component.form.patchValue({ subject: 'presse-2', openCapa: true });

    component.detect();

    // Cas normal : un dossier actif couvre déjà ce sujet.
    expect(component.result?.capaId).toBeNull();
  });
});

describe('AnomalyDetectComponent — matrice tirée des indicateurs', () => {
  const kpi = (id: string, code: string): KpiResponse => ({
    id, tenantId: 't', code, name: 'Indicateur ' + code, direction: 'HIGHER_IS_BETTER',
    status: 'ACTIVE', createdBy: 'u', createdAt: '', updatedAt: ''
  });
  const m = (periodStart: string, value: number) => ({ periodStart, value } as MeasurementResponse);
  const page = (content: MeasurementResponse[]) =>
    ({ content, totalElements: content.length, totalPages: 1, number: 0, size: content.length });

  let anomaly: jasmine.SpyObj<AnomalyService>;
  let kpiService: jasmine.SpyObj<KpisService>;
  let component: AnomalyDetectComponent;

  beforeEach(() => {
    anomaly = jasmine.createSpyObj<AnomalyService>('AnomalyService', ['detect', 'explain']);
    kpiService = jasmine.createSpyObj<KpisService>('KpisService', ['list', 'listMeasurements']);
    kpiService.list.and.returnValue(of({ content: [kpi('a', 'TRS'), kpi('b', 'REBUT')], totalElements: 2, totalPages: 1, number: 0, size: 2 }));
    component = new AnomalyDetectComponent(new FormBuilder(), anomaly, kpiService);
    component.ngOnInit();
  });

  it('propose les indicateurs actifs du catalogue', () => {
    expect(kpiService.list).toHaveBeenCalledWith(0, 100, 'ACTIVE');
    expect(component.kpis.map(k => k.code)).toEqual(['TRS', 'REBUT']);
  });

  it('exige deux indicateurs : l\'analyse est multivariée', () => {
    component.kpiSelection.setValue(['a']);
    component.loadFromKpis();
    expect(kpiService.listMeasurements).not.toHaveBeenCalled();
    expect(component.error).toContain('deux indicateurs');
  });

  it('remplit la matrice sur les périodes communes et nomme les variables', () => {
    kpiService.listMeasurements.and.callFake((id: string) => of(page(id === 'a'
      ? [m('2026-02', 82), m('2026-01', 80), m('2026-03', 85)]
      : [m('2026-01', 3), m('2026-03', 9)])));
    component.kpiSelection.setValue(['a', 'b']);
    component.loadFromKpis();

    expect(component.form.value.matrixText).toBe('80, 3\n85, 9');
    expect(component.columnNames).toEqual(['TRS', 'REBUT']);
    expect(component.form.value.subject).toBe('Indicateur TRS / Indicateur REBUT');
    expect(component.sourceInfo).toContain('2');
    expect(component.variableLabel(1)).toBe('REBUT');
    expect(component.topFeatureLabel({ index: 0, score: 1, isAnomaly: true, topFeature: 0 })).toBe('TRS');
  });

  it('revient aux rangs quand la matrice est retapée à la main', () => {
    component.columnNames = ['TRS'];
    component.onManualEdit();
    expect(component.variableLabel(0)).toBe('Variable 1');
    expect(component.variablesPerSample(3)).toContain('3 variable');
  });

  it('reste utilisable si le catalogue ne répond pas', () => {
    kpiService.list.and.returnValue(throwError(() => new Error('down')));
    const c = new AnomalyDetectComponent(new FormBuilder(), anomaly, kpiService);
    c.ngOnInit();
    expect(c.kpis).toEqual([]);
  });
});
