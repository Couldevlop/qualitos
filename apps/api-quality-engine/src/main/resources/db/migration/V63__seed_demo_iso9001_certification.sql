-- Seed démo d'un projet de certification ISO 9001 « prêt à montrer » pour le
-- tenant démo (00000000-0000-0000-0000-000000000099), afin de pouvoir lancer
-- immédiatement un AUDIT À BLANC et une CERTIFICATION À BLANC riches et réalistes
-- via le SPA (superadmin / superadmin), sans avoir à tout saisir à la main.
--
-- Contenu :
--   1. Une adoption (tenant_standards) IN_PROGRESS d'ISO 9001:2015.
--   2. Les 19 étapes de roadmap (§8.5) avec une progression crédible (1-12 faites,
--      13 « pré-audit » en cours, 14-19 à venir).
--   3. Des preuves liées couvrant l'essentiel des exigences, avec quelques ÉCARTS
--      DÉLIBÉRÉS (aucun MUST à risque élevé non couvert ⇒ 0 NC majeure ; quelques
--      MUST à risque faible/moyen et des SHOULD non couverts ⇒ NC mineures /
--      observations). Verdict attendu : audit blanc « QUASI PRÊT » + certification
--      à blanc « CERTIFIABLE SOUS RÉSERVE » (certificat à blanc émis).
--   4. Quelques preuves typées reliées aux artefacts démo de V54 (PDCA, 5S, CAPA,
--      Ishikawa, KPI) pour illustrer l'agrégation cross-module dans le dossier.
--
-- UUIDs déterministes (préfixe 'dddd5'). Ne touche aucune table partagée
-- inter-tenants : le catalogue ISO 9001 (platform-level) est seulement référencé.

-- ============================================================================
-- 1. Adoption ISO 9001 (projet de certification du tenant démo)
-- ============================================================================
INSERT INTO tenant_standards
    (id, tenant_id, standard_id, status, scope_description, target_certification_date,
     lead_auditor_id, certification_body, certified_at, expires_at, created_at, updated_at)
SELECT 'dddd5000-0000-0000-0000-00000000001a',
       '00000000-0000-0000-0000-000000000099',
       s.id,
       'IN_PROGRESS',
       'Conception, production et service après-vente — site siège + atelier extrusion ligne B.',
       current_date + 90,
       '00000000-0000-0000-0000-000000000001',
       'AFNOR Certification',
       NULL, NULL,
       now() - interval '120 days', now() - interval '3 days'
FROM standards s
WHERE s.code = 'iso-9001';

-- ============================================================================
-- 2. Roadmap de certification — 19 étapes (trame §8.5), progression réaliste
-- ============================================================================
INSERT INTO certification_roadmap_stages
    (tenant_id, tenant_standard_id, step_number, name, description, typical_duration,
     deliverables, responsible_role, involved_modules, status, assignee_id, order_index,
     created_at, updated_at)
SELECT '00000000-0000-0000-0000-000000000099',
       'dddd5000-0000-0000-0000-00000000001a',
       v.step, v.name, v.descr, v.dur, v.deliv, v.role, v.mods, v.status,
       '00000000-0000-0000-0000-000000000001', v.step,
       now() - interval '120 days', now() - interval '5 days'
