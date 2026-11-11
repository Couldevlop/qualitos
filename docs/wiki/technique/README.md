# Espace de documentation technique — QualitOS

Cet espace s'adresse aux **développeurs, architectes et exploitants** de la plateforme.
Il répond à la règle §19.1 de `CLAUDE.md`, qui impose deux espaces de documentation : un
espace technique et un espace utilisateur. L'espace utilisateur est à côté, dans
[`docs/wiki/README.md`](../README.md).

> **Ce que cet espace est.** Une carte : il dit où se trouve une décision, un contrat,
> une procédure, et il nomme le fichier qui fait foi. Il ne recopie pas le code, parce
> qu'une copie prend du retard sans prévenir.

---

## 1. Par question

| Vous cherchez | Page |
| --- | --- |
| Comment les services sont découpés, qui appelle qui, sur quels ports | [Architecture (C4)](architecture.md) |
| Quels contrats existent (REST, webhooks, événements), comment les lire | [API et contrats](api-et-contrats.md) |
| Comment les clients sont isolés, où vivent les migrations, comment nommer une table | [Données et multi-tenancy](donnees-et-multi-tenancy.md) |
| Ce qui protège la plateforme, quels modèles de menace sont écrits | [Sécurité](securite.md) |
| Ce que fait la CI, où l'on déploie, quel runbook ouvrir | [Exploitation et CI/CD](exploitation-et-ci.md) |

## 2. Les sources qui font foi

Cet espace **ne décide rien**. Les décisions sont ailleurs, et c'est là qu'il faut les lire :

| Source | Ce qu'elle porte |
| --- | --- |
| [`CLAUDE.md`](../../../CLAUDE.md) | La spécification de la plateforme et les règles non négociables (§18.2) |
| [`docs/adr/`](../../adr/README.md) | Les décisions d'architecture, une par fichier, indexées |
| [`docs/runbooks/`](../../runbooks/README.md) | Les procédures d'exploitation, rangées par la question à laquelle elles répondent |
| [`docs/security/`](../../security/) | Modèles de menace (IoT, NLQ, ai-service, connecteurs), relevés OWASP, CBOM |
| [`docs/PASSATION.md`](../../PASSATION.md) | L'état de l'environnement servi et les pièges connus |

Les décisions **ne se modifient pas** une fois acceptées : une décision remise en cause
donne lieu à un nouvel ADR qui la complète ou la remplace (§22 point 7).

## 3. Comment contribuer à cet espace

Une fonctionnalité n'est pas livrée sans sa page technique **et** sa page utilisateur
(§18.2 point 7). Concrètement, pour une fonctionnalité nouvelle :

1. l'ADR, s'il y a une décision structurante — et son entrée dans
   [l'index](../../adr/README.md) ;
2. la page utilisateur sous `docs/wiki/modules/`, avec la route et le chemin de menu
   réels ;
3. la mise à jour de la page de cet espace que la fonctionnalité touche (un nouveau
   contrat → [API et contrats](api-et-contrats.md) ; une nouvelle table →
   [Données](donnees-et-multi-tenancy.md) ; une nouvelle surface exposée →
   [Sécurité](securite.md)) ;
4. le runbook, si la fonctionnalité peut tomber en exploitation.

Une règle de rédaction s'applique partout : **ce qui est écrit est vérifiable dans le
dépôt**. Un chiffre sans mesure, une capacité sans code, un protocole sans connecteur
coûtent plus cher qu'une absence — ils se découvrent devant un client.
