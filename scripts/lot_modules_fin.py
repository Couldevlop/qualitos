# -*- coding: utf-8 -*-
"""La fin du lot : changements, DMAIC et poka-yoke, équipements connectés.

Troisième et dernière part de `lot-modules.py`, découpée pour la même raison
que la deuxième : on cherche « où sont les demandes de changement », pas « où
est la ligne 1400 ».
"""
import datetime as dt


AUJOURD_HUI = dt.date.today()


def jour(decalage):
    return (AUJOURD_HUI + dt.timedelta(days=decalage)).isoformat()


def instant(decalage_jours, heure=9):
    base = dt.datetime.combine(AUJOURD_HUI + dt.timedelta(days=decalage_jours),
                               dt.time(hour=heure))
    return base.strftime('%Y-%m-%dT%H:%M:%SZ')


# ---------------------------------------------------------------------------
# Demandes de changement (MOC).
# ---------------------------------------------------------------------------

CHANGEMENTS = [
    ("Remplacer l'outillage de pose du joint torique", 'PERMANENT', 'Usine Nord',
     "Remplacer la pose à la main par un outil de pose conique fabriqué en interne.",
     "Les joints marqués au montage sont la première cause de fuite en charge ; "
     "quatre réclamations client en six mois.", 30, 'analyse'),
    ("Passer le contrôle de cote à 1 pièce sur 20", 'PERMANENT', 'Usine Nord',
     "Contrôler la cote de portée toutes les 20 pièces plutôt qu'en fin de lot.",
     "La dérive se découvre aujourd'hui quand tout le lot est produit.", 20, 'soumis'),
    ("Dérogation temporaire : fournisseur de bruts de secours", 'TEMPORARY',
     'Usine Nord',
     "Approvisionner les bruts de carter chez Fonderie Lévêque pendant l'arrêt "
     "de Beaulieu.",
     "Arrêt technique de quatre semaines chez le fournisseur habituel.", 10, 'soumis'),
    ("Changer la teinte RAL du carter série 400", 'PERMANENT', 'Usine Nord',
     "Passer du RAL 5015 au RAL 5017 à la demande du client Verneuil.",
     "Demande client formalisée au contrat-cadre 2026.", 60, 'ouvert'),
    ("Déplacer le banc d'essai en zone climatisée", 'PERMANENT', 'Usine Nord',
     "Déplacer le banc d'essai 1 dans le local climatisé voisin.",
     "Les écarts de température faussent les relevés d'étanchéité l'été.", 90, 'ouvert'),
]


IMPACTS = [
    ('QUALITY', 'MEDIUM', "Le plan de surveillance doit être revu sur la caractéristique visée."),
    ('SAFETY', 'LOW', "Aucun risque nouveau au poste."),
    ('PRODUCTION', 'MEDIUM', "Arrêt d'une demi-journée pour la mise en place."),
    ('MAINTENANCE', 'LOW', "Pas de maintenance supplémentaire attendue."),
    ('REGULATORY', 'LOW', "Aucune exigence réglementaire touchée."),
    ('TRAINING', 'MEDIUM', "Les deux monteurs doivent être formés au nouveau geste."),
    ('COSTS', 'LOW', "Coût interne, absorbé par l'atelier."),
    ('HEALTH_ENVIRONMENT', 'LOW', "Aucun rejet ni produit nouveau."),
]


def changements(c):
    """Ouvre des demandes de changement et les fait avancer."""
    crees = []
    for titre, nature, site, description, justification, echeance, etape in CHANGEMENTS:
        vue = c.poste('/api/v1/changes', {
            'title': titre, 'type': nature, 'site': site, 'description': description,
            'justification': justification, 'plannedFor': jour(echeance),
        }, 'changement %s' % titre[:30])
        if not vue:
            continue
        crees.append(vue['id'])
        if etape in ('analyse', 'soumis'):
            c.poste('/api/v1/changes/%s/analysis' % vue['id'], None,
                    'analyse %s' % titre[:24])
            # Le module refuse la soumission tant que les huit domaines
            # d'impact ne sont pas cotés : une demande de changement qui n'a
            # pesé ni la sécurité ni la formation n'a pas été analysée.
            for domaine, niveau, note in IMPACTS:
                c.met('/api/v1/changes/%s/impacts/%s' % (vue['id'], domaine),
                      {'level': niveau, 'notes': note},
                      'impact %s %s' % (titre[:16], domaine))
        if etape == 'soumis':
            c.poste('/api/v1/changes/%s/submit' % vue['id'], None,
                    'soumission %s' % titre[:24])
    return crees