FROM (VALUES
    (1, 'Cadrage & engagement direction',
        'Définir le périmètre, le budget, le planning et obtenir l''engagement formel de la direction.',
        '2-4 sem', 'Lettre d''engagement direction, périmètre, budget, planning',
        'Direction, Pilote certification', 'Document Control, PDCA', 'DONE'),
    (2, 'Diagnostic initial / gap analysis',
        'Évaluer l''écart entre les pratiques actuelles et les exigences de la norme.',
        '3-5 sem', 'Rapport d''écarts, matrice de conformité initiale',
        'Pilote, Auditeur interne', 'Audit, Standards Hub (audit blanc IA)', 'DONE'),
    (3, 'Définition de la politique & objectifs',
        'Formaliser la politique, les objectifs SMART et les indicateurs cibles.',
        '2 sem', 'Politique signée, objectifs SMART, indicateurs cibles',
        'Direction Qualité', 'Document Control, Catalogue KPI', 'DONE'),
    (4, 'Analyse du contexte & parties intéressées',
        'Analyser le contexte interne/externe et recenser les parties intéressées.',
        '2-3 sem', 'SWOT/PESTEL, registre des parties intéressées',
        'Pilote + équipes', 'Document Control', 'DONE'),
    (5, 'Analyse des risques & opportunités',
        'Cartographier les risques et opportunités et définir les plans de traitement.',
        '3-4 sem', 'Cartographie des risques, FMEA, plans de traitement',
        'Risk Manager', 'Risk/FMEA, Ishikawa', 'DONE'),
    (6, 'Cartographie des processus',
        'Établir la cartographie des processus, fiches processus et indicateurs.',
        '4-6 sem', 'Cartographie globale, fiches processus, indicateurs',
        'Pilotes de processus', 'Workflow Designer (BPMN), Standards Hub', 'DONE'),
    (7, 'Documentation système',
        'Rédiger le manuel, les procédures, modes opératoires et enregistrements.',
        '6-10 sem', 'Manuel, procédures, modes opératoires, enregistrements',
        'Tous pilotes', 'Document Control', 'DONE'),
    (8, 'Sensibilisation & formation',
        'Former et sensibiliser le personnel aux exigences et au système.',
        '4-6 sem', 'Plan de formation, supports, tests',
        'RH + Formation', 'Module Formation', 'DONE'),
    (9, 'Mise en œuvre opérationnelle',
        'Appliquer réellement les processus et générer les premiers enregistrements.',
        '8-16 sem', 'Application des processus, premiers enregistrements',
        'Toute l''organisation', 'Tous modules', 'DONE'),
    (10, 'Audits internes (1er cycle)',
        'Réaliser le programme d''audits internes et consigner les écarts.',
        '4-8 sem', 'Programme d''audit, rapports, écarts',
        'Auditeurs internes', 'Audit Management', 'DONE'),
    (11, 'Actions correctives',
        'Traiter les écarts via des CAPA et prouver leur efficacité.',
        '4-12 sem', 'CAPA, preuves d''efficacité',
        'Pilotes + responsables', 'CAPA, Non-conformité', 'DONE'),
    (12, 'Revue de direction',
        'Conduire la revue de direction et décider des ressources/orientations.',
        '1-2 sem', 'Compte-rendu de revue, décisions, ressources allouées',
        'Direction', 'PDCA, Document Control', 'DONE'),
    (13, 'Pré-audit (optionnel mais recommandé)',
        'Simuler l''audit de certification pour détecter les écarts résiduels.',
        '1 sem', 'Rapport d''audit blanc, écarts résiduels',
        'Auditeur tiers ou IA', 'Standards Hub (audit blanc IA)', 'IN_PROGRESS'),
    (14, 'Audit de certification — Étape 1 (revue documentaire)',
        'Revue documentaire par l''organisme certificateur.',
        '1-2 j', 'Rapport étape 1, points d''attention',
        'Organisme certificateur', 'Standards Hub, Document Control', 'NOT_STARTED'),
    (15, 'Audit de certification — Étape 2 (audit terrain)',
        'Audit terrain par l''organisme certificateur (NC majeures/mineures).',
        '2-5 j', 'Rapport étape 2, NC majeures/mineures, certificat',
        'Organisme certificateur', 'Standards Hub, tous modules', 'NOT_STARTED'),
    (16, 'Traitement des NC d''audit',
        'Lever les non-conformités relevées lors de l''audit de certification.',
        '1-3 mois', 'Plan d''action, preuves de levée',
        'Pilote', 'CAPA, Audit', 'NOT_STARTED'),
    (17, 'Obtention du certificat',
        'Réception du certificat (valide en général 3 ans).',
        '—', 'Certificat valide 3 ans',
        '—', 'Standards Hub (registre certificats)', 'NOT_STARTED'),
    (18, 'Audits de surveillance annuels',
        'Audits de surveillance périodiques par l''organisme certificateur.',
        '1-2 j/an', 'Rapports, levée des NC',
        'Organisme certificateur', 'Standards Hub', 'NOT_STARTED'),
    (19, 'Recertification (an 3)',
        'Audit complet de renouvellement du certificat.',
        '3-5 j', 'Audit complet de renouvellement',
        'Organisme certificateur', 'Standards Hub', 'NOT_STARTED')
) AS v(step, name, descr, dur, deliv, role, mods, status);

-- ============================================================================
-- 3. Preuves liées — couverture large avec ÉCARTS DÉLIBÉRÉS
--    Exclus (gaps) : 7.4 (MUST/LOW), 6.2.2 (MUST/MOYEN), 10.1 (MUST/MOYEN) → NC mineures
--                    7.1.6 (SHOULD/LOW), 8.3.1 (SHOULD/MOYEN)               → observations
--    Aucune exigence MUST à risque ÉLEVÉ/CRITIQUE n'est exclue ⇒ 0 NC majeure.
-- ============================================================================
INSERT INTO requirement_evidences
    (tenant_id, tenant_standard_id, requirement_id, evidence_type, evidence_ref_id,
     evidence_uri, note, linked_by, linked_at)
SELECT '00000000-0000-0000-0000-000000000099',
       'dddd5000-0000-0000-0000-00000000001a',
       r.id, 'DOCUMENT', NULL, NULL,
       'Procédure / enregistrement documenté couvrant ' || r.code || ' (démo).',
       '00000000-0000-0000-0000-000000000001',
       now() - interval '25 days'
FROM standard_requirements r
JOIN standard_clauses cl   ON cl.id  = r.clause_id
JOIN standard_sections sec ON sec.id = cl.section_id
JOIN standards s           ON s.id   = sec.standard_id
WHERE s.code = 'iso-9001'
  AND r.code NOT IN ('7.4', '6.2.2', '10.1', '7.1.6', '8.3.1');

