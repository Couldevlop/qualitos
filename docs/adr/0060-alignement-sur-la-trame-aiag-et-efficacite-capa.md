# ADR 0060 — Le PFMEA et le control plan alignés sur la trame réelle, et l'efficacité CAPA mesurée

- **Statut** : Accepté
- **Date** : 2026-08-21
- **Owners** : @Couldevlop
- **Source** : `docs/QUALITOS BACKLOG.xlsx`, feuilles 2 et 3 — un PFMEA et un control
  plan réels de faisceau électrique aéronautique

## Contexte

Le lot précédent (ADR 0059) a livré le référentiel produit, le PFMEA rattaché et
le control plan. Confrontés à une trame **réelle**, sept colonnes manquaient — et
l'une des sept n'était pas un oubli mais une **erreur de type**.

Le même document de backlog demandait par ailleurs que l'efficacité des CAPA soit
mesurée, ce que le dossier CAPA ne faisait pas : il portait une case
« efficacité vérifiée », c'est-à-dire une opinion.

## Décisions

### 1. La taille d'échantillon est du texte, pas un nombre

`sample_size` était un `INTEGER`. Les valeurs réelles sont
« 100 % (automatisé) », « 5 pièces au réglage puis 1 sur 50 », « 1 essai
destructif par lot ». Aucune ne tient dans un entier.

**Ce que le mauvais type coûtait :** l'utilisateur devait tronquer la règle ou
l'écrire dans un autre champ — le plus souvent la fréquence — où personne ne la
chercherait. Un modèle qui force à mentir sur la donnée finit par produire des
données fausses.

**Le retour arrière n'est pas symétrique.** Une fois qu'une taille vaut
« 100 % (automatisé) », elle ne redevient pas un entier : revenir suppose de
restaurer la sauvegarde prise avant le déploiement, pas de rejouer une migration
inverse. C'est exactement le cas que couvre le vidage de sûreté de `deploy.sh`.

### 2. Quatre colonnes de plus au control plan, et pourquoi chacune

| Colonne | Ce qu'elle apporte |
| --- | --- |
| `sop_reference` | D'où vient le contrôle : la procédure appliquée au poste |
| `input_output` | Ce qu'on surveille : une entrée, ou une sortie |
| `who_measures` | Qui — ou quoi — mesure |
| `recording_location` | Où la preuve est conservée |

**`input_output` n'est pas un doublon de `characteristic_type`.** Les deux axes se
croisent : la température d'un four est une entrée de procédé, le diamètre d'un
alésage une sortie produit. La distinction porte une lecture qui vaut le champ :
contrôler une sortie constate un défaut déjà fait, contrôler une entrée
l'empêche. Un plan qui ne surveille que des sorties trie, il ne maîtrise pas.

**`recording_location` est la colonne de l'auditeur.** Elle dit où aller chercher
la preuve que le contrôle a eu lieu. Sans elle, le plan décrit une intention.

### 3. « SOP # » est du texte, pas encore un lien

Le référentiel de procédures internes existe (ADR 0057) et serait la cible
naturelle. Le champ reste pourtant textuel.

**Pourquoi :** un control plan cite couramment des procédures antérieures à la
plateforme. Exiger qu'elles y soient d'abord saisies bloquerait la mise en
service — et la première victime serait la donnée qu'on cherche à capturer. Le
jour où le référentiel sera complet, le champ pourra devenir une clé étrangère
sans perte : les références sont déjà là, sous la même forme.

### 4. Le PFMEA porte ce qui a été FAIT, et pas seulement ce qui était recommandé

`actions_taken` et `actions_taken_at` comblent le trou entre l'action recommandée
et la nouvelle cotation. Sans eux, un PFMEA montre une note qui a baissé sans que
rien n'explique pourquoi — exactement ce qu'un auditeur relève.

`action_owner_name` accompagne l'identifiant d'utilisateur : la colonne « Resp. »
de la trame attend « Manufacturing Eng », « Quality Eng » — souvent un **service**
et non une personne. Même choix que le nom d'assigné du dossier CAPA.

### 5. L'efficacité d'une CAPA se mesure, elle ne se déclare pas

Le taux compare deux périodes de MÊME durée : celle qui précède l'ouverture du
dossier, celle qui suit sa clôture.

```
taux = 1 − (récidives après clôture ÷ occurrences avant ouverture)
```

borné à [0, 1], arrondi au point le plus proche.

**La fenêtre « avant » s'arrête à l'OUVERTURE**, pas à la clôture : les
non-conformités survenues pendant le traitement ont motivé l'action, elles ne
sont pas son échec.

