-- Pièces jointes par phase DMAIC : aux six photos de la fiche (emplacements 1 à 6)
-- s'ajoutent cinq pièces par phase, sur les emplacements 11-15 (Définir),
-- 21-25 (Mesurer), 31-35 (Analyser), 41-45 (Améliorer), 51-55 (Contrôler).
-- Elles admettent aussi les PDF et les documents Office, dont le type MIME
-- dépasse 50 caractères.
ALTER TABLE dmaic_resolution_photos DROP CONSTRAINT chk_dmaic_resolution_photos_slot;
ALTER TABLE dmaic_resolution_photos ADD CONSTRAINT chk_dmaic_resolution_photos_slot
    CHECK (slot BETWEEN 1 AND 6 OR (slot BETWEEN 11 AND 55 AND slot % 10 BETWEEN 1 AND 5));
ALTER TABLE dmaic_resolution_photos ALTER COLUMN content_type TYPE VARCHAR(100);
