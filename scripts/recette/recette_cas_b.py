# -*- coding: utf-8 -*-
"""Cas de recette — seconde moitié : risques, opérations, référentiels, GRC,
administration, et les contrôles transverses.

Même format de tuple que CAS_A.
"""

CAS_B = [

# ======================================================================
# 7. RISQUES & OPPORTUNITÉS
# ======================================================================
("Risques", "Registre", "Un risque porte un propriétaire choisi dans l'annuaire",
 "Fonctionnel", "Majeur", "demo.qualite",
 "JD-RSQ-01",
 "1. Ouvrir /risques\n2. Créer un risque\n3. Choisir le propriétaire dans l'annuaire\n"
 "4. Coter gravité et probabilité\n5. Enregistrer",
 "JD-RSQ-01, JD-ORG-02",
 "Le propriétaire est choisi dans l'annuaire et s'affiche par son nom. Aucun "
 "identifiant technique visible, aucune saisie libre de personne.",
 "ADR 0092"),

("Risques", "Registre", "Un plan de traitement fait baisser le risque résiduel",
 "Fonctionnel", "Majeur", "demo.hse",
 "JD-RSQ-01 coté",
 "1. Ouvrir R-005\n2. Ajouter un plan de traitement\n3. L'exécuter\n"
 "4. Recoter le risque résiduel",
 "JD-RSQ-01",
 "Le risque initial et le risque résiduel coexistent, tous deux horodatés. "
 "Le résiduel n'écrase pas l'initial.",
 "CLAUDE.md §4.5"),

("Risques", "AMDEC (FMEA)", "Le RPN se recalcule et la criticité suit",
 "Fonctionnel", "Majeur", "demo.ingenieur",
 "JD-PRD-01 avec AMDEC",
 "1. Ouvrir /fmea\n2. Modifier la détection d'une ligne\n3. Lire le RPN\n"
 "4. Vérifier le calcul G × O × D à la main",
 "JD-PRD-01",
 "Le RPN est le produit exact des trois cotations. Le franchissement du seuil "
 "d'action est signalé immédiatement.",
 "CLAUDE.md §4.5"),

("Risques", "AMDEC (FMEA)", "Une action AMDEC engagée devient un détrompeur réutilisable",
 "Intégration", "Mineur", "demo.ingenieur",
 "Une ligne AMDEC au-dessus du seuil",
 "1. Créer l'action depuis la ligne\n2. La qualifier de détrompeur\n"
 "3. Ouvrir la bibliothèque des détrompeurs",
 "JD-PRD-01",
 "Le détrompeur rejoint la bibliothèque, garde le lien vers l'AMDEC d'origine, "
 "et peut être proposé sur un autre produit.",
 "CLAUDE.md §3.4"),

# ======================================================================
# 8. OPÉRATIONS
# ======================================================================
("Opérations", "Instruments", "Une échéance d'étalonnage proche déclenche une alerte",
 "Fonctionnel", "Majeur", "demo.ingenieur",
 "JD-EQP-02, échéance à 10 jours",
 "1. Ouvrir /calibration\n2. Lire la liste triée par échéance\n3. Ouvrir CAL-118",
 "JD-EQP-02",
 "CAL-118 remonte en tête avec son délai. Une alerte est envoyée au "
 "responsable, et un instrument hors échéance est marqué non conforme.",
 "CLAUDE.md §4.10"),

("Opérations", "Instruments", "Un instrument hors échéance bloque son usage en mesure",
 "Négatif", "Majeur", "demo.ingenieur",
 "Un instrument périmé",
 "1. Tenter de saisir une mesure SPC avec cet instrument",
 "JD-EQP-02, JD-SPC-01",
 "La saisie est refusée ou signalée très visiblement. Une mesure prise avec un "
 "instrument périmé ne doit pas entrer silencieusement dans une carte.",
 "ISO 9001 §7.1.5"),

("Opérations", "Parc IoT", "Un capteur remonte ses mesures et alimente la carte SPC",
 "Intégration", "Majeur", "demo.ingenieur",
 "JD-EQP-03 connecté",
 "1. Ouvrir /iot\n2. Vérifier l'état de VIB-204\n3. Laisser remonter 10 minutes\n"
 "4. Ouvrir la carte alimentée",
 "JD-EQP-03, JD-SPC-01",
 "Les mesures arrivent horodatées, la carte se met à jour, et une coupure du "
 "capteur est signalée plutôt que lue comme une série de zéros.",
 "CLAUDE.md §9"),

("Opérations", "Parc IoT", "Une coupure réseau ne perd pas les mesures",
 "Mobile/Hors ligne", "Majeur", "demo.ingenieur",
 "Passerelle Edge en place",
 "1. Couper la liaison de la passerelle 5 minutes\n2. La rétablir\n"
 "3. Comparer la série avant / après",
 "JD-EQP-03",
 "Les mesures de la coupure sont rattrapées, dans l'ordre et sans doublon. "
 "La série ne présente pas de trou.",
 "CLAUDE.md §9.5 store-and-forward"),

("Opérations", "Audits", "Conduire un audit interne de la planification au rapport",
 "Fonctionnel", "Bloquant", "demo.auditeur",
 "JD-AUD-01 planifié",
 "1. Ouvrir /audits, démarrer AUD-2026-0002\n2. Répondre aux 3 questions\n"
 "3. Saisir un constat de NC mineure\n4. Clôturer\n5. Émettre le rapport",
 "JD-AUD-01",
 "Chaque constat est rattaché à une clause. L'émission du rapport exige une "
 "réauthentification forte, et le rapport est signé et horodaté.",
 "ADR 0089, CLAUDE.md §18.2.5"),

("Opérations", "Audits", "Un constat d'audit ouvre une CAPA sans ressaisie",
 "Intégration", "Majeur", "demo.auditeur",
 "JD-AUD-02 clôturé avec 2 constats",
 "1. Ouvrir le constat de NC mineure\n2. Créer la CAPA depuis le constat\n"
 "3. Vérifier le lien dans les deux sens",
 "JD-AUD-02, JD-CAPA-01",
 "La CAPA reprend l'intitulé et la clause du constat. Le rapport d'audit "
 "montre que le constat est pris en charge.",
 "CLAUDE.md §3.6"),

("Opérations", "Planning audits", "Le programme annuel se construit et alerte sur les retards",
 "Fonctionnel", "Mineur", "demo.qualite",
 "3 audits planifiés dont 1 dépassé",
 "1. Ouvrir /audits/planning\n2. Lire le calendrier\n3. Repérer l'audit en retard",
 "JD-AUD-01, JD-AUD-02",
 "L'audit dépassé est signalé distinctement, et l'indicateur « audits réalisés "
 "vs planifiés » reflète le retard.",
 "ADR 0053"),

("Opérations", "GED", "Un document suit son circuit d'approbation jusqu'à la publication",
 "Fonctionnel", "Bloquant", "demo.qualite",
 "JD-DOC-02 en brouillon",
 "1. Ouvrir /documents, ouvrir MO-MNT-004\n2. Soumettre à approbation\n"
 "3. Approuver avec les deux signataires désignés\n4. Publier",
 "JD-DOC-02",
 "Chaque signature est attribuée, horodatée et attachée à la version précise. "
 "Le statut de la version et celui du document restent distincts.",
 "ADR 0099"),

("Opérations", "GED", "La lecture obligatoire laisse une preuve par personne",
 "Fonctionnel", "Majeur", "demo.resp-service",
 "JD-DOC-01 publiée, lecture obligatoire pour 12 personnes",
 "1. Se connecter en demo.resp-service\n2. Ouvrir PR-QUA-012\n3. Accuser lecture\n"
 "4. En demo.qualite, ouvrir le suivi de lecture",
 "JD-DOC-01",
 "L'accusé est enregistré avec sa date et la version lue. Le suivi montre qui "
 "a lu, qui n'a pas lu, et relance les retardataires.",
 "CLAUDE.md §4.1"),

("Opérations", "GED", "Le même fichier déposé deux fois n'est stocké qu'une fois",
 "Données", "Mineur", "demo.qualite",
 "—",
 "1. Déposer un PDF de 8 Mo sur un document\n2. Déposer le même fichier sur un autre\n"
 "3. Vérifier l'espace consommé",
 "JD-DOC-01",
 "Le second dépôt réutilise l'empreinte SHA-256 du premier. L'espace ne double "
 "pas, et les deux documents restent consultables indépendamment.",
 "ADR 0094"),

("Opérations", "GED", "Un dépôt au-delà de la taille autorisée est refusé proprement",
 "Négatif", "Majeur", "demo.qualite",
 "—",
 "1. Déposer un fichier de 80 Mo",
 "—",
 "Le refus annonce la limite (52 Mo) et laisse le document dans son état "
 "antérieur. Pas de page d'erreur technique, pas de version à demi créée.",
 "ADR 0094"),

("Opérations", "MOC", "Une demande de changement suit son analyse d'impact et ses approbations",
 "Fonctionnel", "Bloquant", "demo.demandeur",
 "JD-MOC-01 en validation",
 "1. Ouvrir /changes, ouvrir MOC-2026-001\n2. Lire le score 54/100 Critique\n"
 "3. Approuver en demo.hse, demo.resp-service, puis les deux directeurs",
 "JD-MOC-01",
 "Le score découle des domaines évalués et des poids paramétrés. Chaque "
 "approbation est attribuée et horodatée ; la dernière déclenche l'exécution.",
 "ADR 0087"),

("Opérations", "MOC", "Un changement impacte documents, formations et fournisseurs",
 "Intégration", "Majeur", "demo.qualite",
 "JD-MOC-01 approuvée",
 "1. Ouvrir l'analyse d'impact\n2. Lire les objets impactés\n3. Ouvrir chacun",
 "JD-MOC-01, JD-DOC-01, JD-FOR-01, JD-FRS-01",
 "Les documents à réviser, les formations à mettre à jour et les fournisseurs "
 "à notifier sont listés et ouvrables. La liste n'est pas vide.",
 "CLAUDE.md §4.8"),

("Opérations", "EHS", "Un incident EHS réutilise le circuit NC/CAPA",
 "Fonctionnel", "Majeur", "demo.hse",
 "Module EHS actif",
 "1. Ouvrir /ehs\n2. Déclarer un presqu'accident\n3. Ouvrir la CAPA associée",
 "JD-ORG-01",
 "L'incident est enregistré avec sa gravité et nourrit les indicateurs ISO 45001. "
 "La CAPA créée est une CAPA ordinaire, pas un objet parallèle.",
 "CLAUDE.md §4.11"),

("Opérations", "Formation", "La matrice de compétences signale les écarts et les échéances",
 "Fonctionnel", "Majeur", "demo.admin",
 "JD-FOR-01 avec 4 échéances dépassées",
 "1. Ouvrir /training/competences\n2. Lire la matrice\n3. Filtrer sur les écarts",
 "JD-FOR-01, JD-ORG-02",
 "Les 4 échéances dépassées sont signalées, par personne et par compétence. "
 "Un parcours de remise à niveau peut être lancé depuis la cellule.",
 "ADR 0098"),

("Opérations", "Formation", "Un certificat est vérifiable par son QR code",
 "Fonctionnel", "Mineur", "demo.resp-service",
 "Un parcours terminé",
 "1. Terminer le parcours\n2. Télécharger le certificat\n3. Scanner le QR code",
 "JD-FOR-01",
 "Le QR mène à une page de vérification qui confirme le titulaire, la date et "
 "l'empreinte. Un certificat modifié n'est pas reconnu.",
 "CLAUDE.md §4.7"),

# ======================================================================
# 9. RÉFÉRENTIELS
# ======================================================================
("Référentiels", "Standards Hub", "La fiche d'une norme ouvre ses huit onglets",
 "Fonctionnel", "Majeur", "demo.qualite",
 "ISO 9001 disponible",
 "1. Ouvrir /standards, ouvrir ISO 9001\n2. Parcourir les 8 onglets",
 "JD-REF-01",
 "Vue d'ensemble, exigences, bibliothèque, processus, roadmap, preuves, audit "
 "blanc et veille sont tous présents et peuplés. Aucun onglet vide sans explication.",
 "CLAUDE.md §8.4"),

("Référentiels", "Standards Hub", "Le score d'alignement se recalcule quand une preuve arrive",
 "Fonctionnel", "Majeur", "demo.qualite",
 "Une clause sans preuve",
 "1. Relever le score de la clause\n2. Rattacher PR-QUA-012 à cette clause\n"
 "3. Relire le score",
 "JD-REF-01, JD-DOC-01",
 "Le score monte, et l'écart est explicable : la preuve ajoutée est citée.",
 "CLAUDE.md §8.7"),

("Référentiels", "Audit blanc IA", "L'audit blanc produit un rapport d'écarts actionnable",
 "Fonctionnel", "Majeur", "demo.qualite",
 "ISO 9001 adoptée, preuves partielles",
 "1. Ouvrir l'onglet Audit blanc\n2. Lancer la simulation\n3. Lire les écarts\n"
 "4. Générer le plan de remédiation",
 "JD-REF-01",
 "Les écarts sont cotés et chacun propose une action concrète. Le plan de "
 "remédiation crée de vraies actions, pas une liste figée.",
 "ADR 0033"),

("Référentiels", "Génération doc IA", "Un document généré reste un brouillon à valider",
 "Fonctionnel", "Majeur", "demo.qualite",
 "Contexte client renseigné",
 "1. Ouvrir /standards-doc-gen\n2. Générer la politique qualité\n3. Lire le brouillon\n"
 "4. Tenter de publier sans approbation",
 "JD-ORG-01, JD-REF-01",
 "Le document arrive en brouillon, pré-rempli avec le contexte réel du client. "
 "La publication sans signature humaine est refusée.",
 "CLAUDE.md §8.8"),

("Référentiels", "Packs sectoriels", "Activer un pack charge ses KPI, normes et modèles",
 "Fonctionnel", "Majeur", "demo.admin",
 "JD-REF-02 disponible",
 "1. Ouvrir /industry-packs\n2. Activer le pack Industrie\n3. Vérifier le "
 "catalogue de KPI, les normes et les modèles ajoutés",
 "JD-REF-02",
 "Le pack ajoute ses éléments sans écraser le paramétrage existant du client. "
 "La désactivation les retire sans perdre les données saisies.",
 "CLAUDE.md §5"),

("Référentiels", "Academy", "Un parcours par rôle se suit et se mesure",
 "Fonctionnel", "Mineur", "demo.resp-service",
 "—",
 "1. Ouvrir /academy\n2. Suivre le parcours proposé pour son rôle\n"
 "3. Passer le quiz\n4. Ouvrir /learning",
 "JD-ORG-02",
 "La progression est enregistrée, le quiz note, et /learning montre l'avancement "
 "personnel. Un parcours non commencé ne s'affiche pas comme réussi.",
 "CLAUDE.md §19.3"),

("Référentiels", "Intégrations ITSM", "Un incident ITSM importé devient un objet qualité",
 "Intégration", "Mineur", "demo.admin",
 "Connecteur configuré en bac à sable",
 "1. Ouvrir /itsm\n2. Importer un incident\n3. Le convertir en NC",
 "JD-NC-01",
 "L'incident importé garde sa référence d'origine, et la NC créée pointe vers "
 "elle. Un second import du même incident ne crée pas de doublon.",
 "CLAUDE.md §13.3"),

# ======================================================================
# 10. CONFORMITÉ (GRC)
# ======================================================================
("Conformité GRC", "RoPA", "Un traitement sans base légale est signalé comme incomplet",
 "Négatif", "Majeur", "demo.admin",
 "JD-GRC-01, 1 traitement incomplet",
 "1. Ouvrir /ropa\n2. Lire l'état de complétude\n3. Ouvrir le traitement fautif",
 "JD-GRC-01",
 "Le registre signale le traitement incomplet et nomme le champ manquant. "
 "Le taux de complétude du registre en tient compte.",
 "RGPD art. 30"),

("Conformité GRC", "Demandes des personnes", "Une demande d'accès respecte le délai réglementaire",
 "Fonctionnel", "Majeur", "demo.admin",
 "—",
 "1. Ouvrir /subject-requests\n2. Enregistrer une demande d'accès\n"
 "3. Observer l'échéance calculée",
 "JD-GRC-01",
 "L'échéance d'un mois est posée automatiquement et l'alerte précède le terme. "
 "Le dossier de réponse est constituable depuis l'écran.",
 "RGPD art. 12"),

("Conformité GRC", "Violations de données", "Une violation déclenche le compte à rebours de 72 h",
 "Fonctionnel", "Majeur", "demo.admin",
 "—",
 "1. Ouvrir /breaches\n2. Déclarer une violation\n3. Lire l'échéance de notification",
 "—",
 "Le délai de 72 h court depuis la prise de connaissance, pas depuis la saisie. "
 "La notification à l'autorité est préparée avec les éléments exigés.",
 "RGPD art. 33"),

("Conformité GRC", "Systèmes d'IA", "Un système haut risque exige sa FRIA",
 "Négatif", "Majeur", "demo.admin",
 "JD-GRC-02, FRIA non faite",
 "1. Ouvrir /ai-systems, ouvrir le système\n2. Tenter de le déclarer conforme",
 "JD-GRC-02",
 "La déclaration de conformité est refusée tant que la FRIA manque, et le "
 "message nomme l'obligation.",
 "AI Act — art. 27"),

("Conformité GRC", "NIS 2", "Les mesures NIS 2 se cotent et alimentent le tableau GRC",
 "Fonctionnel", "Mineur", "demo.admin",
 "—",
 "1. Ouvrir /nis2-measures\n2. Coter 5 mesures\n3. Ouvrir /compliance",
 "—",
 "Le hub de conformité reflète les cotations. Une mesure non cotée n'est pas "
 "comptée comme conforme.",
 "NIS 2"),

("Conformité GRC", "Rétention", "Une règle de rétention est journalisée à sa création",
 "Sécurité", "Majeur", "demo.admin",
 "—",
 "1. Ouvrir /retention\n2. Créer une règle\n3. Ouvrir /admin/audit-log\n"
 "4. Filtrer sur gdpr.retention",
 "—",
 "L'événement d'audit existe, nomme l'auteur par son nom, porte la ressource "
 "et l'horodatage serveur.",
 "CLAUDE.md §11.5"),

# ======================================================================
# 11. ADMINISTRATION
# ======================================================================
("Administration", "Modules du tenant", "Désactiver un module le retire du menu à chaud",
 "Fonctionnel", "Majeur", "demo.admin",
 "Module Cercles actif",
 "1. Ouvrir /admin/modules\n2. Désactiver Cercles\n3. Recharger l'application",
 "JD-ORG-01",
 "L'entrée disparaît du menu et la route devient inaccessible. Les données "
 "saisies ne sont pas détruites et reviennent à la réactivation.",
 "CLAUDE.md §10.4"),

("Administration", "Équipe & habilitations", "Un rôle retiré ferme l'accès immédiatement",
 "Droits", "Bloquant", "demo.admin",
 "demo.qualite porte le rôle manager qualité",
 "1. Retirer le rôle à demo.qualite\n2. En demo.qualite, rafraîchir\n"
 "3. Tenter d'ouvrir /capa",
 "JD-ORG-02",
 "L'accès est refusé à la prochaine requête, sans attendre l'expiration du "
 "jeton. L'entrée disparaît du menu.",
 "CLAUDE.md §11.1 A01"),

("Administration", "Équipe & habilitations", "Un administrateur ne peut pas se retirer son propre rôle",
 "Négatif", "Majeur", "demo.admin",
 "demo.admin est le seul administrateur",
 "1. Tenter de retirer son propre rôle d'administrateur",
 "JD-ORG-02",
 "L'opération est refusée avec un message explicite : un client sans "
 "administrateur serait définitivement bloqué.",
 "ADR 0020"),

("Administration", "Rôles et droits", "La matrice des rôles reflète ce que le menu montre",
 "Droits", "Majeur", "demo.admin",
 "—",
 "1. Ouvrir /admin/roles\n2. Relever les droits du rôle auditeur\n"
 "3. Se connecter en demo.auditeur et comparer le menu",
 "JD-ORG-02",
 "Ce que la matrice annonce et ce que le menu offre coïncident exactement. "
 "Aucun écran accessible qui ne figure pas dans la matrice.",
 "CLAUDE.md §16"),

("Administration", "Clés d'API", "Une clé révoquée cesse d'ouvrir l'API",
 "Sécurité", "Bloquant", "demo.admin",
 "Une clé active",
 "1. Appeler l'API avec la clé (200)\n2. La révoquer\n3. Rappeler l'API",
 "—",
 "Le second appel est refusé en 401. La clé n'est jamais réaffichée en clair "
 "après sa création.",
 "CLAUDE.md §13.1"),

("Administration", "Webhooks", "Un webhook est signé et rejoue en cas d'échec",
 "Intégration", "Majeur", "demo.admin",
 "Un point de réception d'essai",
 "1. Créer le webhook sur nonconformity.detected\n2. Créer une NC\n"
 "3. Vérifier la signature HMAC reçue\n4. Rendre le point indisponible, recréer une NC",
 "JD-NC-01",
 "La charge est signée et horodatée. L'échec déclenche des réessais espacés, "
 "puis la mise en file d'attente morte, sans perte silencieuse.",
 "CLAUDE.md §13.2"),

("Administration", "Quotas d'API", "Le dépassement de quota renvoie 429 et pas une erreur serveur",
 "Négatif", "Majeur", "demo.admin",
 "Quota abaissé pour l'essai",
 "1. Dépasser le quota en rafale\n2. Lire les codes de retour",
 "—",
 "Les appels excédentaires reçoivent 429 avec un en-tête de réessai. Aucun 500.",
 "CLAUDE.md §13.1"),

("Administration", "Journal d'audit", "Le journal se filtre et se vérifie",
 "Sécurité", "Bloquant", "demo.admin",
 "Activité réelle du jour",
 "1. Ouvrir /admin/audit-log\n2. Filtrer par acteur, action, période\n"
 "3. Ouvrir un événement\n4. Lancer la vérification de la chaîne",
 "—",
 "Les filtres fonctionnent, l'acteur s'affiche par son nom, et la vérification "
 "de chaîne confirme l'intégrité sur l'intervalle demandé.",
 "CLAUDE.md §11.5"),

("Administration", "Journal d'audit", "Les actes sensibles laissent tous une trace",
 "Sécurité", "Bloquant", "demo.admin",
 "—",
 "1. Faire successivement : retirer un rôle, publier un document, émettre un "
 "rapport d'audit, créer une clé d'API, changer le fournisseur d'IA\n"
 "2. Ouvrir le journal et retrouver les cinq actes",
 "JD-ORG-02, JD-DOC-01, JD-AUD-02",
 "Les cinq événements sont présents, chacun avec son acteur, sa ressource, son "
 "horodatage et son empreinte chaînée. Aucun acte manquant.",
 "CLAUDE.md §11.5"),

("Administration", "Fournisseur d'IA", "Le fournisseur épinglé est respecté, sans repli",
 "Sécurité", "Majeur", "demo.admin",
 "Fournisseur épinglé sur le client",
 "1. Ouvrir /admin/ia, épingler un fournisseur\n2. Lancer une requête NLQ\n"
 "3. Rendre le fournisseur indisponible\n4. Relancer",
 "JD-ORG-01",
 "La requête part chez le fournisseur épinglé. Son indisponibilité provoque un "
 "échec explicite, jamais un repli silencieux vers un autre fournisseur.",
 "ADR 0102"),

("Administration", "Licence", "Les quotas de la licence sont tenus",
 "Négatif", "Majeur", "demo.admin",
 "Licence limitée à N utilisateurs",
 "1. Ouvrir /admin/licence, relever la limite\n2. Créer des utilisateurs "
 "jusqu'au dépassement",
 "JD-ORG-01",
 "La création au-delà de la limite est refusée en nommant la licence. "
 "Les utilisateurs existants ne sont pas affectés.",
 "Facturation"),

("Administration", "Cabinets", "Un cabinet partenaire voit ses clients et rien d'autre",
 "Droits", "Bloquant", "demo.admin",
 "Un cabinet rattaché à 1 client sur 2",
 "1. Ouvrir /admin/cabinets\n2. Se connecter avec le compte du cabinet\n"
 "3. Lister les clients accessibles",
 "JD-ORG-01, JD-TEN-02",
 "Seul le client rattaché apparaît. L'autre est invisible, et son appel direct "
 "répond 404.",
 "ADR 0106"),

# ======================================================================
# 12. TRANSVERSES
# ======================================================================
("Transverse — Sécurité", "Étanchéité entre clients", "Aucune donnée ne franchit la frontière",
 "Sécurité", "Bloquant", "demo.qualite",
 "JD-TEN-02 peuplé",
 "1. Relever l'identifiant d'une NC de Plasturgie du Centre\n"
 "2. En demo.qualite (Fonderie Atlantique), appeler /nc/{id} directement\n"
 "3. Répéter sur un document, une CAPA et un audit",
 "JD-TEN-02, JD-NC-01",
 "Les quatre appels répondent 404. Aucun message ne révèle que l'objet existe "
 "ailleurs. La RLS bloque même si un filtre applicatif était oublié.",
 "ADR 0086, CLAUDE.md §18.2.2"),

("Transverse — Sécurité", "tenant_id non falsifiable", "Le client vient du jeton, pas de la requête",
 "Sécurité", "Bloquant", "demo.qualite",
 "—",
 "1. Appeler une création de NC en forçant un tenant_id étranger dans le corps\n"
 "2. Vérifier où la NC a été créée",
 "JD-TEN-02",
 "Le champ du corps est ignoré. La NC est créée dans le client du jeton, ou "
 "la requête est refusée.",
 "CLAUDE.md §18.2.2"),

("Transverse — Sécurité", "Élévation de privilège", "Un utilisateur simple ne peut pas administrer",
 "Sécurité", "Bloquant", "demo.demandeur",
 "—",
 "1. Appeler directement les routes /admin/* et leurs API\n"
 "2. Tenter de s'attribuer un rôle par l'API",
 "JD-ORG-02",
 "Toutes les tentatives répondent 403. Aucune n'aboutit, même en forgeant la "
 "requête hors de l'interface.",
 "OWASP A01"),

("Transverse — Sécurité", "Session", "La session expire et le renouvellement est propre",
 "Sécurité", "Majeur", "demo.qualite",
 "—",
 "1. Se connecter, laisser l'onglet inactif au-delà de la durée de session\n"
 "2. Reprendre une action",
 "—",
 "L'utilisateur est redirigé vers l'authentification sans perdre son contexte, "
 "et aucune donnée d'un écran précédent ne reste affichée.",
 "CLAUDE.md §11.1 A07"),

("Transverse — Sécurité", "MFA", "Un acte critique exige une réauthentification forte",
 "Sécurité", "Bloquant", "demo.qualite",
 "—",
 "1. Émettre un rapport d'audit\n2. Signer un document",
 "JD-AUD-02, JD-DOC-02",
 "Les deux actes demandent un second facteur. Un refus de second facteur "
 "annule l'acte sans effet de bord.",
 "CLAUDE.md §18.2.5"),

("Transverse — i18n", "Six langues", "Chaque écran parcouru s'affiche entièrement traduit",
 "i18n", "Majeur", "demo.qualite",
 "—",
 "1. Basculer successivement en fr, en, es, ar, ja, zh\n"
 "2. Parcourir 10 écrans représentatifs par langue",
 "—",
 "Aucun libellé non traduit, aucune clé technique visible, aucun texte tronqué. "
 "En arabe, la mise en page passe bien de droite à gauche.",
 "CLAUDE.md §15.1"),

("Transverse — i18n", "Formats", "Dates, nombres et devises suivent la locale",
 "i18n", "Mineur", "demo.qualite",
 "—",
 "1. En fr puis en en, comparer une date, un nombre décimal et un montant",
 "JD-KPI-01",
 "Les séparateurs, l'ordre des dates et le symbole monétaire suivent la langue "
 "choisie, sans jamais changer la valeur.",
 "CLAUDE.md §15.1"),

("Transverse — Accessibilité", "Clavier", "Tout parcours se fait sans souris",
 "Accessibilité", "Majeur", "demo.qualite",
 "—",
 "1. Parcourir création de NC, CAPA et audit uniquement au clavier\n"
 "2. Vérifier la visibilité du focus à chaque étape",
 "JD-NC-01",
 "Chaque élément interactif est atteignable, le focus est toujours visible, "
 "et aucun piège de focus n'enferme dans une boîte de dialogue.",
 "CLAUDE.md §15.1 WCAG 2.2 AA"),

("Transverse — Accessibilité", "Lecteur d'écran", "Les écrans clés sont annoncés correctement",
 "Accessibilité", "Majeur", "demo.qualite",
 "NVDA ou VoiceOver",
 "1. Parcourir le tableau de bord, la liste des NC et un formulaire\n"
 "2. Écouter les annonces",
 "JD-NC-01",
 "Les titres, les tableaux et les champs sont annoncés avec leur libellé et "
 "leur état. Les messages d'erreur sont lus à leur apparition.",
 "CLAUDE.md §22-10"),

("Transverse — Performance", "Temps de réponse", "Les chemins chauds tiennent le SLO",
 "Performance", "Majeur", "—",
 "Jeu de données volumineux (40 NC, 25 sous-groupes SPC, 8 000 événements d'audit)",
 "1. Mesurer p95 sur : liste NC, plan de travail CAPA, carte SPC, journal d'audit",
 "JD-NC-03, JD-SPC-01",
 "p95 inférieur à 300 ms côté API et p99 inférieur à 800 ms. Le nombre de "
 "requêtes SQL ne croît pas avec le nombre de lignes affichées.",
 "CLAUDE.md §14.3"),

("Transverse — Performance", "Web Vitals", "Les pages principales respectent les seuils",
 "Performance", "Mineur", "—",
 "—",
 "1. Mesurer LCP, INP et CLS sur l'accueil, le tableau de bord et une liste",
 "—",
 "LCP sous 2 s, INP sous 200 ms, CLS sous 0,1.",
 "CLAUDE.md §15.2"),

("Transverse — Mobile", "Terrain", "Les écrans de terrain sont utilisables avec des gants",
 "Mobile/Hors ligne", "Majeur", "demo.demandeur",
 "Téléphone réel",
 "1. Déclarer une NC, réaliser un audit 5S et prendre une photo sur mobile",
 "JD-ORG-01",
 "Les cibles tactiles sont assez grandes, le contraste suffit en extérieur, et "
 "aucun geste ne demande de précision fine.",
 "CLAUDE.md §15.3"),

("Transverse — Données", "Export", "Un export rend exactement ce que l'écran montre",
 "Données", "Majeur", "demo.qualite",
 "Liste filtrée de NC",
 "1. Filtrer la liste des NC\n2. Exporter en Excel\n3. Comparer ligne à ligne",
 "JD-NC-03",
 "L'export contient les mêmes lignes et colonnes que l'écran filtré, ni plus "
 "ni moins, et respecte la portée de l'utilisateur.",
 "CLAUDE.md §7.3"),

("Transverse — Données", "Horodatage", "Les dates sont cohérentes entre écrans et exports",
 "Données", "Mineur", "demo.qualite",
 "—",
 "1. Relever la date de création d'une NC à l'écran, dans l'export et dans "
 "le journal d'audit",
 "JD-NC-01",
 "Les trois coïncident, au fuseau près, et le fuseau est indiqué.",
 "CLAUDE.md §22-9"),
]

