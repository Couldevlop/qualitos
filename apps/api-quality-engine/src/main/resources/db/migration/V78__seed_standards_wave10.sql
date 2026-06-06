-- Standards Hub — vague 10 (CLAUDE.md §8.2) : 5 référentiels durabilité / actifs / éducation / public.
--   csrd-esrs  : Directive CSRD (UE) 2022/2464 + normes ESRS — reporting de durabilité (double matérialité)
--   iso-14064  : ISO 14064-1:2018 — quantification et déclaration des émissions de GES
--   iso-55001  : ISO 55001:2014 — systèmes de management de la gestion d'actifs
--   iso-21001  : ISO 21001:2018 — systèmes de management des organismes d'éducation/formation (EOMS)
--   marianne   : Référentiel Marianne (DITP, France) — qualité de l'accueil et du service public
-- Même format que V72 : catalogue platform-level, UUID déterministes, préfixe 'f' (vague 10).
--   standards 'f000000n' (n=1..5), sections 'fn00000s' (préfixe f1..f5),
--   clauses 'fa/fb/fc/fd' (normes 1..4) + 'f9' (5e norme : marianne),
--   paths 'fe00000n', stages via FK (sans UUID explicite).
-- Note unicité : seul V14 emploie le préfixe 'f' (clauses f0000051/61/91/101) — ces valeurs
-- sont distinctes de notre schéma (standards f000000n, sections fn00000s, clauses fa/fb/fc/fd/f9,
-- paths fe00000n). Les stages des vagues précédentes utilisent 7f/8f/9f/af/bf/cf/ef (FK seules).

-- ============================================================================
-- CSRD / ESRS — Directive (UE) 2022/2464 + European Sustainability Reporting Standards
-- ============================================================================

INSERT INTO standards (id, code, full_name, publisher, current_version, publication_date,
    family, applicable_industries, description, certification_body_required,
    recertification_cycle_months, related_norm_codes, status, created_at, updated_at)
VALUES (
    'f0000001-0000-0000-0000-000000000001',
    'csrd-esrs',
    'CSRD / ESRS — Directive (UE) 2022/2464 sur le reporting de durabilité des entreprises et normes européennes ESRS',
    'UE/EFRAG',
    '2022/2464 + ESRS Set 1',
    DATE '2023-07-31',
    'REGULATORY',
    'all',
    'La directive CSRD (Corporate Sustainability Reporting Directive, UE 2022/2464) impose aux entreprises concernées un reporting de durabilité normalisé selon les ESRS (European Sustainability Reporting Standards) élaborés par l''EFRAG et adoptés par actes délégués. Le reporting repose sur le principe de DOUBLE MATÉRIALITÉ (matérialité d''impact et matérialité financière), des informations générales (ESRS 2), des normes thématiques environnementales (E1 climat avec émissions de GES scopes 1, 2 et 3), sociales (S1 effectifs propres) et de gouvernance (G1 conduite des affaires), ainsi qu''un plan de transition et des points de données quantitatifs alignés sur la taxonomie verte de l''UE. Le rapport de durabilité n''est pas « certifié » au sens d''un certificat de management : il fait l''objet d''une ASSURANCE LIMITÉE par un auditeur légal ou un organisme tiers indépendant (OTI), conduisant à une attestation — nuance importante par rapport à une certification accréditée.',
    TRUE,
    NULL,
    'iso-14001,iso-50001',
    'PUBLISHED', now(), now()
);

INSERT INTO standard_sections (id, standard_id, code, title, description, order_index) VALUES
    ('f100000a-0000-0000-0000-000000000001', 'f0000001-0000-0000-0000-000000000001', 'GOV', 'Gouvernance & double matérialité', 'Gouvernance de la durabilité et analyse de double matérialité (impact et financière).', 1),
    ('f100000a-0000-0000-0000-000000000002', 'f0000001-0000-0000-0000-000000000001', 'ESRS2', 'Informations générales (ESRS 2)', 'Exigences transverses de publication : stratégie, modèle d''affaires, IRO, politiques et cibles.', 2),
    ('f100000a-0000-0000-0000-000000000003', 'f0000001-0000-0000-0000-000000000001', 'E1', 'Changement climatique (ESRS E1)', 'Plan de transition et émissions de gaz à effet de serre (scopes 1, 2 et 3).', 3),
    ('f100000a-0000-0000-0000-000000000004', 'f0000001-0000-0000-0000-000000000001', 'S1', 'Effectifs de l''entreprise (ESRS S1)', 'Indicateurs sociaux relatifs aux effectifs propres.', 4),
    ('f100000a-0000-0000-0000-000000000005', 'f0000001-0000-0000-0000-000000000001', 'G1', 'Conduite des affaires (ESRS G1)', 'Éthique des affaires, lutte anti-corruption et culture d''entreprise.', 5),
    ('f100000a-0000-0000-0000-000000000006', 'f0000001-0000-0000-0000-000000000001', 'ASSUR', 'Taxonomie & assurance', 'Alignement taxonomie verte UE et assurance limitée du rapport de durabilité.', 6);

INSERT INTO standard_clauses (id, section_id, code, title, description, order_index) VALUES
    ('fa000001-0000-0000-0000-000000000001', 'f100000a-0000-0000-0000-000000000001', 'GOV.DM', 'Double matérialité', 'Réalisation et documentation de l''analyse de double matérialité (impact + financière).', 1),
    ('fa000001-0000-0000-0000-000000000002', 'f100000a-0000-0000-0000-000000000001', 'GOV.ROL', 'Rôles de gouvernance', 'Rôles des organes d''administration, de direction et de surveillance dans la durabilité.', 2),
    ('fa000001-0000-0000-0000-000000000003', 'f100000a-0000-0000-0000-000000000002', 'ESRS2.IRO', 'Impacts, risques et opportunités', 'Identification et gestion des impacts, risques et opportunités (IRO) de durabilité.', 3),
    ('fa000001-0000-0000-0000-000000000004', 'f100000a-0000-0000-0000-000000000002', 'ESRS2.MDR', 'Politiques, actions et cibles', 'Publication des politiques, actions, cibles et métriques (Minimum Disclosure Requirements).', 4),
    ('fa000001-0000-0000-0000-000000000005', 'f100000a-0000-0000-0000-000000000003', 'E1.GHG', 'Émissions de GES (scopes 1-2-3)', 'Quantification et publication des émissions de GES des scopes 1, 2 et 3.', 5),
    ('fa000001-0000-0000-0000-000000000006', 'f100000a-0000-0000-0000-000000000003', 'E1.TP', 'Plan de transition climat', 'Plan de transition vers une économie compatible avec la limitation à 1,5 °C.', 6),
    ('fa000001-0000-0000-0000-000000000007', 'f100000a-0000-0000-0000-000000000004', 'S1.WF', 'Indicateurs effectifs propres', 'Caractéristiques, conditions de travail et indicateurs des effectifs propres.', 7),
    ('fa000001-0000-0000-0000-000000000008', 'f100000a-0000-0000-0000-000000000005', 'G1.BC', 'Conduite des affaires', 'Culture d''entreprise, prévention de la corruption et des pots-de-vin.', 8),
    ('fa000001-0000-0000-0000-000000000009', 'f100000a-0000-0000-0000-000000000006', 'TAXO', 'Taxonomie verte UE', 'Publication des indicateurs d''alignement à la taxonomie (UE) 2020/852.', 9),
    ('fa000001-0000-0000-0000-000000000010', 'f100000a-0000-0000-0000-000000000006', 'ASSUR', 'Assurance limitée', 'Soumission du rapport de durabilité à une assurance limitée par auditeur/OTI.', 10);

