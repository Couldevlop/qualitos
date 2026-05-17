-- P4 — IMS (Integrated Management System) multi-norme (CLAUDE.md §8.9).
-- Permet de mutualiser les preuves entre normes HLS (ISO 9001 / 14001 / 45001 / 22301 / 27001).
-- Table de mapping bidirectionnelle clause ↔ clause.

CREATE TABLE norm_clause_mapping (
    id                      UUID         NOT NULL DEFAULT gen_random_uuid(),
    source_standard_code    VARCHAR(100) NOT NULL,
    source_clause_code      VARCHAR(30)  NOT NULL,
    target_standard_code    VARCHAR(100) NOT NULL,
    target_clause_code      VARCHAR(30)  NOT NULL,
    relation_type           VARCHAR(20)  NOT NULL,
    confidence              INTEGER      NOT NULL DEFAULT 100,
    notes                   TEXT,
    created_at              TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT pk_norm_clause_mapping PRIMARY KEY (id),
    CONSTRAINT uk_norm_clause_mapping
        UNIQUE (source_standard_code, source_clause_code, target_standard_code, target_clause_code),
    CONSTRAINT chk_norm_clause_mapping_type
        CHECK (relation_type IN ('EQUIVALENT','COVERS','RELATED','REFERENCES')),
    CONSTRAINT chk_norm_clause_mapping_confidence
        CHECK (confidence BETWEEN 0 AND 100),
    CONSTRAINT chk_norm_clause_mapping_no_self_loop
        CHECK (NOT (source_standard_code = target_standard_code
                    AND source_clause_code = target_clause_code))
);

CREATE INDEX idx_norm_clause_mapping_source
    ON norm_clause_mapping (source_standard_code, source_clause_code);
CREATE INDEX idx_norm_clause_mapping_target
    ON norm_clause_mapping (target_standard_code, target_clause_code);
CREATE INDEX idx_norm_clause_mapping_relation
    ON norm_clause_mapping (relation_type);

COMMENT ON TABLE norm_clause_mapping IS
    'IMS — mutualisation des preuves entre normes (CLAUDE.md §8.9).';
COMMENT ON COLUMN norm_clause_mapping.relation_type IS
    'EQUIVALENT = mêmes exigences (HLS) ; COVERS = la source couvre la cible ; RELATED = liens partiels ; REFERENCES = cite mais ne couvre pas.';

-- ============================================================================
-- Seed des correspondances HLS (High Level Structure) :
-- ISO 9001 / 14001 / 45001 / 22301 / 27001 — clauses §4 à §10.
--
-- L'HLS impose une structure commune à toutes les normes systèmes de management
-- ISO post-2012. Ces correspondances sont publiées par l'ISO et reconnues
-- universellement (cf. Annexe SL).
-- ============================================================================

-- §4.1 Compréhension de l'organisme et de son contexte
INSERT INTO norm_clause_mapping (source_standard_code, source_clause_code, target_standard_code, target_clause_code, relation_type, confidence, notes) VALUES
    ('iso-9001', '4.1', 'iso-14001',   '4.1', 'EQUIVALENT', 100, 'HLS Annexe SL §4.1 — contexte'),
    ('iso-9001', '4.1', 'iso-45001',   '4.1', 'EQUIVALENT', 100, 'HLS Annexe SL §4.1 — contexte'),
    ('iso-9001', '4.1', 'iso-22301',   '4.1', 'EQUIVALENT', 100, 'HLS Annexe SL §4.1 — contexte'),
    ('iso-9001', '4.1', 'iso-27001',   '4.1', 'EQUIVALENT', 100, 'HLS Annexe SL §4.1 — contexte'),
    ('iso-14001','4.1', 'iso-45001',   '4.1', 'EQUIVALENT', 100, 'HLS — contexte'),
    ('iso-14001','4.1', 'iso-27001',   '4.1', 'EQUIVALENT', 100, 'HLS — contexte'),
    ('iso-45001','4.1', 'iso-27001',   '4.1', 'EQUIVALENT', 100, 'HLS — contexte'),
    ('iso-22301','4.1', 'iso-27001',   '4.1', 'EQUIVALENT', 100, 'HLS — contexte');

