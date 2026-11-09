-- V12 : la piste d'audit de la PLATEFORME.
--
-- Il en existait déjà une par client, dans le moteur de qualité
-- (`audit_events`, V26) : elle répond à « qui, chez moi, a touché à quoi ».
-- Elle ne répond pas à « qui, chez l'éditeur, a touché à mon dossier » — et
-- c'est précisément la question d'une autorité (ANSSI, CNIL, un organisme
-- certificateur, un client sous obligation DORA qui audite son prestataire).
-- Les actes les plus sensibles de la plateforme n'étaient nulle part :
-- création et désactivation d'un client, ouverture et fermeture d'un module,
-- changement de tarif, octroi d'un accès à un cabinet partenaire. Ils ne
-- laissaient qu'une ligne de journal applicatif, c'est-à-dire rien :
-- effaçable, non chaîné, non opposable.
--
-- UNE SEULE chaîne, pas une par client. L'éditeur est une entité ; une
-- autorité l'audite en entier. Une chaîne par client rendrait invérifiable
-- l'ordre de deux actes visant deux clients différents — alors que c'est
-- exactement l'enchaînement qu'un auditeur reconstitue.
--
-- `target_tenant_id` est néanmoins porté à part, en plus de
-- (`target_type`, `target_id`), pour que la question « tout ce qui a touché
-- CE client » soit un index et non un balayage. La colonne ne s'appelle pas
-- `tenant_id` À DESSEIN : ce nom déclenche l'isolation par client (filtre
-- Hibernate côté api-core, RLS côté moteur), qui masquerait à l'éditeur les
-- actes visant les autres clients — soit l'inverse de la fonction de cette
-- table.

CREATE TABLE platform_audit_counters (
    scope            VARCHAR(16) PRIMARY KEY,
    last_sequence_no BIGINT      NOT NULL DEFAULT 0,
    CONSTRAINT ck_platform_audit_counter_scope CHECK (scope = 'PLATFORM'),
    CONSTRAINT ck_platform_audit_counter_positive CHECK (last_sequence_no >= 0)
);

COMMENT ON TABLE platform_audit_counters IS
    'Compteur de sequence de la piste d''audit de plateforme. Une seule ligne, verrouillee en ecriture.';

-- La ligne est posée ici et non à la première écriture : un compteur créé à la
-- volée par le code applicatif est un compteur que deux écritures simultanées
-- peuvent créer deux fois.
INSERT INTO platform_audit_counters (scope, last_sequence_no) VALUES ('PLATFORM', 0);

CREATE TABLE platform_audit_events (
    id               UUID PRIMARY KEY,
    sequence_no      BIGINT       NOT NULL,
    occurred_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    recorded_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    actor_type       VARCHAR(32)  NOT NULL,
    actor_user_id    UUID,
    action           VARCHAR(64)  NOT NULL,
    target_type      VARCHAR(64)  NOT NULL,
    target_id        UUID,
    target_tenant_id UUID,
    summary          VARCHAR(512) NOT NULL,
    payload_json     TEXT,
    ip_address       VARCHAR(64),
    user_agent       VARCHAR(256),
    integrity_hash   CHAR(64)     NOT NULL,
    previous_hash    CHAR(64),
    CONSTRAINT uk_platform_audit_seq UNIQUE (sequence_no),
    CONSTRAINT uk_platform_audit_hash UNIQUE (integrity_hash),
    CONSTRAINT ck_platform_audit_actor_type
        CHECK (actor_type IN ('USER', 'SYSTEM', 'SCHEDULER')),
    CONSTRAINT ck_platform_audit_seq_positive CHECK (sequence_no > 0),
    -- Un acteur HUMAIN sans identité n'est pas une piste d'audit : si l'acte
    -- est attribué à une personne, elle est nommée. Les actes de la machine
    -- (SYSTEM, SCHEDULER) n'ont, eux, pas d'utilisateur — et ne doivent pas
    -- s'en inventer un.
    CONSTRAINT ck_platform_audit_human_is_named
        CHECK ((actor_type = 'USER' AND actor_user_id IS NOT NULL)
            OR (actor_type <> 'USER' AND actor_user_id IS NULL))
);

