-- Standards Hub — vague 12 (CLAUDE.md §8.2) : 5 référentiels industrie / IT avancés.
-- VAGUE FINALE : le catalogue atteint 60 normes.
--   nadcap       : NADCAP (PRI) — accréditation des procédés spéciaux aéronautiques
--   as9110       : AS9110C (IAQG) — MRO aéronautique (maintenance, réparation, révision)
--   api-q1       : API Spec Q1 (10e éd. 2024) — systèmes qualité fabrication pétrole & gaz
--   cmmi         : CMMI v3.0 (ISACA) — modèle de maturité des processus
--   cis-controls : CIS Controls v8.1 (CIS) — contrôles de sécurité (auto-évaluation)
-- Même format que V78 : catalogue platform-level, UUID déterministes, préfixe 'd' (vague 12).
--   standards 'd000000n' (n=1..5), sections 'dn00000s' (préfixe d1..d5),
--   clauses 'da/db/dc/dd' (normes 1..4) + 'd9' (5e norme : cis-controls),
--   paths 'de00000n', stages via FK (sans UUID explicite).
-- Note unicité : V10 emploie le préfixe 'd' pour des clauses ISO 27001 (d0000041/051/061/091/101)
--   et V54/V63 des UUID 'dddd....' — toutes ces valeurs sont DISTINCTES de notre schéma
--   (standards d000000n n=1..5, sections dn00000s, clauses da/db/dc/dd/d9, paths de00000n).
--   Contrôle cross-fichier effectué : 0 collision.
-- Note related_norm_codes : cis-controls référence 'iso-27001,nist-csf' ; nist-csf est créée
--   EN PARALLÈLE dans la vague 11 (V79). La colonne related_norm_codes est en TEXT sans clé
--   étrangère : aucun risque d'intégrité même si V79 est mergée après V80.

-- ============================================================================
-- NADCAP — National Aerospace and Defense Contractors Accreditation Program (PRI)
-- ============================================================================

INSERT INTO standards (id, code, full_name, publisher, current_version, publication_date,
    family, applicable_industries, description, certification_body_required,
    recertification_cycle_months, related_norm_codes, status, created_at, updated_at)
VALUES (
    'd0000001-0000-0000-0000-000000000001',
    'nadcap',
    'NADCAP — National Aerospace and Defense Contractors Accreditation Program — Accréditation des procédés spéciaux aéronautiques',
    'PRI',
    'Checklists AC7xxx',
    DATE '1990-01-01',
    'SECTORIEL',
    'aerospace,defense',
    'NADCAP (National Aerospace and Defense Contractors Accreditation Program) est un programme d''ACCRÉDITATION industrielle administré par le PRI (Performance Review Institute) pour les procédés spéciaux du secteur aéronautique et de la défense. Contrairement à une certification de système de management, NADCAP est une accréditation par procédé : des task groups (groupes de travail par procédé — traitement thermique, soudage, contrôle non destructif/CND, traitements de surface, matériaux composites, essais des matériaux, etc.) définissent des checklists d''audit (séries AC7xxx) auxquelles le fournisseur doit se conformer. La conformité préalable à AS9100 est généralement attendue. L''auditeur PRI évalue chaque procédé contre la checklist applicable ; les non-conformités (NCR) doivent faire l''objet d''une analyse de cause racine et d''une réponse, typiquement sous 21 jours. La durée de validité de l''accréditation (12 à 24 mois) est attribuée AU MÉRITE : un fournisseur démontrant une bonne performance d''audit gagne des cycles plus longs, alors qu''un historique d''écarts raccourcit le cycle — nuance importante par rapport à un cycle fixe de certification.',
    TRUE,
    NULL,
    'as9100,iatf-16949',
    'PUBLISHED', now(), now()
);

INSERT INTO standard_sections (id, standard_id, code, title, description, order_index) VALUES
    ('d100000a-0000-0000-0000-000000000001', 'd0000001-0000-0000-0000-000000000001', 'SMQ', 'Système qualité & prérequis', 'Système qualité conforme à AS9100 et prérequis à l''accréditation NADCAP.', 1),
    ('d100000a-0000-0000-0000-000000000002', 'd0000001-0000-0000-0000-000000000001', 'PROC', 'Maîtrise des procédés spéciaux', 'Maîtrise des procédés spéciaux selon les checklists AC7xxx des task groups.', 2),
    ('d100000a-0000-0000-0000-000000000003', 'd0000001-0000-0000-0000-000000000001', 'AUDIT', 'Audit & accréditation PRI', 'Auto-audit, audit PRI et gestion des non-conformités d''accréditation.', 3),
    ('d100000a-0000-0000-0000-000000000004', 'd0000001-0000-0000-0000-000000000001', 'COMP', 'Compétence & traçabilité', 'Qualification du personnel, traçabilité et maintien de l''accréditation.', 4);

INSERT INTO standard_clauses (id, section_id, code, title, description, order_index) VALUES
    ('da000001-0000-0000-0000-000000000001', 'd100000a-0000-0000-0000-000000000001', 'SMQ.AS', 'Conformité AS9100', 'Système qualité conforme à AS9100 comme socle de l''accréditation.', 1),
    ('da000001-0000-0000-0000-000000000002', 'd100000a-0000-0000-0000-000000000001', 'SMQ.SC', 'Périmètre des procédés', 'Définition du périmètre des procédés spéciaux soumis à accréditation.', 2),
    ('da000001-0000-0000-0000-000000000003', 'd100000a-0000-0000-0000-000000000002', 'PROC.CL', 'Checklists AC7xxx', 'Conformité aux checklists AC7xxx du task group applicable.', 3),
    ('da000001-0000-0000-0000-000000000004', 'd100000a-0000-0000-0000-000000000002', 'PROC.PV', 'Validation des procédés', 'Validation et maîtrise des paramètres des procédés spéciaux.', 4),
    ('da000001-0000-0000-0000-000000000005', 'd100000a-0000-0000-0000-000000000002', 'PROC.EQ', 'Équipements & étalonnage', 'Maîtrise et étalonnage des équipements de procédé et de mesure.', 5),
    ('da000001-0000-0000-0000-000000000006', 'd100000a-0000-0000-0000-000000000003', 'AUD.SA', 'Auto-audit', 'Réalisation d''un auto-audit complet contre la checklist avant l''audit PRI.', 6),
    ('da000001-0000-0000-0000-000000000007', 'd100000a-0000-0000-0000-000000000003', 'AUD.PRI', 'Audit PRI', 'Audit d''accréditation par un auditeur du PRI par procédé.', 7),
    ('da000001-0000-0000-0000-000000000008', 'd100000a-0000-0000-0000-000000000003', 'AUD.NCR', 'Non-conformités (NCR)', 'Traitement des non-conformités d''audit avec analyse de cause racine.', 8),
    ('da000001-0000-0000-0000-000000000009', 'd100000a-0000-0000-0000-000000000004', 'CMP.PER', 'Qualification du personnel', 'Qualification et certification du personnel des procédés spéciaux.', 9),
    ('da000001-0000-0000-0000-000000000010', 'd100000a-0000-0000-0000-000000000004', 'CMP.TR', 'Traçabilité', 'Traçabilité des matières, lots et enregistrements de procédé.', 10);

