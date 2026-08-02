# Dépendances d'état de QualitOS (hors chart applicatif)

Le chart `infra/k8s/qualitos` déploie **uniquement les applications** (frontend,
APIs Spring, passerelle IA). Il attend PostgreSQL, Keycloak, Qdrant et Ollama
comme services externes, décrits dans `values.yaml` sous `externalServices`.

Ce répertoire fournit ces dépendances pour un cluster k3s mono-nœud, dans le même
namespace que l'application. Ce n'est **pas** une configuration de haute
disponibilité : un seul réplica par composant, volumes `local-path`. Elle convient
à une préproduction et à une production de premier palier ; passer à du managé ou
à un opérateur (CloudNativePG, opérateur Keycloak) dès que la disponibilité
devient un engagement.

## Contenu

| Fichier | Rôle |
| --- | --- |
| `00-namespace.yaml` | Namespace, avec l'étiquette utilisée par les NetworkPolicy du chart |
| `10-postgres.yaml` | PostgreSQL 17 + PVC 5 Gi + création des quatre bases + rôle lecture seule NLQ |
| `20-keycloak.yaml` | Keycloak 25, adossé à la base `keycloak` |
| `30-qdrant.yaml` | Qdrant (vector store du RAG) + PVC 2 Gi |
| `40-ollama-external.yaml` | Service sans sélecteur + Endpoints vers l'Ollama de l'hôte |

Les mots de passe ne sont **pas** dans ces fichiers : ils sont lus depuis des
Secrets créés séparément (voir plus bas). Le chart prévoit External Secrets +
Vault (`CLAUDE.md` §10.2) ; tant que Vault n'est pas en place sur ce cluster, on
crée les Secrets à la main et on garde la même forme, pour que la bascule vers
ESO ne change que la provenance des valeurs.

## Ordre d'application

```bash
export KUBECONFIG=/etc/rancher/k3s/k3s.yaml
NS=qualitos-preprod          # puis: qualitos

# 1) Namespace
sed "s/__NAMESPACE__/$NS/g" 00-namespace.yaml | kubectl apply -f -

# 2) Secrets (valeurs à générer, jamais versionnées)
kubectl -n "$NS" create secret generic qualitos-postgres \
  --from-literal=POSTGRES_USER=qualitos \
  --from-literal=POSTGRES_PASSWORD="$(openssl rand -base64 24)" \
  --from-literal=NLQ_RO_PASSWORD="$(openssl rand -base64 24)"

kubectl -n "$NS" create secret generic qualitos-keycloak \
  --from-literal=KEYCLOAK_ADMIN=admin \
  --from-literal=KEYCLOAK_ADMIN_PASSWORD="$(openssl rand -base64 24)"

# 3) Realm Keycloak (rôles, clients, comptes de démonstration).
#    À faire AVANT de déployer Keycloak : l'import n'a lieu qu'au premier
#    démarrage sur une base vide. Le script réécrit les URI de redirection sur le
#    domaine réel et remplace les mots de passe d'administration ; il les affiche
#    une seule fois, à consigner immédiatement.
export QOS_HOST=preprod.qualitos.openlabconsulting.com
./render-realm.sh "$NS"

# 4) Dépendances. Le placeholder d'hôte de Keycloak est substitué à la volée.
kubectl -n "$NS" apply -f 10-postgres.yaml -f 30-qdrant.yaml -f 40-ollama-external.yaml
sed "s/__KEYCLOAK_HOST__/auth.$QOS_HOST/g" 20-keycloak.yaml | kubectl -n "$NS" apply -f -

kubectl -n "$NS" rollout status deploy/postgres --timeout=180s
kubectl -n "$NS" rollout status deploy/keycloak --timeout=300s
```

## Comptes livrés par le realm

| Compte | Rôle | Mot de passe |
| --- | --- | --- |
| `superadmin` | `super_admin` | généré au rendu, affiché une fois |
| `admin` | `admin_tenant` | généré au rendu, affiché une fois |
| `demo` | `quality_manager`, `user` | `demo` — compte de démonstration assumé |

Le fichier `infra/keycloak/realm-export.json` donne à `superadmin` et `admin` un
mot de passe égal à leur nom. C'est sans conséquence sur `localhost`, mais ce
sont des comptes d'administration : sur un domaine public, les laisser tels quels
reviendrait à publier un accès `super_admin` en clair. `render-realm.sh` les
remplace donc systématiquement. `demo` est conservé tel quel — c'est sa raison
d'être, et il ne porte aucun privilège d'administration.

Les Secrets applicatifs référencés par le chart (`qualitos-api-core`,
`qualitos-api-quality-engine`, `qualitos-api-iot-hub`, `qualitos-ai-service`)
sont à créer ensuite ; ils portent au minimum `DB_USER` et `DB_PASSWORD`
(cf. `application.yml` de chaque service), l'ai-service n'ayant besoin que de ses
clés de fournisseurs LLM si l'un d'eux est activé.

## Ollama

L'hôte fait déjà tourner Ollama et écoute sur `10.42.0.1:11434`, l'adresse de la
passerelle du réseau de pods : les pods peuvent donc l'atteindre directement.
`40-ollama-external.yaml` publie un Service sans sélecteur et ses Endpoints, ce
qui donne aux applications un nom stable (`ollama.<ns>.svc.cluster.local`) sans
dupliquer plusieurs gigaoctets de modèles sur un disque déjà occupé à 80 %.

Si l'adresse de la passerelle change (réinstallation de k3s, changement de CIDR),
c'est ce fichier qu'il faut corriger — la vérifier avec :

```bash
kubectl -n "$NS" run netcheck --rm -it --restart=Never --image=busybox:1.36 \
  -- wget -qO- http://10.42.0.1:11434/api/tags
```