INSERT INTO standard_requirements (clause_id, code, text, obligation, evidence_types, measurable_criteria, risk_if_missing, order_index) VALUES
    ('fa000001-0000-0000-0000-000000000001', 'GOV.DM.1', 'L''entreprise doit réaliser et documenter une analyse de double matérialité identifiant les enjeux de durabilité matériels du point de vue de l''impact et du point de vue financier.', 'MUST', 'DOCUMENT,AUDIT', 'Analyse de double matérialité documentée, seuils définis et revue au moins annuellement', 'CRITICAL', 1),
    ('fa000001-0000-0000-0000-000000000002', 'GOV.ROL.1', 'L''entreprise doit décrire les rôles et responsabilités des organes d''administration, de direction et de surveillance en matière de durabilité.', 'MUST', 'DOCUMENT', 'Rôles de gouvernance durabilité formalisés et publiés dans le rapport', 'MEDIUM', 2),
    ('fa000001-0000-0000-0000-000000000003', 'ESRS2.IRO.1', 'L''entreprise doit identifier et gérer ses impacts, risques et opportunités (IRO) de durabilité et décrire le processus utilisé.', 'MUST', 'DOCUMENT,AUDIT', 'Registre des IRO tenu à jour et relié aux enjeux matériels', 'HIGH', 3),
    ('fa000001-0000-0000-0000-000000000004', 'ESRS2.MDR.1', 'L''entreprise doit publier ses politiques, actions, cibles et métriques pour chaque enjeu matériel selon les exigences minimales de publication.', 'MUST', 'DOCUMENT,KPI_RECORD', 'Politiques, cibles chiffrées et métriques publiées pour 100 % des enjeux matériels', 'HIGH', 4),
    ('fa000001-0000-0000-0000-000000000005', 'E1.GHG.1', 'L''entreprise doit quantifier et publier ses émissions brutes de gaz à effet de serre des scopes 1, 2 et 3 ainsi que ses émissions totales.', 'MUST', 'KPI_RECORD,DOCUMENT', 'Émissions GES scopes 1, 2 et 3 quantifiées (tCO2e), méthodologie documentée et auditable', 'CRITICAL', 5),
    ('fa000001-0000-0000-0000-000000000006', 'E1.TP.1', 'L''entreprise doit publier, le cas échéant, un plan de transition pour l''atténuation du changement climatique compatible avec la limitation du réchauffement à 1,5 °C.', 'SHOULD', 'DOCUMENT', 'Plan de transition climat documenté avec cibles datées et trajectoire de décarbonation', 'HIGH', 6),
    ('fa000001-0000-0000-0000-000000000007', 'S1.WF.1', 'L''entreprise doit publier les indicateurs relatifs à ses effectifs propres (caractéristiques, conditions de travail, égalité).', 'MUST', 'KPI_RECORD,DOCUMENT', 'Indicateurs effectifs propres publiés et ventilés selon les points de données ESRS S1', 'MEDIUM', 7),
    ('fa000001-0000-0000-0000-000000000008', 'G1.BC.1', 'L''entreprise doit publier ses politiques de conduite des affaires, notamment la prévention de la corruption et des pots-de-vin.', 'MUST', 'DOCUMENT,TRAINING_RECORD', 'Politique anti-corruption publiée ; actions de sensibilisation tracées', 'HIGH', 8),
    ('fa000001-0000-0000-0000-000000000009', 'TAXO.1', 'L''entreprise doit publier les indicateurs clés d''alignement de son chiffre d''affaires, de ses CapEx et OpEx à la taxonomie verte de l''UE.', 'MUST', 'KPI_RECORD,DOCUMENT', 'Indicateurs taxonomie (CA, CapEx, OpEx) calculés et publiés, données quantitatives traçables', 'HIGH', 9),
    ('fa000001-0000-0000-0000-000000000010', 'ASSUR.1', 'L''entreprise doit soumettre son rapport de durabilité à une mission d''assurance limitée réalisée par un auditeur légal ou un organisme tiers indépendant.', 'MUST', 'AUDIT,DOCUMENT', 'Attestation d''assurance limitée émise par l''auditeur/OTI sur le rapport de durabilité', 'CRITICAL', 10);

INSERT INTO standard_certification_paths (id, standard_id, estimated_duration_months_min, estimated_duration_months_max,
    estimated_cost_eur_min, estimated_cost_eur_max, difficulty_level,
    surveillance_audit_frequency, recertification_cycle_years, notes)
VALUES ('fe000001-0000-0000-0000-000000000001', 'f0000001-0000-0000-0000-000000000001',
    9, 18, 30000, 250000, 5, 'annual', NULL,
    'Obligation réglementaire (directive CSRD, transposée en droit national). Le rapport de durabilité fait l''objet d''une ASSURANCE LIMITÉE annuelle par un auditeur légal ou un OTI (attestation), et non d''une certification accréditée — d''où une absence de cycle de recertification au sens classique. Synergie avec les modules KPI (points de données quantitatifs, émissions GES), Document Control (politiques et rapport) et Audit (mission d''assurance) de QualitOS.');

INSERT INTO standard_certification_stages (certification_path_id, stage_number, name, description, typical_duration_weeks, deliverables, actors, qualitos_modules, order_index) VALUES
    ('fe000001-0000-0000-0000-000000000001', 1, 'Cadrage & gouvernance durabilité', 'Définition du périmètre de reporting, des rôles de gouvernance et du calendrier de mise en conformité CSRD.', '3-6', 'Périmètre de reporting, gouvernance durabilité, calendrier', 'Direction, Responsable RSE', 'Document Control', 1),
    ('fe000001-0000-0000-0000-000000000001', 2, 'Analyse de double matérialité', 'Réalisation de l''analyse de double matérialité (impact et financière) et identification des enjeux matériels.', '6-10', 'Rapport de double matérialité, liste des enjeux matériels, registre des IRO', 'Responsable RSE, Risk Manager', 'Document Control, Risk', 2),
    ('fe000001-0000-0000-0000-000000000001', 3, 'Collecte des points de données ESRS', 'Mise en place de la collecte des points de données quantitatifs (GES scopes 1-2-3, S1, taxonomie).', '8-16', 'Bilan GES, indicateurs ESRS, indicateurs taxonomie', 'Contrôle de gestion, RSE', 'KPI, Document Control', 3),
    ('fe000001-0000-0000-0000-000000000001', 4, 'Politiques, cibles & plan de transition', 'Formalisation des politiques, actions, cibles chiffrées et du plan de transition climat.', '4-8', 'Politiques de durabilité, cibles, plan de transition climat', 'Responsable RSE, Direction', 'Document Control, PDCA', 4),
    ('fe000001-0000-0000-0000-000000000001', 5, 'Rédaction du rapport de durabilité', 'Rédaction du rapport de durabilité conforme aux ESRS et préparation de la piste d''audit.', '4-8', 'Rapport de durabilité ESRS, piste d''audit des données', 'Responsable RSE, Communication', 'Document Control, Audit', 5),
    ('fe000001-0000-0000-0000-000000000001', 6, 'Assurance limitée (auditeur/OTI)', 'Mission d''assurance limitée du rapport de durabilité par l''auditeur légal ou l''OTI et attestation.', '4-8', 'Attestation d''assurance limitée', 'Auditeur légal / OTI', 'Standards Hub, Audit', 6);

