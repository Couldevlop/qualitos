# ADR 0089 — Le rapport d'audit, ses documents et son auditeur responsable

- **Statut** : Accepté
- **Date** : 2026-10-14
- **Owners** : @Couldevlop
- **Portée** : `api-quality-engine` (paquet `audit`, migration V140), `web`
  (`features/audits`)
- **S'inscrit dans** : CLAUDE.md §4.4 (audits), §11.5 (journal), §18.2 #2 et #5,
  ADR 0086 (isolation par client)

## Contexte

Quatre retours sur la section Audits :

1. **« Impossible d'ajouter un constat. »** Le serveur n'accepte un constat que
   sur un audit EN COURS (`AuditService.addFinding` → 409 sinon). L'écran
   laissait pourtant le bouton actif tant que l'audit n'était pas terminé, donc
   sur un audit PLANIFIÉ — l'état de tout audit qui vient d'être créé. Le 409
   revenait sous le message générique « État incompatible — rechargez la page »,
   qu'aucun rechargement ne levait. Le démarrage, lui, exige une checklist non
   vide, autre règle que l'écran ne disait pas. Par ailleurs l'auteur du constat
   (`raisedBy`) était lu dans le corps de la requête, contre §18.2 #2.
2. **Pas de rapport.** Seule existait une synthèse en texte libre
   (`report_summary`), générable par IA, sans conclusion, sans émission, sans
   rien de figé.
3. **Pas de documents** joints à un audit.
4. **« D'où vient lead auditor ? »** L'écran de création recopiait en silence
   l'identifiant de l'utilisateur connecté, et le détail l'affichait brut : un
   UUID sous l'étiquette « Lead auditeur ».

## Décisions

### 1. La règle d'ajout d'un constat est dite, pas découverte

La règle serveur est gardée (un constat se relève pendant l'audit, ISO 19011
§6.4). L'écran barre « Ajouter un constat » hors de l'état EN COURS, écrit la
raison à côté et propose « Démarrer l'audit » quand c'est possible ; le
démarrage est barré, avec sa raison, tant que la checklist est vide. Le 409 du
dialogue a son propre message. L'auteur du constat est lu dans le jeton ; le
champ disparaît du contrat (un ancien client qui l'envoie le voit ignoré).

### 2. Un rapport : brouillon, puis émission qui fige

Table `audit_reports` (un rapport par audit). La **synthèse reste
`audit_plans.report_summary`** — c'est là qu'écrivent déjà la génération par IA
et la clôture ; une seconde colonne aurait fait deux synthèses. Le rapport ajoute
points forts, axes d'amélioration et une conclusion à trois issues
(`FAVORABLE`, `FAVORABLE_WITH_RESERVATIONS`, `UNFAVORABLE`), libellées
« certification recommandée / avec réserves / non recommandée » pour un audit de
certification ou de surveillance.

L'émission (`POST …/report/issue`) :

- n'a lieu que sur un audit **clôturé**, avec synthèse et conclusion (sinon 409
  ou 422) ;
- exige un **second facteur** (`StepUpGuard`, comme le rapport 8D) ;
- recopie les chiffres clés (constats par type, score) et la synthèse : le
  rapport émis se relit tel qu'il a été remis, même si un constat est retouché ;
- calcule une **empreinte SHA-256** du contenu et l'inscrit au **journal chaîné**
  du client (`audit.report.issued`), que l'ancrage périodique scelle ;
- prend l'émetteur et son nom **dans le jeton**.

Ensuite, plus rien ne bouge : brouillon, génération IA et documents sont refusés.
Pas de PDF signé à ce stade : l'empreinte journalisée suffit à prouver
l'intégrité ; un rendu PDF s'ajoutera sur le modèle du 8D si le besoin se
confirme.

### 3. Les documents réutilisent le stockage des preuves

Table `audit_documents`, binaire dans le stockage objet existant (`ObjectStorage`,
le même que les preuves CAPA et APQP), clé construite par le serveur
(`tenants/{client}/audits/{audit}/{uuid}.{ext}`), URL présignée de 15 minutes,
propriétaire déclaré au balayeur d'orphelins. Gardes communes
(`UploadedBinaryGuard`) : liste blanche (PDF, images, docx, xlsx, pptx),
signature binaire, nom assaini. Bornes : 10 Mo par fichier, 20 documents et
50 Mo par audit. Verrou : audit annulé ou rapport émis. Dépôt et retrait sont
journalisés ; le retrait suit la règle générale des suppressions (manager qualité
ou administrateur).

### 4. L'auditeur responsable se choisit, et s'affiche par son nom

Il se choisit dans l'annuaire de l'organisation (`/api/v1/users`, comme le
vérificateur d'une CAPA), l'utilisateur connecté par défaut et en tête. La valeur
reste l'identifiant du **compte de connexion** (le `sub`), ce que les audits
existants portent déjà. Le nom est **recopié** à la désignation
(`audit_plans.lead_auditor_name`) : le moteur n'a pas d'annuaire. Pour soi-même,
le nom vient du jeton ; pour un collègue, de l'annuaire affiché. Sans `leadAuditorId`,
le serveur prend l'utilisateur connecté. L'écran n'affiche jamais l'identifiant :
pour un audit antérieur sans nom, « vous » se lit par le nom de session, un autre
par « Membre de l'organisation (nom non enregistré) ».

## Conséquences

- Migration **V140** ; les deux tables portent `tenant_id`, la politique
  d'isolation leur est posée automatiquement (ADR 0086).
- L'annuaire n'est lisible que par les administrateurs : un manager qualité sans
  ce droit ne peut confier un audit qu'à lui-même, et l'écran le dit. Ouvrir une
  lecture restreinte de l'annuaire (identifiant, courriel) à tous les rôles est
  la suite naturelle, hors de cette décision.
  *Mise à jour (ADR 0091) : c'est fait — l'auditeur se choisit dans l'annuaire en
  lecture restreinte par tout rôle, et le nom recopié est son nom d'annuaire (non
  plus son adresse).*
- Le nom recopié d'un collègue vient de l'écran : il est informatif, pas
  probatoire. L'identifiant, lui, fait foi.
