-- Complète les référentiels ISO 27001:2022, ISO 14001:2015 et ISO 45001:2018
-- (Standards Hub, CLAUDE.md §8) afin que le dossier de certification clé en main
-- (roadmap + alignement + audit blanc + export) porte sur la norme COMPLÈTE §4→§10,
-- au même niveau qu'ISO 9001 (V57). Réutilise les sections existantes (V10/V14)
-- et ajoute les sections manquantes + clauses + exigences, sans collision d'ID.
-- Les requirements omettent l'id (DEFAULT gen_random_uuid()).

-- ============================================================================
-- ISO/IEC 27001:2022 — SMSI  (std 2222...)
-- Sections existantes: §4=b1111111 §5=b2222222 §6=b3333333 §9=b6666666 §10=b7777777
-- Clauses existantes : 4.3, 5.2, 6.1.2, 6.1.3, 9.2, 10.1
-- Sections ajoutées  : §7=b4444444 (Support), §8=b5555555 (Fonctionnement)
-- ============================================================================

INSERT INTO standard_sections (id, standard_id, code, title, description, order_index) VALUES
    ('b4444444-4444-4444-4444-444444444444', '22222222-2222-2222-2222-222222222222', '7', 'Support', 'Ressources, compétences, sensibilisation, communication, informations documentées du SMSI.', 7),
    ('b5555555-5555-5555-5555-555555555555', '22222222-2222-2222-2222-222222222222', '8', 'Fonctionnement', 'Planification opérationnelle, appréciation et traitement des risques de sécurité de l''information.', 8);

INSERT INTO standard_clauses (id, section_id, code, title, description, order_index) VALUES
    -- §4
    ('b2700401-0000-0000-0000-000000000001', 'b1111111-1111-1111-1111-111111111111', '4.1', 'Compréhension de l''organisme et de son contexte', NULL, 1),
    ('b2700402-0000-0000-0000-000000000001', 'b1111111-1111-1111-1111-111111111111', '4.2', 'Besoins et attentes des parties intéressées', NULL, 2),
    ('b2700404-0000-0000-0000-000000000001', 'b1111111-1111-1111-1111-111111111111', '4.4', 'Système de management de la sécurité de l''information', NULL, 4),
    -- §5
    ('b2700501-0000-0000-0000-000000000001', 'b2222222-2222-2222-2222-222222222222', '5.1', 'Leadership et engagement', NULL, 1),
    ('b2700503-0000-0000-0000-000000000001', 'b2222222-2222-2222-2222-222222222222', '5.3', 'Rôles, responsabilités et autorités', NULL, 3),
    -- §6
    ('b2700602-0000-0000-0000-000000000001', 'b3333333-3333-3333-3333-333333333333', '6.2', 'Objectifs de sécurité de l''information et planification', NULL, 4),
    ('b2700603-0000-0000-0000-000000000001', 'b3333333-3333-3333-3333-333333333333', '6.3', 'Planification des modifications', NULL, 5),
    -- §7 (nouvelle section)
    ('b2700701-0000-0000-0000-000000000001', 'b4444444-4444-4444-4444-444444444444', '7.1', 'Ressources', NULL, 1),
    ('b2700702-0000-0000-0000-000000000001', 'b4444444-4444-4444-4444-444444444444', '7.2', 'Compétences', NULL, 2),
    ('b2700703-0000-0000-0000-000000000001', 'b4444444-4444-4444-4444-444444444444', '7.3', 'Sensibilisation', NULL, 3),
    ('b2700705-0000-0000-0000-000000000001', 'b4444444-4444-4444-4444-444444444444', '7.5', 'Informations documentées', NULL, 5),
    -- §8 (nouvelle section)
    ('b2700801-0000-0000-0000-000000000001', 'b5555555-5555-5555-5555-555555555555', '8.1', 'Planification et maîtrise opérationnelles', NULL, 1),
    ('b2700802-0000-0000-0000-000000000001', 'b5555555-5555-5555-5555-555555555555', '8.2', 'Appréciation des risques de sécurité de l''information', NULL, 2),
    ('b2700803-0000-0000-0000-000000000001', 'b5555555-5555-5555-5555-555555555555', '8.3', 'Traitement des risques de sécurité de l''information', NULL, 3),
    -- §9
    ('b2700901-0000-0000-0000-000000000001', 'b6666666-6666-6666-6666-666666666666', '9.1', 'Surveillance, mesure, analyse et évaluation', NULL, 1),
    ('b2700903-0000-0000-0000-000000000001', 'b6666666-6666-6666-6666-666666666666', '9.3', 'Revue de direction', NULL, 3),
    -- §10
    ('b2701002-0000-0000-0000-000000000001', 'b7777777-7777-7777-7777-777777777777', '10.2', 'Non-conformité et action corrective', NULL, 2);

