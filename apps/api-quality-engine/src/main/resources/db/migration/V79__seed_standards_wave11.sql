-- Standards Hub — vague 11 (CLAUDE.md §8.2) : 5 référentiels finance / régulation / cybersécurité.
--   bale-iii       : Bâle III/IV via CRR3 (UE) 2024 + CRD VI — exigences prudentielles bancaires
--   solvabilite-ii : Directive Solvabilité II 2009/138/CE (révision 2025) — assurance
--   mifid-ii       : Directive MiFID II 2014/65/UE + MiFIR — marchés financiers
--   lcb-ft         : LCB-FT (paquet AML UE : AMLR/AMLD6 2024, AMLA) — anti-blanchiment
--   nist-csf       : NIST Cybersecurity Framework 2.0 (2024) — cadre cybersécurité (6 fonctions)
-- Même format que V78 : catalogue platform-level, UUID déterministes, préfixe '6' (vague 11).
--   standards '6000000n' (n=1..5), sections '6n00000s' (préfixe 61..65 = norme),
--   clauses '6a/6b/6c/6d' (normes 1..4) + '69' (5e norme : nist-csf),
--   paths '6e00000n', stages via FK (sans UUID explicite).
-- Note unicité : d'anciennes migrations utilisent le préfixe '6' (V14 : '61111111-1111…',
--   '66666666…' ; V59 : '64444444…', '65555555…') — ce sont des motifs à chiffres répétés,
--   distincts de notre schéma structuré (standards 6000000n, sections 6n00000s,
--   clauses 6a/6b/6c/6d/69, paths 6e00000n). Le contrôle CROSS-FICHIER (validate_v79.py)
--   vérifie l'absence de collision sur l'ensemble des V*.sql — 0 collision.

-- ============================================================================
-- Bâle III / IV — CRR3 (Règlement (UE) 2024/1623) + CRD VI — exigences prudentielles
-- ============================================================================

INSERT INTO standards (id, code, full_name, publisher, current_version, publication_date,
    family, applicable_industries, description, certification_body_required,
    recertification_cycle_months, related_norm_codes, status, created_at, updated_at)
VALUES (
    '60000001-0000-0000-0000-000000000001',
    'bale-iii',
    'Bâle III/IV — Règlement (UE) 2024/1623 (CRR3) et Directive CRD VI — exigences prudentielles applicables aux établissements de crédit',
    'Comité de Bâle/UE',
    'CRR3 2024/1623',
    DATE '2024-06-19',
    'REGULATORY',
    'finance',
    'Le cadre de Bâle III, finalisé par les accords dits « Bâle IV » et transposé dans l''Union européenne par le règlement CRR3 (UE) 2024/1623 et la directive CRD VI, fixe les exigences prudentielles des établissements de crédit. Il impose un ratio de fonds propres de base de catégorie 1 (CET1) d''au moins 4,5 % des actifs pondérés par les risques (RWA), complété de coussins (conservation, contracyclique, systémique), un ratio de levier d''au moins 3 %, un ratio de liquidité à court terme (LCR) d''au moins 100 % et un ratio de financement stable net (NSFR) d''au moins 100 %. Bâle IV renforce le calcul du risque opérationnel (méthode standard SMA), encadre les modèles internes par un plancher de production (output floor) fixé à 72,5 % des exigences calculées en méthode standard, et impose un reporting prudentiel harmonisé (COREP/FINREP). Il ne s''agit pas d''une certification accréditée : la conformité relève de la SUPERVISION PRUDENTIELLE exercée par la BCE (MSU) et les autorités nationales compétentes (l''ACPR en France) — nuance importante par rapport à une certification de système de management.',
    FALSE,
    NULL,
    'dora',
    'PUBLISHED', now(), now()
);

INSERT INTO standard_sections (id, standard_id, code, title, description, order_index) VALUES
    ('6100000a-0000-0000-0000-000000000001', '60000001-0000-0000-0000-000000000001', 'CAP', 'Fonds propres & coussins', 'Exigences de fonds propres (CET1) et coussins de fonds propres.', 1),
    ('6100000a-0000-0000-0000-000000000002', '60000001-0000-0000-0000-000000000001', 'LEV', 'Ratio de levier', 'Exigence de ratio de levier indépendante du risque.', 2),
    ('6100000a-0000-0000-0000-000000000003', '60000001-0000-0000-0000-000000000001', 'LIQ', 'Liquidité (LCR/NSFR)', 'Ratios de liquidité à court terme (LCR) et de financement stable (NSFR).', 3),
    ('6100000a-0000-0000-0000-000000000004', '60000001-0000-0000-0000-000000000001', 'RISK', 'Risques & output floor', 'Risque opérationnel (SMA) et plancher de production des modèles internes.', 4),
    ('6100000a-0000-0000-0000-000000000005', '60000001-0000-0000-0000-000000000001', 'REP', 'Reporting prudentiel', 'Reporting prudentiel harmonisé (COREP/FINREP) et gouvernance des données.', 5);

INSERT INTO standard_clauses (id, section_id, code, title, description, order_index) VALUES
    ('6a000001-0000-0000-0000-000000000001', '6100000a-0000-0000-0000-000000000001', 'CAP.CET1', 'Ratio CET1', 'Maintien d''un ratio de fonds propres de base de catégorie 1 (CET1).', 1),
    ('6a000001-0000-0000-0000-000000000002', '6100000a-0000-0000-0000-000000000001', 'CAP.BUF', 'Coussins de fonds propres', 'Constitution des coussins (conservation, contracyclique, systémique).', 2),
    ('6a000001-0000-0000-0000-000000000003', '6100000a-0000-0000-0000-000000000002', 'LEV.R', 'Ratio de levier', 'Maintien d''un ratio de levier au moins égal à 3 %.', 3),
    ('6a000001-0000-0000-0000-000000000004', '6100000a-0000-0000-0000-000000000003', 'LIQ.LCR', 'Ratio LCR', 'Maintien d''un ratio de liquidité à court terme (LCR) au moins égal à 100 %.', 4),
    ('6a000001-0000-0000-0000-000000000005', '6100000a-0000-0000-0000-000000000003', 'LIQ.NSFR', 'Ratio NSFR', 'Maintien d''un ratio de financement stable net (NSFR) au moins égal à 100 %.', 5),
    ('6a000001-0000-0000-0000-000000000006', '6100000a-0000-0000-0000-000000000004', 'RISK.OP', 'Risque opérationnel (SMA)', 'Calcul de l''exigence pour risque opérationnel selon la méthode standard (SMA).', 6),
    ('6a000001-0000-0000-0000-000000000007', '6100000a-0000-0000-0000-000000000004', 'RISK.OF', 'Output floor', 'Application du plancher de production (output floor) de 72,5 %.', 7),
    ('6a000001-0000-0000-0000-000000000008', '6100000a-0000-0000-0000-000000000005', 'REP.PRU', 'Reporting COREP/FINREP', 'Production du reporting prudentiel harmonisé (COREP/FINREP).', 8),
    ('6a000001-0000-0000-0000-000000000009', '6100000a-0000-0000-0000-000000000005', 'REP.GOV', 'Gouvernance des risques', 'Dispositif de gouvernance et de contrôle interne des risques.', 9);

