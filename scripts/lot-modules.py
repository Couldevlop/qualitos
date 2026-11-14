# -*- coding: utf-8 -*-
"""Un lot de données pour TOUS les modules, pas seulement les écarts.

POURQUOI CE SCRIPT. `lot-nc.py` peuple les écarts et leur suite — c'est le
cœur, et c'est tout ce qui existait : une instance où l'on ouvre « Produits »,
« AMDEC », « Formation » ou « Métrologie » et où l'on tombe sur un écran vide
ne démontre rien, et ne permet de relire aucun enchaînement. Les modules
restaient vides pour une raison simple : ils n'étaient pas ouverts pour le
client, et chaque appel se heurtait à un 403.

CE QU'IL ÉCRIT. Un univers cohérent avec celui des écarts — de la mécanique :
pompes hydrauliques, carters, arbres de sortie, un tour, une cabine de
peinture, des fournisseurs de fonderie. Les produits portent les composants et
les opérations que les AMDEC référencent ; les AMDEC portent les causes que
l'on retrouve dans les écarts ; le plan de surveillance surveille les
caractéristiques que l'AMDEC a jugées critiques. Des données qui ne se
contredisent pas valent mieux que des données nombreuses.

PAR L'API, JAMAIS PAR LA BASE. Chaque enregistrement passe par la même route
qu'un utilisateur, donc par les mêmes validations, transitions et événements
d'audit. Un refus est rapporté tel quel : il dit quelque chose de vrai sur le
module, et la sortie vaut autant comme relevé de fonctionnement que comme jeu
de données.

    python scripts/lot-modules.py --api https://… --keycloak https://…/auth \\
        --utilisateur … --secret … [--modules produits,amdec,…]
"""
import argparse
import datetime as dt
import sys

import lot_modules_fin as fin
import lot_modules_suite as suite
from qualitos_api import Client, jeton


AUJOURD_HUI = dt.date.today()


def jour(decalage):
    """Une date ISO, décalée de tant de jours (négatif = passé)."""
    return (AUJOURD_HUI + dt.timedelta(days=decalage)).isoformat()


def instant(decalage_jours, heure=9):
    """Un instant ISO en UTC, décalé de tant de jours."""
    base = dt.datetime.combine(AUJOURD_HUI + dt.timedelta(days=decalage_jours),
                               dt.time(hour=heure))
    return base.strftime('%Y-%m-%dT%H:%M:%SZ')


# ---------------------------------------------------------------------------
# Produits — le socle que les AMDEC et les plans de surveillance référencent.
# ---------------------------------------------------------------------------

