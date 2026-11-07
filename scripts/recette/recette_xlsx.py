# -*- coding: utf-8 -*-
"""Construit le classeur de recette QualitOS.

Neuf onglets, dans l'ordre où on s'en sert : on lit d'abord comment faire, on
prépare l'environnement et les données, on exécute, on relève les anomalies, on
rend compte. Le classeur est l'outil de travail de la campagne — pas un rapport
figé : les statuts sont des listes déroulantes, la synthèse est calculée.
"""

import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from openpyxl import Workbook
from openpyxl.styles import Font, PatternFill, Alignment, Border, Side
from openpyxl.utils import get_column_letter
from openpyxl.worksheet.datavalidation import DataValidation
from openpyxl.formatting.rule import CellIsRule
from openpyxl.drawing.image import Image as XlImage

from recette_socle import (RACINE, SORTIE, LOGO, VERSION, DATE, ENVIRONNEMENT,
                           MARINE, BLEU, BLEU_CLAIR, SIGNAL, PAPIER_FROID,
                           FILET, ENCRE, VERT, ROUGE, AMBRE,
                           COMPTES, JEUX, STATUTS, CRITICITES, TYPES,
                           POLICE_REPLI)
from recette_cas_a import CAS_A
from recette_cas_b import CAS_B, SCENARIOS
from recette_cas_c import CAS_C

CAS = CAS_A + CAS_B + CAS_C

# ------------------------------------------------------------------ styles
F_TITRE = Font(name=POLICE_REPLI, size=20, bold=True, color=MARINE)
F_STITRE = Font(name=POLICE_REPLI, size=12, bold=True, color=BLEU)
F_ENTETE = Font(name=POLICE_REPLI, size=10, bold=True, color="FFFFFF")
F_TXT = Font(name=POLICE_REPLI, size=10, color=ENCRE)
F_PETIT = Font(name=POLICE_REPLI, size=9, color="42536E")
F_GRAS = Font(name=POLICE_REPLI, size=10, bold=True, color=ENCRE)
F_CODE = Font(name="Consolas", size=9, color=MARINE)

R_ENTETE = PatternFill("solid", fgColor=MARINE)
R_BANDE = PatternFill("solid", fgColor=PAPIER_FROID)
R_ACCENT = PatternFill("solid", fgColor=BLEU_CLAIR)
R_SIGNAL = PatternFill("solid", fgColor="FDF0DE")

_t = Side(style="thin", color=FILET)
BORD = Border(left=_t, right=_t, top=_t, bottom=_t)

HAUT = Alignment(vertical="top", wrap_text=True)
HAUT_G = Alignment(vertical="top", wrap_text=True, horizontal="left")
CENTRE = Alignment(vertical="center", horizontal="center", wrap_text=True)


def entete(ws, colonnes, ligne=1):
    """Pose une ligne d'en-tête et fige le volet juste en dessous."""
    for i, (titre, largeur) in enumerate(colonnes, start=1):
        c = ws.cell(row=ligne, column=i, value=titre)
        c.font, c.fill, c.alignment, c.border = F_ENTETE, R_ENTETE, CENTRE, BORD
        ws.column_dimensions[get_column_letter(i)].width = largeur
    ws.row_dimensions[ligne].height = 30
    ws.freeze_panes = ws.cell(row=ligne + 1, column=1)


def remplir(ws, lignes, depart=2, alt=True):
    for j, ligne in enumerate(lignes):
        r = depart + j
        for i, valeur in enumerate(ligne, start=1):
            c = ws.cell(row=r, column=i, value=valeur)
            c.font, c.alignment, c.border = F_TXT, HAUT, BORD
            if alt and j % 2 == 1:
                c.fill = R_BANDE


def bandeau(ws, titre, sous_titre, avec_logo=True):
    """Le bandeau de tête : logo, titre, sous-titre. Même sur chaque onglet long."""
    ws.merge_cells("B2:H2")
    c = ws["B2"]
    c.value, c.font, c.alignment = titre, F_TITRE, Alignment(vertical="center")
    ws.row_dimensions[2].height = 34
    ws.merge_cells("B3:H3")
    c = ws["B3"]
    c.value, c.font, c.alignment = sous_titre, F_PETIT, Alignment(vertical="center")
    ws.column_dimensions["A"].width = 11
    if avec_logo and os.path.exists(LOGO):
        img = XlImage(LOGO)
        img.width = img.height = 62
        ws.add_image(img, "A2")
        ws.row_dimensions[2].height = 34


