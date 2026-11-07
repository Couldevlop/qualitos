# Efficacité des CAPA et matrice de compétences

[← Retour à l'index](../README.md) · Routes : **`/capa/efficacite`** et **`/training/competences`**

Deux écrans qui répondent à des questions qu'on ne pouvait pas poser à la
plateforme jusqu'ici : *nos actions correctives servent-elles à quelque chose ?*
et *qui sait faire quoi, chez nous ?*

---

## Efficacité des CAPA

L'écran `/capa/efficacite` porte **deux onglets**, et ils ne disent pas la même
chose :

- **Évaluations** *juge* l'efficacité. On confronte un relevé au critère posé à
  l'avance, et le verdict décide de la suite du dossier. C'est l'onglet où l'on
  agit, et il vient donc en premier.
- **Mesure par récidive** *vérifie* ce jugement, des mois plus tard, en comptant
  les écarts revenus après la clôture.

Les deux sont sur le même écran exprès. Le premier décide, le second dit si la
décision tenait : les séparer les aurait laissés se contredire sans que personne
ne les compare jamais.

---

### Onglet « Évaluations » : rendre le verdict

> Décision : [ADR 0108](../../adr/0108-verdict-d-efficacite-a-trois-etats-et-critere-pose-a-l-avance.md).

#### Le critère se pose AVANT

Sur la fiche du dossier, un champ porte la phrase qui dit à quoi on reconnaîtra
l'efficacité : « zéro erreur de référence sur 90 jours », « rebut ≤ 2 % sur
trois lots ». Il se saisit à l'ouverture ou pendant le traitement, **jamais au
moment de juger** — un critère choisi en regardant le résultat ne juge plus rien,
c'est un commentaire.

L'écran y tient : un dossier sans critère ne propose pas de formulaire, et un
bandeau compte les dossiers ouverts qui n'en ont pas encore. Cet avertissement
arrive **tant que le critère est encore posable**, et non le jour de l'échéance
où il serait trop tard. Une fois une première évaluation rendue, le critère se
fige : le modifier réécrirait la base sur laquelle un verdict passé s'est
prononcé.

#### Les trois verdicts, et ce que chacun déclenche

| Verdict | Le dossier | Le travail qui suit |
| --- | --- | --- |
| **Efficace** | se clôture | rien — le critère est atteint |
| **Partiellement** | repart en traitement | la cause était la bonne, l'action ne va pas assez loin : **on en ajoute une** |
| **Inefficace** | est rouvert | la cause n'était pas celle-là : **on reprend l'analyse** (5 Pourquoi, Ishikawa) |

Le troisième état est la raison d'être de cet onglet. Une action qui fait passer
le rebut de 4,2 % à 2,5 % pour une cible de 2 % n'est ni efficace ni inefficace ;
avec deux réponses seulement, il fallait mentir — et c'est « oui » qu'on
choisissait, parce que ça fait avancer le tableau de bord.

« Partiellement » et « inefficace » ramènent tous deux le dossier au travail, et
ce n'est pas une raison de les confondre : ce sont deux travaux différents. Le
bouton choisi annonce sa conséquence **avant** l'enregistrement, pour que
personne ne découvre après coup que son dossier s'est rouvert.

#### Le relevé

Trois champs libres — **avant**, **cible**, **constaté** — et le constaté est
obligatoire : un verdict sans mesure est une opinion. Du texte court plutôt que
des nombres, parce que l'unité, la tolérance et l'assiette font partie du
relevé : « 4,2 % », « ≤ 2 g », « 0 retard sur 620 expéditions ». Aucun
indicateur n'est calculé à partir de ces trois champs ; ils se lisent.

Quand on rejuge un dossier, « avant » et « cible » sont reprises de l'évaluation
précédente — on rejuge contre les mêmes bornes, et les ressaisir de mémoire est
le meilleur moyen de les changer sans le vouloir. Le constaté, lui, repart
toujours vide : c'est la mesure du jour.

#### Pourquoi le formulaire est parfois fermé

| Message | Ce qu'il faut faire |
| --- | --- |
| *N actions restent à mener* | l'évaluation s'ouvre quand elles sont faites — juger l'effet d'une action qu'on n'a pas menée n'a pas de sens |
| *Aucune action n'est encore portée par ce dossier* | il n'y a rien dont juger l'effet : définir l'action d'abord |
| *Aucun critère d'efficacité n'est posé* | le poser sur la fiche du dossier |
| *Ce dossier est clôturé sur un verdict d'efficacité* | il ne se rejuge pas ; son historique reste lisible |
| *Ce dossier attend son action complémentaire* | il se présentera de nouveau au jugement quand elle sera menée à terme |

Deux obstacles supplémentaires ne concernent que la **clôture**, et ils sont
annoncés avant le clic plutôt que découverts au refus : un dossier qui ne porte
que des mesures d'endiguement — elles arrêtent l'effet sans supprimer la cause —
et un dossier dont une non-conformité rattachée reste ouverte.

#### L'historique, et ce qu'un auditeur y lit

Chaque évaluation s'ajoute, aucune n'écrase la précédente. La suite *inefficace,
puis partiel, puis efficace* est précisément ce qu'un auditeur cherche : elle
montre un système qui corrige. Un champ écrasé ne montrerait que la fin, et
donnerait du dossier une image de réussite immédiate qui est fausse.

Chaque ligne porte le critère **tel qu'il était** au jour du jugement, les trois
relevés, le commentaire et le nom de la personne qui a jugé — repris du jeton,
pas saisi.

#### Le tableau, et pourquoi il n'a pas de filtre

Les dossiers sont rangés **par urgence** : ce qui est à juger arrive en tête,
puis ce qui attend une action complémentaire, puis les dossiers rouverts, puis
le travail en cours, les dossiers clos en dernier. Un filtre de statut obligeait
à savoir quel statut demander pour obtenir son travail du jour ; le tri le donne
sans rien demander.

Les quatre chiffres de l'en-tête portent sur **tout** le périmètre, même quand
le tableau est borné — un bandeau le dit alors. Le taux d'efficacité ne compte
que les dossiers **déjà jugés**, et reste vide tant qu'aucun ne l'est : « 0 % »
se lirait « tout a échoué », ce qui n'est pas la même chose que « rien n'a encore
été jugé ».

#### Et l'ancienne case « efficacité vérifiée » ?

Elle existe toujours et devient la projection du verdict : *efficace* la coche,
*partiellement* et *inefficace* la décochent. Elle n'est plus saisie à la main.
Elle reste parce que l'onglet voisin l'affiche à côté du taux de récidive, et
parce que les dossiers antérieurs à cette évolution n'ont pas de verdict.

---

### Onglet « Mesure par récidive » : vérifier, des mois plus tard

#### Ce que l'onglet corrige

Le verdict rendu dans l'onglet voisin est une appréciation : datée du jour où
l'action vient d'être menée, portée par la personne qui l'a menée. Cet onglet-ci
fait répondre le terrain, et il répond plus tard.

Il affiche les deux côte à côte. **L'écart entre ce qui a été déclaré et ce qui
s'est produit** est l'information que personne n'avait — et c'est le premier
chiffre qu'un auditeur demandera.

#### Comment le taux est calculé

> **taux = 1 − (récidives après la clôture ÷ occurrences avant l'ouverture)**

Les deux périodes ont la **même durée** — six mois par défaut, ajustable à trois
ou douze. La fenêtre « avant » s'arrête à l'**ouverture** du dossier et non à sa
clôture : les non-conformités survenues pendant le traitement ont motivé
l'action, elles ne sont pas son échec.

Une récidive, c'est une non-conformité portant sur **le même produit et le même
mode de défaillance** que celle qui a déclenché la CAPA. À défaut de ce lien, la
catégorie sert de repli — beaucoup plus large — et la ligne porte alors la
mention « rapprochement par catégorie, taux indicatif ».

#### Ce que l'onglet refuse de dire, et pourquoi

| Situation | Affichage | Raison |
|---|---|---|
| Aucune occurrence avant l'ouverture | *Aucune occurrence antérieure* | On ne mesure pas une réduction à partir de zéro. Annoncer 100 % féliciterait une action dont rien ne dit qu'elle servait |
| Fenêtre encore en cours | *Trop tôt pour conclure* + décompte | Comparer deux mois observés à six mois de référence flatte systématiquement le résultat |
| Plus de récidives qu'avant | **0 %**, jamais un taux négatif | Un négatif se moyennerait avec les autres et masquerait deux dossiers corrects. L'aggravation reste visible ligne par ligne |

La **moyenne** ne porte que sur les dossiers mesurés. Y mêler ceux en observation
la ferait bouger au rythme des clôtures plutôt qu'à celui des résultats.

Enfin, si le bandeau annonce que le périmètre a été tronqué, la moyenne ne couvre
pas tout l'historique : seuls les dossiers les plus récents sont mesurés. C'est
dit plutôt que tu.

#### Les dossiers qui n'apparaissent pas

Une CAPA née d'un audit, d'une réclamation ou d'une décision interne n'a rien à
quoi se comparer : elle est écartée du tableau plutôt que de recevoir des
récidives imaginaires.

---

## Matrice de compétences

> Décision : [ADR 0098](../../adr/0098-matrice-de-competences-editable.md).

### Ce qu'elle montre

Opérateurs en lignes, rangés par atelier. Compétences en colonnes, chacune avec
sa catégorie et son **formateur**. Un niveau à l'intersection :

| Niveau | Sens |
| --- | --- |
| 0 | Non formé |
| 1 | Débutant |
| 2 | Autonome |
| 3 | Expert |
| 4 | Formateur |

La figure se lit **en colonne** : une compétence est **à risque** quand moins de
deux personnes y sont autonomes (niveau 2 ou plus). Le pied de colonne compte les
personnes autonomes et porte ⚠ sous ce seuil ; le panneau « Compétences à
risque » dit qui la tient seule (« Seule personne autonome : … ») ou que
personne ne la tient, et nomme le formateur vers qui se tourner. La bascule
**Mettre en évidence les risques** estompe tout le reste.

En haut : opérateurs, compétences suivies, compétences à risque, compétences
avec au moins un formateur (niveau 4). La colonne **Moy.** donne le niveau moyen
de chaque opérateur sur les compétences affichées. On cherche un opérateur par
son nom et on filtre par atelier ou par catégorie.

### Changer un niveau

Cliquez sur une case : le niveau avance de 0 à 4, puis revient à 0. Au clavier,
les flèches, Début et Fin passent d'une case à l'autre, Entrée ou Espace font
tourner le niveau. Chaque niveau posé **est l'évaluation de compétence** de la
personne : le dossier de formation affiche la même valeur, et l'écriture est
tracée dans le journal d'audit. Si le serveur refuse, la case reprend sa valeur.

Une case sans évaluation se lit « Non formé » : l'opérateur a été inscrit dans
la matrice, il n'a pas encore été formé.

### Ajouter et retirer

- **Opérateur** : une personne choisie dans l'annuaire, et son atelier. Ses
  évaluations existantes la suivent.
- **Compétence** : un intitulé, une catégorie et un **formateur obligatoire**,
  choisi dans l'annuaire. Une compétence déjà au catalogue sous ce nom est
  reprise avec ses évaluations.
- **Retirer** (croix sur la ligne ou sur l'en-tête, après confirmation) ne
  détruit rien : les évaluations restent au dossier de formation, la compétence
  reste au catalogue.

Ces actions demandent le droit **Tenir la matrice de compétences**
(`training.manage`), donné par défaut à l'administrateur du client, au directeur
qualité et au manager qualité. Les autres membres lisent la matrice.

### Les ateliers

L'administrateur du client règle la liste dans **Administration → Ateliers**
(`/admin/competences`, ou le bouton « Ateliers » de la matrice) : ajouter,
renommer, ordonner, retirer, puis enregistrer. Sans réglage, la plateforme
propose Usinage, Assemblage, Qualité, Maintenance. Retirer un atelier ne
réaffecte personne : les opérateurs qui y sont rangés le gardent.

### Bonnes pratiques

- **Visez deux autonomes par compétence**, et un formateur désigné qui puisse
  les former.
- **Regardez les colonnes rouges d'abord.** Ce sont elles qui portent le risque.
- **Rangez vos compétences par catégorie** : le filtre et le tri s'en servent.
