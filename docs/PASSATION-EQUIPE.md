# Passation à une équipe — le plan

> **À qui s'adresse ce document.** Au responsable qui organise la reprise, et aux
> personnes qui la vivent. Il ne remplace pas [`PASSATION.md`](./PASSATION.md),
> qui explique **le code** ; il organise **la reprise** : qui apprend quoi, dans
> quel ordre, avec quels premiers travaux, et comment on saura que ça a marché.

---

## 0. Ce qui fait échouer une passation

Quatre échecs classiques. Le plan qui suit est bâti pour les éviter, et chaque
section dit lequel elle neutralise.

| Échec | Ce à quoi ça ressemble | La parade ici |
| --- | --- | --- |
| **Le déversement** | On donne tout, tout de suite. L'équipe lit trois jours et ne retient rien. | Une **séquence** (§3), pas une bibliothèque. Un seul document à lire le jour 1. |
| **La copropriété** | Tout le monde connaît un peu tout, personne ne répond de rien. Le premier incident cherche son responsable. | Des **zones appropriables** (§2), chacune avec un titulaire nommé. |
| **La découverte tardive** | Au bout de deux mois, on apprend qu'une chose ne tourne que sur une machine, ou n'a jamais été vérifiée. | Le **registre des risques** (§4), donné le premier jour et non à la fin. |
| **La passation sans fin** | Personne ne sait dire si c'est fini. L'ancien reste sollicité indéfiniment. | Des **critères de sortie** datés et mesurables (§6). |

---

## 1. La forme réelle du code

Mesuré le 18 septembre 2026 (`bash scripts/passation-chiffres.sh` pour remesurer).
**Ces proportions décident du découpage** : on ne répartit pas une équipe sur un
découpage imaginaire.

| Composant | Volume | Part |
| --- | --- | --- |
| **`api-quality-engine`** | **1 547** fichiers Java, **78 modules** | **89 %** du Java |
| `api-core` | 65 fichiers | auth, tenants, utilisateurs, facturation |
| `api-iot-hub` | 61 fichiers | protocoles terrain |
| `ai-service` | ~115 fichiers Python | IA / ML |
| `blockchain-service` | 6 fichiers | ancrage |
| `apps/web` | **555** fichiers TS, **70 features** | tout le front |
| `libs/` | 4 bibliothèques | crypto, SPI LDAP, industry |

**Le fait à retenir** : le moteur qualité est le projet. Et à l'intérieur, la masse
est très inégale — **dix modules sur soixante-dix-huit** pèsent près de la moitié :

| Module | Fichiers | | Module | Fichiers |
| --- | --- | --- | --- | --- |
| `standards` | **136** | | `training` | 40 |
| `dashboards` | 66 | | `revisionrequests` | 40 |
| `nonconformity` | 58 | | `academy` | 39 |
| `capa` | 47 | | `apqp` | 32 |
| | | | `controlplan` | 31 |

Les soixante-huit autres font en moyenne **14 fichiers**. Une personne en absorbe
plusieurs sans peine ; c'est `standards` qui demande un titulaire à lui seul.

---

## 2. Le découpage en zones

Cinq zones, taillées sur les mesures du §1. **Chaque zone a un titulaire nommé**,
qui répond des incidents, relit les PR du domaine et tient sa documentation.
Titulaire ne veut pas dire seul : cela veut dire *qui répond quand ça casse*.

### Zone A — Méthodes qualité (le cœur métier)

`pdca`, `ishikawa`, `fivewhys`, `fives`, `circles`, `dmaic`, `nonconformity`,
`capa`, `apqp`, `controlplan`, `risk` (FMEA), `product`

~300 fichiers Java + les features front correspondantes. **C'est la zone qui porte
la promesse du produit** et celle qui bouge le plus. À confier à la personne la
plus solide en métier qualité.

**À lire d'abord** : `PASSATION.md` §2 (les six motifs), puis `CapaService.java`
comme exemple canonique du motif de service, puis `eightd/` comme exemple de la
disposition hexagonale. ADR 0068 à 0074.

### Zone B — Standards Hub & conformité