# =====================================================================
wb = Workbook()

# ------------------------------------------------------------- 1. Lisez-moi
ws = wb.active
ws.title = "Lisez-moi"
ws.sheet_view.showGridLines = False
bandeau(ws, "QualitOS — Cahier de recette",
        "Version %s — %s — %s" % (VERSION, DATE, ENVIRONNEMENT))

TEXTE = [
    ("Pourquoi ce classeur", ""),
    ("", "Avant de proposer QualitOS à un client, il faut pouvoir dire ce qui a été "
         "vérifié, par qui, sur quelles données, et ce qui ne l'a pas été. Ce classeur "
         "est cet état. Il n'est pas une liste de souhaits : chaque ligne est un geste "
         "qu'une personne fait devant l'écran, avec un résultat qu'on peut contredire."),
    ("", ""),
    ("Ce qu'il couvre", ""),
    ("", "Les 11 sections de l'application, leurs %d écrans, les 7 rôles, et les "
         "contrôles transverses qui ne tiennent à aucun écran en particulier : "
         "étanchéité entre clients, langues, accessibilité, performance, mobile et "
         "hors ligne." % 80),
    ("", ""),
    ("Comment on s'en sert", ""),
    ("", "1. Monter l'environnement et charger les jeux de données (onglets "
         "« Environnement » et « Jeux de données »). Tant que les jeux ne sont pas en "
         "place, aucun cas ne se joue : un cas exécuté sur des données approximatives "
         "ne prouve rien."),
    ("", "2. Jouer les cas dans l'ordre de l'onglet « Cas de test », section par "
         "section. Renseigner le statut, le testeur, la date et le lien vers la preuve "
         "(capture, export, enregistrement)."),
    ("", "3. Toute anomalie va dans l'onglet « Anomalies », avec sa reproduction. "
         "Une anomalie sans étapes de reproduction n'est pas une anomalie, c'est une "
         "impression."),
    ("", "4. Jouer les scénarios bout-en-bout en dernier : ils ne testent pas des "
         "écrans mais la promesse du produit, et ils n'ont de sens qu'une fois les "
         "écrans sains."),
    ("", "5. L'onglet « Synthèse » se calcule seul. Il n'y a rien à y saisir."),
    ("", ""),
    ("Échelle de criticité", ""),
    ("", "Bloquant — la fonction ne rend pas le service promis, ou une donnée sort du "
         "périmètre de son client. Aucune mise en production avec un bloquant ouvert."),
    ("", "Majeur — la fonction rend le service mais une partie est fausse, absente ou "
         "pénible au point de changer l'usage. Correction avant la présentation client."),
    ("", "Mineur — gêne, imprécision de libellé, défaut d'ergonomie sans conséquence "
         "sur la donnée. Correction planifiée."),
    ("", ""),
    ("Statuts", ""),
    ("", "À jouer · Conforme · Non conforme · Bloqué (un prérequis manque) · "
         "Sans objet (le module n'est pas souscrit par ce client)."),
    ("", ""),
    ("Deux règles qui valent partout", ""),
    ("", "A. Aucune personne ne s'affiche jamais par un identifiant technique. Un UUID "
         "visible à l'écran est une anomalie majeure, sur n'importe quel écran, même "
         "si le cas de test ne le demande pas."),
    ("", "B. Hors de portée veut dire 404, jamais une liste vide. Une liste vide dit "
         "qu'il n'y a rien ; 404 dit qu'il n'y a rien à voir pour vous. La différence "
         "se lit, et elle renseigne un attaquant."),
    ("", ""),
    ("Critères de sortie", ""),
    ("", "0 bloquant ouvert · 100 % des cas bloquants et majeurs joués · "
         "au moins 95 % de cas conformes · les 5 scénarios bout-en-bout conformes · "
         "chaque anomalie majeure ouverte porte une date de correction engagée."),
]

