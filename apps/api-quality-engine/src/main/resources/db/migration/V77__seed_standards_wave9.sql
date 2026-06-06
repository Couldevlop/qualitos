-- Standards Hub — vague 9 (CLAUDE.md §8.2) : 5 référentiels agro / industrie / BTP.
--   ifs-food-v8  : IFS Food v8 (2023) — sécurité & qualité des denrées (schéma GFSI)
--   brcgs-food-v9: BRCGS Food Safety v9 (2022) — sécurité des denrées (schéma GFSI)
--   globalgap    : GLOBALG.A.P. IFA v6 — assurance intégrée des exploitations agricoles
--   vda-6-3      : VDA 6.3 (2023) — audit de processus automobile
--   iso-19650    : ISO 19650-1/-2:2018 — gestion de l'information BIM (BTP)
-- Même format que V72 : catalogue platform-level, UUID déterministes, préfixe « e ».
--   standards    : e000000n-...                       (n = 1..5)
--   sections     : en00000s-...                       (n = norme 1..5, s = section)
--   clauses      : ea (norme 1), eb (2), ec (3), ed (4), e9 (5e norme) ...
--   paths        : ee00000n-...                       (stages via FK sur le path)
-- Note collision : les vagues précédentes utilisent 7e/8e/9e/ae/be/ce pour LEURS
--   paths, et V14/V59 emploient e1111111…/e1400401… (clauses ISO 14001/45001/22301).
--   Le schéma déterministe ci-dessous (en00000s, e?000001, ee00000n) ne recoupe
--   aucune de ces valeurs ; l'unicité cross-fichier V1..V73 est vérifiée par script.

-- ============================================================================
-- IFS Food v8 (2023) — Sécurité & qualité des denrées alimentaires (GFSI)
-- ============================================================================

INSERT INTO standards (id, code, full_name, publisher, current_version, publication_date,
    family, applicable_industries, description, certification_body_required,
    recertification_cycle_months, related_norm_codes, status, created_at, updated_at)
VALUES (
    'e0000001-0000-0000-0000-000000000001',
    'ifs-food-v8',
    'IFS Food v8 — Référentiel international pour la sécurité et la qualité des denrées alimentaires',
    'IFS Management',
    'v8',
    DATE '2023-04-01',
    'SECTORIEL',
    'food,agro',
    'Référentiel de certification reconnu GFSI destiné aux transformateurs de denrées alimentaires et aux entreprises conditionnant des produits alimentaires en vrac. La version 8 renforce la gouvernance et l''engagement de la direction, la culture de la sécurité des aliments (food safety culture), le plan HACCP, la food defense, l''atténuation de la fraude alimentaire (food fraud), la maîtrise des corps étrangers et l''exactitude de l''étiquetage. L''évaluation est ANNUELLE et donne lieu à une notation par exigence (A/B/C/D) débouchant sur deux niveaux de certification : Foundation level et Higher level — une exigence majeure (« KO » noté D) ou une non-conformité majeure empêche la certification. Au moins un tiers des audits du cycle est réalisé de manière non annoncée (unannounced).',
    TRUE,
    NULL,
    'haccp,fssc-22000,iso-22000',
    'PUBLISHED', now(), now()
);

INSERT INTO standard_sections (id, standard_id, code, title, description, order_index) VALUES
    ('e1000001-0000-0000-0000-000000000001', 'e0000001-0000-0000-0000-000000000001', '1', 'Gouvernance & engagement', 'Responsabilité de la direction, politique, objectifs et culture de la sécurité des aliments.', 1),
    ('e1000001-0000-0000-0000-000000000002', 'e0000001-0000-0000-0000-000000000001', '2', 'Système de management & HACCP', 'Système de management de la sécurité et de la qualité et plan HACCP.', 2),
    ('e1000001-0000-0000-0000-000000000003', 'e0000001-0000-0000-0000-000000000001', '4', 'Processus & maîtrise produit', 'Achats, étiquetage, corps étrangers et maîtrise des processus de fabrication.', 3),
    ('e1000001-0000-0000-0000-000000000004', 'e0000001-0000-0000-0000-000000000001', '5', 'Mesures, analyses & food defense', 'Audits internes, food defense, fraude alimentaire et amélioration.', 4),
    ('e1000001-0000-0000-0000-000000000005', 'e0000001-0000-0000-0000-000000000001', '6', 'Gouvernance de la certification', 'Notation A/B/C/D, niveaux Foundation/Higher et audits non annoncés.', 5);

INSERT INTO standard_clauses (id, section_id, code, title, description, order_index) VALUES
    ('ea000001-0000-0000-0000-000000000001', 'e1000001-0000-0000-0000-000000000001', '1.1', 'Engagement de la direction', 'Responsabilité de la direction, politique et revue de direction.', 1),
    ('ea000001-0000-0000-0000-000000000002', 'e1000001-0000-0000-0000-000000000001', '1.4', 'Culture de la sécurité des aliments', 'Définition et déploiement d''un plan de food safety culture.', 2),
    ('ea000001-0000-0000-0000-000000000003', 'e1000001-0000-0000-0000-000000000002', '2.2', 'Plan HACCP', 'Analyse des dangers, détermination des CCP et plan HACCP fondé sur le Codex.', 3),
    ('ea000001-0000-0000-0000-000000000004', 'e1000001-0000-0000-0000-000000000003', '4.5', 'Spécifications & achats', 'Maîtrise des fournisseurs, spécifications des matières et services.', 4),
    ('ea000001-0000-0000-0000-000000000005', 'e1000001-0000-0000-0000-000000000003', '4.13', 'Maîtrise des corps étrangers', 'Prévention et détection des corps étrangers (métal, verre, plastique dur).', 5),
    ('ea000001-0000-0000-0000-000000000006', 'e1000001-0000-0000-0000-000000000003', '4.16', 'Étiquetage & information produit', 'Exactitude de l''étiquetage et déclaration des allergènes.', 6),
    ('ea000001-0000-0000-0000-000000000007', 'e1000001-0000-0000-0000-000000000004', '5.1', 'Audits internes', 'Programme d''audits internes couvrant l''ensemble du référentiel.', 7),
    ('ea000001-0000-0000-0000-000000000008', 'e1000001-0000-0000-0000-000000000004', '6.1', 'Food defense', 'Évaluation des menaces et plan de protection contre la malveillance.', 8),
    ('ea000001-0000-0000-0000-000000000009', 'e1000001-0000-0000-0000-000000000004', '6.2', 'Atténuation de la fraude alimentaire', 'Évaluation de la vulnérabilité (VACCP) et plan d''atténuation de la fraude.', 9),
    ('ea000001-0000-0000-0000-00000000000a', 'e1000001-0000-0000-0000-000000000005', 'AUD', 'Audits non annoncés & notation', 'Notation A/B/C/D et part d''audits non annoncés sur le cycle.', 10);

