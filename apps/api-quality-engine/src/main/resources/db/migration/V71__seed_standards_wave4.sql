-- Standards Hub — vague 4 (CLAUDE.md §8.2) : 5 référentiels réglementaires & transverses.
--   rgpd       : Règlement (UE) 2016/679 — protection des données personnelles
--   ai-act     : Règlement (UE) 2024/1689 — intelligence artificielle
--   nis2       : Directive (UE) 2022/2555 — cybersécurité des entités essentielles/importantes
--   iso-31000  : ISO 31000:2018 — management du risque (lignes directrices, non certifiable)
--   iso-17025  : ISO/IEC 17025:2017 — laboratoires d'étalonnage et d'essais (accréditation)
-- Même format que V70 : catalogue platform-level, UUID déterministes, préfixe 8
-- (standards 8000000n, sections 8n00000s, clauses 8[a-d|9]…, paths 8e…, stages 8f via FK).
-- Note UUID : pour éviter toute collision avec les paths (8e), les clauses de la 5e
-- norme (ISO 17025) portent le préfixe 89 (8a-8d couvrent les normes 1 à 4).

-- ============================================================================
-- RGPD — Règlement (UE) 2016/679 (protection des données à caractère personnel)
-- ============================================================================

INSERT INTO standards (id, code, full_name, publisher, current_version, publication_date,
    family, applicable_industries, description, certification_body_required,
    recertification_cycle_months, related_norm_codes, status, created_at, updated_at)
VALUES (
    '80000001-0000-0000-0000-000000000001',
    'rgpd',
    'Règlement (UE) 2016/679 — Règlement général sur la protection des données (RGPD/GDPR)',
    'UE',
    '2016/679',
    DATE '2016-04-27',
    'REGULATORY',
    'all',
    'Obligations applicables au traitement de données à caractère personnel (responsables et sous-traitants). Le RGPD n''est pas une norme certifiable par un organisme accrédité : la conformité se démontre par les mécanismes des art. 40-43 (codes de conduite approuvés et certifications/labels volontaires délivrés par des organismes agréés). La certification ISO 27701 reste la voie la plus mature de démonstration.',
    FALSE,
    NULL,
    'iso-27701,iso-27001',
    'PUBLISHED', now(), now()
);

INSERT INTO standard_sections (id, standard_id, code, title, description, order_index) VALUES
    ('81000001-0000-0000-0000-000000000001', '80000001-0000-0000-0000-000000000001', 'Chap.II', 'Principes (art. 5-11)', 'Licéité, loyauté, transparence, minimisation, exactitude, limitation, intégrité et responsabilité.', 1),
    ('81000001-0000-0000-0000-000000000002', '80000001-0000-0000-0000-000000000001', 'Chap.III', 'Droits des personnes (art. 12-23)', 'Information, accès, rectification, effacement, limitation, portabilité, opposition.', 2),
    ('81000001-0000-0000-0000-000000000003', '80000001-0000-0000-0000-000000000001', 'Chap.IV', 'Responsable & sous-traitant (art. 24-43)', 'Responsabilité, privacy by design, registre, sous-traitance, DPO, AIPD.', 3),
    ('81000001-0000-0000-0000-000000000004', '80000001-0000-0000-0000-000000000001', 'Chap.IV-V', 'Violations & transferts (art. 33-49)', 'Notification des violations et encadrement des transferts hors UE.', 4);

INSERT INTO standard_clauses (id, section_id, code, title, description, order_index) VALUES
    ('8a000001-0000-0000-0000-000000000001', '81000001-0000-0000-0000-000000000001', 'art.5', 'Principes relatifs au traitement', 'Six principes + responsabilité (accountability) du responsable de traitement.', 1),
    ('8a000001-0000-0000-0000-000000000002', '81000001-0000-0000-0000-000000000001', 'art.6-7', 'Licéité du traitement et consentement', 'Base légale du traitement et conditions de validité du consentement.', 2),
    ('8a000001-0000-0000-0000-000000000003', '81000001-0000-0000-0000-000000000002', 'art.12-14', 'Information des personnes', 'Information transparente, concise et accessible lors de la collecte.', 3),
    ('8a000001-0000-0000-0000-000000000004', '81000001-0000-0000-0000-000000000002', 'art.15-22', 'Exercice des droits', 'Accès, rectification, effacement, portabilité, opposition, décision automatisée.', 4),
    ('8a000001-0000-0000-0000-000000000005', '81000001-0000-0000-0000-000000000003', 'art.25', 'Protection des données dès la conception', 'Privacy by design et by default ; minimisation paramétrée par défaut.', 5),
    ('8a000001-0000-0000-0000-000000000006', '81000001-0000-0000-0000-000000000003', 'art.30', 'Registre des activités de traitement', 'Tenue d''un registre exhaustif des traitements (RoPA).', 6),
    ('8a000001-0000-0000-0000-000000000007', '81000001-0000-0000-0000-000000000003', 'art.35', 'Analyse d''impact (AIPD/DPIA)', 'AIPD obligatoire pour les traitements à risque élevé.', 7),
    ('8a000001-0000-0000-0000-000000000008', '81000001-0000-0000-0000-000000000003', 'art.37-39', 'Délégué à la protection des données', 'Désignation, position et missions du DPO.', 8),
    ('8a000001-0000-0000-0000-000000000009', '81000001-0000-0000-0000-000000000004', 'art.33-34', 'Notification des violations', 'Notification à l''autorité et communication aux personnes concernées.', 9),
    ('8a000001-0000-0000-0000-00000000000a', '81000001-0000-0000-0000-000000000004', 'art.44-49', 'Transferts hors UE', 'Encadrement par décision d''adéquation, garanties appropriées ou dérogations.', 10);

