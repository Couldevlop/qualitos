# -*- coding: utf-8 -*-
"""Peuple une instance QualitOS de données de démonstration, PAR L'API RÉELLE.

Pourquoi par l'API et non par du SQL : une insertion directe en base contourne
exactement ce qu'on veut éprouver — la validation, les transitions d'état, les
règles de tenant, les événements d'audit. Ce script crée donc des enregistrements
comme un utilisateur le ferait, et **rapporte chaque refus**. Un module qui
répond 400 ou 500 ici est un module qui répondrait pareil à l'écran : la sortie
vaut autant comme jeu de données que comme relevé de fonctionnement.

Usage (moteur démarré, Keycloak debout) :

    python scripts/donnees-demo.py
    python scripts/donnees-demo.py --api http://localhost:8082 --utilisateur demo

Le script est REJOUABLE : les codes portent un suffixe tiré de l'horodatage, donc
un second passage crée un second jeu au lieu d'échouer sur des doublons.
"""
import argparse
import json
import sys
import time
import urllib.error
import urllib.request
import uuid
from datetime import date, datetime, timedelta, timezone

SUFFIXE = datetime.now().strftime('%m%d%H%M')


def maintenant(decalage_jours=0):
    return (datetime.now(timezone.utc) + timedelta(days=decalage_jours)) \
        .strftime('%Y-%m-%dT%H:%M:%SZ')


def jour(decalage_jours=0):
    return (date.today() + timedelta(days=decalage_jours)).isoformat()


class Client:
    """Appelle l'API et tient le compte de ce qui passe et de ce qui casse."""

    def __init__(self, api, jeton, langue):
        self.api = api.rstrip('/')
        self.jeton = jeton
        self.langue = langue
        self.reussites = []
        self.echecs = []
        self.sujet = self._sujet()

    def _sujet(self):
        """Le sub du jeton : plusieurs API attendent un identifiant d'acteur."""
        charge = self.jeton.split('.')[1]
        charge += '=' * (-len(charge) % 4)
        import base64
        return json.loads(base64.urlsafe_b64decode(charge))['sub']

    def poste(self, chemin, corps, quoi):
        return self._appelle('POST', chemin, corps, quoi)

    def patche(self, chemin, corps, quoi):
        """Plusieurs transitions d'etat sont des PATCH, et non des POST."""
        return self._appelle('PATCH', chemin, corps, quoi)

    def _appelle(self, methode, chemin, corps, quoi):
        requete = urllib.request.Request(
            self.api + chemin,
            data=json.dumps(corps).encode('utf8'),
            method=methode,
            headers={
                'Authorization': 'Bearer ' + self.jeton,
                'Content-Type': 'application/json',
                'Accept-Language': self.langue,
            })
        try:
            with urllib.request.urlopen(requete, timeout=60) as reponse:
                contenu = reponse.read().decode('utf8')
                self.reussites.append((quoi, reponse.status))
                print('  OK   %-46s %s' % (quoi, reponse.status))
                return json.loads(contenu) if contenu else {}
        except urllib.error.HTTPError as erreur:
            detail = erreur.read().decode('utf8', 'replace')[:200]
            self.echecs.append((quoi, erreur.code, detail))
            print('  ECHEC %-45s %s  %s' % (quoi, erreur.code, detail))
            return None
        except Exception as erreur:                     # noqa: BLE001
            self.echecs.append((quoi, 0, str(erreur)[:200]))
            print('  ECHEC %-45s  %s' % (quoi, erreur))
            return None


def jeton(keycloak, realm, client_id, utilisateur, secret):
    corps = ('grant_type=password&client_id=%s&username=%s&password=%s'
             % (client_id, utilisateur, secret)).encode('utf8')
    url = '%s/realms/%s/protocol/openid-connect/token' % (keycloak.rstrip('/'), realm)
    with urllib.request.urlopen(urllib.request.Request(url, data=corps), timeout=30) as r:
        return json.load(r)['access_token']


# ---------------------------------------------------------------------------
# Les jeux de données, module par module. Chaque bloc est autonome : un module
# qui refuse n'empêche pas les suivants, ce qui est tout l'objet du relevé.
# ---------------------------------------------------------------------------

