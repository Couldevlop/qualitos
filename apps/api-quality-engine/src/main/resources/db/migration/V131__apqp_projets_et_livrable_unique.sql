-- APQP : le cycle devient celui d'un PROJET, et tout livrable porte les mêmes
-- colonnes que le classeur de suivi du client (ADR 0072).
--
-- Deux changements, et ils vont ensemble.
--
-- 1. Un client menait UN cycle. Il en mène plusieurs de front — une introduction
--    de produit pour l'un, un transfert d'outillage pour l'autre — et chacun
--    remet son propre dossier PPAP. Le cycle unique les mélangeait : impossible
--    de dire de quel programme venait un livrable coché.
--
-- 2. Un livrable portait un GENRE fermé qui décidait de son formulaire. Les
--    livrables « renvoi module » n'étaient alors PAS cochables, et quatre
--    formulaires répondaient à une seule question — « où en est-on ? ». Le genre
--    disparaît ; restent les colonnes D à J du classeur, pour tous.
--
-- Rien n'est perdu. Les cycles déjà saisis sont versés dans un projet
-- « Projet par défaut » par client, cases cochées et pièces jointes comprises,
-- et le contenu des anciens sous-points et mesures est replié dans les notes du
-- livrable plutôt que jeté avec la colonne qui le portait.

-- ---------------------------------------------------------------------------
-- 1. Les projets
-- ---------------------------------------------------------------------------