INSERT INTO standard_requirements (clause_id, code, text, obligation, evidence_types, measurable_criteria, risk_if_missing, order_index) VALUES
    ('6a000001-0000-0000-0000-000000000001', 'CAP.CET1.1', 'L''établissement doit maintenir en permanence un ratio de fonds propres de base de catégorie 1 (CET1) au moins égal à 4,5 % des actifs pondérés par les risques.', 'MUST', 'KPI_RECORD,DOCUMENT', 'Ratio CET1 ≥ 4,5 % des RWA mesuré et suivi en continu, marge de gestion documentée', 'CRITICAL', 1),
    ('6a000001-0000-0000-0000-000000000002', 'CAP.BUF.1', 'L''établissement doit constituer les coussins de fonds propres requis (conservation, contracyclique et systémique le cas échéant).', 'MUST', 'KPI_RECORD,DOCUMENT', 'Coussins de fonds propres calculés et constitués ; exigence globale de coussins (CBR) respectée', 'HIGH', 2),
    ('6a000001-0000-0000-0000-000000000003', 'LEV.R.1', 'L''établissement doit maintenir un ratio de levier au moins égal à 3 % des expositions totales.', 'MUST', 'KPI_RECORD,DOCUMENT', 'Ratio de levier ≥ 3 % calculé et suivi, exposition totale (mesure de l''exposition) tracée', 'CRITICAL', 3),
    ('6a000001-0000-0000-0000-000000000004', 'LIQ.LCR.1', 'L''établissement doit maintenir un ratio de liquidité à court terme (LCR) au moins égal à 100 %.', 'MUST', 'KPI_RECORD,DOCUMENT', 'Ratio LCR ≥ 100 % mesuré quotidiennement, actifs liquides de haute qualité (HQLA) tracés', 'CRITICAL', 4),
    ('6a000001-0000-0000-0000-000000000005', 'LIQ.NSFR.1', 'L''établissement doit maintenir un ratio de financement stable net (NSFR) au moins égal à 100 %.', 'MUST', 'KPI_RECORD,DOCUMENT', 'Ratio NSFR ≥ 100 % calculé et suivi, financement stable disponible et requis documentés', 'HIGH', 5),
    ('6a000001-0000-0000-0000-000000000006', 'RISK.OP.1', 'L''établissement doit calculer son exigence de fonds propres pour risque opérationnel selon la méthode standard (SMA).', 'MUST', 'DOCUMENT,KPI_RECORD', 'Exigence risque opérationnel calculée selon la SMA ; composante de l''indicateur d''activité tracée', 'HIGH', 6),
    ('6a000001-0000-0000-0000-000000000007', 'RISK.OF.1', 'L''établissement utilisant des modèles internes doit appliquer le plancher de production (output floor) de 72,5 % des exigences en méthode standard.', 'MUST', 'DOCUMENT,KPI_RECORD', 'Output floor à 72,5 % appliqué ; comparaison modèles internes vs méthode standard documentée', 'HIGH', 7),
    ('6a000001-0000-0000-0000-000000000008', 'REP.PRU.1', 'L''établissement doit produire et transmettre le reporting prudentiel harmonisé (COREP/FINREP) à l''autorité de supervision selon la périodicité requise.', 'MUST', 'DOCUMENT,AUDIT', 'États COREP/FINREP produits et transmis dans les délais réglementaires ; qualité des données contrôlée', 'CRITICAL', 8),
    ('6a000001-0000-0000-0000-000000000009', 'REP.GOV.1', 'L''établissement doit mettre en place un dispositif de gouvernance et de contrôle interne des risques couvrant l''appétit pour le risque et la fonction de gestion des risques.', 'MUST', 'DOCUMENT,AUDIT', 'Cadre d''appétit pour le risque approuvé ; fonction de gestion des risques indépendante en place', 'HIGH', 9);

INSERT INTO standard_certification_paths (id, standard_id, estimated_duration_months_min, estimated_duration_months_max,
    estimated_cost_eur_min, estimated_cost_eur_max, difficulty_level,
    surveillance_audit_frequency, recertification_cycle_years, notes)
VALUES ('6e000001-0000-0000-0000-000000000001', '60000001-0000-0000-0000-000000000001',
    12, 24, 100000, 1000000, 5, 'continuous', NULL,
    'Cadre prudentiel obligatoire : la conformité ne donne pas lieu à une certification accréditée mais à une SUPERVISION PRUDENTIELLE permanente exercée par la BCE (MSU) et les autorités nationales compétentes (ACPR en France), via le processus de surveillance et d''évaluation prudentielle (SREP) et le reporting périodique COREP/FINREP — d''où l''absence de cycle de recertification au sens classique. Synergie avec les modules Risk (appétit pour le risque, RWA, risque opérationnel), KPI (ratios CET1, levier, LCR, NSFR), Document Control (politiques, états réglementaires) et Audit (contrôle interne) de QualitOS.');

INSERT INTO standard_certification_stages (certification_path_id, stage_number, name, description, typical_duration_weeks, deliverables, actors, qualitos_modules, order_index) VALUES
    ('6e000001-0000-0000-0000-000000000001', 1, 'Cadrage prudentiel & gouvernance', 'Définition du périmètre prudentiel, de l''appétit pour le risque et de la gouvernance des risques.', '4-8', 'Cadre d''appétit pour le risque, organisation de la fonction risques', 'Direction, Risk Manager', 'Document Control, Risk', 1),
    ('6e000001-0000-0000-0000-000000000001', 2, 'Mesure des fonds propres & RWA', 'Calcul des fonds propres réglementaires et des actifs pondérés par les risques (RWA).', '6-12', 'États de fonds propres, calcul des RWA', 'Direction financière, Risk', 'KPI, Risk', 2),
    ('6e000001-0000-0000-0000-000000000001', 3, 'Ratios de liquidité & levier', 'Mise en place du suivi des ratios LCR, NSFR et du ratio de levier.', '4-8', 'Tableaux de bord LCR/NSFR, suivi du ratio de levier', 'Trésorerie, Risk', 'KPI, Risk', 3),
    ('6e000001-0000-0000-0000-000000000001', 4, 'Risque opérationnel & output floor', 'Calcul du risque opérationnel (SMA) et application du plancher de production (output floor).', '4-8', 'Calcul SMA, dossier output floor', 'Risk Manager', 'Risk, Document Control', 4),
    ('6e000001-0000-0000-0000-000000000001', 5, 'Reporting COREP/FINREP', 'Production et contrôle qualité du reporting prudentiel harmonisé.', '6-10', 'États COREP/FINREP, contrôles de qualité des données', 'Reporting réglementaire', 'Document Control, KPI', 5),
    ('6e000001-0000-0000-0000-000000000001', 6, 'Supervision prudentielle (SREP)', 'Dialogue de supervision avec l''autorité compétente (SREP) et levée des observations.', '4-12', 'Dossier SREP, plan de remédiation', 'Direction, Autorité de supervision', 'Standards Hub, Audit', 6);

-- ============================================================================
-- Solvabilité II — Directive 2009/138/CE (révision 2025) — assurance
-- ============================================================================

INSERT INTO standards (id, code, full_name, publisher, current_version, publication_date,
    family, applicable_industries, description, certification_body_required,
    recertification_cycle_months, related_norm_codes, status, created_at, updated_at)
