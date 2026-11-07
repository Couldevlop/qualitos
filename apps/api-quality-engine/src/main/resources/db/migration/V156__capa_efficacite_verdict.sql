-- Verdict d'efficacité à trois états, et critère posé à l'avance (ADR 0108).
--
-- Pourquoi. La vérification d'efficacité d'un dossier CAPA était un BOOLÉEN.
-- Une action qui fait passer le rebut de 4,2 % à 2,5 % pour une cible de 2 %
-- n'est ni efficace ni inefficace : le booléen oblige à mentir dans un sens ou
-- dans l'autre. Répondre « non » rouvre un dossier qui avançait ; répondre
-- « oui » clôt au-dessus d'un objectif non atteint — et c'est la seconde
-- réponse qu'on choisit, parce qu'elle fait avancer le tableau de bord.
--
-- Rien n'est supprimé ici. effectiveness_verified reste et devient la
-- projection du verdict (EFFECTIVE -> true, sinon false) : l'écran d'efficacité
-- MESURÉE l'affiche à côté du taux de récidive, et les dossiers antérieurs à
-- cette migration n'auront jamais de verdict.

-- Le critère : la phrase qui dit à quoi on reconnaîtra l'efficacité.
--
-- Il vit sur le DOSSIER et non sur l'évaluation parce qu'il doit précéder la
-- mesure. Un critère écrit après coup n'est pas un critère, c'est un
-- commentaire — et c'est ce qui se passait : il se décidait au moment de juger,
-- par la personne qui avait mené l'action, en regardant le résultat obtenu.
ALTER TABLE capa_cases
    ADD COLUMN effectiveness_criterion TEXT;

-- L'historique des évaluations. Une ligne par jugement, jamais écrasée.
--
-- Pourquoi un historique et non trois colonnes de plus sur le dossier : un
-- dossier jugé inefficace repart en traitement, reçoit une action, et se fait
-- réévaluer. La suite « inefficace, puis partiel, puis efficace » est
-- exactement ce qu'un auditeur lit — elle montre un système qui corrige. Un
-- champ écrasé ne montre que la fin, et donne du dossier une image de réussite
-- immédiate qui est fausse.
CREATE TABLE capa_effectiveness_reviews (
    id                UUID PRIMARY KEY,
    tenant_id         UUID         NOT NULL,
    capa_case_id      UUID         NOT NULL,

    verdict           VARCHAR(20)  NOT NULL,

    -- Le critère RECOPIÉ du dossier au moment du jugement. Pas de la
    -- redondance : modifier le critère d'un dossier ne doit pas réécrire celui
    -- sur lequel une évaluation passée s'est prononcée. Même raison que
    -- verification_assignee_name, recopié depuis l'annuaire.
    criterion         TEXT         NOT NULL,

    -- Les trois relevés, en TEXTE COURT et non en nombres. « 4,2 % »,
    -- « <= 2 g », « 0 retard sur 620 expéditions » : l'unité, la tolérance et
    -- l'assiette font partie du relevé. Les séparer en nombre + unité
    -- obligerait à inventer un référentiel d'unités que chaque secteur
    -- contredirait. Aucun indicateur ne s'en déduit — ils se lisent, ils ne
    -- s'agrègent pas ; c'est le verdict qui alimente les KPI, et lui est
    -- énuméré (CLAUDE.md §18.2 #8).
    baseline          VARCHAR(120),
    target            VARCHAR(120),
    observed          VARCHAR(120) NOT NULL,

    comment           TEXT,

    -- Qui a jugé. Identifiant de l'annuaire du client, et nom recopié : un
    -- compte désactivé ne doit pas transformer la trace en « utilisateur
    -- inconnu » le jour de l'audit.
    reviewer_id       UUID,
    reviewer_name     VARCHAR(255),

    reviewed_at       TIMESTAMPTZ  NOT NULL,

    CONSTRAINT fk_capa_effectiveness_reviews_case
        FOREIGN KEY (capa_case_id) REFERENCES capa_cases (id) ON DELETE CASCADE,
    CONSTRAINT chk_capa_effectiveness_reviews_verdict
        CHECK (verdict IN ('EFFECTIVE', 'PARTIAL', 'INEFFECTIVE')),
    -- Un constaté vide rendrait le verdict indéfendable : on juge sur un
    -- relevé, ou on ne juge pas.
    CONSTRAINT chk_capa_effectiveness_reviews_observed
        CHECK (length(btrim(observed)) > 0),
    CONSTRAINT chk_capa_effectiveness_reviews_criterion
        CHECK (length(btrim(criterion)) > 0)
);

-- « Montrez-moi l'historique des évaluations de ce dossier » est la requête de
-- l'écran de détail et celle de l'auditeur : la plus récente d'abord.
CREATE INDEX idx_capa_effectiveness_reviews_case
    ON capa_effectiveness_reviews (capa_case_id, reviewed_at DESC);

-- La politique de cloisonnement filtre sur tenant_id (ADR 0086) : sans index,
-- chaque lecture la contrôlerait ligne à ligne.
CREATE INDEX idx_capa_effectiveness_reviews_tenant
    ON capa_effectiveness_reviews (tenant_id);