`standards` (136 fichiers), `ims`, `gdpr`, `consent`, `nis2measures`, `ai_act`,
`compliance`

~250 fichiers. **La plus grosse masse unitaire du dépôt.** Très documentaire, peu
algorithmique : un profil rigoureux, à l'aise avec les référentiels normatifs, y
sera plus utile qu'un profil purement technique.

**À lire d'abord** : `CLAUDE.md` §8 (la promesse du module), puis le modèle de
données d'une norme.

### Zone C — Front Angular

`apps/web` : 555 fichiers, 70 features, 6 langues, PWA hors ligne.

**À lire d'abord** : `docs/web-ng0100.md` (l'ornière la plus coûteuse du dépôt),
`docs/web-design-system.md`, `docs/web-i18n.md`, `docs/web-pwa-offline.md`.
Ces quatre documents existent et sont à jour : c'est la zone la mieux outillée.

**Piège de zone** : les traductions sont **générées**. On ne modifie jamais
`messages.*.xlf` ; les textes vivent dans `apps/web/scripts/i18n/*.py`.

### Zone D — Plateforme & exploitation

`api-core` (auth, tenants, facturation), Keycloak, `infra/k8s`, CI/CD, secrets,
observabilité, sauvegardes.

**C'est la zone qui garde les clés.** Elle doit être tenue dès le premier jour,
avant même que le métier soit compris : sans elle, personne ne déploie ni ne
répare.

**À lire d'abord** : `docs/runbooks/README.md` en entier, puis `PASSATION.md` §7.

### Zone E — IA / ML & IoT

`ai-service` (~115 fichiers Python), `api-iot-hub`, `blockchain-service`,
`libs/security-commons-crypto`.

Zone la plus isolée du reste : elle s'apprend sans bloquer les autres. **Un parti
pris à connaître** : les algorithmes sont écrits en **NumPy pur**, jamais en
scikit-learn ni PyTorch dans le domaine (ADR 0022 à 0025).

### Si l'équipe compte moins de cinq personnes

Fusionner dans cet ordre : **E dans D** (l'IA et l'IoT sont peu sollicités), puis
**B dans A**. Ne jamais fusionner **D** dans une autre : la zone qui déploie et
répare doit avoir un titulaire qui n'est pas absorbé par le métier.

---

## 3. La séquence d'intégration

### Jour 1 — tout le monde, ensemble

1. **Faire tourner la plateforme** (`README.md` § Démarrage rapide). Avant toute
   lecture. On ne comprend pas un logiciel qu'on n'a pas vu marcher.
2. Lire **`PASSATION.md` §0** (les six capacités) — et rien d'autre ce jour-là.
3. Lire le **§4 de ce document** (le registre des risques), à voix haute si
   possible. Ce sont les choses qui, sinon, se découvrent au pire moment.
4. **Vérifier ses accès** (§7). Un accès manquant découvert en semaine 3 coûte
   une semaine.

### Semaine 1 — comprendre avant de toucher

- **Tous** : `PASSATION.md` §2 (les six motifs) puis §3 (tracer une requête de
  bout en bout). Faire l'exercice du §3 **soi-même**, sur un autre module que le
  8D — c'est l'exercice qui ancre l'architecture.
- **Tous** : lire les **cinq derniers ADR** (0070 à 0074). Ils montrent comment
  les décisions se prennent et se révisent ici.
- **Zone D** : lire les douze runbooks. Faire une **restauration de sauvegarde à
  blanc**. C'est le seul moyen de savoir qu'elle marche.
- Chacun ouvre **sa** zone et écrit, en une page, ce qu'il en a compris. Ce
  document sert de contrôle : ce qui n'est pas écrit n'est pas compris.

### Semaine 2 à 4 — livrer pour de vrai

Premiers travaux au §5. La règle : **chacun livre une modification de bout en bout
dans sa zone avant la fin du premier mois** — branche, tests, PR, CI verte,
fusion, vérification en préproduction. Peu importe la taille. Ce qui compte est
d'avoir parcouru la chaîne entière une fois.

### Mois 2 — l'autonomie se mesure

