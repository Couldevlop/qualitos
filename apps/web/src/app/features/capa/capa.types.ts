import { SpringPage } from '../pdca/pdca.types';

/**
 * Raison d'ouverture du dossier. `CONTAINMENT` dit qu'on a d'abord protégé —
 * lot bloqué, ligne arrêtée, client prévenu — sans encore rien avoir corrigé.
 * Le confondre avec un dossier correctif laisse croire que le problème ne peut
 * plus revenir (ISO 9001 §10.2, 8D étape D3).
 */
export type CapaType = 'CONTAINMENT' | 'CORRECTIVE' | 'PREVENTIVE';
export type CapaCriticity = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
export type CapaStatus = 'OPEN' | 'IN_PROGRESS' | 'RESOLVED' | 'CLOSED' | 'REJECTED';
// RISK : dossier ouvert depuis une fiche du registre des risques (sourceRef = R-xxx).
// SPC_ALERT et ANOMALY : dossiers ouverts par un automate (carte de contrôle, détection d'anomalie).
export type CapaSourceType = 'NON_CONFORMITY' | 'AUDIT' | 'COMPLAINT' | 'INTERNAL' | 'IOT_ALERT'
  | 'SPC_ALERT' | 'ANOMALY' | 'RISK' | 'OTHER';

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
  /**
   * Libellé du pilote, recopié de l'annuaire à sa désignation (ADR 0088).
   * Absent sur les dossiers antérieurs ou ouverts par un automate : l'écran
   * le résout alors par l'annuaire, ou affiche « — » — jamais un identifiant.
   */
  ownerName?: string;
  rootCauseId?: string;
  dueDate?: string;
  resolvedAt?: string;
  closedAt?: string;
  effectivenessVerified?: boolean;
  effectivenessVerifiedAt?: string;
  /**
   * Le critère d'efficacité, posé AVANT le jugement (ADR 0108).
   *
   * Il se modifie tant qu'aucune évaluation n'a été rendue, et plus après :
   * les verdicts passés ont été rendus contre lui, et le changer réécrirait
   * après coup la question à laquelle on a répondu.
   */
  effectivenessCriterion?: string;
  /**
   * Le dernier verdict rendu, ou absent si le dossier n'a jamais été évalué.
   *
   * Lu depuis l'historique côté serveur, et seulement sur la fiche détaillée :
   * la liste paginée ne le porte pas, parce qu'elle ne l'affiche pas.
   */
  effectivenessVerdict?: EffectivenessVerdict;
  /**
   * Une vérification d'efficacité est-elle EXIGÉE ?
   *
   * À distinguer de `effectivenessVerified`, qui dit si elle a eu lieu.
   * `undefined` veut dire « la question n'a pas été tranchée » — ce n'est pas
   * la même chose que `false`, qui est une décision.
   */
  verificationRequired?: boolean;
  verificationAssigneeId?: string;
  /** Recopié depuis l'annuaire : un compte désactivé n'efface pas le nom. */
  verificationAssigneeName?: string;
  verificationInstructions?: string;
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

/**
 * Ce que remplit la fenêtre de création CAPA, d'où qu'on l'ouvre (ADR 0088) :
 * le dossier ET son action unique. Le responsable est un compte de l'annuaire,
 * qui pilote le dossier.
 *
 * `sourceType` et `sourceRef` ne servent qu'à la création libre depuis la
 * liste : ouverte depuis une NC, un constat ou un risque, c'est le serveur qui
 * fixe la source.
 */
