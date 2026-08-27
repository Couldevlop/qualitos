-- Preuves jointes aux étapes d'un cycle PDCA (§3.1, ADR 0061).
--
-- Une étape déclarée faite sans document ne prouve rien : elle affirme. La
-- preuve de la mise en place d'une action est toujours un document — relevé
-- signé, procédure approuvée, constat photographique — et le tableau des étapes
-- n'en portait aucun : l'auditeur devait croire sur parole, ou aller chercher la
-- pièce dans un partage réseau.
--
-- La preuve se rattache à l'ÉTAPE et non au cycle, à la différence des preuves
-- CAPA : c'est l'étape qui se prouve, ligne à ligne dans le tableau ; le cycle,
-- lui, ne se prouve que par la somme de ses étapes.
--
-- Seules les métadonnées vivent ici. Le binaire est dans le stockage objet, sous
-- une clé tenantisée — même partage que les photos de non-conformité et les
-- preuves CAPA.
CREATE TABLE pdca_step_evidences (
    id                UUID         NOT NULL DEFAULT gen_random_uuid(),
    -- Redondant avec pdca_cycles.tenant_id, et délibérément : toute lecture
    -- filtre alors sur un seul index sans jointure, et une requête qui
    -- oublierait le filtre de cycle resterait enfermée dans son tenant.
    tenant_id         UUID         NOT NULL,
    cycle_id          UUID         NOT NULL,
    step_id           UUID         NOT NULL,
    -- Clé d'objet : tenants/{tenant}/pdca/{cycle}/steps/{step}/{uuid}.{ext}.
    -- L'extension vient du type MIME validé, jamais du nom de fichier client.
    object_key        VARCHAR(512) NOT NULL,
    content_type      VARCHAR(150) NOT NULL,
    size_bytes        BIGINT       NOT NULL,
    original_filename VARCHAR(255),
    -- Une preuve anonyme se défend mal : la question « qui l'a produite ? »
    -- vient toujours. Nullable car un sujet de jeton non-UUID ne doit pas
    -- fabriquer un auteur faux.
    uploaded_by       UUID,
    created_at        TIMESTAMPTZ  NOT NULL,

    CONSTRAINT pk_pdca_step_evidences PRIMARY KEY (id),
    CONSTRAINT fk_pdca_step_evidences_cycle FOREIGN KEY (cycle_id)
        REFERENCES pdca_cycles (id) ON DELETE CASCADE,
    -- L'étape peut disparaître avant le cycle (suppression d'une étape hors
    -- phase active) : sa preuve doit partir avec elle, sinon la ligne survivrait
    -- en désignant une étape qui n'existe plus.
    CONSTRAINT fk_pdca_step_evidences_step FOREIGN KEY (step_id)
        REFERENCES pdca_steps (id) ON DELETE CASCADE,
    -- Une même clé d'objet ne peut désigner deux lignes : sinon la suppression
    -- de l'une effacerait le binaire de l'autre.
    CONSTRAINT uq_pdca_step_evidences_object_key UNIQUE (object_key),
    -- Une pièce par étape, garanti en base et pas seulement par le service :
    -- deux dépôts concurrents passeraient tous deux le comptage applicatif, et
    -- la cellule du tableau deviendrait indécidable.
    CONSTRAINT uq_pdca_step_evidences_step UNIQUE (step_id),
    CONSTRAINT ck_pdca_step_evidences_size CHECK (size_bytes > 0)
);

-- Lecture systématique par cycle, dans l'ordre du versement : le tableau
-- récupère toutes les pièces d'un coup puis les range par étape.
CREATE INDEX ix_pdca_step_evidences_tenant_cycle
    ON pdca_step_evidences (tenant_id, cycle_id, created_at);