-- §4.2 Compréhension des besoins et attentes des parties intéressées
INSERT INTO norm_clause_mapping (source_standard_code, source_clause_code, target_standard_code, target_clause_code, relation_type, confidence, notes) VALUES
    ('iso-9001', '4.2', 'iso-14001', '4.2', 'EQUIVALENT', 100, 'HLS — parties intéressées'),
    ('iso-9001', '4.2', 'iso-45001', '4.2', 'EQUIVALENT', 100, 'HLS — parties intéressées'),
    ('iso-9001', '4.2', 'iso-27001', '4.2', 'EQUIVALENT', 100, 'HLS — parties intéressées'),
    ('iso-9001', '4.2', 'iso-22301', '4.2', 'EQUIVALENT', 100, 'HLS — parties intéressées'),
    ('iso-14001','4.2', 'iso-45001', '4.2', 'EQUIVALENT', 100, 'HLS — parties intéressées'),
    ('iso-27001','4.2', 'iso-22301', '4.2', 'EQUIVALENT', 100, 'HLS — parties intéressées');

-- §5.1 Leadership et engagement
INSERT INTO norm_clause_mapping (source_standard_code, source_clause_code, target_standard_code, target_clause_code, relation_type, confidence, notes) VALUES
    ('iso-9001', '5.1', 'iso-14001', '5.1', 'EQUIVALENT', 100, 'HLS — leadership'),
    ('iso-9001', '5.1', 'iso-45001', '5.1', 'EQUIVALENT', 100, 'HLS — leadership'),
    ('iso-9001', '5.1', 'iso-22301', '5.1', 'EQUIVALENT', 100, 'HLS — leadership'),
    ('iso-9001', '5.1', 'iso-27001', '5.1', 'EQUIVALENT', 100, 'HLS — leadership');

-- §5.2 Politique
INSERT INTO norm_clause_mapping (source_standard_code, source_clause_code, target_standard_code, target_clause_code, relation_type, confidence, notes) VALUES
    ('iso-9001', '5.2', 'iso-14001', '5.2', 'EQUIVALENT', 95, 'HLS — politique (contenu spécifique par norme)'),
    ('iso-9001', '5.2', 'iso-45001', '5.2', 'EQUIVALENT', 95, 'HLS — politique'),
    ('iso-9001', '5.2', 'iso-22301', '5.2', 'EQUIVALENT', 95, 'HLS — politique'),
    ('iso-9001', '5.2', 'iso-27001', '5.2', 'EQUIVALENT', 95, 'HLS — politique');

-- §5.3 Rôles, responsabilités et autorités organisationnels
INSERT INTO norm_clause_mapping (source_standard_code, source_clause_code, target_standard_code, target_clause_code, relation_type, confidence, notes) VALUES
    ('iso-9001', '5.3', 'iso-14001', '5.3', 'EQUIVALENT', 100, 'HLS — rôles'),
    ('iso-9001', '5.3', 'iso-45001', '5.3', 'EQUIVALENT', 100, 'HLS — rôles'),
    ('iso-9001', '5.3', 'iso-22301', '5.3', 'EQUIVALENT', 100, 'HLS — rôles'),
    ('iso-9001', '5.3', 'iso-27001', '5.3', 'EQUIVALENT', 100, 'HLS — rôles');

