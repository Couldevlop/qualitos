-- Seed donnees demo pour le tenant demo (00000000-0000-0000-0000-000000000099).
-- Cible : rendre les ecrans PDCA / Ishikawa / 5S / CAPA non-vides pour la demo
-- superadmin / superadmin via le SPA. Ne touche AUCUNE table partagee
-- inter-tenants. Idempotent : utilise des UUIDs deterministes prefixe 'd'.

-- ============================================================================
-- PDCA cycles + steps
-- ============================================================================

INSERT INTO pdca_cycles (id, tenant_id, title, description, status, owner_id, created_at, updated_at, completed_at) VALUES
    ('dddd0000-0000-0000-0000-00000000001a', '00000000-0000-0000-0000-000000000099',
     'Reduction defauts de soudure - ligne 3',
     'Pilote : equipe production. Objectif : -30% NC en 90j. KPI cible : FPY > 96%.',
     'DO', '00000000-0000-0000-0000-000000000001',
     now() - interval '21 days', now() - interval '2 days', NULL),
    ('dddd0000-0000-0000-0000-00000000002a', '00000000-0000-0000-0000-000000000099',
     'Amelioration satisfaction patient - service ambulatoire',
     'CHU. Objectif : NPS +10 points en 6 mois. Baseline 42, cible 52.',
     'PLAN', '00000000-0000-0000-0000-000000000001',
     now() - interval '7 days', now() - interval '1 day', NULL),
    ('dddd0000-0000-0000-0000-00000000003a', '00000000-0000-0000-0000-000000000099',
     'Reduction MTTR incidents P1',
     'SRE. Objectif : MTTR < 30 min mediane. Baseline 52 min.',
     'CHECK', '00000000-0000-0000-0000-000000000001',
     now() - interval '60 days', now() - interval '3 days', NULL),
    ('dddd0000-0000-0000-0000-00000000004a', '00000000-0000-0000-0000-000000000099',
     'Conformite RGAA accueil numerique',
     'Service public. Objectif : passer de 67% RGAA AA conformite a 95%.',
     'ACT', '00000000-0000-0000-0000-000000000001',
     now() - interval '120 days', now() - interval '5 days', NULL),
    ('dddd0000-0000-0000-0000-00000000005a', '00000000-0000-0000-0000-000000000099',
     'Reduction taux de rappel produit pharma lot 2025-Q4',
     'Production GMP. Objectif : 0 rappel en 12 mois apres mise en place CAPA.',
     'COMPLETED', '00000000-0000-0000-0000-000000000001',
     now() - interval '180 days', now() - interval '10 days', now() - interval '10 days');

INSERT INTO pdca_steps (id, cycle_id, phase, title, description, status, assignee_id, due_date, created_at, updated_at) VALUES
    ('dddd1000-0000-0000-0000-00000000001a', 'dddd0000-0000-0000-0000-00000000001a', 'PLAN',
     'Analyse Pareto des NC soudure', 'Recensement NC 6 derniers mois, top 5 modes de defaillance.',
     'DONE', '00000000-0000-0000-0000-000000000001', current_date - 15, now() - interval '21 days', now() - interval '15 days'),
    ('dddd1000-0000-0000-0000-00000000002a', 'dddd0000-0000-0000-0000-00000000001a', 'DO',
     'Mise en place Poka-Yoke detrompeur', 'Capteur de positionnement avant arc, alarme sonore.',
     'IN_PROGRESS', '00000000-0000-0000-0000-000000000001', current_date + 7, now() - interval '14 days', now() - interval '1 day'),
    ('dddd1000-0000-0000-0000-00000000003a', 'dddd0000-0000-0000-0000-00000000003a', 'CHECK',
     'Mesure MTTR sur 30 jours', 'Extraction PagerDuty + comparaison baseline.',
     'IN_PROGRESS', '00000000-0000-0000-0000-000000000001', current_date + 3, now() - interval '10 days', now() - interval '2 days'),
    ('dddd1000-0000-0000-0000-00000000004a', 'dddd0000-0000-0000-0000-00000000004a', 'ACT',
     'Standardisation pratiques accessibilite', 'Documentation guides + formation rediger pour 25 contributeurs.',
     'PENDING', '00000000-0000-0000-0000-000000000001', current_date + 30, now() - interval '5 days', now() - interval '5 days');

