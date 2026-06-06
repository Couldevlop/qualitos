-- Standards Hub — vague 5 (CLAUDE.md §8.2) : 5 référentiels sectoriels & réglementaires.
--   iso-15189   : ISO 15189:2022 — laboratoires de biologie médicale (accréditation COFRAC)
--   haccp       : HACCP Codex Alimentarius (CXC 1-1969, rév. 2020) — maîtrise des dangers alimentaires
--   fssc-22000  : FSSC 22000 v6 — schéma de certification GFSI (ISO 22000 + PRP + exigences additionnelles)
--   pci-dss     : PCI DSS v4.0.1 — sécurité des données de cartes de paiement
--   hds         : Certification HDS — Hébergeur de Données de Santé (référentiel ANS v1.1, France)
-- Même format que V71 : catalogue platform-level, UUID déterministes, préfixe 9
-- (standards 9000000n, sections 9n00000s, clauses 9[a-d|9]…, paths 9e…, stages 9f via FK).
-- Note UUID : pour éviter toute collision avec les paths (9e), les clauses de la 5e
-- norme (HDS) portent le préfixe 99 (9a-9d couvrent les normes 1 à 4).

-- ============================================================================
-- ISO 15189:2022 — Laboratoires de biologie médicale (accréditation)
-- ============================================================================

INSERT INTO standards (id, code, full_name, publisher, current_version, publication_date,
    family, applicable_industries, description, certification_body_required,
    recertification_cycle_months, related_norm_codes, status, created_at, updated_at)
VALUES (
    '90000001-0000-0000-0000-000000000001',
    'iso-15189',
    'ISO 15189:2022 — Laboratoires de biologie médicale — Exigences concernant la qualité et la compétence',
    'ISO',
    '2022',
    DATE '2022-12-06',
    'SECTORIEL',
    'healthcare,laboratory',
    'Exigences de qualité et de compétence des laboratoires de biologie médicale. La reconnaissance s''obtient par ACCRÉDITATION (et non certification) délivrée par un organisme signataire des accords ILAC/EA — en France le COFRAC — évalué selon ISO/IEC 17011 ; en France l''accréditation des LBM est rendue obligatoire par l''ordonnance n° 2010-49. Le champ couvre l''impartialité, les examens délocalisés (EBMD/POCT), la validation/vérification des méthodes, l''incertitude de mesure, le contrôle interne de qualité (CIQ) et l''évaluation externe de la qualité (EEQ), les délais de rendu et la gestion des risques pour la sécurité du patient. Cycle d''accréditation typiquement de 4 à 5 ans avec évaluations de surveillance annuelles.',
    TRUE,
    NULL,
    'iso-17025,iso-9001',
    'PUBLISHED', now(), now()
);

INSERT INTO standard_sections (id, standard_id, code, title, description, order_index) VALUES
    ('91000001-0000-0000-0000-000000000001', '90000001-0000-0000-0000-000000000001', '4', 'Exigences générales', 'Impartialité et confidentialité du laboratoire de biologie médicale.', 1),
    ('91000001-0000-0000-0000-000000000002', '90000001-0000-0000-0000-000000000001', '5', 'Exigences structurelles et de gouvernance', 'Entité juridique, direction, objectifs, politiques et structure organisationnelle.', 2),
    ('91000001-0000-0000-0000-000000000003', '90000001-0000-0000-0000-000000000001', '6', 'Exigences relatives aux ressources', 'Personnel, locaux, équipements, réactifs, services externes et SIL.', 3),
    ('91000001-0000-0000-0000-000000000004', '90000001-0000-0000-0000-000000000001', '7', 'Exigences relatives aux processus', 'Phases pré-analytique, analytique et post-analytique, dont EBMD/POCT.', 4),
    ('91000001-0000-0000-0000-000000000005', '90000001-0000-0000-0000-000000000001', '8', 'Exigences relatives au système de management', 'Système de management du laboratoire (options A ou B).', 5);

INSERT INTO standard_clauses (id, section_id, code, title, description, order_index) VALUES
    ('9a000001-0000-0000-0000-000000000001', '91000001-0000-0000-0000-000000000001', '4.1', 'Impartialité', 'Garantir l''impartialité des activités du laboratoire et maîtriser les risques associés.', 1),
    ('9a000001-0000-0000-0000-000000000002', '91000001-0000-0000-0000-000000000001', '4.2', 'Confidentialité', 'Protection des informations des patients obtenues ou créées lors des activités.', 2),
    ('9a000001-0000-0000-0000-000000000003', '91000001-0000-0000-0000-000000000002', '5.5', 'Gestion des risques', 'Gestion des risques pour la sécurité du patient liés aux examens.', 3),
    ('9a000001-0000-0000-0000-000000000004', '91000001-0000-0000-0000-000000000003', '6.2', 'Personnel', 'Compétence, qualification, habilitation et évaluation continue du personnel.', 4),
    ('9a000001-0000-0000-0000-000000000005', '91000001-0000-0000-0000-000000000004', '7.2', 'Processus pré-analytiques', 'Prescription, prélèvement, identification et transport des échantillons.', 5),
    ('9a000001-0000-0000-0000-000000000006', '91000001-0000-0000-0000-000000000004', '7.3', 'Processus analytiques', 'Vérification et validation des méthodes et évaluation de l''incertitude de mesure.', 6),
    ('9a000001-0000-0000-0000-000000000007', '91000001-0000-0000-0000-000000000004', '7.4', 'Processus post-analytiques', 'Revue, validation biologique et compte rendu des résultats dans les délais.', 7),
    ('9a000001-0000-0000-0000-000000000008', '91000001-0000-0000-0000-000000000004', '7.5', 'Assurance qualité des résultats', 'Contrôle interne de qualité (CIQ) et évaluation externe de la qualité (EEQ/EILA).', 8),
    ('9a000001-0000-0000-0000-000000000009', '91000001-0000-0000-0000-000000000004', '7.6', 'Examens de biologie délocalisée (EBMD/POCT)', 'Maîtrise des examens réalisés hors du laboratoire central.', 9);

