-- Standards Hub — vague 6 (CLAUDE.md §8.2) : 5 référentiels de management (gouvernance & conformité).
--   iso-37001  : ISO 37001:2025 — systèmes de management anti-corruption (certifiable, cycle 3 ans)
--   iso-37301  : ISO 37301:2021 — systèmes de management de la conformité (certifiable, cycle 3 ans)
--   iso-19011  : ISO 19011:2018 — lignes directrices pour l'audit des SM (NON certifiable)
--   iso-10002  : ISO 10002:2018 — traitement des réclamations clients (lignes directrices, NON certifiable)
--   iso-26000  : ISO 26000:2010 — responsabilité sociétale (lignes directrices, NON certifiable)
-- Même format que V72 : catalogue platform-level, UUID déterministes, préfixe 'a'
-- (standards a000000n, sections an00000s, clauses a[a-d|9]…, paths ae…, stages af via FK).
-- Note UUID : pour éviter toute collision avec les paths (ae), les clauses de la 5e
-- norme (ISO 26000) portent le préfixe 'a9' (aa-ad couvrent les normes 1 à 4).

-- ============================================================================
-- ISO 37001:2025 — Systèmes de management anti-corruption
-- ============================================================================

INSERT INTO standards (id, code, full_name, publisher, current_version, publication_date,
    family, applicable_industries, description, certification_body_required,
    recertification_cycle_months, related_norm_codes, status, created_at, updated_at)
VALUES (
    'a0000001-0000-0000-0000-000000000001',
    'iso-37001',
    'ISO 37001:2025 — Systèmes de management anti-corruption — Exigences et recommandations de mise en œuvre',
    'ISO',
    '2025',
    DATE '2025-02-01',
    'HLS',
    'all',
    'Norme de système de management anti-corruption (SMAC) construite sur la structure de haut niveau (HLS) commune aux normes ISO. Elle exige une politique anti-corruption portée par l''organe de gouvernance et la direction, une fonction de conformité anti-corruption dotée de l''autorité et de l''indépendance nécessaires, une évaluation des risques de corruption proportionnée, une due diligence sur les tiers (partenaires, fournisseurs, intermédiaires) et les opérations à risque, la maîtrise des cadeaux, invitations, dons et avantages, des contrôles financiers et non financiers, des canaux d''alerte (signalement) protégeant les lanceurs d''alerte, ainsi que des enquêtes internes et des actions correctives en cas de corruption avérée ou suspectée. La conformité est certifiable par un organisme de certification accrédité, avec un cycle de certification de 3 ans et des audits de surveillance annuels.',
    TRUE,
    NULL,
    'iso-37301,iso-9001',
    'PUBLISHED', now(), now()
);

INSERT INTO standard_sections (id, standard_id, code, title, description, order_index) VALUES
    ('a1000001-0000-0000-0000-000000000001', 'a0000001-0000-0000-0000-000000000001', '4', 'Contexte de l''organisme', 'Compréhension du contexte, des parties intéressées et évaluation des risques de corruption.', 1),
    ('a1000001-0000-0000-0000-000000000002', 'a0000001-0000-0000-0000-000000000001', '5', 'Leadership', 'Engagement de la gouvernance et de la direction, politique anti-corruption et fonction de conformité.', 2),
    ('a1000001-0000-0000-0000-000000000003', 'a0000001-0000-0000-0000-000000000001', '7', 'Support', 'Ressources, compétences, sensibilisation et formation anti-corruption.', 3),
    ('a1000001-0000-0000-0000-000000000004', 'a0000001-0000-0000-0000-000000000001', '8', 'Réalisation des activités opérationnelles', 'Due diligence, contrôles, cadeaux/invitations, alertes et enquêtes.', 4),
    ('a1000001-0000-0000-0000-000000000005', 'a0000001-0000-0000-0000-000000000001', '9', 'Évaluation des performances', 'Surveillance, audit interne et revue par la fonction de conformité et la direction.', 5);

INSERT INTO standard_clauses (id, section_id, code, title, description, order_index) VALUES
    ('aa000001-0000-0000-0000-000000000001', 'a1000001-0000-0000-0000-000000000001', '4.5', 'Évaluation des risques de corruption', 'Identification, analyse et évaluation périodique des risques de corruption.', 1),
    ('aa000001-0000-0000-0000-000000000002', 'a1000001-0000-0000-0000-000000000002', '5.2', 'Politique anti-corruption', 'Établissement et communication d''une politique anti-corruption interdisant la corruption.', 2),
    ('aa000001-0000-0000-0000-000000000003', 'a1000001-0000-0000-0000-000000000002', '5.3', 'Fonction de conformité anti-corruption', 'Désignation d''une fonction de conformité dotée d''autorité et d''indépendance.', 3),
    ('aa000001-0000-0000-0000-000000000004', 'a1000001-0000-0000-0000-000000000003', '7.3', 'Sensibilisation & formation', 'Sensibilisation et formation anti-corruption adaptées aux niveaux de risque.', 4),
    ('aa000001-0000-0000-0000-000000000005', 'a1000001-0000-0000-0000-000000000004', '8.2', 'Due diligence', 'Diligence raisonnable sur les transactions, projets, partenaires et personnels à risque.', 5),
    ('aa000001-0000-0000-0000-000000000006', 'a1000001-0000-0000-0000-000000000004', '8.4', 'Contrôles financiers & non financiers', 'Mise en œuvre de contrôles financiers et non financiers anti-corruption.', 6),
    ('aa000001-0000-0000-0000-000000000007', 'a1000001-0000-0000-0000-000000000004', '8.7', 'Cadeaux, invitations, dons & avantages', 'Maîtrise des cadeaux, marques d''hospitalité, dons et avantages similaires.', 7),
    ('aa000001-0000-0000-0000-000000000008', 'a1000001-0000-0000-0000-000000000004', '8.9', 'Remontée de préoccupations (alertes)', 'Canaux de signalement protégeant les lanceurs d''alerte contre les représailles.', 8),
    ('aa000001-0000-0000-0000-000000000009', 'a1000001-0000-0000-0000-000000000004', '8.10', 'Enquêtes & traitement de la corruption', 'Enquêtes internes et traitement des cas de corruption avérés ou suspectés.', 9);