# ---------------------------------------------------------------------------
# DMAIC et dispositifs anti-erreur.
# ---------------------------------------------------------------------------

PROJETS_DMAIC = [
    {
        'titre': "Réduire la dispersion de la cote de portée (arbre 400)",
        'probleme': "La cote de portée Ø45 h6 dérive en fin de série : Cpk mesuré à 0,92 "
                    "pour 1,33 exigé par le client.",
        'objectif': "Atteindre un Cpk supérieur à 1,33 sur trois séries consécutives.",
        'echeance': 75, 'bas': 44.984, 'haut': 45.000, 'cible': 44.992, 'unite': 'mm',
        'economie': 18000.0,
        'mesures': [
            # Trois sous-groupes : la dérive se voit à l'œil nu sur le troisième.
            ('S1', [44.991, 44.993, 44.990, 44.992, 44.994]),
            ('S2', [44.992, 44.995, 44.993, 44.991, 44.996]),
            ('S3', [44.997, 44.999, 45.001, 44.998, 45.002]),
        ],
    },
    {
        'titre': "Réduire le taux de retouche peinture",
        'probleme': "7 % des carters sont retouchés pour coulures ou manques.",
        'objectif': "Descendre sous 2 % de retouches sur deux mois consécutifs.",
        'echeance': 110, 'bas': 18.0, 'haut': 22.0, 'cible': 20.0, 'unite': 's',
        'economie': 9500.0,
        'mesures': [
            ('P1', [19.8, 20.1, 19.9, 20.4, 20.0]),
            ('P2', [21.2, 21.8, 22.4, 21.6, 22.1]),
        ],
    },
]

POKA_YOKE = [
    ("Outil de pose conique du joint torique", 'PREVENTION', 'PHYSICAL_SHAPE',
     "Un cône guide le joint sur la portée sans qu'il passe sur l'arête vive.",
     "Industrie mécanique, hydraulique", 'faible',
     "Cercle ligne 2 — proposition du 12 du mois"),
    ("Butée mécanique sur la presse d'emmanchement", 'PREVENTION', 'LIMIT_SWITCH',
     "La presse s'arrête à la cote, le roulement ne peut plus être enfoncé trop loin.",
     "Industrie mécanique", 'moyen', "AMDEC processus assemblage pompe 400"),
    ("Détrompeur de teinte par code couleur bidon", 'PREVENTION', 'COLOR_CODING',
     "Chaque teinte porte une pastille de couleur reprise sur la fiche de lancement.",
     "Peinture industrielle", 'faible', "Retour d'expérience cabine peinture"),
    ("Contrôle de pression bloquant au banc d'essai", 'DETECTION', 'SENSOR',
     "Le banc refuse de valider la pompe tant que la pression d'épreuve n'est pas tenue "
     "deux minutes.",
     "Industrie, hydraulique", 'moyen', "Plan de surveillance CP-PH400-PROD"),
    ("Liste de vérification de démarrage de poste", 'DETECTION', 'CHECKLIST',
     "Six points à cocher avant la première pièce : viscosité, filtres, gamme, outils.",
     "Tous secteurs", 'faible', "Audit 5S cabine peinture"),
]