def modules(c):
    """Active les modules dont ce jeu de donnees a besoin.

    Sans cela, produits et AMDEC repondent 403 « module non active » : la garde
    fonctionne, mais le jeu de donnees s'arrete a mi-chemin. L'activation est
    reservee a l'administration du client, d'ou l'avertissement plutot qu'un echec
    quand l'utilisateur n'a pas le droit.
    """
    print('\nModules du client')
    refuses = []
    for code in ['product', 'risk', 'kpi', 'training', 'calibration', 'ehs',
                 'change', 'supplier', 'docs', 'audit', 'capa', 'iot']:
        avant = len(c.echecs)
        c.poste('/api/v1/tenant-modules/activations', {'moduleCode': code},
                'activation du module %s' % code)
        if len(c.echecs) > avant:
            refuses.append(code)
    if refuses:
        print('  -> Activation refusee : le droit est reserve a l administration du')
        print('     client. Les modules concernes repondront 403 plus bas.')
        print('     Rejouez avec un compte admin_tenant, ou activez-les depuis')
        print('     Administration > Modules : %s' % ', '.join(refuses))


def produits_et_amdec(c):
    print('\nProduits, AMDEC et plans de surveillance')
    produit = c.poste('/api/v1/products', {
        'code': 'PRD-%s' % SUFFIXE,
        'designation': 'Pompe hydraulique HP-40',
        'reference': 'HP-40-REV-C',
        'family': 'Hydraulique',
    }, 'produit')

    projet = c.poste('/api/v1/fmea/projects', {
        'code': 'PFMEA-%s' % SUFFIXE,
        'name': 'AMDEC processus — assemblage pompe HP-40',
        'type': 'PROCESS_FMEA',
        'createdBy': c.sujet,
        'productId': (produit or {}).get('id'),
        'scope': 'Ligne d assemblage 2, postes 10 a 60',
    }, 'projet AMDEC')

    if projet:
        c.poste('/api/v1/fmea/projects/%s/items' % projet['id'], {
            'processStep': 'Serrage du carter',
            'failureMode': 'Couple de serrage insuffisant',
            'failureEffect': 'Fuite d huile en service',
            'failureCause': 'Visseuse dereglee',
            'severity': 8, 'occurrence': 4, 'detection': 5,
            'currentControls': 'Controle au couplemetre 1 piece sur 20',
        }, 'ligne AMDEC (RPN 160)')
        c.poste('/api/v1/fmea/projects/%s/items' % projet['id'], {
            'processStep': 'Montage du joint torique',
            'failureMode': 'Joint pince',
            'failureEffect': 'Fuite immediate au banc',
            'failureCause': 'Outil de pose use',
            'severity': 9, 'occurrence': 5, 'detection': 6,
            'currentControls': 'Controle visuel 100 %',
        }, 'ligne AMDEC (RPN 270, > 200)')
    return produit


def cycle_pdca(c):
    print('\nCycles PDCA')
    for titre, objet in [
        ('Reduire le taux de rebut de la ligne 2',
         'Passer de 3,1 % a 1,5 % de rebut sur le trimestre'),
        ('Fiabiliser le demarrage de poste',
         'Supprimer les 12 minutes perdues chaque matin au demarrage'),
    ]:
        c.poste('/api/v1/pdca/cycles', {
            'title': titre,
            'objective': objet,
            'ownerId': c.sujet,
            'startDate': jour(-20),
            'targetDate': jour(40),
        }, 'cycle PDCA « %s »' % titre[:28])


def audits_5s(c):
    print('\nAudits 5S')
    for zone, score in [('Atelier mecanique — ilot 3', 72), ('Magasin pieces detachees', 58)]:
        c.poste('/api/v1/fives/audits', {
            'zone': zone,
            'auditorId': c.sujet,
            'auditDate': jour(-3),
            'seiriScore': 4, 'seitonScore': 3, 'seisoScore': 4,
            'seiketsuScore': 3, 'shitsukeScore': 3,
            'comments': 'Marquage au sol a reprendre, etiquetage partiel.',
        }, 'audit 5S « %s » (%d)' % (zone[:24], score))