CREATE TABLE apqp_projects (
    id          UUID PRIMARY KEY,
    tenant_id   UUID         NOT NULL,

    name        VARCHAR(255) NOT NULL,

    -- Ce que le projet ouvre : NPI (produit nouveau), ToW (transfert
    -- d'outillage ou de site), NEW_CUSTOMER (produit connu, client nouveau),
    -- OTHER. Un jeu fermé plutôt qu'un champ libre : c'est sur lui qu'on filtre
    -- la liste, et un texte libre aurait autant d'orthographes que de saisies.
    type        VARCHAR(32)  NOT NULL DEFAULT 'OTHER',

    -- Le destinataire du dossier PPAP. Facultatif : un projet interne n'en a pas.
    customer    VARCHAR(255),
    -- Référence du programme chez le client, ou repère interne.
    reference   VARCHAR(120),
    description VARCHAR(2000),

    created_by  UUID,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT ck_apqp_projects_type
        CHECK (type IN ('NPI', 'TOW', 'NEW_CUSTOMER', 'OTHER'))
);

-- La liste d'un client se lit du plus récent au plus ancien : c'est le
-- programme en cours qu'on vient chercher, pas celui d'il y a trois ans.
CREATE INDEX idx_apqp_projects_tenant_created
    ON apqp_projects (tenant_id, created_at DESC);

-- ---------------------------------------------------------------------------
-- 2. Le projet par défaut, pour ne rien perdre
-- ---------------------------------------------------------------------------

-- Un projet par client QUI A DÉJÀ DES PHASES, et un seul. Les clients qui n'ont
-- jamais ouvert l'écran n'en reçoivent aucun : ils créeront le leur, et un
-- projet vide posé d'office aurait pollué toutes les listes.
--
-- L'identifiant est dérivé du client (`md5` du tenant), pas tiré au hasard :
-- rejouer cette migration sur la même base rendrait le même projet, et la
-- jointure de l'étape suivante n'aurait pas à le retrouver par son nom.
INSERT INTO apqp_projects (id, tenant_id, name, type, description, created_at, updated_at)
SELECT DISTINCT
       ('a0000000-0000-4000-8000-' || substr(md5(p.tenant_id::text), 1, 12))::uuid,
       p.tenant_id,
       'Projet par défaut',
       'OTHER',
       'Cycle APQP repris lors du passage aux projets multiples. '
       || 'Renommez-le, ou répartissez ses livrables dans de nouveaux projets.',
       now(),
       now()
  FROM apqp_phases p;

-- ---------------------------------------------------------------------------
-- 3. Les phases rejoignent leur projet
-- ---------------------------------------------------------------------------

ALTER TABLE apqp_phases
    ADD COLUMN project_id UUID REFERENCES apqp_projects (id) ON DELETE CASCADE;

UPDATE apqp_phases p
   SET project_id = a.id
  FROM apqp_projects a
 WHERE a.tenant_id = p.tenant_id;

-- NOT NULL seulement APRÈS le remplissage : l'imposer d'emblée aurait refusé la
-- colonne sur toute base qui portait déjà des phases.
ALTER TABLE apqp_phases
    ALTER COLUMN project_id SET NOT NULL;

-- Le rang est unique dans un PROJET et non dans un client : deux programmes ont
-- chacun leur phase 1, et l'ancienne contrainte les aurait déclarées en
-- conflit. `DEFERRABLE` pour la même raison qu'avant — une réorganisation passe
-- par des états intermédiaires où deux phases se croisent.
ALTER TABLE apqp_phases
    DROP CONSTRAINT uq_apqp_phases_tenant_position;

ALTER TABLE apqp_phases
    ADD CONSTRAINT uq_apqp_phases_project_position
    UNIQUE (project_id, position) DEFERRABLE INITIALLY DEFERRED;

CREATE INDEX idx_apqp_phases_project_position
    ON apqp_phases (project_id, position);

-- ---------------------------------------------------------------------------
-- 4. Les colonnes du classeur sur chaque livrable
-- ---------------------------------------------------------------------------

ALTER TABLE apqp_deliverables
    -- Colonne D : l'artefact attendu. Le libellé dit ce qu'on doit produire,
    -- l'artefact sous quelle forme — et c'est lui qu'un auditeur confronte à la
    -- pièce versée.
    --
    -- Laissée NULLE sur les lignes existantes, délibérément : les quarante-huit
    -- textes du référentiel vivent dans le code, et les recopier ici en ferait
    -- une seconde définition qui divergerait au premier ajustement. La lecture
    -- retombe sur le référentiel quand la colonne est vide (ApqpService).
    ADD COLUMN expected_artifact VARCHAR(1000),

    -- Colonne F : le responsable. Un nom et non un compte — il peut être externe.
    ADD COLUMN owner             VARCHAR(150),

    -- Colonne G : l'échéance.
    ADD COLUMN due_date          DATE,

    -- Colonne H : le statut. BLOCKED n'est pas un avancement — il dit que la
    -- cause est ailleurs, et c'est l'unique information qu'une revue cherche.
    ADD COLUMN status            VARCHAR(32) NOT NULL DEFAULT 'NOT_STARTED',

    -- Colonne I : l'avancement.
    ADD COLUMN percent_complete  INT         NOT NULL DEFAULT 0;

ALTER TABLE apqp_deliverables
    ADD CONSTRAINT ck_apqp_deliverables_status
        CHECK (status IN ('NOT_STARTED', 'IN_PROGRESS', 'BLOCKED', 'DONE')),
    ADD CONSTRAINT ck_apqp_deliverables_percent
        CHECK (percent_complete BETWEEN 0 AND 100);

-- La case pilote (ADR 0072) : un livrable déjà coché est « terminé à 100 % ».
-- Sans cette reprise, tout le cycle d'un client s'afficherait « non démarré »
-- sous des cases cochées — l'incohérence exacte que la règle supprime.
UPDATE apqp_deliverables
   SET status = 'DONE', percent_complete = 100
 WHERE done;

-- ---------------------------------------------------------------------------
-- 5. Le genre disparaît, et son contenu est replié dans les notes
-- ---------------------------------------------------------------------------

-- `data` portait les sous-points d'une CHECKLIST ou les mesures d'un
-- DATA_ENTRY. Ces deux formulaires n'existent plus, mais ce qu'un utilisateur y
-- a saisi lui appartient : on le replie en texte dans les notes (colonne J) plutôt
-- que de le laisser partir avec la colonne.
--
-- Seules les lignes qui portent une saisie RÉELLE sont reprises — une case
-- cochée, ou une valeur non vide. Recopier l'amorçage intact aurait rempli les
-- notes de tout le monde avec ce que le référentiel disait déjà.
--
-- La colonne est du TEXT et seul le validateur y a jamais écrit, mais une ligne
-- touchée à la main ne doit pas faire échouer la migration entière : la
-- conversion est donc gardée ligne à ligne, et une valeur illisible est ignorée
-- au lieu d'interrompre la mise à jour.
WITH replie AS (
    SELECT d.id,
           string_agg(
               ligne ->> 'label'
               || CASE WHEN coalesce(ligne ->> 'value', '') <> ''
                       THEN ' : ' || (ligne ->> 'value')
                            || coalesce(' ' || nullif(ligne ->> 'unit', ''), '')
                       ELSE '' END
               || CASE WHEN coalesce(ligne ->> 'measuredAt', '') <> ''
                       THEN ' (' || (ligne ->> 'measuredAt') || ')'
                       ELSE '' END
               || CASE WHEN (ligne ->> 'checked') = 'true' THEN ' [acquis]' ELSE '' END,
               ' ; ' ORDER BY ordinalite) AS texte
      FROM apqp_deliverables d
      -- La conversion est portee PAR LA LIGNE et non par la clause WHERE : un
      -- filtre `WHERE d.data LIKE '[%'` n'est pas garanti de s'evaluer AVANT la
      -- fonction laterale, et une seule valeur mal formee ferait alors echouer
      -- la migration entiere -- sur la base d'un client, en pleine mise a jour.
      -- `pg_input_is_valid` (PostgreSQL 16+) repond sans lever, et une valeur
      -- illisible rend un tableau vide : elle est ignoree, pas fatale.
      CROSS JOIN LATERAL jsonb_array_elements(
               CASE WHEN d.data LIKE '[%' AND pg_input_is_valid(d.data, 'jsonb')
                    THEN d.data::jsonb
                    ELSE '[]'::jsonb END)
                    WITH ORDINALITY AS t(ligne, ordinalite)
     WHERE d.data IS NOT NULL
     GROUP BY d.id
    HAVING bool_or((ligne ->> 'checked') = 'true'
                   OR coalesce(ligne ->> 'value', '') <> '')
)
UPDATE apqp_deliverables d
   -- `left(..., 2000)` : la colonne des notes s'arrête là, et un livrable de
   -- trente sous-points tous renseignés pourrait la dépasser. Mieux vaut un
   -- report tronqué qu'une migration qui échoue.
   SET comment = left(
           coalesce(nullif(d.comment, '') || E'\n', '')
           || 'Contenu repris de l''ancien formulaire : ' || r.texte, 2000)
  FROM replie r
 WHERE d.id = r.id;

ALTER TABLE apqp_deliverables
    DROP CONSTRAINT ck_apqp_deliverables_kind;

ALTER TABLE apqp_deliverables
    DROP COLUMN kind,
    DROP COLUMN data;

COMMENT ON COLUMN apqp_deliverables.expected_artifact IS
    'Artefact attendu (colonne D du classeur) ; vide = celui du référentiel.';
COMMENT ON COLUMN apqp_deliverables.ppap IS
    'Colonne E « PPAP Req''d » : pilotée par le client, amorcée par le référentiel.';
COMMENT ON COLUMN apqp_deliverables.status IS
    'Colonne H ; tenue d''accord avec `done` par le service (ADR 0072).';
