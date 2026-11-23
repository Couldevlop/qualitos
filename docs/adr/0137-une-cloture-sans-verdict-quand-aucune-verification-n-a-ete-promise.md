# ADR 0137 — Une clôture sans verdict, quand aucune vérification n'a été promise

- **Statut** : accepté
- **Date** : 2026-11-23
- **Révise** : ADR 0055 §« un dossier dont toutes les actions sont des mesures d'endiguement ne peut pas être clos » (restreint ce refus au chemin du verdict)
- **S'appuie sur** : ADR 0073 (la question posée à l'ouverture), ADR 0055 (la nature des actions et les motifs de clôture énoncés avant le clic), ADR 0108 (le verdict d'efficacité), ADR 0131 et ADR 0136 (la vérification au niveau de l'action), ADR 0135 (une CAPA porte une action, et une seule)

## Contexte

La question « vérifiera-t-on l'effet de cette action ? » se tranche **à
l'ouverture** (ADR 0073). Le formulaire de création l'exige pour une action
corrective ou préventive, et **ne la pose pas pour un endiguement** — un
endiguement arrête un effet, il ne corrige rien, et il n'y a rien à mesurer
chez lui.

L'écran de la fiche CAPA ignorait pourtant cette réponse. Dès que le dossier
passait à `RESOLVED`, il affichait « Vérification d'efficacité requise. » et
**deux boutons seulement** : « Non efficace » et « Efficace — clôturer ».
Conséquences observées en recette sur la préproduction :

1. **Un dossier dont on avait répondu « non » ne pouvait se fermer qu'en
   déclarant une efficacité.** Son en-tête affichait pourtant, noir sur blanc,
   « VÉRIFICATION CONFIÉE : Non ». La seule porte de sortie inscrivait au
   registre une efficacité que personne n'avait constatée, sur un critère que
   personne n'avait mesuré. C'est un faux dans un registre qualité, et un
   auditeur qui tire la ligne ne trouve derrière elle ni relevé ni évaluateur.
2. **Un dossier d'endiguement ne pouvait pas se fermer du tout.** Le serveur
   refuse, depuis l'ADR 0055, de clôturer un dossier qui ne porte que de
   l'endiguement, et l'annonce avant le clic par l'obstacle `CONTAINMENT_ONLY`.
   Ce refus visait un dossier **à plusieurs actions**, où l'endiguement ne
   devait pas tenir lieu de correction. Depuis l'ADR 0135, chaque action porte
   son propre dossier : le dossier d'endiguement ne contient que de
   l'endiguement, et le refus le rendait **définitivement infermable** — il
   restait ouvert pour toujours, à encombrer le registre et les relevés qui
   comptent les dossiers en cours.

## Décision

La clôture suit la réponse donnée à l'ouverture, et rien d'autre.

| À l'ouverture | Clôture |
| --- | --- |
| Endiguement (la question n'est jamais posée) | **directe**, sans verdict |
| Corrective / préventive, vérification **non** | **directe**, sans verdict |
| Corrective / préventive, vérification **oui** | **vérifiée d'abord**, puis verdict (inchangé) |

Un **critère d'efficacité annoncé** vaut promesse, même sans « oui » explicite :
il dit ce qu'on mesurera, et le dossier qui en porte un passe par le verdict.
C'est la lecture que fait déjà `effectivenessPending` côté écran.

### Ce que la clôture directe écrit, et ce qu'elle n'écrit pas

`CapaService.closeWithoutVerification` passe le dossier à `CLOSED`, pose
`closed_at`, et journalise une transition **distincte** :
`CLOSED_WITHOUT_VERIFICATION` (« Dossier CAPA clos sans vérification
d'efficacité »), rattachée au même événement d'audit que `CLOSED`.

Elle **n'écrit aucune ligne d'évaluation d'efficacité**, et laisse
`effectiveness_verified` à `NULL`. Ni vrai — personne n'a démontré —, ni faux —
personne n'a constaté d'échec. `NULL` veut dire « on n'a pas jugé », et c'est
l'état exact.

### Ce qui ne change pas

Tous les autres obstacles de la clôture restent opposés par ce chemin : une
action qui traîne, un écart lié encore ouvert, un objectif qualité qui n'est
pas revenu sous sa cible (ADR 0133). Sans cela, la clôture directe deviendrait
la porte dérobée par laquelle les obstacles se contournent. Le bouton
« Clôturer » s'éteint sur les mêmes motifs, avec la même infobulle, que
« Efficace — clôturer » (ADR 0055).

**Un seul prédicat dit ce qu'est une promesse**, `CapaService.verificationPromise`,
et la liste d'obstacles le lit comme la clôture : `CONTAINMENT_ONLY` n'est plus
annoncé qu'aux dossiers qui passeront par le verdict. Sans cela, l'obstacle
aurait éteint le seul bouton restant à l'endiguement, et la décision n'aurait
rien changé à l'écran. L'écran lit la même définition, actions comprises : une
promesse portée par une seule action (ADR 0131) retire la clôture directe,
faute de quoi le bouton aurait été offert pour un refus en 409.

Le refus opposé par `verifyEffectiveness` à un dossier d'endiguement seul
**reste en place** : on ne déclare pas une efficacité démontrée sur une mesure
qui n'a fait qu'arrêter l'effet. Il ne barre plus la seule sortie, puisqu'il en
existe désormais une autre.

### Surface

`POST /api/v1/capa/cases/{id}/close`, sous la permission `capa.verify` — la
même que le verdict. C'est la même décision, fermer le dossier, prise là où il
n'y a rien à juger ; un droit distinct laisserait fermer sans vérifier à qui
l'on refuse de vérifier.

## Conséquences

- Un dossier d'endiguement se clôt, donc l'écart qu'il retenait se clôt aussi.
- Le registre ne porte plus d'efficacité déclarée sans mesure. En revanche,
  **les dossiers déjà clos par ce chemin gardent leur `effectiveness_verified =
  true`** : aucune reprise de données n'est prévue, parce que rien ne permet de
  distinguer, après coup, une efficacité réellement constatée d'une case cochée
  faute d'autre bouton.
- Un relevé d'exploitation doit désormais lire la transition du journal, et non
  le seul statut, pour savoir si une clôture s'appuie sur une vérification.
- Ce qui réagit à la **fermeture** — requalification des parties intéressées,
  proposition de révision du PFMEA — réagit désormais à `CapaTransition.closesCase()`
  et non à `CLOSED` seul. Ces effets suivent le fait que le dossier s'est fermé,
  pas la démonstration d'une efficacité ; les laisser sur l'ancienne comparaison
  aurait rendu la clôture directe silencieuse pour eux, et le défaut ne se serait
  vu que des mois plus tard, au PFMEA non révisé.
- `EffectivenessBoardService` n'est pas touché : les dossiers sans promesse
  n'ont jamais paru au plan d'efficacité (ADR 0136), et c'est juste.