INSERT INTO standard_requirements (clause_id, code, text, obligation, evidence_types, measurable_criteria, risk_if_missing, order_index) VALUES
    ('8a000001-0000-0000-0000-000000000001', 'art.5.2', 'Le responsable du traitement doit pouvoir démontrer le respect des principes de l''art. 5 (accountability).', 'MUST', 'DOCUMENT,AUDIT', 'Politique de protection des données + preuves de conformité tenues à jour', 'HIGH', 1),
    ('8a000001-0000-0000-0000-000000000002', 'art.6.1', 'Chaque traitement doit reposer sur au moins une base légale identifiée et documentée.', 'MUST', 'DOCUMENT', 'Base légale renseignée pour 100 % des traitements du registre', 'CRITICAL', 2),
    ('8a000001-0000-0000-0000-000000000002', 'art.7.1', 'Lorsque le traitement repose sur le consentement, le responsable doit pouvoir en démontrer le recueil.', 'MUST', 'DOCUMENT,KPI_RECORD', 'Horodatage, portée et preuve de retrait du consentement conservés', 'HIGH', 3),
    ('8a000001-0000-0000-0000-000000000003', 'art.13', 'Fournir aux personnes une information complète au moment de la collecte des données.', 'MUST', 'DOCUMENT', 'Mentions d''information présentes sur 100 % des points de collecte', 'HIGH', 4),
    ('8a000001-0000-0000-0000-000000000004', 'art.12.3', 'Répondre aux demandes d''exercice des droits sans retard injustifié et au plus tard sous 1 mois.', 'MUST', 'DOCUMENT,KPI_RECORD', 'Délai de réponse aux demandes ≤ 1 mois ; taux de respect suivi', 'CRITICAL', 5),
    ('8a000001-0000-0000-0000-000000000005', 'art.25.1', 'Mettre en œuvre les mesures techniques et organisationnelles de protection dès la conception.', 'MUST', 'DOCUMENT,AUDIT', 'Privacy by design tracé dans les projets ; minimisation par défaut effective', 'HIGH', 6),
    ('8a000001-0000-0000-0000-000000000006', 'art.30.1', 'Tenir un registre des activités de traitement reprenant les mentions exigées.', 'MUST', 'DOCUMENT', 'Registre (RoPA) complet, daté et revu au moins annuellement', 'CRITICAL', 7),
    ('8a000001-0000-0000-0000-000000000007', 'art.35.1', 'Réaliser une AIPD avant tout traitement susceptible d''engendrer un risque élevé.', 'MUST', 'DOCUMENT,AUDIT', 'AIPD réalisée pour 100 % des traitements à risque élevé identifiés', 'CRITICAL', 8),
    ('8a000001-0000-0000-0000-000000000008', 'art.37.1', 'Désigner un délégué à la protection des données lorsque les conditions de l''art. 37 sont réunies.', 'MUST', 'DOCUMENT', 'DPO désigné et coordonnées communiquées à l''autorité de contrôle', 'HIGH', 9),
    ('8a000001-0000-0000-0000-000000000009', 'art.33.1', 'Notifier toute violation de données à l''autorité de contrôle dans les meilleurs délais et au plus tard 72 heures après en avoir pris connaissance.', 'MUST', 'DOCUMENT,CAPA', 'Procédure de notification ≤ 72 h ; registre des violations tenu', 'CRITICAL', 10),
    ('8a000001-0000-0000-0000-000000000009', 'art.34.1', 'Communiquer la violation aux personnes concernées lorsque le risque pour leurs droits est élevé.', 'MUST', 'DOCUMENT,CAPA', 'Critères de communication définis ; communications tracées', 'HIGH', 11),
    ('8a000001-0000-0000-0000-00000000000a', 'art.44', 'N''autoriser les transferts hors UE qu''avec une décision d''adéquation, des garanties appropriées ou une dérogation de l''art. 49.', 'MUST', 'DOCUMENT', 'Registre des transferts + garanties documentées (CCT, BCR…) pour 100 % des transferts', 'HIGH', 12);

INSERT INTO standard_certification_paths (id, standard_id, estimated_duration_months_min, estimated_duration_months_max,
    estimated_cost_eur_min, estimated_cost_eur_max, difficulty_level,
    surveillance_audit_frequency, recertification_cycle_years, notes)
VALUES ('8e000001-0000-0000-0000-000000000001', '80000001-0000-0000-0000-000000000001',
    6, 18, 15000, 120000, 4, 'continuous', 3,
    'Mise en conformité (pas de certificat accrédité). La conformité se démontre par la documentation (RoPA, AIPD, procédures) et, le cas échéant, une certification art. 42 ou l''ISO 27701. Synergie directe avec les modules RGPD QualitOS (RoPA, consentements, DSAR, violations).');

INSERT INTO standard_certification_stages (certification_path_id, stage_number, name, description, typical_duration_weeks, deliverables, actors, qualitos_modules, order_index) VALUES
    ('8e000001-0000-0000-0000-000000000001', 1, 'Cartographie des traitements', 'Recensement des traitements, des données, des finalités et des flux (y compris transferts).', '4-8', 'Cartographie des traitements et des flux', 'DPO, Métiers', 'Document Control, RoPA', 1),
    ('8e000001-0000-0000-0000-000000000001', 2, 'Registre & bases légales', 'Construction du registre RoPA et qualification des bases légales par traitement.', '3-6', 'Registre des traitements complet', 'DPO, Juridique', 'RoPA', 2),
    ('8e000001-0000-0000-0000-000000000001', 3, 'Gouvernance & DPO', 'Désignation du DPO, politique de protection des données, sensibilisation.', '2-4', 'Politique signée, DPO désigné, plan de sensibilisation', 'Direction, DPO', 'Document Control, Training', 3),
    ('8e000001-0000-0000-0000-000000000001', 4, 'AIPD & mesures de sécurité', 'AIPD des traitements à risque élevé et mise à niveau des mesures techniques et organisationnelles.', '4-10', 'AIPD, plan de remédiation sécurité', 'DPO, RSSI', 'DPIA, Risk', 4),
    ('8e000001-0000-0000-0000-000000000001', 5, 'Procédures violations & droits', 'Procédure de notification ≤ 72 h et outillage des demandes d''exercice des droits (DSAR).', '3-5', 'Procédures violation/DSAR, registre des violations', 'DPO, Support', 'Breaches, Subject Requests', 5),
    ('8e000001-0000-0000-0000-000000000001', 6, 'Audit & amélioration continue', 'Audit de conformité, tableau de bord RGPD et boucle d''amélioration.', '3-4', 'Rapport d''audit, plan d''amélioration', 'DPO, Auditeur interne', 'Audit, PDCA', 6);

-- ============================================================================
-- AI Act — Règlement (UE) 2024/1689 (intelligence artificielle)
-- ============================================================================

INSERT INTO standards (id, code, full_name, publisher, current_version, publication_date,
    family, applicable_industries, description, certification_body_required,
    recertification_cycle_months, related_norm_codes, status, created_at, updated_at)
