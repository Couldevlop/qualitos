# ADR 0053 — Rappel d'échéance des audits : marque d'idempotence en base, brique courriel optionnelle

- **Statut** : Accepté
- **Date** : 2026-08-10
- **Owners** : @Couldevlop

## Contexte

Le module Audit (§4.4) savait planifier un audit — titre, périmètre, type, référentiel,
pilote, date — mais rien ne se passait entre la planification et l'échéance. Un audit
inscrit au programme en janvier pour septembre n'existait, jusqu'en septembre, que dans
la mémoire de celui qui l'avait saisi. Deux manques distincts en découlaient :

1. **Aucune vue des échéances.** La liste des plans pagine tous les audits, tous statuts
   mêlés, dans l'ordre de la base. Rien n'y distingue « dans trois jours » de « l'an
   dernier, jamais lancé ». Un programme d'audit qui ne se lit pas est un programme
   qu'on ne tient pas — et le §4.4 exige justement la *programmation*, pas seulement
   l'enregistrement.
2. **Aucune relance.** Un audit se prépare : convoquer l'audité, réunir les preuves,
   bloquer les agendas, avertir un organisme externe. Ce travail demande des semaines.
   Découvrir l'échéance la veille revient à la manquer.

La difficulté n'est pas d'envoyer un message. Elle est que **l'engine tourne en plusieurs
répliques** (`values.yaml` : `replicas: 2`), chacune avec son propre ordonnanceur. Deux
répliques repèrent le même audit à la même minute. Sans arbitre partagé, le rappel part
deux fois — et un rappel qui arrive en double fait douter de tous les autres. S'ajoute
que l'ordonnanceur s'exécute **hors requête HTTP** : ni `TenantContext`, ni utilisateur
authentifié, alors que tout le reste du moteur suppose les deux (ADR 0001).

## Décision

1. **Écran de planning sur un endpoint dédié** — `GET /api/v1/audits/planning`, distinct
   de la liste paginée. Il renvoie une ligne mince (ni checklist ni constats) par audit
   `PLANNED` daté, trié par échéance croissante, **retards compris** (aucune borne basse).
2. **Le décompte de jours est calculé par le SERVEUR** et transmis (`daysUntil`, négatif
   = retard). Il n'est jamais recalculé dans le navigateur.
3. **Horizon borné, jamais refusé** — défaut 90 jours, maximum 730, valeur nulle ou
   négative ramenée au défaut. Plafond de 200 lignes : le planning se lit d'un coup
   d'œil, il ne s'exporte pas.
4. **Marque d'idempotence en base** — colonne `audit_plans.reminder_sent_at` (V105),
   posée par un **UPDATE conditionnel** `... WHERE reminder_sent_at IS NULL`, dans sa
   propre transaction, **avant** tout envoi. Ce n'est pas une trace a posteriori : c'est
   le verrou.
5. **Balayage sans filtre tenant, adressage par la ligne** — la requête de l'ordonnanceur
   ne filtre pas par tenant ; chaque ligne rapportée porte son `tenant_id`, que le
   service utilise explicitement pour adresser la notification.
6. **Deux canaux, un seul obligatoire** — la notification interne part toujours (pilote
   et audité, nommément). Le courriel ne part que si un destinataire est renseigné sur
   le plan (`reminder_email`, nullable) **et** si la brique SMTP est active
   (`qualitos.mail.enabled=true`). L'envoi est un port (`AuditReminderMailer`) dont
   l'adaptateur SMTP n'est instancié que sous cette condition.
7. **Réglages par variables d'environnement** — délai (30 j), taille de lot (200, plafonné
   à 500), tempo du balayage (horaire), hôte/identifiants SMTP. Aucun secret en dur.

## Justification

**Pourquoi un endpoint dédié plutôt qu'un filtre de plus sur la liste.** La liste pagine
et charge checklists et constats — deux jointures dont un planning n'a que faire, et qui
coûteraient N+1 requêtes pour des colonnes qu'il n'affiche pas. Surtout, le décompte doit
venir d'une horloge unique : ajouter un tri à la liste aurait laissé le calcul au client.

