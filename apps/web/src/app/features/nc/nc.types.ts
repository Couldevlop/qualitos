import { TeamMemberRef } from '../../shared/user-picker/user-team-picker.component';
import { OpenCapaRequest } from '../capa/capa.types';
import { SpringPage } from '../pdca/pdca.types';

/**
 * Borne haute d'une équipe (miroir de `NcDto.MAX_TEAM_MEMBERS`) : au-delà d'une
 * dizaine de personnes, ce n'est plus une équipe de résolution mais une liste de
 * diffusion, et le D1 du rapport 8D devient illisible.
 */
export const NC_MAX_TEAM_MEMBERS = 12;

export type { TeamMemberRef };

export type NcCategory =
  | 'PRODUCT'
  | 'PROCESS'
  | 'DOCUMENTATION'
  | 'SUPPLIER'
  | 'SAFETY'
  | 'ENVIRONMENT'
  | 'OTHER';

export type NcSeverity = 'MINOR' | 'MAJOR' | 'CRITICAL';

/**
 * Une NATURE d'écart, sous sa catégorie : « Cote hors spécification » sous
 * Produit, « Étalonnage hors validité » sous Processus.
 *
 * <p>C'est une donnée du client, pas une énumération du code : les natures
 * d'écart d'une usine ne sont pas celles d'un CHU, et la plateforme livre
 * seulement un catalogue de départ transverse (§18.2 #9).
 */
export interface NcSubcategory {
  code: string;
  category: NcCategory;
  label: string;
}

/**
 * Les natures d'une même famille, rangées sous elle.
 *
 * <p>Rendu en {@code mat-optgroup} par les deux écrans qui font choisir une
 * nature — le filtre du registre et la fenêtre de création. Soixante natures à
 * plat ne se parcourent pas : les titres de famille disent où l'on pioche.
 */
export interface NcSubcategoryGroup {
  category: NcCategory;
  subcategories: NcSubcategory[];
}

/** Le catalogue en vigueur et son origine. */
export interface NcSubcategoryCatalogue {
  entries: NcSubcategory[];
  /** Vrai quand ce sont les valeurs de la plateforme, faute de réglage du client. */
  defaults: boolean;
}

/**
 * D'où vient la non-conformité (§4.3). Une NC détectée par l'organisation
 * elle-même et une NC signalée du dehors — client, fournisseur, autorité,
 * certificateur — n'ont ni les mêmes délais ni les mêmes destinataires.
 */
export type NcOrigin = 'INTERNAL' | 'EXTERNAL';

export type NcStatus =
  | 'OPEN'
  | 'UNDER_ANALYSIS'
  | 'ACTION_DEFINED'
  | 'RESOLVED'
  | 'CLOSED'
  | 'CANCELLED'
  /**
   * Le constat a été instruit et écarté — réservé aux écarts signalés du dehors.
   *
   * <p>Distinct de `CANCELLED` : annuler dit « ce constat n'avait pas lieu
   * d'être », rejeter dit « il a été examiné, voici pourquoi il n'est pas
   * retenu ». C'est la seconde phrase qu'un client vient lire.
   */
  | 'REJECTED';

export interface NcResponse {
  id: string;
  tenantId?: string;
  reference: string;
  title: string;
  description?: string;
  category: NcCategory;
  /**
   * La nature fine de l'écart, par son code de catalogue. Absente sur les NC
   * saisies avant ce classement, ou que personne n'a encore classées.
   */
  subcategoryCode?: string;
  severity: NcSeverity;
  status: NcStatus;
  origin: NcOrigin;
  detectedAt: string;
  zone?: string;
  geoLat?: number;
  geoLng?: number;
  /** URLs des photos, une par ligne (chaîne unique côté backend). */
  photoUrls?: string;
  reporterId?: string;
  /**
   * Nom de la personne qui a signalé l'écart, figé au signalement par le
   * serveur. Absent sur les NC antérieures à cette donnée : la liste affiche
   * alors « — » plutôt qu'un identifiant qui n'apprend rien.
   */
  reporterName?: string;
  /** Produit concerné. Absent pour une NC de service ou administrative. */
  productId?: string;
  /**
   * Mode de défaillance du PFMEA que ce défaut illustre. Son absence est une
   * information en soi : un défaut que l'analyse n'avait pas prévu.
   */
  fmeaItemId?: string;
  capaCaseId?: string;
  /** Le cycle PDCA ouvert depuis cet ecart, s'il l'a ete. */
  pdcaCycleId?: string;
  /** Le projet DMAIC ouvert depuis cet ecart, s'il l'a ete. */
  dmaicProjectId?: string;
  rootCause?: string;
  resolutionNote?: string;
  /** Pourquoi la réclamation a été écartée. */
  rejectionReason?: string;
  rejectedAt?: string;
  resolvedAt?: string;
  closedAt?: string;
  createdAt: string;
  updatedAt: string;
  /**
   * L'équipe qui prend l'écart en charge, dans l'ordre où elle a été nommée —
   * le premier est le pilote. Vide sur les écarts saisis avant cette donnée, et
   * sur ceux qu'une machine a ouverts (constat d'audit, connecteur EHR) : c'est
   * à l'humain qui reprend la fiche de nommer l'équipe.
   */
  teamMembers?: TeamMemberRef[];
  /** true = saisie hors-ligne, en attente de synchronisation (réponse optimiste). */
  pendingSync?: boolean;
}

