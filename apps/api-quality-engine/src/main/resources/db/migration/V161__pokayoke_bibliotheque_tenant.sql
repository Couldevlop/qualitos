-- La bibliothèque de détrompeurs devient celle de la plateforme, plus celle de
-- chaque client : une action d'amélioration qui a fait la preuve qu'elle empêche
-- l'erreur s'y inscrit, et se réutilise sur une autre ligne (§3.4).
--
-- `tenant_id` nul désigne le catalogue partagé, celui que la plateforme livre
-- (même convention que le Standards Hub) ; non nul, la bibliothèque d'un client.
-- Les dispositifs déjà en base appartiennent au catalogue : la colonne naît
-- nulle, et c'est exactement ce qu'il faut.
ALTER TABLE pokayoke_devices
    ADD COLUMN tenant_id UUID,
    ADD COLUMN source_reference VARCHAR(255);

COMMENT ON COLUMN pokayoke_devices.tenant_id IS
    'Client propriétaire, ou NULL pour le catalogue partagé de la plateforme';
COMMENT ON COLUMN pokayoke_devices.source_reference IS
    'D''où vient le dispositif, cité en clair (projet DMAIC et action) ; NULL au catalogue';

-- Un dispositif né d'une action cite son origine : sans elle, il n'est pas
-- réutilisable en confiance. Le catalogue partagé, lui, est sa propre origine.
ALTER TABLE pokayoke_devices
    ADD CONSTRAINT ck_pokayoke_devices_origine_citee
        CHECK (tenant_id IS NULL OR source_reference IS NOT NULL);

-- Toutes les lectures filtrent sur « partagé ou le mien » : l'index partiel
-- sert la part qui grandit, celle des bibliothèques des clients.
CREATE INDEX ix_pokayoke_devices_tenant
    ON pokayoke_devices (tenant_id)
    WHERE tenant_id IS NOT NULL;