-- ============================================================================
-- ISO 14064-1:2018 — Quantification et déclaration des émissions de GES
-- ============================================================================

INSERT INTO standards (id, code, full_name, publisher, current_version, publication_date,
    family, applicable_industries, description, certification_body_required,
    recertification_cycle_months, related_norm_codes, status, created_at, updated_at)
VALUES (
    'f0000002-0000-0000-0000-000000000002',
    'iso-14064',
    'ISO 14064-1:2018 — Gaz à effet de serre — Partie 1 : Spécifications et lignes directrices, au niveau des organismes, pour la quantification et la déclaration des émissions et des suppressions des GES',
    'ISO',
    '2018',
    DATE '2018-12-19',
    'SECTORIEL',
    'all',
    'ISO 14064-1:2018 spécifie les principes et exigences, au niveau de l''organisme, pour la quantification et la déclaration des émissions et des suppressions de gaz à effet de serre (GES). La norme structure l''inventaire autour des périmètres organisationnels (méthode de consolidation : contrôle ou part de capital), des catégories d''émissions directes et indirectes (la version 2018 introduit six catégories en remplacement des scopes 1/2/3 historiques), des facteurs d''émission documentés et traçables, du traitement des incertitudes et d''un rapport GES vérifiable. Un inventaire est établi sur une année de référence (base year) servant de comparaison. La conformité fait l''objet d''une VÉRIFICATION (et non d''une certification de système de management) par un organisme accrédité, le plus souvent selon ISO 14064-3, conduisant à une déclaration de vérification au niveau d''assurance raisonnable ou limité.',
    TRUE,
    NULL,
    'iso-14001,csrd-esrs',
    'PUBLISHED', now(), now()
);

INSERT INTO standard_sections (id, standard_id, code, title, description, order_index) VALUES
    ('f200000b-0000-0000-0000-000000000001', 'f0000002-0000-0000-0000-000000000002', 'BND', 'Périmètres & limites', 'Périmètres organisationnels et limites de déclaration de l''inventaire GES.', 1),
    ('f200000b-0000-0000-0000-000000000002', 'f0000002-0000-0000-0000-000000000002', 'QUANT', 'Quantification des émissions', 'Catégories d''émissions directes/indirectes, facteurs d''émission et année de référence.', 2),
    ('f200000b-0000-0000-0000-000000000003', 'f0000002-0000-0000-0000-000000000002', 'QUAL', 'Qualité de l''inventaire', 'Gestion des incertitudes, qualité des données et gestion de l''inventaire.', 3),
    ('f200000b-0000-0000-0000-000000000004', 'f0000002-0000-0000-0000-000000000002', 'REP', 'Rapport & vérification', 'Rapport GES et vérification par un organisme accrédité.', 4);

INSERT INTO standard_clauses (id, section_id, code, title, description, order_index) VALUES
    ('fb000002-0000-0000-0000-000000000001', 'f200000b-0000-0000-0000-000000000001', 'ORG', 'Périmètre organisationnel', 'Définition du périmètre organisationnel et de la méthode de consolidation.', 1),
    ('fb000002-0000-0000-0000-000000000002', 'f200000b-0000-0000-0000-000000000001', 'BND', 'Limites de déclaration', 'Détermination des limites de déclaration et des émissions/suppressions incluses.', 2),
    ('fb000002-0000-0000-0000-000000000003', 'f200000b-0000-0000-0000-000000000002', 'DIR', 'Émissions directes', 'Quantification des émissions directes de GES (catégorie 1).', 3),
    ('fb000002-0000-0000-0000-000000000004', 'f200000b-0000-0000-0000-000000000002', 'IND', 'Émissions indirectes', 'Quantification des émissions indirectes (énergie importée et autres catégories).', 4),
    ('fb000002-0000-0000-0000-000000000005', 'f200000b-0000-0000-0000-000000000002', 'EF', 'Facteurs d''émission', 'Sélection, documentation et traçabilité des facteurs d''émission.', 5),
    ('fb000002-0000-0000-0000-000000000006', 'f200000b-0000-0000-0000-000000000002', 'BY', 'Année de référence', 'Établissement et recalcul de l''année de référence (base year).', 6),
    ('fb000002-0000-0000-0000-000000000007', 'f200000b-0000-0000-0000-000000000003', 'UNC', 'Incertitudes', 'Évaluation et traitement des incertitudes de quantification.', 7),
    ('fb000002-0000-0000-0000-000000000008', 'f200000b-0000-0000-0000-000000000004', 'RPT', 'Rapport GES', 'Élaboration du rapport GES conforme aux exigences de déclaration.', 8),
    ('fb000002-0000-0000-0000-000000000009', 'f200000b-0000-0000-0000-000000000004', 'VER', 'Vérification', 'Vérification de l''inventaire par un organisme accrédité.', 9);

INSERT INTO standard_requirements (clause_id, code, text, obligation, evidence_types, measurable_criteria, risk_if_missing, order_index) VALUES
    ('fb000002-0000-0000-0000-000000000001', 'ORG.1', 'L''organisme doit définir son périmètre organisationnel et la méthode de consolidation des émissions (contrôle ou part de capital).', 'MUST', 'DOCUMENT', 'Périmètre organisationnel et méthode de consolidation documentés et justifiés', 'CRITICAL', 1),
    ('fb000002-0000-0000-0000-000000000002', 'BND.1', 'L''organisme doit déterminer les limites de déclaration et identifier les émissions et suppressions directes et indirectes significatives.', 'MUST', 'DOCUMENT,AUDIT', 'Limites de déclaration documentées ; critères de significativité définis', 'HIGH', 2),
    ('fb000002-0000-0000-0000-000000000003', 'DIR.1', 'L''organisme doit quantifier ses émissions directes de GES à partir de données d''activité et de facteurs d''émission appropriés.', 'MUST', 'KPI_RECORD,DOCUMENT', 'Émissions directes (catégorie 1) quantifiées en tCO2e avec données d''activité tracées', 'CRITICAL', 3),
    ('fb000002-0000-0000-0000-000000000004', 'IND.1', 'L''organisme doit quantifier ses émissions indirectes (énergie importée et autres catégories pertinentes).', 'MUST', 'KPI_RECORD,DOCUMENT', 'Émissions indirectes quantifiées par catégorie ; périmètre des catégories justifié', 'HIGH', 4),
    ('fb000002-0000-0000-0000-000000000005', 'EF.1', 'L''organisme doit documenter les facteurs d''émission utilisés et leur source, et en assurer la traçabilité.', 'MUST', 'DOCUMENT', 'Facteurs d''émission documentés, datés et reliés à une source reconnue', 'HIGH', 5),
    ('fb000002-0000-0000-0000-000000000006', 'BY.1', 'L''organisme doit établir une année de référence et définir une politique de recalcul en cas de changements significatifs.', 'MUST', 'DOCUMENT', 'Année de référence établie ; seuil et procédure de recalcul documentés', 'MEDIUM', 6),
    ('fb000002-0000-0000-0000-000000000007', 'UNC.1', 'L''organisme doit évaluer les incertitudes associées à la quantification des émissions et suppressions.', 'MUST', 'DOCUMENT', 'Évaluation des incertitudes documentée par catégorie d''émission', 'MEDIUM', 7),
    ('fb000002-0000-0000-0000-000000000008', 'RPT.1', 'L''organisme doit élaborer un rapport GES contenant les informations de déclaration requises par la norme.', 'MUST', 'DOCUMENT,KPI_RECORD', 'Rapport GES complet (périmètres, méthodes, résultats, incertitudes) produit annuellement', 'HIGH', 8),
    ('fb000002-0000-0000-0000-000000000009', 'VER.1', 'L''organisme doit soumettre son inventaire GES à une vérification par un organisme accrédité afin d''obtenir une assurance sur les déclarations.', 'SHOULD', 'AUDIT,DOCUMENT', 'Déclaration de vérification (assurance limitée ou raisonnable) émise par un organisme accrédité', 'HIGH', 9);