INSERT INTO standard_requirements (clause_id, code, text, obligation, evidence_types, measurable_criteria, risk_if_missing, order_index) VALUES
    ('ea000001-0000-0000-0000-000000000001', '1.1.1', 'La direction doit définir une politique de sécurité et de qualité des aliments, fixer des objectifs et conduire une revue de direction périodique.', 'MUST', 'DOCUMENT,AUDIT', 'Politique et objectifs documentés ; revue de direction réalisée au moins annuellement', 'HIGH', 1),
    ('ea000001-0000-0000-0000-000000000002', '1.4.1', 'L''entreprise doit définir, déployer et entretenir un plan de culture de la sécurité des aliments couvrant la communication, la formation et le retour d''information.', 'MUST', 'DOCUMENT,TRAINING_RECORD', 'Plan de food safety culture documenté ; indicateurs de culture suivis et revus', 'MEDIUM', 2),
    ('ea000001-0000-0000-0000-000000000003', '2.2.1', 'L''entreprise doit établir et maintenir un plan HACCP fondé sur les principes du Codex Alimentarius, couvrant l''analyse des dangers et la maîtrise des CCP.', 'MUST', 'DOCUMENT,AUDIT', 'Plan HACCP couvrant 100 % des étapes ; CCP identifiés, surveillés et enregistrés', 'CRITICAL', 3),
    ('ea000001-0000-0000-0000-000000000004', '4.5.1', 'L''entreprise doit maîtriser ses achats et évaluer ses fournisseurs de matières et de services pouvant affecter la sécurité des denrées.', 'MUST', 'DOCUMENT,AUDIT', 'Fournisseurs critiques évalués et approuvés ; spécifications matières à jour', 'HIGH', 4),
    ('ea000001-0000-0000-0000-000000000005', '4.13.1', 'L''entreprise doit mettre en œuvre des mesures de prévention et de détection des corps étrangers et vérifier l''efficacité des équipements de détection.', 'MUST', 'DOCUMENT,KPI_RECORD', 'Plan de maîtrise des corps étrangers ; vérification des détecteurs à fréquence définie', 'CRITICAL', 5),
    ('ea000001-0000-0000-0000-000000000006', '4.16.1', 'L''entreprise doit garantir l''exactitude de l''étiquetage, y compris la déclaration des allergènes et la conformité réglementaire.', 'MUST', 'DOCUMENT', 'Vérification de l''étiquetage avant mise sur le marché ; 0 erreur d''allergène', 'CRITICAL', 6),
    ('ea000001-0000-0000-0000-000000000007', '5.1.1', 'L''entreprise doit planifier et réaliser des audits internes couvrant l''ensemble des exigences du référentiel et tracer les non-conformités.', 'MUST', 'AUDIT,DOCUMENT', 'Programme d''audits internes couvrant 100 % du référentiel sur le cycle ; écarts suivis', 'HIGH', 7),
    ('ea000001-0000-0000-0000-000000000008', '6.1.1', 'L''entreprise doit réaliser une évaluation des menaces (food defense) et mettre en œuvre un plan de protection documenté.', 'MUST', 'DOCUMENT,AUDIT', 'Évaluation des menaces et plan de food defense revus au moins annuellement', 'HIGH', 8),
    ('ea000001-0000-0000-0000-000000000009', '6.2.1', 'L''entreprise doit réaliser une évaluation de la vulnérabilité à la fraude (VACCP) et définir un plan d''atténuation.', 'MUST', 'DOCUMENT,AUDIT', 'Évaluation de vulnérabilité fraude documentée ; plan d''atténuation revu annuellement', 'HIGH', 9),
    ('ea000001-0000-0000-0000-00000000000a', 'AUD.1', 'L''entreprise doit se soumettre à l''audit annuel et aux audits non annoncés du schéma, et traiter les non-conformités issues de la notation A/B/C/D.', 'MUST', 'AUDIT,CAPA', 'Au moins un tiers des audits du cycle non annoncés ; non-conformités majeures soldées', 'HIGH', 10);

INSERT INTO standard_certification_paths (id, standard_id, estimated_duration_months_min, estimated_duration_months_max,
    estimated_cost_eur_min, estimated_cost_eur_max, difficulty_level,
    surveillance_audit_frequency, recertification_cycle_years, notes)
VALUES ('ee000001-0000-0000-0000-000000000001', 'e0000001-0000-0000-0000-000000000001',
    9, 18, 12000, 50000, 4, 'annual', 1,
    'Certification par organisme accrédité (schéma reconnu GFSI). Évaluation ANNUELLE avec notation par exigence (A/B/C/D) et deux niveaux de certification (Foundation / Higher) ; au moins un tiers des audits du cycle est réalisé de manière non annoncée. Synergie avec les modules Audit, CAPA, Supplier et Training (food safety culture) de QualitOS.');

INSERT INTO standard_certification_stages (certification_path_id, stage_number, name, description, typical_duration_weeks, deliverables, actors, qualitos_modules, order_index) VALUES
    ('ee000001-0000-0000-0000-000000000001', 1, 'Cadrage & gap analysis', 'Définition du périmètre (sites, gammes de produits) et diagnostic d''écarts vs IFS Food v8.', '3-6', 'Périmètre, rapport d''écarts', 'Direction, Responsable qualité', 'Audit, Document Control', 1),
    ('ee000001-0000-0000-0000-000000000001', 2, 'Système de management & HACCP', 'Mise en place du système de management et du plan HACCP (analyse des dangers, CCP).', '6-10', 'Système documenté, plan HACCP', 'Équipe sécurité des aliments', 'Document Control, Ishikawa', 2),
    ('ee000001-0000-0000-0000-000000000001', 3, 'Maîtrise produit & fournisseurs', 'Maîtrise des achats, étiquetage, corps étrangers et spécifications fournisseurs.', '4-8', 'Spécifications fournisseurs, plan corps étrangers, contrôle étiquetage', 'Production, Achats', 'Supplier, Document Control', 3),
    ('ee000001-0000-0000-0000-000000000001', 4, 'Food defense, fraude & culture', 'Évaluation des menaces (food defense), vulnérabilité fraude (VACCP) et plan de culture sécurité.', '4-8', 'Plans food defense/VACCP, plan culture sécurité', 'Responsable qualité, RH', 'Training, CAPA', 4),
    ('ee000001-0000-0000-0000-000000000001', 5, 'Audit interne & revue de direction', 'Audit interne couvrant le référentiel et revue de direction.', '3-5', 'Rapport d''audit interne, compte rendu de revue', 'Auditeur interne, Direction', 'Audit, PDCA', 5),
    ('ee000001-0000-0000-0000-000000000001', 6, 'Audit de certification IFS', 'Audit sur site par l''organisme certificateur, notation A/B/C/D et levée des non-conformités.', '3-6', 'Certificat IFS Food v8 (Foundation / Higher)', 'Organisme certificateur', 'Standards Hub, CAPA', 6);

