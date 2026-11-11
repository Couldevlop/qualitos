import { SpcViolation } from '../spc.types';
import { computeCapability } from './spc-capability.calc';
import { GOLDEN_GRID, GOLDEN_LSL, GOLDEN_USL } from './spc-capability.fixtures';
import {
  nelsonSeries, ruleCode, rulesByColumn, seriesKey, severityTone, violationSubgroups
} from './spc-capability.nelson';
import { CalculationView } from './spc-capability.types';

/** Une violation du service, réduite à ce que l'écran en lit. */
function violation(rule: string, points: number[], severity = 'high'): SpcViolation {
  return { rule, title: rule, description: '', pointIndices: points, severity };
}

describe('spc-capability.nelson', () => {

  const golden = computeCapability(GOLDEN_LSL, GOLDEN_USL, 5, GOLDEN_GRID);

  describe('nelsonSeries', () => {
    it('part de la baseline de la carte, pas d\'une estimation', () => {
      const s = nelsonSeries(golden)!;
      expect(s.center).toBe(golden.grandMean!);
      // σ se lit sur les limites tracées : UCL − X̿ vaut 3σ.
      expect(s.sigma).toBeCloseTo((golden.meanUcl! - golden.grandMean!) / 3, 12);
      expect(s.values.length).toBe(golden.subgroupMeans.filter(v => v !== null).length);
      expect(s.columns).toEqual(s.values.map((_, i) => i));
    });

    it('n\'envoie que les sous-groupes complets, et dit de quelles colonnes ils viennent', () => {
      const partiel: CalculationView = {
        ...golden,
        subgroupMeans: [10, null, 12, null, 11, ...Array(15).fill(null)]
      };
      const s = nelsonSeries(partiel)!;
      expect(s.values).toEqual([10, 12, 11]);
      expect(s.columns).toEqual([0, 2, 4]);
    });

    it('renonce sans limites, sans dispersion ou sous deux points', () => {
      expect(nelsonSeries({ ...golden, grandMean: null })).toBeNull();
      expect(nelsonSeries({ ...golden, meanUcl: null })).toBeNull();
      // Procédé figé : UCL = X̿, donc σ = 0 — aucune règle n'a de sens.
      expect(nelsonSeries({ ...golden, meanUcl: golden.grandMean })).toBeNull();
      expect(nelsonSeries({ ...golden, subgroupMeans: [10, ...Array(19).fill(null)] })).toBeNull();
    });
  });

  describe('seriesKey', () => {
    it('ne change pas à série égale, change dès qu\'une valeur ou la baseline bouge', () => {
      const a = nelsonSeries(golden)!;
      expect(seriesKey(a)).toBe(seriesKey(nelsonSeries(golden)!));
      expect(seriesKey({ ...a, center: a.center + 1 })).not.toBe(seriesKey(a));
      expect(seriesKey({ ...a, values: [...a.values.slice(1), 0] })).not.toBe(seriesKey(a));
    });
  });

  describe('ruleCode', () => {
    it('abrège les règles du service et laisse passer le reste', () => {
      expect(ruleCode('NELSON_1')).toBe('N1');
      expect(ruleCode('NELSON_8')).toBe('N8');
      expect(ruleCode('AUTRE')).toBe('AUTRE');
      expect(ruleCode('')).toBe('?');
      expect(ruleCode(undefined as unknown as string)).toBe('?');
    });
  });

  describe('rulesByColumn', () => {
    it('traduit les rangs de la série en colonnes de la grille, sans doublon', () => {
      const m = rulesByColumn(
        [violation('NELSON_2', [0, 1]), violation('NELSON_5', [1]), violation('NELSON_2', [1])],
        [3, 7]);
      expect(m.get(3)).toEqual(['N2']);
      expect(m.get(7)).toEqual(['N2', 'N5']);
      expect(m.size).toBe(2);
    });

    it('ignore un rang hors de la série envoyée et une réponse incomplète', () => {
      expect(rulesByColumn([violation('NELSON_1', [9])], [0, 1]).size).toBe(0);
      expect(rulesByColumn([{ rule: 'NELSON_1' } as SpcViolation], [0]).size).toBe(0);
      expect(rulesByColumn(null, [0]).size).toBe(0);
      expect(rulesByColumn(undefined, [0]).size).toBe(0);
    });
  });

  describe('violationSubgroups', () => {
    it('nomme les sous-groupes à partir de 1, triés et sans doublon', () => {
      expect(violationSubgroups(violation('NELSON_2', [2, 0, 2]), [5, 6, 7])).toEqual([6, 8]);
      expect(violationSubgroups(violation('NELSON_2', [4]), [0, 1])).toEqual([]);
      expect(violationSubgroups({ rule: 'NELSON_2' } as SpcViolation, [0])).toEqual([]);
    });
  });

  describe('severityTone', () => {
    it('couvre les deux vocabulaires du service', () => {
      expect(severityTone('high')).toBe('sev-crit');
      expect(severityTone('CRITICAL')).toBe('sev-crit');
      expect(severityTone('medium')).toBe('sev-maj');
      expect(severityTone('MAJOR')).toBe('sev-maj');
      expect(severityTone('low')).toBe('sev-min');
      expect(severityTone('')).toBe('sev-min');
    });
  });
});
