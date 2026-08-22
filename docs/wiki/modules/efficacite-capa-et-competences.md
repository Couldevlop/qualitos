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

### Ce qu'elle montre, et dans quel sens la lire

Compétences en lignes, groupées par famille. Collaborateurs en colonnes. Un
niveau à l'intersection.

La lecture **en colonne** dit ce qu'une personne couvre. La lecture **en ligne**
dit qui sait faire quoi — et c'est celle-là qui intéresse : une seule case remplie
sur toute une rangée signale une compétence qui ne tient qu'à une personne. Ce
n'est pas une donnée, c'est un risque d'organisation. La ligne porte alors la
mention **« un seul détenteur »**, et l'interrupteur en haut de page ne garde que
ces lignes-là.

### Une case vide n'est pas un zéro

- **Vide (—)** : cette personne n'a jamais été évaluée sur cette compétence. On
  ne sait pas.
- **0** : elle a été évaluée, et le niveau constaté est nul.

Les confondre reviendrait à affirmer une incompétence que personne n'a constatée.
La distinction tient d'un bout à l'autre de la chaîne, jusqu'à la couleur de la
case.

### L'échelle

Cinq niveaux, de 0 à 4 : *Aucun, Sensibilisé, Pratiquant, Autonome, Expert*. Les
trames papier notent souvent de 1 à 5 ; afficher cette échelle-là supposerait de
décaler les valeurs enregistrées, c'est-à-dire de montrer autre chose que ce que
la plateforme détient.

### D'où viennent les colonnes

Des **évaluations** : seules les personnes évaluées au moins une fois
apparaissent. Le nom affiché est celui saisi au moment de l'évaluation — la
plateforme n'a pas d'annuaire. À défaut, un identifiant abrégé tient lieu
d'en-tête : illisible, mais moins trompeur qu'un nom inventé.

Une réévaluation qui ne transmet pas le nom **n'efface pas** celui déjà connu.

### Bonnes pratiques

- **Classez vos compétences par famille.** Une matrice sans groupes se lit comme
  une liste, et perd ce qui fait sa valeur : la comparaison au sein d'un métier.
- **Évaluez à zéro quand c'est vrai.** Un zéro assumé vaut mieux qu'une case vide
  qui laisse croire à un oubli.
- **Regardez les lignes avant les colonnes.** Ce sont elles qui portent le risque.
