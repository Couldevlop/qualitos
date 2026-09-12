-- APQP : un livrable se coche, se prouve, et dit s'il compose le dossier PPAP.
--
-- Jusqu'ici un livrable n'était qu'un libellé, et l'écran le précédait d'une
-- icône « coché » purement décorative : il donnait donc à lire « tout est fait »
-- alors qu'il ne savait rien de ce qui était fait. Et le document qui prouve le
-- livrable — reçu par courriel, en .docx — n'avait nulle part où se ranger.

ALTER TABLE apqp_deliverables
    -- L'astérisque du référentiel : « this deliverable is a PPAP element ».
    -- C'est cette colonne qui permet au dossier PPAP d'être une VUE du cycle, et
    -- non une seconde liste normative à tenir d'accord avec lui.
    ADD COLUMN ppap        BOOLEAN      NOT NULL DEFAULT false,

    -- Le GENRE du livrable, qui détermine ce que son formulaire demande :
    -- ATTACHMENT (une pièce), MODULE_LINK (un enregistrement QualitOS déjà tenu
    -- ailleurs), DATA_ENTRY (des mesures), CHECKLIST (des sous-points).
    --
    -- Un jeu fermé plutôt qu'un formulaire par livrable : le référentiel en
    -- compte une soixantaine, et les coder un par un les figerait — alors
    -- qu'ils ne diffèrent que par la nature de ce qu'ils produisent.
    ADD COLUMN kind        VARCHAR(32)  NOT NULL DEFAULT 'ATTACHMENT',

    ADD COLUMN done        BOOLEAN      NOT NULL DEFAULT false,
    ADD COLUMN done_at     TIMESTAMPTZ,
    -- Qui a coché. Un livrable coché par personne ne prouve rien : la question
    -- « qui l'a déclaré fait ? » vient toujours, et l'auditeur la pose.
    ADD COLUMN done_by     UUID,
    ADD COLUMN comment     VARCHAR(2000),

    -- Le contenu propre au genre. Sa FORME est fermée et validée par le service
    -- selon `kind` : ce n'est pas un fourre-tout, c'est l'un de deux tableaux
    -- connus — des mesures, ou des sous-points. Une colonne par genre aurait
    -- laissé trois colonnes nulles sur quatre à chaque ligne.
    ADD COLUMN data        JSONB,

    ADD COLUMN linked_kind VARCHAR(32),
    ADD COLUMN linked_id   UUID;

ALTER TABLE apqp_deliverables
    ADD CONSTRAINT ck_apqp_deliverables_kind
        CHECK (kind IN ('ATTACHMENT', 'MODULE_LINK', 'DATA_ENTRY', 'CHECKLIST')),
    ADD CONSTRAINT ck_apqp_deliverables_linked_kind
        CHECK (linked_kind IS NULL OR linked_kind IN ('FMEA', 'CONTROL_PLAN', 'PDCA', 'CAPA')),
    -- Un renvoi à moitié posé ne désigne rien : l'écran afficherait un lien, le
    -- clic tomberait dans le vide. Les deux colonnes vivent ou meurent ensemble.
    ADD CONSTRAINT ck_apqp_deliverables_link_complete
        CHECK ((linked_kind IS NULL) = (linked_id IS NULL));

-- Le dossier PPAP ne lit que les livrables étoilés d'un client. Sans index
-- partiel, il balaie tout le cycle de tous les clients à chaque ouverture.
CREATE INDEX idx_apqp_deliverables_tenant_ppap
    ON apqp_deliverables (tenant_id) WHERE ppap;

-- Les pièces versées en preuve d'un livrable.
--
-- Seule la métadonnée est ici ; le binaire vit dans le stockage objet sous une
-- clé tenantisée, comme pour les preuves PDCA et CAPA. La base ne porte jamais
-- d'octets de fichier.
CREATE TABLE apqp_deliverable_evidences (
    id                UUID PRIMARY KEY,

    -- Tenant dupliqué depuis la phase, délibérément : toute lecture de preuve
    -- filtre alors sur un seul index sans jointure, et une requête qui
    -- oublierait le filtre de livrable resterait malgré tout enfermée dans son
    -- client.
    tenant_id         UUID         NOT NULL,

    phase_id          UUID         NOT NULL REFERENCES apqp_phases (id) ON DELETE CASCADE,

    -- La suppression du livrable emporte ses preuves : une preuve sans livrable
    -- ne prouve plus rien, et son binaire redevient un orphelin que le balayeur
    -- effacera.
    deliverable_id    UUID         NOT NULL REFERENCES apqp_deliverables (id) ON DELETE CASCADE,

    -- Unique : deux lignes pour un même binaire feraient mentir le balayeur
    -- d'orphelins, qui demande « cette clé est-elle encore revendiquée ? ».
    object_key        VARCHAR(512) NOT NULL UNIQUE,

    content_type      VARCHAR(150) NOT NULL,
    size_bytes        BIGINT       NOT NULL,
    original_filename VARCHAR(255),

    -- Qui a versé la pièce. Une preuve anonyme se défend mal devant un auditeur.
    uploaded_by       UUID,

    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_apqp_evidences_deliverable
    ON apqp_deliverable_evidences (tenant_id, deliverable_id);
