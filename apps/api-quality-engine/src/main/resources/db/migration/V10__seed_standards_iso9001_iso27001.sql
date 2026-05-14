-- Seed Standards Hub : ISO 9001:2015 et ISO 27001:2022 (pilotes MVP P1).
-- Couverture représentative : sections 4-10 d'ISO 9001 + Annexe A d'ISO 27001 (synthétique).
-- L'enrichissement complet de chaque clause/exigence est prévu via migrations ultérieures
-- et à terme via un éditeur dédié (CLAUDE.md §8.10).

-- ============================================================================
-- ISO 9001:2015
-- ============================================================================

INSERT INTO standards (id, code, full_name, publisher, current_version, publication_date,
    family, applicable_industries, description, certification_body_required,
    recertification_cycle_months, related_norm_codes, status, created_at, updated_at)
VALUES (
    '11111111-1111-1111-1111-111111111111',
    'iso-9001',
    'ISO 9001:2015 — Systèmes de management de la qualité — Exigences',
    'ISO',
    '2015',
    '2015-09-15',
    'HLS',
    'all',
    'Norme internationale spécifiant les exigences pour un système de management de la qualité. Applicable à toute organisation, quels que soient sa taille, son secteur ou ses produits/services.',
    TRUE,
    36,
    'iso-9000,iso-9004,iso-19011',
    'PUBLISHED',
    now(), now()
);

INSERT INTO standard_sections (id, standard_id, code, title, description, order_index) VALUES
    ('a1111111-1111-1111-1111-111111111111', '11111111-1111-1111-1111-111111111111', '4', 'Contexte de l''organisme', 'Compréhension du contexte interne/externe et des parties intéressées, périmètre du SMQ.', 4),
    ('a2222222-2222-2222-2222-222222222222', '11111111-1111-1111-1111-111111111111', '5', 'Leadership', 'Engagement de la direction, politique qualité, rôles et responsabilités.', 5),
    ('a3333333-3333-3333-3333-333333333333', '11111111-1111-1111-1111-111111111111', '6', 'Planification', 'Risques et opportunités, objectifs qualité, planification des changements.', 6),
    ('a4444444-4444-4444-4444-444444444444', '11111111-1111-1111-1111-111111111111', '7', 'Support', 'Ressources, compétences, sensibilisation, communication, informations documentées.', 7),
    ('a5555555-5555-5555-5555-555555555555', '11111111-1111-1111-1111-111111111111', '8', 'Réalisation des activités opérationnelles', 'Planification opérationnelle, exigences clients, conception, achats, production, libération.', 8),
    ('a6666666-6666-6666-6666-666666666666', '11111111-1111-1111-1111-111111111111', '9', 'Évaluation des performances', 'Surveillance, mesure, satisfaction client, audit interne, revue de direction.', 9),
    ('a7777777-7777-7777-7777-777777777777', '11111111-1111-1111-1111-111111111111', '10', 'Amélioration', 'Non-conformités, actions correctives, amélioration continue.', 10);

-- Section 4 — clauses
INSERT INTO standard_clauses (id, section_id, code, title, description, order_index) VALUES
    ('c0000041-0000-0000-0000-000000000001', 'a1111111-1111-1111-1111-111111111111', '4.1', 'Compréhension de l''organisme et de son contexte', NULL, 1),
    ('c0000041-0000-0000-0000-000000000002', 'a1111111-1111-1111-1111-111111111111', '4.2', 'Compréhension des besoins et attentes des parties intéressées', NULL, 2),
    ('c0000041-0000-0000-0000-000000000003', 'a1111111-1111-1111-1111-111111111111', '4.3', 'Détermination du domaine d''application du SMQ', NULL, 3),
    ('c0000041-0000-0000-0000-000000000004', 'a1111111-1111-1111-1111-111111111111', '4.4', 'Système de management de la qualité et ses processus', NULL, 4);

INSERT INTO standard_requirements (clause_id, code, text, obligation, evidence_types, measurable_criteria, risk_if_missing, order_index) VALUES
    ('c0000041-0000-0000-0000-000000000001', '4.1', 'L''organisme doit déterminer les enjeux externes et internes pertinents par rapport à sa finalité et son orientation stratégique.', 'MUST', 'SWOT, PESTEL, document stratégique', 'Enjeux documentés, revue au moins annuelle', 'HIGH', 1),
    ('c0000041-0000-0000-0000-000000000002', '4.2', 'L''organisme doit déterminer les parties intéressées pertinentes pour le SMQ et leurs exigences.', 'MUST', 'Registre des parties intéressées', 'Liste exhaustive, mise à jour ≥ 1/an', 'HIGH', 1),
    ('c0000041-0000-0000-0000-000000000003', '4.3', 'L''organisme doit déterminer les limites et l''applicabilité du SMQ pour établir son domaine d''application.', 'MUST', 'Manuel qualité, scope statement', 'Périmètre formalisé et publié', 'MEDIUM', 1),
    ('c0000041-0000-0000-0000-000000000004', '4.4.1', 'L''organisme doit établir, mettre en œuvre, tenir à jour et améliorer en continu un SMQ.', 'MUST', 'Cartographie des processus', 'Processus identifiés avec entrées/sorties/KPIs', 'CRITICAL', 1),
    ('c0000041-0000-0000-0000-000000000004', '4.4.2', 'L''organisme doit tenir à jour des informations documentées nécessaires à l''efficacité du SMQ.', 'MUST', 'Documents qualité', 'Procédures versionnées, signées', 'HIGH', 2);

