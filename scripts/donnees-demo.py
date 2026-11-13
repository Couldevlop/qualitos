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

Les modules de palier STANDARD/PRO doivent etre actives avant usage, et ce droit
appartient a l'administration du client : sous un compte de pilotage qualite, les
appels d'activation repondent 403 -- c'est l'autorisation qui fonctionne, pas un
defaut. En local, on peut poser les lignes une fois pour toutes dans
`tenant_module_activations`, en respectant l'ordre des dependances du catalogue
(`ModuleCatalog`) : risk depend de capa, controlplan de risk et product, change de
docs, supplier de capa et audit.

Le script est REJOUABLE : les codes portent un suffixe tiré de l'horodatage, donc
un second passage crée un second jeu au lieu d'échouer sur des doublons.
"""
import argparse
import json
import os
import sys
import time
import unicodedata
import uuid
from datetime import date, datetime, timedelta, timezone

# A la SECONDE, et non a la minute : deux passages rapproches se heurtaient sinon
# aux codes uniques des documents et des fournisseurs (409), ce qui contredisait la
# promesse de rejouabilite annoncee juste au-dessus.
SUFFIXE = datetime.now().strftime('%m%d%H%M%S')


def maintenant(decalage_jours=0):
    return (datetime.now(timezone.utc) + timedelta(days=decalage_jours)) \
        .strftime('%Y-%m-%dT%H:%M:%SZ')


def jour(decalage_jours=0):
    return (date.today() + timedelta(days=decalage_jours)).isoformat()


# Le client HTTP et l'obtention du jeton vivent dans `qualitos_api` : ce
# script et `lot-nc.py` appellent la meme API, et un client recopie aurait
# fini par diverger -- un en-tete corrige d'un cote, pas de l'autre.
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from qualitos_api import Client, jeton   # noqa: E402

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


def equipe(c):
    """L'equipe qui prend un ecart en charge (D1 du 8D) : obligatoire.

    <p>Une PERSONNE, jamais un texte libre (ADR 0092). Ici, le porteur du jeton
    se nomme lui-meme : ces donnees de demonstration ne connaissent qu'un
    compte, et un nom invente ne designerait personne.
    """
    return [{'accountId': c.sujet, 'displayName': c.nom}]


def non_conformites(c):
    print('\nNon-conformites, analyses et CAPA')
    interne = c.poste('/api/v1/nc', {
        'title': 'Jeu excessif sur l arbre de sortie',
        'description': 'Constat au controle final : jeu radial de 0,12 mm pour 0,05 mm admis.',
        'category': 'PRODUCT', 'severity': 'MAJOR', 'origin': 'INTERNAL',
        'detectedAt': maintenant(-2), 'zone': 'Controle final',
        'teamMembers': equipe(c),
    }, 'NC interne')

    externe = c.poste('/api/v1/nc', {
        'title': 'Reclamation client : bruit anormal en charge',
        'description': 'Le client signale un sifflement au-dela de 80 bar sur trois pompes du lot L-2291.',
        'category': 'PRODUCT', 'severity': 'CRITICAL', 'origin': 'EXTERNAL',
        'detectedAt': maintenant(-1),
        'teamMembers': equipe(c),
    }, 'NC externe (reclamation)')

    ecartee = c.poste('/api/v1/nc', {
        'title': 'Reclamation : rayure sur le carter',
        'description': 'Rayure de 3 cm signalee a la reception du lot L-2288.',
        'category': 'PRODUCT', 'severity': 'MINOR', 'origin': 'EXTERNAL',
        'detectedAt': maintenant(-6),
        'teamMembers': equipe(c),
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
                                      'problem': 'Jeu radial hors tolerance'},
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
        # Un audit ne demarre pas sans checklist, et un constat ne se pose que sur un
        # audit EN COURS. Les deux gardes sont justes : un audit sans questions n'a
        # rien a auditer, et un constat hors execution n'a pas de terrain. C'est le
        # scenario qui devait apprendre l'ordre -- questionner, demarrer, constater.
        items = []
        for question, clause, attendu in [
            ('Les enregistrements de controle final portent-ils le visa du controleur ?',
             '8.5.1', 'Fiches de controle final signees, echantillon de 10 lots'),
            ('Les instruments de mesure utilises sont-ils dans leur validite de calibration ?',
             '7.1.5', 'Etiquettes de calibration et registre des equipements'),
            ('Les non-conformites detectees en ligne sont-elles tracees et traitees ?',
             '8.7', 'Registre des NC et preuves de traitement'),
        ]:
            item = c.poste('/api/v1/audits/plans/%s/checklist' % plan['id'],
                           {'question': question, 'clauseRef': clause,
                            'expectedEvidence': attendu, 'weight': 1,
                            'orderIndex': len(items)},
                           'question de checklist %s' % clause)
            if item:
                items.append(item)
        c.patche('/api/v1/audits/plans/%s/start' % plan['id'], {}, 'demarrage de l audit')
        # Repondre aux questions fait l'execution de l'audit : sans reponses, un plan
        # « en cours » reste une coquille et les ecrans d'avancement n'ont rien a montrer.
        for item, conforme, reponse in zip(items, [False, True, True], [
            'Trois fiches sur dix sans visa, poste 2.',
            'Toutes les etiquettes en cours de validite.',
            'Registre tenu, deux NC tracees et soldees.',
        ]):
            c.met('/api/v1/audits/plans/%s/checklist/%s/response' % (plan['id'], item['id']),
                  {'response': reponse, 'conformant': conforme},
                  'reponse a la question %s' % item.get('clauseRef'))
        c.poste('/api/v1/audits/plans/%s/findings' % plan['id'], {
            'type': 'MINOR_NC', 'clauseRef': '8.5.1', 'raisedBy': c.sujet,
            'checklistItemId': items[0]['id'] if items else None,
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
        'type': 'PERMANENT', 'requesterUserId': c.sujet,
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
        'type': 'SENSOR_TEMPERATURE', 'protocol': 'MQTT',
        'site': 'Site principal', 'area': 'Traitement thermique',
        'workCenter': 'Bac de trempe', 'equipment': 'Sonde T-04',
    }, 'equipement IoT')



def parties_prenantes(c):
    """Le registre des parties interessees (ISO 9001 4.2), passe en revue.

    Cinq fiches, choisies pour qu ensemble elles touchent TOUT ce que le module
    sait faire -- et non cinq variantes de la meme. De haut en bas :

    1. Client donneur d ordre : la fiche complete, cotee critique, avec une
       exigence contractuelle maitrisee, preuves a l appui.
    2. Autorite reglementaire : cote reglementaire maximal, exigence opposable
       par un texte, rattachee au document qui la porte.
    3. Fournisseur : l exigence qui n est pas tenue -- on ouvre une action
       depuis sa fiche, ce qui la fait repasser « a reevaluer » et cree le
       dossier CAPA ; la cloturer la rendra maitrisee sans intervention.
    4. Collaborateurs : interne, revue en RETARD (date passee), pour que la
       tuile « revues en retard » et l alerte du registre aient de quoi dire.
    5. Communaute locale : pertinence ECARTEE et justifiee -- le cas qu on
       oublie toujours de tester, et que l auditeur demande en premier.

    Les rattachements visent de VRAIS objets, relus depuis les modules qui les
    portent : risque, ecart, audit, document. C est la connexion transverse du
    cahier des charges, et c est elle qui casse en silence si un module change
    de contrat.
    """
    print('\nParties prenantes')

    # Les cibles de rattachement, relues chez leurs modules. Un module vide ou
    # non active rend simplement None : la fiche se cree quand meme, sans lien.
    def premier(reponse, dans_page=False):
        if not reponse:
            return None
        lignes = reponse.get('content', []) if dans_page else reponse
        return lignes[0]['id'] if lignes else None

    risque = premier(c.lit('/api/v1/risk-register/risks', 'lecture des risques'))
    ecart = premier(c.lit('/api/v1/nc?page=0&size=1', 'lecture des ecarts'), True)
    audit = premier(c.lit('/api/v1/audits/plans?page=0&size=1', 'lecture des audits'), True)
    document = premier(c.lit('/api/v1/documents?page=0&size=1', 'lecture des documents'), True)

    # Les fiches portent leur VRAI nom, sans le suffixe horodate du reste du
    # script : un registre de parties interessees qui afficherait
    # « Client SAS (1105182933) » ne se montre pas. La rejouabilite est tenue
    # autrement -- on relit le registre et on REPREND la fiche qui porte deja ce
    # nom au lieu d en creer une seconde. Un second passage met donc a jour, ce
    # qui est justement le geste attendu sur un registre revu periodiquement.
    def cle(nom):
        # Comparaison SANS accent ni casse : les passages anterieurs du script
        # ecrivaient « Aeronef Systemes SAS ». Comparer les chaines telles
        # quelles creerait un doublon accentue a cote de l ancienne fiche au
        # lieu de la renommer.
        sans = unicodedata.normalize('NFKD', nom.split(' (')[0])
        sans = ''.join(ch for ch in sans if not unicodedata.combining(ch))
        # Les tirets aussi : « DGAC - autorite » et « DGAC — autorite »
        # designent la meme partie interessee, et seule la typographie change.
        for tiret in (u'—', u'–', u'‑'):
            sans = sans.replace(tiret, '-')
        return ' '.join(sans.lower().split())

    def exigence_de(fiche, corps, quoi):
        """Inscrit une exigence, ou REPREND celle qui porte deja ce texte.

        Sans cela, chaque passage du script en ajoutait une copie : apres trois
        passages la fiche en affichait trois identiques, et l action ouverte
        depuis la troisieme creait une troisieme CAPA. Un registre d exigences
        qui grossit a chaque execution n est pas un jeu de demonstration, c est
        un defaut visible a l ecran.

        Le module n expose pas de mise a jour complete d une exigence -- on
        reprend donc celle qui existe telle quelle, et les gestes qui suivent
        (rattachement, action) s appliquent dessus.
        """
        detail = c.lit('/api/v1/stakeholders/%s' % fiche['id'], 'relecture de la fiche')
        for inscrite in (detail or {}).get('requirements', []):
            if cle(inscrite.get('requirement', '')) == cle(corps['requirement']):
                print('  DEJA %-46s reprise' % quoi)
                return inscrite
        return c.poste('/api/v1/stakeholders/%s/requirements' % fiche['id'], corps, quoi)

    deja = {}
    lu = c.lit('/api/v1/stakeholders?page=0&size=200', 'lecture du registre')
    # Le registre se rend en LISTE, la ou d autres modules rendent une page :
    # on accepte les deux plutot que de parier sur l un.
    lignes = lu.get('content', []) if isinstance(lu, dict) else (lu or [])
    for ligne in lignes:
        # Un passage anterieur a pu laisser un nom suffixe : on le reconnait par
        # son debut, et la mise a jour ci-dessous le renomme proprement.
        deja.setdefault(cle(ligne['name']), ligne)

    fiches = []
    for nom, categorie, cote in [
        (u'Aéronef Systèmes SAS', 'CUSTOMERS', 'EXTERNAL'),
        (u'DGAC — autorité de surveillance', 'REGULATORS', 'EXTERNAL'),
        (u'Metalux Traitements de surface', 'SUPPLIERS', 'EXTERNAL'),
        (u'Collaborateurs et représentants du personnel', 'EMPLOYEES', 'INTERNAL'),
        (u'Riverains de la zone industrielle Nord', 'LOCAL_COMMUNITIES', 'EXTERNAL'),
    ]:
        corps = {'name': nom, 'category': categorie, 'side': cote}
        existante = deja.get(cle(nom))
        if existante:
            fiche = c.met('/api/v1/stakeholders/%s' % existante['id'], corps,
                          'fiche reprise %s' % nom[:26]) or existante
        else:
            fiche = c.poste('/api/v1/stakeholders', corps, 'partie prenante %s' % nom[:26])
        fiches.append(fiche)

    client, autorite, fournisseur, personnel, riverains = fiches

    # ---- 1. le client : fiche complete, critique sur les deux axes
    if client:
        c.met('/api/v1/stakeholders/%s' % client['id'], {
            'description': u'Donneur d’ordre aéronautique, 62 % du chiffre d’affaires.',
            'contact': 'qualite.fournisseurs@aeronef-systemes.example',
            'sites': 'Site de Blagnac, site de Nantes',
            'needs': u'Pièces conformes au plan, livrées à date, avec dossier de navigabilité.',
            'requirementsSummary': u'AS9100D, exigences client spécifiques, PPAP à chaque évolution.',
            'relevance': 'RELEVANT',
            'relevanceJustification': u'Premier client en volume ; une défaillance arrête la ligne.',
            'risksAndOpportunities': 'Risque : perte de la qualification fournisseur. '
                                     u'Opportunité : contrat cadre sur la gamme suivante.',
            'qualityImpact': 'HIGH', 'regulatoryImpact': 'HIGH',
            'owner': {'accountId': c.sujet, 'name': u'Pilote qualité'},
            'controls': 'Revue de contrat trimestrielle, AQF, audit fournisseur annuel.',
            'indicators': 'Taux de service, PPM client, retards de livraison',
            'documents': 'Contrat C-2026-114, cahier des charges CDC-AS-07',
            'actions': u'Plan de surveillance renforcé sur la cote critique',
            'nextReviewOn': jour(180),
        }, 'analyse de la fiche client')
        exigence = exigence_de(client, {
            'need': u'Être livré à date, sans rupture de ligne',
            'requirement': u'Taux de service ≥ 98 % mesuré mensuellement, sur référence et quantité.',
            'origin': 'Contrat C-2026-114, article 7',
            'nature': 'CONTRACTUAL',
            'risk': u'Pénalités de retard et perte de la qualification fournisseur',
            'riskLevel': 'HIGH',
            'control': u'Revue hebdomadaire du carnet, stock de sécurité sur les références A',
            'indicator': 'Taux de service mensuel',
            'responsible': u'Directeur des opérations',
            'evidence': u'Relevé mensuel du taux de service, accusés de réception',
            'status': 'CONTROLLED',
        }, 'exigence contractuelle du client')
        if exigence and risque:
            c.met('/api/v1/stakeholders/%s/requirements/%s/link' % (client['id'], exigence['id']),
                  {'link': 'RISK', 'target': risque}, 'exigence rattachee a un risque')
        if exigence and audit:
            c.met('/api/v1/stakeholders/%s/requirements/%s/link' % (client['id'], exigence['id']),
                  {'link': 'AUDIT', 'target': audit}, 'exigence rattachee a un audit')

    # ---- 2. l autorite : l exigence opposable par un texte
    if autorite:
        c.met('/api/v1/stakeholders/%s' % autorite['id'], {
            'description': u'Autorité de surveillance des organismes de production.',
            'needs': u'Conformité au règlement et tenue des enregistrements de navigabilité.',
            'relevance': 'RELEVANT',
            'relevanceJustification': u'Détient l’agrément sans lequel l’activité cesse.',
            'qualityImpact': 'MEDIUM', 'regulatoryImpact': 'HIGH',
            'owner': {'accountId': c.sujet, 'name': u'Pilote qualité'},
            'controls': u'Veille réglementaire mensuelle, audit interne dédié.',
            'indicators': u'Écarts relevés en audit de surveillance',
            'nextReviewOn': jour(90),
        }, 'analyse de la fiche autorite')
        exigence = exigence_de(autorite, {
            'need': u'Pouvoir reconstituer l’historique d’une pièce livrée',
            'requirement': u'Conserver les enregistrements de production 10 ans après livraison.',
            'origin': 'Part 21, sous-partie G',
            'nature': 'REGULATORY',
            'risk': u'Suspension de l’agrément de production',
            'riskLevel': 'HIGH',
            'control': u'Archivage numérique signé, plan de conservation documenté',
            'indicator': u'Taux de dossiers complets à l’archivage',
            'responsible': u'Responsable assurance qualité',
            'evidence': u'Plan de conservation, journal d’archivage',
            'status': 'APPLICABLE',
        }, 'exigence reglementaire de l autorite')
        if exigence and document:
            c.met('/api/v1/stakeholders/%s/requirements/%s/link' % (autorite['id'], exigence['id']),
                  {'link': 'DOCUMENT', 'target': document},
                  'exigence rattachee a un document')

    # ---- 3. le fournisseur : l exigence non tenue, et l action qui s ouvre
    if fournisseur:
        c.met('/api/v1/stakeholders/%s' % fournisseur['id'], {
            'description': u'Sous-traitant de traitement de surface, procédé spécial.',
            'needs': u'Commandes fermes et spécifications stables.',
            'requirementsSummary': u'NADCAP CP, certificat matière 3.1 à chaque lot.',
            'relevance': 'RELEVANT',
            'relevanceJustification': u'Procédé spécial non réalisable en interne.',
            'risksAndOpportunities': u'Risque : rupture de la chaîne de traitement.',
            'qualityImpact': 'HIGH', 'regulatoryImpact': 'MEDIUM',
            'owner': {'accountId': c.sujet, 'name': u'Pilote qualité'},
            'controls': u'Audit fournisseur annuel, contrôle à réception renforcé.',
            'indicators': u'Taux de conformité matière, délai de traitement des NC',
            'nextReviewOn': jour(120),
        }, 'analyse de la fiche fournisseur')
        exigence = exigence_de(fournisseur, {
            'need': u'Disposer de la preuve de conformité du traitement',
            'requirement': u'Fournir un certificat de conformité 3.1 pour chaque lot livré.',
            'origin': 'Cahier des charges CDC-TS-03',
            'nature': 'CONTRACTUAL',
            'risk': u'Lot non libérable, retard de livraison en cascade',
            'riskLevel': 'MEDIUM',
            'control': u'Contrôle documentaire à réception, blocage du lot sans certificat',
            'indicator': u'Taux de lots reçus avec certificat',
            'responsible': u'Responsable achats',
            'evidence': u'Certificats archivés par numéro de lot',
            'status': 'APPLICABLE',
        }, 'exigence du fournisseur')
        if exigence:
            if ecart:
                c.met('/api/v1/stakeholders/%s/requirements/%s/link'
                      % (fournisseur['id'], exigence['id']),
                      {'link': 'NON_CONFORMITY', 'target': ecart},
                      'exigence rattachee a un ecart')
            # L exigence n est pas tenue : on ouvre une action DEPUIS la fiche.
            # Elle repasse « a reevaluer » dans la meme reponse -- c est la regle
            # du module, et c est ce que l ecran doit montrer.
            if exigence.get('capaId'):
                # Une action est deja ouverte sur cette exigence : en reposter
                # une creerait un second dossier CAPA pour le meme ecart.
                print('  DEJA %-46s action en place' % 'action depuis l exigence')
            else:
                c.poste('/api/v1/stakeholders/%s/requirements/%s/capa'
                        % (fournisseur['id'], exigence['id']),
                        {'title': u'Obtenir les certificats 3.1 manquants et sécuriser la réception',
                         'ownerId': c.sujet, 'ownerName': u'Pilote qualité',
                         'dueDate': jour(45)},
                        'action ouverte depuis l exigence')

    # ---- 4. le personnel : interne, et une revue en retard
    if personnel:
        c.met('/api/v1/stakeholders/%s' % personnel['id'], {
            'description': u'Salariés et instances représentatives du personnel.',
            'needs': u'Conditions de travail sûres, compétences entretenues, information.',
            'relevance': 'RELEVANT',
            'relevanceJustification': u'La conformité se tient par les personnes qui l’appliquent.',
            'qualityImpact': 'MEDIUM', 'regulatoryImpact': 'MEDIUM',
            'owner': {'accountId': c.sujet, 'name': u'Pilote qualité'},
            'controls': u'Plan de formation annuel, réunions CSSCT.',
            'indicators': u'Taux de complétion des formations, accidents déclarés',
            # DELIBEREMENT dans le passe : c est ce qui alimente la tuile
            # « revues en retard » et l alerte du registre.
            'nextReviewOn': jour(-30),
        }, 'analyse de la fiche personnel (revue en retard)')
        exigence_de(personnel, {
            'need': u'Travailler sur des postes dont les risques sont évalués',
            'requirement': u'Tenir le document unique à jour et le réviser à chaque changement.',
            'origin': 'Code du travail, art. R4121-1',
            'nature': 'REGULATORY',
            'risk': 'Sanction et accident du travail',
            'riskLevel': 'HIGH',
            'control': u'Revue annuelle du DUERP, mise à jour à chaque modification de poste',
            'indicator': u'Date de dernière révision du DUERP',
            'responsible': 'Responsable HSE',
            'evidence': u'DUERP daté et signé',
            'status': 'TO_REASSESS',
        }, 'exigence a reevaluer du personnel')

    # ---- 5. les riverains : la pertinence ECARTEE, justifiee
    if riverains:
        c.met('/api/v1/stakeholders/%s' % riverains['id'], {
            'description': 'Habitants et associations de la zone industrielle Nord.',
            'needs': 'Nuisances sonores et olfactives contenues.',
            'relevance': 'NOT_RELEVANT',
            'relevanceJustification': u'Aucune exigence portant sur le système de management de '
                                      u'la qualité : le sujet est traité par le système '
                                      'environnemental, qui a son propre registre.',
            'qualityImpact': 'LOW', 'regulatoryImpact': 'LOW',
            'owner': {'accountId': c.sujet, 'name': u'Pilote qualité'},
            'nextReviewOn': jour(365),
        }, 'analyse de la fiche riverains (ecartee)')

    # ---- la cotation par la matrice : le MEME calcul, vu autrement
    cotes = {f['id']: {'qualityImpact': q, 'regulatoryImpact': r}
             for f, q, r in zip(fiches,
                                ['HIGH', 'MEDIUM', 'HIGH', 'MEDIUM', 'LOW'],
                                ['HIGH', 'HIGH', 'MEDIUM', 'MEDIUM', 'LOW'])
             if f}
    if cotes:
        c.met('/api/v1/stakeholders/ratings', {'ratings': cotes},
              'cotation par la matrice d evaluation')

    # ---- une revue actee : la fiche client vient d etre revue
    if client:
        c.poste('/api/v1/stakeholders/%s/review' % client['id'], {},
                'revue actee sur la fiche client')


def nc_et_ishikawa(c):
    """Les deux ensemble : l'arete de poisson part de la NC interne creee ici.

    Les separer en deux sections demanderait de retrouver la NC d'un passage
    precedent, et une section doit pouvoir se rejouer seule.
    """
    interne, _ = non_conformites(c)
    ishikawa(c, interne)


# L'ordre de ce dictionnaire EST l'ordre d'execution quand on demande « tout » :
# certaines sections s'appuient sur les activations de modules posees par la
# premiere. Les cles sont les noms que `--sections` accepte.
SECTIONS = {
    'modules': modules,
    'produits-amdec': produits_et_amdec,
    'parties-prenantes': parties_prenantes,
    'pdca': cycle_pdca,
    'audits-5s': audits_5s,
    'nc-ishikawa': nc_et_ishikawa,
    'audits': audits,
    'documents': documents,
    'fournisseurs': fournisseurs,
    'kpis': kpis,
    'cercles-idees': cercles_et_idees,
    'formation': formation,
    'equipements': equipements,
    'ehs': ehs,
    'changements': changements,
    'dmaic': dmaic,
    'iot': iot,
}


def main():
    analyseur = argparse.ArgumentParser(description=__doc__)
    analyseur.add_argument('--api', default='http://localhost:8082')
    analyseur.add_argument('--keycloak', default='http://localhost:8080')
    analyseur.add_argument('--realm', default='qualitos')
    analyseur.add_argument('--client-id', default='qualitos-web')
    analyseur.add_argument('--utilisateur', default='demo')
    analyseur.add_argument('--secret', default='demo')
    analyseur.add_argument('--langue', default='fr')
    analyseur.add_argument(
        '--sections', default='tout',
        help='Sections a peupler, separees par des virgules (defaut : tout). '
             'Utile pour completer un seul module sur une instance deja peuplee, '
             'sans recreer un jeu complet. Noms disponibles : '
             + ', '.join(SECTIONS))
    options = analyseur.parse_args()

    demandees = [n.strip() for n in options.sections.split(',') if n.strip()]
    if demandees == ['tout']:
        demandees = list(SECTIONS)
    inconnues = [n for n in demandees if n not in SECTIONS]
    if inconnues:
        sys.exit('Section inconnue : %s. Disponibles : %s'
                 % (', '.join(inconnues), ', '.join(SECTIONS)))

    print('QualitOS — donnees de demonstration')
    print('API %s, utilisateur %s, suffixe %s' % (options.api, options.utilisateur, SUFFIXE))

    try:
        acces = jeton(options.keycloak, options.realm, options.client_id,
                      options.utilisateur, options.secret)
    except Exception as erreur:                          # noqa: BLE001
        sys.exit('Jeton impossible a obtenir (%s). Keycloak est-il demarre ?' % erreur)

    c = Client(options.api, acces, options.langue)
    debut = time.time()

    print('Sections : %s' % ', '.join(demandees))
    for nom in demandees:
        SECTIONS[nom](c)
    # Un refus ATTENDU n'est pas une anomalie : activer un module est reserve a
    # l'administration du client, et ce script tourne sous un compte de pilotage
    # qualite. Les confondre ferait rendre 1 a chaque passage, et le code de sortie
    # cesserait de signaler quoi que ce soit. On les compte donc a part, sans les
    # cacher : un refus attendu reste affiche, il ne fait simplement pas echouer.
    attendus = [e for e in c.echecs
                if e[0].startswith('activation du module') and e[1] == 403]
    anomalies = [e for e in c.echecs if e not in attendus]

    print('\n--- releve ---')
    print('%d creations acceptees, %d refus attendus, %d anomalies, en %.1f s'
          % (len(c.reussites), len(attendus), len(anomalies), time.time() - debut))
    if attendus:
        print('\nRefus attendus, l autorisation fait son travail :')
        for quoi, code, _ in attendus:
            print('  %-46s %s' % (quoi, code))
    if anomalies:
        print('\nAnomalies, a lire comme un releve de fonctionnement :')
        for quoi, code, detail in anomalies:
            print('  %-46s %s  %s' % (quoi, code, detail))
    return 1 if anomalies else 0


if __name__ == '__main__':
    sys.exit(main())
