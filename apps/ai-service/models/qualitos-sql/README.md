# `qualitos-sql` — modèle NLQ spécialisé QualitOS (§7.3)

Modèle local **spécialisé** pour le text-to-SQL QualitOS, dérivé de
`qwen2.5-coder:3b` via un `Modelfile` Ollama.

## Pourquoi pas de fine-tuning par gradient ?

Un vrai fine-tuning (LoRA/QLoRA) d'un modèle 3B exige un **GPU**, des heures de
calcul et des frameworks lourds — indisponibles sur l'environnement de dev (CPU
seul). On « entraîne » donc le modèle par **spécialisation Ollama** : on ancre
dans le `SYSTEM` du `Modelfile` le **schéma réel**, les **règles de sûreté** et
des **exemples few-shot** (question → SQL correct). C'est reproductible,
versionné dans le repo, et matérialisé en un vrai modèle nommé par
`ollama create`.

> Le few-shot est dans `SYSTEM` (et non en blocs `MESSAGE`) car `ai-service`
> appelle `/api/generate` ; les `MESSAGE` ne s'appliqueraient qu'à `/api/chat`.

## Build (100% local, pas de réseau)

`qwen2.5-coder:3b` doit déjà être pull. Puis :

```bash
./build-qualitos-sql.sh            # ollama create qualitos-sql -f Modelfile
# ou : docker exec qualitos-ollama ollama create qualitos-sql -f /tmp/qualitos-sql.Modelfile
```

## Activation

Pointer `ai-service` dessus (docker-compose.dev.yml) :

```yaml
OLLAMA_MODEL: qualitos-sql
```

## ⚠ Compromis latence sur CPU (vérifié 2026-05-28)

Le `SYSTEM` enrichi (schéma + 7 exemples ≈ 700 tokens) **améliore la qualité**
mais **alourdit le prompt-eval**. Sur ce poste **CPU sans GPU**, chaque requête
dépasse alors le timeout configuré (`OLLAMA_TIMEOUT_S=240`) → l'inférence ne
revient pas à temps.

**Décision dev** : le modèle actif reste **`qwen2.5-coder:3b`** (latence
acceptable, corrige déjà les hallucinations grâce au prompt durci côté
`ai-service`). Activer `qualitos-sql` **uniquement avec GPU** ou plus de marge
CPU (ou en relevant `OLLAMA_TIMEOUT_S` et le read-timeout de `AiGatewayClient`
si une latence élevée par requête est acceptable).

Pistes pour réduire la latence sans GPU : alléger le `SYSTEM` (2-3 exemples),
ou retirer le prompt système redondant côté `ai-service` quand ce modèle est
actif (le `SYSTEM` baké suffit alors).
