# Intégrations — ITSM, ERP, dossier patient FHIR et outils de conversation

[← Retour à l'index](../README.md) · Routes : **`/itsm`** et **`/connectors`** ·
Menu : *Référentiels › Intégrations* et *Administration › Connecteurs*

## À quoi servent ces écrans

QualitOS ne vit pas seul : les incidents sont dans votre outil de support, les fournisseurs et
les volumes de production dans votre ERP, les événements indésirables dans le dossier patient,
et vos équipes discutent dans Teams ou Slack. Deux écrans branchent la plateforme dessus.

| Écran | Ce qu'il branche | Route |
| --- | --- | --- |
| **Intégrations (ITSM)** | ServiceNow, Jira Service Management — **import** des tickets | `/itsm` |
| **Connecteurs** | ERP, dossier patient FHIR, outils de conversation | `/connectors` |

Quatre familles, une même mécanique : une **connexion nommée**, un **fournisseur**, un
**secret chiffré au repos**, un cycle de vie, et un **compteur d'échecs consécutifs**.

## La règle commune aux quatre familles

### Le secret ne revient jamais

Le secret (mot de passe, jeton, URL de webhook) est **chiffré au repos** et **jamais
réaffiché** — ni dans l'écran, ni dans la réponse de l'API, ni dans les journaux. Pour en
changer, vous en saisissez un nouveau. Il n'est déchiffré qu'au moment de la synchronisation,
en mémoire.

### Une connexion qui échoue se désactive seule

Trois états : **active**, **désactivée** (par vous), et **désactivée sur erreurs**. Ce dernier
état n'est jamais posé à la main : après **dix échecs consécutifs**, la plateforme coupe la
connexion d'elle-même. Sans cela, une URL devenue fausse produirait un échec toutes les
minutes jusqu'à noyer les journaux — et personne ne verrait le vrai incident au milieu.

Le nombre d'échecs consécutifs et la date du **dernier succès** sont affichés sur chaque
connexion. Ce sont les deux chiffres à regarder quand une intégration « ne marche plus ».

## Parcours pas à pas — ITSM (`/itsm`)

**Ce que l'écran fait** : importer les tickets de ServiceNow ou de Jira Service Management pour
boucler la qualité avec le support informatique.

1. **Nouvelle connexion** : le fournisseur, l'**URL de base** de votre instance, l'utilisateur,
   le secret, et le **périmètre** à interroger.
2. Ouvrir la connexion, puis **Synchroniser**.
3. Lire le résultat de la synchronisation, annoncé tel quel : nombre de **tickets récupérés**,
   combien de **nouveaux**, combien **déjà connus**, et l'heure d'exécution. En cas d'échec,
   le message d'erreur est affiché.
4. La table des **mappings** liste ce qui a été importé : identifiant externe, titre, statut et
   priorité côté outil source, entité interne correspondante, date de dernière vue.

L'import est **incrémental** (il repart du dernier succès) et **idempotent** : un même ticket
réimporté met à jour sa date de dernière vue, il ne se duplique pas.

## Parcours pas à pas — Connecteurs (`/connectors`)

L'écran présente trois onglets et, en tête, quatre compteurs : connexions, actives, inactives,
**en échec**.

### Onglet ERP

Importe les **fournisseurs** et les **indicateurs de production** depuis **SAP**,
**Oracle Fusion** ou **Dynamics**. La synchronisation est **manuelle**, déclenchée depuis
l'écran — rien ne tourne en arrière-plan à votre insu.

Le compte rendu est détaillé : fournisseurs importés, fournisseurs **ignorés**, mesures de KPI
écrites, et **mesures sans KPI connu**. Ce dernier chiffre est le plus utile : il vous dit que
l'ERP envoie un indicateur que votre catalogue de KPI ne connaît pas encore.

### Onglet Dossier patient (FHIR)

Récupère les **événements indésirables** d'un serveur **HL7 FHIR R4 ou R5**. L'import est
**incrémental depuis le dernier succès** et, point important pour votre délégué à la protection
des données : il **ne conserve aucune donnée patient**. Le compte rendu donne les ressources
lues et les ressources importées.

### Onglet Conversation

Pousse les **alertes qualité** vers **Teams**, **Slack** ou **Mattermost** par un webhook
entrant fourni par l'outil de chat. Ici, **l'URL est le secret** : elle porte un jeton non
devinable, elle est donc chiffrée comme un mot de passe et jamais réaffichée. Un **envoi de
test** permet de vérifier qu'elle fonctionne encore sans attendre la prochaine alerte réelle.

## Ce qui est réellement livré, et ce qui ne l'est pas

Les clients de ces quatre familles sont **de vrais clients HTTP** qui parlent aux API publiées
par les éditeurs concernés (table des incidents ServiceNow, API REST Jira, OData SAP, REST
Oracle Fusion et Dynamics, *bundles* FHIR, webhooks entrants de chat). Ce ne sont pas des
simulations.

Les listes de fournisseurs sont en revanche **limitées à ce qui est implémenté** :

| Famille | Fournisseurs livrés |
| --- | --- |
| ITSM | ServiceNow, Jira Service Management |
| ERP | SAP, Oracle Fusion, Microsoft Dynamics |
| Dossier patient | serveur FHIR R4 ou R5 |
| Conversation | Teams, Slack, Mattermost |

Les autres intégrations citées dans la vision du produit (GED externe, PLM, GMAO, outils de
BI) **ne sont pas livrées** sous forme de connecteur configurable : ne les annoncez pas à un
client sur la foi d'une plaquette.

Pour une intégration sortante événementielle — « préviens mon système quand une
non-conformité est créée » — c'est l'écran **Webhooks** (*Administration › Webhooks*) qu'il
faut employer, pas ces connecteurs.

## Bonnes pratiques

- **Commencez par un périmètre étroit.** Une première synchronisation ITSM sur toute
  l'historique ramène des milliers de tickets dont aucun ne sera traité.
- **Regardez « mesures sans KPI connu » après chaque synchronisation ERP.** C'est là que se
  cache l'indicateur que vous croyiez suivre.
- **Testez le webhook de conversation après chaque rotation.** Une URL révoquée échoue en
  silence côté outil de chat ; le test est le seul moyen de le savoir avant l'incident.
- **Ne partagez pas une connexion entre deux usages.** Le compteur d'échecs est porté par la
  connexion : une URL fatiguée désactivera les deux.
- **Une connexion « désactivée sur erreurs » demande une correction, pas une réactivation.**
  La remettre active sans corriger l'URL ou le secret reproduit les dix échecs.
