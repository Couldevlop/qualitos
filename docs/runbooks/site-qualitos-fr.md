# Mettre qualitos.fr en ligne

Le site de présentation et la préproduction vivent sur **la même machine**
(`62.238.11.20`) et ne partagent rien d'autre : namespaces séparés, images
séparées, ingress séparés. Déployer l'un ne touche pas l'autre, et
`preprod.qualitos.openlabconsulting.com` reste disponible pour les
démonstrations exactement comme aujourd'hui.

```
                    62.238.11.20  (k3s, ingress-nginx, cert-manager)
                            │
          ┌─────────────────┴──────────────────┐
          │                                    │
  qualitos.fr                    preprod.qualitos.openlabconsulting.com
  namespace qualitos-site              namespace qualitos-preprod
  ├── site          (nginx statique)   └── la plateforme (inchangée)
  └── site-contact  (formulaire)
```

---

## 1. DNS, chez OVH

Dans l'espace client OVH → **Noms de domaine → qualitos.fr → Zone DNS**, créer
ou modifier **deux** enregistrements :

**Où en est le domaine aujourd'hui** (mesuré le 2026-11-06) :

```
qualitos.fr      → 213.186.33.5, HTTP 302 vers www.qualitos.fr   (serveur « openresty »)
www.qualitos.fr  → 213.186.33.5, HTTP 200
```

`213.186.33.5` est l'adresse d'un hébergement mutualisé OVH : le domaine sert
aujourd'hui une page OVH, pas notre site. Les deux enregistrements existent
donc déjà et sont à **modifier**, pas à ajouter — et la redirection du domaine
nu vers `www` est à supprimer, sinon elle court-circuite la zone.

| Type | Sous-domaine | Cible          | TTL |
| ---- | ------------ | -------------- | --- |
| A    | *(vide)*     | `62.238.11.20` | 600 |
| A    | `www`        | `62.238.11.20` | 600 |

Le sous-domaine vide désigne le domaine nu (`qualitos.fr`). Si un enregistrement
A existe déjà vers une page de redirection OVH ou un hébergement « Start10M »,
**le modifier** plutôt qu'en ajouter un second : deux A pour un même nom
répartissent le trafic entre les deux, et une visite sur deux tomberait sur
l'ancienne page.

Si une **redirection** OVH (type « Redirection web ») est active sur le domaine,
la supprimer : elle court-circuite la zone DNS.

Vérifier la propagation avant d'aller plus loin — tant qu'elle n'est pas faite,
le certificat ne peut pas être émis :

```bash
dig +short qualitos.fr      # doit répondre 62.238.11.20
dig +short www.qualitos.fr  # idem
```

Compter de quelques minutes à deux heures. Un TTL de 600 s rend les corrections
rapides ; il pourra être remonté à 3600 une fois tout vérifié.

---

## 2. La boîte contact@qualitos.fr

Le formulaire envoie un vrai courriel : il faut une boîte et son mot de passe.

OVH fournit **MX Plan** gratuitement avec un domaine `.fr`. Dans l'espace client
→ **Emails → qualitos.fr → Comptes e-mail → Ajouter un compte** : créer
`contact@qualitos.fr`, noter le mot de passe.

Les réglages SMTP d'OVH :

| Réglage        | Valeur                                         |
| -------------- | ---------------------------------------------- |
| Serveur        | `ssl0.ovh.net`                                 |
| Port           | `465` (TLS implicite) — ou `587` avec STARTTLS |
| Identifiant    | l'adresse complète, `contact@qualitos.fr`      |
| Authentifcation| mot de passe de la boîte                       |

> **L'expéditeur est notre propre adresse, jamais celle du visiteur.** Envoyer
> au nom du domaine d'un prospect fait rejeter le message par SPF et DMARC, et
> la boîte ne reçoit rien. C'est `Reply-To` qui porte l'adresse du visiteur :
> répondre depuis la boîte tombe donc bien chez lui.

Pendant qu'on y est, vérifier que la zone DNS contient bien les entrées **MX** et
l'enregistrement **SPF** posés par OVH à la création du MX Plan. Sans SPF, les
messages partis de la boîte arriveront en indésirables.

---

## 3. Le secret du relais, sur le nœud

Il est créé **une fois**, à la main, et n'est pas versionné (§18.2.3) :

```bash
ssh root@62.238.11.20
export KUBECONFIG=/etc/rancher/k3s/k3s.yaml

kubectl create namespace qualitos-site 2>/dev/null || true

kubectl -n qualitos-site create secret generic site-contact-smtp \
  --from-literal=SMTP_HOTE=ssl0.ovh.net \
  --from-literal=SMTP_PORT=465 \
  --from-literal=SMTP_UTILISATEUR=contact@qualitos.fr \
  --from-literal=SMTP_MOT_DE_PASSE='<mot de passe de la boîte>' \
  --from-literal=SMTP_EXPEDITEUR=contact@qualitos.fr \
  --from-literal=SMTP_DESTINATAIRE=contact@qualitos.fr
```

Le service **refuse de démarrer** si ce secret manque ou s'il est incomplet.
C'est voulu : un formulaire qui répond « merci » sans que rien ne parte est la
pire des pannes, parce qu'elle est invisible là où elle coûte le plus.