INSERT INTO standard_requirements (clause_id, code, text, obligation, evidence_types, measurable_criteria, risk_if_missing, order_index) VALUES
    ('9a000001-0000-0000-0000-000000000001', '4.1.2', 'Le laboratoire doit identifier en continu les risques pesant sur son impartialité et les éliminer ou les minimiser.', 'MUST', 'DOCUMENT,AUDIT', 'Analyse des risques d''impartialité documentée et revue au moins annuellement', 'CRITICAL', 1),
    ('9a000001-0000-0000-0000-000000000002', '4.2.1', 'Le laboratoire doit assurer la confidentialité des informations des patients par des dispositions exécutoires.', 'MUST', 'DOCUMENT', 'Engagements de confidentialité signés par 100 % du personnel ; accès tracés', 'HIGH', 2),
    ('9a000001-0000-0000-0000-000000000003', '5.5.1', 'Le laboratoire doit gérer les risques pour la sécurité du patient associés à ses examens et activités.', 'MUST', 'DOCUMENT,AUDIT', 'Registre des risques patient tenu à jour ; actions de réduction suivies', 'CRITICAL', 3),
    ('9a000001-0000-0000-0000-000000000004', '6.2.3', 'Le laboratoire doit s''assurer que le personnel est compétent et documenter son habilitation avant toute activité autonome.', 'MUST', 'TRAINING_RECORD,DOCUMENT', 'Habilitations à jour pour 100 % du personnel autorisé ; évaluation périodique', 'HIGH', 4),
    ('9a000001-0000-0000-0000-000000000005', '7.2.6', 'Le laboratoire doit définir des critères d''acceptation/rejet des échantillons et tracer les non-conformités pré-analytiques.', 'MUST', 'DOCUMENT,KPI_RECORD', 'Taux d''échantillons non conformes suivi ; critères de rejet documentés', 'HIGH', 5),
    ('9a000001-0000-0000-0000-000000000006', '7.3.3', 'Le laboratoire doit vérifier les méthodes validées et valider les méthodes non normalisées avant leur mise en service.', 'MUST', 'DOCUMENT,AUDIT', 'Dossier de vérification/validation par méthode (justesse, fidélité, limites)', 'CRITICAL', 6),
    ('9a000001-0000-0000-0000-000000000006', '7.3.4', 'Le laboratoire doit évaluer l''incertitude de mesure des grandeurs mesurées.', 'MUST', 'DOCUMENT', 'Budget d''incertitude documenté pour chaque grandeur quantitative', 'HIGH', 7),
    ('9a000001-0000-0000-0000-000000000007', '7.4.1', 'Le laboratoire doit valider les résultats et les communiquer dans des délais de rendu (TAT) définis et surveillés.', 'MUST', 'DOCUMENT,KPI_RECORD', 'Délais de rendu (TAT) définis par examen ; taux de respect du TAT suivi', 'HIGH', 8),
    ('9a000001-0000-0000-0000-000000000008', '7.5.1', 'Le laboratoire doit mettre en œuvre un contrôle interne de qualité (CIQ) pour surveiller la validité des résultats.', 'MUST', 'KPI_RECORD,DOCUMENT', 'CIQ exécuté à la fréquence définie ; dérives détectées et traitées', 'CRITICAL', 9),
    ('9a000001-0000-0000-0000-000000000008', '7.5.2', 'Le laboratoire doit participer à une évaluation externe de la qualité (EEQ/essais inter-laboratoires) lorsque disponible.', 'MUST', 'KPI_RECORD,DOCUMENT', 'Participation EEQ planifiée ; écarts (z-score |z|<2) suivis et corrigés', 'HIGH', 10),
    ('9a000001-0000-0000-0000-000000000009', '7.6.1', 'Le laboratoire doit maîtriser les examens de biologie délocalisée (EBMD/POCT) sous sa responsabilité.', 'MUST', 'DOCUMENT,AUDIT', 'Procédure EBMD/POCT en place ; habilitation des opérateurs et CIQ délocalisé', 'HIGH', 11);

INSERT INTO standard_certification_paths (id, standard_id, estimated_duration_months_min, estimated_duration_months_max,
    estimated_cost_eur_min, estimated_cost_eur_max, difficulty_level,
    surveillance_audit_frequency, recertification_cycle_years, notes)
VALUES ('9e000001-0000-0000-0000-000000000001', '90000001-0000-0000-0000-000000000001',
    12, 24, 30000, 120000, 5, 'annual', 4,
    'Reconnaissance par ACCRÉDITATION (et non certification) délivrée par un organisme signataire ILAC/EA — COFRAC en France (accréditation obligatoire des LBM). Cycle d''accréditation de 4 à 5 ans avec évaluations de surveillance annuelles. Les modules Calibration/Equipment, Audit, Document Control et Risk de QualitOS soutiennent §6.5, §7.3 à §7.6 et §5.5.');

INSERT INTO standard_certification_stages (certification_path_id, stage_number, name, description, typical_duration_weeks, deliverables, actors, qualitos_modules, order_index) VALUES
    ('9e000001-0000-0000-0000-000000000001', 1, 'Cadrage & portée d''accréditation', 'Définition de la portée (familles d''examens, sites, EBMD), engagement direction et calendrier COFRAC.', '3-5', 'Portée d''accréditation, engagement direction', 'Biologiste responsable, Direction', 'Document Control', 1),
    ('9e000001-0000-0000-0000-000000000001', 2, 'Système de management & impartialité', 'Mise en place du système de management (option A/B), gestion de l''impartialité, de la confidentialité et des risques patient.', '6-10', 'Manuel/procédures, analyse d''impartialité, registre des risques patient', 'Responsable qualité', 'Document Control, Risk', 2),
    ('9e000001-0000-0000-0000-000000000001', 3, 'Ressources & traçabilité métrologique', 'Habilitations du personnel, raccordement métrologique et maintenance des automates et équipements.', '8-12', 'Habilitations, plan d''étalonnage, certificats de raccordement', 'Métrologie, RH', 'Calibration, Training', 3),
    ('9e000001-0000-0000-0000-000000000001', 4, 'Validation des méthodes & maîtrise des phases', 'Vérification/validation des méthodes, budgets d''incertitude, CIQ/EEQ et maîtrise pré/post-analytique.', '8-16', 'Dossiers de validation, budgets d''incertitude, plan CIQ/EEQ', 'Techniciens, Biologistes', 'Document Control, KPI', 4),
    ('9e000001-0000-0000-0000-000000000001', 5, 'Audit interne & revue de direction', 'Audit interne couvrant §4 à §8 et revue de direction du système de management.', '3-5', 'Rapport d''audit interne, compte rendu de revue de direction', 'Auditeur interne, Direction', 'Audit, PDCA', 5),
    ('9e000001-0000-0000-0000-000000000001', 6, 'Évaluation d''accréditation (COFRAC/ILAC)', 'Évaluation documentaire puis évaluation sur site avec évaluation technique, levée des écarts.', '4-8', 'Attestation d''accréditation ISO 15189', 'Organisme d''accréditation', 'Standards Hub, CAPA', 6);

-- ============================================================================
-- HACCP — Codex Alimentarius (CXC 1-1969, rév. 2020) — maîtrise des dangers
-- ============================================================================

INSERT INTO standards (id, code, full_name, publisher, current_version, publication_date,
    family, applicable_industries, description, certification_body_required,
    recertification_cycle_months, related_norm_codes, status, created_at, updated_at)
VALUES (
    '90000002-0000-0000-0000-000000000002',
    'haccp',
    'HACCP — Codex Alimentarius CXC 1-1969 (rév. 2020) — Principes généraux d''hygiène alimentaire et système HACCP',
    'Codex Alimentarius',
    'CXC 1-1969 (rév. 2020)',
    DATE '2020-09-25',
    'SECTORIEL',
    'food,agro',
    'Démarche de maîtrise des dangers liés à la sécurité des denrées alimentaires, fondée sur les programmes prérequis (PRP), les 7 principes et les 12 étapes d''application du système HACCP. HACCP n''est pas une norme certifiable en tant que telle : la conformité se démontre via un schéma de certification reconnu GFSI (FSSC 22000, IFS, BRCGS) ou par contrôle officiel. Le système repose sur l''analyse des dangers (microbiologiques, chimiques, physiques, allergènes), la détermination des points critiques (CCP) et programmes prérequis opérationnels (PRPo), des limites critiques chiffrées, une surveillance continue, des actions correctives, la vérification périodique et la traçabilité amont/aval.',
    FALSE,
    NULL,
    'iso-22000,fssc-22000',
    'PUBLISHED', now(), now()
);

