import {
  constantsFor, computeGageRr, decisionOf, fSurvival, isComplete, lnGamma, ndcOf, parseMeasurement, pasteBlock,
  regularizedBeta
} from './gage-rr.calc';
import { GOLDEN, GOLDEN_GRID, GOLDEN_LSL, GOLDEN_USL, goldenCalculation } from './gage-rr.fixtures';
import { GageRrCalculationView } from './gage-rr.types';

/**
 * Le calcul de l'écran rend les nombres du classeur `docs/spc/Etude_RR_Gage_1.xlsx`,
 * les mêmes que le test doré du serveur (`GageRrCalculationTest`).
 */
describe('gage-rr.calc', () => {

  describe('méthode moyenne et étendue (onglet « Résultats »)', () => {
    let c: GageRrCalculationView;

    beforeEach(() => c = goldenCalculation());

    it('étape 1 : moyennes, étendues, X̄diff, R̄̄, Rp et UCLR', () => {
      GOLDEN.operatorMeans.forEach((v, o) => expect(c.operatorMeans[o]).toBeCloseTo(v, 6));
      GOLDEN.operatorRanges.forEach((v, o) => expect(c.operatorRanges[o]).toBeCloseTo(v, 6));
      expect(c.xDiff).toBeCloseTo(GOLDEN.xDiff, 6);
      expect(c.meanRange).toBeCloseTo(GOLDEN.meanRange, 6);
      expect(c.partRange).toBeCloseTo(GOLDEN.partRange, 6);
      expect(c.rangeUcl).toBeCloseTo(GOLDEN.rangeUcl, 6);
      expect(c.maxRange).toBeCloseTo(GOLDEN.maxRange, 6);
      expect(c.rangesBeyondLimit).toBe(0);
      expect(c.grandMean).toBeCloseTo(GOLDEN.grandMean, 6);
      expect(c.trialMeans[0][0]).toBeCloseTo(10.0103, 6);
      expect(c.cellMeans.length).toBe(3);
      expect(c.partMeans.length).toBe(10);
    });

    it('étape 2 : les constantes du plan 3 × 10 × 3 à 5,15 σ', () => {
      expect(c.k1).toBeCloseTo(3.05, 10);
      expect(c.k2).toBeCloseTo(2.70, 10);
      expect(c.k3).toBeCloseTo(1.62, 10);
      expect(c.d4).toBe(2.58);
      expect(c.tolerance).toBeCloseTo(0.2, 10);
    });

    it('étapes 3 et 4 : EV, AV, GRR, PV, TV, %GRR, ndc et décision', () => {
      expect(c.ev.value).toBeCloseTo(GOLDEN.ev, 6);
      expect(c.av.value).toBeCloseTo(GOLDEN.av, 6);
      expect(c.grr.value).toBeCloseTo(GOLDEN.grr, 6);
      expect(c.pv.value).toBeCloseTo(GOLDEN.pv, 6);
      expect(c.tv.value).toBeCloseTo(GOLDEN.tv, 6);
      expect(c.grr.percentOfTotal).toBeCloseTo(GOLDEN.grrPercent, 4);
      expect(c.grr.percentTolerance!).toBeCloseTo(GOLDEN.grrTolerance, 4);
      expect(c.grr.percentContribution).toBeCloseTo(0.0441, 4);
      expect(c.ev.percentContribution + c.av.percentContribution + c.pv.percentContribution).toBeCloseTo(1, 10);
      expect(c.tv.percentOfTotal).toBe(1);
      expect(c.ndc).toBe(GOLDEN.ndc);
      expect(c.ndcSufficient).toBeTrue();
      expect(c.decision).toBe('CONDITIONAL');
      expect(c.repeatabilityDominant).toBeTrue();
    });

    it('met K1, K2 et K3 à l\'échelle de 6 σ, sans toucher à D4 ni au %GRR de la variation totale', () => {
      const six = computeGageRr(GOLDEN_LSL, GOLDEN_USL, 6, GOLDEN_GRID)!;
      expect(six.k1).toBeCloseTo(3.05 * 6 / 5.15, 10);
      expect(six.d4).toBe(2.58);
      expect(six.grr.percentOfTotal).toBeCloseTo(c.grr.percentOfTotal, 10);
      expect(six.grr.percentTolerance!).toBeCloseTo(0.28825, 4);
    });

    it('laisse le % de tolérance vide sans limites valides', () => {
      const sans = computeGageRr(null, null, 5.15, GOLDEN_GRID)!;
      expect(sans.grr.percentTolerance).toBeNull();
      expect(sans.tolerance).toBe(0);
      expect(computeGageRr(10.1, 9.9, 5.15, GOLDEN_GRID)!.grr.percentTolerance).toBeNull();
    });

    it('compte les étendues au-dessus de UCLR', () => {
      const grille = GOLDEN_GRID.map(b => b.map(e => [...e]));
      grille[0][0] = [9.9, 10.2, 10.0];
      const r = computeGageRr(GOLDEN_LSL, GOLDEN_USL, 5.15, grille)!;
      expect(r.rangesBeyondLimit).toBe(1);
      expect(r.maxRange).toBeCloseTo(0.3, 10);
    });

    it('ramène AV à zéro quand les opérateurs ne diffèrent pas, et dit quel poste domine', () => {
      const r = computeGageRr(0, 10, 5.15, [[[1, 2], [3, 4]], [[1, 2], [3, 4]]])!;
      expect(r.xDiff).toBe(0);
      expect(r.av.value).toBe(0);
      expect(r.repeatabilityDominant).toBeTrue();
      const sansRepet = computeGageRr(0, 10, 5.15, [[[1, 1], [2, 2]], [[1.5, 1.5], [2.5, 2.5]]])!;
      expect(sansRepet.ev.value).toBe(0);
      expect(sansRepet.av.value).toBeCloseTo(1.825, 10);
      expect(sansRepet.repeatabilityDominant).toBeFalse();
    });

    it('rend une variation nulle quand toutes les mesures sont identiques', () => {
      const r = computeGageRr(0, 2, 5.15, [[[1, 1], [1, 1]], [[1, 1], [1, 1]]])!;
      expect(r.tv.value).toBe(0);
      expect(r.ndc).toBe(0);
      expect(r.grr.percentOfTotal).toBe(0);
      expect(r.decision).toBe('ACCEPTABLE');
      expect(r.anova.fullTable[0].f).toBeNull();
      expect(r.anova.interactionPooled).toBeTrue();
    });

    it('refuse un plan hors table ou un multiplicateur inconnu', () => {
      expect(computeGageRr(0, 1, 5.15, [[[1, 2]]])).toBeNull();
      expect(computeGageRr(0, 1, 7, GOLDEN_GRID)).toBeNull();
      expect(computeGageRr(0, 1, 5.15, [])).toBeNull();
    });
  });

  describe('ANOVA à deux facteurs', () => {
    it('rend le tableau complet du classeur et regroupe l\'interaction (p > 0,25)', () => {
      const a = goldenCalculation().anova;
      const [part, op, inter, rep, total] = a.fullTable;
      expect(part.sumOfSquares).toBeCloseTo(GOLDEN.anova.ssParts, 8);
      expect(op.sumOfSquares).toBeCloseTo(GOLDEN.anova.ssOperators, 8);
      expect(inter.sumOfSquares).toBeCloseTo(GOLDEN.anova.ssInteraction, 8);
      expect(rep.sumOfSquares).toBeCloseTo(GOLDEN.anova.ssRepeatability, 8);
      expect(total.sumOfSquares).toBeCloseTo(GOLDEN.anova.ssTotal, 7);
      expect([part, op, inter, rep, total].map(r => r.degreesOfFreedom)).toEqual([9, 2, 18, 60, 89]);
      expect(part.f!).toBeCloseTo(262.55, 2);
      expect(op.f!).toBeCloseTo(14.215, 3);
      expect(inter.f!).toBeCloseTo(GOLDEN.anova.fInteraction, 3);
      expect(inter.p!).toBeCloseTo(GOLDEN.anova.pInteraction, 4);
      expect(rep.f).toBeNull();
      expect(total.meanSquare).toBeNull();
      expect(a.interactionPooled).toBeTrue();
    });

    it('rend le tableau regroupé et les composantes de variance', () => {
      const a = goldenCalculation().anova;
      const [part, op, rep] = a.pooledTable!;
      expect(rep.degreesOfFreedom).toBe(78);
      expect(rep.meanSquare!).toBeCloseTo(GOLDEN.anova.msPool, 10);
      expect(part.f!).toBeCloseTo(GOLDEN.anova.fPartsPooled, 2);
      expect(op.f!).toBeCloseTo(GOLDEN.anova.fOperatorsPooled, 3);
      expect(op.p! / GOLDEN.anova.pOperatorsPooled).toBeCloseTo(1, 2);
      expect(a.variance.repeatability).toBeCloseTo(GOLDEN.anova.msPool, 10);
      expect(a.variance.operator).toBeCloseTo(GOLDEN.anova.varOperator, 10);
      expect(a.variance.interaction).toBe(0);
      expect(a.variance.part).toBeCloseTo(GOLDEN.anova.varPart, 9);
      expect(a.variance.total).toBeCloseTo(GOLDEN.anova.varTotal, 9);
      expect(a.ev.value).toBeCloseTo(GOLDEN.anova.ev, 6);
      expect(a.av.value).toBeCloseTo(GOLDEN.anova.av, 6);
      expect(a.grr.value).toBeCloseTo(GOLDEN.anova.grr, 6);
      expect(a.pv.value).toBeCloseTo(GOLDEN.anova.pv, 6);
      expect(a.tv.value).toBeCloseTo(GOLDEN.anova.tv, 6);
      expect(a.grr.percentOfTotal).toBeCloseTo(GOLDEN.anova.grrPercent, 4);
      expect(a.grr.percentTolerance!).toBeCloseTo(GOLDEN.anova.grrTolerance, 4);
      expect(a.ndc).toBe(GOLDEN.anova.ndc);
      expect(a.ndcSufficient).toBeTrue();
      expect(a.decision).toBe('CONDITIONAL');
    });

    it('garde une interaction forte dans la reproductibilité', () => {
      const r = computeGageRr(null, null, 5.15,
        [[[1, 1.01], [2, 2.01], [1.5, 1.52]], [[2, 2.02], [1, 1], [1.6, 1.61]]])!;
      const inter = r.anova.fullTable[2];
      expect(r.anova.interactionPooled).toBeFalse();
      expect(r.anova.pooledTable).toBeNull();
      expect(inter.f!).toBeCloseTo(11051.27, 1);
      expect(inter.p! / 1.9988e-11).toBeCloseTo(1, 3);
      expect(r.anova.variance.interaction).toBeCloseTo(0.50647, 5);
      expect(r.anova.av.value).toBeCloseTo(3.665088, 5);
      expect(r.anova.grr.percentTolerance).toBeNull();
    });

    it('regroupe une interaction nulle même sans répétabilité', () => {
      const r = computeGageRr(0, 3, 5.15, [[[1, 1], [2, 2]], [[1.5, 1.5], [2.5, 2.5]]])!;
      expect(r.anova.fullTable[2].f).toBeNull();
      expect(r.anova.interactionPooled).toBeTrue();
    });
  });

  describe('outils', () => {
    it('lit les constantes d\'un plan admis seulement', () => {
      expect(constantsFor(2, 5, 2, 5.15)).toEqual({ k1: 4.56, k2: 3.65, k3: 2.08, d4: 3.27 });
      expect(constantsFor(4, 10, 3, 5.15)).toBeNull();
      expect(constantsFor(3, 11, 3, 5.15)).toBeNull();
      expect(constantsFor(3, 10, 4, 5.15)).toBeNull();
      expect(constantsFor(3, 10, 3, 7)).toBeNull();
    });

    it('rend la décision AIAG aux seuils de 10 % et 30 %', () => {
      expect(decisionOf(0.099)).toBe('ACCEPTABLE');
      expect(decisionOf(0.10)).toBe('CONDITIONAL');
      expect(decisionOf(0.30)).toBe('CONDITIONAL');
      expect(decisionOf(0.3001)).toBe('NOT_ACCEPTABLE');
    });

    it('arrondit le ndc à l\'entier inférieur, 0 sans GRR', () => {
      expect(ndcOf(1, 0.3)).toBe(4);
      expect(ndcOf(1, 0)).toBe(0);
    });

    it('reconnaît une grille complète', () => {
      expect(isComplete(GOLDEN_GRID)).toBeTrue();
      expect(isComplete([[[1, null], [1, 2]], [[1, 2], [1, 2]]])).toBeFalse();
      expect(isComplete([[[1, Number.NaN], [1, 2]], [[1, 2], [1, 2]]])).toBeFalse();
      expect(isComplete([[[1, 2], [1, 2]], [[1, 2]]])).toBeFalse();
      expect(isComplete([[[1, 2]]])).toBeFalse();
      expect(isComplete([])).toBeFalse();
    });

    it('calcule la loi F, la bêta incomplète et ln Γ', () => {
      expect(fSurvival(1.118, 18, 60)).toBeCloseTo(0.35842, 4);
      expect(fSurvival(0, 2, 3)).toBe(1);
      expect(fSurvival(-1, 2, 3)).toBe(1);
      expect(fSurvival(Number.POSITIVE_INFINITY, 2, 3)).toBe(0);
      expect(regularizedBeta(0.5, 2, 3)).toBeCloseTo(0.6875, 10);
      expect(regularizedBeta(0, 2, 3)).toBe(0);
      expect(regularizedBeta(1, 2, 3)).toBe(1);
      expect(regularizedBeta(0.9, 2, 3)).toBeCloseTo(0.9963, 4);
      expect(lnGamma(0.25)).toBeCloseTo(1.2880225, 6);
      expect(lnGamma(5)).toBeCloseTo(Math.log(24), 10);
    });

    it('lit une mesure saisie à la virgule ou au point', () => {
      expect(parseMeasurement(' 10,05 ')).toBe(10.05);
      expect(parseMeasurement('9.9')).toBe(9.9);
      expect(parseMeasurement('1 000')).toBe(1000);
      expect(parseMeasurement('')).toBeNull();
      expect(parseMeasurement('4a9')).toBeNull();
    });

    it('colle un bloc de tableur opérateur par opérateur, en ignorant ce qui déborde', () => {
      const vide = [[['', '']], [['', '']]];
      expect(pasteBlock(vide, 0, 1, '1\t2\t3\r\n4\n', 2, 1, 2)).toEqual([[['', '1']], [['2', '3']]]);
      expect(vide).toEqual([[['', '']], [['', '']]]);
      expect(pasteBlock(vide, 0, 0, '1\t2\t3\t4\t5', 2, 1, 2)).toEqual([[['1', '2']], [['3', '4']]]);
    });
  });
});