INSERT INTO standard_certification_paths (id, standard_id, estimated_duration_months_min, estimated_duration_months_max,
    estimated_cost_eur_min, estimated_cost_eur_max, difficulty_level,
    surveillance_audit_frequency, recertification_cycle_years, notes)
VALUES ('fe000002-0000-0000-0000-000000000002', 'f0000002-0000-0000-0000-000000000002',
    4, 9, 8000, 50000, 3, 'annual', NULL,
    'ISO 14064-1 fait l''objet d''une VÉRIFICATION de l''inventaire GES (généralement selon ISO 14064-3) par un organisme accrédité, et non d''une certification de système de management — la reconnaissance prend la forme d''une déclaration de vérification (assurance limitée ou raisonnable), réévaluée à chaque exercice. Synergie avec les modules KPI (quantification des émissions, suivi annuel) et Document Control (rapport GES, facteurs d''émission) de QualitOS.');

INSERT INTO standard_certification_stages (certification_path_id, stage_number, name, description, typical_duration_weeks, deliverables, actors, qualitos_modules, order_index) VALUES
    ('fe000002-0000-0000-0000-000000000002', 1, 'Périmètres & limites', 'Définition du périmètre organisationnel, de la méthode de consolidation et des limites de déclaration.', '2-4', 'Note de périmètre, méthode de consolidation', 'Responsable carbone, Direction', 'Document Control', 1),
    ('fe000002-0000-0000-0000-000000000002', 2, 'Collecte des données d''activité', 'Recueil des données d''activité par source et sélection des facteurs d''émission.', '4-8', 'Tableau des données d''activité, registre des facteurs d''émission', 'Contrôle de gestion, HSE', 'KPI, Document Control', 2),
    ('fe000002-0000-0000-0000-000000000002', 3, 'Quantification & incertitudes', 'Calcul des émissions directes et indirectes, établissement de l''année de référence et évaluation des incertitudes.', '3-6', 'Inventaire GES, année de référence, analyse d''incertitude', 'Responsable carbone', 'KPI, Document Control', 3),
    ('fe000002-0000-0000-0000-000000000002', 4, 'Rapport GES', 'Rédaction du rapport GES conforme aux exigences de déclaration de la norme.', '2-4', 'Rapport GES', 'Responsable carbone', 'Document Control, KPI', 4),
    ('fe000002-0000-0000-0000-000000000002', 5, 'Revue interne & piste d''audit', 'Revue interne de l''inventaire et préparation de la piste d''audit pour la vérification.', '2-3', 'Revue interne, piste d''audit', 'Responsable qualité', 'Audit, Document Control', 5),
    ('fe000002-0000-0000-0000-000000000002', 6, 'Vérification par organisme accrédité', 'Vérification de l''inventaire GES par un organisme accrédité et déclaration de vérification.', '3-6', 'Déclaration de vérification GES', 'Organisme de vérification', 'Standards Hub, Audit', 6);

-- ============================================================================
-- ISO 55001:2014 — Systèmes de management de la gestion d'actifs
-- ============================================================================

INSERT INTO standards (id, code, full_name, publisher, current_version, publication_date,
    family, applicable_industries, description, certification_body_required,
    recertification_cycle_months, related_norm_codes, status, created_at, updated_at)
VALUES (
    'f0000003-0000-0000-0000-000000000003',
    'iso-55001',
    'ISO 55001:2014 — Gestion d''actifs — Systèmes de management — Exigences',
    'ISO',
    '2014',
    DATE '2014-01-15',
    'HLS',
    'energy,manufacturing,utilities,transport',
    'ISO 55001:2014 spécifie les exigences pour établir, mettre en œuvre, tenir à jour et améliorer un système de management de la gestion d''actifs (asset management). Construite sur la structure de haut niveau (HLS) des normes de management, elle vise à dégager de la valeur des actifs en équilibrant coûts, risques et performance sur l''ensemble du cycle de vie. Les exigences clés portent sur le plan stratégique de gestion d''actifs (SAMP) aligné sur les objectifs organisationnels, la gestion du cycle de vie des actifs, l''évaluation de la criticité des actifs, des plans de maintenance alignés sur les risques, la fiabilité de l''information d''actifs et des indicateurs de performance d''actifs. La certification, accréditée, suit un cycle de 3 ans avec audits de surveillance annuels.',
    TRUE,
    NULL,
    'iso-9001,iso-31000',
    'PUBLISHED', now(), now()
);

INSERT INTO standard_sections (id, standard_id, code, title, description, order_index) VALUES
    ('f300000c-0000-0000-0000-000000000001', 'f0000003-0000-0000-0000-000000000003', '5', 'Leadership & SAMP', 'Leadership, politique de gestion d''actifs et plan stratégique (SAMP).', 1),
    ('f300000c-0000-0000-0000-000000000002', 'f0000003-0000-0000-0000-000000000003', '6', 'Planification', 'Objectifs de gestion d''actifs, planification et prise en compte des risques.', 2),
    ('f300000c-0000-0000-0000-000000000003', 'f0000003-0000-0000-0000-000000000003', '7-8', 'Support & réalisation', 'Information d''actifs, ressources, cycle de vie et maîtrise opérationnelle.', 3),
    ('f300000c-0000-0000-0000-000000000004', 'f0000003-0000-0000-0000-000000000003', '9', 'Évaluation des performances', 'Surveillance, indicateurs de performance d''actifs et revue de direction.', 4);

