import { MatrixCell, MatrixOperator, MatrixSkill } from './competency-matrix.types';

/**
 * Les règles de lecture de la matrice, en fonctions pures : niveaux,
 * couverture, moyennes, filtres. Le composant ne fait qu'afficher ce qu'elles
 * rendent ; la même règle que le serveur (`CompetencyMatrix.coverage`) s'applique
 * ici pour que la figure reste juste entre deux lectures, après chaque clic.
 */

export const MIN_LEVEL = 0;
export const MAX_LEVEL = 4;
/** À partir de ce niveau, une personne tient le poste seule. */
export const AUTONOMOUS_LEVEL = 2;
/** En deçà de ce nombre de personnes autonomes, la compétence est à risque. */
export const MIN_AUTONOMOUS = 2;

/** Les mots de l'échelle, dans l'ordre des niveaux. */
export const LEVEL_LABELS: readonly string[] = [
  $localize`:@@training.matrix.level-0:Non formé`,
  $localize`:@@training.matrix.level-1:Débutant`,
  $localize`:@@training.matrix.level-2:Autonome`,
  $localize`:@@training.matrix.level-3:Expert`,
  $localize`:@@training.matrix.level-4:Formateur`
];

export function levelLabel(level: number): string {
  return LEVEL_LABELS[level] ?? String(level);
}

/** Le niveau suivant au clic : 0 → 1 → 2 → 3 → 4 → 0. */
export function nextLevel(level: number): number {
  return level >= MAX_LEVEL ? MIN_LEVEL : level + 1;
}

/** Les niveaux posés, indexés par case ; une case absente vaut « non formé ». */
export class LevelIndex {

  private readonly levels = new Map<string, number>();

  constructor(cells: readonly MatrixCell[] = []) {
    cells.forEach(c => this.levels.set(LevelIndex.key(c.userId, c.skillId), c.level));
  }

  private static key(userId: string, skillId: string): string {
    return `${userId}|${skillId}`;
  }

  get(userId: string, skillId: string): number {
    return this.levels.get(LevelIndex.key(userId, skillId)) ?? MIN_LEVEL;
  }

  set(userId: string, skillId: string, level: number): void {
    this.levels.set(LevelIndex.key(userId, skillId), level);
  }
}

/** Les personnes autonomes sur une compétence, dans l'ordre des lignes. */
export function autonomousOn(index: LevelIndex, operators: readonly MatrixOperator[], skillId: string): MatrixOperator[] {
  return operators.filter(o => index.get(o.userId, skillId) >= AUTONOMOUS_LEVEL);
}

export function isAtRisk(index: LevelIndex, operators: readonly MatrixOperator[], skillId: string): boolean {
  return autonomousOn(index, operators, skillId).length < MIN_AUTONOMOUS;
}

/** Une personne au moins est « formateur » sur la compétence. */
export function hasTrainerLevel(index: LevelIndex, operators: readonly MatrixOperator[], skillId: string): boolean {
  return operators.some(o => index.get(o.userId, skillId) === MAX_LEVEL);
}

/** La moyenne d'une ligne sur les compétences affichées, à un chiffre après la virgule. */
export function operatorAverage(index: LevelIndex, userId: string, skills: readonly MatrixSkill[]): string {
  if (!skills.length) {
    return (0).toFixed(1);
  }
  const total = skills.reduce((sum, s) => sum + index.get(userId, s.skillId), 0);
  return (total / skills.length).toFixed(1);
}

/** Les catégories présentes, sans doublon, dans l'ordre alphabétique. */
export function categoriesOf(skills: readonly MatrixSkill[]): string[] {
  const set = new Set<string>();
  skills.forEach(s => { if (s.category) set.add(s.category); });
  return [...set].sort((a, b) => a.localeCompare(b, 'fr'));
}

/**
 * Les ateliers à proposer au filtre : ceux du client, puis ceux qu'un opérateur
 * porte encore après leur retrait de la liste — sans quoi il deviendrait
 * introuvable par le filtre.
 */
export function workshopFilterOptions(workshops: readonly string[], operators: readonly MatrixOperator[]): string[] {
  const out = [...workshops];
  operators.forEach(o => { if (!out.includes(o.workshop)) out.push(o.workshop); });
  return out;
}

/** Recherche sans égard aux accents ni à la casse. */
export function normalize(text: string): string {
  return text.normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase().trim();
}

export function filterOperators(operators: readonly MatrixOperator[], search: string, workshop: string): MatrixOperator[] {
  const q = normalize(search);
  return operators.filter(o => (!workshop || o.workshop === workshop) && (!q || normalize(o.name).includes(q)));
}

export function filterSkills(skills: readonly MatrixSkill[], category: string): MatrixSkill[] {
  return skills.filter(s => !category || s.category === category);
}
