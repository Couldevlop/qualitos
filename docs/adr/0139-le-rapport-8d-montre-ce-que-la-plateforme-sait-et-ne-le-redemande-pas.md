# ADR 0139 — Le rapport 8D montre ce que la plateforme sait, et ne le redemande pas

- **Statut** : accepté
- **Date** : 2026-11-25
- **Portée** : rapport 8D d'un écart — instantané assemblé, rendu PDF et écran
- **S'appuie sur** : l'ADR 0115 (un dossier CAPA porte plusieurs actions), l'ADR
  0121 (la clôture d'un écart attend tous ses dossiers), l'ADR 0129 (langue figée
  à l'émission), l'ADR 0131 (chaque action porte sa propre évaluation
  d'efficacité)

## Contexte

Le rapport 8D est le document qu'un client reçoit quand un écart l'a touché. Il
porte huit disciplines, et trois d'entre elles — D1 l'équipe, D3 l'endiguement,
D8 la reconnaissance — se **saisissaient à la main**, dans un cadre posé en haut
de la page, au-dessus des huit cartes. On tapait donc l'équipe dans un
formulaire et on la relisait vingt lignes plus bas ; et une discipline qu'on
ressaisit est une discipline qu'on oublie, ce qui donne un D1 vide dans un
document remis à l'extérieur.

Trois autres défauts tenaient à la même cause : le document **disait** ce que la
plateforme **savait déjà**.

1. **L'équipe est sur la fiche depuis le premier jour.** Les membres y sont
   nommés, dans leur ordre de saisie, et l'annuaire connaît leur adresse. Le
   rapport demandait de les retaper en texte libre.
2. **L'endiguement et la prévention sont des actions CAPA tracées.** Le D3 et le
   D7 portaient à la fois un récit (« ce qu'on a fait pour arrêter
   l'hémorragie ») et le tableau de ces mêmes gestes — qui, pour quand, fait ou
   non. Le lecteur rencontrait deux fois le même endiguement, une fois raconté,
   une fois tracé, sans savoir lequel faisait foi.
3. **Le D7 recopiait l'AMDEC et les plans de surveillance** : mode de
   défaillance, effet, cause, contrôles, IPR, révisions de plan. C'est
   l'outillage interne de l'analyse, pas la prévention. Le client vient lire ce
   qui empêche l'écart de revenir, et il lisait d'abord une page de référentiel
   où les gestes décidés se perdaient.

S'y ajoutait une **provenance affichée partout** : chaque discipline servie
annonçait « Agrégé depuis : 3 dossiers CAPA » juste au-dessus du tableau de ces
mêmes actions, et repoussait d'une ligne ce qu'on venait lire. Enfin, le D6
nommait un geste accompli sans jamais dire **s'il avait servi**, alors que
l'évaluation d'efficacité par action existe depuis l'ADR 0131 et vit dans le
dossier CAPA.

## Décision

**Le rapport montre ce que la plateforme sait, et ne redemande que ce qu'elle ne
sait pas.**

1. **D1 est un tableau**, bâti sur l'équipe de la fiche : _Nom et prénom_,
   _Adresse mail_, _Rôle et fonction_. Le nom reste celui figé à la saisie — un
   compte renommé ne réécrit pas un rapport émis ; l'adresse se relit dans
   l'annuaire, parce que c'est une coordonnée et non une trace. Un annuaire
   injoignable laisse la case vide : le document s'assemble quand même.
2. **D3, D5 et D7 ne portent que leur tableau d'actions**, chacun de sa nature —
   endiguement, correctif, prévention. Le texte libre du D3 et le référentiel du
   D7 s'en vont. Ce qui a déjà été tapé reste **stocké** et n'est pas détruit ;
   il ne compose plus le document.
3. **La colonne « Nature » quitte ces tableaux** : une discipline ne montre
   qu'une nature, et le mot se répétait sur chaque ligne, devant l'intitulé
   qu'on vient lire. La colonne « Preuve » reste au D3 et au D7, où une pièce
   atteste que ce geste-là a été fait, et pas au D5, dont la démonstration est
   au D6.
4. **D6 rappelle le titre de l'action, nomme ses pièces jointes, puis donne son
   verdict d'efficacité** (« efficace », « partiellement efficace »,
   « inefficace », avec la date du jugement). « Partiellement efficace » n'est
   pas une politesse pour « inefficace » : la cause était la bonne, l'action ne
   va pas assez loin — et le document porte la nuance.
5. **Une discipline servie ne dit plus sa provenance.** Le libellé de source ne
   survit qu'à la discipline **vide**, où il dit ce qui manque : c'est ce qui
   distingue un 8D d'un formulaire aux cases vides.
6. **D8 reste la seule saisie, et elle est dans sa carte.** Personne d'autre ne
   sait ce que l'équipe mérite qu'on dise. La reconnaissance standard s'ajoute
   dès que l'écart et ses actions sont clos.
7. **D4 se lit.** La cause se travaille dans l'Ishikawa et les 5 Pourquoi, qui
   sont leurs propres écrans. Le rapport la donne à lire — c'est ce qu'un
   auditeur vient y chercher — et les boutons qui ouvraient le diagramme,
   déroulaient l'analyse ou concluaient la cause depuis ses pages en faisaient
   un formulaire de plus. Les boutons d'**ajout d'action** restent : décider une
   action en lisant le rapport est le geste même du 8D.

## Conséquences

- Un écart dont personne n'a nommé l'équipe sort un rapport **partiel**, et le
  dit : « Aucun membre nommé sur la non-conformité ». C'est voulu — le D1 ne se
  rattrape plus par une ligne de texte tapée au moment d'émettre.
- Un D3 ou un D7 **sans action de sa nature** est désormais annoncé manquant,
  même si un brouillon portait un récit. Le document ne tient pour acquis que
  des gestes tracés.
- L'AMDEC et les plans de surveillance **ne figurent plus au rapport**. Ils
  restent entiers sur leurs écrans, qui les montrent avec leurs révisions et
  leur scellé.
- La **fonction** d'un membre n'est stockée nulle part : l'annuaire donne un nom
  et une adresse. La troisième colonne du D1 porte donc le rôle seul — pilote ou
  membre, que l'ordre de saisie désigne — jusqu'à ce que la fonction existe
  quelque part.
- Les rapports **déjà émis ne changent pas** : leur empreinte porte sur les
  octets du PDF, et le rendu relit l'instantané scellé tel qu'il a été écrit —
  tableaux à six colonnes et libellés de provenance compris.
- L'écran rend le tableau du D1 **tel que le serveur le porte** et n'en
  refabrique pas un : une liste de noms d'un côté et un tableau de l'autre
  finiraient par diverger. Les tableaux d'actions, eux, restent manœuvrés par
  leur panneau — les rendre deux fois en donnerait un mort.