INSERT INTO standard_requirements (clause_id, code, text, obligation, evidence_types, measurable_criteria, risk_if_missing, order_index) VALUES
    ('aa000001-0000-0000-0000-000000000001', '4.5.1', 'L''organisme doit réaliser une évaluation des risques de corruption identifiant et hiérarchisant les risques liés à ses activités, partenaires et zones géographiques.', 'MUST', 'DOCUMENT,AUDIT', 'Cartographie des risques de corruption documentée et revue au moins annuellement ou en cas de changement significatif', 'CRITICAL', 1),
    ('aa000001-0000-0000-0000-000000000002', '5.2.1', 'La direction doit établir, approuver et communiquer une politique anti-corruption interdisant toute forme de corruption et exigeant le respect des lois applicables.', 'MUST', 'DOCUMENT', 'Politique anti-corruption signée par la direction et diffusée à 100 % du personnel et aux partenaires concernés', 'CRITICAL', 2),
    ('aa000001-0000-0000-0000-000000000003', '5.3.1', 'L''organisme doit désigner une fonction de conformité anti-corruption disposant de l''autorité, des ressources et de l''indépendance appropriées.', 'MUST', 'DOCUMENT,AUDIT', 'Fonction de conformité nommée par écrit ; accès direct à l''organe de gouvernance documenté', 'HIGH', 3),
    ('aa000001-0000-0000-0000-000000000004', '7.3.1', 'L''organisme doit assurer la sensibilisation et la formation anti-corruption du personnel selon son exposition au risque.', 'MUST', 'TRAINING_RECORD,DOCUMENT', 'Taux de complétion de la formation anti-corruption ≥ 95 % des personnels exposés ; recyclage périodique', 'HIGH', 4),
    ('aa000001-0000-0000-0000-000000000005', '8.2.1', 'L''organisme doit conduire une due diligence proportionnée aux risques sur les transactions, projets et tiers à risque avant engagement.', 'MUST', 'DOCUMENT,AUDIT', 'Due diligence formalisée pour 100 % des tiers à risque élevé ; résultats tracés et conservés', 'CRITICAL', 5),
    ('aa000001-0000-0000-0000-000000000006', '8.4.1', 'L''organisme doit mettre en œuvre des contrôles financiers et non financiers visant à réduire le risque de corruption.', 'MUST', 'DOCUMENT,AUDIT', 'Contrôles financiers (séparation des tâches, double signature) et non financiers documentés et testés', 'HIGH', 6),
    ('aa000001-0000-0000-0000-000000000007', '8.7.1', 'L''organisme doit définir des règles de maîtrise des cadeaux, invitations, dons et avantages, avec seuils et procédure d''enregistrement.', 'MUST', 'DOCUMENT,KPI_RECORD', 'Procédure cadeaux/invitations avec seuils chiffrés ; registre des cadeaux tenu et revu', 'MEDIUM', 7),
    ('aa000001-0000-0000-0000-000000000008', '8.9.1', 'L''organisme doit mettre en place des canaux permettant de signaler tout soupçon de corruption en toute confidentialité et sans crainte de représailles.', 'MUST', 'DOCUMENT,AUDIT', 'Canal d''alerte opérationnel ; mesures anti-représailles formalisées ; signalements tracés et traités', 'CRITICAL', 8),
    ('aa000001-0000-0000-0000-000000000009', '8.10.1', 'L''organisme doit enquêter sur les cas de corruption avérés ou suspectés et engager les actions correctives appropriées.', 'MUST', 'DOCUMENT,CAPA', 'Procédure d''enquête en place ; 100 % des alertes recevables instruites ; actions correctives suivies', 'HIGH', 9);

INSERT INTO standard_certification_paths (id, standard_id, estimated_duration_months_min, estimated_duration_months_max,
    estimated_cost_eur_min, estimated_cost_eur_max, difficulty_level,
    surveillance_audit_frequency, recertification_cycle_years, notes)
VALUES ('ae000001-0000-0000-0000-000000000001', 'a0000001-0000-0000-0000-000000000001',
    9, 18, 15000, 70000, 4, 'annual', 3,
    'Certification par un organisme de certification accrédité (cycle de 3 ans, audits de surveillance annuels). Norme alignée HLS, facilement intégrable avec ISO 9001 et ISO 37301. Synergie directe avec les modules Audit, Document Control, Risk (cartographie des risques de corruption) et Training de QualitOS.');

INSERT INTO standard_certification_stages (certification_path_id, stage_number, name, description, typical_duration_weeks, deliverables, actors, qualitos_modules, order_index) VALUES
    ('ae000001-0000-0000-0000-000000000001', 1, 'Engagement & politique anti-corruption', 'Engagement de la gouvernance, définition du périmètre du SMAC et adoption de la politique anti-corruption.', '3-5', 'Politique anti-corruption signée, périmètre du SMAC', 'Direction, Organe de gouvernance', 'Document Control', 1),
    ('ae000001-0000-0000-0000-000000000001', 2, 'Évaluation des risques de corruption', 'Cartographie des risques de corruption par activité, tiers et géographie, et définition des contrôles.', '4-8', 'Cartographie des risques de corruption, plan de traitement', 'Fonction conformité, Risk Manager', 'Risk, Ishikawa', 2),
    ('ae000001-0000-0000-0000-000000000001', 3, 'Due diligence & contrôles', 'Mise en place de la due diligence des tiers, des contrôles financiers/non financiers et de la maîtrise des cadeaux.', '6-12', 'Procédure de due diligence, registre des cadeaux, matrice de contrôles', 'Fonction conformité, Achats, Finance', 'Document Control, Risk', 3),
    ('ae000001-0000-0000-0000-000000000001', 4, 'Sensibilisation & dispositif d''alerte', 'Formation anti-corruption du personnel et mise en place des canaux d''alerte et de la procédure d''enquête.', '4-8', 'Plan de formation, canal d''alerte, procédure d''enquête', 'Fonction conformité, RH', 'Training, Document Control', 4),
    ('ae000001-0000-0000-0000-000000000001', 5, 'Audit interne & revue de direction', 'Audit interne du SMAC et revue par la fonction de conformité et la direction.', '3-5', 'Rapport d''audit interne, compte rendu de revue de direction', 'Auditeur interne, Direction', 'Audit, PDCA', 5),
    ('ae000001-0000-0000-0000-000000000001', 6, 'Audit de certification (étapes 1 & 2)', 'Audit documentaire puis audit sur site par l''organisme certificateur, levée des non-conformités.', '4-8', 'Certificat ISO 37001', 'Organisme certificateur', 'Standards Hub, CAPA', 6);

-- ============================================================================
-- ISO 37301:2021 — Systèmes de management de la conformité
-- ============================================================================

INSERT INTO standards (id, code, full_name, publisher, current_version, publication_date,
    family, applicable_industries, description, certification_body_required,
    recertification_cycle_months, related_norm_codes, status, created_at, updated_at)
VALUES (
    'a0000002-0000-0000-0000-000000000002',
    'iso-37301',
    'ISO 37301:2021 — Systèmes de management de la conformité — Exigences et recommandations de mise en œuvre',
    'ISO',
    '2021',
    DATE '2021-04-13',
    'HLS',
    'all',
    'Norme certifiable de système de management de la conformité (SMC) construite sur la structure de haut niveau (HLS). Elle aide l''organisme à identifier ses obligations de conformité (lois, règlements, contrats, codes de conduite, engagements volontaires), à évaluer les risques de non-conformité, à développer une culture de la conformité portée par la gouvernance, à doter la fonction de conformité de l''autorité et de l''indépendance nécessaires, à former et sensibiliser le personnel, à mettre en place des contrôles et des canaux de remontée des préoccupations, à mesurer la performance par des indicateurs, et à traiter les non-conformités. La certification est délivrée par un organisme accrédité avec un cycle de 3 ans et des audits de surveillance annuels.',
    TRUE,
    NULL,
    'iso-37001,iso-31000',
    'PUBLISHED', now(), now()
);

