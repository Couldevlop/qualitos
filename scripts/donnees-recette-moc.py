# -*- coding: utf-8 -*-
"""Jeu de recette MOC / CAPA / audits / SMI, PAR L'API RÉELLE.

Complète `donnees-demo.py` (dont il reprend le client HTTP et l'obtention du
jeton) : chaque enregistrement est créé sous le compte qui le créerait à
l'écran — le demandeur crée les demandes de changement, chaque titulaire statue
sur sa carte, le manager qualité crée la non-conformité, les audits et le
risque. Les refus sont rapportés, pas masqués.

Prérequis : les comptes de recette existent (voir la fiche de recette
`docs/recette/recette-moc-capa-audits-smi.md`) et les titulaires des quatre
rôles de validation sont nommés dans « Paramètres MOC ».

Usage :

    python scripts/donnees-recette-moc.py \\
        --api https://preprod.example --keycloak https://preprod.example/auth \\
        --secret '<mot de passe commun des comptes de recette>'

Rejouable : un second passage crée un second jeu (nouveaux codes).
"""
import argparse
import importlib.util
import os
import sys

_ICI = os.path.dirname(os.path.abspath(__file__))
_spec = importlib.util.spec_from_file_location('donnees_demo', os.path.join(_ICI, 'donnees-demo.py'))
demo = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(demo)

# Ordre des domaines d'impact, tel que l'API l'expose.
DOMAINES = ['SAFETY', 'HEALTH_ENVIRONMENT', 'QUALITY', 'PRODUCTION',
            'MAINTENANCE', 'COSTS', 'REGULATORY', 'TRAINING']


class Session:
    """Un client HTTP par compte, ouvert à la première utilisation."""

    def __init__(self, options):
        self.options = options
        self.clients = {}

    def __call__(self, compte):
        if compte not in self.clients:
            acces = demo.jeton(self.options.keycloak, self.options.realm, self.options.client_id,
                               compte, self.options.secret)
            self.clients[compte] = demo.Client(self.options.api, acces, self.options.langue)
        return self.clients[compte]

    def bilan(self):
        reussites = sum(len(c.reussites) for c in self.clients.values())
        echecs = [(compte, e) for compte, c in self.clients.items() for e in c.echecs]
        return reussites, echecs


def evaluer(c, demande, niveaux, notes=None):
    for domaine, niveau in zip(DOMAINES, niveaux):
        c.met('/api/v1/changes/%s/impacts/%s' % (demande['id'], domaine),
              {'level': niveau, 'notes': (notes or {}).get(domaine)},
              '  impact %s = %s' % (domaine, niveau))


def carte(detail, libelle):
    for v in (detail or {}).get('validations', []):
        if v.get('roleLabel') == libelle:
            return v
    return None


