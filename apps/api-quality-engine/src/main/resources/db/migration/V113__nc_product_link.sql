-- Rien ne reliait une non-conformité au mode de défaillance qui l'avait — ou ne
-- l'avait pas — anticipée. Les deux colonnes sont nullables : une NC sans produit
-- (service, administratif) reste parfaitement légitime.
ALTER TABLE non_conformities ADD COLUMN product_id UUID
    REFERENCES products (id) ON DELETE SET NULL;
ALTER TABLE non_conformities ADD COLUMN fmea_item_id UUID
    REFERENCES fmea_items (id) ON DELETE SET NULL;

-- C'est la requête du calculateur d'occurrence : compter les NC d'un produit sur
-- douze mois glissants. Elle doit être servie par l'index, pas par un balayage.
CREATE INDEX idx_nc_tenant_product_detected
    ON non_conformities (tenant_id, product_id, detected_at);
CREATE INDEX idx_nc_fmea_item ON non_conformities (fmea_item_id);
