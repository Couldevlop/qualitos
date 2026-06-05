-- Standards Hub — vague 3 (CLAUDE.md §8.2) : 4 normes inter-secteurs.
--   iso-27701   : Privacy Information Management (synergie modules RGPD)
--   iso-20000-1 : Management des services IT (pack IT/ITSM)
--   iso-50001   : Management de l'énergie (pack Énergie/Utilities)
--   qualiopi    : Qualité des organismes de formation (France, pack Éducation)
-- Même format que V52 : catalogue platform-level, UUID déterministes
-- (standards 7000000n, sections 7n00000s, clauses 7[a-d]…, paths 7e…).

-- ============================================================================
-- ISO/IEC 27701:2019 — Privacy Information Management System (PIMS)
-- ============================================================================

INSERT INTO standards (id, code, full_name, publisher, current_version, publication_date,
    family, applicable_industries, description, certification_body_required,
    recertification_cycle_months, related_norm_codes, status, created_at, updated_at)
VALUES (
    '70000001-0000-0000-0000-000000000001',
    'iso-27701',
    'ISO/IEC 27701:2019 — Techniques de sécurité — Extension d''ISO/IEC 27001 et ISO/IEC 27002 au management de la protection de la vie privée',
    'ISO/IEC',
    '2019',
    DATE '2019-08-06',
    'HLS',
    'all',
    'Étend le SMSI ISO 27001 en PIMS : exigences et lignes directrices pour responsables de traitement (PII controllers) et sous-traitants (PII processors). Démontre la conformité RGPD de manière certifiable.',
    TRUE,
    36,
    'iso-27001,rgpd',
    'PUBLISHED', now(), now()
);

INSERT INTO standard_sections (id, standard_id, code, title, description, order_index) VALUES
    ('71000001-0000-0000-0000-000000000001', '70000001-0000-0000-0000-000000000001', '5', 'Exigences PIMS liées à ISO 27001', 'Extensions des clauses 4 à 10 d''ISO 27001 au périmètre vie privée.', 1),
    ('71000001-0000-0000-0000-000000000002', '70000001-0000-0000-0000-000000000001', '6', 'Lignes directrices PIMS liées à ISO 27002', 'Extensions des mesures de sécurité au traitement de PII.', 2),
    ('71000001-0000-0000-0000-000000000003', '70000001-0000-0000-0000-000000000001', '7', 'Lignes directrices pour les PII controllers', 'Obligations spécifiques aux responsables de traitement.', 3),
    ('71000001-0000-0000-0000-000000000004', '70000001-0000-0000-0000-000000000001', '8', 'Lignes directrices pour les PII processors', 'Obligations spécifiques aux sous-traitants.', 4);

INSERT INTO standard_clauses (id, section_id, code, title, description, order_index) VALUES
    ('7a000001-0000-0000-0000-000000000001', '71000001-0000-0000-0000-000000000001', '5.2', 'Contexte de l''organisme (PIMS)', 'Déterminer son rôle (controller/processor) et les parties intéressées vie privée.', 1),
    ('7a000001-0000-0000-0000-000000000002', '71000001-0000-0000-0000-000000000001', '5.4', 'Planification (PIMS)', 'Appréciation et traitement des risques vie privée intégrés au SMSI.', 2),
    ('7a000001-0000-0000-0000-000000000003', '71000001-0000-0000-0000-000000000002', '6.5', 'Classification et contrôle d''accès aux PII', 'Classification de l''information incluant les PII, moindre privilège.', 3),
    ('7a000001-0000-0000-0000-000000000004', '71000001-0000-0000-0000-000000000002', '6.13', 'Gestion des incidents touchant des PII', 'Processus de notification de violation aux autorités et personnes concernées.', 4),
    ('7a000001-0000-0000-0000-000000000005', '71000001-0000-0000-0000-000000000003', '7.2', 'Conditions de collecte et de traitement', 'Finalités, base légale, consentement, registre des traitements.', 5),
    ('7a000001-0000-0000-0000-000000000006', '71000001-0000-0000-0000-000000000003', '7.3', 'Obligations envers les personnes concernées', 'Information, exercice des droits (accès, rectification, effacement…).', 6),
    ('7a000001-0000-0000-0000-000000000007', '71000001-0000-0000-0000-000000000004', '8.2', 'Conditions de traitement pour le processor', 'Traiter uniquement sur instruction documentée du controller (contrat).', 7),
    ('7a000001-0000-0000-0000-000000000008', '71000001-0000-0000-0000-000000000004', '8.5', 'Transferts de PII et sous-traitance ultérieure', 'Encadrement des transferts internationaux et des sous-traitants de rang 2.', 8);

