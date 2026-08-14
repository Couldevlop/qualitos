# ADR 0057 — Des référentiels appartenant à un tenant dans le catalogue de la plateforme

- **Statut** : Accepté
- **Date** : 2026-08-14
- **Owners** : @Couldevlop

## Contexte

Le Standards Hub (§8) livrait un catalogue **exclusivement plateforme** : ISO 9001, ISO 27001
et consorts, maintenues par migrations, partagées par tous les tenants, jamais écrites par eux.
Tout l'aval s'appuie dessus — adoption, roadmap de certification, preuves rattachées aux
exigences, score d'alignement, audit blanc, dossier signé.

Or un audit interne ne se mène pas contre une norme internationale, mais contre **la procédure de
l'organisation**. Un service qualité qui audite sa procédure d'audit interne n'avait aucun
référentiel où l'exprimer : il saisissait ses questions une à une dans la checklist de chaque
audit, en relisant la procédure à côté. Rien ne reliait ces questions au texte qu'elles
vérifient, rien ne les réutilisait d'un audit à l'autre, et rien ne disait contre quelle
**version** de la procédure un audit passé avait été mené.

## Décision

**Le catalogue `standards` accueille les référentiels d'un tenant**, via une colonne
`owner_tenant_id` : `NULL` = norme de la plateforme, renseigné = référentiel appartenant à ce
tenant et invisible à tous les autres. Aucune table nouvelle. Tout l'aval du Standards Hub
fonctionne alors sans modification, y compris ce qui n'avait pas été pensé pour : un tenant peut
adopter sa propre procédure, y rattacher des preuves, en mesurer l'alignement.

Le référentiel **naît d'une procédure approuvée de la GED** (`source_document_id`), dont il
hérite le code, le titre et le numéro de version publiée — figé à la création
(`source_document_version`). Son arborescence naît **vide** : les clauses sont celles de cette
organisation, que seul le tenant connaît. Les deviner produirait un référentiel plausible et
faux, le pire des deux mondes pour un audit.

## Alternatives écartées

**Des entités dédiées (`tenant_referentials`, `tenant_clauses`…).** Isolation triviale — le
`tenant_id` est obligatoire partout — mais il aurait fallu **dupliquer tout l'aval** : une
seconde adoption, un second moteur de preuves, un second calcul d'alignement, un second audit
blanc, ou bien polymorphiser chacun d'eux. Le coût réel n'est pas la table, c'est la moitié du
module réécrite en double, avec deux comportements à maintenir alignés.

**La Row-Level Security PostgreSQL sur `standards`.** Elle aurait porté l'isolation en base,
mais la politique doit alors laisser passer les lignes `owner_tenant_id IS NULL` (le catalogue
partagé) — la moitié de l'invariant reste donc applicative. Elle suppose en outre une variable
de session posée à chaque emprunt de connexion, y compris pour les tâches planifiées et le
chargement des packs sectoriels au démarrage, hors de toute requête. Complexité déplacée, pas
supprimée. Reste envisageable en défense en profondeur.

## Conséquence assumée : l'invariant de filtrage

Le prix de ce choix est **explicite** : aucune lecture du catalogue ne doit se faire sans filtre
de tenant. Une seule lecture non filtrée exposerait les procédures internes d'une organisation à
toutes les autres.

Une première tentative a placé cette garantie dans un test qui interdisait certains NOMS de
méthode sur `StandardRepository`. **Elle n'a pas tenu, et l'échec est instructif** : le dépôt
étendait `JpaRepository`, dont `findById`/`existsById`/`findAll` sont HÉRITÉS — absents de
`getDeclaredMethods()`, donc invisibles au test, et parfaitement appelables. Deux appels vivaient
déjà là (`StandardsService.adopt()` et `requireStandard()`), permettant à un tenant d'adopter le
référentiel privé d'un autre puis d'en lire clauses, preuves et score d'alignement. Une liste
noire de noms ne protège rien tant que le type continue d'hériter de ce qu'elle interdit.