COMMENT ON TABLE platform_audit_events IS
    'Piste d''audit de la plateforme, en ajout seul, chainee par SHA-256. Lisible par le super-administrateur et exportable pour une autorite.';
COMMENT ON COLUMN platform_audit_events.target_tenant_id IS
    'Le client concerne par l''acte, quand il y en a un. Volontairement pas nomme tenant_id : cette table n''est pas isolee par client.';

CREATE INDEX idx_platform_audit_seq ON platform_audit_events (sequence_no);
CREATE INDEX idx_platform_audit_occurred ON platform_audit_events (occurred_at DESC);
CREATE INDEX idx_platform_audit_target_tenant
    ON platform_audit_events (target_tenant_id, sequence_no DESC)
    WHERE target_tenant_id IS NOT NULL;
CREATE INDEX idx_platform_audit_actor ON platform_audit_events (actor_user_id, sequence_no DESC);
CREATE INDEX idx_platform_audit_action ON platform_audit_events (action, sequence_no DESC);

-- ---------------------------------------------------------------------------
-- Ajout seul, en base.
--
-- Même raisonnement que la V157 du moteur de qualité : une chaîne d'empreintes
-- permet de CONSTATER une réécriture, elle ne l'EMPÊCHE pas, et entre la
-- réécriture et sa découverte le journal ment. Un déclencheur ferme la porte,
-- et s'applique même au propriétaire de la base — contrairement à un REVOKE,
-- que le code de provisionnement peut défaire au déploiement suivant.
--
-- Aucune exception ici, pas même l'ancrage : la table n'a pas de colonne
-- d'ancrage. Un événement de plateforme ne se modifie JAMAIS après insertion.
-- ---------------------------------------------------------------------------

CREATE OR REPLACE FUNCTION platform_audit_append_only()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'DELETE' THEN
        RAISE EXCEPTION
            'La piste d''audit de la plateforme est en ajout seul : suppression interdite (evenement %).',
            OLD.id
            USING ERRCODE = '42501';
    END IF;
    RAISE EXCEPTION
        'La piste d''audit de la plateforme est en ajout seul : modification interdite (evenement %).',
        OLD.id
        USING ERRCODE = '42501';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_platform_audit_append_only
    BEFORE UPDATE OR DELETE ON platform_audit_events
    FOR EACH ROW EXECUTE FUNCTION platform_audit_append_only();

COMMENT ON FUNCTION platform_audit_append_only() IS
    'Ajout seul, sans exception : refuse toute suppression et toute modification.';

-- Le compteur s'incrémente légitimement ; ce qui ne doit jamais arriver, c'est
-- qu'il RECULE. Un compteur ramené en arrière rouvre un numéro de séquence déjà
-- employé, donc permet de réécrire l'histoire par l'AJOUT plutôt que par la
-- modification. La contrainte d'unicité refuserait le doublon, mais après la
-- bascule : mieux vaut refuser la bascule.
CREATE OR REPLACE FUNCTION platform_audit_counter_monotonic()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.last_sequence_no < OLD.last_sequence_no THEN
        RAISE EXCEPTION
            'Le compteur de la piste d''audit de plateforme ne recule pas (% -> %).',
            OLD.last_sequence_no, NEW.last_sequence_no
            USING ERRCODE = '42501';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_platform_audit_counter_monotonic
    BEFORE UPDATE ON platform_audit_counters
    FOR EACH ROW EXECUTE FUNCTION platform_audit_counter_monotonic();

CREATE OR REPLACE FUNCTION platform_audit_counter_no_delete()
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION
        'Le compteur de la piste d''audit de plateforme ne se supprime pas.'
        USING ERRCODE = '42501';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_platform_audit_counter_no_delete
    BEFORE DELETE ON platform_audit_counters
    FOR EACH ROW EXECUTE FUNCTION platform_audit_counter_no_delete();

COMMENT ON FUNCTION platform_audit_counter_monotonic() IS
    'Le compteur de sequence de la piste d''audit de plateforme ne recule jamais.';