INSERT INTO standard_requirements (clause_id, code, text, obligation, evidence_types, measurable_criteria, risk_if_missing, order_index) VALUES
    ('b2700401-0000-0000-0000-000000000001', '4.1', 'L''organisme doit déterminer les enjeux externes et internes pertinents pour le SMSI et qui influent sur sa capacité à atteindre les résultats attendus.', 'MUST', 'Analyse de contexte SSI', 'Enjeux documentés, revue ≥ 1/an', 'HIGH', 1),
    ('b2700402-0000-0000-0000-000000000001', '4.2', 'L''organisme doit déterminer les parties intéressées et leurs exigences pertinentes pour la sécurité de l''information.', 'MUST', 'Registre des parties intéressées', 'Exigences SSI recensées', 'HIGH', 1),
    ('b2700404-0000-0000-0000-000000000001', '4.4', 'L''organisme doit établir, mettre en œuvre, tenir à jour et améliorer en continu un SMSI.', 'MUST', 'Documentation du SMSI', 'SMSI opérationnel et tracé', 'CRITICAL', 1),
    ('b2700501-0000-0000-0000-000000000001', '5.1', 'La direction doit démontrer son leadership et son engagement vis-à-vis du SMSI.', 'MUST', 'PV revue de direction, allocation de ressources', 'Engagement direction documenté', 'CRITICAL', 1),
    ('b2700503-0000-0000-0000-000000000001', '5.3', 'La direction doit attribuer les responsabilités et autorités pour les rôles concernés par la sécurité de l''information.', 'MUST', 'Organigramme SSI, fiches de fonction (RSSI)', 'Rôles SSI assignés', 'HIGH', 1),
    ('b2700602-0000-0000-0000-000000000001', '6.2', 'L''organisme doit établir des objectifs de sécurité de l''information mesurables, cohérents avec la politique, et planifier leur atteinte.', 'MUST', 'Objectifs SSI, plan d''actions', 'Objectifs mesurables et suivis', 'HIGH', 1),
    ('b2700603-0000-0000-0000-000000000001', '6.3', 'Lorsque des modifications du SMSI sont nécessaires, elles doivent être réalisées de façon planifiée.', 'SHOULD', 'Gestion des changements SSI', 'Changements SMSI planifiés', 'MEDIUM', 1),
    ('b2700701-0000-0000-0000-000000000001', '7.1', 'L''organisme doit déterminer et fournir les ressources nécessaires au SMSI.', 'MUST', 'Budget et ressources SSI', 'Ressources allouées', 'HIGH', 1),
    ('b2700702-0000-0000-0000-000000000001', '7.2', 'L''organisme doit s''assurer de la compétence des personnes effectuant un travail influant sur la sécurité de l''information.', 'MUST', 'Matrice de compétences SSI, formations', 'Compétences évaluées et tracées', 'HIGH', 1),
    ('b2700703-0000-0000-0000-000000000001', '7.3', 'Les personnes doivent être sensibilisées à la politique SSI et à leur contribution à l''efficacité du SMSI.', 'MUST', 'Programme de sensibilisation SSI', 'Sensibilisation ≥ 1/an, taux de couverture', 'HIGH', 1),
    ('b2700705-0000-0000-0000-000000000001', '7.5', 'Les informations documentées exigées par le SMSI doivent être maîtrisées (création, mise à jour, disponibilité, protection).', 'MUST', 'Procédure de maîtrise documentaire SSI', 'Versioning et contrôle d''accès actifs', 'HIGH', 1),
    ('b2700801-0000-0000-0000-000000000001', '8.1', 'L''organisme doit planifier, mettre en œuvre et maîtriser les processus nécessaires pour satisfaire aux exigences de sécurité.', 'MUST', 'Procédures opérationnelles de sécurité', 'Processus maîtrisés et tracés', 'CRITICAL', 1),
    ('b2700802-0000-0000-0000-000000000001', '8.2', 'L''organisme doit réaliser des appréciations des risques de sécurité de l''information à intervalles planifiés ou lors de changements significatifs.', 'MUST', 'Rapports d''appréciation des risques', 'Appréciation ≥ 1/an et sur changement majeur', 'CRITICAL', 1),
    ('b2700803-0000-0000-0000-000000000001', '8.3', 'L''organisme doit mettre en œuvre le plan de traitement des risques de sécurité de l''information.', 'MUST', 'Plan de traitement des risques, SoA', 'Plan exécuté, SoA à jour', 'CRITICAL', 1),
    ('b2700901-0000-0000-0000-000000000001', '9.1', 'L''organisme doit évaluer la performance de la sécurité de l''information et l''efficacité du SMSI.', 'MUST', 'Indicateurs SSI, tableaux de bord', 'Mesures définies et analysées', 'HIGH', 1),
    ('b2700903-0000-0000-0000-000000000001', '9.3', 'La direction doit procéder à la revue du SMSI à intervalles planifiés.', 'MUST', 'PV de revue de direction SSI', 'Revue ≥ 1/an avec décisions', 'CRITICAL', 1),
    ('b2701002-0000-0000-0000-000000000001', '10.2', 'Lorsqu''une non-conformité se produit, l''organisme doit réagir, en corriger les effets et entreprendre des actions correctives.', 'MUST', 'Registre des non-conformités SSI, CAPA', 'NC tracées et traitées', 'CRITICAL', 1);

