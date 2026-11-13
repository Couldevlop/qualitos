# -*- coding: utf-8 -*-
"""Enregistre un lot d'écarts réalistes, et la chaîne qui pend à chacun.

POURQUOI CE SCRIPT. Une instance vide ne se démontre pas, et une instance
peuplée de « NC test 1, NC test 2 » ne se démontre pas davantage : on ne sait
pas si un écran est pauvre ou si ce sont les données qui le sont. Ce lot vise
l'inverse — des constats qu'un responsable qualité reconnaîtrait, répartis sur
les sept catégories et leurs natures, avec les analyses, les CAPA et les
clôtures qui vont avec.

CE QU'IL ÉCRIT. Cinquante écarts, et pour chacun ce que son avancement
suppose :

  * `ouvert`  — le constat seul, tel qu'il tombe du terrain ;
  * `analyse` — plus la mise en analyse, sa cause pressentie, et selon les cas
                une analyse 5 Pourquoi ou un Ishikawa renseigné ;
  * `capa`    — plus le dossier CAPA et son action unique (ADR 0088) ;
  * `resolu`  — plus l'action menée jusqu'au bout et l'écart résolu ;
  * `clos`    — plus l'efficacité jugée, la CAPA close, et l'écart clos.

Chaque écart porte son étape dans la table ci-dessous : aucun traitement par
numéro de ligne, qui se décalerait au premier ajout.

PAR L'API, ET NON EN BASE. Un `INSERT` direct produirait des lignes que
l'application n'aurait jamais acceptées : statuts incohérents, références
absentes, aucun événement d'audit, aucune CAPA déduite. Ces écarts-ci passent
par les mêmes portes qu'un utilisateur, donc par les mêmes refus — et la sortie
du script vaut relevé de fonctionnement autant que jeu de données.

REJOUABLE. Rien n'est écrit en dur qui soit unique : les références d'écart
sont attribuées par le serveur. Un second passage ajoute un second lot ; pour
repartir de zéro, videz d'abord (`scripts/vider-donnees-metier.sql`).

Usage (moteur démarré, Keycloak debout) :

    python scripts/lot-nc.py
    python scripts/lot-nc.py --api https://preprod.exemple --utilisateur demo
    python scripts/lot-nc.py --combien 10        # un essai court avant le vrai lot
"""
import argparse
import os
import sys
import time
from datetime import date, datetime, timedelta, timezone

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from qualitos_api import Client, jeton, pdf_minimal   # noqa: E402


def maintenant(decalage_jours=0):
    return (datetime.now(timezone.utc) + timedelta(days=decalage_jours)) \
        .strftime('%Y-%m-%dT%H:%M:%SZ')


def jour(decalage_jours=0):
    return (date.today() + timedelta(days=decalage_jours)).isoformat()


# ---------------------------------------------------------------------------
# LE LOT.
#
# Les natures (`code`) viennent du catalogue de départ de la plateforme
# (`NcSubcategoryDefaults`). Un client qui a remplacé le sien par le sien verra
# ces codes refusés en 400 — c'est le contrôle qui fonctionne, et le refus dit
# lequel. Les dates sont exprimées en JOURS AVANT AUJOURD'HUI : le lot vieillit
# avec l'instance au lieu de se figer sur un mois de 2026.
#
# Chaque entrée :
#   categorie, code, severite, origine, zone, titre, constat, il_y_a, etape
# et, pour celles qui vont jusqu'à la CAPA, la cause pressentie, l'intitulé de
# l'action et le critère d'efficacité.
# ---------------------------------------------------------------------------

def e(categorie, code, severite, origine, zone, titre, constat, il_y_a, etape,
      cause=None, action=None, critere=None, analyse=None):
    return dict(categorie=categorie, code=code, severite=severite, origine=origine,
                zone=zone, titre=titre, constat=constat, il_y_a=il_y_a, etape=etape,
                cause=cause, action=action, critere=critere, analyse=analyse)


