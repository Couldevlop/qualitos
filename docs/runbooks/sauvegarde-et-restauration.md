# Runbook — Sauvegarde et restauration des bases

**Portée :** les deux environnements Kubernetes, `qualitos` (production) et
`qualitos-preprod` (préproduction). Toutes les commandes supposent :

```bash
ssh root@62.238.11.20
export KUBECONFIG=/etc/rancher/k3s/k3s.yaml    # sans lui, kubectl échoue en silence
NS=qualitos                                    # ou qualitos-preprod
```

## Ce qui est sauvegardé, et quand

| Quoi | Comment | Quand |
| --- | --- | --- |
| `qualitos_core`, `qualitos_quality`, `qualitos_iot`, `keycloak` | `pg_dump -Fc` (format custom, compressé, restaurable table par table) | toutes les nuits à 02h00 UTC |
| Rôles et mots de passe globaux (dont `qualitos_nlq_ro`) | `pg_dumpall --globals-only` | idem |
| Les quatre bases | même vidage, déclenché par `deploy.sh` | juste avant chaque mise à jour d'un environnement déjà installé |

Emplacement : PVC `postgres-backups`, monté sur `/backups`, arborescence
`/backups/AAAA-MM-JJ/<base>.dump` et `/backups/AAAA-MM-JJ/globals.sql`.

Rétention : **14 jours**, purge par le job lui-même. Elle est bornée parce que le
disque du nœud est occupé à 80 % — un disque plein arrête PostgreSQL, et une
sauvegarde non bornée finirait par tuer ce qu'elle protège.

## Ce qui n'est PAS couvert

Les vidages vivent sur le disque du nœud qu'ils protègent. Ils couvrent l'erreur
humaine, la migration ratée et la corruption logique. **Ils ne couvrent pas la
perte de la machine.** Un envoi vers un stockage distant reste à décider
(cf. ADR 0058).

Ne sont pas non plus sauvegardés : les objets MinIO (photos de NC, preuves CAPA)
et les vecteurs Qdrant. Les premiers sont des pièces jointes dont l'original
existe généralement ailleurs ; les seconds se reconstruisent par réindexation.

## Vérifier que la sauvegarde de cette nuit a bien eu lieu

```bash
kubectl -n $NS get cronjob postgres-backup
kubectl -n $NS get jobs -l app.kubernetes.io/name=postgres-backup
kubectl -n $NS logs job/$(kubectl -n $NS get jobs -l app.kubernetes.io/name=postgres-backup \
  --sort-by=.status.startTime -o jsonpath='{.items[-1:].metadata.name}')
```

Attendu : quatre lignes `<base> : <n> octets` puis `sauvegarde <date> terminee`.
Une base dont le vidage pèse moins de 1 Ko fait **échouer** le job : `pg_dump`
écrit toujours un en-tête, un fichier vide signale donc une panne, pas une base
vide.

## Déclencher une sauvegarde immédiate

```bash
kubectl -n $NS delete job sauvegarde-manuelle --ignore-not-found
kubectl -n $NS create job --from=cronjob/postgres-backup sauvegarde-manuelle
kubectl -n $NS wait --for=condition=complete job/sauvegarde-manuelle --timeout=600s
kubectl -n $NS logs job/sauvegarde-manuelle
```

## Restaurer une base entière

⚠️ **Arrêter les services qui écrivent avant de restaurer.** Restaurer sous une
application active produit un mélange des deux états.

```bash
JOUR=2026-08-15            # dossier choisi dans /backups
BASE=qualitos_quality

kubectl -n $NS scale deploy/api-quality-engine --replicas=0
kubectl -n $NS scale deploy/api-core --replicas=0

# Un pod jetable qui monte le volume de sauvegarde et parle à PostgreSQL.
kubectl -n $NS run restauration --rm -i --restart=Never --image=postgres:17-alpine \
  --overrides='{"spec":{"priorityClassName":"qualitos-prod","volumes":[{"name":"b","persistentVolumeClaim":{"claimName":"postgres-backups"}}],"containers":[{"name":"restauration","image":"postgres:17-alpine","stdin":true,"tty":false,"envFrom":[{"secretRef":{"name":"qualitos-postgres"}}],"env":[{"name":"PGHOST","value":"postgres"}],"volumeMounts":[{"name":"b","mountPath":"/backups"}],"command":["sh","-c","sleep 3600"]}]}}'
```

