-- Rappel d'échéance des audits planifiés (§4.4).
--
-- Un audit se prépare : convoquer l'audité, réunir les preuves, bloquer les
-- agendas. Prévenir la veille ne sert à rien. Le rappel part donc à l'approche
-- de l'échéance (30 jours par défaut, réglable sans redéploiement).
--
-- POURQUOI UNE MARQUE EN BASE ET NON UNE MÉMOIRE DE PROCESSUS : l'engine tourne
-- en plusieurs répliques (values.yaml : replicas 2) et chaque réplique porte son
-- propre ordonnanceur. Deux répliques repèrent le même audit à la même minute.
-- Sans arbitre partagé, le rappel part deux fois — et un rappel doublé fait
-- douter de tous les autres. La colonne ci-dessous EST cet arbitre : le service
-- la pose par un UPDATE conditionnel (« ... WHERE reminder_sent_at IS NULL »),
-- que la base sérialise ; une seule réplique voit une ligne affectée, les autres
-- en voient zéro et passent leur chemin. Ce n'est pas une trace a posteriori,
-- c'est le verrou lui-même.
ALTER TABLE audit_plans
    -- Destinataire du courriel de rappel. Facultatif : sans lui, la notification
    -- interne part quand même. Nullable plutôt qu'obligatoire — l'engine ne
    -- dispose d'aucun annuaire capable de traduire lead_auditor_id en adresse,
    -- et fabriquer une adresse à partir d'un UUID enverrait dans le vide.
    -- 320 = 64 (partie locale) + 1 (@) + 255 (domaine), borne RFC 5321.
    ADD COLUMN reminder_email   VARCHAR(320),
    ADD COLUMN reminder_sent_at TIMESTAMPTZ;

-- L'ordonnanceur balaie TOUS les tenants (il tourne hors requête, sans contexte
-- tenant) : l'index utile n'est donc pas préfixé par tenant_id. Partiel sur
-- « pas encore rappelé » : les audits déjà rappelés — la grande majorité au fil
-- du temps — sortent de l'index au lieu de l'alourdir indéfiniment.
CREATE INDEX idx_audit_plans_reminder_due
    ON audit_plans (scheduled_date)
    WHERE reminder_sent_at IS NULL;