INSERT INTO standard_sections (id, standard_id, code, title, description, order_index) VALUES
    ('92000002-0000-0000-0000-000000000001', '90000002-0000-0000-0000-000000000002', 'PRP', 'Programmes prérequis (BPH/BPF)', 'Bonnes pratiques d''hygiène et de fabrication, socle du système HACCP.', 1),
    ('92000002-0000-0000-0000-000000000002', '90000002-0000-0000-0000-000000000002', 'P1-2', 'Analyse des dangers', 'Identification, analyse et évaluation des dangers et mesures de maîtrise (principes 1-2).', 2),
    ('92000002-0000-0000-0000-000000000003', '90000002-0000-0000-0000-000000000002', 'P3-5', 'Maîtrise des CCP', 'Limites critiques, surveillance et actions correctives aux points critiques (principes 3-5).', 3),
    ('92000002-0000-0000-0000-000000000004', '90000002-0000-0000-0000-000000000002', 'P6-7', 'Vérification & documentation', 'Procédures de vérification, documentation, enregistrements et traçabilité (principes 6-7).', 4);

INSERT INTO standard_clauses (id, section_id, code, title, description, order_index) VALUES
    ('9b000002-0000-0000-0000-000000000001', '92000002-0000-0000-0000-000000000001', 'PRP.1', 'Programmes prérequis', 'Hygiène du personnel, des locaux, maîtrise des nuisibles, nettoyage-désinfection, chaîne du froid.', 1),
    ('9b000002-0000-0000-0000-000000000002', '92000002-0000-0000-0000-000000000002', 'E1-5', 'Étapes préliminaires', 'Équipe HACCP, description produit, usage prévu, diagramme de fabrication et sa vérification sur site.', 2),
    ('9b000002-0000-0000-0000-000000000003', '92000002-0000-0000-0000-000000000002', 'P1', 'Analyse des dangers', 'Identification des dangers biologiques, chimiques, physiques et allergènes par étape.', 3),
    ('9b000002-0000-0000-0000-000000000004', '92000002-0000-0000-0000-000000000002', 'P2', 'Détermination des CCP', 'Identification des points critiques pour la maîtrise (CCP) et des PRPo.', 4),
    ('9b000002-0000-0000-0000-000000000005', '92000002-0000-0000-0000-000000000003', 'P3', 'Limites critiques', 'Établissement de limites critiques mesurables pour chaque CCP.', 5),
    ('9b000002-0000-0000-0000-000000000006', '92000002-0000-0000-0000-000000000003', 'P4', 'Surveillance des CCP', 'Mise en place d''un système de surveillance de chaque CCP.', 6),
    ('9b000002-0000-0000-0000-000000000007', '92000002-0000-0000-0000-000000000003', 'P5', 'Actions correctives', 'Définition des actions correctives en cas de dépassement d''une limite critique.', 7),
    ('9b000002-0000-0000-0000-000000000008', '92000002-0000-0000-0000-000000000004', 'P6', 'Vérification', 'Procédures de vérification de l''efficacité du système HACCP.', 8),
    ('9b000002-0000-0000-0000-000000000009', '92000002-0000-0000-0000-000000000004', 'P7', 'Documentation & traçabilité', 'Tenue des documents, des enregistrements et de la traçabilité amont/aval.', 9);

INSERT INTO standard_requirements (clause_id, code, text, obligation, evidence_types, measurable_criteria, risk_if_missing, order_index) VALUES
    ('9b000002-0000-0000-0000-000000000001', 'PRP.1', 'L''entreprise doit mettre en œuvre et maintenir des programmes prérequis (BPH/BPF) couvrant l''hygiène, le nettoyage, la maîtrise des nuisibles et la chaîne du froid.', 'MUST', 'DOCUMENT,AUDIT', 'PRP documentés et vérifiés ; plans de nettoyage et de lutte contre les nuisibles tracés', 'HIGH', 1),
    ('9b000002-0000-0000-0000-000000000002', 'E.1', 'Constituer une équipe HACCP pluridisciplinaire et décrire le produit, son usage prévu et le diagramme de fabrication.', 'MUST', 'DOCUMENT', 'Équipe HACCP nommée ; fiches produit et diagramme de fabrication vérifiés sur site', 'MEDIUM', 2),
    ('9b000002-0000-0000-0000-000000000003', 'P1', 'Réaliser une analyse des dangers (biologiques, chimiques, physiques, allergènes) pour chaque étape du procédé.', 'MUST', 'DOCUMENT,AUDIT', 'Analyse des dangers documentée pour 100 % des étapes du diagramme', 'CRITICAL', 3),
    ('9b000002-0000-0000-0000-000000000004', 'P2', 'Déterminer les points critiques pour la maîtrise (CCP) à l''aide d''une approche logique (arbre de décision).', 'MUST', 'DOCUMENT', 'CCP et PRPo identifiés et justifiés pour chaque danger significatif', 'CRITICAL', 4),
    ('9b000002-0000-0000-0000-000000000005', 'P3', 'Établir des limites critiques mesurables et validées pour chaque CCP.', 'MUST', 'DOCUMENT', 'Limites critiques chiffrées (ex. cuisson cœur ≥ 75 °C, pH ≤ 4,6) validées pour chaque CCP', 'CRITICAL', 5),
    ('9b000002-0000-0000-0000-000000000006', 'P4', 'Mettre en place un système de surveillance permettant de détecter toute perte de maîtrise d''un CCP.', 'MUST', 'DOCUMENT,KPI_RECORD', 'Surveillance continue ou à fréquence définie de chaque CCP ; relevés enregistrés', 'CRITICAL', 6),
    ('9b000002-0000-0000-0000-000000000007', 'P5', 'Définir et appliquer les actions correctives à mettre en œuvre lorsqu''un CCP n''est plus maîtrisé.', 'MUST', 'DOCUMENT,CAPA', 'Actions correctives définies par CCP ; produits non conformes isolés et traités', 'CRITICAL', 7),
    ('9b000002-0000-0000-0000-000000000008', 'P6', 'Établir des procédures de vérification pour confirmer que le système HACCP fonctionne efficacement.', 'MUST', 'DOCUMENT,AUDIT', 'Vérification HACCP réalisée au moins annuellement et à chaque modification du procédé', 'HIGH', 8),
    ('9b000002-0000-0000-0000-000000000009', 'P7', 'Tenir une documentation et des enregistrements adaptés aux principes HACCP et à leur application.', 'MUST', 'DOCUMENT', 'Plan HACCP documenté et enregistrements conservés selon la durée définie', 'HIGH', 9),
    ('9b000002-0000-0000-0000-000000000009', 'TRAC', 'Assurer la traçabilité amont/aval permettant un retrait/rappel rapide des produits.', 'MUST', 'DOCUMENT,KPI_RECORD', 'Test de traçabilité ascendante/descendante réalisé ; remontée d''un lot ≤ 4 heures', 'HIGH', 10);

INSERT INTO standard_certification_paths (id, standard_id, estimated_duration_months_min, estimated_duration_months_max,
    estimated_cost_eur_min, estimated_cost_eur_max, difficulty_level,
    surveillance_audit_frequency, recertification_cycle_years, notes)
