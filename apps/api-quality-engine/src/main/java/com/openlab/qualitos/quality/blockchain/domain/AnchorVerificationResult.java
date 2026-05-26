package com.openlab.qualitos.quality.blockchain.domain;

/**
 * Résultat de vérification d'intégrité d'un événement ancré (ADR 0012).
 *
 * <ul>
 *   <li>{@code VERIFIED} — l'événement appartient à un lot dont la racine Merkle
 *       recalculée correspond au reçu, et la signature hybride du reçu est valide.</li>
 *   <li>{@code TAMPERED} — incohérence : racine recalculée ≠ reçu, signature invalide,
 *       ou événement absent de son lot.</li>
 *   <li>{@code NOT_ANCHORED} — événement inconnu ou pas encore ancré.</li>
 * </ul>
 */
public record AnchorVerificationResult(Status status, String detail, String txRef, String merkleRoot) {

    public enum Status { VERIFIED, TAMPERED, NOT_ANCHORED }

    public static AnchorVerificationResult notAnchored(String detail) {
        return new AnchorVerificationResult(Status.NOT_ANCHORED, detail, null, null);
    }

    public static AnchorVerificationResult tampered(String detail, String txRef, String merkleRoot) {
        return new AnchorVerificationResult(Status.TAMPERED, detail, txRef, merkleRoot);
    }

    public static AnchorVerificationResult verified(String txRef, String merkleRoot) {
        return new AnchorVerificationResult(Status.VERIFIED,
                "Intégrité confirmée : racine Merkle et signature hybride valides.", txRef, merkleRoot);
    }
}