-- Section 5 — clauses
INSERT INTO standard_clauses (id, section_id, code, title, description, order_index) VALUES
    ('c0000051-0000-0000-0000-000000000001', 'a2222222-2222-2222-2222-222222222222', '5.1', 'Leadership et engagement', NULL, 1),
    ('c0000051-0000-0000-0000-000000000002', 'a2222222-2222-2222-2222-222222222222', '5.2', 'Politique', NULL, 2),
    ('c0000051-0000-0000-0000-000000000003', 'a2222222-2222-2222-2222-222222222222', '5.3', 'Rôles, responsabilités et autorités', NULL, 3);

INSERT INTO standard_requirements (clause_id, code, text, obligation, evidence_types, measurable_criteria, risk_if_missing, order_index) VALUES
    ('c0000051-0000-0000-0000-000000000001', '5.1.1', 'La direction doit démontrer son leadership et son engagement vis-à-vis du SMQ.', 'MUST', 'PV revue de direction, plan d''engagement', 'Revue de direction ≥ 1/an documentée', 'CRITICAL', 1),
    ('c0000051-0000-0000-0000-000000000002', '5.2.1', 'La direction doit établir, mettre en œuvre et tenir à jour une politique qualité.', 'MUST', 'Politique qualité signée', 'Politique datée, signée, diffusée', 'CRITICAL', 1),
    ('c0000051-0000-0000-0000-000000000003', '5.3', 'La direction doit attribuer les responsabilités et autorités pour les rôles pertinents.', 'MUST', 'Organigramme, fiches de fonction', 'Tous rôles SMQ assignés', 'HIGH', 1);

-- Section 9 — audit interne et revue de direction (clauses pivot)
INSERT INTO standard_clauses (id, section_id, code, title, description, order_index) VALUES
    ('c0000091-0000-0000-0000-000000000001', 'a6666666-6666-6666-6666-666666666666', '9.1', 'Surveillance, mesure, analyse et évaluation', NULL, 1),
    ('c0000091-0000-0000-0000-000000000002', 'a6666666-6666-6666-6666-666666666666', '9.2', 'Audit interne', NULL, 2),
    ('c0000091-0000-0000-0000-000000000003', 'a6666666-6666-6666-6666-666666666666', '9.3', 'Revue de direction', NULL, 3);

INSERT INTO standard_requirements (clause_id, code, text, obligation, evidence_types, measurable_criteria, risk_if_missing, order_index) VALUES
    ('c0000091-0000-0000-0000-000000000002', '9.2.1', 'L''organisme doit réaliser des audits internes à intervalles planifiés.', 'MUST', 'Programme et rapports d''audit interne', 'Audit interne ≥ 1/an, couverture complète', 'CRITICAL', 1),
    ('c0000091-0000-0000-0000-000000000003', '9.3.1', 'La direction doit procéder à la revue du SMQ à intervalles planifiés.', 'MUST', 'PV revue de direction', 'Revue de direction ≥ 1/an avec décisions documentées', 'CRITICAL', 1);

-- Section 10 — amélioration
INSERT INTO standard_clauses (id, section_id, code, title, description, order_index) VALUES
    ('c0000101-0000-0000-0000-000000000001', 'a7777777-7777-7777-7777-777777777777', '10.1', 'Généralités', NULL, 1),
    ('c0000101-0000-0000-0000-000000000002', 'a7777777-7777-7777-7777-777777777777', '10.2', 'Non-conformité et action corrective', NULL, 2),
    ('c0000101-0000-0000-0000-000000000003', 'a7777777-7777-7777-7777-777777777777', '10.3', 'Amélioration continue', NULL, 3);

INSERT INTO standard_requirements (clause_id, code, text, obligation, evidence_types, measurable_criteria, risk_if_missing, order_index) VALUES
    ('c0000101-0000-0000-0000-000000000002', '10.2.1', 'Lorsqu''une non-conformité se produit, l''organisme doit réagir et entreprendre des actions correctives appropriées.', 'MUST', 'Registre CAPA, fiches NC', 'Délai de traitement < 30 jours pour criticité haute', 'CRITICAL', 1),
    ('c0000101-0000-0000-0000-000000000003', '10.3', 'L''organisme doit améliorer en continu la pertinence, l''adéquation et l''efficacité du SMQ.', 'MUST', 'Cycles PDCA, indicateurs amélioration', 'Plan d''amélioration documenté et suivi', 'HIGH', 1);

