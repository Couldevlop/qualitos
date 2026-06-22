# ADR 0036 — Academy LMS-light + gamification (§19.3)

- Statut : Accepté
- Date : 2026-06-22
- Portée : `apps/api-quality-engine` (module `academy`), `apps/web` (feature `academy`)
- Remplace/complète : néant. Réutilise ADR 0011 (signature hybride) et l'ancrage Phase A/Fabric.

> Nota convention : les ADR du dépôt sont nommés `NNNN-slug.md` (sans préfixe
> `ADR-`). Ce fichier suit cette convention ; il correspond à la demande
> « ADR-0036-lms-gamification ».

## Contexte

La §19.3 (Module Formation & Sensibilisation) exigeait un **LMS-light** :
parcours par rôle/secteur (modules → leçons → quiz), **gamification** (badges,
points, ceintures Yellow → Black Belt, classement), **certificats signés ML-DSA
+ ancrés blockchain** vérifiables par QR, et **export SCORM 2004 / xAPI**.

Le module `training` existant (§4.7) couvrait déjà la **matrice de compétences**
(skills, `training_paths`, inscriptions à visée compétence) et un **moteur de
gamification** (`GamificationService`, `LearnerProgress`, `BeltLevel`, `Badge`,
V87). Il manquait toute la **couche de contenu e-learning** et la chaîne
certificat/export.

## Décision

### 1. Nouveau module `academy` distinct de `training`

`training` reste la **matrice de compétences** ; `academy` est la **couche de
contenu d'apprentissage**. Séparation par responsabilité, pas de fusion : un
cours `academy` (contenu) n'est pas un `training_path` (objectif de compétence).

Sous-packages Clean Architecture : `academy.domain` (entités, énums, règles
pures `QuizGrader`, builders `ScormManifestBuilder`/`XapiStatementBuilder`),
`academy.application` (services + DTO), `academy.infrastructure` (repositories
JPA, convertisseurs JSON), `academy.presentation` (contrôleurs + handler RFC 7807).

### 2. Modèle de données (Flyway V91)

`academy_courses` → `academy_modules` → `academy_lessons` ; `academy_quizzes`
(1 par module) → `academy_quiz_questions` ; runtime `academy_enrollments`,
`academy_lesson_completions` (idempotent), `academy_quiz_attempts` ;
`academy_certificates` (signé + ancré). Champs JSON texte (`options_json`,
`answers_json`, `signature`) suivent le pattern projet `@Column(columnDefinition="TEXT")`
+ `@JdbcTypeCode(LONGVARCHAR)` — jamais `@Lob String` (bug oid PostgreSQL).

### 3. Quiz auto-corrigé déterministe

`QuizGrader` (fonction pure) : score = pourcentage pondéré des points des
questions correctes, arrondi, comparé au seuil. Aucune dépendance, testable.
La bonne réponse (`correctIndex`) n'est **jamais** exposée à l'apprenant
(DTO `QuestionForLearner` sans le champ — anti-triche).

### 4. Complétion → gamification + certificat (réutilisation)

À la complétion d'un cours (toutes leçons vues + tous les quiz passés, moyenne ≥
seuil du cours), `AcademyLearningService` :
- octroie des points via le **`GamificationService` existant** (recalcul
  ceinture/badges — aucune réimplémentation) ;
- émet un certificat via `AcademyCertificateService`.

### 5. Certificat : RÉUTILISATION de l'infra crypto/ancrage (pas de crypto maison)

`AcademyCertificateService` rend un HTML auto-contenu, calcule son SHA-256, le
signe via `HybridSignatureService.sign("certificate", …)` (Ed25519 + ML-DSA-65,
contexte déjà enregistré dans `CryptoSuiteConfig`) et ancre l'empreinte via
`BlockchainAnchorPort.submitRoot(...)` — exactement le pattern du Standards Hub
`CertificationDossierService`. La vérification publique (`/api/v1/academy/public/
certificates/{code}/verify`, **permitAll**) revalide la signature contre la clé
plateforme épinglée : un certificat falsifié donne `signatureValid=false`.

### 6. Export SCORM 2004 / xAPI

`ScormManifestBuilder` (manifeste `imsmanifest.xml` 2004 4th Edition) +
`AcademyExportService` produisent un **ZIP** (manifeste + 1 page HTML/leçon).
`XapiStatementBuilder` émet des **statements xAPI** (verbes `passed`/`completed`,
score scaled) pour la complétion d'une inscription. Tout en pur JDK (java.util.zip
+ Jackson), aucune dépendance lourde.

### 7. Sécurité (OWASP A01 / ASVS L3)

- `tenant_id` et acteur résolus **uniquement depuis le JWT** (`TenantContext`,
  `CurrentUser`), jamais du body.
- Autoring réservé aux rôles de pilotage (`@PreAuthorize` :
  `QUALITY_MANAGER`/`ADMIN_TENANT`/`SUPER_ADMIN`/`ADMIN`) ; apprentissage ouvert
  à tout authentifié, mais chaque apprenant n'agit que sur SA propre inscription
  (404 si inscription d'autrui — pas de fuite d'existence).
- Vérification publique whitelistée dans `SecurityConfig` (lecture seule, aucune
  donnée personnelle interne exposée).
- Rendus HTML échappés (anti-XSS stocké A03).

### 8. Frontend (`apps/web/features/academy`)

NgModules (jamais standalone), HTML/SCSS séparés, lazy-loaded sur `/academy`.
Trois écrans : accueil (catalogue + mes formations + classement), lecteur de
cours (leçons + quiz), vue certificat (HTML signé + preuve d'intégrité + lien
QR). Design premium via tokens du design system.

## Conséquences

- Couvre §19.3 de bout en bout : contenu, quiz noté, gamification, certificat
  signé/ancré vérifiable, export SCORM/xAPI.
- Aucune crypto/ancrage dupliqués : la garantie post-quantique et l'ancrage
  héritent du socle existant (profil `test` → stub ; dev → Phase A ; prod →
  Fabric).
- Le module est désactivable par tenant comme les autres (table `tenant_modules`).

## Alternatives écartées

- **Étendre `training_paths`** pour porter le contenu : aurait mélangé
  compétence et e-learning, cassé les invariants du module existant.
- **Bibliothèque PDF (iText/OpenPDF)** pour les certificats : non utilisée
  ailleurs ; le pattern projet est le HTML imprimable signé (cf. Standards Hub).
- **Lib SCORM tierce** : surdimensionnée pour un manifeste 2004 simple.