**Ce que le calcul refuse de dire compte autant :** sans occurrence antérieure il
n'y a pas de réduction à mesurer, et tant que la fenêtre n'est pas écoulée,
comparer une période partielle à une période entière flatterait le résultat. Dans
les deux cas le taux est absent, les décomptes restent rendus.

**Une CAPA suivie de plus de récidives vaut zéro, jamais un taux négatif** : un
négatif se moyennerait avec les autres et masquerait deux dossiers corrects.
L'aggravation reste visible, dossier par dossier.

**Rien n'est stocké.** Le taux se recalcule à chaque lecture : le figer serait le
fausser, puisqu'une récidive survenue demain doit corriger le verdict
d'aujourd'hui.

### 6. La sécurité, en amont, pendant et en aval

**En amont** — les longueurs sont bornées côté HTTP (`@Size`) **et** dans le
domaine. Le moteur de propositions de révision écrit des lignes sans passer par
le contrôleur : sa validation ne le protège pas. Sans garde au domaine, une
valeur trop longue atteignait la base et revenait en erreur d'intégrité — un 500
là où l'appelant méritait un refus nommé.

**Pendant** — la lecture d'efficacité coûte deux comptages par dossier. Sans
borne, un tenant aux dix mille CAPA closes déclencherait vingt mille requêtes sur
un simple GET : un épuisement de ressources qu'un utilisateur légitime peut
provoquer sans le vouloir. Le balayage est plafonné à 500 dossiers, les plus
récents d'abord — ceux dont le verdict peut encore bouger.

**En aval** — la troncature est **dite** (`truncated`), jamais silencieuse. Une
moyenne calculée sur une partie du périmètre et présentée comme complète serait un
mensonge par omission, et cet écran finit en revue de direction.

### 7. La matrice de compétences se lit dans les deux sens

Compétences en lignes groupées par famille, collaborateurs en colonnes, un
niveau à l'intersection. Une colonne montre ce qu'une personne couvre ; une
**ligne** montre qui sait faire quoi — et une seule case remplie sur toute une
rangée signale une compétence qui ne tient qu'à une personne. Ce n'est pas une
donnée, c'est un risque d'organisation, et la figure le rend visible sans qu'on
le cherche. L'écran le porte par du **texte**, pas seulement par une couleur :
une couleur seule ne se lit ni au clavier, ni en daltonisme, ni à l'impression.

**Une case jamais évaluée reste vide.** La confondre avec un niveau zéro
affirmerait une incompétence que personne n'a constatée, et fausserait toute
lecture de couverture. La distinction traverse la pile entière — domaine, JSON,
CSS — et un banc la garde à chaque étage.

**L'échelle affichée est celle qui est stockée**, de zéro à quatre, chacun nommé.
La trame papier note de un à cinq ; l'afficher supposerait de décaler les valeurs
enregistrées, c'est-à-dire de montrer autre chose que ce que le serveur détient.

**Le nom de la personne est stocké sur l'évaluation** (`V118`), faute d'annuaire.
Une réévaluation qui ne le transmet pas ne l'efface pas : un outil tiers ne doit
pas vider les en-têtes de colonnes. À défaut de nom, l'identifiant est abrégé —
l'inventer serait pire que l'abréger.

## Conséquences

- Migrations `V116` (control plan) et `V117` (PFMEA), rejouées sur un PostgreSQL
  réel par `PartialIndexesOnPostgresTest`.
- `ControlPlanLine.describe(...)` prend désormais un objet `Details` au lieu de
  dix-sept paramètres : au-delà d'une poignée, deux arguments de même type
  finissent par s'échanger sans que le compilateur ne bronche, et la tolérance
  basse se retrouve en tolérance haute.
- Nouveau point de lecture `GET /api/v1/capa/effectiveness?months=6`, ouvert à
  tout utilisateur authentifié du tenant — il ne révèle rien que la liste des
  CAPA ne révèle déjà.
- Migration `V118` : le nom de la personne évaluée, et
  `GET /api/v1/training/competencies/matrix` pour la matrice du tenant, à côté
  de la lecture par utilisateur qui existait déjà.
- Deux écrans : `/capa/efficacite` et `/training/competences`, et leurs entrées
  de navigation. Le catalogue de compétences est borné à 500 lignes : au-delà,
  ce n'est plus une matrice mais un export.

## Ce que ce lot ne tient pas

- **Les cinq modules maquettés** (continuité d'activité, revue de contrat, coût
  de la qualité, boîte à idées, suivi ESG) ne sont pas commencés. Chacun est un
  module complet ; ils restent séquencés dans
  `docs/ECART-nouveaux-modules-qms.md`.
- **« SOP # » reste un texte libre**, pas un lien vers le référentiel de
  procédures internes (cf. décision 3).
