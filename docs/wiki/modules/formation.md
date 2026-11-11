# Module Formation — Parcours, Academy et progression personnelle

[← Retour à l'index](../README.md) · Routes : **`/training`**, **`/academy`**, **`/learning`** ·
Menu : *Opérations › Formation*, *Référentiels › Academy*, *Référentiels › Mon apprentissage*

## À quoi sert ce module

Trois écrans, trois questions distinctes — et c'est volontairement séparé :

| Écran | Question à laquelle il répond | Route |
| --- | --- | --- |
| **Formation** | De quelles **compétences** avons-nous besoin, et qui suit quel parcours ? | `/training` |
| **Academy** | Quel **contenu de formation** suivre, et où en est mon cours ? | `/academy` |
| **Mon apprentissage** | Où en suis-**je** : ceinture, points, badges ? | `/learning` |

La distinction entre les deux premières est structurante : un **parcours de formation** est un
*objectif de compétence* (habiliter quelqu'un à tenir un poste), un **cours d'Academy** est un
*contenu pédagogique*. Les confondre revient à croire qu'avoir lu un support rend compétent.

La **matrice de compétences** (route `/training/competences`), qui dit qui est autonome sur
quoi, est décrite dans sa propre page :
[Efficacité des CAPA et matrice de compétences](efficacite-capa-et-competences.md).

## Parcours pas à pas — l'écran Formation (`/training`)

### Déclarer les compétences

**Nouvelle compétence** : son **code**, son nom, sa **catégorie** et sa description. Le
catalogue des compétences est ce à quoi tout le reste se rattache ; il se filtre par catégorie.

### Construire un parcours

**Nouveau parcours** : son code, son nom, le **rôle cible**, sa **durée**. Un parcours vit en
trois états — **brouillon**, **actif**, **archivé** — et seul un parcours actif se propose à
l'inscription. Sur sa fiche, vous déclarez les **compétences visées** : c'est ce qui fait la
différence entre une liste de modules et un objectif vérifiable.

### Inscrire et suivre

**S'inscrire à un parcours** ouvre une inscription qui passe par quatre états : **inscrit**,
**en cours**, **terminé**, **échoué**. Le tableau des inscriptions donne, par ligne : le
parcours, le statut, la **progression**, la date d'inscription et le **score final**.

## Parcours pas à pas — l'Academy (`/academy`)

### Suivre un cours

1. **Ouvrir** `/academy` : le catalogue des cours, chacun annonçant ses **points** et son
   **seuil de réussite**.
2. **S'inscrire** à un cours.
3. Le **lecteur de cours** déroule la structure : un cours contient des **modules**, un module
   contient des **leçons** et **un quiz**.
4. Parcourir les leçons ; chaque leçon vue est enregistrée une fois pour toutes — revenir
   dessus ne fausse rien.
5. **Passer le quiz** du module. Il est corrigé automatiquement : le score est le pourcentage
   des points des questions justes, comparé au seuil annoncé. La bonne réponse ne vous est
   **jamais** servie avant votre validation.

Un cours est **terminé** quand toutes ses leçons ont été vues, tous ses quiz passés, et que la
moyenne atteint le seuil du cours. L'écran l'annonce avec le score obtenu.

### Le classement

L'Academy affiche un **classement** : rang, apprenant, **ceinture**, points, nombre de
complétions et nombre de badges.

### Le certificat

Terminer un cours **émet un certificat**. Sa page donne trois éléments que l'on peut opposer à
un tiers :

- un **code de vérification**,
- l'**empreinte SHA-256** du document,
- la trace de son **ancrage**.

Le certificat s'imprime ou s'enregistre en PDF. Il est **signé** par la plateforme avec une
signature hybride (classique **et** post-quantique) et son empreinte est **ancrée**. La
vérification est **publique** : un tiers qui dispose du code peut la demander sans compte, et
un certificat retouché ressort avec une signature invalide.

Un cours s'**exporte** par ailleurs au format **SCORM 2004** (archive avec son manifeste et une
page par leçon) pour être rejoué dans un LMS externe, et la complétion d'une inscription s'émet
sous forme d'énoncés **xAPI**. Ces deux exports sont servis par l'API
(`/courses/{id}/export/scorm`, `/enrollments/{id}/export/xapi`) ; **aucun bouton ne les propose
encore dans les écrans** — demandez-les à votre administrateur.

## Parcours pas à pas — Mon apprentissage (`/learning`)

C'est la vue personnelle : votre **ceinture actuelle**, le nombre de **points qu'il vous reste
pour la suivante** (ou la mention du palier maximal), vos points, vos **parcours terminés**,
votre **meilleur score** et vos **badges**. Les parcours actifs disponibles y sont listés, et
un parcours suivi peut y être marqué comme terminé.

## Comment lire la progression

Les **ceintures** suivent le vocabulaire Lean Six Sigma et des seuils de points fixes :

| Ceinture | Points requis |
| --- | --- |
| Blanche | 0 |
| Jaune | 100 |
| Verte | 300 |
| Noire | 700 |

Les **badges** récompensent des jalons distincts des ceintures : premiers pas, apprenant
assidu, champion de la qualité, perfectionniste, et les badges de ceinture.

Le même moteur de progression sert aux deux côtés : terminer un **cours** d'Academy crédite les
points de votre progression, recalcule votre ceinture et vos badges. Il n'y a pas deux comptes
de points dans la plateforme.

## Bonnes pratiques

- **Déclarez la compétence avant le parcours.** Un parcours sans compétence visée ne peut pas
  être évalué : il ne reste que le temps passé.
- **Gardez le parcours en brouillon** le temps de le construire. Un parcours *actif* se propose
  immédiatement à l'inscription.
- **Ne confondez pas certificat et habilitation.** Le certificat prouve qu'un contenu a été
  suivi et réussi ; l'autonomie sur un poste se lit dans la
  [matrice de compétences](efficacite-capa-et-competences.md).
- **Donnez le code de vérification** quand on vous demande une preuve de formation : il se
  vérifie sans accès à la plateforme.
- **La gamification sert l'assiduité, pas la conformité.** Un classement motive ; il ne
  remplace jamais une évaluation de compétence par un formateur.