export type NcPage = SpringPage<NcResponse>;

/**
 * Photo binaire attachée à une NC (stockage objet, URL présignée 15 min).
 * Distincte des `photoUrls` texte (saisie libre / legacy / fallback offline).
 */
export interface NcPhoto {
  id: string;
  /** URL présignée d'accès (valide ~15 min) renvoyée par GET. */
  url?: string;
  /** Clé objet en stockage (présente à la création). */
  objectKey?: string;
  contentType: string;
  sizeBytes: number;
  originalFilename: string;
  createdAt: string;
}

export interface CreateNcRequest {
  title: string;
  description?: string;
  category: NcCategory;
  /** La nature fine, par son code de catalogue. Facultative : on classe finement après. */
  subcategoryCode?: string;
  severity: NcSeverity;
  detectedAt?: string;
  zone?: string;
  geoLat?: number;
  geoLng?: number;
  /** URLs des photos, une par ligne. */
  photoUrls?: string;
  reporterId?: string;
  /** Interne par défaut côté serveur si non précisée. */
  origin?: NcOrigin;
  /** Produit concerné, s'il y en a un. */
  productId?: string;
  /** Mode de défaillance du PFMEA que ce défaut illustre, confirmé par l'utilisateur. */
  fmeaItemId?: string;
  /**
   * L'équipe qui prend l'écart en charge (D1 du 8D), au moins une personne, dans
   * l'ordre de saisie : le premier nommé pilote. Le serveur refuse une liste
   * vide — nommer l'équipe à l'ouverture évite de la retaper dans le rapport.
   */
  teamMembers: TeamMemberRef[];
}

export interface UpdateNcRequest {
  title?: string;
  description?: string;
  origin?: NcOrigin;
  category?: NcCategory;
  /** La nature fine. Chaîne vide = effacer le classement fin ; absente = inchangée. */
  subcategoryCode?: string;
  severity?: NcSeverity;
  zone?: string;
  geoLat?: number;
  geoLng?: number;
  photoUrls?: string;
  /**
   * L'équipe en charge, remplacée en bloc. Absente = inchangée ; jamais vide —
   * on ne retire pas la dernière personne d'un écart ouvert.
   */
  teamMembers?: TeamMemberRef[];
}

export interface ResolveNcRequest {
  resolutionNote: string;
}

/** Le rejet d'une réclamation externe : le motif est obligatoire. */
export interface RejectNcRequest {
  reason: string;
}

// --- Analyse Vision 5S par IA (§3.2 / §1.4) ----------------------------------
// Surface la détection CV des non-conformités sur photo. Contrat backend figé :
// POST /api/v1/vision/5s/analyze (multipart, champ 'image'), réponse camelCase.

/** Pilier 5S porté par un finding de vision (enum backend, affiché brut/i18n). */
export type VisionPillar =
  | 'SEIRI'
  | 'SEITON'
  | 'SEISO'
  | 'SEIKETSU'
  | 'SHITSUKE';

/** Sévérité d'un finding vision (enum backend). */
export type VisionSeverity = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';

/** Scores 5S calculés par le modèle (0-100 par pilier + global). */
export interface VisionScore {
  seiri: number;
  seiton: number;
  seiso: number;
  seiketsu: number;
  shitsuke: number;
  overall: number;
}