INSERT INTO standard_sections (id, standard_id, code, title, description, order_index) VALUES
    ('a2000002-0000-0000-0000-000000000001', 'a0000002-0000-0000-0000-000000000002', '4', 'Contexte de l''organisme', 'Obligations de conformité, parties intéressées et évaluation des risques de conformité.', 1),
    ('a2000002-0000-0000-0000-000000000002', 'a0000002-0000-0000-0000-000000000002', '5', 'Leadership', 'Gouvernance, culture de la conformité, politique et fonction de conformité.', 2),
    ('a2000002-0000-0000-0000-000000000003', 'a0000002-0000-0000-0000-000000000002', '7', 'Support', 'Ressources, compétences, sensibilisation, formation et communication.', 3),
    ('a2000002-0000-0000-0000-000000000004', 'a0000002-0000-0000-0000-000000000002', '8', 'Réalisation des activités opérationnelles', 'Contrôles, remontée des préoccupations et traitement des non-conformités.', 4),
    ('a2000002-0000-0000-0000-000000000005', 'a0000002-0000-0000-0000-000000000002', '9', 'Évaluation des performances', 'Indicateurs de conformité, surveillance, audit interne et revue de direction.', 5);

INSERT INTO standard_clauses (id, section_id, code, title, description, order_index) VALUES
    ('ab000002-0000-0000-0000-000000000001', 'a2000002-0000-0000-0000-000000000001', '4.5', 'Obligations de conformité', 'Identification et tenue à jour des obligations de conformité de l''organisme.', 1),
    ('ab000002-0000-0000-0000-000000000002', 'a2000002-0000-0000-0000-000000000001', '4.6', 'Évaluation des risques de conformité', 'Identification et évaluation des risques de non-conformité.', 2),
    ('ab000002-0000-0000-0000-000000000003', 'a2000002-0000-0000-0000-000000000002', '5.1', 'Leadership & culture de conformité', 'Engagement de la gouvernance et promotion d''une culture de la conformité.', 3),
    ('ab000002-0000-0000-0000-000000000004', 'a2000002-0000-0000-0000-000000000002', '5.2', 'Politique de conformité', 'Établissement et communication d''une politique de conformité.', 4),
    ('ab000002-0000-0000-0000-000000000005', 'a2000002-0000-0000-0000-000000000002', '5.3', 'Fonction de conformité', 'Désignation d''une fonction de conformité dotée d''autorité et d''indépendance.', 5),
    ('ab000002-0000-0000-0000-000000000006', 'a2000002-0000-0000-0000-000000000003', '7.3', 'Sensibilisation & formation', 'Sensibilisation et formation à la conformité du personnel.', 6),
    ('ab000002-0000-0000-0000-000000000007', 'a2000002-0000-0000-0000-000000000004', '8.3', 'Remontée des préoccupations', 'Canaux permettant de remonter les préoccupations de conformité.', 7),
    ('ab000002-0000-0000-0000-000000000008', 'a2000002-0000-0000-0000-000000000004', '8.4', 'Processus d''enquête', 'Traitement et investigation des cas de non-conformité.', 8),
    ('ab000002-0000-0000-0000-000000000009', 'a2000002-0000-0000-0000-000000000005', '9.1', 'Surveillance & indicateurs', 'Surveillance, mesure et indicateurs de performance de la conformité.', 9);

INSERT INTO standard_requirements (clause_id, code, text, obligation, evidence_types, measurable_criteria, risk_if_missing, order_index) VALUES
    ('ab000002-0000-0000-0000-000000000001', '4.5.1', 'L''organisme doit identifier systématiquement ses obligations de conformité et les tenir à jour.', 'MUST', 'DOCUMENT,AUDIT', 'Registre des obligations de conformité établi et revu au moins annuellement', 'CRITICAL', 1),
    ('ab000002-0000-0000-0000-000000000002', '4.6.1', 'L''organisme doit évaluer les risques de non-conformité associés à ses obligations et activités.', 'MUST', 'DOCUMENT,AUDIT', 'Cartographie des risques de conformité documentée ; niveaux de risque et traitements définis', 'HIGH', 2),
    ('ab000002-0000-0000-0000-000000000003', '5.1.1', 'La gouvernance et la direction doivent promouvoir activement une culture de la conformité au sein de l''organisme.', 'MUST', 'DOCUMENT,TRAINING_RECORD', 'Actions de promotion de la culture conformité tracées ; indicateurs de culture suivis', 'HIGH', 3),
    ('ab000002-0000-0000-0000-000000000004', '5.2.1', 'L''organisme doit établir et communiquer une politique de conformité approuvée par la direction.', 'MUST', 'DOCUMENT', 'Politique de conformité signée et diffusée à 100 % du personnel', 'HIGH', 4),
    ('ab000002-0000-0000-0000-000000000005', '5.3.1', 'L''organisme doit désigner une fonction de conformité dotée de l''autorité, des ressources et de l''indépendance appropriées.', 'MUST', 'DOCUMENT,AUDIT', 'Fonction de conformité nommée ; accès direct à la gouvernance documenté', 'CRITICAL', 5),
    ('ab000002-0000-0000-0000-000000000006', '7.3.1', 'L''organisme doit sensibiliser et former le personnel à ses obligations de conformité.', 'MUST', 'TRAINING_RECORD,DOCUMENT', 'Taux de complétion de la formation conformité ≥ 95 % ; recyclage périodique planifié', 'MEDIUM', 6),
    ('ab000002-0000-0000-0000-000000000007', '8.3.1', 'L''organisme doit établir des processus permettant la remontée des préoccupations de conformité, de manière confidentielle et sans représailles.', 'MUST', 'DOCUMENT,AUDIT', 'Canal de remontée opérationnel ; mesures anti-représailles définies ; remontées tracées', 'HIGH', 7),
    ('ab000002-0000-0000-0000-000000000008', '8.4.1', 'L''organisme doit disposer d''un processus d''enquête sur les cas de non-conformité avérés ou suspectés.', 'MUST', 'DOCUMENT,CAPA', 'Procédure d''enquête en place ; non-conformités instruites et actions correctives suivies', 'HIGH', 8),
    ('ab000002-0000-0000-0000-000000000009', '9.1.1', 'L''organisme doit surveiller et mesurer la performance de la conformité au moyen d''indicateurs définis.', 'MUST', 'KPI_RECORD,DOCUMENT', 'Indicateurs de conformité définis (formule, seuil, propriétaire) et suivis périodiquement', 'MEDIUM', 9),
    ('ab000002-0000-0000-0000-000000000009', '9.2', 'L''organisme doit réaliser des audits internes planifiés du système de management de la conformité.', 'MUST', 'AUDIT,DOCUMENT', 'Programme d''audit interne couvrant 100 % du SMC sur le cycle ; écarts traités', 'HIGH', 10);

INSERT INTO standard_certification_paths (id, standard_id, estimated_duration_months_min, estimated_duration_months_max,
    estimated_cost_eur_min, estimated_cost_eur_max, difficulty_level,
    surveillance_audit_frequency, recertification_cycle_years, notes)
