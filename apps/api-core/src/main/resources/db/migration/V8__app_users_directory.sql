-- =============================================================================
-- V8 : annuaire en lecture restreinte (ADR 0091)
--
-- display_name : le nom saisi à l'invitation, montré à tous les membres du
--                client pour désigner un responsable. NULL pour un membre
--                enregistré sans nom : l'annuaire montre alors son adresse.
-- search_key   : nom et adresse sans accents ni majuscules, calculés par
--                l'application à chaque écriture. La recherche « contient »
--                porte sur cette seule colonne ; aucune extension PostgreSQL
--                (unaccent, pg_trgm) n'est requise, le rôle des migrations
--                d'une installation cliente ne pouvant pas toujours en créer.
-- =============================================================================

ALTER TABLE app_users ADD COLUMN display_name VARCHAR(200);
ALTER TABLE app_users ADD COLUMN search_key   VARCHAR(512) NOT NULL DEFAULT '';

-- Les membres existants n'ont pas de nom enregistré : leur clé est leur adresse
-- (ASCII par contrainte chk_app_users_email, la mise en minuscules suffit).
-- Le déclencheur updated_at est suspendu le temps du remplissage : calculer une
-- clé technique n'est pas une modification du membre.
ALTER TABLE app_users DISABLE TRIGGER trg_app_users_updated_at;
UPDATE app_users SET search_key = lower(email);
ALTER TABLE app_users ENABLE TRIGGER trg_app_users_updated_at;

-- Tri de l'annuaire : membres actifs d'un client, par nom affiché.
CREATE INDEX idx_app_users_directory
    ON app_users (tenant_id, lower(coalesce(display_name, email)))
    WHERE active = TRUE;

COMMENT ON COLUMN app_users.display_name IS 'Nom affiché dans l''annuaire du client (ADR 0091)';
COMMENT ON COLUMN app_users.search_key IS 'Nom et adresse normalisés pour la recherche, calculés par l''application';
