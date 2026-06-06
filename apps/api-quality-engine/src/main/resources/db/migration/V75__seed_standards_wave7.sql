-- Standards Hub — vague 7 (CLAUDE.md §8.2) : 5 référentiels santé / dispositifs médicaux.
--   mdr                  : Règlement (UE) 2017/745 — dispositifs médicaux (marquage CE, organismes notifiés)
--   iso-14971            : ISO 14971:2019 — gestion des risques des dispositifs médicaux
--   fda-21-cfr-part-820  : FDA 21 CFR Part 820 — Quality System Regulation / QMSR (transition ISO 13485, 2026)
--   gamp-5               : GAMP 5 (2nd ed., ISPE) — validation des systèmes informatisés (CSV)
--   has-v2024            : Certification HAS des établissements de santé V2024 (France)
-- Même format que V72 : catalogue platform-level, UUID déterministes, préfixe « b ».
--   standards  b000000n  (n = norme 1 à 5)
--   sections   bn00000s  (n = norme, s = section)
--   clauses    ba/bb/bc/bd pour les normes 1 à 4 ; b9 pour la 5e norme (HAS-V2024)
--   paths      be00000n  ; stages rattachés au path par FK.
-- Note UUID : les préfixes ba-bd couvrent les normes 1 à 4 ; la 5e (HAS) porte b9 pour
-- éviter toute collision avec les paths (be) et les UUID b27xxxxx/b1111111…b7777777 de V59.

-- ============================================================================
-- MDR — Règlement (UE) 2017/745 relatif aux dispositifs médicaux
-- ============================================================================

INSERT INTO standards (id, code, full_name, publisher, current_version, publication_date,
    family, applicable_industries, description, certification_body_required,
    recertification_cycle_months, related_norm_codes, status, created_at, updated_at)
VALUES (
    'b0000001-0000-0000-0000-000000000001',
    'mdr',
    'Règlement (UE) 2017/745 (MDR) — Dispositifs médicaux',
    'UE',
    '2017/745',
    DATE '2017-05-05',
    'REGULATORY',
    'healthcare,medical-devices',
    'Règlement européen relatif aux dispositifs médicaux, applicable depuis le 26 mai 2021. Il ne s''agit pas d''une « certification » au sens classique : la mise sur le marché repose sur le MARQUAGE CE, obtenu après évaluation de la conformité par un ORGANISME NOTIFIÉ (sauf classe I non stérile/non mesurante/non réutilisable chirurgicale, sous responsabilité du fabricant). Le fabricant doit classer le dispositif selon les règles de l''annexe VIII, constituer une documentation technique (annexes II et III), réaliser une évaluation clinique (art. 61, guidances MDCG), attribuer un IUD/UDI et s''enregistrer dans EUDAMED, exploiter un système de surveillance après commercialisation (PMS, PSUR pour les classes supérieures) et un système de vigilance. Délais de déclaration des incidents : incident grave 15 jours ; menace grave pour la santé publique 2 jours ; décès ou détérioration grave imprévue de l''état de santé 10 jours. Les certificats des organismes notifiés ont une durée maximale de 5 ans avec audits de surveillance annuels.',
    TRUE,
    NULL,
    'iso-13485,iso-14971',
    'PUBLISHED', now(), now()
);

INSERT INTO standard_sections (id, standard_id, code, title, description, order_index) VALUES
    ('b1000001-0000-0000-0000-000000000001', 'b0000001-0000-0000-0000-000000000001', 'CLA', 'Classification & conformité', 'Classification du dispositif (annexe VIII) et procédure d''évaluation de la conformité.', 1),
    ('b1000001-0000-0000-0000-000000000002', 'b0000001-0000-0000-0000-000000000001', 'TEC', 'Documentation technique', 'Documentation technique exigée aux annexes II et III.', 2),
    ('b1000001-0000-0000-0000-000000000003', 'b0000001-0000-0000-0000-000000000001', 'CLI', 'Évaluation clinique', 'Évaluation clinique et données cliniques (art. 61, annexe XIV).', 3),
    ('b1000001-0000-0000-0000-000000000004', 'b0000001-0000-0000-0000-000000000001', 'TRA', 'Traçabilité (UDI/EUDAMED)', 'Identification unique des dispositifs (IUD/UDI) et enregistrement EUDAMED.', 4),
    ('b1000001-0000-0000-0000-000000000005', 'b0000001-0000-0000-0000-000000000001', 'PMS', 'Surveillance & vigilance', 'Surveillance après commercialisation (PMS/PSUR) et système de vigilance.', 5);

INSERT INTO standard_clauses (id, section_id, code, title, description, order_index) VALUES
    ('ba000001-0000-0000-0000-000000000001', 'b1000001-0000-0000-0000-000000000001', 'art.51', 'Classification du dispositif', 'Classement du dispositif (I, IIa, IIb, III) selon les règles de l''annexe VIII.', 1),
    ('ba000001-0000-0000-0000-000000000002', 'b1000001-0000-0000-0000-000000000001', 'art.52', 'Évaluation de la conformité', 'Choix et mise en œuvre de la procédure d''évaluation de la conformité avec organisme notifié.', 2),
    ('ba000001-0000-0000-0000-000000000003', 'b1000001-0000-0000-0000-000000000002', 'art.10.4', 'Documentation technique', 'Établissement et tenue à jour de la documentation technique (annexes II et III).', 3),
    ('ba000001-0000-0000-0000-000000000004', 'b1000001-0000-0000-0000-000000000002', 'art.10.9', 'Système de gestion de la qualité', 'Mise en place d''un SMQ couvrant la gestion des risques et la conformité réglementaire.', 4),
    ('ba000001-0000-0000-0000-000000000005', 'b1000001-0000-0000-0000-000000000003', 'art.61', 'Évaluation clinique', 'Réalisation et mise à jour de l''évaluation clinique (annexe XIV, guidances MDCG).', 5),
    ('ba000001-0000-0000-0000-000000000006', 'b1000001-0000-0000-0000-000000000004', 'art.27', 'Identifiant unique (UDI)', 'Attribution et apposition de l''identifiant unique des dispositifs (IUD/UDI).', 6),
    ('ba000001-0000-0000-0000-000000000007', 'b1000001-0000-0000-0000-000000000004', 'art.29', 'Enregistrement EUDAMED', 'Enregistrement du fabricant et des dispositifs dans la base EUDAMED.', 7),
    ('ba000001-0000-0000-0000-000000000008', 'b1000001-0000-0000-0000-000000000005', 'art.83', 'Surveillance après commercialisation', 'Système de PMS et, selon la classe, rapport périodique actualisé de sécurité (PSUR).', 8),
    ('ba000001-0000-0000-0000-000000000009', 'b1000001-0000-0000-0000-000000000005', 'art.87', 'Vigilance & incidents graves', 'Déclaration des incidents graves et des mesures correctives de sécurité (FSCA) dans les délais.', 9);

