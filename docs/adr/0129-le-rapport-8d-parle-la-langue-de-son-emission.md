# ADR 0129 — Le rapport 8D parle la langue de son émission, et sa prévention se lit au D7

- **Statut** : accepté
- **Date** : 2026-11-20
- **Portée** : rapport 8D (instantané, assemblage, rendu PDF), écran de la fiche d'écart

## Contexte

Le rapport 8D est le document qui part chez le client. Il sortait **en anglais**
sous une interface française : « Team », « Problem description », « Immediate
containment actions », mais aussi les codes du modèle en clair — « Category :
PRODUCT », « Severity : MAJOR », « Status : DONE ». L'ADR 0071 annonçait
pourtant un texte produit « en français par le serveur », avec pour dette
assumée qu'un tenant anglophone recevrait un PDF français : le code avait dérivé
de sa propre décision, et personne ne l'avait vu parce que l'écran autour, lui,
est traduit.

Deux obstacles expliquent que la correction ne soit pas un simple remplacement
de chaînes :

1. **L'empreinte scellée porte sur les octets du PDF** (`EightDService.emettre`
   calcule le SHA-256 du rendu, le signe en ML-DSA et l'ancre). Au
   téléchargement, le service **re-rend** l'instantané et refuse le fichier si
   l'empreinte ne retombe pas. Changer les mots du rendu rendrait donc
   intéléchargeable tout rapport déjà émis. L'ADR 0124 avait contourné la
   difficulté en n'ajoutant que des éléments absents des anciens instantanés ;
   une traduction, elle, réécrit l'existant.
2. **Le D7 se déclarait sans contenu en se contredisant.** L'écran avait été
   corrigé — le D6 est la discipline de la *preuve*, le D7 celle de la
   *prévention* —, mais l'assembleur du document tabulait encore les actions
   préventives sous le D6 et ne lisait au D7 que l'AMDEC procédé et le plan de
   surveillance. Un écart dont l'équipe avait décidé une prévention, sans
   toucher au référentiel, produisait un rapport marqué « partiel —
   disciplines sans contenu : D7 » qui imprimait cette prévention une page plus
   haut. Par le parcours normal, **aucun 8D ne pouvait être complet**.

## Décision

**1. Le document est écrit en français, et sa langue est figée dans
l'instantané.** `EightDSnapshot` porte un champ `langue` (`"fr"` pour tout
rapport neuf). Le rendu PDF lit ce champ pour choisir ses propres mots — titre,
en-tête, encadré de la non-conformité, mention « partiel », pied d'intégrité. Un
instantané relu **sans** langue est un rapport scellé avant cette décision : il
se rend exactement comme il a été émis, et son empreinte continue de tomber
juste. C'est la réponse que l'ADR 0071 annonçait pour un futur tenant
anglophone : figer la langue **à l'émission**, jamais traduire à la lecture.

**2. Les codes du modèle sont mis en mots.** Catégorie, gravité, origine, nature
et état d'action portent les libellés de l'écran, mot pour mot. Un code absent
de la table se rend **tel quel** : une valeur ajoutée au modèle sans passer par
là doit se voir dans le document plutôt que d'y disparaître.

**3. Les actions préventives sont au D7.** Le D6 garde son récit — ce qui a été
mené, quand, et avec quelles preuves — et n'a plus de tableau d'actions. Le D7
porte le tableau des actions préventives, puis l'AMDEC et les plans de
surveillance ; il est « servi » dès qu'il a l'un ou l'autre. Le document se
présente de nouveau comme l'écran (ADR 0124).

## Conséquences

- Les rapports **déjà émis** sont inchangés, octet pour octet : banc dédié.
- Un écart traité avec au moins une action préventive produit désormais un 8D
  **complet** ; sans prévention décidée ni référentiel touché, le D7 dit ce qui
  manque, et le rapport reste partiel — ce qui est le constat juste.
- Le PDF **dégrade encore les accents** (« Equipe », « Non-conformite ») : le
  rendu s'appuie sur les polices standard et un filtre WinAnsi commun à tous les
  rapports. Le lever demande le même garde-fou que ci-dessus, le filtre étant
  appliqué aux **données** des anciens instantanés comme aux libellés. Dette
  consignée, non traitée ici ; l'écran, lui, affiche le texte accentué.
- La ligature « œ » est évitée dans les titres de discipline : les polices
  standard du PDF ne la portent pas, et elle sortait en « ? ».
- Le libellé `Tenant:` du pied de page reste tel quel : c'est un identifiant
  technique, pas une phrase lue par le client.

## Tests d'invariant

- `PdfBoxEightDRenderAdapterTest` : un instantané en français rend « Rapport
  8D », « Emis le », « RAPPORT PARTIEL » et ne porte plus aucun des mots
  anglais ; **un instantané sans langue garde ses mots d'alors** ; deux rendus
  du même rapport donnent les mêmes octets.
- `EightDSnapshotAssemblerTest` : l'instantané neuf porte `langue = "fr"` ; le
  D3 n'affiche que l'endiguement et **le D7 que la prévention**, le D6 n'ayant
  plus de tableau ; le D7 annonce dans sa provenance le nombre d'actions
  préventives ; un état d'action inconnu se rend tel quel.
- `EightDReportOnPostgresTest` : l'instantané fait l'aller-retour en base.

## Références

- ADR 0071 — le rapport 8D agrège et se scelle à la clôture (langue annoncée,
  dette levée ici).
- ADR 0124 — le PDF du 8D se présente comme l'écran.
- CLAUDE.md §15.1 (internationalisation), §22.4 (pas de magie).
