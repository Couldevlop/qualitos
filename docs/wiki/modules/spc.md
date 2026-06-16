# Module SPC — Cartes de contrôle & règles de Nelson

[← Retour à l'index](../README.md) · Route : **`/spc`** · Menu : *Méthodes qualité › SPC*

## À quoi sert ce module

Le **SPC** (*Statistical Process Control*, maîtrise statistique des procédés) surveille la
stabilité d'un procédé à partir d'une série de mesures. QualitOS applique les **8 règles de
Nelson** pour distinguer une variation normale d'un **signal** indiquant que le procédé sort de
contrôle (point hors limites, tendance, série anormale, etc.).

## Parcours pas à pas

### Analyser une série de mesures

1. **Ouvrir** `/spc`.
2. **Fournir une série de mesures** à analyser.
3. **Lancer l'analyse** : le module renvoie la carte de contrôle et les **violations** des règles
   de Nelson détectées, point par point.

### Analyser un KPI existant

- Le module peut analyser directement la carte de contrôle d'un **KPI** en exploitant ses
  dernières mesures.
- Si le procédé est jugé hors-contrôle, une **CAPA corrective** peut être ouverte automatiquement
  (option à activer) — voir [CAPA](capa.md) (source `SPC_ALERT`).

## Comment lire le résultat

- Chaque règle de Nelson violée est signalée : c'est un **signal**, pas forcément un défaut, mais
  cela mérite investigation.
- Au-delà des règles déterministes, la [détection d'anomalies IA](anomaly.md) repère des signaux
  plus diffus sur des données multivariées.

## Bonnes pratiques

- **Réagissez aux signaux, pas au bruit** : c'est tout l'intérêt des règles de Nelson.
- **Reliez à l'action** : un signal SPC récurrent doit alimenter une CAPA ou un projet
  [DMAIC](dmaic.md).
- **Mesures fiables d'abord** : un SPC ne vaut que par la qualité des mesures qui l'alimentent.