VALUES (
    '80000002-0000-0000-0000-000000000002',
    'ai-act',
    'Règlement (UE) 2024/1689 — Règlement établissant des règles harmonisées concernant l''intelligence artificielle (AI Act)',
    'UE',
    '2024/1689',
    DATE '2024-06-13',
    'REGULATORY',
    'all',
    'Cadre par niveau de risque (inacceptable, élevé, limité, minimal). Les systèmes à haut risque exigent un système de gestion de la qualité, une gouvernance des données, de la transparence, une supervision humaine, l''enregistrement EUDB et un suivi post-commercialisation. Certains systèmes à haut risque relèvent d''une évaluation de conformité par organisme notifié (art. 43).',
    TRUE,
    NULL,
    'iso-27701,iso-31000,rgpd',
    'PUBLISHED', now(), now()
);

INSERT INTO standard_sections (id, standard_id, code, title, description, order_index) VALUES
    ('82000002-0000-0000-0000-000000000001', '80000002-0000-0000-0000-000000000002', 'art.6+Ann.III', 'Classification des risques', 'Critères de qualification des systèmes à haut risque et liste de l''Annexe III.', 1),
    ('82000002-0000-0000-0000-000000000002', '80000002-0000-0000-0000-000000000002', 'art.8-15', 'Exigences des systèmes à haut risque', 'SGQ, données, documentation, transparence, supervision, robustesse.', 2),
    ('82000002-0000-0000-0000-000000000003', '80000002-0000-0000-0000-000000000002', 'art.16-27', 'Obligations des opérateurs', 'Obligations des fournisseurs, déployeurs et autres opérateurs.', 3),
    ('82000002-0000-0000-0000-000000000004', '80000002-0000-0000-0000-000000000002', 'art.43+61-73', 'Gouvernance & post-market', 'Évaluation de conformité, surveillance après commercialisation et incidents.', 4);

INSERT INTO standard_clauses (id, section_id, code, title, description, order_index) VALUES
    ('8b000002-0000-0000-0000-000000000001', '82000002-0000-0000-0000-000000000001', 'art.6', 'Règles de classification haut risque', 'Détermination du caractère haut risque (composant de sécurité, Annexe III).', 1),
    ('8b000002-0000-0000-0000-000000000002', '82000002-0000-0000-0000-000000000002', 'art.9', 'Système de gestion des risques', 'Processus itératif de gestion des risques sur tout le cycle de vie.', 2),
    ('8b000002-0000-0000-0000-000000000003', '82000002-0000-0000-0000-000000000002', 'art.10', 'Gouvernance des données', 'Qualité, représentativité et gouvernance des jeux d''entraînement, validation et test.', 3),
    ('8b000002-0000-0000-0000-000000000004', '82000002-0000-0000-0000-000000000002', 'art.13', 'Transparence et information', 'Notice d''utilisation claire permettant une utilisation appropriée.', 4),
    ('8b000002-0000-0000-0000-000000000005', '82000002-0000-0000-0000-000000000002', 'art.14', 'Supervision humaine', 'Mesures permettant une supervision humaine effective du système.', 5),
    ('8b000002-0000-0000-0000-000000000006', '82000002-0000-0000-0000-000000000003', 'art.17', 'Système de gestion de la qualité', 'SGQ documenté du fournisseur de système à haut risque.', 6),
    ('8b000002-0000-0000-0000-000000000007', '82000002-0000-0000-0000-000000000003', 'art.27', 'Analyse d''impact sur les droits fondamentaux', 'FRIA réalisée par certains déployeurs avant mise en service.', 7),
    ('8b000002-0000-0000-0000-000000000008', '82000002-0000-0000-0000-000000000004', 'art.43', 'Évaluation de la conformité', 'Procédure d''évaluation (interne ou par organisme notifié) et marquage CE.', 8),
    ('8b000002-0000-0000-0000-000000000009', '82000002-0000-0000-0000-000000000004', 'art.49+71', 'Enregistrement dans la base UE', 'Enregistrement des systèmes à haut risque dans la base de données EUDB.', 9),
    ('8b000002-0000-0000-0000-00000000000a', '82000002-0000-0000-0000-000000000004', 'art.72-73', 'Post-market & incidents graves', 'Suivi après commercialisation et notification des incidents graves.', 10);

INSERT INTO standard_requirements (clause_id, code, text, obligation, evidence_types, measurable_criteria, risk_if_missing, order_index) VALUES
    ('8b000002-0000-0000-0000-000000000001', 'art.6.2', 'Déterminer et documenter si le système d''IA est à haut risque au sens de l''Annexe III.', 'MUST', 'DOCUMENT', 'Classification documentée et justifiée pour 100 % des systèmes d''IA déployés', 'CRITICAL', 1),
    ('8b000002-0000-0000-0000-000000000002', 'art.9.2', 'Établir, mettre en œuvre et tenir à jour un système de gestion des risques sur tout le cycle de vie.', 'MUST', 'DOCUMENT,AUDIT', 'Processus de gestion des risques documenté et revu à chaque évolution majeure', 'CRITICAL', 2),
    ('8b000002-0000-0000-0000-000000000003', 'art.10.2', 'Soumettre les jeux de données d''entraînement, de validation et de test à des pratiques de gouvernance appropriées.', 'MUST', 'DOCUMENT,KPI_RECORD', 'Critères de qualité/représentativité des données documentés et mesurés', 'HIGH', 3),
    ('8b000002-0000-0000-0000-000000000004', 'art.13.1', 'Concevoir le système de manière suffisamment transparente, avec une notice d''utilisation complète.', 'MUST', 'DOCUMENT', 'Notice d''utilisation conforme à l''art. 13.3 fournie aux déployeurs', 'HIGH', 4),
    ('8b000002-0000-0000-0000-000000000005', 'art.14.1', 'Concevoir le système de façon à permettre une supervision humaine effective pendant son utilisation.', 'MUST', 'DOCUMENT,AUDIT', 'Mesures de supervision humaine documentées et testées', 'CRITICAL', 5),
    ('8b000002-0000-0000-0000-000000000006', 'art.17.1', 'Mettre en place un système de gestion de la qualité documenté couvrant les exigences du règlement.', 'MUST', 'DOCUMENT,AUDIT', 'SGQ documenté couvrant les éléments de l''art. 17.1 (a) à (m)', 'CRITICAL', 6),
    ('8b000002-0000-0000-0000-000000000007', 'art.27.1', 'Réaliser une analyse d''impact sur les droits fondamentaux (FRIA) lorsque l''art. 27 l''exige.', 'MUST', 'DOCUMENT', 'FRIA réalisée et notifiée à l''autorité pour 100 % des cas concernés', 'HIGH', 7),
    ('8b000002-0000-0000-0000-000000000008', 'art.43.1', 'Soumettre le système à haut risque à la procédure d''évaluation de la conformité applicable avant mise sur le marché.', 'MUST', 'DOCUMENT,AUDIT', 'Évaluation de conformité réalisée + déclaration UE de conformité et marquage CE', 'CRITICAL', 8),
    ('8b000002-0000-0000-0000-000000000009', 'art.49.1', 'Enregistrer le système à haut risque dans la base de données de l''UE avant sa mise sur le marché ou en service.', 'MUST', 'DOCUMENT', 'Enregistrement EUDB effectif avant mise en service', 'HIGH', 9),
    ('8b000002-0000-0000-0000-00000000000a', 'art.72.1', 'Établir et documenter un système de surveillance après commercialisation proportionné au risque.', 'MUST', 'DOCUMENT,KPI_RECORD', 'Plan de surveillance post-market actif ; données collectées et analysées', 'HIGH', 10),
    ('8b000002-0000-0000-0000-00000000000a', 'art.73.1', 'Notifier tout incident grave aux autorités de surveillance dans les délais fixés par l''art. 73.', 'MUST', 'DOCUMENT,CAPA', 'Procédure de notification d''incident grave ≤ 15 jours (≤ 2 jours si grave/atteinte vie)', 'CRITICAL', 11);

