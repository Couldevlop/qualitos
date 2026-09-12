# ADR 0068 — Un livrable APQP se coche, se prouve, et son formulaire dépend de son genre

- **Statut** : Accepté
- **Date** : 2026-09-12
- **Owners** : @Couldevlop
- **Portée** : Module APQP (livrables, pièces jointes, dossier PPAP), référentiel
  d'amorçage, stockage objet
- **S'inscrit dans** : ADR 0066 (« le cycle APQP appartient au tenant »), ADR 0061
  (preuves d'étape PDCA), ADR 0065 (autorisation décidée avant la lecture du corps)

## Contexte

Le cycle APQP appartenait au tenant depuis l'ADR 0066, mais un livrable n'était
qu'un **libellé**. L'écran le précédait d'une icône `check_circle` purement
décorative.

Trois conséquences :

1. **L'écran affirmait ce qu'il ne savait pas.** Quarante-six livrables tous
   précédés d'une coche se lisent comme « tout est fait ». Rien, dans la base, ne
   disait où l'on en était.
2. **Le document qui prouve le livrable n'avait nulle part où se ranger.** Ces
   pièces — spécification, rapport de revue de conception, formulaire
   d'approbation PPAP — arrivent par courriel, en `.docx`, et restaient dans une
   boîte aux lettres. L'auditeur les réclame, et il faut alors les retrouver.
3. **Le dossier PPAP n'existait pas.** Le référentiel marque d'un astérisque les
   livrables qui le composent (« this deliverable is a PPAP element ») ; le cycle
   ne portait pas cette marque, et aucune vue ne disait si le dossier était
   complet.

S'y ajoutait une demande explicite : remplacer les libellés d'amorçage par ceux du
document de référence fourni (`docs/APQP delivrables.docx`), en anglais.

## Décisions

### 1. Quatre genres fermés, et non un formulaire par livrable

`ApqpDeliverableKind` : `ATTACHMENT`, `MODULE_LINK`, `DATA_ENTRY`, `CHECKLIST`.
Le genre est porté par le livrable, donné par le serveur, et décide de ce que son
popup demande.

**Alternative écartée : un formulaire par livrable.** C'était la demande à la
lettre — « c'est vraiment spécifique à chaque livrable ». Le référentiel en compte
quarante-six, et un tenant en ajoute : cinquante formulaires codés un par un
auraient figé le module, contredit l'invariant §22.11 (« rien de sectoriel en
dur »), et serait devenu le travail d'un intégrateur à chaque ajout de livrable.
Or ces livrables ne diffèrent que par **la nature de ce qu'ils produisent** : un
document, un enregistrement déjà tenu ailleurs dans QualitOS, des mesures, une
liste de points à acquitter. Le genre capture cette différence, et un cinquième
genre reste possible — il faut alors l'écrire dans l'énumération, dans la
contrainte de la base et dans le formulaire, ce qui est précisément ce qu'on veut :
une décision, pas un effet de bord.

**Alternative écartée : déduire le genre du libellé.** Reconnaître « Control plan »
marcherait sur le référentiel et sur rien d'autre, et personne ne comprendrait
pourquoi son propre libellé n'ouvre pas le même formulaire.

### 2. Un seul `data jsonb`, validé par genre, et non une colonne par genre

Les sous-points d'une checklist et les mesures d'une saisie tiennent dans la même
colonne, sous deux formes fermées. Ce qui empêche cette colonne d'être un
fourre-tout n'est pas son type mais `ApqpDeliverableDataValidator`, qui refuse en
**422** toute forme qui ne correspond pas au genre du livrable.

Écriture du JSON **à la main**, relecture **au mapper** : on contrôle ce qui entre
en base, on ne se défie pas de ce qu'on en ressort. Faire écrire un sérialiseur
ferait dépendre le contenu stocké d'une configuration tenue ailleurs, qui peut
changer sans qu'on s'en avise.

**Alternative écartée : une colonne par genre.** Trois colonnes nulles sur quatre à
chaque ligne, et une migration à chaque genre ajouté.
**Alternative écartée : un JSON libre.** C'est le fourre-tout que le validateur
évite : six mois plus tard, personne ne sait plus quelles formes existent en base.

Changer le genre d'un livrable **vide son contenu** et son renvoi : une liste de
points lue comme une table de mesures ne veut rien dire, et la garder « au cas où »
produirait un état qu'aucun formulaire ne sait rendre.

### 3. Un renvoi est vérifié dans le tenant avant d'être accepté

`MODULE_LINK` désigne un enregistrement de FMEA, de plan de surveillance, de cycle
PDCA ou de CAPA. `ApqpLinkResolver` vérifie son existence **dans le tenant** et
refuse en 422 sinon. Un lien mort est pire qu'une absence de lien : il affirme
qu'une preuve existe. Et un livrable `MODULE_LINK` ne peut pas être coché sans son
enregistrement — coché à vide, il affirmerait qu'une AMDEC existe sans dire
laquelle.