VALUES (
    '60000002-0000-0000-0000-000000000002',
    'solvabilite-ii',
    'Solvabilité II — Directive 2009/138/CE (révisée 2025) sur l''accès aux activités de l''assurance et de la réassurance et leur exercice',
    'UE/EIOPA',
    '2009/138/CE rev 2025',
    DATE '2009-11-25',
    'REGULATORY',
    'finance',
    'La directive Solvabilité II (2009/138/CE), révisée en 2025, fixe le cadre prudentiel des entreprises d''assurance et de réassurance de l''Union européenne. Elle s''articule autour de TROIS PILIERS. Le pilier 1 (exigences quantitatives) impose un capital de solvabilité requis (SCR) calculé selon la formule standard ou un modèle interne, ainsi qu''un minimum de capital requis (MCR) ; le ratio de couverture des fonds propres éligibles sur le SCR doit être d''au moins 100 %. Le pilier 2 (gouvernance) exige un système de gouvernance comprenant quatre fonctions clés (gestion des risques, vérification de la conformité, audit interne, fonction actuarielle) et la réalisation de l''ORSA (Own Risk and Solvency Assessment), évaluation interne des risques et de la solvabilité. Le pilier 3 (information) impose un reporting prudentiel et public : modèles quantitatifs (QRT), rapport sur la solvabilité et la situation financière (SFCR) public et rapport régulier au contrôleur (RSR). La conformité relève de la supervision de l''ACPR et d''EIOPA, et non d''une certification accréditée.',
    FALSE,
    NULL,
    'bale-iii,iso-31000',
    'PUBLISHED', now(), now()
);

INSERT INTO standard_sections (id, standard_id, code, title, description, order_index) VALUES
    ('6200000b-0000-0000-0000-000000000001', '60000002-0000-0000-0000-000000000002', 'P1', 'Pilier 1 — Quantitatif', 'Exigences quantitatives : SCR, MCR et ratio de couverture.', 1),
    ('6200000b-0000-0000-0000-000000000002', '60000002-0000-0000-0000-000000000002', 'P2', 'Pilier 2 — Gouvernance', 'Système de gouvernance, fonctions clés et ORSA.', 2),
    ('6200000b-0000-0000-0000-000000000003', '60000002-0000-0000-0000-000000000002', 'P3', 'Pilier 3 — Information', 'Reporting prudentiel et public (QRT, SFCR, RSR).', 3);

INSERT INTO standard_clauses (id, section_id, code, title, description, order_index) VALUES
    ('6b000002-0000-0000-0000-000000000001', '6200000b-0000-0000-0000-000000000001', 'P1.SCR', 'Capital de solvabilité (SCR)', 'Calcul du capital de solvabilité requis (formule standard ou modèle interne).', 1),
    ('6b000002-0000-0000-0000-000000000002', '6200000b-0000-0000-0000-000000000001', 'P1.MCR', 'Minimum de capital (MCR)', 'Calcul du minimum de capital requis et du ratio de couverture.', 2),
    ('6b000002-0000-0000-0000-000000000003', '6200000b-0000-0000-0000-000000000001', 'P1.PT', 'Provisions techniques', 'Évaluation des provisions techniques (meilleure estimation et marge de risque).', 3),
    ('6b000002-0000-0000-0000-000000000004', '6200000b-0000-0000-0000-000000000002', 'P2.FK', 'Fonctions clés', 'Mise en place des quatre fonctions clés du système de gouvernance.', 4),
    ('6b000002-0000-0000-0000-000000000005', '6200000b-0000-0000-0000-000000000002', 'P2.ORSA', 'ORSA', 'Évaluation interne des risques et de la solvabilité (ORSA).', 5),
    ('6b000002-0000-0000-0000-000000000006', '6200000b-0000-0000-0000-000000000002', 'P2.GOV', 'Système de gouvernance', 'Exigences générales de gouvernance et de compétence/honorabilité.', 6),
    ('6b000002-0000-0000-0000-000000000007', '6200000b-0000-0000-0000-000000000003', 'P3.QRT', 'Reporting quantitatif (QRT)', 'Production des modèles quantitatifs de reporting (QRT).', 7),
    ('6b000002-0000-0000-0000-000000000008', '6200000b-0000-0000-0000-000000000003', 'P3.SFCR', 'SFCR & RSR', 'Rapport public SFCR et rapport régulier au contrôleur (RSR).', 8);

INSERT INTO standard_requirements (clause_id, code, text, obligation, evidence_types, measurable_criteria, risk_if_missing, order_index) VALUES
    ('6b000002-0000-0000-0000-000000000001', 'P1.SCR.1', 'L''entreprise doit calculer son capital de solvabilité requis (SCR) selon la formule standard ou un modèle interne approuvé, et le couvrir par des fonds propres éligibles.', 'MUST', 'KPI_RECORD,DOCUMENT', 'SCR calculé et couvert par des fonds propres éligibles ; ratio de couverture du SCR ≥ 100 %', 'CRITICAL', 1),
    ('6b000002-0000-0000-0000-000000000002', 'P1.MCR.1', 'L''entreprise doit calculer son minimum de capital requis (MCR) et maintenir des fonds propres de base éligibles supérieurs au MCR.', 'MUST', 'KPI_RECORD,DOCUMENT', 'MCR calculé ; ratio de couverture du MCR ≥ 100 % suivi périodiquement', 'CRITICAL', 2),
    ('6b000002-0000-0000-0000-000000000003', 'P1.PT.1', 'L''entreprise doit évaluer ses provisions techniques comme la somme d''une meilleure estimation et d''une marge de risque.', 'MUST', 'DOCUMENT,KPI_RECORD', 'Provisions techniques évaluées (meilleure estimation + marge de risque) et documentées par la fonction actuarielle', 'HIGH', 3),
    ('6b000002-0000-0000-0000-000000000004', 'P2.FK.1', 'L''entreprise doit mettre en place les quatre fonctions clés : gestion des risques, vérification de la conformité, audit interne et fonction actuarielle.', 'MUST', 'DOCUMENT,AUDIT', 'Quatre fonctions clés désignées, indépendantes et dotées de chartes ; rattachement à la gouvernance documenté', 'HIGH', 4),
    ('6b000002-0000-0000-0000-000000000005', 'P2.ORSA.1', 'L''entreprise doit réaliser au moins annuellement son évaluation interne des risques et de la solvabilité (ORSA) et la documenter.', 'MUST', 'DOCUMENT,AUDIT', 'Rapport ORSA produit au moins annuellement et approuvé par l''organe de gouvernance', 'CRITICAL', 5),
    ('6b000002-0000-0000-0000-000000000006', 'P2.GOV.1', 'L''entreprise doit disposer d''un système de gouvernance efficace garantissant la compétence et l''honorabilité des dirigeants et des responsables des fonctions clés.', 'MUST', 'DOCUMENT', 'Politique de compétence et d''honorabilité (fit & proper) appliquée ; politiques écrites de gouvernance en place', 'MEDIUM', 6),
    ('6b000002-0000-0000-0000-000000000007', 'P3.QRT.1', 'L''entreprise doit produire les modèles quantitatifs de reporting (QRT) et les transmettre à l''autorité de contrôle selon la périodicité requise.', 'MUST', 'DOCUMENT,KPI_RECORD', 'QRT produits et transmis dans les délais ; qualité des données de reporting contrôlée', 'HIGH', 7),
    ('6b000002-0000-0000-0000-000000000008', 'P3.SFCR.1', 'L''entreprise doit publier un rapport sur la solvabilité et la situation financière (SFCR) et transmettre un rapport régulier au contrôleur (RSR).', 'MUST', 'DOCUMENT,AUDIT', 'SFCR publié annuellement et RSR transmis au contrôleur selon la périodicité requise', 'HIGH', 8);

