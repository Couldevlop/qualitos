# ADR 0109 — Piste d'audit de la plateforme, et ajout seul garanti en base

- **Statut** : Accepté
- **Date** : 2026-11-08
- **Owners** : @Couldevlop
- **Portée** :
  - api-core — paquet `platformaudit` (`PlatformAuditEvent`,
    `PlatformAuditCounter`, `PlatformAuditHasher`, `PlatformAuditOrigin`,
    `PlatformAuditService`, `PlatformAuditController`, `PlatformAuditAction`,
    `PlatformActorType`), migration `V12`, branchement dans `TenantService`,
    `ModulePriceService`, `SubscriptionService`, `PartnerFirmService`,
    `PartnerAccessService`, règle de chemin dans `SecurityConfig`,
    `GlobalExceptionHandler`
  - api-quality-engine — migration `V157` (ajout seul sur `audit_events`)
  - web — console de l'éditeur, écran `/platform/piste-audit`
  - infra — règle d'ingress `/api/v1/platform-audit`
- **Complète** : la piste d'audit **par client** du moteur de qualité
  (`audit_events`, V26, §11.5), qu'elle ne remplace pas.

## Contexte

La plateforme portait une piste d'audit par client, et elle est sérieuse :
numéro de séquence strictement croissant par client, chaîne SHA-256, acteur lu
du jeton, isolation par RLS, surface de consultation et de vérification, écran
dédié. Un client peut donc montrer ce que **ses** utilisateurs ont fait chez
lui.

Il manquait l'autre moitié, et c'est celle qu'une autorité réclame.

### Ce qui n'existait pas

Les actes de **l'éditeur sur le dossier d'un client** n'étaient consignés nulle
part. `api-core` ne portait aucun journal — seulement un
`JpaAuditingConfig` qui horodate des lignes. Or `api-core` exécute précisément
les actes les plus sensibles de la plateforme :

- créer, modifier, désactiver un client ;
- ouvrir ou fermer un module, donc ce que le client peut faire et ce qu'il paie ;
- fixer un tarif du catalogue ;
- créer un cabinet partenaire, y rattacher un consultant, le suspendre ;
- proposer une mission, et surtout **accorder** puis **retirer** l'accès d'un
  tiers aux données qualité d'un client.

Ces gestes ne laissaient qu'une ligne de journal applicatif
(`log.info("Tenant created: …")`). C'est-à-dire rien d'opposable : un journal
applicatif tourne, s'efface, n'est pas chaîné, n'attribue pas l'acte à une
personne nommée, et ne survit pas au redémarrage d'un conteneur. Devant l'ANSSI,
la CNIL, un organisme certificateur ou un client sous obligation DORA qui audite
son prestataire, la réponse était : « nous n'avons pas la trace ». Un
prestataire qui ne peut pas montrer ses propres actes n'est pas auditable, quel
que soit le soin apporté à la piste d'audit qu'il offre à ses clients.

### Le second défaut, sur l'existant

La chaîne d'empreintes de la V26 permet de **constater** qu'une ligne a été
réécrite. Elle ne l'**empêche pas**. Trois faits se cumulaient :

1. le rôle applicatif porte `UPDATE` et `DELETE` sur toutes les tables ;
2. personne ne lance la vérification de chaîne périodiquement — elle existe en
   API et en écran, elle attend un humain ;
3. une suppression des **derniers** actes laisse une chaîne parfaitement
   cohérente : aucune empreinte ne jure, et seul un comptage la verrait.

Entre la réécriture et sa découverte, le journal mentait. Un journal qu'on peut
réécrire sans être arrêté n'est pas une piste d'audit, c'est un historique.

## Décision

### 1. Une piste d'audit de plateforme, unique et transverse

Une table `platform_audit_events` dans `api-core`, avec son compteur
`platform_audit_counters`. **Une seule chaîne** pour toute la plateforme, pas
une par client : l'éditeur est une entité, une autorité l'audite en entier, et
une chaîne par client rendrait invérifiable l'ordre de deux actes visant deux
clients différents — or c'est exactement l'enchaînement qu'un auditeur
reconstitue.

La colonne `target_tenant_id` est portée **à part** de `(target_type,
target_id)`, pour que « tout ce qui a touché ce client » soit un index partiel
et non un balayage. Elle ne s'appelle **pas** `tenant_id`, à dessein : ce nom
déclenche l'isolation par client (filtre Hibernate dans `api-core`, politique
RLS dans le moteur — ADR 0086), qui masquerait à l'éditeur les actes visant les
autres clients. Soit l'inverse de la fonction de cette table.

