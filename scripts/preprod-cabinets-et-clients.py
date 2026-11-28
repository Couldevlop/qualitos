# -*- coding: utf-8 -*-
"""Provisionne deux cabinets de conseil et leurs quatre clients, PAR L'API REELLE.

Pourquoi par l'API et non par du SQL : creer un client, c'est creer une
entreprise, son premier compte administrateur dans Keycloak et ses modules, en
un seul geste transactionnel. Une insertion directe en base poserait les lignes
sans le compte, donc un client dans lequel personne ne peut entrer.

CE QUE LE SCRIPT FAIT
    1. quatre clients, par `POST /api/v1/tenants/onboard` : l'entreprise, son
       compte administrateur et ses modules. L'enrolement rend un mot de passe
       PROVISOIRE, une seule fois ;
    2. deux cabinets, par `POST /api/v1/partner-firms`, avec deux consultants
       chacun ;
    3. deux clients proposes au portefeuille de chaque cabinet. UNE PROPOSITION
       N'ACCORDE RIEN : c'est l'administrateur du client qui accepte, depuis son
       ecran, et c'est tout l'objet du modele.

CE QU'IL NE FAIT PAS, ET POURQUOI
    * il n'accepte pas les engagements a la place des clients : l'acceptation
      est le consentement du client a etre vu par un tiers. La simuler ici
      viderait de son sens la seule garde du modele ;
    * il ne pose pas les mots de passe definitifs : un compte nait avec
      l'obligation d'en changer a la premiere connexion (`UPDATE_PASSWORD`).
      Retirer cette obligation en base transformerait un mot de passe a usage
      unique en mot de passe permanent, et Keycloak met ses utilisateurs en
      cache -- l'effet ne serait meme pas garanti ;
    * il n'injecte aucune donnee metier : c'est `donnees-demo.py` qui le fait,
      client par client, une fois les comptes actifs.

LES MOTS DE PASSE PROVISOIRES NE S'AFFICHENT PAS : ils sont ecrits dans le
fichier passe a `--sortie`, dont le chemin seul est imprime. Un secret affiche
dans un terminal se retrouve dans l'historique, dans les journaux de la session
et dans les captures d'ecran.

Usage (compte de SUPER ADMINISTRATEUR : creer un client et un cabinet lui est
reserve) :

    python scripts/preprod-cabinets-et-clients.py \\
        --api https://preprod.qualitos.openlabconsulting.com \\
        --keycloak https://preprod.qualitos.openlabconsulting.com/auth \\
        --utilisateur superadmin --secret '<mot de passe>' \\
        --sortie D:/tmp/comptes-cabinets.txt

Le script est REJOUABLE : un client ou un cabinet deja present rend 409, et le
script le signale sans s'arreter. Les identifiants sont STABLES a dessein --
rejouer ne doit pas fabriquer un second jeu de doublons.
"""
import argparse
import io
import os
import sys
import time

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from qualitos_api import Client, jeton   # noqa: E402

# Les modules demandes a l'enrolement. Le moteur rend le sort de CHACUN --
# ouvert, ou refuse avec sa raison -- et le script le recopie : un module refuse
# parce que le palier ne le porte pas n'est pas une panne, c'est une regle de
# facturation qui fonctionne.
MODULES = ['capa', 'docs', 'audit', 'risk', 'kpi', 'standards', 'product',
           'controlplan', 'supplier', 'training', 'calibration', 'ehs',
           'change', 'complaints', 'circle', 'dmaic', 'ishikawa', 'fives',
           'pdca', 'itsm']