INSERT INTO standard_certification_paths (id, standard_id, estimated_duration_months_min, estimated_duration_months_max,
    estimated_cost_eur_min, estimated_cost_eur_max, difficulty_level,
    surveillance_audit_frequency, recertification_cycle_years, notes)
VALUES ('8e000002-0000-0000-0000-000000000002', '80000002-0000-0000-0000-000000000002',
    9, 24, 25000, 200000, 5, 'continuous', 3,
    'Mise en conformité réglementaire ; évaluation par organisme notifié pour certains systèmes (art. 43). Synergie directe avec les modules QualitOS ai-qms (art. 17), ai-conformity (art. 43), ai-eudb (art. 49/71), fria (art. 27), ai-pmm (art. 72) et ai-incidents (art. 73).');

INSERT INTO standard_certification_stages (certification_path_id, stage_number, name, description, typical_duration_weeks, deliverables, actors, qualitos_modules, order_index) VALUES
    ('8e000002-0000-0000-0000-000000000002', 1, 'Inventaire & classification des systèmes IA', 'Recensement des systèmes d''IA et qualification du niveau de risque (Annexe III).', '3-6', 'Inventaire IA, classification de risque', 'AI Officer, Métiers', 'ai-conformity', 1),
    ('8e000002-0000-0000-0000-000000000002', 2, 'SGQ & gestion des risques', 'Mise en place du système de gestion de la qualité (art. 17) et du système de gestion des risques (art. 9).', '8-12', 'SGQ documenté, dossier de gestion des risques', 'AI Officer, Qualité', 'ai-qms, Risk', 2),
    ('8e000002-0000-0000-0000-000000000002', 3, 'Données, transparence & supervision', 'Gouvernance des données (art. 10), notice (art. 13), mesures de supervision humaine (art. 14).', '6-10', 'Datasheets, notices, dispositifs de supervision', 'Data, Engineering', 'ai-qms, ai-conformity', 3),
    ('8e000002-0000-0000-0000-000000000002', 4, 'FRIA & documentation technique', 'Analyse d''impact sur les droits fondamentaux et constitution de la documentation technique (Annexe IV).', '4-8', 'FRIA, documentation technique', 'AI Officer, DPO', 'fria, ai-conformity', 4),
    ('8e000002-0000-0000-0000-000000000002', 5, 'Évaluation de conformité & EUDB', 'Évaluation de conformité (interne ou organisme notifié), déclaration UE, enregistrement EUDB.', '4-12', 'Déclaration UE de conformité, enregistrement EUDB', 'AI Officer, Organisme notifié', 'ai-conformity, ai-eudb', 5),
    ('8e000002-0000-0000-0000-000000000002', 6, 'Surveillance post-market & incidents', 'Plan de surveillance après commercialisation et procédure de notification des incidents graves.', '3-5', 'Plan PMM, procédure incidents graves', 'AI Officer, Qualité', 'ai-pmm, ai-incidents', 6);

-- ============================================================================
-- NIS 2 — Directive (UE) 2022/2555 (cybersécurité des entités essentielles/importantes)
-- ============================================================================

INSERT INTO standards (id, code, full_name, publisher, current_version, publication_date,
    family, applicable_industries, description, certification_body_required,
    recertification_cycle_months, related_norm_codes, status, created_at, updated_at)
VALUES (
    '80000003-0000-0000-0000-000000000003',
    'nis2',
    'Directive (UE) 2022/2555 (NIS 2) — Mesures pour un niveau élevé commun de cybersécurité dans l''Union',
    'UE',
    '2022/2555',
    DATE '2022-12-14',
    'REGULATORY',
    'all',
    'Obligations de cybersécurité pour les entités essentielles et importantes : gouvernance par la direction (art. 20), dix mesures de gestion des risques (art. 21) et notification des incidents par paliers (art. 23). La transposition nationale impose des sanctions et une supervision (art. 31-37). Pas de certification accréditée, mais l''ISO 27001 sert de socle de conformité.',
    FALSE,
    NULL,
    'iso-27001,dora',
    'PUBLISHED', now(), now()
);

INSERT INTO standard_sections (id, standard_id, code, title, description, order_index) VALUES
    ('83000003-0000-0000-0000-000000000001', '80000003-0000-0000-0000-000000000003', 'art.20', 'Gouvernance', 'Responsabilité et formation des organes de direction.', 1),
    ('83000003-0000-0000-0000-000000000002', '80000003-0000-0000-0000-000000000003', 'art.21', 'Mesures de gestion des risques', 'Les dix mesures techniques, opérationnelles et organisationnelles.', 2),
    ('83000003-0000-0000-0000-000000000003', '80000003-0000-0000-0000-000000000003', 'art.23', 'Notification des incidents', 'Alerte précoce, notification et rapport final par paliers.', 3),
    ('83000003-0000-0000-0000-000000000004', '80000003-0000-0000-0000-000000000003', 'art.31-37', 'Supervision & sanctions', 'Pouvoirs de supervision et régime de sanctions.', 4);