VALUES ('ae000002-0000-0000-0000-000000000002', 'a0000002-0000-0000-0000-000000000002',
    9, 18, 15000, 65000, 4, 'annual', 3,
    'Certification par un organisme de certification accrédité (cycle de 3 ans, audits de surveillance annuels). Norme alignée HLS, mutualisable avec ISO 37001 et ISO 31000 (gestion du risque). Synergie avec les modules Document Control, Audit, Risk, Training et KPI (indicateurs de conformité) de QualitOS.');

INSERT INTO standard_certification_stages (certification_path_id, stage_number, name, description, typical_duration_weeks, deliverables, actors, qualitos_modules, order_index) VALUES
    ('ae000002-0000-0000-0000-000000000002', 1, 'Cadrage & obligations de conformité', 'Définition du périmètre du SMC, recensement des obligations de conformité et engagement direction.', '4-6', 'Périmètre du SMC, registre des obligations de conformité', 'Direction, Fonction conformité', 'Document Control', 1),
    ('ae000002-0000-0000-0000-000000000002', 2, 'Évaluation des risques de conformité', 'Cartographie des risques de non-conformité et définition des contrôles associés.', '4-8', 'Cartographie des risques de conformité, plan de traitement', 'Fonction conformité, Risk Manager', 'Risk, Ishikawa', 2),
    ('ae000002-0000-0000-0000-000000000002', 3, 'Politique, fonction & culture', 'Adoption de la politique de conformité, structuration de la fonction et développement de la culture.', '6-10', 'Politique de conformité, charte de la fonction, plan de culture', 'Direction, Fonction conformité', 'Document Control, Training', 3),
    ('ae000002-0000-0000-0000-000000000002', 4, 'Contrôles, alertes & indicateurs', 'Mise en place des contrôles, des canaux de remontée des préoccupations et des indicateurs de conformité.', '6-10', 'Matrice de contrôles, canal de remontée, tableau de bord conformité', 'Fonction conformité', 'Risk, KPI', 4),
    ('ae000002-0000-0000-0000-000000000002', 5, 'Audit interne & revue de direction', 'Audit interne du SMC et revue de direction de la performance de conformité.', '3-5', 'Rapport d''audit interne, compte rendu de revue de direction', 'Auditeur interne, Direction', 'Audit, PDCA', 5),
    ('ae000002-0000-0000-0000-000000000002', 6, 'Audit de certification (étapes 1 & 2)', 'Audit documentaire puis audit sur site par l''organisme certificateur, levée des non-conformités.', '4-8', 'Certificat ISO 37301', 'Organisme certificateur', 'Standards Hub, CAPA', 6);

-- ============================================================================
-- ISO 19011:2018 — Lignes directrices pour l'audit des systèmes de management
-- (NON certifiable : il s'agit de lignes directrices, pas d'exigences certifiables)
-- ============================================================================

INSERT INTO standards (id, code, full_name, publisher, current_version, publication_date,
    family, applicable_industries, description, certification_body_required,
    recertification_cycle_months, related_norm_codes, status, created_at, updated_at)
VALUES (
    'a0000003-0000-0000-0000-000000000003',
    'iso-19011',
    'ISO 19011:2018 — Lignes directrices pour l''audit des systèmes de management',
    'ISO',
    '2018',
    DATE '2018-07-01',
    'HLS',
    'all',
    'Lignes directrices pour l''audit des systèmes de management. ISO 19011 N''EST PAS une norme certifiable : elle ne contient pas d''exigences auditables mais des recommandations applicables aux audits internes (première partie) et aux audits de second partie (fournisseurs). Elle couvre les principes de l''audit, la gestion d''un programme d''audit (objectifs, risques et opportunités du programme, ressources, surveillance et amélioration), la réalisation des audits (de la planification au rapport et au suivi), ainsi que la compétence et l''évaluation des auditeurs. La version 2018 introduit l''approche par les risques du programme d''audit et étend les méthodes d''audit, notamment les activités d''audit à distance et l''usage des technologies.',
    FALSE,
    NULL,
    'iso-9001,iso-17025',
    'PUBLISHED', now(), now()
);

INSERT INTO standard_sections (id, standard_id, code, title, description, order_index) VALUES
    ('a3000003-0000-0000-0000-000000000001', 'a0000003-0000-0000-0000-000000000003', '4', 'Principes de l''audit', 'Principes fondamentaux guidant la conduite des audits.', 1),
    ('a3000003-0000-0000-0000-000000000002', 'a0000003-0000-0000-0000-000000000003', '5', 'Management d''un programme d''audit', 'Établissement, mise en œuvre, surveillance et amélioration du programme d''audit.', 2),
    ('a3000003-0000-0000-0000-000000000003', 'a0000003-0000-0000-0000-000000000003', '6', 'Réalisation d''un audit', 'Déclenchement, préparation, réalisation sur site, rapport et clôture de l''audit.', 3),
    ('a3000003-0000-0000-0000-000000000004', 'a0000003-0000-0000-0000-000000000003', '7', 'Compétence & évaluation des auditeurs', 'Détermination, acquisition et évaluation de la compétence des auditeurs.', 4);

INSERT INTO standard_clauses (id, section_id, code, title, description, order_index) VALUES
    ('ac000003-0000-0000-0000-000000000001', 'a3000003-0000-0000-0000-000000000001', '4', 'Principes de l''audit', 'Intégrité, présentation impartiale, conscience professionnelle, confidentialité, indépendance, approche fondée sur les preuves et les risques.', 1),
    ('ac000003-0000-0000-0000-000000000002', 'a3000003-0000-0000-0000-000000000002', '5.2', 'Objectifs du programme d''audit', 'Établissement des objectifs du programme d''audit alignés sur les orientations de l''organisme.', 2),
    ('ac000003-0000-0000-0000-000000000003', 'a3000003-0000-0000-0000-000000000002', '5.3', 'Risques & opportunités du programme', 'Détermination et traitement des risques et opportunités du programme d''audit.', 3),
    ('ac000003-0000-0000-0000-000000000004', 'a3000003-0000-0000-0000-000000000002', '5.5', 'Mise en œuvre du programme', 'Définition des périmètres, méthodes (dont à distance) et sélection des équipes d''audit.', 4),
    ('ac000003-0000-0000-0000-000000000005', 'a3000003-0000-0000-0000-000000000002', '5.6', 'Surveillance du programme', 'Surveillance et revue du programme d''audit pour son amélioration continue.', 5),
    ('ac000003-0000-0000-0000-000000000006', 'a3000003-0000-0000-0000-000000000003', '6.3', 'Préparation des activités d''audit', 'Revue documentaire, plan d''audit et préparation des documents de travail.', 6),
    ('ac000003-0000-0000-0000-000000000007', 'a3000003-0000-0000-0000-000000000003', '6.4', 'Réalisation des activités d''audit', 'Recueil et vérification des informations, constatations et conclusions d''audit.', 7),
    ('ac000003-0000-0000-0000-000000000008', 'a3000003-0000-0000-0000-000000000003', '6.5', 'Rapport d''audit', 'Préparation, approbation et diffusion du rapport d''audit.', 8),
    ('ac000003-0000-0000-0000-000000000009', 'a3000003-0000-0000-0000-000000000004', '7.2', 'Compétence des auditeurs', 'Détermination et évaluation de la compétence requise des auditeurs.', 9);

