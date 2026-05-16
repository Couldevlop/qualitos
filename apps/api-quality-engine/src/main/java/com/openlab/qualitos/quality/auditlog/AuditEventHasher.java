package com.openlab.qualitos.quality.auditlog;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.UUID;

/**
 * Hash chain SHA-256 sur la représentation canonique d'un événement + hash du
 * précédent. Implémentation pure, sans dépendances Spring, pour tests isolés.
 *
 * Canonical form : suite de champs séparés par '' (record separator) afin
 * d'éviter les collisions par injection de caractères dans des champs texte.
 */
public final class AuditEventHasher {

    private static final char SEP = '';

    private AuditEventHasher() {}

    public static String hash(AuditEvent event) {
        StringBuilder sb = new StringBuilder(512);
        sb.append(safe(event.getTenantId())).append(SEP);
        sb.append(event.getSequenceNo()).append(SEP);
        sb.append(safe(event.getOccurredAt())).append(SEP);
        sb.append(safe(event.getActorType())).append(SEP);
        sb.append(safe(event.getActorUserId())).append(SEP);
        sb.append(safe(event.getAction())).append(SEP);
        sb.append(safe(event.getResourceType())).append(SEP);
        sb.append(safe(event.getResourceId())).append(SEP);
        sb.append(safe(event.getSummary())).append(SEP);
        sb.append(safe(event.getPayloadJson())).append(SEP);
        sb.append(safe(event.getPreviousHash()));
        return sha256(sb.toString());
    }

    static String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    private static String safe(UUID v) { return v == null ? "" : v.toString(); }
    private static String safe(Instant v) { return v == null ? "" : v.toString(); }
    private static String safe(Enum<?> v) { return v == null ? "" : v.name(); }
    private static String safe(String v) { return v == null ? "" : v; }
}