def non_conformites(c):
    print('\nNon-conformites, analyses et CAPA')
    interne = c.poste('/api/v1/nc', {
        'title': 'Jeu excessif sur l arbre de sortie',
        'description': 'Constat au controle final : jeu radial de 0,12 mm pour 0,05 mm admis.',
        'category': 'PRODUCT', 'severity': 'MAJOR', 'origin': 'INTERNAL',
        'detectedAt': maintenant(-2), 'zone': 'Controle final',
    }, 'NC interne')

    externe = c.poste('/api/v1/nc', {
        'title': 'Reclamation client : bruit anormal en charge',
        'description': 'Le client signale un sifflement au-dela de 80 bar sur trois pompes du lot L-2291.',
        'category': 'PRODUCT', 'severity': 'CRITICAL', 'origin': 'EXTERNAL',
        'detectedAt': maintenant(-1),
    }, 'NC externe (reclamation)')

    ecartee = c.poste('/api/v1/nc', {
        'title': 'Reclamation : rayure sur le carter',
        'description': 'Rayure de 3 cm signalee a la reception du lot L-2288.',
        'category': 'PRODUCT', 'severity': 'MINOR', 'origin': 'EXTERNAL',
        'detectedAt': maintenant(-6),
    }, 'NC externe a rejeter')

    if ecartee:
        c.poste('/api/v1/nc/%s/reject' % ecartee['id'], {
            'reason': 'Rayure constatee apres reception chez le transporteur : '
                      'hors perimetre de garantie produit.',
        }, 'rejet de la reclamation')

    if interne:
        c.poste('/api/v1/nc/%s/start-analysis' % interne['id'],
                {'rootCause': 'Usure du roulement de broche'}, 'NC : passage en analyse')
        c.poste('/api/v1/five-whys', {'ncId': interne['id'],
                                      'problemStatement': 'Jeu radial hors tolerance'},
                'analyse 5 Pourquoi')

    c.poste('/api/v1/capa/cases', {
        'title': 'Remplacer le roulement de broche et revoir le plan de maintenance',
        'type': 'CORRECTIVE', 'criticity': 'HIGH', 'sourceType': 'NON_CONFORMITY',
        'sourceRef': (interne or {}).get('reference', 'NC-DEMO'),
        'ownerId': c.sujet, 'dueDate': jour(21),
        'description': 'Changer le roulement, puis passer la broche en maintenance preventive.',
    }, 'dossier CAPA')

    return interne, externe


def ishikawa(c, nc):
    print('\nIshikawa')
    diagramme = c.poste('/api/v1/ishikawa/diagrams', {
        'problemStatement': 'Jeu radial hors tolerance sur l arbre de sortie',
        'ownerId': c.sujet,
        'ncId': (nc or {}).get('id'),
    }, 'diagramme Ishikawa')
    if diagramme:
        # Les categories sont les 6M, nommees en anglais dans l'enum du domaine.
        for categorie, cause in [
            ('MACHINES', 'Roulement de broche use'),
            ('METHODS', 'Frequence de controle trop espacee'),
            ('MANPOWER', 'Reglage confie sans formation formelle'),
            ('MATERIALS', 'Lot d acier en limite basse de durete'),
            ('MEASUREMENTS', 'Calibre de controle hors validite'),
        ]:
            c.poste('/api/v1/ishikawa/diagrams/%s/causes' % diagramme['id'],
                    {'category': categorie, 'label': cause},
                    'cause %s' % categorie.lower())


def audits(c):
    print('\nPlans d audit')
    plan = c.poste('/api/v1/audits/plans', {
        'title': 'Audit interne ISO 9001 — processus production',
        'type': 'INTERNAL', 'leadAuditorId': c.sujet,
        'scope': 'Processus de realisation, ligne 2',
        'plannedDate': jour(14), 'standard': 'ISO 9001:2015',
    }, 'plan d audit interne')
    if plan:
        # Un constat ne se pose que sur un audit EN COURS : le plan doit d'abord
        # demarrer. La garde est juste, c'est le scenario qui devait l'apprendre.
        c.patche('/api/v1/audits/plans/%s/start' % plan['id'], {}, 'demarrage de l audit')
        c.poste('/api/v1/audits/plans/%s/findings' % plan['id'], {
            'type': 'MINOR_NC', 'clauseRef': '8.5.1', 'raisedBy': c.sujet,
            'description': 'Les enregistrements de controle final ne portent pas le visa du controleur.',
        }, 'constat d audit (NC mineure)')
        c.poste('/api/v1/audits/plans/%s/findings' % plan['id'], {
            'type': 'OPPORTUNITY', 'clauseRef': '7.1.5', 'raisedBy': c.sujet,
            'description': 'Le suivi des calibrations gagnerait a etre affiche au poste.',
        }, 'constat d audit (piste de progres)')