INSERT INTO standard_requirements (clause_id, code, text, obligation, evidence_types, measurable_criteria, risk_if_missing, order_index) VALUES
    ('ac000003-0000-0000-0000-000000000001', '4.a', 'Il convient que les audits soient conduits dans le respect des principes d''intégrité, de présentation impartiale et de conscience professionnelle.', 'SHOULD', 'DOCUMENT', 'Charte ou code de conduite d''audit diffusé aux auditeurs ; engagement d''impartialité signé', 'MEDIUM', 1),
    ('ac000003-0000-0000-0000-000000000002', '5.2.a', 'Il convient d''établir des objectifs du programme d''audit cohérents avec les orientations stratégiques de l''organisme.', 'SHOULD', 'DOCUMENT', 'Objectifs du programme d''audit documentés et revus annuellement', 'MEDIUM', 2),
    ('ac000003-0000-0000-0000-000000000003', '5.3.a', 'Il convient de déterminer et de traiter les risques et opportunités susceptibles d''affecter l''atteinte des objectifs du programme d''audit.', 'SHOULD', 'DOCUMENT,AUDIT', 'Risques et opportunités du programme d''audit identifiés et tracés ; actions de traitement définies', 'HIGH', 3),
    ('ac000003-0000-0000-0000-000000000004', '5.5.a', 'Il convient de définir le périmètre, les méthodes d''audit (y compris à distance) et de constituer les équipes d''audit pour chaque audit du programme.', 'SHOULD', 'DOCUMENT', 'Plan de programme couvrant périmètre, méthodes et affectation des équipes pour 100 % des audits planifiés', 'MEDIUM', 4),
    ('ac000003-0000-0000-0000-000000000005', '5.6.a', 'Il convient de surveiller et de revoir le programme d''audit afin d''évaluer l''atteinte de ses objectifs et de l''améliorer.', 'SHOULD', 'DOCUMENT,KPI_RECORD', 'Revue du programme d''audit réalisée au moins annuellement ; indicateurs de réalisation suivis', 'MEDIUM', 5),
    ('ac000003-0000-0000-0000-000000000006', '6.3.a', 'Il convient de préparer chaque audit par une revue documentaire, l''élaboration d''un plan d''audit et la préparation des documents de travail.', 'SHOULD', 'DOCUMENT', 'Plan d''audit établi et communiqué avant chaque audit ; documents de travail préparés', 'MEDIUM', 6),
    ('ac000003-0000-0000-0000-000000000007', '6.4.a', 'Il convient de recueillir et de vérifier les informations pertinentes par échantillonnage afin d''établir des constatations d''audit fondées sur des preuves.', 'SHOULD', 'AUDIT,DOCUMENT', 'Constatations d''audit étayées par des preuves vérifiées ; échantillonnage documenté', 'HIGH', 7),
    ('ac000003-0000-0000-0000-000000000008', '6.5.a', 'Il convient de préparer, faire approuver et diffuser un rapport d''audit complet, exact et clair dans les délais convenus.', 'SHOULD', 'DOCUMENT', 'Rapport d''audit diffusé dans le délai convenu ; contenu conforme au plan d''audit', 'MEDIUM', 8),
    ('ac000003-0000-0000-0000-000000000009', '7.2.a', 'Il convient de déterminer la compétence requise des auditeurs et d''évaluer celle-ci pour atteindre les objectifs du programme d''audit.', 'SHOULD', 'TRAINING_RECORD,DOCUMENT', 'Critères de compétence définis ; évaluation des auditeurs réalisée et qualification tracée', 'HIGH', 9);

INSERT INTO standard_certification_paths (id, standard_id, estimated_duration_months_min, estimated_duration_months_max,
    estimated_cost_eur_min, estimated_cost_eur_max, difficulty_level,
    surveillance_audit_frequency, recertification_cycle_years, notes)
VALUES ('ae000003-0000-0000-0000-000000000003', 'a0000003-0000-0000-0000-000000000003',
    1, 4, 2000, 15000, 2, 'none', NULL,
    'ISO 19011 N''EST PAS certifiable : il s''agit de lignes directrices pour l''audit des systèmes de management, sans exigences auditables ni certificat associé. La « feuille de route » ci-dessous correspond à la mise en place d''un programme d''audit interne robuste et à la qualification des auditeurs. Synergie directe avec les modules Audit (programme et exécution) et Training (compétence des auditeurs) de QualitOS.');

INSERT INTO standard_certification_stages (certification_path_id, stage_number, name, description, typical_duration_weeks, deliverables, actors, qualitos_modules, order_index) VALUES
    ('ae000003-0000-0000-0000-000000000003', 1, 'Principes & cadre du programme d''audit', 'Adoption des principes d''audit et définition du cadre du programme d''audit interne.', '1-2', 'Charte d''audit, principes d''audit', 'Responsable du programme d''audit', 'Document Control', 1),
    ('ae000003-0000-0000-0000-000000000003', 2, 'Objectifs, risques & planification', 'Définition des objectifs du programme, prise en compte des risques/opportunités et planification annuelle.', '2-3', 'Objectifs du programme, plan annuel d''audit, analyse des risques du programme', 'Responsable du programme d''audit', 'Audit, Risk', 2),
    ('ae000003-0000-0000-0000-000000000003', 3, 'Compétence & qualification des auditeurs', 'Définition des compétences requises, formation et évaluation des auditeurs internes.', '2-4', 'Référentiel de compétences, plan de formation, grilles d''évaluation', 'RH, Responsable du programme d''audit', 'Training', 3),
    ('ae000003-0000-0000-0000-000000000003', 4, 'Réalisation des audits', 'Préparation, conduite (sur site et à distance), constatations et rapports d''audit.', '2-6', 'Plans d''audit, rapports d''audit, constatations', 'Auditeurs internes', 'Audit', 4),
    ('ae000003-0000-0000-0000-000000000003', 5, 'Surveillance & amélioration du programme', 'Suivi de la réalisation, mesure de l''efficacité et amélioration continue du programme d''audit.', '1-2', 'Bilan du programme d''audit, indicateurs, plan d''amélioration', 'Responsable du programme d''audit, Direction', 'PDCA, Audit', 5);

-- ============================================================================
-- ISO 10002:2018 — Lignes directrices pour le traitement des réclamations clients
-- (NON certifiable : lignes directrices)
-- ============================================================================

INSERT INTO standards (id, code, full_name, publisher, current_version, publication_date,
    family, applicable_industries, description, certification_body_required,
    recertification_cycle_months, related_norm_codes, status, created_at, updated_at)
