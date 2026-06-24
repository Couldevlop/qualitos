-- ANO-013 : la colonne signature_hash était dimensionnée VARCHAR(128) (« placeholder »),
-- mais DashboardLayoutService y stocke la signature HYBRIDE Ed25519+ML-DSA-65 complète
-- (~4,5 Ko en base64), d'où une troncation SQLState 22001 à chaque enregistrement de
-- dashboard personnalisé (création KO en live). On passe la colonne en TEXT (longueur libre).
ALTER TABLE dashboard_layouts ALTER COLUMN signature_hash TYPE TEXT;

COMMENT ON COLUMN dashboard_layouts.signature_hash IS
    'Enveloppe de signature hybride Ed25519+ML-DSA-65 (base64) du layout canonique (A08).';