PRODUITS = [
    {
        'code': 'PH-400',
        'designation': 'Pompe hydraulique série 400',
        'family': 'Pompes',
        'revisionIndex': 'C',
        'customerLabel': 'Groupe Verneuil',
        'siteLabel': 'Usine Nord — ligne 2',
        'composants': [
            (10, 'ARB-400-02', 'Arbre de sortie acier 42CrMo4', '1', 'pce'),
            (20, 'CAR-400-11', 'Carter de pompe fonte GS', '1', 'pce'),
            (30, 'JNT-400-07', 'Joint torique viton 48x3', '2', 'pce'),
            (40, 'ROU-6208', 'Roulement à billes 6208-2RS', '2', 'pce'),
        ],
        'operations': [
            (10, 'OP-10', 'Tournage de l arbre', 'Tour 3'),
            (20, 'OP-20', 'Rectification de portée', 'Rectifieuse 1'),
            (30, 'OP-30', 'Montage joint et roulements', 'Poste montage A'),
            (40, 'OP-40', 'Essai en pression', 'Banc d essai 1'),
            (50, 'OP-50', 'Peinture et marquage', 'Cabine peinture'),
        ],
    },
    {
        'code': 'CAR-400',
        'designation': 'Carter de pompe série 400',
        'family': 'Carters',
        'revisionIndex': 'B',
        'customerLabel': 'Groupe Verneuil',
        'siteLabel': 'Usine Nord — ligne 2',
        'composants': [
            (10, 'FON-GS500', 'Brut de fonderie fonte GS 500-7', '1', 'pce'),
            (20, 'PEI-RAL5015', 'Peinture époxy RAL 5015', '0.35', 'L'),
        ],
        'operations': [
            (10, 'OP-10', 'Ébavurage du brut', 'Poste ébavurage'),
            (20, 'OP-20', 'Usinage des portées', 'Centre d usinage 2'),
            (30, 'OP-30', 'Peinture', 'Cabine peinture'),
        ],
    },
    {
        'code': 'ARB-400',
        'designation': 'Arbre de sortie série 400',
        'family': 'Arbres',
        'revisionIndex': 'D',
        'customerLabel': 'Groupe Verneuil',
        'siteLabel': 'Usine Nord — ligne 1',
        'composants': [
            (10, 'BAR-42CRMO4', 'Barre acier 42CrMo4 Ø45', '0.4', 'm'),
        ],
        'operations': [
            (10, 'OP-10', 'Débit de barre', 'Scie 1'),
            (20, 'OP-20', 'Tournage', 'Tour 3'),
            (30, 'OP-30', 'Traitement thermique', 'Sous-traitance'),
            (40, 'OP-40', 'Rectification', 'Rectifieuse 1'),
        ],
    },
    {
        'code': 'KIT-MAINT-400',
        'designation': 'Kit de maintenance pompe 400',
        'family': 'Rechange',
        'revisionIndex': 'A',
        'customerLabel': 'Réseau après-vente',
        'siteLabel': 'Magasin pièces',
        'composants': [
            (10, 'JNT-400-07', 'Joint torique viton 48x3', '2', 'pce'),
            (20, 'ROU-6208', 'Roulement à billes 6208-2RS', '2', 'pce'),
        ],
        'operations': [
            (10, 'OP-10', 'Préparation du kit', 'Magasin'),
        ],
    },
]


def produits(c):
    """Crée les produits, leurs composants, leurs opérations — et les active.

    <p>Relançable : un produit dont le code existe déjà est RETROUVÉ plutôt que
    recréé. Sans cela, le second passage ne rendrait aucun identifiant, et les
    AMDEC comme les plans de surveillance se retrouveraient orphelins du
    produit qu'ils décrivent — ce qui est pire qu'un doublon.
    """
    crees = {}
    deja = {v['code']: v for v in (c.lit('/api/v1/products', 'produits existants',
                                         silencieux=True) or [])}
    for p in PRODUITS:
        if p['code'] in deja:
            pid = deja[p['code']]['id']
            operations = {o['code']: o['id'] for o in
                          (c.lit('/api/v1/products/%s/operations' % pid,
                                 'opérations de %s' % p['code'], silencieux=True) or [])}
            crees[p['code']] = {'id': pid, 'operations': operations}
            continue

        vue = c.poste('/api/v1/products', {
            'code': p['code'], 'designation': p['designation'], 'family': p['family'],
            'revisionIndex': p['revisionIndex'], 'customerLabel': p['customerLabel'],
            'siteLabel': p['siteLabel'], 'ownerUserId': c.sujet,
        }, 'produit %s' % p['code'])
        if not vue:
            continue
        pid = vue['id']
        operations = {}
        for rang, reference, libelle, quantite, unite in p['composants']:
            c.poste('/api/v1/products/%s/components' % pid, {
                'sequenceNo': rang, 'reference': reference, 'label': libelle,
                'quantity': quantite, 'unit': unite, 'supplierId': None,
            }, 'composant %s' % reference)
        for rang, code, libelle, poste in p['operations']:
            vue_op = c.poste('/api/v1/products/%s/operations' % pid, {
                'sequenceNo': rang, 'code': code, 'label': libelle, 'workstation': poste,
            }, 'opération %s %s' % (p['code'], code))
            if vue_op:
                operations[code] = vue_op['id']
        # Un produit qui reste en brouillon ne se référence nulle part : ni
        # AMDEC, ni plan de surveillance. L'activation fait partie du jeu.
        c.poste('/api/v1/products/%s/activate' % pid, None, 'activation %s' % p['code'])
        crees[p['code']] = {'id': pid, 'operations': operations}
    return crees