-- ============================================================================
-- ISO 14001:2015 — SME  (std 3333...)
-- Sections existantes: §4=e1111111 §5=e2222222 §6=e3333333 §9=e6666666 §10=e7777777
-- Clauses existantes : 5.2, 6.1.2 (aspects), 6.1.3 (obligations), 9.1.2, 9.2, 10.2
-- Sections ajoutées  : §7=e4444444 (Support), §8=e5555555 (Fonctionnement)
-- ============================================================================

INSERT INTO standard_sections (id, standard_id, code, title, description, order_index) VALUES
    ('e4444444-4444-4444-4444-444444444444', '33333333-3333-3333-3333-333333333333', '7', 'Support', 'Ressources, compétences, sensibilisation, communication, informations documentées du SME.', 7),
    ('e5555555-5555-5555-5555-555555555555', '33333333-3333-3333-3333-333333333333', '8', 'Réalisation des activités opérationnelles', 'Maîtrise opérationnelle et préparation/réponse aux situations d''urgence.', 8);

INSERT INTO standard_clauses (id, section_id, code, title, description, order_index) VALUES
    ('e1400401-0000-0000-0000-000000000001', 'e1111111-1111-1111-1111-111111111111', '4.1', 'Compréhension de l''organisme et de son contexte', NULL, 1),
    ('e1400403-0000-0000-0000-000000000001', 'e1111111-1111-1111-1111-111111111111', '4.3', 'Détermination du domaine d''application du SME', NULL, 3),
    ('e1400501-0000-0000-0000-000000000001', 'e2222222-2222-2222-2222-222222222222', '5.1', 'Leadership et engagement', NULL, 1),
    ('e1400601-0000-0000-0000-000000000001', 'e3333333-3333-3333-3333-333333333333', '6.1.1', 'Généralités — risques et opportunités', NULL, 0),
    ('e1400602-0000-0000-0000-000000000001', 'e3333333-3333-3333-3333-333333333333', '6.2', 'Objectifs environnementaux et planification', NULL, 4),
    ('e1400701-0000-0000-0000-000000000001', 'e4444444-4444-4444-4444-444444444444', '7.2', 'Compétences', NULL, 2),
    ('e1400702-0000-0000-0000-000000000001', 'e4444444-4444-4444-4444-444444444444', '7.4', 'Communication', NULL, 4),
    ('e1400703-0000-0000-0000-000000000001', 'e4444444-4444-4444-4444-444444444444', '7.5', 'Informations documentées', NULL, 5),
    ('e1400801-0000-0000-0000-000000000001', 'e5555555-5555-5555-5555-555555555555', '8.1', 'Planification et maîtrise opérationnelles', NULL, 1),
    ('e1400802-0000-0000-0000-000000000001', 'e5555555-5555-5555-5555-555555555555', '8.2', 'Préparation et réponse aux situations d''urgence', NULL, 2),
    ('e1400901-0000-0000-0000-000000000001', 'e6666666-6666-6666-6666-666666666666', '9.1.1', 'Surveillance, mesure, analyse et évaluation', NULL, 1),
    ('e1400903-0000-0000-0000-000000000001', 'e6666666-6666-6666-6666-666666666666', '9.3', 'Revue de direction', NULL, 4),
    ('e1401003-0000-0000-0000-000000000001', 'e7777777-7777-7777-7777-777777777777', '10.3', 'Amélioration continue', NULL, 3);

