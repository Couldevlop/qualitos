# -*- coding: utf-8 -*-
"""Construit le plan de recette QualitOS (document Word).

Le classeur dit QUOI tester. Ce document dit COMMENT la campagne se conduit :
qui fait quoi, dans quel ordre, avec quelles données, et à quelle condition on
déclare que c'est bon. Les deux se lisent ensemble ; aucun ne remplace l'autre.
"""

import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from docx import Document
from docx.shared import Pt, Cm, RGBColor
from docx.enum.text import WD_ALIGN_PARAGRAPH, WD_BREAK
from docx.enum.table import WD_TABLE_ALIGNMENT
from docx.oxml.ns import qn
from docx.oxml import OxmlElement

from recette_socle import (SORTIE, LOGO, VERSION, DATE, ENVIRONNEMENT,
                           MARINE, BLEU, SIGNAL, PAPIER_FROID, FILET,
                           ENCRE, ENCRE_DOUCE, COMPTES, JEUX, POLICE_REPLI)
from recette_cas_a import CAS_A
from recette_cas_b import CAS_B, SCENARIOS
from recette_cas_c import CAS_C
from recette_cas_d import CAS_D

CAS = CAS_A + CAS_B + CAS_C + CAS_D


def rgb(hexa):
    return RGBColor(int(hexa[0:2], 16), int(hexa[2:4], 16), int(hexa[4:6], 16))


C_MARINE, C_BLEU, C_SIGNAL = rgb(MARINE), rgb(BLEU), rgb(ENCRE)
C_ENCRE, C_DOUX = rgb(ENCRE), rgb(ENCRE_DOUCE)
C_ORANGE = rgb(SIGNAL)

doc = Document()

# ------------------------------------------------------------ mise en page
for section in doc.sections:
    section.top_margin = Cm(2.2)
    section.bottom_margin = Cm(2.0)
    section.left_margin = Cm(2.4)
    section.right_margin = Cm(2.4)

styles = doc.styles
n = styles["Normal"]
n.font.name = POLICE_REPLI
n.font.size = Pt(10.5)
n.font.color.rgb = C_ENCRE
n.paragraph_format.space_after = Pt(7)
n.paragraph_format.line_spacing = 1.18
# La police de repli doit aussi être posée côté Asie de l'Est, sinon Word
# substitue sa propre police sur les caractères qu'il juge non latins — dont
# certains signes typographiques français.
n.element.rPr.rFonts.set(qn("w:eastAsia"), POLICE_REPLI)


def filet_bas(paragraphe, couleur, taille=6):
    p = paragraphe._p.get_or_add_pPr()
    bordures = OxmlElement("w:pBdr")
    bas = OxmlElement("w:bottom")
    bas.set(qn("w:val"), "single")
    bas.set(qn("w:sz"), str(taille))
    bas.set(qn("w:space"), "4")
    bas.set(qn("w:color"), couleur)
    bordures.append(bas)
    p.append(bordures)


def fond(cellule, couleur):
    tc = cellule._tc.get_or_add_tcPr()
    sh = OxmlElement("w:shd")
    sh.set(qn("w:val"), "clear")
    sh.set(qn("w:fill"), couleur)
    tc.append(sh)


def titre(texte, niveau=1):
    p = doc.add_paragraph()
    p.paragraph_format.space_before = Pt(18 if niveau == 1 else 12)
    p.paragraph_format.space_after = Pt(6)
    r = p.add_run(texte)
    r.font.name = POLICE_REPLI
    r.font.bold = True
    r.font.size = Pt(16 if niveau == 1 else 12.5)
    r.font.color.rgb = C_MARINE if niveau == 1 else C_BLEU
    if niveau == 1:
        filet_bas(p, FILET)
    return p


