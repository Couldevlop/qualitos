-- Standards Hub — vague 8 (CLAUDE.md §8.2) : 5 référentiels IT / cloud / cybersécurité.
--   iso-27017   : ISO/IEC 27017:2015 — sécurité du cloud (extension de la certification ISO 27001)
--   iso-27018   : ISO/IEC 27018:2019 — protection des PII dans le cloud public (extension ISO 27001)
--   soc-2       : SOC 2 (AICPA Trust Services Criteria) — rapport d'attestation (PAS une certification)
--   secnumcloud : SecNumCloud 3.2 (ANSSI, France) — qualification (et non certification)
--   itil-4      : ITIL 4 — gestion des services IT (certifications individuelles ; l'organisation s'aligne)
-- Même format que V72 : catalogue platform-level, UUID déterministes, préfixe « c » (vague 8).
-- (standards c000000n, sections cn00000s, clauses c[a-d|9]…, paths ce…, stages via FK).
-- Note UUID : pour éviter toute collision avec les paths (ce), les clauses de la 5e
-- norme (ITIL 4) portent le préfixe c9 (ca-cd couvrent les normes 1 à 4).

-- ============================================================================
-- ISO/IEC 27017:2015 — Sécurité du cloud (extension ISO 27001)
-- ============================================================================

INSERT INTO standards (id, code, full_name, publisher, current_version, publication_date,
    family, applicable_industries, description, certification_body_required,
    recertification_cycle_months, related_norm_codes, status, created_at, updated_at)
VALUES (
    'c0000001-0000-0000-0000-000000000001',
    'iso-27017',
    'ISO/IEC 27017:2015 — Code de bonnes pratiques pour les contrôles de sécurité de l''information fondés sur l''ISO/IEC 27002 pour les services du cloud',
    'ISO/IEC',
    '2015',
    DATE '2015-12-15',
    'SECTORIEL',
    'it,cloud',
    'Code de bonnes pratiques précisant les contrôles de sécurité applicables aux services en nuage, tant pour le fournisseur de services cloud (CSP) que pour le client. NUANCE : ISO/IEC 27017 n''est pas une norme certifiable de façon autonome — la conformité s''obtient comme EXTENSION d''une certification ISO/IEC 27001, l''organisme certificateur attestant la prise en compte des contrôles 27017 dans le périmètre du SMSI. Le référentiel ajoute des recommandations spécifiques au cloud (CLD.6.3 responsabilités partagées CSP/client, CLD.8.1 restitution et suppression des actifs en fin de contrat, CLD.9.5 isolation des environnements virtuels, CLD.12.1 supervision de l''exploitation, CLD.12.4 journalisation, CLD.13.1 alignement des réseaux virtuels et physiques) et le durcissement des machines virtuelles.',
    TRUE,
    NULL,
    'iso-27001,iso-27018',
    'PUBLISHED', now(), now()
);

INSERT INTO standard_sections (id, standard_id, code, title, description, order_index) VALUES
    ('c1000001-0000-0000-0000-000000000001', 'c0000001-0000-0000-0000-000000000001', 'CLD.6', 'Organisation de la sécurité', 'Rôles et responsabilités partagées entre fournisseur de services cloud et client.', 1),
    ('c1000001-0000-0000-0000-000000000002', 'c0000001-0000-0000-0000-000000000001', 'CLD.8', 'Gestion des actifs', 'Restitution et suppression des actifs du client à la fin du contrat.', 2),
    ('c1000001-0000-0000-0000-000000000003', 'c0000001-0000-0000-0000-000000000001', 'CLD.9', 'Contrôle d''accès & isolation', 'Isolation des environnements virtuels et durcissement des machines virtuelles.', 3),
    ('c1000001-0000-0000-0000-000000000004', 'c0000001-0000-0000-0000-000000000001', 'CLD.12', 'Sécurité de l''exploitation', 'Supervision des services cloud, journalisation et synchronisation horaire.', 4),
    ('c1000001-0000-0000-0000-000000000005', 'c0000001-0000-0000-0000-000000000001', 'CLD.13', 'Sécurité des communications', 'Alignement de la sécurité des réseaux virtuels et physiques.', 5);

INSERT INTO standard_clauses (id, section_id, code, title, description, order_index) VALUES
    ('ca000001-0000-0000-0000-000000000001', 'c1000001-0000-0000-0000-000000000001', 'CLD.6.3', 'Responsabilités partagées', 'Répartition documentée des responsabilités de sécurité entre CSP et client.', 1),
    ('ca000001-0000-0000-0000-000000000002', 'c1000001-0000-0000-0000-000000000002', 'CLD.8.1', 'Restitution & suppression des actifs', 'Restitution et suppression sécurisée des actifs du client en fin de contrat.', 2),
    ('ca000001-0000-0000-0000-000000000003', 'c1000001-0000-0000-0000-000000000003', 'CLD.9.5', 'Isolation des environnements virtuels', 'Cloisonnement logique des environnements clients en environnement mutualisé.', 3),
    ('ca000001-0000-0000-0000-000000000004', 'c1000001-0000-0000-0000-000000000003', 'CLD.9.6', 'Durcissement des machines virtuelles', 'Configuration durcie des VM et images selon des standards de sécurité.', 4),
    ('ca000001-0000-0000-0000-000000000005', 'c1000001-0000-0000-0000-000000000004', 'CLD.12.1', 'Supervision des services cloud', 'Surveillance de l''exploitation et fourniture de capacités de supervision au client.', 5),
    ('ca000001-0000-0000-0000-000000000006', 'c1000001-0000-0000-0000-000000000004', 'CLD.12.4', 'Journalisation & traçabilité', 'Journalisation des activités d''administration et d''accès aux services cloud.', 6),
    ('ca000001-0000-0000-0000-000000000007', 'c1000001-0000-0000-0000-000000000005', 'CLD.13.1', 'Sécurité des réseaux virtuels', 'Cohérence des contrôles de sécurité entre réseaux virtuels et physiques.', 7);