-- ============================================================================
-- BRCGS Food Safety v9 (2022) — Sécurité des denrées alimentaires (GFSI)
-- ============================================================================

INSERT INTO standards (id, code, full_name, publisher, current_version, publication_date,
    family, applicable_industries, description, certification_body_required,
    recertification_cycle_months, related_norm_codes, status, created_at, updated_at)
VALUES (
    'e0000002-0000-0000-0000-000000000002',
    'brcgs-food-v9',
    'BRCGS Food Safety Issue 9 — Norme mondiale pour la sécurité des denrées alimentaires',
    'BRCGS',
    'Issue 9',
    DATE '2022-08-01',
    'SECTORIEL',
    'food,agro',
    'Norme mondiale reconnue GFSI pour la sécurité des denrées alimentaires, destinée aux fabricants et transformateurs. L''Issue 9 met l''accent sur l''engagement de la direction, le plan de sécurité des aliments fondé sur HACCP, la culture de la sécurité des aliments, la maîtrise des zones de production à risque (high-risk / high-care), la gestion des fournisseurs et la traçabilité. L''audit donne lieu à une notation de grade (AA, A, B, C, D selon le nombre et la nature des non-conformités) ; un programme d''audits non annoncés est proposé en option. La traçabilité doit pouvoir être démontrée rapidement, dans un délai cible n''excédant pas quatre heures.',
    TRUE,
    NULL,
    'haccp,ifs-food-v8',
    'PUBLISHED', now(), now()
);

INSERT INTO standard_sections (id, standard_id, code, title, description, order_index) VALUES
    ('e2000002-0000-0000-0000-000000000001', 'e0000002-0000-0000-0000-000000000002', '1', 'Engagement de la direction', 'Engagement de la direction et culture de la sécurité des aliments.', 1),
    ('e2000002-0000-0000-0000-000000000002', 'e0000002-0000-0000-0000-000000000002', '2', 'Plan de sécurité des aliments (HACCP)', 'Plan de sécurité des aliments fondé sur les principes HACCP du Codex.', 2),
    ('e2000002-0000-0000-0000-000000000003', 'e0000002-0000-0000-0000-000000000002', '3', 'Système de management de la qualité', 'Audits internes, gestion des fournisseurs et traçabilité.', 3),
    ('e2000002-0000-0000-0000-000000000004', 'e0000002-0000-0000-0000-000000000002', '4-5', 'Site & maîtrise produit', 'Normes relatives au site, zones à risque et maîtrise des produits.', 4);

INSERT INTO standard_clauses (id, section_id, code, title, description, order_index) VALUES
    ('eb000002-0000-0000-0000-000000000001', 'e2000002-0000-0000-0000-000000000001', '1.1', 'Engagement de la direction', 'Engagement, ressources et revue de direction.', 1),
    ('eb000002-0000-0000-0000-000000000002', 'e2000002-0000-0000-0000-000000000001', '1.2', 'Culture de la sécurité des aliments', 'Plan de développement de la culture de sécurité des aliments.', 2),
    ('eb000002-0000-0000-0000-000000000003', 'e2000002-0000-0000-0000-000000000002', '2.1', 'Plan de sécurité des aliments (HACCP)', 'Analyse des dangers, CCP et plan de sécurité des aliments.', 3),
    ('eb000002-0000-0000-0000-000000000004', 'e2000002-0000-0000-0000-000000000003', '3.4', 'Audits internes', 'Programme d''audits internes couvrant la norme.', 4),
    ('eb000002-0000-0000-0000-000000000005', 'e2000002-0000-0000-0000-000000000003', '3.5', 'Gestion des fournisseurs', 'Approbation et suivi des fournisseurs de matières et services.', 5),
    ('eb000002-0000-0000-0000-000000000006', 'e2000002-0000-0000-0000-000000000003', '3.9', 'Traçabilité', 'Traçabilité amont/aval et tests de traçabilité.', 6),
    ('eb000002-0000-0000-0000-000000000007', 'e2000002-0000-0000-0000-000000000004', '8', 'Zones de production à risque', 'Maîtrise des zones high-risk / high-care.', 7);

INSERT INTO standard_requirements (clause_id, code, text, obligation, evidence_types, measurable_criteria, risk_if_missing, order_index) VALUES
    ('eb000002-0000-0000-0000-000000000001', '1.1.1', 'La direction doit démontrer son engagement par une politique, l''allocation de ressources et une revue de direction périodique.', 'MUST', 'DOCUMENT,AUDIT', 'Politique et engagement documentés ; revue de direction au moins annuelle', 'HIGH', 1),
    ('eb000002-0000-0000-0000-000000000002', '1.2.1', 'Le site doit définir et entretenir un plan de développement de la culture de la sécurité des aliments avec des actions et des indicateurs.', 'MUST', 'DOCUMENT,TRAINING_RECORD', 'Plan de culture sécurité documenté ; indicateurs suivis et actions tracées', 'MEDIUM', 2),
    ('eb000002-0000-0000-0000-000000000003', '2.1.1', 'Le site doit établir et maintenir un plan de sécurité des aliments fondé sur les principes HACCP du Codex Alimentarius.', 'MUST', 'DOCUMENT,AUDIT', 'Plan HACCP couvrant 100 % des étapes ; CCP surveillés et enregistrés', 'CRITICAL', 3),
    ('eb000002-0000-0000-0000-000000000004', '3.4.1', 'Le site doit disposer d''un programme d''audits internes couvrant toutes les exigences de la norme et tracer les non-conformités.', 'MUST', 'AUDIT,DOCUMENT', 'Programme d''audits internes couvrant 100 % de la norme ; écarts suivis', 'HIGH', 4),
    ('eb000002-0000-0000-0000-000000000005', '3.5.1', 'Le site doit approuver et surveiller ses fournisseurs de matières premières et d''emballages sur la base d''une évaluation des risques.', 'MUST', 'DOCUMENT,AUDIT', 'Fournisseurs approuvés sur base de risque ; performances suivies', 'HIGH', 5),
    ('eb000002-0000-0000-0000-000000000006', '3.9.1', 'Le site doit assurer la traçabilité amont/aval et démontrer sa capacité à remonter un lot rapidement.', 'MUST', 'DOCUMENT,KPI_RECORD', 'Test de traçabilité réalisé ; reconstitution d''un lot en un délai cible ≤ 4 heures', 'HIGH', 6),
    ('eb000002-0000-0000-0000-000000000007', '8.1', 'Le site doit identifier et maîtriser les zones de production à risque (high-risk / high-care) afin de prévenir la contamination.', 'MUST', 'DOCUMENT,AUDIT', 'Zones à risque cartographiées ; flux, hygiène et séparation maîtrisés et vérifiés', 'CRITICAL', 7);