# ---------------------------------------------------------------------------
# AMDEC processus — les causes que les écarts ont révélées, cotées.
# ---------------------------------------------------------------------------

AMDEC = [
    {
        'code': 'PFMEA-PH400',
        'name': 'AMDEC processus — assemblage pompe 400',
        'scope': 'Du montage du joint à l essai en pression, ligne 2.',
        'type': 'PROCESS_FMEA',
        'produit': 'PH-400',
        'seuil': 100,
        'lignes': [
            ('OP-30', 'Monter le joint torique sans le blesser',
             'Portée de joint marquée au montage', 'Fuite en charge au-delà de 80 bar',
             'Outillage de montage inadapté, montage à la main',
             'Contrôle visuel du joint après montage', 8, 5, 6,
             'Outil de pose conique + formation des deux monteurs',
             jour(45), 'SAFETY'),
            ('OP-40', 'Éprouver la pompe à 120 bar',
             'Essai conclu sans atteindre la pression', 'Pompe non conforme livrée',
             'Manomètre du banc hors étalonnage', 'Lecture du manomètre par l opérateur',
             7, 3, 4, 'Étalonnage annuel du banc et blocage si échéance dépassée',
             jour(60), 'REGULATORY'),
            ('OP-30', 'Monter les roulements en butée',
             'Roulement mal emmanché', 'Jeu radial hors tolérance, bruit en service',
             'Presse réglée au jugé', 'Contrôle du jeu au comparateur sur 1 pièce sur 10',
             6, 4, 5, 'Butée mécanique sur la presse (dispositif anti-erreur)',
             jour(30), 'SPECIAL'),
            ('OP-50', 'Appliquer la peinture d aspect',
             'Coulures de peinture', 'Retouche, retard de livraison',
             'Viscosité non contrôlée en début de poste',
             'Contrôle visuel à 100 %', 3, 6, 3,
             'Contrôle de viscosité ajouté à la fiche de démarrage', jour(20), 'STANDARD'),
        ],
    },
    {
        'code': 'PFMEA-ARB400',
        'name': 'AMDEC processus — usinage arbre 400',
        'scope': 'Du débit de barre à la rectification finale.',
        'type': 'PROCESS_FMEA',
        'produit': 'ARB-400',
        'seuil': 120,
        'lignes': [
            ('OP-20', 'Tourner la portée au diamètre',
             'Cote hors tolérance en fin de série', 'Jeu radial hors tolérance au montage',
             'Usure du roulement de broche du tour 3',
             'Contrôle dimensionnel en début et fin de lot', 7, 6, 5,
             'Maintenance préventive de la broche, contrôle toutes les 20 pièces',
             jour(15), 'SPECIAL'),
            ('OP-30', 'Obtenir la dureté spécifiée',
             'Dureté en limite basse', 'Usure prématurée en service',
             'Cycle de revenu du sous-traitant non maîtrisé',
             'Certificat matière à réception', 6, 4, 6,
             'Audit du sous-traitant et contrôle de dureté à réception', jour(75), 'SAFETY'),
            ('OP-40', 'Rectifier sans brûlure',
             'Brûlure de rectification', 'Amorce de rupture en fatigue',
             'Meule chargée, arrosage insuffisant',
             'Contrôle visuel après rectification', 9, 3, 5,
             'Attaque nital par sondage et suivi du débit d arrosage', jour(50), 'SAFETY'),
        ],
    },
]