def para(texte, gras=False, doux=False, puce=False):
    p = doc.add_paragraph(style="List Bullet" if puce else None)
    r = p.add_run(texte)
    r.font.name = POLICE_REPLI
    r.font.size = Pt(10.5)
    r.font.bold = gras
    r.font.color.rgb = C_DOUX if doux else C_ENCRE
    return p


def encadre(texte):
    """Un bloc qui appelle une décision ou porte un avertissement."""
    t = doc.add_table(rows=1, cols=1)
    t.alignment = WD_TABLE_ALIGNMENT.CENTER
    c = t.cell(0, 0)
    fond(c, "FDF0DE")
    c.text = ""
    p = c.paragraphs[0]
    r = p.add_run(texte)
    r.font.name = POLICE_REPLI
    r.font.size = Pt(10)
    r.font.color.rgb = C_ENCRE
    p.paragraph_format.space_before = Pt(6)
    p.paragraph_format.space_after = Pt(6)
    doc.add_paragraph()
    return t


def tableau(entetes, lignes, largeurs=None):
    t = doc.add_table(rows=1, cols=len(entetes))
    t.style = "Table Grid"
    t.alignment = WD_TABLE_ALIGNMENT.CENTER
    for i, e in enumerate(entetes):
        c = t.rows[0].cells[i]
        fond(c, MARINE)
        c.text = ""
        r = c.paragraphs[0].add_run(e)
        r.font.name = POLICE_REPLI
        r.font.bold = True
        r.font.size = Pt(9)
        r.font.color.rgb = RGBColor(0xFF, 0xFF, 0xFF)
    for j, ligne in enumerate(lignes):
        cells = t.add_row().cells
        for i, v in enumerate(ligne):
            cells[i].text = ""
            r = cells[i].paragraphs[0].add_run(str(v))
            r.font.name = POLICE_REPLI
            r.font.size = Pt(8.5)
            r.font.color.rgb = C_ENCRE
            if j % 2 == 1:
                fond(cells[i], PAPIER_FROID)
    if largeurs:
        for i, lg in enumerate(largeurs):
            for row in t.rows:
                row.cells[i].width = Cm(lg)
    doc.add_paragraph()
    return t


# =====================================================================
# COUVERTURE
# =====================================================================
if os.path.exists(LOGO):
    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p.add_run().add_picture(LOGO, width=Cm(3.6))

p = doc.add_paragraph()
p.alignment = WD_ALIGN_PARAGRAPH.CENTER
p.paragraph_format.space_before = Pt(24)
r = p.add_run("Plan de recette")
r.font.name = POLICE_REPLI
r.font.size = Pt(34)
r.font.bold = True
r.font.color.rgb = C_MARINE

p = doc.add_paragraph()
p.alignment = WD_ALIGN_PARAGRAPH.CENTER
r = p.add_run("QualitOS — plateforme de management de la qualité")
r.font.name = POLICE_REPLI
r.font.size = Pt(14)
r.font.color.rgb = C_BLEU

p = doc.add_paragraph()
p.alignment = WD_ALIGN_PARAGRAPH.CENTER
p.paragraph_format.space_before = Pt(6)
r = p.add_run("Campagne complète — par scénario, par fonctionnalité, "
              "sur jeux de données transversaux")
r.font.name = POLICE_REPLI
r.font.size = Pt(11)
r.font.color.rgb = C_DOUX

doc.add_paragraph()
tableau(["", ""], [
    ["Version du plan", VERSION],
    ["Date", DATE],
    ["Environnement", ENVIRONNEMENT],
    ["Périmètre", "11 sections de navigation, environ 80 écrans, 7 rôles"],
    ["Volume", "%d cas de test, %d scénarios bout-en-bout, %d jeux de données"
     % (len(CAS), len(SCENARIOS), len(JEUX))],
    ["Classeur associé", "QualitOS-cahier-de-recette-v%s.xlsx" % VERSION],
], largeurs=[4.6, 11.4])