INSERT INTO standard_requirements (clause_id, code, text, obligation, evidence_types, measurable_criteria, risk_if_missing, order_index) VALUES
    ('7a000001-0000-0000-0000-000000000001', '5.2.1', 'L''organisme doit déterminer son rôle de PII controller et/ou PII processor pour chaque traitement.', 'MUST', 'DOCUMENT', 'Rôle documenté par traitement dans le registre', 'HIGH', 1),
    ('7a000001-0000-0000-0000-000000000002', '5.4.1', 'L''appréciation des risques sécurité doit intégrer les risques pour les droits et libertés des personnes concernées.', 'MUST', 'DOCUMENT,AUDIT', 'Critères de risque vie privée définis ; AIPD reliées', 'CRITICAL', 2),
    ('7a000001-0000-0000-0000-000000000003', '6.5.1', 'Les PII doivent être classifiées et l''accès restreint au besoin d''en connaître.', 'MUST', 'DOCUMENT,AUDIT', 'Schéma de classification incluant PII ; revues d''accès périodiques', 'HIGH', 3),
    ('7a000001-0000-0000-0000-000000000004', '6.13.1', 'Le processus de gestion d''incident doit couvrir la notification des violations de PII dans les délais réglementaires.', 'MUST', 'DOCUMENT,CAPA', 'Procédure de notification ≤ 72 h ; registre des violations tenu', 'CRITICAL', 4),
    ('7a000001-0000-0000-0000-000000000005', '7.2.1', 'Identifier et documenter la finalité et la base légale de chaque traitement de PII.', 'MUST', 'DOCUMENT', 'Registre des traitements complet (finalité + base légale par traitement)', 'CRITICAL', 5),
    ('7a000001-0000-0000-0000-000000000005', '7.2.2', 'Obtenir et conserver la preuve du consentement quand il constitue la base légale.', 'MUST', 'DOCUMENT,KPI_RECORD', 'Horodatage et portée du consentement conservés', 'HIGH', 6),
    ('7a000001-0000-0000-0000-000000000006', '7.3.1', 'Fournir aux personnes concernées les moyens d''exercer leurs droits et y répondre dans les délais.', 'MUST', 'DOCUMENT,KPI_RECORD', 'Délai de réponse DSAR ≤ 30 jours ; taux de réponse suivi', 'HIGH', 7),
    ('7a000001-0000-0000-0000-000000000007', '8.2.1', 'Ne traiter les PII que sur instructions documentées du client (contrat ou CGU annexées).', 'MUST', 'DOCUMENT', 'Clauses contractuelles (DPA) signées pour 100 % des clients', 'CRITICAL', 8),
    ('7a000001-0000-0000-0000-000000000008', '8.5.1', 'Informer le client de tout transfert international ou sous-traitant ultérieur et obtenir son autorisation.', 'MUST', 'DOCUMENT', 'Registre des transferts + autorisations écrites', 'HIGH', 9);

INSERT INTO standard_certification_paths (id, standard_id, estimated_duration_months_min, estimated_duration_months_max,
    estimated_cost_eur_min, estimated_cost_eur_max, difficulty_level,
    surveillance_audit_frequency, recertification_cycle_years, notes)
VALUES ('7e000001-0000-0000-0000-000000000001', '70000001-0000-0000-0000-000000000001',
    6, 12, 15000, 60000, 4, 'annual', 3,
    'Pré-requis : SMSI ISO 27001 certifié ou en cours. Extension d''audit conjointe possible (mutualisation HLS §8.9). Synergie directe avec les modules RGPD QualitOS (RoPA, consentements, DSAR, violations).');

INSERT INTO standard_certification_stages (certification_path_id, stage_number, name, description, typical_duration_weeks, deliverables, actors, qualitos_modules, order_index) VALUES
    ('7e000001-0000-0000-0000-000000000001', 1, 'Cadrage PIMS + rôles controller/processor', 'Périmètre PIMS, qualification des rôles par traitement, engagement direction.', '2-3', 'Périmètre PIMS, matrice des rôles', 'Direction, DPO', 'Document Control, RoPA', 1),
    ('7e000001-0000-0000-0000-000000000001', 2, 'Gap analysis 27701 vs SMSI existant', 'Écarts entre le SMSI 27001 et les exigences PIMS (clauses 5-8).', '3-4', 'Rapport d''écarts, plan de remédiation', 'DPO, RSSI', 'Audit, Standards Hub', 2),
    ('7e000001-0000-0000-0000-000000000001', 3, 'Mise à niveau registre + bases légales', 'Complétion du registre des traitements, bases légales, AIPD manquantes.', '4-8', 'Registre complet, AIPD', 'DPO, Métiers', 'RoPA, DPIA', 3),
    ('7e000001-0000-0000-0000-000000000001', 4, 'Processus droits des personnes + violations', 'Outillage DSAR, procédure de notification 72 h, tests.', '3-5', 'Procédures DSAR/violation, registre violations', 'DPO, Support', 'Subject Requests, Breaches', 4),
    ('7e000001-0000-0000-0000-000000000001', 5, 'Audit interne PIMS + revue de direction', 'Audit interne couvrant les extensions PIMS, revue de direction conjointe SMSI/PIMS.', '3-4', 'Rapport d''audit interne, CR revue', 'Auditeur interne, Direction', 'Audit, PDCA', 5),
    ('7e000001-0000-0000-0000-000000000001', 6, 'Audit de certification (étapes 1+2)', 'Audit documentaire puis audit sur site par le certificateur (extension du SMSI).', '2-4', 'Certificat ISO 27701', 'Organisme certificateur', 'Standards Hub', 6);