def amdec(c, produits_crees):
    """Crée les projets AMDEC, leurs lignes, et les active."""
    crees = {}
    for projet in AMDEC:
        reference = produits_crees.get(projet['produit'], {})
        vue = c.poste('/api/v1/fmea/projects', {
            'code': projet['code'], 'name': projet['name'], 'scope': projet['scope'],
            'type': projet['type'], 'criticalRpnThreshold': projet['seuil'],
            'ownerUserId': c.sujet, 'createdBy': c.sujet,
            'productId': reference.get('id'),
        }, 'AMDEC %s' % projet['code'])
        if not vue:
            continue
        pid = vue['id']
        for (op, fonction, mode, effet, cause, maitrise, g, o, d,
             action, echeance, classe) in projet['lignes']:
            c.poste('/api/v1/fmea/projects/%s/items' % pid, {
                'function': fonction, 'failureMode': mode, 'failureEffect': effet,
                'failureCause': cause, 'currentControls': maitrise,
                'severity': g, 'occurrence': o, 'detection': d,
                'recommendedAction': action, 'actionOwnerUserId': c.sujet,
                'actionOwnerName': c.nom, 'actionDueDate': echeance,
                'operationId': reference.get('operations', {}).get(op),
                'characteristicClass': classe,
            }, 'ligne AMDEC %s %s' % (projet['code'], mode[:28]))
        c.poste('/api/v1/fmea/projects/%s/activate' % pid, None,
                'activation AMDEC %s' % projet['code'])
        crees[projet['code']] = pid
    return crees


# ---------------------------------------------------------------------------
# Plan de surveillance — ce que l'AMDEC a jugé critique devient surveillé.
# ---------------------------------------------------------------------------

PLANS = [
    {
        'produit': 'PH-400', 'code': 'CP-PH400-PROD', 'phase': 'PRODUCTION',
        'lignes': [
            (10, 'OP-40', 'Banc d essai 1', '10', 'Pression d épreuve',
             'Pression maintenue 2 min', 'PRODUCT', 'SAFETY', '120 bar min',
             '120', '140', 'bar', 'Manomètre étalonné classe 0,5', '100 %',
             'À chaque pompe', 'Isoler la pompe et ouvrir un écart'),
            (20, 'OP-30', 'Poste montage A', '20', 'Jeu radial arbre',
             'Jeu radial après montage', 'PRODUCT', 'SPECIAL', '0,05 mm maxi',
             '0', '0.05', 'mm', 'Comparateur sur marbre', '1 sur 10',
             'Toutes les 10 pompes', 'Bloquer le lot et contrôler à 100 %'),
            (30, 'OP-50', 'Cabine peinture', '30', 'Viscosité peinture',
             'Viscosité en début de poste', 'PROCESS', 'STANDARD', '18 à 22 s coupe 4',
             '18', '22', 's', 'Coupe AFNOR n°4', '1 par poste',
             'Au démarrage', 'Corriger la dilution avant de peindre'),
        ],
    },
    {
        'produit': 'ARB-400', 'code': 'CP-ARB400-PROD', 'phase': 'PRODUCTION',
        'lignes': [
            (10, 'OP-20', 'Tour 3', '10', 'Diamètre de portée',
             'Ø45 h6 sur portée roulement', 'PRODUCT', 'SPECIAL', '44,984 à 45,000 mm',
             '44.984', '45.000', 'mm', 'Micromètre 25-50 étalonné', '1 sur 20',
             'Toutes les 20 pièces', 'Arrêter la série et régler le tour'),
            (20, 'OP-30', 'Sous-traitance', '20', 'Dureté après revenu',
             'Dureté Brinell', 'PRODUCT', 'SAFETY', '180 à 220 HB',
             '180', '220', 'HB', 'Duromètre Brinell', '3 par coulée',
             'À chaque coulée', 'Refuser la coulée et alerter l achat'),
            (30, 'OP-40', 'Rectifieuse 1', '30', 'Absence de brûlure',
             'Attaque nital', 'PRODUCT', 'SAFETY', 'Aucune brûlure admise',
             None, None, '', 'Attaque nital par sondage', '2 par lot',
             'À chaque lot', 'Rebuter la pièce et vérifier l arrosage'),
        ],
    },
]