INSERT INTO standard_certification_paths (id, standard_id, estimated_duration_months_min, estimated_duration_months_max,
    estimated_cost_eur_min, estimated_cost_eur_max, difficulty_level,
    surveillance_audit_frequency, recertification_cycle_years, notes)
VALUES ('ee000002-0000-0000-0000-000000000002', 'e0000002-0000-0000-0000-000000000002',
    9, 18, 12000, 50000, 4, 'annual', 1,
    'Certification par organisme accrédité (schéma reconnu GFSI). Audit annuel avec notation de grade (AA, A, B, C, D) ; programme d''audits non annoncés proposé en option. Synergie avec les modules Audit, Supplier et CAPA de QualitOS.');

INSERT INTO standard_certification_stages (certification_path_id, stage_number, name, description, typical_duration_weeks, deliverables, actors, qualitos_modules, order_index) VALUES
    ('ee000002-0000-0000-0000-000000000002', 1, 'Cadrage & gap analysis', 'Définition du périmètre et diagnostic d''écarts vs BRCGS Food Issue 9.', '3-6', 'Périmètre, rapport d''écarts', 'Direction, Responsable qualité', 'Audit, Document Control', 1),
    ('ee000002-0000-0000-0000-000000000002', 2, 'Engagement & culture sécurité', 'Engagement de la direction et plan de culture de la sécurité des aliments.', '3-6', 'Politique, plan de culture sécurité', 'Direction, RH', 'Training, Document Control', 2),
    ('ee000002-0000-0000-0000-000000000002', 3, 'Plan HACCP & maîtrise du site', 'Plan de sécurité des aliments (HACCP) et maîtrise des zones de production à risque.', '6-10', 'Plan HACCP, cartographie des zones à risque', 'Équipe sécurité des aliments', 'Document Control, Ishikawa', 3),
    ('ee000002-0000-0000-0000-000000000002', 4, 'Fournisseurs & traçabilité', 'Approbation des fournisseurs et tests de traçabilité amont/aval.', '4-8', 'Fournisseurs approuvés, tests de traçabilité', 'Achats, Responsable qualité', 'Supplier, Document Control', 4),
    ('ee000002-0000-0000-0000-000000000002', 5, 'Audit interne & revue de direction', 'Audit interne couvrant la norme et revue de direction.', '3-5', 'Rapport d''audit interne, compte rendu de revue', 'Auditeur interne, Direction', 'Audit, PDCA', 5),
    ('ee000002-0000-0000-0000-000000000002', 6, 'Audit de certification BRCGS', 'Audit sur site par l''organisme certificateur, attribution du grade et levée des non-conformités.', '3-6', 'Certificat BRCGS Food Issue 9 (grade)', 'Organisme certificateur', 'Standards Hub, CAPA', 6);

-- ============================================================================
-- GLOBALG.A.P. IFA v6 — Assurance intégrée des exploitations agricoles
-- ============================================================================

INSERT INTO standards (id, code, full_name, publisher, current_version, publication_date,
    family, applicable_industries, description, certification_body_required,
    recertification_cycle_months, related_norm_codes, status, created_at, updated_at)
VALUES (
    'e0000003-0000-0000-0000-000000000003',
    'globalgap',
    'GLOBALG.A.P. IFA v6 — Integrated Farm Assurance (assurance intégrée des exploitations)',
    'GLOBALG.A.P.',
    'IFA v6',
    DATE '2022-10-01',
    'SECTORIEL',
    'agro,food',
    'Référentiel d''assurance intégrée des exploitations (Integrated Farm Assurance) couvrant les bonnes pratiques agricoles pour les productions végétales, animales et aquacoles. La version 6 (Smart) renforce la traçabilité à la parcelle, l''usage raisonné des produits phytopharmaceutiques (respect des limites maximales de résidus et des délais avant récolte), la gestion de l''eau d''irrigation, le bien-être animal le cas échéant et la sécurité des denrées. Le volet social GRASP (GLOBALG.A.P. Risk Assessment on Social Practice) est une évaluation complémentaire optionnelle. La certification porte sur l''exploitation, selon un cycle annuel reposant sur des enregistrements terrain probants.',
    TRUE,
    NULL,
    'haccp',
    'PUBLISHED', now(), now()
);

INSERT INTO standard_sections (id, standard_id, code, title, description, order_index) VALUES
    ('e3000003-0000-0000-0000-000000000001', 'e0000003-0000-0000-0000-000000000003', 'AF', 'Base exploitation (All Farm Base)', 'Traçabilité, enregistrements et gestion générale de l''exploitation.', 1),
    ('e3000003-0000-0000-0000-000000000002', 'e0000003-0000-0000-0000-000000000003', 'CB', 'Base productions végétales (Crops Base)', 'Phytosanitaires, eau d''irrigation et gestion des parcelles.', 2),
    ('e3000003-0000-0000-0000-000000000003', 'e0000003-0000-0000-0000-000000000003', 'LB', 'Base élevage (Livestock Base)', 'Bien-être animal et traçabilité des animaux le cas échéant.', 3),
    ('e3000003-0000-0000-0000-000000000004', 'e0000003-0000-0000-0000-000000000003', 'GRASP', 'Volet social (GRASP)', 'Évaluation complémentaire optionnelle des pratiques sociales.', 4);

