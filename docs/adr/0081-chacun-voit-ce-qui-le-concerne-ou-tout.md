# ADR 0081 — Chacun voit ce qui le concerne, ou tout le registre

- **Statut** : Accepté
- **Date** : 2026-10-08
- **Owners** : @Couldevlop
- **Portée** : api-quality-engine (`authz`, `nonconformity`, `capa`, `riskregister`),
  matrice « Rôles et droits », listes NC, CAPA et registre des risques
- **S'inscrit dans** : CLAUDE.md §16 (rôles, ABAC), ADR 0078 (droits par client)

## Contexte

Les droits par client (ADR 0078) disent ce qu'un membre peut FAIRE. Rien ne
disait ce qu'il peut VOIR : tout membre d'un client lisait toutes ses
non-conformités, tous ses dossiers CAPA, tout son registre des risques. Un
client qui ouvre la plateforme à des opérateurs, à des sous-traitants ou à un
auditeur externe veut que chacun voie ce qui le concerne, et que le pilotage
voie tout.

## Décisions

### 1. Deux portées, réglées comme une action

Pour chaque registre, une action « voir tout » entre au catalogue :
`nc.view.all`, `capa.view.all`, `risk.view.all`. Elle se coche et se décoche
dans la matrice des droits, rôle par rôle, comme les autres. Sans elle, le
membre ne voit que ce qui le concerne.

Elle est accordée par défaut à tous les rôles : un client qui n'a rien réglé
voit ce qu'il voyait avant. Il restreint ensuite qui il veut.

### 2. Ce que « me concerne » veut dire, module par module

- **Non-conformité** : je l'ai déclarée.
- **Dossier CAPA** : je le pilote, j'en vérifie l'efficacité, ou une de ses
  actions m'est confiée.
- **Risque ou opportunité** : je l'ai inscrit. Le « propriétaire » d'une fiche
  est un nom libre, pas un compte : il ne peut pas servir à cela.

Ces règles s'appuient sur des champs qui existent déjà et que le serveur
remplit depuis le jeton (le déclarant d'une NC, l'auteur d'une fiche).

### 3. Les sites viendront à part

Une portée « mon site » demanderait un référentiel de sites, le rattachement
des membres et un site sur chaque objet. C'est un chantier à part entière ; il
s'ajoutera comme une troisième portée sans changer celles-ci.

### 4. Ce qu'on ne voit pas n'existe pas

Une fiche hors de portée répond 404, en lecture comme en écriture, et ses
dépendances aussi : photos et rapport 8D d'une NC, preuves d'un dossier CAPA,
brouillon d'un risque issu d'une NC. Un 403 dirait que la fiche existe.

Les listes filtrent en base (spécification, requête JPQL) pour que la
pagination reste juste. Les tuiles de la liste des NC comptent le même
périmètre que le tableau.

### 5. Un port, une règle par module

`RecordScope.restrictTo(viewAll)` rend vide si l'utilisateur voit tout, sinon
l'utilisateur du jeton. Sans utilisateur identifié (compte de service) et sans
« voir tout », il rend un identifiant que personne ne porte : rien plutôt que
tout. Chaque module garde sa définition de « concerner » (`NcScope`,
`CapaScope`, le registre par son port de contexte).

## Conséquences

- Les synthèses (tableau de bord exécutif, SMI, efficacité des CAPA, KPI)
  restent calculées sur tout le client : ce sont des indicateurs de pilotage,
  et ceux qui y ont accès pilotent. Les restreindre aussi est possible plus
  tard, module par module.
- Une liste restreinte le dit (« Vous voyez les non-conformités que vous avez
  déclarées ») : une liste courte sans explication ressemble à une panne.
- Les documents publiés restent lisibles par tous : leur lecture est
  l'objet même de la maîtrise documentaire.