def plans_de_surveillance(c, produits_crees):
    """Ouvre un plan de surveillance par produit, le remplit, l'approuve."""
    crees = {}
    for plan in PLANS:
        reference = produits_crees.get(plan['produit'])
        if not reference:
            continue
        chemin = '/api/v1/products/%s/control-plans' % reference['id']
        vue = c.poste(chemin, {
            'phase': plan['phase'], 'code': plan['code'], 'ownerUserId': c.sujet,
        }, 'plan de surveillance %s' % plan['code'])
        if not vue:
            continue
        pid = vue['id']
        for (rang, op, machine, numero, libelle, specifiee, nature, classe,
             specification, bas, haut, unite, technique, taille,
             frequence, reaction) in plan['lignes']:
            c.poste('%s/%s/lines' % (chemin, pid), {
                'sequenceNo': rang,
                'operationId': reference['operations'].get(op),
                'machine': machine, 'characteristicNo': numero,
                'characteristicLabel': libelle, 'specifiedCharacteristic': specifiee,
                'characteristicType': nature, 'specialClass': classe,
                'specification': specification,
                'toleranceLower': bas, 'toleranceUpper': haut, 'unit': unite,
                'measurementTechnique': technique, 'sampleSize': taille,
                'sampleFrequency': frequence, 'reactionPlan': reaction,
            }, 'ligne plan %s %s' % (plan['code'], libelle[:24]))
        c.poste('%s/%s/approve' % (chemin, pid), None, 'approbation %s' % plan['code'])
        crees[plan['code']] = pid
    return crees


# ---------------------------------------------------------------------------
# Réclamations clients.
# ---------------------------------------------------------------------------

RECLAMATIONS = [
    ('REC-2026-001', 'EMAIL', 'Groupe Verneuil', 'qualite@verneuil-groupe.test',
     'Sifflement en charge sur trois pompes du lot L-2288', 'HIGH', 'QUALITY',
     'Trois pompes présentent un sifflement au-delà de 80 bar. Retour demandé pour expertise.',
     -18, 'resolue'),
    ('REC-2026-002', 'PHONE', 'Ateliers Ferrand', 'achats@ferrand-ateliers.test',
     'Livraison incomplète : 4 kits de maintenance manquants', 'MEDIUM', 'DELIVERY',
     'Le bordereau annonce 20 kits, 16 reçus. Le client demande le solde sous 5 jours.',
     -11, 'resolue'),
    ('REC-2026-003', 'WEB_FORM', 'Hydro Services', 'sav@hydro-services.test',
     'Peinture écaillée sur deux carters', 'LOW', 'QUALITY',
     'Écaillage constaté au déballage sur deux carters de la série 400.',
     -7, 'affectee'),
    ('REC-2026-004', 'EMAIL', 'Groupe Verneuil', 'qualite@verneuil-groupe.test',
     'Certificat matière absent du dossier de livraison', 'MEDIUM', 'DELIVERY',
     'Le dossier du lot L-2301 ne contient pas le certificat matière de la coulée.',
     -4, 'affectee'),
    ('REC-2026-005', 'PHONE', 'Ateliers Ferrand', 'production@ferrand-ateliers.test',
     'Bruit de roulement après 200 heures', 'CRITICAL', 'PRODUCT',
     'Deux pompes émettent un bruit de roulement après 200 heures de service. '
     'Arrêt de ligne chez le client.',
     -2, 'nouvelle'),
    ('REC-2026-006', 'POSTAL', 'Mairie de Grandval', 'services@grandval.test',
     'Facture non conforme au devis', 'LOW', 'BILLING',
     'Le montant facturé dépasse le devis de 8 %. Explication demandée.',
     -1, 'nouvelle'),
]


