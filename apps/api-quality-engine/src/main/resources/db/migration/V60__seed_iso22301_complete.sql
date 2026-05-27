-- Complète ISO 22301:2019 (continuité d'activité, BCMS) en structure HLS §4→§10
-- au même niveau que 9001/27001/14001/45001 → set IMS complet (CLAUDE.md §8.9).
-- std 5555..., sections existantes (V14): §6=82222222 §8=83333333 §9=86666666
-- clauses existantes: 8.2.2 (BIA), 8.3, 8.4 (8.4.1), 8.5, 9.2
-- Sections ajoutées: §4=84444444 §5=85555555 §7=87777777 §10=88888888

INSERT INTO standard_sections (id, standard_id, code, title, description, order_index) VALUES
    ('84444444-4444-4444-4444-444444444444', '55555555-5555-5555-5555-555555555555', '4', 'Contexte de l''organisme', 'Enjeux, parties intéressées (exigences légales/réglementaires) et domaine d''application du BCMS.', 4),
    ('85555555-5555-5555-5555-555555555555', '55555555-5555-5555-5555-555555555555', '5', 'Leadership', 'Engagement direction, politique de continuité, rôles et responsabilités.', 5),
    ('87777777-7777-7777-7777-777777777777', '55555555-5555-5555-5555-555555555555', '7', 'Support', 'Ressources, compétences, sensibilisation, communication, informations documentées du BCMS.', 7),
    ('88888888-8888-8888-8888-888888888888', '55555555-5555-5555-5555-555555555555', '10', 'Amélioration', 'Non-conformités, actions correctives, amélioration continue du BCMS.', 10);

INSERT INTO standard_clauses (id, section_id, code, title, description, order_index) VALUES
    -- §4
    ('22300401-0000-0000-0000-000000000001', '84444444-4444-4444-4444-444444444444', '4.1', 'Compréhension de l''organisme et de son contexte', NULL, 1),
    ('22300402-0000-0000-0000-000000000001', '84444444-4444-4444-4444-444444444444', '4.2', 'Besoins et attentes des parties intéressées', NULL, 2),
    ('22300404-0000-0000-0000-000000000001', '84444444-4444-4444-4444-444444444444', '4.4', 'Système de management de la continuité d''activité', NULL, 4),
    -- §5
    ('22300501-0000-0000-0000-000000000001', '85555555-5555-5555-5555-555555555555', '5.1', 'Leadership et engagement', NULL, 1),
    ('22300502-0000-0000-0000-000000000001', '85555555-5555-5555-5555-555555555555', '5.2', 'Politique de continuité d''activité', NULL, 2),
    ('22300503-0000-0000-0000-000000000001', '85555555-5555-5555-5555-555555555555', '5.3', 'Rôles, responsabilités et autorités', NULL, 3),
    -- §6 (section existante, sans clause)
    ('22300601-0000-0000-0000-000000000001', '82222222-2222-2222-2222-222222222222', '6.1', 'Actions face aux risques et opportunités', NULL, 1),
    ('22300602-0000-0000-0000-000000000001', '82222222-2222-2222-2222-222222222222', '6.2', 'Objectifs de continuité d''activité et planification', NULL, 2),
    -- §7
    ('22300701-0000-0000-0000-000000000001', '87777777-7777-7777-7777-777777777777', '7.1', 'Ressources', NULL, 1),
    ('22300702-0000-0000-0000-000000000001', '87777777-7777-7777-7777-777777777777', '7.2', 'Compétences', NULL, 2),
    ('22300704-0000-0000-0000-000000000001', '87777777-7777-7777-7777-777777777777', '7.4', 'Communication', NULL, 4),
    ('22300705-0000-0000-0000-000000000001', '87777777-7777-7777-7777-777777777777', '7.5', 'Informations documentées', NULL, 5),
    -- §8 (section existante) — compléments
    ('22300801-0000-0000-0000-000000000001', '83333333-3333-3333-3333-333333333333', '8.1', 'Planification et maîtrise opérationnelles', NULL, 0),
    ('22300844-0000-0000-0000-000000000001', '83333333-3333-3333-3333-333333333333', '8.4.4', 'Plans de continuité d''activité', NULL, 5),
    -- §9 (section existante) — compléments
    ('22300901-0000-0000-0000-000000000001', '86666666-6666-6666-6666-666666666666', '9.1', 'Surveillance, mesure, analyse et évaluation', NULL, 1),
    ('22300903-0000-0000-0000-000000000001', '86666666-6666-6666-6666-666666666666', '9.3', 'Revue de direction', NULL, 3),
    -- §10
    ('22301001-0000-0000-0000-000000000001', '88888888-8888-8888-8888-888888888888', '10.1', 'Non-conformité et action corrective', NULL, 1),
    ('22301002-0000-0000-0000-000000000001', '88888888-8888-8888-8888-888888888888', '10.2', 'Amélioration continue', NULL, 2);

INSERT INTO standard_requirements (clause_id, code, text, obligation, evidence_types, measurable_criteria, risk_if_missing, order_index) VALUES
    ('22300401-0000-0000-0000-000000000001', '4.1', 'L''organisme doit déterminer les enjeux externes et internes pertinents pour le BCMS et ses résultats attendus.', 'MUST', 'Analyse de contexte BCMS', 'Enjeux documentés, revue ≥ 1/an', 'HIGH', 1),
    ('22300402-0000-0000-0000-000000000001', '4.2', 'L''organisme doit déterminer les parties intéressées pertinentes pour le BCMS ainsi que leurs exigences, y compris légales et réglementaires.', 'MUST', 'Registre des parties intéressées + obligations légales', 'Exigences recensées et tracées', 'HIGH', 1),
    ('22300404-0000-0000-0000-000000000001', '4.4', 'L''organisme doit établir, mettre en œuvre, tenir à jour et améliorer en continu un BCMS.', 'MUST', 'Documentation du BCMS', 'BCMS opérationnel et tracé', 'CRITICAL', 1),
    ('22300501-0000-0000-0000-000000000001', '5.1', 'La direction doit démontrer son leadership et son engagement vis-à-vis du BCMS.', 'MUST', 'PV revue de direction, ressources allouées', 'Engagement direction documenté', 'CRITICAL', 1),
    ('22300502-0000-0000-0000-000000000001', '5.2', 'La direction doit établir une politique de continuité d''activité adaptée à la finalité de l''organisme.', 'MUST', 'Politique BCMS signée', 'Politique datée, signée, diffusée', 'CRITICAL', 1),
    ('22300503-0000-0000-0000-000000000001', '5.3', 'La direction doit attribuer les responsabilités et autorités pour les rôles concernés par le BCMS.', 'MUST', 'Organigramme de crise, fiches de fonction', 'Rôles BCMS assignés', 'HIGH', 1),
    ('22300601-0000-0000-0000-000000000001', '6.1', 'L''organisme doit déterminer les risques et opportunités à traiter pour assurer que le BCMS atteint ses résultats attendus.', 'MUST', 'Analyse risques/opportunités BCMS', 'Risques/opportunités identifiés', 'HIGH', 1),
    ('22300602-0000-0000-0000-000000000001', '6.2', 'L''organisme doit établir des objectifs de continuité d''activité mesurables et cohérents avec la politique.', 'MUST', 'Objectifs BCMS, plan d''actions', 'Objectifs mesurables et suivis', 'HIGH', 1),
    ('22300701-0000-0000-0000-000000000001', '7.1', 'L''organisme doit déterminer et fournir les ressources nécessaires au BCMS.', 'MUST', 'Budget et ressources BCMS', 'Ressources allouées', 'HIGH', 1),
    ('22300702-0000-0000-0000-000000000001', '7.2', 'L''organisme doit s''assurer de la compétence des personnes intervenant dans le BCMS.', 'MUST', 'Matrice de compétences, formations crise', 'Compétences évaluées et tracées', 'HIGH', 1),
    ('22300704-0000-0000-0000-000000000001', '7.4', 'L''organisme doit déterminer les besoins de communication interne et externe pertinents pour le BCMS.', 'MUST', 'Plan de communication de crise', 'Plan formalisé et testé', 'HIGH', 1),
    ('22300705-0000-0000-0000-000000000001', '7.5', 'Les informations documentées exigées par le BCMS doivent être créées, mises à jour et maîtrisées.', 'MUST', 'Procédure de maîtrise documentaire BCMS', 'Versioning et accès contrôlés', 'MEDIUM', 1),
    ('22300801-0000-0000-0000-000000000001', '8.1', 'L''organisme doit planifier, mettre en œuvre et maîtriser les processus nécessaires pour satisfaire aux exigences et aux actions déterminées.', 'MUST', 'Procédures opérationnelles BCMS', 'Processus maîtrisés et tracés', 'HIGH', 1),
    ('22300844-0000-0000-0000-000000000001', '8.4.4', 'L''organisme doit établir et tenir à jour des plans de continuité d''activité documentés détaillant les rôles, procédures et ressources pour la reprise.', 'MUST', 'Plans de continuité (PCA/PCI), fiches réflexes', 'Plans à jour, rôles et procédures définis', 'CRITICAL', 1),
    ('22300901-0000-0000-0000-000000000001', '9.1', 'L''organisme doit évaluer la performance de la continuité d''activité et l''efficacité du BCMS.', 'MUST', 'Indicateurs BCMS, rapports de tests', 'Mesures définies et analysées', 'HIGH', 1),
    ('22300903-0000-0000-0000-000000000001', '9.3', 'La direction doit procéder à la revue du BCMS à intervalles planifiés.', 'MUST', 'PV de revue de direction BCMS', 'Revue ≥ 1/an avec décisions', 'CRITICAL', 1),
    ('22301001-0000-0000-0000-000000000001', '10.1', 'Lorsqu''une non-conformité se produit, l''organisme doit réagir, en corriger les effets et entreprendre des actions correctives.', 'MUST', 'Registre des non-conformités BCMS, CAPA', 'NC tracées et traitées', 'CRITICAL', 1),
    ('22301002-0000-0000-0000-000000000001', '10.2', 'L''organisme doit améliorer en continu la pertinence, l''adéquation et l''efficacité du BCMS.', 'MUST', 'Plan d''amélioration BCMS', 'Amélioration documentée', 'MEDIUM', 1);
