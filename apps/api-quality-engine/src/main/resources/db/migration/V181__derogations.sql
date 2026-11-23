-- LA DEMANDE DE DEROGATION (CONCESSION) CLIENT.
--
-- Une derogation demande au client l'autorisation d'expedier des pieces qui ne
-- respectent pas ses specifications. Elle se signe en INTERNE par le manager
-- qualite, puis part au client, qui accepte ou refuse. Tant que les deux
-- accords ne sont pas la, rien ne doit partir : c'est l'objet meme du dossier.
--
-- POURQUOI UN REGISTRE A PART, et non un etat de plus sur non_conformities :
-- une derogation n'est pas un ecart. L'ecart constate un manquement et se
-- traite ; la derogation demande a un TIERS de l'accepter malgre tout, et son
-- cycle de vie est celui d'une negociation, pas d'un traitement. La
-- presentation le dit : « une section Concessions au dessous des sections NC
-- qui regroupe TOUTES les concessions tous produits confondus ». Un dossier
-- peut citer une CAPA ou un 8D, mais par une reference en texte, pas par une
-- cle : la maquette l'ecrit « Ref. 8D / CAPA et date prevue », un champ libre.
--
-- AUCUN RATTACHEMENT NORMATIF : ni ISO 9001 §8.7, ni IATF 16949, ni AS9100D,
-- bien que les trois soient au catalogue. C'est un arbitrage explicite.
--
-- LA REFERENCE est de la forme DER-<annee>-<4 chiffres>, une suite par client
-- et par annee, et elle est UNIQUE par client : c'est elle qu'on cite dans un
-- courriel au client, donc elle ne se reecrit jamais.
CREATE TABLE derogations (
    id                      UUID         NOT NULL DEFAULT gen_random_uuid(),
    tenant_id               UUID         NOT NULL,
    reference               VARCHAR(40)  NOT NULL,

    -- L'ETAT AU REGISTRE. Les quatre statuts annonces par la presentation
    -- (acceptee, refusee, en attente interne, en attente client) sont la SUITE
    -- du cycle dont la maquette porte le debut (brouillon, signature demandee,
    -- signe) : un seul axe, pas deux a raccorder.
    --   DRAFT            le brouillon, chez l'initiateur
    --   PENDING_INTERNAL la signature est demandee au manager
    --   TO_AMEND         le manager renvoie pour correction, avec un motif
    --   QUALITY_APPROVED le manager a signe -- encore INTERNE, rien n'est parti
    --   PENDING_CUSTOMER le courriel est parti, on attend la reponse du client
    --   ACCEPTED         le client a dit oui (eventuellement sous conditions)
    --   REFUSED          le client a dit non
    -- Le manager ne REFUSE pas : s'il n'accepte pas, le dossier passe TO_AMEND
    -- et revient a l'initiateur, autant de fois qu'il le faut jusqu'a la
    -- signature. La seule sortie par le bas est le retrait par l'initiateur.
    status                  VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',

    -- ---- Section 1 de la maquette : identification -----------------------
    -- Le client est un texte libre : la plateforme n'a pas de registre de
    -- clients, et le modele des reclamations fait de meme.
    -- NULLABLE, et c'est voulu : la maquette enregistre un brouillon
    -- "autant de fois qu'il veut", et le controle des six champs obligatoires
    -- n'intervient qu'a la DEMANDE DE SIGNATURE. Les rendre obligatoires ici
    -- interdirait d'enregistrer un brouillon a peine commence -- le cas le plus
    -- courant de tous.
    customer_name           VARCHAR(200),
    customer_quality_contact VARCHAR(200),
    customer_order_no       VARCHAR(100),
    -- « Ref. piece / designation ». C'est ce champ que la colonne « produit »
    -- du registre affiche et que son filtre interroge : aucun lien vers le
    -- referentiel produit n'a ete demande.
    part_reference          VARCHAR(200),
    part_index              VARCHAR(40),
    batch_number            VARCHAR(100),
    quantity                NUMERIC(18, 3),
    request_date            DATE         NOT NULL,

    -- ---- Section 2 : nature de la demande --------------------------------
    -- Quatre choix fermes, avec les valeurs par defaut de la maquette.
    shipment_stage          VARCHAR(20)  NOT NULL DEFAULT 'TO_SHIP',
    deviation_origin        VARCHAR(20)  NOT NULL DEFAULT 'PRODUCT',
    occurrence              VARCHAR(20)  NOT NULL DEFAULT 'FIRST',
    duration_kind           VARCHAR(20)  NOT NULL DEFAULT 'TEMPORARY',
    duration_detail         VARCHAR(300),

    -- ---- Section 3 : ecart ------------------------------------------------
    -- Les lignes d'ecart vivent dans leur propre table : il y en a plusieurs.
    -- « Justification : pourquoi les pieces peuvent etre utilisees (forme,
    -- ajustement, fonction) » -- c'est le coeur de la demande, et la maquette
    -- l'exige a la demande de signature.
    justification           TEXT,

    -- ---- Section 4 : cause, confinement et actions ------------------------
    root_cause              TEXT,
    containment             TEXT,
    corrective_action       TEXT,
    capa_reference          VARCHAR(200),
    -- « Pieces jointes », une ligne de texte citant « mesures, photos, rapport
    -- 8D ». C'est ce que la maquette porte, et la maquette fait loi ici : un
    -- depot de fichiers reels est un chantier a part, non demande.
    attachments_note        VARCHAR(500),

    -- ---- Section 5 : approbation interne ----------------------------------
    -- Le manager est CHOISI dans l'annuaire : son identifiant de compte fait
    -- le lien, son nom est FIGE a la designation (ADR 0052, ADR 0091) -- un
    -- compte renomme ne doit pas reecrire une signature deja donnee.
    manager_account_id      UUID,
    manager_account_name    VARCHAR(200),
    -- Le nom que le manager SAISIT en signant, et son trace. La maquette les
    -- exige tous les deux : « Indiquez votre nom et signez. » La signature est
    -- DECLARATIVE -- ni second facteur, ni signataire au sens de l'ADR 0099,
    -- ni sceau cryptographique : c'est un arbitrage explicite, coherent avec
    -- le PDF non scelle. La seule piece probante reste le PDF que le client
    -- renvoie signe de son cote.
    signed_by_name          VARCHAR(200),
    signature_png           BYTEA,
    signed_at               TIMESTAMPTZ,
    -- Le motif du renvoi « A modifier ». OBLIGATOIRE au renvoi : sans lui
    -- l'initiateur ne sait pas quoi corriger et la boucle tourne a vide (meme
    -- regle que le rejet motive de la GED, ADR 0099).
    amend_reason            TEXT,

    -- ---- L'envoi au client ------------------------------------------------
    -- Les deux champs qui s'ajoutent au moment de « Validation client ».
    customer_email          VARCHAR(320),
    send_comment            TEXT,
    -- La langue du courrier, choisie a l'envoi entre francais et anglais, et
    -- RETENUE sur le dossier : le PDF n'etant pas scelle il peut etre reemis,
    -- et sans cela un second envoi partirait dans une autre langue sans que
    -- personne l'ait decide (meme garde-fou que l'ADR 0129 pour le 8D).
    pdf_language            VARCHAR(5),
    sent_at                 TIMESTAMPTZ,

    -- ---- Section 6 : decision du client -----------------------------------
    -- Quatre valeurs, celles de la maquette. « Acceptee sous conditions » ne
    -- se replie PAS sur « Acceptee » : elle engage l'expedition autrement, et
    -- la perdre au registre perdrait une information qui compte. Elle se lit
    -- ici ; l'etat du dossier, lui, reste ACCEPTED -- le client a dit oui.
    --   PENDING | ACCEPTED | ACCEPTED_WITH_CONDITIONS | REFUSED
    customer_decision       VARCHAR(30)  NOT NULL DEFAULT 'PENDING',
    customer_decision_date  DATE,

    -- ---- Tracabilite ------------------------------------------------------
    -- `owner` du registre = le CREATEUR de la derogation, defini comme tel par
    -- la presentation. Le nom est fige, pour la meme raison que celui du
    -- manager.
    created_by_user_id      UUID         NOT NULL,
    created_by_name         VARCHAR(200),
    created_at              TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ  NOT NULL DEFAULT now(),
    -- Verrou optimiste : le retrait par l'initiateur et la signature par le
    -- manager visent le meme dossier et peuvent se croiser.
    version                 BIGINT       NOT NULL DEFAULT 0,

    CONSTRAINT pk_derogations PRIMARY KEY (id),
    CONSTRAINT uq_derogations_reference UNIQUE (tenant_id, reference),
    CONSTRAINT ck_derogations_status CHECK (status IN (
        'DRAFT', 'PENDING_INTERNAL', 'TO_AMEND', 'QUALITY_APPROVED',
        'PENDING_CUSTOMER', 'ACCEPTED', 'REFUSED')),
    CONSTRAINT ck_derogations_decision CHECK (customer_decision IN (
        'PENDING', 'ACCEPTED', 'ACCEPTED_WITH_CONDITIONS', 'REFUSED')),
    CONSTRAINT ck_derogations_shipment CHECK (shipment_stage IN (
        'TO_SHIP', 'ALREADY_SHIPPED')),
    CONSTRAINT ck_derogations_origin CHECK (deviation_origin IN (
        'PRODUCT', 'PROCESS')),
    CONSTRAINT ck_derogations_occurrence CHECK (occurrence IN (
        'FIRST', 'REPEAT')),
    CONSTRAINT ck_derogations_duration CHECK (duration_kind IN (
        'TEMPORARY', 'PERMANENT')),
    CONSTRAINT ck_derogations_language CHECK (pdf_language IS NULL
        OR pdf_language IN ('fr', 'en')),
    CONSTRAINT ck_derogations_quantity CHECK (quantity >= 0)
);