-- ============================================================================
-- ISO/IEC 20000-1:2018 — Management des services IT (ITSM)
-- ============================================================================

INSERT INTO standards (id, code, full_name, publisher, current_version, publication_date,
    family, applicable_industries, description, certification_body_required,
    recertification_cycle_months, related_norm_codes, status, created_at, updated_at)
VALUES (
    '70000002-0000-0000-0000-000000000002',
    'iso-20000-1',
    'ISO/IEC 20000-1:2018 — Technologies de l''information — Gestion des services — Exigences du système de management des services',
    'ISO/IEC',
    '2018',
    DATE '2018-09-15',
    'HLS',
    'it-itsm,services,public',
    'Exigences d''un SMS (Service Management System) : conception, transition, fourniture et amélioration des services IT. Aligné ITIL 4 ; socle du pack IT/ITSM QualitOS.',
    TRUE,
    36,
    'iso-9001,iso-27001,itil-4',
    'PUBLISHED', now(), now()
);

INSERT INTO standard_sections (id, standard_id, code, title, description, order_index) VALUES
    ('72000002-0000-0000-0000-000000000001', '70000002-0000-0000-0000-000000000002', '6', 'Planification du SMS', 'Risques, opportunités et objectifs de gestion des services.', 1),
    ('72000002-0000-0000-0000-000000000002', '70000002-0000-0000-0000-000000000002', '8.2', 'Portefeuille de services', 'Planification, catalogue et gestion des actifs de service.', 2),
    ('72000002-0000-0000-0000-000000000003', '70000002-0000-0000-0000-000000000002', '8.5', 'Conception, build et transition', 'Gestion des changements, des mises en production et des nouveaux services.', 3),
    ('72000002-0000-0000-0000-000000000004', '70000002-0000-0000-0000-000000000002', '8.6', 'Résolution et exécution', 'Incidents, demandes de service, problèmes, niveaux de service.', 4);

INSERT INTO standard_clauses (id, section_id, code, title, description, order_index) VALUES
    ('7b000002-0000-0000-0000-000000000001', '72000002-0000-0000-0000-000000000001', '6.1', 'Risques et opportunités du SMS', 'Identifier les risques pesant sur la fourniture des services.', 1),
    ('7b000002-0000-0000-0000-000000000002', '72000002-0000-0000-0000-000000000002', '8.2.2', 'Catalogue de services', 'Tenir un catalogue décrivant les services et leurs dépendances.', 2),
    ('7b000002-0000-0000-0000-000000000003', '72000002-0000-0000-0000-000000000002', '8.2.3', 'Gestion des actifs', 'Maîtriser les actifs nécessaires à la fourniture des services.', 3),
    ('7b000002-0000-0000-0000-000000000004', '72000002-0000-0000-0000-000000000003', '8.5.1', 'Gestion des changements', 'Évaluer, approuver et tracer les changements de services.', 4),
    ('7b000002-0000-0000-0000-000000000005', '72000002-0000-0000-0000-000000000003', '8.5.2', 'Conception et transition de services', 'Planifier les nouveaux services et changements majeurs.', 5),
    ('7b000002-0000-0000-0000-000000000006', '72000002-0000-0000-0000-000000000004', '8.6.1', 'Gestion des incidents', 'Restaurer le service dans les délais convenus.', 6),
    ('7b000002-0000-0000-0000-000000000007', '72000002-0000-0000-0000-000000000004', '8.6.3', 'Gestion des problèmes', 'Analyser les causes racines et prévenir la récurrence des incidents.', 7),
    ('7b000002-0000-0000-0000-000000000008', '72000002-0000-0000-0000-000000000004', '8.3.3', 'Gestion des niveaux de service', 'Définir, suivre et revoir les SLA avec les clients.', 8);