INSERT INTO standard_clauses (id, section_id, code, title, description, order_index) VALUES
    ('ec000003-0000-0000-0000-000000000001', 'e3000003-0000-0000-0000-000000000001', 'AF.1', 'Traçabilité parcelle', 'Traçabilité du produit certifié jusqu''à la parcelle d''origine.', 1),
    ('ec000003-0000-0000-0000-000000000002', 'e3000003-0000-0000-0000-000000000001', 'AF.2', 'Enregistrements terrain', 'Tenue et conservation des enregistrements des activités.', 2),
    ('ec000003-0000-0000-0000-000000000003', 'e3000003-0000-0000-0000-000000000002', 'CB.7', 'Usage des phytosanitaires', 'Application raisonnée, LMR et délais avant récolte.', 3),
    ('ec000003-0000-0000-0000-000000000004', 'e3000003-0000-0000-0000-000000000002', 'CB.5', 'Eau d''irrigation', 'Gestion et qualité de l''eau d''irrigation.', 4),
    ('ec000003-0000-0000-0000-000000000005', 'e3000003-0000-0000-0000-000000000003', 'LB.1', 'Bien-être animal', 'Conditions d''élevage et bien-être animal le cas échéant.', 5),
    ('ec000003-0000-0000-0000-000000000006', 'e3000003-0000-0000-0000-000000000004', 'GR.1', 'Pratiques sociales (GRASP)', 'Évaluation des risques sur les pratiques sociales (optionnel).', 6);

INSERT INTO standard_requirements (clause_id, code, text, obligation, evidence_types, measurable_criteria, risk_if_missing, order_index) VALUES
    ('ec000003-0000-0000-0000-000000000001', 'AF.1.1', 'L''exploitation doit assurer la traçabilité du produit certifié, permettant de remonter à la parcelle d''origine.', 'MUST', 'DOCUMENT,KPI_RECORD', 'Traçabilité produit → parcelle démontrée ; test de traçabilité réalisé', 'CRITICAL', 1),
    ('ec000003-0000-0000-0000-000000000002', 'AF.2.1', 'L''exploitation doit tenir et conserver des enregistrements terrain probants des activités sur la durée requise.', 'MUST', 'DOCUMENT', 'Enregistrements terrain disponibles et conservés selon la durée définie', 'HIGH', 2),
    ('ec000003-0000-0000-0000-000000000003', 'CB.7.1', 'L''exploitation doit appliquer les produits phytopharmaceutiques de façon raisonnée, en respectant les limites maximales de résidus (LMR) et les délais avant récolte (DAR).', 'MUST', 'DOCUMENT,KPI_RECORD', 'Registre phytosanitaire complet ; LMR et délais avant récolte respectés (0 dépassement)', 'CRITICAL', 3),
    ('ec000003-0000-0000-0000-000000000004', 'CB.5.1', 'L''exploitation doit gérer l''eau d''irrigation en évaluant sa qualité et son adéquation à l''usage.', 'MUST', 'DOCUMENT,KPI_RECORD', 'Analyse de l''eau d''irrigation réalisée à fréquence définie ; résultats conformes', 'HIGH', 4),
    ('ec000003-0000-0000-0000-000000000005', 'LB.1.1', 'Lorsque la production animale est concernée, l''exploitation doit garantir le bien-être des animaux et la traçabilité associée.', 'SHOULD', 'DOCUMENT,AUDIT', 'Conditions de bien-être animal documentées et vérifiées le cas échéant', 'MEDIUM', 5),
    ('ec000003-0000-0000-0000-000000000006', 'GR.1.1', 'L''exploitation peut réaliser l''évaluation GRASP des pratiques sociales en complément de la certification IFA.', 'MAY', 'DOCUMENT,AUDIT', 'Évaluation GRASP réalisée et écarts traités (module optionnel)', 'LOW', 6);

INSERT INTO standard_certification_paths (id, standard_id, estimated_duration_months_min, estimated_duration_months_max,
    estimated_cost_eur_min, estimated_cost_eur_max, difficulty_level,
    surveillance_audit_frequency, recertification_cycle_years, notes)
VALUES ('ee000003-0000-0000-0000-000000000003', 'e0000003-0000-0000-0000-000000000003',
    4, 12, 3000, 20000, 3, 'annual', 1,
    'Certification de l''exploitation par un organisme de certification approuvé GLOBALG.A.P., selon un cycle annuel. La traçabilité à la parcelle et les enregistrements terrain probants sont déterminants. Le volet social GRASP est une évaluation complémentaire optionnelle. Synergie avec les modules IoT (capteurs sol, station météo, irrigation), Audit et Document Control de QualitOS.');

INSERT INTO standard_certification_stages (certification_path_id, stage_number, name, description, typical_duration_weeks, deliverables, actors, qualitos_modules, order_index) VALUES
    ('ee000003-0000-0000-0000-000000000003', 1, 'Cadrage & périmètre de production', 'Définition du périmètre (parcelles, produits, productions concernées) et inscription GLOBALG.A.P.', '2-4', 'Périmètre de production, numéro GGN', 'Responsable d''exploitation', 'Document Control', 1),
    ('ee000003-0000-0000-0000-000000000003', 2, 'Mise en place de la traçabilité', 'Traçabilité produit → parcelle et système d''enregistrements terrain.', '3-6', 'Système de traçabilité, enregistrements terrain', 'Responsable d''exploitation', 'Document Control, IoT', 2),
    ('ee000003-0000-0000-0000-000000000003', 3, 'Phytosanitaires & eau', 'Usage raisonné des phytosanitaires (LMR, DAR) et gestion de l''eau d''irrigation.', '4-8', 'Registre phytosanitaire, analyses d''eau', 'Technicien cultures', 'IoT, Document Control', 3),
    ('ee000003-0000-0000-0000-000000000003', 4, 'Bien-être animal & GRASP (le cas échéant)', 'Bien-être animal pour les productions animales et évaluation sociale GRASP optionnelle.', '2-6', 'Dossier bien-être animal, évaluation GRASP', 'Responsable d''exploitation, RH', 'Document Control, Audit', 4),
    ('ee000003-0000-0000-0000-000000000003', 5, 'Auto-évaluation & inspection interne', 'Auto-évaluation selon la liste de points de contrôle et inspection interne.', '2-4', 'Auto-évaluation, rapport d''inspection interne', 'Responsable d''exploitation', 'Audit, PDCA', 5),
    ('ee000003-0000-0000-0000-000000000003', 6, 'Audit de certification GLOBALG.A.P.', 'Audit sur site par l''organisme de certification et levée des non-conformités.', '2-4', 'Certificat GLOBALG.A.P. IFA v6', 'Organisme de certification', 'Standards Hub, CAPA', 6);