Pour changer le mot de passe plus tard :

```bash
kubectl -n qualitos-site delete secret site-contact-smtp
# …recréer avec la commande ci-dessus…
kubectl -n qualitos-site rollout restart deployment/site-contact
```

---

## 4. Déploiement

Rien de particulier à faire : **le cluster tire**. Le minuteur systemd qui
déploie déjà la préproduction (`infra/k8s/auto-deploy.sh`) applique aussi
`infra/k8s/site/deploy.sh` au même tag, dans le namespace `qualitos-site`.

Pour le lancer à la main (première mise en ligne, ou reprise) :

```bash
ssh root@62.238.11.20
cd /var/lib/qualitos/deploy
export KUBECONFIG=/etc/rancher/k3s/k3s.yaml
./infra/k8s/site/deploy.sh "$(cat .deployed)"
```

Le site **ne peut pas faire échouer** le déploiement de l'application : il passe
après, et son échec est journalisé sans remettre en cause ce qui est déjà en
place. Pour le désactiver complètement, poser `QOS_DEPLOY_SITE=0` dans
`/etc/qualitos/deploy.env`.

---

## 5. Le certificat

cert-manager le demande automatiquement à Let's Encrypt dès que l'ingress est
appliqué **et** que le DNS pointe sur le nœud. Suivre l'émission :

```bash
kubectl -n qualitos-site get certificate qualitos-fr-tls
kubectl -n qualitos-site describe certificate qualitos-fr-tls | tail -20
```

`READY=True` au bout d'une à deux minutes. S'il reste à `False` :

| Ce qu'on lit                              | Ce que c'est                                         |
| ----------------------------------------- | ---------------------------------------------------- |
| `no such host` / `NXDOMAIN`               | le DNS n'est pas encore propagé — attendre           |
| challenge en `pending` sur `www`          | l'enregistrement A de `www` manque                   |
| `too many certificates already issued`    | quota Let's Encrypt (5 par domaine et par semaine)   |

---

## 6. Vérification

```bash
curl -I https://qualitos.fr                 # 200, et HSTS posé par l'ingress
curl -I https://www.qualitos.fr             # 308 vers https://qualitos.fr
curl -s https://qualitos.fr/robots.txt      # doit nommer le sitemap
curl -s -o /dev/null -w '%{http_code}\n' https://qualitos.fr/healthz   # 200

# Le formulaire, pour de vrai — le message doit arriver dans la boîte.
curl -s -X POST https://qualitos.fr/api/contact \
  -H 'Content-Type: application/json' \
  -d '{"nom":"Essai de mise en ligne","courriel":"votre.adresse@exemple.fr","besoin":"Test"}'
# → {"message":"Demande reçue."}
```

Puis dans un navigateur : remplir le formulaire de la page et vérifier que le
message arrive. **Le faire une fois après chaque changement de mot de passe de
la boîte** — c'est le seul contrôle qui prouve que la chaîne complète tient.

Et vérifier que la démonstration n'a pas bougé :

```bash
curl -I https://preprod.qualitos.openlabconsulting.com
```

---

## 7. Ce qui protège le formulaire

Il est public et sans authentification. Quatre bornes, toutes testées
(`apps/site-contact/tests/`) :

- **Cinq demandes par heure et par adresse**, lue dans `X-Forwarded-For` — sans
  quoi tout le monde partagerait le quota du proxy.
- **Un piège à robots** : un champ invisible que le formulaire laisse vide. S'il
  revient rempli, le service répond « reçu » sans rien envoyer. Lui dire qu'il
  a été repéré ne ferait que lui apprendre à mieux se cacher.
- **Des longueurs bornées**, et le refus des retours à la ligne dans les champs
  courts : un `\nBcc:` dans le champ « nom » ajouterait un destinataire au
  message.
- **32 ko de corps au plus**, refusés sans même être analysés.

Rien n'est stocké, rien du contenu n'est journalisé (§22-9) : ce sont les
coordonnées d'un tiers. On note qu'une demande est passée, pas qui l'a envoyée.

---

## 8. En cas de panne

```bash
kubectl -n qualitos-site get pods
kubectl -n qualitos-site logs deployment/site-contact --tail=50
kubectl -n qualitos-site logs deployment/site --tail=50
```

| Symptôme                                   | Cause la plus probable                                    |
| ------------------------------------------ | --------------------------------------------------------- |
| `site-contact` en `CrashLoopBackOff`       | secret absent ou incomplet — le service le dit au démarrage |
| Le formulaire répond **502**               | le relais refuse : mot de passe changé, ou compte bloqué   |
| Le formulaire répond **429**               | limite de débit atteinte depuis cette connexion            |
| La page s'affiche sans style               | la CSP de l'ingress a été écrasée — voir l'annotation `configuration-snippet` |
| `404` sur `/api/contact`                   | la règle de chemin est passée après la règle fourre-tout `/` |

Le repli reste en place côté navigateur : si le service ne répond pas, le
formulaire ouvre la messagerie du visiteur avec la demande déjà rédigée. Rien de
ce qui a été écrit n'est perdu.