INSERT INTO standard_requirements (clause_id, code, text, obligation, evidence_types, measurable_criteria, risk_if_missing, order_index) VALUES
    ('da000001-0000-0000-0000-000000000001', 'SMQ.AS.1', 'Le fournisseur doit disposer d''un système de management de la qualité conforme à AS9100 comme prérequis à l''accréditation NADCAP.', 'MUST', 'DOCUMENT,AUDIT', 'Certificat AS9100 valide présenté ; périmètre couvrant les procédés concernés', 'CRITICAL', 1),
    ('da000001-0000-0000-0000-000000000002', 'SMQ.SC.1', 'Le fournisseur doit définir le périmètre des procédés spéciaux soumis à l''accréditation et les task groups applicables.', 'MUST', 'DOCUMENT', 'Périmètre des procédés et task groups (AC7xxx) documenté et justifié', 'HIGH', 2),
    ('da000001-0000-0000-0000-000000000003', 'PROC.CL.1', 'Le fournisseur doit se conformer à la checklist AC7xxx du task group applicable à chaque procédé spécial.', 'MUST', 'AUDIT,DOCUMENT', 'Conformité démontrée à 100 % des items applicables de la checklist AC7xxx', 'CRITICAL', 3),
    ('da000001-0000-0000-0000-000000000004', 'PROC.PV.1', 'Le fournisseur doit valider ses procédés spéciaux et maîtriser leurs paramètres critiques.', 'MUST', 'DOCUMENT,KPI_RECORD', 'Procédés validés ; paramètres critiques surveillés et enregistrés', 'CRITICAL', 4),
    ('da000001-0000-0000-0000-000000000005', 'PROC.EQ.1', 'Le fournisseur doit maîtriser et étalonner les équipements de procédé et de mesure utilisés.', 'MUST', 'DOCUMENT', 'Plan d''étalonnage à jour ; équipements de procédé qualifiés et tracés', 'HIGH', 5),
    ('da000001-0000-0000-0000-000000000006', 'AUD.SA.1', 'Le fournisseur doit réaliser un auto-audit complet contre la checklist applicable avant l''audit PRI.', 'MUST', 'AUDIT,DOCUMENT', 'Auto-audit documenté couvrant l''intégralité de la checklist avant l''audit PRI', 'HIGH', 6),
    ('da000001-0000-0000-0000-000000000007', 'AUD.PRI.1', 'Le procédé doit être audité par un auditeur du PRI contre la checklist du task group correspondant.', 'MUST', 'AUDIT,DOCUMENT', 'Audit PRI réalisé par procédé ; rapport d''audit enregistré', 'CRITICAL', 7),
    ('da000001-0000-0000-0000-000000000008', 'AUD.NCR.1', 'Le fournisseur doit traiter chaque non-conformité d''audit (NCR) par une analyse de cause racine et une réponse dans le délai imparti (typiquement 21 jours).', 'MUST', 'CAPA,DOCUMENT', 'NCR clôturées avec analyse de cause racine ; réponse soumise sous 21 jours', 'CRITICAL', 8),
    ('da000001-0000-0000-0000-000000000009', 'CMP.PER.1', 'Le fournisseur doit qualifier et certifier le personnel réalisant les procédés spéciaux (ex. opérateurs CND, soudeurs).', 'MUST', 'TRAINING_RECORD,DOCUMENT', 'Personnel qualifié/certifié pour chaque procédé ; certifications à jour et tracées', 'HIGH', 9),
    ('da000001-0000-0000-0000-000000000010', 'CMP.TR.1', 'Le fournisseur doit assurer la traçabilité des matières, lots et enregistrements de procédé.', 'MUST', 'DOCUMENT', 'Traçabilité matière/lot assurée ; enregistrements de procédé conservés et retrouvables', 'HIGH', 10);

INSERT INTO standard_certification_paths (id, standard_id, estimated_duration_months_min, estimated_duration_months_max,
    estimated_cost_eur_min, estimated_cost_eur_max, difficulty_level,
    surveillance_audit_frequency, recertification_cycle_years, notes)
VALUES ('de000001-0000-0000-0000-000000000001', 'd0000001-0000-0000-0000-000000000001',
    6, 12, 15000, 80000, 5, 'periodic', NULL,
    'NADCAP est une ACCRÉDITATION par procédé administrée par le PRI, et non une certification de système de management. La conformité préalable à AS9100 est attendue. L''accréditation est attribuée AU MÉRITE : la durée de validité (12 à 24 mois) augmente avec une bonne performance d''audit et se raccourcit en cas d''écarts récurrents — d''où l''absence de cycle fixe de recertification. Synergie avec les modules Audit (auto-audit et audit PRI), CAPA (traitement des NCR sous 21 jours), Supplier (qualification des procédés fournisseurs) et Document Control (checklists AC7xxx, traçabilité) de QualitOS.');

INSERT INTO standard_certification_stages (certification_path_id, stage_number, name, description, typical_duration_weeks, deliverables, actors, qualitos_modules, order_index) VALUES
    ('de000001-0000-0000-0000-000000000001', 1, 'Cadrage & prérequis AS9100', 'Définition du périmètre des procédés, des task groups applicables et vérification du prérequis AS9100.', '3-5', 'Périmètre des procédés, liste des checklists AC7xxx, certificat AS9100', 'Direction qualité, Responsable procédés', 'Document Control, Supplier', 1),
    ('de000001-0000-0000-0000-000000000001', 2, 'Mise en conformité des procédés', 'Mise en conformité des procédés spéciaux aux exigences des checklists AC7xxx (paramètres, équipements, étalonnage).', '8-16', 'Dossiers de validation des procédés, plan d''étalonnage', 'Responsable procédés, Méthodes', 'Document Control', 2),
    ('de000001-0000-0000-0000-000000000001', 3, 'Qualification du personnel', 'Qualification et certification du personnel réalisant les procédés spéciaux.', '4-8', 'Matrice de qualification, certifications du personnel', 'RH, Responsable procédés', 'Training', 3),
    ('de000001-0000-0000-0000-000000000001', 4, 'Auto-audit complet', 'Réalisation d''un auto-audit complet contre la checklist AC7xxx applicable avant l''audit PRI.', '2-4', 'Rapport d''auto-audit, plan d''actions préalable', 'Auditeur interne', 'Audit', 4),
    ('de000001-0000-0000-0000-000000000001', 5, 'Audit d''accréditation PRI', 'Audit du PRI par procédé contre la checklist du task group ; émission des NCR éventuelles.', '1-2', 'Rapport d''audit PRI, liste des NCR', 'Auditeur PRI', 'Audit', 5),
    ('de000001-0000-0000-0000-000000000001', 6, 'Traitement NCR & accréditation', 'Analyse de cause racine et réponse aux NCR sous 21 jours, puis obtention de l''accréditation NADCAP.', '3-6', 'Réponses NCR avec cause racine, accréditation NADCAP', 'Responsable qualité', 'CAPA, Standards Hub', 6);

-- ============================================================================
-- AS9110C — Systèmes qualité MRO aéronautique (maintenance, réparation, révision)
-- ============================================================================

