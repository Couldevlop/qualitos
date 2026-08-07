-- Preuves jointes à un dossier CAPA (§4.2, ISO 9001 §10.2).
--
-- Une CAPA se clôt sur une vérification d'efficacité, et l'efficacité se prouve.
-- Le dossier ne portait aucune pièce : l'auditeur devait croire sur parole que
-- l'action avait produit son effet, ou aller chercher la preuve dans un partage
-- réseau. La preuve se rattache au DOSSIER et non à l'action : c'est le niveau
-- où elle a valeur d'audit.
--
-- Seules les métadonnées vivent ici. Le binaire est dans le stockage objet, sous
-- une clé tenantisée — même partage que les photos de non-conformité.
CREATE TABLE capa_evidences (
    id                UUID         NOT NULL DEFAULT gen_random_uuid(),
    tenant_id         UUID         NOT NULL,
    capa_id           UUID         NOT NULL,
    -- Clé d'objet : tenants/{tenant}/capa/{capa}/{uuid}.{ext}. L'extension vient
    -- du type MIME validé, jamais du nom de fichier client.
    object_key        VARCHAR(512) NOT NULL,
    content_type      VARCHAR(150) NOT NULL,
    size_bytes        BIGINT       NOT NULL,
    original_filename VARCHAR(255),
    -- Une preuve anonyme se défend mal : la question « qui l'a produite ? » vient
    -- toujours. Nullable car un sujet de jeton non-UUID ne doit pas fabriquer un
    -- auteur faux.
    uploaded_by       UUID,
    created_at        TIMESTAMPTZ  NOT NULL,

    CONSTRAINT pk_capa_evidences PRIMARY KEY (id),
    CONSTRAINT fk_capa_evidences_case FOREIGN KEY (capa_id)
        REFERENCES capa_cases (id) ON DELETE CASCADE,
    -- Une même clé d'objet ne peut désigner deux lignes : sinon la suppression de
    -- l'une effacerait le binaire de l'autre.
    CONSTRAINT uq_capa_evidences_object_key UNIQUE (object_key),
    CONSTRAINT ck_capa_evidences_size CHECK (size_bytes > 0)
);

-- Lecture systématique par dossier, dans l'ordre du versement.
CREATE INDEX ix_capa_evidences_tenant_capa
    ON capa_evidences (tenant_id, capa_id, created_at);
