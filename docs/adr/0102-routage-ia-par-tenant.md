# ADR 0102 — Chaque client choisit son fournisseur d'IA, et la souveraineté ne se dégrade jamais en silence

- **Statut** : Accepté
- **Date** : 2026-10-25
- **Owners** : @Couldevlop
- **Portée** : ai-service (`GET /v1/ai/providers`, `is_configured()` des
  fournisseurs), api-quality-engine (`airouting`, `AiGatewayClient`, migration
  V151), écran `/admin/ia`
- **Complète** : ADR 0014 (passerelle `AIProvider`), ADR 0017 (garde-fous
  LLM04), ADR 0048 (bascule de fournisseur par l'environnement), ADR 0078
  (droits par client), ADR 0086 (isolation par client en base)

## Contexte

Le fournisseur de modèle était un choix de **plateforme** : la variable
`AI_DEFAULT_PROVIDER` de l'ai-service valait pour tous les clients (ADR 0048).
Un client régulé — santé, défense, finance (§10.3, §12.2 « Souveraineté ») —
ne pouvait donc pas exiger que ses données restent sur l'infrastructure : si
la plateforme tournait sur Mistral ou Anthropic, ses prompts, ses questions en
langage naturel (NLQ), ses rapports générés et ses audits blancs partaient chez
ce fournisseur.

Or ce que l'IA lit, ce sont les **données du client** : le texte d'une NC,
les causes d'un Ishikawa, le résultat d'une requête NLQ sur ses tables, les
preuves d'un audit blanc. La souveraineté n'a de sens que si elle couvre
**tous** ces chemins, et si une panne ou une erreur de configuration ne la fait
pas tomber sans que personne le sache.

## Décision

1. **Une politique par client** (`tenant_ai_policy`, V151) : au plus une ligne
   par `tenant_id`, un fournisseur pris dans `ollama | anthropic | mistral`
   (contrainte `CHECK` en base, en plus du domaine), la date et l'auteur du
   choix. Pas de ligne = le client suit le fournisseur par défaut de la
   plateforme. La table porte un `tenant_id` : la RLS automatique
   (ADR 0086) s'y applique.

2. **Le choix appartient à l'admin du client**, par le droit
   `authz.manage` (ADR 0078) — `GET/PUT/DELETE /api/v1/admin/ai-policy`. Le
   client et l'acteur viennent du jeton, jamais du corps (§18.2 n° 2). Chaque
   épinglage et chaque retour au défaut est inscrit au journal chaîné
   (`ai.policy.pinned`, `ai.policy.released`), avec le fournisseur précédent.

3. **Le moteur impose le fournisseur du client sur chaque appel au modèle** :
   `complete` (et ses huit appelants : Ishikawa, CAPA, rapports,
   recommandation de méthode…), `askNlq` et `mockAudit`. La politique du
   client **écrase** un fournisseur que l'appelant aurait posé. Les calculs
   purs (SPC, anomalies, prévision, clustering, SHAP, NLP réclamations) ne
   portent aucun fournisseur : ils ne sortent pas de l'ai-service.

4. **Invariants de souveraineté** :
   - **Aucun repli** : l'ai-service sert exactement le fournisseur demandé
     (`self._providers.get(req.provider)`) ; s'il est indisponible, la demande
     échoue, elle ne bascule pas sur un autre fournisseur.
   - **Échec fermé** : si la politique du client ne peut pas être lue, le
     moteur **n'envoie pas** la demande — il ne la laisse pas partir vers le
     défaut de la plateforme, qui peut être externe.
   - **Pas d'épinglage vers un fournisseur non configuré** : l'ai-service
     publie l'état de ses fournisseurs (`GET /v1/ai/providers`,
     `is_configured()` = clé d'API présente pour Anthropic et Mistral). Le
     moteur refuse (422 `ai-provider-not-configured`) d'épingler un
     fournisseur que l'ai-service déclare non configuré. Si l'ai-service est
     injoignable, l'épinglage est accepté : le refus se ferait sur une
     information absente, et un appel ultérieur échouerait de toute façon
     sans repli.
   - Le défaut de l'audit blanc s'aligne sur le défaut de la plateforme au
     lieu d'être figé à `ollama`.

5. **« Sur site » veut dire Ollama** (`AiProvider.isOnPremise()`). L'écran
   l'affiche comme souverain. Cette garantie suppose que `OLLAMA_BASE_URL`
   pointe vers une instance de l'infrastructure du client ou de l'opérateur :
   c'est une responsabilité d'exploitation, que la plateforme ne peut pas
   vérifier seule.

6. **L'écran `/admin/ia`** montre l'état (souverain, externe, inconnu), le
   fournisseur effectif, chaque fournisseur avec son badge « sur site » ou
   « externe » et sa configuration, la liste des usages couverts, et avertit
   avant d'enregistrer un fournisseur externe que les demandes vont quitter
   l'infrastructure.

### Vérification

- `AiGatewayProviderRoutingTest` : avec une politique `ollama`, les trois
  appels au modèle portent `provider=ollama` ; la politique écrase l'appelant ;
  sans politique, aucun fournisseur n'est posé ; SPC n'en porte jamais ; une
  politique illisible n'envoie **aucune** requête.
- `AiPolicyAdminServiceTest`, `AiPolicyControllerTest` (droit, 400, 422, jeton
  seul source du client), `AiPolicyInfrastructureTest`.
- `RoutageIaParTenantOnPostgresTest` : une politique par client, fournisseur
  hors liste refusé par la base.
- ai-service : `test_providers_router.py`, `is_configured()` des fournisseurs.

## Conséquences

- Un client peut être servi sur site alors que la plateforme tourne sur un
  fournisseur externe, et inversement.
- Un client épinglé sur Ollama subit la latence d'Ollama (CPU : plusieurs
  dizaines de secondes par réponse) ; l'écran ne le masque pas.
- Le choix du **modèle** à l'intérieur d'un fournisseur reste un réglage de
  plateforme (ADR 0048).

## Références

CLAUDE.md §10.3, §12.2, §18.2 n° 2 et n° 4 ; OWASP LLM06 ; ADR 0014, 0017,
0048, 0078, 0086.