INSERT INTO standard_requirements (clause_id, code, text, obligation, evidence_types, measurable_criteria, risk_if_missing, order_index) VALUES
    ('ca000001-0000-0000-0000-000000000001', 'CLD.6.3.1', 'Le fournisseur et le client de service cloud doivent définir et documenter la répartition des responsabilités de sécurité de l''information (modèle de responsabilité partagée).', 'MUST', 'DOCUMENT,AUDIT', 'Matrice de responsabilité partagée CSP/client documentée et revue au moins annuellement', 'CRITICAL', 1),
    ('ca000001-0000-0000-0000-000000000002', 'CLD.8.1.5', 'Le fournisseur doit garantir la restitution et la suppression sécurisée des actifs et données du client à la fin de l''accord de service cloud.', 'MUST', 'DOCUMENT,AUDIT', 'Procédure de restitution/suppression testée ; délais et preuves de suppression contractualisés', 'HIGH', 2),
    ('ca000001-0000-0000-0000-000000000003', 'CLD.9.5.1', 'Le fournisseur doit assurer l''isolation logique des environnements virtuels des différents clients dans une infrastructure mutualisée.', 'MUST', 'DOCUMENT,AUDIT', 'Cloisonnement multi-locataire vérifié par tests ; 0 fuite inter-tenant constatée', 'CRITICAL', 3),
    ('ca000001-0000-0000-0000-000000000004', 'CLD.9.5.2', 'Le fournisseur doit durcir les machines virtuelles et les images conformément à des standards de configuration de sécurité.', 'MUST', 'DOCUMENT,AUDIT', 'Standards de durcissement (ex. CIS Benchmarks) appliqués à 100 % des images de VM', 'HIGH', 4),
    ('ca000001-0000-0000-0000-000000000005', 'CLD.12.1.5', 'Le fournisseur doit superviser l''exploitation des services cloud et fournir au client des capacités de supervision de ses propres ressources.', 'MUST', 'DOCUMENT,KPI_RECORD', 'Supervision opérationnelle en place ; tableaux de bord client disponibles', 'MEDIUM', 5),
    ('ca000001-0000-0000-0000-000000000006', 'CLD.12.4.5', 'Le fournisseur doit journaliser les activités d''administration et d''accès aux services cloud et permettre au client d''accéder à ses journaux.', 'MUST', 'DOCUMENT,KPI_RECORD', 'Journaux d''administration conservés selon la durée définie ; accès client aux logs assuré', 'HIGH', 6),
    ('ca000001-0000-0000-0000-000000000007', 'CLD.13.1.4', 'Le fournisseur doit assurer la cohérence des contrôles de sécurité entre les réseaux virtuels et les réseaux physiques sous-jacents.', 'MUST', 'DOCUMENT,AUDIT', 'Politique de sécurité réseau alignée virtuel/physique ; segmentation revue périodiquement', 'HIGH', 7),
    ('ca000001-0000-0000-0000-000000000002', 'CLD.8.1.6', 'Le fournisseur doit identifier et documenter la localisation de stockage des données du client afin d''en permettre la maîtrise.', 'SHOULD', 'DOCUMENT', 'Localisation des données documentée et communicable au client sur demande', 'MEDIUM', 8);

INSERT INTO standard_certification_paths (id, standard_id, estimated_duration_months_min, estimated_duration_months_max,
    estimated_cost_eur_min, estimated_cost_eur_max, difficulty_level,
    surveillance_audit_frequency, recertification_cycle_years, notes)
VALUES ('ce000001-0000-0000-0000-000000000001', 'c0000001-0000-0000-0000-000000000001',
    3, 9, 8000, 40000, 3, 'annual', 3,
    'NUANCE : ISO/IEC 27017 n''est pas certifiable seule — l''attestation s''obtient comme EXTENSION d''une certification ISO/IEC 27001 (le périmètre du SMSI intègre les contrôles cloud CLD.x). Cycle aligné sur celui de l''ISO 27001 (3 ans, surveillance annuelle). Synergie avec les modules Audit, Document Control et Risk de QualitOS.');

INSERT INTO standard_certification_stages (certification_path_id, stage_number, name, description, typical_duration_weeks, deliverables, actors, qualitos_modules, order_index) VALUES
    ('ce000001-0000-0000-0000-000000000001', 1, 'Cadrage cloud & socle ISO 27001', 'Définition du périmètre cloud et vérification de la couverture du SMSI ISO 27001 support.', '2-4', 'Périmètre cloud, état du SMSI ISO 27001', 'RSSI, Direction', 'Document Control', 1),
    ('ce000001-0000-0000-0000-000000000001', 2, 'Modèle de responsabilité partagée', 'Définition et documentation de la répartition des responsabilités CSP/client (CLD.6.3).', '2-4', 'Matrice de responsabilité partagée', 'RSSI, Architectes cloud', 'Document Control, Risk', 2),
    ('ce000001-0000-0000-0000-000000000001', 3, 'Isolation, durcissement & exploitation', 'Isolation des environnements virtuels, durcissement des VM, supervision et journalisation cloud.', '4-8', 'Standards de durcissement, plan de supervision, politique de journalisation', 'RSSI, IT', 'Document Control, Risk', 3),
    ('ce000001-0000-0000-0000-000000000001', 4, 'Restitution & fin de contrat', 'Procédures de restitution et de suppression sécurisée des actifs client (CLD.8.1).', '2-4', 'Procédure de réversibilité/suppression testée', 'RSSI, Juridique', 'Document Control', 4),
    ('ce000001-0000-0000-0000-000000000001', 5, 'Audit interne (contrôles cloud)', 'Audit interne des contrôles CLD.x intégrés au SMSI et revue de direction.', '2-4', 'Rapport d''audit interne, compte rendu de revue', 'Auditeur interne, Direction', 'Audit, PDCA', 5),
    ('ce000001-0000-0000-0000-000000000001', 6, 'Audit de certification (extension 27001)', 'Audit par l''organisme certificateur attestant la prise en compte d''ISO 27017 dans le périmètre 27001.', '2-4', 'Attestation ISO/IEC 27017 (extension du certificat 27001)', 'Organisme certificateur', 'Standards Hub, CAPA', 6);

-- ============================================================================
-- ISO/IEC 27018:2019 — Protection des PII dans le cloud public (extension ISO 27001)
-- ============================================================================

INSERT INTO standards (id, code, full_name, publisher, current_version, publication_date,
    family, applicable_industries, description, certification_body_required,
    recertification_cycle_months, related_norm_codes, status, created_at, updated_at)
VALUES (
    'c0000002-0000-0000-0000-000000000002',
    'iso-27018',
    'ISO/IEC 27018:2019 — Code de bonnes pratiques pour la protection des informations personnelles identifiables (PII) dans l''informatique en nuage public agissant comme processeur de PII',
    'ISO/IEC',
    '2019',
    DATE '2019-01-15',
    'SECTORIEL',
    'it,cloud',
    'Code de bonnes pratiques applicable aux fournisseurs de cloud public agissant en qualité de sous-traitant (processeur) de PII. NUANCE : comme ISO/IEC 27017, ce référentiel n''est pas certifiable de façon autonome — l''attestation s''obtient comme EXTENSION d''une certification ISO/IEC 27001. Il complète les principes de protection de la vie privée d''ISO/IEC 29100 : traitement des PII limité aux instructions et finalités du client, transparence sur les sous-traitants ultérieurs et la localisation des données, retour/transfert/suppression des PII, notification au client en cas de violation de données, restrictions sur l''usage des PII à des fins de marketing/publicité, droits des personnes concernées et chiffrement des PII transmises sur les réseaux publics.',
    TRUE,
    NULL,
    'iso-27001,iso-27701,rgpd',
    'PUBLISHED', now(), now()
);

