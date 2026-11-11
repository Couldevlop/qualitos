# API et contrats

[← Retour à l'espace technique](README.md)

Quatre contrats sortent de la plateforme : l'API REST, les webhooks sortants, les clés
d'API avec leurs quotas, et le relais d'événements. Cette page dit où chacun est décrit
et ce qu'il garantit.

---

## 1. API REST

**Préfixe.** `/api/v1/…` sur `api-core`, `api-quality-engine` et `api-iot-hub`.

**Description.** Chaque service Spring expose sa description OpenAPI 3 générée par
springdoc :

| Service | Description | Interface |
| --- | --- | --- |
| `api-core` (8081) | `/v3/api-docs` | `/swagger-ui.html` |
| `api-quality-engine` (8082) | `/v3/api-docs` | `/swagger-ui.html` |
| `api-iot-hub` (8083) | `/v3/api-docs` | `/swagger-ui.html` |

`springdoc.use-fqn: true` est posé volontairement : sans lui, deux enregistrements
homonymes venant de modules différents — un `ActivateRequest` côté packs sectoriels et un
autre côté modules du client, un `CreateRequest` côté non-conformité et un autre côté
notifications — s'écrasent dans `#/components/schemas`, et la description devient
trompeuse. Le nom qualifié est plus long à lire ; il est juste.

**Aucun fichier `openapi.yaml` n'est versionné dans le dépôt.** La description est
générée à l'exécution depuis les contrôleurs et les annotations. C'est un choix qui
tient tant que la description n'est pas un engagement contractuel envers un tiers ; le
jour où elle le devient, il faudra la figer et la comparer en intégration continue.

**Authentification.** Jeton OIDC émis par Keycloak, validé par la clé publique du realm
(`jwk-set-uri`). Le client vient du jeton, jamais du corps de la requête, et c'est la
première règle non négociable du projet (§18.2 point 2, ADR 0001). L'autorisation se
décide **avant** que le corps ne soit lu (ADR 0065) : une requête refusée ne doit pas
avoir eu l'occasion de faire travailler le service.

**L'ai-service (8085)** n'est pas appelé par le navigateur. Il est joint par le moteur de
qualité, avec un jeton de service, et il n'accepte que ce porteur (ADR 0014, 0021, 0048).
Ses sondes sont `/healthz` et `/readyz` — pas `/health`, qui rend 404.

---

## 2. Webhooks sortants

**Signature.** `HMAC-SHA256(secret, timestamp + "." + payload)`, rendu au format
`sha256=<hex>` et vérifié en temps constant. L'horodatage entre **dans** le tampon
signé : le consommateur peut donc rejeter une requête rejouée plus tard. C'est ce que
`HmacSigner` implémente, et l'anti-rejeu n'est utile que si le consommateur applique
réellement sa fenêtre.

**Événements souscriptibles**, tels que les porte `EventType` :

| Famille | Événements |
| --- | --- |
| PDCA | `pdca.cycle.created`, `pdca.cycle.advanced`, `pdca.cycle.completed`, `pdca.cycle.cancelled` |
| CAPA | `capa.case.opened`, `capa.case.resolved`, `capa.case.closed`, `capa.effectiveness.verified` |
| Audits | `audit.finding.raised`, `audit.plan.completed` |
| 5S, DMAIC, Ishikawa | `fives.audit.completed`, `dmaic.project.completed`, `ishikawa.diagram.validated` |
| Cercles | `circles.proposal.approved`, `circles.proposal.measured` |
| Documents | `documents.version.published`, `documents.version.acknowledged` |
| Normes | `standards.adoption.certified`, `standards.adoption.expired` |
| Indicateurs | `kpi.threshold.breached` |
| Vérification | `webhook.test.ping` |

**Livraison.** `WebhookDispatcher` avec état de livraison (`DeliveryStatus`) et reprise
en attente croissante (`RetryBackoff`). Une souscription a son propre statut
(`SubscriptionStatus`) : une destination durablement muette se suspend au lieu de faire
grossir une file indéfiniment.

**Administration.** Écran `/admin/webhooks` côté client.

---

## 3. Clés d'API et quotas

Deux modules séparés, et la séparation est volontaire :

- `apikeys` — émission, révocation et portée des clés, écran `/admin/api-keys` ;
- `ratelimit` — débit autorisé par client, écran `/admin/quotas`.

Les garde-fous du chemin de modèle sont **un troisième mécanisme**, distinct des deux
précédents (ADR 0017) : débit par minute, quota journalier, disjoncteur et borne de taille
de requête, cloisonnés par client, appliqués avant tout départ réseau. Les rejets
ressortent en 429, 413 et 503 — jamais en réponse inventée.

---

## 4. Événements internes

Le relais Kafka existe et il est **désactivé par défaut** (`qualitos.kafka.enabled`,
`QUALITOS_KAFKA_ENABLED: "false"` dans le chart). Son principe : la table `audit_events`
du client sert de file d'attente de sortie — elle porte déjà une séquence sans trou par
client, donc elle sait dire ce qui n'a pas encore été publié. Rien n'est écrit deux fois,
et une plateforme sans Kafka fonctionne à l'identique.

**Aucun fichier AsyncAPI n'est versionné**, alors que `CLAUDE.md` §19.1 l'annonce. Les
événements publiés sont ceux de `EventType` ci-dessus ; c'est le contrat réel, et il est
dans le code. L'écart est connu et assumé tant que le relais reste désactivé — il devra
être comblé avant d'ouvrir le flux à un consommateur tiers.

---

## 5. Ce qui n'existe pas, et qu'il ne faut pas promettre

| Annoncé ailleurs | État réel |
| --- | --- |
| Service `api-connectors` dédié (ADR 0007) | **Pas dans la construction** : le module est commenté dans `pom.xml` et vit sur une branche. Les connecteurs livrés sont des paquets du moteur (`commconnector`, `erpconnector`, `ehrconnector`, `itsm`) et de l'IoT Hub |
| Spring Cloud Gateway en façade (`CLAUDE.md` §10.1) | L'entrée est l'**ingress nginx**, avec une règle par surface exposée. Toute nouvelle surface de `api-core` doit être routée explicitement, sinon elle répond 404 en préproduction alors qu'elle fonctionne en local |
| GraphQL (`CLAUDE.md` §6.4) | Absent. Les tableaux de bord consomment du REST |
