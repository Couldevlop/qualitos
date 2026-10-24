# ADR 0098 — La matrice de compétences éditable (ateliers, formateurs, niveaux)

- **Statut** : Accepté
- **Date** : 2026-10-23
- **Owners** : @Couldevlop
- **Portée** : api-quality-engine (`training.competencymatrix`, migration V147),
  écran `/training/competences`, écran `/admin/competences`
- **Remplace** : ADR 0060 §7 (matrice en lecture seule, « une case jamais
  évaluée reste vide »)
- **Complète** : ADR 0078 (droits par client), ADR 0086 (isolation par client
  en base), ADR 0092 (une personne est un utilisateur de l'annuaire)

## Contexte

La matrice de compétences de l'ADR 0060 se lisait sans s'écrire : compétences
en lignes, personnes évaluées en colonnes, niveaux venus des évaluations. Le
support de recette et la maquette « Matrice de compétences.html » la
retournent et la rendent vivante : opérateurs en lignes, rangés par atelier ;
compétences en colonnes, chacune avec sa catégorie et son **formateur** ; un
clic sur une case fait tourner le niveau. L'échelle est celle de l'atelier :
0 Non formé, 1 Débutant, 2 Autonome, 3 Expert, 4 Formateur. La règle de lecture
est fixée : une compétence est **à risque** quand moins de deux personnes y sont
autonomes (niveau 2 ou plus).

La maquette garde tout dans le navigateur (`localStorage`). Une matrice qui
décide qui forme qui doit vivre côté serveur, par client, tracée.

## Décisions

### 1. Un niveau posé est une évaluation, pas une donnée de plus

Les niveaux ne sont pas stockés à part : ce sont les évaluations de compétence
existantes (`training_user_skills`, 0 à 4). Faire tourner une case, c'est
réévaluer la personne : la ligne est écrite (ou créée) avec la source
`MANAGER`, l'évaluateur (`sub` du jeton) et la date du jour ; l'échéance de
validité déjà posée est conservée, et le nom de la personne est recopié. Il
n'existe donc pas deux vérités sur ce qu'une personne sait : le dossier de
formation et la matrice lisent la même ligne.

Les compétences restent celles du **catalogue** (`training_skills`, avec sa
catégorie). Ajouter une compétence à la matrice reprend la compétence du
catalogue qui porte déjà ce nom (sans égard à la casse), avec ses
évaluations ; sa catégorie n'est complétée que si elle était vide. Sinon une
compétence est créée au catalogue, avec un code tiré de son nom (sans accents,
80 caractères au plus, suffixé `-2`, `-3`… en cas de collision).

Ce qui manquait, la migration V147 l'ajoute en trois tables, toutes portant
`tenant_id` (politique RLS posée automatiquement, ADR 0086) :

- `competency_workshops` : les ateliers du client, ordonnés ;
- `competency_matrix_operators` : qui la matrice suit, son atelier ;
- `competency_matrix_skills` : quelles compétences elle suit, et leur formateur
  (clé étrangère vers le catalogue, `ON DELETE CASCADE`).

### 2. L'échelle : mêmes entiers, nouveaux mots

L'énumération `CompetencyLevel` (`NONE`, `AWARE`, `PRACTITIONER`, `COMPETENT`,
`EXPERT`) garde ses noms et ses entiers : l'API du catalogue et les
évaluations existantes ne bougent pas. La matrice nomme ces mêmes entiers avec
le vocabulaire de l'atelier (0 Non formé … 4 Formateur). Le sens « autonome »
est fixé au niveau 2 (`SkillLevel.MIN_AUTONOMOUS`), le seuil de risque à deux
personnes, des deux côtés de la pile.

### 3. Dans la matrice, l'absence d'évaluation vaut « Non formé »

C'est le point où l'ADR 0060 §7 est remplacé. Une personne n'entre plus dans
la matrice parce qu'elle a été évaluée : on l'**inscrit**, explicitement, dans
un atelier. Pour un opérateur inscrit, une case sans évaluation se lit « Non
formé » — c'est le point de départ d'un plan de formation, pas une
incompétence constatée. La couverture ne compte que les niveaux 2 et plus, donc
la confusion ne fausse plus la lecture : vide et 0 ne comptent ni l'un ni
l'autre. Seules les évaluations posées voyagent dans la réponse ; une cellule
absente vaut 0.

### 4. Les personnes viennent de l'annuaire

Opérateurs et formateurs sont choisis dans l'annuaire (`qos-user-picker`,
ADR 0092) : on stocke le compte (`sub`) et le nom recopié au moment du choix,
pour que la ligne reste lisible si la personne quitte l'annuaire. Aucun nom
tapé, aucun identifiant affiché. Une personne n'est inscrite qu'une fois
(409). Le formateur est **obligatoire** : une compétence à risque sans personne
vers qui se tourner n'est pas un risque géré.