INSERT INTO standard_sections (id, standard_id, code, title, description, order_index) VALUES
    ('c2000002-0000-0000-0000-000000000001', 'c0000002-0000-0000-0000-000000000002', 'A.1', 'Consentement & finalités', 'Traitement des PII limité aux instructions et finalités du client.', 1),
    ('c2000002-0000-0000-0000-000000000002', 'c0000002-0000-0000-0000-000000000002', 'A.2', 'Transparence', 'Information du client sur les sous-traitants ultérieurs et la localisation des PII.', 2),
    ('c2000002-0000-0000-0000-000000000003', 'c0000002-0000-0000-0000-000000000002', 'A.3', 'Cycle de vie des PII', 'Retour, transfert et suppression des PII en fin de service.', 3),
    ('c2000002-0000-0000-0000-000000000004', 'c0000002-0000-0000-0000-000000000002', 'A.4', 'Sécurité & violations', 'Chiffrement des PII et notification des violations au client.', 4);

INSERT INTO standard_clauses (id, section_id, code, title, description, order_index) VALUES
    ('cb000002-0000-0000-0000-000000000001', 'c2000002-0000-0000-0000-000000000001', 'A.1.1', 'Traitement sur instructions', 'PII traitées uniquement sur instructions documentées du client (responsable).', 1),
    ('cb000002-0000-0000-0000-000000000002', 'c2000002-0000-0000-0000-000000000001', 'A.1.2', 'Restriction marketing', 'Interdiction d''utiliser les PII à des fins de marketing/publicité sans consentement.', 2),
    ('cb000002-0000-0000-0000-000000000003', 'c2000002-0000-0000-0000-000000000002', 'A.2.1', 'Sous-traitants ultérieurs', 'Divulgation et encadrement du recours à des sous-traitants ultérieurs.', 3),
    ('cb000002-0000-0000-0000-000000000004', 'c2000002-0000-0000-0000-000000000002', 'A.2.2', 'Localisation des données', 'Information du client sur les pays de localisation et de traitement des PII.', 4),
    ('cb000002-0000-0000-0000-000000000005', 'c2000002-0000-0000-0000-000000000003', 'A.3.1', 'Retour & suppression des PII', 'Politique de retour, transfert et suppression sécurisée des PII.', 5),
    ('cb000002-0000-0000-0000-000000000006', 'c2000002-0000-0000-0000-000000000003', 'A.3.2', 'Droits des personnes concernées', 'Assistance au client pour répondre aux demandes des personnes concernées.', 6),
    ('cb000002-0000-0000-0000-000000000007', 'c2000002-0000-0000-0000-000000000004', 'A.4.1', 'Chiffrement des PII', 'Chiffrement des PII transmises sur les réseaux de transmission publics.', 7),
    ('cb000002-0000-0000-0000-000000000008', 'c2000002-0000-0000-0000-000000000004', 'A.4.2', 'Notification de violation', 'Notification au client des violations de données affectant ses PII.', 8);

INSERT INTO standard_requirements (clause_id, code, text, obligation, evidence_types, measurable_criteria, risk_if_missing, order_index) VALUES
    ('cb000002-0000-0000-0000-000000000001', 'A.1.1', 'Le fournisseur ne doit traiter les PII que conformément aux instructions documentées du client (responsable du traitement).', 'MUST', 'DOCUMENT,AUDIT', 'Instructions de traitement formalisées au contrat ; aucun traitement hors instructions', 'CRITICAL', 1),
    ('cb000002-0000-0000-0000-000000000002', 'A.1.2', 'Le fournisseur ne doit pas utiliser les PII traitées à des fins de marketing ou de publicité sans le consentement préalable de la personne concernée.', 'MUST', 'DOCUMENT', 'Politique d''interdiction de réutilisation marketing en place ; preuve d''absence d''usage', 'HIGH', 2),
    ('cb000002-0000-0000-0000-000000000003', 'A.2.1', 'Le fournisseur doit divulguer au client le recours à des sous-traitants ultérieurs traitant des PII avant leur autorisation.', 'MUST', 'DOCUMENT,AUDIT', 'Registre des sous-traitants ultérieurs tenu à jour ; client informé avant tout changement', 'HIGH', 3),
    ('cb000002-0000-0000-0000-000000000004', 'A.2.2', 'Le fournisseur doit informer le client des pays dans lesquels les PII peuvent être stockées ou traitées.', 'MUST', 'DOCUMENT', 'Liste des pays de localisation/traitement communiquée et tenue à jour', 'MEDIUM', 4),
    ('cb000002-0000-0000-0000-000000000005', 'A.3.1', 'Le fournisseur doit disposer d''une politique de retour, de transfert et de suppression sécurisée des PII en fin de service.', 'MUST', 'DOCUMENT,AUDIT', 'Procédure de retour/suppression des PII testée ; preuves de suppression fournies au client', 'HIGH', 5),
    ('cb000002-0000-0000-0000-000000000006', 'A.3.2', 'Le fournisseur doit assister le client dans la réponse aux demandes d''exercice des droits des personnes concernées.', 'MUST', 'DOCUMENT', 'Mécanisme d''assistance documenté ; délais de réponse aux demandes respectés', 'MEDIUM', 6),
    ('cb000002-0000-0000-0000-000000000007', 'A.4.1', 'Le fournisseur doit chiffrer les PII transmises sur les réseaux de transmission publics.', 'MUST', 'DOCUMENT,AUDIT', 'Chiffrement fort (TLS 1.2+) imposé sur 100 % des flux transportant des PII', 'CRITICAL', 7),
    ('cb000002-0000-0000-0000-000000000008', 'A.4.2', 'Le fournisseur doit notifier au client toute violation de données impliquant ses PII dans les délais convenus.', 'MUST', 'DOCUMENT,CAPA', 'Procédure de notification de violation en place ; notifications tracées dans les délais contractuels', 'CRITICAL', 8);

INSERT INTO standard_certification_paths (id, standard_id, estimated_duration_months_min, estimated_duration_months_max,
    estimated_cost_eur_min, estimated_cost_eur_max, difficulty_level,
    surveillance_audit_frequency, recertification_cycle_years, notes)
VALUES ('ce000002-0000-0000-0000-000000000002', 'c0000002-0000-0000-0000-000000000002',
    3, 9, 8000, 45000, 3, 'annual', 3,
    'NUANCE : comme ISO/IEC 27017, ISO/IEC 27018 s''atteste comme EXTENSION d''une certification ISO/IEC 27001 (intégration des contrôles PII Annexe A au périmètre du SMSI), souvent conjointement avec 27017. Cycle aligné sur l''ISO 27001 (3 ans, surveillance annuelle). Synergie avec les modules breach (notification de violation), retention (cycle de vie/suppression des PII) et Document Control de QualitOS.');