INSERT INTO standard_requirements (clause_id, code, text, obligation, evidence_types, measurable_criteria, risk_if_missing, order_index) VALUES
    ('ba000001-0000-0000-0000-000000000001', 'art.51.1', 'Le fabricant doit classer le dispositif (I, IIa, IIb, III) en appliquant les règles de classification de l''annexe VIII et documenter la justification.', 'MUST', 'DOCUMENT', 'Classification justifiée et documentée pour 100 % des dispositifs au catalogue', 'CRITICAL', 1),
    ('ba000001-0000-0000-0000-000000000002', 'art.52.1', 'Le fabricant doit appliquer la procédure d''évaluation de la conformité appropriée à la classe, avec intervention d''un organisme notifié lorsqu''elle est requise.', 'MUST', 'DOCUMENT,AUDIT', 'Procédure d''évaluation choisie tracée ; organisme notifié engagé pour les classes IIa/IIb/III', 'CRITICAL', 2),
    ('ba000001-0000-0000-0000-000000000003', 'art.10.4', 'Le fabricant doit établir, tenir à jour et fournir la documentation technique conformément aux annexes II et III.', 'MUST', 'DOCUMENT,AUDIT', 'Documentation technique complète (annexes II/III) revue à chaque modification significative', 'CRITICAL', 3),
    ('ba000001-0000-0000-0000-000000000004', 'art.10.9', 'Le fabricant doit mettre en place un système de gestion de la qualité couvrant la gestion des risques et le respect des exigences réglementaires.', 'MUST', 'DOCUMENT,AUDIT', 'SMQ documenté couvrant l''ensemble des processus de l''art. 10.9 ; gestion des risques intégrée', 'HIGH', 4),
    ('ba000001-0000-0000-0000-000000000005', 'art.61.1', 'Le fabricant doit réaliser une évaluation clinique fondée sur des données cliniques suffisantes et la tenir à jour tout au long du cycle de vie.', 'MUST', 'DOCUMENT', 'Rapport d''évaluation clinique disponible et mis à jour selon le plan d''évaluation clinique', 'CRITICAL', 5),
    ('ba000001-0000-0000-0000-000000000006', 'art.27.1', 'Le fabricant doit attribuer un identifiant unique des dispositifs (IUD/UDI) et l''apposer sur l''étiquette et les niveaux d''emballage requis.', 'MUST', 'DOCUMENT', 'UDI attribué et apposé pour 100 % des dispositifs mis sur le marché', 'HIGH', 6),
    ('ba000001-0000-0000-0000-000000000007', 'art.29.1', 'Le fabricant doit enregistrer les informations relatives à lui-même et à ses dispositifs dans la base de données EUDAMED.', 'MUST', 'DOCUMENT', 'Enregistrement EUDAMED réalisé et maintenu à jour (fabricant + dispositifs + UDI)', 'HIGH', 7),
    ('ba000001-0000-0000-0000-000000000008', 'art.83.1', 'Le fabricant doit planifier, établir et tenir à jour un système de surveillance après commercialisation (PMS) proportionné à la classe de risque.', 'MUST', 'DOCUMENT,KPI_RECORD', 'Plan PMS en place ; PSUR produit selon la périodicité exigée pour les classes IIa/IIb/III', 'HIGH', 8),
    ('ba000001-0000-0000-0000-000000000009', 'art.87.1', 'Le fabricant doit déclarer aux autorités compétentes les incidents graves et les mesures correctives de sécurité dans les délais réglementaires.', 'MUST', 'DOCUMENT,CAPA', 'Incident grave déclaré ≤ 15 jours ; menace grave pour la santé publique ≤ 2 jours ; décès/détérioration grave imprévue ≤ 10 jours', 'CRITICAL', 9);

INSERT INTO standard_certification_paths (id, standard_id, estimated_duration_months_min, estimated_duration_months_max,
    estimated_cost_eur_min, estimated_cost_eur_max, difficulty_level,
    surveillance_audit_frequency, recertification_cycle_years, notes)
VALUES ('be000001-0000-0000-0000-000000000001', 'b0000001-0000-0000-0000-000000000001',
    12, 36, 30000, 300000, 5, 'annual', 5,
    'La conformité MDR conduit au MARQUAGE CE (et non à un « certificat » de système comme en ISO) : l''évaluation de la conformité est réalisée par un organisme notifié pour les classes IIa, IIb et III, tandis que la classe I (hors stérile/mesurante/réutilisable chirurgicale) relève de la responsabilité du fabricant. Les certificats d''organisme notifié ont une durée maximale de 5 ans avec audits de surveillance annuels. Synergie forte avec les modules Document Control (documentation technique), Risk (ISO 14971), CAPA (mesures correctives) et Audit de QualitOS.');

INSERT INTO standard_certification_stages (certification_path_id, stage_number, name, description, typical_duration_weeks, deliverables, actors, qualitos_modules, order_index) VALUES
    ('be000001-0000-0000-0000-000000000001', 1, 'Qualification & classification', 'Qualification de dispositif médical et classification selon l''annexe VIII (I, IIa, IIb, III).', '3-6', 'Note de qualification, classification justifiée', 'Affaires réglementaires, Direction', 'Document Control', 1),
    ('be000001-0000-0000-0000-000000000001', 2, 'SMQ & gestion des risques', 'Mise en place du SMQ (ISO 13485) et du système de gestion des risques (ISO 14971).', '8-16', 'Manuel SMQ, dossier de gestion des risques', 'Responsable qualité', 'Document Control, Risk', 2),
    ('be000001-0000-0000-0000-000000000001', 3, 'Documentation technique', 'Constitution de la documentation technique conforme aux annexes II et III.', '8-20', 'Documentation technique (annexes II/III)', 'Affaires réglementaires, R&D', 'Document Control', 3),
    ('be000001-0000-0000-0000-000000000001', 4, 'Évaluation clinique & UDI/EUDAMED', 'Évaluation clinique (art. 61), attribution UDI et enregistrement EUDAMED.', '8-16', 'Rapport d''évaluation clinique, UDI, enregistrement EUDAMED', 'Affaires cliniques, Réglementaire', 'Document Control', 4),
    ('be000001-0000-0000-0000-000000000001', 5, 'Évaluation par organisme notifié', 'Audit du SMQ et revue de la documentation technique par l''organisme notifié (classes IIa/IIb/III).', '8-16', 'Certificat CE, certificat SMQ', 'Organisme notifié', 'Audit, Standards Hub, CAPA', 5),
    ('be000001-0000-0000-0000-000000000001', 6, 'Surveillance après commercialisation', 'Mise en œuvre du PMS/PSUR et du système de vigilance, audits de surveillance annuels.', '4-8', 'Plan PMS, PSUR, procédure de vigilance', 'Réglementaire, Qualité', 'CAPA, Audit, KPI', 6);

-- ============================================================================
-- ISO 14971:2019 — Gestion des risques des dispositifs médicaux
-- ============================================================================

INSERT INTO standards (id, code, full_name, publisher, current_version, publication_date,
    family, applicable_industries, description, certification_body_required,
    recertification_cycle_months, related_norm_codes, status, created_at, updated_at)
