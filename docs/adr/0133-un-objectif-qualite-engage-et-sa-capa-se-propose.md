# ADR 0133 — Un objectif qualité engage, et sa CAPA se propose

- **Statut** : accepté
- **Date** : 2026-11-21
- **Portée** : objectifs qualité (ISO 9001 §6.2), revue mensuelle objectifs vs
  KPI réels, nouvelle source CAPA, clôture d'un dossier né d'un écart
  d'objectif
- **S'appuie sur** : l'ADR 0073 et l'ADR 0108 (critère et verdict d'efficacité),
  l'ADR 0131 et l'ADR 0132 (vérification portée par l'action), l'ADR 0052
  (libellés figés), l'ADR 0078 (droits par client), l'ADR 0086 (isolation par la
  base)

## Contexte

La plateforme savait définir un indicateur et le mesurer : `kpi_definitions`
porte un code, une unité, un sens d'amélioration, une cible et deux seuils ;
`kpi_measurements` porte les relevés. Elle ne savait pas dire **ce qu'on attend**
de cet indicateur, ni **ce qu'il faut faire** quand l'attente n'est pas tenue.

C'est tout l'écart entre un indicateur et un objectif : **l'indicateur constate,
l'objectif engage**. ISO 9001 §6.2 demande des objectifs qualité mesurables,
suivis, revus, et §9.3 en fait une entrée obligatoire de la revue de direction.
Une revue qui montre des courbes sans dire laquelle manque son engagement ne
répond pas à la norme.

Trois choses manquaient donc :

1. l'engagement lui-même — le processus concerné, la clause, le pilote, la
   période sur laquelle on juge, et les seuils de l'objectif, distincts de ceux
   du catalogue ;
2. une **règle de déclenchement** : au bout de combien de périodes hors cible
   faut-il agir, et comment ;
3. de quoi **refermer** l'écart autrement que sur une déclaration.

## Décision

### 1. L'objectif est une entité à part, et il n'emporte ni unité ni sens

`quality_objectives` pointe un KPI du catalogue et porte **ses** seuils.

Pourquoi pas des colonnes de plus sur `kpi_definitions` : un même KPI sert
plusieurs objectifs — le taux de rebut visé à 1,5 % en production et à 0,8 % sur
la ligne neuve — et chaque objectif a son processus, sa clause, son pilote et sa
règle. Les seuils de la définition disent la **santé de l'indicateur** ; ceux de
l'objectif sont l'**engagement pris devant la direction**. Les confondre
obligerait à choisir entre les deux lectures.

**L'unité et le sens d'amélioration ne sont pas recopiés.** Ils appartiennent au
KPI. Un objectif qui porterait son unité pourrait la contredire, et c'est la
cible affichée qui mentirait. C'est aussi ce que demande l'annotation de la
maquette : pas d'unité en dur sur les champs de cible et de seuils, puisqu'elle
change d'un indicateur à l'autre. Le client la règle une fois, sur le KPI.

### 2. La période est fermée à 1, 3, 6 ou 12 mois

La période est l'unité dans laquelle se comptent les dépassements consécutifs.
Une période libre rendrait la règle incomparable d'un objectif à l'autre, et un
objectif réglé sur sept semaines ne voudrait rien dire devant une revue
trimestrielle.

Elle n'est pas la **fréquence** du KPI : un indicateur relevé chaque mois peut
servir un objectif revu au trimestre. D'où `ObjectivePeriodSeries`, qui regroupe
les relevés en fenêtres de *n* mois **comptées à rebours depuis le dernier
relevé** — c'est la dernière période qui intéresse une revue, et c'est elle qui
doit tomber juste.

La valeur d'une fenêtre est la **moyenne** de ses relevés. C'est le seul agrégat
défendable sans connaître la formule du KPI : sommer fausserait un taux, prendre
le dernier jetterait les autres. Dans le cas courant — indicateur mensuel,
objectif mensuel — la fenêtre contient un seul relevé et la moyenne est ce
relevé, sans aucune déformation. Une fenêtre **sans relevé est absente** de la
série : la compter romprait une suite de dépassements sur un simple trou de
mesure, et un mois non mesuré n'est pas un mois à zéro.

### 3. La CAPA est proposée, jamais imposée

`quality_objective_proposals` est une **étape**, pas une CAPA. Tant qu'elle est
`PENDING`, aucun dossier n'existe. Le pilote l'accepte (« Accepter et enregistrer
la CAPA »), la rejette avec justification, ou lui ajoute une action.

La maquette HTML disait « créer la CAPA automatiquement » au niveau critique ;
l'annotation du `.pptx` l'a corrigé en « la CAPA est proposée automatiquement ».
C'est la correction qui est retenue, et elle est juste : créer le dossier
d'autorité remplirait le registre de CAPA que personne n'a décidées, alors que
l'ISO demande l'inverse — une action décidée par qui en répond (§10.2). C'est
aussi l'invariant de la plateforme : l'outil propose, l'humain décide
(CLAUDE.md §12). La différence entre les deux niveaux reste donc le **nombre de
périodes** qu'il faut pour déclencher (deux en alerte, une en critique, tous
deux paramétrables), pas le pouvoir d'écrire au registre sans arbitrage.

