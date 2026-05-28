package com.openlab.qualitos.blockchain.anchor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** DTO de la passerelle d'ancrage. Seuls des hashes/identifiants transitent (RGPD §11.3). */
public final class AnchorDto {

    private AnchorDto() {}

    /**
     * Requête d'ancrage. {@code timestamp} (RFC3339) et {@code eventCount} optionnels :
     * le port {@code BlockchainAnchorPort} de l'engine ne transmet que tenant + root
     * (ADR 0012, port inchangé) ; la passerelle complète l'horodatage si absent.
     */
    public record AnchorRequest(
            @NotBlank @Size(max = 64) String tenantId,
            @NotBlank @Size(max = 128) String merkleRoot,
            String timestamp,
            Integer eventCount
    ) {}

    public record AnchorResponse(String txId, String record) {}

    public record VerifyResponse(String record) {}
}