def dmaic(c):
    """Ouvre des projets DMAIC, leurs relevés, et la bibliothèque anti-erreur."""
    projets = []
    for p in PROJETS_DMAIC:
        vue = c.poste('/api/v1/dmaic/projects', {
            'title': p['titre'], 'problemStatement': p['probleme'],
            'goalStatement': p['objectif'], 'blackBeltId': c.sujet, 'championId': c.sujet,
            'targetCompletionDate': jour(p['echeance']),
            'specLowerLimit': p['bas'], 'specUpperLimit': p['haut'],
            'specTarget': p['cible'], 'specUnit': p['unite'],
            'estimatedSavingsEur': p['economie'],
        }, 'projet DMAIC %s' % p['titre'][:26])
        if not vue:
            continue
        projets.append(vue['id'])
        rang = 0
        for sous_groupe, valeurs in p['mesures']:
            for valeur in valeurs:
                rang += 1
                c.poste('/api/v1/dmaic/projects/%s/measures' % vue['id'], {
                    'value': valeur, 'subgroupId': sous_groupe,
                    'sourceRef': 'Relevé atelier', 'recordedAt': instant(-30 + rang, 8),
                    'operatorId': c.sujet, 'note': None,
                }, 'relevé %s %s n°%d' % (p['titre'][:16], sous_groupe, rang))

    for nom, nature, mecanisme, description, secteurs, cout, source in POKA_YOKE:
        c.poste('/api/v1/dmaic/pokayoke', {
            'name': nom, 'description': description, 'type': nature,
            'mechanism': mecanisme, 'applicableIndustries': secteurs,
            'examples': description, 'implementationCost': cout,
            'sourceReference': source, 'projectId': None,
        }, 'poka-yoke %s' % nom[:28])
    return projets


# ---------------------------------------------------------------------------
# Équipements connectés.
# ---------------------------------------------------------------------------

OBJETS = [
    ('CAP-VIB-T3', 'Capteur de vibration — tour 3', 'SENSOR_VIBRATION', 'MQTT',
     'Atelier usinage — tour 3',
     "Surveillance de la broche, dont l'usure a déjà produit une dérive de cote."),
    ('CAP-TEMP-CAB', 'Sonde de température — cabine peinture', 'SENSOR_TEMPERATURE',
     'MODBUS_TCP', 'Cabine peinture',
     "La viscosité de la peinture dépend de la température de la cabine."),
    ('CAP-PRES-BE1', "Capteur de pression — banc d'essai 1", 'SENSOR_PRESSURE', 'OPC_UA',
     "Banc d'essai 1", "Relevé de la pression d'épreuve, sans recopie manuelle."),
    ('CAM-5S-L2', 'Caméra 5S — ligne 2', 'CAMERA', 'MQTT', 'Ligne 2 — montage',
     "Image horodatée du poste en fin de poste, pour l'audit 5S."),
    ('PASS-ATELIER', 'Passerelle atelier', 'GATEWAY', 'SPARKPLUG_B', 'Local technique',
     "Passerelle de collecte des capteurs de l'atelier."),
    ('CAP-HUM-MAG', 'Sonde d humidité — magasin', 'SENSOR_HUMIDITY', 'LORAWAN',
     'Magasin pièces', "Les roulements craignent l'humidité en stock."),
]


def objets_connectes(c):
    """Enregistre les équipements connectés et en active une partie."""
    crees = []
    for code, nom, nature, protocole, lieu, description in OBJETS:
        vue = c.poste('/api/v1/iot/devices', {
            # `type` ET `deviceType` : la version déployée attend le premier
            # nom, le code actuel le second. Envoyer les deux rend le script
            # utilisable des deux côtés d'un déploiement.
            'code': code, 'name': nom, 'type': nature, 'deviceType': nature,
            'protocol': protocole,
            'location': lieu, 'description': description, 'metadataJson': None,
            'createdBy': c.sujet,
        }, 'équipement connecté %s' % code)
        if not vue:
            continue
        crees.append(vue['id'])
        if code != 'CAM-5S-L2':
            # Tous sauf un : un équipement déclaré mais pas encore raccordé est
            # une situation ordinaire, et l'écran doit savoir la montrer.
            c.poste('/api/v1/iot/devices/%s/activate' % vue['id'], None,
                    'activation %s' % code)
    return crees
