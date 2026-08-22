import { SpringPage } from '../pdca/pdca.types';

export type CapaType = 'CORRECTIVE' | 'PREVENTIVE';
export type CapaCriticity = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
export type CapaStatus = 'OPEN' | 'IN_PROGRESS' | 'RESOLVED' | 'CLOSED' | 'REJECTED';
export type CapaSourceType = 'NON_CONFORMITY' | 'AUDIT' | 'COMPLAINT' | 'INTERNAL' | 'IOT_ALERT' | 'OTHER';

/**
 * Nature d'une action (§4.2, ISO 9001 §10.2, 8D étape D3).
 *
 * Elle sépare ce qui ARRÊTE L'EFFET de ce qui SUPPRIME LA CAUSE. Sans elle, un
 * dossier où l'on a trié le lot suspect se lit comme un dossier où l'on a corrigé
 * la machine : les deux affichent « toutes les actions faites », et le second
 * seul empêche la récidive.
 */
export type CapaActionType = 'CONTAINMENT' | 'CORRECTIVE' | 'PREVENTIVE';

export interface CapaActionResponse {
  id: string;
  capaId: string;
  title: string;
  status: 'PENDING' | 'IN_PROGRESS' | 'DONE';
  /** Corrective sur toutes les actions antérieures au type : elles ne pouvaient être autre chose. */
  actionType: CapaActionType;
  assigneeId?: string;
  /**
   * Nom lisible du porteur, figé à la décision (ADR 0052).
   *
   * Il double `assigneeId` sans le remplacer : l'identifiant rattache l'action à
   * un compte, le nom est ce qui se lit dans un dossier d'audit. Absent sur les
   * actions créées avant cette colonne — l'écran affiche « — » plutôt que de
   * retomber sur un UUID que personne ne reconnaît.
   */
  assigneeName?: string;
  /**
   * Jour où l'action a été DÉCIDÉE, distinct de sa date de saisie. Absent sur
   * les actions antérieures : recopier leur date de création fabriquerait une
   * décision que l'organisation n'a jamais enregistrée.
   */
  decidedOn?: string;
  dueDate?: string;
  completedAt?: string;
}

/** Écart d'origine du dossier, réduit à ce qu'un tableau doit en montrer. */
export interface LinkedNonConformity {
  id: string;
  reference: string;
  title: string;
}

export interface CapaCaseResponse {
  id: string;
  tenantId: string;
  title: string;
  description?: string;
  type: CapaType;
  criticity: CapaCriticity;
  status: CapaStatus;
  sourceType: CapaSourceType;
  sourceRef?: string;
  ownerId: string;
  rootCauseId?: string;
  dueDate?: string;
  resolvedAt?: string;
  closedAt?: string;
  effectivenessVerified?: boolean;
  createdAt: string;
  updatedAt: string;
  actions: CapaActionResponse[];
  /**
   * Non-conformité dont procède le dossier, donc dont procèdent ses actions.
   * Portée par le dossier et non répétée sur chaque action : la répéter
   * laisserait croire qu'elle peut différer d'une ligne à l'autre. Absente sur
   * la liste paginée, qui ne la résout pas (une requête par ligne pour une
   * colonne qu'elle n'affiche pas).
   */
  sourceNonConformity?: LinkedNonConformity;
  /**
   * Ce qui s'oppose encore à la clôture, énoncé AVANT le clic.
   *
   * Absent sur la liste paginée (non calculé). Sur la fiche il est toujours
   * présent : un tableau VIDE dit « rien ne s'y oppose », ce qui est une
   * information — d'où la distinction entre `undefined` et `[]`.
   */
  closureBlockers?: ClosureBlocker[];
}

/** Motif de refus de clôture : un code et un décompte, jamais une phrase serveur. */
export interface ClosureBlocker {
  code: ClosureBlockerCode;
  /** Nombre d'éléments concernés — actions restantes, écarts ouverts… */
  count: number;
}

export type ClosureBlockerCode =
  | 'NO_ACTION'
  | 'ACTIONS_NOT_DONE'
  | 'CONTAINMENT_ONLY'
  | 'OPEN_NON_CONFORMITIES';

