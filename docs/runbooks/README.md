# Runbooks — index

> Douze procédures d'exploitation. Ce fichier existe pour une raison simple :
> quand on en a besoin, on n'a pas le temps de lire les douze titres pour deviner
> lequel s'applique. Chacun est rangé sous la **question à laquelle il répond**.

## Avant tout : où l'on opère

```bash
ssh root@62.238.11.20
export KUBECONFIG=/etc/rancher/k3s/k3s.yaml   # sans lui, kubectl échoue en silence
NS=qualitos-preprod
```

> **Il n'existe pas de production pour QualitOS.** La préproduction EST
> l'environnement servi (`preprod.qualitos.openlabconsulting.com`), et son espace
> de noms est **`qualitos-preprod`**. Ne lancez jamais `deploy.sh prod`.
>
> Certains runbooks ont été écrits quand un espace `qualitos` « production » était
> envisagé et le prennent encore par défaut. **Vérifiez l'espace avant toute
> commande destructrice** — une restauration jouée au mauvais endroit ne se
> rattrape pas.

Ce qui tourne, en une commande :

```bash
kubectl -n "$NS" get pods,deploy,ingress
kubectl -n "$NS" get deploy -o custom-columns=NOM:.metadata.name,IMAGE:'.spec.template.spec.containers[0].image'
```

La seconde dit **quelle version** est servie. C'est la première question à se
poser quand un correctif « ne semble pas déployé » — souvent il l'est, et c'est le
navigateur qui sert l'ancien code (voir `PASSATION.md` §6.3, le piège PWA).

---

## Réparer — quand quelque chose est cassé

| Runbook | Quand |
| --- | --- |
| [`retour-arriere-production.md`](./retour-arriere-production.md) | Une version vient d'être déployée et se révèle mauvaise. **Contient le piège central : un `helm rollback` seul ne suffit pas.** |
| [`sauvegarde-et-restauration.md`](./sauvegarde-et-restauration.md) | Restaurer une base, ou vérifier qu'une sauvegarde existe vraiment. À lire **avant** d'en avoir besoin. |
| [`chaos-engineering.md`](./chaos-engineering.md) | Éprouver la résistance aux pannes, plutôt que de la découvrir en vrai. |

## Exploiter — quand tout marche et qu'il faut le surveiller

| Runbook | Quand |
| --- | --- |
| [`observability.md`](./observability.md) | Métriques, traces, journaux : où regarder, et quoi. |
| [`nc-photos-storage.md`](./nc-photos-storage.md) | Stockage objet (MinIO) des pièces jointes : quotas, orphelins, URL présignées. |
| [`keycloak-spi-ldap.md`](./keycloak-spi-ldap.md) | Brancher l'annuaire d'un client sur Keycloak (Active Directory, OpenLDAP). |
| [`edge-inference.md`](./edge-inference.md) | Passerelle Edge et modèles embarqués. |
| [`vision-5s.md`](./vision-5s.md) | Service de vision 5S : modèles, seuils, exploitation. |

## Modifier sans casser — les garde-fous du code

| Runbook | Quand |
| --- | --- |
| [`perf-k6.md`](./perf-k6.md) | Le SLO de performance (p95 < 300 ms) et comment le mesurer. |
| [`dast-owasp-zap.md`](./dast-owasp-zap.md) | Scan de sécurité dynamique, en local comme en CI. |
| [`ci-sca-owasp-activation.md`](./ci-sca-owasp-activation.md) | Activer l'analyse de dépendances OWASP en CI. |
| [`secrets-hygiene.md`](./secrets-hygiene.md) | Aucun secret en clair : la règle, et l'outillage qui la tient. |

---

## Ce que ces runbooks ne couvrent pas

- **Démarrer la plateforme en local** → `README.md`, section « Démarrage rapide ».
- **Comprendre le code** → `docs/PASSATION.md`, §2 à §5.
- **Livrer une modification** → `docs/PASSATION.md` §7, et `docs/git-workflow.md`.
