package com.openlab.qualitos.quality.erpconnector;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public final class ErpDto {

    private ErpDto() {}

    public record CreateConnectionRequest(
            @NotBlank @Size(max = 120) String name,
            @NotNull ErpProvider provider,
            @NotBlank @Size(max = 512)
            @Pattern(regexp = "^https://.+", message = "baseUrl must be https://")
            String baseUrl,
            @Size(max = 200) String username,
            @NotBlank @Size(min = 4, max = 1024) String secret,
            @Size(max = 200) String externalScope,
            @NotNull UUID createdBy
    ) {}

    public record UpdateConnectionRequest(
            @Size(max = 120) String name,
            @Size(max = 512)
            @Pattern(regexp = "^https://.+", message = "baseUrl must be https://")
            String baseUrl,
            @Size(max = 200) String username,
            @Size(min = 4, max = 1024) String secret,
            @Size(max = 200) String externalScope,
            ErpConnectionStatus status
    ) {}

    /** Réponse connexion : N'EXPOSE AUCUN champ sensible (pas de secret/ciphertext). */
    public record ConnectionResponse(
            UUID id,
            UUID tenantId,
            String name,
            ErpProvider provider,
            String baseUrl,
            String username,
            String externalScope,
            ErpConnectionStatus status,
            int consecutiveFailures,
            Instant lastSyncAt,
            Instant lastSuccessAt,
            UUID createdBy,
            Instant createdAt,
            Instant updatedAt
    ) {}

    /**
     * Rapport de synchronisation ERP. Comme l'ITSM, on renvoie un compte rendu plutôt
     * que de propager les erreurs aval (la connexion reste pilotable).
     *
     * @param suppliersImported  fournisseurs créés ou mis à jour (upsert)
     * @param suppliersIgnored   fournisseurs ignorés (code manquant)
     * @param kpisImported       mesures KPI écrites (upsert par (kpi, période))
     * @param kpisIgnored        mesures ignorées (code KPI inconnu → WARN, pas de création sauvage)
     * @param errorMessage       null si succès ; sinon cause (auth, réseau, déchiffrement…)
     */
    public record SyncReport(
            UUID connectionId,
            int suppliersImported,
            int suppliersIgnored,
            int kpisImported,
            int kpisIgnored,
            Instant ranAt,
            String errorMessage
    ) {}
}