INSERT INTO standard_requirements (clause_id, code, text, obligation, evidence_types, measurable_criteria, risk_if_missing, order_index) VALUES
    ('e1400401-0000-0000-0000-000000000001', '4.1', 'L''organisme doit déterminer les enjeux externes et internes pertinents pour le SME, y compris les conditions environnementales susceptibles de l''affecter ou d''être affectées par lui.', 'MUST', 'Analyse de contexte environnemental', 'Enjeux documentés, revue ≥ 1/an', 'HIGH', 1),
    ('e1400403-0000-0000-0000-000000000001', '4.3', 'L''organisme doit déterminer le domaine d''application du SME (limites et applicabilité).', 'MUST', 'Document de périmètre SME', 'Périmètre formalisé', 'MEDIUM', 1),
    ('e1400501-0000-0000-0000-000000000001', '5.1', 'La direction doit démontrer son leadership et son engagement vis-à-vis du SME.', 'MUST', 'PV revue de direction, ressources allouées', 'Engagement direction documenté', 'CRITICAL', 1),
    ('e1400601-0000-0000-0000-000000000001', '6.1.1', 'L''organisme doit déterminer les risques et opportunités liés à ses aspects environnementaux, obligations de conformité et autres enjeux.', 'MUST', 'Analyse risques/opportunités environnementaux', 'Risques/opportunités identifiés', 'HIGH', 1),
    ('e1400602-0000-0000-0000-000000000001', '6.2', 'L''organisme doit établir des objectifs environnementaux mesurables et planifier les actions pour les atteindre.', 'MUST', 'Programme environnemental, objectifs SMART', 'Objectifs mesurables et suivis', 'HIGH', 1),
    ('e1400701-0000-0000-0000-000000000001', '7.2', 'L''organisme doit s''assurer de la compétence des personnes dont le travail a une incidence sur sa performance environnementale.', 'MUST', 'Matrice de compétences, formations', 'Compétences évaluées', 'HIGH', 1),
    ('e1400702-0000-0000-0000-000000000001', '7.4', 'L''organisme doit établir les processus de communication interne et externe pertinents pour le SME, y compris ses obligations de conformité.', 'MUST', 'Plan de communication environnementale', 'Communication tracée', 'MEDIUM', 1),
    ('e1400703-0000-0000-0000-000000000001', '7.5', 'Les informations documentées exigées par le SME doivent être créées, mises à jour et maîtrisées.', 'MUST', 'Procédure de maîtrise documentaire', 'Versioning actif', 'MEDIUM', 1),
    ('e1400801-0000-0000-0000-000000000001', '8.1', 'L''organisme doit établir, mettre en œuvre et maîtriser les processus nécessaires en cohérence avec une perspective de cycle de vie.', 'MUST', 'Procédures de maîtrise opérationnelle', 'Processus maîtrisés', 'HIGH', 1),
    ('e1400802-0000-0000-0000-000000000001', '8.2', 'L''organisme doit établir les processus nécessaires pour se préparer et répondre aux situations d''urgence environnementales potentielles.', 'MUST', 'Plans d''urgence, comptes rendus d''exercices', 'Exercices ≥ 1/an', 'CRITICAL', 1),
    ('e1400901-0000-0000-0000-000000000001', '9.1.1', 'L''organisme doit surveiller, mesurer, analyser et évaluer sa performance environnementale.', 'MUST', 'Indicateurs environnementaux, mesures', 'Indicateurs définis et suivis', 'HIGH', 1),
    ('e1400903-0000-0000-0000-000000000001', '9.3', 'La direction doit procéder à la revue du SME à intervalles planifiés.', 'MUST', 'PV de revue de direction', 'Revue ≥ 1/an avec décisions', 'CRITICAL', 1),
    ('e1401003-0000-0000-0000-000000000001', '10.3', 'L''organisme doit améliorer en continu la pertinence, l''adéquation et l''efficacité du SME.', 'MUST', 'Plan d''amélioration, indicateurs', 'Amélioration documentée', 'MEDIUM', 1);