ECARTS = [
    # --- PRODUIT (12) ------------------------------------------------------
    e('PRODUCT', 'PRD-DIM', 'MAJOR', 'INTERNAL', 'Contrôle final',
      'Jeu radial hors tolérance sur arbre de sortie',
      'Au contrôle final du lot L-2291, jeu radial relevé à 0,12 mm pour 0,05 mm admis '
      'sur 4 pièces de 20. Les 16 autres sont dans la tolérance.',
      3, 'clos',
      cause='Usure du roulement de broche du tour 3',
      action='Remplacer le roulement de broche et passer la broche en maintenance préventive',
      critere='Zéro dépassement de 0,05 mm sur les trois lots suivants',
      analyse='5w'),
    e('PRODUCT', 'PRD-ASP', 'MINOR', 'INTERNAL', 'Peinture',
      'Coulures de peinture sur carters série 400',
      'Coulures visibles sur la face supérieure de 7 carters. Retouche possible, aucun '
      'impact fonctionnel.',
      9, 'capa',
      cause='Viscosité de la peinture non contrôlée en début de poste',
      action='Ajouter le contrôle de viscosité à la fiche de démarrage de la cabine',
      critere='Moins de 2 retouches pour 100 carters sur deux mois'),
    e('PRODUCT', 'PRD-FON', 'CRITICAL', 'EXTERNAL', None,
      'Réclamation client : sifflement en charge au-delà de 80 bar',
      'Le client signale un sifflement sur trois pompes du lot L-2288, au-delà de 80 bar. '
      'Retour des trois pompes demandé pour expertise.',
      6, 'capa',
      cause='Portée de joint marquée au montage',
      action='Revoir l outillage de montage du joint et former les deux monteurs',
      critere='Aucune réclamation de même nature sur six mois'),
    e('PRODUCT', 'PRD-MAT', 'MAJOR', 'INTERNAL', 'Réception',
      'Dureté acier en limite basse sur coulée C-7741',
      'Certificat matière conforme mais dureté relevée à 182 HB pour 180-220 attendus. '
      'Lot bloqué en attente de décision.',
      14, 'analyse',
      cause='Dispersion de la coulée chez le fournisseur'),
    e('PRODUCT', 'PRD-ASS', 'MINOR', 'INTERNAL', 'Montage ligne 2',
      'Couple de serrage non atteint sur 3 brides',
      'Contrôle par échantillonnage : 3 brides à 42 N·m pour 55 N·m prescrits.',
      21, 'capa',
      cause='Clé dynamométrique hors validité d étalonnage',
      action='Étalonner les quatre clés de la ligne et poser un rappel à 6 mois',
      critere='100 % des clés dans leur période de validité à chaque audit'),
    e('PRODUCT', 'PRD-EMB', 'MINOR', 'EXTERNAL', 'Expédition',
      'Étiquette de lot absente sur deux palettes',
      'Le client signale deux palettes reçues sans étiquette de lot ; la traçabilité a dû '
      'être reconstituée à partir du bon de livraison.',
      17, 'analyse',
      cause='Imprimante d étiquettes en défaut non signalé'),
    e('PRODUCT', 'PRD-QTE', 'MINOR', 'EXTERNAL', 'Expédition',
      'Manquant de 12 pièces sur commande CM-4417',
      'Le client a reçu 288 pièces pour 300 commandées. Écart confirmé par recomptage du '
      'reliquat en stock.',
      25, 'ouvert'),
    e('PRODUCT', 'PRD-CTA', 'CRITICAL', 'INTERNAL', 'Usinage',
      'Copeau métallique retrouvé dans un corps de pompe',
      'Copeau de 4 mm dans le canal d aspiration, détecté au rinçage final. Lot entier mis '
      'en quarantaine.',
      11, 'capa',
      cause='Rinçage insuffisant après perçage du canal',
      action='Porter la durée de rinçage à 90 s et vérifier par endoscope sur 100 % du lot',
      critere='Zéro corps étranger détecté sur dix lots consécutifs'),
    e('PRODUCT', 'PRD-PER', 'MAJOR', 'INTERNAL', 'Magasin consommables',
      'Colle structurale périmée encore en rayon',
      'Deux cartouches dont la date limite est dépassée de 5 semaines, rangées avec les '
      'cartouches valides.',
      30, 'resolu',
      cause='Pas de contrôle de péremption au rangement',
      action='Mettre en place un contrôle mensuel des dates en magasin consommables',
      critere='Aucun consommable périmé trouvé lors des trois audits 5S suivants'),
    e('PRODUCT', 'PRD-SRV', 'MINOR', 'EXTERNAL', None,
      'Intervention sur site reportée sans prévenir le client',
      'Le client signale une intervention de maintenance reportée de 48 h sans information '
      'préalable de sa part.',
      34, 'ouvert'),
    e('PRODUCT', 'PRD-LOG', 'MAJOR', 'EXTERNAL', None,
      'Export CSV tronqué au-delà de 10 000 lignes',
      'Le client signale que l export du journal de production s arrête à 10 000 lignes '
      'sans message d erreur.',
      40, 'analyse',
      cause='Limite de pagination non remontée à l interface'),
    e('PRODUCT', 'PRD-FON', 'MINOR', 'INTERNAL', 'Banc d essai',
      'Débit inférieur de 4 % à la courbe garantie',
      'Trois pompes du lot L-2301 mesurées à 4 % sous la courbe au point nominal. Reste '
      'dans la tolérance commerciale mais la tendance est nouvelle.',
      46, 'ouvert'),

    # --- PROCESSUS (11) ----------------------------------------------------
    e('PROCESS', 'PRC-DER', 'MAJOR', 'INTERNAL', 'Traitement thermique',
      'Dérive de température du four 2 sur deux cycles',
      'Relevé à 862 °C pour 850 ± 5 °C sur les cycles 118 et 119. La carte de contrôle '
      'montre sept points consécutifs au-dessus de la moyenne.',
      5, 'capa',
      cause='Sonde de régulation vieillissante',
      action='Remplacer la sonde du four 2 et vérifier la régulation sur trois cycles',
      critere='Cp supérieur à 1,33 sur les vingt cycles suivants',
      analyse='ishikawa'),
    e('PROCESS', 'PRC-PRO', 'MAJOR', 'INTERNAL', 'Ligne 1',
      'Mode opératoire de changement de série non suivi',
      'Observation terrain : trois étapes sur onze sautées lors du changement de série, '
      'dont le contrôle de première pièce.',
      8, 'capa',
      cause='Mode opératoire jugé trop long par les opérateurs',
      action='Réécrire le mode opératoire avec les opérateurs et le valider en poste',
      critere='Audit de poste conforme sur trois changements de série consécutifs',
      analyse='ishikawa'),
    e('PROCESS', 'PRC-EQP', 'CRITICAL', 'INTERNAL', 'Ligne 2',
      'Arrêt de la ligne 2 : défaut variateur',
      'Arrêt de 4 h 20 sur défaut du variateur de l axe principal. Production reportée sur '
      'la ligne 3.',
      12, 'capa',
      cause='Ventilation du variateur encrassée',
      action='Intégrer le nettoyage des ventilations au plan de maintenance trimestriel',
      critere='Aucun arrêt pour échauffement variateur sur six mois'),
    e('PROCESS', 'PRC-ETA', 'MAJOR', 'INTERNAL', 'Métrologie',
      'Pied à coulisse utilisé hors période d étalonnage',
      'Instrument M-114 utilisé en contrôle réception alors que son étalonnage était échu '
      'depuis 3 semaines.',
      16, 'resolu',
      cause='Alerte d échéance non relayée au poste',
      action='Brancher les alertes d échéance métrologie sur la messagerie des chefs de poste',
      critere='Zéro instrument hors validité lors des deux audits suivants'),
    e('PROCESS', 'PRC-MSA', 'MINOR', 'INTERNAL', 'Métrologie',
      'Gage R&R à 28 % sur le contrôle de planéité',
      'Étude R&R : 28 % de la tolérance, au-delà des 10 % visés. Le moyen de mesure ne '
      'discrimine pas assez.',
      23, 'analyse',
      cause='Appui de la pièce non reproductible sur le marbre'),
    e('PROCESS', 'PRC-MNT', 'MINOR', 'INTERNAL', 'Ligne 3',
      'Maintenance préventive de juillet non réalisée',
      'Deux gammes préventives sur cinq non réalisées en juillet, sans report enregistré.',
      28, 'ouvert'),
    e('PROCESS', 'PRC-SAI', 'MINOR', 'INTERNAL', 'Saisie production',
      'Quantités produites saisies à la fin de la semaine',
      'Les quantités de trois postes ont été saisies le vendredi pour toute la semaine, de '
      'mémoire.',
      33, 'ouvert'),
    e('PROCESS', 'PRC-TRA', 'MAJOR', 'INTERNAL', 'Fonderie',
      'Traçabilité du lot amont perdue sur deux bacs',
      'Deux bacs de pièces brutes sans identification de coulée en entrée d usinage.',
      38, 'analyse',
      cause='Étiquettes de bac décollées par le bain de lavage'),
    e('PROCESS', 'PRC-HYG', 'MINOR', 'INTERNAL', 'Atelier conditionnement A',
      'Zone de conditionnement encombrée en fin de poste',
      'Audit 5S : cartons et palettes vides stockés dans l allée de circulation.',
      44, 'ouvert'),
    e('PROCESS', 'PRC-DEL', 'MAJOR', 'EXTERNAL', None,
      'Retard de livraison de 6 jours sur commande CM-4392',
      'Le client signale une livraison avec 6 jours de retard sans information préalable.',
      50, 'analyse',
      cause='Reprise non planifiée après non-conformité interne'),
    e('PROCESS', 'PRC-CMP', 'MINOR', 'INTERNAL', 'Ligne 1',
      'Poste tenu par un opérateur non habilité',
      'Un opérateur a tenu le poste de contrôle final sans habilitation enregistrée, en '
      'remplacement d un absent.',
      55, 'capa',
      cause='Matrice de compétences non consultée lors des remplacements',
      action='Afficher la matrice de polyvalence au poste et la vérifier à chaque remplacement',
      critere='Aucun poste tenu sans habilitation lors des audits de poste du trimestre'),

    # --- DOCUMENTATION (6) -------------------------------------------------
    e('DOCUMENTATION', 'DOC-OBS', 'MAJOR', 'INTERNAL', 'Ligne 2',
      'Version périmée du plan au poste de montage',
      'Le plan P-2219 indice B est affiché au poste alors que l indice D est en vigueur '
      'depuis quatre mois.',
      7, 'capa',
      cause='Retrait des indices périmés non organisé',
      action='Mettre en place le retrait des indices périmés à chaque diffusion',
      critere='Aucun document périmé trouvé lors des trois audits documentaires suivants'),
    e('DOCUMENTATION', 'DOC-SIG', 'MINOR', 'INTERNAL', 'Qualité',
      'Rapport de contrôle non signé',
      'Trois rapports de contrôle final du mois sans signature du contrôleur.',
      19, 'analyse',
      cause='Signature laissée en fin de journée et oubliée'),
    e('DOCUMENTATION', 'DOC-INC', 'MINOR', 'INTERNAL', 'Méthodes',
      'Instruction de travail incomplète : critère d acceptation absent',
      'L instruction IT-308 décrit le geste mais ne dit pas à partir de quand la pièce est '
      'refusée.',
      27, 'ouvert'),
    e('DOCUMENTATION', 'DOC-ARC', 'MINOR', 'INTERNAL', 'Archives',
      'Enregistrements de 2024 non archivés',
      'Les enregistrements de contrôle de 2024 sont restés en classeur courant, hors du '
      'plan d archivage.',
      36, 'ouvert'),
    e('DOCUMENTATION', 'DOC-MAN', 'MAJOR', 'EXTERNAL', None,
      'Notice utilisateur non à jour après évolution produit',
      'Remontée du service après-vente : la notice décrit un réglage qui n existe plus '
      'depuis l évolution de juin.',
      42, 'analyse',
      cause='Évolution produit non reliée à la documentation client'),
    e('DOCUMENTATION', 'DOC-DIF', 'MINOR', 'INTERNAL', 'Qualité',
      'Procédure révisée non diffusée aux deux sites',
      'La révision de la procédure PR-14 n a été diffusée qu au site principal.',
      48, 'ouvert'),

    # --- FOURNISSEUR (7) ---------------------------------------------------
    e('SUPPLIER', 'FOU-REC', 'MAJOR', 'INTERNAL', 'Réception',
      'Lot fournisseur refusé : 6 % de pièces hors cote',
      'Contrôle réception du lot F-8821 : 6 % hors cote sur la portée d étanchéité. Lot '
      'refusé en totalité.',
      4, 'capa',
      cause='Changement d outil non déclaré chez le fournisseur',
      action='Exiger la déclaration des changements d outil et auditer le fournisseur',
      critere='Zéro lot refusé sur les six livraisons suivantes'),
    e('SUPPLIER', 'FOU-CER', 'MAJOR', 'INTERNAL', 'Réception',
      'Certificat matière absent sur deux livraisons',
      'Deux livraisons du fournisseur FS-04 arrivées sans certificat 3.1.',
      13, 'analyse',
      cause='Certificat transmis par messagerie et non joint au colis'),
    e('SUPPLIER', 'FOU-RET', 'MINOR', 'INTERNAL', None,
      'Livraison fournisseur avec 9 jours de retard',
      'Le fournisseur FS-11 a livré avec 9 jours de retard, provoquant un report de '
      'lancement.',
      20, 'ouvert'),
    e('SUPPLIER', 'FOU-QTE', 'MINOR', 'INTERNAL', 'Réception',
      'Quantité livrée inférieure au bon de commande',
      'Livraison de 480 pièces pour 500 commandées, sans reliquat annoncé.',
      26, 'ouvert'),
    e('SUPPLIER', 'FOU-MOD', 'CRITICAL', 'INTERNAL', 'Réception',
      'Modification produit non notifiée par le fournisseur',
      'Le revêtement de surface a changé de fournisseur sans notification ; détecté au '
      'contrôle d aspect.',
      31, 'capa',
      cause='Clause de notification absente du contrat',
      action='Ajouter la clause de notification de modification aux contrats des 12 fournisseurs critiques',
      critere='100 % des contrats critiques portant la clause d ici trois mois'),
    e('SUPPLIER', 'FOU-8D', 'MINOR', 'INTERNAL', None,
      'Réponse 8D du fournisseur attendue depuis 5 semaines',
      'Le 8D demandé au fournisseur FS-07 sur le lot refusé de septembre n a toujours pas '
      'été transmis, cinq semaines après la demande.',
      39, 'analyse',
      cause='Aucune relance organisée sur les 8D fournisseurs'),
    e('SUPPLIER', 'FOU-STR', 'MAJOR', 'INTERNAL', None,
      'Sous-traitant intervenant sans habilitation à jour',
      'Un sous-traitant de maintenance est intervenu avec une habilitation électrique '
      'expirée depuis deux mois.',
      45, 'capa',
      cause='Habilitations des sous-traitants non vérifiées à l accueil',
      action='Vérifier les habilitations à l accueil et refuser l accès sans justificatif valide',
      critere='Zéro intervention sans habilitation valide sur le trimestre'),

    # --- SÉCURITÉ (6) ------------------------------------------------------
    e('SAFETY', 'SEC-EPI', 'MAJOR', 'INTERNAL', 'Atelier mécanique',
      'Port des lunettes non respecté en zone meulage',
      'Trois opérateurs observés sans lunettes de protection en zone de meulage.',
      2, 'capa',
      cause='Lunettes rayées et non remplacées',
      action='Remplacer le stock de lunettes et poser un distributeur à l entrée de la zone',
      critere='100 % de port constaté lors des quatre audits sécurité du trimestre'),
    e('SAFETY', 'SEC-MAC', 'CRITICAL', 'INTERNAL', 'Presse 4',
      'Protecteur mobile de la presse 4 neutralisé',
      'Le contacteur du protecteur mobile a été ponté. La presse a été consignée '
      'immédiatement.',
      10, 'clos',
      cause='Protecteur gênant le réglage, contourné pour gagner du temps',
      action='Revoir le protecteur avec le fabricant pour permettre le réglage sans neutralisation',
      critere='Aucune neutralisation constatée sur six mois'),
    e('SAFETY', 'SEC-ACC', 'MAJOR', 'INTERNAL', 'Expédition',
      'Accident sans arrêt : écrasement de doigt au quai',
      'Écrasement du majeur gauche entre une palette et le montant du quai. Soins sur '
      'place, pas d arrêt de travail.',
      18, 'analyse',
      cause='Palette manipulée à la main faute de transpalette disponible'),
    e('SAFETY', 'SEC-PRQ', 'MINOR', 'INTERNAL', 'Atelier mécanique',
      'Presque-accident : chute d un bac depuis un rack',
      'Un bac de 15 kg est tombé du troisième niveau d un rack, sans personne en dessous.',
      24, 'ouvert'),
    e('SAFETY', 'SEC-CHM', 'MAJOR', 'INTERNAL', 'Peinture',
      'Fiche de données de sécurité absente pour un solvant',
      'Un solvant en cabine sans FDS disponible au poste.',
      32, 'ouvert'),
    e('SAFETY', 'SEC-INC', 'MAJOR', 'INTERNAL', 'Magasin',
      'Accès à un extincteur obstrué par des palettes',
      'Deux palettes stockées devant l extincteur E-12, accès impossible.',
      41, 'analyse',
      cause='Zone de stockage saturée en fin de mois'),

    # --- ENVIRONNEMENT (4) -------------------------------------------------
    e('ENVIRONMENT', 'ENV-DEC', 'MINOR', 'INTERNAL', 'Déchets',
      'Tri des déchets non respecté : souillés avec les inertes',
      'Chiffons souillés retrouvés dans la benne des déchets inertes.',
      15, 'capa',
      cause='Bac dédié absent au poste de nettoyage',
      action='Installer un bac dédié au poste et rappeler les consignes de tri',
      critere='Aucun mélange constaté lors des trois contrôles mensuels suivants'),
    e('ENVIRONMENT', 'ENV-EAU', 'MAJOR', 'INTERNAL', 'Traitement de surface',
      'pH du rejet en limite haute',
      'pH relevé à 8,9 pour une limite à 9,0 sur le rejet du traitement de surface.',
      22, 'analyse',
      cause='Neutralisation insuffisante en fin de bain'),
    e('ENVIRONMENT', 'ENV-DEV', 'MINOR', 'INTERNAL', 'Usinage',
      'Fuite d huile de coupe sous la machine 7',
      'Flaque d huile de coupe sous la machine 7, absorbant posé en urgence.',
      29, 'ouvert'),
    e('ENVIRONMENT', 'ENV-NRG', 'MINOR', 'INTERNAL', 'Atelier',
      'Compresseur laissé en marche le week-end',
      'Le compresseur principal est resté en marche tout le week-end, sans production.',
      37, 'ouvert'),

    # --- AUTRE (4) ---------------------------------------------------------
    e('OTHER', 'AUT-AUD', 'MAJOR', 'EXTERNAL', None,
      'Écart d audit de certification : revue de direction incomplète',
      'L auditeur relève que la revue de direction ne traite pas les résultats des audits '
      'internes, exigés au §9.3.2 c).',
      1, 'capa',
      cause='Trame de revue de direction incomplète',
      action='Compléter la trame de revue de direction avec les entrées du §9.3.2',
      critere='Revue de direction suivante conforme aux onze entrées exigées'),
    e('OTHER', 'AUT-REC', 'MAJOR', 'EXTERNAL', None,
      'Réclamation client sur la réponse apportée à une précédente réclamation',
      'Le client juge la réponse à sa réclamation de mai non argumentée et demande une '
      'analyse de cause.',
      35, 'analyse',
      cause='Réponse rédigée sans analyse de cause documentée'),
    e('OTHER', 'AUT-REG', 'MAJOR', 'INTERNAL', None,
      'Veille réglementaire non mise à jour depuis 8 mois',
      'Le registre de veille réglementaire n a pas été mis à jour depuis huit mois.',
      43, 'ouvert'),
    e('OTHER', 'AUT-OPE', 'MINOR', 'INTERNAL', 'Planification',
      'Plan de charge non diffusé aux équipes',
      'Le plan de charge hebdomadaire n a pas été diffusé deux semaines de suite.',
      52, 'ouvert'),
]