VALUES ('9e000002-0000-0000-0000-000000000002', '90000002-0000-0000-0000-000000000002',
    3, 9, 5000, 35000, 3, 'none', NULL,
    'HACCP est une démarche réglementaire (Paquet hygiène, Codex) et non un certificat accrédité : la reconnaissance par tiers passe par un schéma GFSI (FSSC 22000, IFS Food, BRCGS) ou par contrôle officiel. Synergie directe avec les modules IoT (surveillance CCP en continu : T°, pH), CAPA (actions correctives), Audit et Document Control de QualitOS.');

INSERT INTO standard_certification_stages (certification_path_id, stage_number, name, description, typical_duration_weeks, deliverables, actors, qualitos_modules, order_index) VALUES
    ('9e000002-0000-0000-0000-000000000002', 1, 'Équipe HACCP & programmes prérequis', 'Constitution de l''équipe HACCP et mise en place des programmes prérequis (BPH/BPF).', '3-6', 'Équipe HACCP, plan de PRP', 'Direction, Responsable qualité', 'Document Control, Training', 1),
    ('9e000002-0000-0000-0000-000000000002', 2, 'Description produit & diagramme', 'Description des produits, usage prévu et élaboration/vérification du diagramme de fabrication.', '2-4', 'Fiches produit, diagramme de fabrication vérifié', 'Équipe HACCP', 'Document Control', 2),
    ('9e000002-0000-0000-0000-000000000002', 3, 'Analyse des dangers & CCP', 'Analyse des dangers par étape et détermination des CCP/PRPo (principes 1-2).', '4-8', 'Tableau d''analyse des dangers, plan HACCP (CCP/PRPo)', 'Équipe HACCP', 'Ishikawa, Risk', 3),
    ('9e000002-0000-0000-0000-000000000002', 4, 'Limites, surveillance & corrections', 'Définition des limites critiques, du plan de surveillance et des actions correctives (principes 3-5).', '4-8', 'Limites critiques, plan de surveillance, procédures correctives', 'Équipe HACCP, Production', 'IoT, CAPA', 4),
    ('9e000002-0000-0000-0000-000000000002', 5, 'Vérification & traçabilité', 'Mise en place de la vérification, de la documentation et des tests de traçabilité (principes 6-7).', '3-5', 'Procédures de vérification, plan de traçabilité, enregistrements', 'Responsable qualité', 'Audit, Document Control', 5),
    ('9e000002-0000-0000-0000-000000000002', 6, 'Validation & revue du plan HACCP', 'Validation du plan HACCP, revue périodique et amélioration continue.', '2-4', 'Rapport de validation, revue HACCP, plan d''amélioration', 'Équipe HACCP, Direction', 'PDCA, Audit', 6);

-- ============================================================================
-- FSSC 22000 v6 — Schéma de certification GFSI (sécurité des denrées alimentaires)
-- ============================================================================

INSERT INTO standards (id, code, full_name, publisher, current_version, publication_date,
    family, applicable_industries, description, certification_body_required,
    recertification_cycle_months, related_norm_codes, status, created_at, updated_at)
VALUES (
    '90000003-0000-0000-0000-000000000003',
    'fssc-22000',
    'FSSC 22000 v6 — Schéma de certification de la sécurité des denrées alimentaires (reconnu GFSI)',
    'Foundation FSSC',
    'v6',
    DATE '2023-04-01',
    'SECTORIEL',
    'food,agro',
    'Schéma de certification reconnu GFSI construit sur trois piliers : ISO 22000 (système de management de la sécurité des denrées alimentaires), les programmes prérequis sectoriels (séries ISO/TS 22002-x) et les exigences additionnelles propres au schéma FSSC. La version 6 renforce la culture de la sécurité des aliments (food safety culture), la food defense, la prévention de la fraude alimentaire (food fraud/VACCP), la maîtrise des allergènes, l''exactitude de l''étiquetage et la gestion multisite. La certification (cycle de 3 ans) est délivrée par un organisme accrédité ; au moins un audit de surveillance par cycle est inopiné (non annoncé).',
    TRUE,
    NULL,
    'iso-22000,haccp',
    'PUBLISHED', now(), now()
);

INSERT INTO standard_sections (id, standard_id, code, title, description, order_index) VALUES
    ('93000003-0000-0000-0000-000000000001', '90000003-0000-0000-0000-000000000003', 'P1', 'ISO 22000 (FSMS)', 'Système de management de la sécurité des denrées alimentaires selon ISO 22000.', 1),
    ('93000003-0000-0000-0000-000000000002', '90000003-0000-0000-0000-000000000003', 'P2', 'Programmes prérequis (PRP)', 'PRP sectoriels selon les séries ISO/TS 22002-x.', 2),
    ('93000003-0000-0000-0000-000000000003', '90000003-0000-0000-0000-000000000003', 'P3', 'Exigences additionnelles FSSC v6', 'Exigences propres au schéma : culture, food defense, fraude, allergènes, étiquetage.', 3),
    ('93000003-0000-0000-0000-000000000004', '90000003-0000-0000-0000-000000000003', 'GOV', 'Gouvernance de la certification', 'Multisite, audits inopinés et règles du schéma.', 4);

INSERT INTO standard_clauses (id, section_id, code, title, description, order_index) VALUES
    ('9c000003-0000-0000-0000-000000000001', '93000003-0000-0000-0000-000000000001', '22000', 'Système de management FSMS', 'Mise en œuvre des exigences ISO 22000 (HLS, HACCP, communication, PDCA).', 1),
    ('9c000003-0000-0000-0000-000000000002', '93000003-0000-0000-0000-000000000002', '22002', 'PRP sectoriels', 'Application de la spécification ISO/TS 22002-x correspondant au secteur.', 2),
    ('9c000003-0000-0000-0000-000000000003', '93000003-0000-0000-0000-000000000003', '2.5.1', 'Gestion des services & matériaux', 'Maîtrise des services, des achats et des matériaux à risque.', 3),
    ('9c000003-0000-0000-0000-000000000004', '93000003-0000-0000-0000-000000000003', '2.5.2', 'Étiquetage produit', 'Exactitude de l''étiquetage, notamment des allergènes.', 4),
    ('9c000003-0000-0000-0000-000000000005', '93000003-0000-0000-0000-000000000003', '2.5.3', 'Food defense', 'Évaluation des menaces et plan de protection contre la malveillance (TACCP).', 5),
    ('9c000003-0000-0000-0000-000000000006', '93000003-0000-0000-0000-000000000003', '2.5.4', 'Atténuation de la fraude alimentaire', 'Évaluation de la vulnérabilité (VACCP) et plan d''atténuation de la fraude.', 6),
    ('9c000003-0000-0000-0000-000000000007', '93000003-0000-0000-0000-000000000003', '2.5.6', 'Maîtrise des allergènes', 'Plan de gestion des allergènes et validation du nettoyage allergènes.', 7),
    ('9c000003-0000-0000-0000-000000000008', '93000003-0000-0000-0000-000000000003', '2.5.7', 'Culture de la sécurité des aliments', 'Objectifs et plan de développement de la food safety culture.', 8),
    ('9c000003-0000-0000-0000-000000000009', '93000003-0000-0000-0000-000000000004', 'A.2', 'Audits inopinés & multisite', 'Audits de surveillance non annoncés et règles d''échantillonnage multisite.', 9);

