-- LES OBJECTIFS QUALITE, ET LA CAPA QU'UN ECART D'OBJECTIF PROPOSE.
--
-- ISO 9001 §6.2 demande des objectifs qualite mesurables, suivis, et revus. La
-- plateforme savait deja definir un KPI et le mesurer (kpi_definitions,
-- kpi_measurements) ; elle ne savait pas dire ce qu'on en ATTEND, ni ce qu'il
-- faut faire quand l'attente n'est pas tenue. C'est tout l'ecart entre un
-- indicateur et un objectif : l'indicateur constate, l'objectif engage.
--
-- POURQUOI UNE TABLE A PART, et non des colonnes de plus sur kpi_definitions :
-- un meme KPI sert plusieurs objectifs -- le taux de rebut vise 1,5 % en
-- production et 0,8 % sur la ligne neuve -- et chaque objectif a son processus,
-- sa clause, son pilote et sa regle de declenchement. Les seuils de la
-- definition de KPI restent ceux du catalogue (sante de l'indicateur) ; ceux de
-- l'objectif sont l'engagement pris devant la direction. Voir l'ADR 0133.
--
-- L'UNITE ET LE SENS NE SONT PAS REPRIS ICI : ils appartiennent au KPI
-- (kpi_definitions.unit, .direction). Un objectif qui porterait son unite
-- pourrait la contredire, et c'est la cible affichee qui mentirait.
CREATE TABLE quality_objectives (
    id                  UUID         NOT NULL DEFAULT gen_random_uuid(),
    tenant_id           UUID         NOT NULL,
    title               VARCHAR(250) NOT NULL,
    -- Le KPI reel auquel l'objectif se compare. Sans lui, un objectif n'est
    -- qu'une intention : §6.2 exige qu'il soit mesurable.
    kpi_id              UUID         NOT NULL,
    -- Le processus concerne, repris du referentiel documentaire. L'identifiant
    -- fait le lien, le libelle est FIGE a la saisie : un processus renomme ne
    -- doit pas reecrire l'objectif d'une revue deja tenue (ADR 0052).
    process_id          UUID,
    process_name        VARCHAR(120),
    iso_clause          VARCHAR(40),
    -- La periode de comparaison, en mois : 1, 3, 6 ou 12. C'est l'unite dans
    -- laquelle se comptent les depassements consecutifs.
    period_months       SMALLINT     NOT NULL,
    target_value        NUMERIC(24, 6) NOT NULL,
    threshold_warning   NUMERIC(24, 6) NOT NULL,
    threshold_critical  NUMERIC(24, 6) NOT NULL,
    -- La regle de declenchement : combien de periodes consecutives au-dela du
    -- seuil avant que le logiciel propose une CAPA. Parametrable parce qu'un
    -- mois rate n'a pas le meme sens sur un indicateur mensuel et sur un
    -- indicateur annuel.
    warning_periods     SMALLINT     NOT NULL DEFAULT 2,
    critical_periods    SMALLINT     NOT NULL DEFAULT 1,
    -- Combien de periodes consecutives SOUS LA CIBLE il faut pour que l'objectif
    -- redevienne « atteint ». C'est aussi, mot pour mot, le critere d'efficacite
    -- de la CAPA nee de cet objectif : le retour du KPI sous la cible sur une
    -- duree definie. Un seul chiffre pour les deux, parce que ce sont la meme
    -- question posee deux fois.
    recovery_periods    SMALLINT     NOT NULL DEFAULT 1,
    -- Le pilote a qui la CAPA sera proposee. Choisi dans l'annuaire du client,
    -- son nom recopie a la saisie (ADR 0052) : une fiche de revue ne doit pas
    -- afficher un identifiant technique.
    capa_owner_id       UUID         NOT NULL,
    capa_owner_name     VARCHAR(255),
    -- Le delai d'analyse de cause racine, en jours ouvres : il devient
    -- l'echeance de la CAPA proposee.
    root_cause_due_days SMALLINT     NOT NULL DEFAULT 5,
    status              VARCHAR(32)  NOT NULL DEFAULT 'ACTIVE',
    created_by          UUID         NOT NULL,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT pk_quality_objectives PRIMARY KEY (id),
    CONSTRAINT fk_quality_objectives_kpi
        FOREIGN KEY (kpi_id) REFERENCES kpi_definitions (id),
    CONSTRAINT fk_quality_objectives_process
        FOREIGN KEY (process_id) REFERENCES document_processes (id) ON DELETE SET NULL,
    CONSTRAINT ck_quality_objectives_title CHECK (btrim(title) <> ''),
    -- 1, 3, 6 ou 12 mois, et rien d'autre : une periode libre rendrait les
    -- depassements consecutifs incomparables d'un objectif a l'autre.
    CONSTRAINT ck_quality_objectives_period CHECK (period_months IN (1, 3, 6, 12)),
    CONSTRAINT ck_quality_objectives_warning_periods CHECK (warning_periods >= 1),
    CONSTRAINT ck_quality_objectives_critical_periods CHECK (critical_periods >= 1),
    CONSTRAINT ck_quality_objectives_recovery_periods CHECK (recovery_periods >= 1),
    CONSTRAINT ck_quality_objectives_due_days CHECK (root_cause_due_days >= 1)
);

-- La revue lit les objectifs vivants d'un client. C'est la seule lecture de
-- masse, et elle a lieu a chaque ouverture de l'ecran.
CREATE INDEX ix_quality_objectives_tenant_status
    ON quality_objectives (tenant_id, status);

-- « Quels objectifs suivent ce KPI ? » -- posee quand une mesure arrive.
CREATE INDEX ix_quality_objectives_kpi
    ON quality_objectives (tenant_id, kpi_id);

COMMENT ON TABLE quality_objectives IS
    'Objectifs qualite mesurables (ISO 9001 §6.2) : ce qu''on attend d''un KPI, et la regle qui propose une CAPA quand l''attente n''est pas tenue (ADR 0133)';
COMMENT ON COLUMN quality_objectives.period_months IS
    'Periode de comparaison en mois : 1, 3, 6 ou 12 ; unite de comptage des depassements consecutifs';
COMMENT ON COLUMN quality_objectives.recovery_periods IS
    'Periodes consecutives sous la cible exigees pour redevenir « atteint » ; aussi le critere d''efficacite de la CAPA de cet objectif';
COMMENT ON COLUMN quality_objectives.process_name IS
    'Libelle du processus fige a la saisie ; jamais resolu a l''affichage (ADR 0052)';
COMMENT ON COLUMN quality_objectives.capa_owner_name IS
    'Nom du pilote fige a la saisie ; jamais resolu a l''affichage (ADR 0052)';

-- LA CAPA EST PROPOSEE, JAMAIS IMPOSEE.
--
-- Le logiciel constate l'ecart et redige le dossier ; c'est le pilote qui
-- l'accepte, le rejette avec justification, ou lui ajoute une action. Creer la
-- CAPA d'autorite remplirait le registre de dossiers que personne n'a decides,
-- et l'ISO demande l'inverse : une action decidee par qui en repond (§10.2).
-- C'est aussi l'invariant de la plateforme -- l'outil propose, l'humain decide
-- (CLAUDE.md §12).
--
-- Une proposition est donc une ETAPE, pas une CAPA : tant qu'elle est en
-- attente, aucun dossier n'existe. Acceptee, elle porte l'identifiant du
-- dossier cree ; rejetee, elle garde la justification -- et c'est cette trace
-- que l'auditeur vient lire quand un objectif critique n'a produit aucune CAPA.
CREATE TABLE quality_objective_proposals (
    id                     UUID         NOT NULL DEFAULT gen_random_uuid(),
    tenant_id              UUID         NOT NULL,
    objective_id           UUID         NOT NULL,
    -- WARNING ou CRITICAL : le niveau qui a declenche. Il se lit dans le
    -- dossier propose et fixe sa criticite.
    level                  VARCHAR(16)  NOT NULL,
    -- La periode qui a declenche, et la valeur qu'on y a relevee : une
    -- proposition doit pouvoir se relire sans recalculer quoi que ce soit, meme
    -- des annees apres, meme si les mesures ont ete corrigees depuis.
    period_start           TIMESTAMPTZ  NOT NULL,
    period_end             TIMESTAMPTZ  NOT NULL,
    observed_value         NUMERIC(24, 6) NOT NULL,
    breach_periods         SMALLINT     NOT NULL,
    title                  VARCHAR(255) NOT NULL,
    description            TEXT,
    -- Le critere d'efficacite, redige par la regle et non par un utilisateur :
    -- pour un ecart d'objectif, l'efficacite EST le retour du KPI sous la cible
    -- sur la duree definie. Le laisser saisir reviendrait a permettre de clore
    -- sur un critere plus facile que celui qu'on s'etait donne.
    effectiveness_criterion TEXT        NOT NULL,
    status                 VARCHAR(16)  NOT NULL DEFAULT 'PENDING',
    capa_case_id           UUID,
    rejection_reason       TEXT,
    decided_by             UUID,
    decided_at             TIMESTAMPTZ,
    created_at             TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT pk_quality_objective_proposals PRIMARY KEY (id),
    CONSTRAINT fk_qo_proposals_objective
        FOREIGN KEY (objective_id) REFERENCES quality_objectives (id) ON DELETE CASCADE,
    -- Reevaluer la meme periode ne cree pas une seconde proposition. Sans cela,
    -- un ecran rafraichi trois fois proposerait trois fois le meme dossier, et
    -- le pilote aurait a rejeter deux doublons pour prouver qu'il a lu le
    -- premier.
    CONSTRAINT uk_qo_proposals_periode
        UNIQUE (tenant_id, objective_id, level, period_start),
    CONSTRAINT ck_qo_proposals_level CHECK (level IN ('WARNING', 'CRITICAL')),
    CONSTRAINT ck_qo_proposals_status
        CHECK (status IN ('PENDING', 'ACCEPTED', 'REJECTED')),
    CONSTRAINT ck_qo_proposals_breach_periods CHECK (breach_periods >= 1),
    -- Acceptee, elle cite son dossier ; rejetee, elle dit pourquoi. Un rejet
    -- sans motif est exactement ce qu'un auditeur ne peut pas instruire.
    --
    -- Le `coalesce` n'est pas une precaution de style : dans une contrainte SQL,
    -- un predicat qui vaut NULL n'est PAS une violation. Ecrit
    -- `btrim(rejection_reason) <> ''`, un rejet sans motif rendait NULL, et la
    -- contrainte le laissait passer -- exactement la ligne qu'elle existe pour
    -- refuser. Rendu non nul, le predicat vaut faux et refuse.
    CONSTRAINT ck_qo_proposals_decision CHECK (
        (status = 'PENDING'  AND capa_case_id IS NULL AND rejection_reason IS NULL)
     OR (status = 'ACCEPTED' AND capa_case_id IS NOT NULL)
     OR (status = 'REJECTED' AND btrim(coalesce(rejection_reason, '')) <> '')
    )
);

-- « Qu'est-ce qui attend une decision ? » : la question de l'ecran de revue.
CREATE INDEX ix_qo_proposals_attente
    ON quality_objective_proposals (tenant_id, status, created_at DESC);

-- L'historique d'un objectif, du plus recent au plus ancien.
CREATE INDEX ix_qo_proposals_objectif
    ON quality_objective_proposals (tenant_id, objective_id, created_at DESC);

COMMENT ON TABLE quality_objective_proposals IS
    'CAPA proposee sur ecart d''objectif qualite : en attente, acceptee (dossier cree) ou rejetee avec justification (ADR 0133)';
COMMENT ON COLUMN quality_objective_proposals.observed_value IS
    'Valeur relevee sur la periode declenchante, recopiee : une proposition se relit sans recalcul';
COMMENT ON COLUMN quality_objective_proposals.effectiveness_criterion IS
    'Critere d''efficacite verrouille : le retour du KPI sous la cible sur la duree definie par l''objectif';
COMMENT ON COLUMN quality_objective_proposals.rejection_reason IS
    'Pourquoi le pilote n''a pas retenu la CAPA proposee ; ce que l''auditeur vient lire';
