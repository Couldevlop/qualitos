# Module EHS — Environnement, hygiène et sécurité

[← Retour à l'index](../README.md) · Route : **`/ehs`** · Menu : *Opérations › EHS*

## À quoi sert ce module

Le module **EHS** tient le registre des **incidents** d'environnement, d'hygiène et de sécurité :
accidents, presque-accidents, atteintes à l'environnement, incidents de sûreté, dommages
matériels. C'est le registre qu'un audit ISO 14001 ou ISO 45001 demande à voir en premier.

Il ne duplique pas les mécanismes de traitement : un incident EHS se **rattache** à une
[non-conformité](non-conformites.md) et à un dossier [CAPA](capa.md), qui portent l'analyse et les
actions. Le module apporte la déclaration, le suivi d'investigation et la statistique.

## Parcours pas à pas

### 1. Déclarer un incident

1. **Ouvrir** `/ehs`, puis **Déclarer un incident**.
2. Renseigner un **code**, un **titre**, une **description**, le **type**, la **sévérité**, la date
   de **survenue** et le **lieu**.
3. L'incident est enregistré au statut **déclaré**, avec sa date de déclaration et son déclarant.

Les six types d'incident sont : **accident corporel**, **presque-accident**, **environnemental**,
**sûreté**, **dommage matériel**, **autre**. La sévérité se note **faible**, **moyenne**,
**élevée** ou **critique**.

### 2. Investiguer

**Investiguer** fait passer l'incident en investigation et permet de désigner le **propriétaire de
l'investigation** — une personne de votre organisation.

### 3. Maîtriser l'incident

**Mitiger** exige deux éléments, tous les deux obligatoires :

- la **cause racine** identifiée ;
- les **actions correctives** engagées.

C'est le point de passage qui empêche un incident de se clore sur un simple constat. Pour creuser
la cause, ouvrez un [Ishikawa](ishikawa.md) ou les [5 Pourquoi](five-whys.md) depuis la
non-conformité rattachée.

### 4. Rattacher et clôturer

- **Lier un CAPA** et **Lier une NC** rattachent l'incident au dossier qui porte son traitement.
- La fiche permet aussi de renseigner les **personnes impliquées** et les **normes concernées**.
- **Clôturer** ferme l'incident. **Annuler** le retire du suivi sans le supprimer — par exemple
  après constat qu'il s'agissait d'un doublon.

### Le cycle de vie

```
Déclaré → En investigation → Maîtrisé → Clôturé
                                   ↘ Annulé
```

## Comment lire le résultat

La liste se filtre par **statut** et par **sévérité**, et porte en tête les compteurs du registre :
incidents déclarés, en investigation, maîtrisés, clôturés, ainsi que le nombre d'**accidents** et
de **presque-accidents**.

Le rapport entre ces deux derniers chiffres est le plus parlant du module : une organisation qui
déclare beaucoup de presque-accidents et peu d'accidents est une organisation où l'on déclare. Une
organisation qui ne déclare que des accidents n'a pas moins de risques — elle en voit moins.

La fiche d'un incident affiche sa chronologie complète : survenu le, déclaré le, maîtrisé le,
clôturé le, et les liens vers le dossier CAPA et la non-conformité rattachés.

## Bonnes pratiques

- **Déclarez les presque-accidents.** Ils coûtent une déclaration et évitent un accident ; c'est
  le seul indicateur du module qu'il est bon de voir augmenter.
- **Ne clôturez pas sans cause racine** : la plateforme l'exige pour maîtriser un incident,
  profitez-en pour la chercher vraiment plutôt que pour la nommer.
- **Rattachez systématiquement la CAPA** : sans elle, les actions correctives décrites dans
  l'incident ne sont ni assignées, ni suivies, ni vérifiées pour leur
  [efficacité](efficacite-capa-et-competences.md).
- **Renseignez les normes concernées** : c'est ce qui permet au [Standards Hub](standards-hub.md)
  de rapprocher l'incident de la clause ISO 14001 ou ISO 45001 qu'il concerne.
- **Reliez l'EHS au 5S** : beaucoup d'incidents de sécurité naissent d'un poste encombré, et un
  [audit 5S](fives.md) les voit avant qu'ils ne se produisent.