INSERT INTO standard_requirements (clause_id, code, text, obligation, evidence_types, measurable_criteria, risk_if_missing, order_index) VALUES
    ('9c000003-0000-0000-0000-000000000001', '22000', 'L''organisation doit établir et maintenir un système de management de la sécurité des denrées alimentaires conforme à ISO 22000, incluant un plan HACCP.', 'MUST', 'DOCUMENT,AUDIT', 'FSMS documenté et opérationnel couvrant 100 % des clauses ISO 22000', 'CRITICAL', 1),
    ('9c000003-0000-0000-0000-000000000002', '22002', 'L''organisation doit mettre en œuvre les programmes prérequis selon la spécification ISO/TS 22002-x applicable à son secteur.', 'MUST', 'DOCUMENT,AUDIT', 'PRP sectoriels (ISO/TS 22002-x) déployés et vérifiés sur l''ensemble du site', 'HIGH', 2),
    ('9c000003-0000-0000-0000-000000000003', '2.5.1', 'L''organisation doit maîtriser les achats de services et de matériaux susceptibles d''affecter la sécurité des denrées.', 'MUST', 'DOCUMENT,AUDIT', 'Fournisseurs critiques évalués et approuvés ; spécifications matières à jour', 'HIGH', 3),
    ('9c000003-0000-0000-0000-000000000004', '2.5.2', 'L''organisation doit garantir l''exactitude de l''étiquetage, y compris la déclaration des allergènes et des exigences légales.', 'MUST', 'DOCUMENT', 'Vérification de l''étiquetage avant mise sur le marché ; 0 erreur d''allergène', 'CRITICAL', 4),
    ('9c000003-0000-0000-0000-000000000005', '2.5.3', 'L''organisation doit réaliser une évaluation des menaces (food defense) et mettre en place un plan de protection documenté.', 'MUST', 'DOCUMENT,AUDIT', 'Évaluation des menaces (TACCP) et plan de food defense revus au moins annuellement', 'HIGH', 5),
    ('9c000003-0000-0000-0000-000000000006', '2.5.4', 'L''organisation doit réaliser une évaluation de la vulnérabilité à la fraude (VACCP) et un plan d''atténuation.', 'MUST', 'DOCUMENT,AUDIT', 'Évaluation de vulnérabilité fraude documentée ; plan d''atténuation revu annuellement', 'HIGH', 6),
    ('9c000003-0000-0000-0000-000000000007', '2.5.6', 'L''organisation doit gérer les allergènes par un plan dédié et valider l''efficacité du nettoyage allergènes.', 'MUST', 'DOCUMENT,KPI_RECORD', 'Plan allergènes en place ; validation du nettoyage par analyses (seuil de détection)', 'CRITICAL', 7),
    ('9c000003-0000-0000-0000-000000000008', '2.5.7', 'L''organisation doit définir des objectifs et un plan de développement de la culture de la sécurité des aliments.', 'MUST', 'DOCUMENT,TRAINING_RECORD', 'Objectifs de culture sécurité définis ; indicateurs et actions de sensibilisation suivis', 'MEDIUM', 8),
    ('9c000003-0000-0000-0000-000000000009', 'A.2', 'L''organisation doit se soumettre aux audits de surveillance inopinés et, en multisite, aux règles d''échantillonnage du schéma.', 'MUST', 'AUDIT,DOCUMENT', 'Au moins un audit de surveillance inopiné par cycle ; échantillonnage multisite respecté', 'HIGH', 9);

INSERT INTO standard_certification_paths (id, standard_id, estimated_duration_months_min, estimated_duration_months_max,
    estimated_cost_eur_min, estimated_cost_eur_max, difficulty_level,
    surveillance_audit_frequency, recertification_cycle_years, notes)
VALUES ('9e000003-0000-0000-0000-000000000003', '90000003-0000-0000-0000-000000000003',
    9, 18, 15000, 60000, 4, 'annual', 3,
    'Certification par organisme accrédité (schéma reconnu GFSI). Cycle de 3 ans avec audits de surveillance annuels dont au moins un inopiné. Construit sur ISO 22000 + PRP ISO/TS 22002-x + exigences additionnelles v6. Synergie avec les modules Audit, CAPA, Training (food safety culture) et Supplier de QualitOS.');

INSERT INTO standard_certification_stages (certification_path_id, stage_number, name, description, typical_duration_weeks, deliverables, actors, qualitos_modules, order_index) VALUES
    ('9e000003-0000-0000-0000-000000000003', 1, 'Cadrage & gap analysis', 'Définition du périmètre (sites, catégories), choix du PRP ISO/TS 22002-x et diagnostic d''écarts vs FSSC v6.', '3-6', 'Périmètre, rapport d''écarts', 'Direction, Responsable qualité', 'Audit, Document Control', 1),
    ('9e000003-0000-0000-0000-000000000003', 2, 'FSMS & plan HACCP', 'Mise en place du système ISO 22000 et du plan HACCP (analyse des dangers, CCP/PRPo).', '6-10', 'FSMS documenté, plan HACCP', 'Équipe sécurité des aliments', 'Document Control, Ishikawa', 2),
    ('9e000003-0000-0000-0000-000000000003', 3, 'Programmes prérequis sectoriels', 'Déploiement des PRP selon ISO/TS 22002-x et maîtrise des matériaux/services.', '4-8', 'PRP déployés, spécifications fournisseurs', 'Production, Achats', 'Supplier, Document Control', 3),
    ('9e000003-0000-0000-0000-000000000003', 4, 'Exigences additionnelles v6', 'Food defense (TACCP), fraude (VACCP), allergènes, étiquetage et culture sécurité.', '4-8', 'Plans TACCP/VACCP, plan allergènes, plan culture sécurité', 'Responsable qualité, RH', 'Training, CAPA', 4),
    ('9e000003-0000-0000-0000-000000000003', 5, 'Audit interne & revue de direction', 'Audit interne couvrant les trois piliers et revue de direction du FSMS.', '3-5', 'Rapport d''audit interne, compte rendu de revue', 'Auditeur interne, Direction', 'Audit, PDCA', 5),
    ('9e000003-0000-0000-0000-000000000003', 6, 'Audit de certification (étapes 1 & 2)', 'Audit documentaire puis audit sur site par l''organisme certificateur, levée des non-conformités.', '4-8', 'Certificat FSSC 22000 v6', 'Organisme certificateur', 'Standards Hub, CAPA', 6);

-- ============================================================================
-- PCI DSS v4.0.1 — Sécurité des données de cartes de paiement
-- ============================================================================

INSERT INTO standards (id, code, full_name, publisher, current_version, publication_date,
    family, applicable_industries, description, certification_body_required,
    recertification_cycle_months, related_norm_codes, status, created_at, updated_at)