INSERT INTO standard_certification_stages (certification_path_id, stage_number, name, description, typical_duration_weeks, deliverables, actors, qualitos_modules, order_index) VALUES
    ('ce000002-0000-0000-0000-000000000002', 1, 'Cadrage PII & socle ISO 27001', 'Identification des traitements de PII en tant que processeur et état du SMSI ISO 27001 support.', '2-4', 'Cartographie des PII traitées, état du SMSI', 'DPO, RSSI', 'Document Control', 1),
    ('ce000002-0000-0000-0000-000000000002', 2, 'Consentement, finalités & contrat', 'Encadrement contractuel du traitement sur instructions et restrictions marketing.', '2-4', 'Clauses de sous-traitance, instructions de traitement', 'DPO, Juridique', 'Document Control', 2),
    ('ce000002-0000-0000-0000-000000000002', 3, 'Transparence & sous-traitants', 'Registre des sous-traitants ultérieurs et information sur la localisation des PII.', '2-4', 'Registre des sous-traitants, liste des localisations', 'DPO, RSSI', 'Document Control', 3),
    ('ce000002-0000-0000-0000-000000000002', 4, 'Cycle de vie & sécurité des PII', 'Politique de retour/suppression, chiffrement et droits des personnes concernées.', '4-8', 'Procédure de suppression, politique de chiffrement', 'RSSI, DPO', 'retention, Document Control', 4),
    ('ce000002-0000-0000-0000-000000000002', 5, 'Violations & audit interne', 'Procédure de notification de violation et audit interne des contrôles PII.', '2-4', 'Procédure de notification, rapport d''audit interne', 'DPO, Auditeur interne', 'breach, Audit', 5),
    ('ce000002-0000-0000-0000-000000000002', 6, 'Audit de certification (extension 27001)', 'Audit attestant la prise en compte d''ISO 27018 dans le périmètre de la certification 27001.', '2-4', 'Attestation ISO/IEC 27018 (extension du certificat 27001)', 'Organisme certificateur', 'Standards Hub, CAPA', 6);

-- ============================================================================
-- SOC 2 (AICPA Trust Services Criteria) — rapport d'attestation
-- ============================================================================

INSERT INTO standards (id, code, full_name, publisher, current_version, publication_date,
    family, applicable_industries, description, certification_body_required,
    recertification_cycle_months, related_norm_codes, status, created_at, updated_at)
VALUES (
    'c0000003-0000-0000-0000-000000000003',
    'soc-2',
    'SOC 2 — System and Organization Controls 2 (AICPA Trust Services Criteria)',
    'AICPA',
    '2017 (rév. 2022)',
    DATE '2022-01-01',
    'REGULATORY',
    'it,cloud,all',
    'Référentiel d''audit des contrôles d''une organisation de services au regard des Trust Services Criteria (TSC) de l''AICPA. NUANCE IMPORTANTE : SOC 2 n''est PAS une certification mais un RAPPORT D''ATTESTATION émis par un cabinet d''expertise comptable (CPA) indépendant. Il existe deux types : le Type I atteste la conception des contrôles à une date donnée, le Type II atteste leur efficacité opérationnelle sur une période d''observation (généralement 3 à 12 mois). Le référentiel repose sur cinq catégories de critères : Security (Common Criteria CC1 à CC9, obligatoire), Availability, Confidentiality, Processing Integrity et Privacy (optionnelles, selon le périmètre). Exigences typiques : revues d''accès périodiques (trimestrielles), gestion des changements, supervision des sous-traitants, tests de continuité d''activité, gestion des incidents et surveillance.',
    TRUE,
    NULL,
    'iso-27001',
    'PUBLISHED', now(), now()
);

INSERT INTO standard_sections (id, standard_id, code, title, description, order_index) VALUES
    ('c3000003-0000-0000-0000-000000000001', 'c0000003-0000-0000-0000-000000000003', 'CC', 'Security (Common Criteria)', 'Critères communs CC1 à CC9 — socle de sécurité obligatoire.', 1),
    ('c3000003-0000-0000-0000-000000000002', 'c0000003-0000-0000-0000-000000000003', 'A', 'Availability', 'Disponibilité du système selon les engagements et obligations.', 2),
    ('c3000003-0000-0000-0000-000000000003', 'c0000003-0000-0000-0000-000000000003', 'C', 'Confidentiality', 'Protection des informations désignées comme confidentielles.', 3),
    ('c3000003-0000-0000-0000-000000000004', 'c0000003-0000-0000-0000-000000000003', 'PI', 'Processing Integrity', 'Exhaustivité, exactitude et autorisation du traitement.', 4),
    ('c3000003-0000-0000-0000-000000000005', 'c0000003-0000-0000-0000-000000000003', 'P', 'Privacy', 'Collecte, utilisation et conservation des informations personnelles.', 5);

INSERT INTO standard_clauses (id, section_id, code, title, description, order_index) VALUES
    ('cc000003-0000-0000-0000-000000000001', 'c3000003-0000-0000-0000-000000000001', 'CC6.1', 'Contrôles d''accès logique', 'Restriction de l''accès logique aux systèmes et données.', 1),
    ('cc000003-0000-0000-0000-000000000002', 'c3000003-0000-0000-0000-000000000001', 'CC6.2', 'Revues d''accès', 'Provisionnement, revue périodique et retrait des accès.', 2),
    ('cc000003-0000-0000-0000-000000000003', 'c3000003-0000-0000-0000-000000000001', 'CC7.2', 'Surveillance & détection', 'Surveillance des systèmes et détection des anomalies de sécurité.', 3),
    ('cc000003-0000-0000-0000-000000000004', 'c3000003-0000-0000-0000-000000000001', 'CC7.4', 'Gestion des incidents', 'Réponse aux incidents de sécurité identifiés.', 4),
    ('cc000003-0000-0000-0000-000000000005', 'c3000003-0000-0000-0000-000000000001', 'CC8.1', 'Gestion des changements', 'Autorisation, conception, test et déploiement des changements.', 5),
    ('cc000003-0000-0000-0000-000000000006', 'c3000003-0000-0000-0000-000000000001', 'CC9.2', 'Gestion des sous-traitants', 'Gestion des risques liés aux fournisseurs et partenaires.', 6),
    ('cc000003-0000-0000-0000-000000000007', 'c3000003-0000-0000-0000-000000000002', 'A1.2', 'Continuité & sauvegarde', 'Sauvegardes, reprise et tests de continuité d''activité.', 7),
    ('cc000003-0000-0000-0000-000000000008', 'c3000003-0000-0000-0000-000000000003', 'C1.1', 'Protection de la confidentialité', 'Identification et protection des informations confidentielles.', 8),
    ('cc000003-0000-0000-0000-000000000009', 'c3000003-0000-0000-0000-000000000004', 'PI1.2', 'Intégrité du traitement', 'Exhaustivité et exactitude des entrées et traitements.', 9);