def demandes_de_changement(s):
    print('\nDemandes de changement (MOC)')
    demandeur = s('demo.demandeur')
    codes = {}

    # 1. La demande de la maquette : 13 points sur 24, soit 54/100, critique.
    p204 = demandeur.poste('/api/v1/changes', {
        'title': 'Remplacement de la pompe P-204 par un modèle à débit supérieur',
        'type': 'PERMANENT', 'site': 'Unité 2 – Production',
        'description': 'La pompe P-204 du circuit de refroidissement est remplacée par un '
                       'modèle de débit nominal 45 m³/h (au lieu de 32 m³/h).',
        'justification': 'Débit insuffisant en été : trois arrêts de ligne sur alarme de '
                         'température depuis juin.',
        'plannedFor': demo.jour(30),
    }, 'MOC pompe P-204 (brouillon)')
    if p204:
        codes['p204'] = p204['code']
        demandeur.poste('/api/v1/changes/%s/analysis' % p204['id'], {}, '  envoi en analyse')
        evaluer(demandeur, p204, ['MEDIUM', 'LOW', 'LOW', 'HIGH', 'MEDIUM', 'MEDIUM', 'NONE', 'MEDIUM'],
                {'PRODUCTION': 'Arrêt de la ligne 2 pendant 6 h pour le remplacement.',
                 'SAFETY': 'Consignation électrique et hydraulique du circuit.'})
        soumise = demandeur.poste('/api/v1/changes/%s/submit' % p204['id'], {}, '  soumission à validation')
        v = carte(soumise, 'Responsable de service')
        if v:
            s('demo.resp-service').poste(
                '/api/v1/changes/%s/validations/%s/decision' % (p204['id'], v['id']),
                {'decision': 'APPROVED', 'comment': 'Conforme au plan de maintenance.'},
                '  Responsable de service : approuvé')

    # 2. En analyse, classée « majeur » (9 points sur 24, soit 38/100), non soumise.
    lubrifiant = demandeur.poste('/api/v1/changes', {
        'title': 'Changement de fournisseur du lubrifiant de la ligne 3',
        'type': 'PERMANENT', 'site': 'Ligne 3 – Usinage',
        'description': 'Passage du lubrifiant de coupe actuel à une référence équivalente '
                       "d'un second fournisseur.",
        'justification': "Rupture d'approvisionnement annoncée par le fournisseur actuel.",
        'plannedFor': demo.jour(45),
    }, 'MOC lubrifiant ligne 3 (brouillon)')
    if lubrifiant:
        codes['lubrifiant'] = lubrifiant['code']
        demandeur.poste('/api/v1/changes/%s/analysis' % lubrifiant['id'], {}, '  envoi en analyse')
        evaluer(demandeur, lubrifiant, ['LOW', 'MEDIUM', 'MEDIUM', 'LOW', 'LOW', 'LOW', 'NONE', 'LOW'])

    # 3. Brouillon temporaire.
    t12 = demandeur.poste('/api/v1/changes', {
        'title': "Contournement temporaire de l'alarme niveau haut du réservoir T-12",
        'type': 'TEMPORARY', 'site': 'Parc de stockage',
        'description': "Shunt de l'alarme LAH-12 pendant le remplacement du capteur de niveau.",
        'justification': 'Capteur défaillant générant des alarmes intempestives.',
        'plannedFor': demo.jour(7),
    }, 'MOC contournement alarme T-12 (brouillon)')
    if t12:
        codes['t12'] = t12['code']

    # 4. Rejetée par HSE, avec commentaire : sert à la réouverture.
    four = demandeur.poste('/api/v1/changes', {
        'title': 'Abaissement de la consigne de température du four F-2',
        'type': 'PERMANENT', 'site': 'Traitement thermique',
        'description': 'Consigne ramenée de 860 °C à 840 °C pour réduire la consommation.',
        'justification': 'Gain énergétique estimé à 6 %.',
        'plannedFor': demo.jour(20),
    }, 'MOC consigne four F-2 (brouillon)')
    if four:
        codes['four'] = four['code']
        demandeur.poste('/api/v1/changes/%s/analysis' % four['id'], {}, '  envoi en analyse')
        evaluer(demandeur, four, ['LOW', 'LOW', 'LOW', 'LOW', 'NONE', 'NONE', 'NONE', 'NONE'])
        soumise = demandeur.poste('/api/v1/changes/%s/submit' % four['id'], {}, '  soumission à validation')
        v = carte(soumise, 'HSE')
        if v:
            s('demo.hse').poste(
                '/api/v1/changes/%s/validations/%s/decision' % (four['id'], v['id']),
                {'decision': 'REJECTED',
                 'comment': "Impact sur la dureté des pièces non évalué : fournir les essais "
                            "métallurgiques avant de resoumettre."},
                '  HSE : rejeté (motivé)')

    # 5. Soumise, toutes cartes en attente : sert au rappel (date de soumission reculée à part).
    flexibles = demandeur.poste('/api/v1/changes', {
        'title': 'Remplacement des flexibles hydrauliques de la presse PR-7',
        'type': 'PERMANENT', 'site': 'Atelier presses',
        'description': 'Flexibles d\'origine remplacés par une référence à tresse inox.',
        'justification': 'Deux fuites en trois mois sur les flexibles actuels.',
        'plannedFor': demo.jour(15),
    }, 'MOC flexibles presse PR-7 (brouillon)')
    if flexibles:
        codes['flexibles'] = flexibles['code']
        demandeur.poste('/api/v1/changes/%s/analysis' % flexibles['id'], {}, '  envoi en analyse')
        evaluer(demandeur, flexibles, ['LOW', 'LOW', 'NONE', 'LOW', 'LOW', 'NONE', 'NONE', 'NONE'])
        demandeur.poste('/api/v1/changes/%s/submit' % flexibles['id'], {}, '  soumission à validation')

    # 6. Brouillon jetable, pour le scénario de suppression.
    jetable = demandeur.poste('/api/v1/changes', {
        'title': 'Brouillon à supprimer — essai de recette',
        'type': 'TEMPORARY', 'site': 'Unité 2 – Production',
        'description': 'Demande créée pour vérifier la suppression d\'un brouillon.',
    }, 'MOC brouillon jetable')
    if jetable:
        codes['jetable'] = jetable['code']
    return codes


