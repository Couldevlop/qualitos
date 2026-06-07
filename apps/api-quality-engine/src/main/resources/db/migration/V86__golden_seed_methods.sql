-- ============================================================================
-- V86 — Jeu de données « GOLDEN » de référence (vitrine / onboarding)
-- ----------------------------------------------------------------------------
-- UN cas exemplaire, complet et réaliste (contenu FR) par méthode qualité,
-- chargé pour le tenant démo 00000000-0000-0000-0000-000000000099.
-- Objectif : afficher, pour chaque méthode, un cas « fait dans les règles de
-- l'art » servant de référence et remplir les modules vides (Cercle, DMAIC).
--
-- Acteur (owner_id / auditor_id / proposed_by / black_belt_id …) :
--   utilisateur démo 00000000-0000-0000-0000-000000000001 (cf. seeds V54/V63).
-- Tenant démo : 00000000-0000-0000-0000-000000000099.
--
-- Préfixe UUID déterministe DÉDIÉ au golden : 90100000-0000-0000-0000-…
--   (distinct de 'dddd…' utilisé par V54/V63 — aucune collision).
--   Tous les segments sont hexadécimaux valides. Plan de numérotation :
--     PDCA       cycle  …0000000000a1   / steps …000000a1NN
--     5S         audit  …0000000000b1   / items …000000b1NN
--     Cercle     circle …0000000000c1   / membres …c1aNN / réunions …c1bNN /
--                propositions …c1cNN / utilisateurs membres …00000c1002..1005
--     DMAIC      projet …0000000000d1   / mesures …000000d1fNN / poka-yoke …d1e01
--     Ishikawa   diag   …0000000000e1   / causes …00000e1NNN
--
-- Idempotence : chaque INSERT est gardé par WHERE NOT EXISTS (… id …)
--   (sécurité même si Flyway venait à rejouer).
-- Timestamps : littéraux déterministes (TIMESTAMP '2026-…') — pas de now().
-- ============================================================================


-- ============================================================================
-- 1. PDCA — Cycle complet (4 étapes Plan/Do/Check/Act) — cycle abouti COMPLETED
-- ============================================================================
INSERT INTO pdca_cycles (id, tenant_id, title, description, status, owner_id, created_at, updated_at, completed_at)
SELECT '90100000-0000-0000-0000-0000000000a1'::uuid,
       '00000000-0000-0000-0000-000000000099',
       '[GOLDEN] Réduction du taux de rebut sur la ligne d''assemblage A',
       'Cas de référence PDCA — Roue de Deming complète. Objectif : ramener le taux de rebut de 4,2 % à moins de 1,5 % en 90 jours sur la ligne A. KPI cible : First Pass Yield (FPY) >= 98 %. Cycle clôturé : objectif atteint (FPY mesuré 98,4 %, rebut 1,3 %).',
       'COMPLETED', '00000000-0000-0000-0000-000000000001',
       TIMESTAMP '2026-02-03 08:00:00+00', TIMESTAMP '2026-05-12 17:00:00+00', TIMESTAMP '2026-05-12 17:00:00+00'
WHERE NOT EXISTS (SELECT 1 FROM pdca_cycles WHERE id = '90100000-0000-0000-0000-0000000000a1'::uuid);

INSERT INTO pdca_steps (id, cycle_id, phase, title, description, status, assignee_id, due_date, created_at, updated_at)
SELECT v.id::uuid, '90100000-0000-0000-0000-0000000000a1'::uuid, v.phase, v.title, v.descr, v.status,
       '00000000-0000-0000-0000-000000000001', v.due::date, v.created::timestamptz, v.updated::timestamptz