-- ============================================================================
-- ISO 45001:2018 — SST  (std 4444...)
-- Sections existantes: §5=61111111 §6=62222222 §8=63333333 §9=66666666 §10=67777777
-- Clauses existantes : 5.4, 6.1.2 (dangers), 8.2 (urgence), 9.2, 10.2
-- Sections ajoutées  : §4=64444444 (Contexte), §7=65555555 (Support)
-- ============================================================================

INSERT INTO standard_sections (id, standard_id, code, title, description, order_index) VALUES
    ('64444444-4444-4444-4444-444444444444', '44444444-4444-4444-4444-444444444444', '4', 'Contexte de l''organisme', 'Enjeux, parties intéressées (dont travailleurs) et domaine d''application du SST.', 4),
    ('65555555-5555-5555-5555-555555555555', '44444444-4444-4444-4444-444444444444', '7', 'Support', 'Ressources, compétences, sensibilisation, communication, informations documentées SST.', 7);

INSERT INTO standard_clauses (id, section_id, code, title, description, order_index) VALUES
    ('45000401-0000-0000-0000-000000000001', '64444444-4444-4444-4444-444444444444', '4.1', 'Compréhension de l''organisme et de son contexte', NULL, 1),
    ('45000402-0000-0000-0000-000000000001', '64444444-4444-4444-4444-444444444444', '4.2', 'Besoins et attentes des travailleurs et autres parties intéressées', NULL, 2),
    ('45000501-0000-0000-0000-000000000001', '61111111-1111-1111-1111-111111111111', '5.1', 'Leadership et engagement', NULL, 1),
    ('45000502-0000-0000-0000-000000000001', '61111111-1111-1111-1111-111111111111', '5.2', 'Politique SST', NULL, 2),
    ('45000601-0000-0000-0000-000000000001', '62222222-2222-2222-2222-222222222222', '6.1.3', 'Détermination des exigences légales et autres', NULL, 3),
    ('45000602-0000-0000-0000-000000000001', '62222222-2222-2222-2222-222222222222', '6.2', 'Objectifs SST et planification', NULL, 5),
    ('45000701-0000-0000-0000-000000000001', '65555555-5555-5555-5555-555555555555', '7.2', 'Compétences', NULL, 2),
    ('45000702-0000-0000-0000-000000000001', '65555555-5555-5555-5555-555555555555', '7.3', 'Sensibilisation', NULL, 3),
    ('45000703-0000-0000-0000-000000000001', '65555555-5555-5555-5555-555555555555', '7.5', 'Informations documentées', NULL, 5),
    ('45000801-0000-0000-0000-000000000001', '63333333-3333-3333-3333-333333333333', '8.1', 'Planification et maîtrise opérationnelles', NULL, 1),
    ('45000901-0000-0000-0000-000000000001', '66666666-6666-6666-6666-666666666666', '9.1', 'Surveillance, mesure, analyse et évaluation', NULL, 1),
    ('45000903-0000-0000-0000-000000000001', '66666666-6666-6666-6666-666666666666', '9.3', 'Revue de direction', NULL, 3),
    ('45001003-0000-0000-0000-000000000001', '67777777-7777-7777-7777-777777777777', '10.3', 'Amélioration continue', NULL, 3);

