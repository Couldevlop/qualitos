# -*- coding: utf-8 -*-
"""Cas de recette — complément : les écrans que les deux premières parties
n'atteignaient pas, pour que « toutes les fonctionnalités » soit exact.

Le reproche qu'on fait aux cahiers de recette est toujours le même : ils
couvrent ce dont on se souvient. L'inventaire a donc été tiré de la navigation
réelle de l'application (11 sections, 80 entrées), et cette partie ferme les
écarts constatés après la première passe.
"""

CAS_C = [

# ---------------------------------------------------------- Méthodes (suite)
("Méthodes qualité", "Ishikawa", "Construire un diagramme à six branches en collaboratif",
 "Fonctionnel", "Majeur", "demo.qualite",
 "JD-NC-01 ouverte",
 "1. Ouvrir l'Ishikawa depuis NC-2026-0006\n2. Poser le problème\n"
 "3. Remplir les 6 branches (6M)\n4. Ouvrir la même session depuis un second "
 "navigateur\n5. Ajouter une cause depuis le second",
 "JD-NC-01",
 "Les deux sessions voient la même chose en quelques secondes, sans recharger. "
 "Aucune cause n'est perdue quand deux personnes écrivent en même temps.",
 "CLAUDE.md §3.5"),

("Méthodes qualité", "Ishikawa", "L'IA propose des causes par famille, citées",
 "Fonctionnel", "Majeur", "demo.qualite",
 "Problème décrit, historique de NC chargé",
 "1. Lancer la suggestion IA\n2. Lire les propositions par branche\n"
 "3. Ouvrir une source citée",
 "JD-NC-03",
 "Les propositions sont rangées par famille et chacune cite le cas historique "
 "qui l'inspire. Rien n'est ajouté au diagramme sans validation.",
 "CLAUDE.md §12.1"),

("Méthodes qualité", "Cinq pourquoi", "La chaîne s'arrête sur une cause actionnable",
 "Fonctionnel", "Mineur", "demo.qualite",
 "JD-NC-01",
 "1. Ouvrir /five-whys depuis la NC\n2. Dérouler les cinq niveaux\n"
 "3. Convertir la cause racine en action",
 "JD-NC-01",
 "Les cinq niveaux sont conservés et relisibles. La conversion en action garde "
 "le lien vers la chaîne de raisonnement.",
 "CLAUDE.md §3.5"),

("Méthodes qualité", "Pareto", "Le Pareto classe les causes et montre les 80 %",
 "Fonctionnel", "Mineur", "demo.qualite",
 "JD-NC-03",
 "1. Ouvrir le Pareto des motifs de NC\n2. Lire la courbe cumulée\n"
 "3. Vérifier que la somme des barres fait 100 %",
 "JD-NC-03",
 "Les barres sont triées par fréquence décroissante, la courbe cumulée est "
 "juste, et le seuil des 80 % est marqué.",
 "CLAUDE.md §3.5"),

# ------------------------------------------------------------ Analyses (suite)
("Analyses IA", "Exploration NLQ", "L'exploration montre la requête avant de l'exécuter",
 "Sécurité", "Majeur", "demo.qualite",
 "Rôle en lecture seule sur la base NLQ",
 "1. Ouvrir /nlq-explore\n2. Poser une question\n3. Lire la requête proposée\n"
 "4. Tenter une question qui impliquerait une écriture",
 "JD-NC-03",
 "La requête est lisible avant exécution. Toute tentative d'écriture est "
 "refusée : la connexion NLQ est en lecture seule, côté base.",
 "ADR 0049"),

# ------------------------------------------------------------- Opérations (suite)
("Opérations", "Connecteurs", "Un connecteur se teste sans engager de données réelles",
 "Intégration", "Majeur", "demo.admin",
 "—",
 "1. Ouvrir /connectors\n2. Configurer un connecteur en bac à sable\n"
 "3. Lancer le test de connexion\n4. Lire le diagnostic en cas d'échec",
 "—",
 "Le test dit ce qui ne va pas (adresse, identifiants, certificat) sans "
 "divulguer le secret configuré. Aucune donnée n'est échangée avant activation.",
 "CLAUDE.md §13.3"),

("Opérations", "Paramètres SPC", "Les seuils SPC sont paramétrables par client",
 "Fonctionnel", "Mineur", "demo.admin",
 "—",
 "1. Ouvrir /admin/spc\n2. Modifier le nombre de sous-groupes de calcul\n"
 "3. Rouvrir une carte existante",
 "JD-SPC-01",
 "Le nouveau réglage s'applique aux calculs à venir. Les cartes déjà figées "
 "gardent les limites avec lesquelles elles ont été établies.",
 "ADR 0096, règle « figées à l'étape »"),

("Opérations", "Paramètres DMAIC", "Les livrables obligatoires par phase se paramètrent",
 "Fonctionnel", "Mineur", "demo.admin",
 "—",
 "1. Ouvrir /admin/dmaic\n2. Rendre un livrable facultatif\n"
 "3. Ouvrir un projet en cours",
 "JD-PRD-01",
 "Le projet en cours garde les exigences de son ouverture. Un nouveau projet "
 "applique le nouveau paramétrage.",
 "feedback_parametrage_admin_tenant"),

("Opérations", "Natures de NC", "Les natures de NC sont propres au client",
 "Fonctionnel", "Mineur", "demo.admin",
 "—",
 "1. Ouvrir /admin/nc\n2. Ajouter une nature\n3. Créer une NC et la choisir\n"
 "4. Tenter de supprimer la nature utilisée",
 "JD-NC-01",
 "La nature ajoutée est proposée à la saisie. Sa suppression est refusée tant "
 "qu'une NC l'utilise, plutôt que de laisser des NC orphelines.",
 "Intégrité référentielle"),

("Opérations", "Paramètres MOC", "Les seuils et les poids du score MOC sont réglables",
 "Fonctionnel", "Majeur", "demo.admin",
 "JD-MOC-01 en validation",
 "1. Ouvrir /admin/moc\n2. Porter le seuil Critique de 50 à 60\n"
 "3. Rouvrir MOC-2026-001",
 "JD-MOC-01",
 "La demande déjà soumise garde la cotation de sa soumission. Une nouvelle "
 "demande applique le nouveau seuil.",
 "ADR 0087"),

("Opérations", "Documentation", "L'aide contextuelle mène à la bonne page du wiki",
 "Fonctionnel", "Mineur", "demo.qualite",
 "—",
 "1. Sur cinq écrans différents, cliquer sur l'icône d'aide\n"
 "2. Vérifier la page atteinte",
 "—",
 "Chaque icône ouvre la page qui décrit cet écran-là, pas l'accueil du wiki.",
 "CLAUDE.md §19.2"),

# ------------------------------------------------------------ Référentiels (suite)
("Référentiels", "Marketplace", "Un pack du marché s'installe et se retire proprement",
 "Fonctionnel", "Mineur", "demo.admin",
 "Module Marketplace activé pour l'essai",
 "1. Ouvrir /marketplace\n2. Installer un pack\n3. Vérifier ses apports\n"
 "4. Le désinstaller",
 "JD-REF-02",
 "L'installation ajoute ses éléments, la désinstallation les retire sans "
 "toucher aux données saisies par le client entre-temps.",
 "CLAUDE.md §8.11"),

("Référentiels", "Veille normative", "Une révision de norme prévient les clients concernés",
 "Fonctionnel", "Mineur", "demo.qualite",
 "ISO 9001 adoptée, une révision publiée",
 "1. Publier une révision côté éditeur\n2. En demo.qualite, ouvrir l'onglet Veille\n"
 "3. Lire l'analyse d'impact",
 "JD-REF-01",
 "La notification arrive, l'analyse nomme les clauses touchées, et les "
 "ajustements proposés sont rattachables au SMI existant.",
 "CLAUDE.md §8.10"),

# --------------------------------------------------------- Conformité (suite)
("Conformité GRC", "Consentements", "Un consentement retiré cesse de légitimer le traitement",
 "Fonctionnel", "Majeur", "demo.admin",
 "Un consentement actif",
 "1. Ouvrir /consents\n2. Retirer le consentement\n3. Ouvrir le traitement lié "
 "dans le RoPA",
 "JD-GRC-01",
 "Le traitement est signalé comme privé de base légale, avec la date du retrait. "
 "L'historique du consentement reste consultable.",
 "RGPD art. 7"),

("Conformité GRC", "Mentions d'information", "Une mention publiée est versionnée",
 "Fonctionnel", "Mineur", "demo.admin",
 "—",
 "1. Ouvrir /privacy-notices\n2. Publier une mention\n3. La modifier et republier\n"
 "4. Ouvrir l'historique",
 "JD-GRC-01",
 "Les deux versions coexistent avec leurs dates. On peut dire quelle mention "
 "était en vigueur à une date donnée.",
 "RGPD art. 13"),

("Conformité GRC", "AIPD (DPIA)", "Une AIPD obligatoire est exigée avant mise en service",
 "Négatif", "Majeur", "demo.admin",
 "Un traitement à risque élevé sans AIPD",
 "1. Ouvrir /dpia\n2. Tenter de déclarer le traitement opérationnel",
 "JD-GRC-01",
 "La déclaration est refusée et l'obligation est nommée. L'AIPD peut être "
 "lancée depuis le refus.",
 "RGPD art. 35"),

("Conformité GRC", "Désignation DPO", "La désignation du DPO est datée et traçable",
 "Fonctionnel", "Mineur", "demo.admin",
 "—",
 "1. Ouvrir /dpo-appointments\n2. Désigner un DPO\n3. Le remplacer\n"
 "4. Lire l'historique",
 "JD-ORG-02",
 "Les deux désignations sont conservées avec leurs périodes. Le DPO s'affiche "
 "par son nom.",
 "RGPD art. 37"),

("Conformité GRC", "Transferts hors UE", "Un transfert sans garantie est signalé",
 "Négatif", "Majeur", "demo.admin",
 "—",
 "1. Ouvrir /cross-border\n2. Déclarer un transfert sans mécanisme de garantie\n"
 "3. Lire l'alerte",
 "JD-GRC-01",
 "Le transfert est enregistré mais marqué non conforme, et la nature de la "
 "garantie manquante est nommée.",
 "RGPD chap. V"),

("Conformité GRC", "Sous-traitants", "Un contrat de sous-traitance expire et alerte",
 "Fonctionnel", "Mineur", "demo.admin",
 "Un contrat à échéance proche",
 "1. Ouvrir /processor-agreements\n2. Lire les échéances\n3. Renouveler",
 "JD-GRC-01",
 "L'échéance proche est signalée avant son terme. Le renouvellement crée une "
 "nouvelle période sans effacer l'ancienne.",
 "RGPD art. 28"),

("Conformité GRC", "SMQ de l'IA", "Le système de management de l'IA relie ses exigences",
 "Fonctionnel", "Mineur", "demo.admin",
 "JD-GRC-02",
 "1. Ouvrir /ai-qms\n2. Parcourir les exigences\n3. Rattacher une preuve",
 "JD-GRC-02",
 "Le taux de couverture progresse avec la preuve rattachée, de la même façon "
 "que pour une norme du Standards Hub.",
 "AI Act art. 17"),

("Conformité GRC", "Conformité IA", "La déclaration de conformité liste ce qui manque",
 "Fonctionnel", "Majeur", "demo.admin",
 "JD-GRC-02, dossier incomplet",
 "1. Ouvrir /ai-conformity\n2. Lancer le contrôle\n3. Lire les manques",
 "JD-GRC-02",
 "Chaque manque est nommé avec son article de référence, et renvoie à l'écran "
 "qui permet de le combler.",
 "AI Act"),

("Conformité GRC", "Incidents IA", "Un incident grave déclenche le signalement",
 "Fonctionnel", "Majeur", "demo.admin",
 "JD-GRC-02",
 "1. Ouvrir /ai-incidents\n2. Déclarer un incident grave\n3. Lire l'échéance",
 "JD-GRC-02",
 "Le délai de signalement est calculé et affiché. Le dossier de notification "
 "reprend les éléments exigés.",
 "AI Act art. 73"),

("Conformité GRC", "Base de données UE", "L'enregistrement UE est préparé depuis la fiche",
 "Fonctionnel", "Mineur", "demo.admin",
 "JD-GRC-02",
 "1. Ouvrir /ai-eudb\n2. Générer le dossier d'enregistrement",
 "JD-GRC-02",
 "Le dossier reprend les informations déjà saisies, sans ressaisie, et signale "
 "les champs encore vides.",
 "AI Act art. 49"),

("Conformité GRC", "Surveillance après mise sur le marché", "Le plan PMM est suivi dans le temps",
 "Fonctionnel", "Mineur", "demo.admin",
 "JD-GRC-02",
 "1. Ouvrir /ai-pmm\n2. Définir le plan\n3. Enregistrer une observation",
 "JD-GRC-02",
 "Les observations s'empilent avec leurs dates. Une période sans observation "
 "est visible, plutôt que silencieuse.",
 "AI Act art. 72"),

("Conformité GRC", "FRIA", "L'analyse d'impact sur les droits fondamentaux est complète",
 "Fonctionnel", "Majeur", "demo.admin",
 "JD-GRC-02",
 "1. Ouvrir /fria\n2. Conduire l'analyse\n3. Conclure",
 "JD-GRC-02",
 "Toutes les rubriques exigées sont présentes. La conclusion ne peut pas être "
 "posée tant qu'une rubrique reste vide.",
 "AI Act art. 27"),

("Conformité GRC", "Décisions automatisées", "Une décision automatisée est explicable et contestable",
 "Fonctionnel", "Majeur", "demo.admin",
 "Une décision automatisée enregistrée",
 "1. Ouvrir /automated-decisions\n2. Ouvrir une décision\n3. Lire l'explication\n"
 "4. Enregistrer une contestation",
 "JD-GRC-02",
 "L'explication nomme les facteurs déterminants. La contestation est "
 "enregistrée et appelle une intervention humaine.",
 "RGPD art. 22"),

("Conformité GRC", "Incidents cyber", "Un incident cyber suit le calendrier NIS 2",
 "Fonctionnel", "Majeur", "demo.admin",
 "—",
 "1. Ouvrir /cyber-incidents\n2. Déclarer un incident significatif\n"
 "3. Lire les trois échéances (24 h, 72 h, 1 mois)",
 "—",
 "Les trois jalons sont posés depuis la prise de connaissance, et chacun "
 "prépare le contenu attendu par l'autorité.",
 "NIS 2 art. 23"),

# ------------------------------------------------------------ Transverses (suite)
("Transverse — Sécurité", "Journal d'audit", "Le journal ne se laisse pas réécrire sans trace",
 "Sécurité", "Bloquant", "demo.admin",
 "Accès base en lecture pour le contrôle",
 "1. Relever l'empreinte de trois événements consécutifs\n"
 "2. Modifier directement une ligne en base\n"
 "3. Relancer la vérification de chaîne",
 "—",
 "La vérification signale la rupture et nomme le premier événement incohérent. "
 "Un journal modifié ne peut pas se faire passer pour intact.",
 "CLAUDE.md §11.5"),

("Transverse — Sécurité", "Entêtes HTTP", "Les en-têtes de sécurité sont posés",
 "Sécurité", "Majeur", "—",
 "—",
 "1. Relever les en-têtes de réponse sur l'application et l'API\n"
 "2. Vérifier CSP, HSTS, X-Content-Type-Options, Referrer-Policy",
 "—",
 "Les quatre en-têtes sont présents et restrictifs. La CSP n'autorise pas "
 "'unsafe-inline' sur les scripts.",
 "OWASP A05"),

("Transverse — Sécurité", "Téléversement", "Un fichier piégé est refusé",
 "Négatif", "Bloquant", "demo.qualite",
 "—",
 "1. Déposer un fichier exécutable renommé en .pdf\n"
 "2. Déposer un PDF contenant un script\n3. Déposer un fichier à nom traversant "
 "(../../etc/passwd)",
 "JD-DOC-01",
 "Les trois sont refusés sur le type réel, pas sur l'extension. Le nom "
 "traversant ne crée aucun fichier hors du dépôt.",
 "OWASP A03, A08"),

("Transverse — Données", "Suppression", "Une suppression est réversible ou annoncée comme définitive",
 "Données", "Majeur", "demo.qualite",
 "Un brouillon supprimable",
 "1. Supprimer un brouillon\n2. Lire le message\n3. Chercher l'objet dans la corbeille",
 "JD-MOC-01",
 "Le message dit clairement si la suppression est réversible. Si elle ne l'est "
 "pas, elle est confirmée explicitement avant d'être faite.",
 "Ergonomie — action destructive"),

("Transverse — Données", "Reprise après coupure", "Un formulaire long ne se perd pas",
 "Données", "Majeur", "demo.qualite",
 "—",
 "1. Remplir un formulaire d'audit long\n2. Couper le réseau\n3. Valider\n"
 "4. Rétablir le réseau",
 "JD-AUD-01",
 "La saisie n'est pas perdue : elle est conservée localement et rejouée, ou "
 "l'utilisateur est averti avant de perdre quoi que ce soit.",
 "CLAUDE.md §15.2"),

("Transverse — Robustesse", "Double soumission", "Un double clic ne crée pas deux objets",
 "Négatif", "Majeur", "demo.qualite",
 "—",
 "1. Sur cinq formulaires de création, double-cliquer sur « Enregistrer »\n"
 "2. Compter les objets créés",
 "JD-NC-01, JD-CAPA-01",
 "Un seul objet est créé à chaque fois. Le bouton se désarme pendant l'envoi.",
 "Idempotence"),

("Transverse — Robustesse", "Retour arrière du navigateur", "La navigation arrière reste cohérente",
 "Fonctionnel", "Mineur", "demo.qualite",
 "—",
 "1. Enchaîner liste → fiche → onglet → édition\n2. Revenir en arrière quatre fois",
 "JD-NC-01",
 "Chaque retour ramène à l'état attendu, filtres conservés, sans écran blanc "
 "ni formulaire à demi rempli.",
 "Ergonomie"),

("Transverse — Robustesse", "Concurrence", "Deux modifications simultanées ne s'écrasent pas",
 "Données", "Majeur", "demo.qualite",
 "Même objet ouvert dans deux sessions",
 "1. Ouvrir la même CAPA en demo.qualite et demo.resp-service\n"
 "2. Modifier et enregistrer des deux côtés",
 "JD-CAPA-01",
 "Le second enregistrement est averti du conflit plutôt que d'écraser "
 "silencieusement le premier.",
 "Intégrité — 409"),
]
