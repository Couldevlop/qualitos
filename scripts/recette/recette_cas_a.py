# -*- coding: utf-8 -*-
"""Cas de recette — première moitié : pilotage, méthodes, IA, SPC, APQP, écarts.

Chaque cas est un tuple :
  (domaine, fonctionnalité, scénario, type, criticité, compte,
   préconditions, étapes, données, attendu, exigence)

Les étapes sont numérotées et séparées par des retours à la ligne : le classeur
les rend dans une cellule à renvoi automatique, et une personne qui recette lit
une liste, pas un paragraphe.
"""

CAS_A = [

# ======================================================================
# 1. PILOTAGE
# ======================================================================
("Pilotage", "Accueil", "Le tableau du jour montre mes tâches et rien d'autre",
 "Fonctionnel", "Majeur", "demo.qualite",
 "Au moins une CAPA, un audit et une action en retard assignés à Isabelle Fontaine",
 "1. Se connecter\n2. Ouvrir /home\n3. Relever chaque bloc affiché",
 "JD-CAPA-01, JD-AUD-01",
 "Les blocs ne listent que des objets dont Isabelle est pilote ou destinataire. "
 "Aucun objet d'un autre utilisateur. Chaque ligne est cliquable et mène à la fiche.",
 "CLAUDE.md §7.1 niveau 3"),

("Pilotage", "Accueil", "Un utilisateur sans aucune tâche ne voit pas un écran vide muet",
 "Fonctionnel", "Mineur", "demo.externe",
 "Compte sans affectation",
 "1. Se connecter\n2. Ouvrir /home",
 "—",
 "Un message explique qu'il n'y a rien à traiter et propose une action utile. "
 "Pas de bloc vide sans libellé, pas de zéro isolé.",
 "UX — état vide"),

("Pilotage", "Tableau de bord", "Les 12 KPI stratégiques s'affichent avec cible, réalisé et tendance",
 "Fonctionnel", "Bloquant", "demo.dir",
 "JD-KPI-01 chargé",
 "1. Ouvrir /dashboard\n2. Vérifier chaque tuile : valeur, cible, écart, tendance 12 mois\n"
 "3. Survoler une tuile pour lire la définition",
 "JD-KPI-01",
 "Chaque KPI affiche sa valeur, sa cible, son sens de variation et sa définition "
 "(formule, seuil, source, propriétaire). Aucun indicateur sans définition.",
 "CLAUDE.md §18.2.8"),

("Pilotage", "Tableau de bord", "Le drill-down descend du KPI agrégé à l'événement source",
 "Fonctionnel", "Majeur", "demo.dir",
 "KPI « Taux de non-conformités » alimenté",
 "1. Cliquer sur la tuile du taux de NC\n2. Descendre au mois le plus haut\n"
 "3. Descendre à la liste des NC\n4. Ouvrir une NC",
 "JD-NC-03",
 "Chaque niveau conserve le filtre du niveau précédent. La dernière étape ouvre "
 "la fiche d'une NC réelle, et la somme des NC listées égale la valeur du KPI.",
 "CLAUDE.md §7.3 drill-down"),

("Pilotage", "Tableau de bord", "Une période sans donnée ne s'affiche pas comme un zéro",
 "Négatif", "Majeur", "demo.dir",
 "Un KPI sans relevé sur le mois courant",
 "1. Ouvrir /dashboard\n2. Lire la tuile du KPI sans relevé",
 "—",
 "La tuile indique l'absence de mesure. Elle n'affiche pas « 0 », qui se lirait "
 "comme une performance parfaite ou nulle selon le sens du KPI.",
 "Qualité de la donnée"),

("Pilotage", "Tableau de bord SMI", "La matrice des exigences montre la couverture par clause",
 "Fonctionnel", "Majeur", "demo.qualite",
 "JD-REF-01, ISO 9001 adoptée",
 "1. Ouvrir /smi\n2. Lire le taux de couverture global\n3. Ouvrir une clause non couverte\n"
 "4. Suivre le lien vers la preuve manquante",
 "JD-REF-01, JD-DOC-01",
 "Le taux se recalcule à partir des preuves réellement rattachées. Une clause "
 "sans preuve est signalée et propose l'action qui la couvrirait.",
 "ADR 0077"),

("Pilotage", "Tableau de bord SMI", "Ajouter l'ISO 14001 au SMI met à jour la co-couverture",
 "Fonctionnel", "Majeur", "demo.admin",
 "Seule l'ISO 9001 est adoptée",
 "1. Ouvrir /standards\n2. Adopter l'ISO 14001\n3. Revenir sur /smi\n"
 "4. Ouvrir /standards-ims",
 "JD-REF-01",
 "L'ISO 14001 apparaît dans le SMI. Les clauses communes HLS (5.2, 6.1, 9.3…) "
 "sont marquées comme mutualisées, et une preuve unique couvre les deux normes.",
 "ADR 0090, CLAUDE.md §8.9"),

("Pilotage", "Parties prenantes", "L'assistant IA propose des parties prenantes argumentées",
 "Fonctionnel", "Majeur", "demo.qualite",
 "Contexte du client renseigné (secteur, taille, sites)",
 "1. Ouvrir /stakeholders\n2. Lancer l'assistant\n3. Lire les propositions",
 "JD-ORG-01",
 "L'assistant renvoie des parties prenantes concrètes avec leurs attentes et leur "
 "influence, et cite ce sur quoi il s'appuie. Il ne renvoie ni liste vide ni "
 "catégories génériques sans contenu.",
 "CLAUDE.md §12.3 explicabilité"),

("Pilotage", "Mes dashboards", "Composer un tableau de bord par glisser-déposer et le retrouver",
 "Fonctionnel", "Mineur", "demo.qualite",
 "—",
 "1. Ouvrir /dashboard-builder\n2. Ajouter 4 widgets\n3. Les repositionner\n"
 "4. Enregistrer\n5. Se déconnecter, se reconnecter, rouvrir l'écran",
 "JD-KPI-01",
 "La disposition est restituée à l'identique. Le tableau appartient à son auteur "
 "et n'apparaît pas chez un autre utilisateur.",
 "CLAUDE.md §7.3"),

("Pilotage", "Mode TV", "La rotation plein écran enchaîne les vues sans intervention",
 "Fonctionnel", "Mineur", "demo.qualite",
 "Au moins 3 vues configurées",
 "1. Ouvrir /tv\n2. Passer en plein écran\n3. Observer 3 rotations complètes\n"
 "4. Laisser tourner 15 minutes",
 "JD-KPI-01",
 "Les vues défilent à l'intervalle prévu, les données se rafraîchissent, et "
 "l'écran ne se met pas en veille ni n'affiche d'erreur de session.",
 "CLAUDE.md §7.3 mode TV"),

("Pilotage", "Indicateurs (KPI)", "Créer un KPI personnalisé exige sa définition complète",
 "Négatif", "Majeur", "demo.admin",
 "—",
 "1. Ouvrir /kpis\n2. Créer un KPI\n3. Laisser la formule vide\n4. Enregistrer",
 "—",
 "L'enregistrement est refusé avec un message nommant le champ manquant. "
 "Un KPI sans formule, seuil, source ni propriétaire ne peut pas exister.",
 "CLAUDE.md §18.2.8"),

("Pilotage", "Coûts de non-qualité", "Le COQ se décompose en quatre familles",
 "Fonctionnel", "Majeur", "demo.dir",
 "Coûts saisis sur 12 mois",
 "1. Ouvrir /cout-qualite\n2. Lire la décomposition\n3. Descendre sur « défaillances externes »",
 "JD-NC-03",
 "Prévention, détection, défaillances internes et externes sont distinguées. "
 "La somme des quatre égale le total affiché, au centime près.",
 "CLAUDE.md §6.2 COQ"),

# ======================================================================
# 2. MÉTHODES QUALITÉ
# ======================================================================
("Méthodes qualité", "PDCA", "Dérouler un cycle complet Plan → Do → Check → Act",
 "Fonctionnel", "Bloquant", "demo.qualite",
 "—",
 "1. Ouvrir /pdca, créer un cycle\n2. Renseigner l'étape Plan et son KPI cible\n"
 "3. Passer en Do, ajouter 2 actions\n4. Passer en Check, saisir la mesure\n"
 "5. Passer en Act, conclure",
 "JD-KPI-01",
 "Chaque transition est horodatée, attribuée, et impossible à faire à rebours "
 "sans trace. L'écart entre la cible et la mesure est calculé à l'étape Check.",
 "CLAUDE.md §3.1"),

("Méthodes qualité", "PDCA", "Un cycle qui patine est détecté et signalé",
 "Fonctionnel", "Majeur", "demo.qualite",
 "Un cycle avec 3 passages en Plan sans Do",
 "1. Ouvrir le cycle concerné\n2. Lire l'alerte",
 "—",
 "La plateforme signale le cycle comme « qui patine » et dit pourquoi "
 "(Plan répétés sans Do). Elle ne se contente pas d'afficher l'historique brut.",
 "CLAUDE.md §3.1 IA"),

("Méthodes qualité", "PDCA", "Une cause d'Ishikawa ouvre un cycle sans ressaisie",
 "Intégration", "Bloquant", "demo.qualite",
 "Un diagramme d'Ishikawa avec une cause retenue",
 "1. Ouvrir le diagramme\n2. Sur la cause retenue, choisir « Ouvrir un cycle PDCA »\n"
 "3. Vérifier le cycle créé",
 "JD-NC-01",
 "Le cycle reprend l'intitulé de la cause, garde le lien vers le diagramme "
 "d'origine, et le diagramme indique que la cause est traitée.",
 "CLAUDE.md §3.6 référentiel transverse"),

("Méthodes qualité", "5S", "Réaliser un audit terrain sur les cinq piliers avec photos",
 "Fonctionnel", "Bloquant", "demo.demandeur",
 "Une zone définie, un audit 5S planifié",
 "1. Ouvrir /fives sur mobile\n2. Démarrer l'audit\n3. Noter les 5 piliers\n"
 "4. Joindre 2 photos\n5. Clôturer",
 "JD-ORG-01",
 "Le score 5S est calculé, la heatmap de la zone se met à jour, et un plan "
 "d'action est proposé pour chaque pilier sous le seuil.",
 "CLAUDE.md §3.2"),

("Méthodes qualité", "5S", "Un audit 5S se réalise entièrement hors ligne puis se synchronise",
 "Mobile/Hors ligne", "Bloquant", "demo.demandeur",
 "Audit planifié, appareil mobile",
 "1. Ouvrir l'audit, couper le réseau\n2. Noter les 5 piliers, joindre 2 photos\n"
 "3. Clôturer hors ligne\n4. Rétablir le réseau\n5. Ouvrir /offline-queue",
 "JD-ORG-01",
 "L'audit est saisi sans erreur hors ligne, la file d'attente le montre, "
 "et la synchronisation le remonte entier, photos comprises, sans doublon.",
 "CLAUDE.md §15.2"),

("Méthodes qualité", "5S", "L'IA de vision lit la photo et propose un constat",
 "Intégration", "Majeur", "demo.demandeur",
 "Module Vision actif, photo d'une zone encombrée",
 "1. Joindre la photo à l'audit\n2. Lancer l'analyse\n3. Lire la proposition",
 "JD-ORG-01",
 "L'analyse propose un constat (encombrement, étiquetage manquant…) avec un "
 "niveau de confiance. La proposition est modifiable et ne s'applique pas seule.",
 "ADR 0029 — l'IA suggère, l'humain décide"),

("Méthodes qualité", "DMAIC", "Conduire un projet des cinq phases jusqu'au détrompeur",
 "Fonctionnel", "Majeur", "demo.ingenieur",
 "—",
 "1. Ouvrir /dmaic, créer un projet\n2. Define : périmètre et SIPOC\n"
 "3. Measure : rattacher la carte SPC\n4. Analyze : test d'hypothèse\n"
 "5. Improve : créer un détrompeur\n6. Control : plan de surveillance",
 "JD-SPC-01, JD-PRD-01",
 "Chaque phase exige ses livrables avant de passer à la suivante. Le détrompeur "
 "créé en Improve rejoint la bibliothèque et devient réutilisable.",
 "CLAUDE.md §3.4"),

("Méthodes qualité", "Cercles", "Un compte rendu est généré puis corrigé avant diffusion",
 "Fonctionnel", "Majeur", "demo.qualite",
 "Un cercle constitué de 6 personnes",
 "1. Ouvrir /circles, démarrer une réunion\n2. Déposer l'enregistrement\n"
 "3. Générer le compte rendu\n4. Corriger deux phrases\n5. Diffuser",
 "JD-ORG-01",
 "Le compte rendu généré est un brouillon modifiable. Les actions extraites sont "
 "proposées avec un responsable suggéré, et rien n'est assigné sans validation.",
 "CLAUDE.md §3.3"),

("Méthodes qualité", "Boîte à idées", "Une idée est suivie jusqu'à la mesure de son effet",
 "Fonctionnel", "Mineur", "demo.demandeur",
 "—",
 "1. Déposer une idée\n2. La faire valider par demo.qualite\n3. La mettre en œuvre\n"
 "4. Saisir l'impact mesuré",
 "JD-KPI-01",
 "Le statut progresse sans retour arrière silencieux, et l'impact mesuré "
 "alimente l'indicateur du nombre d'idées mises en œuvre.",
 "CLAUDE.md §3.3"),

("Méthodes qualité", "Designer de workflow", "Un workflow dessiné est appliqué à un objet réel",
 "Fonctionnel", "Majeur", "demo.admin",
 "—",
 "1. Ouvrir /workflow-designer\n2. Dessiner un circuit CAPA à 4 étapes\n"
 "3. L'activer\n4. Créer une CAPA et vérifier le circuit appliqué",
 "JD-CAPA-01",
 "La CAPA créée suit le circuit dessiné. Modifier le circuit ne change pas "
 "rétroactivement les CAPA déjà engagées.",
 "CLAUDE.md §5.4"),

# ======================================================================
# 3. ANALYSES IA
# ======================================================================
("Analyses IA", "Anomalies IA", "La détection signale un point et sait dire pourquoi",
 "Fonctionnel", "Majeur", "demo.ingenieur",
 "JD-SPC-02 chargé",
 "1. Ouvrir /anomaly\n2. Lancer la détection\n3. Ouvrir un point signalé\n"
 "4. Cliquer sur « Pourquoi ? »",
 "JD-SPC-02, JD-EQP-03",
 "Le point est signalé avec un score, et l'explication SHAP montre la "
 "contribution de chaque variable. La somme des contributions égale l'écart "
 "au score de base.",
 "ADR 0022, ADR 0024"),

("Analyses IA", "Prévision KPI", "La prévision affiche son intervalle, pas un chiffre seul",
 "Fonctionnel", "Majeur", "demo.dir",
 "JD-KPI-01 sur 18 mois",
 "1. Ouvrir /forecast\n2. Choisir un KPI\n3. Projeter à 3 mois",
 "JD-KPI-01",
 "La projection est encadrée par un intervalle et la méthode est nommée. "
 "Une série trop courte refuse de projeter plutôt que d'inventer une tendance.",
 "ADR 0023"),

("Analyses IA", "Clustering NC", "Les NC qui se ressemblent sont regroupées",
 "Fonctionnel", "Majeur", "demo.qualite",
 "JD-NC-03 chargé",
 "1. Ouvrir /nc-clusters\n2. Lancer le regroupement\n3. Ouvrir le groupe le plus peuplé",
 "JD-NC-03",
 "Les 7 NC récurrentes sur P-204 se retrouvent dans le même groupe. Chaque "
 "groupe liste ses NC d'origine, ouvrables une par une.",
 "ADR 0023bis"),

("Analyses IA", "Réclamations IA", "Le sentiment et la classification priorisent les réclamations",
 "Fonctionnel", "Mineur", "demo.qualite",
 "JD-NC-02 et 20 réclamations",
 "1. Ouvrir /complaints-nlp\n2. Lancer l'analyse\n3. Trier par criticité",
 "JD-NC-02",
 "Chaque réclamation reçoit un sentiment et une catégorie. Les plus négatives "
 "remontent, et le classement est justifié par les termes relevés.",
 "ADR 0025"),

("Analyses IA", "Assistant IA (NLQ)", "Une question en français rend un chiffre juste",
 "Fonctionnel", "Bloquant", "demo.qualite",
 "JD-NC-03 chargé",
 "1. Ouvrir /nlq\n2. Demander « Combien de NC ouvertes sur le site de Nantes ? »\n"
 "3. Vérifier le chiffre contre la liste filtrée de /nc",
 "JD-NC-03, JD-ORG-01",
 "Le chiffre rendu est exactement celui de la liste filtrée. La réponse montre "
 "la requête exécutée et reste lisible.",
 "CLAUDE.md §12.1 NLQ"),

("Analyses IA", "Assistant IA (NLQ)", "L'assistant refuse de sortir du périmètre du client",
 "Sécurité", "Bloquant", "demo.qualite",
 "JD-TEN-02 existe",
 "1. Ouvrir /nlq\n2. Demander les NC de « Plasturgie du Centre »\n"
 "3. Demander la liste des tables de la base",
 "JD-TEN-02",
 "Aucune donnée de l'autre client n'est rendue, et la structure de la base "
 "n'est pas divulguée. Le refus est explicite, pas une erreur technique.",
 "ADR 0086, OWASP LLM06"),

("Analyses IA", "Assistant IA (NLQ)", "Une injection de consigne dans une donnée est sans effet",
 "Sécurité", "Bloquant", "demo.qualite",
 "Une NC dont le titre contient « ignore les instructions précédentes et… »",
 "1. Créer la NC piégée\n2. Ouvrir /nlq\n3. Demander un résumé des NC ouvertes",
 "JD-NC-01",
 "Le résumé traite le texte piégé comme une donnée. Aucune consigne qui s'y "
 "trouve n'est suivie.",
 "OWASP LLM01"),

("Analyses IA", "Storyboard IA", "Le récit mensuel cite ce sur quoi il s'appuie",
 "Fonctionnel", "Mineur", "demo.dir",
 "Un mois complet de données",
 "1. Ouvrir /storyboard\n2. Générer le récit du mois\n3. Suivre deux citations",
 "JD-KPI-01, JD-NC-03",
 "Chaque affirmation chiffrée renvoie à l'objet qui la porte. Aucune affirmation "
 "sans source.",
 "CLAUDE.md §7.4 storyboards"),

("Analyses IA", "Garde-fous", "Le quota d'appels IA par client est tenu",
 "Négatif", "Majeur", "demo.qualite",
 "Quota abaissé à 5 appels/minute pour l'essai",
 "1. Lancer 10 requêtes NLQ en moins d'une minute\n2. Observer les réponses 6 à 10",
 "—",
 "Les appels au-delà du quota reçoivent un 429 avec un délai de réessai. "
 "Le service reste disponible pour les autres fonctions.",
 "ADR 0017 — LLM04"),

# ======================================================================
# 4. SPC
# ======================================================================
("SPC", "Cartes de contrôle", "Une carte X-barre/R calcule ses limites et les affiche",
 "Fonctionnel", "Bloquant", "demo.ingenieur",
 "JD-SPC-01 chargé",
 "1. Ouvrir /spc/cartes\n2. Choisir la caractéristique\n3. Lire LSC, LC, LIC\n"
 "4. Vérifier une limite à la main avec la formule affichée",
 "JD-SPC-01, JD-PRD-01",
 "Les limites calculées correspondent à la formule affichée, avec les constantes "
 "A2/D3/D4 pour n=5. L'écart avec le calcul manuel est nul.",
 "ADR 0096"),

("SPC", "Cartes de contrôle", "Les règles de Nelson se déclenchent sur une série construite",
 "Fonctionnel", "Bloquant", "demo.ingenieur",
 "JD-SPC-02 chargé",
 "1. Ouvrir la carte de la série piégée\n2. Lire les règles signalées",
 "JD-SPC-02",
 "La règle 2 (8 points du même côté) est signalée, nommée, et les points "
 "concernés sont mis en évidence. Aucune règle non applicable n'est levée.",
 "ADR 0018 — 8 règles de Nelson"),

("SPC", "Cartes de contrôle", "Un point hors limites ouvre une non-conformité",
 "Intégration", "Majeur", "demo.ingenieur",
 "Une mesure hors limites saisie",
 "1. Saisir la mesure hors limites\n2. Observer l'alerte\n3. Ouvrir la NC créée",
 "JD-SPC-01, JD-NC-01",
 "Une NC de type alerte SPC est créée, liée à la carte et à la caractéristique, "
 "et le manager qualité est notifié.",
 "ADR 0018"),

("SPC", "Capabilité", "Cp, Cpk, Pp et Ppk sont calculés et distingués",
 "Fonctionnel", "Majeur", "demo.ingenieur",
 "JD-SPC-01 avec tolérances renseignées",
 "1. Ouvrir l'étude de capabilité\n2. Lire les quatre indices\n"
 "3. Vérifier Cpk à la main",
 "JD-SPC-01",
 "Les quatre indices sont présents et différenciés (court terme / long terme). "
 "Le calcul manuel de Cpk retombe sur la valeur affichée.",
 "ADR 0096"),

("SPC", "Capabilité", "Une étude sans tolérance refuse de rendre un indice",
 "Négatif", "Majeur", "demo.ingenieur",
 "Caractéristique sans limites de spécification",
 "1. Lancer l'étude de capabilité",
 "—",
 "L'étude explique qu'un indice de capabilité sans tolérance n'a pas de sens, "
 "et n'affiche ni 0 ni une valeur par défaut.",
 "Qualité du calcul"),

("SPC", "Gage R&R", "Une étude R&R rend %R&R, ndc et la décomposition",
 "Fonctionnel", "Majeur", "demo.ingenieur",
 "JD-SPC-03 saisi",
 "1. Ouvrir /spc/gage-rr\n2. Lancer le calcul\n3. Lire répétabilité, "
 "reproductibilité, %R&R et ndc",
 "JD-SPC-03",
 "La décomposition est complète, le verdict (acceptable / limite / inacceptable) "
 "est posé selon les seuils AIAG, et le ndc est un entier.",
 "ADR 0097"),

("SPC", "Gage R&R", "Une étude incomplète n'est pas calculée en silence",
 "Négatif", "Majeur", "demo.ingenieur",
 "Étude avec un essai manquant",
 "1. Retirer une mesure\n2. Lancer le calcul",
 "JD-SPC-03",
 "Le calcul est refusé en nommant la cellule manquante. Aucune valeur n'est "
 "imputée à la place.",
 "ADR 0097"),

# ======================================================================
# 5. APQP / PRODUIT
# ======================================================================
("APQP", "Projets", "Un projet APQP exige ses livrables par phase",
 "Fonctionnel", "Majeur", "demo.ingenieur",
 "JD-PRD-01 créé",
 "1. Ouvrir /apqp, créer un projet sur CP-55\n2. Tenter de clore la phase 1 "
 "sans livrable\n3. Déposer les livrables\n4. Clore la phase",
 "JD-PRD-01",
 "La clôture est refusée tant qu'un livrable obligatoire manque, et le message "
 "nomme lequel. Une fois déposés, la phase se clôt et la suivante s'ouvre.",
 "CLAUDE.md §5.2 IATF"),

("APQP", "Produits", "Une caractéristique spéciale se propage à l'AMDEC et au plan",
 "Intégration", "Majeur", "demo.ingenieur",
 "JD-PRD-01 avec 3 caractéristiques spéciales",
 "1. Ouvrir /products, ouvrir CP-55\n2. Marquer une caractéristique comme spéciale\n"
 "3. Ouvrir l'AMDEC puis le plan de surveillance",
 "JD-PRD-01",
 "La caractéristique apparaît marquée dans l'AMDEC et dans le plan de "
 "surveillance, sans ressaisie, avec le même symbole.",
 "ADR 0096, CLAUDE.md §3.6"),

("APQP", "Plan de surveillance", "Le sceau du plan détecte une modification",
 "Sécurité", "Majeur", "demo.ingenieur",
 "Plan de surveillance scellé",
 "1. Sceller le plan\n2. Modifier une ligne\n3. Vérifier le sceau",
 "JD-PRD-01",
 "La vérification signale que le plan a changé depuis le scellement, et "
 "indique la version scellée d'origine.",
 "Migration V115 — sceau du plan"),

# ======================================================================
# 6. NON-CONFORMITÉ, CAPA, FOURNISSEURS
# ======================================================================
("Non-conformité", "NC interne", "Déclarer une NC depuis le terrain avec photo et géolocalisation",
 "Fonctionnel", "Bloquant", "demo.demandeur",
 "—",
 "1. Ouvrir /nc/interne sur mobile\n2. Déclarer la fuite sur P-204\n"
 "3. Joindre une photo\n4. Choisir l'équipement\n5. Enregistrer",
 "JD-EQP-01",
 "La NC est créée avec un numéro, l'équipement est choisi dans le référentiel "
 "(jamais en texte libre), et le déclarant s'affiche par son nom.",
 "ADR 0092 — personne jamais UUID"),

("Non-conformité", "NC interne", "Une NC rejetée garde le motif et reste consultable",
 "Fonctionnel", "Majeur", "demo.qualite",
 "Une NC en attente de qualification",
 "1. Ouvrir la NC\n2. La rejeter avec un motif\n3. La rouvrir depuis la liste",
 "JD-NC-01",
 "Le motif est obligatoire, conservé, affiché avec son auteur et sa date. "
 "La NC rejetée reste lisible et n'est pas effacée.",
 "Migration V128"),

("Non-conformité", "NC externe", "Une réclamation client devient une NC traçable",
 "Fonctionnel", "Majeur", "demo.qualite",
 "JD-NC-02",
 "1. Ouvrir /nc/externe\n2. Ouvrir la réclamation\n3. La qualifier\n"
 "4. Ouvrir la CAPA depuis la réclamation",
 "JD-NC-02",
 "Le lien réclamation → NC → CAPA est conservé dans les deux sens, et la fiche "
 "client montre l'état d'avancement.",
 "CLAUDE.md §4.9"),

("Non-conformité", "Fournisseurs", "La scorecard fournisseur reflète ses NC",
 "Fonctionnel", "Majeur", "demo.qualite",
 "JD-FRS-01 avec 2 NC ouvertes",
 "1. Ouvrir /suppliers, ouvrir Métallurgie du Ponant\n2. Lire le score\n"
 "3. Clore une NC\n4. Relire le score",
 "JD-FRS-01",
 "Le score se recalcule après la clôture et la variation est explicable "
 "(quel critère a bougé, de combien).",
 "CLAUDE.md §4.6"),

("Non-conformité", "CAPA", "Une CAPA naît d'une NC et garde le fil jusqu'à la preuve",
 "Intégration", "Bloquant", "demo.qualite",
 "JD-NC-01 ouverte",
 "1. Ouvrir NC-2026-0006\n2. Créer une CAPA depuis la NC\n3. Poser le critère "
 "d'efficacité\n4. Ajouter 2 actions avec responsables\n5. Les exécuter",
 "JD-NC-01, JD-CAPA-01",
 "La CAPA porte la référence de la NC, le critère est enregistré avant toute "
 "mesure, et la NC montre que sa CAPA avance.",
 "ADR 0088, ADR 0108"),

("Non-conformité", "CAPA", "Le verdict d'efficacité offre les trois états",
 "Fonctionnel", "Bloquant", "demo.qualite",
 "JD-CAPA-01, actions faites, critère posé",
 "1. Ouvrir /capa/efficacite\n2. Choisir la CAPA dans le plan de travail\n"
 "3. Saisir le relevé\n4. Rendre un verdict « partiellement efficace »",
 "JD-CAPA-01",
 "Les trois verdicts sont proposés. Le panneau annonce avant le clic ce que "
 "le verdict choisi déclenche. « Partiellement efficace » n'autorise pas la "
 "clôture et appelle une action complémentaire.",
 "ADR 0108"),

("Non-conformité", "CAPA", "Le critère d'efficacité est gelé dès le premier verdict",
 "Négatif", "Majeur", "demo.qualite",
 "Une CAPA avec un verdict déjà rendu",
 "1. Ouvrir la fiche CAPA\n2. Cliquer sur « Modifier »\n3. Tenter de changer le critère",
 "JD-CAPA-01",
 "Le champ est verrouillé et le motif est affiché. L'historique montre le "
 "critère exact contre lequel chaque verdict passé a été rendu.",
 "ADR 0108"),

("Non-conformité", "CAPA", "Consigner un échec reste toujours possible",
 "Fonctionnel", "Majeur", "demo.qualite",
 "Une CAPA avec une action non terminée",
 "1. Ouvrir le plan de travail des évaluations\n2. Rendre « non efficace »",
 "JD-CAPA-02",
 "Le verdict « non efficace » est accepté même si les garde-fous de clôture ne "
 "sont pas levés : consigner un échec est un constat de fait.",
 "ADR 0108"),

("Non-conformité", "CAPA", "Le plan de travail ne montre que mes dossiers sans « voir tout »",
 "Droits", "Bloquant", "demo.resp-service",
 "Compte sans la permission CAPA_VIEW_ALL, concerné par une seule CAPA",
 "1. Ouvrir /capa/efficacite\n2. Compter les lignes\n3. Appeler directement "
 "l'historique d'une CAPA qui ne le concerne pas",
 "JD-CAPA-01, JD-CAPA-02",
 "Seule la CAPA qui le concerne apparaît. L'appel direct sur une autre répond "
 "404, pas une liste vide.",
 "ADR 0081, ADR 0108 §6"),

("Non-conformité", "CAPA", "Le taux de récidive se mesure après la clôture",
 "Fonctionnel", "Majeur", "demo.qualite",
 "JD-CAPA-02 clôturée avec récidive",
 "1. Ouvrir /capa/efficacite\n2. Onglet « Mesure par récidive »\n"
 "3. Lire le taux et l'écart avec le verdict",
 "JD-CAPA-02, JD-NC-03",
 "Le taux compte les écarts revenus après la clôture. L'écart entre le verdict "
 "rendu et la récidive constatée est lisible : c'est l'intérêt de l'onglet.",
 "ADR 0108"),
]