INSERT INTO standard_requirements (clause_id, code, text, obligation, evidence_types, measurable_criteria, risk_if_missing, order_index) VALUES
    ('7b000002-0000-0000-0000-000000000001', '6.1.1', 'Déterminer les risques et opportunités relatifs au SMS et planifier leur traitement.', 'MUST', 'DOCUMENT,AUDIT', 'Registre de risques SMS tenu et revu', 'HIGH', 1),
    ('7b000002-0000-0000-0000-000000000002', '8.2.2.1', 'Créer et tenir à jour un catalogue de services incluant les dépendances entre services.', 'MUST', 'DOCUMENT', 'Catalogue publié et revu au moins annuellement', 'MEDIUM', 2),
    ('7b000002-0000-0000-0000-000000000003', '8.2.3.1', 'Les actifs utilisés pour fournir les services doivent être gérés et leur intégrité protégée.', 'MUST', 'DOCUMENT,KPI_RECORD', 'Inventaire d''actifs fiable (écart d''inventaire < 5 %)', 'HIGH', 3),
    ('7b000002-0000-0000-0000-000000000004', '8.5.1.1', 'Tout changement doit être enregistré, évalué (risque/impact), approuvé puis revu après déploiement.', 'MUST', 'DOCUMENT,AUDIT,KPI_RECORD', 'Change Success Rate suivi ; 0 changement non autorisé', 'CRITICAL', 4),
    ('7b000002-0000-0000-0000-000000000005', '8.5.2.1', 'Les nouveaux services et changements majeurs doivent être planifiés, conçus et testés avant transition.', 'MUST', 'DOCUMENT', 'Plans de transition + critères d''acceptation documentés', 'HIGH', 5),
    ('7b000002-0000-0000-0000-000000000006', '8.6.1.1', 'Les incidents doivent être enregistrés, priorisés et résolus en respectant les cibles de service.', 'MUST', 'KPI_RECORD,DOCUMENT', 'MTTR et taux de respect SLA suivis et dans les cibles', 'CRITICAL', 6),
    ('7b000002-0000-0000-0000-000000000007', '8.6.3.1', 'Les problèmes doivent être analysés jusqu''à la cause racine et les erreurs connues documentées.', 'MUST', 'ISHIKAWA,CAPA,DOCUMENT', 'Base d''erreurs connues alimentée ; taux de récurrence en baisse', 'HIGH', 7),
    ('7b000002-0000-0000-0000-000000000008', '8.3.3.1', 'Les SLA doivent être convenus avec le client, mesurés et revus à intervalles planifiés.', 'MUST', 'DOCUMENT,KPI_RECORD', 'Revues SLA documentées ; % uptime mesuré', 'HIGH', 8);

INSERT INTO standard_certification_paths (id, standard_id, estimated_duration_months_min, estimated_duration_months_max,
    estimated_cost_eur_min, estimated_cost_eur_max, difficulty_level,
    surveillance_audit_frequency, recertification_cycle_years, notes)
VALUES ('7e000002-0000-0000-0000-000000000002', '70000002-0000-0000-0000-000000000002',
    8, 15, 18000, 70000, 3, 'annual', 3,
    'Synergie forte avec ITIL 4 (pratiques) et le connecteur ITSM QualitOS (incidents/problèmes importés). Mutualisable avec ISO 9001/27001 (HLS).');

INSERT INTO standard_certification_stages (certification_path_id, stage_number, name, description, typical_duration_weeks, deliverables, actors, qualitos_modules, order_index) VALUES
    ('7e000002-0000-0000-0000-000000000002', 1, 'Cadrage SMS + périmètre de services', 'Définition du périmètre (services, sites, clients) et engagement direction.', '2-3', 'Périmètre SMS, lettre d''engagement', 'Direction, Service Manager', 'Document Control', 1),
    ('7e000002-0000-0000-0000-000000000002', 2, 'Gap analysis vs ISO 20000-1', 'Diagnostic des pratiques ITSM existantes contre les exigences.', '3-5', 'Rapport d''écarts', 'Service Manager, Consultant', 'Audit, Standards Hub', 2),
    ('7e000002-0000-0000-0000-000000000002', 3, 'Catalogue, SLA et processus cœur', 'Formalisation catalogue, SLA, incidents/problèmes/changements.', '8-12', 'Catalogue, SLA signés, procédures', 'Équipes IT', 'Document Control, ITSM', 3),
    ('7e000002-0000-0000-0000-000000000002', 4, 'Outillage KPI + tableaux de bord', 'Mise en place des indicateurs (MTTR, SLA, change success rate).', '4-6', 'Dashboards, définitions KPI', 'Service Manager', 'KPI, Dashboards', 4),
    ('7e000002-0000-0000-0000-000000000002', 5, 'Audit interne + revue de direction', 'Cycle complet d''audit interne du SMS et revue de direction.', '3-4', 'Rapport audit, CR revue', 'Auditeur interne, Direction', 'Audit, PDCA', 5),
    ('7e000002-0000-0000-0000-000000000002', 6, 'Audit de certification', 'Étape 1 (documentaire) puis étape 2 (terrain).', '2-4', 'Certificat ISO 20000-1', 'Organisme certificateur', 'Standards Hub', 6);

