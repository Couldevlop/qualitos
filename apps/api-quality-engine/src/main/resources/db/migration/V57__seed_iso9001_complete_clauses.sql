-- Complète le référentiel ISO 9001:2015 (Standards Hub, CLAUDE.md §8).
-- V10 n'avait seedé qu'un échantillon (sections 4,5,9,10 partielles ; 6,7,8 vides).
-- Cette migration ajoute les clauses des sections 6,7,8 et les exigences manquantes
-- afin que le scoring d'alignement et l'audit blanc portent sur la norme COMPLÈTE.
-- Réutilise les sections existantes (a3333333=§6, a4444444=§7, a5555555=§8, a6666666=§9).
-- Les requirements omettent l'id (DEFAULT gen_random_uuid()), comme V10.

-- ============================================================================
-- §5 Leadership — compléments (orientation client)
-- ============================================================================
INSERT INTO standard_requirements (clause_id, code, text, obligation, evidence_types, measurable_criteria, risk_if_missing, order_index) VALUES
    ('c0000051-0000-0000-0000-000000000001', '5.1.2', 'La direction doit démontrer son leadership vis-à-vis de l''orientation client en assurant la détermination et le respect des exigences clients et réglementaires.', 'MUST', 'Revue des exigences clients, indicateurs satisfaction', 'Exigences clients identifiées et suivies', 'HIGH', 2);

-- ============================================================================
-- §6 Planification — clauses 6.1, 6.2, 6.3
-- ============================================================================
INSERT INTO standard_clauses (id, section_id, code, title, description, order_index) VALUES
    ('c0000061-0000-0000-0000-000000000001', 'a3333333-3333-3333-3333-333333333333', '6.1', 'Actions à mettre en œuvre face aux risques et opportunités', NULL, 1),
    ('c0000061-0000-0000-0000-000000000002', 'a3333333-3333-3333-3333-333333333333', '6.2', 'Objectifs qualité et planification des actions pour les atteindre', NULL, 2),
    ('c0000061-0000-0000-0000-000000000003', 'a3333333-3333-3333-3333-333333333333', '6.3', 'Planification des modifications', NULL, 3);

INSERT INTO standard_requirements (clause_id, code, text, obligation, evidence_types, measurable_criteria, risk_if_missing, order_index) VALUES
    ('c0000061-0000-0000-0000-000000000001', '6.1.1', 'Lors de la planification du SMQ, l''organisme doit tenir compte des enjeux (4.1) et des exigences (4.2) et déterminer les risques et opportunités à traiter.', 'MUST', 'Analyse des risques et opportunités', 'Risques/opportunités documentés et reliés aux enjeux', 'HIGH', 1),
    ('c0000061-0000-0000-0000-000000000001', '6.1.2', 'L''organisme doit planifier les actions de traitement des risques et opportunités et la manière de les intégrer aux processus du SMQ et d''en évaluer l''efficacité.', 'MUST', 'Plan d''actions risques, FMEA', 'Actions planifiées et efficacité évaluée', 'HIGH', 2),
    ('c0000061-0000-0000-0000-000000000002', '6.2.1', 'L''organisme doit établir des objectifs qualité aux fonctions, niveaux et processus pertinents, cohérents avec la politique qualité, mesurables et surveillés.', 'MUST', 'Objectifs qualité SMART, tableau de bord KPI', 'Objectifs mesurables définis et suivis', 'HIGH', 3),
    ('c0000061-0000-0000-0000-000000000002', '6.2.2', 'L''organisme doit planifier comment atteindre ses objectifs qualité : ce qui sera fait, les ressources, les responsables, les échéances et l''évaluation des résultats.', 'MUST', 'Plans d''actions, jalons', 'Plan d''atteinte des objectifs documenté', 'MEDIUM', 4),
    ('c0000061-0000-0000-0000-000000000003', '6.3', 'Lorsque l''organisme détermine le besoin de modifier le SMQ, ces modifications doivent être réalisées de façon planifiée.', 'MUST', 'Gestion des changements, analyse d''impact', 'Modifications du SMQ planifiées et tracées', 'MEDIUM', 5);

