# -*- coding: utf-8 -*-
"""Cas de recette — compléments livrés après la première campagne.

Ce fichier porte ce qui est entré dans le produit APRÈS la date du socle
(`recette_socle.DATE`). Il n'est pas une quatrième moitié arbitraire : c'est le
delta d'une campagne à la suivante, et le tenir à part a une raison précise.

Une campagne de recette se juge sur ce qu'elle couvre du produit TEL QU'IL EST
le jour où elle se joue. Mélanger les nouveaux cas dans les trois fichiers
existants rendrait impossible la seule question qui se pose en relecture :
« qu'est-ce qui n'avait jamais été recetté ? ». Ici, la réponse est le contenu
du fichier.

Même tuple que les autres :
  (domaine, fonctionnalité, scénario, type, criticité, compte,
   préconditions, étapes, données, attendu, exigence)

Les domaines réemploient ceux des trois premiers fichiers : le classeur compte
par domaine, et un domaine inventé ici créerait une ligne de plus dans la
synthèse pour les mêmes écrans.
"""

CAS_D = [

# ======================================================================
# SPC — les règles sur la carte de l'étude, et la prévision de sortie
# ======================================================================
("SPC", "Cartes de contrôle",
 "Les règles de Nelson se posent sur la carte de l'étude, sans bouton à chercher",
 "Fonctionnel", "Majeur", "demo.ingenieur",
 "Une étude de capabilité ouverte, grille partiellement remplie (JD-SPC-01)",
 "1. Ouvrir /spc/cartes puis l'étude\n"
 "2. Saisir ou coller les sous-groupes\n"
 "3. Attendre la fin de la saisie, sans rien actionner\n"
 "4. Lire la carte des moyennes, puis la liste sous la carte",
 "JD-SPC-01, JD-SPC-02",
 "L'analyse part d'elle-même peu après la dernière frappe. Chaque point visé porte un halo, "
 "un abrégé (N1, N2, N5…) et une infobulle nommant ses règles. Les frontières ±1σ et ±2σ "
 "sont tracées. Sous la carte, une ligne par règle déclenchée avec sa sévérité et les "
 "NUMÉROS DE SOUS-GROUPES concernés. Aucune règle ne modifie d'elle-même le statut du procédé.",
 "ADR 0112"),

("SPC", "Cartes de contrôle",
 "Les règles sont jugées contre les limites affichées, pas contre une estimation étrangère",
 "Données", "Bloquant", "demo.ingenieur",
 "Étude dont X̿ et UCL sont visibles à l'écran",
 "1. Relever X̿ et UCL affichés dans le panneau des formules\n"
 "2. Relever les points signalés par l'analyse\n"
 "3. Vérifier à la main qu'un point marqué « hors limites » dépasse bien l'UCL affiché",
 "JD-SPC-01",
 "La ligne centrale employée est X̿ de la carte et σ vaut (UCL − X̿)/3. Aucun point ne peut "
 "être signalé hors d'une limite que l'écran ne dessine pas, ni l'inverse.",
 "ADR 0112"),

("SPC", "Cartes de contrôle",
 "La carte de dispersion est exclue de l'analyse des règles, et l'écran le dit",
 "Négatif", "Mineur", "demo.ingenieur",
 "Étude avec au moins dix sous-groupes complets",
 "1. Ouvrir l'étude\n2. Lire la carte R (ou S)\n3. Chercher des marques de règles sur cette carte",
 "JD-SPC-01",
 "Aucune marque de règle sur la carte de dispersion. Une mention explique que les règles de "
 "série supposent une distribution symétrique, ce que la loi d'une étendue n'est pas.",
 "ADR 0112"),

("SPC", "Cartes de contrôle",
 "Le service d'analyse indisponible n'altère ni les limites ni le Cpk",
 "Négatif", "Majeur", "demo.ingenieur",
 "Service d'analyse arrêté ou injoignable",
 "1. Ouvrir une étude renseignée\n2. Constater le message d'indisponibilité\n"
 "3. Relever Cp, Cpk et les limites\n4. Actionner la relance proposée",
 "JD-SPC-01",
 "Un message dit que l'analyse est indisponible et propose de relancer. Les limites, Cp, Cpk "
 "et le verdict de capabilité restent affichés et exacts : ils sont calculés sans le service. "
 "Aucune règle inventée, aucune liste vide présentée comme un résultat.",
 "ADR 0112 — panne dite et relançable"),

("SPC", "Prévision de dérive",
 "La carte annonce quand le procédé va sortir de ses limites",
 "Fonctionnel", "Bloquant", "demo.ingenieur",
 "Série présentant une tendance régulière vers l'UCL",
 "1. Ouvrir l'étude\n2. Ouvrir le panneau « Prévision de dérive »\n"
 "3. Choisir un horizon en périodes\n4. Lancer la prévision",
 "JD-SPC-01, JD-KPI-01",
 "Un tableau période par période : valeur projetée, intervalle, et RISQUE de sortie en "
 "pourcentage ; la première période dépassant le seuil est mise en évidence. La projection est "
 "tracée en pointillé DANS la carte, dans le prolongement de l'axe, et part du dernier point "
 "mesuré. L'annonce est accompagnée de la confiance, de la tendance par période, du R², du σ "
 "résiduel, du modèle et du nombre de mesures employées.",
 "ADR 0110 ; CLAUDE.md §6.5"),

("SPC", "Prévision de dérive",
 "La CAPA ouverte sur une prévision est préventive et ne prend pas la place du dossier correctif",
 "Fonctionnel", "Majeur", "demo.qualite",
 "Prévision annonçant une sortie de limites sur un KPI du catalogue",
 "1. Cocher « Ouvrir une CAPA préventive si une sortie est annoncée »\n"
 "2. Lancer la prévision\n3. Ouvrir le dossier créé\n"
 "4. Relancer la même prévision\n"
 "5. Faire ensuite survenir un point réellement hors limites sur le même KPI",
 "JD-KPI-01, JD-CAPA-01",
 "Le dossier créé est de nature PRÉVENTIVE, de source SPC_ALERT, et sa criticité suit la "
 "CONFIANCE de la prévision. La seconde prévision ne crée pas de doublon. Le dossier correctif "
 "ouvert plus tard par le point réellement hors limites est un dossier DISTINCT : la référence "
 "d'un dossier prévu n'empêche pas celle d'un dossier constaté.",
 "ADR 0110"),

("SPC", "Capabilité",
 "Un prélèvement de plus de vingt-cinq mesures est refusé au lieu d'être calculé",
 "Négatif", "Majeur", "demo.ingenieur",
 "Droit de saisie SPC",
 "1. Créer une étude\n2. Porter la taille d'échantillon au-delà de 25\n"
 "3. Lire ce que l'écran annonce\n4. Tenter d'obtenir un Cpk",
 "—",
 "L'écran refuse explicitement et dit pourquoi : les tables de constantes publiées s'arrêtent "
 "là. Aucun indice n'est rendu depuis une constante inexistante. Entre 2 et 10 mesures la "
 "carte est X̄ – R (σ = R̄/d2) ; de 11 à 25, X̄ – S (σ = s̄/c4).",
 "ADR 0107"),

# ======================================================================
# NON-CONFORMITÉ — l'écart devient la méthode qui le traitera
# ======================================================================
("Non-conformité", "NC interne",
 "Un écart devient le cycle PDCA ou le projet DMAIC qui le traitera",
 "Fonctionnel", "Bloquant", "demo.qualite",
 "JD-NC-01 ouverte, modules pdca et dmaic actifs",
 "1. Ouvrir la fiche de NC-2026-0006\n2. Déclencher la création d'une méthode de traitement\n"
 "3. Lire ce que la fenêtre propose et ce qu'elle explique\n"
 "4. Désigner la personne qui mènera le travail\n5. Valider, puis ouvrir l'objet créé",
 "JD-NC-01",
 "La fenêtre ne redemande ni l'intitulé ni l'énoncé du problème : le serveur les reprend de "
 "l'écart avec sa référence, sa gravité, sa description et sa zone. Elle exige en revanche la "
 "personne responsable, choisie dans l'annuaire. Le cycle ou le projet créé cite l'écart "
 "d'origine, et la fiche de l'écart porte le lien en retour.",
 "Référentiel transverse — CLAUDE.md §3.6"),

("Non-conformité", "NC interne",
 "La gravité propose la méthode, mais le choix contraire part quand même",
 "Fonctionnel", "Majeur", "demo.qualite",
 "Un écart majeur et un écart critique ouverts",
 "1. Sur l'écart critique, ouvrir la fenêtre : relever la méthode proposée\n"
 "2. Sur l'écart majeur, relever la proposition\n"
 "3. Choisir délibérément l'autre méthode que celle proposée\n4. Valider",
 "JD-NC-01, JD-NC-02",
 "Critique propose le projet DMAIC, mineur et majeur proposent le cycle PDCA, et la fenêtre DIT "
 "pourquoi. Le choix contraire part tel quel, et l'écran le signale au lieu de l'interdire : la "
 "personne qui tient le dossier en sait plus qu'une table.",
 "L'IA et la plateforme proposent, l'humain décide — CLAUDE.md §12.3"),

("Non-conformité", "CAPA",
 "Le critère d'efficacité s'écrit une fois, en tête du panneau",
 "Fonctionnel", "Mineur", "demo.qualite",
 "JD-CAPA-01 avec critère posé et une évaluation déjà enregistrée",
 "1. Ouvrir /capa/efficacite\n2. Ouvrir le dossier à juger\n"
 "3. Relever où le critère est écrit, et combien de fois\n4. Suivre le renvoi vers la fiche CAPA",
 "JD-CAPA-01",
 "Le critère apparaît une seule fois, en tête du panneau. « Jugé contre » ne réapparaît que "
 "s'il diffère du critère courant. Le renvoi ouvre directement le dossier à juger, et son "
 "libellé est lisible sur son fond.",
 "ADR 0108"),

# ======================================================================
# OPÉRATIONS — le constat d'audit entre au registre
# ======================================================================
("Opérations", "Audits",
 "Un constat d'audit non conforme entre au registre des écarts",
 "Fonctionnel", "Bloquant", "demo.auditeur",
 "JD-AUD-02 avec un constat non conforme non encore instruit ; compte portant nc.create",
 "1. Ouvrir le rapport d'audit\n2. Sur le constat non conforme, déclarer l'écart\n"
 "3. Renseigner l'intitulé, la catégorie et la nature\n4. Valider\n"
 "5. Ouvrir le registre des NC et retrouver l'écart",
 "JD-AUD-02",
 "Le formulaire ne demande QUE l'intitulé, la catégorie et la nature : la description, la "
 "gravité, la date de relevé, la clause et l'origine sont reprises du constat. L'écart apparaît "
 "au registre, compte dans les indicateurs et peut porter une analyse de cause. Le constat "
 "porte le lien vers l'écart créé.",
 "Référentiel transverse — CLAUDE.md §3.6"),

("Opérations", "Audits",
 "Un constat déjà instruit ne crée pas un second écart",
 "Négatif", "Majeur", "demo.auditeur",
 "Constat ayant déjà ouvert une NC (cas précédent joué)",
 "1. Revenir sur le même constat\n2. Tenter de déclarer l'écart une seconde fois",
 "JD-AUD-02",
 "Refus en 409 avec un message qui dit que le travail est déjà fait et renvoie vers l'écart "
 "existant — pas une invitation à recharger un écran qui refusera toujours.",
 "ADR 0088 — un fait, un dossier"),

# ======================================================================
# MÉTHODES QUALITÉ — 5S vers le cercle, DMAIC vers le détrompeur
# ======================================================================
("Méthodes qualité", "5S",
 "Un pilier faible est porté au cercle de qualité avec ses chiffres",
 "Fonctionnel", "Majeur", "demo.demandeur",
 "Un audit 5S terminé, au moins un pilier noté bas ; un cercle actif",
 "1. Ouvrir la fiche de l'audit terminé\n2. Sur le pilier noté bas, « Porter au cercle »\n"
 "3. Lire ce que la fenêtre demande et ce qu'elle rappelle\n4. Choisir le cercle, valider\n"
 "5. Ouvrir le cercle et retrouver l'idée",
 "JD-ORG-01",
 "La fenêtre ne demande QUE le cercle qui prend le sujet, et rappelle en lecture la zone, le "
 "pilier et la note. L'idée déposée dans le cercle CITE son origine (audit, zone, pilier, note) "
 "au lieu de pointer l'audit par une clé : l'audit se clôture et s'archive, l'idée reste à "
 "traiter.",
 "Référentiel transverse — CLAUDE.md §3.6"),

("Méthodes qualité", "5S",
 "Un pilier noté porte la photo de son constat, même hors ligne",
 "Mobile/Hors ligne", "Majeur", "demo.demandeur",
 "Audit 5S en cours sur téléphone, photo déjà prise avec l'appareil, réseau coupé",
 "1. Couper le réseau\n2. Noter un pilier\n3. Joindre le fichier image déjà pris\n"
 "4. Terminer l'audit\n5. Rétablir le réseau et attendre la synchronisation\n"
 "6. Rouvrir l'audit depuis un poste",
 "JD-ORG-01",
 "L'import se fait depuis un fichier déjà pris, sans ouvrir l'appareil. Hors ligne, la photo "
 "est rattachée au pilier PAR SON NOM et non à une ligne de notation par identifiant — la "
 "notation attend dans la file et n'a pas encore d'identifiant. Après synchronisation, la photo "
 "est visible à côté de la note du bon pilier.",
 "CLAUDE.md §3.2, §15.3"),

("Méthodes qualité", "DMAIC",
 "Une action d'amélioration devient un détrompeur inscrit à la bibliothèque du client",
 "Fonctionnel", "Majeur", "demo.ingenieur",
 "Un projet DMAIC en phase Improve avec un plan d'actions renseigné",
 "1. Ouvrir le projet, onglet du plan d'actions\n"
 "2. Sur une ligne, « Inscrire un détrompeur »\n"
 "3. Vérifier que le texte de l'action est repris\n"
 "4. Renseigner ce que l'action ne porte pas : prévenir ou détecter, et le mécanisme\n"
 "5. Valider\n6. Ouvrir un AUTRE projet du même client et ouvrir sa fenêtre d'assignation",
 "JD-PRD-01",
 "La fenêtre est préremplie du texte de l'action et ne demande que le type et le mécanisme. Le "
 "dispositif est inscrit ET déployé sur le projet dans le même mouvement. Il apparaît ensuite "
 "dans la fenêtre d'assignation de n'importe quel autre projet du client. Il cite son origine "
 "(projet, rang de l'action) plutôt que de pointer la ligne par une clé.",
 "CLAUDE.md §3.4 — bibliothèque de dispositifs"),

("Transverse — Sécurité", "Bibliothèque de détrompeurs",
 "Un détrompeur conçu par un client n'est pas visible d'un autre",
 "Sécurité", "Bloquant", "demo.qualite",
 "JD-TEN-02 : un détrompeur inscrit chez « Fonderie Atlantique », session ouverte sur le second client",
 "1. Inscrire un détrompeur depuis une action, chez le premier client\n"
 "2. Se connecter sur le second client\n"
 "3. Ouvrir un projet DMAIC et lister les dispositifs assignables\n"
 "4. Tenter d'atteindre directement le dispositif du premier client",
 "JD-TEN-02",
 "Le second client ne voit QUE le catalogue livré par la plateforme et ses propres dispositifs. "
 "L'accès direct au dispositif de l'autre client échoue. Verser un dispositif de client au "
 "catalogue partagé montrerait à tous la solution d'un seul.",
 "ADR 0086 ; CLAUDE.md §18.2 #2"),

# ======================================================================
# PILOTAGE — l'exigence qui devient un risque, et l'accueil de direction
# ======================================================================
("Pilotage", "Parties prenantes",
 "Le risque énoncé sur une exigence entre au registre",
 "Fonctionnel", "Majeur", "demo.qualite",
 "Une fiche d'exigence de partie intéressée portant un risque énoncé, module risk actif",
 "1. Ouvrir /stakeholders puis la fiche d'exigence\n2. Inscrire le risque au registre\n"
 "3. Vérifier ce que l'écran redemande\n4. Ouvrir /risques et retrouver la ligne\n"
 "5. Revenir sur l'exigence",
 "JD-RSQ-01",
 "L'écran ne redemande ni l'intitulé, ni la cause, ni l'effet : ils viennent de l'exigence. La "
 "ligne naît avec sa référence, sa cotation, son journal et sa trace d'audit, exactement comme "
 "depuis l'écran du registre — elle est créée par le module du registre, pas par un accès "
 "direct au dépôt. L'exigence porte en retour l'identifiant de la ligne.",
 "ADR 0076 ; ADR 0104 ; CLAUDE.md §4.2"),

("Pilotage", "Accueil",
 "Les indicateurs d'accueil sont ceux qui engagent, pas un volume d'activité",
 "Fonctionnel", "Mineur", "demo.dir",
 "Réclamations clients ouvertes et CAPA de criticité haute et moyenne présentes",
 "1. Ouvrir /home\n2. Relever les indicateurs clés affichés\n"
 "3. Vérifier qu'ils mènent aux listes correspondantes",
 "JD-NC-02, JD-CAPA-01",
 "Les indicateurs portent les réclamations clients ouvertes et les CAPA de criticité haute et "
 "moyenne — ce qui sort de l'organisation et ce qui engage. Le décompte des cycles et des "
 "écarts internes appartient au tableau de bord de son module, pas à l'accueil de la direction.",
 "CLAUDE.md §7.1 niveau 1"),

# ======================================================================
# RÉFÉRENTIELS — la feuille de route de certification
# ======================================================================
("Référentiels", "Standards Hub",
 "Une étape de certification porte une échéance au calendrier et un responsable de l'annuaire",
 "Fonctionnel", "Majeur", "demo.qualite",
 "JD-REF-01 : ISO 9001 adoptée, feuille de route de certification ouverte",
 "1. Ouvrir /standards puis la feuille de route\n2. Ouvrir une étape\n"
 "3. Fixer l'échéance\n4. Désigner le responsable\n5. Enregistrer et relire la ligne\n"
 "6. Depuis l'onglet des exigences, suivre « Couvrir cet écart »",
 "JD-REF-01",
 "L'échéance se choisit au calendrier, la durée type du modèle restant une indication. Le "
 "responsable est une personne de l'annuaire, jamais un texte libre, et s'affiche par son nom. "
 "« Couvrir cet écart » mène à l'onglet des preuves, pas à celui d'où l'on vient de cliquer.",
 "ADR 0092 ; CLAUDE.md §8.5"),

# ======================================================================
# ADMINISTRATION — listes du client, et piste d'audit de l'éditeur
# ======================================================================
("Administration", "Listes déroulantes",
 "Les douze listes du client se règlent dans un seul écran",
 "Fonctionnel", "Majeur", "demo.admin",
 "Compte administrateur du client portant authz.manage",
 "1. Ouvrir /admin/listes\n2. Choisir une liste ouverte (zones d'audit 5S)\n"
 "3. Ajouter une entrée, renommer une autre, réordonner, désactiver une troisième\n"
 "4. Enregistrer\n5. Ouvrir l'écran qui consomme la liste et vérifier les choix proposés\n"
 "6. Revenir et réinitialiser la liste",
 "JD-ORG-01",
 "Un seul écran porte les douze listes. Sur une liste OUVERTE, l'ajout, le retrait, le "
 "renommage, le réordonnancement et la désactivation fonctionnent. L'écran consommateur propose "
 "exactement les entrées actives, dans l'ordre choisi. La réinitialisation rend les entrées de "
 "la plateforme.",
 "ADR 0111 ; CLAUDE.md §18.2 #9"),

("Administration", "Listes déroulantes",
 "Une liste adossée à une énumération se renomme, mais ne s'étend pas",
 "Négatif", "Bloquant", "demo.admin",
 "Une liste en mode « renommer seulement » (types d'audit)",
 "1. Ouvrir /admin/listes et choisir la liste\n2. Lire ce que l'écran annonce AVANT d'essayer\n"
 "3. Renommer une entrée et réordonner : enregistrer\n"
 "4. Tenter d'ajouter un code nouveau\n5. Tenter de désactiver une entrée",
 "JD-ORG-01",
 "L'écran annonce le mode avant toute tentative. Le renommage et le réordonnancement passent. "
 "L'ajout d'un code hors de l'ensemble de la plateforme est refusé, la désactivation aussi : "
 "un code que nul chemin de code ne sait traiter ne doit pas pouvoir entrer.",
 "ADR 0111"),

("Administration", "Listes déroulantes",
 "Les mots du client s'affichent partout, et disparaissent à la déconnexion",
 "Données", "Majeur", "demo.admin",
 "Au moins deux listes renommées chez « Fonderie Atlantique » ; JD-TEN-02 disponible",
 "1. Renommer des entrées de deux listes\n"
 "2. Parcourir les écrans qui les consomment : listes déroulantes, tableaux, exports CSV\n"
 "3. Se déconnecter\n4. Se reconnecter sur le second client et parcourir les mêmes écrans\n"
 "5. Vider les réglages du premier client et recharger",
 "JD-ORG-01, JD-TEN-02",
 "Les libellés du client apparaissent dans les listes, les tableaux ET les exports. Le second "
 "client ne lit jamais les mots du premier. Sans réglage, tout retombe sur les libellés de "
 "l'écran, traduits — exactement l'état d'avant le mécanisme.",
 "ADR 0111 ; CLAUDE.md §18.2 #2"),

("Administration", "Piste d'audit de la plateforme",
 "Les actes de l'éditeur sur le dossier d'un client sont consignés et vérifiables",
 "Sécurité", "Bloquant", "superadmin",
 "Console de la plateforme accessible, un client et un cabinet existants",
 "1. Ouvrir /platform/piste-audit : relever le dernier numéro de séquence\n"
 "2. Créer un client, ouvrir un module, fixer un tarif, accorder puis retirer l'accès d'un cabinet\n"
 "3. Revenir sur la piste et relire les actes\n4. Lancer la vérification de la chaîne\n"
 "5. Exporter le dossier et relever son sceau",
 "JD-ORG-01, JD-TEN-02",
 "Chaque acte est consigné avec son numéro de séquence, son acteur nommé, son horodatage, son "
 "origine et le client visé. La chaîne se vérifie et se déclare intacte. Le dossier exporté "
 "porte un sceau et sa plage de séquence. Aucune surface d'écriture n'est offerte : on ne peut "
 "pas fabriquer un acte qui n'a pas eu lieu.",
 "ADR 0109 ; CLAUDE.md §11.5"),

("Administration", "Piste d'audit de la plateforme",
 "Une ligne de piste d'audit ne se modifie ni ne se supprime, même en base",
 "Sécurité", "Bloquant", "superadmin",
 "Accès psql au rôle applicatif, au moins trois actes consignés",
 "1. Tenter un UPDATE sur une ligne de platform_audit_events\n"
 "2. Tenter un DELETE sur la dernière ligne\n"
 "3. Tenter de ramener le compteur de séquence en arrière\n"
 "4. Rejouer les trois essais sur audit_events du moteur\n5. Relancer la vérification de chaîne",
 "—",
 "Les trois ordres sont refusés par la base (SQLSTATE 42501), sur les deux journaux. Le refus "
 "ne dépend pas des droits accordés au rôle applicatif : il tient même pour le propriétaire de "
 "la base. La vérification de chaîne reste intacte après les tentatives.",
 "ADR 0109"),

("Administration", "Console de la plateforme",
 "Un client et un cabinet se corrigent sans toucher au repère ni à l'état",
 "Fonctionnel", "Majeur", "superadmin",
 "Un client existant dont la raison sociale comporte une faute",
 "1. Ouvrir la console, le client\n2. Corriger la raison sociale et l'offre\n"
 "3. Chercher le repère du client et son état dans la fenêtre de correction\n"
 "4. Enregistrer, puis vérifier le repère dans les adresses et les journaux\n"
 "5. Désactiver le client depuis le bouton dédié, lire la confirmation\n"
 "6. Reprendre un cabinet partenaire de la même façon",
 "JD-ORG-01, JD-TEN-02",
 "La correction porte sur la raison sociale et l'offre. Le repère n'est PAS modifiable : il "
 "nomme le client dans les adresses, les jetons et les journaux, et le changer rendrait "
 "illisible ce qu'un auditeur vient relire. L'état ne se change pas par une case au milieu "
 "d'un formulaire d'orthographe : couper l'accès d'un établissement se décide sur un bouton "
 "qui le dit, avec confirmation. La correction laisse une trace à la piste d'audit.",
 "ADR 0106 ; ADR 0109"),

]