-- Le registre se lit par client, trie par date de creation, et se filtre sur
-- le statut : c'est l'index qui porte l'ecran.
CREATE INDEX ix_derogations_tenant_status
    ON derogations (tenant_id, status, created_at DESC);
-- Les filtres nommes par la presentation : owner (le createur), produit, client.
CREATE INDEX ix_derogations_tenant_owner
    ON derogations (tenant_id, created_by_user_id);
-- « Mes validations » du manager : les dossiers qui l'attendent, lui.
-- Index PARTIEL, parce que c'est une file d'attente : seuls les dossiers en
-- attente interne y figurent, et ils sont une poignee face a l'historique.
CREATE INDEX ix_derogations_manager_pending
    ON derogations (tenant_id, manager_account_id)
    WHERE status = 'PENDING_INTERNAL';

COMMENT ON TABLE derogations IS
    'Demandes de derogation (concession) client : expedier des pieces non conformes avec l''accord du client.';

-- LES LIGNES D'ECART (section 3 de la maquette).
--
-- Une table a part parce qu'un dossier en porte PLUSIEURS : la maquette ouvre
-- deux lignes vides et donne un bouton « + Ajouter une ligne ». La colonne
-- « defaut » du registre affiche la PREMIERE ligne ; les autres se lisent dans
-- la fiche. C'est `position` qui dit laquelle est la premiere, et elle est
-- unique par dossier pour que l'ordre affiche soit celui qui a ete saisi.
CREATE TABLE derogation_deviation_lines (
    id                  UUID         NOT NULL DEFAULT gen_random_uuid(),
    tenant_id           UUID         NOT NULL,
    derogation_id       UUID         NOT NULL,
    position            SMALLINT     NOT NULL,
    specification       VARCHAR(500),
    actual_value        VARCHAR(500),
    non_conforming_qty  NUMERIC(18, 3),

    CONSTRAINT pk_derogation_deviation_lines PRIMARY KEY (id),
    CONSTRAINT fk_derogation_lines_dossier FOREIGN KEY (derogation_id)
        REFERENCES derogations (id) ON DELETE CASCADE,
    CONSTRAINT uq_derogation_lines_position UNIQUE (derogation_id, position),
    CONSTRAINT ck_derogation_lines_qty CHECK (non_conforming_qty IS NULL
        OR non_conforming_qty >= 0)
);

CREATE INDEX ix_derogation_lines_dossier
    ON derogation_deviation_lines (tenant_id, derogation_id, position);

COMMENT ON TABLE derogation_deviation_lines IS
    'Lignes d''ecart d''une derogation : specification attendue, valeur reelle, quantite non conforme.';