INSERT INTO standard_clauses (id, section_id, code, title, description, order_index) VALUES
    ('fc000003-0000-0000-0000-000000000001', 'f300000c-0000-0000-0000-000000000001', '5.2', 'Politique de gestion d''actifs', 'Établissement d''une politique de gestion d''actifs alignée sur les objectifs organisationnels.', 1),
    ('fc000003-0000-0000-0000-000000000002', 'f300000c-0000-0000-0000-000000000001', 'SAMP', 'Plan stratégique (SAMP)', 'Élaboration du plan stratégique de gestion d''actifs (SAMP).', 2),
    ('fc000003-0000-0000-0000-000000000003', 'f300000c-0000-0000-0000-000000000002', '6.2.2', 'Plans de gestion d''actifs', 'Définition des plans de gestion d''actifs et alignement sur les objectifs.', 3),
    ('fc000003-0000-0000-0000-000000000004', 'f300000c-0000-0000-0000-000000000002', 'CRIT', 'Criticité des actifs', 'Évaluation de la criticité des actifs et priorisation associée.', 4),
    ('fc000003-0000-0000-0000-000000000005', 'f300000c-0000-0000-0000-000000000002', 'RISK', 'Risques liés aux actifs', 'Identification et traitement des risques sur le cycle de vie des actifs.', 5),
    ('fc000003-0000-0000-0000-000000000006', 'f300000c-0000-0000-0000-000000000003', 'LCM', 'Cycle de vie des actifs', 'Maîtrise des activités sur l''ensemble du cycle de vie des actifs.', 6),
    ('fc000003-0000-0000-0000-000000000007', 'f300000c-0000-0000-0000-000000000003', 'MAINT', 'Plans de maintenance', 'Plans de maintenance alignés sur la criticité et les risques.', 7),
    ('fc000003-0000-0000-0000-000000000008', 'f300000c-0000-0000-0000-000000000003', 'INFO', 'Information d''actifs', 'Détermination et fiabilité de l''information de gestion d''actifs.', 8),
    ('fc000003-0000-0000-0000-000000000009', 'f300000c-0000-0000-0000-000000000004', '9.1', 'Performance des actifs', 'Surveillance et mesure de la performance des actifs et du système.', 9);

INSERT INTO standard_requirements (clause_id, code, text, obligation, evidence_types, measurable_criteria, risk_if_missing, order_index) VALUES
    ('fc000003-0000-0000-0000-000000000001', '5.2.1', 'La direction doit établir une politique de gestion d''actifs cohérente avec les objectifs organisationnels et le plan stratégique.', 'MUST', 'DOCUMENT', 'Politique de gestion d''actifs approuvée, diffusée et revue périodiquement', 'HIGH', 1),
    ('fc000003-0000-0000-0000-000000000002', 'SAMP.1', 'L''organisme doit élaborer un plan stratégique de gestion d''actifs (SAMP) qui décline les objectifs organisationnels en objectifs de gestion d''actifs.', 'MUST', 'DOCUMENT,AUDIT', 'SAMP documenté reliant objectifs organisationnels et objectifs de gestion d''actifs', 'CRITICAL', 2),
    ('fc000003-0000-0000-0000-000000000003', '6.2.2', 'L''organisme doit établir des plans de gestion d''actifs pour atteindre les objectifs de gestion d''actifs.', 'MUST', 'DOCUMENT', 'Plans de gestion d''actifs définis, planifiés et ressourcés', 'HIGH', 3),
    ('fc000003-0000-0000-0000-000000000004', 'CRIT.1', 'L''organisme doit évaluer la criticité de ses actifs afin de prioriser les décisions de gestion et de maintenance.', 'MUST', 'DOCUMENT,KPI_RECORD', 'Criticité évaluée pour 100 % des actifs majeurs ; matrice de criticité tenue à jour', 'HIGH', 4),
    ('fc000003-0000-0000-0000-000000000005', 'RISK.1', 'L''organisme doit identifier et traiter les risques liés aux actifs sur l''ensemble de leur cycle de vie.', 'MUST', 'DOCUMENT,AUDIT', 'Registre des risques d''actifs tenu à jour ; actions de traitement suivies', 'HIGH', 5),
    ('fc000003-0000-0000-0000-000000000006', 'LCM.1', 'L''organisme doit maîtriser les activités de gestion d''actifs sur l''ensemble du cycle de vie (acquisition, exploitation, maintenance, renouvellement, mise au rebut).', 'MUST', 'DOCUMENT,AUDIT', 'Processus de cycle de vie documentés et appliqués pour les actifs critiques', 'HIGH', 6),
    ('fc000003-0000-0000-0000-000000000007', 'MAINT.1', 'L''organisme doit définir des plans de maintenance alignés sur la criticité et les risques des actifs.', 'MUST', 'DOCUMENT,KPI_RECORD', 'Plans de maintenance déployés pour les actifs critiques ; taux de réalisation suivi', 'HIGH', 7),
    ('fc000003-0000-0000-0000-000000000008', 'INFO.1', 'L''organisme doit déterminer ses besoins en information d''actifs et en assurer l''exactitude et la cohérence.', 'MUST', 'DOCUMENT,KPI_RECORD', 'Référentiel d''actifs maintenu ; qualité/complétude des données d''actifs mesurée', 'MEDIUM', 8),
    ('fc000003-0000-0000-0000-000000000009', '9.1.1', 'L''organisme doit surveiller et mesurer la performance des actifs et du système de management de la gestion d''actifs.', 'MUST', 'KPI_RECORD,DOCUMENT', 'Indicateurs de performance d''actifs définis et suivis (disponibilité, fiabilité, coûts)', 'HIGH', 9);

INSERT INTO standard_certification_paths (id, standard_id, estimated_duration_months_min, estimated_duration_months_max,
    estimated_cost_eur_min, estimated_cost_eur_max, difficulty_level,
    surveillance_audit_frequency, recertification_cycle_years, notes)
VALUES ('fe000003-0000-0000-0000-000000000003', 'f0000003-0000-0000-0000-000000000003',
    9, 18, 20000, 100000, 4, 'annual', 3,
    'Certification accréditée selon la structure HLS ; cycle de 3 ans avec audits de surveillance annuels. Le système peut être intégré avec ISO 9001 et ISO 31000 (mutualisation des clauses HLS et du management du risque). Synergie avec les modules Calibration/Equipment (gestion des équipements), IoT (données d''actifs en temps réel), Risk (criticité et risques) et KPI (performance des actifs) de QualitOS.');