VALUES (
    'a0000004-0000-0000-0000-000000000004',
    'iso-10002',
    'ISO 10002:2018 — Management de la qualité — Satisfaction du client — Lignes directrices pour le traitement des réclamations dans les organismes',
    'ISO',
    '2018',
    DATE '2018-07-01',
    'HLS',
    'all',
    'Lignes directrices pour la conception et la mise en œuvre d''un processus efficace de traitement des réclamations clients. ISO 10002 N''EST PAS certifiable en tant que telle (lignes directrices), mais elle s''intègre naturellement à un système ISO 9001. Elle promeut un processus accessible et visible, l''accusé de réception de chaque réclamation dans un délai défini, un traitement objectif, équitable et confidentiel, le suivi de la réclamation jusqu''à sa clôture, le retour d''information au réclamant, ainsi que l''analyse des tendances pour l''amélioration continue. Les principes directeurs incluent la transparence, l''accessibilité, la réactivité, l''objectivité, la gratuité pour le réclamant, la confidentialité, l''approche orientée client, la responsabilité et l''amélioration continue.',
    FALSE,
    NULL,
    'iso-9001',
    'PUBLISHED', now(), now()
);

INSERT INTO standard_sections (id, standard_id, code, title, description, order_index) VALUES
    ('a4000004-0000-0000-0000-000000000001', 'a0000004-0000-0000-0000-000000000004', '4', 'Principes directeurs', 'Principes guidant le traitement efficace des réclamations.', 1),
    ('a4000004-0000-0000-0000-000000000002', 'a0000004-0000-0000-0000-000000000004', '6', 'Planification & conception', 'Objectifs, ressources et conception du processus de traitement des réclamations.', 2),
    ('a4000004-0000-0000-0000-000000000003', 'a0000004-0000-0000-0000-000000000004', '7', 'Mise en œuvre du processus', 'Réception, suivi, accusé de réception, évaluation, résolution et communication.', 3),
    ('a4000004-0000-0000-0000-000000000004', 'a0000004-0000-0000-0000-000000000004', '8', 'Maintien & amélioration', 'Analyse, évaluation, satisfaction et amélioration continue du processus.', 4);

INSERT INTO standard_clauses (id, section_id, code, title, description, order_index) VALUES
    ('ad000004-0000-0000-0000-000000000001', 'a4000004-0000-0000-0000-000000000001', '4.1', 'Accessibilité', 'Processus de réclamation facilement accessible et visible pour tous les clients.', 1),
    ('ad000004-0000-0000-0000-000000000002', 'a4000004-0000-0000-0000-000000000001', '4.2', 'Objectivité & confidentialité', 'Traitement objectif, équitable et confidentiel des réclamations.', 2),
    ('ad000004-0000-0000-0000-000000000003', 'a4000004-0000-0000-0000-000000000002', '6.1', 'Objectifs & ressources', 'Définition des objectifs du processus et allocation des ressources nécessaires.', 3),
    ('ad000004-0000-0000-0000-000000000004', 'a4000004-0000-0000-0000-000000000003', '7.2', 'Réception de la réclamation', 'Enregistrement et identification de chaque réclamation reçue.', 4),
    ('ad000004-0000-0000-0000-000000000005', 'a4000004-0000-0000-0000-000000000003', '7.3', 'Suivi de la réclamation', 'Suivi de la réclamation depuis sa réception jusqu''à sa clôture.', 5),
    ('ad000004-0000-0000-0000-000000000006', 'a4000004-0000-0000-0000-000000000003', '7.4', 'Accusé de réception', 'Accusé de réception adressé au réclamant dans un délai défini.', 6),
    ('ad000004-0000-0000-0000-000000000007', 'a4000004-0000-0000-0000-000000000003', '7.8', 'Communication de la décision', 'Communication au réclamant de la décision ou des actions prises.', 7),
    ('ad000004-0000-0000-0000-000000000008', 'a4000004-0000-0000-0000-000000000004', '8.2', 'Analyse & évaluation', 'Analyse et évaluation des réclamations pour identifier les tendances.', 8),
    ('ad000004-0000-0000-0000-000000000009', 'a4000004-0000-0000-0000-000000000004', '8.6', 'Amélioration continue', 'Amélioration continue de l''efficacité du processus de traitement des réclamations.', 9);

INSERT INTO standard_requirements (clause_id, code, text, obligation, evidence_types, measurable_criteria, risk_if_missing, order_index) VALUES
    ('ad000004-0000-0000-0000-000000000001', '4.1.a', 'Il convient que le processus de traitement des réclamations soit accessible et visible, et que les informations pour réclamer soient mises à disposition des clients.', 'SHOULD', 'DOCUMENT', 'Canaux de réclamation publiés et accessibles (multi-canal) ; information disponible et gratuite', 'HIGH', 1),
    ('ad000004-0000-0000-0000-000000000002', '4.2.a', 'Il convient de traiter chaque réclamation de manière objective, équitable et confidentielle.', 'SHOULD', 'DOCUMENT', 'Procédure garantissant objectivité et confidentialité ; accès aux données restreint', 'MEDIUM', 2),
    ('ad000004-0000-0000-0000-000000000003', '6.1.a', 'Il convient de fixer des objectifs de traitement des réclamations et d''allouer les ressources nécessaires.', 'SHOULD', 'DOCUMENT,KPI_RECORD', 'Objectifs de traitement définis (ex. délai cible) ; ressources affectées documentées', 'MEDIUM', 3),
    ('ad000004-0000-0000-0000-000000000004', '7.2.a', 'Il convient d''enregistrer chaque réclamation avec les informations nécessaires à son traitement et à son identification.', 'SHOULD', 'DOCUMENT', 'Registre des réclamations ; chaque réclamation identifiée par un numéro unique', 'MEDIUM', 4),
    ('ad000004-0000-0000-0000-000000000005', '7.3.a', 'Il convient de suivre chaque réclamation tout au long du processus jusqu''à la décision finale ou la clôture.', 'SHOULD', 'DOCUMENT,KPI_RECORD', 'Statut de chaque réclamation tracé ; taux de réclamations clôturées suivi', 'HIGH', 5),
    ('ad000004-0000-0000-0000-000000000006', '7.4.a', 'Il convient d''accuser réception de chaque réclamation auprès du réclamant immédiatement ou dans un délai défini.', 'SHOULD', 'DOCUMENT,KPI_RECORD', 'Accusé de réception envoyé sous le délai cible (ex. 48 h) pour ≥ 95 % des réclamations', 'HIGH', 6),
    ('ad000004-0000-0000-0000-000000000007', '7.8.a', 'Il convient de communiquer au réclamant la décision ou les actions prises à l''issue du traitement de sa réclamation.', 'SHOULD', 'DOCUMENT,KPI_RECORD', 'Réponse communiquée au réclamant dans le délai cible ; taux de réponse suivi', 'HIGH', 7),
    ('ad000004-0000-0000-0000-000000000008', '8.2.a', 'Il convient d''analyser et d''évaluer les réclamations afin d''identifier les tendances et les causes récurrentes.', 'SHOULD', 'DOCUMENT,KPI_RECORD', 'Analyse des tendances de réclamations réalisée périodiquement (Pareto, top causes)', 'MEDIUM', 8),
    ('ad000004-0000-0000-0000-000000000009', '8.6.a', 'Il convient d''améliorer en continu l''efficacité du processus en exploitant les enseignements tirés des réclamations.', 'SHOULD', 'DOCUMENT,CAPA', 'Actions d''amélioration issues de l''analyse des réclamations engagées et suivies', 'MEDIUM', 9),
    ('ad000004-0000-0000-0000-000000000009', '8.4', 'Il convient de mesurer la satisfaction du réclamant à l''égard du processus de traitement des réclamations.', 'SHOULD', 'KPI_RECORD,DOCUMENT', 'Indicateur de satisfaction post-réclamation mesuré et suivi dans le temps', 'LOW', 10);