INSERT INTO standards (id, code, full_name, publisher, current_version, publication_date,
    family, applicable_industries, description, certification_body_required,
    recertification_cycle_months, related_norm_codes, status, created_at, updated_at)
VALUES (
    'd0000002-0000-0000-0000-000000000002',
    'as9110',
    'AS9110C — Systèmes de management de la qualité — Exigences pour les organismes de maintenance aéronautique (MRO)',
    'IAQG',
    'C',
    DATE '2016-11-01',
    'SECTORIEL',
    'aerospace',
    'AS9110C est la norme de système de management de la qualité de l''IAQG (International Aerospace Quality Group) dédiée aux organismes de MAINTENANCE, RÉPARATION et RÉVISION (MRO — Maintenance, Repair and Overhaul) aéronautiques. Elle reprend les exigences d''AS9100 et y ajoute des exigences spécifiques au MRO : maintien de la navigabilité, prévention, détection et maîtrise des pièces non approuvées ou suspectes (SUP — Suspected Unapproved Parts), prise en compte des facteurs humains (human factors) dans les tâches de maintenance, gestion et maîtrise des données de maintenance (cartes de travail, manuels, consignes de navigabilité), et traçabilité des pièces et composants. La certification, accréditée et enregistrée dans la base OASIS de l''IAQG, suit un cycle de 3 ans avec audits de surveillance annuels.',
    TRUE,
    NULL,
    'as9100',
    'PUBLISHED', now(), now()
);

INSERT INTO standard_sections (id, standard_id, code, title, description, order_index) VALUES
    ('d200000b-0000-0000-0000-000000000001', 'd0000002-0000-0000-0000-000000000002', 'AIR', 'Navigabilité', 'Maintien de la navigabilité et conformité réglementaire des prestations MRO.', 1),
    ('d200000b-0000-0000-0000-000000000002', 'd0000002-0000-0000-0000-000000000002', 'PARTS', 'Pièces & traçabilité', 'Maîtrise des pièces non approuvées/suspectes et traçabilité des composants.', 2),
    ('d200000b-0000-0000-0000-000000000003', 'd0000002-0000-0000-0000-000000000002', 'HF', 'Facteurs humains', 'Prise en compte des facteurs humains dans les opérations de maintenance.', 3),
    ('d200000b-0000-0000-0000-000000000004', 'd0000002-0000-0000-0000-000000000002', 'DATA', 'Données de maintenance', 'Gestion et maîtrise des données et de la documentation de maintenance.', 4);

INSERT INTO standard_clauses (id, section_id, code, title, description, order_index) VALUES
    ('db000002-0000-0000-0000-000000000001', 'd200000b-0000-0000-0000-000000000001', 'AIR.CW', 'Maintien de navigabilité', 'Garantie du maintien de la navigabilité des aéronefs et composants.', 1),
    ('db000002-0000-0000-0000-000000000002', 'd200000b-0000-0000-0000-000000000001', 'AIR.RG', 'Conformité réglementaire', 'Conformité aux exigences réglementaires de navigabilité applicables.', 2),
    ('db000002-0000-0000-0000-000000000003', 'd200000b-0000-0000-0000-000000000002', 'PRT.SUP', 'Pièces suspectes (SUP)', 'Prévention, détection et maîtrise des pièces non approuvées ou suspectes.', 3),
    ('db000002-0000-0000-0000-000000000004', 'd200000b-0000-0000-0000-000000000002', 'PRT.TR', 'Traçabilité des pièces', 'Traçabilité des pièces, composants et matières utilisés en MRO.', 4),
    ('db000002-0000-0000-0000-000000000005', 'd200000b-0000-0000-0000-000000000003', 'HF.AW', 'Sensibilisation facteurs humains', 'Sensibilisation du personnel aux facteurs humains en maintenance.', 5),
    ('db000002-0000-0000-0000-000000000006', 'd200000b-0000-0000-0000-000000000003', 'HF.ERR', 'Erreurs de maintenance', 'Prévention et investigation des erreurs de maintenance.', 6),
    ('db000002-0000-0000-0000-000000000007', 'd200000b-0000-0000-0000-000000000004', 'DAT.CTL', 'Maîtrise des données', 'Maîtrise des données et de la documentation de maintenance.', 7),
    ('db000002-0000-0000-0000-000000000008', 'd200000b-0000-0000-0000-000000000004', 'DAT.WC', 'Cartes de travail', 'Établissement et maîtrise des cartes de travail et instructions.', 8);

INSERT INTO standard_requirements (clause_id, code, text, obligation, evidence_types, measurable_criteria, risk_if_missing, order_index) VALUES
    ('db000002-0000-0000-0000-000000000001', 'AIR.CW.1', 'L''organisme MRO doit assurer le maintien de la navigabilité des aéronefs et composants qu''il entretient.', 'MUST', 'DOCUMENT,AUDIT', 'Processus de maintien de navigabilité documenté et appliqué ; libérations conformes', 'CRITICAL', 1),
    ('db000002-0000-0000-0000-000000000002', 'AIR.RG.1', 'L''organisme MRO doit identifier et respecter les exigences réglementaires de navigabilité applicables (consignes, agréments).', 'MUST', 'DOCUMENT', 'Veille réglementaire tenue à jour ; consignes de navigabilité appliquées et tracées', 'CRITICAL', 2),
    ('db000002-0000-0000-0000-000000000003', 'PRT.SUP.1', 'L''organisme MRO doit prévenir, détecter et maîtriser l''introduction de pièces non approuvées ou suspectes (SUP).', 'MUST', 'DOCUMENT,AUDIT', 'Processus de prévention des SUP en place ; pièces suspectes isolées et déclarées', 'CRITICAL', 3),
    ('db000002-0000-0000-0000-000000000004', 'PRT.TR.1', 'L''organisme MRO doit assurer la traçabilité des pièces, composants et matières utilisés lors des opérations de maintenance.', 'MUST', 'DOCUMENT,KPI_RECORD', 'Traçabilité pièce/composant assurée de la réception à l''installation ; enregistrements conservés', 'HIGH', 4),
    ('db000002-0000-0000-0000-000000000005', 'HF.AW.1', 'L''organisme MRO doit sensibiliser et former son personnel aux facteurs humains influant sur la sécurité des opérations de maintenance.', 'MUST', 'TRAINING_RECORD,DOCUMENT', 'Formation facteurs humains déployée ; taux de personnel formé suivi', 'HIGH', 5),
    ('db000002-0000-0000-0000-000000000006', 'HF.ERR.1', 'L''organisme MRO doit investiguer les erreurs de maintenance et mettre en œuvre des actions pour en prévenir la répétition.', 'MUST', 'CAPA,DOCUMENT', 'Erreurs de maintenance investiguées ; actions correctives suivies et efficacité mesurée', 'HIGH', 6),
    ('db000002-0000-0000-0000-000000000007', 'DAT.CTL.1', 'L''organisme MRO doit maîtriser les données de maintenance (manuels, consignes, révisions) et en garantir l''actualité.', 'MUST', 'DOCUMENT', 'Données de maintenance maîtrisées ; versions à jour et accessibles aux opérateurs', 'HIGH', 7),
    ('db000002-0000-0000-0000-000000000008', 'DAT.WC.1', 'L''organisme MRO doit établir et maîtriser des cartes de travail et instructions reflétant les données de maintenance applicables.', 'MUST', 'DOCUMENT,AUDIT', 'Cartes de travail établies, approuvées et conformes aux données de maintenance en vigueur', 'MEDIUM', 8);

