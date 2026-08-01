/**
 * Connecteurs tiers — ERP / EHR / Communication (CLAUDE.md §13.3).
 *
 * Les trois familles sont exposées par trois contrôleurs distincts
 * (`/api/v1/erp`, `/api/v1/ehr`, `/api/v1/comm`) mais partagent la même
 * mécanique : une connexion nommée, un fournisseur, un secret chiffré côté
 * serveur, un cycle de vie et un compteur d'échecs consécutifs. On modélise
 * donc le socle commun une seule fois pour que l'écran puisse traiter les trois
 * onglets avec le même code de présentation.
 */

/** Page Spring renvoyée par les trois listes de connexions. */
export interface ConnectorPage<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

/**
 * Plafond de pagination du serveur (`spring.data.web.pageable.max-page-size: 100`).
 *
 * Au-delà, Spring rabote la taille SANS erreur : proposer 200 rendrait la moitié
 * des connexions inatteignable en donnant l'illusion qu'elles n'existent pas.
 */
export const CONNECTOR_MAX_PAGE_SIZE = 100;

/**
 * Cycle de vie commun aux trois familles (le serveur réutilise l'énumération du
 * module ITSM pour EHR et Communication, et une énumération identique pour l'ERP).
 * `DISABLED_ON_ERRORS` n'est jamais posé par l'utilisateur : c'est la désactivation
 * automatique après N échecs consécutifs.
 */
export type ConnectorStatus = 'ACTIVE' | 'DISABLED' | 'DISABLED_ON_ERRORS';

/** Statuts qu'un administrateur a le droit de poser lui-même. */
export type ConnectorEditableStatus = Extract<ConnectorStatus, 'ACTIVE' | 'DISABLED'>;

/**
 * Socle commun à toute connexion, quel que soit le contrôleur d'origine.
 * Permet à l'écran de compter, colorer et trier les trois onglets sans dupliquer
 * la logique de présentation.
 */
export interface ConnectorRow {
  id: string;
  name: string;
  status: ConnectorStatus;
  consecutiveFailures: number;
  lastSuccessAt: string | null;
  createdBy: string;
  createdAt: string | null;
  updatedAt: string | null;
}

// ---------------------------------------------------------------------------
// ERP — SAP / Oracle Fusion / Dynamics (§13.3 « indicateurs production, achats,
// fournisseurs »).
// ---------------------------------------------------------------------------

export type ErpProvider = 'SAP' | 'ORACLE_FUSION' | 'DYNAMICS';

/** Aucun champ sensible : la réponse serveur n'expose ni secret ni ciphertext. */
export interface ErpConnection extends ConnectorRow {
  tenantId: string;
  provider: ErpProvider;
  baseUrl: string;
  username: string | null;
  externalScope: string | null;
  lastSyncAt: string | null;
}

export interface ErpCreateRequest {
  name: string;
  provider: ErpProvider;
  /** https:// obligatoire (motif serveur `^https://.+`). */
  baseUrl: string;
  username?: string;
  /** 4 à 1024 caractères ; chiffré au repos, jamais retourné par l'API. */
  secret: string;
  externalScope?: string;
  createdBy: string;
}

export interface ErpUpdateRequest {
  name?: string;
  baseUrl?: string;
  username?: string;
  /** Absent = secret conservé ; présent = rotation. */
  secret?: string;
  externalScope?: string;
  status?: ConnectorEditableStatus;
}

/** Compte rendu de synchronisation ERP : le serveur ne propage pas l'erreur aval. */
export interface ErpSyncReport {
  connectionId: string;
  suppliersImported: number;
  suppliersIgnored: number;
  kpisImported: number;
  kpisIgnored: number;
  ranAt: string;
  errorMessage: string | null;
}

// ---------------------------------------------------------------------------
// EHR — HL7 FHIR R4/R5 (§13.3 « EHR/HIS »).
// ---------------------------------------------------------------------------

export type EhrProvider = 'FHIR_R4' | 'FHIR_R5';

export type EhrAuthMode = 'BASIC' | 'BEARER';

export interface EhrConnection extends ConnectorRow {
  tenantId: string;
  provider: EhrProvider;
  fhirBaseUrl: string;
  authMode: EhrAuthMode;
  username: string | null;
  resourceCategory: string | null;
  lastSyncAt: string | null;
}

export interface EhrCreateRequest {
  name: string;
  provider: EhrProvider;
  fhirBaseUrl: string;
  authMode: EhrAuthMode;
  username?: string;
  secret: string;
  resourceCategory?: string;
  createdBy: string;
}

export interface EhrUpdateRequest {
  name?: string;
  fhirBaseUrl?: string;
  authMode?: EhrAuthMode;
  username?: string;
  secret?: string;
  resourceCategory?: string;
  status?: ConnectorEditableStatus;
}

/** Compte rendu d'import FHIR — jamais de donnée patient, uniquement des compteurs. */
export interface EhrSyncReport {
  connectionId: string;
  totalFetched: number;
  created: number;
  skipped: number;
  errors: number;
  ranAt: string;
  errorMessage: string | null;
}

// ---------------------------------------------------------------------------
// Communication — Teams / Slack / Mattermost (§13.3 « Slack, Teams, Mattermost »).
// ---------------------------------------------------------------------------

export type CommProvider = 'TEAMS' | 'SLACK' | 'MATTERMOST';

/**
 * L'URL d'incoming-webhook EST le secret (elle porte un jeton non devinable) :
 * le serveur ne la renvoie jamais. La connexion n'expose donc que le salon.
 */
export interface CommConnection extends ConnectorRow {
  tenantId: string;
  provider: CommProvider;
  channel: string | null;
  lastNotifiedAt: string | null;
}

export interface CommCreateRequest {
  name: string;
  provider: CommProvider;
  /** 8 à 1024 caractères, https:// obligatoire — c'est le secret. */
  webhookUrl: string;
  channel?: string;
  createdBy: string;
}

export interface CommUpdateRequest {
  name?: string;
  /** Absent = URL conservée ; présent = rotation du webhook. */
  webhookUrl?: string;
  channel?: string;
  status?: ConnectorEditableStatus;
}

/** Résultat d'un envoi de test réel vers le salon configuré. */
export interface CommTestResult {
  connectionId: string;
  success: boolean;
  errorMessage: string | null;
}
