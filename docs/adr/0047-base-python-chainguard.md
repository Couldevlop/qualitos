# ADR 0047 — Bases Python : passage de distroless (gcr.io) à Chainguard

- **Statut** : Accepté
- **Date** : 2026-08-02
- **Owners** : @Couldevlop
- **Amende** : [ADR 0013](./0013-ai-service-distroless-python-alignment.md) — le
  principe « pas de venv, dépendances dans `/deps` exposées par `PYTHONPATH`, même
  interpréteur au builder et au runtime » est **conservé** ; seul le fournisseur
  de la base change, et avec lui l'ancrage sur Python 3.11.

## Contexte

Les trois composants Python du dépôt (`apps/ai-service`, `apps/ai-vision-5s`,
`infra/edge/inference`) tournaient sur `gcr.io/distroless/python3-debian12`.

Cette base n'a **pas de gestionnaire de paquets** : c'est sa qualité (surface
d'attaque minimale) et sa limite. Quand une CVE tombe sur `cpython`, `krb5` ou
`libssl`, nous ne pouvons rien patcher — il faut attendre que Google reconstruise
l'image. Le fichier `.trivyignore` avait été conçu autour de cette attente, avec
une hypothèse écrite noir sur blanc : « rebuild Google, cadence ~2 semaines ».

Le scan direct des images du **2026-08-02** a démenti cette hypothèse :

| Paquet | Installé | Correctif publié | Retard |
| --- | --- | --- | --- |
| `libpython3.11-*`, `python3.11-minimal` | `3.11.2-6+deb12u6` | `deb12u8` | deux crans |
| `libgssapi-krb5-2`, `libk5crypto3`, `libkrb5-3`, `libkrb5support0` | `1.20.1-2+deb12u4` | `deb12u5` | un cran |
| `libssl3` | `3.0.19-1~deb12u2` | `3.0.20-1~deb12u2` | > 5 semaines |

Fait aggravant, la base **java21-debian12**, elle, avait bien été reconstruite
(seul `liblcms2` y subsistait) : le retard était propre à l'image Python, pas
généralisé — donc pas une fatalité qu'on subit partout.

Conséquence pratique : cinq des six exceptions de `.trivyignore` portaient sur
cette seule base, et leurs dates de revue étaient dépassées. Or **CVE-2026-6100**
(use-after-free dans la décompression cpython, exécution de code ou divulgation
d'information) est atteignable : `httpx` décompresse de façon transparente les
réponses `gzip`/`deflate` des fournisseurs LLM amont. La reconduire revenait à
accepter un risque en le formulant comme un faux positif. Ce n'est pas tenable.

## Décision

Migrer les trois images Python vers **Chainguard**, explicitement autorisé par
CLAUDE.md §10.2 (« images distroless (gcr.io/distroless) **ou** Chainguard »).

1. **Builder** = `cgr.dev/chainguard/python:latest-dev` (pip, apk, shell).
   **Runtime** = `cgr.dev/chainguard/python:latest` (sans shell, entrypoint =
   interpréteur, utilisateur `nonroot` 65532 par défaut).
   Les deux étages partagent la même base, donc la **même mineure de Python et la
   même libc** (Wolfi, glibc) — l'invariant d'ADR 0013 est préservé, et il l'est
   désormais *structurellement* au lieu de reposer sur deux numéros de version
   écrits à la main dans le Dockerfile.

2. **La CI teste dans `latest-dev`.** Le job `ai-service-tests` épinglait
   `setup-python 3.11` « pour tester sur la même mineure que la prod ». L'intention
   était juste mais le lien était déclaratif. Le lint, les contrats d'architecture
   et pytest s'exécutent maintenant dans l'image de base du runtime : l'interpréteur
   testé **est** celui livré, quoi que fasse l'amont.

3. **`--only-binary=:all:`** à l'installation. Le runtime n'a ni compilateur ni
   en-têtes ; une dépendance sans roue doit faire échouer le build de façon
   visible plutôt que se compiler en douce dans le builder.

4. **Suppression de cinq exceptions Trivy** au lieu de les reconduire. Il ne reste
   que `CVE-2026-41254` (liblcms2, base **java**).

## Compromis assumé — la version de Python flotte

L'offre gratuite Chainguard ne publie que `latest` / `latest-dev` ; les étiquettes
figées par mineure sont payantes (vérifié : `python:3.11`, `3.12`, `3.13`, `3.14`
sont tous inaccessibles anonymement). La mineure suivra donc l'amont — **3.14.6**
au moment de la migration, contre 3.11 auparavant.

C'est délibéré, et c'est le point 2 qui le rend acceptable : puisque la CI
s'exécute dans cette même image, une montée de version amont **casse la CI
bruyamment** au lieu de livrer en silence sur un interpréteur jamais testé. On
échange une dérive silencieuse contre un échec visible, ce qui est précisément le
défaut que la migration corrige.

Si cette flottaison devient gênante en exploitation, deux sorties existent, dans
cet ordre : souscrire aux étiquettes figées Chainguard, ou épingler par digest et
automatiser la montée (un digest épinglé annule toutefois le bénéfice du rebuild
quotidien — c'est exactement le piège dont on sort).

## Ce que la migration a révélé

Aligner réellement builder et runtime a fait apparaître des défauts qui étaient
masqués :

- **`ai-vision-5s` ne pouvait pas fonctionner.** Le builder était `python:3.13-slim`,
  le runtime distroless embarque 3.11, et les paquets étaient posés via
  `--prefix=/install` → `/usr/local/lib/python3.13/site-packages`, répertoire que
  l'interpréteur 3.11 ne consulte même pas. L'image ne pouvait importer `fastapi`.
- **`infra/edge/inference`** avait le même écart (builder 3.12) ; le cœur
  fonctionnait par accident, uniquement parce que le paquet est aussi copié dans le
  répertoire de travail. L'extra ONNX, lui, était bien invisible.
- **Les extras ONNX étaient insatisfiables** : `onnxruntime==1.19.2` et
  `numpy>=1.26,<2` ne publient aucune roue pour 3.14. Remontés à `onnxruntime==1.28.0`,
  `onnx==1.22.0`, `numpy>=2.3,<3`.
- **`apps/ai-service` n'avait pas de `.dockerignore`** : une construction depuis un
  poste de dev embarquait le virtualenv complet et la suite de tests dans l'image de
  production. La CI ne le voyait pas (checkout propre), donc image locale et image
  CI divergeaient en silence.
- **Les pins d'`ai-vision-5s` traînaient des CVE** : `python-jose 3.3.0`
  (CVE-2024-33663, CRITICAL, confusion d'algorithme JWT — sur le chemin
  d'authentification du service), `Pillow 10.4.0` (treize CVE HIGH sur le décodage
  d'images, soit exactement la surface d'attaque du service), `python-multipart 0.0.20`.

## Conséquences

- ✅ Les trois images scannent à **0 CVE Critique/Haute** (Trivy, `--ignore-unfixed`).
- ✅ `.trivyignore` passe de six exceptions à une, et la seule restante ne concerne
  plus Python.
- ✅ Interpréteur testé = interpréteur livré, par construction.
- ✅ `ai-vision-5s` et l'extra ONNX d'`edge-inference` sont réparés.
- ⚠ La mineure de Python n'est plus maîtrisée par nous (voir « Compromis assumé »).
- ⚠ `libmagic` doit être installé dans le builder et copié explicitement dans le
  runtime (`/usr/lib/libmagic.so.1*`, `/usr/share/misc/magic.mgc`) — le sniffing MIME
  d'`image_safety` est un contrôle de sécurité, on ne le laisse pas se dégrader en
  silence vers le repli Pillow.
- ⚠ La liste des dépendances runtime reste **dupliquée** dans le Dockerfile — dette
  héritée d'ADR 0013 (flat-layout setuptools), non résorbée ici.

## Tests d'invariant

- `docker build` des trois images, puis :
  - `ai-service` : `GET /healthz` et `GET /readyz` → 200 ;
  - `ai-vision-5s` : import de l'app, `magic.from_buffer` reconnaît PNG et JPEG,
    `image_safety.sanitize` accepte un PNG valide ;
  - `edge-inference` : l'entrypoint d'auto-vérification imprime version et backend.
- `trivy image --severity HIGH,CRITICAL --ignore-unfixed` → aucun résultat sur les
  trois images.
- Suites de tests exécutées **dans** `cgr.dev/chainguard/python:latest-dev` :
  ai-service 396 passés / 4 ignorés (couverture 88,44 % ≥ 85), ai-vision-5s 49
  passés avec l'extra ONNX, edge-inference 37 passés.
- Le builder et le runtime doivent référencer la **même** étiquette Chainguard.

## Références

- CLAUDE.md §10.2 (conteneurs : distroless ou Chainguard), §18.2.1 (aucune CVE
  Critique/Haute en CI), §14.2 (scan d'image).
- ADR [0013](./0013-ai-service-distroless-python-alignment.md) (amendé),
  [0029](./0029-vision-5s-toy-onnx-real-path.md) (backend ONNX Vision 5S),
  [0030](./0030-edge-inference-store-and-forward.md) (Edge Gateway).
- `apps/ai-service/Dockerfile`, `apps/ai-vision-5s/Dockerfile`,
  `infra/edge/inference/Dockerfile`, `.trivyignore`, `.github/workflows/ci.yml`.