FROM (VALUES
    ('90100000-0000-0000-0000-000000a10001', 'PLAN',
     'Diagnostic & objectif chiffré',
     'Analyse Pareto des rebuts sur 6 mois : 70 % imputables au mauvais serrage de 2 vis. Objectif SMART défini : FPY >= 98 % sous 90 j. Baseline FPY = 95,8 %, rebut = 4,2 %.',
     'DONE', '2026-02-14', '2026-02-03 08:00:00+00', '2026-02-14 16:00:00+00'),
    ('90100000-0000-0000-0000-000000a10002', 'DO',
     'Déploiement contre-mesures',
     'Mise en place d''une visseuse à couple contrôlé + détrompeur de positionnement (Poka-Yoke). Formation des 6 opérateurs. Mode opératoire mis à jour et affiché au poste.',
     'DONE', '2026-03-20', '2026-02-15 08:00:00+00', '2026-03-20 16:00:00+00'),
    ('90100000-0000-0000-0000-000000a10003', 'CHECK',
     'Mesure d''efficacité sur 30 jours',
     'Suivi FPY quotidien sur 30 jours après déploiement. Résultat : FPY = 98,4 % (cible >= 98 % atteinte), rebut = 1,3 % (cible < 1,5 % atteinte). Carte de contrôle stable, aucun signal hors limites.',
     'DONE', '2026-04-22', '2026-03-21 08:00:00+00', '2026-04-22 16:00:00+00'),
    ('90100000-0000-0000-0000-000000a10004', 'ACT',
     'Standardisation & généralisation',
     'Standard validé et intégré au plan de contrôle. Généralisation des contre-mesures aux lignes B et C planifiée. Revue de clôture en revue de direction. Leçons capitalisées dans la base de connaissances qualité.',
     'DONE', '2026-05-12', '2026-04-23 08:00:00+00', '2026-05-12 17:00:00+00')
) AS v(id, phase, title, descr, status, due, created, updated)
WHERE NOT EXISTS (SELECT 1 FROM pdca_steps WHERE id = v.id::uuid);


-- ============================================================================
-- 2. 5S — Audit noté sur les 5 piliers + score global cohérent, COMPLETED
--    Score global = moyenne des items ramenée sur 100 :
--    (9+8+9+8+7)/5 = 8,2/10 -> 82,0/100.
-- ============================================================================
INSERT INTO fives_audits (id, tenant_id, zone, description, status, auditor_id, scheduled_at, completed_at, overall_score, created_at, updated_at)
SELECT '90100000-0000-0000-0000-0000000000b1'::uuid,
       '00000000-0000-0000-0000-000000000099',
       '[GOLDEN] Atelier d''usinage CNC — îlot 1',
       'Audit 5S de référence réalisé tablette + photos, mode terrain. Notation des 5 piliers, observations consignées, plan d''action généré. Score global 82/100 (bon niveau, axes d''amélioration sur Shitsuke).',
       'COMPLETED', '00000000-0000-0000-0000-000000000001',
       TIMESTAMP '2026-05-06 09:00:00+00', TIMESTAMP '2026-05-06 11:30:00+00', 82.0,
       TIMESTAMP '2026-05-06 09:00:00+00', TIMESTAMP '2026-05-06 11:30:00+00'
WHERE NOT EXISTS (SELECT 1 FROM fives_audits WHERE id = '90100000-0000-0000-0000-0000000000b1'::uuid);

INSERT INTO fives_audit_items (id, audit_id, pillar, score, note, created_at, updated_at)
SELECT v.id::uuid, '90100000-0000-0000-0000-0000000000b1'::uuid, v.pillar, v.score, v.note,
       TIMESTAMP '2026-05-06 11:30:00+00', TIMESTAMP '2026-05-06 11:30:00+00'
FROM (VALUES
    ('90100000-0000-0000-0000-000000b10001', 'SEIRI',    9, 'Débarras exemplaire : seuls les outils du jour sont présents. 2 fûts vides à évacuer en zone de tri.'),
    ('90100000-0000-0000-0000-000000b10002', 'SEITON',   8, 'Rangement à sa place : ombrage des outils en place. Marquage au sol d''une allée à rafraîchir.'),
    ('90100000-0000-0000-0000-000000b10003', 'SEISO',    9, 'Nettoyage : poste propre, plan de nettoyage affiché et suivi. Aucune fuite d''huile constatée.'),
    ('90100000-0000-0000-0000-000000b10004', 'SEIKETSU', 8, 'Standardisation : standards visuels présents et à jour. Un standard de réglage manque sur la CNC-3.'),
    ('90100000-0000-0000-0000-000000b10005', 'SHITSUKE', 7, 'Rigueur : 1 action du précédent audit non soldée. Renforcer l''ancrage des routines d''auto-contrôle.')
) AS v(id, pillar, score, note)
WHERE NOT EXISTS (SELECT 1 FROM fives_audit_items WHERE id = v.id::uuid);


