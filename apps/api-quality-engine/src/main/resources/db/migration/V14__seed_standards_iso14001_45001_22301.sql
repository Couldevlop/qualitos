-- Extension Standards Hub (CLAUDE.md §17 P2).
-- ISO 14001:2015 (environnement), ISO 45001:2018 (santé & sécurité au travail),
-- ISO 22301:2019 (continuité d'activité). Toutes en High Level Structure (HLS),
-- ce qui mutualise les clauses 4-10 avec ISO 9001 pour les systèmes intégrés (IMS).

-- ============================================================================
-- ISO 14001:2015 — Management environnemental
-- ============================================================================

INSERT INTO standards (id, code, full_name, publisher, current_version, publication_date,
    family, applicable_industries, description, certification_body_required,
    recertification_cycle_months, related_norm_codes, status, created_at, updated_at)
VALUES (
    '33333333-3333-3333-3333-333333333333',
    'iso-14001',
    'ISO 14001:2015 — Systèmes de management environnemental — Exigences et lignes directrices',
    'ISO',
    '2015',
    '2015-09-15',
    'HLS',
    'all',
    'Norme internationale spécifiant les exigences d''un système de management environnemental (SME) pour améliorer la performance environnementale, respecter les obligations de conformité et atteindre les objectifs environnementaux.',
    TRUE,
    36,
    'iso-9001,iso-45001,iso-14064',
    'PUBLISHED',
    now(), now()
);

INSERT INTO standard_sections (id, standard_id, code, title, description, order_index) VALUES
    ('e1111111-1111-1111-1111-111111111111', '33333333-3333-3333-3333-333333333333', '4', 'Contexte de l''organisme', 'Compréhension des enjeux internes/externes et des parties intéressées pour le SME.', 4),
    ('e2222222-2222-2222-2222-222222222222', '33333333-3333-3333-3333-333333333333', '5', 'Leadership', 'Engagement direction + politique environnementale + rôles SME.', 5),
    ('e3333333-3333-3333-3333-333333333333', '33333333-3333-3333-3333-333333333333', '6', 'Planification', 'Aspects environnementaux + obligations de conformité + objectifs SME.', 6),
    ('e6666666-6666-6666-6666-666666666666', '33333333-3333-3333-3333-333333333333', '9', 'Évaluation des performances', 'Surveillance, mesure, conformité réglementaire, audit, revue de direction.', 9),
    ('e7777777-7777-7777-7777-777777777777', '33333333-3333-3333-3333-333333333333', '10', 'Amélioration', 'Non-conformités, actions correctives, amélioration continue du SME.', 10);

INSERT INTO standard_clauses (id, section_id, code, title, description, order_index) VALUES
    ('f0000061-0000-0000-0000-000000000001', 'e3333333-3333-3333-3333-333333333333', '6.1.2', 'Aspects environnementaux', 'Identification & évaluation des aspects environnementaux des activités, produits et services.', 1),
    ('f0000061-0000-0000-0000-000000000002', 'e3333333-3333-3333-3333-333333333333', '6.1.3', 'Obligations de conformité', 'Identification des obligations légales et autres applicables.', 2),
    ('f0000051-0000-0000-0000-000000000001', 'e2222222-2222-2222-2222-222222222222', '5.2', 'Politique environnementale', NULL, 2),
    ('f0000091-0000-0000-0000-000000000002', 'e6666666-6666-6666-6666-666666666666', '9.1.2', 'Évaluation de la conformité', 'Évaluation périodique du respect des obligations de conformité.', 2),
    ('f0000091-0000-0000-0000-000000000003', 'e6666666-6666-6666-6666-666666666666', '9.2', 'Audit interne SME', NULL, 3),
    ('f0000101-0000-0000-0000-000000000002', 'e7777777-7777-7777-7777-777777777777', '10.2', 'Non-conformité et action corrective', NULL, 2);

INSERT INTO standard_requirements (clause_id, code, text, obligation, evidence_types, measurable_criteria, risk_if_missing, order_index) VALUES
    ('f0000061-0000-0000-0000-000000000001', '6.1.2', 'L''organisme doit déterminer les aspects environnementaux de ses activités, produits et services qu''il peut maîtriser et ceux sur lesquels il peut avoir une influence, ainsi que leurs impacts environnementaux associés, en tenant compte d''une perspective de cycle de vie.', 'MUST', 'Analyse environnementale + matrice aspects/impacts', 'Mise à jour ≥ 1/an, évaluation systématique impacts significatifs', 'CRITICAL', 1),
    ('f0000061-0000-0000-0000-000000000002', '6.1.3', 'L''organisme doit déterminer et avoir accès aux obligations de conformité relatives à ses aspects environnementaux.', 'MUST', 'Veille réglementaire, registre des obligations', 'Registre maintenu, revue trimestrielle minimum', 'CRITICAL', 1),
    ('f0000051-0000-0000-0000-000000000001', '5.2', 'La direction doit établir, mettre en œuvre et tenir à jour une politique environnementale incluant un engagement pour la prévention de la pollution.', 'MUST', 'Politique environnementale signée', 'Politique datée, signée, diffusée, accessible aux parties intéressées', 'CRITICAL', 1),
    ('f0000091-0000-0000-0000-000000000002', '9.1.2', 'L''organisme doit établir, mettre en œuvre et tenir à jour les processus nécessaires pour évaluer le respect de ses obligations de conformité.', 'MUST', 'Rapports d''évaluation de conformité', 'Évaluation documentée ≥ 1/an', 'CRITICAL', 1),
    ('f0000091-0000-0000-0000-000000000003', '9.2.1', 'L''organisme doit réaliser des audits internes SME à intervalles planifiés.', 'MUST', 'Programme et rapports d''audit interne SME', 'Audit interne ≥ 1/an, couverture complète des processus SME', 'HIGH', 1),
    ('f0000101-0000-0000-0000-000000000002', '10.2.1', 'L''organisme doit réagir aux non-conformités et entreprendre les actions appropriées pour les maîtriser et les corriger.', 'MUST', 'Registre CAPA environnemental', 'Délai traitement < 30j criticité haute', 'CRITICAL', 1);

-- ============================================================================
-- ISO 45001:2018 — Santé et sécurité au travail
-- ============================================================================

INSERT INTO standards (id, code, full_name, publisher, current_version, publication_date,
    family, applicable_industries, description, certification_body_required,
    recertification_cycle_months, related_norm_codes, status, created_at, updated_at)
VALUES (
    '44444444-4444-4444-4444-444444444444',
    'iso-45001',
    'ISO 45001:2018 — Systèmes de management de la santé et de la sécurité au travail — Exigences et lignes directrices',
    'ISO',
    '2018',
    '2018-03-12',
    'HLS',
    'all',
    'Norme internationale spécifiant les exigences d''un système de management de la santé et de la sécurité au travail (SST) pour prévenir les traumatismes et pathologies liés au travail et fournir des lieux de travail sûrs et sains.',
    TRUE,
    36,
    'iso-9001,iso-14001',
    'PUBLISHED',
    now(), now()
);

INSERT INTO standard_sections (id, standard_id, code, title, description, order_index) VALUES
    ('61111111-1111-1111-1111-111111111111', '44444444-4444-4444-4444-444444444444', '5', 'Leadership et participation des travailleurs', 'Engagement direction + politique SST + consultation des travailleurs.', 5),
    ('62222222-2222-2222-2222-222222222222', '44444444-4444-4444-4444-444444444444', '6', 'Planification', 'Identification dangers + évaluation risques + opportunités SST.', 6),
    ('63333333-3333-3333-3333-333333333333', '44444444-4444-4444-4444-444444444444', '8', 'Réalisation des activités opérationnelles', 'Maîtrise opérationnelle, préparation aux urgences.', 8),
    ('66666666-6666-6666-6666-666666666666', '44444444-4444-4444-4444-444444444444', '9', 'Évaluation des performances', 'Surveillance + audit interne SST + revue de direction.', 9),
    ('67777777-7777-7777-7777-777777777777', '44444444-4444-4444-4444-444444444444', '10', 'Amélioration', 'Événements indésirables, non-conformités, amélioration continue.', 10);

INSERT INTO standard_clauses (id, section_id, code, title, description, order_index) VALUES
    ('70000051-0000-0000-0000-000000000001', '61111111-1111-1111-1111-111111111111', '5.4', 'Consultation et participation des travailleurs', 'Mécanismes de consultation et participation effective des travailleurs (et leurs représentants).', 4),
    ('70000061-0000-0000-0000-000000000001', '62222222-2222-2222-2222-222222222222', '6.1.2', 'Identification des dangers et évaluation des risques', NULL, 1),
    ('70000081-0000-0000-0000-000000000001', '63333333-3333-3333-3333-333333333333', '8.2', 'Préparation et réponse aux situations d''urgence', NULL, 2),
    ('70000091-0000-0000-0000-000000000002', '66666666-6666-6666-6666-666666666666', '9.2', 'Audit interne SST', NULL, 2),
    ('70000101-0000-0000-0000-000000000002', '67777777-7777-7777-7777-777777777777', '10.2', 'Événement indésirable, non-conformité et action corrective', NULL, 2);

INSERT INTO standard_requirements (clause_id, code, text, obligation, evidence_types, measurable_criteria, risk_if_missing, order_index) VALUES
    ('70000051-0000-0000-0000-000000000001', '5.4', 'L''organisme doit établir, mettre en œuvre et tenir à jour un processus pour la consultation et la participation des travailleurs à tous les niveaux et fonctions applicables.', 'MUST', 'PV CSE/CSSCT, sondages travailleurs, comités SST', 'Réunions CSE ≥ trimestriel, taux participation > 80%', 'CRITICAL', 1),
    ('70000061-0000-0000-0000-000000000001', '6.1.2.1', 'L''organisme doit établir un processus permanent et proactif d''identification des dangers liés au travail.', 'MUST', 'Document unique d''évaluation des risques (DUER)', 'DUER à jour ≥ annuel, plan d''action associé', 'CRITICAL', 1),
    ('70000061-0000-0000-0000-000000000001', '6.1.2.2', 'L''organisme doit établir un processus pour évaluer les risques SST associés aux dangers identifiés.', 'MUST', 'Méthode d''évaluation (matrice gravité × fréquence)', 'Méthode formalisée, cotation systématique', 'CRITICAL', 2),
    ('70000081-0000-0000-0000-000000000001', '8.2', 'L''organisme doit établir, mettre en œuvre et tenir à jour les processus nécessaires pour se préparer aux situations d''urgence potentielles.', 'MUST', 'POI, plans d''urgence, comptes rendus d''exercices', 'Exercices ≥ 1/an, mise à jour POI ≥ 2 ans', 'CRITICAL', 1),
    ('70000091-0000-0000-0000-000000000002', '9.2.1', 'L''organisme doit réaliser des audits internes SST à intervalles planifiés.', 'MUST', 'Programme et rapports d''audit interne SST', 'Audit interne ≥ 1/an, couverture risques significatifs', 'HIGH', 1),
    ('70000101-0000-0000-0000-000000000002', '10.2.1', 'Lorsqu''un événement indésirable ou une non-conformité se produit, l''organisme doit réagir, examiner et entreprendre les actions correctives appropriées.', 'MUST', 'Registre événements indésirables + CAPA SST', 'Délai investigation < 5j ouvrés pour accidents graves', 'CRITICAL', 1);

-- ============================================================================
-- ISO 22301:2019 — Continuité d'activité (BCMS)
-- ============================================================================

INSERT INTO standards (id, code, full_name, publisher, current_version, publication_date,
    family, applicable_industries, description, certification_body_required,
    recertification_cycle_months, related_norm_codes, status, created_at, updated_at)
VALUES (
    '55555555-5555-5555-5555-555555555555',
    'iso-22301',
    'ISO 22301:2019 — Sécurité et résilience — Systèmes de management de la continuité d''activité — Exigences',
    'ISO',
    '2019',
    '2019-10-31',
    'HLS',
    'all',
    'Norme internationale spécifiant les exigences d''un système de management de la continuité d''activité (SMCA / BCMS) pour planifier, mettre en œuvre, exploiter, surveiller, revoir et améliorer la capacité d''une organisation à fournir des produits ou services à des niveaux acceptables prédéfinis après un incident perturbateur.',
    TRUE,
    36,
    'iso-9001,iso-27001,iso-31000',
    'PUBLISHED',
    now(), now()
);

INSERT INTO standard_sections (id, standard_id, code, title, description, order_index) VALUES
    ('82222222-2222-2222-2222-222222222222', '55555555-5555-5555-5555-555555555555', '6', 'Planification', 'Risques + objectifs BCMS + bilan d''impact sur l''activité (BIA).', 6),
    ('83333333-3333-3333-3333-333333333333', '55555555-5555-5555-5555-555555555555', '8', 'Fonctionnement', 'BIA, appréciation des risques, stratégie de continuité, procédures, exercices.', 8),
    ('86666666-6666-6666-6666-666666666666', '55555555-5555-5555-5555-555555555555', '9', 'Évaluation des performances', 'Surveillance, audit, revue de direction.', 9);

INSERT INTO standard_clauses (id, section_id, code, title, description, order_index) VALUES
    ('90000081-0000-0000-0000-000000000001', '83333333-3333-3333-3333-333333333333', '8.2.2', 'Bilan d''impact sur l''activité (BIA)', 'Évaluation des impacts de la perturbation des activités prioritaires.', 1),
    ('90000081-0000-0000-0000-000000000002', '83333333-3333-3333-3333-333333333333', '8.3', 'Stratégie et solutions de continuité d''activité', NULL, 2),
    ('90000081-0000-0000-0000-000000000003', '83333333-3333-3333-3333-333333333333', '8.4', 'Procédures de continuité d''activité', NULL, 3),
    ('90000081-0000-0000-0000-000000000004', '83333333-3333-3333-3333-333333333333', '8.5', 'Programme d''exercices', NULL, 4),
    ('90000091-0000-0000-0000-000000000001', '86666666-6666-6666-6666-666666666666', '9.2', 'Audit interne BCMS', NULL, 2);

INSERT INTO standard_requirements (clause_id, code, text, obligation, evidence_types, measurable_criteria, risk_if_missing, order_index) VALUES
    ('90000081-0000-0000-0000-000000000001', '8.2.2', 'L''organisme doit mettre en œuvre et tenir à jour un processus de bilan d''impact sur l''activité (BIA) pour déterminer les exigences de continuité et la priorité des activités.', 'MUST', 'Rapport BIA, RTO/RPO par activité prioritaire', 'BIA mise à jour ≥ 2 ans, RTO/RPO documentés', 'CRITICAL', 1),
    ('90000081-0000-0000-0000-000000000002', '8.3', 'L''organisme doit identifier et sélectionner des stratégies de continuité d''activité prenant en compte les options de prévention, préparation, réponse et reprise.', 'MUST', 'Document de stratégie BCMS, options évaluées', 'Stratégie validée direction, alignée RTO', 'CRITICAL', 1),
    ('90000081-0000-0000-0000-000000000003', '8.4.1', 'L''organisme doit mettre en œuvre des procédures de continuité d''activité qui répondent aux objectifs définis.', 'MUST', 'PCA / PCI documentés', 'Procédures testées, version diffusée', 'CRITICAL', 1),
    ('90000081-0000-0000-0000-000000000004', '8.5', 'L''organisme doit mettre en œuvre et tenir à jour un programme d''exercices et de tests de ses procédures de continuité d''activité.', 'MUST', 'Calendrier d''exercices + rapports de tests', 'Exercice ≥ 1/an, debriefing post-exercice formalisé', 'CRITICAL', 1),
    ('90000091-0000-0000-0000-000000000001', '9.2.1', 'L''organisme doit réaliser des audits internes BCMS à intervalles planifiés.', 'MUST', 'Programme et rapports d''audit interne BCMS', 'Audit interne ≥ 1/an', 'HIGH', 1);