VALUES (
    'b0000002-0000-0000-0000-000000000002',
    'iso-14971',
    'ISO 14971:2019 — Dispositifs médicaux — Application de la gestion des risques aux dispositifs médicaux',
    'ISO',
    '2019',
    DATE '2019-12-10',
    'SECTORIEL',
    'healthcare,medical-devices',
    'Norme de référence pour l''application d''un processus de gestion des risques aux dispositifs médicaux tout au long de leur cycle de vie. Ce n''est pas une norme certifiable de façon autonome : sa conformité se démontre dans le cadre d''un SMQ (ISO 13485) et de la réglementation (MDR/IVDR, FDA), en tant que processus appliqué. La norme exige un plan de gestion des risques, l''analyse des risques (identification des dangers et des situations dangereuses, estimation des risques), l''évaluation des risques, la maîtrise des risques et l''évaluation des risques résiduels (y compris l''appréciation du rapport bénéfice/risque global), ainsi que les activités de production et de post-production alimentant en continu le dossier de gestion des risques. Le guide ISO/TR 24971 en accompagne l''application.',
    FALSE,
    NULL,
    'iso-13485,mdr',
    'PUBLISHED', now(), now()
);

INSERT INTO standard_sections (id, standard_id, code, title, description, order_index) VALUES
    ('b2000002-0000-0000-0000-000000000001', 'b0000002-0000-0000-0000-000000000002', '4', 'Gestion des risques générale', 'Processus, responsabilités et plan de gestion des risques.', 1),
    ('b2000002-0000-0000-0000-000000000002', 'b0000002-0000-0000-0000-000000000002', '5', 'Analyse des risques', 'Identification des dangers, des situations dangereuses et estimation des risques.', 2),
    ('b2000002-0000-0000-0000-000000000003', 'b0000002-0000-0000-0000-000000000002', '6-7', 'Évaluation & maîtrise des risques', 'Évaluation des risques et mise en œuvre des mesures de maîtrise.', 3),
    ('b2000002-0000-0000-0000-000000000004', 'b0000002-0000-0000-0000-000000000002', '8-10', 'Risque résiduel & post-production', 'Risque résiduel global, dossier de gestion des risques et activités de production/post-production.', 4);

INSERT INTO standard_clauses (id, section_id, code, title, description, order_index) VALUES
    ('bb000002-0000-0000-0000-000000000001', 'b2000002-0000-0000-0000-000000000001', '4.4', 'Plan de gestion des risques', 'Établissement d''un plan de gestion des risques pour le dispositif considéré.', 1),
    ('bb000002-0000-0000-0000-000000000002', 'b2000002-0000-0000-0000-000000000002', '5.4', 'Identification des dangers', 'Identification des dangers et des situations dangereuses prévisibles.', 2),
    ('bb000002-0000-0000-0000-000000000003', 'b2000002-0000-0000-0000-000000000002', '5.5', 'Estimation des risques', 'Estimation des risques associés à chaque situation dangereuse.', 3),
    ('bb000002-0000-0000-0000-000000000004', 'b2000002-0000-0000-0000-000000000003', '6', 'Évaluation des risques', 'Évaluation des risques au regard des critères d''acceptabilité définis.', 4),
    ('bb000002-0000-0000-0000-000000000005', 'b2000002-0000-0000-0000-000000000003', '7', 'Maîtrise des risques', 'Sélection et mise en œuvre des mesures de maîtrise et vérification de leur efficacité.', 5),
    ('bb000002-0000-0000-0000-000000000006', 'b2000002-0000-0000-0000-000000000004', '8', 'Risque résiduel global', 'Évaluation du risque résiduel global et appréciation du rapport bénéfice/risque.', 6),
    ('bb000002-0000-0000-0000-000000000007', 'b2000002-0000-0000-0000-000000000004', '9', 'Dossier de gestion des risques', 'Revue de la gestion des risques et tenue du dossier de gestion des risques.', 7),
    ('bb000002-0000-0000-0000-000000000008', 'b2000002-0000-0000-0000-000000000004', '10', 'Production & post-production', 'Collecte et examen des informations de production et de post-production.', 8);

INSERT INTO standard_requirements (clause_id, code, text, obligation, evidence_types, measurable_criteria, risk_if_missing, order_index) VALUES
    ('bb000002-0000-0000-0000-000000000001', '4.4', 'Le fabricant doit établir un plan de gestion des risques, incluant les critères d''acceptabilité des risques, pour le dispositif considéré.', 'MUST', 'DOCUMENT', 'Plan de gestion des risques approuvé avec critères d''acceptabilité définis avant le développement', 'CRITICAL', 1),
    ('bb000002-0000-0000-0000-000000000002', '5.4', 'Le fabricant doit identifier les dangers connus et prévisibles ainsi que les situations dangereuses associées au dispositif, en utilisation normale et en cas d''usage erroné raisonnablement prévisible.', 'MUST', 'DOCUMENT,AUDIT', 'Liste des dangers et situations dangereuses documentée et revue ; usage erroné prévisible inclus', 'CRITICAL', 2),
    ('bb000002-0000-0000-0000-000000000003', '5.5', 'Le fabricant doit estimer le risque pour chaque situation dangereuse à partir de la probabilité d''occurrence et de la gravité du dommage.', 'MUST', 'DOCUMENT', 'Estimation du risque (probabilité × gravité) documentée pour 100 % des situations dangereuses', 'HIGH', 3),
    ('bb000002-0000-0000-0000-000000000004', '6.1', 'Le fabricant doit évaluer chaque risque estimé par rapport aux critères d''acceptabilité du plan afin de décider de la nécessité d''une maîtrise.', 'MUST', 'DOCUMENT', 'Décision d''acceptabilité tracée pour chaque risque au regard des critères du plan', 'HIGH', 4),
    ('bb000002-0000-0000-0000-000000000005', '7.1', 'Le fabricant doit mettre en œuvre des mesures de maîtrise du risque selon l''ordre de priorité (conception sûre, protection, information de sécurité).', 'MUST', 'DOCUMENT,CAPA', 'Mesures de maîtrise sélectionnées selon l''ordre de priorité de la norme et tracées', 'CRITICAL', 5),
    ('bb000002-0000-0000-0000-000000000005', '7.2', 'Le fabricant doit vérifier la mise en œuvre et l''efficacité de chaque mesure de maîtrise du risque.', 'MUST', 'DOCUMENT,AUDIT', 'Vérification de mise en œuvre et d''efficacité documentée pour 100 % des mesures de maîtrise', 'HIGH', 6),
    ('bb000002-0000-0000-0000-000000000006', '8.1', 'Le fabricant doit évaluer le risque résiduel global et, lorsqu''il subsiste un risque, apprécier le rapport bénéfice/risque global du dispositif.', 'MUST', 'DOCUMENT', 'Évaluation du risque résiduel global et analyse bénéfice/risque documentées et approuvées', 'CRITICAL', 7),
    ('bb000002-0000-0000-0000-000000000007', '9.1', 'Le fabricant doit réaliser une revue de la gestion des risques avant la mise sur le marché et tenir à jour un dossier de gestion des risques.', 'MUST', 'DOCUMENT,AUDIT', 'Revue de gestion des risques réalisée avant libération ; dossier de gestion des risques complet', 'HIGH', 8),
    ('bb000002-0000-0000-0000-000000000008', '10.1', 'Le fabricant doit collecter et examiner les informations de production et de post-production et réévaluer les risques en conséquence.', 'MUST', 'DOCUMENT,KPI_RECORD', 'Données de post-production collectées et examinées ; réévaluation des risques tracée', 'HIGH', 9);