INSERT INTO standard_requirements (clause_id, code, text, obligation, evidence_types, measurable_criteria, risk_if_missing, order_index) VALUES
    ('cc000003-0000-0000-0000-000000000001', 'CC6.1', 'L''entité doit mettre en œuvre des contrôles d''accès logique restreignant l''accès aux systèmes et données aux seules personnes autorisées.', 'MUST', 'DOCUMENT,AUDIT', 'Contrôles d''accès (RBAC, MFA) déployés sur 100 % des systèmes du périmètre', 'CRITICAL', 1),
    ('cc000003-0000-0000-0000-000000000002', 'CC6.2', 'L''entité doit réaliser des revues périodiques des droits d''accès et retirer les accès non nécessaires.', 'MUST', 'DOCUMENT,KPI_RECORD', 'Revues d''accès réalisées au moins trimestriellement ; écarts corrigés et tracés', 'HIGH', 2),
    ('cc000003-0000-0000-0000-000000000003', 'CC7.2', 'L''entité doit surveiller ses systèmes pour détecter les anomalies et événements de sécurité.', 'MUST', 'DOCUMENT,KPI_RECORD', 'Surveillance/SIEM en place ; alertes de sécurité traitées et tracées', 'HIGH', 3),
    ('cc000003-0000-0000-0000-000000000004', 'CC7.4', 'L''entité doit disposer d''un processus de réponse aux incidents de sécurité identifiés.', 'MUST', 'DOCUMENT,CAPA', 'Procédure de réponse aux incidents en place ; incidents traités dans les délais définis', 'HIGH', 4),
    ('cc000003-0000-0000-0000-000000000005', 'CC8.1', 'L''entité doit autoriser, concevoir, tester et approuver les changements avant leur mise en production.', 'MUST', 'DOCUMENT,AUDIT', 'Processus de gestion des changements suivi ; 100 % des changements autorisés et tracés', 'HIGH', 5),
    ('cc000003-0000-0000-0000-000000000006', 'CC9.2', 'L''entité doit évaluer et superviser les risques liés à ses sous-traitants et fournisseurs.', 'MUST', 'DOCUMENT,AUDIT', 'Sous-traitants critiques évalués ; rapports SOC 2 des fournisseurs revus annuellement', 'MEDIUM', 6),
    ('cc000003-0000-0000-0000-000000000007', 'A1.2', 'L''entité doit mettre en œuvre des sauvegardes et des dispositifs de reprise, et en tester la continuité d''activité.', 'MUST', 'DOCUMENT,KPI_RECORD', 'Sauvegardes exécutées et restaurations testées ≥ 1/an ; RTO/RPO respectés', 'HIGH', 7),
    ('cc000003-0000-0000-0000-000000000008', 'C1.1', 'L''entité doit identifier et protéger les informations désignées comme confidentielles tout au long de leur cycle de vie.', 'MUST', 'DOCUMENT,AUDIT', 'Classification des informations en place ; chiffrement des données confidentielles', 'HIGH', 8),
    ('cc000003-0000-0000-0000-000000000009', 'PI1.2', 'L''entité doit assurer l''exhaustivité, l''exactitude et l''autorisation des entrées et des traitements.', 'SHOULD', 'DOCUMENT,KPI_RECORD', 'Contrôles d''intégrité (validation, rapprochement) en place ; taux d''erreur de traitement suivi', 'MEDIUM', 9);

INSERT INTO standard_certification_paths (id, standard_id, estimated_duration_months_min, estimated_duration_months_max,
    estimated_cost_eur_min, estimated_cost_eur_max, difficulty_level,
    surveillance_audit_frequency, recertification_cycle_years, notes)
VALUES ('ce000003-0000-0000-0000-000000000003', 'c0000003-0000-0000-0000-000000000003',
    6, 14, 20000, 100000, 4, 'annual', 1,
    'NUANCE : SOC 2 est un RAPPORT D''ATTESTATION (et non une certification), émis par un cabinet CPA indépendant. Type I = conception à une date donnée ; Type II = efficacité opérationnelle sur une période d''observation de 3 à 12 mois. Renouvellement annuel (le rapport couvre une période). Synergie avec les modules Audit, Document Control, Risk et Change de QualitOS.');

INSERT INTO standard_certification_stages (certification_path_id, stage_number, name, description, typical_duration_weeks, deliverables, actors, qualitos_modules, order_index) VALUES
    ('ce000003-0000-0000-0000-000000000003', 1, 'Choix du périmètre & des TSC', 'Sélection des catégories Trust Services (Security obligatoire + options) et du type de rapport (I ou II).', '2-4', 'Périmètre, liste des TSC retenus, type de rapport', 'RSSI, Direction', 'Document Control', 1),
    ('ce000003-0000-0000-0000-000000000003', 2, 'Readiness assessment (gap analysis)', 'Évaluation de préparation des contrôles vs les Common Criteria et plan de remédiation.', '4-8', 'Rapport de readiness, plan de remédiation', 'RSSI, Auditeur', 'Audit, Risk', 2),
    ('ce000003-0000-0000-0000-000000000003', 3, 'Mise en œuvre des contrôles', 'Déploiement des contrôles d''accès, de gestion des changements et de surveillance.', '6-12', 'Contrôles déployés, politiques et procédures', 'RSSI, IT', 'Document Control, Change', 3),
    ('ce000003-0000-0000-0000-000000000003', 4, 'Période d''observation (Type II)', 'Exploitation des contrôles et collecte des preuves sur la période d''observation (3-12 mois).', '12-52', 'Preuves d''efficacité opérationnelle, revues d''accès trimestrielles', 'RSSI, SOC', 'Audit, Risk', 4),
    ('ce000003-0000-0000-0000-000000000003', 5, 'Audit CPA & émission du rapport', 'Tests des contrôles par le cabinet CPA et émission du rapport SOC 2 (Type I ou II).', '4-8', 'Rapport SOC 2 (Type I ou II)', 'Cabinet CPA, RSSI', 'Standards Hub, CAPA', 5),
    ('ce000003-0000-0000-0000-000000000003', 6, 'Suivi & renouvellement annuel', 'Suivi continu des contrôles et préparation du rapport de la période suivante.', '2-4', 'Plan de suivi, calendrier de renouvellement', 'RSSI, Direction', 'PDCA, Audit', 6);

-- ============================================================================
-- SecNumCloud 3.2 (ANSSI, France) — qualification (et non certification)
-- ============================================================================

INSERT INTO standards (id, code, full_name, publisher, current_version, publication_date,
    family, applicable_industries, description, certification_body_required,
    recertification_cycle_months, related_norm_codes, status, created_at, updated_at)