INSERT INTO standard_certification_paths (id, standard_id, estimated_duration_months_min, estimated_duration_months_max,
    estimated_cost_eur_min, estimated_cost_eur_max, difficulty_level,
    surveillance_audit_frequency, recertification_cycle_years, notes)
VALUES ('de000002-0000-0000-0000-000000000002', 'd0000002-0000-0000-0000-000000000002',
    9, 18, 20000, 90000, 4, 'annual', 3,
    'Certification accréditée de l''IAQG, enregistrée dans la base OASIS ; cycle de 3 ans avec audits de surveillance annuels. AS9110C reprend les exigences d''AS9100 en y ajoutant les spécificités MRO (navigabilité, pièces suspectes SUP, facteurs humains, données de maintenance). Synergie avec les modules Audit (audits OASIS), CAPA (erreurs de maintenance), Document Control (cartes de travail, données de maintenance) et Training (facteurs humains) de QualitOS.');

INSERT INTO standard_certification_stages (certification_path_id, stage_number, name, description, typical_duration_weeks, deliverables, actors, qualitos_modules, order_index) VALUES
    ('de000002-0000-0000-0000-000000000002', 1, 'Cadrage & diagnostic MRO', 'Définition du périmètre MRO, des agréments visés et diagnostic d''écart par rapport à AS9110C.', '3-5', 'Périmètre MRO, rapport de diagnostic d''écart', 'Direction qualité, Responsable navigabilité', 'Document Control', 1),
    ('de000002-0000-0000-0000-000000000002', 2, 'Navigabilité & données de maintenance', 'Mise en place des processus de maintien de navigabilité et de maîtrise des données de maintenance.', '6-12', 'Processus de navigabilité, référentiel de données de maintenance, cartes de travail', 'Responsable navigabilité, Bureau technique', 'Document Control', 2),
    ('de000002-0000-0000-0000-000000000002', 3, 'Pièces suspectes & traçabilité', 'Déploiement du processus de prévention des pièces suspectes (SUP) et de la traçabilité des composants.', '4-8', 'Procédure SUP, système de traçabilité des pièces', 'Logistique, Qualité', 'Document Control', 3),
    ('de000002-0000-0000-0000-000000000002', 4, 'Facteurs humains & formation', 'Déploiement du programme facteurs humains et formation du personnel de maintenance.', '4-6', 'Programme facteurs humains, attestations de formation', 'RH, Responsable maintenance', 'Training', 4),
    ('de000002-0000-0000-0000-000000000002', 5, 'Audit interne & revue de direction', 'Audit interne du SMQ MRO et revue de direction préalable à la certification.', '3-5', 'Rapport d''audit interne, compte rendu de revue de direction', 'Auditeur interne, Direction', 'Audit, CAPA', 5),
    ('de000002-0000-0000-0000-000000000002', 6, 'Audit de certification (étapes 1 & 2)', 'Audit documentaire puis audit sur site par l''organisme certificateur et enregistrement OASIS.', '4-8', 'Certificat AS9110C, enregistrement OASIS', 'Organisme certificateur', 'Standards Hub, CAPA', 6);

-- ============================================================================
-- API Spec Q1 (10e éd. 2024) — Systèmes qualité fabrication pétrole & gaz
-- ============================================================================

INSERT INTO standards (id, code, full_name, publisher, current_version, publication_date,
    family, applicable_industries, description, certification_body_required,
    recertification_cycle_months, related_norm_codes, status, created_at, updated_at)
VALUES (
    'd0000003-0000-0000-0000-000000000003',
    'api-q1',
    'API Spec Q1 (10e édition, 2024) — Système de management de la qualité pour la fabrication dans l''industrie pétrolière et gazière',
    'API',
    '10e éd. 2024',
    DATE '2024-01-01',
    'SECTORIEL',
    'energy',
    'API Spec Q1 (10e édition, 2024), publiée par l''American Petroleum Institute (API), spécifie les exigences d''un système de management de la qualité pour les organismes qui fabriquent des produits destinés à l''industrie pétrolière et gazière. Elle conditionne l''octroi de la licence d''utilisation du MONOGRAMME API. Au-delà des exigences génériques de type ISO 9001, API Q1 met l''accent sur la gestion des risques liés au produit, l''établissement de plans qualité, la maîtrise du design package (dossier de conception), la traçabilité des matières, la maîtrise des procédés spéciaux, le management of change (MOC — gestion des modifications) et la maintenance préventive des équipements de production. La licence/monogramme suit un cycle de 3 ans avec audits de surveillance.',
    TRUE,
    NULL,
    'iso-9001',
    'PUBLISHED', now(), now()
);

INSERT INTO standard_sections (id, standard_id, code, title, description, order_index) VALUES
    ('d300000c-0000-0000-0000-000000000001', 'd0000003-0000-0000-0000-000000000003', 'RISK', 'Risques & plans qualité', 'Gestion des risques liés au produit et établissement des plans qualité.', 1),
    ('d300000c-0000-0000-0000-000000000002', 'd0000003-0000-0000-0000-000000000003', 'DESIGN', 'Conception & traçabilité', 'Maîtrise du design package et traçabilité des matières.', 2),
    ('d300000c-0000-0000-0000-000000000003', 'd0000003-0000-0000-0000-000000000003', 'PROC', 'Procédés & maintenance', 'Maîtrise des procédés spéciaux et maintenance préventive.', 3),
    ('d300000c-0000-0000-0000-000000000004', 'd0000003-0000-0000-0000-000000000003', 'MOC', 'Management of change', 'Gestion des modifications (MOC) et maîtrise documentaire.', 4);

INSERT INTO standard_clauses (id, section_id, code, title, description, order_index) VALUES
    ('dc000003-0000-0000-0000-000000000001', 'd300000c-0000-0000-0000-000000000001', 'RSK.PR', 'Gestion des risques produit', 'Identification et traitement des risques liés au produit.', 1),
    ('dc000003-0000-0000-0000-000000000002', 'd300000c-0000-0000-0000-000000000001', 'QP', 'Plans qualité', 'Établissement et application des plans qualité produit.', 2),
    ('dc000003-0000-0000-0000-000000000003', 'd300000c-0000-0000-0000-000000000002', 'DSG', 'Design package', 'Maîtrise du dossier de conception (design package).', 3),
    ('dc000003-0000-0000-0000-000000000004', 'd300000c-0000-0000-0000-000000000002', 'MAT', 'Traçabilité matière', 'Traçabilité des matières et identification des produits.', 4),
    ('dc000003-0000-0000-0000-000000000005', 'd300000c-0000-0000-0000-000000000003', 'SPC', 'Procédés spéciaux', 'Validation et maîtrise des procédés spéciaux.', 5),
    ('dc000003-0000-0000-0000-000000000006', 'd300000c-0000-0000-0000-000000000003', 'PM', 'Maintenance préventive', 'Maintenance préventive des équipements de production.', 6),
    ('dc000003-0000-0000-0000-000000000007', 'd300000c-0000-0000-0000-000000000004', 'MOC', 'Management of change', 'Gestion des modifications de produit, procédé ou système.', 7),
    ('dc000003-0000-0000-0000-000000000008', 'd300000c-0000-0000-0000-000000000004', 'DOC', 'Maîtrise documentaire', 'Maîtrise des informations documentées et des enregistrements.', 8);