**Trois états, et pas deux.** Un rejet n'est pas une absence de décision. Sans
`REJECTED` et son motif obligatoire, un objectif critique sans dossier ne se
distinguerait pas d'un objectif critique que personne n'a regardé — et c'est
précisément la question qu'un auditeur pose.

La période déclenchante et la valeur relevée sont **recopiées** dans la
proposition : elle doit se relire sans recalcul, des années après, même si les
relevés ont été corrigés depuis.

### 4. Pour cette source, la vérification d'efficacité est obligatoire et son critère verrouillé

Un dossier né de `CapaSourceType.QUALITY_OBJECTIVE` naît avec
`verificationRequired = true`, **sans question posée** — contrairement à tous les
autres, où elle se décide à l'ouverture (ADR 0073, ADR 0131).

Et son critère n'est pas saisissable : il est **rédigé par la règle**.

> Le KPI « *nom* » (*code*) revient à ou sous sa cible de *x unité* pendant
> *n* période(s) consécutive(s) de *m* mois. L'objectif « *titre* » ne repasse
> « atteint » qu'à ce moment.

Le laisser saisir reviendrait à permettre de clore sur un critère plus facile que
celui qu'on s'était donné. Pour un écart d'objectif, l'efficacité **n'est pas une
appréciation** : c'est un fait mesurable, et la plateforme sait le mesurer.

### 5. Le verrou de clôture est mesuré, et il est annoncé avant d'être tenu

`verifyEffectiveness` refuse un verdict qui clôt tant que le retour sous la cible
n'est pas constaté, et le nouveau motif `OBJECTIVE_RECOVERY_PENDING` le dit
**à l'avance** dans `closureBlockers`.

Les deux, comme pour l'ADR 0132 : une liste d'obstacles qui n'engage pas le
serveur ne vaut rien, et un refus que l'écran n'a pas annoncé surprend au clic.
Le décompte est le nombre de périodes **qu'il reste** à tenir, et non celles déjà
acquises : c'est ce que le vérificateur a besoin de savoir pour arrêter de
réessayer.

Le verrou ne vaut que pour un verdict qui **clôt**. Constater l'échec —
`INEFFECTIVE` — ou l'insuffisance — `PARTIAL` — ne ferme rien : l'interdire
empêcherait d'écrire que les actions n'ont pas produit leur effet, et c'est ce
constat dont la boucle d'amélioration a besoin pour repartir.

### 6. La dépendance est inversée, pour ne pas fermer de cycle

Le module des objectifs appelle `CapaService` pour créer le dossier ; le module
CAPA a besoin de savoir où en est le retour sous la cible avant d'accepter un
verdict qui clôt. Les faire se connaître tous les deux fermerait le cycle.

`ObjectiveRecoveryPort` est donc **déclaré dans le module CAPA** — qui dit ce
qu'il lui faut, et rien de plus — et **implémenté dans le module des objectifs**.
L'adaptateur ne dépend que des dépôts, jamais de `QualityObjectiveService` :
passer par le service refermerait le cycle par l'autre côté.

