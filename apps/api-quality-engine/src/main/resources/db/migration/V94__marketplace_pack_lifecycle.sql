-- §8.11 Marketplace de packs normatifs — cycle de vie complet.
-- Fait évoluer marketplace_packs (V56) du booléen 'verified' vers un cycle de vie
-- SUBMITTED → IN_REVIEW → PUBLISHED / REJECTED → DEPRECATED, + métadonnées de
-- soumission/revue, normes couvertes, manifeste inline scanné et notation.

ALTER TABLE marketplace_packs
    ADD COLUMN status         VARCHAR(16),
    ADD COLUMN norms_csv      VARCHAR(1000),
    ADD COLUMN submitted_by   UUID,
    ADD COLUMN submitted_at   TIMESTAMPTZ,
    ADD COLUMN reviewed_by    UUID,
    ADD COLUMN reviewed_at    TIMESTAMPTZ,
    ADD COLUMN review_notes   VARCHAR(2000),
    ADD COLUMN manifest_json  TEXT,
    ADD COLUMN rating_avg     DOUBLE PRECISION NOT NULL DEFAULT 0,
    ADD COLUMN rating_count   INTEGER          NOT NULL DEFAULT 0;

-- Backfill : un pack 'verified' devient PUBLISHED, sinon SUBMITTED (en attente).
UPDATE marketplace_packs SET status = 'PUBLISHED' WHERE verified = TRUE;
UPDATE marketplace_packs SET status = 'SUBMITTED' WHERE verified = FALSE;

-- Backfill de la soumission : faute d'historique, on dérive de created_at et, à
-- défaut d'auteur connu, du verified_by (peut être NULL → comblé ci-dessous).
UPDATE marketplace_packs SET submitted_at = created_at WHERE submitted_at IS NULL;
UPDATE marketplace_packs SET submitted_by = COALESCE(verified_by, id) WHERE submitted_by IS NULL;
UPDATE marketplace_packs
    SET reviewed_by = verified_by, reviewed_at = verified_at
    WHERE verified = TRUE;

-- Contraintes définitives une fois les colonnes peuplées.
ALTER TABLE marketplace_packs
    ALTER COLUMN status       SET NOT NULL,
    ALTER COLUMN submitted_by SET NOT NULL,
    ALTER COLUMN submitted_at SET NOT NULL;

ALTER TABLE marketplace_packs
    ADD CONSTRAINT chk_mp_status
        CHECK (status IN ('SUBMITTED','IN_REVIEW','PUBLISHED','REJECTED','DEPRECATED')),
    ADD CONSTRAINT chk_mp_rating_avg   CHECK (rating_avg >= 0 AND rating_avg <= 5),
    ADD CONSTRAINT chk_mp_rating_count CHECK (rating_count >= 0);

-- L'ancien index/colonne 'verified' est remplacé par un index sur 'status'.
DROP INDEX IF EXISTS idx_mp_verified;
ALTER TABLE marketplace_packs DROP COLUMN verified;
CREATE INDEX idx_mp_status ON marketplace_packs (status);

COMMENT ON COLUMN marketplace_packs.status IS
    'Cycle de vie §8.11 : SUBMITTED→IN_REVIEW→PUBLISHED/REJECTED→DEPRECATED. Catalogue public = PUBLISHED.';
COMMENT ON COLUMN marketplace_packs.manifest_json IS
    'Manifeste inline scanné à la soumission (structure/anti path-traversal) avant mise en revue.';