INSERT INTO standard_requirements (clause_id, code, text, obligation, evidence_types, measurable_criteria, risk_if_missing, order_index) VALUES
    ('dc000003-0000-0000-0000-000000000001', 'RSK.PR.1', 'L''organisme doit identifier et traiter les risques liés au produit tout au long du cycle de réalisation.', 'MUST', 'DOCUMENT,AUDIT', 'Analyse de risques produit documentée ; actions de traitement suivies', 'CRITICAL', 1),
    ('dc000003-0000-0000-0000-000000000002', 'QP.1', 'L''organisme doit établir des plans qualité définissant les activités, contrôles et critères d''acceptation pour chaque produit ou famille de produits.', 'MUST', 'DOCUMENT', 'Plans qualité établis et appliqués pour 100 % des produits sous monogramme', 'HIGH', 2),
    ('dc000003-0000-0000-0000-000000000003', 'DSG.1', 'L''organisme doit maîtriser le design package (dossier de conception) et la validation de la conception.', 'MUST', 'DOCUMENT,AUDIT', 'Design package maîtrisé, vérifié et validé ; revues de conception enregistrées', 'HIGH', 3),
    ('dc000003-0000-0000-0000-000000000004', 'MAT.1', 'L''organisme doit assurer la traçabilité des matières et l''identification des produits tout au long de la fabrication.', 'MUST', 'DOCUMENT,KPI_RECORD', 'Traçabilité matière assurée ; certificats matière reliés aux lots fabriqués', 'CRITICAL', 4),
    ('dc000003-0000-0000-0000-000000000005', 'SPC.1', 'L''organisme doit valider et maîtriser ses procédés spéciaux (ex. soudage, traitement thermique, revêtement).', 'MUST', 'DOCUMENT,KPI_RECORD', 'Procédés spéciaux validés ; paramètres surveillés et enregistrés', 'HIGH', 5),
    ('dc000003-0000-0000-0000-000000000006', 'PM.1', 'L''organisme doit établir et appliquer un programme de maintenance préventive des équipements de production.', 'MUST', 'DOCUMENT,KPI_RECORD', 'Programme de maintenance préventive déployé ; taux de réalisation des interventions suivi', 'HIGH', 6),
    ('dc000003-0000-0000-0000-000000000007', 'MOC.1', 'L''organisme doit gérer les modifications (management of change) affectant le produit, les procédés ou le système qualité, avec analyse d''impact et approbation.', 'MUST', 'CAPA,DOCUMENT', 'Processus MOC en place ; modifications analysées, approuvées et tracées', 'HIGH', 7),
    ('dc000003-0000-0000-0000-000000000008', 'DOC.1', 'L''organisme doit maîtriser les informations documentées et conserver les enregistrements requis par la spécification.', 'MUST', 'DOCUMENT', 'Documents maîtrisés (versions, accès) ; enregistrements conservés selon les durées définies', 'MEDIUM', 8);

INSERT INTO standard_certification_paths (id, standard_id, estimated_duration_months_min, estimated_duration_months_max,
    estimated_cost_eur_min, estimated_cost_eur_max, difficulty_level,
    surveillance_audit_frequency, recertification_cycle_years, notes)
VALUES ('de000003-0000-0000-0000-000000000003', 'd0000003-0000-0000-0000-000000000003',
    9, 18, 18000, 85000, 4, 'annual', 3,
    'API Spec Q1 conditionne l''octroi de la LICENCE et du MONOGRAMME API ; cycle de 3 ans avec audits de surveillance. Au-delà du socle de type ISO 9001, API Q1 insiste sur la gestion des risques produit, les plans qualité, le design package, la traçabilité matière, les procédés spéciaux, le management of change (MOC) et la maintenance préventive. Synergie avec les modules Risk (risques produit), Document Control (plans qualité, design package), Change (MOC) et Calibration/Equipment (maintenance préventive) de QualitOS.');

INSERT INTO standard_certification_stages (certification_path_id, stage_number, name, description, typical_duration_weeks, deliverables, actors, qualitos_modules, order_index) VALUES
    ('de000003-0000-0000-0000-000000000003', 1, 'Cadrage & analyse des risques produit', 'Définition du périmètre, des produits sous monogramme et analyse des risques liés au produit.', '3-6', 'Périmètre, analyse de risques produit', 'Direction qualité, Bureau d''études', 'Document Control, Risk', 1),
    ('de000003-0000-0000-0000-000000000003', 2, 'Plans qualité & design package', 'Établissement des plans qualité et maîtrise du dossier de conception (design package).', '6-12', 'Plans qualité, design package, revues de conception', 'Bureau d''études, Qualité', 'Document Control', 2),
    ('de000003-0000-0000-0000-000000000003', 3, 'Procédés, traçabilité & maintenance', 'Validation des procédés spéciaux, mise en place de la traçabilité matière et de la maintenance préventive.', '8-14', 'Dossiers de validation, système de traçabilité, programme de maintenance préventive', 'Production, Maintenance', 'Calibration, Document Control', 3),
    ('de000003-0000-0000-0000-000000000003', 4, 'Management of change (MOC)', 'Déploiement du processus de gestion des modifications avec analyse d''impact et approbation.', '3-5', 'Procédure MOC, registre des modifications', 'Qualité, Méthodes', 'Change, Document Control', 4),
    ('de000003-0000-0000-0000-000000000003', 5, 'Audit interne & revue de direction', 'Audit interne du système qualité API Q1 et revue de direction.', '3-5', 'Rapport d''audit interne, compte rendu de revue de direction', 'Auditeur interne, Direction', 'Audit, CAPA', 5),
    ('de000003-0000-0000-0000-000000000003', 6, 'Audit API & licence/monogramme', 'Audit par l''API en vue de l''octroi de la licence et du monogramme, levée des écarts.', '4-8', 'Licence API, autorisation d''usage du monogramme', 'Auditeur API', 'Standards Hub, CAPA', 6);

-- ============================================================================
-- CMMI v3.0 (ISACA) — Modèle de maturité des processus
-- ============================================================================

INSERT INTO standards (id, code, full_name, publisher, current_version, publication_date,
    family, applicable_industries, description, certification_body_required,
    recertification_cycle_months, related_norm_codes, status, created_at, updated_at)
