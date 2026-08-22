-- Lien FORT entre un audit et le référentiel qu'il vise (§8, §4.4).
--
-- La colonne texte `standard` reste, et garde son rôle : elle porte les normes
-- citées librement (« ISO 9001 §9.2 », « procédure achats »), que rien n'oblige à
-- exister dans le catalogue. Elle ne permet en revanche PAS de générer une
-- checklist : on ne dérive pas des questions d'audit d'une chaîne de caractères.
-- D'où cette référence, qui désigne un référentiel réel et ses exigences.
ALTER TABLE audit_plans ADD COLUMN standard_id UUID;

-- ON DELETE SET NULL et non CASCADE : supprimer un référentiel ne doit jamais
-- faire disparaître des audits déjà menés. L'audit conserve ses questions et ses
-- réponses — ce sont des lignes autonomes, copiées à la génération — il perd
-- seulement le lien vers un référentiel qui n'existe plus.
ALTER TABLE audit_plans ADD CONSTRAINT fk_audit_plans_standard
    FOREIGN KEY (standard_id) REFERENCES standards (id) ON DELETE SET NULL;

CREATE INDEX idx_audit_plans_standard ON audit_plans (standard_id);