INSERT INTO standard_clauses (id, section_id, code, title, description, order_index) VALUES
    ('8c000003-0000-0000-0000-000000000001', '83000003-0000-0000-0000-000000000001', 'art.20.1', 'Responsabilité de la direction', 'Approbation des mesures de gestion des risques et supervision par les dirigeants.', 1),
    ('8c000003-0000-0000-0000-000000000002', '83000003-0000-0000-0000-000000000001', 'art.20.2', 'Formation des dirigeants', 'Formation des organes de direction à l''identification et la gestion des risques.', 2),
    ('8c000003-0000-0000-0000-000000000003', '83000003-0000-0000-0000-000000000002', 'art.21.2', 'Mesures de gestion des risques', 'Les dix mesures minimales fondées sur une approche tous risques.', 3),
    ('8c000003-0000-0000-0000-000000000004', '83000003-0000-0000-0000-000000000002', 'art.21.2.d', 'Sécurité de la chaîne d''approvisionnement', 'Maîtrise des risques liés aux fournisseurs et prestataires.', 4),
    ('8c000003-0000-0000-0000-000000000005', '83000003-0000-0000-0000-000000000002', 'art.21.2.j', 'Authentification multifactorielle', 'MFA, communications sécurisées et continuité d''activité.', 5),
    ('8c000003-0000-0000-0000-000000000006', '83000003-0000-0000-0000-000000000003', 'art.23.4', 'Notification par paliers', 'Alerte précoce, notification d''incident et rapport final.', 6),
    ('8c000003-0000-0000-0000-000000000007', '83000003-0000-0000-0000-000000000004', 'art.32', 'Coopération avec la supervision', 'Réponse aux demandes des autorités de supervision.', 7);

INSERT INTO standard_requirements (clause_id, code, text, obligation, evidence_types, measurable_criteria, risk_if_missing, order_index) VALUES
    ('8c000003-0000-0000-0000-000000000001', 'art.20.1', 'Les organes de direction doivent approuver les mesures de gestion des risques de cybersécurité et superviser leur mise en œuvre.', 'MUST', 'DOCUMENT,AUDIT', 'Mesures approuvées par la direction ; revue de supervision tracée', 'CRITICAL', 1),
    ('8c000003-0000-0000-0000-000000000002', 'art.20.2', 'Les membres des organes de direction doivent suivre une formation à la cybersécurité.', 'MUST', 'TRAINING_RECORD', 'Formation des dirigeants réalisée et renouvelée périodiquement', 'HIGH', 2),
    ('8c000003-0000-0000-0000-000000000003', 'art.21.2', 'Mettre en œuvre les dix mesures minimales de gestion des risques (politiques, gestion des incidents, continuité, chaîne d''approvisionnement, etc.).', 'MUST', 'DOCUMENT,AUDIT', 'Les 10 mesures de l''art. 21§2 (a-j) documentées et opérationnelles', 'CRITICAL', 3),
    ('8c000003-0000-0000-0000-000000000004', 'art.21.2.d', 'Maîtriser la sécurité de la chaîne d''approvisionnement, y compris les fournisseurs directs.', 'MUST', 'DOCUMENT,AUDIT', 'Exigences cybersécurité contractualisées ; évaluation des fournisseurs critiques', 'HIGH', 4),
    ('8c000003-0000-0000-0000-000000000005', 'art.21.2.j', 'Recourir à l''authentification multifactorielle ou continue et à des communications sécurisées.', 'MUST', 'DOCUMENT,AUDIT', 'MFA déployée sur 100 % des accès critiques', 'HIGH', 5),
    ('8c000003-0000-0000-0000-000000000006', 'art.23.4.a', 'Émettre une alerte précoce à l''autorité compétente (CSIRT) dans les 24 heures suivant la connaissance d''un incident important.', 'MUST', 'DOCUMENT,CAPA', 'Alerte précoce ≤ 24 h depuis la détection', 'CRITICAL', 6),
    ('8c000003-0000-0000-0000-000000000006', 'art.23.4.b', 'Transmettre une notification d''incident dans les 72 heures suivant la connaissance de l''incident.', 'MUST', 'DOCUMENT,CAPA', 'Notification ≤ 72 h depuis la détection', 'CRITICAL', 7),
    ('8c000003-0000-0000-0000-000000000006', 'art.23.4.d', 'Remettre un rapport final au plus tard un mois après la notification de l''incident.', 'MUST', 'DOCUMENT,CAPA', 'Rapport final ≤ 1 mois après la notification', 'HIGH', 8),
    ('8c000003-0000-0000-0000-000000000007', 'art.32.1', 'Coopérer avec les autorités de supervision et répondre à leurs demandes d''information et d''audit.', 'MUST', 'DOCUMENT,AUDIT', 'Demandes des autorités traitées dans les délais impartis', 'MEDIUM', 9);

INSERT INTO standard_certification_paths (id, standard_id, estimated_duration_months_min, estimated_duration_months_max,
    estimated_cost_eur_min, estimated_cost_eur_max, difficulty_level,
    surveillance_audit_frequency, recertification_cycle_years, notes)
VALUES ('8e000003-0000-0000-0000-000000000003', '80000003-0000-0000-0000-000000000003',
    6, 18, 20000, 150000, 4, 'continuous', 3,
    'Mise en conformité réglementaire (pas de certificat accrédité ; supervision par l''autorité nationale). L''ISO 27001 couvre une large part des mesures de l''art. 21. Synergie avec les modules QualitOS nis2-measures (art. 21) et cyber-incidents (art. 23).');