INSERT INTO standard_certification_paths (id, standard_id, estimated_duration_months_min, estimated_duration_months_max,
    estimated_cost_eur_min, estimated_cost_eur_max, difficulty_level,
    surveillance_audit_frequency, recertification_cycle_years, notes)
VALUES ('ae000004-0000-0000-0000-000000000004', 'a0000004-0000-0000-0000-000000000004',
    2, 6, 3000, 20000, 2, 'none', NULL,
    'ISO 10002 N''EST PAS certifiable (lignes directrices) ; elle est le plus souvent mise en œuvre dans le cadre d''un système ISO 9001 et peut faire l''objet d''une attestation de conformité par tierce partie. La feuille de route ci-dessous décrit la mise en place du processus de traitement des réclamations. Synergie directe avec les modules Complaints (Customer Complaints & VoC), CAPA (actions correctives) et KPI (délais et satisfaction) de QualitOS.');

INSERT INTO standard_certification_stages (certification_path_id, stage_number, name, description, typical_duration_weeks, deliverables, actors, qualitos_modules, order_index) VALUES
    ('ae000004-0000-0000-0000-000000000004', 1, 'Cadrage & principes directeurs', 'Engagement direction, adoption des principes directeurs et définition de la politique de traitement des réclamations.', '2-3', 'Politique réclamations, principes directeurs', 'Direction, Responsable relation client', 'Document Control', 1),
    ('ae000004-0000-0000-0000-000000000004', 2, 'Conception du processus & canaux', 'Conception du processus, mise en place des canaux d''accès (multi-canal) et des objectifs de délais.', '3-5', 'Processus de traitement, canaux de réception, objectifs de délais', 'Responsable relation client', 'Complaints, Document Control', 2),
    ('ae000004-0000-0000-0000-000000000004', 3, 'Mise en œuvre & traitement', 'Déploiement du traitement : enregistrement, accusé de réception, suivi, résolution et communication au client.', '4-8', 'Registre des réclamations, modèles d''accusé/réponse, suivi des dossiers', 'Équipe relation client', 'Complaints, CAPA', 3),
    ('ae000004-0000-0000-0000-000000000004', 4, 'Analyse des tendances & indicateurs', 'Analyse des réclamations (Pareto, causes récurrentes) et suivi des indicateurs (délais, satisfaction).', '2-4', 'Tableau de bord réclamations, analyse des tendances', 'Responsable qualité', 'KPI, Complaints', 4),
    ('ae000004-0000-0000-0000-000000000004', 5, 'Amélioration continue & revue', 'Exploitation des enseignements, actions d''amélioration et revue périodique du processus.', '2-4', 'Plan d''amélioration, compte rendu de revue du processus', 'Direction, Responsable qualité', 'PDCA, CAPA', 5);

-- ============================================================================
-- ISO 26000:2010 — Lignes directrices relatives à la responsabilité sociétale
-- (NON certifiable : ISO 26000 ne se prête PAS à la certification — point notoire)
-- ============================================================================

INSERT INTO standards (id, code, full_name, publisher, current_version, publication_date,
    family, applicable_industries, description, certification_body_required,
    recertification_cycle_months, related_norm_codes, status, created_at, updated_at)
VALUES (
    'a0000005-0000-0000-0000-000000000005',
    'iso-26000',
    'ISO 26000:2010 — Lignes directrices relatives à la responsabilité sociétale',
    'ISO',
    '2010',
    DATE '2010-11-01',
    'HLS',
    'all',
    'Lignes directrices relatives à la responsabilité sociétale (RSE) applicables à tout type d''organisation. Point notoire : ISO 26000 N''EST PAS destinée à des fins de certification ni à un usage réglementaire ou contractuel — toute offre de « certification ISO 26000 » est une mauvaise interprétation de la norme. Elle fournit un cadre pour intégrer un comportement responsable à travers sept questions centrales : la gouvernance de l''organisation, les droits de l''Homme, les relations et conditions de travail, l''environnement, la loyauté des pratiques, les questions relatives aux consommateurs, et les communautés et le développement local. Elle repose sur des principes (redevabilité, transparence, comportement éthique, respect des intérêts des parties prenantes, du principe de légalité, des normes internationales de comportement et des droits de l''Homme) et sur l''identification de la sphère d''influence et des parties prenantes.',
    FALSE,
    NULL,
    'iso-14001,iso-45001',
    'PUBLISHED', now(), now()
);

INSERT INTO standard_sections (id, standard_id, code, title, description, order_index) VALUES
    ('a5000005-0000-0000-0000-000000000001', 'a0000005-0000-0000-0000-000000000005', '4', 'Principes de la responsabilité sociétale', 'Principes guidant un comportement sociétalement responsable.', 1),
    ('a5000005-0000-0000-0000-000000000002', 'a0000005-0000-0000-0000-000000000005', '5', 'Reconnaissance & parties prenantes', 'Reconnaissance de la responsabilité sociétale et dialogue avec les parties prenantes.', 2),
    ('a5000005-0000-0000-0000-000000000003', 'a0000005-0000-0000-0000-000000000005', '6', 'Questions centrales', 'Les sept questions centrales de la responsabilité sociétale.', 3),
    ('a5000005-0000-0000-0000-000000000004', 'a0000005-0000-0000-0000-000000000005', '7', 'Intégration de la RSE', 'Intégration de la responsabilité sociétale dans l''ensemble de l''organisation.', 4);

INSERT INTO standard_clauses (id, section_id, code, title, description, order_index) VALUES
    ('a9000005-0000-0000-0000-000000000001', 'a5000005-0000-0000-0000-000000000001', '4', 'Principes', 'Redevabilité, transparence, comportement éthique, respect des parties prenantes, de la légalité et des droits de l''Homme.', 1),
    ('a9000005-0000-0000-0000-000000000002', 'a5000005-0000-0000-0000-000000000002', '5.3', 'Identification des parties prenantes', 'Identification des parties prenantes et dialogue avec elles.', 2),
    ('a9000005-0000-0000-0000-000000000003', 'a5000005-0000-0000-0000-000000000003', '6.2', 'Gouvernance de l''organisation', 'Processus et structures de décision intégrant la responsabilité sociétale.', 3),
    ('a9000005-0000-0000-0000-000000000004', 'a5000005-0000-0000-0000-000000000003', '6.3', 'Droits de l''Homme', 'Respect et promotion des droits de l''Homme dans la sphère d''influence.', 4),
    ('a9000005-0000-0000-0000-000000000005', 'a5000005-0000-0000-0000-000000000003', '6.4', 'Relations & conditions de travail', 'Emploi, conditions de travail, dialogue social, santé et sécurité au travail.', 5),
    ('a9000005-0000-0000-0000-000000000006', 'a5000005-0000-0000-0000-000000000003', '6.5', 'Environnement', 'Prévention de la pollution, usage durable des ressources et protection de l''environnement.', 6),
    ('a9000005-0000-0000-0000-000000000007', 'a5000005-0000-0000-0000-000000000003', '6.6', 'Loyauté des pratiques', 'Lutte contre la corruption, concurrence loyale et promotion de la RSE dans la chaîne de valeur.', 7),
    ('a9000005-0000-0000-0000-000000000008', 'a5000005-0000-0000-0000-000000000003', '6.7', 'Questions relatives aux consommateurs', 'Pratiques loyales, protection de la santé, de la sécurité et des données des consommateurs.', 8),
    ('a9000005-0000-0000-0000-000000000009', 'a5000005-0000-0000-0000-000000000003', '6.8', 'Communautés & développement local', 'Engagement auprès des communautés et contribution au développement local.', 9);

