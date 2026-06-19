# Feuille de route vers 100 % — QualitOS

> Établie 2026-06-19, base **~87 %** (cf. `AUDIT-realisation-2026-06-15.md`).
> Principe : on attaque d'abord ce qui a **le plus de valeur** et qui est **indépendant** (parallélisable
> sans collision), on garde pour la fin ce qui est **bloqué hors-code** (GPU / infra réelle / contenu humain).

## Lecture rapide

- Le **plafond atteignable par le code** est ~**95 %**. Les 5 % restants exigent des **prérequis externes**
  (carte GPU, cluster managé, réseau Fabric, datasets, budget pentest) — listés en **Niveau 4**.
- Chaque **vague** = un lot d'agents dont les périmètres de fichiers sont **disjoints** → exécutables en
  parallèle. On intègre en série (revue anti-stub + CI verte) entre deux vagues.
- **Convention anti-collision** (leçon des lots précédents) : numéros d'ADR **réservés à l'avance** par item ;
  `docs/adr/README.md`, `docs/AUDIT-*`, et les XLF i18n sont des fichiers **partagés** → résolus à l'intégration,
  pas par les agents.

---

## Niveau 1 — Différenciateurs commerciaux (ce qui nous distingue de MasterControl/ETQ)

> Le plus fort levier de valeur. Tout est **codable maintenant** (dépendances déjà en place : AiGatewayClient/Ollama,
> event-sourcing Kafka socle, ECharts/AG-Grid).

| # | Item | Périmètre (propriété fichiers) | Dépend de | ADR |
|---|------|-------------------------------|-----------|-----|
| 1.1 | **Standards Hub — génération doc IA** (onglet 3 §8.8) : Manuel/Politique/Procédures pré-remplis par contexte tenant, workflow de validation humaine | `apps/api-quality-engine/.../standards` + `apps/ai-service` (prompt structuré) | AiGatewayClient (✅) | 0032 |
| 1.2 | **Standards Hub — audit blanc IA avancé** (onglet 7 §8.4) : 30-100 questions ciblées clauses à risque, gap analysis + plan de remédiation auto | `apps/api-quality-engine/.../standards` + RAG ai-service | RAG Qdrant (✅) | 0033 |
| 1.3 | **Dashboards interactifs premium** (§7.3) : cross-filtering, drill-down infini, annotations collaboratives | `apps/web/.../features/dashboard*` | ECharts (✅) | 0034 |
| 1.4 | **Dashboards time-travel** (§7.3) : « état du dashboard au 15 mars » | `apps/web` + endpoint replay engine | event-sourcing Kafka socle (✅ OFF) | 0035 |

**Indépendance** : 1.1+1.2 (Standards, backend) ∥ 1.3+1.4 (Dashboards, web). Deux agents max par vague pour limiter
les conflits XLF/routing web.

---

## Niveau 2 — Complétude fonctionnelle (combler les manques visibles de la vision)

> Codable maintenant. Modules largement **disjoints** au niveau répertoire.

| # | Item | Périmètre | Dépend de | ADR |
|---|------|-----------|-----------|-----|
| 2.1 | **IoT — connecteur DICOM** (§9.4) : dernier protocole santé, même patron que Modbus/LoRaWAN/Sparkplug | `apps/api-iot-hub/.../infrastructure/dicom` | patron connecteur (✅) | 0036 |
| 2.2 | **IoT — runtime Edge long-running** (§9.5) : souscription MQTT → orchestrateur d'inférence (mosquitto local), boucle de service | `infra/edge/inference` (runtime) | composant Edge (✅ 0030), broker compose (✅) | 0037 |
| 2.3 | **Industry Packs — profondeur sectorielle** (§5) : KPIs/normes/templates/glossaire pour 3-4 secteurs prioritaires | `libs/industry-packs` (YAML déclaratif) | loader (✅) | — (déclaratif) |
| 2.4 | **Formation & LMS-light** (§19.3) : parcours par rôle/secteur, quiz, gamification (badges Yellow→Black Belt), simulateurs | `apps/web/.../features/learning` + module engine training | feature learning (✅) | 0038 |

