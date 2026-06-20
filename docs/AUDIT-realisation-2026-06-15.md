# Audit de réalisation QualitOS — 2026-06-15

> Mesuré sur le code (pas la documentation). Méthode : comptes réels (modules,
> normes, packs, ADR, tests) + relecture de `docs/AUDIT-stub-vs-reel.md`.
> Référence précédente : audit 2026-06-04 (~60 %).

## Chiffres durs

| Preuve | Valeur |
|---|---|
| Modules métier (backend `api-quality-engine`) | 68 |
| Industry Packs sectoriels | 14 / 14 |
| Normes Standards Hub | 60 / 60 |
| Migrations Flyway (engine) | 86 |
| ADR (décisions tracées) | 21 |
| Features front Angular | 47 |
| Tests (front specs + suites Python) | 59 + 27 |
| Capacités IA/IoT/blockchain « RÉEL » (audit stub-vs-réel) | 20 |

## Taux de réalisation : ~87 % (≈ 96 % rapporté à un MVP commercialisable)

> Màj 2026-06-19 (lot GPU/scaffold intégré, CI verte `3efbd94` : Vision ONNX vrai chemin ADR 0029,
> Edge inference store-and-forward ADR 0030, backends ML lourds opt-in Prophet/LSTM/HDBSCAN/BERT/Whisper ADR 0031).
> Màj 2026-06-16 (après 3 lots de chantiers parallèles : IA, IoT, Dashboards, Formation, Doc).
> Pondéré par l'importance des axes de la vision CLAUDE.md. Le reliquat est essentiellement
> **GPU / infra réelle / contenu externe** (non livrable en CI sans stub — donc non fait par principe).

| Axe | Réalisation | Reste à faire |
|---|---|---|
| 5 méthodes (§3) | ~95 % | — (complet + golden seed) |
| Modules transverses (§4) | ~90 % | profondeur de quelques workflows |
| Standards Hub (§8) | ~85 % | audit blanc IA avancé, génération doc IA, marketplace |
| Industry Packs (§5) | ~85 % | profondeur sectorielle |
| Pilier confiance (§11) | ~95 % | bc-fips (bloqué upstream) |
| Sécurité (§11) | ~80 % | DAST/pentest, durcissement prod |
| Prod-readiness (§14) | ~80 % | chaos/perf |
| Frontend (§15) | ~85 % | couverture tests composants |
| **IA (§12)** | **~85 %** | reste : **modèles entraînés réels** (YOLOv8 5S, GPU pour BERT/Whisper/LSTM/Prophet/HDBSCAN) — la **plomberie est complète** : Vision chemin ONNX réel exercé (ADR 0029), backends lourds **opt-in câblés** import paresseux/extra `ml` (ADR 0031, défaut léger réel intact, 422/501 si absent) ; anomalies/forecast/clustering DBSCAN/SHAP/NLP lexical pleinement livrés |
| **IoT (§9)** | **~80 %** | reste : **runtime Edge long-running** (souscription MQTT→orchestrateur) + modèle ONNX réel, vrai cluster TimescaleDB, DICOM — livrés : Digital Twin/Shadow, LoRaWAN, Sparkplug B, Modbus, rollups + continuous aggregate TimescaleDB, **composant inférence Edge store-and-forward + ONNX/repli (ADR 0030, 37 tests)** |
| **Dashboards (§7)** | **~85 %** | livrés : **NLQ→graphe, Mode TV, Storyboards IA, cross-filtering + drill-down + annotations collaboratives persistées (ADR 0034), time-travel as-of réel (ADR 0035)** — reste : builder drag&drop avancé (`/dashboard-builder` existe) |
| **Doc & formation (§19)** | **~70 %** | LMS/gamification, vidéos ; espace Wiki utilisateur par rôle/module **et par secteur** livré (`docs/wiki/`, dont `docs/wiki/secteurs/` : 14 guides) |

## Capacités IA — état (post chantiers du 13-14 juin)

| Capacité | Verdict |
|---|---|
| LLM (Ollama/Anthropic/Mistral), RAG, NLQ | RÉEL |
| SPC (8 règles de Nelson) | RÉEL |
| Anomalies non-supervisées (Isolation Forest + reconstruction ACP) | RÉEL (ADR 0022) |
| Prévision KPI (Holt-Winters) | RÉEL (ADR 0023) |
| Scoring fournisseur (logistique pondéré) | RÉEL |
| Clustering NC (TF-IDF + DBSCAN densité) | RÉEL (commit 5d7045e ; HDBSCAN GPU = futur) |
| Storyboards IA (récit LLM des KPIs) | RÉEL (§7.4, commit 7518b10) |
| Explicabilité SHAP (Kernel SHAP) | RÉEL (ADR 0024) |
| NLP réclamations (sentiment + classification, lexical) | RÉEL (ADR 0025) |
| NLP plein BERT / transcription Whisper | ABSENT (lot GPU) |
| Vision 5S (YOLOv8) | backend ONNX réel, **modèle non fourni** |
| Federated learning | scaffold |

## Plan vers 100 % (attaqué un par un)

Ordre d'attaque retenu (du plus self-contained au plus lourd) :

1. **Clustering NC** — densité réelle (DBSCAN/HDBSCAN-like, NumPy pur) en remplacement du TF-IDF.
2. **Explicabilité SHAP** — valeurs de Shapley exactes pour les modèles linéaires/pondérés (scoring fournisseur, forecast), NumPy pur.
3. **NLP réclamations** — classification + sentiment (lexique/embeddings légers) honnête sans dépendance lourde.
4. **IoT** — Digital Twin réhydraté, protocole supplémentaire, TimescaleDB.
5. **Dashboards premium (§7)** — builder de widgets, NLQ→graphe.
6. ~~**Doc utilisateur (§19)** — espace Wiki par rôle/module.~~ **LIVRÉ** — espace de
   documentation utilisateur sous `docs/wiki/` : index + glossaire, 6 pages de rôles
   (Super Admin, Admin Tenant, Directeur/Manager Qualité, Auditeur+Externe, Utilisateur),
   16 pages de modules (5 méthodes + NC/CAPA/Audits + SPC/anomalies/prévision/clustering NC/
   NLP réclamations/explicabilité SHAP + Standards Hub + Packs sectoriels), et une FAQ de
   15 questions.
7. ~~**Guides par secteur (§19.1.B)** — doc utilisateur sectorielle.~~ **LIVRÉ** —
   `docs/wiki/secteurs/` : index (catalogue + activation déclarative d'un Industry Pack) +
   **14 guides sectoriels** alignés sur les 14 Industry Packs réels (industrie, santé, pharma,
   banque, IT/ITSM, agro, aéro/défense, auto, BTP, énergie, public, éducation, retail, logistique).
   Chaque guide : enjeux qualité, normes (Standards Hub), KPIs clés (cohérents avec les YAML des
   packs et CLAUDE.md §6.3), modules recommandés (routes réelles `/spc`, `/anomaly`…), connecteurs
   IoT typiques et exemple de parcours. Reste pour atteindre 100 % de l'axe : LMS/gamification
   (§19.3) et vidéos courtes.

> Note : certains items (vision 5S modèle entraîné, LSTM/Prophet, BERT/Whisper pleins)
> exigent un budget GPU/données ; ils restent branchables derrière les mêmes contrats
> (`KpiForecast`, `NcClusteringResult`…) — la dette est documentée, pas masquée.

_Document d'audit — màj à chaque palier franchi._