VALUES (
    '90000004-0000-0000-0000-000000000004',
    'pci-dss',
    'PCI DSS v4.0.1 — Payment Card Industry Data Security Standard',
    'PCI SSC',
    'v4.0.1',
    DATE '2024-06-11',
    'REGULATORY',
    'finance,retail,all',
    'Standard de sécurité des données applicable à toute entité qui stocke, traite ou transmet des données de titulaires de carte. Il s''agit d''un standard contractuel imposé par les schémas de paiement (Visa, Mastercard, etc.), et non d''une réglementation publique. La validation de conformité est ANNUELLE : pour les marchands/prestataires de niveau 1, par un QSA produisant un rapport de conformité (ROC) ; pour les niveaux inférieurs, par auto-évaluation (SAQ). Le standard organise douze exigences en six objectifs : construire et maintenir un réseau et des systèmes sécurisés, protéger les données de compte, gérer les vulnérabilités, mettre en œuvre des contrôles d''accès stricts, surveiller et tester les réseaux, et maintenir une politique de sécurité de l''information.',
    TRUE,
    NULL,
    'iso-27001,nis2',
    'PUBLISHED', now(), now()
);

INSERT INTO standard_sections (id, standard_id, code, title, description, order_index) VALUES
    ('94000004-0000-0000-0000-000000000001', '90000004-0000-0000-0000-000000000004', 'req.1-2', 'Réseau & systèmes sécurisés', 'Maîtrise des accès réseau et configuration sécurisée des composants.', 1),
    ('94000004-0000-0000-0000-000000000002', '90000004-0000-0000-0000-000000000004', 'req.3-4', 'Protection des données de compte', 'Protection au repos et chiffrement lors de la transmission.', 2),
    ('94000004-0000-0000-0000-000000000003', '90000004-0000-0000-0000-000000000004', 'req.5-6', 'Gestion des vulnérabilités', 'Protection anti-logiciels malveillants et systèmes/logiciels sécurisés.', 3),
    ('94000004-0000-0000-0000-000000000004', '90000004-0000-0000-0000-000000000004', 'req.7-9', 'Contrôle d''accès', 'Restriction logique et physique de l''accès aux données de compte.', 4),
    ('94000004-0000-0000-0000-000000000005', '90000004-0000-0000-0000-000000000004', 'req.10-11', 'Surveillance & tests', 'Journalisation, surveillance et tests réguliers des réseaux et systèmes.', 5),
    ('94000004-0000-0000-0000-000000000006', '90000004-0000-0000-0000-000000000004', 'req.12', 'Politique de sécurité', 'Politiques et programme de sécurité de l''information.', 6);

INSERT INTO standard_clauses (id, section_id, code, title, description, order_index) VALUES
    ('9d000004-0000-0000-0000-000000000001', '94000004-0000-0000-0000-000000000001', 'req.1', 'Contrôles de sécurité réseau', 'Installation et maintenance de contrôles de sécurité du réseau.', 1),
    ('9d000004-0000-0000-0000-000000000002', '94000004-0000-0000-0000-000000000001', 'req.2', 'Configurations sécurisées', 'Application de configurations sécurisées à tous les composants du système.', 2),
    ('9d000004-0000-0000-0000-000000000003', '94000004-0000-0000-0000-000000000002', 'req.3', 'Protection des données stockées', 'Protection des données de compte stockées (PAN illisible).', 3),
    ('9d000004-0000-0000-0000-000000000004', '94000004-0000-0000-0000-000000000002', 'req.4', 'Chiffrement en transmission', 'Chiffrement fort du PAN lors de la transmission sur réseaux ouverts.', 4),
    ('9d000004-0000-0000-0000-000000000005', '94000004-0000-0000-0000-000000000003', 'req.6', 'Logiciels et systèmes sécurisés', 'Développement et maintenance de systèmes et logiciels sécurisés.', 5),
    ('9d000004-0000-0000-0000-000000000006', '94000004-0000-0000-0000-000000000004', 'req.8', 'Identification & authentification', 'Identification des utilisateurs et authentification des accès aux composants.', 6),
    ('9d000004-0000-0000-0000-000000000007', '94000004-0000-0000-0000-000000000005', 'req.10', 'Journalisation & surveillance', 'Journalisation et surveillance de tous les accès aux composants et données.', 7),
    ('9d000004-0000-0000-0000-000000000008', '94000004-0000-0000-0000-000000000005', 'req.11', 'Tests de sécurité', 'Tests réguliers de la sécurité des systèmes et des réseaux.', 8),
    ('9d000004-0000-0000-0000-000000000009', '94000004-0000-0000-0000-000000000006', 'req.12', 'Programme de sécurité', 'Soutien de la sécurité de l''information par des politiques et programmes.', 9);

INSERT INTO standard_requirements (clause_id, code, text, obligation, evidence_types, measurable_criteria, risk_if_missing, order_index) VALUES
    ('9d000004-0000-0000-0000-000000000001', 'req.1.2', 'L''entité doit installer et maintenir des contrôles de sécurité réseau (pare-feu) entre les réseaux de confiance et non fiables.', 'MUST', 'DOCUMENT,AUDIT', 'Règles de filtrage documentées ; revue des règles de pare-feu au moins tous les 6 mois', 'CRITICAL', 1),
    ('9d000004-0000-0000-0000-000000000002', 'req.2.2', 'L''entité doit appliquer des configurations sécurisées et changer les paramètres par défaut des composants du système.', 'MUST', 'DOCUMENT,AUDIT', 'Standards de configuration durcie appliqués à 100 % des composants ; comptes par défaut désactivés', 'HIGH', 2),
    ('9d000004-0000-0000-0000-000000000003', 'req.3.5', 'L''entité doit rendre le numéro de compte primaire (PAN) illisible partout où il est stocké (chiffrement fort, troncature ou hachage).', 'MUST', 'DOCUMENT,AUDIT', 'PAN stocké rendu illisible (AES-256 ou équivalent) sur 100 % des emplacements de stockage', 'CRITICAL', 3),
    ('9d000004-0000-0000-0000-000000000003', 'req.3.7', 'L''entité doit gérer le cycle de vie des clés cryptographiques, y compris leur rotation périodique.', 'MUST', 'DOCUMENT,AUDIT', 'Procédure de gestion des clés en place ; rotation des clés à la cryptopériode définie', 'HIGH', 4),
    ('9d000004-0000-0000-0000-000000000004', 'req.4.2', 'L''entité doit chiffrer le PAN par une cryptographie forte lors de sa transmission sur des réseaux ouverts ou publics.', 'MUST', 'DOCUMENT,AUDIT', 'TLS 1.2+ (protocoles forts uniquement) imposé sur 100 % des flux transportant le PAN', 'CRITICAL', 5),
    ('9d000004-0000-0000-0000-000000000005', 'req.6.3', 'L''entité doit identifier et corriger les vulnérabilités de sécurité par un processus de gestion des correctifs.', 'MUST', 'DOCUMENT,CAPA', 'Vulnérabilités critiques corrigées sous 1 mois ; processus de patch management tracé', 'HIGH', 6),
    ('9d000004-0000-0000-0000-000000000006', 'req.8.4', 'L''entité doit mettre en œuvre l''authentification multifactorielle (MFA) pour tout accès à l''environnement des données de carte.', 'MUST', 'DOCUMENT,AUDIT', 'MFA déployée sur 100 % des accès au CDE (administratifs et distants)', 'CRITICAL', 7),
    ('9d000004-0000-0000-0000-000000000007', 'req.10.4', 'L''entité doit examiner les journaux et événements de sécurité afin d''identifier les anomalies ou activités suspectes.', 'MUST', 'DOCUMENT,KPI_RECORD', 'Revue des journaux quotidienne (automatisée) ; alertes traitées et tracées', 'HIGH', 8),
    ('9d000004-0000-0000-0000-000000000008', 'req.11.3', 'L''entité doit réaliser des tests d''intrusion internes et externes au moins annuellement et après tout changement significatif.', 'MUST', 'DOCUMENT,AUDIT', 'Pentest interne/externe ≥ 1/an ; vulnérabilités exploitables corrigées et re-testées', 'HIGH', 9),
    ('9d000004-0000-0000-0000-000000000008', 'req.11.3.2', 'L''entité doit faire réaliser des scans de vulnérabilité externes par un ASV approuvé sur une base trimestrielle.', 'MUST', 'DOCUMENT,KPI_RECORD', 'Scans ASV ≥ 1/trimestre avec résultat conforme (passing) ; remédiations tracées', 'HIGH', 10),
    ('9d000004-0000-0000-0000-000000000009', 'req.12.1', 'L''entité doit établir, publier et maintenir une politique de sécurité de l''information revue au moins annuellement.', 'MUST', 'DOCUMENT', 'Politique de sécurité approuvée et revue au moins une fois par an', 'MEDIUM', 11);

