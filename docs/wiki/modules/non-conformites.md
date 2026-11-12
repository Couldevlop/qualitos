# Module Non-conformités (NC)

[← Retour à l'index](../README.md) · Route : **`/nc`** · Menu : *Qualité opérationnelle › Non-conformités*

## À quoi sert ce module

Une **non-conformité (NC)** est un écart entre une exigence (norme, procédure, spécification) et
la réalité observée. Le module permet de **déclarer**, **qualifier** et **suivre** les NC, avec
possibilité d'y joindre des **photos**.

## Parcours pas à pas

1. **Ouvrir** `/nc` : la liste des non-conformités et leur statut.
2. **Déclarer une NC** : décrire l'écart (quoi, où, quand), sa gravité et le périmètre concerné.
   Vous **nommez l'équipe** qui le prend en charge — au moins une personne, choisie dans l'annuaire.
   Vous y êtes posé d'office en tête : retirez-vous si vous ne suivez pas l'écart.
3. **Joindre des photos** pour documenter l'écart.
4. **Suivre le traitement** : la NC évolue jusqu'à sa résolution.
5. **Relier à une action** : une NC peut déclencher une [CAPA](capa.md), un cycle
   [PDCA](pdca.md) ou un projet [DMAIC](dmaic.md) selon la gravité.
6. **Clôturer, et remettre le rapport** : le [rapport 8D](rapport-8d.md) rassemble les huit
   disciplines attendues par le client, dont six sont reprises automatiquement de ce dossier.

## L'équipe en charge

L'équipe nommée à la déclaration est la **première discipline du rapport 8D** (D1 « Constituer
l'équipe ») : elle n'est donc pas à retaper à la clôture.

- **L'ordre compte** : le premier nommé est le **pilote** de l'écart. Les flèches de la fiche
  remontent quelqu'un d'un rang, c'est ainsi qu'on change de pilote.
- **Douze personnes au plus** : au-delà, ce n'est plus une équipe de résolution mais une liste de
  diffusion.
- **Les noms sont figés à la saisie** : un rapport relu trois ans plus tard porte les noms de
  l'époque, même si la personne a quitté l'organisation.
- **L'équipe se remanie** tant que l'écart est ouvert (bouton *Modifier l'équipe* sur sa fiche).
  Sur un écart clos, annulé ou rejeté, elle se lit sans se modifier : c'est l'archive du D1.
- **Un écart ouvert par la plateforme** — un constat d'audit, un relevé venu d'un connecteur —
  n'a pas d'équipe : la fiche le dit, et c'est à vous de la nommer en reprenant le dossier.
- Être nommé dans une équipe **ne donne aucun droit supplémentaire** : la visibilité des dossiers
  reste celle de votre rôle.

## Ce que la fiche montre sans qu'on en sorte

La fiche d'un écart rassemble ce que le rapport 8D rassemble, mais **pendant** le dossier et non
à sa clôture. Après les faits et l'équipe, deux cartes :

### Analyse de cause (ce que le 8D appelle D4)

Chaque **chaîne 5 Pourquoi** et chaque **diagramme Ishikawa** rattachés à l'écart s'affichent
dessinés — la cascade et l'arête de poisson, pas une liste de libellés. C'est la même figure que
dans l'écran de la méthode : les familles de causes y sont dans l'ordre des M, qui est l'ordre de
lecture d'un qualiticien.

- Les cartes sont **en lecture**. Pour modifier une cause, suivez le lien vers l'écran du
  diagramme ou de la chaîne : c'est là qu'est l'outillage d'édition.
- **Aucune analyse rattachée ?** La carte le dit explicitement. Un blanc se lirait comme un
  défaut d'affichage ; l'écart sans analyse de cause est une information en soi — et un écart
  significatif qu'on clôture sans analyse revient.

### Action corrective en cours (D5)

L'action du dossier CAPA lié s'affiche sur place : sa nature (corrective, préventive ou
d'endiguement), son statut, et le bouton qui la **fait avancer d'un cran** — *Démarrer*, puis
*Terminer* — sans quitter l'écart.

- Le responsable et l'échéance **de l'action** n'apparaissent que s'ils diffèrent de ceux du
  dossier : les répéter à l'identique ne renseigne personne.
- *Voir la CAPA* reste là pour ce que la carte ne montre pas : la **preuve** de l'action, son
  historique, et le verdict d'efficacité.
- **Un dossier sans action** — ancien, ou ouvert par un automate — le dit, et propose de définir
  la sienne. Un dossier CAPA n'a **qu'une** action : il n'y a donc rien à y ajouter ensuite, et
  sans action il ne pourra pas être clôturé.
- Si le dossier CAPA ne peut pas être lu (service indisponible), la carte **le dit** et propose de
  réessayer. Elle ne reste jamais vide : « vide » voudrait dire « aucune action ».

## L'IA au service des NC

- [Clustering de NC](nc-clusters.md) (`/nc-clusters`) : regroupe automatiquement les NC similaires
  pour révéler des familles de problèmes récurrents, invisibles à l'œil nu.

## Bonnes pratiques

- **Factuel et daté** : une NC bien décrite se traite plus vite.
- **Cause-racine, pas pansement** : reliez la NC à une analyse de cause ([Ishikawa](ishikawa.md))
  avant d'agir.
- **Surveillez les récurrences** : une même NC qui revient signale une cause non traitée — vérifiez
  les clusters.
- **Nommez l'équipe tout de suite** : c'est la première question posée en revue, et la retrouver
  des semaines plus tard, de mémoire, donne un D1 approximatif.
- **Renseignez l'endiguement tôt** : le [rapport 8D](rapport-8d.md) le demande (D3), et c'est ce
  qu'un client veut savoir en premier. Il s'oublie dès que la cause est trouvée.
