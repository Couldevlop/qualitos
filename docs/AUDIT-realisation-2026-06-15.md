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

## Taux de réalisation : ~95 % (≈ 99 % rapporté à un MVP commercialisable)

> Màj 2026-06-22 ter (3ᵉ lot « 100 % faisable en CI » intégré sur `develop`, vérifié bout-en-bout :
> **Marketplace de packs normatifs** §8.11 (ADR 0041, commit `a0499a6`, cycle soumission→modération→publication→
> installation tenant ; 77 tests backend + 722 front), **Chaos Engineering** §14 (ADR 0042, `d231d57`, manifests
> Chaos Mesh PodChaos/NetworkChaos/StressChaos/IOChaos + runbook + CI manuelle, 6 manifests valides), **Export PDF
> dashboard signé** ML-DSA + ancré + QR §7.3/§7.4 (ADR 0043, `e7194e2`, round-trip signature testé, 48 tests
> backend 95,4 % + 691 front). Le reliquat « faisable en CI » est désormais épuisé ; ne reste que le **plafond
> dur** (GPU/données/infra réelle : YOLOv8 5S, BERT/Whisper/LSTM pleins, runtime Edge MQTT, bc-fips PQ) + pentest manuel.
> Màj 2026-06-22 bis (2ᵉ lot prod-readiness intégré sur `develop`, vérifié : **DAST OWASP ZAP en CI**
> ADR 0039 (`c76ca85`, run ZAP réel 59 PASS/0 FAIL), **perf k6 + budgets SLO bloquants en CI** ADR 0040
> (`79b3c4b`, gate p95<300/p99<800 fonctionnel), **couverture front +14 specs cœur** (`5b9d811`, suite Karma
> 683 verte). Reste pour le « 100 % faisable en CI » : marketplace normatif (§8.11), chaos engineering (§14),
> export PDF dashboard, pentest manuel. Le reliquat dur reste GPU/données/infra réelle (cf. lignes IA/IoT).
> Màj 2026-06-22 (lot « vers 100 % » — 3 agents parallèles intégrés sur `develop`, vérifiés bout-en-bout :
> **Dashboard builder drag&drop avancé** gridster2 (ADR 0038, commit `510b54a`, 69 specs builder + 531 suite front),
> **Génération doc IA multi-documents** Standards Hub §8.8 (ADR 0037, commit `d880cb9`, dossier en lot + finalisation
> signée/ancrée, 408 tests standards + context-loads), **Academy LMS-light + gamification** §19.3 (ADR 0036, commit
> `c0b8d95`, e-learning + quiz notés + certificats ML-DSA/blockchain + SCORM/xAPI, `mvn verify` 4035 tests + gate JaCoCo).
> Reliquat CI restant : DAST/perf en CI (§11/§14), couverture tests composants front (§15), marketplace normatif (§8.11).
> Màj 2026-06-19 (lot GPU/scaffold intégré, CI verte `3efbd94` : Vision ONNX vrai chemin ADR 0029,
> Edge inference store-and-forward ADR 0030, backends ML lourds opt-in Prophet/LSTM/HDBSCAN/BERT/Whisper ADR 0031).
> Màj 2026-06-16 (après 3 lots de chantiers parallèles : IA, IoT, Dashboards, Formation, Doc).
> Pondéré par l'importance des axes de la vision CLAUDE.md. Le reliquat est essentiellement
> **GPU / infra réelle / contenu externe** (non livrable en CI sans stub — donc non fait par principe).

| Axe | Réalisation | Reste à faire |
|---|---|---|
| 5 méthodes (§3) | ~95 % | — (complet + golden seed) |
| Modules transverses (§4) | ~90 % | profondeur de quelques workflows |
| Standards Hub (§8) | ~98 % | ~~audit blanc IA avancé~~ (ADR 0033), ~~génération doc IA~~ (ADR 0037), ~~marketplace~~ (ADR 0041, cycle complet, livré 2026-06-22) — complet |
| Industry Packs (§5) | ~85 % | profondeur sectorielle |
| Pilier confiance (§11) | ~95 % | bc-fips (bloqué upstream) |
| Sécurité (§11) | ~90 % | ~~DAST~~ (OWASP ZAP en CI, ADR 0039, 2026-06-22) ; reste : pentest manuel |
| Prod-readiness (§14) | ~95 % | ~~perf~~ (k6 + SLO, ADR 0040), ~~chaos engineering~~ (Chaos Mesh, ADR 0042, 2026-06-22) ; reste : exécution sur cluster réel |
| Frontend (§15) | ~90 % | couverture composants renforcée (+14 specs cœur, suite 683 verte, 2026-06-22) ; reste : seuil global ≥85 % généralisé |
| **IA (§12)** | **~85 %** | reste : **modèles entraînés réels** (YOLOv8 5S, GPU pour BERT/Whisper/LSTM/Prophet/HDBSCAN) — la **plomberie est complète** : Vision chemin ONNX réel exercé (ADR 0029), backends lourds **opt-in câblés** import paresseux/extra `ml` (ADR 0031, défaut léger réel intact, 422/501 si absent) ; anomalies/forecast/clustering DBSCAN/SHAP/NLP lexical pleinement livrés |
| **IoT (§9)** | **~80 %** | reste : **runtime Edge long-running** (souscription MQTT→orchestrateur) + modèle ONNX réel, vrai cluster TimescaleDB, DICOM — livrés : Digital Twin/Shadow, LoRaWAN, Sparkplug B, Modbus, rollups + continuous aggregate TimescaleDB, **composant inférence Edge store-and-forward + ONNX/repli (ADR 0030, 37 tests)** |
| **Dashboards (§7)** | **~95 %** | livrés : **NLQ→graphe, Mode TV, Storyboards IA, cross-filtering + drill-down + annotations collaboratives persistées (ADR 0034), time-travel as-of réel (ADR 0035), builder drag&drop avancé gridster2 + palette + 9 widgets + config (ADR 0038), export PDF signé ML-DSA + ancré + QR (ADR 0043, 2026-06-22)** — complet |
| **Doc & formation (§19)** | **~88 %** | livrés : espace Wiki utilisateur par rôle/module **et par secteur** (`docs/wiki/`, 14 guides), **Academy LMS-light + gamification : cours/quiz notés/badges/ceintures Yellow→Black + certificats ML-DSA/blockchain + SCORM/xAPI (ADR 0036, 2026-06-22)** — reste : vidéos courtes, simulateurs Ishikawa |

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