VALUES (
    'd0000004-0000-0000-0000-000000000004',
    'cmmi',
    'CMMI v3.0 — Capability Maturity Model Integration — Modèle de maturité et de capacité des processus',
    'ISACA',
    'v3.0',
    DATE '2023-04-01',
    'SECTORIEL',
    'it,manufacturing,services',
    'CMMI v3.0 (Capability Maturity Model Integration), géré par l''ISACA, est un modèle d''amélioration et d''évaluation de la maturité des processus organisationnels. Il s''organise en domaines de pratique (practice areas — ex. planification, gestion des exigences, assurance qualité produit et processus, analyse causale et résolution, gestion de configuration) regroupés par capacités. La maturité d''une organisation est mesurée selon des NIVEAUX (1 Initial à 5 Optimisation). Contrairement à une certification accréditée, CMMI repose sur des APPRAISALS (évaluations) menés par un Lead Appraiser certifié : un appraisal de type Benchmark établit le niveau de maturité, et un appraisal de Sustainment le maintient. Le résultat (niveau atteint) reste valide 3 ans — nuance importante : on parle d''évaluation et de niveau de maturité, et non de certificat de conformité.',
    TRUE,
    NULL,
    'iso-9001,itil-4',
    'PUBLISHED', now(), now()
);

INSERT INTO standard_sections (id, standard_id, code, title, description, order_index) VALUES
    ('d400000d-0000-0000-0000-000000000001', 'd0000004-0000-0000-0000-000000000004', 'PA', 'Domaines de pratique', 'Domaines de pratique (practice areas) du modèle CMMI.', 1),
    ('d400000d-0000-0000-0000-000000000002', 'd0000004-0000-0000-0000-000000000004', 'QUAL', 'Qualité produit & process', 'Assurance qualité produit/processus et analyse causale.', 2),
    ('d400000d-0000-0000-0000-000000000003', 'd0000004-0000-0000-0000-000000000004', 'MAT', 'Niveaux de maturité', 'Définition et atteinte des niveaux de maturité (1 à 5).', 3),
    ('d400000d-0000-0000-0000-000000000004', 'd0000004-0000-0000-0000-000000000004', 'APPR', 'Appraisals', 'Évaluations (appraisals Benchmark et Sustainment).', 4);

INSERT INTO standard_clauses (id, section_id, code, title, description, order_index) VALUES
    ('dd000004-0000-0000-0000-000000000001', 'd400000d-0000-0000-0000-000000000001', 'PA.PLAN', 'Planification', 'Pratiques de planification des activités et des estimations.', 1),
    ('dd000004-0000-0000-0000-000000000002', 'd400000d-0000-0000-0000-000000000001', 'PA.RDM', 'Gestion des exigences', 'Développement et gestion des exigences.', 2),
    ('dd000004-0000-0000-0000-000000000003', 'd400000d-0000-0000-0000-000000000001', 'PA.CM', 'Gestion de configuration', 'Maîtrise de la configuration des produits et artefacts.', 3),
    ('dd000004-0000-0000-0000-000000000004', 'd400000d-0000-0000-0000-000000000002', 'QL.PPQA', 'Assurance qualité', 'Assurance qualité produit et processus (PPQA).', 4),
    ('dd000004-0000-0000-0000-000000000005', 'd400000d-0000-0000-0000-000000000002', 'QL.CAR', 'Analyse causale', 'Analyse causale et résolution (causal analysis and resolution).', 5),
    ('dd000004-0000-0000-0000-000000000006', 'd400000d-0000-0000-0000-000000000003', 'MT.LVL', 'Niveau de maturité', 'Définition et atteinte d''un niveau de maturité cible.', 6),
    ('dd000004-0000-0000-0000-000000000007', 'd400000d-0000-0000-0000-000000000003', 'MT.PI', 'Amélioration des process', 'Amélioration continue des processus organisationnels.', 7),
    ('dd000004-0000-0000-0000-000000000008', 'd400000d-0000-0000-0000-000000000004', 'AP.BM', 'Appraisal Benchmark', 'Réalisation d''un appraisal Benchmark par un Lead Appraiser.', 8);

INSERT INTO standard_requirements (clause_id, code, text, obligation, evidence_types, measurable_criteria, risk_if_missing, order_index) VALUES
    ('dd000004-0000-0000-0000-000000000001', 'PA.PLAN.1', 'L''organisation doit établir et maintenir des plans pour ses activités, incluant estimations, échéancier et ressources.', 'MUST', 'DOCUMENT,PDCA_CYCLE', 'Plans établis et tenus à jour ; écarts plan/réel suivis', 'HIGH', 1),
    ('dd000004-0000-0000-0000-000000000002', 'PA.RDM.1', 'L''organisation doit développer et gérer les exigences ainsi que leur traçabilité tout au long du cycle de vie.', 'MUST', 'DOCUMENT', 'Exigences gérées avec matrice de traçabilité tenue à jour', 'HIGH', 2),
    ('dd000004-0000-0000-0000-000000000003', 'PA.CM.1', 'L''organisation doit établir et maintenir l''intégrité des produits et artefacts via une gestion de configuration.', 'MUST', 'DOCUMENT,AUDIT', 'Éléments de configuration identifiés ; baselines établies et audits de configuration réalisés', 'MEDIUM', 3),
    ('dd000004-0000-0000-0000-000000000004', 'QL.PPQA.1', 'L''organisation doit évaluer objectivement la conformité des processus et des produits aux descriptions et normes applicables.', 'MUST', 'AUDIT,DOCUMENT', 'Évaluations objectives PPQA réalisées ; non-conformités tracées et traitées', 'HIGH', 4),
    ('dd000004-0000-0000-0000-000000000005', 'QL.CAR.1', 'L''organisation doit identifier les causes des résultats observés (défauts, problèmes) et agir pour prévenir leur récurrence ou renforcer les bonnes pratiques.', 'MUST', 'CAPA,DOCUMENT', 'Analyses causales menées ; actions d''amélioration suivies et efficacité mesurée', 'HIGH', 5),
    ('dd000004-0000-0000-0000-000000000006', 'MT.LVL.1', 'L''organisation doit définir un niveau de maturité cible et démontrer la satisfaction des pratiques requises pour ce niveau.', 'MUST', 'DOCUMENT,KPI_RECORD', 'Niveau de maturité cible défini ; pratiques requises satisfaites et mesurées', 'HIGH', 6),
    ('dd000004-0000-0000-0000-000000000007', 'MT.PI.1', 'L''organisation doit améliorer en continu ses processus sur la base d''indicateurs de performance et de retours d''appraisal.', 'MUST', 'KPI_RECORD,PDCA_CYCLE', 'Indicateurs de performance des processus suivis ; cycles d''amélioration documentés', 'MEDIUM', 7),
    ('dd000004-0000-0000-0000-000000000008', 'AP.BM.1', 'L''organisation doit faire réaliser un appraisal Benchmark par un Lead Appraiser certifié pour établir son niveau de maturité.', 'MUST', 'AUDIT,DOCUMENT', 'Appraisal Benchmark réalisé par un Lead Appraiser ; niveau de maturité établi et publié', 'CRITICAL', 8);

INSERT INTO standard_certification_paths (id, standard_id, estimated_duration_months_min, estimated_duration_months_max,
    estimated_cost_eur_min, estimated_cost_eur_max, difficulty_level,
    surveillance_audit_frequency, recertification_cycle_years, notes)