# ---------------------------------------------------------------------------
# L'équipe. Une personne se choisit dans l'annuaire, jamais en texte libre
# (ADR 0092) : on lit donc l'annuaire réel du client, et on s'y limite. À
# défaut — annuaire injoignable, un seul membre — le porteur du jeton se nomme
# lui-même, ce qui reste vrai.
# ---------------------------------------------------------------------------

def equipe_disponible(c):
    page = c.lit('/api/v1/directory/users?size=50', 'annuaire du client',
                 silencieux=True)
    membres = []
    for u in (page or {}).get('content', []):
        compte = u.get('accountId')
        nom = u.get('displayName') or u.get('email')
        if compte and nom:
            membres.append({'accountId': compte, 'displayName': nom})
    if not membres:
        print('  (annuaire indisponible : les écarts seront pris en charge par %s seul)'
              % c.nom)
        membres = [{'accountId': c.sujet, 'displayName': c.nom}]
    else:
        print('  %d membres lus dans l annuaire' % len(membres))
    return membres


def equipe_de(membres, rang):
    """Deux à trois personnes, qui tournent — le premier nommé pilote l'écart."""
    if len(membres) == 1:
        return membres
    taille = 2 + (rang % 2)
    return [membres[(rang + i) % len(membres)] for i in range(min(taille, len(membres)))]


