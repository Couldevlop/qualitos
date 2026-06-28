# ADR 0045 — Sentiment réclamations : DistilCamemBERT FR prouvé, modèle configurable, golden test opt-in

- **Statut** : Accepté
- **Date** : 2026-06-28
- **Owners** : Architecte principal

## Contexte

Le backend de sentiment **BERT** des réclamations (ADR 0031) était câblé mais
jamais **exécuté** : ses dépendances lourdes (`torch`/`transformers`) vivent dans
l'extra `ml`, absent de la CI, et le défaut codé était le modèle multilingue
`nlptown/bert-base-multilingual-uncased-sentiment` (~700 Mo). QualitOS s'adressant
en priorité à un public francophone, et la contrainte disque locale étant réelle
(C: saturé), il fallait : (a) **prouver** que « prendre un modèle ouvert » fonctionne
réellement bout-en-bout, (b) privilégier un modèle **français** plus léger, (c)
figer le comportement par un test de référence sans casser le gate de couverture CI.

## Décision

1. **Modèle français par défaut, configurable** : `sentiment_bert.analyze` charge
   `cmarkea/distilcamembert-base-sentiment` (distillé, ~270 Mo, FR), surchargeable
   par le paramètre `model_name` **ou** la variable d'environnement
   `COMPLAINT_BERT_MODEL` (souveraineté / on-prem / autre langue).
2. **Mapping de labels robuste** vers la polarité ∈ [-1, 1] du contrat
   `ComplaintAnalysis` : familles « étoiles » (`1 star`…`5 stars`), index brut
   (`LABEL_0`…`LABEL_4` → étoiles 1..5) et binaire (POSITIVE/NEGATIVE pondéré).
   La **classification par catégorie** et la **criticité** restent celles du backend
   lexical (seul le sentiment passe au neuronal).
3. **Golden test opt-in** (`tests/domain/test_sentiment_bert_golden.py`) :
   `skipif` si `torch`+`transformers` absents → **skippé en CI**, exécuté en local.
   Il prouve notamment que le neuronal **rattrape un cas que le lexical rate**
   (« facture erronée » : hors lexique mais sémantiquement négatif).

Périmètre volontairement limité à **ai-service** (le relais engine→UI du choix de
backend reste hors scope, cf. ADR 0031 « à suivre »).

## Justification

- **Preuve réelle, pas un scaffold** : modèle ouvert chargé et exécuté sur des
  réclamations FR (négatif/positif/critique corrects ; cas facturation rattrapé là
  où le lexical donne ~0). « Prendre un modèle ouvert » est ainsi démontré, pas
  supposé.
- **FR + léger** : DistilCamemBERT est spécialisé français et 2,5× plus léger que le
  multilingue — meilleure pertinence et empreinte disque/latence moindre.
- **Honnêteté du défaut** : `backend="lexical"` (NumPy pur) reste le défaut réel,
  intact ; aucune lib lourde n'entre dans la CI ni l'image par défaut.

## Conséquences

- ✅ Sentiment réclamations FR neuronal **prouvé** en local (DistilCamemBERT FR).
- ✅ Modèle configurable par env (`COMPLAINT_BERT_MODEL`) — industry/lang-agnostic.
- ✅ Golden test fige le comportement (skippé en CI, exécuté avec l'extra `ml`).
- ⚠ **Tension couverture assumée** : un chemin qui charge un modèle lourd **ne peut
  pas** être couvert par la CI (libs exclues) ; le golden test le couvre en local,
  le gate `fail_under` CI reste tenu par les chemins légers (cf. ADR 0031).
- ⚠ Le golden test télécharge le modèle au 1er run (réseau/HF cache) ; il est
  réservé aux environnements disposant de l'extra `ml`.

## Tests d'invariant

- `tests/domain/test_sentiment_bert_golden.py` : négatif/positif, criticité,
  supériorité sémantique sur le lexical, stabilité du contrat (skip si lib absente).
- `tests/domain/test_ml_backends.py` : message d'erreur si `bert` sélectionné sans
  la lib (inchangé), défaut lexical inchangé.
- `lint-imports` : `transformers`/`torch` jamais importés au niveau module (import
  paresseux dans `analyze`).

## Références

CLAUDE.md §4.9, §12.1, §12.3 ; [ADR 0031](./0031-pluggable-ml-backends-opt-in.md)
(backends ML opt-in) ; [ADR 0025](./0025-complaint-nlp-lexical.md) (défaut lexical).
Modèle : `cmarkea/distilcamembert-base-sentiment` (Hugging Face, licence ouverte).