**Pourquoi le décompte côté serveur.** Calculé dans le navigateur, « J-30 » dépendrait du
fuseau et de l'heure du poste. Deux utilisateurs d'un même tenant, l'un à Paris l'autre à
Nouméa, verraient deux échéances pour le même audit — et l'écran cesserait de dire quoi
que ce soit de vérifiable. Le seuil d'alerte de l'écran (30 jours) est d'ailleurs le même
que celui du rappel : les deux doivent affirmer la même chose.

**Pourquoi inclure les retards.** Un audit planifié le mois dernier et jamais lancé est
exactement ce qu'un programme d'audit doit faire remonter. L'exclure produirait un écran
rassurant et faux. Le rappel par courriel, lui, s'arrête à la date du jour : annoncer
« votre audit approche » pour une date dépassée décrédibilise le dispositif entier ; le
retard se lit sur l'écran, qui est fait pour ça.

**Pourquoi un UPDATE conditionnel et non un verrou applicatif.** Un « lire puis écrire »
côté application laisse les deux répliques croire qu'elles sont seules. C'est la base qui
tranche, en sérialisant les deux UPDATE sur la même ligne : la seconde en affecte zéro et
passe son chemin. L'alternative — un verrou distribué (Redis, ShedLock) — ajouterait une
dépendance et un mode de panne pour arbitrer une ligne que la base sait déjà arbitrer.

