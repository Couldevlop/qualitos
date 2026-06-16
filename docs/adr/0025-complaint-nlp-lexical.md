# ADR 0025 — NLP des réclamations : sentiment lexical + classification (sans dépendance lourde)

- **Statut** : Accepté
- **Date** : 2026-06-16
- **Owners** : Architecte principal

## Contexte

Le §4.9 (Customer Complaints & Voice of Customer) promet « NLP de classification, sentiment
analysis, détection de réclamations critiques » ; la matrice §12.1 cite BERT multilingue +
Whisper. L'audit `docs/AUDIT-stub-vs-reel.md` marquait « NLP audits / sentiment » ABSENT.
Il fallait une capacité NLP réelle et utilisable, sans introduire torch/transformers (BERT)
ni un modèle de transcription audio (Whisper) — incompatibles avec la couche domaine NumPy/
pur, l'image distroless et la surface CVE.

## Décision

Implémenter un NLP **lexical et par termes-graines** en Python pur (`domain/service/
complaint_nlp.py`), FR + EN, déterministe et explicable :

- **Sentiment** : score de polarité ∈ [-1, 1] depuis un lexique pondéré, avec gestion de la
  **négation** (inverse le mot suivant) et des **intensifieurs** ; étiquette negative/neutral/
  positive.
- **Classification** : score par catégorie via des **termes-graines** (taxonomie par défaut
  produit/livraison/service/facturation/sécurité, **surchargeable** par le client) ; la
  catégorie au plus fort recouvrement gagne (sinon « autre »).
- **Criticité** : réclamation critique si sentiment très négatif OU marqueur d'urgence/gravité
  (sécurité, juridique, rappel…).
- Câblé bout-en-bout : `POST /v1/ai/complaints/analyze` (tenant via JWT) → engine
  `complaintnlp/{ComplaintNlpController,Service,Dto}` (`POST /api/v1/ai/complaints/analyze`,
  sous `/ai/`, distinct du module CRUD `complaints`) + `AiGatewayClient.analyzeComplaints`
  (garde-fou LLM04 op « complaint-nlp ») → UI `/complaints-nlp` (sentiment, catégorie,
  criticité par réclamation, critiques en tête).

## Justification

- **Sans dépendance lourde** : même ligne que SPC/anomalies/forecast/clustering — la couche
  domaine reste légère, auditable, on-prem sans GPU. Le sentiment lexical et la classification
  par mots-clés sont des baselines NLP reconnues, transparentes (chaque décision est traçable
  au mot).
- **Explicable & déterministe** (§12.3) : pas de boîte noire ; résultats reproductibles.
- **Surchargeable** : la taxonomie de catégories est fournie par le tenant (industry-agnostic,
  §18.2 #9).

## Alternatives écartées

- **BERT (transformers/torch)** : meilleur sur le sémantique fin et le multilingue, mais
  dépendance lourde + GPU ; pourra se brancher derrière le **même contrat** `ComplaintAnalysis`
  quand un budget existera.
- **Whisper (transcription audio)** : hors périmètre (nécessite le modèle audio) — l'entrée
  reste textuelle en v1.
- **Calcul côté engine (Java)** : l'analytique/NLP vit dans `ai-service` (§10.2) ; l'engine
  relaie.

## Conséquences

- ✅ NLP réclamations réel (sentiment + catégorie + criticité) ; ligne d'audit ABSENT → RÉEL.
- ✅ Bout-en-bout ai-service → engine → UI `/complaints-nlp`.
- ✅ Tests : `test_complaint_nlp.py` (sentiment, négation, intensifieur, catégories, custom,
  criticité, validations, déterminisme) + router ; engine `ComplaintNlpServiceTest`/
  `ComplaintNlpControllerTest`/`AiGatewayClientTest` ; UI service spec. pytest 240 (cov 91,2%),
  engine vert.
- ⚠ Lexique compact (FR/EN) — extensible ; pas de sémantique profonde ni de langues hors FR/EN
  en v1 (BERT multilingue = lot ultérieur). Pas de transcription audio (Whisper).

## Tests d'invariant

- `tests/domain/test_complaint_nlp.py` ; `tests/presentation/test_complaint_router.py`.
- Engine : `ComplaintNlpServiceTest`, `ComplaintNlpControllerTest`, cas `analyzeComplaints`
  dans `AiGatewayClientTest`.
- `lint-imports` : contrats hexagonaux verts.

## Références

CLAUDE.md §4.9, §12.1, §12.3 ; [ADR 0022](./0022-unsupervised-anomaly-detection.md),
[ADR 0024](./0024-shap-explainability.md) (même pattern NumPy pur derrière contrat stable).