-- ============================================================================
-- VDA 6.3 (2023) — Audit de processus automobile
-- ============================================================================

INSERT INTO standards (id, code, full_name, publisher, current_version, publication_date,
    family, applicable_industries, description, certification_body_required,
    recertification_cycle_months, related_norm_codes, status, created_at, updated_at)
VALUES (
    'e0000004-0000-0000-0000-000000000004',
    'vda-6-3',
    'VDA 6.3 — Audit de processus (automobile)',
    'VDA QMC',
    '2023',
    DATE '2023-01-01',
    'SECTORIEL',
    'automotive,manufacturing',
    'Méthode d''audit de processus de l''industrie automobile allemande (VDA), utilisée pour évaluer la maîtrise des processus en interne et chez les fournisseurs et pour qualifier les nouveaux projets. Elle ne donne pas lieu à un certificat délivré par un organisme tiers : la reconnaissance repose sur la qualification des auditeurs VDA 6.3 et sur la réalisation d''audits internes et fournisseurs. L''audit s''appuie sur un questionnaire standardisé couvrant les éléments P2 à P7 (analyse de la gestion de projet, développement du produit et du processus, fournisseurs, production, service client). Le résultat est exprimé par un degré de conformité avec une notation A/B/C (un taux global ≥ 90 % correspond généralement au classement A), assortie de règles de déclassement en cas de questions critiques.',
    FALSE,
    NULL,
    'iatf-16949',
    'PUBLISHED', now(), now()
);

INSERT INTO standard_sections (id, standard_id, code, title, description, order_index) VALUES
    ('e4000004-0000-0000-0000-000000000001', 'e0000004-0000-0000-0000-000000000004', 'P2', 'Gestion de projet', 'Analyse de la gestion de projet (potentiel fournisseur, planification).', 1),
    ('e4000004-0000-0000-0000-000000000002', 'e0000004-0000-0000-0000-000000000004', 'P3-P4', 'Développement produit & process', 'Développement du produit et du processus et leur mise en œuvre.', 2),
    ('e4000004-0000-0000-0000-000000000003', 'e0000004-0000-0000-0000-000000000004', 'P5-P6', 'Fournisseurs & production', 'Gestion des fournisseurs et analyse du processus de production.', 3),
    ('e4000004-0000-0000-0000-000000000004', 'e0000004-0000-0000-0000-000000000004', 'P7', 'Service client & évaluation', 'Service client/satisfaction et règles d''évaluation A/B/C.', 4);

INSERT INTO standard_clauses (id, section_id, code, title, description, order_index) VALUES
    ('ed000004-0000-0000-0000-000000000001', 'e4000004-0000-0000-0000-000000000001', 'P2', 'Analyse de la gestion de projet', 'Évaluation du potentiel et de la planification du projet.', 1),
    ('ed000004-0000-0000-0000-000000000002', 'e4000004-0000-0000-0000-000000000002', 'P3', 'Développement du produit', 'Maîtrise du développement du produit (jalons, ressources).', 2),
    ('ed000004-0000-0000-0000-000000000003', 'e4000004-0000-0000-0000-000000000002', 'P4', 'Développement du processus', 'Maîtrise du développement du processus de fabrication.', 3),
    ('ed000004-0000-0000-0000-000000000004', 'e4000004-0000-0000-0000-000000000003', 'P5', 'Gestion des fournisseurs', 'Maîtrise et développement des fournisseurs.', 4),
    ('ed000004-0000-0000-0000-000000000005', 'e4000004-0000-0000-0000-000000000003', 'P6', 'Analyse du processus de production', 'Analyse de la maîtrise du processus de production.', 5),
    ('ed000004-0000-0000-0000-000000000006', 'e4000004-0000-0000-0000-000000000004', 'P7', 'Service client & satisfaction', 'Service client, satisfaction et traitement des réclamations.', 6),
    ('ed000004-0000-0000-0000-000000000007', 'e4000004-0000-0000-0000-000000000004', 'EVAL', 'Notation & qualification auditeur', 'Règles de notation A/B/C et qualification de l''auditeur.', 7);

INSERT INTO standard_requirements (clause_id, code, text, obligation, evidence_types, measurable_criteria, risk_if_missing, order_index) VALUES
    ('ed000004-0000-0000-0000-000000000001', 'P2.1', 'L''organisation doit évaluer la gestion de projet (potentiel, planification, ressources) à l''aide du questionnaire VDA 6.3.', 'MUST', 'AUDIT,DOCUMENT', 'Élément P2 audité ; questions évaluées et écarts documentés', 'HIGH', 1),
    ('ed000004-0000-0000-0000-000000000002', 'P3.1', 'L''organisation doit maîtriser le développement du produit (jalons, validations, ressources) conformément à P3.', 'MUST', 'AUDIT,DOCUMENT', 'Élément P3 audité ; jalons de développement produit tracés', 'HIGH', 2),
    ('ed000004-0000-0000-0000-000000000003', 'P4.1', 'L''organisation doit maîtriser le développement du processus de fabrication conformément à P4.', 'MUST', 'AUDIT,DOCUMENT', 'Élément P4 audité ; plans de surveillance et validations process en place', 'HIGH', 3),
    ('ed000004-0000-0000-0000-000000000004', 'P5.1', 'L''organisation doit maîtriser et développer ses fournisseurs conformément à P5.', 'MUST', 'AUDIT,DOCUMENT', 'Élément P5 audité ; fournisseurs évalués et plans de développement suivis', 'HIGH', 4),
    ('ed000004-0000-0000-0000-000000000005', 'P6.1', 'L''organisation doit démontrer la maîtrise de son processus de production (matériel, personnel, efficacité) conformément à P6.', 'MUST', 'AUDIT,KPI_RECORD', 'Élément P6 audité ; indicateurs process (capabilité, rebuts) suivis', 'CRITICAL', 5),
    ('ed000004-0000-0000-0000-000000000006', 'P7.1', 'L''organisation doit maîtriser le service client, la satisfaction et le traitement des réclamations conformément à P7.', 'MUST', 'AUDIT,CAPA', 'Élément P7 audité ; réclamations clients traitées et délais suivis', 'HIGH', 6),
    ('ed000004-0000-0000-0000-000000000007', 'EVAL.1', 'L''audit de processus doit être réalisé par un auditeur qualifié VDA 6.3 et donner lieu à une notation A/B/C avec plan d''action sur les écarts.', 'MUST', 'AUDIT,CAPA', 'Auditeur qualifié VDA 6.3 ; degré de conformité calculé (A si ≥ 90 %) ; écarts traités', 'HIGH', 7);