INSERT INTO standard_certification_stages (certification_path_id, stage_number, name, description, typical_duration_weeks, deliverables, actors, qualitos_modules, order_index) VALUES
    ('8e000003-0000-0000-0000-000000000003', 1, 'Qualification de l''entité & périmètre', 'Déterminer le statut (essentielle/importante), le secteur (Annexe I/II) et le périmètre concerné.', '2-4', 'Note de qualification, périmètre', 'Direction, RSSI', 'Document Control', 1),
    ('8e000003-0000-0000-0000-000000000003', 2, 'Gouvernance & responsabilité direction', 'Engagement des organes de direction, formation et supervision (art. 20).', '2-4', 'Décision direction, plan de formation', 'Direction', 'Document Control, Training', 2),
    ('8e000003-0000-0000-0000-000000000003', 3, 'Analyse des écarts vs art. 21', 'Gap analysis des dix mesures de gestion des risques (socle ISO 27001).', '4-6', 'Rapport d''écarts, plan de remédiation', 'RSSI, Auditeur', 'Audit, nis2-measures', 3),
    ('8e000003-0000-0000-0000-000000000003', 4, 'Déploiement des mesures de sécurité', 'Mise en œuvre des mesures techniques et organisationnelles (MFA, chaîne d''appro, continuité).', '8-16', 'Mesures déployées, registre de risques', 'RSSI, IT', 'nis2-measures, Risk', 4),
    ('8e000003-0000-0000-0000-000000000003', 5, 'Processus de notification d''incidents', 'Outillage de l''alerte 24 h / notification 72 h / rapport final 1 mois.', '3-5', 'Procédure de notification, runbooks', 'RSSI, SOC', 'cyber-incidents, CAPA', 5),
    ('8e000003-0000-0000-0000-000000000003', 6, 'Audit & supervision continue', 'Audit interne des mesures, exercices et amélioration continue sous supervision.', '3-4', 'Rapport d''audit, plan d''amélioration', 'Auditeur interne, RSSI', 'Audit, PDCA', 6);

-- ============================================================================
-- ISO 31000:2018 — Management du risque (lignes directrices, non certifiable)
-- ============================================================================

INSERT INTO standards (id, code, full_name, publisher, current_version, publication_date,
    family, applicable_industries, description, certification_body_required,
    recertification_cycle_months, related_norm_codes, status, created_at, updated_at)
VALUES (
    '80000004-0000-0000-0000-000000000004',
    'iso-31000',
    'ISO 31000:2018 — Management du risque — Lignes directrices',
    'ISO',
    '2018',
    DATE '2018-02-15',
    'HLS',
    'all',
    'Lignes directrices (et non exigences) pour le management du risque, applicables à toute organisation. ISO 31000 n''est pas certifiable : aucun organisme accrédité ne délivre de certificat ISO 31000. Les exigences sont majoritairement des recommandations (il convient que / should). Cadre de référence pour les approches risque des autres systèmes de management.',
    FALSE,
    NULL,
    'iso-9001,iso-27001',
    'PUBLISHED', now(), now()
);

INSERT INTO standard_sections (id, standard_id, code, title, description, order_index) VALUES
    ('84000004-0000-0000-0000-000000000001', '80000004-0000-0000-0000-000000000004', '4', 'Principes', 'Finalité (création et préservation de la valeur) et principes du management du risque.', 1),
    ('84000004-0000-0000-0000-000000000002', '80000004-0000-0000-0000-000000000004', '5', 'Cadre organisationnel', 'Leadership, intégration, conception, mise en œuvre, évaluation et amélioration.', 2),
    ('84000004-0000-0000-0000-000000000003', '80000004-0000-0000-0000-000000000004', '6', 'Processus', 'Communication, domaine d''application, appréciation et traitement du risque.', 3);

INSERT INTO standard_clauses (id, section_id, code, title, description, order_index) VALUES
    ('8d000004-0000-0000-0000-000000000001', '84000004-0000-0000-0000-000000000001', '4', 'Principes du management du risque', 'Le management du risque crée et préserve la valeur ; principes (intégré, structuré, adapté…).', 1),
    ('8d000004-0000-0000-0000-000000000002', '84000004-0000-0000-0000-000000000002', '5.2', 'Leadership et engagement', 'Engagement de la direction et attribution des responsabilités.', 2),
    ('8d000004-0000-0000-0000-000000000003', '84000004-0000-0000-0000-000000000002', '5.3', 'Intégration', 'Intégration du management du risque dans la gouvernance et les processus.', 3),
    ('8d000004-0000-0000-0000-000000000004', '84000004-0000-0000-0000-000000000002', '5.7', 'Amélioration du cadre', 'Surveillance, revue et amélioration continue du cadre organisationnel.', 4),
    ('8d000004-0000-0000-0000-000000000005', '84000004-0000-0000-0000-000000000003', '6.3', 'Domaine d''application et contexte', 'Définition du périmètre, du contexte et des critères de risque.', 5),
    ('8d000004-0000-0000-0000-000000000006', '84000004-0000-0000-0000-000000000003', '6.4', 'Appréciation du risque', 'Identification, analyse et évaluation des risques.', 6),
    ('8d000004-0000-0000-0000-000000000007', '84000004-0000-0000-0000-000000000003', '6.5', 'Traitement du risque', 'Sélection et mise en œuvre des options de traitement.', 7),
    ('8d000004-0000-0000-0000-000000000008', '84000004-0000-0000-0000-000000000003', '6.6', 'Surveillance et revue', 'Surveillance et revue du processus et de ses résultats.', 8);

INSERT INTO standard_requirements (clause_id, code, text, obligation, evidence_types, measurable_criteria, risk_if_missing, order_index) VALUES
    ('8d000004-0000-0000-0000-000000000001', '4.a', 'Il convient que le management du risque soit intégré à l''ensemble des activités de l''organisation.', 'SHOULD', 'DOCUMENT', 'Management du risque référencé dans les processus clés', 'MEDIUM', 1),
    ('8d000004-0000-0000-0000-000000000002', '5.2', 'Il convient que la direction démontre son engagement et définisse une politique de management du risque.', 'SHOULD', 'DOCUMENT', 'Politique de management du risque approuvée par la direction', 'HIGH', 2),
    ('8d000004-0000-0000-0000-000000000003', '5.3', 'Il convient d''intégrer le management du risque dans la gouvernance, la stratégie et les processus.', 'SHOULD', 'DOCUMENT,AUDIT', 'Points d''intégration identifiés dans la cartographie des processus', 'MEDIUM', 3),
    ('8d000004-0000-0000-0000-000000000004', '5.7', 'Il convient de surveiller et de revoir en continu le cadre afin de l''améliorer.', 'SHOULD', 'DOCUMENT,PDCA_CYCLE', 'Revue du cadre réalisée au moins annuellement', 'MEDIUM', 4),
    ('8d000004-0000-0000-0000-000000000005', '6.3.4', 'Il convient de définir des critères de risque (vraisemblance, conséquences, niveau d''acceptabilité).', 'SHOULD', 'DOCUMENT', 'Critères de risque formalisés et approuvés', 'HIGH', 5),
    ('8d000004-0000-0000-0000-000000000006', '6.4.2', 'Il convient d''identifier les risques susceptibles d''affecter l''atteinte des objectifs.', 'SHOULD', 'DOCUMENT,AUDIT', 'Registre des risques tenu et revu à intervalles planifiés', 'HIGH', 6),
    ('8d000004-0000-0000-0000-000000000007', '6.5.2', 'Il convient de sélectionner et de mettre en œuvre des options de traitement du risque.', 'SHOULD', 'DOCUMENT,CAPA', 'Plans de traitement définis pour les risques hors appétence', 'HIGH', 7),
    ('8d000004-0000-0000-0000-000000000008', '6.6', 'Il convient de surveiller et de revoir le processus de management du risque et ses résultats.', 'SHOULD', 'DOCUMENT,KPI_RECORD', 'Indicateurs de suivi des risques (KRI) définis et revus', 'MEDIUM', 8);