# ---------------------------------------------------------------------------
# La chaîne, étape par étape. Chaque fonction ne fait qu'un pas et rend ce
# qu'elle a obtenu : un pas qui échoue arrête la chaîne de CET écart, et pas
# celle des quarante-neuf autres.
# ---------------------------------------------------------------------------

def declare(c, fiche, membres, rang):
    corps = {
        'title': fiche['titre'],
        'description': fiche['constat'],
        'category': fiche['categorie'],
        'subcategoryCode': fiche['code'],
        'severity': fiche['severite'],
        'origin': fiche['origine'],
        'detectedAt': maintenant(-fiche['il_y_a']),
        'teamMembers': equipe_de(membres, rang),
    }
    if fiche['zone']:
        corps['zone'] = fiche['zone']
    return c.poste('/api/v1/nc', corps, 'écart %02d — %s' % (rang + 1, fiche['code']))


def figure_d_analyse(fiche, rang):
    """La figure qui soutient la cause : 5 Pourquoi, Ishikawa, ou aucune.

    <p>Deux écarts sur trois en reçoivent une, et c'est volontaire : dans une
    instance où les cinquante portent un diagramme complet, on ne distingue
    plus l'écart instruit de celui qu'on a classé à la main. Une fiche peut
    imposer la sienne quand le constat l'appelle — un Ishikawa pour une dérive
    machine, un 5 Pourquoi pour un défaut qui se remonte en ligne droite.
    """
    if fiche['analyse']:
        return fiche['analyse']
    return {0: '5w', 1: 'ishikawa'}.get(rang % 3)