Ces quatre modules seulement : ce sont ceux dont l'identifiant est stable et
vérifiable dans ce même service.

### 4. Les pièces reprennent le motif des preuves PDCA, avec trois écarts

Métadonnée en base, binaire en stockage objet sous clé tenantisée, URL présignée à
la lecture, propriétaire déclaré au balayeur d'orphelins — rien de nouveau, et
c'est le but : ce motif est éprouvé (ADR 0061).

Trois écarts voulus :

- **cinq pièces par livrable** au lieu d'une sur une étape PDCA : un dossier PPAP
  se compose de pièces distinctes, et les forcer dans un seul fichier reviendrait à
  demander à l'utilisateur de les agréger lui-même ;
- **le `.docx` en tête de la liste blanche** : c'est LA forme sous laquelle ces
  livrables circulent. Qu'un document bureautique reste modifiable après coup est
  assumé, comme pour les preuves PDCA — c'est la pièce réelle, et le journal
  d'audit fige ce qui a été versé, quand et par qui ;
- **cinquante mégaoctets par tenant**, le cycle étant l'unité qu'un tenant possède
  (il n'en a qu'un).

Les trois gardes communes — liste blanche de types, octets magiques, nom de fichier
assaini — sortent dans `UploadedBinaryGuard` plutôt que de devenir une **quatrième**
copie. Les preuves PDCA, les preuves CAPA et les photos de NC portent encore la
leur, écrite avant celle-ci ; elles pourront s'y replier. C'est toujours la copie
oubliée qui laisse passer l'exécutable renommé.

### 5. L'heure vient du serveur, l'acteur du jeton ; décocher efface les deux

`done_at` et `done_by` ne sont jamais acceptés du corps de la requête (règle 18.2
§2). Et décocher **efface** qui et quand : garder la trace d'un achèvement retiré
la rendrait fausse, et c'est cette trace que l'auditeur lit.

### 6. Le dossier PPAP est une VUE du cycle

La section sous le schéma agrège les livrables `ppap = true` du cycle. Aucune liste
propre.

**Alternative écartée : les dix-neuf éléments PPAP du manuel, tenus à part.** Deux
listes à tenir d'accord divergent dès que le tenant adapte son cycle — et c'est le
cycle qui fait foi, puisqu'il lui appartient (ADR 0066).

Le compte (`ppapDone` / `ppapTotal`) est calculé **par le serveur** et voyage avec
le cycle : deux vues du même cycle doivent afficher le même chiffre, et la règle
changera le jour où « acquis » voudra dire « coché **et** prouvé ».

Conséquence sur le contrat : cocher un livrable rend le **cycle entier** et non la
seule phase, puisque le compte en dépend. Laisser l'écran recomposer ce compte
l'amènerait à le deviner faux.

### 7. La reprise supprime les cycles non touchés, elle ne les réécrit pas

Le référentiel d'amorçage a changé. La V127 **supprime** les cycles que personne
n'a adaptés : l'amorçage étant paresseux (ADR 0066 §2), la prochaine ouverture de
l'écran les reconstruit depuis le code.

**Alternative écartée : réécrire ligne à ligne dans la migration.** Deux
définitions du référentiel auraient existé — celle du code et celle de la migration
— et elles auraient divergé au premier ajustement.

« Non touché » demande **deux** conditions, et les deux sont nécessaires : les cinq
titres et les quarante-six libellés de l'ancien référentiel, et aucune ligne dont
`updated_at` ait bougé. Un tenant peut avoir renommé une phase sans rien d'autre,
ou reformulé un seul libellé en laissant les titres intacts — ce second cas est le
plus traître, et il a son banc sur vrai PostgreSQL.

Pour qui a adapté son cycle et veut malgré tout la nouvelle liste :
`POST /api/v1/apqp/phases/reset`, destructif, confirmé deux fois à l'écran.

### 8. Les libellés du référentiel ne passent pas par la traduction

Ils sont écrits une fois, dans la langue du document de référence, et appartiennent
au tenant dès la première copie. Un livrable normatif traduit librement n'est plus
le même livrable : « Control plan » désigne un document précis, pas un plan de
contrôle quelconque. C'est au tenant de les traduire s'il le souhaite — il en est
propriétaire.

## Conséquences

- Migrations **V126** (colonnes d'achèvement, genre, marque PPAP, table des pièces)
  et **V127** (reprise des cycles non touchés).
- Un cinquième genre de livrable coûte trois écritures : l'énumération, la
  contrainte de la base, le formulaire. C'est voulu.
- Les preuves PDCA/CAPA/NC gardent leur propre copie des gardes binaires : dette
  identifiée, sans urgence, à solder quand l'une d'elles sera rouverte.
- `ApqpService` rend désormais deux formes : `CycleResponse` pour la lecture, la
  complétion et la réinitialisation ; `PhaseResponse` pour les écritures qui ne
  changent pas le compte PPAP.
