<!-- Conforme à la Definition of Done (CLAUDE.md §20) -->

## Résumé

<!-- Décris ce que fait la PR en 1-3 phrases. -->

## Type de changement

- [ ] feat: nouvelle fonctionnalité
- [ ] fix: correction de bug
- [ ] refactor: refactoring sans changement fonctionnel
- [ ] chore: outillage, dépendances, CI
- [ ] docs: documentation uniquement
- [ ] test: ajout/modification de tests
- [ ] security: correctif sécurité

## Module impacté

<!-- pdca / ishikawa / fives / capa / docs / audit / standards-hub / core / infra / ... -->

## Checklist Definition of Done

- [ ] Tests unitaires + slice + intégration ajoutés/mis à jour
- [ ] Couverture JaCoCo ≥ 85 % lignes / 75 % branches (CI verte)
- [ ] Pas de CVE Critique/Haute dans les dépendances (OWASP DC)
- [ ] Multi-tenancy respecté : `tenant_id` toujours issu du JWT (jamais du body)
- [ ] Validation des entrées (Jakarta + OpenAPI)
- [ ] Logging structuré, aucune PII en clair
- [ ] Feature flag défini si feature à risque
- [ ] Migration Flyway versionnée (V\<n>__...) si schéma modifié
- [ ] Documentation Wiki (technique + utilisateur) prévue si feature publique
- [ ] ADR rédigé si décision structurante

## Lien tickets / issues

<!-- Closes #<num>, Refs #<num> -->

## Notes pour les reviewers

<!-- Points sensibles, choix discutables, performances à valider, ... -->