-- §9.2 Audit interne
INSERT INTO norm_clause_mapping (source_standard_code, source_clause_code, target_standard_code, target_clause_code, relation_type, confidence, notes) VALUES
    ('iso-9001', '9.2',   'iso-14001', '9.2',   'EQUIVALENT', 100, 'HLS — audit interne'),
    ('iso-9001', '9.2',   'iso-45001', '9.2',   'EQUIVALENT', 100, 'HLS — audit interne'),
    ('iso-9001', '9.2',   'iso-22301', '9.2',   'EQUIVALENT', 100, 'HLS — audit interne'),
    ('iso-9001', '9.2',   'iso-27001', '9.2',   'EQUIVALENT', 100, 'HLS — audit interne'),
    ('iso-9001', '9.2.1', 'iso-14001', '9.2.1', 'EQUIVALENT', 100, 'HLS — audit interne'),
    ('iso-9001', '9.2.1', 'iso-45001', '9.2.1', 'EQUIVALENT', 100, 'HLS — audit interne'),
    ('iso-9001', '9.2.1', 'iso-27001', '9.2.1', 'EQUIVALENT', 100, 'HLS — audit interne'),
    ('iso-14001','9.2',   'iso-45001', '9.2',   'EQUIVALENT', 100, 'HLS — audit interne'),
    ('iso-14001','9.2',   'iso-27001', '9.2',   'EQUIVALENT', 100, 'HLS — audit interne');

-- §9.3 Revue de direction
INSERT INTO norm_clause_mapping (source_standard_code, source_clause_code, target_standard_code, target_clause_code, relation_type, confidence, notes) VALUES
    ('iso-9001', '9.3',   'iso-14001', '9.3',   'EQUIVALENT', 100, 'HLS — revue direction'),
    ('iso-9001', '9.3',   'iso-45001', '9.3',   'EQUIVALENT', 100, 'HLS — revue direction'),
    ('iso-9001', '9.3',   'iso-22301', '9.3',   'EQUIVALENT', 100, 'HLS — revue direction'),
    ('iso-9001', '9.3',   'iso-27001', '9.3',   'EQUIVALENT', 100, 'HLS — revue direction'),
    ('iso-9001', '9.3.1', 'iso-14001', '9.3',   'EQUIVALENT', 95,  'HLS — revue direction'),
    ('iso-9001', '9.3.1', 'iso-45001', '9.3',   'EQUIVALENT', 95,  'HLS — revue direction'),
    ('iso-9001', '9.3.1', 'iso-27001', '9.3',   'EQUIVALENT', 95,  'HLS — revue direction'),
    ('iso-14001','9.3',   'iso-45001', '9.3',   'EQUIVALENT', 100, 'HLS — revue direction'),
    ('iso-14001','9.3',   'iso-27001', '9.3',   'EQUIVALENT', 100, 'HLS — revue direction'),
    ('iso-45001','9.3',   'iso-27001', '9.3',   'EQUIVALENT', 100, 'HLS — revue direction');

-- §10.2 Non-conformité et action corrective
INSERT INTO norm_clause_mapping (source_standard_code, source_clause_code, target_standard_code, target_clause_code, relation_type, confidence, notes) VALUES
    ('iso-9001', '10.2',   'iso-14001', '10.2',   'EQUIVALENT', 100, 'HLS — NC & actions correctives'),
    ('iso-9001', '10.2',   'iso-45001', '10.2',   'EQUIVALENT', 100, 'HLS — NC & actions correctives'),
    ('iso-9001', '10.2',   'iso-22301', '10.2',   'EQUIVALENT', 100, 'HLS — NC & actions correctives'),
    ('iso-9001', '10.2',   'iso-27001', '10.2',   'EQUIVALENT', 100, 'HLS — NC & actions correctives'),
    ('iso-9001', '10.2.1', 'iso-14001', '10.2',   'EQUIVALENT', 95,  'HLS — NC'),
    ('iso-9001', '10.2.1', 'iso-45001', '10.2',   'EQUIVALENT', 95,  'HLS — NC'),
    ('iso-9001', '10.2.1', 'iso-27001', '10.2',   'EQUIVALENT', 95,  'HLS — NC'),
    ('iso-14001','10.2',   'iso-45001', '10.2',   'EQUIVALENT', 100, 'HLS — NC & actions correctives'),
    ('iso-14001','10.2',   'iso-27001', '10.2',   'EQUIVALENT', 100, 'HLS — NC & actions correctives'),
    ('iso-45001','10.2',   'iso-27001', '10.2',   'EQUIVALENT', 100, 'HLS — NC & actions correctives');