-- ============================================================================
-- ISO 50001:2018 — Management de l'énergie
-- ============================================================================

INSERT INTO standards (id, code, full_name, publisher, current_version, publication_date,
    family, applicable_industries, description, certification_body_required,
    recertification_cycle_months, related_norm_codes, status, created_at, updated_at)
VALUES (
    '70000003-0000-0000-0000-000000000003',
    'iso-50001',
    'ISO 50001:2018 — Systèmes de management de l''énergie — Exigences et recommandations pour la mise en œuvre',
    'ISO',
    '2018',
    DATE '2018-08-21',
    'HLS',
    'energie,manufacturing,datacenter,public,btp',
    'SMÉ : amélioration continue de la performance énergétique (usages énergétiques significatifs, IPÉ, situation de référence). Synergie IoT QualitOS (compteurs, sondes) pour la mesure en continu.',
    TRUE,
    36,
    'iso-9001,iso-14001',
    'PUBLISHED', now(), now()
);

INSERT INTO standard_sections (id, standard_id, code, title, description, order_index) VALUES
    ('73000003-0000-0000-0000-000000000001', '70000003-0000-0000-0000-000000000003', '4', 'Contexte de l''organisme', 'Enjeux, parties intéressées et périmètre du SMÉ.', 1),
    ('73000003-0000-0000-0000-000000000002', '70000003-0000-0000-0000-000000000003', '6', 'Planification énergétique', 'Revue énergétique, IPÉ, situation de référence, objectifs.', 2),
    ('73000003-0000-0000-0000-000000000003', '70000003-0000-0000-0000-000000000003', '8', 'Réalisation', 'Maîtrise opérationnelle, conception et achats efficients.', 3),
    ('73000003-0000-0000-0000-000000000004', '70000003-0000-0000-0000-000000000003', '9', 'Évaluation des performances', 'Surveillance, mesure, analyse de la performance énergétique.', 4);

INSERT INTO standard_clauses (id, section_id, code, title, description, order_index) VALUES
    ('7c000003-0000-0000-0000-000000000001', '73000003-0000-0000-0000-000000000001', '4.3', 'Périmètre du SMÉ', 'Déterminer les limites physiques et organisationnelles.', 1),
    ('7c000003-0000-0000-0000-000000000002', '73000003-0000-0000-0000-000000000002', '6.3', 'Revue énergétique', 'Analyser les usages et identifier les usages énergétiques significatifs (UES).', 2),
    ('7c000003-0000-0000-0000-000000000003', '73000003-0000-0000-0000-000000000002', '6.4', 'Indicateurs de performance énergétique (IPÉ)', 'Définir des IPÉ permettant de mesurer la performance.', 3),
    ('7c000003-0000-0000-0000-000000000004', '73000003-0000-0000-0000-000000000002', '6.5', 'Situation énergétique de référence (SÉR)', 'Établir la baseline de comparaison de la performance.', 4),
    ('7c000003-0000-0000-0000-000000000005', '73000003-0000-0000-0000-000000000003', '8.1', 'Maîtrise opérationnelle', 'Exploiter les UES selon des critères maîtrisés.', 5),
    ('7c000003-0000-0000-0000-000000000006', '73000003-0000-0000-0000-000000000003', '8.3', 'Achats', 'Intégrer la performance énergétique dans les achats d''équipements et d''énergie.', 6),
    ('7c000003-0000-0000-0000-000000000007', '73000003-0000-0000-0000-000000000004', '9.1', 'Surveillance et mesure', 'Surveiller les caractéristiques clés (UES, IPÉ vs SÉR).', 7),
    ('7c000003-0000-0000-0000-000000000008', '73000003-0000-0000-0000-000000000004', '9.1.2', 'Évaluation de la conformité réglementaire', 'Évaluer la conformité aux exigences légales énergétiques.', 8);

