# Données et multi-tenancy

[← Retour à l'espace technique](README.md)

---

## 1. Les bases

Une base PostgreSQL **par service**, créée par `infra/k8s/deps/10-postgres.yaml` :

| Base | Service | Migrations Flyway versionnées |
| --- | --- | --- |
| `qualitos_core` | `api-core` | 12 fichiers, dernier numéro V12 |
| `qualitos_quality` | `api-quality-engine` | 163 fichiers, dernier numéro V164 |
| `qualitos_iot` | `api-iot-hub` | 2 fichiers, dernier numéro V2 |

> **Relevé du 2026-11-11.** Compté sur `src/main/resources/db/migration`. Un
> `find` lancé sans exclure `target/` rend le double pour le moteur : les mêmes
> fichiers recopiés à la construction. C'est le genre de chiffre qu'il faut mesurer
> avant de l'écrire.

Un **quatrième rôle** existe sans base propre : `qualitos_nlq_ro`, en lecture seule sur
`qualitos_quality`. C'est par lui que passe la requête en langage naturel, et le fait
qu'il n'ait aucun droit d'écriture est la dernière barrière derrière le validateur SQL
(ADR 0009).

Les numéros de migration ne se réemploient jamais et ne se réécrivent pas après
déploiement. Deux branches qui créent le même numéro entrent en collision à la fusion ;
c'est un incident courant du projet et il se règle en renumérotant **avant** d'intégrer.

---

## 2. Le client, de bout en bout

Trois barrières, dans cet ordre :

**1. Le jeton.** `TenantJwtFilter` lit le client depuis le jeton validé et le place dans
`TenantContext`. `CurrentUser` donne l'acteur. Aucun des deux ne se lit dans le corps de
la requête (§18.2 point 2, ADR 0001), et l'absence de contexte de client lève
`MissingTenantContextException` plutôt que de laisser passer une requête sans portée.

**2. Le code.** Chaque dépôt filtre par client. Une requête qui ne le ferait pas est un
défaut de sécurité, pas une optimisation à faire plus tard.

**3. La base.** L'isolation par *Row-Level Security* (ADR 0086) ferme le chemin que les
deux premières barrières laisseraient ouvert en cas d'oubli : un rôle applicatif non
privilégié, une politique sur chaque table portant une colonne `tenant_id`, découverte à
chaque démarrage, et `app.tenant_id` posé à chaque emprunt de connexion
(`TenantAwareDataSource`). Elle est pilotée par `QUALITOS_RLS_ENABLED` — **absente par
défaut dans `application.yml`, active en préproduction** (`values-preprod.yaml`).

`RlsSchema` ré-accorde les droits du rôle applicatif après chaque migration, par un
rappel Flyway. Cette mécanique a une conséquence qu'il faut connaître avant d'écrire une
migration de sécurité : **un `REVOKE` posé dans une migration sera défait au déploiement
suivant, silencieusement.** C'est précisément pourquoi l'ajout seul des pistes d'audit est
garanti par un déclencheur et non par un retrait de droit (ADR 0109).

---

## 3. Les pistes d'audit

Deux journaux, et ils ne se remplacent pas :

| Journal | Où | Portée |
| --- | --- | --- |
| `audit_events` (+ `audit_event_counters`) | moteur de qualité, V26 | ce que **les utilisateurs d'un client** ont fait chez lui |
| `platform_audit_events` (+ `platform_audit_counters`) | `api-core`, V12 | ce que **l'éditeur** a fait sur le dossier d'un client (ADR 0109) |

Les deux partagent trois propriétés : séquence strictement croissante, chaîne SHA-256
d'une ligne à la suivante, acteur lu du jeton et horodatage pris de l'horloge serveur —
jamais des paramètres d'appel. Depuis les migrations V12 et V157, des déclencheurs
refusent `UPDATE` et `DELETE` (SQLSTATE 42501) et refusent qu'un compteur de séquence
recule ou disparaisse.

Deux différences délibérées : le journal de plateforme porte **une seule chaîne** pour
tout l'éditeur (une chaîne par client rendrait invérifiable l'ordre de deux actes visant
deux clients), un **vocabulaire fermé** (`PlatformAuditAction`), et il fait entrer
l'adresse d'origine **dans** l'empreinte. Sa colonne de client s'appelle
`target_tenant_id` et non `tenant_id` : ce second nom déclencherait l'isolation par
client et masquerait à l'éditeur les actes visant les autres clients — soit l'inverse de
la fonction de la table.