# Deux cabinets, deux clients chacun. Les secteurs diffèrent a dessein : un
# cabinet qui suivrait quatre fois la meme industrie ne montrerait pas que les
# referentiels et les indicateurs s'adaptent au domaine.
CABINETS = [
    {
        'slug': 'boreal-qualite',
        'name': 'Boréal Qualité Conseil',
        'legalName': 'Boréal Qualité Conseil SAS',
        'legalForm': 'SAS',
        'city': 'Lyon',
        'countryCode': 'FR',
        'contactEmail': 'contact@boreal-qualite.qualitos.local',
        'consultants': [
            ('Hélène Dubreuil', 'helene.dubreuil@boreal-qualite.qualitos.local'),
            ('Marc Tissier', 'marc.tissier@boreal-qualite.qualitos.local'),
        ],
        'clients': [
            {
                'slug': 'fonderie-du-rhone',
                'name': 'Fonderie du Rhône',
                'admin': ('admin@fonderie-du-rhone.qualitos.local', 'Sophie', 'Marchand'),
                'purpose': "Accompagnement ISO 9001 et reprise du plan de surveillance fonderie",
            },
            {
                'slug': 'clinique-saint-martin',
                'name': 'Clinique Saint-Martin',
                'admin': ('admin@clinique-saint-martin.qualitos.local', 'Pierre', 'Alvès'),
                'purpose': "Certification HAS : gestion documentaire et déclaration des événements indésirables",
            },
        ],
    },
    {
        'slug': 'meridien-conseil',
        'name': 'Méridien Conseil & Audit',
        'legalName': 'Méridien Conseil & Audit SARL',
        'legalForm': 'SARL',
        'city': 'Nantes',
        'countryCode': 'FR',
        'contactEmail': 'contact@meridien-conseil.qualitos.local',
        'consultants': [
            ('Nadia Benali', 'nadia.benali@meridien-conseil.qualitos.local'),
            ('Thomas Lefèvre', 'thomas.lefevre@meridien-conseil.qualitos.local'),
        ],
        'clients': [
            {
                'slug': 'laiterie-des-causses',
                'name': 'Laiterie des Causses',
                'admin': ('admin@laiterie-des-causses.qualitos.local', 'Claire', 'Fournier'),
                'purpose': "HACCP et IFS Food : maîtrise des non-conformités et traçabilité des lots",
            },
            {
                'slug': 'banque-cephee',
                'name': 'Banque Céphée',
                'admin': ('admin@banque-cephee.qualitos.local', 'Olivier', 'Rameau'),
                'purpose': "Qualité de service et réclamations clients : dispositif de pilotage",
            },
        ],
    },
]


def enrole(c, client, journal):
    """Cree un client et son administrateur. Rend (tenant_id, deja_present)."""
    reponse = c.poste('/api/v1/tenants/onboard', {
        'name': client['name'],
        'slug': client['slug'],
        'modules': MODULES,
        'admin': {
            'email': client['admin'][0],
            'firstName': client['admin'][1],
            'lastName': client['admin'][2],
        },
    }, 'client « %s »' % client['name'])
    if reponse is None:
        # 409 inclus : rejouer ne doit pas s'arreter sur un client deja la. On
        # le retrouve par la liste, pour que la suite du script travaille quand
        # meme sur son portefeuille.
        existants = c.lit('/api/v1/tenants?size=100', '  relecture des clients', silencieux=True)
        for t in ((existants or {}).get('content') or []):
            if t.get('slug') == client['slug']:
                print('  deja present, on reprend son identifiant')
                return t.get('id'), True
        return None, False

    tenant = (reponse.get('tenant') or {})
    provisoire = reponse.get('temporaryPassword')
    if provisoire:
        journal.append((client['name'], client['admin'][0], provisoire))
    elif reponse.get('invitationSent'):
        print('  invitation envoyee par courriel : aucun mot de passe a noter')
    else:
        print('  ATTENTION : ni mot de passe ni invitation — ce compte sera '
              'inutilisable')

    for m in (reponse.get('modules') or []):
        if not m.get('activated'):
            print('  module refuse : %-14s %s' % (m.get('code'), m.get('message')))
    return tenant.get('id'), False