def reclamations(c):
    """Déclare les réclamations, en affecte, en résout.

    <p>Relançable comme les produits : une réclamation déjà déclarée est
    retrouvée par son code, et son parcours reprend là où il s'était arrêté.
    """
    crees = []
    page = c.lit('/api/v1/complaints?page=0&size=200', 'réclamations existantes',
                 silencieux=True) or {}
    deja = {v['code']: v for v in page.get('content', [])}
    for (code, canal, client, courriel, sujet, gravite, categorie,
         description, il_y_a, etape) in RECLAMATIONS:
        if code in deja:
            rid = deja[code]['id']
            crees.append(rid)
            etat = deja[code].get('status')
            if etape == 'nouvelle' or etat in ('RESOLVED', 'CLOSED'):
                continue
            if etat == 'NEW':
                c.poste('/api/v1/complaints/%s/assign' % rid,
                        {'assigneeUserId': c.sujet}, 'affectation %s' % code)
            if etape == 'resolue':
                _resout(c, rid, code, canal)
            continue
        vue = c.poste('/api/v1/complaints', {
            'code': code, 'channel': canal, 'customerName': client,
            'customerEmail': courriel, 'subject': sujet, 'description': description,
            'severity': gravite, 'category': categorie,
            'createdBy': c.sujet, 'receivedAt': instant(il_y_a),
        }, 'réclamation %s' % code)
        if not vue:
            continue
        rid = vue['id']
        crees.append(rid)
        if etape == 'nouvelle':
            continue
        c.poste('/api/v1/complaints/%s/assign' % rid,
                {'assigneeUserId': c.sujet}, 'affectation %s' % code)
        if etape == 'resolue':
            _resout(c, rid, code, canal)
    return crees


def _resout(c, rid, code, canal):
    """Répond au client, puis résout, puis relève sa satisfaction.

    <p>Le module refuse de résoudre une réclamation dont le client n'a rien
    reçu — et il a raison : une réclamation « résolue » sans réponse est
    résolue pour nous seuls.
    """
    c.poste('/api/v1/complaints/%s/responses' % rid, {
        'authorUserId': c.sujet, 'channel': canal,
        'body': 'Expertise menée sur les pièces retournées. Reprise effectuée '
                'et dispositions prises pour que le défaut ne revienne pas.',
        'internalNote': False,
    }, 'réponse au client %s' % code)
    c.poste('/api/v1/complaints/%s/resolve' % rid, {'capaCaseId': None},
            'résolution %s' % code)
    c.poste('/api/v1/complaints/%s/satisfaction' % rid, {'score': 8},
            'satisfaction %s' % code)


# ---------------------------------------------------------------------------
# EHS — incidents, presque-accidents.
# ---------------------------------------------------------------------------

INCIDENTS = [
    ('EHS-2026-001', 'Presque-accident : chute de palette en zone expédition',
     'NEAR_MISS', 'MEDIUM', -22, 'Zone expédition',
     'Une palette mal filmée a glissé du chariot. Personne à proximité.', 'clos'),
    ('EHS-2026-002', 'Coupure à la main au poste ébavurage', 'INJURY', 'HIGH', -16,
     'Poste ébavurage',
     'Coupure superficielle à la main gauche, gant percé. Soins sur place, pas d arrêt.',
     'attenue'),
    ('EHS-2026-003', 'Déversement d huile hydraulique au banc d essai',
     'ENVIRONMENTAL', 'MEDIUM', -9, 'Banc d essai 1',
     'Environ 5 litres d huile répandus, absorbant posé, bac de rétention saturé.',
     'enquete'),
    ('EHS-2026-004', 'Fuite d air comprimé au compresseur 2', 'PROPERTY_DAMAGE', 'LOW',
     -5, 'Local compresseur', 'Sifflement continu, perte de pression sur le réseau.',
     'declare'),
    ('EHS-2026-005', 'Porte coupe-feu bloquée ouverte', 'SECURITY', 'HIGH', -3,
     'Atelier usinage',
     'La porte coupe-feu entre usinage et magasin était calée ouverte par une palette.',
     'enquete'),
]