export type CapaPage = SpringPage<CapaCaseResponse>;

export interface CreateCapaCaseRequest {
  title: string;
  description?: string;
  type: CapaType;
  criticity: CapaCriticity;
  sourceType: CapaSourceType;
  sourceRef?: string;
  ownerId: string;
  dueDate?: string;
}

export type CapaActionStatus = 'PENDING' | 'IN_PROGRESS' | 'DONE';

export interface CreateCapaActionRequest {
  title: string;
  description?: string;
  status?: CapaActionStatus;
  /** Absent = corrective, le cas de loin le plus fréquent. */
  actionType?: CapaActionType;
  assigneeId?: string;
  assigneeName?: string;
  /** Jour de la décision ; déduit du jour de l'enregistrement s'il est omis. */
  decidedOn?: string;
  dueDate?: string;
}

/**
 * Mise à jour partielle d'une action (§4.2).
 *
 * Tous les champs sont facultatifs : c'est un PATCH, où l'absence signifie « ne
 * touche pas ». L'édition en ligne du tableau n'envoie donc que le libellé et le
 * statut, sans risquer d'effacer la date de décision ou le porteur au passage.
 */
export interface UpdateCapaActionRequest {
  title?: string;
  status?: CapaActionStatus;
  actionType?: CapaActionType;
  description?: string;
  assigneeId?: string;
  assigneeName?: string;
  decidedOn?: string;
  dueDate?: string;
}

/** Action corrective/préventive suggérée par l'IA (à valider/ajouter). §4.2 */
export interface SuggestedAction {
  title: string;
  description?: string;
}

/**
 * Pièce jointe apportée en preuve au dossier (§4.2, ISO 9001 §10.2).
 *
 * La preuve se rattache au dossier et non à l'action : c'est le niveau où elle a
 * valeur d'audit. `url` est une adresse de lecture à durée de vie courte, signée
 * par le stockage — elle n'est pas stable et ne doit pas être conservée.
 */
export interface CapaEvidence {
  id: string;
  capaId: string;
  /**
   * Action visée par la pièce, absente quand elle vaut pour le dossier entier
   * (ADR 0050 puis 0052). C'est ce champ qui range la pièce dans la colonne
   * « Preuve » de la bonne ligne du tableau.
   */
  actionId?: string;
  contentType: string;
  sizeBytes: number;
  originalFilename?: string;
  /** Auteur du dépôt ; absent si le jeton ne portait pas d'identifiant exploitable. */
  uploadedBy?: string;
  createdAt: string;
  url?: string;
}

export interface UpdateCapaCaseRequest {
  title?: string;
  description?: string;
  criticity?: CapaCriticity;
  sourceRef?: string;
  rootCauseId?: string;
  dueDate?: string;
}

/** Ce que le terrain dit d'une CAPA close, par opposition a ce qu'on avait declare. */
export type MeasurementStatus = 'NOT_MEASURABLE' | 'IN_OBSERVATION' | 'MEASURED';

export interface CapaEffectivenessRow {
  capaId: string;
  title: string;
  criticity?: string;
  closedAt: string;
  status: MeasurementStatus;
  occurrencesBefore: number;
  occurrencesAfter: number;
  /** Absent quand le statut interdit de conclure. */
  ratePercent?: number;
  aggravated: boolean;
  daysObserved: number;
  daysInWindow: number;
  /** Ce que le responsable avait coche a la cloture. L'ecart avec la mesure est l'information. */
  declaredEffective?: boolean;
  /** Faux quand la recidive se devine a la seule categorie : le taux est alors indicatif. */
  preciseMatch: boolean;
}

export interface CapaEffectivenessSummary {
  windowMonths: number;
  measured: number;
  inObservation: number;
  notMeasurable: number;
  averageRatePercent?: number;
  aggravated: number;
  declaredButFailed: number;
  /** Le perimetre depassait la borne de lecture : les plus anciens sont ecartes. */
  truncated: boolean;
  rows: CapaEffectivenessRow[];
}
