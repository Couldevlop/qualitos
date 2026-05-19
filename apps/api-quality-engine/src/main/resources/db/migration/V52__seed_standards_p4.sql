-- P4 — Standards Hub extension (CLAUDE.md §8.3, §8.5, §17 P4).
-- 6 nouvelles normes : IATF 16949 (auto), AS9100D (aéro), ISO 13485 (DM),
-- FDA 21 CFR Part 11 (signatures), ISO 22000 (agro), DORA (finance).
-- Chaque norme : ≥ 15 clauses réelles + exigences + roadmap certification (§8.5).

-- ============================================================================
-- Création de la table certification_path (§8.5) si absente.
-- ============================================================================

CREATE TABLE IF NOT EXISTS standard_certification_paths (
    id                              UUID         NOT NULL DEFAULT gen_random_uuid(),
    standard_id                     UUID         NOT NULL,
    estimated_duration_months_min   INTEGER,
    estimated_duration_months_max   INTEGER,
    estimated_cost_eur_min          INTEGER,
    estimated_cost_eur_max          INTEGER,
    difficulty_level                INTEGER,
    surveillance_audit_frequency    VARCHAR(30),
    recertification_cycle_years     INTEGER,
    notes                           TEXT,
    created_at                      TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT pk_standard_certification_paths PRIMARY KEY (id),
    CONSTRAINT fk_scp_standard FOREIGN KEY (standard_id)
        REFERENCES standards (id) ON DELETE CASCADE,
    CONSTRAINT uk_scp_standard UNIQUE (standard_id),
    CONSTRAINT chk_scp_difficulty CHECK (difficulty_level IS NULL OR difficulty_level BETWEEN 1 AND 5)
);

CREATE INDEX IF NOT EXISTS idx_scp_standard ON standard_certification_paths (standard_id);

CREATE TABLE IF NOT EXISTS standard_certification_stages (
    id                       UUID         NOT NULL DEFAULT gen_random_uuid(),
    certification_path_id    UUID         NOT NULL,
    stage_number             INTEGER      NOT NULL,
    name                     VARCHAR(255) NOT NULL,
    description              TEXT,
    typical_duration_weeks   VARCHAR(30),
    deliverables             TEXT,
    actors                   TEXT,
    qualitos_modules         TEXT,
    order_index              INTEGER      NOT NULL,

    CONSTRAINT pk_standard_certification_stages PRIMARY KEY (id),
    CONSTRAINT fk_scs_path FOREIGN KEY (certification_path_id)
        REFERENCES standard_certification_paths (id) ON DELETE CASCADE,
    CONSTRAINT uk_scs_path_stage UNIQUE (certification_path_id, stage_number)
);

CREATE INDEX IF NOT EXISTS idx_scs_path ON standard_certification_stages (certification_path_id);

-- Table pour les modèles de documents normatifs (§8.3 documents_required).
CREATE TABLE IF NOT EXISTS standard_document_templates (
    id              UUID         NOT NULL DEFAULT gen_random_uuid(),
    standard_id     UUID         NOT NULL,
    code            VARCHAR(50)  NOT NULL,
    name            VARCHAR(500) NOT NULL,
    obligation      VARCHAR(20)  NOT NULL DEFAULT 'RECOMMENDED',
    template_uri    VARCHAR(1024) NOT NULL,
    description     TEXT,
    fingerprint_sha256 VARCHAR(64),

    CONSTRAINT pk_standard_document_templates PRIMARY KEY (id),
    CONSTRAINT fk_sdt_standard FOREIGN KEY (standard_id)
        REFERENCES standards (id) ON DELETE CASCADE,
    CONSTRAINT uk_sdt_standard_code UNIQUE (standard_id, code),
    CONSTRAINT chk_sdt_obligation CHECK (obligation IN ('MANDATORY','RECOMMENDED','OPTIONAL'))
);

CREATE INDEX IF NOT EXISTS idx_sdt_standard ON standard_document_templates (standard_id);

-- ============================================================================
-- IATF 16949:2016 — Automobile (BIQS, OEM CSR, APQP, PPAP, MSA, SPC).
-- ============================================================================

INSERT INTO standards (id, code, full_name, publisher, current_version, publication_date,
    family, applicable_industries, description, certification_body_required,
    recertification_cycle_months, related_norm_codes, status, created_at, updated_at)
VALUES (
    '50000001-0000-0000-0000-000000000001',
    'iatf-16949',
    'IATF 16949:2016 — Exigences pour les SMQ des organismes de production et de pièces de rechange automobiles',
    'IATF',
    '2016',
    '2016-10-01',
    'AUTOMOTIVE',
    'automotive,manufacturing',
    'Standard automobile mondial complémentaire à ISO 9001:2015 pour les fournisseurs de l''industrie automobile. Inclut exigences spécifiques OEM (CSR — Customer Specific Requirements), APQP, PPAP, FMEA, MSA, SPC.',
    TRUE,
    36,
    'iso-9001,vda-6-3,as9100',
    'PUBLISHED',
    now(), now()
);

INSERT INTO standard_sections (id, standard_id, code, title, description, order_index) VALUES
    ('51010001-0000-0000-0000-000000000001', '50000001-0000-0000-0000-000000000001', '4', 'Contexte de l''organisme', 'Compréhension de l''organisme et exigences spécifiques clients (CSR).', 4),
    ('51010002-0000-0000-0000-000000000001', '50000001-0000-0000-0000-000000000001', '5', 'Leadership', 'Engagement direction, politique qualité automobile, responsabilité sociale.', 5),
    ('51010003-0000-0000-0000-000000000001', '50000001-0000-0000-0000-000000000001', '6', 'Planification', 'Risques, opportunités, objectifs, planification des changements et plans de contingence.', 6),
    ('51010004-0000-0000-0000-000000000001', '50000001-0000-0000-0000-000000000001', '7', 'Support', 'Ressources, compétences, équipements de surveillance (MSA), informations documentées.', 7),
    ('51010005-0000-0000-0000-000000000001', '50000001-0000-0000-0000-000000000001', '8', 'Réalisation opérationnelle', 'APQP, PPAP, conception, production, contrôle changements, traçabilité.', 8),
    ('51010006-0000-0000-0000-000000000001', '50000001-0000-0000-0000-000000000001', '9', 'Évaluation des performances', 'Audits internes, audits processus, audits produit, revue de direction.', 9),
    ('51010007-0000-0000-0000-000000000001', '50000001-0000-0000-0000-000000000001', '10', 'Amélioration', 'Non-conformités, actions correctives (8D), amélioration continue.', 10);

