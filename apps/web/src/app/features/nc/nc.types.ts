import { SpringPage } from '../pdca/pdca.types';

export type NcCategory =
  | 'PRODUCT'
  | 'PROCESS'
  | 'DOCUMENTATION'
  | 'SUPPLIER'
  | 'SAFETY'
  | 'ENVIRONMENT'
  | 'OTHER';

export type NcSeverity = 'MINOR' | 'MAJOR' | 'CRITICAL';

export type NcStatus =
  | 'OPEN'
  | 'UNDER_ANALYSIS'
  | 'ACTION_DEFINED'
  | 'RESOLVED'
  | 'CLOSED'
  | 'CANCELLED';

export interface NcResponse {
  id: string;
  tenantId?: string;
  reference: string;
  title: string;
  description?: string;
  category: NcCategory;
  severity: NcSeverity;
  status: NcStatus;
  detectedAt: string;
  zone?: string;
  geoLat?: number;
  geoLng?: number;
  /** URLs des photos, une par ligne (chaîne unique côté backend). */
  photoUrls?: string;
  reporterId?: string;
  capaCaseId?: string;
  rootCause?: string;
  resolutionNote?: string;
  resolvedAt?: string;
  closedAt?: string;
  createdAt: string;
  updatedAt: string;
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
  severity: NcSeverity;
  detectedAt?: string;
  zone?: string;
  geoLat?: number;
  geoLng?: number;
  /** URLs des photos, une par ligne. */
  photoUrls?: string;
  reporterId?: string;
}

export interface UpdateNcRequest {
  title?: string;
  description?: string;
  category?: NcCategory;
  severity?: NcSeverity;
  zone?: string;
  geoLat?: number;
  geoLng?: number;
  photoUrls?: string;
}

export interface ResolveNcRequest {
  resolutionNote: string;
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

export interface EscalateCapaNcRequest {
  ownerId: string;
}
