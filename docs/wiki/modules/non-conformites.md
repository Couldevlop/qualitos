# Module Non-conformités (NC)

[← Retour à l'index](../README.md) · Route : **`/nc`** · Menu : *Qualité opérationnelle › Non-conformités*

## À quoi sert ce module

Une **non-conformité (NC)** est un écart entre une exigence (norme, procédure, spécification) et
la réalité observée. Le module permet de **déclarer**, **qualifier** et **suivre** les NC, avec
possibilité d'y joindre des **photos**.

## Parcours pas à pas

1. **Ouvrir** `/nc` : la liste des non-conformités et leur statut.
2. **Déclarer une NC** : décrire l'écart (quoi, où, quand), sa gravité et le périmètre concerné.
   Vous choisissez sa **nature** dans une liste unique, où les natures sont rangées sous leur
   famille (produit, processus, documentaire…) : la famille se déduit de la nature retenue, il n'y
   a donc qu'une question à répondre au lieu de deux enchaînées. La nature est **obligatoire** —
   c'est elle qui regroupera cet écart avec ses semblables.
   Vous **nommez l'équipe** qui le prend en charge — au moins une personne, choisie dans l'annuaire.
   Vous y êtes posé d'office en tête : retirez-vous si vous ne suivez pas l'écart.
3. **Joindre des photos** pour documenter l'écart.
4. **Suivre le traitement** : la NC évolue jusqu'à sa résolution.
5. **Relier à une action** : *Nouvelle action* ouvre le dossier CAPA qui traite l'écart et sa
   première action (source verrouillée ; le titre et la description partent **vides** — ce sont
   des phrases à écrire, pas le constat à recopier). Une fois le dossier ouvert, le même bouton
   devient *Voir les actions* et liste **ce qui reste à faire** ; une ligne mène au dossier.
   Les actions suivantes s'ajoutent, s'avancent et se prouvent dans les **tableaux d'actions** de
   la fiche, chacun sous sa discipline — endiguement (D3), corrective (D5), préventive (D7). Une
   NC peut déclencher une [CAPA](capa.md), un cycle [PDCA](pdca.md) ou un projet
   [DMAIC](dmaic.md) selon la gravité.
6. **Clôturer** — par quelqu'un **d'autre** que celui qui a résolu l'écart : voir
   « Le second regard sur la clôture » plus bas.
7. **Remettre le rapport** : le [rapport 8D](rapport-8d.md) rassemble les huit disciplines
   attendues par le client, reprises de ce dossier.

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

## La fiche : le traitement en huit disciplines

La fiche d'un écart suit **l'ordre des huit disciplines du 8D**, c'est-à-dire exactement la trame
du [rapport 8D](rapport-8d.md) que le client recevra. Un sommaire, à gauche, marque les
disciplines qui portent déjà quelque chose et vous emmène sur celle que vous choisissez.

> **Rien n'y est obligatoire.** Une discipline vide se dit plutôt que de disparaître — son absence
> se lirait comme un écran incomplet — mais aucune ne barre la suivante. Un écart mineur n'a pas
> d'action d'endiguement à décrire, et l'exiger ferait inventer une ligne pour passer. Le sommaire
> marque ce qui est **fait** ; il n'autorise ni n'interdit rien.

### D1 — Constituer l'équipe

L'équipe nommée à la déclaration (voir ci-dessus), et le **valideur de la clôture** — voir
« Le second regard » plus bas.

### D2 — Décrire le problème

Les faits : gravité, nature, date de détection, zone, position. S'y ajoutent deux champs que
toute réclamation porte, et qui s'écrivaient jusqu'ici dans la description :

- **Client** — celui qui réclame, par le nom qu'il s'est donné. Un constat interne n'en a pas.
- **N° de lot** — tel qu'il est marqué sur la pièce ou l'emballage.

Les deux sont facultatifs. Renseignés, ils deviennent filtrables, comptables, et paraissent en
tête du rapport remis au client.

Y vivent aussi les **photos du constat** — elles décrivent le problème — et l'**analyse visuelle
par IA**, qui lit l'une d'elles.

En **option**, non comme une étape : ouvrir le cycle [PDCA](pdca.md) ou le projet
[DMAIC](dmaic.md) qui traitera ce que l'écart *révèle*, inscrire un risque au registre, ou éditer
le rapport 8D. Ce que l'écart révèle peut mériter un cycle : c'est une décision, pas un passage
obligé.

### D3, D5, D7 — Les trois natures d'actions

Le tableau des actions du dossier CAPA paraît **trois fois**, chacune filtrée sur la nature de sa
discipline :

| Discipline | Nature | Ce qu'elle répond |
|---|---|---|
| **D3** | Endiguement | Ce qui protège le client **dès maintenant**, avant la cause racine |
| **D5** | Corrective | Ce qui **supprime** la cause racine |
| **D7** | Préventive | Ce qui évite que le problème **revienne ailleurs** |

Une ligne par action, avec son intitulé, son responsable, son échéance, son statut, le bouton qui
la **fait avancer d'un cran** — *Démarrer*, puis *Terminer* — et une colonne **Preuve** portant la
pièce qui atteste que *ce geste-là* a été fait.

- **Cliquez une ligne** pour corriger l'action. Sur un dossier terminé, elle s'ouvre quand même,
  **en lecture seule** : relire ce qui a été fait reste légitime après la clôture.
- Le **pilote du dossier** est le responsable de la *première* action. Les suivantes ne le
  déplacent pas.
- **Un écart peut porter plusieurs dossiers CAPA** ; chacun a son tableau, nommé.
- **Aucun dossier ?** L'invite à en ouvrir un paraît **une seule fois**, au D5. Trois boutons
  identiques empilés sous trois titres feraient croire à trois dossiers à ouvrir.
- Si le dossier ne peut pas être lu (service indisponible), le tableau **le dit** et propose de
  réessayer. Il ne reste jamais vide : « vide » voudrait dire « aucune action ».

### D4 — Analyse des causes

Chaque **chaîne 5 Pourquoi** et chaque **diagramme Ishikawa** rattachés à l'écart s'affichent
dessinés — la cascade et l'arête de poisson, pas une liste de libellés. C'est la même figure que
dans l'écran de la méthode : les familles de causes y sont dans l'ordre des M, qui est l'ordre de
lecture d'un qualiticien.

- Les figures sont **en lecture**. Pour modifier une cause, suivez le lien vers l'écran du
  diagramme ou de la chaîne : c'est là qu'est l'outillage d'édition.
- **Aucune analyse rattachée ?** La discipline reste, vide. Un écart significatif qu'on clôture
  sans analyse de cause revient.

### D6 — Preuves des actions correctives

Les preuves se joignent **action par action**, dans la colonne *Preuve* des trois tableaux : une
pièce détachée de son action ne démontre rien. Le D6 porte le jugement d'ensemble — la note de
résolution — et renvoie au dossier pour le **verdict d'efficacité**, qui se prononce avec son
critère annoncé d'avance.

À ne pas confondre : un dossier peut très bien avoir toutes ses actions prouvées et aucune preuve
d'efficacité.

### D8 — Remercier l'équipe

Le mot de clôture, qui part dans le rapport 8D. Facultatif, comme tout le reste.

## Reprendre un écart refermé

Un écart revient : la récidive se constate après la clôture, le client relance, un constat avait
été écarté à tort. Le bouton **Rouvrir** le ramène en instruction, sur un écart résolu, clos,
annulé ou rejeté.

- **Rien n'est effacé** : l'équipe, l'analyse de cause, les actions, les preuves et ce qui l'avait
  refermé restent attachés. Le bandeau de la fiche montre le fait le plus récent.
- **Le motif est facultatif.** Sans lui la reprise passe quand même — mais rien n'expliquera ce
  retour en arrière six mois plus tard.
- Sans cette reprise, il fallait ouvrir un **second** écart, qui perdait tout le travail du premier
  et comptait deux fois le même problème dans les indicateurs.
- Demande le droit de **clôturer** : défaire un acte suppose l'autorité de le poser.

## Le second regard sur la clôture

Clôturer un écart, c'est déclarer « plus rien à faire ici » — donc **valider** que la résolution
tient. Deux règles en découlent.

**Personne ne valide son propre travail.** La personne qui a écrit la résolution ne peut pas
clôturer l'écart. Le bouton est éteint pour elle, avec son explication, et la fiche précise que
l'écart n'est pas en défaut : c'est la personne qui ne peut pas le porter, pas l'écart qui résiste.
La règle est celle que l'ISO 9001 §10.2 attend de la vérification de l'efficacité.

**Quelqu'un d'autre qu'un responsable qualité peut clôturer.** Au D1, vous pouvez désigner un
**valideur de la clôture**, choisi dans l'annuaire. Cette personne pourra clôturer **cet** écart
même sans rôle qualité — et jamais s'il l'a résolu elle-même. La désignation ne lui donne aucun
droit sur les autres écarts.

> **Pourquoi cette désignation ?** Une organisation qui ne compte qu'un responsable qualité ne
> pourrait plus rien clôturer : c'est lui qui résout, et lui seul qui aurait le droit de clore.
> La désignation ouvre ce regard à un chef d'atelier, un pilote de processus, un confrère d'un
> autre service.

**Désigner demande le droit de clôturer** — on ne donne pas un pouvoir qu'on n'a pas. Si vous
déclarez un écart sans ce droit, le champ du valideur vous est refusé : demandez la désignation à
qui détient ce droit.

**Et une fois l'écart résolu, celui qui l'a résolu ne peut plus changer le valideur** : il y
nommerait quelqu'un de complaisant, et le second regard deviendrait un tampon qu'il aurait choisi
lui-même. Un autre détenteur du droit, lui, peut encore le faire — c'est ce qui débloque un écart
dont le valideur a quitté l'organisation.

La fiche d'un écart clos porte les **deux signatures** — *Résolu par*, *Clôturé par* — au D6.
C'est ce qu'un auditeur vient lire. Les écarts déclarés avant cette règle n'ont pas ces noms, et
la fiche n'en invente pas.

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