- Chaque zone tient ses PR sans aide extérieure.
- Un incident de préproduction est traité par l'équipe seule, du diagnostic au
  correctif.
- Les critères du §6 sont revus ensemble.

---

## 4. Le registre des risques de passation

**La section la plus importante du document.** Ce sont les choses qui ne se
déduisent pas du code et qui, découvertes tard, coûtent cher.

### Ce qui n'existe nulle part ailleurs que dans ce registre

| Fait | Pourquoi c'est un risque |
| --- | --- |
| **Il n'existe PAS de production.** La préproduction EST l'environnement servi, espace `qualitos-preprod`. | Plusieurs runbooks prennent encore `NS=qualitos` par défaut. Une restauration au mauvais endroit ne se rattrape pas. **Ne jamais lancer `deploy.sh prod`.** |
| **Le déploiement est TIRÉ par le cluster**, pas poussé par la CI. | Une CI verte ne veut pas dire « en ligne ». Sans ce fait, on cherche un bug de déploiement qui n'existe pas. |
| **L'application est une PWA.** Après déploiement, un bandeau apparaît et la page **continue de servir l'ancien code** tant qu'on ne recharge pas. | A déjà fait conclure à tort qu'un correctif n'était pas déployé. |
| **Karma sort en code 0 même quand des tests échouent** (« Found 1 load error »). | Une CI maison ou un script qui lit le code de sortie déclarerait vert un front cassé. **Lire la ligne `TOTAL:`.** |
| **`ng test --include` est ignoré** sur les postes constatés. | On croit tester un fichier, on ne teste rien. Lancer la suite entière. |
| **Chrome traduit automatiquement la préproduction.** | On lit « Maison » pour *Home* et on croit à un bug d'i18n. Désactiver la traduction sur le domaine. |
| **Les doublures de test cachent les défauts de traversée de base.** | Un bug d'empreinte a survécu à 46 tests parce qu'un dépôt simulé rend l'objet d'origine. Tout ce qui est haché, scellé ou contraint en base exige un banc sur un vrai PostgreSQL (tag `migration`). |
| **PostgreSQL arrondit à la microseconde**, `Instant` porte la nanoseconde. | Toute empreinte calculée sur un horodatage doit passer par `StorableInstant.micros(...)`, sinon elle ne retombe pas après un aller-retour en base. |

### Contraintes de poste

