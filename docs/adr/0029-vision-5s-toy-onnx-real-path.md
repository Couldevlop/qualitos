# ADR 0029 — Modèle ONNX jouet committé pour exercer le vrai chemin d'inférence Vision 5S

- **Statut** : Accepté
- **Date** : 2026-06-19
- **Owners** : @Couldevlop

## Contexte

Le service `ai-vision-5s` (CLAUDE.md §3.2, §12.1) embarque deux backends derrière
le même `InferenceBackend` (Protocol) :

- `OnnxInferenceBackend` — vraie inférence ONNX si un modèle est fourni via
  `VISION5S_ONNX_MODEL_PATH` ;
- `DeterministicStubBackend` — fallback déterministe (scores dérivés du SHA-256
  de l'image) quand aucun modèle n'est disponible / `onnxruntime` absent.

**Problème** : aucun modèle n'était versionné. Le chemin ONNX n'était couvert
qu'en **mockant** `onnxruntime.InferenceSession` (`test_onnx_backend.py`). En
pratique, **seul le fallback stub tournait réellement** — le code de chargement
d'un vrai `.onnx`, l'exécution d'une session `onnxruntime` réelle, et le
décodage de la sortie `[1,5]` n'étaient jamais exercés bout-en-bout. Écart
« annoncé/livré » relevé dans `docs/AUDIT-stub-vs-reel.md` (dette P1 #4).

Contrainte : pas de GPU en CI, pas de gros poids dans le repo, et `onnxruntime`/
`onnx` sont des **dépendances optionnelles** (extra `[onnx]`). On ne peut donc
pas exiger le runtime sur tous les environnements.

## Décision

1. **Script d'export reproductible** `scripts/export_5s_model.py` : construit un
   **vrai graphe ONNX** respectant le contrat du backend — entrée `[1,3,H,W]`
   float32 NCHW 0..1, sortie `[1,5]` scores par pilier 0..1 — via des opérateurs
   ONNX réels :
   `GlobalAveragePool → Flatten → MatMul(W[3,5]) + B[5] → Sigmoid`.
   Poids **fixes** (pas d'aléa) → `.onnx` byte-stable et ré-exportable à
   l'identique.

2. **Modèle jouet committé** `models/vision5s-toy.onnx` (~368 octets) généré par
   le script. Les 5 scores sont une fonction déterministe de la moyenne R/G/B de
   l'image — donc **dépendants des pixels**, mais **ce n'est PAS un modèle de
   qualité**. Son seul rôle : permettre de charger/exécuter un **vrai** `.onnx`.

3. **Test du chemin réel** `tests/infrastructure/test_onnx_real_model.py` :
   pointe `VISION5S_ONNX_MODEL_PATH` sur le modèle committé et vérifie via une
   **session `onnxruntime` réelle** que (a) la factory sélectionne bien
   `OnnxInferenceBackend`, (b) la sortie respecte le contrat (5 scores 0..100 +
   `overall`), (c) elle est déterministe, (d) dépendante de l'image, (e)
   **distincte** du stub. Le module est **skippé** (`pytest.importorskip`) si
   `onnxruntime`/`onnx` ne sont pas installés → la suite reste verte partout.

## Conséquences

- Le **vrai chemin d'inférence ONNX** est désormais exercé en test (plus
  seulement le mock), supprimant l'angle mort « seul le fallback tourne ».
- Les 43 tests existants restent verts ; +6 nouveaux (49 au total quand le
  runtime est présent ; 43 + 6 skipped sinon).
- **Modèle de production toujours à fournir** : YOLOv8 fine-tuné sur labels 5S.
  Le pipeline d'entraînement (dataset non fourni) et l'export ONNX conforme au
  contrat sont décrits dans `docs/runbooks/vision-5s.md` §2.3. Le modèle jouet
  est explicitement à **remplacer** en prod.
- Dépendance d'export documentée : extra `[onnx-export]` (paquet `onnx`) dans
  `pyproject.toml` — distinct de l'extra `[onnx]` (runtime `onnxruntime`).

## Alternatives écartées

- **Ne committer aucun modèle, rester au mock** : laisse le chemin réel
  non exercé (statu quo, dette non résolue).
- **Committer un vrai YOLOv8 ONNX** : plusieurs Mo de poids, GPU pour
  (ré)entraîner, dataset propriétaire — disproportionné pour valider le *chemin*
  d'inférence. Repoussé à la livraison du modèle de prod.