VALUES (
    'c0000004-0000-0000-0000-000000000004',
    'secnumcloud',
    'SecNumCloud 3.2 — Référentiel d''exigences ANSSI pour les prestataires de services d''informatique en nuage',
    'ANSSI',
    '3.2',
    DATE '2022-03-08',
    'REGULATORY',
    'it,cloud',
    'Référentiel français de l''ANSSI permettant la QUALIFICATION des prestataires de services cloud (SecNumCloud). NUANCE : il s''agit d''une QUALIFICATION délivrée par l''ANSSI (et non d''une certification au sens accréditation) ; le cycle de qualification est de 3 ans. Le référentiel s''appuie sur un socle ISO/IEC 27001 RENFORCÉ et ajoute des exigences propres : localisation des données et des opérations au sein de l''Union européenne et immunité aux lois extraterritoriales (protection contre l''accès par des autorités hors UE), cloisonnement des environnements, personnels habilités et soumis à des engagements, supervision de sécurité (SOC) et journalisation, ainsi que la réversibilité (restitution des données et continuité de service en fin de contrat).',
    TRUE,
    NULL,
    'iso-27001,hds',
    'PUBLISHED', now(), now()
);

INSERT INTO standard_sections (id, standard_id, code, title, description, order_index) VALUES
    ('c4000004-0000-0000-0000-000000000001', 'c0000004-0000-0000-0000-000000000004', 'SOCLE', 'Socle ISO 27001 renforcé', 'Système de management de la sécurité de l''information renforcé.', 1),
    ('c4000004-0000-0000-0000-000000000002', 'c0000004-0000-0000-0000-000000000004', 'LOC', 'Localisation & souveraineté', 'Localisation UE des données et immunité aux lois extraterritoriales.', 2),
    ('c4000004-0000-0000-0000-000000000003', 'c0000004-0000-0000-0000-000000000004', 'OPS', 'Exploitation & personnels', 'Cloisonnement, habilitation des personnels et supervision de sécurité.', 3),
    ('c4000004-0000-0000-0000-000000000004', 'c0000004-0000-0000-0000-000000000004', 'REV', 'Réversibilité & continuité', 'Réversibilité et continuité de service en fin de contrat.', 4);

INSERT INTO standard_clauses (id, section_id, code, title, description, order_index) VALUES
    ('cd000004-0000-0000-0000-000000000001', 'c4000004-0000-0000-0000-000000000001', 'SMSI', 'SMSI ISO 27001 renforcé', 'SMSI conforme ISO 27001 complété des exigences SecNumCloud.', 1),
    ('cd000004-0000-0000-0000-000000000002', 'c4000004-0000-0000-0000-000000000002', 'LOC.1', 'Localisation UE des données', 'Localisation des données et opérations au sein de l''Union européenne.', 2),
    ('cd000004-0000-0000-0000-000000000003', 'c4000004-0000-0000-0000-000000000002', 'LOC.2', 'Immunité extraterritoriale', 'Protection contre l''accès aux données par des autorités hors UE.', 3),
    ('cd000004-0000-0000-0000-000000000004', 'c4000004-0000-0000-0000-000000000003', 'OPS.1', 'Cloisonnement', 'Cloisonnement des environnements et des données des clients.', 4),
    ('cd000004-0000-0000-0000-000000000005', 'c4000004-0000-0000-0000-000000000003', 'OPS.2', 'Personnels habilités', 'Habilitation et engagements des personnels accédant aux données.', 5),
    ('cd000004-0000-0000-0000-000000000006', 'c4000004-0000-0000-0000-000000000003', 'OPS.3', 'Supervision de sécurité (SOC)', 'Supervision de sécurité, détection et journalisation des événements.', 6),
    ('cd000004-0000-0000-0000-000000000007', 'c4000004-0000-0000-0000-000000000004', 'REV.1', 'Réversibilité', 'Restitution des données et réversibilité en fin de contrat.', 7);

INSERT INTO standard_requirements (clause_id, code, text, obligation, evidence_types, measurable_criteria, risk_if_missing, order_index) VALUES
    ('cd000004-0000-0000-0000-000000000001', 'SMSI', 'Le prestataire doit disposer d''un système de management de la sécurité de l''information conforme à ISO/IEC 27001 et renforcé selon SecNumCloud, couvrant le périmètre du service cloud qualifié.', 'MUST', 'DOCUMENT,AUDIT', 'Certificat ISO 27001 + exigences SecNumCloud couvrant 100 % du périmètre du service', 'CRITICAL', 1),
    ('cd000004-0000-0000-0000-000000000002', 'LOC.1', 'Le prestataire doit héberger les données et réaliser les opérations d''administration et de supervision au sein de l''Union européenne.', 'MUST', 'DOCUMENT,AUDIT', 'Localisation UE attestée pour 100 % des données et des opérations d''exploitation', 'CRITICAL', 2),
    ('cd000004-0000-0000-0000-000000000003', 'LOC.2', 'Le prestataire doit garantir l''immunité des données vis-à-vis des lois extraterritoriales et empêcher tout accès non autorisé par des autorités hors UE.', 'MUST', 'DOCUMENT,AUDIT', 'Mesures juridiques et techniques d''immunité documentées ; absence de contrôle capitalistique soumis au droit extra-UE', 'CRITICAL', 3),
    ('cd000004-0000-0000-0000-000000000004', 'OPS.1', 'Le prestataire doit cloisonner les environnements et les données des différents clients du service cloud.', 'MUST', 'DOCUMENT,AUDIT', 'Cloisonnement vérifié par tests ; 0 fuite inter-client constatée', 'HIGH', 4),
    ('cd000004-0000-0000-0000-000000000005', 'OPS.2', 'Le prestataire doit habiliter les personnels accédant aux données et leur faire souscrire des engagements de sécurité et de confidentialité.', 'MUST', 'DOCUMENT,TRAINING_RECORD', 'Habilitations à jour et engagements signés pour 100 % du personnel sensible', 'HIGH', 5),
    ('cd000004-0000-0000-0000-000000000006', 'OPS.3', 'Le prestataire doit assurer une supervision de sécurité (SOC), la détection des incidents et la journalisation des événements de sécurité.', 'MUST', 'DOCUMENT,KPI_RECORD', 'SOC opérationnel ; journaux de sécurité conservés selon la durée définie ; incidents tracés', 'HIGH', 6),
    ('cd000004-0000-0000-0000-000000000007', 'REV.1', 'Le prestataire doit garantir la réversibilité et la restitution des données du client en fin de contrat dans des conditions documentées.', 'MUST', 'DOCUMENT,AUDIT', 'Procédure de réversibilité documentée et testée ; délais et format de restitution contractualisés', 'HIGH', 7),
    ('cd000004-0000-0000-0000-000000000006', 'INC', 'Le prestataire doit notifier les incidents de sécurité majeurs au client et, le cas échéant, à l''ANSSI.', 'MUST', 'DOCUMENT,CAPA', 'Procédure de notification d''incident en place ; notifications tracées dans les délais requis', 'HIGH', 8);