def documents(c):
    print('\nDocuments')
    for code, titre, type_ in [
        ('PRO-QUA-01', 'Procedure de maitrise des non-conformites', 'PROCEDURE'),
        ('POL-QUA-01', 'Politique qualite', 'POLICY'),
        ('MOP-ASS-12', 'Mode operatoire — serrage du carter', 'WORK_INSTRUCTION'),
    ]:
        c.poste('/api/v1/documents', {
            'code': '%s-%s' % (code, SUFFIXE), 'title': titre, 'type': type_,
            'ownerId': c.sujet, 'version': '1.0',
        }, 'document « %s »' % titre[:28])


def fournisseurs(c):
    print('\nFournisseurs')
    for code, nom, type_ in [
        ('FOU-ACIER', 'Acieries de l Est', 'RAW_MATERIAL'),
        ('FOU-JOINT', 'Jointech SAS', 'COMPONENT'),
    ]:
        c.poste('/api/v1/suppliers', {
            'code': '%s-%s' % (code, SUFFIXE), 'name': nom,
            'supplierType': type_, 'createdBy': c.sujet,
            'country': 'FR', 'contactEmail': 'qualite@example.test',
        }, 'fournisseur « %s »' % nom[:26])


def kpis(c):
    print('\nIndicateurs')
    for code, nom, direction, cible in [
        # Les codes suivent le motif impose par l'API : minuscules, tirets.
        ('dpmo', 'Defauts par million d opportunites', 'LOWER_IS_BETTER', 1200),
        ('fpy', 'Taux de conformite au premier passage', 'HIGHER_IS_BETTER', 97),
        ('otd', 'Livraisons a l heure', 'HIGHER_IS_BETTER', 95),
    ]:
        kpi = c.poste('/api/v1/kpis', {
            'code': '%s-%s' % (code, SUFFIXE), 'name': nom,
            'direction': direction, 'createdBy': c.sujet,
            'unit': '%' if direction == 'HIGHER_IS_BETTER' else 'ppm',
            'targetValue': cible,
        }, 'KPI %s' % code)
        if kpi:
            # Un KPI nait en brouillon : il porte des mesures une fois active. La
            # garde est juste -- mesurer un indicateur qu'on n'a pas encore arrete
            # produirait une serie qu'il faudrait jeter.
            c.poste('/api/v1/kpis/%s/activate' % kpi['id'], {}, '  activation %s' % code)
            # Une mesure couvre une PERIODE : c'est ce qui permet de comparer des
            # mois entre eux plutot que des instants isoles.
            for debut, fin, valeur in [(-90, -60, cible * 0.9),
                                       (-60, -30, cible * 0.95),
                                       (-30, 0, cible)]:
                c.poste('/api/v1/kpis/%s/measurements' % kpi['id'], {
                    'value': round(valeur, 2),
                    'periodStart': maintenant(debut), 'periodEnd': maintenant(fin),
                    'recordedByUserId': c.sujet,
                }, '  mesure %s (%+d a %+d j)' % (code, debut, fin))


def cercles_et_idees(c):
    print('\nCercles de qualite et boite a idees')
    cercle = c.poste('/api/v1/circles', {
        'name': 'Cercle qualite — ligne 2',
        'topic': 'Reduction des arrets de ligne',
        'facilitatorId': c.sujet,
    }, 'cercle de qualite')
    for titre in ['Chariot de pieces a hauteur de poste',
                  'Double affichage du couple de serrage']:
        c.poste('/api/v1/ideas', {
            'title': titre,
            'description': 'Proposee en reunion d equipe, a chiffrer.',
            'circleId': (cercle or {}).get('id'),
        }, 'idee « %s »' % titre[:28])


def formation(c):
    print('\nCompetences et parcours')
    for code, nom in [('skl-serrage', 'Serrage au couple'), ('skl-amdec', 'Animation AMDEC')]:
        c.poste('/api/v1/training/skills',
                {'code': '%s-%s' % (code, SUFFIXE), 'name': nom,
                 'description': 'Competence requise sur la ligne 2.'},
                'competence « %s »' % nom)
    c.poste('/api/v1/training/paths', {
        'code': 'par-qual-%s' % SUFFIXE, 'name': 'Parcours qualite operateur',
        'createdBy': c.sujet, 'durationHours': 14,
        'description': 'Prise de poste sur la ligne 2.',
    }, 'parcours de formation')


