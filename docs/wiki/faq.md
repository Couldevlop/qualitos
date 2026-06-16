# FAQ — Questions fréquentes

[← Retour à l'index](README.md)

### 1. Comment me connecter à QualitOS ?

Avec les identifiants fournis par l'administrateur de votre organisation
([Admin Tenant](roles/admin-tenant.md)). Si votre entreprise utilise un annuaire (LDAP / Active
Directory) ou un SSO, vous vous connectez avec vos identifiants habituels.

### 2. Je ne vois pas un module dont on m'a parlé. Pourquoi ?

Trois raisons possibles : le module **n'est pas activé** pour votre organisation, **votre rôle**
ne vous y donne pas accès, ou il appartient à un domaine non couvert par votre pack sectoriel.
Rapprochez-vous de votre Admin Tenant. Consultez la page de [votre rôle](README.md#1-naviguer-par-rôle)
pour savoir ce à quoi vous avez droit.

### 3. Quel rôle ai-je et qu'est-ce que ça change ?

Votre rôle détermine les écrans visibles et les actions autorisées. Les 6 rôles principaux sont
décrits dans l'espace [Naviguer par rôle](README.md#1-naviguer-par-rôle) : Super Admin, Admin
Tenant, Directeur Qualité, Manager Qualité, Auditeur, Utilisateur.

### 4. Puis-je travailler sans connexion Internet (sur le terrain) ?

Oui pour les usages terrain comme les [audits 5S](modules/fives.md). Vos saisies sont mises en
**file d'attente locale** et **se synchronisent automatiquement** au retour du réseau. Vous pouvez
consulter cette file via la page `/offline-queue`.

### 5. Par où commencer quand j'ai un problème qualité à résoudre ?

- Causes multiples à explorer → [Ishikawa](modules/ishikawa.md).
- Amélioration itérative → [PDCA](modules/pdca.md).
- Projet structuré et chiffré (Six Sigma) → [DMAIC](modules/dmaic.md).
- Écart à déclarer → [Non-conformité](modules/non-conformites.md), puis [CAPA](modules/capa.md).

### 6. Quelle différence entre une NC et une CAPA ?

Une [NC](modules/non-conformites.md) **constate** un écart. Une [CAPA](modules/capa.md) **organise
les actions** pour corriger la cause et prévenir la récidive. Une NC peut déclencher une CAPA.

### 7. Qu'est-ce que le SPC, et en quoi diffère-t-il de la détection d'anomalies ?

Le [SPC](modules/spc.md) applique des **règles statistiques connues** (les 8 règles de Nelson) sur
une série de mesures. La [détection d'anomalies IA](modules/anomaly.md) repère des comportements
inhabituels **sans règle prédéfinie**, sur des données multivariées. Les deux sont complémentaires.

### 8. L'IA peut-elle décider à ma place ?

Non. Le principe est : **l'IA suggère, l'humain décide**. Les prédictions et détections sont
**explicables** (voir [Explicabilité IA](modules/explicabilite-ia.md)) et les actions critiques
nécessitent une validation humaine.

### 9. Que signifie « explicable » pour une recommandation IA ?

Cela veut dire que vous pouvez voir **pourquoi** le modèle a abouti à un résultat : la contribution
de chaque variable (méthode SHAP). Détails sur la page
[Explicabilité IA](modules/explicabilite-ia.md).

### 10. Comment préparer une certification (ISO, IATF, FDA…) ?

Via le [Standards Hub](modules/standards-hub.md) : adoptez la norme, rattachez vos preuves, suivez
votre **score d'alignement**, lancez un **audit blanc** pour repérer les écarts, suivez la
**roadmap** puis générez le **dossier de certification**.

### 11. Qu'est-ce qu'un pack sectoriel et dois-je en activer un ?

Un [Industry Pack](modules/industry-packs.md) pré-configure KPIs, templates et normes pour votre
domaine. L'activer vous fait gagner du temps en partant d'une base pertinente. L'activation peut
relever de l'administration.

### 12. Comment signer ou garantir l'intégrité d'un rapport ?

Les rapports et décisions critiques (audits, dossiers de certification) peuvent être **signés** et
**ancrés** : une empreinte inaltérable (sans donnée personnelle) garantit qu'ils n'ont pas été
modifiés. Les actions critiques requièrent une authentification renforcée (MFA).

### 13. Mes réclamations clients peuvent-elles être triées automatiquement ?

Oui : l'[analyse NLP des réclamations](modules/complaints-nlp.md) propose un **sentiment** et une
**catégorie** pour chaque texte, ce qui aide à prioriser. La validation finale reste humaine.

### 14. Comment repérer des problèmes récurrents parmi de nombreuses NC ?

Utilisez le [clustering de non-conformités](modules/nc-clusters.md) : il regroupe automatiquement
les NC similaires pour révéler des familles de problèmes à traiter à la racine.

### 15. Dans quelles langues l'interface est-elle disponible ?

QualitOS est conçu multilingue. Les langues effectivement disponibles dépendent du déploiement de
votre organisation ; renseignez-vous auprès de votre Admin Tenant.

---

*Une question manque ? Signalez-la à votre référent qualité pour enrichir cette FAQ.*