INSERT INTO standard_clauses (id, section_id, code, title, description, order_index) VALUES
    ('51020001-0000-0000-0000-000000000001', '51010001-0000-0000-0000-000000000001', '4.3.2', 'Exigences spécifiques des clients (CSR)
    ('51020001-0000-0000-0000-000000000002', '51010001-0000-0000-0000-000000000001', '4.4.1.2', 'Sécurité produit', 'Identification des caractéristiques de sécurité et de réglementation produit.', 2),
    ('51020002-0000-0000-0000-000000000001', '51010002-0000-0000-0000-000000000001', '5.1.1.1', 'Responsabilité d''entreprise', 'Politique anticorruption, code de conduite éthique, escalade éthique.', 1),
    ('51020003-0000-0000-0000-000000000001', '51010003-0000-0000-0000-000000000001', '6.1.2.3', 'Plans de contingence', 'Plans de continuité production en cas de panne, catastrophe, indisponibilité fournisseur critique.', 1),
    ('51020004-0000-0000-0000-000000000001', '51010004-0000-0000-0000-000000000001', '7.1.5.1.1', 'Analyse système de mesure (MSA)
    ('51020004-0000-0000-0000-000000000002', '51010004-0000-0000-0000-000000000001', '7.1.5.2.1', 'Étalonnage / registres de vérification', 'Traçabilité des étalonnages, certificats reliés à étalons nationaux.', 2),
    ('51020004-0000-0000-0000-000000000003', '51010004-0000-0000-0000-000000000001', '7.2.1', 'Compétences — supplémentaire', 'Programme de compétences pour personnel affecté à activités impactant qualité, sécurité, environnement.', 3),
    ('51020005-0000-0000-0000-000000000001', '51010005-0000-0000-0000-000000000001', '8.3.2.1', 'Planification de la conception et du développement — APQP', 'Application de la méthode APQP (Advanced Product Quality Planning) 
    ('51020005-0000-0000-0000-000000000002', '51010005-0000-0000-0000-000000000001', '8.3.4.4', 'Processus de validation produit — PPAP', 'Soumission PPAP (Production Part Approval Process) 
    ('51020005-0000-0000-0000-000000000003', '51010005-0000-0000-0000-000000000001', '8.3.5.2', 'Caractéristiques spéciales', 'Identification, documentation et maîtrise des caractéristiques spéciales (sécurité, critiques, significatives)
    ('51020005-0000-0000-0000-000000000004', '51010005-0000-0000-0000-000000000001', '8.4.2.3.1', 'Logiciel — gestion qualité fournisseur (CARA logiciel embarqué)
    ('51020005-0000-0000-0000-000000000005', '51010005-0000-0000-0000-000000000001', '8.5.1.1', 'Plan de contrôle', 'Plans de contrôle par étape de processus (prototype, pré-série, production)
    ('51020005-0000-0000-0000-000000000006', '51010005-0000-0000-0000-000000000001', '8.5.6.1.1', 'Maîtrise des changements de processus de production temporaire', 'Documentation et autorisation explicite avant changement temporaire.', 6),
    ('51020006-0000-0000-0000-000000000001', '51010006-0000-0000-0000-000000000001', '9.2.2.3', 'Audit processus de fabrication', 'Audit de chaque processus de production avec fréquence définie selon performance et risque.', 1),
    ('51020006-0000-0000-0000-000000000002', '51010006-0000-0000-0000-000000000001', '9.2.2.4', 'Audit produit', 'Audit produit à fréquence définie pour vérifier conformité aux spécifications.', 2),
    ('51020007-0000-0000-0000-000000000001', '51010007-0000-0000-0000-000000000001', '10.2.3', 'Résolution de problèmes', 'Application méthode 8D (8 Disciplines) 
    ('51020007-0000-0000-0000-000000000002', '51010007-0000-0000-0000-000000000001', '10.2.4', 'Détrompage (Poka-Yoke)
;

INSERT INTO standard_requirements (clause_id, code, text, obligation, evidence_types, measurable_criteria, risk_if_missing, order_index) VALUES
    ('51020001-0000-0000-0000-000000000001', '4.3.2', 'L''organisme doit évaluer et intégrer les exigences spécifiques des clients dans son SMQ.', 'MUST', 'Matrice CSR par client OEM, plans d''action', 'Toutes CSR identifiées et statut couverture ≥ 95%', 'CRITICAL', 1),
    ('51020001-0000-0000-0000-000000000002', '4.4.1.2', 'L''organisme doit avoir un processus documenté pour la sécurité produit.', 'MUST', 'Procédure sécurité produit, registre des caractéristiques sécurité', 'Caractéristiques sécurité ≥ 100% identifiées et maîtrisées', 'CRITICAL', 1),
    ('51020002-0000-0000-0000-000000000001', '5.1.1.1', 'La direction doit définir et appliquer des politiques de responsabilité d''entreprise.', 'MUST', 'Politique anticorruption, code éthique', 'Politique signée, formation annuelle 100% personnel', 'HIGH', 1),
    ('51020003-0000-0000-0000-000000000001', '6.1.2.3', 'L''organisme doit établir des plans de contingence pour satisfaire les exigences clients lors d''interruptions.', 'MUST', 'Plans de contingence documentés, tests', 'Plan révisé ≥ 1/an, test exécuté ≥ 1/an', 'CRITICAL', 1),
    ('51020004-0000-0000-0000-000000000001', '7.1.5.1.1', 'L''organisme doit conduire des études statistiques d''analyse du système de mesure (MSA).', 'MUST', 'Études R&R, rapports MSA', 'Études MSA ≥ 1/an pour équipements critiques, %GRR < 10%', 'CRITICAL', 1),
    ('51020005-0000-0000-0000-000000000001', '8.3.2.1', 'L''organisme doit appliquer une approche multidisciplinaire de planification qualité (APQP) lors de la conception.', 'MUST', 'Plans APQP, jalons P0-P5', 'Jalons P0-P5 documentés avec livrables', 'CRITICAL', 1),
    ('51020005-0000-0000-0000-000000000002', '8.3.4.4', 'L''organisme doit soumettre les pièces neuves ou modifiées à la procédure PPAP avant production série.', 'MUST', 'Dossiers PPAP signés client', 'PPAP approuvé avant lancement série, taux de rejet < 5%', 'CRITICAL', 1),
    ('51020005-0000-0000-0000-000000000005', '8.5.1.1', 'L''organisme doit développer un plan de contrôle pour chaque processus de production.', 'MUST', 'Plans de contrôle par poste/processus', 'Plan de contrôle à jour pour 100% des processus actifs', 'CRITICAL', 1),
    ('51020006-0000-0000-0000-000000000001', '9.2.2.3', 'L''organisme doit auditer chaque processus de fabrication à fréquence définie.', 'MUST', 'Programme d''audit processus, rapports', '100% des processus audités sur 3 ans', 'HIGH', 1),
    ('51020007-0000-0000-0000-000000000001', '10.2.3', 'L''organisme doit utiliser une méthodologie disciplinée de résolution de problèmes (8D).', 'MUST', 'Rapports 8D pour NC clients', 'Délai 8D D0-D8 < 90j, efficacité D8 ≥ 95%', 'CRITICAL', 1);

-- IATF certification path (§8.5)
INSERT INTO standard_certification_paths (id, standard_id, estimated_duration_months_min, estimated_duration_months_max,
    estimated_cost_eur_min, estimated_cost_eur_max, difficulty_level,
    surveillance_audit_frequency, recertification_cycle_years, notes)
VALUES ('5a000001-0000-0000-0000-000000000001', '50000001-0000-0000-0000-000000000001',
    12, 24, 25000, 120000, 5, 'annual', 3,
    'IATF 16949 requiert ISO 9001 préalable. Audit conduit par IATF-recognised CB. CSR par OEM à intégrer.');

INSERT INTO standard_certification_stages (certification_path_id, stage_number, name, description, typical_duration_weeks, deliverables, actors, qualitos_modules, order_index) VALUES
    ('5a000001-0000-0000-0000-000000000001', 1, 'Cadrage IATF + engagement direction', 'Confirmation du périmètre IATF, désignation pilote, engagement budgétaire.', '2-4', 'Lettre d''engagement, périmètre IATF, planning', 'Direction, Pilote IATF', 'Document Control, PDCA', 1),
    ('5a000001-0000-0000-0000-000000000001', 2, 'Diagnostic initial + CSR mapping', 'Gap analysis contre IATF 16949 + cartographie CSR par client OEM.', '4-6', 'Rapport d''écarts, matrice CSR', 'Pilote, Auditeur', 'Audit, Standards Hub', 2),
    ('5a000001-0000-0000-0000-000000000001', 3, 'Déploiement APQP/PPAP', 'Mise en place processus APQP 5-phases et workflow PPAP par client.', '8-12', 'Procédures APQP, templates PPAP', 'Engineering, Qualité', 'Document Control', 3),
    ('5a000001-0000-0000-0000-000000000001', 4, 'MSA + études capabilité', 'Études R&R, Cp/Cpk sur équipements critiques.', '6-8', 'Rapports MSA, registres SPC', 'Métrologie, DMAIC', 'DMAIC', 4),
    ('5a000001-0000-0000-0000-000000000001', 5, 'Plans de contrôle + Poka-Yoke', 'Plans de contrôle par poste + détrompeurs sur caractéristiques spéciales.', '6-10', 'Plans de contrôle, catalogue Poka-Yoke', 'Production, Qualité', 'DMAIC, Poka-Yoke', 5),
    ('5a000001-0000-0000-0000-000000000001', 6, 'Audit processus + audit produit', 'Programme d''audits internes processus + produit selon IATF 16949.', '4-8', 'Programme et rapports d''audits', 'Auditeurs internes', 'Audit Management', 6),
    ('5a000001-0000-0000-0000-000000000001', 7, '8D + actions correctives', 'Application méthode 8D sur NC clients et internes.', '4-12', 'Rapports 8D', 'Qualité', 'CAPA', 7),
    ('5a000001-0000-0000-0000-000000000001', 8, 'Pré-audit blanc IA', 'Simulation d''audit IATF avec IA Standards Hub.', '1-2', 'Rapport d''écarts résiduels', 'Auditeur tiers / IA', 'Standards Hub (audit blanc)', 8),
    ('5a000001-0000-0000-0000-000000000001', 9, 'Audit certification étape 1 (documentaire)', 'Revue documentaire par IATF-recognised CB.', '1', 'Rapport étape 1', 'Organisme certificateur', 'Standards Hub', 9),
    ('5a000001-0000-0000-0000-000000000001', 10, 'Audit certification étape 2 (terrain)', 'Audit terrain multi-sites.', '1-2', 'Rapport étape 2, certificat IATF', 'Organisme certificateur', 'Standards Hub', 10),
    ('5a000001-0000-0000-0000-000000000001', 11, 'Traitement NC d''audit (majeures < 60j)', 'Plan d''action et preuves de levée des NC.', '8-12', 'Plans d''action, preuves', 'Pilote', 'CAPA', 11),
    ('5a000001-0000-0000-0000-000000000001', 12, 'Audit de surveillance annuel', 'Audit annuel IATF.', '1', 'Rapport annuel', 'Organisme certificateur', 'Standards Hub', 12);

INSERT INTO standard_document_templates (standard_id, code, name, obligation, template_uri, description) VALUES
    ('50000001-0000-0000-0000-000000000001', 'IATF-MQ', 'Manuel Qualité IATF 16949', 'MANDATORY', '/standards/templates/iatf-16949/manuel-qualite-iatf.md', 'Manuel qualité aligné IATF 16949 + ISO 9001'),
    ('50000001-0000-0000-0000-000000000001', 'IATF-APQP', 'Procédure APQP — 5 phases', 'MANDATORY', '/standards/templates/iatf-16949/procedure-apqp.md', 'Procédure APQP avec jalons P0-P5'),
    ('50000001-0000-0000-0000-000000000001', 'IATF-PPAP', 'Dossier PPAP — checklist niveau 3', 'MANDATORY', '/standards/templates/iatf-16949/dossier-ppap-niveau3.md', 'Checklist PPAP niveau 3 (AIAG)');

-- ============================================================================
-- AS9100D — Aéronautique, espace, défense.
-- ============================================================================

INSERT INTO standards (id, code, full_name, publisher, current_version, publication_date,
    family, applicable_industries, description, certification_body_required,
    recertification_cycle_months, related_norm_codes, status, created_at, updated_at)
VALUES (
    '50000002-0000-0000-0000-000000000002',
    'as9100',
    'AS9100D / EN 9100:2018 — SMQ aéronautique, espace et défense',
    'SAE / IAQG',
    '2016 (Rev D)',
    '2016-09-20',
    'AEROSPACE',
    'aerospace,defense,space',
    'Standard mondial aéronautique aligné ISO 9001:2015 avec exigences additionnelles : sécurité produit, prévention des pièces contrefaites, gestion configuration, FAI (First Article Inspection), risk-based thinking renforcé.',
    TRUE,
    36,
    'iso-9001,as9110,as9120,nadcap',
    'PUBLISHED',
    now(), now()
);

INSERT INTO standard_sections (id, standard_id, code, title, description, order_index) VALUES
    ('52010004-0000-0000-0000-000000000002', '50000002-0000-0000-0000-000000000002', '4', 'Contexte de l''organisme', NULL, 4),
    ('52010005-0000-0000-0000-000000000002', '50000002-0000-0000-0000-000000000002', '5', 'Leadership', NULL, 5),
    ('52010006-0000-0000-0000-000000000002', '50000002-0000-0000-0000-000000000002', '6', 'Planification', 'Gestion du risque aéronautique, sécurité produit.', 6),
    ('52010007-0000-0000-0000-000000000002', '50000002-0000-0000-0000-000000000002', '7', 'Support', 'Compétences, sensibilisation aux pièces contrefaites.', 7),
    ('52010008-0000-0000-0000-000000000002', '50000002-0000-0000-0000-000000000002', '8', 'Réalisation opérationnelle', 'Gestion configuration, FAI, prévention FOD.', 8),
    ('52010009-0000-0000-0000-000000000002', '50000002-0000-0000-0000-000000000002', '9', 'Évaluation des performances', NULL, 9),
    ('52010010-0000-0000-0000-000000000002', '50000002-0000-0000-0000-000000000002', '10', 'Amélioration', NULL, 10);

INSERT INTO standard_clauses (id, section_id, code, title, description, order_index) VALUES
    ('52020004-0000-0000-0000-000000000001', '52010004-0000-0000-0000-000000000002', '4.1.1', 'Contexte — exigences sectorielles aérospatiales', 'Exigences réglementaires EASA, FAA, DGA, organismes équivalents.', 1),
    ('52020004-0000-0000-0000-000000000002', '52010004-0000-0000-0000-000000000002', '4.4.2', 'Sécurité produit', 'Plan de sécurité produit, classification criticité.', 2),
    ('52020005-0000-0000-0000-000000000001', '52010005-0000-0000-0000-000000000002', '5.1.1', 'Leadership — conscience sécurité produit', NULL, 1),
    ('52020006-0000-0000-0000-000000000001', '52010006-0000-0000-0000-000000000002', '6.1.1', 'Risques et opportunités — risk-based thinking aéro', 'Évaluation des risques produit, processus, programme.', 1),
    ('52020006-0000-0000-0000-000000000002', '52010006-0000-0000-0000-000000000002', '6.1.2', 'Gestion du risque opérationnel', 'Plan d''atténuation par risque identifié.', 2),
    ('52020007-0000-0000-0000-000000000001', '52010007-0000-0000-0000-000000000002', '7.2', 'Compétences — formation aéronautique spécifique', NULL, 1),
    ('52020007-0000-0000-0000-000000000002', '52010007-0000-0000-0000-000000000002', '7.3', 'Sensibilisation — pièces contrefaites & FOD', 'Programme de sensibilisation à la prévention des Counterfeit Parts et Foreign Object Debris.', 2),
    ('52020008-0000-0000-0000-000000000001', '52010008-0000-0000-0000-000000000002', '8.1.2', 'Prévention des pièces contrefaites', 'Procédure d''approvisionnement avec sources autorisées (OCM, OEM)
    ('52020008-0000-0000-0000-000000000002', '52010008-0000-0000-0000-000000000002', '8.1.3', 'Sécurité produit — analyse', 'Analyse FMEA orientée sécurité produit.', 2),
    ('52020008-0000-0000-0000-000000000003', '52010008-0000-0000-0000-000000000002', '8.1.4', 'Prévention des FOD', 'Programme FOD (Foreign Object Debris) 
    ('52020008-0000-0000-0000-000000000004', '52010008-0000-0000-0000-000000000002', '8.3.4', 'Vérification et validation conception', NULL, 4),
    ('52020008-0000-0000-0000-000000000005', '52010008-0000-0000-0000-000000000002', '8.4.3', 'Maîtrise des fournisseurs externes', 'Validation, surveillance, performances fournisseurs aéro.', 5),
    ('52020008-0000-0000-0000-000000000006', '52010008-0000-0000-0000-000000000002', '8.5.1.1', 'Maîtrise des équipements — production aéronautique', NULL, 6),
    ('52020008-0000-0000-0000-000000000007', '52010008-0000-0000-0000-000000000002', '8.5.1.2', 'Validation des processus spéciaux', 'Procédés spéciaux (soudure, traitement thermique, NDT) 
    ('52020008-0000-0000-0000-000000000008', '52010008-0000-0000-0000-000000000002', '8.5.1.3', 'First Article Inspection (FAI)
    ('52020008-0000-0000-0000-000000000009', '52010008-0000-0000-0000-000000000002', '8.5.2', 'Identification et traçabilité', 'Traçabilité matière, lot, batch, série pour pièces critiques.', 9),
    ('52020008-0000-0000-0000-000000000010', '52010008-0000-0000-0000-000000000002', '8.5.4', 'Préservation', 'Préservation matériaux et produits durant manutention, stockage, transport.', 10),
    ('52020009-0000-0000-0000-000000000001', '52010009-0000-0000-0000-000000000002', '9.1.1', 'Surveillance & mesure — performance fournisseurs aéro', NULL, 1),
    ('52020010-0000-0000-0000-000000000001', '52010010-0000-0000-0000-000000000002', '10.2.1.1', 'Non-conformités — escape rate', 'Suivi escape rate (pièces NC livrées au client)
;

INSERT INTO standard_requirements (clause_id, code, text, obligation, evidence_types, measurable_criteria, risk_if_missing, order_index) VALUES
    ('52020004-0000-0000-0000-000000000002', '4.4.2', 'L''organisme doit assurer un plan de sécurité produit cohérent.', 'MUST', 'Plan de sécurité produit, registre des items critiques', 'Plan révisé ≥ 1/an, classification documentée', 'CRITICAL', 1),
    ('52020006-0000-0000-0000-000000000001', '6.1.1', 'L''organisme doit gérer les risques pour la sécurité du produit et la qualité.', 'MUST', 'Registre des risques aéro, FMEA', 'Couverture FMEA ≥ 90% des processus aéro', 'CRITICAL', 1),
    ('52020008-0000-0000-0000-000000000001', '8.1.2', 'L''organisme doit prévenir l''introduction de pièces contrefaites.', 'MUST', 'Procédure approvisionnement, liste sources autorisées', '100% approvisionnements depuis sources autorisées ou tracés', 'CRITICAL', 1),
    ('52020008-0000-0000-0000-000000000003', '8.1.4', 'L''organisme doit prévenir, détecter et éliminer les FOD.', 'MUST', 'Procédure FOD, audits FOD, registre incidents', 'Audit FOD ≥ 1/mois zones critiques, taux FOD escape ≤ 0.1%', 'CRITICAL', 1),
    ('52020008-0000-0000-0000-000000000007', '8.5.1.2', 'L''organisme doit valider les processus spéciaux à intervalles définis.', 'MUST', 'Rapports validation NADCAP, qualifications opérateurs', 'Re-qualification ≥ 1/an, opérateurs certifiés 100%', 'CRITICAL', 1),
    ('52020008-0000-0000-0000-000000000008', '8.5.1.3', 'L''organisme doit réaliser une FAI selon AS9102 pour toute nouvelle pièce ou modification.', 'MUST', 'Rapport FAI AS9102', 'FAI complet et signé avant production série, 100% des pièces nouvelles', 'CRITICAL', 1),
    ('52020008-0000-0000-0000-000000000009', '8.5.2', 'L''organisme doit assurer l''identification et la traçabilité tout au long de la production.', 'MUST', 'Registre traçabilité, marquage produit', '100% des pièces critiques traçables matière → livraison', 'CRITICAL', 1),
    ('52020010-0000-0000-0000-000000000001', '10.2.1.1', 'L''organisme doit suivre et améliorer son taux d''échappement de défauts (escape rate).', 'MUST', 'Indicateur escape rate, plans d''action', 'Escape rate < seuil contractuel client', 'HIGH', 1);

INSERT INTO standard_certification_paths (id, standard_id, estimated_duration_months_min, estimated_duration_months_max,
    estimated_cost_eur_min, estimated_cost_eur_max, difficulty_level,
    surveillance_audit_frequency, recertification_cycle_years, notes)
VALUES ('5a000002-0000-0000-0000-000000000002', '50000002-0000-0000-0000-000000000002',
    14, 26, 30000, 150000, 5, 'annual', 3,
    'AS9100D requiert ISO 9001 préalable. Audit conduit par CB accrédité IAQG. Programme OASIS obligatoire. NADCAP en plus pour procédés spéciaux.');

INSERT INTO standard_certification_stages (certification_path_id, stage_number, name, description, typical_duration_weeks, deliverables, actors, qualitos_modules, order_index) VALUES
    ('5a000002-0000-0000-0000-000000000002', 1, 'Cadrage AS9100 + IAQG OASIS', 'Inscription OASIS, désignation pilote.', '2-4', 'Inscription OASIS, périmètre, planning', 'Direction, Pilote AS9100', 'Document Control', 1),
    ('5a000002-0000-0000-0000-000000000002', 2, 'Diagnostic + risk-based thinking', 'Gap analysis AS9100 + cartographie risques produit/processus.', '5-8', 'Rapport d''écarts, registre risques', 'Pilote, Risk Manager', 'Risk/FMEA, Standards Hub', 2),
    ('5a000002-0000-0000-0000-000000000002', 3, 'Politique sécurité produit + FOD', 'Politique formalisée, programme FOD déployé.', '4-6', 'Politique signée, procédure FOD', 'Direction, Production', 'Document Control', 3),
    ('5a000002-0000-0000-0000-000000000002', 4, 'Prévention pièces contrefaites', 'Procédure approvisionnement, validation OCM/OEM.', '4-6', 'Procédure achats aéro, liste OCM', 'Achats, Qualité', 'Supplier Quality', 4),
    ('5a000002-0000-0000-0000-000000000002', 5, 'Validation processus spéciaux (NADCAP)', 'Pré-audit NADCAP pour soudure/NDT/traitement thermique.', '12-26', 'Rapports validation NADCAP', 'Procédés spéciaux', 'Standards Hub', 5),
    ('5a000002-0000-0000-0000-000000000002', 6, 'FAI selon AS9102', 'Mise en place processus FAI pour pièces nouvelles.', '6-10', 'Templates FAI, FAI exécutés', 'Engineering, Qualité', 'Document Control', 6),
    ('5a000002-0000-0000-0000-000000000002', 7, 'Audits internes AS9100', 'Programme d''audits internes spécifique AS9100.', '4-8', 'Programme et rapports audits', 'Auditeurs internes', 'Audit', 7),
    ('5a000002-0000-0000-0000-000000000002', 8, 'Pré-audit IA Standards Hub', 'Simulation d''audit AS9100 avec IA.', '1-2', 'Rapport écarts résiduels', 'IA / Auditeur tiers', 'Standards Hub', 8),
    ('5a000002-0000-0000-0000-000000000002', 9, 'Audit certification étape 1', 'Revue documentaire CB IAQG.', '1', 'Rapport étape 1', 'CB IAQG', 'Standards Hub', 9),
    ('5a000002-0000-0000-0000-000000000002', 10, 'Audit certification étape 2', 'Audit terrain multi-sites.', '1-2', 'Rapport, certificat AS9100', 'CB IAQG', 'Standards Hub', 10),
    ('5a000002-0000-0000-0000-000000000002', 11, 'Traitement NC majeures', 'Plan d''action + preuves de levée.', '8-12', 'Preuves de levée', 'Pilote', 'CAPA', 11),
    ('5a000002-0000-0000-0000-000000000002', 12, 'Audit de surveillance OASIS', 'Audit annuel + maintien inscription OASIS.', '1', 'Rapport OASIS', 'CB IAQG', 'Standards Hub', 12);

INSERT INTO standard_document_templates (standard_id, code, name, obligation, template_uri, description) VALUES
    ('50000002-0000-0000-0000-000000000002', 'AS9100-MQ', 'Manuel Qualité AS9100D', 'MANDATORY', '/standards/templates/as9100/manuel-qualite-as9100.md', 'Manuel qualité aérospatial'),
    ('50000002-0000-0000-0000-000000000002', 'AS9100-FOD', 'Procédure prévention FOD', 'MANDATORY', '/standards/templates/as9100/procedure-fod.md', 'Procédure FOD'),
    ('50000002-0000-0000-0000-000000000002', 'AS9100-FAI', 'Template FAI AS9102', 'MANDATORY', '/standards/templates/as9100/template-fai-as9102.md', 'First Article Inspection');

-- ============================================================================
-- ISO 13485:2016 — Dispositifs médicaux.
-- ============================================================================

INSERT INTO standards (id, code, full_name, publisher, current_version, publication_date,
    family, applicable_industries, description, certification_body_required,
    recertification_cycle_months, related_norm_codes, status, created_at, updated_at)
VALUES (
    '50000003-0000-0000-0000-000000000003',
    'iso-13485',
    'ISO 13485:2016 — Dispositifs médicaux — Systèmes de management de la qualité — Exigences à des fins réglementaires',
    'ISO',
    '2016',
    '2016-03-01',
    'MEDICAL_DEVICES',
    'medical_devices,pharma,healthcare',
    'Standard SMQ spécifique aux dispositifs médicaux, harmonisé avec ISO 9001 mais avec exigences réglementaires renforcées (UDI, vigilance, dossier technique, évaluation clinique).',
    TRUE,
    36,
    'iso-9001,iso-14971,mdr-2017-745,ivdr-2017-746,fda-21-cfr-820',
    'PUBLISHED',
    now(), now()
);

INSERT INTO standard_sections (id, standard_id, code, title, description, order_index) VALUES
    ('53010004-0000-0000-0000-000000000003', '50000003-0000-0000-0000-000000000003', '4', 'SMQ pour dispositifs médicaux', NULL, 4),
    ('53010005-0000-0000-0000-000000000003', '50000003-0000-0000-0000-000000000003', '5', 'Responsabilité de la direction', NULL, 5),
    ('53010006-0000-0000-0000-000000000003', '50000003-0000-0000-0000-000000000003', '6', 'Management des ressources', NULL, 6),
    ('53010007-0000-0000-0000-000000000003', '50000003-0000-0000-0000-000000000003', '7', 'Réalisation du produit', NULL, 7),
    ('53010008-0000-0000-0000-000000000003', '50000003-0000-0000-0000-000000000003', '8', 'Mesure, analyse, amélioration', NULL, 8);

INSERT INTO standard_clauses (id, section_id, code, title, description, order_index) VALUES
    ('53020004-0000-0000-0000-000000000001', '53010004-0000-0000-0000-000000000003', '4.1.1', 'Exigences générales SMQ DM', 'SMQ documenté couvrant cycle de vie du DM.', 1),
    ('53020004-0000-0000-0000-000000000002', '53010004-0000-0000-0000-000000000003', '4.1.6', 'Validation des logiciels SMQ', 'Validation logiciels utilisés dans le SMQ et les processus de production.', 2),
    ('53020004-0000-0000-0000-000000000003', '53010004-0000-0000-0000-000000000003', '4.2.3', 'Dossier de dispositif médical', 'Dossier technique par DM (Device Master Record)
    ('53020005-0000-0000-0000-000000000001', '53010005-0000-0000-0000-000000000003', '5.5.2', 'Représentant de la direction', NULL, 1),
    ('53020006-0000-0000-0000-000000000001', '53010006-0000-0000-0000-000000000003', '6.4.1', 'Environnement de travail', 'Conditions environnementales pour conformité produit (salle blanche, ESD…)
    ('53020006-0000-0000-0000-000000000002', '53010006-0000-0000-0000-000000000003', '6.4.2', 'Maîtrise de la contamination', 'Mesures contre contamination produit & environnement.', 2),
    ('53020007-0000-0000-0000-000000000001', '53010007-0000-0000-0000-000000000003', '7.1', 'Planification de la réalisation du produit', NULL, 1),
    ('53020007-0000-0000-0000-000000000002', '53010007-0000-0000-0000-000000000003', '7.2.1', 'Détermination exigences clients (DM)
    ('53020007-0000-0000-0000-000000000003', '53010007-0000-0000-0000-000000000003', '7.3.2', 'Planification conception & développement', NULL, 3),
    ('53020007-0000-0000-0000-000000000004', '53010007-0000-0000-0000-000000000003', '7.3.7', 'Transfert de conception', 'Procédure transfert R&D → production.', 4),
    ('53020007-0000-0000-0000-000000000005', '53010007-0000-0000-0000-000000000003', '7.3.9', 'Gestion des modifications de conception', NULL, 5),
    ('53020007-0000-0000-0000-000000000006', '53010007-0000-0000-0000-000000000003', '7.4.1', 'Achats — sélection fournisseurs DM', NULL, 6),
    ('53020007-0000-0000-0000-000000000007', '53010007-0000-0000-0000-000000000003', '7.5.2', 'Propreté du produit', NULL, 7),
    ('53020007-0000-0000-0000-000000000008', '53010007-0000-0000-0000-000000000003', '7.5.5', 'Exigences particulières — DM stériles', 'Validation processus de stérilisation.', 8),
    ('53020007-0000-0000-0000-000000000009', '53010007-0000-0000-0000-000000000003', '7.5.6', 'Validation des processus de production', NULL, 9),
    ('53020007-0000-0000-0000-000000000010', '53010007-0000-0000-0000-000000000003', '7.5.8', 'Identification — UDI', 'Identification unique des dispositifs (UDI)
    ('53020007-0000-0000-0000-000000000011', '53010007-0000-0000-0000-000000000003', '7.5.9', 'Traçabilité', NULL, 11),
    ('53020008-0000-0000-0000-000000000001', '53010008-0000-0000-0000-000000000003', '8.2.1', 'Retour d''information (vigilance)
    ('53020008-0000-0000-0000-000000000002', '53010008-0000-0000-0000-000000000003', '8.2.3', 'Reporting aux autorités réglementaires', 'Notification d''incidents (ANSM, FDA, etc.)
    ('53020008-0000-0000-0000-000000000003', '53010008-0000-0000-0000-000000000003', '8.3.4', 'Actions correctives — DM', NULL, 3)
;

INSERT INTO standard_requirements (clause_id, code, text, obligation, evidence_types, measurable_criteria, risk_if_missing, order_index) VALUES
    ('53020004-0000-0000-0000-000000000002', '4.1.6', 'L''organisme doit documenter les procédures de validation de l''application de logiciels utilisés dans le SMQ.', 'MUST', 'Plan de validation logiciels (CSV), rapports IQ/OQ/PQ', '100% logiciels SMQ critiques validés, re-validation après changement', 'CRITICAL', 1),
    ('53020004-0000-0000-0000-000000000003', '4.2.3', 'L''organisme doit établir un dossier pour chaque type ou famille de dispositif.', 'MUST', 'Device Master Record (DMR), Device History Record (DHR)', 'DMR à jour pour 100% DM commercialisés', 'CRITICAL', 1),
    ('53020006-0000-0000-0000-000000000002', '6.4.2', 'L''organisme doit planifier et documenter les exigences pour la maîtrise des produits contaminés ou potentiellement contaminés.', 'MUST', 'Procédure contamination, monitoring environnement', 'Monitoring particulaire/microbio dans tolérances, alarmes traitées < 24h', 'CRITICAL', 1),
    ('53020007-0000-0000-0000-000000000004', '7.3.7', 'L''organisme doit documenter les procédures de transfert de conception.', 'MUST', 'Procédure transfert R&D → industrialisation', 'Transfert documenté et validé avant production série', 'CRITICAL', 1),
    ('53020007-0000-0000-0000-000000000008', '7.5.5', 'Pour les DM stériles, l''organisme doit valider chaque processus de stérilisation.', 'MUST', 'Validation stérilisation (chaleur, gamma, EtO…)', 'Validation initiale + revalidation périodique', 'CRITICAL', 1),
    ('53020007-0000-0000-0000-000000000010', '7.5.8', 'L''organisme doit appliquer un UDI à chaque DM conformément aux exigences réglementaires applicables.', 'MUST', 'Registre UDI, intégration EUDAMED / GUDID', '100% DM avec UDI conforme au pays de mise sur marché', 'CRITICAL', 1),
    ('53020008-0000-0000-0000-000000000001', '8.2.1', 'L''organisme doit mettre en place une procédure documentée pour le retour d''information y compris la vigilance post-commercialisation.', 'MUST', 'Procédure vigilance, registre incidents', 'Délai analyse incident grave < 48h, déclaration autorité < 15j', 'CRITICAL', 1),
    ('53020008-0000-0000-0000-000000000002', '8.2.3', 'L''organisme doit signaler aux autorités tout incident répondant aux critères de signalement.', 'MUST', 'Rapports vigilance ANSM/FDA, suivi', '100% incidents éligibles signalés dans les délais réglementaires', 'CRITICAL', 1);

INSERT INTO standard_certification_paths (id, standard_id, estimated_duration_months_min, estimated_duration_months_max,
    estimated_cost_eur_min, estimated_cost_eur_max, difficulty_level,
    surveillance_audit_frequency, recertification_cycle_years, notes)
VALUES ('5a000003-0000-0000-0000-000000000003', '50000003-0000-0000-0000-000000000003',
    12, 24, 25000, 100000, 5, 'annual', 3,
    'ISO 13485 souvent couplée MDR/IVDR (UE) ou FDA 21 CFR Part 820 (US). DMR / DHR obligatoires. UDI conforme EUDAMED.');

INSERT INTO standard_certification_stages (certification_path_id, stage_number, name, description, typical_duration_weeks, deliverables, actors, qualitos_modules, order_index) VALUES
    ('5a000003-0000-0000-0000-000000000003', 1, 'Cadrage ISO 13485 + classification DM', 'Identification classe DM (I/IIa/IIb/III).', '3-5', 'Périmètre, classification DM, planning', 'Direction, Affaires Régl.', 'Document Control', 1),
    ('5a000003-0000-0000-0000-000000000003', 2, 'Diagnostic + gestion du risque ISO 14971', 'Gap analysis + plan de gestion du risque par DM.', '5-8', 'Rapport d''écarts, plan gestion risque', 'Risk Manager', 'Risk/FMEA, Standards Hub', 2),
    ('5a000003-0000-0000-0000-000000000003', 3, 'Constitution du Device Master Record', 'DMR par famille de DM avec spécifications, processus, contrôles.', '8-14', 'DMR documenté', 'R&D, Industrialisation', 'Document Control', 3),
    ('5a000003-0000-0000-0000-000000000003', 4, 'Validation des processus production (IQ/OQ/PQ)', 'Validation des processus critiques selon GAMP 5.', '12-20', 'Rapports IQ/OQ/PQ', 'Industrialisation, Validation', 'Standards Hub', 4),
    ('5a000003-0000-0000-0000-000000000003', 5, 'UDI + intégration EUDAMED/GUDID', 'Génération UDI, enregistrement dans bases réglementaires.', '4-8', 'Registre UDI, soumissions', 'Affaires Régl.', 'Document Control', 5),
    ('5a000003-0000-0000-0000-000000000003', 6, 'Système vigilance PMS / PSUR', 'Mise en place processus surveillance post-commercialisation.', '6-10', 'Procédure vigilance, PSUR', 'Vigilance, Affaires Régl.', 'CAPA, Complaints', 6),
    ('5a000003-0000-0000-0000-000000000003', 7, 'Évaluation clinique (si MDR)', 'Dossier d''évaluation clinique selon MEDDEV 2.7/1 rev 4.', '8-26', 'Plan + rapport évaluation clinique', 'Affaires Régl., R&D', 'Standards Hub', 7),
    ('5a000003-0000-0000-0000-000000000003', 8, 'Audits internes ISO 13485', 'Programme d''audits internes spécifique DM.', '4-8', 'Programme, rapports audits', 'Auditeurs internes', 'Audit', 8),
    ('5a000003-0000-0000-0000-000000000003', 9, 'Pré-audit IA Standards Hub', 'Simulation d''audit ISO 13485.', '1-2', 'Rapport écarts résiduels', 'IA / Auditeur tiers', 'Standards Hub', 9),
    ('5a000003-0000-0000-0000-000000000003', 10, 'Audit certification étape 1', 'Revue documentaire CB notifié.', '1', 'Rapport étape 1', 'Organisme notifié', 'Standards Hub', 10),
    ('5a000003-0000-0000-0000-000000000003', 11, 'Audit certification étape 2', 'Audit terrain.', '1-2', 'Rapport, certificat ISO 13485', 'Organisme notifié', 'Standards Hub', 11),
    ('5a000003-0000-0000-0000-000000000003', 12, 'Audit surveillance annuel', NULL, '1', 'Rapport surveillance', 'Organisme notifié', 'Standards Hub', 12);

INSERT INTO standard_document_templates (standard_id, code, name, obligation, template_uri, description) VALUES
    ('50000003-0000-0000-0000-000000000003', 'ISO13485-DMR', 'Device Master Record', 'MANDATORY', '/standards/templates/iso-13485/device-master-record.md', 'Dossier maître de dispositif médical'),
    ('50000003-0000-0000-0000-000000000003', 'ISO13485-VIGIL', 'Procédure de vigilance post-marché', 'MANDATORY', '/standards/templates/iso-13485/procedure-vigilance.md', 'PMS + vigilance'),
    ('50000003-0000-0000-0000-000000000003', 'ISO13485-RISK', 'Plan gestion du risque (ISO 14971)', 'MANDATORY', '/standards/templates/iso-13485/plan-gestion-risque.md', 'Risk Management File');

-- ============================================================================
-- FDA 21 CFR Part 11 — Signatures électroniques & records (US FDA).
-- ============================================================================

INSERT INTO standards (id, code, full_name, publisher, current_version, publication_date,
    family, applicable_industries, description, certification_body_required,
    recertification_cycle_months, related_norm_codes, status, created_at, updated_at)
VALUES (
    '50000004-0000-0000-0000-000000000004',
    'fda-21-cfr-part-11',
    'FDA 21 CFR Part 11 — Electronic Records; Electronic Signatures',
    'US FDA',
    '1997 (amendé 2003)',
    '1997-08-20',
    'REGULATORY',
    'pharma,medical_devices,biotech',
    'Règlement FDA encadrant l''utilisation des enregistrements et signatures électroniques en lieu et place du papier. Exigences sur audit trail, validation système, contrôles d''accès, signatures.',
    FALSE,
    NULL,
    'iso-13485,gamp-5,fda-21-cfr-820,fda-21-cfr-211',
    'PUBLISHED',
    now(), now()
);

INSERT INTO standard_sections (id, standard_id, code, title, description, order_index) VALUES
    ('54010001-0000-0000-0000-000000000004', '50000004-0000-0000-0000-000000000004', 'A', 'Subpart A — General Provisions', 'Scope, definitions, implementation.', 1),
    ('54010002-0000-0000-0000-000000000004', '50000004-0000-0000-0000-000000000004', 'B', 'Subpart B — Electronic Records', 'Controls for closed/open systems, signature manifestations, copies.', 2),
    ('54010003-0000-0000-0000-000000000004', '50000004-0000-0000-0000-000000000004', 'C', 'Subpart C — Electronic Signatures', 'General requirements, signature components, controls for identification codes/passwords.', 3);

INSERT INTO standard_clauses (id, section_id, code, title, description, order_index) VALUES
    ('54020001-0000-0000-0000-000000000001', '54010001-0000-0000-0000-000000000004', '11.1', 'Scope', 'Records in electronic form created/modified/stored/transmitted under FDA regulations.', 1),
    ('54020001-0000-0000-0000-000000000002', '54010001-0000-0000-0000-000000000004', '11.2', 'Implementation', 'Equivalence with paper for FDA acceptance.', 2),
    ('54020001-0000-0000-0000-000000000003', '54010001-0000-0000-0000-000000000004', '11.3', 'Definitions', 'Definitions of biometrics, closed system, digital signature, electronic record, electronic signature.', 3),
    ('54020002-0000-0000-0000-000000000001', '54010002-0000-0000-0000-000000000004', '11.10(a)', 'Validation of systems', 'System validation to ensure accuracy, reliability, consistent intended performance, ability to discern invalid/altered records.', 1),
    ('54020002-0000-0000-0000-000000000002', '54010002-0000-0000-0000-000000000004', '11.10(b)', 'Generation of accurate copies', 'Ability to produce accurate and complete copies in human-readable and electronic form for inspection.', 2),
    ('54020002-0000-0000-0000-000000000003', '54010002-0000-0000-0000-000000000004', '11.10(c)', 'Protection of records', 'Records protection throughout retention period.', 3),
    ('54020002-0000-0000-0000-000000000004', '54010002-0000-0000-0000-000000000004', '11.10(d)', 'Access limited to authorized individuals', NULL, 4),
    ('54020002-0000-0000-0000-000000000005', '54010002-0000-0000-0000-000000000004', '11.10(e)', 'Audit trails', 'Secure, computer-generated, time-stamped audit trails to record date/time of operator entries and actions; original information shall not be obscured.', 5),
    ('54020002-0000-0000-0000-000000000006', '54010002-0000-0000-0000-000000000004', '11.10(f)', 'Operational system checks', 'Sequencing of steps, events enforcement.', 6),
    ('54020002-0000-0000-0000-000000000007', '54010002-0000-0000-0000-000000000004', '11.10(g)', 'Authority checks', 'Authorized individuals can use the system, electronically sign, access, alter records.', 7),
    ('54020002-0000-0000-0000-000000000008', '54010002-0000-0000-0000-000000000004', '11.10(h)', 'Device (terminal) 
    ('54020002-0000-0000-0000-000000000009', '54010002-0000-0000-0000-000000000004', '11.10(i)', 'Education, training, experience', 'Personnel qualified for system use.', 9),
    ('54020002-0000-0000-0000-000000000010', '54010002-0000-0000-0000-000000000004', '11.10(k)', 'Documentation controls', 'Distribution, access, use of system documentation.', 10),
    ('54020002-0000-0000-0000-000000000011', '54010002-0000-0000-0000-000000000004', '11.30', 'Controls for open systems', 'Encryption + digital signature for open systems.', 11),
    ('54020002-0000-0000-0000-000000000012', '54010002-0000-0000-0000-000000000004', '11.50', 'Signature manifestations', 'Printed/displayed name, date/time, meaning of signature.', 12),
    ('54020003-0000-0000-0000-000000000001', '54010003-0000-0000-0000-000000000004', '11.100', 'General requirements (e-signature)
    ('54020003-0000-0000-0000-000000000002', '54010003-0000-0000-0000-000000000004', '11.200', 'Signature components and controls', 'Two distinct identification components (e.g. ID + password) 
    ('54020003-0000-0000-0000-000000000003', '54010003-0000-0000-0000-000000000004', '11.300', 'Controls for identification codes/passwords', 'Uniqueness, periodic change, loss management, unauthorized use safeguards.', 3)
;

INSERT INTO standard_requirements (clause_id, code, text, obligation, evidence_types, measurable_criteria, risk_if_missing, order_index) VALUES
    ('54020002-0000-0000-0000-000000000001', '11.10(a)', 'Persons who use closed systems shall employ procedures and controls designed to ensure validation of systems to ensure accuracy, reliability, consistent intended performance, and the ability to discern invalid or altered records.', 'MUST', 'System Validation Plan, IQ/OQ/PQ reports, Validation Summary Report', 'CSV complète pour 100% systèmes GxP, re-validation après changement majeur', 'CRITICAL', 1),
    ('54020002-0000-0000-0000-000000000005', '11.10(e)', 'Secure, computer-generated, time-stamped audit trails to independently record the date and time of operator entries and actions that create, modify, or delete electronic records. Record changes shall not obscure previously recorded information.', 'MUST', 'Audit trail technique (DB triggers / event log), revue audit trail', 'Audit trail activé 100% des systèmes GxP, revue ≥ 1/mois pour systèmes critiques', 'CRITICAL', 1),
    ('54020002-0000-0000-0000-000000000007', '11.10(g)', 'Use of authority checks to ensure that only authorized individuals can use the system, electronically sign a record, access the operation or computer system input or output device, alter a record, or perform the operation at hand.', 'MUST', 'Matrice RBAC, logs accès, revue accès', 'Revue accès ≥ 1/trimestre, 0 partage de compte', 'CRITICAL', 1),
    ('54020003-0000-0000-0000-000000000001', '11.100', 'Each electronic signature shall be unique to one individual and shall not be reused by, or reassigned to, anyone else. Before an organization establishes, assigns, certifies, or otherwise sanctions an individual''s electronic signature, the organization shall verify the identity of the individual.', 'MUST', 'Procédure enregistrement utilisateur, preuve d''identification', '100% utilisateurs identifiés et vérifiés avant attribution e-signature', 'CRITICAL', 1),
    ('54020003-0000-0000-0000-000000000002', '11.200', 'Electronic signatures that are not based upon biometrics shall employ at least two distinct identification components such as an identification code and password.', 'MUST', 'Configuration système (MFA/2FA), preuve double composante', '2FA activé 100% pour e-signatures GxP', 'CRITICAL', 1),
    ('54020003-0000-0000-0000-000000000003', '11.300', 'Controls to ensure security and integrity of identification codes and passwords, including uniqueness, periodic checks, recalls, loss/theft management, transaction safeguards.', 'MUST', 'Politique mots de passe, rotation 90j, logs', 'Rotation password ≤ 90j, blocage après 5 échecs, alerte sur tentative anormale', 'CRITICAL', 1);

INSERT INTO standard_certification_paths (id, standard_id, estimated_duration_months_min, estimated_duration_months_max,
    estimated_cost_eur_min, estimated_cost_eur_max, difficulty_level,
    surveillance_audit_frequency, recertification_cycle_years, notes)
VALUES ('5a000004-0000-0000-0000-000000000004', '50000004-0000-0000-0000-000000000004',
    6, 18, 15000, 80000, 4, 'fda_inspection_periodic', 0,
    'Pas de certification au sens ISO. Conformité auditée par FDA durant inspections (PAI, BIMO, GMP). CSV + audit trail + e-signature 2FA requis.');

INSERT INTO standard_certification_stages (certification_path_id, stage_number, name, description, typical_duration_weeks, deliverables, actors, qualitos_modules, order_index) VALUES
    ('5a000004-0000-0000-0000-000000000004', 1, 'Cadrage Part 11 + inventaire systèmes', 'Identifier les systèmes GxP soumis à Part 11.', '2-4', 'Inventaire systèmes, classification GxP', 'IT, Validation', 'Document Control', 1),
    ('5a000004-0000-0000-0000-000000000004', 2, 'Gap analysis Part 11 (GAMP 5)', 'Évaluation contre §11.10/11.30/11.50/11.100/11.200/11.300.', '4-6', 'Rapport d''écarts', 'Validation, IT', 'Standards Hub', 2),
    ('5a000004-0000-0000-0000-000000000004', 3, 'CSV — Plan de validation', 'User Requirements, Functional Specs, Risk-based test plan.', '6-10', 'URS, FS, Validation Plan', 'Validation, R&D', 'Document Control', 3),
    ('5a000004-0000-0000-0000-000000000004', 4, 'IQ — Qualification d''installation', 'Tests installation matériel/logiciel.', '2-4', 'Rapport IQ', 'IT, Validation', 'Document Control', 4),
    ('5a000004-0000-0000-0000-000000000004', 5, 'OQ — Qualification opérationnelle', 'Tests fonctions à vide vs spécifications.', '4-8', 'Rapport OQ', 'Validation', 'Document Control', 5),
    ('5a000004-0000-0000-0000-000000000004', 6, 'PQ — Qualification de performance', 'Tests avec données et utilisateurs réels en environnement de prod.', '4-8', 'Rapport PQ', 'Métiers, Validation', 'Document Control', 6),
    ('5a000004-0000-0000-0000-000000000004', 7, 'Mise en place audit trails', 'Activation audit trail + procédure de revue.', '2-4', 'Configuration, procédure revue', 'IT, Qualité', 'Document Control', 7),
    ('5a000004-0000-0000-0000-000000000004', 8, 'E-signature 2FA + RBAC', 'Configuration 2FA, attribution unique, rotation passwords.', '2-4', 'Politique e-signature, RBAC matrix', 'IT, RH', 'Document Control', 8),
    ('5a000004-0000-0000-0000-000000000004', 9, 'Formation Part 11', 'Formation utilisateurs et administrateurs.', '2-3', 'Plan formation, attestations', 'RH, Formation', 'Training', 9),
    ('5a000004-0000-0000-0000-000000000004', 10, 'Audit interne Part 11', 'Audit interne CSV + audit trail + e-signature.', '2-4', 'Rapport audit interne', 'Auditeurs', 'Audit', 10),
    ('5a000004-0000-0000-0000-000000000004', 11, 'Préparation inspection FDA', 'Dossier pré-inspection, runbook FDA Form 483.', '2-4', 'Pre-inspection package', 'Affaires Régl.', 'Document Control', 11),
    ('5a000004-0000-0000-0000-000000000004', 12, 'Maintien continu (re-validation)', 'Re-validation après changement, revue annuelle.', 'continu', 'Rapports revue annuelle', 'Validation', 'PDCA, Standards Hub', 12);

INSERT INTO standard_document_templates (standard_id, code, name, obligation, template_uri, description) VALUES
    ('50000004-0000-0000-0000-000000000004', 'P11-VMP', 'Validation Master Plan (Part 11)', 'MANDATORY', '/standards/templates/fda-21-cfr-part-11/validation-master-plan.md', 'VMP CSV GAMP 5'),
    ('50000004-0000-0000-0000-000000000004', 'P11-AT', 'Procédure de revue d''audit trail', 'MANDATORY', '/standards/templates/fda-21-cfr-part-11/audit-trail-review-procedure.md', 'Revue audit trail GxP'),
    ('50000004-0000-0000-0000-000000000004', 'P11-ESIG', 'Politique signature électronique 2FA', 'MANDATORY', '/standards/templates/fda-21-cfr-part-11/politique-esignature.md', 'Politique e-signature 21 CFR 11.200');

-- ============================================================================
-- ISO 22000:2018 / HACCP — Sécurité des denrées alimentaires.
-- ============================================================================

INSERT INTO standards (id, code, full_name, publisher, current_version, publication_date,
    family, applicable_industries, description, certification_body_required,
    recertification_cycle_months, related_norm_codes, status, created_at, updated_at)
VALUES (
    '50000005-0000-0000-0000-000000000005',
    'iso-22000',
    'ISO 22000:2018 — Systèmes de management de la sécurité des denrées alimentaires (HACCP + HLS)',
    'ISO',
    '2018',
    '2018-06-19',
    'FOOD_SAFETY',
    'agro,food,beverage,catering',
    'Standard international SMSDA intégrant les principes HACCP (Codex Alimentarius) avec la structure HLS. Couvre la chaîne alimentaire de la production primaire à la consommation. Pré-requis (PRP/PRPo) + plan HACCP.',
    TRUE,
    36,
    'haccp-codex,fssc-22000,ifs-food,brcgs-food',
    'PUBLISHED',
    now(), now()
);

INSERT INTO standard_sections (id, standard_id, code, title, description, order_index) VALUES
    ('55010004-0000-0000-0000-000000000005', '50000005-0000-0000-0000-000000000005', '4', 'Contexte de l''organisme', NULL, 4),
    ('55010005-0000-0000-0000-000000000005', '50000005-0000-0000-0000-000000000005', '5', 'Leadership', NULL, 5),
    ('55010006-0000-0000-0000-000000000005', '50000005-0000-0000-0000-000000000005', '6', 'Planification', NULL, 6),
    ('55010007-0000-0000-0000-000000000005', '50000005-0000-0000-0000-000000000005', '7', 'Support', NULL, 7),
    ('55010008-0000-0000-0000-000000000005', '50000005-0000-0000-0000-000000000005', '8', 'Activités opérationnelles (HACCP)', 'PRP, traçabilité, analyse des dangers, plan HACCP, CCP, surveillance.', 8),
    ('55010009-0000-0000-0000-000000000005', '50000005-0000-0000-0000-000000000005', '9', 'Évaluation des performances', NULL, 9),
    ('55010010-0000-0000-0000-000000000005', '50000005-0000-0000-0000-000000000005', '10', 'Amélioration', NULL, 10);

INSERT INTO standard_clauses (id, section_id, code, title, description, order_index) VALUES
    ('55020004-0000-0000-0000-000000000001', '55010004-0000-0000-0000-000000000005', '4.1', 'Compréhension du contexte (filière alimentaire)
    ('55020004-0000-0000-0000-000000000002', '55010004-0000-0000-0000-000000000005', '4.2', 'Parties intéressées (consommateur, autorité sanitaire)
    ('55020005-0000-0000-0000-000000000001', '55010005-0000-0000-0000-000000000005', '5.2', 'Politique sécurité des aliments', NULL, 1),
    ('55020006-0000-0000-0000-000000000001', '55010006-0000-0000-0000-000000000005', '6.2', 'Objectifs SMSDA', NULL, 1),
    ('55020007-0000-0000-0000-000000000001', '55010007-0000-0000-0000-000000000005', '7.1.6', 'Maîtrise des prestataires & sous-traitants', NULL, 1),
    ('55020007-0000-0000-0000-000000000002', '55010007-0000-0000-0000-000000000005', '7.2', 'Compétences (équipe HACCP)
    ('55020008-0000-0000-0000-000000000001', '55010008-0000-0000-0000-000000000005', '8.2', 'Programmes prérequis (PRP)
    ('55020008-0000-0000-0000-000000000002', '55010008-0000-0000-0000-000000000005', '8.3', 'Système de traçabilité', 'Traçabilité amont/aval lot par lot.', 2),
    ('55020008-0000-0000-0000-000000000003', '55010008-0000-0000-0000-000000000005', '8.4.1', 'Préparation et réponse aux situations d''urgence', 'Plan de gestion crise alimentaire (retrait/rappel)
    ('55020008-0000-0000-0000-000000000004', '55010008-0000-0000-0000-000000000005', '8.5.1', 'Étapes préliminaires à l''analyse des dangers', 'Équipe HACCP, caractéristiques produit, diagramme de flux.', 4),
    ('55020008-0000-0000-0000-000000000005', '55010008-0000-0000-0000-000000000005', '8.5.2', 'Analyse des dangers (microbio, chimique, physique, allergènes)
    ('55020008-0000-0000-0000-000000000006', '55010008-0000-0000-0000-000000000005', '8.5.4', 'Plan de maîtrise des dangers (PRPo + CCP)
    ('55020008-0000-0000-0000-000000000007', '55010008-0000-0000-0000-000000000005', '8.7', 'Maîtrise de la surveillance et de la mesure', NULL, 7),
    ('55020008-0000-0000-0000-000000000008', '55010008-0000-0000-0000-000000000005', '8.8', 'Vérification PRP et plan HACCP', NULL, 8),
    ('55020008-0000-0000-0000-000000000009', '55010008-0000-0000-0000-000000000005', '8.9.1', 'Maîtrise des non-conformités produits', 'Produits potentiellement dangereux : quarantaine, retrait, rappel.', 9),
    ('55020008-0000-0000-0000-000000000010', '55010008-0000-0000-0000-000000000005', '8.9.4', 'Retrait/rappel', 'Procédure documentée, exercice annuel.', 10),
    ('55020009-0000-0000-0000-000000000001', '55010009-0000-0000-0000-000000000005', '9.2', 'Audit interne SMSDA', NULL, 1),
    ('55020009-0000-0000-0000-000000000002', '55010009-0000-0000-0000-000000000005', '9.3', 'Revue de direction SMSDA', NULL, 2),
    ('55020010-0000-0000-0000-000000000001', '55010010-0000-0000-0000-000000000005', '10.2', 'Actions correctives (HACCP)
;

INSERT INTO standard_requirements (clause_id, code, text, obligation, evidence_types, measurable_criteria, risk_if_missing, order_index) VALUES
    ('55020008-0000-0000-0000-000000000001', '8.2', 'L''organisme doit établir, mettre en œuvre et tenir à jour des PRP pour aider à maîtriser la probabilité d''introduire des dangers liés à la sécurité des aliments.', 'MUST', 'Procédures PRP (nettoyage, hygiène, lutte nuisibles…)', 'Audit PRP ≥ 1/an, conformité ≥ 95%', 'CRITICAL', 1),
    ('55020008-0000-0000-0000-000000000002', '8.3', 'L''organisme doit établir un système de traçabilité permettant l''identification des lots, leur relation avec les lots de matières premières et les enregistrements de transformation et de livraison.', 'MUST', 'Système ERP traçabilité, exercice traçabilité', 'Temps de remontée traçabilité ≤ 2h, exactitude ≥ 99%', 'CRITICAL', 1),
    ('55020008-0000-0000-0000-000000000005', '8.5.2', 'L''équipe sécurité des aliments doit identifier et évaluer tous les dangers raisonnablement attendus en relation avec le type de produit, le procédé de production et les installations actuelles.', 'MUST', 'Analyse des dangers documentée par étape', 'Couverture 100% étapes du process, mise à jour à chaque changement', 'CRITICAL', 1),
    ('55020008-0000-0000-0000-000000000006', '8.5.4', 'L''organisme doit établir un plan de maîtrise des dangers qui identifie les CCP et leurs limites critiques.', 'MUST', 'Plan HACCP, registre CCP, limites critiques', '100% CCP identifiés avec limites validées scientifiquement', 'CRITICAL', 1),
    ('55020008-0000-0000-0000-000000000007', '8.7', 'L''organisme doit assurer la maîtrise des activités de surveillance et de mesure pour les CCP/PRPo (étalonnage, fréquence, méthodes).', 'MUST', 'Plans de surveillance, registres mesure, étalonnages', 'Surveillance CCP continue ou fréquence définie, étalonnage ≥ 1/an', 'CRITICAL', 1),
    ('55020008-0000-0000-0000-000000000010', '8.9.4', 'L''organisme doit établir une procédure de retrait/rappel.', 'MUST', 'Procédure retrait/rappel, exercices', 'Exercice de simulation ≥ 1/an, temps de réaction < 4h', 'CRITICAL', 1);

INSERT INTO standard_certification_paths (id, standard_id, estimated_duration_months_min, estimated_duration_months_max,
    estimated_cost_eur_min, estimated_cost_eur_max, difficulty_level,
    surveillance_audit_frequency, recertification_cycle_years, notes)
VALUES ('5a000005-0000-0000-0000-000000000005', '50000005-0000-0000-0000-000000000005',
    8, 18, 12000, 60000, 4, 'annual', 3,
    'ISO 22000 couvre la chaîne alimentaire complète. Souvent combinée FSSC 22000 (GFSI). Exercice retrait/rappel obligatoire annuel.');

INSERT INTO standard_certification_stages (certification_path_id, stage_number, name, description, typical_duration_weeks, deliverables, actors, qualitos_modules, order_index) VALUES
    ('5a000005-0000-0000-0000-000000000005', 1, 'Cadrage SMSDA + équipe HACCP', 'Constitution équipe HACCP multidisciplinaire.', '2-4', 'Charte équipe HACCP, périmètre', 'Direction, Qualité', 'Document Control', 1),
    ('5a000005-0000-0000-0000-000000000005', 2, 'PRP — programmes prérequis', 'Construction, hygiène personnel, nettoyage, nuisibles, déchets.', '4-8', 'Procédures PRP', 'Production, Qualité', 'Document Control, Audit', 2),
    ('5a000005-0000-0000-0000-000000000005', 3, 'Étapes préliminaires HACCP', 'Caractéristiques produit, usage prévu, diagrammes de flux.', '3-5', 'Fiches produit, diagrammes', 'Équipe HACCP', 'Document Control', 3),
    ('5a000005-0000-0000-0000-000000000005', 4, 'Analyse des dangers (microbio/chimique/physique/allergènes)', 'Identification et évaluation par étape.', '4-8', 'Analyse des dangers documentée', 'Équipe HACCP', 'Ishikawa, Risk/FMEA', 4),
    ('5a000005-0000-0000-0000-000000000005', 5, 'Identification CCP + PRPo + limites critiques', 'Arbre de décision Codex, limites scientifiquement validées.', '4-6', 'Plan HACCP', 'Équipe HACCP', 'Document Control', 5),
    ('5a000005-0000-0000-0000-000000000005', 6, 'Système traçabilité lot par lot', 'Implémentation ERP/MES de traçabilité.', '4-8', 'Système traçabilité opérationnel', 'IT, Production', 'IoT Connectivity', 6),
    ('5a000005-0000-0000-0000-000000000005', 7, 'Plan de surveillance CCP/PRPo', 'Fréquences, responsables, méthodes, actions correctives.', '3-5', 'Plans de surveillance', 'Qualité', 'Document Control', 7),
    ('5a000005-0000-0000-0000-000000000005', 8, 'Procédure retrait/rappel + exercice', 'Procédure documentée + simulation annuelle.', '2-4', 'Procédure, rapport exercice', 'Direction, Logistique', 'CAPA', 8),
    ('5a000005-0000-0000-0000-000000000005', 9, 'Vérification PRP + plan HACCP', 'Vérifications périodiques, validation.', '4-6', 'Rapports vérification', 'Qualité', 'Audit', 9),
    ('5a000005-0000-0000-0000-000000000005', 10, 'Audits internes ISO 22000', 'Programme d''audits internes.', '4-6', 'Rapports audits', 'Auditeurs internes', 'Audit', 10),
    ('5a000005-0000-0000-0000-000000000005', 11, 'Audit de certification', 'Étape 1 (doc) + étape 2 (terrain) par CB accrédité.', '2-4', 'Certificat ISO 22000', 'Organisme certificateur', 'Standards Hub', 11),
    ('5a000005-0000-0000-0000-000000000005', 12, 'Audit de surveillance annuel', NULL, '1', 'Rapport surveillance', 'Organisme certificateur', 'Standards Hub', 12);

INSERT INTO standard_document_templates (standard_id, code, name, obligation, template_uri, description) VALUES
    ('50000005-0000-0000-0000-000000000005', 'ISO22000-PRP', 'Manuel PRP (programmes prérequis)', 'MANDATORY', '/standards/templates/iso-22000/manuel-prp.md', 'Programmes prérequis hygiène'),
    ('50000005-0000-0000-0000-000000000005', 'ISO22000-HACCP', 'Plan HACCP', 'MANDATORY', '/standards/templates/iso-22000/plan-haccp.md', 'Plan HACCP CCP+PRPo'),
    ('50000005-0000-0000-0000-000000000005', 'ISO22000-RAPPEL', 'Procédure retrait/rappel', 'MANDATORY', '/standards/templates/iso-22000/procedure-retrait-rappel.md', 'Procédure de retrait/rappel');

-- ============================================================================
-- DORA — Règlement (UE) 2022/2554 sur la résilience opérationnelle numérique.
-- ============================================================================

INSERT INTO standards (id, code, full_name, publisher, current_version, publication_date,
    family, applicable_industries, description, certification_body_required,
    recertification_cycle_months, related_norm_codes, status, created_at, updated_at)
VALUES (
    '50000006-0000-0000-0000-000000000006',
    'dora',
    'Règlement (UE) 2022/2554 — DORA — Digital Operational Resilience Act',
    'Union européenne',
    '2022',
    '2022-12-14',
    'EU_REGULATION',
    'banking,insurance,finance,fintech,asset_management',
    'Règlement UE imposant aux entités financières et à leurs prestataires TIC tiers critiques un cadre uniforme de résilience opérationnelle numérique : gouvernance, gestion des risques TIC, gestion des incidents, tests de résilience, gestion des risques tiers.',
    FALSE,
    NULL,
    'iso-27001,iso-22301,nis2,basel-iii',
    'PUBLISHED',
    now(), now()
);

INSERT INTO standard_sections (id, standard_id, code, title, description, order_index) VALUES
    ('56010002-0000-0000-0000-000000000006', '50000006-0000-0000-0000-000000000006', 'II', 'Chapitre II — Gestion des risques liés aux TIC', NULL, 2),
    ('56010003-0000-0000-0000-000000000006', '50000006-0000-0000-0000-000000000006', 'III', 'Chapitre III — Gestion des incidents liés aux TIC', NULL, 3),
    ('56010004-0000-0000-0000-000000000006', '50000006-0000-0000-0000-000000000006', 'IV', 'Chapitre IV — Tests de résilience opérationnelle numérique', NULL, 4),
    ('56010005-0000-0000-0000-000000000006', '50000006-0000-0000-0000-000000000006', 'V', 'Chapitre V — Risques liés aux prestataires TIC tiers', NULL, 5),
    ('56010006-0000-0000-0000-000000000006', '50000006-0000-0000-0000-000000000006', 'VI', 'Chapitre VI — Partage d''informations', NULL, 6);

INSERT INTO standard_clauses (id, section_id, code, title, description, order_index) VALUES
    ('56020002-0000-0000-0000-000000000001', '56010002-0000-0000-0000-000000000006', 'Art. 5', 'Cadre de gouvernance et de contrôle interne (TIC)
    ('56020002-0000-0000-0000-000000000002', '56010002-0000-0000-0000-000000000006', 'Art. 6', 'Cadre de gestion du risque lié aux TIC', NULL, 2),
    ('56020002-0000-0000-0000-000000000003', '56010002-0000-0000-0000-000000000006', 'Art. 7', 'Systèmes, protocoles et outils TIC', NULL, 3),
    ('56020002-0000-0000-0000-000000000004', '56010002-0000-0000-0000-000000000006', 'Art. 8', 'Identification — actifs TIC, processus, dépendances', NULL, 4),
    ('56020002-0000-0000-0000-000000000005', '56010002-0000-0000-0000-000000000006', 'Art. 9', 'Protection et prévention', 'Confidentialité, intégrité, disponibilité, authentification, autorisation.', 5),
    ('56020002-0000-0000-0000-000000000006', '56010002-0000-0000-0000-000000000006', 'Art. 10', 'Détection (cybermenaces, anomalies)
    ('56020002-0000-0000-0000-000000000007', '56010002-0000-0000-0000-000000000006', 'Art. 11', 'Réponse et reprise (continuité)
    ('56020002-0000-0000-0000-000000000008', '56010002-0000-0000-0000-000000000006', 'Art. 12', 'Apprentissage et évolution (post-incident)
    ('56020003-0000-0000-0000-000000000001', '56010003-0000-0000-0000-000000000006', 'Art. 17', 'Processus de gestion des incidents TIC', NULL, 1),
    ('56020003-0000-0000-0000-000000000002', '56010003-0000-0000-0000-000000000006', 'Art. 18', 'Classification des incidents', 'Critères : nombre clients impactés, criticité services, durée, impact réputationnel/économique.', 2),
    ('56020003-0000-0000-0000-000000000003', '56010003-0000-0000-0000-000000000006', 'Art. 19', 'Notification aux autorités (incidents majeurs)
    ('56020004-0000-0000-0000-000000000001', '56010004-0000-0000-0000-000000000006', 'Art. 24', 'Programme de tests de résilience opérationnelle numérique', NULL, 1),
    ('56020004-0000-0000-0000-000000000002', '56010004-0000-0000-0000-000000000006', 'Art. 25', 'Tests généraux (annuels)
    ('56020004-0000-0000-0000-000000000003', '56010004-0000-0000-0000-000000000006', 'Art. 26', 'Tests avancés — TLPT (Threat-Led Penetration Testing)
    ('56020005-0000-0000-0000-000000000001', '56010005-0000-0000-0000-000000000006', 'Art. 28', 'Principes généraux — risques prestataires TIC tiers', NULL, 1),
    ('56020005-0000-0000-0000-000000000002', '56010005-0000-0000-0000-000000000006', 'Art. 29', 'Évaluation préliminaire — concentration TIC', NULL, 2),
    ('56020005-0000-0000-0000-000000000003', '56010005-0000-0000-0000-000000000006', 'Art. 30', 'Dispositions contractuelles', 'Clauses minimales obligatoires dans contrats TIC : audit, exit, sécurité, localisation.', 3),
    ('56020005-0000-0000-0000-000000000004', '56010005-0000-0000-0000-000000000006', 'Art. 31', 'Désignation des prestataires TIC tiers critiques', 'Désignation par les ESA, supervision directe.', 4)
;

INSERT INTO standard_requirements (clause_id, code, text, obligation, evidence_types, measurable_criteria, risk_if_missing, order_index) VALUES
    ('56020002-0000-0000-0000-000000000001', 'Art. 5', 'L''organe de direction de l''entité financière définit, approuve, supervise et est responsable de la mise en œuvre de tous les dispositifs liés au cadre de gestion du risque lié aux TIC.', 'MUST', 'Charte de gouvernance TIC, PV organe de direction', 'Cadre approuvé en formelle ≥ 1/an, revue annuelle', 'CRITICAL', 1),
    ('56020002-0000-0000-0000-000000000002', 'Art. 6', 'Les entités financières disposent d''un cadre de gestion du risque lié aux TIC solide, complet et bien documenté.', 'MUST', 'Cadre formalisé, registre des risques TIC', 'Cadre documenté, registre maintenu, revue ≥ 1/an', 'CRITICAL', 1),
    ('56020002-0000-0000-0000-000000000004', 'Art. 8', 'Les entités identifient, classent et documentent toutes les fonctions opérationnelles supportées par les TIC, les rôles, ainsi que les actifs informatiques et leurs interdépendances.', 'MUST', 'Inventaire actifs TIC, cartographie dépendances', 'Inventaire à jour, mise à jour ≥ 1/an, 100% fonctions critiques cartographiées', 'CRITICAL', 1),
    ('56020003-0000-0000-0000-000000000001', 'Art. 17', 'Les entités financières définissent, établissent et mettent en œuvre un processus de gestion des incidents liés aux TIC.', 'MUST', 'Procédure de gestion incidents TIC', 'Procédure formalisée, RACI, tests', 'CRITICAL', 1),
    ('56020003-0000-0000-0000-000000000003', 'Art. 19', 'Les entités financières notifient l''autorité compétente sans retard injustifié l''incident TIC majeur en respectant les délais réglementaires.', 'MUST', 'Procédure notification, registre incidents majeurs', 'Notification initiale ≤ 4h, rapport intermédiaire ≤ 72h, rapport final ≤ 1 mois', 'CRITICAL', 1),
    ('56020004-0000-0000-0000-000000000001', 'Art. 24', 'Les entités financières mettent en place, maintiennent et révisent un programme solide et complet de tests de résilience opérationnelle numérique.', 'MUST', 'Programme de tests, calendrier, rapports', 'Programme révisé ≥ 1/an, exécution selon calendrier', 'CRITICAL', 1),
    ('56020004-0000-0000-0000-000000000003', 'Art. 26', 'Les entités significatives effectuent des tests avancés (TLPT) au moins une fois tous les trois ans.', 'MUST', 'Rapport TLPT, plan d''action', 'TLPT exécuté ≤ 3 ans pour entités significatives, conduit par testeurs accrédités', 'CRITICAL', 1),
    ('56020005-0000-0000-0000-000000000003', 'Art. 30', 'Les contrats entre les entités financières et les prestataires TIC tiers comportent les clauses obligatoires DORA (description services, lieux, sécurité, accès/audit, RTO/RPO, exit).', 'MUST', 'Registre contrats TIC, clauses contractuelles', '100% nouveaux contrats TIC alignés DORA, contrats existants migrés ≤ 17/01/2025', 'CRITICAL', 1);

INSERT INTO standard_certification_paths (id, standard_id, estimated_duration_months_min, estimated_duration_months_max,
    estimated_cost_eur_min, estimated_cost_eur_max, difficulty_level,
    surveillance_audit_frequency, recertification_cycle_years, notes)
VALUES ('5a000006-0000-0000-0000-000000000006', '50000006-0000-0000-0000-000000000006',
    12, 24, 50000, 500000, 5, 'continuous_supervision', 0,
    'Pas de certification au sens ISO. Conformité supervisée en continu par autorités sectorielles (ACPR, BCE, ESMA, EIOPA). Date d''application : 17 janvier 2025.');

INSERT INTO standard_certification_stages (certification_path_id, stage_number, name, description, typical_duration_weeks, deliverables, actors, qualitos_modules, order_index) VALUES
    ('5a000006-0000-0000-0000-000000000006', 1, 'Cadrage DORA + gouvernance', 'Mandat de l''organe de direction, désignation pilote DORA.', '4-6', 'Mandat, charte gouvernance TIC', 'Direction, RSSI, CIO', 'Document Control', 1),
    ('5a000006-0000-0000-0000-000000000006', 2, 'Cartographie actifs & dépendances TIC', 'Inventaire des fonctions critiques + actifs TIC + dépendances tiers.', '6-10', 'Registre actifs, cartographie', 'IT, Risk', 'IoT Connectivity', 2),
    ('5a000006-0000-0000-0000-000000000006', 3, 'Cadre gestion risque TIC (Art. 6-12)', 'Cadre formalisé, KRIs, contrôles.', '6-10', 'Cadre, registre risques TIC', 'Risk Manager', 'Risk/FMEA', 3),
    ('5a000006-0000-0000-0000-000000000006', 4, 'Programme de protection + détection', 'SIEM, EDR, XDR, MFA, segmentation réseau.', '8-16', 'Architecture cible, configurations', 'RSSI, SOC', 'Standards Hub', 4),
    ('5a000006-0000-0000-0000-000000000006', 5, 'Plan de continuité TIC (RTO/RPO)', 'PCA TIC avec exercices.', '6-10', 'PCA, scénarios, exercices', 'Continuité, IT', 'Standards Hub', 5),
    ('5a000006-0000-0000-0000-000000000006', 6, 'Processus gestion incidents + classification', 'Procédure incidents, taxonomie, escalade.', '4-6', 'Procédure, RACI', 'SOC, Compliance', 'CAPA', 6),
    ('5a000006-0000-0000-0000-000000000006', 7, 'Mécanismes notification autorités (≤4h)', 'Templates de notification, runbook', '2-4', 'Templates, runbook, intégration', 'Compliance', 'Document Control', 7),
    ('5a000006-0000-0000-0000-000000000006', 8, 'Programme tests de résilience', 'Plan tests généraux (vulnérabilité, scénarios).', '4-6', 'Programme tests annuel', 'RSSI', 'Standards Hub', 8),
    ('5a000006-0000-0000-0000-000000000006', 9, 'TLPT (entités significatives)', 'Threat-Led Penetration Testing par testeurs accrédités.', '12-26', 'Rapport TLPT, plan d''action', 'Testeurs accrédités', 'Standards Hub', 9),
    ('5a000006-0000-0000-0000-000000000006', 10, 'Registre des tiers TIC + clauses contractuelles', 'Registre + migration clauses DORA dans les contrats.', '8-16', 'Registre tiers TIC, contrats DORA-ready', 'Achats, Juridique', 'Document Control', 10),
    ('5a000006-0000-0000-0000-000000000006', 11, 'Reporting régulateur + supervision continue', 'Reporting périodique ACPR/BCE/ESMA.', 'continu', 'Rapports réglementaires', 'Compliance', 'Document Control', 11),
    ('5a000006-0000-0000-0000-000000000006', 12, 'Cycle amélioration continue', 'Revue annuelle, intégration lessons learned.', 'continu', 'Plans d''amélioration', 'Pilote DORA', 'PDCA', 12);

INSERT INTO standard_document_templates (standard_id, code, name, obligation, template_uri, description) VALUES
    ('50000006-0000-0000-0000-000000000006', 'DORA-CADRE', 'Cadre de gestion du risque TIC (Art. 6)', 'MANDATORY', '/standards/templates/dora/cadre-gestion-risque-tic.md', 'Cadre risque TIC DORA'),
    ('50000006-0000-0000-0000-000000000006', 'DORA-TIERS', 'Registre des prestataires TIC tiers + clauses', 'MANDATORY', '/standards/templates/dora/registre-tiers-tic.md', 'Registre + clauses Art. 30'),
    ('50000006-0000-0000-0000-000000000006', 'DORA-INC', 'Procédure de gestion et notification des incidents TIC', 'MANDATORY', '/standards/templates/dora/procedure-incidents-tic.md', 'Procédure incidents Art. 17-19');