INSERT INTO standard_certification_paths (id, standard_id, estimated_duration_months_min, estimated_duration_months_max,
    estimated_cost_eur_min, estimated_cost_eur_max, difficulty_level,
    surveillance_audit_frequency, recertification_cycle_years, notes)
VALUES ('ce000004-0000-0000-0000-000000000004', 'c0000004-0000-0000-0000-000000000004',
    12, 24, 100000, 500000, 5, 'annual', 3,
    'NUANCE : SecNumCloud est une QUALIFICATION délivrée par l''ANSSI (et non une certification accréditée), de cycle 3 ans. Socle ISO 27001 renforcé + exigences de souveraineté (localisation UE, immunité aux lois extraterritoriales). Niveau d''exigence très élevé (souveraineté, cloisonnement, SOC). Synergie avec les modules Audit, Document Control et cyber-incidents de QualitOS.');

INSERT INTO standard_certification_stages (certification_path_id, stage_number, name, description, typical_duration_weeks, deliverables, actors, qualitos_modules, order_index) VALUES
    ('ce000004-0000-0000-0000-000000000004', 1, 'Cadrage & périmètre du service', 'Définition du service cloud visé, du périmètre de qualification et engagement direction.', '4-8', 'Périmètre du service qualifié, engagement direction', 'RSSI, Direction', 'Document Control', 1),
    ('ce000004-0000-0000-0000-000000000004', 2, 'Socle ISO 27001 renforcé', 'Mise en place ou renforcement du SMSI ISO 27001 selon les exigences SecNumCloud.', '12-24', 'SMSI renforcé, SoA, certificat ou plan ISO 27001', 'RSSI', 'Document Control, Risk', 2),
    ('ce000004-0000-0000-0000-000000000004', 3, 'Souveraineté & localisation UE', 'Mise en conformité localisation UE et immunité aux lois extraterritoriales.', '6-12', 'Analyse juridique d''immunité, attestation de localisation UE', 'RSSI, Juridique', 'Document Control, Risk', 3),
    ('ce000004-0000-0000-0000-000000000004', 4, 'Exploitation, cloisonnement & SOC', 'Cloisonnement des environnements, habilitation des personnels et supervision de sécurité.', '8-16', 'Habilitations, dispositif SOC, plan de cloisonnement', 'RSSI, Production', 'cyber-incidents, Document Control', 4),
    ('ce000004-0000-0000-0000-000000000004', 5, 'Réversibilité & audit interne', 'Procédure de réversibilité et audit interne couvrant socle 27001 + exigences SecNumCloud.', '4-8', 'Procédure de réversibilité testée, rapport d''audit interne', 'RSSI, Auditeur interne', 'Audit, PDCA', 5),
    ('ce000004-0000-0000-0000-000000000004', 6, 'Audit de qualification ANSSI', 'Audit par un évaluateur, instruction et délivrance de la qualification par l''ANSSI.', '8-16', 'Attestation de qualification SecNumCloud', 'Évaluateur, ANSSI', 'Standards Hub, CAPA', 6);

-- ============================================================================
-- ITIL 4 — Gestion des services IT (l'organisation s'aligne ; certifs individuelles)
-- ============================================================================

INSERT INTO standards (id, code, full_name, publisher, current_version, publication_date,
    family, applicable_industries, description, certification_body_required,
    recertification_cycle_months, related_norm_codes, status, created_at, updated_at)
VALUES (
    'c0000005-0000-0000-0000-000000000005',
    'itil-4',
    'ITIL 4 — Cadre de gestion des services IT (IT Service Management)',
    'PeopleCert/Axelos',
    '4 (2019)',
    DATE '2019-02-28',
    'SECTORIEL',
    'it',
    'Cadre de bonnes pratiques de gestion des services IT (ITSM) structuré autour du Système de Valeur des Services (Service Value System — SVS), de la chaîne de valeur des services (Service Value Chain), de sept principes directeurs, de quatre dimensions et de 34 pratiques (dont la gestion des incidents, des problèmes, des changements, des niveaux de service, des demandes et l''amélioration continue — Continual Service Improvement, CSI). NUANCE IMPORTANTE : ITIL 4 ne donne PAS lieu à une certification de l''ORGANISATION — seules existent des certifications INDIVIDUELLES (Foundation, Managing Professional, Strategic Leader) délivrées par PeopleCert. Une organisation ne « se certifie » donc pas ITIL : elle s''ALIGNE sur le cadre (et peut, pour une certification de système, viser ISO/IEC 20000-1). Le champ « certification_body_required » est positionné à FALSE en conséquence.',
    FALSE,
    NULL,
    'iso-20000-1',
    'PUBLISHED', now(), now()
);

INSERT INTO standard_sections (id, standard_id, code, title, description, order_index) VALUES
    ('c5000005-0000-0000-0000-000000000001', 'c0000005-0000-0000-0000-000000000005', 'SVS', 'Service Value System', 'Système de valeur des services et chaîne de valeur (value streams).', 1),
    ('c5000005-0000-0000-0000-000000000002', 'c0000005-0000-0000-0000-000000000005', 'GP', 'Pratiques générales de management', 'Pratiques de gouvernance, dont l''amélioration continue (CSI).', 2),
    ('c5000005-0000-0000-0000-000000000003', 'c0000005-0000-0000-0000-000000000005', 'SMP', 'Pratiques de gestion des services', 'Incident, problème, changement, niveau de service, demande.', 3),
    ('c5000005-0000-0000-0000-000000000004', 'c0000005-0000-0000-0000-000000000005', 'TMP', 'Pratiques de gestion technique', 'Déploiement, gestion des infrastructures et plateformes.', 4);

INSERT INTO standard_clauses (id, section_id, code, title, description, order_index) VALUES
    ('c9000005-0000-0000-0000-000000000001', 'c5000005-0000-0000-0000-000000000001', 'SVS.1', 'Chaîne de valeur des services', 'Activités de la chaîne de valeur et flux de valeur (value streams).', 1),
    ('c9000005-0000-0000-0000-000000000002', 'c5000005-0000-0000-0000-000000000002', 'CSI', 'Amélioration continue', 'Pratique d''amélioration continue (Continual Service Improvement).', 2),
    ('c9000005-0000-0000-0000-000000000003', 'c5000005-0000-0000-0000-000000000003', 'INC', 'Gestion des incidents', 'Rétablissement du service normal dans les meilleurs délais.', 3),
    ('c9000005-0000-0000-0000-000000000004', 'c5000005-0000-0000-0000-000000000003', 'PRB', 'Gestion des problèmes', 'Identification et élimination des causes racines des incidents.', 4),
    ('c9000005-0000-0000-0000-000000000005', 'c5000005-0000-0000-0000-000000000003', 'CHG', 'Activation des changements', 'Évaluation, autorisation et planification des changements (change enablement).', 5),
    ('c9000005-0000-0000-0000-000000000006', 'c5000005-0000-0000-0000-000000000003', 'SLM', 'Gestion des niveaux de service', 'Définition, suivi et revue des niveaux de service (SLA).', 6),
    ('c9000005-0000-0000-0000-000000000007', 'c5000005-0000-0000-0000-000000000003', 'SRM', 'Gestion des demandes de service', 'Traitement des demandes de service standard des utilisateurs.', 7);