-- Liaisons IATF/AS9100 vers ISO 9001 (ces normes sont surcouches d'ISO 9001).
INSERT INTO norm_clause_mapping (source_standard_code, source_clause_code, target_standard_code, target_clause_code, relation_type, confidence, notes) VALUES
    ('iatf-16949', '4.4.1.2',   'iso-9001', '4.4',   'COVERS', 90, 'IATF étend §4.4 ISO 9001 — sécurité produit auto'),
    ('iatf-16949', '8.5.1.1',   'iso-9001', '8.5.1', 'COVERS', 95, 'Plan de contrôle = extension du §8.5.1'),
    ('iatf-16949', '10.2.3',    'iso-9001', '10.2',  'COVERS', 95, '8D = méthode renforcée pour NC clients'),
    ('as9100',     '4.4.2',     'iso-9001', '4.4',   'COVERS', 90, 'AS9100 ajoute exigence sécurité produit'),
    ('as9100',     '8.1.2',     'iso-9001', '8.4',   'COVERS', 90, 'Prévention contrefaçon = extension §8.4 (achats)'),
    ('as9100',     '8.5.1.3',   'iso-9001', '8.5.1', 'COVERS', 95, 'FAI = extension §8.5.1');

-- ISO 13485 vers ISO 9001 (DM est aligné HLS mais avec exigences spécifiques).
INSERT INTO norm_clause_mapping (source_standard_code, source_clause_code, target_standard_code, target_clause_code, relation_type, confidence, notes) VALUES
    ('iso-13485', '4.1.1',  'iso-9001', '4.4',   'COVERS', 85, 'SMQ DM couvre §4.4 ISO 9001 + spécificités réglementaires'),
    ('iso-13485', '4.1.6',  'iso-9001', '7.1.3', 'COVERS', 80, 'Validation logiciels SMQ — recouvre §7.1.3 partiellement'),
    ('iso-13485', '7.3.7',  'iso-9001', '8.3.4', 'COVERS', 85, 'Transfert de conception = extension §8.3'),
    ('iso-13485', '8.2.1',  'iso-9001', '9.1.2', 'COVERS', 85, 'PMS = vigilance étendant §9.1.2 satisfaction client');

-- DORA ↔ ISO 27001 (très fortement liés sur ICT)
INSERT INTO norm_clause_mapping (source_standard_code, source_clause_code, target_standard_code, target_clause_code, relation_type, confidence, notes) VALUES
    ('dora', 'Art. 6',  'iso-27001', '6.1',   'RELATED',    85, 'Cadre risque TIC ≈ traitement du risque SMSI'),
    ('dora', 'Art. 8',  'iso-27001', '8.1',   'RELATED',    90, 'Inventaire actifs TIC ≈ planification opérationnelle SMSI + Annexe A.8'),
    ('dora', 'Art. 9',  'iso-27001', '8.1',   'RELATED',    90, 'Protection & prévention ≈ Annexe A controls'),
    ('dora', 'Art. 11', 'iso-22301', '8.4',   'RELATED',    95, 'Réponse & reprise ≈ stratégie de continuité ISO 22301'),
    ('dora', 'Art. 17', 'iso-27001', '5.24',  'COVERS',     90, 'Gestion incidents TIC = Annexe A.5.24 ISO 27001:2022'),
    ('dora', 'Art. 19', 'iso-27001', '5.25',  'COVERS',     85, 'Notification incidents ≈ A.5.25 évaluation'),
    ('dora', 'Art. 30', 'iso-27001', '5.19',  'COVERS',     90, 'Clauses prestataires TIC ≈ A.5.19 fournisseurs');