p = doc.add_paragraph()
p.alignment = WD_ALIGN_PARAGRAPH.CENTER
p.paragraph_format.space_before = Pt(30)
r = p.add_run("Document interne — préparation à la mise sur le marché")
r.font.name = POLICE_REPLI
r.font.size = Pt(9)
r.font.color.rgb = C_DOUX

doc.add_paragraph().add_run().add_break(WD_BREAK.PAGE)

# =====================================================================
titre("1. Objet et enjeu")
para("Cette campagne existe pour une raison simple : avant de présenter QualitOS "
     "à un client, l'équipe doit pouvoir dire ce qui a été vérifié, par qui, sur "
     "quelles données, et — ce qui compte autant — ce qui ne l'a pas été.")
para("La différence entre une démonstration et une recette tient à cela. Une "
     "démonstration montre ce qui marche. Une recette cherche ce qui ne marche "
     "pas, le consigne, et donne à la direction de quoi décider en connaissance "
     "de cause. Un cahier qui ne trouve rien n'est pas un bon cahier : c'est un "
     "cahier qui n'a pas cherché.")
para("La plateforme promet beaucoup — cinq méthodes, une douzaine de modules "
     "transverses, un référentiel de normes, de l'IA explicable, du multi-client "
     "étanche. Chacune de ces promesses est un engagement que le premier client "
     "vérifiera. Autant les vérifier d'abord.")

titre("2. Périmètre")
titre("2.1 Ce qui est recetté", 2)
para("Les onze sections de la navigation, prises écran par écran, et pour "
     "chacune : le chemin nominal, au moins un cas de refus, les droits selon le "
     "rôle, et la cohérence de la donnée affichée avec celle du reste de la "
     "plateforme.")
para("S'y ajoutent les contrôles qui ne tiennent à aucun écran en particulier et "
     "que personne ne fait spontanément : l'étanchéité entre clients, les six "
     "langues, l'accessibilité au clavier et au lecteur d'écran, les temps de "
     "réponse sur les chemins chauds, le terrain mobile et le mode hors ligne.")
para("Enfin cinq scénarios bout-en-bout. Ils ne testent pas des écrans : ils "
     "testent la promesse du produit, c'est-à-dire que les objets soient "
     "réellement liés entre eux. C'est ce qu'aucun concurrent ne fait, donc c'est "
     "ce qu'il faut pouvoir démontrer sans trembler.")

titre("2.2 Ce qui n'est pas recetté ici", 2)
para("La charge et la tenue en montée en charge, qui relèvent des campagnes k6 "
     "automatisées et non d'une recette manuelle.", puce=True)
para("Le test d'intrusion, qui demande un prestataire et un mandat écrit. La "
     "recette couvre les contrôles d'autorisation, pas la recherche de "
     "vulnérabilités inconnues.", puce=True)
para("La reprise après sinistre et la bascule de site, qui se répètent sur "
     "l'environnement d'exploitation.", puce=True)
para("La console super-administrateur au-delà de ce qu'un client en perçoit : "
     "elle fait l'objet de sa propre campagne.", puce=True)

encadre("À noter — un manque identifié avant même la campagne : la console "
        "super-administrateur ne dispose pas aujourd'hui de piste d'audit "
        "inviolable, alors que les actes qu'elle porte (créer ou suspendre un "
        "client, activer un module, changer un prix) sont les plus sensibles de "
        "la plateforme. C'est le premier point qu'un auditeur ANSSI regardera. "
        "Le chantier est ouvert et ne doit pas être découvert pendant la recette.")

titre("3. Organisation et rôles")
para("On ne recette pas avec un compte tout-puissant. Un administrateur voit "
     "tout, donc ne voit rien des défauts de droits — et les défauts de droits "
     "sont ceux qui coûtent le plus cher, parce qu'ils se découvrent chez le "
     "client. Chaque cas désigne donc le compte avec lequel il se joue.")
