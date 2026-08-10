-- Le tableau des actions d'une CAPA devient lisible en audit (§4.2, ISO 9001 §10.2).
--
-- Trois manques, tous constatés en lecture de fiche :
--   1. aucune date de DÉCISION — seule la date technique de création existait, et
--      s'en servir pour dire « décidé le » ferait mentir la colonne : une action
--      saisie trois semaines après le comité d'engagement n'a pas été décidée le
--      jour de sa saisie ;
--   2. aucun NOM de porteur — l'action ne portait qu'un UUID d'assigné, illisible ;
--   3. aucune PREUVE au niveau de l'action — l'ADR 0050 avait posé la preuve au
--      niveau du dossier en prévoyant explicitement ce cas (« reste ajoutable
--      par-dessus »). ADR 0052 l'ajoute sans rien retirer.

-- 1 & 2 — la décision et son porteur, portés par l'action elle-même.
ALTER TABLE capa_actions
    ADD COLUMN decided_on    DATE,
    -- Nom lisible du porteur, figé au moment de la décision. La plateforme n'a
    -- pas d'annuaire consultable depuis le moteur qualité (cf. ADR 0052) ; et
    -- même si elle en avait un, un dossier d'audit doit montrer le nom
    -- ENREGISTRÉ à la décision, pas celui qu'une résolution vivante donnerait
    -- après un départ ou un changement d'état civil.
    ADD COLUMN assignee_name VARCHAR(255);

-- Les lignes existantes restent à NULL, délibérément. Recopier created_at
-- fabriquerait une date de décision que l'organisation n'a jamais enregistrée —
-- exactement ce que cette colonne existe pour éviter. L'écran affiche « — ».
COMMENT ON COLUMN capa_actions.decided_on IS
    'Jour où l''action a été décidée (comité, revue). Distinct de created_at, qui est la date de saisie.';
COMMENT ON COLUMN capa_actions.assignee_name IS
    'Nom lisible du porteur, figé à la décision. Complète assignee_id, ne le remplace pas.';

-- 3 — la preuve peut désormais viser une action précise du dossier.
ALTER TABLE capa_evidences
    ADD COLUMN action_id UUID;

-- NULL = preuve du DOSSIER (comportement d'origine, ADR 0050, inchangé pour
-- toutes les lignes déjà versées). Non NULL = preuve de CETTE action.
ALTER TABLE capa_evidences
    ADD CONSTRAINT fk_capa_evidences_action FOREIGN KEY (action_id)
        REFERENCES capa_actions (id) ON DELETE CASCADE;

COMMENT ON COLUMN capa_evidences.action_id IS
    'Action visée par la preuve ; NULL = preuve du dossier (ADR 0050).';

-- Une action porte AU PLUS une pièce : la colonne « Preuve » du tableau montre
-- un document, pas une liste. La contrainte est en base et pas seulement en
-- service, parce qu'un doublon rendrait la cellule indécidable. Index partiel :
-- les preuves de dossier (action_id NULL) ne sont pas concernées.
CREATE UNIQUE INDEX uq_capa_evidences_one_per_action
    ON capa_evidences (action_id)
    WHERE action_id IS NOT NULL;

-- Lecture du tableau : toutes les preuves d'actions d'un dossier, en une passe.
CREATE INDEX ix_capa_evidences_tenant_capa_action
    ON capa_evidences (tenant_id, capa_id, action_id);