**Pourquoi réserver AVANT d'envoyer.** L'arbitrage est assumé : au pire un rappel est
perdu (panne entre la réservation et l'envoi), jamais dupliqué. L'ordre inverse —
envoyer puis marquer — garantit le doublon à chaque incident. Un rappel manquant se
rattrape par l'écran de planning, qui montre la même échéance ; un rappel doublé, non.

**Pourquoi pas de transaction englobante sur le passage.** Elle ferait dépendre les
réservations du succès des envois : un relais SMTP en panne annulerait toutes les marques,
et le passage suivant réenverrait tout — précisément le doublon qu'on écarte.

**Pourquoi le courriel est optionnel et le port abstrait.** L'engine ne dispose d'aucun
annuaire capable de traduire `lead_auditor_id` en adresse (l'annuaire d'api-core ne porte
pas de champ de courriel et n'est ouvert qu'aux administrateurs). Déduire une adresse d'un
UUID enverrait dans le vide. Le destinataire est donc saisi, facultatif, et son absence
n'empêche rien : la notification interne, elle, fonctionne toujours. La plateforme suit
déjà ce schéma pour le stockage objet et le relais Kafka — brique OFF par défaut,
adaptateur conditionnel, chemin nominal intact.

**Pourquoi du texte brut.** Le corps porte le titre de l'audit, saisi par un utilisateur.
En HTML il faudrait l'échapper, et un défaut d'échappement dans un courriel se découvre
rarement avant qu'il ne serve (OWASP A03). Le texte brut retire le problème au lieu de le
gérer.

**Pourquoi STARTTLS `required` et non `enable` seul.** `enable` seul se rabat en clair si
le serveur n'annonce pas l'extension : les identifiants partiraient en clair sans qu'aucune
erreur ne le signale (OWASP A02). `required` fait échouer l'envoi plutôt que de le dégrader
en silence.

**Pourquoi le message n'est pas traduit.** Il est produit hors requête : pas d'en-tête
`Accept-Language`, pas d'utilisateur connecté, donc aucune locale à laquelle se fier. Le
français est la langue de référence du produit et la date est écrite en ISO, qui se lit
sans ambiguïté partout. Traduire suppose de savoir à qui l'on écrit — c'est une évolution
(cf. Conséquences), pas un oubli.

## Conséquences

- ✅ Le programme d'audit devient tenable : échéances et retards se lisent sur un écran,
  et l'organisation est prévenue un mois avant, sans intervention.
- ✅ Aucun doublon possible, quel que soit le nombre de répliques — la garantie tient dans
  la base, pas dans une convention de déploiement.
- ✅ L'application démarre à l'identique sans configuration SMTP : la brique courriel est
  inerte, le rappel interne fonctionne.
- ✅ Aucun secret en dur ; échec au **démarrage** si l'envoi est actif sans expéditeur,
  plutôt qu'au premier envoi un mois plus tard dans un ordonnanceur que personne ne regarde.
- ⚠ **Un rappel peut être perdu** si le processus tombe entre la réservation et l'envoi.
  Assumé (cf. Justification) ; l'écran de planning reste le filet.
- ⚠ **Un seul rappel par audit.** Repousser une échéance ne réarme pas la marque : le
  rappel ne repartira pas. Traiter ce cas demande de remettre `reminder_sent_at` à `NULL`
  au changement de date — à faire quand le besoin se manifestera, pas avant.
- ⚠ **Corps du message non traduit** (cf. Justification). Le traduire suppose de rattacher
  une locale au destinataire.
- ⚠ **Nouvelle dépendance** `spring-boot-starter-mail`. L'adaptateur SMTP reste inerte
  tant que `qualitos.mail.enabled=false`.

  *Correction factuelle (2026-08-11, après passage en CI).* La rédaction initiale
  affirmait ici que l'auto-configuration ne crée un `JavaMailSender` que si
  `spring.mail.host` est renseigné. C'est faux : `MailSenderAutoConfiguration` est
  conditionnée à la **présence** de la propriété, et `host: ${MAIL_HOST:}` la définit —
  vide, mais définie. Un `JavaMailSender` était donc créé sans serveur, Boot lui
  adjoignait son indicateur de santé, et `/actuator/health` passait **DOWN** sur une
  brique volontairement éteinte : en cluster, la sonde de disponibilité n'aurait jamais
  été satisfaite. Le job DAST l'a arrêté avant la préproduction. L'indicateur de santé
  du courriel est désormais lié au même interrupteur que la brique
  (`management.health.mail.enabled = ${MAIL_ENABLED:false}`) ; le bean inerte, lui,
  subsiste sans conséquence puisque rien ne l'utilise. La décision n'est pas modifiée —
  seule une justification erronée est rectifiée.

## Tests d'invariant

- `AuditReminderServiceTest` — une ligne déjà réservée par une autre réplique
  (`claimReminder` → 0) n'envoie rien ; un échec d'envoi n'interrompt pas le lot ;
  l'adresse n'apparaît dans aucun journal ; le tenant vient de la ligne, jamais d'un
  contexte ambiant.
- `AuditServiceTest` — horloge **figée** : le planning compte des jours, et l'horloge
  système ferait basculer « J-30 » en « J-29 » entre deux assertions à 23 h 59 UTC.
  Couvre le décompte, le drapeau de retard, le bornage de l'horizon.
- `SmtpAuditReminderMailerTest` — expéditeur absent ⇒ échec au démarrage.
- `AuditReminderPropertiesTest` / `AuditMailPropertiesTest` — délai ou lot non positif
  refusé au démarrage ; lot plafonné.
- `AuditReminderSchedulerTest` — une exception ne remonte jamais à l'ordonnanceur Spring
  (elle tuerait la tâche planifiée jusqu'au redémarrage, sans rien signaler).
- Migration **V105** — index partiel sur `reminder_sent_at IS NULL`, non préfixé par
  tenant : l'ordonnanceur balaie tous les tenants.

## Références

- CLAUDE.md §4.4 (Audit Management), §10.1 (socle), §18.2-2 (tenant jamais lu du body),
  §18.2-3 (aucun secret en clair), §22-9 (jamais de PII en clair dans les journaux)
- OWASP A02 (Cryptographic Failures — STARTTLS), A03 (Injection — corps en texte brut)
- [ADR 0001](./0001-multi-tenant-via-jwt-claim.md) — tenant par claim JWT ; le présent ADR
  en documente la seule exception admise : l'exécution hors requête, où le tenant vient de
  la ligne traitée.
- [ADR 0012](./0012-blockchain-anchoring-fabric.md) — même forme d'ordonnanceur mince et
  de reprise sans doublon.
