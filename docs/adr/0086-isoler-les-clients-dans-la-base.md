# ADR 0086 — Isoler les clients dans la base (Row-Level Security)

- **Statut** : Accepté
- **Date** : 2026-10-10
- **Owners** : @Couldevlop
- **Portée** : `api-quality-engine` (paquet `rls`, `application.yml`), `ai-service`
  (exécuteur NLQ), `deploy.sh`, valeurs du chart
- **S'inscrit dans** : CLAUDE.md §10.3 (« les violations sont impossibles côté
  code et côté base »), §18.2.2

## Contexte

L'isolation entre clients du moteur qualité reposait sur le seul code : chaque
dépôt filtre par `tenant_id` lu dans le jeton. La spécification promet aussi une
barrière dans la base. Elle n'existait pas : aucune politique RLS, et
l'application se connectait sous le rôle `qualitos`, **superutilisateur**, que
PostgreSQL exempte de toute politique. Un filtre oublié dans une requête, sur
l'une des cent tables à client, exposait les données d'un autre client sans
qu'aucune couche ne s'y oppose. Même chose pour le SQL généré par l'IA (NLQ), que
seul un validateur syntaxique bornait.

## Décisions

### 1. Deux rôles

Les migrations gardent le rôle propriétaire (`DB_USER`). L'application travaille
sous `qualitos_app` (`DB_APP_USER`) : ni superutilisateur, ni propriétaire, ni
`BYPASSRLS`. L'engine le crée lui-même après chaque migration (callback Flyway),
pose son mot de passe (mot de passe généré une fois par `deploy.sh`) et ses
droits (DML, séquences, fonctions ; pas l'historique des migrations). Sans
`DB_APP_USER`, rien ne change.

### 2. Une politique par table à client, découverte et non listée

Après chaque migration, chaque table du schéma portant une colonne `tenant_id`
reçoit la politique `qualitos_tenant_isolation`. Une table créée demain est
couverte sans que personne n'y pense. Les types `uuid`, `text` et `varchar` sont
traités ; la conversion porte sur le réglage, jamais sur la colonne, pour garder
les index.

### 3. Contraindre quand un client est fixé

Prédicat : pas de client fixé (`app.tenant_id` vide) **ou** ligne partagée
(`tenant_id` NULL) **ou** `tenant_id` = le client. Pour la lecture comme pour
l'écriture (`WITH CHECK`).

- Les requêtes portent toujours un client : elles ne voient et n'écrivent que
  le leur. C'est le risque réel, un filtre oublié, et il est désormais fermé.
- Les travaux de fond (relais Kafka, ancrage, rappels, chargements au
  démarrage) n'ont pas de client et parcourent légitimement tous les clients :
  inchangés. Les rendre stricts demanderait de leur attribuer un rôle
  d'exploitation distinct ; c'est la suite (phase 2).

`TenantAwareDataSource` pose `app.tenant_id` à **chaque** emprunt d'une
connexion, client ou non. Une connexion rendue au pool garde ses réglages, et le
client d'une requête ne doit jamais déteindre sur la suivante. Un banc le vérifie
sur une même connexion physique.

### 4. L'IA aussi

L'exécuteur NLQ de l'ai-service pose le client de la requête
(`set_config(..., true)`, local à la transaction) avant d'exécuter le SQL généré,
et refuse de partir sans client. Le rôle `qualitos_nlq_ro` n'étant pas
superutilisateur, même un SQL qui échapperait au validateur ne rendrait que les
lignes de ce client.

### 5. Un interrupteur, un retour arrière immédiat

`QUALITOS_RLS_ENABLED` active ou désactive les politiques au démarrage suivant,
sans migration. Activé en préproduction ; inactif par défaut ailleurs, jusqu'à
validation sur la préproduction.

## Conséquences

- Un coût d'un aller-retour (`set_config`) par emprunt de connexion, de l'ordre
  du dixième de milliseconde.
- Banc `RlsPostgresTest` (vrai PostgreSQL, toutes les migrations) : politique sur
  chaque table à client, lecture et écriture bornées, pas de fuite d'un emprunt
  à l'autre, rôle non privilégié, désactivation, rejeu.
- Reste à faire : phase 2 (travaux de fond sous un rôle d'exploitation, puis
  prédicat strict sans client) ; `api-core` et `api-iot-hub` ; le mode léger
  (compose) n'a pas encore de rôle applicatif.
