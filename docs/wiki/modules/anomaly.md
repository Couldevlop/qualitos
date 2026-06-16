# Module Détection d'anomalies IA

[← Retour à l'index](../README.md) · Route : **`/anomaly`** · Menu : *Méthodes qualité › Anomalies IA*

## À quoi sert ce module

Là où le [SPC](spc.md) applique des règles statistiques connues, la **détection d'anomalies**
repère des comportements inhabituels sur des données **multivariées**, sans règle prédéfinie. Elle
s'appuie sur des méthodes non-supervisées : **Isolation Forest** et **reconstruction par ACP**
(analyse en composantes principales).

C'est utile pour faire émerger des **signaux faibles** : combinaisons de valeurs anormales qu'un
seuil unique ne détecterait pas.

## Parcours pas à pas

1. **Ouvrir** `/anomaly`.
2. **Fournir les données** à analyser.
3. **Lancer la détection** : le module identifie les points anormaux.
4. **Expliquer** une anomalie : le module fournit une **explication** des variables qui
   contribuent le plus à son caractère atypique — voir [Explicabilité IA](explicabilite-ia.md).

## Bonnes pratiques

- **Une anomalie n'est pas un verdict** : c'est une alerte à investiguer, pas une conclusion.
- **Croisez avec le SPC** : anomalie IA + signal SPC = forte probabilité de problème réel.
- **Exploitez l'explication** : comprendre *pourquoi* un point est anormal oriente l'action.