export interface OpenCapaRequest {
  title: string;
  description?: string;
  /** Corrective ou préventive : un dossier qui ne ferait qu'endiguer ne se clôturerait jamais. */
  type: Exclude<CapaType, 'CONTAINMENT'>;
  /**
   * La nature de l'ACTION du dossier, quand elle diffère de celle du dossier :
   * une mesure d'endiguement se tient dans un dossier correctif, qui restera
   * clôturable quand la cause sera traitée (ISO 9001 §10.2, 8D étape D3).
   * Absente, elle se déduit du type du dossier.
   */
  actionType?: CapaActionType;
  criticity: CapaCriticity;
  sourceType?: CapaSourceType;
  sourceRef?: string;
  ownerId: string;
  ownerName?: string;
  dueDate: string;
  /**
   * La décision de vérification d'efficacité, prise à l'ouverture (ADR 0073,
   * 0088). Absente : question laissée ouverte ; `false` est une décision.
   */
  verificationRequired?: boolean;
  /** Le compte de connexion du vérificateur — seulement si la vérification est exigée. */
  verificationAssigneeId?: string;
  /** Son nom d'annuaire, recopié dans le dossier. */
  verificationAssigneeName?: string;
  verificationInstructions?: string;
  /**
   * Le critère d'efficacité, posé dès l'ouverture : la phrase qui dira à quoi
   * on reconnaîtra que l'action a produit son effet. Posé ici, il précède la
   * mesure ; posé le jour du jugement, il serait choisi en regardant le
   * résultat (ADR 0108).
   */
  effectivenessCriterion?: string;
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
  /**
   * `undefined` laisse la décision en l'état, `false` la prend explicitement.
   * Le serveur applique la règle : exiger sans désigner est refusé (422), et
   * ne plus exiger efface le vérificateur et les consignes.
   */
  verificationRequired?: boolean;
  verificationAssigneeId?: string;
  verificationAssigneeName?: string;
  verificationInstructions?: string;
  /**
   * Le critère d'efficacité. Paramétré par l'admin du client ou posé par le
   * manager à l'ouverture ; refusé par le serveur dès qu'un verdict existe.
   */
  effectivenessCriterion?: string;
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

// ---- Évaluation de l'efficacité (ADR 0108) ----------------------------------

/**
 * Le verdict d'une évaluation d'efficacité.
 *
 * Trois états et non un booléen : `PARTIAL` est l'état qui manquait. Une action
 * qui a réduit le problème sans le supprimer n'est ni efficace ni inefficace, et
 * forcer le choix obligeait à mentir dans un sens ou dans l'autre — puis à clore
 * un dossier qui méritait une action de plus, ou à rouvrir un dossier qui
 * progressait.
 */
export type EffectivenessVerdict = 'EFFECTIVE' | 'PARTIAL' | 'INEFFECTIVE';

/**
 * Où en est un dossier dans la boucle d'évaluation.
 *
 * CALCULÉE par le serveur à chaque lecture, jamais stockée : une colonne serait
 * plus rapide et fausse dès la première action terminée par un autre chemin.
 * L'écran ne la recalcule donc pas non plus.
 */
export type EvaluationStage =
  | 'ACTIONS_IN_PROGRESS'
  | 'AWAITING_EVALUATION'
  | 'CLOSED_EFFECTIVE'
  | 'COMPLEMENTARY_ACTION_EXPECTED'
  | 'REOPENED';

/** Une évaluation rendue : le jugement, le critère contre lequel il l'a été, et sa mesure. */
export interface EffectivenessReview {
  id: string;
  verdict: EffectivenessVerdict;
  /** Recopié au moment du jugement : modifier le critère du dossier ne le réécrit pas. */
  criterion?: string;
  baseline?: string;
  target?: string;
  observed?: string;
  comment?: string;
  /** Recopié depuis l'annuaire : un compte désactivé n'efface pas le nom (jamais un UUID). */
  reviewerName?: string;
  reviewedAt: string;
}

/** Une ligne du plan de travail des évaluations. */
export interface EvaluationRow {
  capaId: string;
  /** La référence de l'écart d'origine, telle que le dossier la porte. */
  reference?: string;
  title: string;
  criticity?: string;
  ownerName?: string;
  stage: EvaluationStage;
  /**
   * Le panneau d'évaluation est-il ouvert sur cette ligne ?
   *
   * Donné par le serveur et non déduit de `stage` : la règle appartient au
   * domaine, et la refaire ici en ferait une deuxième, qui divergerait.
   */
  evaluable: boolean;
  actionsTotal: number;
  actionsDone: number;
  criterion?: string;
  lastReview?: EffectivenessReview;
  reviewCount: number;
  /** Quelque chose s'oppose à la clôture : les deux champs suivants disent quoi. */
  closureBlocked: boolean;
  containmentOnly: boolean;
  openNcCount: number;
  updatedAt?: string;
}

/**
 * Le plan de travail complet : quatre chiffres, puis les lignes.
 *
 * Les chiffres portent sur l'ENSEMBLE du périmètre, les lignes sont bornées.
 * `truncated` le dit, pour qu'un tableau coupé ne se lise pas comme un total.
 */
export interface EvaluationBoard {
  /** Absent — et non zéro — quand rien n'a encore été évalué. */
  effectiveRatePercent?: number;
  evaluated: number;
  awaitingEvaluation: number;
  stillOpen: number;
  reopened: number;
  partial: number;
  /** Dossiers encore ouverts sans critère posé : à poser avant de pouvoir juger. */
  missingCriterion: number;
  total: number;
  truncated: boolean;
  rows: EvaluationRow[];
}

/**
 * Ce qu'on envoie pour rendre un verdict.
 *
 * `observed` est obligatoire : un verdict sans mesure n'est pas un jugement.
 * L'ancien champ booléen `effective` existe encore côté serveur pour les
 * intégrations, mais l'écran n'envoie jamais que cette forme-ci.
 */
export interface RecordEffectivenessRequest {
  verdict: EffectivenessVerdict;
  baseline?: string;
  target?: string;
  observed: string;
  comment?: string;
}
