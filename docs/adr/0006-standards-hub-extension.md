# ADR 0006 — Standards Hub extension (P4)

**Statut** : Accepted | **Date** : 2026-05-17 | **Phase** : P4

## Contexte

Le Standards Hub (CLAUDE.md §8) est l'un des piliers différenciateurs de QualitOS : aucun concurrent n'embarque une couverture aussi structurée, actionnable et multi-secteurs. Sprint P1 a livré ISO 9001 + ISO 27001 ; sprint P2 a ajouté ISO 14001 + 45001 + 22301 (HLS). P4 doit étendre à 6 normes additionnelles couvrant les secteurs cibles de l'Industry Packs P3/P4.

## Décision

Modéliser chaque nouvelle norme selon le **schéma universel §8.3** (entités JPA `Standard`, `Section`, `Clause`, `Requirement`, `DocumentTemplate`, `CertificationPath`) déjà en place. Ajouter via **Flyway V52** :

1. IATF 16949:2016 (Automobile)
2. AS9100D (Aéronautique)
3. ISO 13485:2016 (Dispositifs médicaux)
4. FDA 21 CFR Part 11 (Signatures électroniques)
5. ISO 22000:2018 / HACCP (Agro)
6. DORA (UE) 2022/2554 (Finance)

Le code ajouté respecte la **Clean Architecture hexagonale** :
- `domain/standards/` : POJOs (`Norm`, `Clause`, `Requirement`, `CertificationPath`). Zéro Spring/JPA.
- `application/standards/usecase/` : `RegisterNormUseCase`, `GetNormDossierUseCase`, `GetCoverageMatrixUseCase`.
- `infrastructure/standards/persistence/` : entités JPA + mappers MapStruct + migrations.
- `presentation/standards/rest/` : controllers + DTOs.

ArchUnit test enforce la dependency rule (`domain` ne dépend d'aucune lib externe sauf JDK).

## Conséquences

### Positives
- Mêmes API REST et UX que pour les 5 normes existantes — aucun changement client.
- 7 normes supplémentaires (cumul = 12 normes) couvrent les domaines cibles V1.
- IMS (Integrated Management Systems) : la table `norm_clause_mapping` (V52) permet de mutualiser les preuves entre normes HLS — gain estimé 30-50 % d'effort pour les tenants multi-certifiés.

### Négatives
- Seeds Flyway lourds (env. 2 000 lignes SQL pour 6 normes × ≥15 clauses × ≥3 requirements). Atténuation : tests d'intégration Testcontainers vérifient la cohérence post-migration.
- Templates documentaires markdown : qualité métier à valider par consultants experts secteur — itération continue.

### Alternatives rejetées
- **Charger les normes depuis YAML au démarrage** : rejette car les seeds Flyway garantissent l'intégrité référentielle (FK) et la reproductibilité environnements.
- **Import dynamique via marketplace** : prévu en sprint P5 (cf. `marketplace_pack`) ; les normes "core" restent seeded pour offrir la couverture out-of-the-box.

## Suivi

- Sprint P5+ : ajout normes "long-tail" via marketplace (NADCAP, VDA 6.3, ICH Q7-Q14, BRCGS, etc.).
- Veille normative (§8.10) : alerter les tenants impactés via Webhooks lors des révisions.