-- ============================================================================
-- Ishikawa diagrams
-- ============================================================================

INSERT INTO ishikawa_diagrams (id, tenant_id, problem_statement, description, mode, status, owner_id, created_at, updated_at) VALUES
    ('dddd2000-0000-0000-0000-00000000001a', '00000000-0000-0000-0000-000000000099',
     'Pourquoi le taux de defauts soudure augmente-t-il depuis Q3 ?',
     'Lie au cycle PDCA dddd0000-...-001a. Analyse 6M.',
     'SIX_M', 'IN_REVIEW', '00000000-0000-0000-0000-000000000001',
     now() - interval '18 days', now() - interval '3 days'),
    ('dddd2000-0000-0000-0000-00000000002a', '00000000-0000-0000-0000-000000000099',
     'Pourquoi le temps d''attente urgences depasse 45 min ?',
     'Analyse pluridisciplinaire avec urgentistes et IAO.',
     'EIGHT_M', 'DRAFT', '00000000-0000-0000-0000-000000000001',
     now() - interval '6 days', now() - interval '1 day'),
    ('dddd2000-0000-0000-0000-00000000003a', '00000000-0000-0000-0000-000000000099',
     'Pourquoi l''escape rate fournisseur Alpha a triple ?',
     'Cluster NC matiere validee par echantillonnage 200 pieces.',
     'SEVEN_M', 'VALIDATED', '00000000-0000-0000-0000-000000000001',
     now() - interval '45 days', now() - interval '8 days');

-- ============================================================================
-- 5S audits + items
-- ============================================================================

INSERT INTO fives_audits (id, tenant_id, zone, description, status, auditor_id, scheduled_at, completed_at, overall_score, created_at, updated_at) VALUES
    ('dddd3000-0000-0000-0000-00000000001a', '00000000-0000-0000-0000-000000000099',
     'Atelier extrusion - ligne B', 'Audit mensuel recurrent. Photos drones + tablette.',
     'COMPLETED', '00000000-0000-0000-0000-000000000001',
     now() - interval '14 days', now() - interval '14 days', 78.0,
     now() - interval '14 days', now() - interval '14 days'),
    ('dddd3000-0000-0000-0000-00000000002a', '00000000-0000-0000-0000-000000000099',
     'Bloc operatoire OR-2', 'Audit pre-certification HAS V2024.',
     'IN_PROGRESS', '00000000-0000-0000-0000-000000000001',
     now() - interval '3 days', NULL, NULL,
     now() - interval '3 days', now() - interval '1 day'),
    ('dddd3000-0000-0000-0000-00000000003a', '00000000-0000-0000-0000-000000000099',
     'Datacenter Salle 3', 'Audit annuel ISO 27001 controle physique.',
     'DRAFT', '00000000-0000-0000-0000-000000000001',
     now() + interval '7 days', NULL, NULL,
     now() - interval '1 day', now() - interval '1 day');

INSERT INTO fives_audit_items (id, audit_id, pillar, score, note, created_at, updated_at) VALUES
    ('dddd3100-0000-0000-0000-00000000001a', 'dddd3000-0000-0000-0000-00000000001a', 'SEIRI',    8, 'Quelques outils non utilises a evacuer.', now() - interval '14 days', now() - interval '14 days'),
    ('dddd3100-0000-0000-0000-00000000002a', 'dddd3000-0000-0000-0000-00000000001a', 'SEITON',   7, 'Marquage au sol partiellement efface.',     now() - interval '14 days', now() - interval '14 days'),
    ('dddd3100-0000-0000-0000-00000000003a', 'dddd3000-0000-0000-0000-00000000001a', 'SEISO',    9, 'Nettoyage planifie OK.',                    now() - interval '14 days', now() - interval '14 days'),
    ('dddd3100-0000-0000-0000-00000000004a', 'dddd3000-0000-0000-0000-00000000001a', 'SEIKETSU', 8, 'Standards documentes recents.',             now() - interval '14 days', now() - interval '14 days'),
    ('dddd3100-0000-0000-0000-00000000005a', 'dddd3000-0000-0000-0000-00000000001a', 'SHITSUKE', 7, 'Audit precedent : 3 actions non clos.',     now() - interval '14 days', now() - interval '14 days');

