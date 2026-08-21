import { SpringPage } from '../pdca/pdca.types';

export type TrainingPathStatus = 'DRAFT' | 'ACTIVE' | 'ARCHIVED';
export type EnrollmentStatus   = 'ENROLLED' | 'IN_PROGRESS' | 'COMPLETED' | 'FAILED' | 'CANCELLED';

export type CompetencyLevelName = 'NONE' | 'AWARE' | 'PRACTITIONER' | 'COMPETENT' | 'EXPERT';
export type CompetencySource    = 'SELF' | 'MANAGER' | 'TRAINING' | 'CERTIFICATION' | 'AUDIT';

// --- Skills ---

export interface SkillResponse {
  id: string;
  tenantId: string;
  code: string;
  name: string;
  description?: string;
  category?: string;
  createdAt: string;
  updatedAt: string;
}

export type SkillPage = SpringPage<SkillResponse>;

export interface CreateSkillRequest {
  code: string;
  name: string;
  description?: string;
  category?: string;
}

export interface UpdateSkillRequest {
  name?: string;
  description?: string;
  category?: string;
}

// --- Competencies ---

export interface AssessCompetencyRequest {
  userId: string;
  skillId: string;
  level: number;                // 0-4
  source: CompetencySource;
  assessedBy?: string;
  expiresOn?: string;
}

export interface CompetencyResponse {
  id: string;
  tenantId: string;
  userId: string;
  skillId: string;
  level: number;
  levelName: CompetencyLevelName;
  source: CompetencySource;
  assessedBy?: string;
  assessedAt: string;
  expiresOn?: string;
  expired: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface CompetencyMatrix {
  userId: string;
  competencies: CompetencyResponse[];
}

export interface SkillGap {
  skillId: string;
  skillCode: string;
  currentLevel: number;
  targetLevel: number;
  gap: number;
}

export interface RoleGapAnalysis {
  userId: string;
  pathId: string;
  pathCode: string;
  totalRequirements: number;
  satisfied: number;
  gaps: SkillGap[];
}

// --- Training paths ---

export interface PathResponse {
  id: string;
  tenantId: string;
  code: string;
  name: string;
  description?: string;
  targetRole?: string;
  durationHours: number;
  passingScore: number;
  validityMonths?: number;
  status: TrainingPathStatus;
  createdBy: string;
  createdAt: string;
  updatedAt: string;
}

export type PathPage = SpringPage<PathResponse>;

export interface CreatePathRequest {
  code: string;
  name: string;
  description?: string;
  targetRole?: string;
  durationHours: number;
  passingScore?: number;
  validityMonths?: number;
  createdBy: string;
}

export interface UpdatePathRequest {
  name?: string;
  description?: string;
  targetRole?: string;
  durationHours?: number;
  passingScore?: number;
  validityMonths?: number;
}

export interface AttachSkillRequirementRequest {
  skillId: string;
  targetLevel: number;
}

export interface SkillRequirementResponse {
  id: string;
  pathId: string;
  skillId: string;
  targetLevel: number;
  createdAt: string;
}

// --- Enrollments ---

export interface EnrollmentResponse {
  id: string;
  tenantId: string;
  userId: string;
  pathId: string;
  status: EnrollmentStatus;
  progressPct: number;
  finalScore?: number;
  enrolledOn: string;
  startedOn?: string;
  completedOn?: string;
  expiresOn?: string;
  certificateCode?: string;
  createdAt: string;
  updatedAt: string;
}

export type EnrollmentPage = SpringPage<EnrollmentResponse>;

export interface EnrollRequest { userId: string; pathId: string; }
export interface ProgressUpdateRequest { progressPct: number; }
export interface CompleteRequest { finalScore: number; }

// --- Gamification (CLAUDE.md §19.3 — Yellow → Black Belt) ---

export type BeltLevel = 'WHITE' | 'YELLOW' | 'GREEN' | 'BLACK';

export type Badge =
  | 'FIRST_STEPS'
  | 'DEDICATED_LEARNER'
  | 'QUALITY_CHAMPION'
  | 'PERFECTIONIST'
  | 'YELLOW_BELT'
  | 'GREEN_BELT'
  | 'BLACK_BELT';

export interface LearnerProgressResponse {
  userId: string;
  tenantId: string;
  points: number;
  completedCount: number;
  bestScore?: number;
  beltLevel: BeltLevel;
  pointsToNextBelt: number;
  badges: Badge[];
  createdAt: string;
  updatedAt: string;
}

export interface CompleteLearningRequest {
  itemCode: string;
  score: number;
}

/**
 * La matrice du tenant : competences en lignes groupees, collaborateurs en
 * colonnes, un niveau a l'intersection.
 */
export interface CompetencyGridPerson {
  userId: string;
  /** Le nom si on le connait, sinon l'identifiant abrege. */
  label: string;
}

export interface CompetencyGridRow {
  skillId: string;
  code: string;
  name: string;
  /**
   * Niveaux alignes sur l'ordre des colonnes. `null` la ou la personne n'a
   * jamais ete evaluee : a ne pas confondre avec le niveau zero, qui est un
   * constat.
   */
  levels: (number | null)[];
  holders: number;
  /** Une seule personne detient la competence : un risque d'organisation. */
  singlePointOfKnowledge: boolean;
}

export interface CompetencyGridGroup {
  /** Famille de competences ; absente pour les competences non classees. */
  category?: string;
  rows: CompetencyGridRow[];
}

export interface CompetencyGrid {
  people: CompetencyGridPerson[];
  groups: CompetencyGridGroup[];
}