INSERT INTO standard_certification_stages (certification_path_id, stage_number, name, description, typical_duration_weeks, deliverables, actors, qualitos_modules, order_index) VALUES
    ('fe000003-0000-0000-0000-000000000003', 1, 'Cadrage & politique d''actifs', 'Définition du périmètre, de la politique de gestion d''actifs et de l''engagement de la direction.', '3-5', 'Périmètre, politique de gestion d''actifs', 'Direction, Asset Manager', 'Document Control', 1),
    ('fe000003-0000-0000-0000-000000000003', 2, 'SAMP & objectifs d''actifs', 'Élaboration du plan stratégique de gestion d''actifs (SAMP) et des objectifs de gestion d''actifs.', '4-8', 'SAMP, objectifs de gestion d''actifs, plans de gestion d''actifs', 'Asset Manager', 'Document Control, KPI', 2),
    ('fe000003-0000-0000-0000-000000000003', 3, 'Criticité & risques d''actifs', 'Évaluation de la criticité des actifs et identification/traitement des risques sur le cycle de vie.', '4-8', 'Matrice de criticité, registre des risques d''actifs', 'Asset Manager, Risk Manager', 'Risk, Calibration', 3),
    ('fe000003-0000-0000-0000-000000000003', 4, 'Cycle de vie, maintenance & information', 'Mise en place des processus de cycle de vie, des plans de maintenance et du référentiel d''information d''actifs.', '8-16', 'Processus cycle de vie, plans de maintenance, référentiel d''actifs', 'Maintenance, IT', 'Calibration, IoT', 4),
    ('fe000003-0000-0000-0000-000000000003', 5, 'Audit interne & revue de direction', 'Audit interne du système de management de la gestion d''actifs et revue de direction.', '3-5', 'Rapport d''audit interne, compte rendu de revue de direction', 'Auditeur interne, Direction', 'Audit, PDCA', 5),
    ('fe000003-0000-0000-0000-000000000003', 6, 'Audit de certification (étapes 1 & 2)', 'Audit documentaire puis audit sur site par l''organisme certificateur, levée des non-conformités.', '4-8', 'Certificat ISO 55001', 'Organisme certificateur', 'Standards Hub, CAPA', 6);

-- ============================================================================
-- ISO 21001:2018 — Organismes d'éducation/formation (EOMS)
-- ============================================================================

INSERT INTO standards (id, code, full_name, publisher, current_version, publication_date,
    family, applicable_industries, description, certification_body_required,
    recertification_cycle_months, related_norm_codes, status, created_at, updated_at)
VALUES (
    'f0000004-0000-0000-0000-000000000004',
    'iso-21001',
    'ISO 21001:2018 — Organismes d''éducation/formation — Systèmes de management des organismes d''éducation/formation — Exigences avec recommandations pour leur application',
    'ISO',
    '2018',
    DATE '2018-05-01',
    'HLS',
    'education',
    'ISO 21001:2018 spécifie les exigences d''un système de management des organismes d''éducation/formation (EOMS — Educational Organizations Management System). Fondée sur la structure de haut niveau (HLS), la norme est centrée sur les apprenants et les autres bénéficiaires : elle exige la détermination des besoins et attentes des apprenants (y compris ceux ayant des besoins éducatifs particuliers), la conception et la maîtrise des programmes d''éducation/formation, l''évaluation des acquis d''apprentissage, le recueil et l''exploitation des retours des apprenants et des bénéficiaires, ainsi que la compétence et la disponibilité d''un personnel qualifié. La certification, accréditée, suit un cycle de 3 ans avec audits de surveillance annuels.',
    TRUE,
    NULL,
    'qualiopi,iso-9001',
    'PUBLISHED', now(), now()
);

INSERT INTO standard_sections (id, standard_id, code, title, description, order_index) VALUES
    ('f400000d-0000-0000-0000-000000000001', 'f0000004-0000-0000-0000-000000000004', '4', 'Contexte & bénéficiaires', 'Compréhension des besoins des apprenants et des autres bénéficiaires.', 1),
    ('f400000d-0000-0000-0000-000000000002', 'f0000004-0000-0000-0000-000000000004', '7', 'Support & ressources', 'Personnel compétent et ressources pour les activités d''éducation/formation.', 2),
    ('f400000d-0000-0000-0000-000000000003', 'f0000004-0000-0000-0000-000000000004', '8', 'Réalisation des activités', 'Conception, prestation et évaluation des programmes d''éducation/formation.', 3),
    ('f400000d-0000-0000-0000-000000000004', 'f0000004-0000-0000-0000-000000000004', '9', 'Évaluation des performances', 'Satisfaction des apprenants, surveillance et amélioration de l''EOMS.', 4);

INSERT INTO standard_clauses (id, section_id, code, title, description, order_index) VALUES
    ('fd000004-0000-0000-0000-000000000001', 'f400000d-0000-0000-0000-000000000001', '4.2', 'Besoins des apprenants', 'Détermination des besoins et attentes des apprenants et bénéficiaires.', 1),
    ('fd000004-0000-0000-0000-000000000002', 'f400000d-0000-0000-0000-000000000001', '4.2.S', 'Besoins éducatifs particuliers', 'Prise en compte des apprenants ayant des besoins éducatifs particuliers.', 2),
    ('fd000004-0000-0000-0000-000000000003', 'f400000d-0000-0000-0000-000000000002', '7.2', 'Compétence du personnel', 'Compétence, qualification et développement du personnel éducatif.', 3),
    ('fd000004-0000-0000-0000-000000000004', 'f400000d-0000-0000-0000-000000000003', '8.3', 'Conception des programmes', 'Conception et développement des programmes d''éducation/formation.', 4),
    ('fd000004-0000-0000-0000-000000000005', 'f400000d-0000-0000-0000-000000000003', '8.5', 'Prestation pédagogique', 'Maîtrise de la prestation des activités d''éducation/formation.', 5),
    ('fd000004-0000-0000-0000-000000000006', 'f400000d-0000-0000-0000-000000000003', '8.6', 'Évaluation des acquis', 'Évaluation des acquis d''apprentissage des apprenants.', 6),
    ('fd000004-0000-0000-0000-000000000007', 'f400000d-0000-0000-0000-000000000004', '9.1.2', 'Retour des apprenants', 'Recueil et exploitation de la satisfaction des apprenants et bénéficiaires.', 7);

INSERT INTO standard_requirements (clause_id, code, text, obligation, evidence_types, measurable_criteria, risk_if_missing, order_index) VALUES
    ('fd000004-0000-0000-0000-000000000001', '4.2.1', 'L''organisme doit déterminer les besoins et attentes des apprenants et des autres bénéficiaires pertinents pour son EOMS.', 'MUST', 'DOCUMENT,AUDIT', 'Besoins des apprenants et bénéficiaires identifiés et revus périodiquement', 'HIGH', 1),
    ('fd000004-0000-0000-0000-000000000002', '4.2.S', 'L''organisme doit identifier et prendre en compte les besoins des apprenants ayant des besoins éducatifs particuliers.', 'MUST', 'DOCUMENT', 'Dispositif d''accueil des besoins particuliers documenté et appliqué', 'MEDIUM', 2),
    ('fd000004-0000-0000-0000-000000000003', '7.2.1', 'L''organisme doit s''assurer que le personnel intervenant dans l''éducation/formation est compétent et maintient ses compétences.', 'MUST', 'TRAINING_RECORD,DOCUMENT', 'Compétences du personnel évaluées ; plan de développement professionnel suivi', 'HIGH', 3),
    ('fd000004-0000-0000-0000-000000000004', '8.3.1', 'L''organisme doit concevoir et développer les programmes d''éducation/formation en réponse aux besoins des apprenants.', 'MUST', 'DOCUMENT,AUDIT', 'Programmes conçus avec objectifs d''apprentissage définis et revus', 'HIGH', 4),
    ('fd000004-0000-0000-0000-000000000005', '8.5.1', 'L''organisme doit maîtriser la prestation des activités d''éducation/formation conformément aux programmes prévus.', 'MUST', 'DOCUMENT', 'Prestation pédagogique tracée et conforme aux programmes définis', 'MEDIUM', 5),
    ('fd000004-0000-0000-0000-000000000006', '8.6.1', 'L''organisme doit évaluer les acquis d''apprentissage des apprenants selon des critères définis.', 'MUST', 'DOCUMENT,KPI_RECORD', 'Méthodes d''évaluation des acquis définies ; taux de réussite/atteinte des objectifs suivi', 'HIGH', 6),
    ('fd000004-0000-0000-0000-000000000007', '9.1.2', 'L''organisme doit recueillir et analyser la satisfaction et les retours des apprenants et des autres bénéficiaires.', 'MUST', 'KPI_RECORD,DOCUMENT', 'Enquêtes de satisfaction réalisées ; taux de satisfaction des apprenants suivi et exploité', 'HIGH', 7),
    ('fd000004-0000-0000-0000-000000000007', '10.2', 'L''organisme doit traiter les réclamations des apprenants et bénéficiaires et mettre en œuvre les actions correctives associées.', 'MUST', 'CAPA,DOCUMENT', 'Réclamations tracées et traitées ; délai moyen de traitement suivi', 'MEDIUM', 8);