-- ============================================================================
-- 3. CERCLE DE QUALITÉ — cercle + membres (rôles) + 2 réunions + 2 propositions
-- ============================================================================
INSERT INTO quality_circles (id, tenant_id, name, description, topic, status, created_at, updated_at)
SELECT '90100000-0000-0000-0000-0000000000c1'::uuid,
       '00000000-0000-0000-0000-000000000099',
       '[GOLDEN] Cercle Qualité — Ergonomie & flux poste d''emballage',
       'Cas de référence Cercle de Qualité. Groupe pluridisciplinaire de 5 personnes (animateur, secrétaire, 3 membres) travaillant sur l''ergonomie et la réduction des temps morts au poste d''emballage. Méthode : réunions cadencées, propositions tracées de l''idée à la mesure d''impact.',
       'Ergonomie et fluidité du poste d''emballage',
       'ACTIVE', TIMESTAMP '2026-03-02 09:00:00+00', TIMESTAMP '2026-05-15 17:00:00+00'
WHERE NOT EXISTS (SELECT 1 FROM quality_circles WHERE id = '90100000-0000-0000-0000-0000000000c1'::uuid);

INSERT INTO circle_members (id, circle_id, user_id, role, joined_at)
SELECT v.id::uuid, '90100000-0000-0000-0000-0000000000c1'::uuid, v.user_id::uuid, v.role, TIMESTAMP '2026-03-02 09:00:00+00'
FROM (VALUES
    ('90100000-0000-0000-0000-000000c1a001', '00000000-0000-0000-0000-000000000001', 'FACILITATOR'),
    ('90100000-0000-0000-0000-000000c1a002', '90100000-0000-0000-0000-000000c10002', 'SECRETARY'),
    ('90100000-0000-0000-0000-000000c1a003', '90100000-0000-0000-0000-000000c10003', 'MEMBER'),
    ('90100000-0000-0000-0000-000000c1a004', '90100000-0000-0000-0000-000000c10004', 'MEMBER'),
    ('90100000-0000-0000-0000-000000c1a005', '90100000-0000-0000-0000-000000c10005', 'MEMBER')
) AS v(id, user_id, role)
WHERE NOT EXISTS (SELECT 1 FROM circle_members WHERE id = v.id::uuid);

INSERT INTO circle_meetings (id, circle_id, title, agenda, scheduled_at, duration_minutes, location, status, minutes, held_at, created_at, updated_at)
SELECT v.id::uuid, '90100000-0000-0000-0000-0000000000c1'::uuid, v.title, v.agenda,
       v.scheduled::timestamptz, v.dur, v.loc, v.status, v.minutes, v.held::timestamptz,
       v.created::timestamptz, v.updated::timestamptz
FROM (VALUES
    ('90100000-0000-0000-0000-000000c1b001',
     'Réunion 1 — Cadrage & recensement des irritants',
     'Tour de table, recensement des irritants du poste, vote des sujets prioritaires.',
     '2026-03-09 14:00:00+00', 60, 'Salle qualité — bâtiment B', 'HELD',
     'CR : 7 irritants recensés, 2 retenus (postures contraignantes, ruptures d''approvisionnement consommables). 2 propositions ouvertes.',
     '2026-03-09 14:00:00+00', '2026-03-02 09:00:00+00', '2026-03-09 15:30:00+00'),
    ('90100000-0000-0000-0000-000000c1b002',
     'Réunion 2 — Évaluation des propositions & décisions',
     'Présentation des chiffrages, décision de mise en œuvre, mesure d''impact attendue.',
     '2026-04-13 14:00:00+00', 75, 'Salle qualité — bâtiment B', 'HELD',
     'CR : proposition « table réglable » approuvée et déployée ; proposition « kanban consommables » mise en œuvre, impact mesuré (-35 % de ruptures).',
     '2026-04-13 14:00:00+00', '2026-03-09 15:30:00+00', '2026-04-13 15:45:00+00')
) AS v(id, title, agenda, scheduled, dur, loc, status, minutes, held, created, updated)
WHERE NOT EXISTS (SELECT 1 FROM circle_meetings WHERE id = v.id::uuid);