def analyse_de_cause(c, nc, fiche, rang):
    """La mise en analyse, et la figure qui la soutient."""
    c.poste('/api/v1/nc/%s/start-analysis' % nc['id'],
            {'rootCause': fiche['cause']}, '  mise en analyse')

    figure = figure_d_analyse(fiche, rang)
    if figure == '5w':
        analyse = c.poste('/api/v1/five-whys',
                          {'ncId': nc['id'], 'problem': fiche['titre']},
                          '  analyse 5 Pourquoi')
        if analyse:
            # Cinq pourquoi, et le cinquième EST la cause retenue : une analyse
            # dont la dernière marche ne rejoint pas la cause déclarée est une
            # analyse qu'on a remplie après coup.
            for reponse in [
                'Le défaut n a pas été arrêté au poste où il se produit.',
                'Le contrôle à ce poste ne couvre pas ce point.',
                'Le point n a pas été jugé critique lors de l écriture de la gamme.',
                'L analyse de risque du poste n a pas été revue depuis sa création.',
                fiche['cause'],
            ]:
                c.poste('/api/v1/five-whys/%s/steps' % analyse['id'],
                        {'answer': reponse}, '  pourquoi')
            c.met('/api/v1/five-whys/%s/root-cause' % analyse['id'],
                  {'rootCause': fiche['cause']}, '  cause racine retenue')
    elif figure == 'ishikawa':
        diagramme = c.poste('/api/v1/ishikawa/diagrams',
                            {'problemStatement': fiche['titre'],
                             'ownerId': c.sujet, 'ncId': nc['id']},
                            '  diagramme Ishikawa')
        if diagramme:
            # Les 6M, nommées en anglais dans l'énumération du domaine. Cinq
            # branches sur six : un Ishikawa complet à six branches sur chaque
            # écart serait un formulaire rempli, pas une analyse.
            for categorie, cause in [
                ('MACHINES', fiche['cause']),
                ('METHODS', 'Fréquence de contrôle trop espacée'),
                ('MANPOWER', 'Geste appris sur le tas, sans qualification'),
                ('MATERIALS', 'Variabilité du lot amont'),
                ('MEASUREMENTS', 'Moyen de mesure en limite de capabilité'),
            ]:
                c.poste('/api/v1/ishikawa/diagrams/%s/causes' % diagramme['id'],
                        {'category': categorie, 'label': cause},
                        '  cause %s' % categorie.lower())