La garantie porte donc désormais sur la **structure du type** : `StandardRepository` se limite au
marqueur `org.springframework.data.repository.Repository<Standard, UUID>` et déclare
explicitement chaque méthode dont l'application a besoin, toutes porteuses d'un tenant. Il n'y a
plus rien à interdire, puisqu'il n'existe plus rien de non filtré à appeler — le compilateur
tient l'invariant, pas une convention.

Trois issues sont distinguées à l'écriture, et la distinction porte du sens : le référentiel du
tenant (on écrit), une norme de la plateforme (**403** — elle existe, elle est visible de tous,
mais son contenu vient des migrations), ou rien du tout (**404**, y compris pour le référentiel
d'un autre tenant, dont on ne confirme pas l'existence).

## Le piège des index partiels

L'unicité globale du code (`uk_standards_code`, V9) n'a plus de sens : deux tenants peuvent
parfaitement appeler leur procédure « PRO-002 ».

Le remplacement naturel — `UNIQUE (owner_tenant_id, code)` — **est faux**. En PostgreSQL, `NULL`
n'est jamais égal à `NULL` : un index composite laisserait donc passer deux normes de plateforme
portant le même code, exactement la garantie qu'on croyait conserver. D'où **deux index
partiels** : `UNIQUE (code) WHERE owner_tenant_id IS NULL` et
`UNIQUE (owner_tenant_id, code) WHERE owner_tenant_id IS NOT NULL`.

Ces index ne sont **pas vérifiables par les tests** : `application-test.yml` désactive Flyway et
fait générer le schéma par Hibernate sur H2, qui ne supporte pas les index partiels. La
vérification a été faite à la main sur le PostgreSQL de la stack, et c'est elle qui a débusqué
que la contrainte d'origine porte un nom explicite (`uk_standards_code`) et non le nom par défaut
`standards_code_key` — un `DROP CONSTRAINT` sur le mauvais nom aurait laissé l'ancienne unicité
en place, et la migration aurait « réussi » sans rien changer.

## La checklist d'audit est une photo, pas un renvoi

Un audit peut viser un référentiel (`audit_plans.standard_id`, V109) et en tirer sa checklist :
chaque exigence devient une question. Les items produits sont des **lignes autonomes**, copiées,
et non des références vers les exigences. Une clause corrigée en mars ne doit pas réécrire le
rapport de janvier : ce qu'un auditeur externe relira est l'état du référentiel au moment où
l'audit a été préparé. C'est aussi pourquoi la suppression d'un référentiel remet seulement le
lien à `NULL` (`ON DELETE SET NULL`) au lieu d'emporter les audits.

La référence de clause portée par chaque question n'est **pas** la concaténation des trois codes.
La plupart des référentiels numérotent en cascade (section « 4 », clause « 4.1 », exigence
« 4.1.1 ») et coller les trois produirait « 4.4.1.4.1.1 », que personne ne peut rapprocher du
texte qu'il a sous les yeux. Un code n'est ajouté que s'il ne porte pas déjà celui de son parent.

## Suppression : ce qui retient le geste

Supprimer un référentiel emporte son arborescence. C'est refusé (**409**) tant qu'une adoption
existe : l'adoption porte les preuves rattachées aux exigences et le score d'alignement du projet
de certification, et les effacer en cascade ferait disparaître un dossier d'audit sans que
personne ne l'ait demandé. La garde interroge les adoptions **sans filtre de tenant** : le
référentiel visé appartient déjà au tenant courant, et une adoption par un autre — impossible
aujourd'hui, mais qu'aucune contrainte de base n'interdit — doit retenir la suppression plutôt
que d'être ignorée.

## Références

- Migrations `V108__standards_owner_tenant.sql`, `V109__audit_plans_standard_id.sql`
- `StandardRepository`, `StandardTenantIsolationTest`, `ProcedureStandardService`
- `AuditService.generateChecklistFromStandard`
- CLAUDE.md §8 (Standards Hub), §18.2-2 (tenant depuis le JWT uniquement)