def cabinet(c, fiche):
    """Cree le cabinet et ses consultants. Rend son identifiant."""
    corps = {k: fiche[k] for k in
             ('slug', 'name', 'legalName', 'legalForm', 'city', 'countryCode',
              'contactEmail')}
    reponse = c.poste('/api/v1/partner-firms', corps, 'cabinet « %s »' % fiche['name'])
    if reponse is None:
        page = c.lit('/api/v1/partner-firms?size=100', '  relecture des cabinets',
                     silencieux=True)
        for f in ((page or {}).get('content') or []):
            if f.get('slug') == fiche['slug']:
                print('  deja present, on reprend son identifiant')
                return f.get('id')
        return None
    firm_id = reponse.get('id')
    for nom, courriel in fiche['consultants']:
        c.poste('/api/v1/partner-firms/%s/consultants' % firm_id,
                {'displayName': nom, 'email': courriel},
                '  consultant %s' % nom)
    return firm_id


def main():
    a = argparse.ArgumentParser(description=__doc__)
    a.add_argument('--api', default='http://localhost:8080')
    a.add_argument('--keycloak', default='http://localhost:8080')
    a.add_argument('--realm', default='qualitos')
    a.add_argument('--client-id', default='qualitos-web')
    a.add_argument('--utilisateur', default='superadmin')
    a.add_argument('--secret', required=True)
    a.add_argument('--langue', default='fr')
    a.add_argument('--sortie', required=True,
                   help='Fichier ou ecrire les mots de passe provisoires. '
                        'Ils ne sont JAMAIS imprimes a l ecran.')
    o = a.parse_args()

    print('QualitOS — deux cabinets, quatre clients')
    print('API %s, utilisateur %s' % (o.api, o.utilisateur))

    try:
        acces = jeton(o.keycloak, o.realm, o.client_id, o.utilisateur, o.secret)
    except Exception as erreur:                           # noqa: BLE001
        sys.exit('Jeton impossible a obtenir (%s). Compte de super '
                 'administrateur attendu : creer un client et un cabinet lui '
                 'est reserve.' % erreur)

    c = Client(o.api, acces, o.langue)
    debut = time.time()
    journal = []
    engagements = []

    for fiche in CABINETS:
        print('\nCabinet %s' % fiche['name'])
        firm_id = cabinet(c, fiche)

        for client in fiche['clients']:
            print('\nClient %s' % client['name'])
            tenant_id, _ = enrole(c, client, journal)
            if not (firm_id and tenant_id):
                continue
            # INTERVENE et non READ : un cabinet qu'on appelle pour redresser
            # une certification doit pouvoir ecrire. La portee reste ce que le
            # client accordera -- il peut n'accorder que la lecture.
            prop = c.poste('/api/v1/partner-firms/%s/portfolio' % firm_id, {
                'tenantId': tenant_id,
                'scope': 'INTERVENE',
                'purpose': client['purpose'],
            }, '  proposition au portefeuille')
            if prop:
                engagements.append((fiche['name'], client['name'], prop.get('id')))

    if journal:
        with io.open(o.sortie, 'w', encoding='utf-8') as f:
            f.write('# Mots de passe PROVISOIRES des administrateurs de client.\n')
            f.write('# Chacun doit etre change a la premiere connexion.\n')
            f.write('# A supprimer des que les quatre connexions sont faites.\n\n')
            for nom, courriel, mdp in journal:
                f.write('%-28s %-52s %s\n' % (nom, courriel, mdp))
        try:
            os.chmod(o.sortie, 0o600)
        except OSError:
            pass
        print('\n%d mot(s) de passe provisoire(s) ecrit(s) dans %s'
              % (len(journal), os.path.abspath(o.sortie)))

    if engagements:
        print('\nEngagements proposes, EN ATTENTE D ACCEPTATION par le client :')
        for cab, cli, eid in engagements:
            print('  %-26s -> %-26s %s' % (cab, cli, eid))
        print('\nL administrateur de chaque client accepte depuis son ecran, ou '
              'par POST /api/v1/partner-access/{engagementId}/grant.')

    print('\nReussites %d, refus %d, en %.1f s'
          % (len(c.reussites), len(c.echecs), time.time() - debut))
    if c.echecs:
        print('\nRefus :')
        for quoi, code, detail in c.echecs:
            print('  %-46s %s  %s' % (quoi, code, detail))
    return 1 if c.echecs else 0


if __name__ == '__main__':
    sys.exit(main())