INSERT INTO standard_certification_paths (id, standard_id, estimated_duration_months_min, estimated_duration_months_max,
    estimated_cost_eur_min, estimated_cost_eur_max, difficulty_level,
    surveillance_audit_frequency, recertification_cycle_years, notes)
VALUES ('ee000004-0000-0000-0000-000000000004', 'e0000004-0000-0000-0000-000000000004',
    2, 6, 3000, 25000, 4, 'none', NULL,
    'VDA 6.3 n''est pas un certificat délivré par un organisme tiers : la reconnaissance repose sur la qualification des auditeurs (formation VDA QMC) et sur la réalisation d''audits de processus internes et fournisseurs. Notation A/B/C (≥ 90 % = A) avec règles de déclassement sur questions critiques. Complémentaire à la certification IATF 16949. Synergie avec les modules Audit, Supplier et CAPA de QualitOS.');

INSERT INTO standard_certification_stages (certification_path_id, stage_number, name, description, typical_duration_weeks, deliverables, actors, qualitos_modules, order_index) VALUES
    ('ee000004-0000-0000-0000-000000000004', 1, 'Qualification des auditeurs', 'Formation et qualification des auditeurs internes selon VDA 6.3 (VDA QMC).', '2-6', 'Certificats d''auditeur VDA 6.3', 'Responsable qualité, Auditeurs', 'Training', 1),
    ('ee000004-0000-0000-0000-000000000004', 2, 'Préparation de l''audit (questionnaire)', 'Sélection du périmètre (P2-P7), préparation du questionnaire et planification.', '1-3', 'Plan d''audit, questionnaire VDA 6.3', 'Auditeur qualifié', 'Audit', 2),
    ('ee000004-0000-0000-0000-000000000004', 3, 'Audit de processus interne', 'Réalisation de l''audit des éléments P2 à P7 et notation A/B/C.', '1-2', 'Rapport d''audit, degré de conformité', 'Auditeur qualifié', 'Audit', 3),
    ('ee000004-0000-0000-0000-000000000004', 4, 'Audit de processus fournisseur', 'Audit des fournisseurs critiques selon la même méthode.', '2-6', 'Rapports d''audit fournisseurs', 'Auditeur qualifié, Achats', 'Supplier, Audit', 4),
    ('ee000004-0000-0000-0000-000000000004', 5, 'Plan d''action & suivi des écarts', 'Définition et suivi des actions correctives sur les écarts identifiés.', '4-12', 'Plan d''action, preuves de levée des écarts', 'Pilotes processus', 'CAPA, PDCA', 5),
    ('ee000004-0000-0000-0000-000000000004', 6, 'Revue & amélioration continue', 'Revue des résultats d''audit et amélioration continue des processus.', '2-4', 'Compte rendu de revue, plan d''amélioration', 'Responsable qualité, Direction', 'PDCA, Audit', 6);

-- ============================================================================
-- ISO 19650-1/-2:2018 — Gestion de l'information BIM (BTP)
-- ============================================================================

INSERT INTO standards (id, code, full_name, publisher, current_version, publication_date,
    family, applicable_industries, description, certification_body_required,
    recertification_cycle_months, related_norm_codes, status, created_at, updated_at)
VALUES (
    'e0000005-0000-0000-0000-000000000005',
    'iso-19650',
    'ISO 19650-1/-2:2018 — Organisation et numérisation des informations relatives aux bâtiments et ouvrages de génie civil (BIM)',
    'ISO',
    '2018',
    DATE '2018-12-01',
    'SECTORIEL',
    'construction,bim',
    'Cadre de gestion de l''information tout au long du cycle de vie des actifs bâtis utilisant le BIM (Building Information Modelling). La partie 1 définit les concepts et principes ; la partie 2 spécifie le processus de livraison des informations en phase de réalisation des actifs. Le cadre repose sur la hiérarchie des exigences d''information (OIR — organizational information requirements, AIR — asset information requirements, PIR — project information requirements, EIR — exchange information requirements), l''environnement commun de données (CDE — common data environment), le plan d''exécution BIM (BEP — BIM execution plan), des conventions de nommage, des jalons d''échange d''informations et la sécurité de l''information (ISO 19650-5). La certification des organisations est possible (par exemple via des schémas reconnus UKAS / Kitemark), même si elle n''est pas universellement requise.',
    TRUE,
    NULL,
    'iso-9001,iso-27001',
    'PUBLISHED', now(), now()
);

INSERT INTO standard_sections (id, standard_id, code, title, description, order_index) VALUES
    ('e5000005-0000-0000-0000-000000000001', 'e0000005-0000-0000-0000-000000000005', 'IR', 'Exigences d''information', 'Hiérarchie OIR / AIR / PIR / EIR.', 1),
    ('e5000005-0000-0000-0000-000000000002', 'e0000005-0000-0000-0000-000000000005', 'CDE', 'Environnement commun de données', 'Mise en place et exploitation du CDE.', 2),
    ('e5000005-0000-0000-0000-000000000003', 'e0000005-0000-0000-0000-000000000005', 'BEP', 'Plan d''exécution BIM & échanges', 'Plan d''exécution BIM, conventions de nommage et jalons d''échange.', 3),
    ('e5000005-0000-0000-0000-000000000004', 'e0000005-0000-0000-0000-000000000005', 'SEC', 'Sécurité de l''information', 'Sécurité de l''information du BIM (ISO 19650-5).', 4);