### 2. Un vocabulaire fermé

`PlatformAuditAction` est une énumération, là où le journal du moteur porte une
chaîne contrainte par une expression régulière. Les actes du moteur sont
ouverts — chaque module métier en ajoute ; ceux de la plateforme ne le sont
pas. Une énumération donne à l'auditeur une liste qu'il peut lire en entier et
confronter au code, là où une chaîne libre l'obligerait à deviner si
`tenant.deactivate` et `tenant.deactivated` sont le même acte.

Le revers est utile : ajouter un pouvoir à l'éditeur force à ajouter une valeur
ici, donc à **décider** s'il est auditable.

### 3. L'attribution n'est jamais un paramètre

L'acteur vient du `sub` du jeton (`CurrentUser`), l'origine du contexte de
requête (`PlatformAuditOrigin`), l'horodatage de l'horloge serveur. Aucun des
trois n'est un argument de `record(...)`. Une piste d'audit dont l'appelant
choisit l'auteur ne vaut rien, et c'est le genre de défaut qu'une revue laisse
passer parce que le code « a l'air » correct.

Sans identité exploitable, l'acte est attribué à `SYSTEM` et le dit. La base
refuse les deux incohérences : un acte humain sans identité, et un acte machine
qui s'inventerait une identité (`ck_platform_audit_human_is_named`).

### 4. L'adresse d'origine entre dans l'empreinte