tableau(["Rôle dans la campagne", "Responsabilité"], [
    ["Pilote de recette", "Tient le classeur, arbitre les statuts, décide de la "
     "gravité d'une anomalie, prononce les critères de sortie"],
    ["Testeurs métier", "Jouent les cas fonctionnels dans leur domaine de "
     "compétence, avec le compte prescrit"],
    ["Testeur technique", "Joue les cas de sécurité, de performance, "
     "d'intégration et d'accessibilité"],
    ["Développeur de permanence", "Qualifie les anomalies, distingue le défaut "
     "du prérequis manquant, corrige les bloquants dans la journée"],
    ["Direction", "Lit la synthèse, décide de la mise à disposition client au "
     "vu des bloquants restants"],
], largeurs=[5.0, 11.0])

titre("4. Environnement et prérequis")
para("Aucun cas ne se joue tant que l'environnement n'est pas conforme. Un cas "
     "exécuté sur des données approximatives ne prouve rien, et pire : il occupe "
     "une ligne « conforme » qui fera croire à une vérification.")
para("La version recettée est relevée avant de commencer et ne change plus "
     "pendant la campagne. Recetter une cible mouvante revient à ne pas "
     "recetter : un cas joué lundi sur une version et rejoué mercredi sur une "
     "autre ne dit rien de ni l'une ni l'autre.")
para("Le détail des points à vérifier figure dans l'onglet « Environnement » du "
     "classeur. Deux d'entre eux méritent d'être soulignés ici : un second client "
     "doit exister et contenir des données — c'est le seul moyen de prouver "
     "l'étanchéité plutôt que de l'affirmer — et une sauvegarde doit être prise "
     "avant la campagne, car certains cas détruisent des données exprès.")

titre("5. Jeux de données transversaux")
para("Le principe tient en une phrase : un seul fil rouge industriel traverse "
     "toute la campagne.")
para("La pompe P-204 vibre anormalement. Le capteur le voit, la carte de contrôle "
     "lève une règle, une non-conformité naît, un Ishikawa trouve la cause, une "
     "CAPA la traite contre un critère posé à l'avance, l'audit interne rattache "
     "la preuve à la clause ISO 9001, et un MOC finit par remplacer la pompe.")
para("Des données éparpillées testeraient les écrans un par un. Un fil rouge "
     "teste le produit, dont la valeur est justement que ces objets soient liés. "
     "C'est aussi ce qui rend la campagne racontable : à la fin, on ne présente "
     "pas 145 cases cochées, on raconte une histoire industrielle que la "
     "plateforme a su suivre d'un bout à l'autre.")
para("Les %d jeux sont décrits dans l'onglet « Jeux de données » du classeur, "
     "avec leur état de départ. Cet état fait partie du test : une CAPA dont les "
     "actions sont faites mais l'efficacité non jugée n'est pas un hasard, c'est "
     "la condition qui ouvre l'évaluation." % len(JEUX))

titre("6. Méthode d'exécution")
para("Monter l'environnement, charger les jeux, puis les vérifier ligne à ligne "
     "contre le classeur.", puce=True)
para("Jouer les cas section par section, dans l'ordre du classeur. Renseigner le "
     "statut, le testeur, la date, et le lien vers la preuve.", puce=True)
para("Toute anomalie va dans l'onglet dédié avec ses étapes de reproduction. Une "
     "anomalie sans reproduction n'est pas une anomalie : c'est une impression, "
     "et elle sera refermée sans être corrigée.", puce=True)
para("Un défaut n'est retenu que s'il se reproduit sur deux navigateurs — sauf "
     "s'il est précisément propre à l'un d'eux, ce qui se note.", puce=True)
para("Les scénarios bout-en-bout se jouent en dernier. Ils n'ont de sens qu'une "
     "fois les écrans sains : échouer à l'étape 7 parce que l'étape 2 était déjà "
     "cassée n'apprend rien.", puce=True)