INSERT INTO standard_certification_paths (id, standard_id, estimated_duration_months_min, estimated_duration_months_max,
    estimated_cost_eur_min, estimated_cost_eur_max, difficulty_level,
    surveillance_audit_frequency, recertification_cycle_years, notes)
VALUES ('be000002-0000-0000-0000-000000000002', 'b0000002-0000-0000-0000-000000000002',
    3, 9, 8000, 40000, 3, 'none', NULL,
    'ISO 14971 n''est pas certifiable de manière autonome : c''est un processus dont la conformité est évaluée dans le cadre d''un SMQ (ISO 13485) et de la réglementation (MDR/IVDR, FDA). La mise en conformité consiste à déployer le processus et à constituer le dossier de gestion des risques. Le guide ISO/TR 24971 facilite l''application. Synergie directe avec les modules Risk/FMEA (estimation et maîtrise des risques) et CAPA de QualitOS.');

INSERT INTO standard_certification_stages (certification_path_id, stage_number, name, description, typical_duration_weeks, deliverables, actors, qualitos_modules, order_index) VALUES
    ('be000002-0000-0000-0000-000000000002', 1, 'Plan & politique de gestion des risques', 'Définition de la politique, du plan et des critères d''acceptabilité des risques.', '2-4', 'Politique et plan de gestion des risques', 'Responsable qualité, R&D', 'Document Control, Risk', 1),
    ('be000002-0000-0000-0000-000000000002', 2, 'Analyse des risques', 'Identification des dangers et des situations dangereuses, estimation des risques.', '4-8', 'Tableau d''analyse des risques (dangers, situations dangereuses)', 'Équipe risques, R&D', 'Risk, Ishikawa', 2),
    ('be000002-0000-0000-0000-000000000002', 3, 'Évaluation & maîtrise des risques', 'Évaluation des risques vs critères et mise en œuvre des mesures de maîtrise.', '4-10', 'Matrice d''évaluation, plan de maîtrise des risques', 'Équipe risques, R&D', 'Risk, CAPA', 3),
    ('be000002-0000-0000-0000-000000000002', 4, 'Risque résiduel & bénéfice/risque', 'Évaluation du risque résiduel global et de l''analyse bénéfice/risque.', '2-4', 'Évaluation du risque résiduel global, analyse bénéfice/risque', 'Direction qualité, Clinique', 'Risk', 4),
    ('be000002-0000-0000-0000-000000000002', 5, 'Dossier de gestion des risques', 'Revue de la gestion des risques et constitution du dossier avant mise sur le marché.', '2-4', 'Dossier de gestion des risques, rapport de revue', 'Responsable qualité', 'Document Control, Audit', 5),
    ('be000002-0000-0000-0000-000000000002', 6, 'Production & post-production', 'Boucle de retour production/post-production et réévaluation des risques.', '3-5', 'Procédure de retour post-production, réévaluations tracées', 'Qualité, Surveillance marché', 'CAPA, KPI', 6);

-- ============================================================================
-- FDA 21 CFR Part 820 — Quality System Regulation (QSR / QMSR)
-- ============================================================================

INSERT INTO standards (id, code, full_name, publisher, current_version, publication_date,
    family, applicable_industries, description, certification_body_required,
    recertification_cycle_months, related_norm_codes, status, created_at, updated_at)
VALUES (
    'b0000003-0000-0000-0000-000000000003',
    'fda-21-cfr-part-820',
    'FDA 21 CFR Part 820 — Quality System Regulation (QSR) / Quality Management System Regulation (QMSR)',
    'FDA',
    'QMSR 2024',
    DATE '2024-02-02',
    'REGULATORY',
    'healthcare,medical-devices',
    'Réglementation de la FDA imposant un système qualité aux fabricants de dispositifs médicaux commercialisés aux États-Unis. Il ne s''agit PAS d''une certification : il n''existe aucun certificat délivré, la conformité étant vérifiée lors des INSPECTIONS de la FDA. La règle finale QMSR (Quality Management System Regulation) publiée en 2024 aligne le Part 820 sur ISO 13485:2016 (incorporée par référence) et entre en vigueur le 2 février 2026, remplaçant l''ancien QSR. Le périmètre couvre notamment les design controls (820.30 / ISO 13485 §7.3), les actions correctives et préventives (CAPA), le traitement des réclamations (complaint files), les enregistrements de conception et de fabrication (DHF, DMR, DHR) et la déclaration des incidents (MDR reporting, 21 CFR Part 803).',
    TRUE,
    NULL,
    'iso-13485,fda-21-cfr-part-11',
    'PUBLISHED', now(), now()
);

INSERT INTO standard_sections (id, standard_id, code, title, description, order_index) VALUES
    ('b3000003-0000-0000-0000-000000000001', 'b0000003-0000-0000-0000-000000000003', 'DSG', 'Design controls', 'Maîtrise de la conception et du développement (820.30).', 1),
    ('b3000003-0000-0000-0000-000000000002', 'b0000003-0000-0000-0000-000000000003', 'CAPA', 'Actions correctives & préventives', 'Processus CAPA (820.100).', 2),
    ('b3000003-0000-0000-0000-000000000003', 'b0000003-0000-0000-0000-000000000003', 'CMP', 'Réclamations & MDR reporting', 'Traitement des réclamations et déclaration des incidents (820.198, Part 803).', 3),
    ('b3000003-0000-0000-0000-000000000004', 'b0000003-0000-0000-0000-000000000003', 'REC', 'Enregistrements (DHF/DMR/DHR)', 'Dossiers de conception, maître du dispositif et historique du dispositif.', 4);

INSERT INTO standard_clauses (id, section_id, code, title, description, order_index) VALUES
    ('bc000003-0000-0000-0000-000000000001', 'b3000003-0000-0000-0000-000000000001', '820.30', 'Design controls', 'Procédures de maîtrise de la conception et du développement.', 1),
    ('bc000003-0000-0000-0000-000000000002', 'b3000003-0000-0000-0000-000000000001', '820.30(g)', 'Validation de la conception', 'Validation de la conception incluant la validation logicielle le cas échéant.', 2),
    ('bc000003-0000-0000-0000-000000000003', 'b3000003-0000-0000-0000-000000000002', '820.100', 'Actions correctives et préventives', 'Établissement et maintien de procédures CAPA.', 3),
    ('bc000003-0000-0000-0000-000000000004', 'b3000003-0000-0000-0000-000000000003', '820.198', 'Complaint files', 'Procédures de réception, examen et évaluation des réclamations.', 4),
    ('bc000003-0000-0000-0000-000000000005', 'b3000003-0000-0000-0000-000000000003', 'part.803', 'Medical Device Reporting (MDR)', 'Déclaration des décès, blessures graves et dysfonctionnements à la FDA.', 5),
    ('bc000003-0000-0000-0000-000000000006', 'b3000003-0000-0000-0000-000000000004', '820.30(j)', 'Design History File (DHF)', 'Constitution et tenue du dossier d''historique de la conception.', 6),
    ('bc000003-0000-0000-0000-000000000007', 'b3000003-0000-0000-0000-000000000004', '820.181', 'Device Master Record (DMR)', 'Tenue du dossier maître du dispositif.', 7),
    ('bc000003-0000-0000-0000-000000000008', 'b3000003-0000-0000-0000-000000000004', '820.184', 'Device History Record (DHR)', 'Tenue du dossier d''historique du dispositif (par lot/unité).', 8);

