# Co-couverture IMS, audit blanc IA et génération documentaire

[← Retour à l'index](../README.md) · Routes : **`/standards-ims`**, **`/standards-doc-gen`** ·
Menu : *Opérations › Co-couverture IMS* et *Opérations › Génération doc IA*

Ces deux écrans prolongent le [Standards Hub](standards-hub.md), qui reste le point d'entrée des
normes, de leurs exigences et de leurs preuves. Le premier sert à **préparer une certification
multi-normes** ; le second à **produire le dossier documentaire** qu'elle exige.

---

## Co-couverture IMS (`/standards-ims`)

L'écran s'intitule « Système intégré, audit blanc IA et preuves ancrées » et porte trois leviers
d'un même dossier de certification : **mutualiser** les exigences entre normes, **se faire
auditer** avant l'auditeur, et **prouver** que rien n'a bougé depuis.

### 1. La matrice de co-couverture

Un **IMS** (*Integrated Management System*, système de management intégré) est un système qui
répond à plusieurs normes à la fois — typiquement ISO 9001 + ISO 14001 + ISO 45001, qui
partagent une même structure de haut niveau.

**Parcours :**

1. Par défaut, les normes **adoptées par votre organisation** sont comparées entre elles. Vous
   pouvez choisir explicitement les normes à comparer, puis « Revenir aux normes adoptées ».
2. La **matrice** présente une ligne par clause source et une colonne par norme. **Une cellule
   remplie signifie qu'une preuve produite pour la clause vaut aussi pour la norme de la
   colonne.**
3. Le filtre « Clauses mutualisables uniquement » masque les clauses spécifiques à une seule
   norme ; « Afficher tous les liens » les ramène.

**Comment la lire :** chaque clause est marquée *Mutualisation* ou *Spécifique*. Les clauses
mutualisables sont celles où un document, un audit ou une formation unique couvre plusieurs
systèmes : c'est là que se trouve l'économie d'effort d'un système intégré. Les clauses
spécifiques sont celles qu'aucune autre norme ne reprend — elles demandent un travail dédié, et
les oublier est l'erreur classique d'un projet multi-normes.

### 2. Les deux audits blancs, qui ne sont pas la même chose

L'écran énonce lui-même la distinction, et elle est importante :

| | Audit blanc « règles » | Audit blanc IA |
|---|---|---|
| **Où** | sur la fiche de la norme, dans le Standards Hub | sur cet écran |
| **Ce qu'il fait** | calcul déterministe et instantané du taux de couverture des exigences par les preuves rattachées | 30 à 100 questions d'auditeur ciblées sur les clauses à risque, constats rédigés, plan de remédiation |
| **Ce qu'il en reste** | rien : c'est une photo à l'instant T | chaque exécution est **persistée, datée, attribuée à son auteur** et relisible à l'identique |

**Lancer un audit blanc IA :**

1. Choisir la **norme adoptée** à auditer.
2. « Lancer l'audit blanc IA ». Si vous n'avez pas le droit requis, l'écran le dit au lieu de
   laisser le bouton échouer.
3. L'**historique** liste les exécutions : date, niveau de préparation, écarts (majeures /
   mineures / observations), nombre de questions posées, et le **moteur** qui les a produites.
4. « Ouvrir le rapport » affiche l'exécution conservée telle quelle.

**Lire un rapport d'audit blanc** (`/standards-ims/audit-blanc-ia/…`) :

- **Synthèse** : niveau de préparation, NC majeures, NC mineures, observations, questions
  posées, date d'exécution.
- **Analyse des écarts** : une entrée par clause auditée, avec la **couverture réelle des
  exigences** — calculée sur vos preuves, pas devinée — et le constat rédigé.
- **Plan de remédiation** : une action par écart majeur ou mineur, **orientée vers le module
  QualitOS qui produira la preuve manquante**. Les observations n'en génèrent pas, volontairement :
  une observation n'est pas un écart.

La criticité d'un écart (majeure, mineure, observation) suit une règle, elle n'est pas laissée à
l'appréciation du modèle.

### 3. L'ancrage des preuves

Le dernier panneau ancre par lot les événements de preuve et rend les éléments de vérification :
nombre d'**événements ancrés**, **racine Merkle**, **référence de transaction** et **hash
d'intégrité**. C'est ce qui permet, des mois plus tard, de démontrer à un auditeur qu'un dossier
n'a pas été réécrit après coup. Aucune donnée personnelle n'est ancrée — uniquement des
empreintes.

---

## Génération documentaire IA (`/standards-doc-gen`)

L'écran génère **en lot** un dossier documentaire complet — Manuel Qualité, Politique,
procédures — pré-rempli à partir du contexte de votre organisation. **Chaque pièce passe par une
validation humaine avant signature et ancrage.**

### Parcours pas à pas

1. **Nouvelle génération** : choisir la **norme visée**, l'**organisation**, le **secteur**, la
   **langue de rédaction** (français, anglais, espagnol, allemand) et les **pièces à générer**.
2. « Générer le dossier ». Le **dossier en cours** suit l'avancement pièce par pièce ; les
   pièces en échec peuvent être relancées sans refaire le dossier.
3. Pour chaque pièce, « Réviser » ouvre la **revue du document** (`/standards-doc-gen/documents/:id`).
4. En revue : relire le brouillon, le **soumettre à validation**, puis l'**approuver en signant**
   (avec des notes facultatives) ou le **rejeter avec un motif**. Un rejet renvoie le document en
   brouillon, et le dernier motif de rejet reste affiché.
5. Quand toutes les pièces sont approuvées et signées, le dossier se **finalise** : il est signé
   et ancré, avec son référence d'ancrage.

### Ce qu'il faut comprendre avant de l'utiliser

- **Aucune publication sans validation humaine.** C'est écrit dans l'écran et tenu par le
  serveur : un document reste un brouillon jusqu'à sa signature, et **l'approbateur ne peut pas
  être celui qui a soumis**.
- **Aucun document n'est stocké en dur.** Le contenu est produit à partir de votre contexte ;
  il n'y a pas de modèle pré-écrit déguisé en génération.
- Les **clauses** couvertes par la pièce sont rappelées dans l'écran de revue : c'est ce qui
  relie le document au référentiel.
- Un brouillon rédigé vite n'est pas un document maîtrisé : la relecture est le travail, pas une
  formalité.

---

## Lien avec le tableau de bord SMI

Les normes **adoptées** par votre organisation sont celles que comparent la matrice de
co-couverture et que suit le tableau de bord SMI. Ajouter une norme au système de management
intégré se fait par le [Standards Hub](standards-hub.md), et **seul l'administrateur du client**
peut le faire : c'est une décision de périmètre, pas un réglage d'écran.

## Bonnes pratiques

- **Commencez par la matrice**, avant d'écrire une ligne de procédure : vous saurez quelles
  clauses se mutualisent et quel effort réel vous attend.
- **Jouez l'audit blanc IA à mi-parcours, pas la veille** : son intérêt est de laisser le temps
  d'exécuter le plan de remédiation.
- **Traitez le plan de remédiation dans les modules qu'il désigne** — une action qui produit la
  preuve manquante vaut mieux qu'une action qui la décrit.
- **Ne diffusez jamais un document généré sans l'avoir relu clause par clause.** La signature
  engage la personne qui signe.