-- ============================================================================
-- §7 Support — clauses 7.1 à 7.5
-- ============================================================================
INSERT INTO standard_clauses (id, section_id, code, title, description, order_index) VALUES
    ('c0000071-0000-0000-0000-000000000001', 'a4444444-4444-4444-4444-444444444444', '7.1', 'Ressources', NULL, 1),
    ('c0000071-0000-0000-0000-000000000002', 'a4444444-4444-4444-4444-444444444444', '7.2', 'Compétences', NULL, 2),
    ('c0000071-0000-0000-0000-000000000003', 'a4444444-4444-4444-4444-444444444444', '7.3', 'Sensibilisation', NULL, 3),
    ('c0000071-0000-0000-0000-000000000004', 'a4444444-4444-4444-4444-444444444444', '7.4', 'Communication', NULL, 4),
    ('c0000071-0000-0000-0000-000000000005', 'a4444444-4444-4444-4444-444444444444', '7.5', 'Informations documentées', NULL, 5);

INSERT INTO standard_requirements (clause_id, code, text, obligation, evidence_types, measurable_criteria, risk_if_missing, order_index) VALUES
    ('c0000071-0000-0000-0000-000000000001', '7.1.1', 'L''organisme doit déterminer et fournir les ressources nécessaires à l''établissement, la mise en œuvre, la tenue à jour et l''amélioration continue du SMQ.', 'MUST', 'Plan de ressources, budget qualité', 'Ressources allouées et documentées', 'HIGH', 1),
    ('c0000071-0000-0000-0000-000000000001', '7.1.5', 'L''organisme doit déterminer et fournir les ressources pour la surveillance et la mesure, et assurer la validité des résultats (étalonnage, traçabilité métrologique).', 'MUST', 'Plan de calibration, registres MSA', 'Équipements de mesure étalonnés et tracés', 'HIGH', 2),
    ('c0000071-0000-0000-0000-000000000001', '7.1.6', 'L''organisme doit déterminer les connaissances nécessaires à la mise en œuvre de ses processus et les tenir à jour.', 'SHOULD', 'Base de connaissances, retours d''expérience', 'Connaissances organisationnelles capitalisées', 'LOW', 3),
    ('c0000071-0000-0000-0000-000000000002', '7.2', 'L''organisme doit déterminer les compétences nécessaires, s''assurer que les personnes sont compétentes et conserver des informations documentées comme preuves.', 'MUST', 'Matrice de compétences, attestations de formation', 'Compétences évaluées, écarts traités', 'HIGH', 4),
    ('c0000071-0000-0000-0000-000000000003', '7.3', 'Les personnes effectuant un travail sous le contrôle de l''organisme doivent être sensibilisées à la politique qualité, aux objectifs et à leur contribution.', 'MUST', 'Supports de sensibilisation, émargements', 'Personnel sensibilisé (taux de couverture)', 'MEDIUM', 5),
    ('c0000071-0000-0000-0000-000000000004', '7.4', 'L''organisme doit déterminer les besoins de communication interne et externe pertinents pour le SMQ (quoi, quand, avec qui, comment, qui).', 'MUST', 'Plan de communication', 'Plan de communication formalisé', 'LOW', 6),
    ('c0000071-0000-0000-0000-000000000005', '7.5.1', 'Le SMQ doit inclure les informations documentées exigées par la norme et celles jugées nécessaires par l''organisme.', 'MUST', 'Liste maîtresse des documents', 'Documentation du SMQ établie', 'HIGH', 7),
    ('c0000071-0000-0000-0000-000000000005', '7.5.2', 'Lors de la création/mise à jour des informations documentées, l''organisme doit assurer identification, format et revue/approbation appropriés.', 'MUST', 'Procédure de maîtrise documentaire', 'Documents identifiés, revus et approuvés', 'HIGH', 8),
    ('c0000071-0000-0000-0000-000000000005', '7.5.3', 'Les informations documentées doivent être maîtrisées : disponibilité, protection, distribution, accès, stockage, maîtrise des modifications, conservation.', 'MUST', 'GED versionnée, contrôle d''accès', 'Versioning actif, accès contrôlé, rétention définie', 'CRITICAL', 9);

