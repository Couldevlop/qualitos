# Rapport 8D (clôture d'une non-conformité)

[← Retour à l'index](../README.md) · Route : **`/nc/:id/8d`** · Ouvert depuis la fiche d'une
[non-conformité](non-conformites.md), bouton **Rapport 8D**

## À quoi sert ce document

Le **8D** (« eight disciplines ») est le format que les clients — l'automobile en tête, via
l'IATF 16949 — demandent à la clôture d'un écart. Il raconte, en huit étapes, ce qui s'est passé
et ce qui fait qu'il ne se reproduira pas.

QualitOS ne vous demande pas de le ressaisir : **sept disciplines sur huit sont remplies
automatiquement** à partir du travail déjà fait dans les autres modules. Une seule se tape, le
**D8** — personne d'autre que vous ne sait ce que l'équipe mérite qu'on dise.

| Discipline | D'où vient le contenu |
| --- | --- |
| **D1 — Équipe** | L'[équipe nommée sur la fiche de l'écart](non-conformites.md), **en tableau** : *Nom et prénom*, *Adresse mail*, *Rôle et fonction*. L'ordre de désignation porte le rôle — le premier est pilote, les suivants membres — et l'adresse vient de l'annuaire. Un compte sans adresse connue laisse la case vide |
| **D2 — Description du problème** | La non-conformité : référence, constat, gravité, zone, photos |
| **D3 — Actions immédiates** | Le tableau des actions **d'endiguement** du dossier CAPA, où on les lit, on les avance et on en ajoute sans quitter le rapport. Rien d'autre : un récit tapé à côté des gestes tracés faisait lire deux fois le même endiguement, sans dire lequel faisait foi |
| **D4 — Cause racine** | Les [Ishikawa](ishikawa.md) et [5 Pourquoi](five-whys.md) rattachés à l'écart, et la cause racine saisie sur la NC. Les mêmes figures se lisent, dessinées, sur la [fiche de l'écart](non-conformites.md) : le rapport ne les découvre pas |
| **D5 — Définition des actions correctives** | Le tableau des actions **correctives** du dossier [CAPA](capa.md) : ce qui supprime la cause de l'écart survenu. L'endiguement est au D3, la prévention au D7 — montrées sous deux disciplines, les mêmes lignes laissaient croire à deux décisions. Pas de colonne *Preuve* : ce que le D5 démontre n'est pas « l'action a été faite » mais « la cause a disparu », et cela se juge au D6 |
| **D6 — Mise en œuvre et preuves** | Chaque action corrective menée à terme : son titre, le **nom de ses pièces jointes**, et son **verdict d'efficacité** quand elle a été jugée (« efficace », « partiellement efficace », « inefficace », avec la date). Le verdict se prononce dans la [CAPA](capa.md) et remonte ici |
| **D7 — Prévention de la récurrence** | Le tableau des actions **préventives** du dossier, avec sa colonne *Preuve*. L'[AMDEC procédé](produit-pfmea-control-plan.md) et les plans de surveillance **n'y figurent plus** : c'est l'outillage de l'analyse, pas la prévention, et ils se lisent entiers sur leurs propres écrans |
| **D8 — Reconnaissance de l'équipe** | **À saisir**, dans la carte du D8 : ce que l'équipe a appris, et ce qui mérite d'être dit. Dès que l'écart et ses actions sont clos, la reconnaissance standard s'y ajoute |

### Où se dépose une preuve, et laquelle

Deux choses distinctes portent le même mot, et elles ne se rangent pas au même endroit.

- **La preuve d'une action** atteste que *ce geste-là* a été fait : une barrière installée, un
  contrôle ajouté. Elle se joint **sur la ligne de l'action**, dans la colonne *Preuve* du
  tableau — au **D3** et au **D7**.
- **La preuve du dossier** démontre que *le problème a cessé* : un relevé, une carte de
  contrôle, une procédure signée. Elle se dépose au **D6**, sur le dossier.

C'est pourquoi le **D5 n'a pas de colonne *Preuve*** : ce qu'il a à démontrer n'est pas
« l'action a été faite » mais « la cause a disparu », et cela se juge au D6.


