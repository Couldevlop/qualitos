# -*- coding: utf-8 -*-
"""La suite du lot : formation, cercles, PDCA, 5S, audits, KPI, fournisseurs.

Séparé de `lot-modules.py` pour une raison de lecture, pas de technique : un
seul fichier de mille lignes où l'on cherche « où sont les audits » n'aide
personne. Les deux parts se lancent ensemble (`lot-modules.py` les appelle) et
partagent le même client HTTP, donc le même relevé de refus.
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
# Formation & compétences.
# ---------------------------------------------------------------------------

COMPETENCES = [
    ('tournage-cn', 'Tournage sur commande numérique', 'Usinage',
     'Régler et conduire un tour à commande numérique en autonomie.'),
    ('controle-dimensionnel', 'Contrôle dimensionnel', 'Qualité',
     'Mesurer une cote au micromètre et au comparateur, interpréter une tolérance.'),
    ('montage-hydraulique', 'Montage hydraulique', 'Assemblage',
     'Monter joints et roulements selon la gamme, au couple prescrit.'),
    ('essai-pression', 'Essai en pression', 'Qualité',
     'Conduire un essai d étanchéité et statuer sur la conformité.'),
    ('audit-interne-9001', 'Audit interne ISO 9001', 'Qualité',
     'Préparer, conduire et restituer un audit interne selon ISO 19011.'),
    ('amdec-processus', 'AMDEC processus', 'Qualité',
     'Animer une AMDEC processus et coter gravité, occurrence et détection.'),
    ('securite-machine', 'Sécurité machine', 'EHS',
     'Appliquer les consignes de consignation et les protections machine.'),
]

PARCOURS = [
    ('operateur-usinage', 'Parcours opérateur usinage', 'Opérateur usinage', 35, 70, 24,
     "Prise de poste sur les machines d'usinage, du débit à la rectification.",
     [('tournage-cn', 3), ('controle-dimensionnel', 2), ('securite-machine', 2)]),
    ('monteur-pompe', 'Parcours monteur pompe', 'Monteur', 21, 70, 24,
     "Montage et essai des pompes série 400.",
     [('montage-hydraulique', 3), ('essai-pression', 2), ('securite-machine', 2)]),
    ('auditeur-interne', 'Parcours auditeur interne', 'Auditeur', 14, 80, 36,
     "Former les auditeurs internes du système de management.",
     [('audit-interne-9001', 3), ('amdec-processus', 2)]),
]

EVALUATIONS = [
    ('tournage-cn', 3, 'MANAGER', 400),
    ('controle-dimensionnel', 4, 'CERTIFICATION', 300),
    ('montage-hydraulique', 2, 'MANAGER', 180),
    ('essai-pression', 3, 'TRAINING', 160),
    ('audit-interne-9001', 4, 'CERTIFICATION', 540),
    ('amdec-processus', 2, 'SELF', 90),
    ('securite-machine', 3, 'TRAINING', 60),
]


def formation(c):
    """Déclare les compétences, les parcours et les évaluations."""
    page = c.lit('/api/v1/training/skills?page=0&size=200', 'compétences existantes',
                 silencieux=True) or {}
    competences = {v['code']: v['id'] for v in page.get('content', [])}
    for code, nom, categorie, description in COMPETENCES:
        if code in competences:
            continue
        vue = c.poste('/api/v1/training/skills', {
            'code': code, 'name': nom, 'description': description, 'category': categorie,
        }, 'compétence %s' % code)
        if vue:
            competences[code] = vue['id']

    for code, nom, role, heures, note, validite, description, exigences in PARCOURS:
        vue = c.poste('/api/v1/training/paths', {
            'code': code, 'name': nom, 'description': description, 'targetRole': role,
            'durationHours': heures, 'passingScore': note, 'validityMonths': validite,
            'createdBy': c.sujet,
        }, 'parcours %s' % code)
        if not vue:
            continue
        for competence, niveau in exigences:
            if competence in competences:
                c.poste('/api/v1/training/paths/%s/requirements' % vue['id'], {
                    'skillId': competences[competence], 'targetLevel': niveau,
                }, 'exigence %s / %s' % (code, competence))
        c.poste('/api/v1/training/paths/%s/activate' % vue['id'], None,
                'activation parcours %s' % code)

    for competence, niveau, source, validite_jours in EVALUATIONS:
        if competence not in competences:
            continue
        c.poste('/api/v1/training/competencies/assess', {
            'userId': c.sujet, 'userName': c.nom, 'skillId': competences[competence],
            'level': niveau, 'source': source, 'assessedBy': c.sujet,
            'expiresOn': jour(validite_jours),
        }, 'évaluation %s' % competence)
    return competences


# ---------------------------------------------------------------------------
# Cercles de qualité.
# ---------------------------------------------------------------------------

CERCLES = [
    ('Cercle ligne 2 — montage pompes', 'Réduction des reprises au montage',
     "Les monteurs de la ligne 2 examinent chaque semaine les reprises de la semaine passée.",
     [('Reprises de montage : où perd-on du temps ?',
       'Relevé des reprises, classement par cause, choix de deux pistes.', -21, 90,
       'Salle qualité'),
      ('Essai de l outil de pose conique',
       "Retour des monteurs après deux semaines d'essai de l'outil.", -7, 60, 'Ligne 2')],
     [('Outil de pose conique pour le joint torique',
       "Un outil qui guide le joint sans le marquer, à fabriquer en interne."),
      ('Bac de rangement des roulements au poste',
       "Éviter de poser les roulements sur l'établi où ils ramassent la poussière.")]),
    ('Cercle usinage — tenue des cotes', 'Stabilité dimensionnelle sur le tour 3',
     "Opérateurs et régleurs du secteur usinage.",
     [('Dérive de cote en fin de série',
       'Analyse des relevés des trois dernières séries.', -14, 75, 'Atelier usinage')],
     [('Contrôle toutes les 20 pièces plutôt qu en fin de lot',
       "Détecter la dérive avant d'avoir produit tout le lot.")]),
    ('Cercle peinture — aspect', "Réduction des retouches d'aspect",
     "Équipe de la cabine de peinture.",
     [('Coulures : viscosité ou pistolet ?',
       'Comparaison des relevés de viscosité et des réglages de pistolet.', -5, 60,
       'Cabine peinture')],
     [('Contrôle de viscosité à la prise de poste',
       "Une mesure à la coupe AFNOR, notée sur la fiche de démarrage.")]),
]


def cercles(c):
    """Crée les cercles, leurs réunions et leurs propositions."""
    crees = []
    for nom, sujet, description, reunions, propositions in CERCLES:
        vue = c.poste('/api/v1/circles', {
            'name': nom, 'description': description, 'topic': sujet,
        }, 'cercle %s' % nom[:28])
        if not vue:
            continue
        cid = vue['id']
        crees.append(cid)
        c.poste('/api/v1/circles/%s/members' % cid,
                {'userId': c.sujet, 'role': 'FACILITATOR'}, 'animateur %s' % nom[:20])
        premiere = None
        for titre, ordre_du_jour, il_y_a, duree, lieu in reunions:
            r = c.poste('/api/v1/circles/%s/meetings' % cid, {
                'title': titre, 'agenda': ordre_du_jour, 'scheduledAt': instant(il_y_a, 14),
                'durationMinutes': duree, 'location': lieu,
            }, 'réunion %s' % titre[:26])
            if r and premiere is None:
                premiere = r['id']
        for titre, description_proposition in propositions:
            c.poste('/api/v1/circles/%s/proposals' % cid, {
                'title': titre, 'description': description_proposition,
                'meetingId': premiere,
            }, 'proposition %s' % titre[:26])
    return crees


# ---------------------------------------------------------------------------
# PDCA — les cycles qui portent les plans d'action.
# ---------------------------------------------------------------------------

CYCLES = [
    ("Réduire les reprises au montage de 30 %",
     "Les reprises au montage représentent 4,2 % des heures de la ligne 2.",
     [('Relever les reprises par cause sur quatre semaines', 'PLAN', 'DONE', -40),
      ("Concevoir et fabriquer l'outil de pose conique", 'DO', 'DONE', -22),
      ('Mesurer les reprises sur les deux séries suivantes', 'CHECK', 'IN_PROGRESS', 12),
      ('Généraliser aux autres postes de montage', 'ACT', 'PENDING', 45)]),
    ("Stabiliser la cote de portée sur le tour 3",
     "Dérive dimensionnelle constatée en fin de série sur l'arbre 400.",
     [('Analyser les relevés des trois dernières séries', 'PLAN', 'DONE', -30),
      ('Passer la broche en maintenance préventive', 'DO', 'IN_PROGRESS', 8),
      ('Vérifier la capabilité sur trois séries', 'CHECK', 'PENDING', 35),
      ('Inscrire la fréquence de contrôle au plan de surveillance', 'ACT', 'PENDING', 60)]),
    ("Supprimer les coulures de peinture",
     "Sept carters retouchés en un mois pour coulures d'aspect.",
     [('Relever viscosité et réglages pendant deux semaines', 'PLAN', 'DONE', -18),
      ('Ajouter le contrôle de viscosité à la fiche de démarrage', 'DO', 'DONE', -6),
      ('Compter les retouches sur deux mois', 'CHECK', 'IN_PROGRESS', 30),
      ('Étendre la fiche aux autres teintes', 'ACT', 'PENDING', 70)]),
    ("Préparer l'audit de surveillance ISO 9001",
     "Audit de surveillance prévu dans trois mois ; écarts du cycle précédent à solder.",
     [('Dresser la liste des écarts restants', 'PLAN', 'DONE', -12),
      ('Solder les actions correctives ouvertes', 'DO', 'IN_PROGRESS', 25),
      ('Audit blanc interne', 'CHECK', 'PENDING', 55),
      ('Revue de direction avant audit', 'ACT', 'PENDING', 80)]),
]


def pdca(c):
    """Ouvre des cycles PDCA et leurs étapes, à des avancements différents."""
    crees = []
    for titre, description, etapes in CYCLES:
        vue = c.poste('/api/v1/pdca/cycles', {
            'title': titre, 'description': description, 'ownerId': c.sujet,
        }, 'cycle PDCA %s' % titre[:26])
        if not vue:
            continue
        crees.append(vue['id'])
        for libelle, phase, etat, echeance in etapes:
            c.poste('/api/v1/pdca/cycles/%s/steps' % vue['id'], {
                'title': libelle, 'description': None, 'phase': phase, 'status': etat,
                'assigneeId': c.sujet, 'dueDate': jour(echeance),
            }, 'étape %s %s' % (phase, libelle[:22]))
    return crees


# ---------------------------------------------------------------------------
# Audits 5S.
# ---------------------------------------------------------------------------

AUDITS_5S = [
    ('Atelier usinage', "Audit mensuel du secteur usinage.", -20,
     {'SEIRI': 7, 'SEITON': 6, 'SEISO': 5, 'SEIKETSU': 7, 'SHITSUKE': 6},
     {'SEISO': 'Copeaux au pied du tour 3 en fin de poste.',
      'SEITON': 'Outils de contrôle sans emplacement défini.'}),
    ('Ligne 2 — montage', "Audit mensuel de la ligne de montage.", -13,
     {'SEIRI': 8, 'SEITON': 8, 'SEISO': 7, 'SEIKETSU': 7, 'SHITSUKE': 8},
     {'SEISO': "Plan de travail propre, rien à signaler."}),
    ('Cabine peinture', "Audit du poste peinture.", -6,
     {'SEIRI': 6, 'SEITON': 5, 'SEISO': 4, 'SEIKETSU': 5, 'SHITSUKE': 5},
     {'SEISO': 'Projections anciennes sur les parois, filtres encrassés.',
      'SEIKETSU': "Pas d'affichage du standard de nettoyage au poste."}),
    ('Magasin pièces', "Audit trimestriel du magasin.", -3,
     {'SEIRI': 9, 'SEITON': 8, 'SEISO': 8, 'SEIKETSU': 8, 'SHITSUKE': 7}, {}),
    ('Zone expédition', "Audit après le presque-accident de palette.", -1,
     {'SEIRI': 6, 'SEITON': 6, 'SEISO': 7, 'SEIKETSU': 6, 'SHITSUKE': 5},
     {'SEITON': "Zone de circulation encombrée par les palettes en attente."}),
]


def cinq_s(c):
    """Programme des audits 5S et les note pilier par pilier."""
    crees = []
    for zone, description, il_y_a, notes, remarques in AUDITS_5S:
        vue = c.poste('/api/v1/fives/audits', {
            'zone': zone, 'description': description, 'auditorId': c.sujet,
            'scheduledAt': instant(il_y_a, 10),
        }, 'audit 5S %s' % zone)
        if not vue:
            continue
        aid = vue['id']
        crees.append(aid)
        for pilier, note in notes.items():
            c.met('/api/v1/fives/audits/%s/score' % aid, {
                'pillar': pilier, 'score': note, 'note': remarques.get(pilier),
                'photoUrl': None,
            }, 'note 5S %s %s' % (zone[:16], pilier))
    return crees


# ---------------------------------------------------------------------------
# Audits internes et constats.
# ---------------------------------------------------------------------------

PLANS_AUDIT = [
    ('Audit interne ISO 9001 — processus de production', 'INTERNAL', 'ISO 9001:2015',
     "Processus de production, du lancement d'ordre à la libération.", -25,
     [('La gamme de fabrication est-elle disponible au poste ?', '8.5.1',
       'Gamme affichée ou consultable au poste', 3),
      ("Les contrôles prévus au plan de surveillance sont-ils enregistrés ?", '8.5.1',
       'Fiches de relevés remplies et signées', 5),
      ("Les moyens de mesure utilisés sont-ils étalonnés et identifiés ?", '7.1.5',
       "Étiquette d'étalonnage valide sur chaque moyen", 5),
      ("Le personnel est-il habilité aux opérations qu'il réalise ?", '7.2',
       'Matrice de compétences à jour', 4)],
     [('MINOR_NC', "Deux relevés de contrôle du lot L-2291 ne sont pas signés.", '8.5.1'),
      ('OBSERVATION', "L'étiquette d'étalonnage du pied à coulisse est peu lisible.", '7.1.5'),
      ('CONFORMITY', "Les gammes sont disponibles et à jour à tous les postes visités.", '8.5.1')]),
    ('Audit interne ISO 9001 — maîtrise des documents', 'INTERNAL', 'ISO 9001:2015',
     'Gestion documentaire du système de management.', -10,
     [("Les documents en vigueur sont-ils identifiables de leur version périmée ?", '7.5.3',
       'Indice de révision et date sur chaque document', 5),
      ("Les documents obsolètes sont-ils retirés des postes ?", '7.5.3',
       'Aucun exemplaire périmé en circulation', 4)],
     [('MAJOR_NC', "Une instruction de montage en indice B subsistait au poste alors que "
       "l'indice C est en vigueur depuis six semaines.", '7.5.3'),
      ('OPPORTUNITY', "La consultation pourrait se faire sur tablette au poste.", '7.5.3')]),
    ('Audit fournisseur — fonderie Beaulieu', 'SUPPLIER', 'ISO 9001:2015',
     'Audit du fournisseur de bruts de fonderie.', -45,
     [("Le fournisseur maîtrise-t-il ses coulées (composition, dureté) ?", '8.4',
       'Certificats matière par coulée', 5)],
     [('MINOR_NC', "Les certificats matière ne mentionnent pas la dureté relevée.", '8.4')]),
]


def audits(c):
    """Programme des audits internes, leur grille et leurs constats.

    <p>Relançable : un audit dont le titre existe déjà est laissé tel quel.
    Deux audits du même nom à deux jours d'intervalle ne racontent rien et
    faussent les compteurs de l'écran.
    """
    page = c.lit('/api/v1/audits/plans?page=0&size=200', 'audits existants',
                 silencieux=True) or {}
    deja = {v['title'] for v in page.get('content', [])}
    crees = []
    for titre, nature, norme, perimetre, il_y_a, questions, constats in PLANS_AUDIT:
        if titre in deja:
            continue
        vue = c.poste('/api/v1/audits/plans', {
            'title': titre, 'scope': perimetre, 'type': nature, 'standard': norme,
            'leadAuditorId': c.sujet, 'leadAuditorName': c.nom, 'auditeeId': None,
            'scheduledDate': jour(il_y_a), 'reminderEmail': None,
        }, 'audit %s' % titre[:30])
        if not vue:
            continue
        aid = vue['id']
        crees.append(aid)
        lignes = []
        for rang, (question, clause, preuve, poids) in enumerate(questions):
            ligne = c.poste('/api/v1/audits/plans/%s/checklist' % aid, {
                'question': question, 'clauseRef': clause, 'expectedEvidence': preuve,
                'weight': poids, 'orderIndex': rang,
            }, 'question audit %s #%d' % (titre[:18], rang + 1))
            if ligne:
                lignes.append(ligne['id'])
        # Un constat ne se lève que sur un audit EN COURS : le module refuse
        # d'enregistrer ce qu'on aurait vu avant d'avoir commencé à regarder.
        c.patche('/api/v1/audits/plans/%s/start' % aid, None,
                 'ouverture audit %s' % titre[:22])
        for nature_constat, description, clause in constats:
            c.poste('/api/v1/audits/plans/%s/findings' % aid, {
                'type': nature_constat, 'description': description, 'clauseRef': clause,
                'photoUrl': None, 'checklistItemId': None, 'capaId': None,
            }, 'constat %s %s' % (nature_constat, titre[:16]))
        # Un audit ne se clôt pas tant qu'une question reste sans réponse : la
        # grille est le compte rendu, pas un pense-bête.
        for rang, ligne in enumerate(lignes):
            c.met('/api/v1/audits/plans/%s/checklist/%s/response' % (aid, ligne), {
                'response': 'Vérifié sur place, preuves présentées.' if rang else
                            'Vérifié sur place.',
                'conformant': not (rang == 1 and il_y_a <= -25),
            }, 'réponse audit %s #%d' % (titre[:16], rang + 1))
        if il_y_a <= -25:
            # Les audits anciens sont clos ; les récents restent en cours, pour
            # que l'écran montre les deux situations.
            c.patche('/api/v1/audits/plans/%s/complete' % aid, {
                'conclusion': 'Système conforme dans son ensemble ; écarts tracés en CAPA.',
            }, 'clôture audit %s' % titre[:22])
    return crees


# ---------------------------------------------------------------------------
# Indicateurs.
# ---------------------------------------------------------------------------

INDICATEURS = [
    ('taux-nc-production', 'Taux de non-conformités en production', 'Qualité', '%',
     'LOWER_IS_BETTER', 'MONTHLY', 2.0, 2.5, 4.0,
     "Nombre d'écarts ouverts rapporté aux ordres de fabrication clos sur le mois.",
     [(-150, 3.4), (-120, 3.1), (-90, 2.9), (-60, 2.6), (-30, 2.2), (0, 2.0)]),
    ('reprises-montage', 'Part des heures de reprise au montage', 'Production', '%',
     'LOWER_IS_BETTER', 'MONTHLY', 2.0, 3.0, 4.5,
     'Heures de reprise rapportées aux heures de montage.',
     [(-150, 4.2), (-120, 4.0), (-90, 3.6), (-60, 3.0), (-30, 2.6), (0, 2.4)]),
    ('delai-cloture-capa', 'Délai moyen de clôture CAPA', 'Qualité', 'jours',
     'LOWER_IS_BETTER', 'MONTHLY', 30, 45, 60,
     "Moyenne des délais entre ouverture et clôture des dossiers CAPA.",
     [(-150, 58), (-120, 52), (-90, 47), (-60, 41), (-30, 36), (0, 33)]),
    ('score-5s-moyen', 'Score 5S moyen', 'Production', 'points',
     'HIGHER_IS_BETTER', 'MONTHLY', 40, 32, 25,
     'Moyenne des scores 5S des zones auditées, sur 50.',
     [(-150, 28), (-120, 30), (-90, 31), (-60, 33), (-30, 32), (0, 34)]),
    ('satisfaction-client', 'Satisfaction client', 'Client', '/10',
     'HIGHER_IS_BETTER', 'QUARTERLY', 8.5, 7.5, 6.5,
     'Moyenne des notes de satisfaction relevées à la clôture des réclamations.',
     [(-270, 7.2), (-180, 7.6), (-90, 7.9), (0, 8.1)]),
    ('ponctualite-livraison', 'Ponctualité de livraison (OTIF)', 'Logistique', '%',
     'HIGHER_IS_BETTER', 'MONTHLY', 95, 92, 88,
     'Livraisons complètes et à l heure rapportées au total des livraisons.',
     [(-150, 89), (-120, 91), (-90, 93), (-60, 94), (-30, 95), (0, 96)]),
]


def indicateurs(c):
    """Déclare les indicateurs, les active et dépose leur historique."""
    crees = []
    for (code, nom, categorie, unite, sens, frequence, cible,
         alerte, critique, description, mesures) in INDICATEURS:
        vue = c.poste('/api/v1/kpis', {
            'code': code, 'name': nom, 'description': description, 'category': categorie,
            'unit': unite, 'direction': sens, 'frequency': frequence,
            'targetValue': cible, 'thresholdWarning': alerte, 'thresholdCritical': critique,
            'ownerUserId': c.sujet, 'createdBy': c.sujet,
        }, 'indicateur %s' % code)
        if not vue:
            continue
        kid = vue['id']
        crees.append(kid)
        c.poste('/api/v1/kpis/%s/activate' % kid, None, 'activation %s' % code)
        for il_y_a, valeur in mesures:
            c.poste('/api/v1/kpis/%s/measurements' % kid, {
                'periodStart': instant(il_y_a - 29, 0), 'periodEnd': instant(il_y_a, 23),
                'value': valeur, 'unit': unite, 'source': 'MANUAL',
                'recordedByUserId': c.sujet, 'notes': None,
            }, 'mesure %s %s' % (code, il_y_a))
    return crees


# ---------------------------------------------------------------------------
# Fournisseurs.
# ---------------------------------------------------------------------------

FOURNISSEURS = [
    ('FON-BEAULIEU', 'Fonderie Beaulieu', 'FR', 'qualite@fonderie-beaulieu.test',
     'RAW_MATERIAL',
     [(-45, 78, 0, 1, 3, "Maîtrise des coulées correcte, traçabilité à renforcer.")],
     [('L-7741', "Dureté en limite basse sur la coulée C-7741.", 'MAJOR',
       'FON-GS500', 40)]),
    ('ROUL-MEYER', 'Roulements Meyer', 'DE', 'vertrieb@meyer-roulements.test',
     'COMPONENT',
     [(-120, 92, 0, 0, 2, "Fournisseur régulier, aucun écart majeur depuis deux ans.")],
     []),
    ('TTH-RHONE', 'Traitements thermiques du Rhône', 'FR', 'contact@tth-rhone.test',
     'SERVICE',
     [(-60, 65, 1, 2, 4, "Cycle de revenu non enregistré, four 2 sans suivi de charge.")],
     [('TTH-2291', "Cycle de revenu non conforme au cahier des charges.", 'CRITICAL',
       'ARB-400-02', 120)]),
    ('PEINT-ALLIER', "Peintures de l'Allier", 'FR', 'commande@peintures-allier.test',
     'RAW_MATERIAL', [(-200, 88, 0, 0, 1, "Rien à signaler.")], []),
    ('LOG-TRANSEX', 'Transex Logistique', 'FR', 'exploitation@transex.test',
     'LOGISTICS',
     [(-90, 71, 0, 2, 2, "Deux retards de livraison non signalés au client.")],
     [('EXP-4410', "Livraison incomplète : 4 kits manquants au bordereau.", 'MINOR',
       'KIT-MAINT-400', 4)]),
]


def fournisseurs(c):
    """Référence les fournisseurs, leurs audits et leurs écarts."""
    deja = set()
    page = c.lit('/api/v1/suppliers?page=0&size=200', 'fournisseurs existants',
                 silencieux=True) or {}
    for v in (page.get('content', page) if isinstance(page, dict) else page):
        deja.add(v.get('code'))
    crees = []
    for code, nom, pays, courriel, nature, audits_fournisseur, ecarts in FOURNISSEURS:
        if code in deja:
            continue
        vue = c.poste('/api/v1/suppliers', {
            'code': code, 'name': nom, 'countryCode': pays, 'contactEmail': courriel,
            'supplierType': nature, 'createdBy': c.sujet,
        }, 'fournisseur %s' % code)
        if not vue:
            continue
        sid = vue['id']
        crees.append(sid)
        for il_y_a, note, critiques, majeurs, mineurs, synthese in audits_fournisseur:
            c.poste('/api/v1/suppliers/%s/audits' % sid, {
                'auditedOn': jour(il_y_a), 'score': note, 'auditorUserId': c.sujet,
                'findingsSummary': synthese, 'criticalFindingsCount': critiques,
                'majorFindingsCount': majeurs, 'minorFindingsCount': mineurs,
            }, 'audit fournisseur %s' % code)
        for lot, description, gravite, piece, quantite in ecarts:
            c.poste('/api/v1/suppliers/%s/non-conformities' % sid, {
                'lotReference': lot, 'description': description, 'severity': gravite,
                'detectedOn': jour(-20), 'partNumber': piece, 'partQuantity': quantite,
                'eightDRequested': gravite in ('MAJOR', 'CRITICAL'),
                'eightDReceived': False,
            }, 'écart fournisseur %s' % lot)
    return crees