INSERT INTO standard_requirements (clause_id, code, text, obligation, evidence_types, measurable_criteria, risk_if_missing, order_index) VALUES
    ('7c000003-0000-0000-0000-000000000001', '4.3.1', 'Documenter le périmètre et les limites du SMÉ ; aucune source d''énergie du périmètre ne peut être exclue.', 'MUST', 'DOCUMENT', 'Périmètre documenté et approuvé', 'MEDIUM', 1),
    ('7c000003-0000-0000-0000-000000000002', '6.3.1', 'Réaliser et tenir à jour une revue énergétique identifiant les usages significatifs et les facteurs pertinents.', 'MUST', 'DOCUMENT,KPI_RECORD', 'Revue énergétique mise à jour à intervalles définis et lors de changements majeurs', 'CRITICAL', 2),
    ('7c000003-0000-0000-0000-000000000003', '6.4.1', 'Déterminer des IPÉ appropriés, les revoir et les comparer à la SÉR.', 'MUST', 'KPI_RECORD', 'IPÉ définis au catalogue KPI (formule + source + propriétaire)', 'HIGH', 3),
    ('7c000003-0000-0000-0000-000000000004', '6.5.1', 'Établir des situations énergétiques de référence et les normaliser par les facteurs pertinents.', 'MUST', 'DOCUMENT,KPI_RECORD', 'SÉR normalisée (météo, production) documentée', 'HIGH', 4),
    ('7c000003-0000-0000-0000-000000000005', '8.1.1', 'Exploiter et entretenir les installations liées aux UES selon des critères opérationnels définis.', 'MUST', 'DOCUMENT,FIVES_AUDIT', 'Critères opérationnels affichés ; dérives détectées (IoT)', 'HIGH', 5),
    ('7c000003-0000-0000-0000-000000000006', '8.3.1', 'Évaluer la performance énergétique sur la durée de vie prévue lors des achats d''équipements énergivores.', 'MUST', 'DOCUMENT', 'Critères énergie dans les dossiers d''achat', 'MEDIUM', 6),
    ('7c000003-0000-0000-0000-000000000007', '9.1.1', 'Surveiller, mesurer et analyser les caractéristiques clés à intervalles planifiés ; investiguer les écarts significatifs.', 'MUST', 'KPI_RECORD,CAPA', 'Plan de mesure (compteurs/IoT) ; écarts investigués avec actions', 'CRITICAL', 7),
    ('7c000003-0000-0000-0000-000000000008', '9.1.2.1', 'Évaluer à intervalles planifiés la conformité aux exigences légales et autres exigences énergétiques.', 'MUST', 'AUDIT,DOCUMENT', 'Évaluation de conformité tracée (audits énergie)', 'HIGH', 8);

INSERT INTO standard_certification_paths (id, standard_id, estimated_duration_months_min, estimated_duration_months_max,
    estimated_cost_eur_min, estimated_cost_eur_max, difficulty_level,
    surveillance_audit_frequency, recertification_cycle_years, notes)
VALUES ('7e000003-0000-0000-0000-000000000003', '70000003-0000-0000-0000-000000000003',
    8, 14, 15000, 60000, 3, 'annual', 3,
    'Le module IoT QualitOS (compteurs, sondes, MQTT/OPC-UA) alimente directement la surveillance §9.1 et les IPÉ. Éligible aides (CEE, audits réglementaires).');

INSERT INTO standard_certification_stages (certification_path_id, stage_number, name, description, typical_duration_weeks, deliverables, actors, qualitos_modules, order_index) VALUES
    ('7e000003-0000-0000-0000-000000000003', 1, 'Cadrage SMÉ + périmètre', 'Limites du SMÉ, équipe énergie, engagement direction.', '2-3', 'Périmètre, lettre d''engagement', 'Direction, Energy Manager', 'Document Control', 1),
    ('7e000003-0000-0000-0000-000000000003', 2, 'Revue énergétique initiale', 'Cartographie des usages, identification des UES, facteurs pertinents.', '4-8', 'Revue énergétique, liste UES', 'Energy Manager', 'IoT Hub, KPI', 2),
    ('7e000003-0000-0000-0000-000000000003', 3, 'IPÉ + situation de référence', 'Définition des IPÉ au catalogue KPI et de la SÉR normalisée.', '3-4', 'Catalogue IPÉ, SÉR', 'Energy Manager', 'KPI, Dashboards', 3),
    ('7e000003-0000-0000-0000-000000000003', 4, 'Plan de mesure + maîtrise opérationnelle', 'Instrumentation (compteurs, sondes IoT), critères opérationnels des UES.', '6-10', 'Plan de mesure, consignes', 'Maintenance, Production', 'IoT Hub, FiveS', 4),
    ('7e000003-0000-0000-0000-000000000003', 5, 'Audit interne + revue de direction', 'Audit interne du SMÉ, revue de direction avec décisions d''amélioration.', '3-4', 'Rapport d''audit, CR revue', 'Auditeur interne, Direction', 'Audit, PDCA', 5),
    ('7e000003-0000-0000-0000-000000000003', 6, 'Audit de certification', 'Étape 1 (documentaire) puis étape 2 (sur site).', '2-4', 'Certificat ISO 50001', 'Organisme certificateur', 'Standards Hub', 6);

-- ============================================================================
-- Qualiopi (RNQ) — Référentiel National Qualité des organismes de formation (France)
-- ============================================================================

INSERT INTO standards (id, code, full_name, publisher, current_version, publication_date,
    family, applicable_industries, description, certification_body_required,
    recertification_cycle_months, related_norm_codes, status, created_at, updated_at)