INSERT INTO standard_certification_paths (id, standard_id, estimated_duration_months_min, estimated_duration_months_max,
    estimated_cost_eur_min, estimated_cost_eur_max, difficulty_level,
    surveillance_audit_frequency, recertification_cycle_years, notes)
VALUES ('6e000002-0000-0000-0000-000000000002', '60000002-0000-0000-0000-000000000002',
    12, 24, 80000, 800000, 5, 'continuous', NULL,
    'Cadre prudentiel obligatoire pour les assureurs et réassureurs : la conformité relève de la SUPERVISION de l''ACPR et d''EIOPA (et non d''une certification accréditée), via le contrôle des trois piliers, l''examen de l''ORSA et le reporting prudentiel (QRT/SFCR/RSR) — d''où l''absence de cycle de recertification au sens classique. Synergie avec les modules Risk (SCR/MCR, ORSA, fonctions clés), KPI (ratios de couverture, provisions techniques), Document Control (politiques, SFCR/RSR) et Audit (fonction d''audit interne) de QualitOS.');

INSERT INTO standard_certification_stages (certification_path_id, stage_number, name, description, typical_duration_weeks, deliverables, actors, qualitos_modules, order_index) VALUES
    ('6e000002-0000-0000-0000-000000000002', 1, 'Cadrage & gouvernance', 'Mise en place du système de gouvernance et désignation des quatre fonctions clés.', '4-8', 'Politiques de gouvernance, chartes des fonctions clés', 'Direction, Fonctions clés', 'Document Control, Risk', 1),
    ('6e000002-0000-0000-0000-000000000002', 2, 'Provisions techniques', 'Évaluation des provisions techniques (meilleure estimation et marge de risque) par la fonction actuarielle.', '6-10', 'Note actuarielle, provisions techniques évaluées', 'Fonction actuarielle', 'Document Control, KPI', 2),
    ('6e000002-0000-0000-0000-000000000002', 3, 'Calcul du SCR & du MCR', 'Calcul du SCR (formule standard ou modèle interne) et du MCR, suivi des ratios de couverture.', '6-12', 'Dossier de calcul SCR/MCR, suivi des ratios de couverture', 'Fonction risques, Actuariat', 'KPI, Risk', 3),
    ('6e000002-0000-0000-0000-000000000002', 4, 'ORSA', 'Réalisation de l''évaluation interne des risques et de la solvabilité (ORSA) et approbation.', '4-8', 'Rapport ORSA approuvé', 'Fonction gestion des risques', 'Risk, Document Control', 4),
    ('6e000002-0000-0000-0000-000000000002', 5, 'Reporting QRT/SFCR/RSR', 'Production des QRT, du rapport public SFCR et du rapport régulier au contrôleur (RSR).', '6-10', 'QRT, SFCR, RSR', 'Reporting réglementaire', 'Document Control, KPI', 5),
    ('6e000002-0000-0000-0000-000000000002', 6, 'Supervision ACPR/EIOPA', 'Dialogue de supervision avec l''ACPR et levée des observations.', '4-12', 'Échanges de supervision, plan de remédiation', 'Direction, ACPR', 'Standards Hub, Audit', 6);

-- ============================================================================
-- MiFID II — Directive 2014/65/UE + MiFIR — marchés d'instruments financiers
-- ============================================================================

INSERT INTO standards (id, code, full_name, publisher, current_version, publication_date,
    family, applicable_industries, description, certification_body_required,
    recertification_cycle_months, related_norm_codes, status, created_at, updated_at)
VALUES (
    '60000003-0000-0000-0000-000000000003',
    'mifid-ii',
    'MiFID II — Directive 2014/65/UE concernant les marchés d''instruments financiers, complétée par le règlement MiFIR (UE) 600/2014',
    'UE/ESMA',
    '2014/65/UE',
    DATE '2014-05-15',
    'REGULATORY',
    'finance',
    'La directive MiFID II (2014/65/UE), complétée par le règlement MiFIR (UE) 600/2014, encadre la fourniture de services d''investissement et le fonctionnement des marchés d''instruments financiers dans l''Union européenne. Elle impose une GOUVERNANCE PRODUITS (définition d''un marché cible pour chaque instrument par les producteurs et distributeurs), l''évaluation de l''adéquation et du caractère approprié des produits au regard de la situation des clients, l''obligation de MEILLEURE EXÉCUTION des ordres (avec, historiquement, la publication des rapports RTS 28), un encadrement strict des avantages et rémunérations (inducements), l''ENREGISTREMENT et la conservation des communications relatives aux transactions (au moins cinq ans), des obligations de transparence pré- et post-négociation, et le reporting des transactions aux autorités (RTS 22, au plus tard le jour ouvré suivant, T+1). La conformité relève de la supervision de l''AMF/ESMA et n''est pas une certification accréditée.',
    FALSE,
    NULL,
    'lcb-ft',
    'PUBLISHED', now(), now()
);

INSERT INTO standard_sections (id, standard_id, code, title, description, order_index) VALUES
    ('6300000c-0000-0000-0000-000000000001', '60000003-0000-0000-0000-000000000003', 'GOV', 'Gouvernance produits', 'Gouvernance des produits financiers et marché cible.', 1),
    ('6300000c-0000-0000-0000-000000000002', '60000003-0000-0000-0000-000000000003', 'CONDUCT', 'Protection des clients', 'Adéquation, caractère approprié et encadrement des avantages.', 2),
    ('6300000c-0000-0000-0000-000000000003', '60000003-0000-0000-0000-000000000003', 'EXEC', 'Exécution & enregistrement', 'Meilleure exécution et enregistrement des communications.', 3),
    ('6300000c-0000-0000-0000-000000000004', '60000003-0000-0000-0000-000000000003', 'TRANSP', 'Transparence & reporting', 'Transparence des marchés et reporting des transactions.', 4);

INSERT INTO standard_clauses (id, section_id, code, title, description, order_index) VALUES
    ('6c000003-0000-0000-0000-000000000001', '6300000c-0000-0000-0000-000000000001', 'GOV.TM', 'Marché cible', 'Définition et revue du marché cible des produits.', 1),
    ('6c000003-0000-0000-0000-000000000002', '6300000c-0000-0000-0000-000000000002', 'CON.SUIT', 'Adéquation', 'Évaluation de l''adéquation lors du conseil et de la gestion de portefeuille.', 2),
    ('6c000003-0000-0000-0000-000000000003', '6300000c-0000-0000-0000-000000000002', 'CON.APPR', 'Caractère approprié', 'Évaluation du caractère approprié hors conseil.', 3),
    ('6c000003-0000-0000-0000-000000000004', '6300000c-0000-0000-0000-000000000002', 'CON.IND', 'Inducements', 'Encadrement des avantages et rémunérations (inducements).', 4),
    ('6c000003-0000-0000-0000-000000000005', '6300000c-0000-0000-0000-000000000003', 'EXE.BEST', 'Meilleure exécution', 'Obligation de meilleure exécution des ordres clients.', 5),
    ('6c000003-0000-0000-0000-000000000006', '6300000c-0000-0000-0000-000000000003', 'EXE.REC', 'Enregistrement des comms', 'Enregistrement et conservation des communications.', 6),
    ('6c000003-0000-0000-0000-000000000007', '6300000c-0000-0000-0000-000000000004', 'TR.PRE', 'Transparence négociation', 'Transparence pré- et post-négociation.', 7),
    ('6c000003-0000-0000-0000-000000000008', '6300000c-0000-0000-0000-000000000004', 'TR.REP', 'Transaction reporting', 'Déclaration des transactions aux autorités (RTS 22).', 8);