VALUES ('de000004-0000-0000-0000-000000000004', 'd0000004-0000-0000-0000-000000000004',
    9, 24, 25000, 150000, 5, 'periodic', 3,
    'CMMI n''est pas une certification accréditée mais une ÉVALUATION (appraisal) de la maturité des processus : un appraisal Benchmark, conduit par un Lead Appraiser certifié, établit un niveau de maturité (1 à 5) valide 3 ans, maintenu par un appraisal de Sustainment. Synergie avec les modules PDCA (amélioration continue des processus), KPI (mesure de la performance des processus), Audit (assurance qualité PPQA, analyse causale) et Document Control (gestion des exigences et de la configuration) de QualitOS.');

INSERT INTO standard_certification_stages (certification_path_id, stage_number, name, description, typical_duration_weeks, deliverables, actors, qualitos_modules, order_index) VALUES
    ('de000004-0000-0000-0000-000000000004', 1, 'Cadrage & niveau cible', 'Définition du périmètre organisationnel, du modèle CMMI applicable et du niveau de maturité cible.', '3-6', 'Périmètre, niveau de maturité cible, plan d''amélioration', 'Direction, Responsable processus', 'Document Control, PDCA', 1),
    ('de000004-0000-0000-0000-000000000004', 2, 'Définition des processus', 'Définition et standardisation des processus couvrant les domaines de pratique requis.', '8-16', 'Référentiel de processus, descriptions des practice areas', 'Responsable processus, EPG', 'Document Control', 2),
    ('de000004-0000-0000-0000-000000000004', 3, 'Déploiement & mesure', 'Déploiement opérationnel des processus et mise en place des indicateurs de performance.', '12-24', 'Processus déployés, tableau de bord des indicateurs', 'Équipes projet, PPQA', 'KPI, PDCA', 3),
    ('de000004-0000-0000-0000-000000000004', 4, 'Assurance qualité & analyse causale', 'Mise en œuvre de l''assurance qualité (PPQA) et de l''analyse causale et résolution.', '6-10', 'Rapports PPQA, analyses causales, actions d''amélioration', 'PPQA, Responsable qualité', 'Audit, CAPA', 4),
    ('de000004-0000-0000-0000-000000000004', 5, 'Appraisal préliminaire (readiness)', 'Évaluation préliminaire de la maturité (gap appraisal) et préparation de l''appraisal Benchmark.', '4-8', 'Rapport de readiness, plan de remédiation', 'Lead Appraiser, Responsable processus', 'Audit, PDCA', 5),
    ('de000004-0000-0000-0000-000000000004', 6, 'Appraisal Benchmark', 'Réalisation de l''appraisal Benchmark par un Lead Appraiser certifié et publication du niveau de maturité.', '2-4', 'Rapport d''appraisal, niveau de maturité CMMI publié', 'Lead Appraiser', 'Standards Hub, Document Control', 6);

-- ============================================================================
-- CIS Controls v8.1 (2024) — Contrôles de sécurité (Center for Internet Security)
-- ============================================================================

INSERT INTO standards (id, code, full_name, publisher, current_version, publication_date,
    family, applicable_industries, description, certification_body_required,
    recertification_cycle_months, related_norm_codes, status, created_at, updated_at)
VALUES (
    'd0000005-0000-0000-0000-000000000005',
    'cis-controls',
    'CIS Controls v8.1 — Critical Security Controls — Cadre de contrôles de cybersécurité priorisés',
    'Center for Internet Security',
    'v8.1',
    DATE '2024-06-01',
    'SECTORIEL',
    'it,all',
    'Les CIS Controls v8.1 (Critical Security Controls), publiés par le Center for Internet Security (CIS), constituent un cadre VOLONTAIRE de bonnes pratiques de cybersécurité priorisées. La version 8.1 organise la sécurité en 18 contrôles déclinés en 153 safeguards (mesures), regroupés en trois groupes d''implémentation (Implementation Groups IG1 à IG3) selon la maturité et l''exposition au risque de l''organisation. Les contrôles couvrent notamment l''inventaire et la maîtrise des actifs matériels et logiciels, la protection des données, la configuration sécurisée, la gestion des comptes et des accès, la gestion continue des vulnérabilités, l''authentification multifacteur (MFA), la journalisation et la surveillance, les sauvegardes testées et la sensibilisation à la sécurité. Il ne s''agit pas d''un référentiel certifiable au sens accrédité : l''évaluation se fait typiquement par AUTO-ÉVALUATION (CIS CSAT — Controls Self Assessment Tool), même s''il peut servir de base à un audit tiers.',
    FALSE,
    NULL,
    'iso-27001,nist-csf',
    'PUBLISHED', now(), now()
);

INSERT INTO standard_sections (id, standard_id, code, title, description, order_index) VALUES
    ('d500000e-0000-0000-0000-000000000001', 'd0000005-0000-0000-0000-000000000005', 'INV', 'Inventaire des actifs', 'Inventaire et maîtrise des actifs matériels et logiciels.', 1),
    ('d500000e-0000-0000-0000-000000000002', 'd0000005-0000-0000-0000-000000000005', 'PROT', 'Protection & accès', 'Protection des données, gestion des accès et authentification.', 2),
    ('d500000e-0000-0000-0000-000000000003', 'd0000005-0000-0000-0000-000000000005', 'OPS', 'Détection & résilience', 'Vulnérabilités, journalisation et sauvegardes.', 3),
    ('d500000e-0000-0000-0000-000000000004', 'd0000005-0000-0000-0000-000000000005', 'GOV', 'Sensibilisation & IG', 'Sensibilisation et groupes d''implémentation (IG1-IG3).', 4);

INSERT INTO standard_clauses (id, section_id, code, title, description, order_index) VALUES
    ('d9000005-0000-0000-0000-000000000001', 'd500000e-0000-0000-0000-000000000001', 'CTL01', 'Inventaire des actifs', 'Contrôle 01 — Inventaire et maîtrise des actifs matériels.', 1),
    ('d9000005-0000-0000-0000-000000000002', 'd500000e-0000-0000-0000-000000000001', 'CTL02', 'Inventaire des logiciels', 'Contrôle 02 — Inventaire et maîtrise des actifs logiciels.', 2),
    ('d9000005-0000-0000-0000-000000000003', 'd500000e-0000-0000-0000-000000000002', 'CTL03', 'Protection des données', 'Contrôle 03 — Protection des données.', 3),
    ('d9000005-0000-0000-0000-000000000004', 'd500000e-0000-0000-0000-000000000002', 'CTL06', 'Gestion des accès & MFA', 'Contrôles 05-06 — Gestion des comptes, des accès et MFA.', 4),
    ('d9000005-0000-0000-0000-000000000005', 'd500000e-0000-0000-0000-000000000003', 'CTL07', 'Gestion des vulnérabilités', 'Contrôle 07 — Gestion continue des vulnérabilités.', 5),
    ('d9000005-0000-0000-0000-000000000006', 'd500000e-0000-0000-0000-000000000003', 'CTL08', 'Journalisation', 'Contrôle 08 — Gestion des journaux d''audit.', 6),
    ('d9000005-0000-0000-0000-000000000007', 'd500000e-0000-0000-0000-000000000003', 'CTL11', 'Sauvegardes testées', 'Contrôle 11 — Récupération des données et sauvegardes testées.', 7),
    ('d9000005-0000-0000-0000-000000000008', 'd500000e-0000-0000-0000-000000000004', 'CTL14', 'Sensibilisation', 'Contrôle 14 — Sensibilisation et formation à la sécurité.', 8),
    ('d9000005-0000-0000-0000-000000000009', 'd500000e-0000-0000-0000-000000000004', 'IG', 'Groupes d''implémentation', 'Sélection du groupe d''implémentation (IG1-IG3) et auto-évaluation.', 9);

