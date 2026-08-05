# ADR 0049 — Embeddings du RAG servies par Ollama, et fin du repli silencieux

- **Statut** : Accepté
- **Date** : 2026-08-05
- **Owners** : @Couldevlop

## Contexte

Le RAG (§8.7, §12.2) calculait ses embeddings avec BGE-M3 chargé **dans le processus**
via `FlagEmbedding`, qui suppose torch — plusieurs gigaoctets. L'image de production
n'embarque ni l'un ni l'autre (extra `ml` volontairement exclu, comme Presidio).

L'adaptateur avait prévu ce cas… par un repli silencieux :

```python
if self._model is None:
    return DeterministicEmbedder(self.DIMENSION).embed(texts)
```

Conséquence en production : l'ingestion et l'interrogation utilisaient des vecteurs
dérivés d'un **hachage SHA-256**. Le service répondait normalement, les temps étaient
bons, aucune erreur n'était journalisée — et les voisinages retournés n'avaient
aucun rapport avec le sens des textes. Sur un module dont toute la valeur tient à
citer des sources pertinentes, c'est la pire des pannes : celle qui a l'air de
fonctionner.

## Décision

1. **Déléguer le calcul à Ollama**, déjà déployé et déjà utilisé pour l'inférence.
   Nouvel adaptateur `OllamaEmbedder` (`POST /api/embed`), modèle `bge-m3` : même
   modèle qu'avant, aucun poids dans l'image, un seul endroit où le tirer
   (`ollama pull bge-m3`).
2. **Rendre le choix explicite** : `EMBEDDINGS_PROVIDER` ∈ {`ollama`, `local`,
   `deterministic`}. `local` charge le modèle en processus (déploiement sans serveur
   d'inférence) et **échoue au démarrage** s'il est absent ; `deterministic` reste
   disponible pour le développement et les tests, qui le déclarent alors sciemment.
3. **Supprimer tout repli implicite** : sans configuration exploitable, le conteneur
   refuse de se construire ; toute défaillance du service d'embeddings remonte en
   `ProviderUnavailableError` → 503. Un lot de vecteurs plus court que le lot de
   textes est également refusé : indexer quand même associerait durablement un
   fragment au vecteur d'un autre.

## Alternatives écartées

- **Embarquer torch + les poids dans l'image** : plusieurs gigaoctets par image, des
  minutes de tirage à chaque déploiement, et une surface de CVE considérable pour un
  service dont ce n'est pas le métier. À l'inverse de l'ADR 0047 (bases Chainguard,
  surface minimale).
- **Monter les poids par un volume** : évite le poids d'image mais impose de gérer
  un volume, sa recopie et sa version sur chaque environnement — pour un résultat
  qu'Ollama fournit déjà.
- **Conserver le repli déterministe en production, en le journalisant** : un
  avertissement dans un journal ne se voit pas ; les réponses, elles, sont lues et
  crues. Une panne franche est plus honnête qu'une pertinence dégradée en silence.

## Conséquences

- Provisionnement : `ollama pull bge-m3` sur la machine qui héberge Ollama, puis
  `EMBEDDINGS_PROVIDER=ollama` et `EMBEDDINGS_MODEL=bge-m3` sur l'ai-service
  (préprod et prod). Modèle absent → 503 explicite sur les points RAG, jamais de
  réponse trompeuse.
- La dimension reste 1024 dans les trois modes : une dimension différente rendrait
  la collection Qdrant incompatible d'un environnement à l'autre.
- Les tests déclarent `EMBEDDINGS_PROVIDER=deterministic` : la pertinence sémantique
  du RAG ne s'évalue donc pas en test unitaire, et ne doit pas y être affirmée.
- L'adaptateur `BgeM3Embedder` expose `is_loaded()`, interrogé au démarrage : le
  diagnostic se fait au lancement du service, plus à la première requête d'un
  utilisateur.