INSERT INTO standard_requirements (clause_id, code, text, obligation, evidence_types, measurable_criteria, risk_if_missing, order_index) VALUES
    ('bc000003-0000-0000-0000-000000000001', '820.30(a)', 'Le fabricant doit établir et maintenir des procédures de maîtrise de la conception garantissant le respect des exigences spécifiées.', 'MUST', 'DOCUMENT,AUDIT', 'Procédures de design controls établies ; plan de conception tenu à jour par projet', 'CRITICAL', 1),
    ('bc000003-0000-0000-0000-000000000002', '820.30(g)', 'Le fabricant doit valider la conception du dispositif, y compris la validation des logiciels lorsqu''ils sont utilisés, dans des conditions d''utilisation réelles ou simulées.', 'MUST', 'DOCUMENT,AUDIT', 'Protocoles et rapports de validation de conception (et de logiciel) approuvés avant libération', 'CRITICAL', 2),
    ('bc000003-0000-0000-0000-000000000003', '820.100(a)', 'Le fabricant doit établir et maintenir des procédures d''actions correctives et préventives (CAPA) et analyser les sources de données qualité.', 'MUST', 'DOCUMENT,CAPA', 'Procédure CAPA en place ; analyse des données qualité tracée et CAPA suivis jusqu''à clôture', 'CRITICAL', 3),
    ('bc000003-0000-0000-0000-000000000004', '820.198(a)', 'Le fabricant doit établir et maintenir des procédures de réception, d''examen et d''évaluation des réclamations par une unité formellement désignée.', 'MUST', 'DOCUMENT,CAPA', 'Procédure de gestion des réclamations en place ; 100 % des réclamations enregistrées et évaluées', 'HIGH', 4),
    ('bc000003-0000-0000-0000-000000000005', 'part.803', 'Le fabricant doit déclarer à la FDA les décès, blessures graves et dysfonctionnements déclarables dans les délais du Medical Device Reporting.', 'MUST', 'DOCUMENT,CAPA', 'Événements déclarables transmis à la FDA dans les délais MDR (Part 803) ; déclarations tracées', 'CRITICAL', 5),
    ('bc000003-0000-0000-0000-000000000006', '820.30(j)', 'Le fabricant doit établir et tenir à jour un Design History File (DHF) pour chaque type de dispositif.', 'MUST', 'DOCUMENT', 'DHF constitué et complet pour 100 % des types de dispositifs conçus', 'HIGH', 6),
    ('bc000003-0000-0000-0000-000000000007', '820.181', 'Le fabricant doit tenir un Device Master Record (DMR) regroupant les spécifications, procédés et critères d''acceptation du dispositif.', 'MUST', 'DOCUMENT', 'DMR à jour et approuvé regroupant spécifications, procédés de fabrication et critères d''acceptation', 'HIGH', 7),
    ('bc000003-0000-0000-0000-000000000008', '820.184', 'Le fabricant doit tenir un Device History Record (DHR) démontrant que chaque lot ou unité est fabriqué conformément au DMR.', 'MUST', 'DOCUMENT,KPI_RECORD', 'DHR disponible et complet pour 100 % des lots/unités produits ; conformité au DMR démontrée', 'HIGH', 8);

INSERT INTO standard_certification_paths (id, standard_id, estimated_duration_months_min, estimated_duration_months_max,
    estimated_cost_eur_min, estimated_cost_eur_max, difficulty_level,
    surveillance_audit_frequency, recertification_cycle_years, notes)
VALUES ('be000003-0000-0000-0000-000000000003', 'b0000003-0000-0000-0000-000000000003',
    9, 24, 25000, 200000, 5, 'annual', NULL,
    'Le 21 CFR Part 820 n''est pas certifié : aucun certificat n''est délivré, la conformité est vérifiée par INSPECTION de la FDA (souvent suivant le programme QSIT). La règle finale QMSR (2024) aligne le Part 820 sur ISO 13485:2016 (incorporée par référence) et entre en vigueur le 2 février 2026 ; les fabricants doivent migrer leur SMQ en conséquence. La fréquence d''inspection est risque-dépendante (indiquée ici « annual » à titre de réservation budgétaire/préparation). Synergie avec les modules CAPA, Document Control (DHF/DMR/DHR), Complaints et Audit de QualitOS.');

INSERT INTO standard_certification_stages (certification_path_id, stage_number, name, description, typical_duration_weeks, deliverables, actors, qualitos_modules, order_index) VALUES
    ('be000003-0000-0000-0000-000000000003', 1, 'Cadrage & gap analysis QMSR', 'Diagnostic d''écarts vs Part 820/QMSR (ISO 13485) et plan de remédiation, en vue de la transition 2026.', '4-6', 'Rapport d''écarts, plan de transition QMSR', 'Direction qualité, Réglementaire', 'Audit, Document Control', 1),
    ('be000003-0000-0000-0000-000000000003', 2, 'Design controls', 'Mise en place des procédures de maîtrise et de validation de la conception (820.30).', '8-16', 'Procédures design controls, plans de conception', 'R&D, Qualité', 'Document Control, Change', 2),
    ('be000003-0000-0000-0000-000000000003', 3, 'CAPA & réclamations', 'Déploiement des processus CAPA (820.100) et de gestion des réclamations (820.198).', '6-10', 'Procédures CAPA et réclamations, registre des réclamations', 'Qualité, Support', 'CAPA, Complaints', 3),
    ('be000003-0000-0000-0000-000000000003', 4, 'Enregistrements DHF/DMR/DHR & MDR', 'Constitution des DHF/DMR/DHR et procédure de Medical Device Reporting (Part 803).', '6-12', 'DHF, DMR, DHR, procédure MDR reporting', 'Qualité, Production', 'Document Control, CAPA', 4),
    ('be000003-0000-0000-0000-000000000003', 5, 'Audit interne & revue de direction', 'Audit interne du système qualité (style QSIT) et revue de direction.', '3-5', 'Rapport d''audit interne, compte rendu de revue de direction', 'Auditeur interne, Direction', 'Audit, PDCA', 5),
    ('be000003-0000-0000-0000-000000000003', 6, 'Préparation à l''inspection FDA', 'Préparation et conduite de l''inspection FDA, traitement des observations (Form 483).', '2-6', 'Dossier d''inspection, réponses aux observations 483', 'Réglementaire, Qualité', 'Standards Hub, CAPA', 6);

-- ============================================================================
-- GAMP 5 (2nd ed., ISPE) — Validation des systèmes informatisés (CSV)
-- ============================================================================

INSERT INTO standards (id, code, full_name, publisher, current_version, publication_date,
    family, applicable_industries, description, certification_body_required,
    recertification_cycle_months, related_norm_codes, status, created_at, updated_at)