VALUES (
    '70000004-0000-0000-0000-000000000004',
    'qualiopi',
    'Qualiopi — Référentiel National Qualité (RNQ) des prestataires d''actions concourant au développement des compétences',
    'Ministère du Travail (France)',
    'V9',
    DATE '2023-09-01',
    'SECTORIEL',
    'education,formation,services',
    'Certification obligatoire en France pour l''accès aux fonds publics/mutualisés de la formation (loi Avenir professionnel). 7 critères, 32 indicateurs ; audit par organisme accrédité COFRAC.',
    TRUE,
    36,
    'iso-21001,iso-9001',
    'PUBLISHED', now(), now()
);

INSERT INTO standard_sections (id, standard_id, code, title, description, order_index) VALUES
    ('74000004-0000-0000-0000-000000000001', '70000004-0000-0000-0000-000000000004', 'C1', 'Critère 1 — Information du public', 'Conditions d''information sur les prestations, délais et résultats.', 1),
    ('74000004-0000-0000-0000-000000000002', '70000004-0000-0000-0000-000000000004', 'C2-C3', 'Critères 2-3 — Conception et adaptation des prestations', 'Identification des objectifs et adaptation aux publics bénéficiaires.', 2),
    ('74000004-0000-0000-0000-000000000003', '70000004-0000-0000-0000-000000000004', 'C4-C5', 'Critères 4-5 — Moyens et compétences', 'Adéquation des moyens pédagogiques et qualification des intervenants.', 3),
    ('74000004-0000-0000-0000-000000000004', '70000004-0000-0000-0000-000000000004', 'C6-C7', 'Critères 6-7 — Environnement professionnel et amélioration', 'Veille, appréciations et traitement des réclamations.', 4);

INSERT INTO standard_clauses (id, section_id, code, title, description, order_index) VALUES
    ('7d000004-0000-0000-0000-000000000001', '74000004-0000-0000-0000-000000000001', 'IND-1', 'Indicateur 1 — Information accessible et vérifiable', 'Diffusion d''une information exhaustive sur prérequis, objectifs, durée, tarifs, délais et accessibilité.', 1),
    ('7d000004-0000-0000-0000-000000000002', '74000004-0000-0000-0000-000000000001', 'IND-2', 'Indicateur 2 — Indicateurs de résultats', 'Diffusion d''indicateurs de résultats adaptés (taux de satisfaction, réussite, insertion).', 2),
    ('7d000004-0000-0000-0000-000000000003', '74000004-0000-0000-0000-000000000002', 'IND-4', 'Indicateur 4 — Analyse du besoin', 'Analyse du besoin du bénéficiaire en lien avec le financeur.', 3),
    ('7d000004-0000-0000-0000-000000000004', '74000004-0000-0000-0000-000000000002', 'IND-9', 'Indicateur 9 — Information des publics', 'Conditions de déroulement et modalités communiquées aux bénéficiaires.', 4),
    ('7d000004-0000-0000-0000-000000000005', '74000004-0000-0000-0000-000000000003', 'IND-17', 'Indicateur 17 — Moyens humains et techniques', 'Mise à disposition des moyens nécessaires aux prestations.', 5),
    ('7d000004-0000-0000-0000-000000000006', '74000004-0000-0000-0000-000000000003', 'IND-21', 'Indicateur 21 — Compétences des intervenants', 'Détermination et maintien des compétences des formateurs.', 6),
    ('7d000004-0000-0000-0000-000000000007', '74000004-0000-0000-0000-000000000004', 'IND-30', 'Indicateur 30 — Recueil des appréciations', 'Recueil des appréciations des parties prenantes (bénéficiaires, financeurs, équipes).', 7),
    ('7d000004-0000-0000-0000-000000000008', '74000004-0000-0000-0000-000000000004', 'IND-31-32', 'Indicateurs 31-32 — Réclamations et amélioration continue', 'Traitement des réclamations/aléas et mise en œuvre d''actions d''amélioration.', 8);