def non_conformite(s):
    print('\nNon-conformité ouverte')
    nc = s('demo.qualite').poste('/api/v1/nc', {
        'title': 'Fuite d huile au palier de la pompe P-204',
        'description': 'Suintement constaté au palier côté accouplement lors de la ronde '
                       'du matin ; bac de rétention à moitié plein.',
        'category': 'PROCESS', 'severity': 'MAJOR', 'origin': 'INTERNAL',
        'detectedAt': demo.maintenant(-1), 'zone': 'Unité 2 – Production',
    }, 'NC ouverte')
    return nc


def audits(s, auditeur_id, auditeur_nom):
    print('\nAudits')
    qualite = s('demo.qualite')
    resultats = {}

    def plan(titre, jours):
        return qualite.poste('/api/v1/audits/plans', {
            'title': titre, 'type': 'INTERNAL', 'standard': 'ISO 9001:2015',
            'scope': 'Unité 2 – Production', 'scheduledDate': demo.jour(jours),
            'leadAuditorId': auditeur_id, 'leadAuditorName': auditeur_nom,
        }, 'audit « %s »' % titre[:40])

    def checklist(p):
        items = []
        for question, clause, attendu in [
            ('Les enregistrements de maintenance des pompes sont-ils tenus à jour ?',
             '7.1.3', 'Carnets de maintenance P-201 à P-206'),
            ('Les modifications d installation passent-elles par une demande de changement ?',
             '8.5.6', 'Registre MOC, échantillon de 5 modifications'),
            ('Les non-conformités relevées en ronde sont-elles enregistrées ?',
             '8.7', 'Registre des NC du trimestre'),
        ]:
            item = qualite.poste('/api/v1/audits/plans/%s/checklist' % p['id'],
                                 {'question': question, 'clauseRef': clause,
                                  'expectedEvidence': attendu, 'weight': 1, 'orderIndex': len(items)},
                                 '  question %s' % clause)
            if item:
                items.append(item)
        return items

    # Planifié, non démarré : le constat doit y être refusé.
    planifie = plan('Audit interne — magasin pièces de rechange', 21)
    if planifie:
        checklist(planifie)
        resultats['planifie'] = planifie.get('reference')

    # Démarré : on peut y ajouter un constat, puis « Créer une CAPA » depuis le constat.
    en_cours = plan('Audit interne — maîtrise des modifications, Unité 2', 0)
    if en_cours:
        checklist(en_cours)
        qualite.patche('/api/v1/audits/plans/%s/start' % en_cours['id'], {}, '  démarrage')
        resultats['en_cours'] = en_cours.get('reference')

    # Clôturé avec constats : prêt pour la rédaction et l'émission du rapport.
    clos = plan('Audit interne — maintenance des équipements tournants', -7)
    if clos:
        items = checklist(clos)
        qualite.patche('/api/v1/audits/plans/%s/start' % clos['id'], {}, '  démarrage')
        for item, conforme, reponse in zip(items, [False, True, True], [
            'Deux carnets sans la dernière intervention.',
            'Toutes les modifications échantillonnées ont une demande.',
            'Registre tenu.',
        ]):
            qualite.met('/api/v1/audits/plans/%s/checklist/%s/response' % (clos['id'], item['id']),
                        {'response': reponse, 'conformant': conforme}, '  réponse %s' % item.get('clauseRef'))
        qualite.poste('/api/v1/audits/plans/%s/findings' % clos['id'], {
            'type': 'MINOR_NC', 'clauseRef': '7.1.3',
            'checklistItemId': items[0]['id'] if items else None,
            'description': 'Les carnets de maintenance de P-203 et P-205 ne portent pas la '
                           'dernière intervention.',
        }, '  constat NC mineure')
        qualite.poste('/api/v1/audits/plans/%s/findings' % clos['id'], {
            'type': 'OBSERVATION', 'clauseRef': '8.5.6',
            'description': 'Le registre MOC gagnerait à référencer les bons de travaux associés.',
        }, '  constat observation')
        qualite.patche('/api/v1/audits/plans/%s/complete' % clos['id'],
                       {'reportSummary': 'Maintenance globalement maîtrisée ; une NC mineure sur '
                                         'la traçabilité des interventions.'}, '  clôture')
        resultats['clos'] = clos.get('reference')
    return resultats