INSERT INTO circle_proposals (id, circle_id, meeting_id, title, description, status, proposed_by, validated_by, validated_at, implemented_at, measured_at, impact_note, rejection_reason, created_at, updated_at)
SELECT v.id::uuid, '90100000-0000-0000-0000-0000000000c1'::uuid, v.meeting_id::uuid, v.title, v.descr, v.status,
       v.proposed_by::uuid, v.validated_by::uuid, v.validated_at::timestamptz, v.implemented_at::timestamptz,
       v.measured_at::timestamptz, v.impact, v.reject, v.created::timestamptz, v.updated::timestamptz
FROM (VALUES
    ('90100000-0000-0000-0000-000000c1c001', '90100000-0000-0000-0000-000000c1b001',
     'Installer une table d''emballage à hauteur réglable',
     'Réduire les troubles musculo-squelettiques liés aux postures statiques debout. Table électrique réglable + tapis anti-fatigue.',
     'IMPLEMENTED', '90100000-0000-0000-0000-000000c10003', '00000000-0000-0000-0000-000000000001',
     '2026-04-13 15:00:00+00', '2026-04-28 12:00:00+00', NULL,
     'Déployée sur le poste pilote. Retour opérateurs très positif. Mesure d''impact ergonomique programmée à 60 jours.', NULL,
     '2026-03-09 15:00:00+00', '2026-04-28 12:00:00+00'),
    ('90100000-0000-0000-0000-000000c1c002', '90100000-0000-0000-0000-000000c1b001',
     'Mettre en place un kanban des consommables d''emballage',
     'Supprimer les ruptures d''adhésif et de film en instaurant un réapprovisionnement à seuil visuel (2 bacs).',
     'MEASURED', '90100000-0000-0000-0000-000000c10002', '00000000-0000-0000-0000-000000000001',
     '2026-04-13 15:10:00+00', '2026-04-20 12:00:00+00', '2026-05-15 12:00:00+00',
     'Ruptures de consommables réduites de 35 % sur le premier mois. Temps morts associés quasi éliminés. Standard retenu.', NULL,
     '2026-03-09 15:10:00+00', '2026-05-15 12:00:00+00')
) AS v(id, meeting_id, title, descr, status, proposed_by, validated_by, validated_at, implemented_at, measured_at, impact, reject, created, updated)
WHERE NOT EXISTS (SELECT 1 FROM circle_proposals WHERE id = v.id::uuid);


-- ============================================================================
-- 4. DMAIC + Poka-Yoke — projet parcourant les 5 phases, CONTROL, COMPLETED
--    Spécification : couple de serrage cible 12,0 Nm (LSL 11,0 / USL 13,0).
--    12 relevés en phase Measure ⇒ illustration capabilité Cp/Cpk.
-- ============================================================================
INSERT INTO dmaic_projects
    (id, tenant_id, title, problem_statement, goal_statement, phase, status,
     champion_id, black_belt_id, target_completion_date,
     spec_lower_limit, spec_upper_limit, spec_target, spec_unit,
     estimated_savings_eur, started_at, completed_at, created_at, updated_at)
SELECT '90100000-0000-0000-0000-0000000000d1'::uuid,
       '00000000-0000-0000-0000-000000000099',
       '[GOLDEN] Maîtrise du couple de serrage — visserie critique moteur',
       'Le couple de serrage des vis critiques dérive hors spécification (LSL 11,0 Nm / USL 13,0 Nm), générant 3,1 % de reprises et un risque de desserrage en service. Cpk initial estimé à 0,78 (procédé non capable).',
       'Porter le procédé à un niveau capable Cpk >= 1,33 et réduire les reprises sous 0,5 % en 4 mois, en centrant le procédé sur la cible 12,0 Nm.',
       'CONTROL', 'COMPLETED',
       '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000001', '2026-05-31'::date,
       11.0, 13.0, 12.0, 'Nm',
       48000.0, TIMESTAMP '2026-01-20 08:00:00+00', TIMESTAMP '2026-05-20 17:00:00+00',
       TIMESTAMP '2026-01-20 08:00:00+00', TIMESTAMP '2026-05-20 17:00:00+00'