r = 5
for titre, corps in TEXTE:
    if titre:
        c = ws.cell(row=r, column=2, value=titre)
        c.font = F_STITRE
        r += 1
    if corps:
        ws.merge_cells(start_row=r, start_column=2, end_row=r, end_column=8)
        c = ws.cell(row=r, column=2, value=corps)
        c.font, c.alignment = F_TXT, HAUT_G
        ws.row_dimensions[r].height = max(15, 13 * (len(corps) // 108 + 1))
    r += 1

for col, lg in zip("BCDEFGH", (22, 18, 18, 18, 18, 18, 18)):
    ws.column_dimensions[col].width = lg

# --------------------------------------------------------- 2. Environnement
ws = wb.create_sheet("Environnement")
ws.sheet_view.showGridLines = False
bandeau(ws, "Environnement et prérequis", "À vérifier avant le premier cas", False)
ws.column_dimensions["A"].width = 4
entete(ws, [("Point", 34), ("Attendu", 62), ("Relevé le jour J", 34),
            ("Conforme", 14)], ligne=5)
ENV = [
    ("Adresse de recette", ENVIRONNEMENT, "", ""),
    ("Version recettée", "Étiquette d'image déployée (main-xxxxxxxx) — à relever "
     "avant de commencer et à ne plus changer pendant la campagne", "", ""),
    ("Navigateurs", "Chrome et Edge à jour (poste), Safari iOS et Chrome Android "
     "(terrain). Un défaut n'est retenu que s'il se reproduit sur deux navigateurs.", "", ""),
    ("Comptes", "Les 10 comptes de l'onglet « Comptes » existent, mot de passe connu, "
     "second facteur enrôlé pour demo.admin et demo.qualite", "", ""),
    ("Second client", "« Plasturgie du Centre » existe et contient des données — il ne "
     "sert qu'à prouver l'étanchéité", "", ""),
    ("Modules", "Tous les modules actifs sauf Marketplace, pour que rien ne soit "
     "« sans objet » par accident", "", ""),
    ("Fournisseur d'IA", "Épinglé et joignable ; sinon les cas d'analyses IA sont "
     "« Bloqué », pas « Non conforme »", "", ""),
    ("Capteur IoT", "VIB-204 émet réellement, ou son simulateur tourne", "", ""),
    ("Courriel", "Les envois sortants aboutissent (relais SMTP en 587)", "", ""),
    ("Jeu de données", "Chargé par scripts/donnees-demo.py, puis vérifié ligne à ligne "
     "contre l'onglet « Jeux de données »", "", ""),
    ("Outils du testeur", "De quoi capturer l'écran, mesurer un temps de réponse "
     "(outils de développement), et un lecteur d'écran pour les cas d'accessibilité", "", ""),
    ("Sauvegarde", "Une sauvegarde est prise avant la campagne : certains cas "
     "détruisent des données volontairement", "", ""),
]
remplir(ws, ENV, depart=6)

# ------------------------------------------------------------- 3. Comptes
ws = wb.create_sheet("Comptes")
ws.sheet_view.showGridLines = False
bandeau(ws, "Comptes de recette", "Une personne par rôle — on recette des rôles, "
        "pas un compte tout-puissant", False)
ws.column_dimensions["A"].width = 4
entete(ws, [("Compte", 20), ("Personne", 22), ("Rôle", 26),
            ("Ce qu'il sert à vérifier", 70)], ligne=5)
remplir(ws, COMPTES, depart=6)
r = 6 + len(COMPTES) + 1
ws.merge_cells(start_row=r, start_column=1, end_row=r, end_column=4)
c = ws.cell(row=r, column=1,
            value="Le mot de passe commun est dans le fichier d'identifiants de "
                  "l'équipe, non versionné. Il n'est écrit nulle part dans le dépôt, "
                  "ni dans ce classeur. — Utiliser une fenêtre privée par compte : la "
                  "session Keycloak est partagée par le navigateur, et deux comptes "
                  "ouverts dans la même fenêtre donnent des résultats faux.")
c.font, c.alignment, c.fill = F_PETIT, HAUT_G, R_SIGNAL
ws.row_dimensions[r].height = 46

# ---------------------------------------------------- 4. Jeux de données
ws = wb.create_sheet("Jeux de données")
ws.sheet_view.showGridLines = False
bandeau(ws, "Jeux de données transversaux",
        "Un seul fil rouge industriel traverse toute la campagne", False)
ws.column_dimensions["A"].width = 4
entete(ws, [("Code", 14), ("Objet", 66), ("État au départ", 42),
            ("Domaine", 20)], ligne=5)
remplir(ws, JEUX, depart=6)
for i in range(len(JEUX)):
    ws.cell(row=6 + i, column=1).font = F_CODE
r = 6 + len(JEUX) + 1
ws.merge_cells(start_row=r, start_column=1, end_row=r, end_column=4)
c = ws.cell(row=r, column=1,
            value="Pourquoi un fil rouge et non des données éparpillées : la pompe "
                  "P-204 casse, la NC naît, la CAPA la traite, l'audit la vérifie, la "
                  "carte SPC l'avait vue venir, le MOC la remplace. Des données sans "
                  "lien testeraient les écrans un par un ; un fil rouge teste ce que "
                  "le produit promet, c'est-à-dire que ces objets soient liés.")
c.font, c.alignment, c.fill = F_PETIT, HAUT_G, R_ACCENT
ws.row_dimensions[r].height = 46

# ------------------------------------------------------ 5. Cas de test
ws = wb.create_sheet("Cas de test")
ws.sheet_view.showGridLines = False
COLS = [("ID", 11), ("Domaine", 20), ("Fonctionnalité", 22), ("Scénario", 46),
        ("Type", 16), ("Criticité", 12), ("Compte", 17), ("Préconditions", 32),
        ("Étapes", 54), ("Données", 20), ("Résultat attendu", 58),
        ("Exigence couverte", 24), ("Statut", 15), ("Testeur", 14),
        ("Date", 12), ("Preuve", 22), ("Anomalie", 12)]
entete(ws, COLS, ligne=1)

compteurs = {}
lignes = []
for (dom, fon, sce, typ, cri, cpt, pre, eta, don, att, exi) in CAS:
    cle = "".join(w[0] for w in dom.replace("—", " ").split() if w)[:3].upper()
    compteurs[cle] = compteurs.get(cle, 0) + 1
    ident = "%s-%03d" % (cle, compteurs[cle])
    lignes.append([ident, dom, fon, sce, typ, cri, cpt, pre, eta, don, att, exi,
                   "À jouer", "", "", "", ""])
remplir(ws, lignes, depart=2)

for i in range(len(lignes)):
    r = 2 + i
    ws.cell(row=r, column=1).font = F_CODE
    ws.cell(row=r, column=6).alignment = CENTRE
    ws.cell(row=r, column=13).alignment = CENTRE
    ws.row_dimensions[r].height = 78

ws.auto_filter.ref = "A1:Q%d" % (1 + len(lignes))

dv_statut = DataValidation(type="list", formula1='"%s"' % ",".join(STATUTS),
                           allow_blank=False, showDropDown=False)
dv_crit = DataValidation(type="list", formula1='"%s"' % ",".join(CRITICITES),
                         allow_blank=False, showDropDown=False)
ws.add_data_validation(dv_statut)
ws.add_data_validation(dv_crit)
dv_statut.add("M2:M%d" % (1 + len(lignes)))
dv_crit.add("F2:F%d" % (1 + len(lignes)))

fin = 1 + len(lignes)
ws.conditional_formatting.add(
    "M2:M%d" % fin, CellIsRule(operator="equal", formula=['"Conforme"'],
                               fill=PatternFill("solid", fgColor="DFF0E4"),
                               font=Font(name=POLICE_REPLI, size=10, bold=True, color=VERT)))
ws.conditional_formatting.add(
    "M2:M%d" % fin, CellIsRule(operator="equal", formula=['"Non conforme"'],
                               fill=PatternFill("solid", fgColor="FADBD8"),
                               font=Font(name=POLICE_REPLI, size=10, bold=True, color=ROUGE)))
ws.conditional_formatting.add(
    "M2:M%d" % fin, CellIsRule(operator="equal", formula=['"Bloqué"'],
                               fill=R_SIGNAL,
                               font=Font(name=POLICE_REPLI, size=10, bold=True, color=AMBRE)))
ws.conditional_formatting.add(
    "F2:F%d" % fin, CellIsRule(operator="equal", formula=['"Bloquant"'],
                               font=Font(name=POLICE_REPLI, size=10, bold=True, color=ROUGE)))

# -------------------------------------------------- 6. Scénarios bout-en-bout
ws = wb.create_sheet("Bout-en-bout")
ws.sheet_view.showGridLines = False
bandeau(ws, "Scénarios bout-en-bout",
        "Ils ne testent pas des écrans : ils testent la promesse du produit", False)
ws.column_dimensions["A"].width = 4
entete(ws, [("ID", 11), ("Scénario", 34), ("Pourquoi il compte", 40),
            ("Acteurs", 26), ("Déroulé", 62), ("Attendu", 54),
            ("Criticité", 12), ("Statut", 15), ("Preuve", 22)], ligne=5)
lignes_e2e = [list(s) + ["À jouer", ""] for s in SCENARIOS]
# réordonner : criticité avant statut
lignes_e2e = [[s[0], s[1], s[2], s[3], s[4], s[5], s[6], "À jouer", ""] for s in SCENARIOS]
remplir(ws, lignes_e2e, depart=6)
for i in range(len(lignes_e2e)):
    ws.row_dimensions[6 + i].height = 150
    ws.cell(row=6 + i, column=1).font = F_CODE
    ws.cell(row=6 + i, column=7).alignment = CENTRE
    ws.cell(row=6 + i, column=8).alignment = CENTRE
dv2 = DataValidation(type="list", formula1='"%s"' % ",".join(STATUTS), allow_blank=False)
ws.add_data_validation(dv2)
dv2.add("H6:H%d" % (5 + len(lignes_e2e)))

# ------------------------------------------------------ 7. Couverture
ws = wb.create_sheet("Couverture")
ws.sheet_view.showGridLines = False
bandeau(ws, "Matrice de couverture", "Ce qui est vérifié, par domaine et par type "
        "— et surtout ce qui ne l'est pas", False)
ws.column_dimensions["A"].width = 4
domaines = []
for c in CAS:
    if c[0] not in domaines:
        domaines.append(c[0])
entete(ws, [("Domaine", 26)] + [(t, 13) for t in TYPES] + [("Total", 10)], ligne=5)
for j, dom in enumerate(domaines):
    r = 6 + j
    c = ws.cell(row=r, column=1, value=dom)
    c.font, c.alignment, c.border = F_GRAS, HAUT, BORD
    total = 0
    for i, typ in enumerate(TYPES, start=2):
        n = sum(1 for x in CAS if x[0] == dom and x[3] == typ)
        total += n
        c = ws.cell(row=r, column=i, value=n if n else "—")
        c.font, c.alignment, c.border = F_TXT, CENTRE, BORD
        if n:
            c.fill = R_ACCENT
    c = ws.cell(row=r, column=2 + len(TYPES), value=total)
    c.font, c.alignment, c.border, c.fill = F_GRAS, CENTRE, BORD, R_BANDE
r = 6 + len(domaines)
c = ws.cell(row=r, column=1, value="TOTAL")
c.font, c.alignment, c.border, c.fill = F_ENTETE, CENTRE, BORD, R_ENTETE
for i, typ in enumerate(TYPES, start=2):
    n = sum(1 for x in CAS if x[3] == typ)
    c = ws.cell(row=r, column=i, value=n)
    c.font, c.alignment, c.border, c.fill = F_ENTETE, CENTRE, BORD, R_ENTETE
c = ws.cell(row=r, column=2 + len(TYPES), value=len(CAS))
c.font, c.alignment, c.border, c.fill = F_ENTETE, CENTRE, BORD, R_ENTETE

r += 2
ws.merge_cells(start_row=r, start_column=1, end_row=r, end_column=2 + len(TYPES))
c = ws.cell(row=r, column=1,
            value="Un tiret n'est pas un oubli automatique : certains croisements "
                  "n'ont pas de sens (il n'y a pas de cas « Mobile » pour "
                  "l'administration du client). En revanche, un domaine sans aucun "
                  "cas « Droits » ou « Négatif » doit alerter : ce sont les deux "
                  "colonnes où les défauts coûteux se cachent.")
c.font, c.alignment, c.fill = F_PETIT, HAUT_G, R_SIGNAL
ws.row_dimensions[r].height = 46

# ------------------------------------------------------- 8. Anomalies
ws = wb.create_sheet("Anomalies")
ws.sheet_view.showGridLines = False
entete(ws, [("ID", 10), ("Cas lié", 12), ("Domaine", 20), ("Titre", 44),
            ("Gravité", 12), ("Étapes de reproduction", 58), ("Observé", 40),
            ("Attendu", 40), ("Environnement", 20), ("Preuve", 22),
            ("Ouverte le", 13), ("Statut", 15), ("Correctif / PR", 20),
            ("Vérifiée le", 13)], ligne=1)
for r in range(2, 120):
    for i in range(1, 15):
        c = ws.cell(row=r, column=i)
        c.border, c.font, c.alignment = BORD, F_TXT, HAUT
        if r % 2 == 1:
            c.fill = R_BANDE
    ws.row_dimensions[r].height = 42
dv3 = DataValidation(type="list", formula1='"Bloquant,Majeur,Mineur"', allow_blank=True)
dv4 = DataValidation(type="list",
                     formula1='"Ouverte,En cours,Corrigée,Vérifiée,Refusée,Différée"',
                     allow_blank=True)
ws.add_data_validation(dv3)
ws.add_data_validation(dv4)
dv3.add("E2:E119")
dv4.add("L2:L119")
ws.auto_filter.ref = "A1:N119"

# --------------------------------------------------------- 9. Synthèse
ws = wb.create_sheet("Synthèse")
ws.sheet_view.showGridLines = False
bandeau(ws, "Synthèse de la campagne",
        "Entièrement calculée — ne rien saisir dans cet onglet")
n = 1 + len(CAS)
ws.column_dimensions["B"].width = 34
for col in "CDEFG":
    ws.column_dimensions[col].width = 16

r = 6
c = ws.cell(row=r, column=2, value="Avancement")
c.font = F_STITRE
r += 1
MESURES = [
    ("Cas au total", "=COUNTA('Cas de test'!A2:A%d)" % n),
    ("Joués", "=COUNTA('Cas de test'!A2:A%d)-COUNTIF('Cas de test'!M2:M%d,\"À jouer\")" % (n, n)),
    ("Conformes", "=COUNTIF('Cas de test'!M2:M%d,\"Conforme\")" % n),
    ("Non conformes", "=COUNTIF('Cas de test'!M2:M%d,\"Non conforme\")" % n),
    ("Bloqués", "=COUNTIF('Cas de test'!M2:M%d,\"Bloqué\")" % n),
    ("Sans objet", "=COUNTIF('Cas de test'!M2:M%d,\"Sans objet\")" % n),
    ("Taux de conformité",
     "=IFERROR(COUNTIF('Cas de test'!M2:M%d,\"Conforme\")/"
     "MAX(1;COUNTA('Cas de test'!A2:A%d)-COUNTIF('Cas de test'!M2:M%d;\"Sans objet\"));0)"
     % (n, n, n)),
]
for titre, formule in MESURES:
    c = ws.cell(row=r, column=2, value=titre)
    c.font, c.border = F_TXT, BORD
    c = ws.cell(row=r, column=3, value=formule)
    c.font, c.border, c.alignment = F_GRAS, BORD, CENTRE
    if "Taux" in titre:
        c.number_format = "0.0%"
    r += 1

r += 1
c = ws.cell(row=r, column=2, value="Anomalies ouvertes")
c.font = F_STITRE
r += 1
for titre, formule in [
        ("Bloquantes", "=COUNTIFS(Anomalies!E2:E119,\"Bloquant\";Anomalies!L2:L119;\"<>Vérifiée\")"),
        ("Majeures", "=COUNTIFS(Anomalies!E2:E119,\"Majeur\";Anomalies!L2:L119;\"<>Vérifiée\")"),
        ("Mineures", "=COUNTIFS(Anomalies!E2:E119,\"Mineur\";Anomalies!L2:L119;\"<>Vérifiée\")")]:
    c = ws.cell(row=r, column=2, value=titre)
    c.font, c.border = F_TXT, BORD
    c = ws.cell(row=r, column=3, value=formule)
    c.font, c.border, c.alignment = F_GRAS, BORD, CENTRE
    r += 1

r += 1
c = ws.cell(row=r, column=2, value="Critère de sortie")
c.font = F_STITRE
r += 1
ws.merge_cells(start_row=r, start_column=2, end_row=r, end_column=7)
c = ws.cell(row=r, column=2,
            value="=IF(AND(COUNTIFS(Anomalies!E2:E119,\"Bloquant\";Anomalies!L2:L119;"
                  "\"<>Vérifiée\")=0;COUNTIF('Cas de test'!M2:M%d;\"À jouer\")=0);"
                  "\"PRÊT — aucun bloquant ouvert, tous les cas joués\";"
                  "\"PAS ENCORE — voir les compteurs ci-dessus\")" % n)
c.font, c.alignment, c.fill, c.border = F_GRAS, CENTRE, R_ACCENT, BORD
ws.row_dimensions[r].height = 26

r += 2
c = ws.cell(row=r, column=2, value="Répartition par domaine")
c.font = F_STITRE
r += 1
for titre, lg in (("Domaine", 34), ("Cas", 10), ("Conformes", 14), ("Non conformes", 16)):
    pass
for i, titre in enumerate(("Domaine", "Cas", "Conformes", "Non conformes"), start=2):
    c = ws.cell(row=r, column=i, value=titre)
    c.font, c.fill, c.alignment, c.border = F_ENTETE, R_ENTETE, CENTRE, BORD
r += 1
for dom in domaines:
    c = ws.cell(row=r, column=2, value=dom)
    c.font, c.border = F_TXT, BORD
    c = ws.cell(row=r, column=3,
                value="=COUNTIF('Cas de test'!B2:B%d,B%d)" % (n, r))
    c.font, c.border, c.alignment = F_TXT, BORD, CENTRE
    c = ws.cell(row=r, column=4,
                value="=COUNTIFS('Cas de test'!B2:B%d,B%d,'Cas de test'!M2:M%d,\"Conforme\")"
                      % (n, r, n))
    c.font, c.border, c.alignment = F_TXT, BORD, CENTRE
    c = ws.cell(row=r, column=5,
                value="=COUNTIFS('Cas de test'!B2:B%d,B%d,'Cas de test'!M2:M%d,\"Non conforme\")"
                      % (n, r, n))
    c.font, c.border, c.alignment = F_TXT, BORD, CENTRE
    r += 1

# ------------------------------------------------- normalisation des formules
# Le format de fichier XLSX separe TOUJOURS les arguments par une virgule ;
# c'est Excel qui affiche un point-virgule selon la locale. Une formule ecrite
# avec des points-virgules dans le fichier s'ouvre en erreur. Les formules
# etant assemblees par concatenation, on normalise ici, une fois, plutot que
# de compter sur la vigilance a chaque ligne.
for feuille in wb.worksheets:
    for rangee in feuille.iter_rows():
        for cellule in rangee:
            v = cellule.value
            if isinstance(v, str) and v.startswith("=") and ";" in v:
                cellule.value = v.replace(";", ",")

# ----------------------------------------------------------------- écriture
if not os.path.isdir(SORTIE):
    os.makedirs(SORTIE)
chemin = os.path.join(SORTIE, "QualitOS-cahier-de-recette-v%s.xlsx" % VERSION)
wb.save(chemin)
print("classeur ecrit : %s" % chemin)
print("  %d cas de test, %d scenarios bout-en-bout, %d domaines, %d jeux de donnees"
      % (len(CAS), len(SCENARIOS), len(domaines), len(JEUX)))