-- ============================================================================
-- ISO/IEC 27001:2022
-- ============================================================================

INSERT INTO standards (id, code, full_name, publisher, current_version, publication_date,
    family, applicable_industries, description, certification_body_required,
    recertification_cycle_months, related_norm_codes, status, created_at, updated_at)
VALUES (
    '22222222-2222-2222-2222-222222222222',
    'iso-27001',
    'ISO/IEC 27001:2022 — Sécurité de l''information — Systèmes de management',
    'ISO/IEC',
    '2022',
    '2022-10-25',
    'HLS',
    'all',
    'Spécifie les exigences pour établir, mettre en œuvre, tenir à jour et améliorer un système de management de la sécurité de l''information (SMSI).',
    TRUE,
    36,
    'iso-27002,iso-27005,iso-27017,iso-27018',
    'PUBLISHED',
    now(), now()
);

INSERT INTO standard_sections (id, standard_id, code, title, description, order_index) VALUES
    ('b1111111-1111-1111-1111-111111111111', '22222222-2222-2222-2222-222222222222', '4', 'Contexte de l''organisme', 'Compréhension du contexte, parties intéressées, périmètre du SMSI.', 4),
    ('b2222222-2222-2222-2222-222222222222', '22222222-2222-2222-2222-222222222222', '5', 'Leadership', 'Politique sécurité, rôles, responsabilités.', 5),
    ('b3333333-3333-3333-3333-333333333333', '22222222-2222-2222-2222-222222222222', '6', 'Planification', 'Risques sécurité, traitement, SoA, objectifs.', 6),
    ('b6666666-6666-6666-6666-666666666666', '22222222-2222-2222-2222-222222222222', '9', 'Évaluation des performances', 'Surveillance, audit interne, revue de direction.', 9),
    ('b7777777-7777-7777-7777-777777777777', '22222222-2222-2222-2222-222222222222', '10', 'Amélioration', 'Non-conformités, amélioration continue.', 10);

INSERT INTO standard_clauses (id, section_id, code, title, description, order_index) VALUES
    ('d0000041-0000-0000-0000-000000000001', 'b1111111-1111-1111-1111-111111111111', '4.3', 'Détermination du domaine d''application du SMSI', NULL, 3),
    ('d0000051-0000-0000-0000-000000000001', 'b2222222-2222-2222-2222-222222222222', '5.2', 'Politique de sécurité de l''information', NULL, 2),
    ('d0000061-0000-0000-0000-000000000001', 'b3333333-3333-3333-3333-333333333333', '6.1.2', 'Appréciation des risques de sécurité de l''information', NULL, 1),
    ('d0000061-0000-0000-0000-000000000002', 'b3333333-3333-3333-3333-333333333333', '6.1.3', 'Traitement des risques et Déclaration d''Applicabilité (SoA)', NULL, 2),
    ('d0000091-0000-0000-0000-000000000002', 'b6666666-6666-6666-6666-666666666666', '9.2', 'Audit interne SMSI', NULL, 2),
    ('d0000101-0000-0000-0000-000000000002', 'b7777777-7777-7777-7777-777777777777', '10.1', 'Amélioration continue', NULL, 1);

INSERT INTO standard_requirements (clause_id, code, text, obligation, evidence_types, measurable_criteria, risk_if_missing, order_index) VALUES
    ('d0000041-0000-0000-0000-000000000001', '4.3', 'L''organisme doit déterminer les limites et l''applicabilité du SMSI.', 'MUST', 'Document de périmètre SMSI', 'Périmètre formalisé, validé direction', 'CRITICAL', 1),
    ('d0000051-0000-0000-0000-000000000001', '5.2', 'La direction doit établir une politique de sécurité de l''information.', 'MUST', 'Politique SSI signée', 'Politique datée, signée, diffusée', 'CRITICAL', 1),
    ('d0000061-0000-0000-0000-000000000001', '6.1.2', 'L''organisme doit définir et appliquer un processus d''appréciation des risques.', 'MUST', 'Méthode + registre de risques', 'Appréciation ≥ 1/an, méthodologie documentée', 'CRITICAL', 1),
    ('d0000061-0000-0000-0000-000000000002', '6.1.3', 'L''organisme doit définir un plan de traitement et établir une SoA.', 'MUST', 'SoA, plan de traitement', 'SoA à jour avec justification chaque contrôle Annexe A', 'CRITICAL', 1),
    ('d0000091-0000-0000-0000-000000000002', '9.2', 'L''organisme doit réaliser des audits internes SMSI à intervalles planifiés.', 'MUST', 'Programme et rapports d''audit', 'Audit interne ≥ 1/an, couvre périmètre SoA', 'CRITICAL', 1),
    ('d0000101-0000-0000-0000-000000000002', '10.1', 'L''organisme doit améliorer en continu la pertinence et l''efficacité du SMSI.', 'MUST', 'Plan amélioration, indicateurs', 'Suivi PDCA documenté', 'HIGH', 1);