-- ============================================================================
-- §8 Réalisation des activités opérationnelles — clauses 8.1 à 8.7
-- ============================================================================
INSERT INTO standard_clauses (id, section_id, code, title, description, order_index) VALUES
    ('c0000081-0000-0000-0000-000000000001', 'a5555555-5555-5555-5555-555555555555', '8.1', 'Planification et maîtrise opérationnelles', NULL, 1),
    ('c0000081-0000-0000-0000-000000000002', 'a5555555-5555-5555-5555-555555555555', '8.2', 'Exigences relatives aux produits et services', NULL, 2),
    ('c0000081-0000-0000-0000-000000000003', 'a5555555-5555-5555-5555-555555555555', '8.3', 'Conception et développement de produits et services', NULL, 3),
    ('c0000081-0000-0000-0000-000000000004', 'a5555555-5555-5555-5555-555555555555', '8.4', 'Maîtrise des processus, produits et services fournis par des prestataires externes', NULL, 4),
    ('c0000081-0000-0000-0000-000000000005', 'a5555555-5555-5555-5555-555555555555', '8.5', 'Production et prestation de service', NULL, 5),
    ('c0000081-0000-0000-0000-000000000006', 'a5555555-5555-5555-5555-555555555555', '8.6', 'Libération des produits et services', NULL, 6),
    ('c0000081-0000-0000-0000-000000000007', 'a5555555-5555-5555-5555-555555555555', '8.7', 'Maîtrise des éléments de sortie non conformes', NULL, 7);