Puis, dans ce pod :

```sh
export PGPASSWORD="$POSTGRES_PASSWORD"
# --clean --if-exists remplace l'existant ; sans lui, pg_restore empile sur des
# tables déjà présentes et échoue objet par objet.
pg_restore -U "$POSTGRES_USER" -d "$BASE" --clean --if-exists --no-owner \
  "/backups/$JOUR/$BASE.dump"
```

Enfin :

```bash
kubectl -n $NS scale deploy/api-quality-engine --replicas=2
kubectl -n $NS scale deploy/api-core --replicas=2
```

## Restaurer une seule table

C'est la raison du format `custom` : on n'a presque jamais besoin de tout.

```sh
pg_restore -U "$POSTGRES_USER" -d qualitos_quality --data-only --no-owner \
  --table=non_conformities "/backups/$JOUR/qualitos_quality.dump"
```

## Restaurer les rôles globaux

Nécessaire seulement après une reconstruction complète de l'instance : les rôles
ne vivent dans aucune base, et sans eux les tables restaurées appartiendraient à
des rôles inexistants — `qualitos_nlq_ro`, créé à l'initialisation et jamais
migré, disparaîtrait sans que rien ne le signale.

```sh
psql -U "$POSTGRES_USER" -d postgres -f "/backups/$JOUR/globals.sql"
```

## Éprouver la restauration — à rejouer chaque trimestre

**Une sauvegarde jamais restaurée est une hypothèse, pas une sauvegarde.**
Le job ci-dessous restaure dans une base jetable et compare à l'originale ; il
échoue si les deux diffèrent.

```bash
cat <<'YAML' | kubectl -n $NS apply -f -
apiVersion: batch/v1
kind: Job
metadata:
  name: essai-restauration
spec:
  backoffLimit: 0
  template:
    spec:
      restartPolicy: Never
      containers:
        - name: restauration
          image: postgres:17-alpine
          envFrom: [{ secretRef: { name: qualitos-postgres } }]
          env: [{ name: PGHOST, value: postgres }]
          command: ["/bin/sh", "-c"]
          args:
            - |
              set -eu
              export PGPASSWORD="$POSTGRES_PASSWORD"
              # Le réseau du pod n'est pas joignable dès le démarrage : sans
              # cette attente, la première connexion est refusée alors que le
              # serveur est sain.
              until pg_isready -h "$PGHOST" -U "$POSTGRES_USER" -q; do sleep 2; done
              JOUR="$(date -u +%Y-%m-%d)"
              SRC="/backups/$JOUR/qualitos_quality.dump"
              psql -U "$POSTGRES_USER" -d postgres -c "DROP DATABASE IF EXISTS essai_restauration;" >/dev/null
              psql -U "$POSTGRES_USER" -d postgres -c "CREATE DATABASE essai_restauration;" >/dev/null
              pg_restore -U "$POSTGRES_USER" -d essai_restauration --no-owner "$SRC"
              c() { psql -U "$POSTGRES_USER" -d "$1" -t -A -c "$2"; }
              TA=$(c essai_restauration "select count(*) from information_schema.tables where table_schema='public'")
              TO=$(c qualitos_quality  "select count(*) from information_schema.tables where table_schema='public'")
              VA=$(c essai_restauration "select max(version::int) from flyway_schema_history")
              VO=$(c qualitos_quality  "select max(version::int) from flyway_schema_history")
              echo "tables restauree=$TA origine=$TO / version restauree=$VA origine=$VO"
              psql -U "$POSTGRES_USER" -d postgres -c "DROP DATABASE essai_restauration;" >/dev/null
              [ "$TA" = "$TO" ] && [ "$VA" = "$VO" ] || { echo "ECHEC"; exit 1; }
              echo "RESTAURATION EPROUVEE"
          volumeMounts: [{ name: b, mountPath: /backups, readOnly: true }]
      volumes:
        - name: b
          persistentVolumeClaim: { claimName: postgres-backups }
YAML

kubectl -n $NS wait --for=condition=complete job/essai-restauration --timeout=600s
kubectl -n $NS logs job/essai-restauration
kubectl -n $NS delete job essai-restauration
```

Dernière épreuve concluante : **15 août 2026**, production — 126 tables contre
126, 108 migrations contre 108, version maximale 109 des deux côtés.