def ouvre_capa(c, nc, fiche, membres, rang):
    """La CAPA de l'écart, avec son action unique (ADR 0088)."""
    pilote = equipe_de(membres, rang)[0]
    return c.poste('/api/v1/nc/%s/escalate-capa' % nc['id'], {
        'title': fiche['action'],
        'description': 'Traitement de l écart %s. Cause retenue : %s.'
                       % (nc.get('reference', ''), fiche['cause']),
        'type': 'CORRECTIVE',
        'actionType': 'CORRECTIVE',
        'criticity': 'HIGH' if fiche['severite'] == 'CRITICAL' else 'MEDIUM',
        'ownerId': pilote['accountId'],
        'ownerName': pilote['displayName'],
        # Une échéance D'AVENIR, toujours : le module refuse une date déjà
        # passée, et il a raison — une action qu'on ouvre en retard n'a pas
        # d'échéance, elle a un retard. Les écarts anciens reçoivent donc une
        # échéance proche, les récents une plus lointaine : la pression monte
        # avec l'âge du constat, ce qui est l'ordre naturel.
        'dueDate': jour(max(7, 45 - fiche['il_y_a'] // 2)),
        'verificationRequired': True,
        'verificationAssigneeId': pilote['accountId'],
        'verificationAssigneeName': pilote['displayName'],
        'effectivenessCriterion': fiche['critere'],
    }, '  dossier CAPA')


def mene_action_a_son_terme(c, capa_id):
    """Fait avancer l'action du dossier : à faire → en cours → faite.

    <p>Deux appels, et non un saut direct à DONE : c'est le chemin que suit un
    pilote, et c'est celui qui écrit les deux transitions au journal. Le statut
    du dossier les suit (ADR 0117), et le passe à RESOLVED une fois la dernière
    action faite.
    """
    dossier = c.lit('/api/v1/capa/cases/%s' % capa_id, '  relecture du dossier')
    if not dossier or not dossier.get('actions'):
        return False
    action = dossier['actions'][0]
    for etat in ('IN_PROGRESS', 'DONE'):
        if c.patche('/api/v1/capa/cases/%s/actions/%s' % (capa_id, action['id']),
                    {'status': etat}, '  action → %s' % etat) is None:
            return False
    return True


def depose_les_preuves(c, capa_id, nc, fiche):
    """Les pièces qui montrent que l'action a eu lieu.

    <p>Deux, et pas une : le CONSTAT de ce qui a été fait, et le RELEVÉ qui
    montre que ça a marché. Un dossier qui ne porte que le premier dit qu'on a
    agi, pas que le problème a cessé — et c'est la seconde pièce que
    l'auditeur demande.
    """
    reference = nc.get('reference', '')
    c.depose('/api/v1/capa/cases/%s/evidences' % capa_id,
             'constat-realisation.pdf', 'application/pdf',
             pdf_minimal('Constat de realisation', [
                 'Ecart : %s' % reference,
                 'Action : %s' % fiche['action'],
                 'Cause retenue : %s' % fiche['cause'],
                 '',
                 "L'action decrite ci-dessus a ete realisee et constatee sur site.",
                 'Les elements materiels (photos, bons d intervention) sont',
                 'archives au dossier physique.',
             ]), '  preuve : constat de réalisation')

    c.depose('/api/v1/capa/cases/%s/evidences' % capa_id,
             'releve-de-verification.pdf', 'application/pdf',
             pdf_minimal('Releve de verification', [
                 'Ecart : %s' % reference,
                 'Critere retenu a l ouverture : %s' % fiche['critere'],
                 '',
                 'Periode de surveillance : 30 jours apres realisation.',
                 'Resultat observe : critere atteint, aucune recidive constatee.',
             ]), '  preuve : relevé de vérification')


def resout(c, nc, fiche):
    """Résout l'écart. Il doit être en ACTION_DEFINED : l'escalade l'y a mis."""
    return c.poste('/api/v1/nc/%s/resolve' % nc['id'],
                   {'resolutionNote': 'Action menée : %s. Critère retenu : %s.'
                                      % (fiche['action'], fiche['critere'])},
                   '  écart résolu')


def clot(c, nc, capa_id, fiche):
    """Juge l'efficacité, ce qui clôt la CAPA, puis clôt l'écart.

    <p>L'ORDRE N'EST PAS LIBRE, et c'est le seul endroit du script où il
    compte. Clore la CAPA exige que ses écarts soient réglés — résolu suffit —
    et clore l'écart exige que sa CAPA soit close. Résoudre l'écart d'abord,
    juger ensuite, clore enfin : toute autre séquence se heurte à l'un des deux
    verrous, et c'est tant mieux, ce sont eux qui empêchent de déclarer un
    problème réglé pendant que le constat dit le contraire.
    """
    c.patche('/api/v1/capa/cases/%s/effectiveness' % capa_id, {
        'verdict': 'EFFECTIVE',
        'observed': 'Critère atteint sur la période de surveillance.',
        'comment': 'Vérification réalisée sur pièces et sur relevés : %s.' % fiche['critere'],
    }, '  efficacité jugée')
    return c.poste('/api/v1/nc/%s/close' % nc['id'], None, '  écart clos')


def traite(c, fiche, membres, rang):
    """Mène un écart jusqu'à l'étape que sa fiche annonce, et pas plus loin."""
    nc = declare(c, fiche, membres, rang)
    if not nc or fiche['etape'] == 'ouvert':
        return

    analyse_de_cause(c, nc, fiche, rang)
    if fiche['etape'] == 'analyse':
        return

    capa = ouvre_capa(c, nc, fiche, membres, rang)
    capa_id = (capa or {}).get('capaCaseId')
    if not capa_id or fiche['etape'] == 'capa':
        return

    if not mene_action_a_son_terme(c, capa_id):
        return
    depose_les_preuves(c, capa_id, nc, fiche)
    if not resout(c, nc, fiche):
        return
    if fiche['etape'] == 'resolu':
        return

    clot(c, nc, capa_id, fiche)


def main():
    analyseur = argparse.ArgumentParser(description=__doc__)
    analyseur.add_argument('--api', default='http://localhost:8082')
    analyseur.add_argument('--keycloak', default='http://localhost:8080')
    analyseur.add_argument('--realm', default='qualitos')
    analyseur.add_argument('--client-id', default='qualitos-web')
    analyseur.add_argument('--utilisateur', default='demo')
    analyseur.add_argument('--secret', default='demo')
    analyseur.add_argument('--langue', default='fr')
    analyseur.add_argument('--combien', type=int, default=len(ECARTS),
                           help='Nombre d écarts à enregistrer, dans l ordre de la table. '
                                'Utile pour un essai court avant le lot complet.')
    options = analyseur.parse_args()

    lot = ECARTS[:max(0, options.combien)]
    compte = {}
    for fiche in lot:
        compte[fiche['etape']] = compte.get(fiche['etape'], 0) + 1

    print('QualitOS — lot d écarts')
    print('API %s, utilisateur %s' % (options.api, options.utilisateur))
    print('%d écarts : %s' % (len(lot), ', '.join(
        '%d %s' % (n, etape) for etape, n in sorted(compte.items()))))

    try:
        acces = jeton(options.keycloak, options.realm, options.client_id,
                      options.utilisateur, options.secret)
    except Exception as erreur:                          # noqa: BLE001
        sys.exit('Jeton impossible a obtenir (%s). Keycloak est-il demarre ?' % erreur)

    c = Client(options.api, acces, options.langue)
    membres = equipe_disponible(c)

    debut = time.time()
    for rang, fiche in enumerate(lot):
        traite(c, fiche, membres, rang)

    print('\n--- relevé ---')
    print('%d appels acceptés, %d refus, en %.1f s'
          % (len(c.reussites), len(c.echecs), time.time() - debut))
    if c.echecs:
        print('\nRefus, à lire comme un relevé de fonctionnement :')
        for quoi, code, detail in c.echecs:
            print('  %-46s %s  %s' % (quoi, code, detail))
    return 1 if c.echecs else 0


if __name__ == '__main__':
    sys.exit(main())