VALUES (
    'b0000004-0000-0000-0000-000000000004',
    'gamp-5',
    'GAMP 5 (2nd edition) — A Risk-Based Approach to Compliant GxP Computerized Systems',
    'ISPE',
    '2nd edition (2022)',
    DATE '2022-07-01',
    'SECTORIEL',
    'healthcare,medical-devices,pharma',
    'Guide de bonnes pratiques de l''ISPE pour l''assurance qualité et la validation des systèmes informatisés en environnement GxP. Ce n''est pas une norme certifiable mais un référentiel de bonnes pratiques largement reconnu par les autorités (FDA, EMA, MHRA), aligné avec les attentes du data integrity et de l''Annexe 11 / 21 CFR Part 11. La 2e édition renforce l''approche fondée sur les risques qualité, l''intégration des méthodes agiles/itératives, le recours aux fournisseurs et services (notamment cloud/SaaS) et l''intégrité des données. Le cadre repose sur la catégorisation des logiciels (catégories 1, 3, 4, 5), une approche cycle de vie (spécifications et vérification : URS/FS/DS, IQ/OQ/PQ), le levier des activités fournisseur, le data integrity selon les principes ALCOA+ et l''exploitation/maintien de l''état validé.',
    FALSE,
    NULL,
    'fda-21-cfr-part-11,iso-13485',
    'PUBLISHED', now(), now()
);

INSERT INTO standard_sections (id, standard_id, code, title, description, order_index) VALUES
    ('b4000004-0000-0000-0000-000000000001', 'b0000004-0000-0000-0000-000000000004', 'CAT', 'Catégorisation & approche risque', 'Catégorisation des logiciels et approche fondée sur les risques qualité.', 1),
    ('b4000004-0000-0000-0000-000000000002', 'b0000004-0000-0000-0000-000000000004', 'LC', 'Cycle de vie (specs & vérif.)', 'Spécifications et vérification, y compris IQ/OQ/PQ.', 2),
    ('b4000004-0000-0000-0000-000000000003', 'b0000004-0000-0000-0000-000000000004', 'DI', 'Intégrité des données', 'Intégrité des données selon les principes ALCOA+.', 3),
    ('b4000004-0000-0000-0000-000000000004', 'b0000004-0000-0000-0000-000000000004', 'OPS', 'Fournisseurs & exploitation', 'Effet de levier fournisseur et maintien de l''état validé en exploitation.', 4);

INSERT INTO standard_clauses (id, section_id, code, title, description, order_index) VALUES
    ('bd000004-0000-0000-0000-000000000001', 'b4000004-0000-0000-0000-000000000001', 'cat.sw', 'Catégorisation des logiciels', 'Détermination de la catégorie logicielle (1, 3, 4, 5).', 1),
    ('bd000004-0000-0000-0000-000000000002', 'b4000004-0000-0000-0000-000000000001', 'qrm', 'Approche risque qualité', 'Évaluation des risques qualité pilotant l''effort de validation.', 2),
    ('bd000004-0000-0000-0000-000000000003', 'b4000004-0000-0000-0000-000000000002', 'spec', 'Spécifications (URS/FS/DS)', 'Établissement des exigences et spécifications du système.', 3),
    ('bd000004-0000-0000-0000-000000000004', 'b4000004-0000-0000-0000-000000000002', 'iq-oq-pq', 'Vérification (IQ/OQ/PQ)', 'Qualification d''installation, opérationnelle et de performance.', 4),
    ('bd000004-0000-0000-0000-000000000005', 'b4000004-0000-0000-0000-000000000002', 'trace', 'Traçabilité exigences/tests', 'Matrice de traçabilité entre exigences et vérifications.', 5),
    ('bd000004-0000-0000-0000-000000000006', 'b4000004-0000-0000-0000-000000000003', 'alcoa', 'Intégrité des données (ALCOA+)', 'Contrôles garantissant l''intégrité des données GxP.', 6),
    ('bd000004-0000-0000-0000-000000000007', 'b4000004-0000-0000-0000-000000000004', 'sup', 'Effet de levier fournisseur', 'Évaluation des fournisseurs et exploitation de leurs livrables qualité.', 7),
    ('bd000004-0000-0000-0000-000000000008', 'b4000004-0000-0000-0000-000000000004', 'ops', 'Maintien de l''état validé', 'Gestion des changements, sauvegardes et maintien en exploitation.', 8);

INSERT INTO standard_requirements (clause_id, code, text, obligation, evidence_types, measurable_criteria, risk_if_missing, order_index) VALUES
    ('bd000004-0000-0000-0000-000000000001', 'cat.1', 'L''organisation doit catégoriser chaque système informatisé GxP (catégories GAMP 1, 3, 4, 5) afin d''adapter l''effort de validation.', 'MUST', 'DOCUMENT', 'Catégorie GAMP attribuée et justifiée pour 100 % des systèmes GxP de l''inventaire', 'HIGH', 1),
    ('bd000004-0000-0000-0000-000000000002', 'qrm.1', 'L''organisation doit réaliser une évaluation des risques qualité pour déterminer l''étendue et la profondeur des activités de validation.', 'MUST', 'DOCUMENT,AUDIT', 'Évaluation des risques qualité documentée pilotant le périmètre de validation de chaque système', 'HIGH', 2),
    ('bd000004-0000-0000-0000-000000000003', 'spec.1', 'L''organisation doit définir des exigences utilisateurs (URS) et les spécifications nécessaires, approuvées avant la vérification.', 'MUST', 'DOCUMENT', 'URS et spécifications approuvées et versionnées avant le début des activités de vérification', 'HIGH', 3),
    ('bd000004-0000-0000-0000-000000000004', 'iq-oq-pq.1', 'L''organisation doit vérifier le système au moyen de qualifications appropriées (IQ/OQ/PQ) couvrant l''installation, le fonctionnement et la performance.', 'MUST', 'DOCUMENT,AUDIT', 'Protocoles et rapports IQ/OQ/PQ exécutés et approuvés ; écarts traités avant mise en service', 'CRITICAL', 4),
    ('bd000004-0000-0000-0000-000000000005', 'trace.1', 'L''organisation doit assurer la traçabilité entre les exigences et les éléments de vérification correspondants.', 'MUST', 'DOCUMENT', 'Matrice de traçabilité exigences ↔ tests complète et tenue à jour pour chaque système', 'MEDIUM', 5),
    ('bd000004-0000-0000-0000-000000000006', 'alcoa.1', 'L''organisation doit mettre en œuvre des contrôles garantissant l''intégrité des données GxP selon les principes ALCOA+ (audit trail, contrôle d''accès, sauvegardes).', 'MUST', 'DOCUMENT,AUDIT', 'Audit trail activé et revu, contrôle d''accès et sauvegardes en place sur 100 % des systèmes GxP', 'CRITICAL', 6),
    ('bd000004-0000-0000-0000-000000000007', 'sup.1', 'L''organisation doit évaluer les fournisseurs et, le cas échéant, exploiter leurs livrables qualité dans son approche de validation.', 'MUST', 'DOCUMENT,AUDIT', 'Évaluation fournisseur (audit/questionnaire) réalisée ; livrables exploités tracés', 'MEDIUM', 7),
    ('bd000004-0000-0000-0000-000000000008', 'ops.1', 'L''organisation doit maintenir l''état validé du système en exploitation via la gestion des changements, des incidents et des sauvegardes.', 'MUST', 'DOCUMENT,CAPA', 'Changements évalués et tracés ; sauvegardes/restaurations testées ; état validé maintenu', 'HIGH', 8);

