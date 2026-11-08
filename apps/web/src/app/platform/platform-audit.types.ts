/**
 * La piste d'audit de la PLATEFORME — contrat de `/api/v1/platform-audit`.
 *
 * <p>Ne pas confondre avec `features/admin-audit-log`, qui est la piste d'audit
 * d'UN client (`/api/v1/audit/events`, moteur qualité). Celle-ci consigne les
 * actes de l'éditeur sur les dossiers de TOUS les clients : création d'un
 * client, ouverture d'un module, changement de tarif, accès accordé à un
 * cabinet. C'est la piste qu'une autorité demande.
 *
 * <p>Journal en AJOUT SEUL côté serveur, et jusqu'au contrat : aucun type
 * d'écriture, de modification ou de suppression n'existe ici — le serveur
 * n'expose aucune de ces opérations, délibérément.
 */

/** Qui a agi : une personne, ou la machine. Pas d'acteur extérieur côté plateforme. */
export type PlatformActorType = 'USER' | 'SYSTEM' | 'SCHEDULER';

/**
 * Le vocabulaire fermé des actes consignés.
 *
 * <p>Fermé côté serveur aussi (une énumération Java) : un acte hors de cette
 * liste est refusé en 400 plutôt qu'accepté puis ignoré.
 */
export type PlatformAuditAction =
  | 'TENANT_CREATED'
  | 'TENANT_UPDATED'
  | 'TENANT_DEACTIVATED'
  | 'MODULE_SUBSCRIBED'
  | 'MODULE_CANCELLED'
  | 'MODULE_PRICE_SET'
  | 'PARTNER_FIRM_CREATED'
  | 'PARTNER_FIRM_UPDATED'
  | 'PARTNER_FIRM_ACTIVE_CHANGED'
  | 'PARTNER_CONSULTANT_ADDED'
  | 'PARTNER_CONSULTANT_ACTIVE_CHANGED'
  | 'PARTNER_ENGAGEMENT_PROPOSED'
  | 'PARTNER_ENGAGEMENT_GRANTED'
  | 'PARTNER_ENGAGEMENT_DECLINED'
  | 'PARTNER_ENGAGEMENT_REVOKED'
  | 'AUDIT_EXPORTED';

/** Un acte consigné, tel que `PlatformAuditDto.Event` le rend. */
export interface PlatformAuditEvent {
  id: string;
  /** Numéro strictement croissant pour TOUTE la plateforme : c'est la clé du chaînage. */
  sequenceNo: number;
  occurredAt: string;
  recordedAt: string;
  actorType: PlatformActorType;
  actorUserId: string | null;
  action: PlatformAuditAction;
  targetType: string;
  targetId: string | null;
  /** Le client concerné, quand il y en a un. Un tarif du catalogue n'en vise aucun. */
  targetTenantId: string | null;
  summary: string;
  payloadJson: string | null;
  ipAddress: string | null;
  userAgent: string | null;
  integrityHash: string;
  /** Empreinte de l'acte précédent ; `null` sur le tout premier. */
  previousHash: string | null;
}

/** Enveloppe `Page<T>` de Spring Data. */
export interface PlatformAuditPage {
  content: PlatformAuditEvent[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

/** Une rupture : acte retouché, chaînage rompu, trou de séquence, acte manquant. */
export interface PlatformAuditBreak {
  /** `null` quand la rupture est l'ABSENCE d'un acte : il n'y a pas d'id à citer. */
  eventId: string | null;
  sequenceNo: number;
  reason: string;
}

/**
 * Le verdict sur une fenêtre.
 *
 * <p>`intact` et non `valid` : le mot est fort exprès côté serveur. On vérifie
 * que le journal n'a pas été RETOUCHÉ, pas qu'il est complet — un acte jamais
 * consigné ne laisse aucun vide.
 */
export interface PlatformAuditVerification {
  fromSeq: number;
  toSeq: number;
  examined: number;
  intact: boolean;
  breaks: PlatformAuditBreak[];
}

/** Le dossier remis à une autorité. */
export interface PlatformAuditExport {
  generatedAt: string;
  fromSeq: number;
  toSeq: number;
  count: number;
  /** Empreinte de l'extrait pris comme un tout : il atteste l'intégrité, pas l'origine. */
  seal: string;
  verification: PlatformAuditVerification;
  events: PlatformAuditEvent[];
}

/**
 * Les critères acceptés par `GET /api/v1/platform-audit`.
 *
 * <p>Ils NE SE COMBINENT PAS : le serveur applique un seul critère, dans
 * l'ordre client → acte → auteur → période. L'écran doit le dire plutôt que de
 * laisser déduire un ET inexistant de ses résultats.
 */
export interface PlatformAuditFilter {
  action?: PlatformAuditAction;
  targetTenantId?: string;
  actorUserId?: string;
  from?: string;
  to?: string;
}

/** Le critère réellement retenu par le serveur, pour pouvoir l'annoncer. */
export type PlatformAuditCriterion = 'tenant' | 'action' | 'actor' | 'period' | 'none';