WHERE NOT EXISTS (SELECT 1 FROM dmaic_projects WHERE id = '90100000-0000-0000-0000-0000000000d1'::uuid);

-- Mesures de procédé (phase MEASURE) — 12 relevés centrés ~12,0 Nm, faible dispersion
INSERT INTO dmaic_process_measures (id, project_id, value, subgroup_id, source_ref, recorded_at, operator_id, note, created_at)
SELECT v.id::uuid, '90100000-0000-0000-0000-0000000000d1'::uuid, v.val, v.sg, 'Banc de mesure couple BMC-07',
       v.rec::timestamptz, '00000000-0000-0000-0000-000000000001', v.note, v.rec::timestamptz
FROM (VALUES
    ('90100000-0000-0000-0000-000000d1f001', 12.05, 'G1', '2026-02-10 09:00:00+00', 'Relevé baseline sous-groupe 1'),
    ('90100000-0000-0000-0000-000000d1f002', 11.92, 'G1', '2026-02-10 09:05:00+00', NULL),
    ('90100000-0000-0000-0000-000000d1f003', 12.11, 'G1', '2026-02-10 09:10:00+00', NULL),
    ('90100000-0000-0000-0000-000000d1f004', 11.98, 'G2', '2026-02-11 09:00:00+00', 'Sous-groupe 2'),
    ('90100000-0000-0000-0000-000000d1f005', 12.03, 'G2', '2026-02-11 09:05:00+00', NULL),
    ('90100000-0000-0000-0000-000000d1f006', 12.20, 'G2', '2026-02-11 09:10:00+00', NULL),
    ('90100000-0000-0000-0000-000000d1f007', 11.88, 'G3', '2026-02-12 09:00:00+00', 'Sous-groupe 3'),
    ('90100000-0000-0000-0000-000000d1f008', 12.07, 'G3', '2026-02-12 09:05:00+00', NULL),
    ('90100000-0000-0000-0000-000000d1f009', 11.95, 'G3', '2026-02-12 09:10:00+00', NULL),
    ('90100000-0000-0000-0000-000000d1f010', 12.14, 'G4', '2026-02-13 09:00:00+00', 'Sous-groupe 4 — après réglage'),
    ('90100000-0000-0000-0000-000000d1f011', 12.00, 'G4', '2026-02-13 09:05:00+00', NULL),
    ('90100000-0000-0000-0000-000000d1f012', 11.97, 'G4', '2026-02-13 09:10:00+00', 'Procédé centré, dispersion réduite')
) AS v(id, val, sg, rec, note)
WHERE NOT EXISTS (SELECT 1 FROM dmaic_process_measures WHERE id = v.id::uuid);

-- Poka-Yoke associé (phase IMPROVE) : dispositif catalogue PY-LIMIT-SW-DEPTH-001
INSERT INTO pokayoke_assignments
    (id, tenant_id, project_id, device_id, status, note, implemented_at, verified_at, defect_reduction_pct, created_at, updated_at)
SELECT '90100000-0000-0000-0000-000000d1e001'::uuid,
       '00000000-0000-0000-0000-000000000099',
       '90100000-0000-0000-0000-0000000000d1'::uuid,
       d.id,
       'VERIFIED',
       'Visseuse à couple contrôlé avec arrêt automatique au couple cible : empêche le sur/sous-serrage. Vérifié sur 30 jours, reprises ramenées à 0,4 %.',
       TIMESTAMP '2026-04-05 12:00:00+00', TIMESTAMP '2026-05-10 12:00:00+00', 87.0,
       TIMESTAMP '2026-03-25 09:00:00+00', TIMESTAMP '2026-05-10 12:00:00+00'
FROM pokayoke_devices d
WHERE d.code = 'PY-LIMIT-SW-DEPTH-001'
  AND NOT EXISTS (SELECT 1 FROM pokayoke_assignments WHERE id = '90100000-0000-0000-0000-000000d1e001'::uuid);


