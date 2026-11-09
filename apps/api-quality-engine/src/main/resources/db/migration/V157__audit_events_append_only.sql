-- V157 : le journal d'audit devient inscriptible seulement en ajout.
--
-- Jusqu'ici, la chaîne SHA-256 (V26) permettait de DÉTECTER une réécriture ;
-- elle ne l'EMPÊCHAIT pas. Le rôle applicatif porte `UPDATE` et `DELETE` sur
-- toutes les tables, et personne ne lance la vérification de chaîne
-- périodiquement : une ligne modifiée pouvait donc passer inaperçue
-- indéfiniment. Un journal qu'on peut réécrire sans être arrêté n'est pas une
-- piste d'audit, c'est un historique.
--
-- La garantie est posée ici, en base, et non dans le code applicatif : le code
-- se contourne en ouvrant une session SQL, la base non. Elle est posée par un
-- DÉCLENCHEUR et non par un REVOKE, parce que `RlsSchema` ré-accorde
-- `SELECT, INSERT, UPDATE, DELETE ON ALL TABLES` au rôle applicatif après
-- CHAQUE migration (callback Flyway) : un REVOKE écrit ici serait défait au
-- déploiement suivant, silencieusement. Le déclencheur, lui, ne dépend
-- d'aucun droit et s'applique même au propriétaire.
--
-- Une seule écriture reste permise après l'insertion : `blockchain_tx_ref`,
-- qui passe de NULL à sa valeur quand l'événement est ancré. C'est une
-- information sur la PREUVE, pas sur le FAIT : elle n'entre pas dans
-- l'empreinte (cf. AuditEventHasher), donc la renseigner ne peut pas réécrire
-- l'histoire. Elle ne peut être posée qu'une fois.

CREATE OR REPLACE FUNCTION audit_events_append_only()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'DELETE' THEN
        RAISE EXCEPTION
            'Le journal d''audit est en ajout seul : suppression interdite (evenement %).',
            OLD.id
            USING ERRCODE = '42501';
    END IF;

    -- UPDATE : tout doit être identique, sauf l'ancrage qui se pose une fois.
    IF OLD.blockchain_tx_ref IS NOT NULL
       AND NEW.blockchain_tx_ref IS DISTINCT FROM OLD.blockchain_tx_ref THEN
        RAISE EXCEPTION
            'L''ancrage d''un evenement d''audit ne se pose qu''une fois (evenement %).',
            OLD.id
            USING ERRCODE = '42501';
    END IF;

    IF NEW.id                IS DISTINCT FROM OLD.id
       OR NEW.tenant_id      IS DISTINCT FROM OLD.tenant_id
       OR NEW.sequence_no    IS DISTINCT FROM OLD.sequence_no
       OR NEW.occurred_at    IS DISTINCT FROM OLD.occurred_at
       OR NEW.recorded_at    IS DISTINCT FROM OLD.recorded_at
       OR NEW.actor_type     IS DISTINCT FROM OLD.actor_type
       OR NEW.actor_user_id  IS DISTINCT FROM OLD.actor_user_id
       OR NEW.action         IS DISTINCT FROM OLD.action
       OR NEW.resource_type  IS DISTINCT FROM OLD.resource_type
       OR NEW.resource_id    IS DISTINCT FROM OLD.resource_id
       OR NEW.summary        IS DISTINCT FROM OLD.summary
       OR NEW.payload_json   IS DISTINCT FROM OLD.payload_json
       OR NEW.ip_address     IS DISTINCT FROM OLD.ip_address
       OR NEW.user_agent     IS DISTINCT FROM OLD.user_agent
       OR NEW.integrity_hash IS DISTINCT FROM OLD.integrity_hash
       OR NEW.previous_hash  IS DISTINCT FROM OLD.previous_hash THEN
        RAISE EXCEPTION
            'Le journal d''audit est en ajout seul : seul l''ancrage peut etre pose apres coup (evenement %).',
            OLD.id
            USING ERRCODE = '42501';
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_audit_events_append_only
    BEFORE UPDATE OR DELETE ON audit_events
    FOR EACH ROW EXECUTE FUNCTION audit_events_append_only();

COMMENT ON FUNCTION audit_events_append_only() IS
    'Ajout seul : refuse toute suppression et toute modification hors pose unique de blockchain_tx_ref.';

-- Le compteur de séquence, lui, s'incrémente légitimement. Ce qui ne doit
-- jamais arriver, c'est qu'il RECULE : un compteur ramené en arrière
-- permettrait de réinsérer un numéro de séquence déjà utilisé, donc de
-- réécrire l'histoire par l'ajout plutôt que par la modification. La
-- contrainte d'unicité (tenant_id, sequence_no) refuserait le doublon, mais
-- après la bascule du compteur ; mieux vaut refuser la bascule elle-même.
CREATE OR REPLACE FUNCTION audit_counter_monotonic()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.last_sequence_no < OLD.last_sequence_no THEN
        RAISE EXCEPTION
            'Le compteur du journal d''audit ne recule pas (client %, % -> %).',
            OLD.tenant_id, OLD.last_sequence_no, NEW.last_sequence_no
            USING ERRCODE = '42501';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_audit_counter_monotonic
    BEFORE UPDATE ON audit_event_counters
    FOR EACH ROW EXECUTE FUNCTION audit_counter_monotonic();

CREATE OR REPLACE FUNCTION audit_counter_no_delete()
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION
        'Le compteur du journal d''audit ne se supprime pas (client %).', OLD.tenant_id
        USING ERRCODE = '42501';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_audit_counter_no_delete
    BEFORE DELETE ON audit_event_counters
    FOR EACH ROW EXECUTE FUNCTION audit_counter_no_delete();

COMMENT ON FUNCTION audit_counter_monotonic() IS
    'Le compteur de sequence du journal d''audit ne recule jamais.';