INSERT INTO standard_requirements (clause_id, code, text, obligation, evidence_types, measurable_criteria, risk_if_missing, order_index) VALUES
    ('c9000005-0000-0000-0000-000000000001', 'SVS.1', 'L''organisation devrait définir et opérer ses flux de valeur (value streams) reliant la demande à la création de valeur via la chaîne de valeur des services.', 'SHOULD', 'DOCUMENT', 'Flux de valeur principaux cartographiés et reliés aux pratiques', 'MEDIUM', 1),
    ('c9000005-0000-0000-0000-000000000002', 'CSI', 'L''organisation doit mettre en œuvre une démarche d''amélioration continue (CSI) avec un registre des améliorations alimenté et suivi.', 'MUST', 'DOCUMENT,PDCA_CYCLE', 'Registre d''amélioration continue tenu à jour ; améliorations priorisées et suivies', 'HIGH', 2),
    ('c9000005-0000-0000-0000-000000000003', 'INC', 'L''organisation doit gérer les incidents afin de rétablir le service normal dans les meilleurs délais et selon des cibles définies.', 'MUST', 'DOCUMENT,KPI_RECORD', 'Cibles de résolution (MTTR) définies par priorité ; taux de respect des SLA suivi', 'HIGH', 3),
    ('c9000005-0000-0000-0000-000000000004', 'PRB', 'L''organisation doit gérer les problèmes en identifiant et en éliminant les causes racines des incidents récurrents.', 'MUST', 'DOCUMENT,CAPA', 'Analyses de causes racines réalisées ; erreurs connues documentées et taux de récurrence suivi', 'HIGH', 4),
    ('c9000005-0000-0000-0000-000000000005', 'CHG', 'L''organisation doit évaluer, autoriser et planifier les changements afin de maîtriser le risque pour les services.', 'MUST', 'DOCUMENT,AUDIT', 'Changements évalués et autorisés ; taux de changements en échec (failed changes) suivi', 'HIGH', 5),
    ('c9000005-0000-0000-0000-000000000006', 'SLM', 'L''organisation doit définir, surveiller et revoir les niveaux de service convenus avec les clients (SLA).', 'MUST', 'DOCUMENT,KPI_RECORD', 'SLA définis et mesurés ; revues de service périodiques tenues', 'MEDIUM', 6),
    ('c9000005-0000-0000-0000-000000000007', 'SRM', 'L''organisation devrait traiter les demandes de service standard via un catalogue et des flux de traitement définis.', 'SHOULD', 'DOCUMENT,KPI_RECORD', 'Catalogue de services en place ; délai de traitement des demandes suivi', 'MEDIUM', 7),
    ('c9000005-0000-0000-0000-000000000002', 'CSI.PDCA', 'L''organisation devrait structurer l''amélioration continue selon une boucle PDCA (Plan-Do-Check-Act) reliée aux indicateurs de service.', 'SHOULD', 'DOCUMENT,PDCA_CYCLE', 'Cycles PDCA d''amélioration ouverts et clôturés ; impact mesuré sur les KPI de service', 'MEDIUM', 8);

INSERT INTO standard_certification_paths (id, standard_id, estimated_duration_months_min, estimated_duration_months_max,
    estimated_cost_eur_min, estimated_cost_eur_max, difficulty_level,
    surveillance_audit_frequency, recertification_cycle_years, notes)
VALUES ('ce000005-0000-0000-0000-000000000005', 'c0000005-0000-0000-0000-000000000005',
    3, 12, 5000, 50000, 3, 'none', NULL,
    'NUANCE IMPORTANTE : ITIL 4 ne certifie PAS l''organisation — seules des certifications INDIVIDUELLES (Foundation, Managing Professional, Strategic Leader) existent via PeopleCert. L''organisation s''ALIGNE sur le cadre ; pour une certification de système de gestion des services, elle vise ISO/IEC 20000-1. Le parcours ci-dessous décrit un projet d''ADOPTION/alignement ITIL 4. Synergie avec les modules ITSM, KPI et PDCA (CSI) de QualitOS.');

INSERT INTO standard_certification_stages (certification_path_id, stage_number, name, description, typical_duration_weeks, deliverables, actors, qualitos_modules, order_index) VALUES
    ('ce000005-0000-0000-0000-000000000005', 1, 'Cadrage & évaluation de maturité', 'Définition du périmètre ITSM et évaluation de maturité des pratiques ITIL existantes.', '3-6', 'Périmètre ITSM, rapport de maturité', 'Responsable des services, Direction IT', 'itsm, Document Control', 1),
    ('ce000005-0000-0000-0000-000000000005', 2, 'Formation & certifications individuelles', 'Montée en compétence des équipes et certifications individuelles ITIL 4 (Foundation, MP, SL).', '4-8', 'Plan de formation, certificats individuels ITIL 4', 'RH, équipes ITSM', 'Training', 2),
    ('ce000005-0000-0000-0000-000000000005', 3, 'Conception du SVS & des value streams', 'Définition du Service Value System et des flux de valeur prioritaires.', '4-8', 'SVS documenté, flux de valeur cartographiés', 'Responsable des services', 'itsm, Document Control', 3),
    ('ce000005-0000-0000-0000-000000000005', 4, 'Déploiement des pratiques clés', 'Mise en œuvre incident, problème, changement, niveaux de service et demandes.', '8-16', 'Processus ITSM déployés, catalogue de services, SLA', 'Équipes ITSM', 'itsm, kpi', 4),
    ('ce000005-0000-0000-0000-000000000005', 5, 'Amélioration continue (CSI)', 'Mise en place du registre d''amélioration continue et des cycles PDCA reliés aux KPI.', '4-8', 'Registre CSI, cycles PDCA, tableau de bord KPI de service', 'Responsable amélioration', 'pdca, kpi', 5),
    ('ce000005-0000-0000-0000-000000000005', 6, 'Revue d''alignement & maturité cible', 'Revue de l''alignement ITIL 4 et, le cas échéant, préparation à ISO/IEC 20000-1.', '3-5', 'Rapport d''alignement, feuille de route vers ISO 20000-1', 'Direction IT', 'pdca, Standards Hub', 6);