INSERT INTO standard_requirements (clause_id, code, text, obligation, evidence_types, measurable_criteria, risk_if_missing, order_index) VALUES
    ('45000401-0000-0000-0000-000000000001', '4.1', 'L''organisme doit déterminer les enjeux externes et internes pertinents pour le SST.', 'MUST', 'Analyse de contexte SST', 'Enjeux documentés', 'HIGH', 1),
    ('45000402-0000-0000-0000-000000000001', '4.2', 'L''organisme doit déterminer les travailleurs et autres parties intéressées ainsi que leurs besoins et attentes pertinents pour le SST.', 'MUST', 'Registre des parties intéressées SST', 'Besoins travailleurs recensés', 'HIGH', 1),
    ('45000501-0000-0000-0000-000000000001', '5.1', 'La direction doit démontrer son leadership et son engagement, en assumant la responsabilité globale de la prévention des traumatismes et pathologies.', 'MUST', 'PV revue de direction, engagement signé', 'Engagement direction documenté', 'CRITICAL', 1),
    ('45000502-0000-0000-0000-000000000001', '5.2', 'La direction doit établir une politique SST incluant un engagement à fournir des conditions de travail sûres et saines et à éliminer les dangers.', 'MUST', 'Politique SST signée', 'Politique datée, signée, diffusée', 'CRITICAL', 1),
    ('45000601-0000-0000-0000-000000000001', '6.1.3', 'L''organisme doit déterminer et avoir accès aux exigences légales et autres applicables à ses dangers et risques SST.', 'MUST', 'Veille réglementaire SST, registre', 'Registre maintenu et à jour', 'CRITICAL', 1),
    ('45000602-0000-0000-0000-000000000001', '6.2', 'L''organisme doit établir des objectifs SST mesurables et planifier les actions pour les atteindre.', 'MUST', 'Programme SST, objectifs SMART', 'Objectifs mesurables et suivis', 'HIGH', 1),
    ('45000701-0000-0000-0000-000000000001', '7.2', 'L''organisme doit s''assurer de la compétence des travailleurs (dont l''aptitude à identifier les dangers).', 'MUST', 'Habilitations, matrice de compétences SST', 'Compétences/habilitations à jour', 'CRITICAL', 1),
    ('45000702-0000-0000-0000-000000000001', '7.3', 'Les travailleurs doivent être sensibilisés à la politique SST, aux dangers et risques, et aux mesures de prévention.', 'MUST', 'Accueil sécurité, sensibilisations', 'Sensibilisation tracée, taux de couverture', 'HIGH', 1),
    ('45000703-0000-0000-0000-000000000001', '7.5', 'Les informations documentées exigées par le SST doivent être créées, mises à jour et maîtrisées.', 'MUST', 'Procédure de maîtrise documentaire SST', 'Versioning actif', 'MEDIUM', 1),
    ('45000801-0000-0000-0000-000000000001', '8.1', 'L''organisme doit planifier, mettre en œuvre et maîtriser les processus pour satisfaire aux exigences SST et appliquer la hiérarchie des mesures de prévention.', 'MUST', 'Procédures de maîtrise opérationnelle, plans de prévention', 'Hiérarchie des mesures appliquée', 'CRITICAL', 1),
    ('45000901-0000-0000-0000-000000000001', '9.1', 'L''organisme doit surveiller, mesurer, analyser et évaluer la performance SST.', 'MUST', 'Indicateurs SST (TF, TG), mesures', 'Indicateurs définis et suivis', 'HIGH', 1),
    ('45000903-0000-0000-0000-000000000001', '9.3', 'La direction doit procéder à la revue du SST à intervalles planifiés.', 'MUST', 'PV de revue de direction SST', 'Revue ≥ 1/an avec décisions', 'CRITICAL', 1),
    ('45001003-0000-0000-0000-000000000001', '10.3', 'L''organisme doit améliorer en continu la pertinence, l''adéquation et l''efficacité du SST.', 'MUST', 'Plan d''amélioration SST', 'Amélioration documentée', 'MEDIUM', 1);