INSERT INTO standard_requirements (clause_id, code, text, obligation, evidence_types, measurable_criteria, risk_if_missing, order_index) VALUES
    ('6c000003-0000-0000-0000-000000000001', 'GOV.TM.1', 'L''entreprise doit définir, pour chaque instrument financier produit ou distribué, un marché cible et une stratégie de distribution adaptés, et les revoir périodiquement.', 'MUST', 'DOCUMENT,AUDIT', 'Marché cible défini et documenté pour 100 % des produits ; revue périodique tracée', 'HIGH', 1),
    ('6c000003-0000-0000-0000-000000000002', 'CON.SUIT.1', 'L''entreprise fournissant du conseil ou de la gestion de portefeuille doit évaluer l''adéquation des produits à la situation et aux objectifs du client et remettre une déclaration d''adéquation.', 'MUST', 'DOCUMENT,KPI_RECORD', 'Questionnaire d''adéquation collecté ; déclaration d''adéquation remise pour les services concernés', 'HIGH', 2),
    ('6c000003-0000-0000-0000-000000000003', 'CON.APPR.1', 'L''entreprise doit évaluer le caractère approprié des produits hors conseil et alerter le client le cas échéant.', 'MUST', 'DOCUMENT', 'Test du caractère approprié réalisé ; avertissements tracés en cas d''inadéquation', 'MEDIUM', 3),
    ('6c000003-0000-0000-0000-000000000004', 'CON.IND.1', 'L''entreprise doit encadrer les avantages et rémunérations (inducements) reçus ou versés et démontrer qu''ils améliorent le service rendu au client.', 'MUST', 'DOCUMENT,AUDIT', 'Registre des inducements tenu ; justification de l''amélioration du service documentée', 'MEDIUM', 4),
    ('6c000003-0000-0000-0000-000000000005', 'EXE.BEST.1', 'L''entreprise doit prendre toutes les mesures suffisantes pour obtenir le meilleur résultat possible lors de l''exécution des ordres et publier sa politique d''exécution.', 'MUST', 'DOCUMENT,KPI_RECORD', 'Politique de meilleure exécution publiée ; suivi de la qualité d''exécution et des lieux d''exécution', 'HIGH', 5),
    ('6c000003-0000-0000-0000-000000000006', 'EXE.REC.1', 'L''entreprise doit enregistrer les communications relatives aux transactions (téléphone et électroniques) et les conserver au moins cinq ans.', 'MUST', 'DOCUMENT,AUDIT', 'Communications enregistrées et conservées ≥ 5 ans ; accès et intégrité contrôlés', 'HIGH', 6),
    ('6c000003-0000-0000-0000-000000000007', 'TR.PRE.1', 'L''entreprise et les plates-formes doivent respecter les obligations de transparence pré- et post-négociation applicables aux instruments concernés.', 'MUST', 'DOCUMENT', 'Obligations de transparence pré/post-négociation identifiées et appliquées par instrument', 'MEDIUM', 7),
    ('6c000003-0000-0000-0000-000000000008', 'TR.REP.1', 'L''entreprise doit déclarer ses transactions à l''autorité compétente au plus tard le jour ouvré suivant (T+1) conformément aux champs requis (RTS 22).', 'MUST', 'DOCUMENT,KPI_RECORD', 'Transactions déclarées en T+1 ; taux de rejet de transaction reporting suivi et corrigé', 'HIGH', 8);

INSERT INTO standard_certification_paths (id, standard_id, estimated_duration_months_min, estimated_duration_months_max,
    estimated_cost_eur_min, estimated_cost_eur_max, difficulty_level,
    surveillance_audit_frequency, recertification_cycle_years, notes)
VALUES ('6e000003-0000-0000-0000-000000000003', '60000003-0000-0000-0000-000000000003',
    9, 18, 50000, 400000, 4, 'continuous', NULL,
    'Cadre réglementaire obligatoire pour les entreprises d''investissement : la conformité relève de la supervision de l''AMF et d''ESMA (et non d''une certification accréditée), via le contrôle des pratiques de gouvernance produits, de protection des investisseurs et de reporting (RTS 22/RTS 28) — d''où l''absence de cycle de recertification au sens classique. Synergie avec les modules Document Control (politiques, déclarations d''adéquation, enregistrements), Audit (contrôle de la meilleure exécution), Training (compétence des conseillers) et Complaints (réclamations clients) de QualitOS.');

INSERT INTO standard_certification_stages (certification_path_id, stage_number, name, description, typical_duration_weeks, deliverables, actors, qualitos_modules, order_index) VALUES
    ('6e000003-0000-0000-0000-000000000003', 1, 'Cadrage & gouvernance produits', 'Définition de la gouvernance des produits et des marchés cibles.', '4-6', 'Politique de gouvernance produits, marchés cibles', 'Conformité, Direction', 'Document Control', 1),
    ('6e000003-0000-0000-0000-000000000003', 2, 'Protection des investisseurs', 'Mise en place des évaluations d''adéquation/caractère approprié et de l''encadrement des inducements.', '6-10', 'Procédures d''adéquation, registre des inducements', 'Conformité, Front office', 'Document Control, Training', 2),
    ('6e000003-0000-0000-0000-000000000003', 3, 'Meilleure exécution', 'Définition de la politique d''exécution et du suivi de la qualité d''exécution.', '4-8', 'Politique de meilleure exécution, indicateurs d''exécution', 'Front office, Conformité', 'Document Control, KPI', 3),
    ('6e000003-0000-0000-0000-000000000003', 4, 'Enregistrement des communications', 'Mise en place de l''enregistrement et de la conservation des communications (≥ 5 ans).', '4-8', 'Dispositif d''enregistrement, politique de conservation', 'IT, Conformité', 'Document Control, Audit', 4),
    ('6e000003-0000-0000-0000-000000000003', 5, 'Transparence & transaction reporting', 'Mise en place de la transparence et du transaction reporting (RTS 22, T+1).', '6-10', 'Chaîne de transaction reporting, contrôles de qualité', 'Reporting réglementaire, IT', 'KPI, Document Control', 5),
    ('6e000003-0000-0000-0000-000000000003', 6, 'Contrôle & supervision AMF/ESMA', 'Contrôles de conformité internes et dialogue de supervision avec l''AMF.', '4-10', 'Plan de contrôle de conformité, suivi des observations', 'Conformité, Audit interne', 'Standards Hub, Audit', 6);

-- ============================================================================
-- LCB-FT — Paquet AML UE (AMLR/AMLD6 2024, AMLA) — anti-blanchiment
-- ============================================================================

INSERT INTO standards (id, code, full_name, publisher, current_version, publication_date,
    family, applicable_industries, description, certification_body_required,
    recertification_cycle_months, related_norm_codes, status, created_at, updated_at)
