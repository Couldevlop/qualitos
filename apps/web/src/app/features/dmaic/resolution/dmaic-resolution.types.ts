/**
 * La fiche de résolution d'un projet DMAIC (ADR 0101), telle que
 * `/api/v1/dmaic/projects/{id}/resolution` la rend et la reçoit : le classeur
 * client retranscrit — en-tête de réclamation, questionnaire par phase,
 * synthèses, 5 pourquoi, plan d'actions, photos.
 */

export type SheetSection = 'DEFINE' | 'MEASURE' | 'CONTAIN' | 'ANALYZE' | 'IMPROVE' | 'CONTROL';
export type DefineTopic = 'WHAT' | 'WHO' | 'WHY' | 'WHERE' | 'WHEN' | 'HOW_MANY';
export type AnswerType = 'TEXT' | 'YES_NO' | 'YES_NO_NA' | 'YES_NO_UNKNOWN';
/** Ce qu'enregistre une liste fermée : un code, que l'écran traduit. */
export type AnswerCode = 'YES' | 'NO' | 'NA' | 'UNKNOWN';
export type WhyBranch = 'OCCURRENCE' | 'DETECTION' | 'SYSTEMIC';
export type ActionStatus = 'OPEN' | 'IN_PROGRESS' | 'IN_REVIEW' | 'CLOSED';

export const SHEET_SECTIONS: readonly SheetSection[] = ['DEFINE', 'MEASURE', 'CONTAIN', 'ANALYZE', 'IMPROVE', 'CONTROL'];
export const DEFINE_TOPICS: readonly DefineTopic[] = ['WHAT', 'WHO', 'WHY', 'WHERE', 'WHEN', 'HOW_MANY'];
export const ANSWER_TYPES: readonly AnswerType[] = ['TEXT', 'YES_NO', 'YES_NO_NA', 'YES_NO_UNKNOWN'];
export const WHY_BRANCHES: readonly WhyBranch[] = ['OCCURRENCE', 'DETECTION', 'SYSTEMIC'];
export const ACTION_STATUSES: readonly ActionStatus[] = ['OPEN', 'IN_PROGRESS', 'IN_REVIEW', 'CLOSED'];

/** Les réponses qu'admet chaque forme de question, dans l'ordre du classeur. */
export const ANSWER_CHOICES: Readonly<Record<AnswerType, readonly AnswerCode[]>> = {
  TEXT: [],
  YES_NO: ['YES', 'NO'],
  YES_NO_NA: ['YES', 'NO', 'NA'],
  YES_NO_UNKNOWN: ['YES', 'NO', 'UNKNOWN']
};

/** Les bornes du serveur, pour arrêter une saisie avant l'aller-retour. */
export const RESOLUTION_LIMITS = {
  short: 200,
  claim: 60,
  summary: 4000,
  answer: 2000,
  why: 500,
  whysPerChain: 5,
  chains: 30,
  actions: 100,
  actionText: 1000,
  team: 15,
  remark: 1000,
  photoSlots: 6,
  photoBytes: 10 * 1024 * 1024,
  questions: 150,
  questionLabel: 500,
  failureTypes: 300,
  failureLabel: 200,
  severities: 10,
  severityLabel: 80
} as const;

export const PHOTO_TYPES: readonly string[] = ['image/jpeg', 'image/png', 'image/gif', 'image/webp'];

/** Une personne de l'annuaire : son compte, et son nom recopié pour l'affichage et le rapport. */
export interface Person {
  accountId: string;
  name: string;
}

export interface SheetQuestion {
  code: string;
  section: SheetSection;
  topic: DefineTopic | null;
  label: string;
  answerType: AnswerType;
}

export interface FailureType {
  code: string;
  label: string;
}

export interface SeverityLevel {
  level: number;
  label: string;
}

export interface ResolutionSettings {
  questions: SheetQuestion[];
  failureTypes: FailureType[];
  severities: SeverityLevel[];
  /** Vrai quand ce sont les valeurs de la plateforme, faute de réglage du client. */
  defaults: boolean;
}

export interface WhyChain {
  branch: WhyBranch;
  cause: string | null;
  whys: string[];
  rootCause: boolean;
  action: string | null;
}

export interface ActionLine {
  createdOn: string | null;
  action: string | null;
  responsible: Person | null;
  status: ActionStatus;
  plannedOn: string | null;
  doneOn: string | null;
  percent: number;
  notes: string | null;
}

export interface ResolutionContent {
  clientName: string | null;
  claimNumber: string | null;
  problemOwner: Person | null;
  team: Person[];
  openedOn: string | null;
  closedOn: string | null;
  severity: number | null;
  failureTypeCode: string | null;
  customerSymptoms: string | null;
  answers: Record<string, string>;
  measureSummary: string | null;
  containmentMeasures: string | null;
  rootCause: string | null;
  retainedSolutions: string | null;
  surveillancePlan: string | null;
  whys: WhyChain[];
  actions: ActionLine[];
  ishikawaId: string | null;
}

export interface SheetPhoto {
  slot: number;
  contentType: string;
  sizeBytes: number;
  remark: string | null;
  /** Adresse de lecture à durée de vie courte : ne se garde pas. */
  url: string;
}

export interface ResolutionSheet {
  projectId: string;
  projectTitle: string;
  problemStatement: string | null;
  /** Faux tant que personne n'a enregistré la fiche : rien n'existe encore en base. */
  saved: boolean;
  content: ResolutionContent;
  settings: ResolutionSettings;
  photos: SheetPhoto[];
  updatedAt: string | null;
}

export function emptyContent(): ResolutionContent {
  return {
    clientName: null, claimNumber: null, problemOwner: null, team: [], openedOn: null, closedOn: null,
    severity: null, failureTypeCode: null, customerSymptoms: null, answers: {}, measureSummary: null,
    containmentMeasures: null, rootCause: null, retainedSolutions: null, surveillancePlan: null,
    whys: [], actions: [], ishikawaId: null
  };
}
