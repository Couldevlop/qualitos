-- §8.11 Marketplace de packs normatifs — installations par tenant.
-- Un tenant (Admin Tenant) installe un pack PUBLIÉ. Multi-tenant strict : tenant_id
-- vient du JWT. L'historique est préservé (jamais de suppression physique).

CREATE TABLE marketplace_installations (
    id                   UUID         NOT NULL,
    tenant_id            UUID         NOT NULL,
    marketplace_pack_id  UUID         NOT NULL,
    pack_id              VARCHAR(64)  NOT NULL,
    pack_version         VARCHAR(32)  NOT NULL,
    status               VARCHAR(16)  NOT NULL,
    installed_by         UUID         NOT NULL,
    installed_at         TIMESTAMPTZ  NOT NULL,
    uninstalled_by       UUID,
    uninstalled_at       TIMESTAMPTZ,
    CONSTRAINT pk_marketplace_installations PRIMARY KEY (id),
    CONSTRAINT fk_mpi_pack FOREIGN KEY (marketplace_pack_id)
        REFERENCES marketplace_packs (id),
    CONSTRAINT chk_mpi_status CHECK (status IN ('INSTALLED','UNINSTALLED'))
);

CREATE INDEX idx_mpi_tenant      ON marketplace_installations (tenant_id);
CREATE INDEX idx_mpi_tenant_pack ON marketplace_installations (tenant_id, marketplace_pack_id);

-- Une seule installation ACTIVE (INSTALLED) par (tenant, pack) ; historique préservé.
CREATE UNIQUE INDEX uk_mpi_active_per_tenant_pack
    ON marketplace_installations (tenant_id, marketplace_pack_id)
    WHERE status = 'INSTALLED';

COMMENT ON TABLE marketplace_installations IS
    'Installations de packs marketplace par tenant (§8.11). tenant_id issu du JWT, historique conservé (audit ISO 19011).';
