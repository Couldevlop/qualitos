# ADR 0116 — Le référentiel normatif est en anglais, les livrables restent dans la langue du client

- Statut : accepté
- Date : 2026-11-12
- Portée : Standards Hub (`standards`, `standard_sections`, `standard_clauses`, `standard_requirements`)

## Contexte

Le Standards Hub a été semé en français : le titre de la norme, les intitulés de
sections, les titres de clauses et le texte des exigences (migrations `V10` et
`V57` pour ISO 9001).

Cela posait trois problèmes concrets, tous observés sur la fiche de norme :

1. **Le texte opposable n'est pas celui-là.** Une norme ISO est publiée en
   anglais ; c'est cette formulation que l'auditeur cite, que le certificat
   mentionne et que les organismes certificateurs emploient. Un responsable
   qualité qui prépare son audit confrontait donc une reformulation française à
   un reproche qui lui arriverait en anglais, et devait retraduire mentalement
   chaque exigence.
2. **La structure HLS se compare mot pour mot.** ISO 9001, 14001, 45001 et 27001
   partagent la même structure de haut niveau : §5.2 « Policy » est littéralement
   la même phrase d'une norme à l'autre. La matrice de co-couverture du système
   de management intégré (CLAUDE.md §8.9) ne peut mutualiser une preuve que si
   les deux exigences se lisent dans la même langue — et une traduction française
   maison diverge d'un référentiel à l'autre.
3. **Deux équipes de deux pays ne lisaient pas la même phrase.** Les clauses
   n'étaient pas traduites par l'i18n du front (ce sont des données, pas des
   libellés d'interface) : un auditeur anglophone lisait du français.

## Décision

**Le référentiel normatif est en anglais**, langue de publication des normes.

**Les livrables du client restent dans sa langue** : modèles de documents
(manuel qualité, politique, procédures — `V64`, `/standards/templates/…`),
processus types, plans de dossier de certification, et toute l'interface, qui
reste traduite par l'i18n Angular en six langues. Un client français génère
toujours son manuel qualité en français ; c'est son document, pas la norme.

La frontière est donc : **ce qui cite la norme est en anglais ; ce que le client
produit est dans sa langue.**

### ISO 9001 est la norme pilote

Le passage se fait **par vagues**, comme les Industry Packs (CLAUDE.md §17), et
non d'un bloc : ISO 9001 d'abord — la norme la plus lue, le socle HLS — pour
valider la méthode (une migration d'`UPDATE` par norme), puis les autres
référentiels. Tant qu'une norme n'est pas passée, elle reste en français ; les
deux coexistent sans contrainte technique, la langue n'étant pas une clé.

### La migration ne réécrit pas les semis

`V167` ne contient que des `UPDATE`. Une migration déjà appliquée ne se modifie
pas : la somme de contrôle Flyway casserait en préproduction. Les clés des
`UPDATE` sont les **codes** de clauses et d'exigences (« 4.1 », « 9.2.1 ») et les
identifiants fixes des semis — stables par définition, puisque ce sont ceux de la
norme. Rien n'est créé, rien n'est supprimé : les preuves déjà rattachées aux
exigences, les scores d'alignement et les dossiers de certification en cours
restent liés aux mêmes lignes.

Le texte semé n'est pas une copie de la norme — elle est sous droits d'auteur :
c'est la même reformulation fidèle que le français qu'elle remplace, dans la
langue de publication.

## Conséquences

- Un auditeur, francophone ou non, lit l'exigence dans les termes où elle lui
  sera opposée.
- La co-couverture HLS devient comparable d'une norme à l'autre.
- Les nouvelles normes du catalogue sont semées **directement en anglais** : la
  question ne se repose pas à chaque ajout.
- Une norme encore semée en français n'est pas un défaut mais une vague non
  faite ; la liste des normes restant à passer se lit par `grep` sur la langue
  des titres de sections dans les migrations de semis.
- L'interface, les documents générés et l'assistance IA continuent de répondre
  dans la langue du client : le changement porte sur la donnée normative seule.