INSERT INTO standard_requirements (clause_id, code, text, obligation, evidence_types, measurable_criteria, risk_if_missing, order_index) VALUES
    ('a9000005-0000-0000-0000-000000000001', '4.a', 'Il convient que l''organisation se fonde sur les principes de redevabilité, de transparence et de comportement éthique.', 'SHOULD', 'DOCUMENT', 'Engagement RSE formalisé reprenant les principes ; reporting public de transparence', 'MEDIUM', 1),
    ('a9000005-0000-0000-0000-000000000002', '5.3.a', 'Il convient que l''organisation identifie ses parties prenantes et dialogue avec elles sur les enjeux de responsabilité sociétale.', 'SHOULD', 'DOCUMENT', 'Cartographie des parties prenantes établie ; dispositif de dialogue tracé', 'MEDIUM', 2),
    ('a9000005-0000-0000-0000-000000000003', '6.2.a', 'Il convient que l''organisation intègre la responsabilité sociétale dans ses processus de gouvernance et de décision.', 'SHOULD', 'DOCUMENT', 'Responsabilités RSE attribuées dans la gouvernance ; intégration aux décisions documentée', 'HIGH', 3),
    ('a9000005-0000-0000-0000-000000000004', '6.3.a', 'Il convient que l''organisation respecte et promeuve les droits de l''Homme dans sa sphère d''influence et exerce une diligence raisonnable.', 'SHOULD', 'DOCUMENT,AUDIT', 'Politique droits de l''Homme ; diligence raisonnable réalisée sur les activités et la chaîne de valeur', 'HIGH', 4),
    ('a9000005-0000-0000-0000-000000000005', '6.4.a', 'Il convient que l''organisation assure des relations et conditions de travail décentes, le dialogue social et la santé et sécurité au travail.', 'SHOULD', 'DOCUMENT,TRAINING_RECORD', 'Politiques emploi/SST en place ; indicateurs sociaux et de SST suivis', 'HIGH', 5),
    ('a9000005-0000-0000-0000-000000000006', '6.5.a', 'Il convient que l''organisation prévienne la pollution, utilise durablement les ressources et atténue son impact environnemental.', 'SHOULD', 'DOCUMENT,KPI_RECORD', 'Indicateurs environnementaux (énergie, déchets, émissions) suivis ; objectifs de réduction définis', 'HIGH', 6),
    ('a9000005-0000-0000-0000-000000000007', '6.6.a', 'Il convient que l''organisation agisse loyalement : lutte contre la corruption, concurrence loyale et promotion de la RSE auprès des fournisseurs.', 'SHOULD', 'DOCUMENT,AUDIT', 'Code de conduite anti-corruption/concurrence ; critères RSE intégrés aux achats', 'MEDIUM', 7),
    ('a9000005-0000-0000-0000-000000000008', '6.7.a', 'Il convient que l''organisation protège la santé, la sécurité, les intérêts et les données des consommateurs par des pratiques loyales.', 'SHOULD', 'DOCUMENT', 'Pratiques loyales d''information ; protection des données consommateurs ; traitement des réclamations', 'MEDIUM', 8),
    ('a9000005-0000-0000-0000-000000000009', '6.8.a', 'Il convient que l''organisation s''engage auprès des communautés locales et contribue à leur développement.', 'SHOULD', 'DOCUMENT', 'Actions d''engagement communautaire documentées ; contribution au développement local mesurée', 'LOW', 9);

INSERT INTO standard_certification_paths (id, standard_id, estimated_duration_months_min, estimated_duration_months_max,
    estimated_cost_eur_min, estimated_cost_eur_max, difficulty_level,
    surveillance_audit_frequency, recertification_cycle_years, notes)
VALUES ('ae000005-0000-0000-0000-000000000005', 'a0000005-0000-0000-0000-000000000005',
    6, 18, 5000, 40000, 3, 'none', NULL,
    'POINT NOTOIRE : ISO 26000 N''EST PAS certifiable — elle ne fournit pas d''exigences et son usage à des fins de certification est explicitement déconseillé par l''ISO. La feuille de route ci-dessous correspond au déploiement d''une démarche RSE structurée (évaluation possible par labels tiers tels que l''évaluation AFNOR/Engagé RSE, alignement CSRD/GRI). Synergie avec les modules EHS (environnement/SST), Document Control (politiques RSE) et Training (sensibilisation) de QualitOS.');

INSERT INTO standard_certification_stages (certification_path_id, stage_number, name, description, typical_duration_weeks, deliverables, actors, qualitos_modules, order_index) VALUES
    ('ae000005-0000-0000-0000-000000000005', 1, 'Engagement & principes RSE', 'Engagement de la direction et adoption des principes de responsabilité sociétale.', '3-5', 'Engagement RSE, principes adoptés', 'Direction', 'Document Control', 1),
    ('ae000005-0000-0000-0000-000000000005', 2, 'Parties prenantes & matérialité', 'Cartographie des parties prenantes, dialogue et identification des enjeux RSE prioritaires.', '4-8', 'Cartographie des parties prenantes, matrice de matérialité', 'Responsable RSE', 'Document Control', 2),
    ('ae000005-0000-0000-0000-000000000005', 3, 'Déploiement des sept questions centrales', 'Plans d''action sur gouvernance, droits de l''Homme, travail, environnement, loyauté, consommateurs et communautés.', '8-16', 'Plans d''action par question centrale, politiques associées', 'Responsable RSE, EHS', 'EHS, Document Control', 3),
    ('ae000005-0000-0000-0000-000000000005', 4, 'Sensibilisation & intégration', 'Sensibilisation du personnel et intégration de la RSE dans les processus de l''organisation.', '4-8', 'Plan de sensibilisation, intégration aux processus', 'RH, Responsable RSE', 'Training, Document Control', 4),
    ('ae000005-0000-0000-0000-000000000005', 5, 'Reporting, revue & amélioration', 'Reporting RSE (alignement CSRD/GRI possible), revue de direction et amélioration continue.', '3-6', 'Rapport RSE, compte rendu de revue, plan d''amélioration', 'Direction, Responsable RSE', 'PDCA, KPI', 5);