INSERT INTO standard_requirements (clause_id, code, text, obligation, evidence_types, measurable_criteria, risk_if_missing, order_index) VALUES
    ('d9000005-0000-0000-0000-000000000001', 'CTL01.1', 'L''organisation doit établir et maintenir un inventaire précis et à jour de l''ensemble de ses actifs matériels.', 'MUST', 'DOCUMENT,KPI_RECORD', 'Inventaire des actifs matériels tenu à jour ; taux de couverture des actifs mesuré', 'CRITICAL', 1),
    ('d9000005-0000-0000-0000-000000000002', 'CTL02.1', 'L''organisation doit établir et maintenir un inventaire des logiciels autorisés et empêcher l''exécution des logiciels non autorisés.', 'MUST', 'DOCUMENT,KPI_RECORD', 'Inventaire des logiciels tenu à jour ; logiciels non autorisés détectés et traités', 'HIGH', 2),
    ('d9000005-0000-0000-0000-000000000003', 'CTL03.1', 'L''organisation doit identifier, classifier et protéger ses données selon leur sensibilité.', 'MUST', 'DOCUMENT', 'Données classifiées ; mesures de protection (chiffrement, contrôle d''accès) en place', 'HIGH', 3),
    ('d9000005-0000-0000-0000-000000000004', 'CTL06.1', 'L''organisation doit gérer les comptes et les droits d''accès et imposer l''authentification multifacteur (MFA) pour les accès sensibles.', 'MUST', 'DOCUMENT,KPI_RECORD', 'Comptes et accès gérés ; MFA activée pour les accès sensibles ; taux de couverture MFA suivi', 'CRITICAL', 4),
    ('d9000005-0000-0000-0000-000000000005', 'CTL07.1', 'L''organisation doit mettre en place une gestion continue des vulnérabilités (analyse et remédiation).', 'MUST', 'KPI_RECORD,DOCUMENT', 'Analyses de vulnérabilités récurrentes ; délai moyen de remédiation suivi', 'HIGH', 5),
    ('d9000005-0000-0000-0000-000000000006', 'CTL08.1', 'L''organisation doit collecter, conserver et analyser les journaux d''audit afin de détecter les incidents de sécurité.', 'MUST', 'DOCUMENT,AUDIT', 'Journaux d''audit collectés et conservés ; revue/analyse des journaux tracée', 'HIGH', 6),
    ('d9000005-0000-0000-0000-000000000007', 'CTL11.1', 'L''organisation doit réaliser des sauvegardes et tester périodiquement leur restauration.', 'MUST', 'DOCUMENT,KPI_RECORD', 'Sauvegardes réalisées selon le plan ; tests de restauration réussis et tracés', 'CRITICAL', 7),
    ('d9000005-0000-0000-0000-000000000008', 'CTL14.1', 'L''organisation doit sensibiliser et former son personnel à la sécurité afin de réduire les risques liés au comportement.', 'MUST', 'TRAINING_RECORD,DOCUMENT', 'Programme de sensibilisation déployé ; taux de personnel formé suivi', 'MEDIUM', 8),
    ('d9000005-0000-0000-0000-000000000009', 'IG.1', 'L''organisation doit sélectionner un groupe d''implémentation (IG1, IG2 ou IG3) adapté à son profil de risque et réaliser une auto-évaluation des safeguards applicables.', 'SHOULD', 'AUDIT,DOCUMENT', 'Groupe d''implémentation choisi et justifié ; auto-évaluation (CSAT) des safeguards réalisée', 'MEDIUM', 9);

INSERT INTO standard_certification_paths (id, standard_id, estimated_duration_months_min, estimated_duration_months_max,
    estimated_cost_eur_min, estimated_cost_eur_max, difficulty_level,
    surveillance_audit_frequency, recertification_cycle_years, notes)
VALUES ('de000005-0000-0000-0000-000000000005', 'd0000005-0000-0000-0000-000000000005',
    3, 9, 5000, 40000, 3, 'periodic', NULL,
    'Les CIS Controls v8.1 forment un cadre VOLONTAIRE non certifiable au sens accrédité : l''évaluation se fait typiquement par AUTO-ÉVALUATION via l''outil CIS CSAT (Controls Self Assessment Tool), selon le groupe d''implémentation retenu (IG1-IG3). Le cadre peut néanmoins servir de socle à un audit tiers et est largement aligné avec ISO 27001 et le NIST CSF. Synergie avec les modules Document Control (inventaires d''actifs, classification), KPI (couverture MFA, délai de remédiation des vulnérabilités), Audit (revue des journaux, auto-évaluation CSAT) et Training (sensibilisation) de QualitOS.');

INSERT INTO standard_certification_stages (certification_path_id, stage_number, name, description, typical_duration_weeks, deliverables, actors, qualitos_modules, order_index) VALUES
    ('de000005-0000-0000-0000-000000000005', 1, 'Cadrage & groupe d''implémentation', 'Définition du périmètre, du profil de risque et sélection du groupe d''implémentation (IG1-IG3).', '2-4', 'Périmètre, profil de risque, choix de l''IG', 'RSSI, Direction', 'Document Control', 1),
    ('de000005-0000-0000-0000-000000000005', 2, 'Inventaires des actifs & logiciels', 'Établissement des inventaires des actifs matériels et logiciels et de la classification des données.', '3-6', 'Inventaire des actifs, inventaire des logiciels, classification des données', 'Équipe IT, RSSI', 'Document Control', 2),
    ('de000005-0000-0000-0000-000000000005', 3, 'Protection, accès & MFA', 'Déploiement de la protection des données, de la gestion des accès et de l''authentification multifacteur.', '4-8', 'Politique d''accès, déploiement MFA, mesures de chiffrement', 'Équipe IT, RSSI', 'Document Control, KPI', 3),
    ('de000005-0000-0000-0000-000000000005', 4, 'Vulnérabilités, journaux & sauvegardes', 'Mise en place de la gestion des vulnérabilités, de la journalisation et des sauvegardes testées.', '4-8', 'Processus de gestion des vulnérabilités, journalisation centralisée, tests de restauration', 'Équipe SecOps', 'KPI, Audit', 4),
    ('de000005-0000-0000-0000-000000000005', 5, 'Sensibilisation du personnel', 'Déploiement du programme de sensibilisation et de formation à la sécurité.', '2-4', 'Programme de sensibilisation, attestations de formation', 'RSSI, RH', 'Training', 5),
    ('de000005-0000-0000-0000-000000000005', 6, 'Auto-évaluation CIS CSAT', 'Réalisation de l''auto-évaluation des safeguards via CIS CSAT et plan d''amélioration de la posture.', '2-4', 'Rapport CIS CSAT, plan d''amélioration de la cybersécurité', 'RSSI', 'Standards Hub, Audit', 6);
