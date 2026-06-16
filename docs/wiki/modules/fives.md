# Module 5S — Excellence opérationnelle

[← Retour à l'index](../README.md) · Route : **`/fives`** · Menu : *Méthodes qualité › 5S*

## À quoi sert ce module

La méthode **5S** organise et fiabilise les postes de travail autour de cinq piliers :

| Pilier | Sens |
|---|---|
| **Seiri** (Trier) | Éliminer l'inutile. |
| **Seiton** (Ranger) | Une place pour chaque chose. |
| **Seiso** (Nettoyer) | Maintenir la propreté, détecter les anomalies. |
| **Seiketsu** (Standardiser) | Définir des standards visuels. |
| **Shitsuke** (Respecter) | Maintenir la discipline dans la durée. |

Le module permet de réaliser des **audits 5S de terrain** par zone, d'attribuer un **score** et
de suivre les **plans d'action**.

## Parcours pas à pas

1. **Ouvrir** `/fives` : la liste présente les audits 5S et leurs scores.
2. **Créer un audit** : nommer la zone auditée et le périmètre.
3. **Évaluer les 5 piliers** sur le terrain et consigner les constats.
4. **Enregistrer** : le score 5S est calculé et l'audit historisé.
5. **Suivre les actions** issues des écarts constatés.

> **Mode terrain hors-ligne** : la saisie fonctionne même sans réseau. Les enregistrements sont
> mis en file d'attente et se synchronisent automatiquement au retour de la connexion
> (voir la [FAQ](../faq.md) et la page `/offline-queue`).

## Liens avec les autres modules

- Un constat 5S peut générer une [non-conformité](non-conformites.md) ou alimenter un
  [Cercle de Qualité](circles.md).
- Les scores 5S alimentent les indicateurs (`/kpis`).

## Bonnes pratiques

- **Auditez régulièrement** : la valeur du 5S vient de la répétition, pas du coup d'éclat.
- **Photo systématique** sur les écarts : la preuve visuelle accélère la correction.
- **Standardisez avant de discipliner** : on ne peut maintenir (*Shitsuke*) que ce qui est défini
  (*Seiketsu*).