Une réponse **vide** veut dire « la question ne se pose pas pour ce dossier », et
non « pas encore revenu ». Confondre les deux rendrait tout dossier ordinaire
impossible à clore.

### 7. L'objectif garde la mémoire de l'écart

Tant qu'une proposition acceptée a un dossier encore ouvert, l'objectif **ne
repasse pas « atteint »** au premier relevé redevenu bon : il attend le retour
sous la cible pendant `recovery_periods`.

Un retour au vert sur un seul relevé n'est pas une preuve d'efficacité, c'est une
fluctuation. Et `recovery_periods` sert **aux deux** — le statut de l'objectif et
le critère de sa CAPA — parce que ce sont la même question posée deux fois.

Le statut lui-même **n'est jamais persisté** : il se recalcule à chaque lecture
depuis les relevés. Le figer le ferait mentir dès qu'un relevé est corrigé — et un
relevé se corrige, c'est même l'objet d'une bonne part du module KPI.

### 8. L'écriture relève du pilotage, la lecture de tout le monde

`OBJECTIVE_MANAGE` (`objective.manage`), rattachée au module `kpi` — un objectif
n'existe que contre un KPI du catalogue, et l'annotation de la maquette place la
section « soit dans Dashboard soit dans indicators KPI ».

Réservée au pilotage pour la même raison que `KPI_MANAGE` : déplacer la cible
d'un objectif qu'on n'atteint pas le fait passer au vert sans que rien n'ait
changé sur le terrain. **Accepter ou rejeter en relève aussi** : un utilisateur
qui pourrait rejeter les propositions ferait disparaître des écarts du registre
sans en répondre. La lecture reste ouverte à tout authentifié : la revue des
objectifs est ce qu'une organisation montre à ses équipes.

## Conséquences

- ✅ **§6.2 et §9.3 sont tenus** : des objectifs mesurables, une revue qui dit
  lequel manque son engagement, et la trace de ce qu'on a décidé — y compris de
  ne rien décider, avec son motif.
- ✅ **Un écart d'objectif ne se referme plus sur une déclaration.** C'est la
  demande explicite de l'annotation, et c'est la partie que son auteur jugeait
  « compliquée » : elle est tenue par un calcul, pas par une consigne.
- ✅ **Un objectif archivé reste lisible** : il n'est pas supprimé, sans quoi la
  raison des CAPA qu'il a ouvertes disparaîtrait avec lui, et une revue relue
  l'année suivante ne se comprendrait plus.
- ✅ **Une réévaluation est idempotente** : la même période et le même niveau ne
  se proposent qu'une fois, évité dans le service et interdit en base. Sans cela,
  un écran rafraîchi trois fois proposerait trois fois le même dossier.
- ⚠️ **Le KPI visé ne change pas après la création.** Un objectif qui changerait
  d'indicateur rendrait incomparable son propre historique, et les dossiers qu'il
  a ouverts cesseraient de se rapporter à ce qu'ils mesuraient. Il faut en créer
  un autre et archiver l'ancien.
- ⚠️ **La moyenne est le seul agrégat offert** pour une période qui contient
  plusieurs relevés. Un KPI dont l'agrégat correct serait une somme ou un dernier
  relevé n'est pas servi fidèlement sur une période plus longue que sa fréquence.
  Le cas ne se présente pas quand période et fréquence coïncident, ce qui est
  l'usage attendu. À reprendre si un client règle un objectif annuel sur un
  compteur mensuel.
- ⚠️ **Les jours fériés ne sont pas connus de la plateforme** : l'échéance
  d'analyse de cause racine compte les jours ouvrés et saute les week-ends, pas
  les fériés.
- ⚠️ **Le vérificateur est le pilote de la CAPA**, faute d'un second rôle
  demandé. Le critère étant mesuré par la plateforme, l'enjeu est moindre que sur
  un dossier ordinaire — mais c'est bien la même personne qui mène et qui
  constate.
- 📌 **Signalé, non livré** : l'annotation souhaite « donner la main au client
  pour paramétrer ses objectifs avec les unités de son choix ». L'unité vient
  aujourd'hui du KPI, que le client règle déjà ; une unité propre à l'objectif
  n'a pas été ajoutée, faute d'avoir été arbitrée (règle 18.2 #11).
