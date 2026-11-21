import {
  objectiveLevelLabel,
  objectiveStatusLabel,
  objectiveStatusTone,
  periodLabel,
  PERIODES,
  proposalStatusLabel
} from './objectives.labels';

describe('objectives.labels — les mots des objectifs qualité', () => {

  it('nomme les quatre statuts', () => {
    expect(objectiveStatusLabel('ON_TARGET')).toBe('Atteint');
    expect(objectiveStatusLabel('WARNING')).toBe('Alerte');
    expect(objectiveStatusLabel('CRITICAL')).toBe('Critique');
    expect(objectiveStatusLabel('NOT_EVALUATED')).toBe('Non mesuré');
  });

  /**
   * « Non mesuré » reste NEUTRE. Le peindre en vert ferait passer un objectif
   * qu'on ne mesure pas pour un objectif tenu — c'est le contraire de ce que la
   * revue doit montrer.
   */
  it('laisse « non mesuré » en neutre, jamais en succès', () => {
    expect(objectiveStatusTone('NOT_EVALUATED')).toBe('neutral');
    expect(objectiveStatusTone('ON_TARGET')).toBe('success');
    expect(objectiveStatusTone('WARNING')).toBe('warn');
    expect(objectiveStatusTone('CRITICAL')).toBe('danger');
  });

  it('rend une chaîne vide et une teinte neutre sur une valeur absente', () => {
    expect(objectiveStatusLabel(null)).toBe('');
    expect(objectiveStatusLabel(undefined)).toBe('');
    expect(objectiveStatusTone(null)).toBe('neutral');
  });

  it('nomme les niveaux de déclenchement et les décisions', () => {
    expect(objectiveLevelLabel('WARNING')).toBe('Alerte');
    expect(objectiveLevelLabel('CRITICAL')).toBe('Critique');
    expect(proposalStatusLabel('PENDING')).toBe('À trancher');
    expect(proposalStatusLabel('ACCEPTED')).toBe('Acceptée');
    expect(proposalStatusLabel('REJECTED')).toBe('Rejetée');
  });

  /** Les quatre périodes admises, et elles seules : la table suit la contrainte SQL. */
  it('n\'offre que les périodes 1, 3, 6 et 12 mois', () => {
    expect(PERIODES.map(p => p.months)).toEqual([1, 3, 6, 12]);
  });

  it('nomme une période, et rend le nombre brut pour une valeur hors table', () => {
    expect(periodLabel(3)).toBe('3 mois');
    expect(periodLabel(7)).toBe('7');
    expect(periodLabel(null)).toBe('');
  });

  /**
   * Un code que l'écran ne connaît pas encore — une valeur ajoutée au serveur
   * avant le front — s'affiche tel quel plutôt que de laisser un trou.
   */
  it('affiche un code inconnu tel quel', () => {
    expect(objectiveStatusLabel('INEDIT' as never)).toBe('INEDIT');
    expect(objectiveStatusTone('INEDIT' as never)).toBe('neutral');
  });
});
