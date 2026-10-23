/**
 * La matrice de compétences éditable (ADR 0098), telle que
 * `/api/v1/training/competencies/matrix` la rend.
 *
 * Les personnes (opérateurs, formateurs) sont des comptes de l'annuaire
 * (`userId` = `sub` du jeton) : l'écran n'affiche jamais que leur nom.
 */

/** L'échelle de l'atelier : 0 Non formé · 1 Débutant · 2 Autonome · 3 Expert · 4 Formateur. */
export type SkillLevel = 0 | 1 | 2 | 3 | 4;

export interface MatrixOperator {
  /** L'inscription dans la matrice (pas la personne). */
  id: string;
  userId: string;
  name: string;
  workshop: string;
}

export interface MatrixSkill {
  skillId: string;
  name: string;
  category: string | null;
  trainerUserId: string;
  trainerName: string;
  /** Personnes autonomes (niveau 2 ou plus) au moment de la lecture. */
  autonomous: number;
  atRisk: boolean;
}

export interface MatrixCell {
  userId: string;
  skillId: string;
  level: number;
}

export interface CompetencyMatrixView {
  workshops: string[];
  /** Vrai quand ce sont les ateliers de la plateforme, faute de réglage. */
  defaultWorkshops: boolean;
  operators: MatrixOperator[];
  skills: MatrixSkill[];
  /** Seules les évaluations posées voyagent : une case absente vaut « non formé ». */
  cells: MatrixCell[];
}

export interface AddOperatorRequest {
  userId: string;
  userName: string;
  workshop: string;
}

export interface AddSkillRequest {
  name: string;
  category: string;
  trainerUserId: string;
  trainerName: string;
}

export interface SetLevelRequest {
  userId: string;
  skillId: string;
  level: number;
}

export interface WorkshopsView {
  names: string[];
  defaults: boolean;
}
