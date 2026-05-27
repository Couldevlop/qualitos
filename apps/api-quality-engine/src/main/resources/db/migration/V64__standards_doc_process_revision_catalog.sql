-- Standards Hub §8.4 — complète le catalogue d'une norme avec :
--   onglet 3 « Bibliothèque documentaire »  → standard_document_templates (existe déjà, V52) + colonnes
--   onglet 4 « Cartographie des processus » → standard_process_templates (nouvelle table)
--   onglet 8 « Veille normative »            → standard_revisions (nouvelle table)
-- Platform-level (rattaché à standards, pas de tenant_id). Seed ISO 9001 (norme démo).
-- Les normes P4 (IATF, AS9100, ISO 13485…) ont déjà leurs document_templates (V52).

-- ============================================================================
-- 1. Enrichir la table des modèles de documents (catégorie, clauses, ordre)
-- ============================================================================
ALTER TABLE standard_document_templates ADD COLUMN IF NOT EXISTS category        VARCHAR(40);
ALTER TABLE standard_document_templates ADD COLUMN IF NOT EXISTS maps_to_clauses TEXT;
ALTER TABLE standard_document_templates ADD COLUMN IF NOT EXISTS order_index     INTEGER;

-- ============================================================================
-- 2. Cartographie des processus types (§8.3 processes_required)
-- ============================================================================
CREATE TABLE IF NOT EXISTS standard_process_templates (
    id              UUID         NOT NULL DEFAULT gen_random_uuid(),
    standard_id     UUID         NOT NULL,
    code            VARCHAR(50)  NOT NULL,
    name            VARCHAR(500) NOT NULL,
    description     TEXT,
    maps_to_clauses TEXT,
    bpmn_uri        VARCHAR(1024),
    order_index     INTEGER      NOT NULL,

    CONSTRAINT pk_standard_process_templates PRIMARY KEY (id),
    CONSTRAINT fk_spt_standard FOREIGN KEY (standard_id)
        REFERENCES standards (id) ON DELETE CASCADE,
    CONSTRAINT uk_spt_standard_code UNIQUE (standard_id, code)
);
CREATE INDEX IF NOT EXISTS idx_spt_standard ON standard_process_templates (standard_id);

-- ============================================================================
-- 3. Veille normative (§8.10)
-- ============================================================================
CREATE TABLE IF NOT EXISTS standard_revisions (
    id              UUID         NOT NULL DEFAULT gen_random_uuid(),
    standard_id     UUID         NOT NULL,
    version         VARCHAR(60)  NOT NULL,
    status          VARCHAR(20)  NOT NULL,
    published_date  DATE,
    effective_date  DATE,
    summary         TEXT,
    impact_note     TEXT,
    source_url      VARCHAR(1024),
    order_index     INTEGER      NOT NULL,

    CONSTRAINT pk_standard_revisions PRIMARY KEY (id),
    CONSTRAINT fk_srev_standard FOREIGN KEY (standard_id)
        REFERENCES standards (id) ON DELETE CASCADE,
    CONSTRAINT uk_srev_standard_version UNIQUE (standard_id, version),
    CONSTRAINT chk_srev_status CHECK (status IN ('CURRENT','PLANNED','SUPERSEDED'))
);
CREATE INDEX IF NOT EXISTS idx_srev_standard ON standard_revisions (standard_id);

-- ============================================================================
-- 4. Seed ISO 9001 — bibliothèque documentaire (modèles .md créés en resources)
-- ============================================================================
INSERT INTO standard_document_templates
    (standard_id, code, name, obligation, template_uri, description, category, maps_to_clauses, order_index)
SELECT s.id, v.code, v.name, v.obl, v.uri, v.descr, v.cat, v.clauses, v.ord
FROM standards s
CROSS JOIN (VALUES
    ('ISO9001-MQ',        'Manuel Qualité',
        'RECOMMENDED', '/standards/templates/iso-9001/manuel-qualite.md',
        'Manuel qualité structurant le SMQ (non obligatoire en 2015, recommandé).', 'MANUAL', '4,5', 1),
    ('ISO9001-POL-Q',     'Politique Qualité',
        'MANDATORY', '/standards/templates/iso-9001/politique-qualite.md',
        'Politique qualité signée par la direction.', 'POLICY', '5.2', 2),
    ('ISO9001-PROC-DOC',  'Procédure — Maîtrise des informations documentées',
        'RECOMMENDED', '/standards/templates/iso-9001/procedure-maitrise-informations-documentees.md',
        'Création, approbation, diffusion, accès, conservation des documents.', 'PROCEDURE', '7.5', 3),
    ('ISO9001-PROC-AUDIT','Procédure — Audit interne',
        'RECOMMENDED', '/standards/templates/iso-9001/procedure-audit-interne.md',
        'Programme, conduite et suites des audits internes (ISO 19011).', 'PROCEDURE', '9.2', 4),
    ('ISO9001-PROC-AC',   'Procédure — Non-conformités & actions correctives',
        'RECOMMENDED', '/standards/templates/iso-9001/procedure-actions-correctives.md',
        'Traitement des NC, analyse de causes, efficacité (module CAPA).', 'PROCEDURE', '10.2', 5),
    ('ISO9001-PV-RD',     'Compte-rendu — Revue de direction',
        'RECOMMENDED', '/standards/templates/iso-9001/pv-revue-direction.md',
        'Trame d''enregistrement de la revue de direction.', 'RECORD', '9.3', 6)
) AS v(code, name, obl, uri, descr, cat, clauses, ord)
WHERE s.code = 'iso-9001';