titre("7. Échelle de criticité")
tableau(["Niveau", "Définition", "Conséquence"], [
    ["Bloquant", "La fonction ne rend pas le service promis, ou une donnée sort "
     "du périmètre de son client, ou un acte critique s'exécute sans trace.",
     "Aucune mise à disposition client. Correction et contre-essai immédiats."],
    ["Majeur", "La fonction rend le service, mais une partie est fausse, absente "
     "ou pénible au point de changer l'usage.",
     "Correction avant la présentation client."],
    ["Mineur", "Gêne, libellé imprécis, défaut d'ergonomie sans conséquence sur "
     "la donnée.", "Correction planifiée, sans bloquer."],
], largeurs=[2.6, 8.4, 5.0])

para("Deux règles relèvent le niveau quel que soit le cas où le défaut apparaît :")
para("Un identifiant technique affiché à la place d'une personne est une anomalie "
     "majeure, sur n'importe quel écran. Le produit s'adresse à des gens qui se "
     "connaissent par leur nom.", puce=True)
para("Un objet hors de portée qui répond par une liste vide au lieu d'un 404 est "
     "une anomalie bloquante. Une liste vide dit qu'il n'y a rien ; 404 dit qu'il "
     "n'y a rien à voir pour vous. La différence se lit, et elle renseigne.", puce=True)

titre("8. Critères d'entrée et de sortie")
titre("8.1 Entrée", 2)
para("Environnement conforme, jeux de données chargés et vérifiés, version "
     "relevée et gelée, comptes opérationnels avec second facteur enrôlé.")
titre("8.2 Sortie", 2)
para("Aucune anomalie bloquante ouverte.", puce=True)
para("Cent pour cent des cas bloquants et majeurs joués — pas « conformes », "
     "joués : un cas non joué est une zone d'ombre, pas un succès.", puce=True)
para("Au moins quatre-vingt-quinze pour cent de cas conformes, hors « sans "
     "objet ».", puce=True)
para("Les cinq scénarios bout-en-bout conformes.", puce=True)
para("Chaque anomalie majeure encore ouverte porte une date de correction "
     "engagée, pas une intention.", puce=True)
para("L'onglet « Synthèse » du classeur calcule ces critères seul. Il affiche "
     "« PRÊT » ou « PAS ENCORE », et c'est cette cellule qui fait foi en comité.")

titre("9. Gestion des anomalies")
para("Une anomalie bien écrite se corrige dix fois plus vite qu'une anomalie "
     "vague, parce que le développeur n'a pas à deviner. Les quatre champs qui "
     "comptent sont les étapes de reproduction, ce qui a été observé, ce qui "
     "était attendu, et la preuve.")
para("« Ça ne marche pas » n'est pas une anomalie. « Depuis demo.resp-service, "
     "le plan de travail des évaluations affiche 14 lignes alors que ce compte "
     "n'est concerné que par 1 CAPA — capture jointe » en est une, et elle se "
     "corrige le jour même.")
para("Le cycle est : ouverte, en cours, corrigée, vérifiée. Seul le testeur qui "
     "l'a ouverte peut la passer à « vérifiée », et seulement après avoir rejoué "
     "le cas d'origine. Une anomalie fermée par son correcteur n'est pas vérifiée, "
     "elle est espérée.")

titre("10. Planning indicatif")
tableau(["Jalon", "Durée", "Contenu"], [
    ["J0", "0,5 j", "Montage de l'environnement, chargement et vérification des "
     "jeux de données, gel de la version"],
    ["J1 – J2", "2 j", "Pilotage, méthodes qualité, SPC, APQP"],
    ["J3 – J4", "2 j", "Non-conformité, CAPA, fournisseurs, opérations"],
    ["J5", "1 j", "Référentiels, conformité GRC, administration"],
    ["J6", "1 j", "Transverses : sécurité, langues, accessibilité, performance, "
     "mobile et hors ligne"],
    ["J7", "1 j", "Scénarios bout-en-bout"],
    ["J8", "0,5 j", "Contre-essais des corrections, synthèse, comité de décision"],
], largeurs=[2.4, 2.0, 11.6])
para("Soit huit jours ouvrés avec deux testeurs métier et un testeur technique. "
     "Le chiffre suppose que les corrections des bloquants arrivent dans la "
     "journée ; sinon le contre-essai décale d'autant.", doux=True)