# ======================================================================
# SCÉNARIOS BOUT-EN-BOUT
# Le fil rouge : la pompe P-204 casse, et l'on suit la chaîne complète.
# C'est ce que les concurrents ne savent pas faire — c'est donc ce qu'il
# faut démontrer, et dans cet ordre.
# ======================================================================
SCENARIOS = [
 ("E2E-01", "De la dérive détectée à la preuve d'efficacité",
  "Le cœur de la promesse : une donnée capteur devient une preuve d'audit.",
  "demo.ingenieur → demo.qualite → demo.auditeur",
  "1. Le capteur VIB-204 remonte une dérive (JD-EQP-03)\n"
  "2. La carte SPC lève une règle de Nelson (JD-SPC-02)\n"
  "3. Une NC est créée automatiquement (JD-NC-01)\n"
  "4. Un Ishikawa identifie la cause racine\n"
  "5. La cause ouvre une CAPA avec son critère d'efficacité (JD-CAPA-01)\n"
  "6. Les actions sont exécutées et tracées\n"
  "7. Le verdict d'efficacité est rendu contre le critère (ADR 0108)\n"
  "8. L'audit interne rattache la preuve à la clause ISO 9001 §10.2\n"
  "9. Le dossier de certification inclut la preuve",
  "La chaîne est parcourable dans les deux sens, de bout en bout, sans aucune "
  "ressaisie et sans rupture de lien. Chaque étape nomme son acteur.",
  "Bloquant"),

 ("E2E-02", "De la réclamation client au changement maîtrisé",
  "Le parcours vu par le client final, celui qui se raconte en revue de direction.",
  "demo.qualite → demo.demandeur → demo.hse → demo.dir",
  "1. Une réclamation client arrive (JD-NC-02)\n"
  "2. Elle est qualifiée et classée par le NLP\n"
  "3. Le clustering la rapproche de 7 NC antérieures (JD-NC-03)\n"
  "4. Un projet DMAIC est ouvert sur le motif récurrent\n"
  "5. La phase Improve conclut au remplacement de la pompe\n"
  "6. Un MOC est ouvert et son impact analysé (JD-MOC-01)\n"
  "7. Les quatre approbations sont recueillies\n"
  "8. Les documents et formations impactés sont mis à jour\n"
  "9. Le KPI de récidive est suivi sur trois mois",
  "Le changement est tracé de la plainte à l'exécution, l'impact documentaire "
  "et formatif est réel, et le gain se lit sur l'indicateur.",
  "Bloquant"),

 ("E2E-03", "Du zéro à l'audit de certification ISO 14001",
  "La promesse du Standards Hub : viser une norme et savoir chaque jour où l'on en est.",
  "demo.admin → demo.qualite → demo.externe",
  "1. L'ISO 14001 est adoptée au SMI (JD-REF-01)\n"
  "2. Le diagnostic initial chiffre l'écart\n"
  "3. La co-couverture HLS mutualise les preuves de l'ISO 9001\n"
  "4. Les documents manquants sont générés puis validés\n"
  "5. Les processus types sont importés et adaptés\n"
  "6. L'audit blanc IA produit son rapport d'écarts\n"
  "7. Les écarts deviennent des CAPA\n"
  "8. Le dossier de certification est exporté, signé et horodaté\n"
  "9. L'auditeur externe y accède en lecture, sur une période bornée",
  "Le score de conformité progresse de façon explicable, la mutualisation "
  "économise des preuves réellement, et l'auditeur externe ne voit que ce "
  "qu'il doit voir, pendant la durée prévue.",
  "Bloquant"),

 ("E2E-04", "La journée d'un responsable de site",
  "Le parcours quotidien : si celui-là est pénible, rien d'autre ne compte.",
  "demo.dir",
  "1. Ouvrir l'accueil et lire ses tâches du jour\n"
  "2. Consulter le tableau de bord exécutif\n"
  "3. Descendre sur le KPI le plus dégradé\n"
  "4. Ouvrir les trois NC qui l'expliquent\n"
  "5. Relancer le pilote d'une CAPA en retard\n"
  "6. Valider un rapport d'audit\n"
  "7. Lancer le mode TV pour la salle qualité",
  "Sept gestes, moins de dix minutes, aucun détour par un export. Chaque "
  "chiffre lu est descendable jusqu'à son événement source.",
  "Majeur"),

 ("E2E-05", "L'étanchéité, prouvée plutôt qu'affirmée",
  "Ce qu'un client régulé demandera avant de signer.",
  "demo.qualite + compte du second client",
  "1. Peupler les deux clients (JD-ORG-01, JD-TEN-02)\n"
  "2. Depuis le client A, tenter l'accès direct à 6 objets du client B\n"
  "3. Interroger le NLQ sur les données du client B\n"
  "4. Exporter et vérifier le contenu\n"
  "5. Lire le journal d'audit du client A\n"
  "6. Vérifier la chaîne d'intégrité du journal",
  "Aucune donnée du client B n'apparaît nulle part : ni par appel direct, ni "
  "par l'IA, ni dans un export. Le journal du client A ne mentionne que le "
  "client A, et sa chaîne est intacte.",
  "Bloquant"),
]