INSERT INTO standard_certification_paths (id, standard_id, estimated_duration_months_min, estimated_duration_months_max,
    estimated_cost_eur_min, estimated_cost_eur_max, difficulty_level,
    surveillance_audit_frequency, recertification_cycle_years, notes)
VALUES ('be000004-0000-0000-0000-000000000004', 'b0000004-0000-0000-0000-000000000004',
    2, 9, 5000, 50000, 3, 'none', NULL,
    'GAMP 5 est un guide de bonnes pratiques (ISPE) et non une norme certifiable : il n''existe pas de certificat GAMP. La conformité se démontre lors des inspections GxP (FDA, EMA, MHRA) et se matérialise par les dossiers de validation des systèmes informatisés (CSV) et le maintien de l''état validé. La 2e édition (2022) intègre l''agilité, le cloud/SaaS et le data integrity. Synergie avec les modules Document Control (dossiers de validation), Change (gestion des changements) et Audit de QualitOS.');

INSERT INTO standard_certification_stages (certification_path_id, stage_number, name, description, typical_duration_weeks, deliverables, actors, qualitos_modules, order_index) VALUES
    ('be000004-0000-0000-0000-000000000004', 1, 'Inventaire & catégorisation', 'Inventaire des systèmes GxP et catégorisation GAMP (1, 3, 4, 5).', '2-4', 'Inventaire des systèmes, catégorisation GAMP', 'Assurance qualité, IT', 'Document Control', 1),
    ('be000004-0000-0000-0000-000000000004', 2, 'Plan de validation & risques', 'Plan de validation et évaluation des risques qualité pilotant l''effort de CSV.', '2-4', 'Plan de validation, évaluation des risques qualité', 'AQ, Propriétaire système', 'Document Control, Risk', 2),
    ('be000004-0000-0000-0000-000000000004', 3, 'Spécifications & traçabilité', 'Rédaction des spécifications (URS/FS/DS) et de la matrice de traçabilité.', '3-6', 'URS/FS/DS, matrice de traçabilité', 'Métier, IT, AQ', 'Document Control', 3),
    ('be000004-0000-0000-0000-000000000004', 4, 'Vérification IQ/OQ/PQ', 'Exécution des qualifications d''installation, opérationnelle et de performance.', '4-10', 'Protocoles et rapports IQ/OQ/PQ', 'AQ, IT, Métier', 'Document Control, Audit', 4),
    ('be000004-0000-0000-0000-000000000004', 5, 'Data integrity & fournisseurs', 'Mise en place des contrôles ALCOA+ et exploitation des livrables fournisseur.', '2-6', 'Évaluation data integrity, dossier fournisseur', 'AQ, IT', 'Audit, Document Control', 5),
    ('be000004-0000-0000-0000-000000000004', 6, 'Rapport de validation & exploitation', 'Rapport de validation, mise en service et maintien de l''état validé (changements).', '2-4', 'Rapport de validation, procédure de maintien de l''état validé', 'AQ, IT', 'Change, CAPA', 6);

-- ============================================================================
-- HAS V2024 — Certification des établissements de santé (France)
-- ============================================================================

INSERT INTO standards (id, code, full_name, publisher, current_version, publication_date,
    family, applicable_industries, description, certification_body_required,
    recertification_cycle_months, related_norm_codes, status, created_at, updated_at)
VALUES (
    'b0000005-0000-0000-0000-000000000005',
    'has-v2024',
    'Certification HAS des établissements de santé — Référentiel V2024 (France)',
    'HAS',
    'V2024',
    DATE '2024-01-01',
    'REGULATORY',
    'healthcare',
    'Dispositif obligatoire d''évaluation externe de la qualité et de la sécurité des soins des établissements de santé en France, conduit par la Haute Autorité de Santé (HAS). La certification est prononcée par la HAS à l''issue d''une VISITE de certification réalisée par des experts-visiteurs ; le cycle est de 4 ans. Le référentiel V2024 est structuré en 3 chapitres (le patient ; les équipes de soins ; l''établissement), déclinés en 15 objectifs et en critères (standards, impératifs, avancés). Il met l''accent sur l''engagement du patient, le travail en équipe et la culture de l''évaluation des pratiques. L''évaluation s''appuie sur des méthodes spécifiques : le patient traceur, le traceur ciblé, le parcours traceur, l''audit système et l''observation.',
    TRUE,
    48,
    'iso-9001',
    'PUBLISHED', now(), now()
);

INSERT INTO standard_sections (id, standard_id, code, title, description, order_index) VALUES
    ('b5000005-0000-0000-0000-000000000001', 'b0000005-0000-0000-0000-000000000005', 'CH1', 'Le patient', 'Chapitre 1 : engagement et sécurité du patient.', 1),
    ('b5000005-0000-0000-0000-000000000002', 'b0000005-0000-0000-0000-000000000005', 'CH2', 'Les équipes de soins', 'Chapitre 2 : pertinence, coordination et pratiques des équipes.', 2),
    ('b5000005-0000-0000-0000-000000000003', 'b0000005-0000-0000-0000-000000000005', 'CH3', 'L''établissement', 'Chapitre 3 : gouvernance, culture qualité et gestion des risques.', 3),
    ('b5000005-0000-0000-0000-000000000004', 'b0000005-0000-0000-0000-000000000005', 'MET', 'Méthodes d''évaluation', 'Méthodes de la visite : patient traceur, traceur ciblé, audit système.', 4);

INSERT INTO standard_clauses (id, section_id, code, title, description, order_index) VALUES
    ('b9000005-0000-0000-0000-000000000001', 'b5000005-0000-0000-0000-000000000001', '1.1', 'Engagement du patient', 'Information, consentement et participation du patient à sa prise en charge.', 1),
    ('b9000005-0000-0000-0000-000000000002', 'b5000005-0000-0000-0000-000000000001', '1.2', 'Respect des droits & dignité', 'Respect des droits, de la dignité et de l''intimité du patient.', 2),
    ('b9000005-0000-0000-0000-000000000003', 'b5000005-0000-0000-0000-000000000002', '2.1', 'Pertinence des soins', 'Pertinence et personnalisation de la prise en charge.', 3),
    ('b9000005-0000-0000-0000-000000000004', 'b5000005-0000-0000-0000-000000000002', '2.2', 'Coordination & travail en équipe', 'Coordination des soins et fonctionnement en équipe pluriprofessionnelle.', 4),
    ('b9000005-0000-0000-0000-000000000005', 'b5000005-0000-0000-0000-000000000002', '2.3', 'Sécurité des actes & risques associés aux soins', 'Maîtrise des risques liés aux actes de soins (médicament, identito-vigilance, infections).', 5),
    ('b9000005-0000-0000-0000-000000000006', 'b5000005-0000-0000-0000-000000000003', '3.1', 'Gouvernance & culture qualité', 'Pilotage de la démarche qualité et culture de sécurité.', 6),
    ('b9000005-0000-0000-0000-000000000007', 'b5000005-0000-0000-0000-000000000003', '3.2', 'Gestion des risques & événements indésirables', 'Gestion des risques et traitement des événements indésirables associés aux soins.', 7),
    ('b9000005-0000-0000-0000-000000000008', 'b5000005-0000-0000-0000-000000000004', 'PT', 'Patient traceur & traceurs ciblés', 'Mise en œuvre des méthodes patient traceur et traceur ciblé.', 8),
    ('b9000005-0000-0000-0000-000000000009', 'b5000005-0000-0000-0000-000000000004', 'AS', 'Audit système & EPP', 'Audit système et évaluation des pratiques professionnelles (EPP).', 9);