-- ============================================================================
-- 5. Seed ISO 9001 — cartographie des processus types
-- ============================================================================
INSERT INTO standard_process_templates
    (standard_id, code, name, description, maps_to_clauses, order_index)
SELECT s.id, v.code, v.name, v.descr, v.clauses, v.ord
FROM standards s
CROSS JOIN (VALUES
    ('PROC-DIRE',  'Processus de direction (revue de direction)', 'Pilotage stratégique, revue de direction, allocation des ressources.', '5.1,9.3', 1),
    ('PROC-RISK',  'Maîtrise des risques & opportunités',         'Identification, évaluation et traitement des risques et opportunités.', '6.1', 2),
    ('PROC-DOC',   'Maîtrise documentaire',                       'Gestion des informations documentées (création, diffusion, conservation).', '7.5', 3),
    ('PROC-COMP',  'Gestion des compétences',                     'Détermination, évaluation et développement des compétences.', '7.2,7.3', 4),
    ('PROC-ACHAT', 'Maîtrise des prestataires externes',          'Évaluation, sélection et surveillance des fournisseurs/sous-traitants.', '8.4', 5),
    ('PROC-PROD',  'Production & prestation de service',          'Réalisation en conditions maîtrisées, identification et traçabilité.', '8.1,8.5', 6),
    ('PROC-LIB',   'Libération des produits & services',          'Vérification de la conformité avant livraison.', '8.6', 7),
    ('PROC-NC',    'Maîtrise des sorties non conformes',          'Identification, isolement et traitement des produits non conformes.', '8.7', 8),
    ('PROC-SAT',   'Surveillance, mesure & satisfaction client',  'Indicateurs, satisfaction client, analyse des données.', '9.1', 9),
    ('PROC-AUDIT', 'Audit interne',                               'Programmation et réalisation des audits internes.', '9.2', 10),
    ('PROC-AC',    'Actions correctives & amélioration',          'Traitement des NC, actions correctives, amélioration continue.', '10.1,10.2', 11)
) AS v(code, name, descr, clauses, ord)
WHERE s.code = 'iso-9001';

-- ============================================================================
-- 6. Seed ISO 9001 — veille normative
-- ============================================================================
INSERT INTO standard_revisions
    (standard_id, version, status, published_date, effective_date, summary, impact_note, source_url, order_index)
SELECT s.id, v.version, v.status, v.pub, v.eff, v.summary, v.impact, v.url, v.ord
FROM standards s
CROSS JOIN (VALUES
    ('2008', 'SUPERSEDED', DATE '2008-11-15', DATE '2008-11-15',
        'Version antérieure, organisée par exigences (8 chapitres). Remplacée par la version 2015.',
        'Migration achevée. Conservée pour historique.', 'https://www.iso.org/standard/46486.html', 1),
    ('2015', 'CURRENT', DATE '2015-09-15', DATE '2015-09-15',
        'Structure de haut niveau (HLS), approche par les risques, leadership et contexte renforcés.',
        'Version en vigueur — référentiel actif du SMQ.', 'https://www.iso.org/standard/62085.html', 2),
    ('Amendement 2024 (climat)', 'CURRENT', DATE '2024-02-23', DATE '2024-02-23',
        'Amendement « Climate Action » : ajout de la prise en compte du changement climatique dans le contexte (§4.1) et les parties intéressées (§4.2).',
        'À intégrer : déterminer si le changement climatique est un enjeu pertinent et l''attente des parties intéressées.', 'https://www.iso.org/contents/news/2024/02/iso-9001-climate-action.html', 3),
    ('2025-2026 (planifiée)', 'PLANNED', NULL, NULL,
        'Révision majeure attendue (~2025-2026) : modernisation, numérique, alignement HLS révisé.',
        'Surveiller : anticiper les écarts probables (numérique, données, durabilité) pour lisser la transition.', 'https://www.iso.org/standard/iso-9001', 4)
) AS v(version, status, pub, eff, summary, impact, url, ord)
WHERE s.code = 'iso-9001';