-- ============================================================================
-- CAPA cases + actions
-- ============================================================================

INSERT INTO capa_cases (id, tenant_id, title, description, type, criticity, status, source_type, source_ref, owner_id, due_date, created_at, updated_at) VALUES
    ('dddd4000-0000-0000-0000-00000000001a', '00000000-0000-0000-0000-000000000099',
     'NC repetitive sur joint torique fournisseur Alpha',
     'Defaut etancheite detecte sur 4 lots successifs. Impact production majeur.',
     'CORRECTIVE', 'HIGH', 'IN_PROGRESS', 'NON_CONFORMITY', 'NC-2026-0142',
     '00000000-0000-0000-0000-000000000001', current_date + 14,
     now() - interval '10 days', now() - interval '2 days'),
    ('dddd4000-0000-0000-0000-00000000002a', '00000000-0000-0000-0000-000000000099',
     'Reclamation client - delai de livraison Q4',
     'Cluster reclamations sur la zone Sud-Est, OTIF tombe a 87%.',
     'CORRECTIVE', 'MEDIUM', 'OPEN', 'COMPLAINT', 'CMP-2026-0021',
     '00000000-0000-0000-0000-000000000001', current_date + 30,
     now() - interval '5 days', now() - interval '5 days'),
    ('dddd4000-0000-0000-0000-00000000003a', '00000000-0000-0000-0000-000000000099',
     'Calibration multimetre M-2241 expiree',
     'Detecte lors de l''audit interne metrologie. Equipement utilise sur ligne 5.',
     'CORRECTIVE', 'CRITICAL', 'RESOLVED', 'AUDIT', 'AUDIT-2026-0007',
     '00000000-0000-0000-0000-000000000001', current_date - 7,
     now() - interval '30 days', now() - interval '7 days'),
    ('dddd4000-0000-0000-0000-00000000004a', '00000000-0000-0000-0000-000000000099',
     'Prevention erreur etiquetage produits multi-langues',
     'Action preventive suite a near-miss sur lot 2026-EXP-0098.',
     'PREVENTIVE', 'MEDIUM', 'OPEN', 'INTERNAL', NULL,
     '00000000-0000-0000-0000-000000000001', current_date + 45,
     now() - interval '2 days', now() - interval '2 days');

INSERT INTO capa_actions (id, capa_id, title, description, status, assignee_id, due_date, created_at, updated_at) VALUES
    ('dddd4100-0000-0000-0000-00000000001a', 'dddd4000-0000-0000-0000-00000000001a',
     'Audit qualite fournisseur Alpha sur site', 'Visite techniciens + revue PPAP.',
     'IN_PROGRESS', '00000000-0000-0000-0000-000000000001', current_date + 7,
     now() - interval '10 days', now() - interval '3 days'),
    ('dddd4100-0000-0000-0000-00000000002a', 'dddd4000-0000-0000-0000-00000000001a',
     'Renforcer plan de controle reception joints', 'Echantillonnage 100% sur 3 lots suivants.',
     'PENDING', '00000000-0000-0000-0000-000000000001', current_date + 10,
     now() - interval '8 days', now() - interval '8 days'),
    ('dddd4100-0000-0000-0000-00000000003a', 'dddd4000-0000-0000-0000-00000000003a',
     'Calibration externe immediate + remise en service', 'Envoi labo accredite COFRAC.',
     'DONE', '00000000-0000-0000-0000-000000000001', current_date - 14,
     now() - interval '30 days', now() - interval '14 days');