VALUES (
    '60000004-0000-0000-0000-000000000004',
    'lcb-ft',
    'LCB-FT — Lutte contre le blanchiment de capitaux et le financement du terrorisme (paquet UE : règlement AMLR, directive AMLD6 et autorité AMLA, 2024)',
    'UE',
    'AMLR/AMLD6 2024',
    DATE '2024-06-19',
    'REGULATORY',
    'finance',
    'Le dispositif de lutte contre le blanchiment de capitaux et le financement du terrorisme (LCB-FT) repose, dans l''Union européenne, sur le paquet AML 2024 : le règlement AMLR (règles directement applicables), la sixième directive AMLD6 et l''autorité européenne de lutte contre le blanchiment (AMLA). Il impose une APPROCHE PAR LES RISQUES : classification et évaluation des risques de blanchiment selon les clients, produits, canaux et zones géographiques. Les entités assujetties doivent appliquer des mesures de vigilance à l''égard de la clientèle (KYC) — standard, simplifiée ou renforcée pour les personnes politiquement exposées (PPE) et les situations à haut risque — assurer une surveillance continue des opérations, effectuer des déclarations de soupçon (DS) à la cellule de renseignement financier (TRACFIN en France) sans délai, procéder au gel des avoirs par criblage des listes de sanctions, former périodiquement leur personnel et conserver les pièces et documents au moins cinq ans. La conformité relève du contrôle de l''ACPR et de TRACFIN (et de l''AMLA pour les entités sélectionnées), et non d''une certification accréditée.',
    FALSE,
    NULL,
    'mifid-ii',
    'PUBLISHED', now(), now()
);

INSERT INTO standard_sections (id, standard_id, code, title, description, order_index) VALUES
    ('6400000d-0000-0000-0000-000000000001', '60000004-0000-0000-0000-000000000004', 'RBA', 'Approche par les risques', 'Classification et évaluation des risques de blanchiment.', 1),
    ('6400000d-0000-0000-0000-000000000002', '60000004-0000-0000-0000-000000000004', 'KYC', 'Vigilance clientèle (KYC)', 'Mesures de vigilance standard, simplifiée et renforcée.', 2),
    ('6400000d-0000-0000-0000-000000000003', '60000004-0000-0000-0000-000000000004', 'MON', 'Surveillance & déclarations', 'Surveillance des opérations et déclarations de soupçon.', 3),
    ('6400000d-0000-0000-0000-000000000004', '60000004-0000-0000-0000-000000000004', 'GOV', 'Gouvernance & formation', 'Gel des avoirs, formation du personnel et conservation.', 4);

INSERT INTO standard_clauses (id, section_id, code, title, description, order_index) VALUES
    ('6d000004-0000-0000-0000-000000000001', '6400000d-0000-0000-0000-000000000001', 'RBA.CLASS', 'Classification des risques', 'Classification des clients et activités selon le risque de blanchiment.', 1),
    ('6d000004-0000-0000-0000-000000000002', '6400000d-0000-0000-0000-000000000002', 'KYC.STD', 'Vigilance standard', 'Identification et vérification de l''identité du client et du bénéficiaire effectif.', 2),
    ('6d000004-0000-0000-0000-000000000003', '6400000d-0000-0000-0000-000000000002', 'KYC.EDD', 'Vigilance renforcée (PPE)', 'Mesures de vigilance renforcée pour les PPE et les situations à haut risque.', 3),
    ('6d000004-0000-0000-0000-000000000004', '6400000d-0000-0000-0000-000000000003', 'MON.OPS', 'Surveillance des opérations', 'Surveillance continue des opérations et détection des anomalies.', 4),
    ('6d000004-0000-0000-0000-000000000005', '6400000d-0000-0000-0000-000000000003', 'MON.DS', 'Déclaration de soupçon', 'Déclaration de soupçon à TRACFIN sans délai.', 5),
    ('6d000004-0000-0000-0000-000000000006', '6400000d-0000-0000-0000-000000000004', 'GEL', 'Gel des avoirs', 'Criblage des listes de sanctions et gel des avoirs.', 6),
    ('6d000004-0000-0000-0000-000000000007', '6400000d-0000-0000-0000-000000000004', 'FORM', 'Formation du personnel', 'Formation périodique du personnel à la LCB-FT.', 7),
    ('6d000004-0000-0000-0000-000000000008', '6400000d-0000-0000-0000-000000000004', 'CONS', 'Conservation', 'Conservation des pièces et documents au moins cinq ans.', 8);

INSERT INTO standard_requirements (clause_id, code, text, obligation, evidence_types, measurable_criteria, risk_if_missing, order_index) VALUES
    ('6d000004-0000-0000-0000-000000000001', 'RBA.CLASS.1', 'L''entité assujettie doit établir et tenir à jour une classification des risques de blanchiment et de financement du terrorisme selon les clients, produits, canaux et zones géographiques.', 'MUST', 'DOCUMENT,AUDIT', 'Classification des risques LCB-FT documentée et revue au moins annuellement', 'CRITICAL', 1),
    ('6d000004-0000-0000-0000-000000000002', 'KYC.STD.1', 'L''entité doit identifier et vérifier l''identité de ses clients et de leurs bénéficiaires effectifs avant l''entrée en relation d''affaires.', 'MUST', 'DOCUMENT,KPI_RECORD', 'Identification et vérification réalisées pour 100 % des entrées en relation ; bénéficiaire effectif identifié', 'CRITICAL', 2),
    ('6d000004-0000-0000-0000-000000000003', 'KYC.EDD.1', 'L''entité doit appliquer des mesures de vigilance renforcée aux personnes politiquement exposées (PPE) et aux situations à haut risque.', 'MUST', 'DOCUMENT,AUDIT', 'Détection des PPE en place ; mesures de vigilance renforcée appliquées et tracées', 'HIGH', 3),
    ('6d000004-0000-0000-0000-000000000004', 'MON.OPS.1', 'L''entité doit assurer une surveillance continue des opérations afin de détecter les opérations atypiques ou suspectes.', 'MUST', 'DOCUMENT,KPI_RECORD', 'Dispositif de surveillance des opérations en place ; alertes traitées dans des délais suivis', 'HIGH', 4),
    ('6d000004-0000-0000-0000-000000000005', 'MON.DS.1', 'L''entité doit déclarer sans délai à la cellule de renseignement financier (TRACFIN) toute opération suspectée de blanchiment ou de financement du terrorisme.', 'MUST', 'DOCUMENT,AUDIT', 'Procédure de déclaration de soupçon en place ; DS transmises à TRACFIN sans délai et tracées', 'CRITICAL', 5),
    ('6d000004-0000-0000-0000-000000000006', 'GEL.1', 'L''entité doit procéder au criblage de sa clientèle et de ses opérations au regard des listes de sanctions et appliquer sans délai le gel des avoirs requis.', 'MUST', 'DOCUMENT,KPI_RECORD', 'Criblage des listes de sanctions opérationnel ; gel des avoirs appliqué et tracé sans délai', 'CRITICAL', 6),
    ('6d000004-0000-0000-0000-000000000007', 'FORM.1', 'L''entité doit assurer une formation périodique de son personnel aux obligations de LCB-FT.', 'MUST', 'TRAINING_RECORD,DOCUMENT', 'Plan de formation LCB-FT déployé ; taux de personnel formé suivi et périodicité respectée', 'MEDIUM', 7),
    ('6d000004-0000-0000-0000-000000000008', 'CONS.1', 'L''entité doit conserver les documents et pièces relatifs à la vigilance et aux opérations pendant au moins cinq ans.', 'MUST', 'DOCUMENT', 'Documents de vigilance et d''opérations conservés ≥ 5 ans ; accès et intégrité contrôlés', 'HIGH', 8);