def risque(s, proprietaire_id, proprietaire_nom):
    print('\nRisque')
    return s('demo.qualite').poste('/api/v1/risk-register/risks', {
        'title': 'Rupture du circuit de refroidissement de la ligne 2 en période estivale',
        'type': 'HEALTH_SAFETY', 'process': 'Production', 'site': 'Unité 2 – Production',
        'owner': proprietaire_nom, 'ownerUserId': proprietaire_id,
        'cause': 'Débit de la pompe P-204 insuffisant par forte chaleur.',
        'effect': 'Surchauffe, arrêt de ligne, risque de brûlure lors des interventions.',
        'origin': 'DIRECT', 'grossSeverity': 4, 'grossProbability': 3,
        'decision': 'REDUCE', 'status': 'TO_TREAT', 'nextReviewOn': demo.jour(60),
    }, 'risque (propriétaire : %s)' % proprietaire_nom)


def main():
    a = argparse.ArgumentParser(description=__doc__)
    a.add_argument('--api', required=True)
    a.add_argument('--keycloak', required=True)
    a.add_argument('--realm', default='qualitos')
    a.add_argument('--client-id', default='qualitos-web')
    a.add_argument('--secret', required=True, help='mot de passe commun des comptes de recette')
    a.add_argument('--langue', default='fr')
    options = a.parse_args()

    s = Session(options)
    auditeur = s('demo.auditeur').sujet
    hse = s('demo.hse').sujet
    codes = demandes_de_changement(s)
    nc = non_conformite(s)
    plans = audits(s, auditeur, 'Marc Delorme')
    r = risque(s, hse, 'Sophie Lambert')

    print('\n--- jeu créé ---')
    for cle, code in codes.items():
        print('  MOC %-10s %s' % (cle, code))
    print('  NC         %s' % (nc or {}).get('reference'))
    for cle, ref in plans.items():
        print('  Audit %-8s %s' % (cle, ref))
    print('  Risque     %s' % (r or {}).get('reference'))
    reussites, echecs = s.bilan()
    print('\n%d appels acceptés, %d refus' % (reussites, len(echecs)))
    for compte, (quoi, code, detail) in echecs:
        print('  [%s] %-40s %s  %s' % (compte, quoi, code, detail))
    return 1 if echecs else 0


if __name__ == '__main__':
    sys.exit(main())
