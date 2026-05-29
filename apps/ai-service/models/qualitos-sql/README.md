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

**Décision dev** : activer `qualitos-sql` **uniquement avec GPU** ou plus de
marge CPU (ou en relevant `OLLAMA_TIMEOUT_S` et le read-timeout de
`AiGatewayClient`). Sur CPU, on utilise la **variante allégée** ci-dessous.

## Variante allégée `qualitos-sql-lite` (artefact — non active sur CPU)

`Modelfile.lite` : même base, mais `SYSTEM` minimal = **4 exemples few-shot** +
2 garde-fous (le schéma vient déjà du prompt d'`ai-service`, inutile de le
répéter). Build : `ollama create qualitos-sql-lite -f Modelfile.lite`.

Latences **mesurées** (CPU, ce poste, 2026-05-28) :

| Requête | à froid | à chaud (direct) | E2E via ai-service |
| --- | --- | --- | --- |
| **dans** les exemples (5S, criticité) | ~302 s | **~97 s** | < 240 s ✅ (SQL correct) |
| **hors** exemples (fournisseurs) | — | — | **247 s ❌ → 503** |

Constat : le `SYSTEM` few-shot (~200 tokens) accélère les requêtes-exemples
(quasi-copie) mais **alourdit le prompt-eval** ; sur les requêtes nouvelles,
ça dépasse le timeout `OLLAMA_TIMEOUT_S=240` sur CPU sans GPU.

**Décision** : modèle dev actif = **`qwen2.5-coder:3b`** (sans SYSTEM baké → plus
léger, passe sur les requêtes nouvelles). `qualitos-sql` / `qualitos-sql-lite`
restent des **artefacts** à activer **avec GPU** (ou en relevant les timeouts si
une latence élevée est acceptable). Toujours **préchauffer** après un restart
(`OLLAMA_KEEP_ALIVE=-1`).

> Ne pas empiler plusieurs modèles épinglés (`keep_alive=-1`) : ils se disputent
> le CPU. `ollama stop <model>` pour décharger les inutilisés.