INSERT INTO standard_requirements (clause_id, code, text, obligation, evidence_types, measurable_criteria, risk_if_missing, order_index) VALUES
    ('c0000081-0000-0000-0000-000000000001', '8.1', 'L''organisme doit planifier, mettre en œuvre et maîtriser les processus nécessaires à la réalisation des produits et services (critères, ressources, informations documentées).', 'MUST', 'Plans qualité, gammes opératoires', 'Processus opérationnels planifiés et maîtrisés', 'HIGH', 1),
    ('c0000081-0000-0000-0000-000000000002', '8.2.1', 'L''organisme doit établir des processus de communication avec les clients (information produit, traitement des demandes, retours, réclamations).', 'MUST', 'Procédure relation client, registre réclamations', 'Canaux de communication client définis', 'MEDIUM', 2),
    ('c0000081-0000-0000-0000-000000000002', '8.2.2', 'L''organisme doit déterminer les exigences relatives aux produits et services proposés aux clients (réglementaires et propres).', 'MUST', 'Cahier des charges, revue d''offre', 'Exigences produit déterminées', 'HIGH', 3),
    ('c0000081-0000-0000-0000-000000000002', '8.2.3', 'L''organisme doit revoir sa capacité à satisfaire les exigences avant de s''engager à fournir des produits et services au client.', 'MUST', 'Revue de contrat/commande', 'Revue formalisée avant engagement', 'HIGH', 4),
    ('c0000081-0000-0000-0000-000000000003', '8.3.1', 'Lorsque applicable, l''organisme doit établir un processus de conception et développement adapté pour assurer la fourniture ultérieure.', 'SHOULD', 'Procédure de conception, jalons', 'Processus de conception maîtrisé (si applicable)', 'MEDIUM', 5),
    ('c0000081-0000-0000-0000-000000000004', '8.4.1', 'L''organisme doit s''assurer que les processus, produits et services fournis par des prestataires externes sont conformes aux exigences.', 'MUST', 'Évaluation fournisseurs, contrats', 'Prestataires externes évalués et sélectionnés', 'HIGH', 6),
    ('c0000081-0000-0000-0000-000000000004', '8.4.2', 'L''organisme doit définir le type et l''étendue de la maîtrise exercée sur les prestataires externes et leurs livrables.', 'MUST', 'Plan de surveillance fournisseurs, audits', 'Maîtrise des fournisseurs proportionnée au risque', 'MEDIUM', 7),
    ('c0000081-0000-0000-0000-000000000005', '8.5.1', 'L''organisme doit mettre en œuvre la production et la prestation de service dans des conditions maîtrisées (instructions, équipements, surveillance).', 'MUST', 'Instructions de travail, enregistrements de production', 'Conditions maîtrisées appliquées', 'HIGH', 8),
    ('c0000081-0000-0000-0000-000000000005', '8.5.2', 'L''organisme doit identifier les éléments de sortie et leur état, et maîtriser l''identification unique lorsque la traçabilité est exigée.', 'MUST', 'Système d''identification/traçabilité (lots, n° série)', 'Traçabilité assurée lorsque requise', 'MEDIUM', 9),
    ('c0000081-0000-0000-0000-000000000006', '8.6', 'L''organisme doit vérifier que les exigences relatives aux produits et services ont été satisfaites avant leur libération au client.', 'MUST', 'Contrôles libération, PV de réception', 'Libération conditionnée à la conformité', 'HIGH', 10),
    ('c0000081-0000-0000-0000-000000000007', '8.7', 'L''organisme doit identifier et maîtriser les éléments de sortie non conformes pour empêcher leur utilisation ou fourniture non intentionnelle.', 'MUST', 'Procédure NC, registre des non-conformités', 'Sorties non conformes isolées et traitées', 'CRITICAL', 11);

-- ============================================================================
-- §9 Évaluation des performances — compléments (9.1 surveillance & satisfaction)
-- ============================================================================
INSERT INTO standard_requirements (clause_id, code, text, obligation, evidence_types, measurable_criteria, risk_if_missing, order_index) VALUES
    ('c0000091-0000-0000-0000-000000000001', '9.1.1', 'L''organisme doit déterminer ce qui doit être surveillé et mesuré, les méthodes, le moment et l''analyse, pour évaluer la performance et l''efficacité du SMQ.', 'MUST', 'Plan de surveillance et mesure, tableaux KPI', 'Indicateurs définis et analysés périodiquement', 'HIGH', 1),
    ('c0000091-0000-0000-0000-000000000001', '9.1.2', 'L''organisme doit surveiller la perception des clients sur le niveau de satisfaction de leurs besoins et attentes.', 'MUST', 'Enquêtes satisfaction, NPS, analyse réclamations', 'Satisfaction client mesurée et exploitée', 'HIGH', 2),
    ('c0000091-0000-0000-0000-000000000001', '9.1.3', 'L''organisme doit analyser et évaluer les données et informations appropriées issues de la surveillance et de la mesure.', 'MUST', 'Revues d''indicateurs, analyses statistiques', 'Données analysées pour décision', 'MEDIUM', 3);

-- ============================================================================
-- §10 Amélioration — complément (10.1 généralités)
-- ============================================================================
INSERT INTO standard_requirements (clause_id, code, text, obligation, evidence_types, measurable_criteria, risk_if_missing, order_index) VALUES
    ('c0000101-0000-0000-0000-000000000001', '10.1', 'L''organisme doit déterminer et sélectionner les opportunités d''amélioration et entreprendre les actions nécessaires pour satisfaire le client et accroître sa satisfaction.', 'MUST', 'Plan d''amélioration, projets DMAIC/PDCA', 'Opportunités d''amélioration identifiées et traitées', 'MEDIUM', 1);