titre("11. Risques de la campagne")
tableau(["Risque", "Effet", "Parade"], [
    ["La version bouge pendant la campagne", "Les cas joués ne disent plus rien",
     "Gel de l'étiquette d'image, relevée à J0 et inscrite au classeur"],
    ["Les jeux de données dérivent au fil des cas",
     "Un cas échoue à cause du précédent, pas à cause du produit",
     "Rechargement depuis le script entre deux sections, et sauvegarde initiale"],
    ["Tout est joué avec un compte administrateur",
     "Les défauts de droits restent invisibles jusque chez le client",
     "Chaque cas désigne son compte ; le pilote le vérifie par sondage"],
    ["Un service externe indisponible (IA, SMTP, capteur)",
     "Des cas marqués « non conforme » à tort",
     "Statut « Bloqué », distinct de « Non conforme », prévu dans le classeur"],
    ["La campagne cherche à confirmer que ça marche",
     "Un cahier vert qui ne protège de rien",
     "Les cas de refus et de droits sont obligatoires, pas optionnels ; la "
     "matrice de couverture rend visible un domaine qui n'en aurait aucun"],
], largeurs=[4.2, 5.4, 6.4])

doc.add_paragraph().add_run().add_break(WD_BREAK.PAGE)

titre("Annexe A — Comptes de recette")
tableau(["Compte", "Personne", "Rôle", "Ce qu'il sert à vérifier"],
        [list(c) for c in COMPTES], largeurs=[3.0, 3.2, 3.4, 6.4])
para("Le mot de passe commun est dans le fichier d'identifiants de l'équipe, non "
     "versionné. Il n'apparaît ni dans ce document, ni dans le classeur, ni dans "
     "le dépôt.", doux=True)

titre("Annexe B — Répartition des cas")
domaines = []
for c in CAS:
    if c[0] not in domaines:
        domaines.append(c[0])
lignes = []
for d in domaines:
    sous = [c for c in CAS if c[0] == d]
    lignes.append([d, len(sous),
                   sum(1 for c in sous if c[4] == "Bloquant"),
                   sum(1 for c in sous if c[4] == "Majeur"),
                   sum(1 for c in sous if c[4] == "Mineur")])
lignes.append(["TOTAL", len(CAS),
               sum(1 for c in CAS if c[4] == "Bloquant"),
               sum(1 for c in CAS if c[4] == "Majeur"),
               sum(1 for c in CAS if c[4] == "Mineur")])
tableau(["Domaine", "Cas", "Bloquants", "Majeurs", "Mineurs"], lignes,
        largeurs=[6.6, 2.0, 2.6, 2.4, 2.4])

titre("Annexe C — Scénarios bout-en-bout")
for ident, nom, pourquoi, acteurs, deroule, attendu, crit in SCENARIOS:
    p = para("%s — %s" % (ident, nom), gras=True)
    p.runs[0].font.color.rgb = C_BLEU
    para(pourquoi, doux=True)
    para("Acteurs : %s" % acteurs, doux=True)
    for ligne in deroule.split("\n"):
        para(ligne.strip(), puce=True)
    para("Attendu : %s" % attendu)
    doc.add_paragraph()

if not os.path.isdir(SORTIE):
    os.makedirs(SORTIE)
chemin = os.path.join(SORTIE, "QualitOS-plan-de-recette-v%s.docx" % VERSION)
doc.save(chemin)
print("document ecrit : %s" % chemin)
