-- LE SECOND REGARD sur la clôture d'un écart.
--
-- Clore un écart, c'est déclarer « plus rien à faire ici ». Jusqu'ici, la même
-- personne pouvait écrire la résolution puis la valider : l'organisation n'avait
-- aucune trace d'un contrôle indépendant, alors que c'est précisément ce que
-- l'ISO 9001 §10.2 attend de la vérification de l'efficacité, et ce que tout
-- auditeur demande à voir.
--
-- Trois paires de colonnes, pour trois questions distinctes :
--   * qui a résolu  -- sans elle, la règle du second regard ne peut pas exister,
--   * qui a clos    -- la trace du second regard lui-même,
--   * qui valide    -- la personne DÉSIGNÉE pour ce regard sur cet écart, quand
--                      ce n'est pas un rôle de pilotage qui s'en charge.
--
-- Les identifiants sont ceux de l'annuaire (le `sub` du jeton), jamais un texte
-- libre ; le nom est copié au moment du geste pour que la fiche reste lisible
-- même si la personne quitte l'organisation.
ALTER TABLE non_conformities
    ADD COLUMN resolved_by_id   UUID,
    ADD COLUMN resolved_by_name VARCHAR(255),
    ADD COLUMN closed_by_id     UUID,
    ADD COLUMN closed_by_name   VARCHAR(255),
    ADD COLUMN validator_id     UUID,
    ADD COLUMN validator_name   VARCHAR(255);

-- « Quels écarts attendent MON regard ? » est la question du valideur désigné.
-- Partiel : la très grande majorité des écarts n'en désigne aucun, et l'index
-- n'a pas à les porter.
CREATE INDEX idx_non_conformities_validator
    ON non_conformities (tenant_id, validator_id)
    WHERE validator_id IS NOT NULL;