def ehs(c):
    """Déclare les incidents EHS et les fait avancer."""
    crees = []
    for code, titre, nature, gravite, il_y_a, lieu, description, etape in INCIDENTS:
        vue = c.poste('/api/v1/ehs/incidents', {
            'code': code, 'title': titre, 'description': description,
            'type': nature, 'severity': gravite,
            'occurredAt': instant(il_y_a, 14), 'location': lieu,
            'reportedBy': c.sujet,
        }, 'incident EHS %s' % code)
        if not vue:
            continue
        iid = vue['id']
        crees.append(iid)
        if etape == 'declare':
            continue
        c.poste('/api/v1/ehs/incidents/%s/investigate' % iid,
                {'ownerUserId': c.sujet}, 'enquête EHS %s' % code)
        if etape in ('attenue', 'clos'):
            c.poste('/api/v1/ehs/incidents/%s/mitigate' % iid, {
                'rootCause': 'Consigne de zone non appliquée faute de rappel périodique.',
                'correctiveActions': 'Consigne rappelée en réunion d équipe et affichée au poste.',
            }, 'mesure EHS %s' % code)
        if etape == 'clos':
            c.poste('/api/v1/ehs/incidents/%s/close' % iid, None, 'clôture EHS %s' % code)
    return crees


# ---------------------------------------------------------------------------
# Métrologie — les moyens de mesure que le plan de surveillance invoque.
# ---------------------------------------------------------------------------

EQUIPEMENTS = [
    ('MIC-25-50', 'Micromètre 25-50 mm', 'Mitutoyo', '103-138', 'SN-44120',
     'Atelier usinage', True, [(-320, 'PASS'), (-45, 'PASS')]),
    ('COMP-001', 'Comparateur 0-10 mm', 'Mitutoyo', '2046S', 'SN-77301',
     'Poste montage A', True, [(-200, 'PASS')]),
    ('MANO-BE1', 'Manomètre banc d essai 1', 'Bourdon', 'MEX5', 'SN-10455',
     'Banc d essai 1', True, [(-370, 'PASS'), (-12, 'CONDITIONAL')]),
    ('DURO-BRI', 'Duromètre Brinell', 'Innovatest', 'Nexus 3001', 'SN-90022',
     'Laboratoire', True, [(-150, 'PASS')]),
    ('COUPE-4', 'Coupe AFNOR n°4', 'Elcometer', '2210', 'SN-33018',
     'Cabine peinture', False, [(-90, 'PASS')]),
    ('PAC-150', 'Pied à coulisse 150 mm', 'Facom', '1300A', 'SN-55214',
     'Contrôle final', False, [(-400, 'FAIL'), (-30, 'PASS')]),
]


def metrologie(c):
    """Enregistre les moyens de mesure et leurs constats d étalonnage."""
    crees = []
    for (code, nom, marque, modele, serie, lieu, critique, constats) in EQUIPEMENTS:
        vue = c.poste('/api/v1/calibration/equipment', {
            'code': code, 'name': nom, 'manufacturer': marque, 'model': modele,
            'serialNumber': serie, 'location': lieu, 'critical': critique,
            'iotDeviceId': None, 'ownerUserId': c.sujet, 'createdBy': c.sujet,
        }, 'équipement %s' % code)
        if not vue:
            continue
        eid = vue['id']
        crees.append(eid)
        for il_y_a, resultat in constats:
            c.poste('/api/v1/calibration/equipment/%s/records' % eid, {
                'performedOn': jour(il_y_a), 'performedByUserId': c.sujet,
                'performedByOrg': 'Laboratoire accrédité Métrolog',
                'result': resultat,
                'measurements': 'Écart maximal relevé dans les limites du constructeur.',
                'certificateReference': 'CERT-%s-%s' % (code, abs(il_y_a)),
                'nextDueOverride': None,
            }, 'étalonnage %s %s' % (code, resultat))
    return crees


