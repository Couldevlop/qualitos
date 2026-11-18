# QualitOS — L IA, epine dorsale de la plateforme

> Chapitre de la specification produit, extrait de `CLAUDE.md` pour
> n alourdir que les sessions qui en ont besoin. La numerotation des sections
> est celle de la specification.

---

## 12. L'IA, épine dorsale de la plateforme

### 12.1 Capacités IA (matrice complète)

| Capacité                           | Modèle / Approche                              | Cas d'usage                                                |
| ---------------------------------- | ---------------------------------------------- | ---------------------------------------------------------- |
| **Recommandation de méthode**      | Classification (XGBoost) + RAG                 | "Décris ton problème" → suggère PDCA, DMAIC ou Ishikawa    |
| **Génération causes Ishikawa**     | LLM + RAG sur cas historiques                  | Suggestions par branche 6M                                 |
| **Génération de workflow no-code** | LLM (Claude/Mistral) avec output structuré     | "Crée-moi un workflow CAPA en 4 étapes"                    |
| **Suggestion CAPA**                | RAG + similarité cosinus                       | "Pour cette NC, voici les actions qui ont marché ailleurs" |
| **Clustering de NC**               | HDBSCAN + embeddings                           | Détection de patterns invisibles                           |
| **Détection anomalies SPC**        | Isolation Forest, Autoencoders, LSTM-AE        | Dérive d'indicateurs, signaux faibles                      |
| **Vision 5S/EHS**                  | YOLOv8 fine-tuné                               | Encombrement, EPI, étiquetage, zones                       |
| **NLP audits**                     | BERT multilingue + Whisper                     | Analyse de comptes-rendus, transcription Cercle            |
| **Prédiction KPI**                 | LSTM / Prophet / Temporal Fusion Transformer   | "Atteindras-tu ton KPI cible ?"                            |
| **Conformité normative**           | RAG sur référentiel ISO + scoring              | "Es-tu aligné ISO 9001 §8.5 ?"                             |
| **Aide à la décision**             | LLM avec garde-fous (guardrails)               | Synthèse multi-source, recommandations                     |
| **Sécurité (UEBA)**                | Détection comportements anormaux               | Alertes SOC, fraude interne                                |
| **Sentiment réclamations**         | NLP (BERT)                                     | Priorisation auto                                          |
| **Génération de rapports**         | LLM avec citations                             | Rapport d'audit en 30 secondes                             |
| **Natural Language Query**         | Text-to-SQL/Mongo                              | "Combien de NC ce mois ?"                                  |
| **Génération de formulaires**      | LLM structured output                          | "Formulaire d'audit 5S atelier méca"                       |
| **Storyboards**                    | LLM narratif                                   | Récit autour des chiffres mensuels                         |
| **Personnalisation formation**     | RecSys (filtrage collaboratif + content-based) | Parcours adaptatifs                                        |

### 12.2 Architecture IA

- **Inférence** : modèles servis via **TorchServe** (modèles propriétaires) + **vLLM** ou **Ollama** (LLM open-source) ; appel optionnel à Anthropic/OpenAI/Mistral via passerelle abstraite (`AIProvider` interface).
- **MLOps** : MLflow (registre), DVC (datasets), Kubeflow Pipelines, monitoring de drift (Evidently AI).
- **RAG** : **Qdrant** comme vector store, embeddings multilingues (BGE-M3, E5).
- **Garde-fous** : Guardrails AI + NeMo Guardrails pour valider entrées/sorties, anti-prompt-injection.
- **Souveraineté** : option **100 % on-premise** (Ollama + Mistral 7B/Llama 3.1 70B local) pour les tenants régulés.

### 12.3 Explicabilité

Chaque décision IA est **explicable** :

- SHAP / LIME pour modèles classiques.
- Citations de sources pour le RAG.
- Logs des prompts (anonymisés) pour les LLM.
- Visualisation des poids dans le dashboard.

**L'IA suggère, l'humain décide.** Tout effet décisionnel critique passe par validation humaine + signature ML-DSA + ancrage blockchain.

### 12.4 Apprentissage continu (privacy-preserving)

- **Federated learning** optionnel : un tenant peut bénéficier de modèles entraînés sur les données agrégées et anonymisées d'autres tenants du même secteur (opt-in).
- **Differential privacy** pour les benchmarks.
- Garantie : aucune donnée de tenant A n'est jamais exposée à un tenant B.

---

