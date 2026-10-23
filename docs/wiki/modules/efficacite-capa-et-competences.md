# Efficacité des CAPA et matrice de compétences

[← Retour à l'index](../README.md) · Routes : **`/capa/efficacite`** et **`/training/competences`**

Deux écrans qui répondent à des questions qu'on ne pouvait pas poser à la
plateforme jusqu'ici : *nos actions correctives servent-elles à quelque chose ?*
et *qui sait faire quoi, chez nous ?*

---

## Efficacité des CAPA

### Ce que l'écran corrige

Le dossier CAPA porte une case **« efficacité vérifiée »**. C'est une opinion :
datée du jour de la clôture, portée par la personne qui a mené l'action, et jamais
revue ensuite. Cet écran fait répondre le terrain, et il répond plus tard.

Il affiche les deux côte à côte. **L'écart entre ce qui a été déclaré et ce qui
s'est produit** est l'information que personne n'avait — et c'est le premier
chiffre qu'un auditeur demandera.

### Comment le taux est calculé

> **taux = 1 − (récidives après la clôture ÷ occurrences avant l'ouverture)**

Les deux périodes ont la **même durée** — six mois par défaut, ajustable à trois
ou douze. La fenêtre « avant » s'arrête à l'**ouverture** du dossier et non à sa
clôture : les non-conformités survenues pendant le traitement ont motivé
l'action, elles ne sont pas son échec.

Une récidive, c'est une non-conformité portant sur **le même produit et le même
mode de défaillance** que celle qui a déclenché la CAPA. À défaut de ce lien, la
catégorie sert de repli — beaucoup plus large — et la ligne porte alors la
mention « rapprochement par catégorie, taux indicatif ».

### Ce que l'écran refuse de dire, et pourquoi

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

### Les dossiers qui n'apparaissent pas

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