# ---------------------------------------------------------------------------

LOTS = [
    ('produits', 'les produits, leurs composants et leurs opérations'),
    ('amdec', 'les AMDEC processus et leurs lignes'),
    ('plans', 'les plans de surveillance'),
    ('reclamations', 'les réclamations clients'),
    ('ehs', 'les incidents EHS'),
    ('metrologie', 'les moyens de mesure et leurs étalonnages'),
    ('formation', 'les compétences, parcours et évaluations'),
    ('cercles', 'les cercles de qualité, leurs réunions et propositions'),
    ('pdca', 'les cycles PDCA et leurs étapes'),
    ('cinq-s', 'les audits 5S et leurs notes'),
    ('audits', 'les audits internes, leurs grilles et leurs constats'),
    ('indicateurs', 'les indicateurs et leur historique'),
    ('fournisseurs', 'les fournisseurs, leurs audits et leurs écarts'),
    ('changements', 'les demandes de changement'),
    ('dmaic', 'les projets DMAIC et la bibliothèque anti-erreur'),
    ('objets', 'les équipements connectés'),
]


def main():
    analyseur = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    analyseur.add_argument('--api', default='http://localhost:8082')
    analyseur.add_argument('--keycloak', default='http://localhost:8080')
    analyseur.add_argument('--realm', default='qualitos')
    analyseur.add_argument('--client-id', default='qualitos-web')
    analyseur.add_argument('--utilisateur', default='demo')
    analyseur.add_argument('--secret', default='demo')
    analyseur.add_argument('--langue', default='fr')
    analyseur.add_argument('--modules', default='',
                           help='liste séparée par des virgules ; vide = tout')
    options = analyseur.parse_args()

    demandes = [m.strip() for m in options.modules.split(',') if m.strip()]
    voulu = (lambda nom: not demandes or nom in demandes)

    acces = jeton(options.keycloak, options.realm, options.client_id,
                  options.utilisateur, options.secret)
    c = Client(options.api, acces, options.langue)
    print('Peuplement des modules, au nom de %s.\n' % c.nom)

    crees = {}
    if voulu('produits'):
        crees['produits'] = produits(c)
        print('  produits : %d' % len(crees['produits']))
    if voulu('amdec'):
        print('  AMDEC : %d' % len(amdec(c, crees.get('produits', {}))))
    if voulu('plans'):
        print('  plans de surveillance : %d'
              % len(plans_de_surveillance(c, crees.get('produits', {}))))
    if voulu('reclamations'):
        print('  réclamations : %d' % len(reclamations(c)))
    if voulu('ehs'):
        print('  incidents EHS : %d' % len(ehs(c)))
    if voulu('metrologie'):
        print('  moyens de mesure : %d' % len(metrologie(c)))
    if voulu('formation'):
        print('  compétences : %d' % len(suite.formation(c)))
    if voulu('cercles'):
        print('  cercles de qualité : %d' % len(suite.cercles(c)))
    if voulu('pdca'):
        print('  cycles PDCA : %d' % len(suite.pdca(c)))
    if voulu('cinq-s'):
        print('  audits 5S : %d' % len(suite.cinq_s(c)))
    if voulu('audits'):
        print('  audits internes : %d' % len(suite.audits(c)))
    if voulu('indicateurs'):
        print('  indicateurs : %d' % len(suite.indicateurs(c)))
    if voulu('fournisseurs'):
        print('  fournisseurs : %d' % len(suite.fournisseurs(c)))
    if voulu('changements'):
        print('  demandes de changement : %d' % len(fin.changements(c)))
    if voulu('dmaic'):
        print('  projets DMAIC : %d' % len(fin.dmaic(c)))
    if voulu('objets'):
        print('  équipements connectés : %d' % len(fin.objets_connectes(c)))

    return c.bilan()


if __name__ == '__main__':
    sys.exit(main())
