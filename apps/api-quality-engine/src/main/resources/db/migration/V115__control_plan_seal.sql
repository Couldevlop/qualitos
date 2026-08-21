-- Le scellement d'un control plan approuvé : l'empreinte du document lui-même,
-- sa signature hybride, et la référence de la transaction qui l'ancre.
--
-- Le journal chaîné, ancré par lots, prouvait déjà que l'APPROBATION avait eu
-- lieu. Il ne disait rien de CE QUI avait été approuvé : les lignes vivent dans
-- une autre table, qu'un accès direct à la base pourrait modifier sans laisser
-- de trace au journal. C'est ce trou que ces trois colonnes ferment.
--
-- Nullables, et c'est voulu : un brouillon n'a rien d'opposable à prouver, et
-- les plans approuvés AVANT cette migration ne peuvent pas être scellés
-- rétroactivement — sceller après coup reviendrait à certifier un contenu qu'on
-- n'a pas vu approuver. Ils restent couverts par le journal chaîné, et
-- l'absence de scellement se lit telle quelle.
ALTER TABLE control_plans
    ADD COLUMN seal_sha256    VARCHAR(64),
    ADD COLUMN seal_signature VARCHAR(20000),
    ADD COLUMN anchor_tx_ref  VARCHAR(200);

-- Retrouver un document à partir de son empreinte : c'est la question que pose
-- un auditeur qui tient un PDF et demande « celui-ci, vous l'avez approuvé ? ».
-- Partiel, car seuls les plans scellés y répondent.
CREATE INDEX ix_control_plans_seal_sha256
    ON control_plans (tenant_id, seal_sha256)
    WHERE seal_sha256 IS NOT NULL;