INSERT INTO standard_clauses (id, section_id, code, title, description, order_index) VALUES
    ('e9000005-0000-0000-0000-000000000001', 'e5000005-0000-0000-0000-000000000001', 'IR.1', 'Exigences d''information (OIR/AIR/EIR)', 'Définition des exigences d''information à chaque niveau.', 1),
    ('e9000005-0000-0000-0000-000000000002', 'e5000005-0000-0000-0000-000000000002', 'CDE.1', 'Environnement commun de données', 'Mise en place et gouvernance du CDE.', 2),
    ('e9000005-0000-0000-0000-000000000003', 'e5000005-0000-0000-0000-000000000003', 'BEP.1', 'Plan d''exécution BIM (BEP)', 'Élaboration et tenue du plan d''exécution BIM.', 3),
    ('e9000005-0000-0000-0000-000000000004', 'e5000005-0000-0000-0000-000000000003', 'NAM.1', 'Conventions de nommage', 'Convention de nommage des conteneurs d''information.', 4),
    ('e9000005-0000-0000-0000-000000000005', 'e5000005-0000-0000-0000-000000000003', 'EXC.1', 'Jalons d''échange d''informations', 'Planification et respect des jalons d''échange (information delivery).', 5),
    ('e9000005-0000-0000-0000-000000000006', 'e5000005-0000-0000-0000-000000000004', 'SEC.1', 'Sécurité de l''information', 'Gestion de la sécurité de l''information selon ISO 19650-5.', 6);

INSERT INTO standard_requirements (clause_id, code, text, obligation, evidence_types, measurable_criteria, risk_if_missing, order_index) VALUES
    ('e9000005-0000-0000-0000-000000000001', 'IR.1.1', 'L''organisation doit définir et documenter ses exigences d''information (OIR, AIR, PIR, EIR) à chaque niveau pertinent.', 'MUST', 'DOCUMENT,AUDIT', 'Exigences d''information (OIR/AIR/PIR/EIR) documentées et tenues à jour', 'HIGH', 1),
    ('e9000005-0000-0000-0000-000000000002', 'CDE.1.1', 'Le projet doit mettre en place et exploiter un environnement commun de données (CDE) gérant les états et la traçabilité des conteneurs d''information.', 'MUST', 'DOCUMENT,AUDIT', 'CDE opérationnel ; états (WIP/Shared/Published/Archived) et versions tracés', 'CRITICAL', 2),
    ('e9000005-0000-0000-0000-000000000003', 'BEP.1.1', 'L''équipe de livraison doit élaborer et tenir à jour un plan d''exécution BIM (BEP) couvrant les rôles, responsabilités et méthodes.', 'MUST', 'DOCUMENT', 'BEP approuvé, à jour et couvrant rôles, responsabilités et méthodes', 'HIGH', 3),
    ('e9000005-0000-0000-0000-000000000004', 'NAM.1.1', 'Les conteneurs d''information doivent respecter une convention de nommage cohérente et documentée.', 'MUST', 'DOCUMENT,AUDIT', 'Convention de nommage documentée ; conformité des conteneurs vérifiée (échantillon)', 'MEDIUM', 4),
    ('e9000005-0000-0000-0000-000000000005', 'EXC.1.1', 'Le projet doit planifier des jalons d''échange d''informations et vérifier la livraison des informations attendues à chaque jalon.', 'MUST', 'DOCUMENT,KPI_RECORD', 'Jalons d''échange planifiés ; taux de livraison conforme aux jalons suivi', 'HIGH', 5),
    ('e9000005-0000-0000-0000-000000000006', 'SEC.1.1', 'L''organisation doit gérer la sécurité de l''information du BIM conformément à ISO 19650-5, par une évaluation de sensibilité et des mesures adaptées.', 'MUST', 'DOCUMENT,AUDIT', 'Évaluation de sensibilité réalisée ; mesures de sécurité de l''information en place', 'HIGH', 6);

INSERT INTO standard_certification_paths (id, standard_id, estimated_duration_months_min, estimated_duration_months_max,
    estimated_cost_eur_min, estimated_cost_eur_max, difficulty_level,
    surveillance_audit_frequency, recertification_cycle_years, notes)
VALUES ('ee000005-0000-0000-0000-000000000005', 'e0000005-0000-0000-0000-000000000005',
    6, 15, 10000, 60000, 4, 'annual', 3,
    'La certification des organisations est possible (par exemple via des schémas reconnus UKAS / Kitemark), bien qu''elle ne soit pas universellement requise. Le cadre repose sur les exigences d''information (OIR/AIR/EIR), le CDE, le BEP, les conventions de nommage, les jalons d''échange et la sécurité de l''information (ISO 19650-5). Synergie avec les modules Document Control (CDE, BEP, conventions) et Audit de QualitOS.');

INSERT INTO standard_certification_stages (certification_path_id, stage_number, name, description, typical_duration_weeks, deliverables, actors, qualitos_modules, order_index) VALUES
    ('ee000005-0000-0000-0000-000000000005', 1, 'Cadrage & exigences d''information', 'Définition des exigences d''information (OIR, AIR, PIR, EIR) et du périmètre BIM.', '3-6', 'Exigences d''information documentées (OIR/AIR/PIR/EIR)', 'Maître d''ouvrage, BIM Manager', 'Document Control', 1),
    ('ee000005-0000-0000-0000-000000000005', 2, 'Mise en place du CDE', 'Sélection et configuration de l''environnement commun de données (états, droits, traçabilité).', '4-8', 'CDE configuré, procédure de gestion des conteneurs', 'BIM Manager, IT', 'Document Control', 2),
    ('ee000005-0000-0000-0000-000000000005', 3, 'Plan d''exécution BIM (BEP) & nommage', 'Élaboration du BEP et des conventions de nommage des conteneurs d''information.', '3-6', 'BEP approuvé, convention de nommage', 'Équipe de livraison', 'Document Control', 3),
    ('ee000005-0000-0000-0000-000000000005', 4, 'Jalons d''échange & sécurité', 'Planification des jalons d''échange et mise en œuvre de la sécurité de l''information (ISO 19650-5).', '4-8', 'Plan des jalons d''échange, plan de sécurité de l''information', 'BIM Manager, RSSI', 'Document Control, Risk', 4),
    ('ee000005-0000-0000-0000-000000000005', 5, 'Audit interne & revue', 'Audit interne du processus de gestion de l''information et revue de direction.', '3-5', 'Rapport d''audit interne, compte rendu de revue', 'Auditeur interne, Direction', 'Audit, PDCA', 5),
    ('ee000005-0000-0000-0000-000000000005', 6, 'Certification de l''organisation', 'Audit de certification par l''organisme (schéma reconnu) et levée des écarts.', '4-8', 'Certificat ISO 19650', 'Organisme certificateur', 'Standards Hub, CAPA', 6);
