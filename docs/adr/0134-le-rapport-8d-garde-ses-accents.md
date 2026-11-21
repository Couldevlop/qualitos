# ADR 0134 — Le rapport 8D garde ses accents

- **Statut** : accepté
- **Date** : 2026-11-21
- **Portée** : rendu PDF du rapport 8D, jeu de caractères des polices standard
- **S'appuie sur** : l'ADR 0129 (langue figée à l'émission), dont il lève la
  dette consignée sur les accents

## Contexte

L'ADR 0129 a laissé une dette explicite : « le PDF **dégrade encore les
accents** (« Equipe », « Non-conformite ») ». Le rapport qui part chez le client
écrivait donc « Description du probleme » et « Prevention de la recidive » là où
l'instantané porte « Description du problème » et « Prévention de la récidive ».

Deux raisons avaient été données, et il faut les séparer.

**La première est fausse.** Le code la posait ainsi : « les polices standard ne
savent pas encoder un caractère arbitraire ». C'est vrai d'un caractère
arbitraire, et faux du français. Les trois polices du document — Helvetica,
Helvetica-Bold, Courier — sont en WinAnsiEncoding, qui porte les accents, la
cédille, la ligature « œ », l'apostrophe typographique et le tiret cadratin. Le
jeu a été relevé sur les polices elles-mêmes : des **219 caractères de
windows-1252 à partir de l'espace, les trois acceptent tout sauf U+007F**, qui
est un caractère de commande. Ce qui fait réellement échouer le rendu est
ailleurs — « ≤ », « → », un idéogramme : PDFBox refuse alors d'écrire et lève
une exception. Le filtre protégeait donc d'un risque réel, mais il était
beaucoup trop large : il abîmait ce qui passait très bien.

**La seconde était la vraie contrainte, et elle ne tient plus.** L'empreinte
scellée porte sur les octets du PDF, et `EightDService` **re-rend le document à
chaque téléchargement** pour comparer son SHA-256 à l'empreinte. Changer le
rendu rend donc intéléchargeable tout rapport déjà émis — refus explicite, « le
rendu ne correspond plus à l'empreinte scellée ». C'est ce qui imposait de
versionner le rendu : une colonne sur le rapport, les anciens en v1, les
nouveaux en v2, et le chemin v1 à garder indéfiniment.

Or il n'y a **aucun rapport émis à protéger**. Relevé en préproduction le
2026-11-21 avant remise à zéro : 9 rapports 8D, tous à l'état `DRAFT`, aucun
`ISSUED` ; et il n'existe pas d'environnement de production. Un scellé sans
scellement n'est rien à garder.

## Décision

**Le filtre ne dégrade que ce que les polices ne savent pas écrire.**

1. Le jeu accepté est **calculé**, une fois pour toutes, depuis `windows-1252`,
   et non écrit à la main. Une table de deux cents entrées tenue à la main se
   trompe, et elle se tromperait en silence : le rendu échouerait sur un
   caractère oublié, au moment d'émettre un rapport.
2. Ce qui sort de ce jeu, et les caractères de commande, deviennent `?`. Perdre
   un signe vaut mieux que perdre le document : une exception remontée à
   l'utilisateur ferait échouer l'émission entière pour un caractère.
3. **Pas de rendu versionné.** La décision n'est prise que parce qu'aucun
   rapport n'est scellé. Si l'un l'avait été, il aurait fallu la colonne et les
   deux chemins — ce constat est la condition de cette décision, pas un détail
   de contexte.

Le rendu reste **déterministe** : même instantané, mêmes octets. Le filtre ne
lit ni l'horloge, ni la base, ni une police installée.

## Conséquences

- Un rapport neuf porte le texte de son instantané tel quel : « Équipe »,
  « Description du problème », « Écart constaté », « Joint usé ». Les libellés
  comme les **données** — le filtre s'appliquait aux deux.
- Le 8D rejoint ce que font déjà `PdfBoxClaimReportRenderer` et
  `SupplierNcReportRenderer`, qui gardent les accents : il était l'exception,
  pas la règle.
- L'ADR 0129 disait aussi que « la ligature « œ » est évitée dans les titres de
  discipline : les polices standard du PDF ne la portent pas ». **C'est faux** :
  les trois polices l'acceptent. Elle sortait en « ? » parce que l'ancien filtre
  n'avait pas de cas pour elle et tombait sur son défaut. La raison invoquée
  n'existe donc plus. Le titre du D6 écrit encore « Mise en oeuvre » : signalé,
  non traité ici, c'est une donnée d'instantané et non un défaut de rendu
  (règle 18.2 #11).
- `PdfBoxDashboardRenderAdapter` dégrade toujours les accents, de la même façon
  et pour la même raison fausse. Dette consignée, non traitée ici : l'export de
  tableau de bord est signé lui aussi, et sa situation est à relever avant de
  la lever.
- Si un 8D venait à être émis entre cette décision et son déploiement, il
  deviendrait intéléchargeable. Les deux vont ensemble.

## Tests d'invariant

- `PdfBoxEightDRenderAdapterTest.les_accents_du_contenu_sortent_tels_quels` :
  un instantané accentué rend « Écart constaté », « cédille ç, ligature œuvre »,
  « où l'opérateur vise l'arête » — lus dans le PDF, pas dans la source.
- `PdfBoxEightDRenderAdapterTest.un_caractere_hors_du_jeu_des_polices_est_remplace_sans_casser_le_rendu` :
  « ≤ », « → » et un idéogramme deviennent `?`, le document sort non vide, et le
  rendu n'échoue pas.
- `PdfBoxEightDRenderAdapterTest.l_arete_de_poisson_porte_son_enonce_et_ses_branches`
  et `...la_descente_des_pourquoi_numerote_ses_rangs_et_nomme_la_cause_racine` :
  les figures rendent « Fuite au presse-étoupe », « Joint usé », « Fuite
  constatée », « Absence de plan préventif ».
- `PdfBoxEightDRenderAdapterTest.deux_rendus_du_meme_rapport_donnent_exactement_les_memes_octets`
  et les trois bancs « un rapport scellé avant X rend les mêmes octets qu'avant »
  : le déterminisme et la stabilité des formes anciennes d'instantané tiennent.