INSERT INTO standard_certification_paths (id, standard_id, estimated_duration_months_min, estimated_duration_months_max,
    estimated_cost_eur_min, estimated_cost_eur_max, difficulty_level,
    surveillance_audit_frequency, recertification_cycle_years, notes)
VALUES ('6e000004-0000-0000-0000-000000000004', '60000004-0000-0000-0000-000000000004',
    6, 12, 30000, 300000, 4, 'continuous', NULL,
    'Dispositif réglementaire obligatoire pour les entités assujetties : la conformité relève du contrôle de l''ACPR et de TRACFIN (et de l''AMLA pour les entités sélectionnées), et non d''une certification accréditée — d''où l''absence de cycle de recertification au sens classique. Synergie avec les modules Risk (classification et évaluation des risques LCB-FT), Training (formation périodique du personnel), Audit (contrôle interne et déclarations de soupçon) et KPI (délais de traitement des alertes, taux de personnel formé) de QualitOS.');

INSERT INTO standard_certification_stages (certification_path_id, stage_number, name, description, typical_duration_weeks, deliverables, actors, qualitos_modules, order_index) VALUES
    ('6e000004-0000-0000-0000-000000000004', 1, 'Classification des risques LCB-FT', 'Établissement de la classification et de l''évaluation des risques de blanchiment.', '3-6', 'Cartographie des risques LCB-FT, classification clientèle', 'Conformité, Risk Manager', 'Risk, Document Control', 1),
    ('6e000004-0000-0000-0000-000000000004', 2, 'Procédures de vigilance (KYC)', 'Définition des procédures de vigilance standard, simplifiée et renforcée (PPE).', '4-8', 'Procédures KYC, dispositif de détection des PPE', 'Conformité', 'Document Control', 2),
    ('6e000004-0000-0000-0000-000000000004', 3, 'Surveillance & criblage', 'Mise en place de la surveillance des opérations et du criblage des listes de sanctions.', '6-10', 'Dispositif de surveillance, outil de criblage, procédure de gel', 'Conformité, IT', 'Risk, KPI', 3),
    ('6e000004-0000-0000-0000-000000000004', 4, 'Déclarations de soupçon', 'Mise en place du circuit de déclaration de soupçon vers TRACFIN.', '3-5', 'Procédure de déclaration de soupçon, registre des DS', 'Déclarant TRACFIN, Conformité', 'Document Control, Audit', 4),
    ('6e000004-0000-0000-0000-000000000004', 5, 'Formation du personnel', 'Déploiement de la formation périodique du personnel à la LCB-FT.', '3-5', 'Plan de formation LCB-FT, attestations', 'RH, Conformité', 'Training', 5),
    ('6e000004-0000-0000-0000-000000000004', 6, 'Contrôle interne & supervision', 'Contrôles internes périodiques et dialogue de supervision avec l''ACPR.', '4-10', 'Plan de contrôle LCB-FT, suivi des observations', 'Audit interne, ACPR', 'Standards Hub, Audit', 6);

-- ============================================================================
-- NIST CSF 2.0 — NIST Cybersecurity Framework version 2.0 (2024)
-- ============================================================================

INSERT INTO standards (id, code, full_name, publisher, current_version, publication_date,
    family, applicable_industries, description, certification_body_required,
    recertification_cycle_months, related_norm_codes, status, created_at, updated_at)
VALUES (
    '60000005-0000-0000-0000-000000000005',
    'nist-csf',
    'NIST Cybersecurity Framework (CSF) 2.0 — cadre d''amélioration de la cybersécurité (NIST CSWP 29)',
    'NIST',
    '2.0',
    DATE '2024-02-26',
    'SECTORIEL',
    'all',
    'Le NIST Cybersecurity Framework (CSF) 2.0, publié par le National Institute of Standards and Technology en 2024, est un cadre VOLONTAIRE d''amélioration de la posture de cybersécurité, applicable à tout type d''organisation. La version 2.0 introduit une sixième fonction transverse, GOVERN, qui s''ajoute aux cinq fonctions historiques : IDENTIFY (identifier les actifs et risques), PROTECT (mettre en place les mesures de protection), DETECT (détecter les événements de cybersécurité), RESPOND (répondre aux incidents) et RECOVER (rétablir les capacités). Le cadre s''utilise via des PROFILS (profil courant décrivant l''état actuel et profil cible décrivant l''objectif) et des NIVEAUX DE MISE EN ŒUVRE (tiers 1 à 4, du partiel à l''adaptatif), permettant d''élaborer un plan d''amélioration priorisé. Il ne s''agit PAS d''une norme certifiable : aucune certification accréditée n''est délivrée — la valeur réside dans l''auto-évaluation, l''alignement et la communication du risque cyber (nuance importante par rapport aux normes ISO certifiables).',
    FALSE,
    NULL,
    'iso-27001,nis2',
    'PUBLISHED', now(), now()
);

INSERT INTO standard_sections (id, standard_id, code, title, description, order_index) VALUES
    ('6500000e-0000-0000-0000-000000000001', '60000005-0000-0000-0000-000000000005', 'GV', 'Govern (gouvernance)', 'Gouvernance de la cybersécurité (fonction GOVERN, nouvelle en 2.0).', 1),
    ('6500000e-0000-0000-0000-000000000002', '60000005-0000-0000-0000-000000000005', 'ID', 'Identify', 'Identification des actifs, des risques et de l''environnement.', 2),
    ('6500000e-0000-0000-0000-000000000003', '60000005-0000-0000-0000-000000000005', 'PR', 'Protect', 'Mesures de protection des actifs et des données.', 3),
    ('6500000e-0000-0000-0000-000000000004', '60000005-0000-0000-0000-000000000005', 'DE', 'Detect', 'Détection des événements et anomalies de cybersécurité.', 4),
    ('6500000e-0000-0000-0000-000000000005', '60000005-0000-0000-0000-000000000005', 'RS', 'Respond', 'Réponse aux incidents de cybersécurité.', 5),
    ('6500000e-0000-0000-0000-000000000006', '60000005-0000-0000-0000-000000000005', 'RC', 'Recover', 'Rétablissement après incident.', 6),
    ('6500000e-0000-0000-0000-000000000007', '60000005-0000-0000-0000-000000000005', 'PROF', 'Profils & niveaux', 'Profils (courant/cible), niveaux (tiers) et plan d''amélioration.', 7);