| Fait | Conséquence |
| --- | --- |
| `TEMP`/`TMP` doivent pointer sur un disque ayant de la place (`D:\tmp` sur le poste d'origine, `C:` étant saturé). | Sinon Maven échoue de façon obscure. |
| Testcontainers : Ryuk échoue par moments → `TESTCONTAINERS_RYUK_DISABLED=true`. | Mais alors les conteneurs survivent aux exécutions interrompues et saturent la mémoire **en cascade**. Réflexe : `docker container prune -f`. |
| Le PostgreSQL local du compose écoute sur **5434**, pas 5432. | Un PostgreSQL natif occupe souvent le port standard ; le conflit se manifeste par un démarrage qui *semble* réussir mais parle à la mauvaise base. |
| La suite backend complète est lourde. | Sur une machine contrainte, elle peut être tuée avant la fin. **C'est alors la CI qui fait foi**, et il faut le dire plutôt que de supposer.

### Dette technique connue, assumée

| Sujet | État |
| --- | --- |
| **Tests end-to-end** | Ils couvrent la navigation, le Standards Hub et la vérification CAPA. **Ni l'APQP ni le 8D.** C'est la dette la plus rentable à combler. |
| **Couverture front** | Seuil global à 93 %, marge mince. Une fonctionnalité peu testée fera échouer la CI. |
| **Journal d'audit hors transaction** | L'émission d'un 8D et l'approbation d'un control plan écrivent le journal dans une transaction séparée. Un incident entre les deux laisserait un acte non journalisé. |
| **Modules en disposition plate** | `capa`, `nonconformity`, `apqp`, `risk` n'ont pas la découpe hexagonale. À migrer **quand on y retouche**, pas avant. |
| **`CLAUDE.md` est une spécification, pas un état des lieux** | Il décrit la cible (Java 25, microservices…) ; le code est en **Java 21** et en modulith. Lire `PASSATION.md` pour ce qui **est**, `CLAUDE.md` pour ce qui **est visé**. |

---

## 5. Les premiers travaux

Choisis pour être **réels, utiles, et de difficulté croissante**. Chacun fait
parcourir la chaîne complète.

| # | Travail | Zone | Ce qu'il enseigne |
| --- | --- | --- | --- |
| 1 | **Tests e2e de l'APQP** — recopier le patron de `capa-verification.spec.ts` | A + C | La chaîne entière, sans risque : un test qui échoue ne casse rien. Le meilleur premier travail. |
| 2 | **Tests e2e du rapport 8D** | A + C | Idem, sur le module le plus récent et le plus représentatif. |
| 3 | **Styler le bloc de vérification du dialogue d'édition CAPA** | C | Une dette visuelle identifiée et documentée. Petit, visible, gratifiant. |
| 4 | **Journaliser dans la même transaction** (8D, control plan) | A | Touche à un invariant. À faire **après** avoir compris le motif du document scellé. |
| 5 | **Migrer un module plat en hexagonal** (`capa` par exemple) | A | Le plus formateur, et le plus risqué. À ne lancer qu'au mois 2. |

**Règle** : aucun de ces travaux ne se fait sans son test et sans sa PR. La
« Definition of Done » est au §20 de `CLAUDE.md` et elle n'est pas négociable —
c'est elle qui a maintenu la qualité jusqu'ici.

---

## 6. Comment savoir que la passation a réussi

Critères mesurables, à évaluer ensemble aux dates dites.

### J+30

- [ ] Chaque personne a **lancé la plateforme** sur son poste, seule.
- [ ] Chaque zone a un **titulaire nommé**, qui a écrit sa page de compréhension.
- [ ] Chaque personne a **livré une modification de bout en bout** (PR, CI verte,
      fusion, vérification en préproduction).
- [ ] La zone D a **rejoué une restauration de sauvegarde à blanc**.
- [ ] Tous les accès du §7 sont détenus.

### J+60

- [ ] Un **incident de préproduction** a été traité par l'équipe seule.
- [ ] Un **retour arrière** a été exécuté au moins une fois, à blanc ou en réel.
- [ ] L'équipe a **rédigé un ADR** pour une décision qui lui appartient.
- [ ] Les tests e2e couvrent l'APQP **et** le 8D.

### J+90 — la passation est finie

- [ ] Aucune sollicitation de l'équipe précédente depuis 30 jours.
- [ ] L'équipe a **refusé ou révisé** une décision de conception antérieure, ADR à
      l'appui. C'est le vrai signe de l'appropriation : on ne s'approprie pas un
      code qu'on n'ose pas contredire.

---

## 7. Les accès à détenir avant le jour 1

À vérifier **avant**, pas pendant. Un accès manquant découvert en semaine 3 coûte
une semaine.

| Accès | Pour quoi | Qui en a besoin |
| --- | --- | --- |
| Dépôt GitHub (écriture) | Livrer | Tout le monde |
| Droits sur les workflows GitHub Actions | Relancer un job, lire les traces | Zone D |
| **SSH du nœud de préproduction** (`root@62.238.11.20`) | Exploiter, réparer | Zone D, au moins deux personnes |
| Console admin Keycloak (préproduction) | Comptes, rôles, second facteur | Zone D |
| MinIO (préproduction) | Pièces jointes, preuves | Zone D |
| Clé du fournisseur de modèle IA | `ai-service` en mode Mistral | Zone E |
| Registre d'images GHCR | Lire les images publiées | Zone D |

> **Deux personnes minimum** sur l'accès SSH et Keycloak. Un accès détenu par une
> seule personne est une panne en attente.

---

## 8. Par où commencer, en une phrase

Faites tourner la plateforme, lisez `PASSATION.md` §0 puis §2, nommez les
titulaires de zone, distribuez le registre du §4 le premier jour — et faites
livrer à chacun un test end-to-end de l'APQP dans les trois semaines.
