# Runbook — Retour arrière en production

**Quand l'utiliser :** une version vient d'être promue en production et se révèle
mauvaise (régression fonctionnelle, panne, corruption de données).

```bash
ssh root@62.238.11.20
export KUBECONFIG=/etc/rancher/k3s/k3s.yaml
NS=qualitos
```

## Le piège à connaître avant tout

**Un `helm rollback` seul ne suffit pas.**

Les migrations Flyway ne se défont pas. Si la version fautive a joué une
migration, revenir à l'image précédente la place devant un schéma qu'elle ne
connaît pas : selon les cas, elle démarre et écrit de travers, ou refuse de
démarrer. Le retour arrière complet restaure **d'abord la base**, ensuite
l'image.

C'est pourquoi `deploy.sh` prend un vidage de sûreté juste avant chaque mise à
jour d'un environnement déjà installé : le filet doit dater du saut, pas de la
nuit précédente.

## 1. Établir ce qui tourne et vers quoi revenir

```bash
helm history qualitos-prod -n $NS
kubectl -n $NS get deploy -o custom-columns=NAME:.metadata.name,IMAGE:.spec.template.spec.containers[0].image
kubectl -n $NS exec deploy/postgres -- psql -U qualitos -d qualitos_quality -t \
  -c "select max(version::int) from flyway_schema_history where success;"
```

Noter : la révision Helm visée, le tag d'image correspondant, et la version de
schéma actuelle.

## 2. Décider si la base doit être restaurée

```bash
kubectl -n $NS get jobs -l app.kubernetes.io/name=postgres-backup --sort-by=.status.startTime
```

- **La version fautive n'a joué aucune migration** (version de schéma inchangée
  depuis la version précédente) → passer directement à l'étape 4. Un simple
  retour d'image suffit.
- **Elle en a joué au moins une**, ou des données ont été corrompues → étape 3.

## 3. Restaurer la base depuis le vidage pris avant le déploiement

Le vidage porte le nom de dossier du jour dans `/backups`. Suivre la section
« Restaurer une base entière » de [`sauvegarde-et-restauration.md`](sauvegarde-et-restauration.md),
en **arrêtant d'abord les services qui écrivent** :

```bash
kubectl -n $NS scale deploy/api-core deploy/api-quality-engine deploy/api-iot-hub --replicas=0
```

⚠️ Tout ce qui a été écrit depuis le déploiement fautif est perdu. C'est le prix
du retour arrière, et c'est pourquoi il se décide vite.

## 4. Revenir à la révision Helm précédente

```bash
helm rollback qualitos-prod <révision> -n $NS --wait --timeout 10m
```

Sans numéro, Helm revient à la révision immédiatement précédente.

## 5. Vérifier la cohérence entre l'image et le schéma

```bash
kubectl -n $NS get deploy -o custom-columns=NAME:.metadata.name,IMAGE:.spec.template.spec.containers[0].image
kubectl -n $NS exec deploy/postgres -- psql -U qualitos -d qualitos_quality -t \
  -c "select max(version::int) from flyway_schema_history where success;"
kubectl -n $NS logs deploy/api-quality-engine --tail=50 | grep -iE "flyway|error|exception"
curl -s -o /dev/null -w "%{http_code}\n" https://qualitos.openlabconsulting.com/fr/
```

Attendu : la version maximale de schéma correspond à ce que l'image restaurée
attend, aucune erreur Flyway au démarrage, et le site répond `200`.

## 6. Remettre les réplicas

```bash
kubectl -n $NS scale deploy/api-core deploy/api-quality-engine --replicas=2
kubectl -n $NS scale deploy/api-iot-hub --replicas=1
```

## Si aucune révision de repli n'existe

`helm history` peut ne montrer qu'une seule révision (cas d'une toute première
installation). Il n'y a alors rien vers quoi revenir : la seule voie est de
redéployer explicitement un tag antérieur connu.

```bash
cd /var/lib/qualitos/deploy
./infra/k8s/deploy.sh prod <tag-anterieur>
```

## Après coup

Consigner dans le journal d'incident : la version fautive, l'heure de détection,
si la base a été restaurée, et le volume d'écritures perdu. Ouvrir une NC interne
— la plateforme sert précisément à ça.