INSERT INTO standard_requirements (clause_id, code, text, obligation, evidence_types, measurable_criteria, risk_if_missing, order_index) VALUES
    ('b9000005-0000-0000-0000-000000000001', '1.1', 'L''établissement doit garantir l''information du patient et recueillir son consentement, en favorisant sa participation aux décisions le concernant.', 'MUST', 'DOCUMENT,AUDIT', 'Information et consentement tracés ; dispositifs de participation du patient en place et évalués', 'HIGH', 1),
    ('b9000005-0000-0000-0000-000000000002', '1.2', 'L''établissement doit respecter les droits, la dignité et l''intimité du patient tout au long de sa prise en charge.', 'MUST', 'DOCUMENT,AUDIT', 'Respect des droits évalué (patient traceur) ; réclamations relatives aux droits suivies', 'HIGH', 2),
    ('b9000005-0000-0000-0000-000000000003', '2.1', 'Les équipes doivent assurer la pertinence et la personnalisation de la prise en charge sur la base de pratiques actualisées.', 'MUST', 'DOCUMENT,AUDIT', 'Pertinence évaluée par traceur ciblé/EPP ; protocoles actualisés et appliqués', 'HIGH', 3),
    ('b9000005-0000-0000-0000-000000000004', '2.2', 'Les équipes doivent coordonner les soins et travailler en équipe pluriprofessionnelle, y compris aux interfaces et transitions de parcours.', 'MUST', 'DOCUMENT,AUDIT', 'Coordination tracée (transmissions, réunions pluripro) ; transitions de parcours sécurisées', 'HIGH', 4),
    ('b9000005-0000-0000-0000-000000000005', '2.3', 'Les équipes doivent maîtriser les risques associés aux soins (sécurité médicamenteuse, identito-vigilance, prévention des infections).', 'MUST', 'DOCUMENT,KPI_RECORD', 'Indicateurs de sécurité des soins suivis (médicament, identito-vigilance, infections) avec cibles', 'CRITICAL', 5),
    ('b9000005-0000-0000-0000-000000000006', '3.1', 'L''établissement doit piloter sa démarche qualité et développer une culture de sécurité partagée.', 'MUST', 'DOCUMENT,TRAINING_RECORD', 'Gouvernance qualité formalisée ; mesure de la culture de sécurité et plan d''actions associé', 'MEDIUM', 6),
    ('b9000005-0000-0000-0000-000000000007', '3.2', 'L''établissement doit gérer les risques et traiter les événements indésirables associés aux soins, dont les déclarations obligatoires.', 'MUST', 'DOCUMENT,CAPA', 'Événements indésirables déclarés et analysés (CREX/RMM) ; EIG déclarés aux autorités tracés', 'CRITICAL', 7),
    ('b9000005-0000-0000-0000-000000000008', 'PT', 'L''établissement doit mettre en œuvre les méthodes d''évaluation patient traceur et traceur ciblé pour évaluer la qualité réelle des prises en charge.', 'MUST', 'AUDIT,DOCUMENT', 'Patients traceurs et traceurs ciblés réalisés selon le programme ; écarts suivis en plan d''actions', 'HIGH', 8),
    ('b9000005-0000-0000-0000-000000000009', 'AS', 'L''établissement doit conduire des audits système et des évaluations des pratiques professionnelles (EPP) au titre de la culture de l''évaluation.', 'MUST', 'AUDIT,DOCUMENT', 'Programme d''audits système et d''EPP réalisé ; résultats exploités en amélioration continue', 'HIGH', 9);

INSERT INTO standard_certification_paths (id, standard_id, estimated_duration_months_min, estimated_duration_months_max,
    estimated_cost_eur_min, estimated_cost_eur_max, difficulty_level,
    surveillance_audit_frequency, recertification_cycle_years, notes)
VALUES ('be000005-0000-0000-0000-000000000005', 'b0000005-0000-0000-0000-000000000005',
    12, 24, 20000, 100000, 4, 'none', 4,
    'Certification prononcée par la HAS (autorité publique) à l''issue d''une VISITE réalisée par des experts-visiteurs ; cycle de 4 ans (pas d''audit de surveillance annuel comme en ISO, d''où « none »). Le référentiel V2024 est organisé en 3 chapitres / 15 objectifs / critères (standards, impératifs, avancés) et privilégie l''engagement du patient, le travail en équipe et la culture de l''évaluation (patient traceur, traceur ciblé, audit système). Synergie avec les modules Audit (patient traceur / audit système), CAPA (événements indésirables), KPI (indicateurs de sécurité des soins) et Training de QualitOS.');

INSERT INTO standard_certification_stages (certification_path_id, stage_number, name, description, typical_duration_weeks, deliverables, actors, qualitos_modules, order_index) VALUES
    ('be000005-0000-0000-0000-000000000005', 1, 'Appropriation du référentiel V2024', 'Appropriation du référentiel (3 chapitres, 15 objectifs, critères) et engagement de la gouvernance.', '3-6', 'Plan d''appropriation, engagement gouvernance', 'Direction, CME, Qualité', 'Document Control, Training', 1),
    ('be000005-0000-0000-0000-000000000005', 2, 'Auto-évaluation & cartographie des écarts', 'Auto-évaluation au regard des critères et identification des écarts par chapitre.', '6-10', 'Auto-évaluation, cartographie des écarts', 'Coordonnateur qualité, Équipes', 'Audit, Standards Hub', 2),
    ('be000005-0000-0000-0000-000000000005', 3, 'Plan d''amélioration & engagement patient', 'Plan d''amélioration priorisé, dispositifs d''engagement du patient et du travail en équipe.', '8-16', 'Plan d''amélioration, dispositifs d''engagement patient', 'Qualité, Équipes de soins', 'CAPA, PDCA', 3),
    ('be000005-0000-0000-0000-000000000005', 4, 'Méthodes d''évaluation (traceurs/EPP)', 'Mise en œuvre des patients traceurs, traceurs ciblés, audits système et EPP.', '8-16', 'Comptes rendus de patients traceurs, audits système, EPP', 'Experts internes, Équipes', 'Audit, KPI', 4),
    ('be000005-0000-0000-0000-000000000005', 5, 'Préparation à la visite', 'Préparation logistique et documentaire de la visite des experts-visiteurs.', '3-6', 'Dossier de visite, planning, preuves rassemblées', 'Coordonnateur qualité', 'Document Control, Standards Hub', 5),
    ('be000005-0000-0000-0000-000000000005', 6, 'Visite de certification HAS', 'Visite des experts-visiteurs, rapport et décision de certification de la HAS.', '1-2', 'Rapport de certification, décision HAS', 'Experts-visiteurs HAS', 'Standards Hub, CAPA', 6);
