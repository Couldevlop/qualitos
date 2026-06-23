import { SpringPage } from '../pdca/pdca.types';

export type FiveSAuditStatus = 'DRAFT' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED';
export type FiveSPillar = 'SEIRI' | 'SEITON' | 'SEISO' | 'SEIKETSU' | 'SHITSUKE';

export interface FiveSItemResponse {
  id: string;
  auditId: string;
  pillar: FiveSPillar;
  score: number;
  note?: string;
  photoUrl?: string;
  /** true = saisi hors-ligne, en attente de synchronisation (réponse optimiste). */
  pendingSync?: boolean;
}

export interface FiveSAuditResponse {
  id: string;
  tenantId: string;
  zone: string;
  description?: string;
  status: FiveSAuditStatus;
  auditorId: string;
  scheduledAt?: string;
  completedAt?: string;
  overallScore?: number;
  createdAt: string;
  updatedAt: string;
  items: FiveSItemResponse[];
  /** true = créé hors-ligne, en attente de synchronisation (réponse optimiste). */
  pendingSync?: boolean;
}

export type FiveSPage = SpringPage<FiveSAuditResponse>;

export interface CreateFiveSAuditRequest {
  zone: string;
  description?: string;
  auditorId: string;
  scheduledAt?: string;
}

export interface ScorePillarRequest {
  pillar: FiveSPillar;
  score: number;     // 0..10
  note?: string;
  photoUrl?: string;
}

export interface UpdateFiveSAuditRequest {
  zone?: string;
  description?: string;
  scheduledAt?: string;
}

// ---- Analyse Vision CV (YOLOv8) d'une photo de zone (§3.2) -------------------

/** Scores 5S détectés par le modèle (0-100 par pilier + global). */
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
  pillar: FiveSPillar;
  description: string;
  severity: string;          // LOW | MEDIUM | HIGH | CRITICAL (chaîne libre backend)
  confidence: number;        // 0.0 → 1.0
  bbox: number[] | null;     // [x, y, w, h] en pixels, ou null
}

/** Réponse complète de l'analyse vision 5S d'un audit. */
export interface VisionAnalysis {
  imageSha256: string;
  width: number;
  height: number;
  score: VisionScore;
  findings: VisionFinding[];
}
