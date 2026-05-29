import { HttpErrorResponse } from '@angular/common/http';
import { FormBuilder } from '@angular/forms';
import { of, throwError } from 'rxjs';

import { NlqService } from '../../nlq.service';
import { NlqAskResponse } from '../../nlq.types';
import { NlqAskComponent } from './nlq-ask.component';

/**
 * Tests unitaires de la logique du composant NLQ (instanciation directe, sans
 * TestBed/template — rapide et sans dépendance navigateur supplémentaire).
 */
describe('NlqAskComponent', () => {
  let component: NlqAskComponent;
  let nlq: jasmine.SpyObj<NlqService>;

  const response: NlqAskResponse = {
    question: 'Combien de CAPA par statut ?',
    sql: 'SELECT status, COUNT(*) AS count FROM capa_cases WHERE tenant_id = :tenant_id GROUP BY status',
    tenantFilterApplied: true,
    tablesUsed: ['capa_cases'],
    functionsUsed: ['count'],
    rows: [
      { status: 'OPEN', count: 4 },
      { status: 'CLOSED', count: 12 }
    ],
    rowCount: 2,
    confidence: 0.85,
    chart: { chart_type: 'bar' },
    narrative: '16 CAPA au total.'
  };

  beforeEach(() => {
    nlq = jasmine.createSpyObj<NlqService>('NlqService', ['ask']);
    component = new NlqAskComponent(new FormBuilder(), nlq);
  });

  it('crée le composant avec un formulaire invalide (question requise)', () => {
    expect(component).toBeTruthy();
    expect(component.form.invalid).toBeTrue();
  });

  describe('ask()', () => {
    it('peuple le résultat et arrête le chargement en cas de succès', () => {
      nlq.ask.and.returnValue(of(response));
      component.form.setValue({ question: 'Combien de CAPA par statut ?' });

      component.ask();

      expect(nlq.ask).toHaveBeenCalledWith('Combien de CAPA par statut ?');
      expect(component.result).toEqual(response);
      expect(component.loading).toBeFalse();
      expect(component.error).toBeNull();
    });

    it('applique un exemple (preset) puis interroge', () => {
      nlq.ask.and.returnValue(of(response));
      component.ask('Score moyen des audits 5S par zone');
      expect(component.form.value.question).toBe('Score moyen des audits 5S par zone');
      expect(nlq.ask).toHaveBeenCalledWith('Score moyen des audits 5S par zone');
    });

    it('ne fait rien si la question est vide', () => {
      component.form.setValue({ question: '   ' });
      component.ask();
      expect(nlq.ask).not.toHaveBeenCalled();
    });

    it('ne relance pas un appel pendant le chargement', () => {
      component.loading = true;
      component.form.setValue({ question: 'q' });
      component.ask();
      expect(nlq.ask).not.toHaveBeenCalled();
    });

    it('mappe une erreur 502/503 sur un message « indisponible »', () => {
      nlq.ask.and.returnValue(throwError(() => new HttpErrorResponse({ status: 503 })));
      component.form.setValue({ question: 'q' });
      component.ask();
      expect(component.loading).toBeFalse();
      expect(component.error).toContain('indisponible');
    });

    it('mappe une erreur 422 sur un message de reformulation', () => {
      nlq.ask.and.returnValue(throwError(() => new HttpErrorResponse({ status: 422 })));
      component.form.setValue({ question: 'q' });
      component.ask();
      expect(component.error).toContain('Reformule');
    });

    it('mappe les autres erreurs sur un message générique', () => {
      nlq.ask.and.returnValue(throwError(() => new HttpErrorResponse({ status: 500 })));
      component.form.setValue({ question: 'q' });
      component.ask();
      expect(component.error).toContain('Une erreur est survenue');
    });
  });

  describe('dérivation des colonnes / barres', () => {
    beforeEach(() => { component.result = response; });

    it('columns() dérive les clés de la première ligne', () => {
      expect(component.columns()).toEqual(['status', 'count']);
    });

    it('numericColumns() ne garde que les colonnes 100% numériques', () => {
      expect(component.numericColumns()).toEqual(['count']);
      expect(component.isNumericColumn('count')).toBeTrue();
      expect(component.isNumericColumn('status')).toBeFalse();
    });

    it('barPercent() est relatif au max de la colonne', () => {
      expect(component.barPercent(12, 'count')).toBe(100); // max
      expect(component.barPercent(4, 'count')).toBe(33);
      expect(component.barPercent('x', 'count')).toBe(0);  // non-numérique
    });

    it('columns() vide si aucune ligne', () => {
      component.result = { ...response, rows: [] };
      expect(component.columns()).toEqual([]);
      expect(component.numericColumns()).toEqual([]);
    });
  });

  describe('cellules & confiance', () => {
    it('cell() rend — pour null/undefined', () => {
      expect(component.cell(null)).toBe('—');
      expect(component.cell(undefined)).toBe('—');
      expect(component.cell('OPEN')).toBe('OPEN');
    });

    it('confidencePct()/confidenceClass() selon le seuil', () => {
      component.result = { ...response, confidence: 0.85 };
      expect(component.confidencePct()).toBe(85);
      expect(component.confidenceClass()).toBe('ok');

      component.result = { ...response, confidence: 0.6 };
      expect(component.confidenceClass()).toBe('warn');

      component.result = { ...response, confidence: 0.3 };
      expect(component.confidenceClass()).toBe('bad');
    });
  });
});