Toute empreinte se calcule sur un horodatage que la base rend à l'identique
(`StorableInstant`, ADR 0062). Sans cette précaution, une vérification de chaîne échoue
après un aller-retour en base, pour une raison de précision et non de falsification.

---

## 4. Conventions

- **Nommage.** Tables et colonnes en `snake_case`, anglais pour les noms techniques ;
  les libellés métier destinés à l'écran ne sont pas en base, ils sont traduits côté
  application (voir [§6](#6-ce-qui-nest-pas-en-base)).
- **`tenant_id`.** Toute table métier la porte, et ce nom a un effet : il est reconnu par
  la découverte de politiques RLS. Une table qui doit échapper à l'isolation ne doit pas
  nommer sa colonne ainsi.
- **Personnes.** Une personne désignée par un dossier est un utilisateur de l'annuaire :
  on stocke son compte (`account_id`, le `sub` du jeton) **et** son nom recopié, jamais un
  texte libre, et jamais un identifiant technique à l'écran (ADR 0091, 0092).
- **Paramètres du client.** Les listes plates passent par `tenant_catalog_entries` et le
  mécanisme unique de l'ADR 0111 ; ajouter une liste, c'est ajouter une constante à
  `CatalogId`. Les réglages structurés (questionnaire DMAIC, natures d'écart, barème
  FMEA) gardent leur table parce que leur donnée l'est aussi.
- **Règles métier chiffrées.** Elles sont paramétrables par client et **recopiées sur
  l'objet au moment de l'étape** : une étude de capabilité garde les seuils de Cpk en
  vigueur à sa création, un changement garde ses seuils d'impact à la soumission. Un
  verdict ne doit pas changer parce qu'un réglage a bougé six mois plus tard.

---

## 5. Pas de schéma DBML, et pourquoi

`CLAUDE.md` §19.1 annonce des schémas de base en DBML. **Aucun fichier `.dbml` n'existe
dans le dépôt**, et il ne faut pas en déduire une négligence : un schéma dessiné à la main
pour cent soixante-trois migrations serait faux la semaine suivante, et un schéma faux
est pire qu'absent — on l'ouvre à la place de la base.

Ce qui tient lieu de schéma, et qui ne peut pas mentir :

- les migrations elles-mêmes, lisibles dans l'ordre, chacune portant en commentaire la
  raison de son existence ;
- le champ **Portée** des ADR, qui nomme la migration associée à chaque décision ;
- les entités JPA, qui sont la vue applicative de ces tables.

Si un schéma visuel devient nécessaire — pour un audit, pour un nouvel arrivant — il doit
être **généré** depuis la base, pas dessiné.

## 6. Ce qui n'est pas en base

- **Les pièces jointes.** Photos de non-conformité, preuves de CAPA et fichiers de la
  documentation vivent dans un stockage objet compatible S3 (MinIO en préproduction),
  pas dans PostgreSQL. La route de lecture est publiée par le chart et **désactivée par
  défaut** : sans stockage, les pièces jointes répondent 503 et l'écran le dit. La GED
  stocke chaque fichier une fois, par empreinte SHA-256 au sein d'un client, et le
  compresse seulement s'il y gagne plus de 10 % (ADR 0094).
- **Les libellés d'interface.** Ils sont traduits côté application : français
  source, plus l'anglais, l'espagnol, l'arabe, le japonais et le chinois.
  Servir un libellé depuis la base reviendrait à faire lire du français à un utilisateur
  arabophone — c'est l'une des raisons du mode `RENAME` des listes paramétrables
  (ADR 0111).
- **Les données personnelles sur la chaîne de blocs.** Jamais. Seules des empreintes sont
  ancrées (§11.3).