**Indépendance** : 2.1 ∥ 2.2 (tous deux IoT mais répertoires distincts : `api-iot-hub` vs `infra/edge`) ∥ 2.3 (libs) ∥ 2.4 (web+engine training). 4 agents parallèles possibles.

---

## Niveau 3 — Durcissement & qualité (prod-readiness, §11/§14)

> Codable maintenant. Touche surtout **CI/infra/tests**, donc indépendant des features → idéal en vague de fond
> pendant les niveaux 1-2.

| # | Item | Périmètre | Dépend de | ADR |
|---|------|-----------|-----------|-----|
| 3.1 | **DAST automatisé** (§14.2) : OWASP ZAP en CI contre la stack éphémère | `.github/workflows`, `infra` | stack compose (✅) | 0039 |
| 3.2 | **Tests de charge** (§14.3) : k6/Gatling, objectifs p95<300ms, en job CI dédié non-bloquant | `tests/perf`, `.github/workflows` | endpoints (✅) | — |
| 3.3 | **Chaos engineering** (§14.3) : Litmus/Chaos Mesh, scénarios de base | `infra/k8s/chaos` | Helm (✅) | — |
| 3.4 | **Couverture tests composants front** (§15) : monter la couverture Karma des features récentes | `apps/web/**/*.spec.ts` | — | — |

**Indépendance** : 3.1/3.2/3.3 (CI/infra, fichiers distincts) ∥ 3.4 (web specs). Parallélisables, faible risque de collision.

---

## Niveau 4 — Bloqués hors-code (prérequis à fournir — ce sont les ~5 % finaux)

> **Non livrable par des agents sans tricher (stub).** Chaque ligne attend une ressource externe. À traiter
> quand le prérequis est disponible ; le **code d'accueil est déjà en place** (chemins ONNX/opt-in, profils Fabric/TLS).

| Item | Prérequis bloquant | Code d'accueil déjà prêt ? |
|------|--------------------|----------------------------|
| **Vision 5S — modèle YOLOv8 entraîné** | GPU + dataset 5S annoté | ✅ chemin ONNX réel (ADR 0029) |
| **LLM lourds pleins** (BERT/Whisper/LSTM/Prophet/HDBSCAN) | GPU | ✅ backends opt-in (ADR 0031) |
| **Ancrage Fabric Phase B** | réseau Hyperledger Fabric réel | ✅ `@Profile("fabric")` + service |
| **Cluster TimescaleDB de prod** | instance/cluster managé | ✅ migration V2 gardée |
| **Crypto bc-fips post-quantique** | release upstream BC-FIPS avec PQ | ✅ crypto-agilité en place |
| **Pentest manuel + vidéos formation** | budget humain / prestataire | — |

---

## Séquencement proposé

```
Vague A (N1) : 1.1+1.2 Standards IA   ∥  1.3+1.4 Dashboards interactifs
                                   ↓ (intégration + CI verte)
Vague B (N2) : 2.1 DICOM ∥ 2.2 Edge runtime ∥ 2.3 Packs ∥ 2.4 LMS
                                   ↓
Vague C (N3) : 3.1 DAST ∥ 3.2 perf ∥ 3.3 chaos ∥ 3.4 couverture front
                                   ↓
   → plafond code ~95 %.  Niveau 4 = au fil des prérequis externes (→ 100 %).
```

Gain estimé : Vague A ≈ +3 pts (différenciateurs), Vague B ≈ +3 pts, Vague C ≈ +2 pts → **~95 %**.
Le solde (~5 %) bascule à 100 % uniquement avec les enablers du Niveau 4.

## Définition de « fait » (rappel, §20)

Chaque item : code complet et fonctionnel (zéro stub), Clean Architecture, OWASP (tenant via JWT, `@PreAuthorize`,
validation), tests + couverture ≥ 85 %, CI verte, ADR si décision structurante, **sans signature Claude**.