INSERT INTO standard_clauses (id, section_id, code, title, description, order_index) VALUES
    ('69000005-0000-0000-0000-000000000001', '6500000e-0000-0000-0000-000000000001', 'GV.OC', 'Stratégie & gouvernance', 'Stratégie, rôles, politiques et supervision de la cybersécurité.', 1),
    ('69000005-0000-0000-0000-000000000002', '6500000e-0000-0000-0000-000000000001', 'GV.SC', 'Risque chaîne d''appro.', 'Gestion du risque de cybersécurité de la chaîne d''approvisionnement.', 2),
    ('69000005-0000-0000-0000-000000000003', '6500000e-0000-0000-0000-000000000002', 'ID.AM', 'Gestion des actifs', 'Identification et inventaire des actifs et des risques.', 3),
    ('69000005-0000-0000-0000-000000000004', '6500000e-0000-0000-0000-000000000003', 'PR.AA', 'Accès & protection', 'Gestion des identités, des accès et protection des données.', 4),
    ('69000005-0000-0000-0000-000000000005', '6500000e-0000-0000-0000-000000000004', 'DE.CM', 'Surveillance continue', 'Surveillance continue et détection des événements.', 5),
    ('69000005-0000-0000-0000-000000000006', '6500000e-0000-0000-0000-000000000005', 'RS.MA', 'Gestion des incidents', 'Gestion et traitement des incidents de cybersécurité.', 6),
    ('69000005-0000-0000-0000-000000000007', '6500000e-0000-0000-0000-000000000006', 'RC.RP', 'Plan de rétablissement', 'Exécution du plan de rétablissement après incident.', 7),
    ('69000005-0000-0000-0000-000000000008', '6500000e-0000-0000-0000-000000000007', 'PROF.PL', 'Profils & amélioration', 'Établissement des profils et du plan d''amélioration.', 8);

INSERT INTO standard_requirements (clause_id, code, text, obligation, evidence_types, measurable_criteria, risk_if_missing, order_index) VALUES
    ('69000005-0000-0000-0000-000000000001', 'GV.OC.1', 'L''organisation doit établir et communiquer une stratégie, des rôles et des politiques de gestion du risque de cybersécurité, et en assurer la supervision.', 'MUST', 'DOCUMENT,AUDIT', 'Stratégie cyber, rôles et politiques formalisés, approuvés et revus ; supervision tracée', 'CRITICAL', 1),
    ('69000005-0000-0000-0000-000000000002', 'GV.SC.1', 'L''organisation doit gérer le risque de cybersécurité lié à la chaîne d''approvisionnement et à ses tiers.', 'SHOULD', 'DOCUMENT,KPI_RECORD', 'Programme de gestion du risque tiers en place ; fournisseurs critiques évalués', 'HIGH', 2),
    ('69000005-0000-0000-0000-000000000003', 'ID.AM.1', 'L''organisation doit identifier et inventorier ses actifs (matériels, logiciels, données, services) et les risques associés.', 'MUST', 'DOCUMENT,KPI_RECORD', 'Inventaire des actifs tenu à jour ; risques associés identifiés et priorisés', 'HIGH', 3),
    ('69000005-0000-0000-0000-000000000004', 'PR.AA.1', 'L''organisation doit gérer les identités et les accès et mettre en œuvre les mesures de protection des actifs et des données.', 'MUST', 'DOCUMENT,AUDIT', 'Gestion des identités et accès en place ; mesures de protection (chiffrement, durcissement) déployées', 'CRITICAL', 4),
    ('69000005-0000-0000-0000-000000000005', 'DE.CM.1', 'L''organisation doit assurer une surveillance continue pour détecter les événements et anomalies de cybersécurité.', 'MUST', 'DOCUMENT,KPI_RECORD', 'Surveillance continue opérationnelle ; couverture de journalisation et délai de détection suivis', 'HIGH', 5),
    ('69000005-0000-0000-0000-000000000006', 'RS.MA.1', 'L''organisation doit gérer les incidents de cybersécurité selon un processus défini de réponse aux incidents.', 'MUST', 'DOCUMENT,CAPA', 'Plan de réponse aux incidents en place ; incidents tracés et délais de traitement suivis', 'HIGH', 6),
    ('69000005-0000-0000-0000-000000000007', 'RC.RP.1', 'L''organisation doit définir et exécuter un plan de rétablissement pour restaurer les capacités affectées après un incident.', 'MUST', 'DOCUMENT,AUDIT', 'Plan de rétablissement documenté et testé ; objectifs de rétablissement (RTO/RPO) définis', 'HIGH', 7),
    ('69000005-0000-0000-0000-000000000008', 'PROF.PL.1', 'L''organisation doit établir un profil courant et un profil cible, déterminer son niveau (tier) et élaborer un plan d''amélioration priorisé.', 'SHOULD', 'DOCUMENT,PDCA_CYCLE', 'Profils courant et cible établis ; niveau (tier) déterminé ; plan d''amélioration priorisé et suivi', 'MEDIUM', 8);

INSERT INTO standard_certification_paths (id, standard_id, estimated_duration_months_min, estimated_duration_months_max,
    estimated_cost_eur_min, estimated_cost_eur_max, difficulty_level,
    surveillance_audit_frequency, recertification_cycle_years, notes)
VALUES ('6e000005-0000-0000-0000-000000000005', '60000005-0000-0000-0000-000000000005',
    3, 9, 10000, 80000, 3, 'annual', NULL,
    'Le NIST CSF 2.0 est un cadre VOLONTAIRE : il ne donne lieu à AUCUNE certification accréditée — la valeur réside dans l''auto-évaluation via les profils (courant/cible) et les niveaux (tiers 1 à 4), puis dans un plan d''amélioration priorisé piloté en amélioration continue. Le CSF est fréquemment utilisé en complément d''ISO 27001 (certifiable) et de NIS 2. Synergie avec les modules Cyber-Incidents (fonctions DETECT/RESPOND/RECOVER), Risk (gouvernance et identification des risques), Audit (évaluation de la posture) et PDCA (plan d''amélioration profil courant → cible) de QualitOS.');

INSERT INTO standard_certification_stages (certification_path_id, stage_number, name, description, typical_duration_weeks, deliverables, actors, qualitos_modules, order_index) VALUES
    ('6e000005-0000-0000-0000-000000000005', 1, 'Gouvernance & cadrage (GOVERN)', 'Établissement de la stratégie, des rôles et des politiques de cybersécurité et du périmètre.', '3-5', 'Stratégie cyber, politiques, rôles et responsabilités', 'RSSI, Direction', 'Document Control, Risk', 1),
    ('6e000005-0000-0000-0000-000000000005', 2, 'Profil courant & inventaire (IDENTIFY)', 'Inventaire des actifs et des risques et établissement du profil courant.', '4-6', 'Inventaire des actifs, profil courant, registre des risques', 'RSSI, Risk Manager', 'Risk, Document Control', 2),
    ('6e000005-0000-0000-0000-000000000005', 3, 'Mesures de protection (PROTECT)', 'Déploiement des mesures de protection (identités, accès, données, durcissement).', '6-12', 'Mesures de protection déployées, gestion des accès', 'Équipe sécurité, IT', 'Document Control', 3),
    ('6e000005-0000-0000-0000-000000000005', 4, 'Détection & réponse (DETECT/RESPOND)', 'Mise en place de la surveillance continue et du processus de réponse aux incidents.', '4-8', 'Dispositif de détection, plan de réponse aux incidents', 'SOC, RSSI', 'Cyber-Incidents, KPI', 4),
    ('6e000005-0000-0000-0000-000000000005', 5, 'Rétablissement (RECOVER)', 'Définition et test du plan de rétablissement (RTO/RPO).', '3-6', 'Plan de rétablissement testé, objectifs RTO/RPO', 'IT, RSSI', 'Cyber-Incidents, Audit', 5),
    ('6e000005-0000-0000-0000-000000000005', 6, 'Profil cible & plan d''amélioration', 'Établissement du profil cible, du niveau (tier) et du plan d''amélioration priorisé.', '3-5', 'Profil cible, niveau (tier), plan d''amélioration', 'RSSI, Direction', 'PDCA, Standards Hub', 6);