Différence assumée avec le journal du moteur, qui laisse IP et logiciel hors de
son empreinte. Ces deux champs décrivent la **provenance** d'un acte
d'administration, et c'est l'une des premières choses qu'une autorité recoupe
(« ce changement de tarif vient-il du réseau de l'éditeur ? »). Hors empreinte,
ils seraient modifiables sans que la vérification s'en aperçoive — autant ne pas
les écrire.

Limite écrite dans le code : `X-Forwarded-For` est fourni par le client. Le
champ ne vaut que parce que l'ingress le réécrit. C'est un indice de
provenance, pas une preuve d'identité ; l'identité, elle, vient du jeton signé.

### 5. L'ajout seul est garanti par un déclencheur, en base

`V12` (plateforme) et `V157` (moteur) posent des déclencheurs qui refusent
`DELETE` et `UPDATE` avec le code SQLSTATE `42501`, et qui refusent qu'un
compteur de séquence **recule** ou disparaisse.

**Pourquoi un déclencheur et non un `REVOKE`.** `RlsSchema` ré-accorde
`SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public` au rôle
applicatif après **chaque** migration, par un rappel Flyway. Un `REVOKE` écrit
dans une migration serait donc défait au déploiement suivant, **silencieusement**.
Le déclencheur ne dépend d'aucun droit et s'applique jusqu'au propriétaire de la
base.

**Pourquoi refuser qu'un compteur recule.** C'est la réécriture de l'histoire
par l'**ajout** plutôt que par la modification : un compteur ramené en arrière
rouvre un numéro de séquence déjà employé. La contrainte d'unicité refuserait le
doublon, mais seulement après la bascule ; mieux vaut refuser la bascule.

Seule exception, côté moteur : `blockchain_tx_ref` passe de `NULL` à sa valeur,
**une fois**. C'est une information sur la *preuve*, pas sur le *fait* — elle
n'entre pas dans l'empreinte, donc la renseigner ne peut pas réécrire
l'histoire. La table de plateforme, elle, n'a aucune exception : rien ne s'y
écrit après l'insertion.

### 6. Aucune surface d'écriture n'est exposée

Différence la plus importante avec le journal du moteur, qui porte un `POST`
d'enregistrement. Une surface d'écriture, même réservée au super-administrateur,
permet de **fabriquer** un acte qui n'a pas eu lieu — exactement ce qu'un
auditeur cherche à exclure. Les actes n'entrent que depuis le code qui les
exécute. Le contrat TypeScript et le client Angular n'offrent eux non plus
aucune écriture, et un banc l'interdit.

### 7. Un export scellé

`GET /api/v1/platform-audit/export` rend la fenêtre d'actes, son verdict
d'intégrité et un **sceau** : l'empreinte de la concaténation des empreintes,
bornes et date de génération comprises.

Le sceau répond à ce que les empreintes individuelles ne couvrent pas : un
extrait **amputé de ses dernières lignes** reste parfaitement chaîné. Le sceau
fixe l'extrait comme un tout ; deux copies qui portent le même sceau sont
identiques jusqu'à la dernière ligne.

Il atteste l'intégrité, **pas l'origine** : quiconque détient le dossier peut le
recalculer — c'est justement ce qui le rend vérifiable sans nous. Lier le
dossier à l'éditeur demanderait une signature ML-DSA et un ancrage ; voir
« Ce que cette décision ne règle pas ».

L'export est lui-même consigné (`AUDIT_EXPORTED`) : le seul geste qui fait
sortir le journal de la plateforme serait, sinon, le seul qu'elle ne raconte
pas.

### 8. L'écran dit la limite de la mécanique

Le verdict parle de chaîne **intacte**, pas **valide**, et l'écran ajoute en
clair : « Aucune retouche détectée. La chaîne ne dit rien d'un acte qui n'aurait
jamais été consigné. » Il porte aussi la **portée** du verdict (totalité du
journal, ou page affichée seulement) : un « intacte » obtenu sur vingt-cinq
lignes ne dit rien du journal. Une piste d'audit qui laisse croire plus qu'elle
ne prouve est pire qu'une absence de piste d'audit.

## Conséquences

**La complétude n'est pas une propriété cryptographique.** La chaîne prouve
qu'un journal n'a pas été retouché ; un acte **jamais consigné** ne laisse aucun
vide, aucune empreinte qui jure, aucun trou de séquence. La complétude ne tient
qu'au branchement du journal dans chaque acte sensible — une discipline de code.
Deux bancs la tiennent, et il en faut deux :
`PlatformAuditCoversSensitiveActsTest` lit les sources et attrape l'acte
**déclaré mais jamais branché** ; `PlatformAuditWiringTest` vérifie que l'appel
part bien du vrai service, avec le bon acte et le bon client visé. Le premier ne
sait pas si l'appel est correct, le second ne sait pas s'il en manque.

**L'écriture partage la transaction de l'acte.** Un journal écrit à part
consignerait des actes avortés. Le revers est accepté : un acte dont la
consignation échoue est lui-même annulé — on préfère refuser l'acte que de
l'exécuter sans trace. C'est aussi pourquoi un compteur absent (migration non
jouée) fait échouer l'acte sensible au lieu de recréer un compteur à zéro, qui
réattribuerait des numéros déjà employés.

**Un `REVOKE` sur une table d'audit ne sera jamais la bonne réponse** dans ce
dépôt, tant que `RlsSchema` ré-accorde en masse. C'est une contrainte à
connaître avant d'écrire la prochaine migration de durcissement.

**Effet de bord corrigé en chemin.** Le banc qui vérifie qu'aucune écriture
n'est exposée a révélé que le fourre-tout de `GlobalExceptionHandler`
transformait les exceptions de Spring MVC en **500** : une méthode HTTP non
supportée, un type de contenu refusé, un corps illisible sortaient en « erreur
inattendue ». Le défaut touchait **toute** surface d'api-core — un intégrateur
qui appelait un `GET` en `POST` partait chercher une panne de serveur pour une
faute d'appel. Le gestionnaire honore désormais le statut que le cadre a déjà
choisi.

## Ce que cette décision ne règle pas

Écrit ici plutôt que tu, parce qu'une piste d'audit dont les manques sont tacites
donne une fausse assurance :

1. **Les lectures ne sont pas journalisées.** Qui a consulté le dossier d'un
   client, qui a listé ses utilisateurs : rien. ISO 27001 §A.8.15 le demande, et
   c'est le reproche le plus probable d'un auditeur après celui qu'on vient de
   lever.
2. **Personne ne vérifie la chaîne périodiquement.** La vérification existe en
   API et en écran ; elle attend un humain. Une tâche planifiée qui la lance et
   alerte sur rupture reste à poser.
3. **Le journal n'a pas de politique de conservation.** Ni durée, ni archivage,
   ni purge — et une purge sur une table en ajout seul demandera une décision
   explicite, pas un `DELETE`.
4. **Le dossier exporté n'est pas signé ni ancré.** Il est scellé, donc
   infalsifiable à l'insu du lecteur, mais rien ne le lie à l'éditeur. Le service
   de preuve du moteur de qualité sait signer en ML-DSA et ancrer ; le brancher
   ici est un chantier à part.
5. **L'auteur d'un acte est un identifiant de compte.** L'écran le résout dans
   l'annuaire pour l'afficher ; le dossier exporté, lui, porte l'identifiant brut
   — un nom change, un acte consigné ne doit pas changer avec lui. Une autorité
   qui reçoit le dossier aura donc besoin de la correspondance.