INSERT INTO standard_certification_paths (id, standard_id, estimated_duration_months_min, estimated_duration_months_max,
    estimated_cost_eur_min, estimated_cost_eur_max, difficulty_level,
    surveillance_audit_frequency, recertification_cycle_years, notes)
VALUES ('9e000004-0000-0000-0000-000000000004', '90000004-0000-0000-0000-000000000004',
    6, 18, 20000, 200000, 4, 'annual', 1,
    'Validation de conformité ANNUELLE (le standard est contractuel, imposé par les schémas de paiement). Niveau 1 : audit par un QSA produisant un ROC + AOC ; niveaux inférieurs : auto-évaluation (SAQ) + scans ASV trimestriels. Synergie avec les modules cyber-incidents, Audit, Document Control et Risk de QualitOS.');

INSERT INTO standard_certification_stages (certification_path_id, stage_number, name, description, typical_duration_weeks, deliverables, actors, qualitos_modules, order_index) VALUES
    ('9e000004-0000-0000-0000-000000000004', 1, 'Définition du périmètre (CDE)', 'Délimitation de l''environnement des données de carte (CDE), des flux de PAN et de la segmentation réseau.', '3-6', 'Schéma des flux PAN, périmètre CDE, niveau de marchand/prestataire', 'RSSI, Architectes', 'Document Control', 1),
    ('9e000004-0000-0000-0000-000000000004', 2, 'Analyse des écarts vs 12 exigences', 'Gap analysis vs PCI DSS v4.0.1 (socle ISO 27001) et plan de remédiation priorisé.', '4-6', 'Rapport d''écarts, plan de remédiation', 'RSSI, Auditeur', 'Audit, Risk', 2),
    ('9e000004-0000-0000-0000-000000000004', 3, 'Protection des données & accès', 'Chiffrement du PAN, gestion des clés, MFA et contrôles d''accès stricts au CDE.', '6-12', 'Chiffrement déployé, gestion des clés, matrice d''accès, MFA', 'RSSI, IT', 'Document Control, Risk', 3),
    ('9e000004-0000-0000-0000-000000000004', 4, 'Surveillance, journalisation & tests', 'Journalisation centralisée, revue quotidienne, scans ASV trimestriels et pentest annuel.', '4-10', 'SIEM/journalisation, rapports de scans ASV, rapport de pentest', 'RSSI, SOC', 'cyber-incidents, Audit', 4),
    ('9e000004-0000-0000-0000-000000000004', 5, 'Politique & sensibilisation', 'Politique de sécurité, gestion des incidents, sensibilisation et gestion des prestataires.', '3-5', 'Politique de sécurité, plan de réponse aux incidents, sensibilisation', 'RSSI, RH', 'Document Control, Training', 5),
    ('9e000004-0000-0000-0000-000000000004', 6, 'Évaluation de conformité (QSA/SAQ)', 'Évaluation par un QSA (ROC) ou auto-évaluation (SAQ) et production de l''attestation de conformité (AOC).', '3-6', 'ROC ou SAQ, AOC', 'QSA, RSSI', 'Standards Hub, CAPA', 6);

-- ============================================================================
-- HDS — Hébergeur de Données de Santé (référentiel ANS v1.1, France)
-- ============================================================================

INSERT INTO standards (id, code, full_name, publisher, current_version, publication_date,
    family, applicable_industries, description, certification_body_required,
    recertification_cycle_months, related_norm_codes, status, created_at, updated_at)
VALUES (
    '90000005-0000-0000-0000-000000000005',
    'hds',
    'Certification HDS — Hébergeur de Données de Santé (référentiel ANS v1.1)',
    'ANS',
    'v1.1',
    DATE '2024-05-01',
    'REGULATORY',
    'healthcare,it',
    'Certification française obligatoire pour tout hébergeur de données de santé à caractère personnel recueillies à l''occasion d''activités de prévention, de diagnostic, de soins ou de suivi médico-social (art. L.1111-8 du Code de la santé publique). Le référentiel de l''Agence du Numérique en Santé (ANS) s''appuie sur un socle ISO 27001 complété d''exigences spécifiques santé et couvre six activités d''hébergement. La certification est accréditée (organisme accrédité COFRAC), avec un cycle de 3 ans et des audits de surveillance annuels. Exigences clés : périmètre des activités d''hébergement, localisation des données dans l''UE, contrat d''hébergement conforme à l''art. L.1111-8 CSP, réversibilité et traçabilité des accès aux données de santé.',
    TRUE,
    NULL,
    'iso-27001,iso-27701,rgpd',
    'PUBLISHED', now(), now()
);

INSERT INTO standard_sections (id, standard_id, code, title, description, order_index) VALUES
    ('95000005-0000-0000-0000-000000000001', '90000005-0000-0000-0000-000000000005', 'SOCLE', 'Socle ISO 27001 (SMSI)', 'Système de management de la sécurité de l''information conforme à ISO 27001.', 1),
    ('95000005-0000-0000-0000-000000000002', '90000005-0000-0000-0000-000000000005', 'ACT', 'Activités d''hébergement', 'Périmètre des six activités d''hébergement de données de santé.', 2),
    ('95000005-0000-0000-0000-000000000003', '90000005-0000-0000-0000-000000000005', 'SPEC', 'Exigences spécifiques santé', 'Localisation UE, contrat, réversibilité et traçabilité des accès aux données de santé.', 3),
    ('95000005-0000-0000-0000-000000000004', '90000005-0000-0000-0000-000000000005', 'CONT', 'Continuité & disponibilité', 'Continuité d''activité et disponibilité du service d''hébergement.', 4);

