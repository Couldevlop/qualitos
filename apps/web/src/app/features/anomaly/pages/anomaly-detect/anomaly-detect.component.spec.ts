import { FormBuilder } from '@angular/forms';
import { of } from 'rxjs';

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
    anomaly.detect.and.returnValue(of(response));
    component = new AnomalyDetectComponent(new FormBuilder(), anomaly);
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
