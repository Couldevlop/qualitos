-- Le FMEA se rattache enfin à un produit. Nullable : les FMEA système, service et
-- bow-tie existants n'en ont pas, et les rendre obligatoires casserait les données
-- en place pour un gain nul.
ALTER TABLE fmea_projects ADD COLUMN product_id UUID
    REFERENCES products (id) ON DELETE SET NULL;

-- UN PFMEA en vigueur par produit. L'index est partiel parce que les révisions
-- passées (ARCHIVED) et les brouillons en préparation (DRAFT) ne doivent pas
-- entrer en collision avec celui qui est réellement applicable.
CREATE UNIQUE INDEX uk_pfmea_active_per_product
    ON fmea_projects (tenant_id, product_id)
    WHERE product_id IS NOT NULL
      AND type = 'PROCESS_FMEA'
      AND status = 'ACTIVE';

CREATE INDEX idx_fmea_projects_product ON fmea_projects (tenant_id, product_id);

-- L'opération de gamme : le mot commun entre le PFMEA et le control plan.
ALTER TABLE fmea_items ADD COLUMN operation_id UUID
    REFERENCES product_operations (id) ON DELETE SET NULL;

-- La classification des caractéristiques spéciales (sécurité, réglementaire) est
-- exigée par l'IATF et pilote l'obligation de contrôle dans le control plan.
ALTER TABLE fmea_items ADD COLUMN characteristic_class VARCHAR(20)
    NOT NULL DEFAULT 'STANDARD';

-- L'AP est stockée pour permettre le tri « top priorités » sans recalcul.
ALTER TABLE fmea_items ADD COLUMN action_priority VARCHAR(8);

-- Ce CASE doit produire exactement ce que produit ActionPriorityCalculator : une
-- divergence donnerait un tri faux sur l'historique, sans la moindre erreur visible.
UPDATE fmea_items SET action_priority = CASE
    WHEN severity < 1 OR occurrence < 1 OR detection < 1 THEN NULL
    WHEN severity >= 9 THEN
        CASE WHEN occurrence <= 2 AND detection <= 6 THEN 'MEDIUM' ELSE 'HIGH' END
    WHEN severity >= 5 THEN
        CASE WHEN occurrence >= 6 THEN 'HIGH'
             WHEN occurrence >= 3 THEN CASE WHEN detection >= 7 THEN 'HIGH' ELSE 'MEDIUM' END
             ELSE CASE WHEN detection >= 4 THEN 'MEDIUM' ELSE 'LOW' END END
    ELSE
        CASE WHEN occurrence >= 6 THEN 'MEDIUM'
             ELSE CASE WHEN detection >= 7 THEN 'MEDIUM' ELSE 'LOW' END END
END;

CREATE INDEX idx_fmea_items_operation ON fmea_items (tenant_id, operation_id);
