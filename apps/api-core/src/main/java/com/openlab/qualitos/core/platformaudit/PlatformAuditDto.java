package com.openlab.qualitos.core.platformaudit;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Ce que la piste d'audit rend au-dehors.
 *
 * <p>Les empreintes sont rendues, y compris celle de l'acte précédent : sans
 * elles, l'extrait ne serait pas vérifiable par son destinataire, qui devrait
 * nous croire sur parole. C'est l'inverse de ce qu'on attend d'une piste
 * d'audit.
 *
 * <p>L'acteur est rendu sous la forme de son identifiant de compte. La
 * plateforme le présente par son NOM côté interface, en le résolvant dans
 * l'annuaire — le journal, lui, garde l'identifiant, parce qu'un nom change
 * et qu'un acte consigné ne doit pas changer avec lui.
 */
public final class PlatformAuditDto {

    private PlatformAuditDto() {
    }

    /** Un acte consigné. */
    public record Event(
            UUID id,
            long sequenceNo,
            Instant occurredAt,
            Instant recordedAt,
            PlatformActorType actorType,
            UUID actorUserId,
            PlatformAuditAction action,
            String targetType,
            UUID targetId,
            UUID targetTenantId,
            String summary,
            String payloadJson,
            String ipAddress,
            String userAgent,
            String integrityHash,
            String previousHash) {

        public static Event from(PlatformAuditEvent source) {
            return new Event(
                    source.getId(),
                    source.getSequenceNo(),
                    source.getOccurredAt(),
                    source.getRecordedAt(),
                    source.getActorType(),
                    source.getActorUserId(),
                    source.getAction(),
                    source.getTargetType(),
                    source.getTargetId(),
                    source.getTargetTenantId(),
                    source.getSummary(),
                    source.getPayloadJson(),
                    source.getIpAddress(),
                    source.getUserAgent(),
                    source.getIntegrityHash(),
                    source.getPreviousHash());
        }
    }

    /**
     * Une rupture de chaîne.
     *
     * @param eventId     l'acte fautif, ou {@code null} quand la rupture est
     *                    l'ABSENCE d'un acte — il n'y a alors aucun
     *                    identifiant à montrer
     * @param sequenceNo  le numéro concerné
     * @param reason      ce qui ne concorde pas, en clair
     */
    public record Break(UUID eventId, long sequenceNo, String reason) {
    }

    /**
     * Le verdict sur une fenêtre.
     *
     * @param intact {@code true} seulement si AUCUNE rupture n'a été trouvée.
     *               Le mot est fort exprès : « valide » laisserait croire que
     *               le journal est complet, alors qu'on ne vérifie ici que
     *               qu'il n'a pas été retouché.
     */
    public record Verification(
            long fromSeq,
            long toSeq,
            int examined,
            boolean intact,
            List<Break> breaks) {
    }

    /**
     * Le dossier remis à une autorité.
     *
     * @param seal l'empreinte de l'extrait pris comme un tout — voir
     *             {@link PlatformAuditService#export(long, long)} pour ce
     *             qu'elle prouve et ce qu'elle ne prouve pas
     */
    public record Export(
            Instant generatedAt,
            long fromSeq,
            long toSeq,
            int count,
            String seal,
            Verification verification,
            List<Event> events) {
    }
}