### 5. Les ateliers sont réglés par l'administrateur du client

Sans réglage, la plateforme propose Usinage, Assemblage, Qualité, Maintenance.
L'administrateur du client remplace la liste d'un bloc dans « Ateliers »
(`/admin/competences`, `PUT /api/v1/training/competencies/workshops`, droit
`authz.manage`) : 1 à 30 ateliers, 1 à 60 caractères, sans doublon à la casse
près. L'atelier d'un opérateur est recopié en clair : retirer un atelier de la
liste ne réaffecte personne, et le filtre de la matrice le propose encore tant
qu'un opérateur le porte. Inscrire un opérateur exige un atelier de la liste en
vigueur (422 sinon).

### 6. Lire est ouvert, écrire demande `training.manage`

Tout membre du client lit la matrice (`GET /api/v1/training/competencies/matrix`).
Inscrire ou retirer un opérateur, ajouter ou retirer une compétence, poser un
niveau demandent le nouveau droit `training.manage` (module `training`), donné
par défaut au pilotage : administrateur du client, directeur qualité, manager
qualité. La formation est un module du socle : pas de `@RequiresModule`. Le
client vient toujours du jeton ; un `tenantId` glissé dans le corps est ignoré.
L'écran n'affiche les boutons et les cases cliquables qu'à qui détient le
droit ; le serveur tranche.

### 7. Retirer ne détruit rien

Retirer un opérateur supprime sa ligne d'inscription : ses évaluations restent
à son dossier de formation, et reviennent s'il est réinscrit. Retirer une
compétence supprime son suivi par la matrice : elle reste au catalogue, avec
ses évaluations. Les deux passent par une confirmation.

### 8. Tout est tracé, sans donnée personnelle

Chaque écriture produit un événement d'audit : `training.matrix.operator-added`,
`operator-removed`, `skill-added`, `skill-removed`, `level-set` (avec le niveau
précédent), `workshops-updated`. Les charges utiles ne portent que des
identifiants, jamais de nom.

### 9. L'écran suit la maquette, dans le design system

Indicateurs (opérateurs, compétences suivies, compétences à risque, avec un
formateur — le niveau moyen global est écarté, la colonne « Moy. » le porte par
personne), recherche, filtres par atelier et par catégorie, bascule « Mettre en
évidence les risques » qui estompe les colonnes couvertes, panneau des risques
(« Seule personne autonome : X » ou « Aucune personne autonome »), pied de
colonne « Personnes autonomes (2+) » avec ⚠, en-têtes verticaux, première
colonne collante, défilement horizontal limité au tableau. Les cinq teintes se
dérivent des jetons `--qos-*` et suivent les thèmes clair et sombre.

Au clavier : une seule case dans l'ordre de tabulation, les flèches, Début et
Fin déplacent le focus, Entrée et Espace font tourner le niveau. Chaque case a
pour nom « Opérateur – Compétence : Niveau ». L'écriture est optimiste et
sérialisée par case : un refus rétablit la valeur du serveur.

## Conséquences

- L'ancienne lecture `GET /api/v1/training/competencies/matrix` (grille par
  famille, colonnes évaluées) est remplacée par la vue éditable ; le client web
  est le seul consommateur.
- Une personne évaluée hors matrice n'y apparaît plus d'office : il faut
  l'inscrire. Ses évaluations la suivent alors.
- Le dossier de formation et la matrice montrent la même évaluation : changer
  l'un change l'autre.

## Vérification

- `CompetencyMatrixDomainTest` : ateliers, tri, couverture, codes.
- `CompetencyMatrixServiceTest` : règles d'ajout, 404 / 409 / 422, audit du
  niveau précédent.
- `CompetencyMatrixAdaptersTest` (H2) : isolation par client, reprise d'une
  compétence du catalogue, évaluation écrite.
- `CompetencyMatrixControllerTest`, `CompetencyMatrixDeleteSecurityTest` :
  droits (lecture ouverte, 403 hors pilotage, ateliers réservés à
  l'administrateur), 400 de validation, `tenantId` du corps ignoré.
- `MatriceCompetencesOnPostgresTest` : migration V147 et RLS sur PostgreSQL.
- `competency-matrix.component.spec.ts`, dialogues, `competency-workshops`,
  modèle et service côté web.

## Références

- CLAUDE.md §4.7 (matrices de compétences), §16 (rôles), §18.2.
- ADR 0060 §7, ADR 0078, ADR 0086, ADR 0092.
