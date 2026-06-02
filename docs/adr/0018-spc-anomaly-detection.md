# ADR 0018 — Détection d'anomalies SPC (règles de Nelson) dans ai-service

- **Statut** : Accepté
- **Date** : 2026-05-31
- **Owners** : Architecte principal

## Contexte

Le CLAUDE.md positionne l'IA comme « épine dorsale » (§12) et promet, pour DMAIC,
des **cartes de contrôle SPC** avec « détection d'anomalies … en plus des règles
WECO/Nelson classiques » (§3.4, §12.1). L'audit `docs/AUDIT-stub-vs-reel.md` (P2 #6)
constatait que **toute l'IA ML/prédictive annoncée était absente** (0 import). Il
fallait livrer une première capacité analytique **réelle et déterministe**, sans
dépendre d'un entraînement de modèle ni de GPU.

## Décision

Implémenter la **détection d'anomalies SPC univariée (carte des individus, I-chart)**
dans `ai-service`, en architecture hexagonale stricte (contrats import-linter) :

- **Domaine** (`domain/model/spc.py`, `domain/service/spc_rules.py`) : value objects
  immuables + fonctions pures. Limites de contrôle estimées la manière SPC-correcte
  pour les individus (σ = MR̄ / d2, d2=1.128 pour n=2 — robuste aux points hors
  contrôle), ou fournies par l'appelant (baseline connue). Les **8 règles de Nelson**
  (surensemble des règles Western Electric) sont implémentées avec NumPy.
- **Application** (`application/usecase/spc_detect.py`) : `SpcDetectUseCase` — couche
  mince portant le tenant (auditabilité) et le logging.
- **Présentation** (`presentation/routers/spc_router.py`, `schemas/spc.py`) :
  `POST /v1/ai/spc/analyze`, authentifié via `current_user` (JWT/X-Dev-Claims), tenant
  issu du `UserContext` jamais du body. Validation Pydantic (baseline cohérente,
  bornes de taille). Réponse : limites + violations (règle, fenêtre de points,
  sévérité, description FR explicable).
- **Dépendance** : ajout de `numpy>=1.26` aux dépendances runtime (autorisé par les
  contrats — NumPy n'est pas un framework web/IO).

## Justification

- **Déterministe & explicable** (§12.3) : chaque alerte cite la règle et les points
  fautifs — aucune boîte noire, aucune hallucination possible. Idéal comme socle
  avant d'ajouter du ML non-supervisé.
- **Pur domaine** : la logique statistique est testable sans FastAPI ni I/O.
- **Sans entraînement** : utilisable immédiatement, contrairement à LSTM/autoencoder.

## Alternatives écartées

- **Isolation Forest / autoencoder d'emblée** : nécessitent données + tuning ; gardés
  pour un lot ultérieur (anomalies multivariées, §12.1).
- **Calcul SPC côté engine (Java)** : le ML/analytique vit dans ai-service (§10.2,
  §12.2) ; garde l'engine fin et réutilise les garde-fous/observabilité Python.

## Conséquences

- ✅ Première capacité « IA analytique » réelle ; P2 #6 de l'audit entamé.
- ✅ 19 tests (13 domaine couvrant chaque règle + bords ; 6 routeur). import-linter
  « 2 kept, 0 broken ». Régression router existante verte (7/7).
- ⚠ **Non encore branché côté engine** : pas de controller `api-quality-engine` ni de
  méthode `AiGatewayClient` pour SPC, ni de dérivation auto depuis `kpi_measurements`
  / télémétrie IoT. Prochain lot (réutilisera le chemin AiGateway + garde-fous LLM04
  d'ADR 0017, et pourra ouvrir une CAPA comme la chaîne IoT d'ADR 0016).
- ⚠ Univarié uniquement (I-chart). Cartes X̄-R/EWMA/CUSUM et anomalies multivariées
  restent à faire.

## Tests d'invariant

- `tests/domain/test_spc_rules.py` : une assertion par règle de Nelson + séries plates/vides.
- `tests/presentation/test_spc_router.py` : auth requise, détection, estimation des limites, validations 422.
- `python -m importlinter.cli lint` : les contrats hexagonaux restent verts.

## Références

CLAUDE.md §3.4, §12.1, §12.3 ; [ADR 0008](./0008-ai-service-architecture.md) (archi ai-service) ;
[ADR 0017](./0017-ai-guardrails-llm-dos.md) (garde-fous, futur câblage engine).