## Parcours pas à pas

1. **Ouvrir la fiche de la non-conformité**, puis **Rapport 8D**. L'écran montre les huit
   disciplines, telles qu'elles sont à cet instant.
2. **Renseigner D8**, dans sa carte, puis **Enregistrer**. C'est la seule saisie du rapport.
   D1 vient de l'équipe de la fiche, D3, D5 et D7 des actions du dossier [CAPA](capa.md) : si une
   de ces disciplines est vide, ce n'est pas un champ qui manque, c'est un geste à décider — et le
   bouton **Ajouter une action** est sur place.
3. **Clôturer la non-conformité** (depuis sa fiche). Tant qu'elle ne l'est pas, le rapport ne
   s'émet pas — un 8D affirme qu'un problème est réglé.
4. **Émettre**. Confirmation demandée, et **second facteur d'authentification** exigé : l'émission
   produit un document opposable.
5. **Télécharger le PDF** et le transmettre au client.

## Ce que « partiel » veut dire

Une discipline sans contenu **le dit** — à l'écran comme dans le PDF — et le rapport porte alors
la mention **Partiel**, suivie de la liste des disciplines concernées.

C'est volontaire. Un rapport aux cases remplies à la hâte trompe son lecteur ; un rapport qui
annonce ce qui lui manque reste lisible et honnête devant un auditeur. Vous pouvez émettre un
rapport partiel : à vous de juger si l'écart le justifie.

Trois absences sont fréquentes et normales :

- **D3, D5, D6 et D7 vides** quand aucune CAPA n'a été escaladée depuis la NC : ces quatre
  disciplines ne lisent que les actions du dossier ;
- **D3 ou D7 vide** sur un dossier qui existe, quand aucune action de cette nature-là n'y a été
  décidée. Les dispositifs **Poka-Yoke** ne s'y affichent pas : ils se rattachent aujourd'hui à un
  projet [DMAIC](dmaic.md), jamais à une non-conformité ;
- **D1 vide** quand personne n'a été nommé sur la fiche de l'écart — un constat d'audit ou un
  import système en produit.

## Un rapport émis ne change plus

À l'émission, le texte des huit disciplines est **figé**. Même si la CAPA est rouverte ou
l'Ishikawa complété ensuite, le rapport continue de dire ce qu'il disait le jour de la clôture :
c'est ce que le client a lu qui fait foi.

Le PDF porte en pied de page :

- la mention de sa **signature** (Ed25519 + ML-DSA-65, résistante au quantique) ;
- un **QR code** : quiconque le scanne — votre client, un auditeur — vérifie l'authenticité du
  document sans avoir de compte QualitOS. La vérification ne révèle que des faits d'intégrité
  (le document est authentique, voici son empreinte), jamais son contenu.

Si le rapport d'une non-conformité doit être refait, il faut en reprendre le traitement : un 8D
ne se réémet pas.

## Qui peut faire quoi

| Rôle | Lire le rapport et le PDF | Renseigner D8 | Émettre |
| --- | --- | --- | --- |
| Utilisateur, Auditeur | ✅ | — | — |
| Manager Qualité | ✅ | ✅ | ✅ (second facteur) |
| Directeur Qualité, Admin Tenant | ✅ | ✅ | ✅ (second facteur) |

Lire est ouvert à tous : la personne qui a signalé le défaut doit pouvoir lire la suite qu'on y a
donnée.

## Bonnes pratiques

- **Décidez l'endiguement le jour même**, en action du dossier. C'est ce qu'un client demande en
  premier, et il s'oublie vite une fois la cause trouvée.
- **Jugez l'efficacité de chaque action corrective** dans la [CAPA](capa.md) : le verdict remonte
  au D6 du rapport, et c'est ce qu'un client cherche — non pas qu'un geste ait été fait, mais
  qu'il ait servi.
- **N'émettez qu'une fois la CAPA avancée.** Un 8D émis trop tôt montre des actions décidées mais
  aucune preuve de mise en œuvre.
