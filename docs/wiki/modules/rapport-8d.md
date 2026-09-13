# Rapport 8D (clôture d'une non-conformité)

[← Retour à l'index](../README.md) · Route : **`/nc/:id/8d`** · Ouvert depuis la fiche d'une
[non-conformité](non-conformites.md), bouton **Rapport 8D**

## À quoi sert ce document

Le **8D** (« eight disciplines ») est le format que les clients — l'automobile en tête, via
l'IATF 16949 — demandent à la clôture d'un écart. Il raconte, en huit étapes, ce qui s'est passé
et ce qui fait qu'il ne se reproduira pas.

QualitOS ne vous demande pas de le ressaisir : **cinq disciplines sur huit sont remplies
automatiquement** à partir du travail déjà fait dans les autres modules.

| Discipline | D'où vient le contenu |
| --- | --- |
| **D1 — Équipe** | À saisir : seule une personne sait qui a traité l'écart |
| **D2 — Description du problème** | La non-conformité : référence, constat, gravité, zone, photos |
| **D3 — Endiguement immédiat** | À saisir : ce qui a protégé le client pendant l'analyse |
| **D4 — Cause racine** | Les [Ishikawa](ishikawa.md) et [5 Pourquoi](five-whys.md) rattachés à l'écart, et la cause racine saisie sur la NC |
| **D5 — Actions correctives** | La [CAPA](capa.md) escaladée depuis la NC, et ses actions |
| **D6 — Mise en œuvre et preuves** | Les actions menées à terme et les pièces versées à la CAPA |
| **D7 — Prévention de la récurrence** | Le mode de défaillance [PFMEA](produit-pfmea-control-plan.md) rattaché et les plans de surveillance du produit |
| **D8 — Reconnaissance de l'équipe** | À saisir : ce que l'équipe a appris |

## Parcours pas à pas

1. **Ouvrir la fiche de la non-conformité**, puis **Rapport 8D**. L'écran montre les huit
   disciplines, telles qu'elles sont à cet instant.
2. **Renseigner D1, D3 et D8**, puis **Enregistrer**. Faites-le au fil du traitement : ces trois
   champs se conservent, et les retrouver à la clôture fait gagner du temps.
3. **Clôturer la non-conformité** (depuis sa fiche). Tant qu'elle ne l'est pas, le rapport ne
   s'émet pas — un 8D affirme qu'un problème est réglé.
4. **Émettre**. Confirmation demandée, et **second facteur d'authentification** exigé : l'émission
   produit un document opposable.
5. **Télécharger le PDF** et le transmettre au client.

## Ce que « partiel » veut dire

Une discipline sans contenu **le dit** — à l'écran comme dans le PDF — et le rapport porte alors
la mention **Partiel**, suivie de la liste des disciplines concernées.

C'est volontaire. Un rapport aux cases remplies à la hâte trompe son lecteur ; un rapport qui
annonce ce qui lui manque reste lisible et honnête devant un auditeur. Vous pouvez émettre un
rapport partiel : à vous de juger si l'écart le justifie.

Deux absences sont fréquentes et normales :

- **D5/D6 vides** quand aucune CAPA n'a été escaladée depuis la NC ;
- **D7 vide** quand l'écart n'est rattaché à aucun mode de défaillance PFMEA et que le produit
  n'a pas de plan de surveillance. Les dispositifs **Poka-Yoke** ne s'y affichent pas : ils se
  rattachent aujourd'hui à un projet [DMAIC](dmaic.md), jamais à une non-conformité.

## Un rapport émis ne change plus

À l'émission, le texte des huit disciplines est **figé**. Même si la CAPA est rouverte ou
l'Ishikawa complété ensuite, le rapport continue de dire ce qu'il disait le jour de la clôture :
c'est ce que le client a lu qui fait foi.

Le PDF porte en pied de page :

- la mention de sa **signature** (Ed25519 + ML-DSA-65, résistante au quantique) ;
- un **QR code** : quiconque le scanne — votre client, un auditeur — vérifie l'authenticité du
  document sans avoir de compte QualitOS. La vérification ne révèle que des faits d'intégrité
  (le document est authentique, voici son empreinte), jamais son contenu.

Si le rapport d'une non-conformité doit être refait, il faut en reprendre le traitement : un 8D
ne se réémet pas.

## Qui peut faire quoi

| Rôle | Lire le rapport et le PDF | Renseigner D1/D3/D8 | Émettre |
| --- | --- | --- | --- |
| Utilisateur, Auditeur | ✅ | — | — |
| Manager Qualité | ✅ | ✅ | ✅ (second facteur) |
| Directeur Qualité, Admin Tenant | ✅ | ✅ | ✅ (second facteur) |

Lire est ouvert à tous : la personne qui a signalé le défaut doit pouvoir lire la suite qu'on y a
donnée.

## Bonnes pratiques

- **Renseignez D3 le jour même.** L'endiguement est ce qu'un client demande en premier, et il
  s'oublie vite une fois la cause trouvée.
- **Reliez l'écart à son mode de défaillance PFMEA** à la saisie de la NC : D7 se remplit alors
  tout seul, avec l'avant/après du RPN.
- **N'émettez qu'une fois la CAPA avancée.** Un 8D émis trop tôt montre des actions décidées mais
  aucune preuve de mise en œuvre.
