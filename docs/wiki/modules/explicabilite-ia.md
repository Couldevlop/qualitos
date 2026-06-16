# Explicabilité de l'IA (SHAP) et human-in-the-loop

[← Retour à l'index](../README.md) · *Capacité transverse à plusieurs modules*

## À quoi sert l'explicabilité

Dans QualitOS, l'IA **suggère, l'humain décide**. Pour que cette règle soit tenable, chaque
prédiction ou détection doit être **compréhensible** : on ne fait pas confiance à une « boîte
noire ».

QualitOS fournit une **explication SHAP** : pour une prédiction donnée, elle attribue à chaque
variable d'entrée sa **contribution** (positive ou négative) au résultat. Vous voyez ainsi
*pourquoi* le modèle a abouti à sa conclusion.

## Où la retrouver

- [Détection d'anomalies](anomaly.md) : explication des variables qui rendent un point atypique.
- Indicateurs prédictifs et scorings (ex. scoring fournisseur, [prévision KPI](forecast.md)) :
  contribution des facteurs au résultat.

## Comment l'exploiter

1. Obtenir une prédiction / détection dans le module concerné.
2. Demander l'**explication** associée.
3. Lire les contributions par variable : les plus fortes pointent les leviers d'action.
4. **Valider ou écarter** : la décision finale vous revient.

## Bonnes pratiques

- **Lisez l'explication avant d'agir** : elle évite de réagir à un artefact statistique.
- **Documentez la décision** : conserver pourquoi vous avez suivi (ou non) une recommandation IA
  est précieux en audit.
- **Signalez les incohérences** : si une explication semble absurde, remontez-la — c'est ainsi
  qu'on fiabilise les modèles.