def equipements(c):
    print('\nEquipements et calibration')
    for code, nom in [('EQP-COUPLE-01', 'Couplemetre numerique'),
                      ('EQP-MICRO-04', 'Micrometre 0-25 mm')]:
        c.poste('/api/v1/calibration/equipment', {
            'code': '%s-%s' % (code, SUFFIXE), 'name': nom, 'createdBy': c.sujet,
            'location': 'Controle final', 'calibrationIntervalMonths': 12,
            'nextCalibrationDate': jour(120),
        }, 'equipement « %s »' % nom[:26])


def ehs(c):
    print('\nEHS')
    c.poste('/api/v1/ehs/incidents', {
        'code': 'EHS-%s' % SUFFIXE, 'title': 'Presque-accident : chute de piece au depalettisage',
        'type': 'NEAR_MISS', 'reportedBy': c.sujet,
        'description': 'Une piece de 12 kg a glisse de la palette, sans blesse.',
        'occurredAt': maintenant(-4),
    }, 'presque-accident EHS')


def changements(c):
    print('\nGestion du changement')
    c.poste('/api/v1/changes', {
        'code': 'CHG-%s' % SUFFIXE,
        'title': 'Passage au joint torique FKM sur la pompe HP-40',
        'type': 'PROCESS', 'requesterUserId': c.sujet,
        'description': 'Le NBR ne tient pas la temperature en service continu.',
    }, 'demande de changement')


def dmaic(c):
    print('\nDMAIC')
    c.poste('/api/v1/dmaic/projects', {
        'title': 'Reduire la variabilite du couple de serrage',
        'blackBeltId': c.sujet,
        'problemStatement': 'Cpk de 0,92 sur le couple, cible 1,33.',
        'goalStatement': 'Atteindre Cpk 1,33 en douze semaines.',
    }, 'projet DMAIC')


def iot(c):
    print('\nParc IoT')
    c.poste('/api/v1/iot/devices', {
        'code': 'IOT-%s' % SUFFIXE, 'name': 'Sonde de temperature — bac de trempe',
        'deviceType': 'SENSOR_TEMPERATURE', 'protocol': 'MQTT',
        'createdBy': c.sujet, 'location': 'Traitement thermique',
    }, 'equipement IoT')


def main():
    analyseur = argparse.ArgumentParser(description=__doc__)
    analyseur.add_argument('--api', default='http://localhost:8082')
    analyseur.add_argument('--keycloak', default='http://localhost:8080')
    analyseur.add_argument('--realm', default='qualitos')
    analyseur.add_argument('--client-id', default='qualitos-web')
    analyseur.add_argument('--utilisateur', default='demo')
    analyseur.add_argument('--secret', default='demo')
    analyseur.add_argument('--langue', default='fr')
    options = analyseur.parse_args()

    print('QualitOS — donnees de demonstration')
    print('API %s, utilisateur %s, suffixe %s' % (options.api, options.utilisateur, SUFFIXE))

    try:
        acces = jeton(options.keycloak, options.realm, options.client_id,
                      options.utilisateur, options.secret)
    except Exception as erreur:                          # noqa: BLE001
        sys.exit('Jeton impossible a obtenir (%s). Keycloak est-il demarre ?' % erreur)

    c = Client(options.api, acces, options.langue)
    debut = time.time()

    modules(c)
    produits_et_amdec(c)
    cycle_pdca(c)
    audits_5s(c)
    interne, _ = non_conformites(c)
    ishikawa(c, interne)
    audits(c)
    documents(c)
    fournisseurs(c)
    kpis(c)
    cercles_et_idees(c)
    formation(c)
    equipements(c)
    ehs(c)
    changements(c)
    dmaic(c)
    iot(c)

    print('\n--- releve ---')
    print('%d creations acceptees, %d refusees, en %.1f s'
          % (len(c.reussites), len(c.echecs), time.time() - debut))
    if c.echecs:
        print('\nCe qui a ete refuse — a lire comme un relevé de fonctionnement :')
        for quoi, code, detail in c.echecs:
            print('  %-46s %s  %s' % (quoi, code, detail))
    return 1 if c.echecs else 0


if __name__ == '__main__':
    sys.exit(main())