INSERT INTO standard_requirements (clause_id, code, text, obligation, evidence_types, measurable_criteria, risk_if_missing, order_index) VALUES
    ('7d000004-0000-0000-0000-000000000001', 'IND-1.1', 'Publier une information accessible, exhaustive et vérifiable : prérequis, objectifs, durée, modalités, tarifs, délais d''accès, accessibilité PSH.', 'MUST', 'DOCUMENT,EXTERNAL_FILE', 'Site/catalogue à jour ; mentions PSH présentes', 'CRITICAL', 1),
    ('7d000004-0000-0000-0000-000000000002', 'IND-2.1', 'Diffuser des indicateurs de résultats adaptés à la nature des prestations et aux publics.', 'MUST', 'KPI_RECORD,DOCUMENT', 'Taux de satisfaction/réussite publiés et actualisés', 'HIGH', 2),
    ('7d000004-0000-0000-0000-000000000003', 'IND-4.1', 'Analyser le besoin du bénéficiaire en lien avec l''entreprise et/ou le financeur concerné.', 'MUST', 'DOCUMENT', 'Fiche d''analyse du besoin par dossier', 'HIGH', 3),
    ('7d000004-0000-0000-0000-000000000004', 'IND-9.1', 'Informer les publics des conditions de déroulement de la prestation (objectifs, contenus, planning, modalités d''évaluation).', 'MUST', 'DOCUMENT', 'Convocations/livrets remis et tracés', 'MEDIUM', 4),
    ('7d000004-0000-0000-0000-000000000005', 'IND-17.1', 'Mettre à disposition les moyens humains et techniques adaptés (locaux, plateaux, outils distanciels).', 'MUST', 'DOCUMENT,AUDIT', 'Inventaire moyens par prestation ; conformité locaux', 'HIGH', 5),
    ('7d000004-0000-0000-0000-000000000006', 'IND-21.1', 'Déterminer, suivre et développer les compétences des intervenants internes et externes.', 'MUST', 'TRAINING_RECORD,DOCUMENT', 'CV + plan de professionnalisation à jour pour 100 % des formateurs', 'HIGH', 6),
    ('7d000004-0000-0000-0000-000000000007', 'IND-30.1', 'Recueillir les appréciations des parties prenantes, y compris les financeurs.', 'MUST', 'KPI_RECORD,DOCUMENT', 'Enquêtes systématiques ; taux de retour suivi', 'HIGH', 7),
    ('7d000004-0000-0000-0000-000000000008', 'IND-31.1', 'Traiter les réclamations et difficultés rencontrées, et mettre en œuvre des actions correctives.', 'MUST', 'CAPA,DOCUMENT', 'Registre des réclamations ; délai de traitement suivi', 'CRITICAL', 8),
    ('7d000004-0000-0000-0000-000000000008', 'IND-32.1', 'Mettre en œuvre une démarche d''amélioration continue à partir des appréciations et réclamations.', 'MUST', 'PDCA_CYCLE,CAPA', 'Plan d''amélioration alimenté et suivi (cycles PDCA)', 'HIGH', 9);

INSERT INTO standard_certification_paths (id, standard_id, estimated_duration_months_min, estimated_duration_months_max,
    estimated_cost_eur_min, estimated_cost_eur_max, difficulty_level,
    surveillance_audit_frequency, recertification_cycle_years, notes)
VALUES ('7e000004-0000-0000-0000-000000000004', '70000004-0000-0000-0000-000000000004',
    3, 9, 3000, 15000, 2,
    'mid-cycle (14-22 mois)', 3,
    'Obligatoire pour les fonds publics/mutualisés (CPF, OPCO) en France. Audit initial sur site ou à distance selon CA ; audit de surveillance entre le 14e et le 22e mois.');

INSERT INTO standard_certification_stages (certification_path_id, stage_number, name, description, typical_duration_weeks, deliverables, actors, qualitos_modules, order_index) VALUES
    ('7e000004-0000-0000-0000-000000000004', 1, 'Cadrage + catégories d''actions', 'Identifier les catégories (AF, BC, VAE, CFA) et le périmètre de certification.', '1-2', 'Périmètre, NDA active', 'Direction OF', 'Document Control', 1),
    ('7e000004-0000-0000-0000-000000000004', 2, 'Auto-diagnostic 32 indicateurs', 'Évaluation de l''existant contre les indicateurs applicables.', '2-4', 'Grille d''auto-diagnostic, plan d''action', 'Référent qualité', 'Audit, Standards Hub', 2),
    ('7e000004-0000-0000-0000-000000000004', 3, 'Mise en conformité documentaire', 'Procédures, traçabilité des dossiers bénéficiaires, indicateurs publiés.', '4-8', 'Procédures, site mis à jour, registres', 'Référent qualité, Équipe', 'Document Control, KPI', 3),
    ('7e000004-0000-0000-0000-000000000004', 4, 'Enquêtes + traitement des réclamations', 'Outillage du recueil d''appréciations et du registre de réclamations.', '2-4', 'Questionnaires, registre réclamations', 'Référent qualité', 'Complaints, CAPA', 4),
    ('7e000004-0000-0000-0000-000000000004', 5, 'Audit blanc', 'Simulation d''audit sur les indicateurs applicables (échantillon de dossiers).', '1-2', 'Rapport d''audit blanc, actions résiduelles', 'Auditeur interne ou IA', 'Standards Hub (audit blanc IA)', 5),
    ('7e000004-0000-0000-0000-000000000004', 6, 'Audit initial de certification', 'Audit par certificateur accrédité COFRAC ; levée des non-conformités le cas échéant.', '1-2', 'Certificat Qualiopi (3 ans)', 'Organisme certificateur', 'Standards Hub', 6);