INSERT INTO standard_certification_paths (id, standard_id, estimated_duration_months_min, estimated_duration_months_max,
    estimated_cost_eur_min, estimated_cost_eur_max, difficulty_level,
    surveillance_audit_frequency, recertification_cycle_years, notes)
VALUES ('8e000004-0000-0000-0000-000000000004', '80000004-0000-0000-0000-000000000004',
    3, 9, 8000, 40000, 2, 'none', NULL,
    'ISO 31000 n''est pas certifiable : il s''agit d''une adoption de lignes directrices, sans audit de certification ni audit de surveillance. Sert de socle au management du risque transverse (FMEA, risques SMSI, risques projet). Les modules Risk et Ishikawa de QualitOS instrumentent le processus §6.');

INSERT INTO standard_certification_stages (certification_path_id, stage_number, name, description, typical_duration_weeks, deliverables, actors, qualitos_modules, order_index) VALUES
    ('8e000004-0000-0000-0000-000000000004', 1, 'Engagement & politique', 'Engagement de la direction et politique de management du risque.', '2-3', 'Politique de management du risque', 'Direction, Risk Manager', 'Document Control', 1),
    ('8e000004-0000-0000-0000-000000000004', 2, 'Conception du cadre', 'Rôles, responsabilités, ressources et intégration dans la gouvernance.', '3-5', 'Cadre de management du risque, matrice RACI', 'Risk Manager', 'Document Control', 2),
    ('8e000004-0000-0000-0000-000000000004', 3, 'Critères & contexte', 'Définition du domaine d''application, du contexte et des critères de risque.', '2-3', 'Critères de risque, appétence au risque', 'Risk Manager, Métiers', 'Risk', 3),
    ('8e000004-0000-0000-0000-000000000004', 4, 'Appréciation & traitement', 'Identification, analyse, évaluation et traitement des risques.', '4-8', 'Registre des risques, plans de traitement', 'Risk Manager, Pilotes', 'Risk, Ishikawa, CAPA', 4),
    ('8e000004-0000-0000-0000-000000000004', 5, 'Surveillance & amélioration', 'KRI, surveillance, revue et amélioration continue du cadre.', '3-4', 'Tableau de bord des risques, revue', 'Risk Manager, Direction', 'KPI, PDCA', 5);

-- ============================================================================
-- ISO/IEC 17025:2017 — Laboratoires d'étalonnage et d'essais (accréditation)
-- ============================================================================

INSERT INTO standards (id, code, full_name, publisher, current_version, publication_date,
    family, applicable_industries, description, certification_body_required,
    recertification_cycle_months, related_norm_codes, status, created_at, updated_at)
VALUES (
    '80000005-0000-0000-0000-000000000005',
    'iso-17025',
    'ISO/IEC 17025:2017 — Exigences générales concernant la compétence des laboratoires d''étalonnages et d''essais',
    'ISO/IEC',
    '2017',
    DATE '2017-11-30',
    'SECTORIEL',
    'laboratory,healthcare,manufacturing,energie',
    'Exigences de compétence, d''impartialité et de fonctionnement des laboratoires. La reconnaissance s''obtient par accréditation (et non certification) délivrée par un organisme signataire des accords ILAC/EA (en France : COFRAC), évalué selon ISO/IEC 17011. Le champ couvre l''impartialité, la validation des méthodes, l''incertitude de mesure, la traçabilité métrologique et les essais d''aptitude.',
    TRUE,
    NULL,
    'iso-9001,iso-15189',
    'PUBLISHED', now(), now()
);

INSERT INTO standard_sections (id, standard_id, code, title, description, order_index) VALUES
    ('85000005-0000-0000-0000-000000000001', '80000005-0000-0000-0000-000000000005', '4', 'Exigences générales', 'Impartialité et confidentialité.', 1),
    ('85000005-0000-0000-0000-000000000002', '80000005-0000-0000-0000-000000000005', '5', 'Exigences structurelles', 'Statut juridique, organisation et responsabilités.', 2),
    ('85000005-0000-0000-0000-000000000003', '80000005-0000-0000-0000-000000000005', '6', 'Exigences relatives aux ressources', 'Personnel, installations, équipements, traçabilité métrologique.', 3),
    ('85000005-0000-0000-0000-000000000004', '80000005-0000-0000-0000-000000000005', '7', 'Exigences relatives aux processus', 'Méthodes, échantillonnage, incertitude, validité des résultats, rapports.', 4),
    ('85000005-0000-0000-0000-000000000005', '80000005-0000-0000-0000-000000000005', '8', 'Exigences relatives au système de management', 'Système de management du laboratoire (options A ou B).', 5);

INSERT INTO standard_clauses (id, section_id, code, title, description, order_index) VALUES
    ('89000005-0000-0000-0000-000000000001', '85000005-0000-0000-0000-000000000001', '4.1', 'Impartialité', 'Garantir l''impartialité des activités de laboratoire et gérer les risques associés.', 1),
    ('89000005-0000-0000-0000-000000000002', '85000005-0000-0000-0000-000000000001', '4.2', 'Confidentialité', 'Protéger les informations obtenues ou créées lors des activités.', 2),
    ('89000005-0000-0000-0000-000000000003', '85000005-0000-0000-0000-000000000003', '6.2', 'Personnel', 'Compétence, qualification, autorisation et surveillance du personnel.', 3),
    ('89000005-0000-0000-0000-000000000004', '85000005-0000-0000-0000-000000000003', '6.5', 'Traçabilité métrologique', 'Établir et maintenir la traçabilité métrologique des résultats.', 4),
    ('89000005-0000-0000-0000-000000000005', '85000005-0000-0000-0000-000000000004', '7.2', 'Sélection et validation des méthodes', 'Choix, vérification et validation des méthodes employées.', 5),
    ('89000005-0000-0000-0000-000000000006', '85000005-0000-0000-0000-000000000004', '7.6', 'Évaluation de l''incertitude de mesure', 'Identifier les contributions et estimer l''incertitude de mesure.', 6),
    ('89000005-0000-0000-0000-000000000007', '85000005-0000-0000-0000-000000000004', '7.7', 'Assurance de la validité des résultats', 'Surveillance de la validité, dont les essais d''aptitude (EILA).', 7),
    ('89000005-0000-0000-0000-000000000008', '85000005-0000-0000-0000-000000000004', '7.8', 'Rapports sur les résultats', 'Contenu et exactitude des rapports d''essais et certificats d''étalonnage.', 8);

