-- Le circuit de validation s'active ou se suspend (ADR 0095).
--
-- Jusqu'ici, revenir à l'approbation simple obligeait à effacer les étapes.
-- Le client peut désormais suspendre son circuit sans le perdre : les étapes
-- restent réglées, mais aucune soumission nouvelle n'y entre. Sans ligne ici,
-- le circuit est actif, comme avant.
--
-- L'isolation par client se pose d'elle-même sur la colonne tenant_id (ADR 0086).

CREATE TABLE circuit_settings (
    tenant_id   UUID          NOT NULL,
    subject     VARCHAR(40)   NOT NULL,
    enabled     BOOLEAN       NOT NULL DEFAULT TRUE,
    updated_by  UUID,
    updated_at  TIMESTAMPTZ   NOT NULL DEFAULT now(),
    PRIMARY KEY (tenant_id, subject)
);