-- ----------------------------------------------------------------------------
-- 3bis. Preuves typées reliées aux artefacts démo (V54) — agrégation cross-module.
--       evidence_ref_id pointe vers de vrais objets démo (pas de FK : référence souple).
-- ----------------------------------------------------------------------------
-- 6.1.1 (détermination des risques) ← diagramme Ishikawa validé (analyse de causes)
INSERT INTO requirement_evidences
    (tenant_id, tenant_standard_id, requirement_id, evidence_type, evidence_ref_id, evidence_uri, note, linked_by, linked_at)
SELECT '00000000-0000-0000-0000-000000000099', 'dddd5000-0000-0000-0000-00000000001a', r.id,
       'ISHIKAWA', 'dddd2000-0000-0000-0000-00000000003a', NULL,
       'Analyse causes-racines (escape rate fournisseur Alpha) alimentant la détermination des risques.',
       '00000000-0000-0000-0000-000000000001', now() - interval '40 days'
FROM standard_requirements r JOIN standard_clauses cl ON cl.id=r.clause_id
  JOIN standard_sections sec ON sec.id=cl.section_id JOIN standards s ON s.id=sec.standard_id
WHERE s.code='iso-9001' AND r.code='6.1.1';

-- 6.1.2 (plan de traitement des risques) ← cycle PDCA en cours
INSERT INTO requirement_evidences
    (tenant_id, tenant_standard_id, requirement_id, evidence_type, evidence_ref_id, evidence_uri, note, linked_by, linked_at)
SELECT '00000000-0000-0000-0000-000000000099', 'dddd5000-0000-0000-0000-00000000001a', r.id,
       'PDCA_CYCLE', 'dddd0000-0000-0000-0000-00000000001a', NULL,
       'Cycle PDCA « Réduction défauts de soudure » : action de traitement du risque qualité.',
       '00000000-0000-0000-0000-000000000001', now() - interval '20 days'
FROM standard_requirements r JOIN standard_clauses cl ON cl.id=r.clause_id
  JOIN standard_sections sec ON sec.id=cl.section_id JOIN standards s ON s.id=sec.standard_id
WHERE s.code='iso-9001' AND r.code='6.1.2';

-- 8.5.1 (conditions maîtrisées de production) ← audit 5S atelier extrusion
INSERT INTO requirement_evidences
    (tenant_id, tenant_standard_id, requirement_id, evidence_type, evidence_ref_id, evidence_uri, note, linked_by, linked_at)
SELECT '00000000-0000-0000-0000-000000000099', 'dddd5000-0000-0000-0000-00000000001a', r.id,
       'FIVES_AUDIT', 'dddd3000-0000-0000-0000-00000000001a', NULL,
       'Audit 5S « Atelier extrusion ligne B » (score 78) : maîtrise des conditions opérationnelles.',
       '00000000-0000-0000-0000-000000000001', now() - interval '14 days'
FROM standard_requirements r JOIN standard_clauses cl ON cl.id=r.clause_id
  JOIN standard_sections sec ON sec.id=cl.section_id JOIN standards s ON s.id=sec.standard_id
WHERE s.code='iso-9001' AND r.code='8.5.1';

-- 8.7 (maîtrise des non-conformités) ← dossier CAPA
INSERT INTO requirement_evidences
    (tenant_id, tenant_standard_id, requirement_id, evidence_type, evidence_ref_id, evidence_uri, note, linked_by, linked_at)
SELECT '00000000-0000-0000-0000-000000000099', 'dddd5000-0000-0000-0000-00000000001a', r.id,
       'CAPA', 'dddd4000-0000-0000-0000-00000000001a', NULL,
       'CAPA « NC répétitive joint torique fournisseur Alpha » : traitement des sorties non conformes.',
       '00000000-0000-0000-0000-000000000001', now() - interval '10 days'
FROM standard_requirements r JOIN standard_clauses cl ON cl.id=r.clause_id
  JOIN standard_sections sec ON sec.id=cl.section_id JOIN standards s ON s.id=sec.standard_id
WHERE s.code='iso-9001' AND r.code='8.7';

-- 9.1.1 (surveillance & mesure) ← tableau de bord KPI (FPY, NPS, MTTR)
INSERT INTO requirement_evidences
    (tenant_id, tenant_standard_id, requirement_id, evidence_type, evidence_ref_id, evidence_uri, note, linked_by, linked_at)
SELECT '00000000-0000-0000-0000-000000000099', 'dddd5000-0000-0000-0000-00000000001a', r.id,
       'KPI_RECORD', 'dddd6000-0000-0000-0000-00000000009a', '/dashboards/quality-overview',
       'Tableau de bord qualité (FPY, NPS, MTTR) : preuve de surveillance et mesure.',
       '00000000-0000-0000-0000-000000000001', now() - interval '7 days'
FROM standard_requirements r JOIN standard_clauses cl ON cl.id=r.clause_id
  JOIN standard_sections sec ON sec.id=cl.section_id JOIN standards s ON s.id=sec.standard_id
WHERE s.code='iso-9001' AND r.code='9.1.1';