-- ============================================================================
-- 5. ISHIKAWA — diagramme 6M complet, causes réparties sur les 6 branches
-- ============================================================================
INSERT INTO ishikawa_diagrams (id, tenant_id, problem_statement, description, mode, status, owner_id, created_at, updated_at)
SELECT '90100000-0000-0000-0000-0000000000e1'::uuid,
       '00000000-0000-0000-0000-000000000099',
       '[GOLDEN] Pourquoi le couple de serrage des vis critiques dérive-t-il hors spécification ?',
       'Diagramme de référence Ishikawa 6M (Machines, Méthodes, Main-d''œuvre, Matières, Mesures, Milieu). Analyse causes-racines alimentant le projet DMAIC « Maîtrise du couple de serrage ». Causes scorées, racines probables identifiées.',
       'SIX_M', 'VALIDATED', '00000000-0000-0000-0000-000000000001',
       TIMESTAMP '2026-02-05 09:00:00+00', TIMESTAMP '2026-03-15 17:00:00+00'
WHERE NOT EXISTS (SELECT 1 FROM ishikawa_diagrams WHERE id = '90100000-0000-0000-0000-0000000000e1'::uuid);

INSERT INTO ishikawa_causes (id, diagram_id, parent_id, category, label, description, root_cause_score, created_at, updated_at)
SELECT v.id::uuid, '90100000-0000-0000-0000-0000000000e1'::uuid, NULL, v.cat, v.label, v.descr, v.score,
       TIMESTAMP '2026-02-06 10:00:00+00', TIMESTAMP '2026-03-15 17:00:00+00'
FROM (VALUES
    -- MACHINES
    ('90100000-0000-0000-0000-00000e100101', 'MACHINES', 'Visseuse pneumatique sans contrôle de couple', 'L''outil ne régule pas le couple final : dépend de la pression réseau et du ressenti opérateur.', 0.9),
    ('90100000-0000-0000-0000-00000e100102', 'MACHINES', 'Maintenance préventive de la visseuse non planifiée', 'Usure de l''embrayage non suivie, dérive progressive du couple délivré.', 0.55),
    -- METHODS
    ('90100000-0000-0000-0000-00000e100201', 'METHODS', 'Mode opératoire ne définit pas le couple cible', 'Aucune valeur cible ni tolérance documentée au poste.', 0.7),
    ('90100000-0000-0000-0000-00000e100202', 'METHODS', 'Pas d''auto-contrôle du serrage', 'Aucune vérification au banc en cours de série.', 0.5),
    -- MANPOWER
    ('90100000-0000-0000-0000-00000e100301', 'MANPOWER', 'Opérateurs non formés au serrage contrôlé', 'Pratiques hétérogènes selon l''opérateur et l''équipe.', 0.6),
    ('90100000-0000-0000-0000-00000e100302', 'MANPOWER', 'Rotation de poste fréquente', 'Perte de savoir-faire spécifique au poste de serrage.', 0.35),
    -- MATERIALS
    ('90100000-0000-0000-0000-00000e100401', 'MATERIALS', 'Variabilité du revêtement des vis', 'Coefficient de frottement variable entre lots fournisseurs => couple résultant variable.', 0.45),
    ('90100000-0000-0000-0000-00000e100402', 'MATERIALS', 'Filetage taraudé hors tolérance', 'Quelques taraudages limites augmentent le frottement.', 0.3),
    -- MEASUREMENTS
    ('90100000-0000-0000-0000-00000e100501', 'MEASUREMENTS', 'Clé dynamométrique de contrôle non étalonnée', 'Calibration périmée : mesures de contrôle non fiables (incertitude élevée).', 0.5),
    ('90100000-0000-0000-0000-00000e100502', 'MEASUREMENTS', 'Absence de carte de contrôle SPC', 'Aucune surveillance statistique de la dérive du couple.', 0.4),
    -- ENVIRONMENT
    ('90100000-0000-0000-0000-00000e100601', 'ENVIRONMENT', 'Variation de la pression du réseau d''air comprimé', 'Pression fluctuante selon la charge atelier => couple pneumatique instable.', 0.5),
    ('90100000-0000-0000-0000-00000e100602', 'ENVIRONMENT', 'Poste mal éclairé / encombré', 'Conditions dégradant la précision du geste de serrage.', 0.25)
) AS v(id, cat, label, descr, score)
WHERE NOT EXISTS (SELECT 1 FROM ishikawa_causes WHERE id = v.id::uuid);
