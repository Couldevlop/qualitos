# ADR 0014 — Intégration de la passerelle IA (ai-service) au moteur qualité

- **Statut** : Accepté
- **Date** : 2026-05-27
- **Owners** : @Couldevlop

## Contexte

L'`ai-service` (passerelle IA FastAPI : completion, RAG, NLQ — redaction PII + bouclier
anti-injection) tournait mais n'était **branché à aucune fonctionnalité visible**. On veut
exposer une première capacité LLM réelle dans l'IHM : la **génération assistée de brouillons
de documents normatifs** (CLAUDE.md §8.8), via Ollama local.

Contraintes constatées :
- `ai-service` **n'a pas de CORS** → appel direct navigateur→ai-service bloqué.
- Son JWT exige l'audience **`qualitos-ai`** ; le token du SPA porte `qualitos-web`.
- Le modèle Ollama par défaut (`llama3.1:8b`) dépasse souvent le timeout CPU (30 s).
- CLAUDE.md §18.2 règle 4 : *aucun appel LLM externe sans passer par la passerelle*
  (l'ai-service EST cette passerelle : PII + injection + audit).

## Décision

**Le SPA ne parle jamais directement à ai-service.** Chemin : `SPA → api-quality-engine → ai-service`
(cohérent avec les features ai-incidents/ai-conformity déjà sur 8082).

1. **api-quality-engine** porte un client serveur-à-serveur `AiGatewayClient` (Spring `RestClient`,
   timeout lecture 60 s > 30 s LLM). Le SPA appelle `POST /api/v1/standards/{id}/document-templates/{templateId}/ai-draft`
   (JWT utilisateur validé normalement) ; le moteur relaie vers `ai-service /v1/ai/complete`.
2. **Auth interne** :
   - *Dev* : `ai-service` avec `QOS_DEV_AUTH=1`, le moteur envoie l'en-tête `X-Dev-Claims`
     `{"sub","tid","roles"}` — le `tid` provient du `TenantContext` (issu du JWT, **jamais du body**, §18.2 règle 2).
   - *Prod* : jeton OIDC **client_credentials** d'audience `qualitos-ai` (client de service dédié) ;
     `QOS_DEV_AUTH` désactivé. (À implémenter avant prod.)
3. **Modèle Ollama configurable** par env (`OLLAMA_MODEL`, `OLLAMA_BASE_URL`) ; dev = `llama3.2:1b`
   (tient dans le timeout CPU). Génération bornée à ~450 tokens.
4. **Humain dans la boucle** : le brouillon est un point de départ à valider (aucun document
   publié sans revue + signature, §8.8).

## Justification

- Le proxy par le moteur **réutilise l'auth JWT existante** du SPA, évite d'ouvrir CORS sur
  ai-service et de gérer l'audience côté navigateur, et respecte la règle « tout LLM via la passerelle ».
- `X-Dev-Claims` est l'échappatoire **dev déjà prévue** par ai-service (`QOS_DEV_AUTH`), sans
  minter de jeton de service en local. Strictement dev (documenté, désactivé en prod).
- Petit modèle en dev = latence acceptable pour une démo ; le choix reste configurable par env.

## Conséquences

- ✅ Première capacité LLM réelle visible dans l'IHM (brouillon de document normatif).
- ✅ Pas de CORS ni de secret LLM côté navigateur ; PII/injection/audit appliqués par la passerelle.
- ⚠ `QOS_DEV_AUTH=1` (compose dev) désactive la validation JWT d'ai-service : **dev uniquement**.
  Le flux prod (client_credentials aud `qualitos-ai`) reste à implémenter — bloquant avant mise en prod.
- ⚠ Latence CPU : Ollama local est lent ; prévoir GPU ou provider distant (Anthropic/Mistral via
  la passerelle) en environnement chargé.
- ℹ Réutilisable : `AiGatewayClient` servira d'autres usages (suggestion de causes Ishikawa, NLQ…).

## Tests d'invariant

- `StandardsControllerTest` : `ai-draft` → 200 ; passerelle indisponible → **502**.
- Le `tid` envoyé à ai-service vient de `TenantContext` (jamais d'un paramètre client).
- E2E (dev) : `POST .../ai-draft` retourne un brouillon généré par Ollama (`provider=ollama`).

## Références

- CLAUDE.md §8.8 (génération IA des documents), §12.2 (architecture IA), §18.2 règles 2 & 4.
- ADR [0013](./0013-ai-service-distroless-python-alignment.md) (image ai-service).
- `apps/api-quality-engine/.../aigateway/`, `apps/ai-service/presentation/{security,container}.py`.