INSERT INTO standard_requirements (clause_id, code, text, obligation, evidence_types, measurable_criteria, risk_if_missing, order_index) VALUES
    ('89000005-0000-0000-0000-000000000001', '4.1.4', 'Le laboratoire doit identifier en continu les risques pesant sur son impartialité et les éliminer ou les minimiser.', 'MUST', 'DOCUMENT,AUDIT', 'Analyse des risques d''impartialité tenue à jour et revue', 'CRITICAL', 1),
    ('89000005-0000-0000-0000-000000000002', '4.2.1', 'Le laboratoire doit être responsable de la gestion de toutes les informations obtenues ou créées (confidentialité).', 'MUST', 'DOCUMENT', 'Engagements de confidentialité signés ; informations protégées', 'HIGH', 2),
    ('89000005-0000-0000-0000-000000000003', '6.2.3', 'Le laboratoire doit s''assurer que le personnel possède la compétence requise et documenter les autorisations.', 'MUST', 'TRAINING_RECORD,DOCUMENT', 'Habilitations à jour pour 100 % du personnel autorisé', 'HIGH', 3),
    ('89000005-0000-0000-0000-000000000004', '6.5.1', 'Le laboratoire doit établir et maintenir la traçabilité métrologique de ses résultats par une chaîne ininterrompue d''étalonnages.', 'MUST', 'DOCUMENT,KPI_RECORD', 'Certificats d''étalonnage raccordés à des étalons (SI/national) pour 100 % des équipements critiques', 'CRITICAL', 4),
    ('89000005-0000-0000-0000-000000000005', '7.2.2.1', 'Le laboratoire doit valider les méthodes non normalisées, développées ou modifiées avant utilisation.', 'MUST', 'DOCUMENT,AUDIT', 'Dossier de validation par méthode (justesse, fidélité, limites)', 'CRITICAL', 5),
    ('89000005-0000-0000-0000-000000000006', '7.6.1', 'Le laboratoire doit identifier les contributions à l''incertitude et évaluer l''incertitude de mesure.', 'MUST', 'DOCUMENT', 'Budget d''incertitude documenté par méthode/grandeur', 'HIGH', 6),
    ('89000005-0000-0000-0000-000000000007', '7.7.2', 'Le laboratoire doit surveiller sa performance par comparaison avec d''autres laboratoires (essais d''aptitude) lorsque disponibles.', 'MUST', 'KPI_RECORD,DOCUMENT', 'Participation aux EILA planifiée ; écarts (z-score |z|<2) suivis et traités', 'HIGH', 7),
    ('89000005-0000-0000-0000-000000000008', '7.8.2', 'Les rapports d''essais et certificats d''étalonnage doivent contenir les informations exigées et être exacts.', 'MUST', 'DOCUMENT', 'Modèle de rapport conforme à §7.8 ; taux d''erreur de rapport suivi', 'HIGH', 8);

INSERT INTO standard_certification_paths (id, standard_id, estimated_duration_months_min, estimated_duration_months_max,
    estimated_cost_eur_min, estimated_cost_eur_max, difficulty_level,
    surveillance_audit_frequency, recertification_cycle_years, notes)
VALUES ('8e000005-0000-0000-0000-000000000005', '80000005-0000-0000-0000-000000000005',
    12, 24, 30000, 100000, 5, 'annual', 4,
    'Reconnaissance par ACCRÉDITATION (et non certification) délivrée par un organisme signataire ILAC/EA — COFRAC en France. Cycle d''accréditation typiquement quadriennal avec évaluations de surveillance annuelles. Les modules Calibration/Equipment et Audit de QualitOS soutiennent §6.5 et §7.');

INSERT INTO standard_certification_stages (certification_path_id, stage_number, name, description, typical_duration_weeks, deliverables, actors, qualitos_modules, order_index) VALUES
    ('8e000005-0000-0000-0000-000000000005', 1, 'Cadrage & portée d''accréditation', 'Définition de la portée (essais/étalonnages, méthodes, grandeurs) et engagement direction.', '3-5', 'Portée d''accréditation, engagement', 'Direction, Responsable qualité labo', 'Document Control', 1),
    ('8e000005-0000-0000-0000-000000000005', 2, 'Système de management & impartialité', 'Mise en place du système de management (option A/B), gestion de l''impartialité et de la confidentialité.', '6-10', 'Manuel/procédures, analyse d''impartialité', 'Responsable qualité', 'Document Control, Risk', 2),
    ('8e000005-0000-0000-0000-000000000005', 3, 'Ressources & traçabilité métrologique', 'Compétences du personnel, étalonnage des équipements et chaîne de traçabilité métrologique.', '8-12', 'Habilitations, plan d''étalonnage, certificats', 'Métrologie, RH', 'Calibration, Training', 3),
    ('8e000005-0000-0000-0000-000000000005', 4, 'Méthodes, incertitude & validité', 'Validation des méthodes, budgets d''incertitude et programme d''essais d''aptitude (EILA).', '8-16', 'Dossiers de validation, budgets d''incertitude, plan EILA', 'Techniciens, Qualité', 'Document Control, KPI', 4),
    ('8e000005-0000-0000-0000-000000000005', 5, 'Audit interne & revue de direction', 'Audit interne couvrant §4 à §8 et revue de direction.', '3-5', 'Rapport d''audit interne, CR revue', 'Auditeur interne, Direction', 'Audit, PDCA', 5),
    ('8e000005-0000-0000-0000-000000000005', 6, 'Évaluation d''accréditation (COFRAC/ILAC)', 'Évaluation documentaire puis évaluation sur site (avec évaluation technique), levée des écarts.', '4-8', 'Attestation d''accréditation ISO/IEC 17025', 'Organisme d''accréditation', 'Standards Hub', 6);