INSERT INTO standard_clauses (id, section_id, code, title, description, order_index) VALUES
    ('99000005-0000-0000-0000-000000000001', '95000005-0000-0000-0000-000000000001', 'SMSI', 'Système de management ISO 27001', 'Mise en œuvre d''un SMSI certifié couvrant le périmètre HDS.', 1),
    ('99000005-0000-0000-0000-000000000002', '95000005-0000-0000-0000-000000000002', 'ACT.1-6', 'Périmètre des activités', 'Détermination des activités d''hébergement (1 à 6) couvertes par le certificat.', 2),
    ('99000005-0000-0000-0000-000000000003', '95000005-0000-0000-0000-000000000003', 'LOC', 'Localisation des données', 'Localisation des données de santé et des opérations associées au sein de l''UE.', 3),
    ('99000005-0000-0000-0000-000000000004', '95000005-0000-0000-0000-000000000003', 'CTR', 'Contrat d''hébergement', 'Contrat conforme à l''art. L.1111-8 CSP et obligations contractuelles HDS.', 4),
    ('99000005-0000-0000-0000-000000000005', '95000005-0000-0000-0000-000000000003', 'REV', 'Réversibilité', 'Garantie de restitution et de réversibilité des données en fin de contrat.', 5),
    ('99000005-0000-0000-0000-000000000006', '95000005-0000-0000-0000-000000000003', 'TRAC', 'Traçabilité des accès', 'Traçabilité des accès aux données de santé hébergées.', 6),
    ('99000005-0000-0000-0000-000000000007', '95000005-0000-0000-0000-000000000004', 'PCA', 'Continuité & disponibilité', 'Plan de continuité d''activité et engagements de disponibilité.', 7);

INSERT INTO standard_requirements (clause_id, code, text, obligation, evidence_types, measurable_criteria, risk_if_missing, order_index) VALUES
    ('99000005-0000-0000-0000-000000000001', 'SMSI', 'L''hébergeur doit disposer d''un système de management de la sécurité de l''information certifié ISO 27001 couvrant le périmètre d''hébergement des données de santé.', 'MUST', 'DOCUMENT,AUDIT', 'Certificat ISO 27001 en cours de validité couvrant 100 % du périmètre HDS', 'CRITICAL', 1),
    ('99000005-0000-0000-0000-000000000002', 'ACT', 'L''hébergeur doit déterminer et documenter les activités d''hébergement (parmi les six) couvertes par sa certification.', 'MUST', 'DOCUMENT', 'Activités d''hébergement (1 à 6) identifiées et reflétées dans le périmètre certifié', 'HIGH', 2),
    ('99000005-0000-0000-0000-000000000003', 'LOC', 'L''hébergeur doit héberger les données de santé et réaliser les opérations associées au sein de l''Union européenne.', 'MUST', 'DOCUMENT,AUDIT', 'Localisation UE attestée pour 100 % des données de santé et des opérations d''administration', 'CRITICAL', 3),
    ('99000005-0000-0000-0000-000000000004', 'CTR', 'L''hébergeur doit conclure un contrat d''hébergement conforme aux mentions de l''art. L.1111-8 du Code de la santé publique.', 'MUST', 'DOCUMENT', 'Modèle de contrat HDS conforme à l''art. L.1111-8 CSP utilisé avec 100 % des clients', 'HIGH', 4),
    ('99000005-0000-0000-0000-000000000005', 'REV', 'L''hébergeur doit garantir la réversibilité et la restitution des données de santé en fin de contrat.', 'MUST', 'DOCUMENT,AUDIT', 'Procédure de réversibilité documentée et testée ; délais de restitution contractualisés', 'HIGH', 5),
    ('99000005-0000-0000-0000-000000000006', 'TRAC', 'L''hébergeur doit assurer la traçabilité des accès aux données de santé hébergées.', 'MUST', 'DOCUMENT,KPI_RECORD', 'Journaux d''accès aux données de santé conservés ; revue des accès périodique', 'CRITICAL', 6),
    ('99000005-0000-0000-0000-000000000007', 'PCA', 'L''hébergeur doit mettre en œuvre un plan de continuité d''activité et respecter les engagements de disponibilité du service.', 'MUST', 'DOCUMENT,KPI_RECORD', 'PCA documenté et testé au moins annuellement ; taux de disponibilité mesuré vs SLA', 'HIGH', 7),
    ('99000005-0000-0000-0000-000000000007', 'INC', 'L''hébergeur doit notifier au client (et le cas échéant aux autorités) les incidents de sécurité affectant les données de santé.', 'MUST', 'DOCUMENT,CAPA', 'Procédure de notification d''incident en place ; notifications tracées dans les délais contractuels', 'HIGH', 8);

INSERT INTO standard_certification_paths (id, standard_id, estimated_duration_months_min, estimated_duration_months_max,
    estimated_cost_eur_min, estimated_cost_eur_max, difficulty_level,
    surveillance_audit_frequency, recertification_cycle_years, notes)
VALUES ('9e000005-0000-0000-0000-000000000005', '90000005-0000-0000-0000-000000000005',
    9, 18, 25000, 90000, 5, 'annual', 3,
    'Certification accréditée (organisme accrédité COFRAC), obligatoire pour l''hébergement de données de santé (art. L.1111-8 CSP). Socle ISO 27001 complété d''exigences spécifiques santé ; cycle de 3 ans avec audits de surveillance annuels. Synergie avec les modules Audit, Document Control, breach (notification d''incidents) et retention (réversibilité/conservation) de QualitOS.');

INSERT INTO standard_certification_stages (certification_path_id, stage_number, name, description, typical_duration_weeks, deliverables, actors, qualitos_modules, order_index) VALUES
    ('9e000005-0000-0000-0000-000000000005', 1, 'Périmètre & activités d''hébergement', 'Définition du périmètre, des activités d''hébergement (1 à 6) visées et engagement direction.', '3-5', 'Périmètre certifié, liste des activités d''hébergement', 'Direction, RSSI', 'Document Control', 1),
    ('9e000005-0000-0000-0000-000000000005', 2, 'Socle ISO 27001 (SMSI)', 'Mise en place ou extension du SMSI ISO 27001 couvrant le périmètre d''hébergement de données de santé.', '8-16', 'SMSI documenté, SoA, certificat ou plan de certification ISO 27001', 'RSSI', 'Document Control, Risk', 2),
    ('9e000005-0000-0000-0000-000000000005', 3, 'Exigences spécifiques santé', 'Localisation UE des données, contrat conforme art. L.1111-8 CSP, réversibilité et traçabilité des accès.', '4-8', 'Modèle de contrat HDS, procédure de réversibilité, dispositif de traçabilité', 'RSSI, Juridique', 'Document Control, retention', 3),
    ('9e000005-0000-0000-0000-000000000005', 4, 'Continuité, disponibilité & incidents', 'Plan de continuité d''activité, engagements de disponibilité et procédure de notification d''incidents.', '4-8', 'PCA testé, SLA de disponibilité, procédure de notification d''incidents', 'RSSI, Production', 'breach, Risk', 4),
    ('9e000005-0000-0000-0000-000000000005', 5, 'Audit interne & revue de direction', 'Audit interne couvrant socle ISO 27001 et exigences HDS, revue de direction.', '3-5', 'Rapport d''audit interne, compte rendu de revue de direction', 'Auditeur interne, Direction', 'Audit, PDCA', 5),
    ('9e000005-0000-0000-0000-000000000005', 6, 'Audit de certification HDS (COFRAC)', 'Audit documentaire puis audit sur site par l''organisme accrédité, levée des écarts et obtention du certificat.', '4-8', 'Certificat HDS', 'Organisme certificateur accrédité', 'Standards Hub, CAPA', 6);