INSERT INTO standard_certification_paths (id, standard_id, estimated_duration_months_min, estimated_duration_months_max,
    estimated_cost_eur_min, estimated_cost_eur_max, difficulty_level,
    surveillance_audit_frequency, recertification_cycle_years, notes)
VALUES ('fe000004-0000-0000-0000-000000000004', 'f0000004-0000-0000-0000-000000000004',
    6, 12, 10000, 45000, 3, 'annual', 3,
    'Certification accréditée selon la structure HLS ; cycle de 3 ans avec audits de surveillance annuels. Complémentaire de Qualiopi (France) et compatible avec ISO 9001 (mutualisation des clauses HLS). Synergie avec les modules Training (compétences, parcours), Complaints (réclamations apprenants) et KPI (satisfaction, acquis d''apprentissage) de QualitOS.');

INSERT INTO standard_certification_stages (certification_path_id, stage_number, name, description, typical_duration_weeks, deliverables, actors, qualitos_modules, order_index) VALUES
    ('fe000004-0000-0000-0000-000000000004', 1, 'Cadrage & contexte EOMS', 'Définition du périmètre, du contexte et des besoins des apprenants et bénéficiaires.', '3-5', 'Périmètre EOMS, analyse des besoins des apprenants', 'Direction, Responsable pédagogique', 'Document Control', 1),
    ('fe000004-0000-0000-0000-000000000004', 2, 'Conception des programmes', 'Conception et développement des programmes d''éducation/formation et de leurs objectifs.', '4-8', 'Référentiels de programmes, objectifs d''apprentissage', 'Responsable pédagogique', 'Document Control, Training', 2),
    ('fe000004-0000-0000-0000-000000000004', 3, 'Compétences & ressources', 'Évaluation des compétences du personnel et mise à disposition des ressources pédagogiques.', '4-6', 'Matrice de compétences, plan de développement professionnel', 'RH, Responsable pédagogique', 'Training', 3),
    ('fe000004-0000-0000-0000-000000000004', 4, 'Prestation & évaluation des acquis', 'Maîtrise de la prestation pédagogique et évaluation des acquis d''apprentissage.', '6-10', 'Dispositifs d''évaluation, relevés d''acquis', 'Formateurs, Responsable pédagogique', 'KPI, Document Control', 4),
    ('fe000004-0000-0000-0000-000000000004', 5, 'Satisfaction & amélioration', 'Recueil de la satisfaction, traitement des réclamations et audit interne de l''EOMS.', '3-5', 'Enquêtes de satisfaction, registre des réclamations, rapport d''audit interne', 'Responsable qualité', 'Complaints, Audit', 5),
    ('fe000004-0000-0000-0000-000000000004', 6, 'Audit de certification (étapes 1 & 2)', 'Audit documentaire puis audit sur site par l''organisme certificateur, levée des non-conformités.', '4-8', 'Certificat ISO 21001', 'Organisme certificateur', 'Standards Hub, CAPA', 6);

-- ============================================================================
-- Marianne — Référentiel qualité de l'accueil et du service public (DITP, France)
-- ============================================================================

INSERT INTO standards (id, code, full_name, publisher, current_version, publication_date,
    family, applicable_industries, description, certification_body_required,
    recertification_cycle_months, related_norm_codes, status, created_at, updated_at)
VALUES (
    'f0000005-0000-0000-0000-000000000005',
    'marianne',
    'Référentiel Marianne — Qualité de l''accueil et de la relation usager dans les services publics',
    'DITP',
    '2023',
    DATE '2023-01-01',
    'REGULATORY',
    'public',
    'Le référentiel Marianne, piloté par la Direction interministérielle de la transformation publique (DITP), définit les engagements de qualité de l''accueil et de la relation usager pour les services publics français. Il ne s''agit pas d''une certification accréditée au sens ISO : la reconnaissance prend la forme d''une LABELLISATION possible (label Marianne), obtenue via des audits/évaluations menés par un tiers, ou d''un déploiement volontaire piloté par l''administration. Les engagements couvrent notamment des délais de réponse (réponse au courrier sous 15 jours ouvrés, réponse aux courriels sous 7 jours), un accueil téléphonique de qualité (prise en charge en moins de 5 sonneries), des horaires d''ouverture adaptés aux usagers, l''accessibilité de l''accueil (y compris aux personnes en situation de handicap), la prise en compte des avis des usagers et la formation des agents à la relation usager.',
    FALSE,
    NULL,
    'iso-9001,iso-10002',
    'PUBLISHED', now(), now()
);

INSERT INTO standard_sections (id, standard_id, code, title, description, order_index) VALUES
    ('f500000e-0000-0000-0000-000000000001', 'f0000005-0000-0000-0000-000000000005', 'ACCES', 'Accès & accueil', 'Accessibilité du service, horaires adaptés et qualité de l''accueil physique.', 1),
    ('f500000e-0000-0000-0000-000000000002', 'f0000005-0000-0000-0000-000000000005', 'REPONSE', 'Délais de réponse', 'Engagements de délais sur les courriers, courriels et appels téléphoniques.', 2),
    ('f500000e-0000-0000-0000-000000000003', 'f0000005-0000-0000-0000-000000000005', 'ECOUTE', 'Écoute & amélioration', 'Prise en compte des avis des usagers et amélioration continue du service.', 3),
    ('f500000e-0000-0000-0000-000000000004', 'f0000005-0000-0000-0000-000000000005', 'AGENTS', 'Compétence des agents', 'Formation et professionnalisation des agents à la relation usager.', 4);

