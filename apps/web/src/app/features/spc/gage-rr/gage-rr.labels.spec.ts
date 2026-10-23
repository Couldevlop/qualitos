import { goldenCalculation } from './gage-rr.fixtures';
import {
  anovaSourceLabel, decisionLabel, decisionTone, fieldMessage, fmt, interpretation, ndcLabel, operatorFallback,
  operatorLetter, pValue, pct
} from './gage-rr.labels';

describe('gage-rr.labels', () => {

  it('dit le verdict et sa couleur', () => {
    expect(decisionLabel('ACCEPTABLE')).toBe('Acceptable');
    expect(decisionLabel('CONDITIONAL')).toBe('Acceptable sous conditions');
    expect(decisionLabel('NOT_ACCEPTABLE')).toBe('Non acceptable');
    expect(decisionTone('ACCEPTABLE')).toBe('ok');
    expect(decisionTone('CONDITIONAL')).toBe('warn');
    expect(decisionTone('NOT_ACCEPTABLE')).toBe('bad');
  });

  it('dit la discrimination et nomme un opérateur pas encore choisi', () => {
    expect(ndcLabel(true)).toBe('Discrimination suffisante');
    expect(ndcLabel(false)).toBe('Discrimination insuffisante');
    expect(operatorLetter(2)).toBe('C');
    expect(operatorFallback(1)).toBe('Opérateur B');
  });

  it('nomme chaque ligne du tableau d\'ANOVA', () => {
    expect(anovaSourceLabel('PART')).toBe('Pièces');
    expect(anovaSourceLabel('OPERATOR')).toBe('Opérateurs');
    expect(anovaSourceLabel('INTERACTION')).toBe('Interaction pièce × opérateur');
    expect(anovaSourceLabel('REPEATABILITY')).toBe('Répétabilité');
    expect(anovaSourceLabel('REPEATABILITY', true)).toBe('Répétabilité (interaction regroupée)');
    expect(anovaSourceLabel('TOTAL')).toBe('Total');
  });

  it('formate nombres, pourcentages et p-values', () => {
    expect(fmt(0.04137833)).toBe('0.0414');
    expect(fmt(1.5, 2)).toBe('1.50');
    expect(fmt(null)).toBe('–');
    expect(fmt(undefined)).toBe('–');
    expect(fmt(Number.NaN)).toBe('–');
    expect(pct(0.20998)).toBe('21.0 %');
    expect(pct(0.20998, 2)).toBe('21.00 %');
    expect(pct(null)).toBe('–');
    expect(pct(Number.POSITIVE_INFINITY)).toBe('–');
    expect(pValue(0.358386)).toBe('0.3584');
    expect(pValue(3.19e-17)).toBe('3.19e-17');
    expect(pValue(null)).toBe('–');
  });

  it('recompose l\'interprétation du classeur (cellules A42 à A46)', () => {
    const lignes = interpretation(goldenCalculation());
    expect(lignes.length).toBe(5);
    expect(lignes[0]).toBe('Répétabilité (EV) = 0.0414, soit 17.6 % de la variation totale : dispersion d\'un même opérateur mesurant plusieurs fois la même pièce avec le même instrument.');
    expect(lignes[1]).toBe('Reproductibilité (AV) = 0.0271, soit 11.5 % : écart entre opérateurs (X̄diff = 0.0104).');
    expect(lignes[2]).toBe('GRR = 0.0495 : 21.0 % de la variation totale et 24.7 % de la tolérance. Décision : Acceptable sous conditions.');
    expect(lignes[3]).toContain('6 catégories');
    expect(lignes[3]).toContain('suffisant pour piloter');
    expect(lignes[4]).toContain('répétabilité (instrument ou méthode)');
  });

  it('dit l\'autre branche de l\'interprétation', () => {
    const c = goldenCalculation();
    const lignes = interpretation({
      ...c, ndc: 3, ndcSufficient: false, repeatabilityDominant: false,
      grr: { ...c.grr, percentTolerance: null }
    });
    expect(lignes[2]).toContain('– % de la tolérance');
    expect(lignes[3]).toContain('insuffisant');
    expect(lignes[4]).toContain('reproductibilité (opérateurs)');
  });

  it('dit un refus du serveur sans reprendre son texte', () => {
    expect(fieldMessage('partReference')).toContain('référence de la pièce');
    expect(fieldMessage('studyDate')).toContain('date');
    expect(fieldMessage('lsl')).toBe(fieldMessage('usl'));
    expect(fieldMessage('operators')).toContain('annuaire');
    expect(fieldMessage('responsibleName')).toBe(fieldMessage('responsibleUserId'));
    for (const f of ['operatorCount', 'partCount', 'trialCount', 'sigmaMultiplier']) {
      expect(fieldMessage(f)).toContain('2 ou 3 opérateurs');
    }
    expect(fieldMessage('measurements')).toContain('mesures');
    expect(fieldMessage('autre')).toContain('vérifiez la saisie');
  });
});