/** Non-conformité 5S détectée sur la photo. */
export interface VisionFinding {
  pillar: VisionPillar;
  description: string;
  /** Chaîne libre côté backend (LOW/MEDIUM/HIGH/CRITICAL le plus souvent). */
  severity: string;
  /** Confiance du modèle, 0.0 → 1.0. */
  confidence: number;
  /** Boîte englobante [x, y, w, h] en pixels, ou null si non localisée. */
  bbox: number[] | null;
}

/** Réponse complète de l'analyse vision 5S. */
export interface VisionAnalysis {
  imageSha256: string;
  width: number;
  height: number;
  score: VisionScore;
  findings: VisionFinding[];
}

export interface StartAnalysisNcRequest {
  rootCause?: string;
}

/**
 * Ce que la fenêtre de création CAPA commune envoie pour ouvrir la CAPA d'une
 * NC (ADR 0088) : le dossier et son action unique. La source est la NC — le
 * serveur la fixe, elle n'est pas lue dans le corps.
 */
export type EscalateCapaNcRequest = OpenCapaRequest;

/** Les deux methodes par lesquelles un ecart se traite (CLAUDE.md 3.6). */
export type NcMethod = 'PDCA' | 'DMAIC';

/**
 * Ouvrir, depuis un ecart, le cycle ou le projet qui le traitera.
 *
 * <p>`method` absente : la gravite decide, cote serveur. `pilotId` ne l'est
 * jamais -- un dossier dont personne ne repond ne se revoit pas. `dueDate` ne
 * concerne que le projet DMAIC ; un cycle PDCA n'en porte pas.
 */
export interface ConvertNcRequest {
  method?: NcMethod;
  pilotId: string;
  dueDate?: string | null;
}

/**
 * Dénombrements par statut, affichés en tuiles au-dessus d'une liste de NC.
 *
 * <p>`origin` rappelle SUR QUOI portent ces nombres : la même liste existe en
 * interne, en externe et toutes origines confondues. Un total sans son
 * périmètre se lit de travers.
 *
 * <p>`cancelled` n'a de sens que sur les écarts signalés du dehors — on
 * n'annule pas un constat qu'on a fait soi-même, on le résout ou on le clôt.
 */
export interface NcStatistics {
  tenantId: string;
  origin: NcOrigin | null;
  total: number;
  open: number;
  underAnalysis: number;
  actionDefined: number;
  resolved: number;
  closed: number;
  cancelled: number;
  /** Rejetées — toujours 0 sur l'écran interne, qui ne rejette pas. */
  rejected: number;
}

// ---- Rapport 8D (extrait à la clôture d'une non-conformité) -----------------

/** Brouillon tant qu'il se remanie ; émis, il est scellé et ne change plus. */
export type EightDStatus = 'DRAFT' | 'ISSUED';

/**
 * Une des huit disciplines.
 *
 * `sourced` à false veut dire « rien à montrer », et `sourceLabel` dit alors
 * POURQUOI : c'est ce qui distingue un 8D d'un formulaire aux cases vides.
 * `editable` n'est vrai que pour D1, D3 et D8, les trois qui n'ont aucune source
 * dans la plateforme.
 */
export interface EightDDiscipline {
  code: string;
  title: string;
  sourced: boolean;
  sourceLabel: string;
  lines: string[];
  editable: boolean;
}

/** La preuve d'intégrité d'un rapport émis. */
export interface EightDSeal {
  sha256Hex: string;
  anchorTxRef: string;
  verificationCode: string;
  issuedAt: string;
  issuedByName: string | null;
}

export interface EightDReport {
  ncId: string;
  ncReference: string;
  ncTitle: string;
  status: EightDStatus;
  /** Vrai quand la NC est clôturée et que le rapport n'est pas encore émis. */
  issuable: boolean;
  /** Vrai dès qu'une discipline n'a rien à montrer. */
  partial: boolean;
  missingCodes: string[];
  team: string | null;
  containment: string | null;
  recognition: string | null;
  disciplines: EightDDiscipline[];
  seal: EightDSeal | null;
}

/** Les trois disciplines saisies. Toutes facultatives : un rapport partiel s'émet. */
export interface SaveEightDRequest {
  team?: string | null;
  containment?: string | null;
  recognition?: string | null;
}
