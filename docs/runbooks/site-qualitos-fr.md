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

**La zone, relevée le 2026-11-06** — et ce qu'il advient de chaque ligne :

| Enregistrement                    | Valeur                        | Sort        |
| --------------------------------- | ----------------------------- | ----------- |
| `NS qualitos.fr`                  | `dns103` / `ns103.ovh.net`    | garder      |
| `A qualitos.fr`                   | `213.186.33.5`                | **→ 62.238.11.20** |
| `A www.qualitos.fr`               | `213.186.33.5`                | **→ 62.238.11.20** |
| `TXT qualitos.fr`                 | `"1\|www.qualitos.fr"`        | **disparaît** avec la redirection |
| `TXT www.qualitos.fr`             | `"3\|welcome"`                | **disparaît** avec la page d'accueil OVH |
| `CNAME ftp.qualitos.fr`           | `qualitos.fr`                 | supprimer (facultatif) |
| `MX qualitos.fr`                  | `mx0..mx3.mail.ovh.net`       | **ne pas toucher** |
| `SPF qualitos.fr`                 | `v=spf1 include:mx.ovh.com ~all` | **ne pas toucher** |
| `SRV _autodiscover._tcp`          | `zimbra1.mail.ovh.net`        | **ne pas toucher** |

Les quatre dernières lignes font fonctionner `contact@qualitos.fr`. Déplacer un
enregistrement A ne les concerne pas : A et MX sont indépendants.

Les deux `TXT` en `1|…` et `3|…` ne sont **pas** à supprimer à la main. Ce sont
des marqueurs qu'OVH pose et réécrit lui-même : les effacer dans la zone les
fait revenir. On retire la fonction qui les a créés, et ils partent avec elle.

**Attention à l'ordre** : retirer la redirection peut emporter l'enregistrement
A du domaine nu. S'il a disparu, il faut alors le **créer** et non le modifier.

État visé :

```
A      qualitos.fr        62.238.11.20     TTL 600
A      www.qualitos.fr    62.238.11.20     TTL 600
MX, SPF, SRV, NS                            inchangés
```

Il n'y a **rien à router** : `213.186.33.5` est l'adresse d'un hébergement
mutualisé OVH, pas une machine à nous, et on ne renvoie pas de trafic depuis
elle. C'est le NOM qu'on déplace. Trois gestes, **dans cet ordre** :

1. **Supprimer la redirection** du domaine nu vers `www` (marqueur `"1|www…"`).
   *Espace client → Noms de domaine → qualitos.fr → onglet **Redirection**.*

2. **Retirer la page d'accueil OVH** sur `www` (marqueur `"3|welcome"`), via le
   multisite de l'hébergement ou la redirection, selon l'offre.
   *Hébergements → votre offre → **Multisite** → retirer `qualitos.fr` et
   `www.qualitos.fr`.*

3. **Poser les deux A** vers `62.238.11.20`.
   *Noms de domaine → qualitos.fr → onglet **Zone DNS**.*

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

**Faite.** La boîte existe et les MX d'OVH sont en place dans la zone. Il ne
reste qu'à donner son mot de passe au service, à l'étape suivante.

Pour mémoire, si elle devait être recréée : espace client → **Emails →
qualitos.fr → Comptes e-mail → Ajouter un compte** (MX Plan, gratuit avec un
`.fr`).

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

Les **MX** (`mx0` à `mx3.mail.ovh.net`) et le **SPF**
(`v=spf1 include:mx.ovh.com ~all`) sont **déjà en place** dans la zone : rien à
faire de ce côté, et surtout rien à y changer en déplaçant les A.

Il n'y a pas d'enregistrement **DMARC**. Ce n'est pas bloquant — SPF suffit pour
que les messages partent et arrivent — mais en ajouter un améliore la
délivrabilité le jour où le volume monte :

```
TXT   _dmarc   "v=DMARC1; p=none; rua=mailto:contact@qualitos.fr"
```

---

## 3. Le secret du relais, sur le nœud

Il est créé **une fois**, à la main, et n'est pas versionné (§18.2.3).

Le mot de passe est **saisi à l'invite**, jamais écrit dans la ligne de
commande : une ligne tapée reste dans l'historique du shell, et une ligne
collée dans un outil reste dans sa trace. `read -rs` ne l'affiche pas et ne le
garde nulle part.

```bash
ssh root@62.238.11.20
export KUBECONFIG=/etc/rancher/k3s/k3s.yaml

kubectl create namespace qualitos-site 2>/dev/null || true

read -rsp 'Mot de passe de contact@qualitos.fr : ' MDP && echo
kubectl -n qualitos-site create secret generic site-contact-smtp   --from-literal=SMTP_HOTE=ssl0.ovh.net   --from-literal=SMTP_PORT=465   --from-literal=SMTP_UTILISATEUR=contact@qualitos.fr   --from-literal=SMTP_MOT_DE_PASSE="$MDP"   --from-literal=SMTP_EXPEDITEUR=contact@qualitos.fr   --from-literal=SMTP_DESTINATAIRE=contact@qualitos.fr
unset MDP
```

Le service **refuse de démarrer** si ce secret manque ou s'il est incomplet.
C'est voulu : un formulaire qui répond « merci » sans que rien ne parte est la
pire des pannes, parce qu'elle est invisible là où elle coûte le plus.

Pour changer le mot de passe plus tard : supprimer le secret, le recréer avec
la commande ci-dessus, puis redémarrer le service — il ne lit sa configuration
qu'au démarrage.

```bash
kubectl -n qualitos-site delete secret site-contact-smtp
# …recréer…
kubectl -n qualitos-site rollout restart deployment/site-contact
```

Et refaire l'envoi d'essai du §6 : c'est le seul contrôle qui prouve que la
chaîne complète tient.

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
  quoi tout le monde partagerait le quota du proxy. C'est le **dernier** maillon
  de la liste qui est retenu, pas le premier : la liste s'allonge par la droite,
  et le maillon de gauche est celui que l'appelant a écrit lui-même. Le lire
  laisserait un robot changer d'identité à chaque requête, et la borne ne
  bornerait plus personne.

  > Cela suppose **un seul proxy de confiance** devant le service, qui ajoute
  > l'adresse réelle de la connexion (`$proxy_add_x_forwarded_for`, le défaut
  > d'ingress-nginx). Si le contrôleur passait un jour à
  > `use-forwarded-headers: true`, ou si un second proxy s'intercalait,
  > relire cette hypothèse. L'erreur, dans ce cas, resserre la limite au lieu de
  > l'ouvrir — tout le monde partagerait un compteur — ce qui se voit.
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