INSERT INTO standard_clauses (id, section_id, code, title, description, order_index) VALUES
    ('f9000005-0000-0000-0000-000000000001', 'f500000e-0000-0000-0000-000000000001', 'ACC.H', 'Horaires adaptés', 'Horaires d''ouverture adaptés aux besoins des usagers.', 1),
    ('f9000005-0000-0000-0000-000000000002', 'f500000e-0000-0000-0000-000000000001', 'ACC.A', 'Accessibilité de l''accueil', 'Accessibilité de l''accueil, y compris aux personnes en situation de handicap.', 2),
    ('f9000005-0000-0000-0000-000000000003', 'f500000e-0000-0000-0000-000000000002', 'REP.C', 'Réponse au courrier', 'Réponse aux courriers dans le délai d''engagement.', 3),
    ('f9000005-0000-0000-0000-000000000004', 'f500000e-0000-0000-0000-000000000002', 'REP.M', 'Réponse aux courriels', 'Réponse aux courriels dans le délai d''engagement.', 4),
    ('f9000005-0000-0000-0000-000000000005', 'f500000e-0000-0000-0000-000000000002', 'REP.T', 'Accueil téléphonique', 'Prise en charge téléphonique de qualité dans le délai d''engagement.', 5),
    ('f9000005-0000-0000-0000-000000000006', 'f500000e-0000-0000-0000-000000000003', 'ECO.A', 'Avis des usagers', 'Recueil et prise en compte des avis et réclamations des usagers.', 6),
    ('f9000005-0000-0000-0000-000000000007', 'f500000e-0000-0000-0000-000000000004', 'AGT.F', 'Formation des agents', 'Formation des agents à l''accueil et à la relation usager.', 7);

INSERT INTO standard_requirements (clause_id, code, text, obligation, evidence_types, measurable_criteria, risk_if_missing, order_index) VALUES
    ('f9000005-0000-0000-0000-000000000001', 'ACC.H.1', 'Le service doit proposer des horaires d''ouverture adaptés aux besoins des usagers et les communiquer clairement.', 'MUST', 'DOCUMENT', 'Horaires d''ouverture définis, affichés et communiqués sur les canaux usuels', 'MEDIUM', 1),
    ('f9000005-0000-0000-0000-000000000002', 'ACC.A.1', 'Le service doit garantir l''accessibilité de son accueil, y compris pour les personnes en situation de handicap.', 'MUST', 'DOCUMENT,AUDIT', 'Accessibilité de l''accueil évaluée ; aménagements en place pour les personnes en situation de handicap', 'HIGH', 2),
    ('f9000005-0000-0000-0000-000000000003', 'REP.C.1', 'Le service doit répondre aux courriers des usagers dans un délai maximal de 15 jours ouvrés.', 'MUST', 'KPI_RECORD,DOCUMENT', 'Délai de réponse au courrier ≤ 15 jours ouvrés pour ≥ 90 % des courriers ; délai mesuré', 'HIGH', 3),
    ('f9000005-0000-0000-0000-000000000004', 'REP.M.1', 'Le service doit répondre aux courriels des usagers dans un délai maximal de 7 jours.', 'MUST', 'KPI_RECORD,DOCUMENT', 'Délai de réponse aux courriels ≤ 7 jours pour ≥ 90 % des courriels ; délai mesuré', 'HIGH', 4),
    ('f9000005-0000-0000-0000-000000000005', 'REP.T.1', 'Le service doit assurer une prise en charge téléphonique de qualité, avec une prise d''appel en moins de 5 sonneries.', 'MUST', 'KPI_RECORD,DOCUMENT', 'Prise d''appel en moins de 5 sonneries ; taux de décroché et délai de prise en charge suivis', 'MEDIUM', 5),
    ('f9000005-0000-0000-0000-000000000006', 'ECO.A.1', 'Le service doit recueillir et prendre en compte les avis et réclamations des usagers pour améliorer ses prestations.', 'MUST', 'DOCUMENT,KPI_RECORD', 'Dispositif de recueil des avis en place ; réclamations tracées et taux de satisfaction suivi', 'HIGH', 6),
    ('f9000005-0000-0000-0000-000000000006', 'ECO.A.2', 'Le service doit mettre en œuvre des actions d''amélioration à partir des retours et réclamations des usagers.', 'SHOULD', 'CAPA,DOCUMENT', 'Plan d''amélioration alimenté par les avis usagers ; actions suivies et mesurées', 'MEDIUM', 7),
    ('f9000005-0000-0000-0000-000000000007', 'AGT.F.1', 'Le service doit former ses agents à l''accueil et à la relation usager.', 'MUST', 'TRAINING_RECORD,DOCUMENT', 'Plan de formation à la relation usager déployé ; taux d''agents formés suivi', 'MEDIUM', 8);

INSERT INTO standard_certification_paths (id, standard_id, estimated_duration_months_min, estimated_duration_months_max,
    estimated_cost_eur_min, estimated_cost_eur_max, difficulty_level,
    surveillance_audit_frequency, recertification_cycle_years, notes)
VALUES ('fe000005-0000-0000-0000-000000000005', 'f0000005-0000-0000-0000-000000000005',
    4, 9, 5000, 30000, 2, 'annual', NULL,
    'Le référentiel Marianne n''est pas une certification accréditée : la reconnaissance prend la forme d''une LABELLISATION (label Marianne) obtenue via des audits/évaluations par un tiers, ou d''un déploiement volontaire piloté par l''administration. Synergie avec les modules Complaints (avis et réclamations des usagers), KPI (délais de réponse, taux de décroché, satisfaction) et Training (formation des agents) de QualitOS.');

INSERT INTO standard_certification_stages (certification_path_id, stage_number, name, description, typical_duration_weeks, deliverables, actors, qualitos_modules, order_index) VALUES
    ('fe000005-0000-0000-0000-000000000005', 1, 'Cadrage & engagements de service', 'Définition du périmètre, des canaux d''accueil et des engagements de service Marianne.', '2-4', 'Périmètre, référentiel d''engagements de service', 'Direction, Responsable accueil', 'Document Control', 1),
    ('fe000005-0000-0000-0000-000000000005', 2, 'Accessibilité & horaires', 'Mise en conformité de l''accessibilité de l''accueil et des horaires adaptés aux usagers.', '3-5', 'Diagnostic d''accessibilité, horaires d''ouverture publiés', 'Responsable accueil', 'Document Control', 2),
    ('fe000005-0000-0000-0000-000000000005', 3, 'Délais de réponse multicanal', 'Mise en place du suivi des délais de réponse (courrier, courriel, téléphone) et des seuils d''engagement.', '4-8', 'Procédures de traitement multicanal, tableau de bord des délais', 'Responsable accueil, IT', 'KPI, Complaints', 3),
    ('fe000005-0000-0000-0000-000000000005', 4, 'Formation des agents', 'Formation et professionnalisation des agents à l''accueil et à la relation usager.', '3-5', 'Plan de formation, supports, attestations', 'RH, Responsable accueil', 'Training', 4),
    ('fe000005-0000-0000-0000-000000000005', 5, 'Écoute usager & amélioration', 'Mise en place du recueil des avis usagers, du traitement des réclamations et des actions d''amélioration.', '3-5', 'Dispositif de recueil des avis, registre des réclamations, plan d''amélioration', 'Responsable qualité', 'Complaints, PDCA', 5),
    ('fe000005-0000-0000-0000-000000000005', 6, 'Audit / évaluation de labellisation', 'Audit ou évaluation par un tiers en vue de la labellisation Marianne et levée des écarts.', '2-5', 'Rapport d''évaluation, label Marianne', 'Évaluateur tiers', 'Standards Hub, CAPA', 6);
