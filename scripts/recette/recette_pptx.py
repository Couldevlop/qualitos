# -*- coding: utf-8 -*-
"""Construit les trois présentations de recette QualitOS.

Trois publics, trois questions différentes — et c'est pour cela qu'il y a trois
fichiers et non un seul avec des diapositives à sauter :

  * une direction demande « est-ce que je peux y aller ? » ;
  * un manager demande « qui fait quoi, quand, et comment je le suis ? » ;
  * un ingénieur demande « concrètement, je fais comment, et où sont les
    pièges ? ».

Le dessin reprend celui du site : une feuille de relevé. Les filets bornent, ils
ne décorent pas ; l'orange signale, il ne colore pas.
"""

import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from pptx import Presentation
from pptx.util import Inches, Pt, Emu
from pptx.dml.color import RGBColor
from pptx.enum.text import PP_ALIGN, MSO_ANCHOR
from pptx.enum.shapes import MSO_SHAPE

from recette_socle import (SORTIE, LOGO, VERSION, DATE, ENVIRONNEMENT,
                           MARINE, MARINE_SOMBRE, BLEU, BLEU_CLAIR, SIGNAL,
                           PAPIER_FROID, FILET, ENCRE, ENCRE_DOUCE,
                           COMPTES, JEUX, POLICE_REPLI)
from recette_cas_a import CAS_A
from recette_cas_b import CAS_B, SCENARIOS
from recette_cas_c import CAS_C

CAS = CAS_A + CAS_B + CAS_C
L, H = Inches(13.333), Inches(7.5)


def rgb(h):
    return RGBColor(int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16))


C_MARINE, C_MARINE_S, C_BLEU = rgb(MARINE), rgb(MARINE_SOMBRE), rgb(BLEU)
C_BLEU_CLAIR, C_SIGNAL = rgb(BLEU_CLAIR), rgb(SIGNAL)
C_FROID, C_FILET = rgb(PAPIER_FROID), rgb(FILET)
C_ENCRE, C_DOUX = rgb(ENCRE), rgb(ENCRE_DOUCE)
C_BLANC = RGBColor(0xFF, 0xFF, 0xFF)


def bloc(dia, x, y, w, h, couleur, ligne=None):
    f = dia.shapes.add_shape(MSO_SHAPE.RECTANGLE, x, y, w, h)
    f.fill.solid()
    f.fill.fore_color.rgb = couleur
    if ligne is None:
        f.line.fill.background()
    else:
        f.line.color.rgb = ligne
        f.line.width = Pt(1)
    f.shadow.inherit = False
    return f


def texte(dia, x, y, w, h, contenu, taille=18, gras=False, couleur=None,
          align=PP_ALIGN.LEFT, interligne=1.2, espace=6):
    zone = dia.shapes.add_textbox(x, y, w, h)
    tf = zone.text_frame
    tf.word_wrap = True
    tf.vertical_anchor = MSO_ANCHOR.TOP
    lignes = contenu.split("\n") if isinstance(contenu, str) else contenu
    for i, ligne in enumerate(lignes):
        p = tf.paragraphs[0] if i == 0 else tf.add_paragraph()
        p.alignment = align
        p.line_spacing = interligne
        p.space_after = Pt(espace)
        r = p.add_run()
        r.text = ligne
        r.font.name = POLICE_REPLI
        r.font.size = Pt(taille)
        r.font.bold = gras
        r.font.color.rgb = couleur if couleur is not None else C_ENCRE
    return zone


def puces(dia, x, y, w, h, items, taille=16, couleur=None):
    """Des puces dessinées : un filet court orange, pas un rond générique."""
    zone = dia.shapes.add_textbox(x, y, w, h)
    tf = zone.text_frame
    tf.word_wrap = True
    for i, item in enumerate(items):
        p = tf.paragraphs[0] if i == 0 else tf.add_paragraph()
        p.line_spacing = 1.22
        p.space_after = Pt(11)
        r = p.add_run()
        r.text = "— " + item
        r.font.name = POLICE_REPLI
        r.font.size = Pt(taille)
        r.font.color.rgb = couleur if couleur is not None else C_ENCRE
    return zone


def couverture(prs, surtitre, titre, sous_titre):
    dia = prs.slides.add_slide(prs.slide_layouts[6])
    bloc(dia, 0, 0, L, H, C_MARINE)
    # La trace de mesure du site, réduite à son idée : deux limites et une
    # moyenne. Elle dit le métier sans rien illustrer de faux.
    for y in (Inches(5.55), Inches(6.35)):
        bloc(dia, Inches(0.9), y, Inches(11.5), Pt(1), rgb("2A4A7A"))
    bloc(dia, Inches(0.9), Inches(5.95), Inches(11.5), Pt(1.4), rgb("3C6AA8"))
    bloc(dia, Inches(9.3), Inches(5.92), Pt(11), Pt(11), C_SIGNAL)

    if os.path.exists(LOGO):
        dia.shapes.add_picture(LOGO, Inches(0.9), Inches(0.75),
                               height=Inches(1.05))
    texte(dia, Inches(0.9), Inches(2.15), Inches(11), Inches(0.5), surtitre,
          taille=15, couleur=C_SIGNAL, gras=True)
    texte(dia, Inches(0.9), Inches(2.7), Inches(11.5), Inches(1.8), titre,
          taille=46, gras=True, couleur=C_BLANC, interligne=1.0)
    texte(dia, Inches(0.9), Inches(4.45), Inches(10.5), Inches(0.9), sous_titre,
          taille=17, couleur=rgb("B9CBE6"))
    texte(dia, Inches(0.9), Inches(6.65), Inches(11), Inches(0.4),
          "QualitOS  ·  version du plan %s  ·  %s" % (VERSION, DATE),
          taille=11, couleur=rgb("7E97BC"))
    return dia


def page(prs, titre, surtitre=None):
    dia = prs.slides.add_slide(prs.slide_layouts[6])
    bloc(dia, 0, 0, L, Inches(1.18), C_BLANC)
    bloc(dia, 0, Inches(1.18), L, Pt(1), C_FILET)
    bloc(dia, Inches(0.85), Inches(0.42), Pt(4), Inches(0.52), C_SIGNAL)
    y = Inches(0.52) if surtitre is None else Inches(0.3)
    if surtitre:
        texte(dia, Inches(1.05), Inches(0.26), Inches(10), Inches(0.3),
              surtitre, taille=10.5, couleur=C_DOUX, gras=True)
    texte(dia, Inches(1.05), y if surtitre is None else Inches(0.55),
          Inches(11.4), Inches(0.6), titre, taille=25, gras=True,
          couleur=C_MARINE)
    if os.path.exists(LOGO):
        dia.shapes.add_picture(LOGO, Inches(12.5), Inches(6.72),
                               height=Inches(0.42))
    return dia


def chiffres(dia, valeurs, y=Inches(1.8), hauteur=Inches(1.55)):
    """Une rangée de nombres. Le nombre porte l'information, le libellé l'explique."""
    n = len(valeurs)
    marge, ecart = Inches(0.85), Inches(0.28)
    larg = (L - 2 * marge - ecart * (n - 1)) / n
    for i, (nombre, libelle) in enumerate(valeurs):
        x = marge + i * (larg + ecart)
        bloc(dia, x, y, larg, hauteur, C_FROID, C_FILET)
        texte(dia, x + Inches(0.2), y + Inches(0.17), larg - Inches(0.4),
              Inches(0.7), str(nombre), taille=36, gras=True, couleur=C_MARINE)
        texte(dia, x + Inches(0.2), y + Inches(0.92), larg - Inches(0.4),
              Inches(0.5), libelle, taille=11, couleur=C_DOUX, interligne=1.08)


def tableau(dia, entetes, lignes, x=Inches(0.85), y=Inches(1.75),
            larg=None, taille=11.5, haut_ligne=Inches(0.44)):
    larg = larg or (L - 2 * x)
    n = len(entetes)
    t = dia.shapes.add_table(len(lignes) + 1, n, x, y, larg,
                             haut_ligne * (len(lignes) + 1)).table
    for i, e in enumerate(entetes):
        c = t.cell(0, i)
        c.text = ""
        c.fill.solid()
        c.fill.fore_color.rgb = C_MARINE
        p = c.text_frame.paragraphs[0]
        r = p.add_run()
        r.text = e
        r.font.name, r.font.size, r.font.bold = POLICE_REPLI, Pt(taille), True
        r.font.color.rgb = C_BLANC
    for j, ligne in enumerate(lignes, start=1):
        for i, v in enumerate(ligne):
            c = t.cell(j, i)
            c.text = ""
            c.fill.solid()
            c.fill.fore_color.rgb = C_FROID if j % 2 else C_BLANC
            p = c.text_frame.paragraphs[0]
            r = p.add_run()
            r.text = str(v)
            r.font.name, r.font.size = POLICE_REPLI, Pt(taille)
            r.font.color.rgb = C_ENCRE
    return t


def enonce(dia, phrase, y=Inches(5.75)):
    """La phrase à retenir, posée sur fond clair en pied de diapositive."""
    bloc(dia, Inches(0.85), y, L - Inches(1.7), Inches(1.0), C_BLEU_CLAIR)
    bloc(dia, Inches(0.85), y, Pt(4), Inches(1.0), C_SIGNAL)
    texte(dia, Inches(1.2), y + Inches(0.2), L - Inches(2.4), Inches(0.7),
          phrase, taille=14.5, couleur=C_MARINE, interligne=1.2)


def nouvelle():
    prs = Presentation()
    prs.slide_width, prs.slide_height = L, H
    return prs


# chiffres communs, calculés et non recopiés
NB = len(CAS)
NB_BLOQ = sum(1 for c in CAS if c[5] == "Bloquant")
NB_MAJ = sum(1 for c in CAS if c[5] == "Majeur")
NB_MIN = sum(1 for c in CAS if c[5] == "Mineur")
DOMAINES = []
for c in CAS:
    if c[0] not in DOMAINES:
        DOMAINES.append(c[0])


# =====================================================================
# 1. DIRECTION — directeurs de site, directeurs qualité
# =====================================================================
def deck_direction():
    prs = nouvelle()
    couverture(prs, "RECETTE AVANT MISE SUR LE MARCHÉ",
               "Ce que nous avons vérifié,\net ce que nous n'avons pas vérifié",
               "QualitOS — plateforme de management de la qualité. "
               "Présentation à la direction de site et à la direction qualité.")

    d = page(prs, "Pourquoi une recette, et pas une démonstration de plus")
    texte(d, Inches(0.85), Inches(1.7), Inches(11.6), Inches(1.4),
          "Une démonstration montre ce qui marche. Une recette cherche ce qui ne "
          "marche pas, le consigne, et vous donne de quoi décider.",
          taille=21, couleur=C_MARINE, interligne=1.25)
    puces(d, Inches(0.85), Inches(3.1), Inches(11.6), Inches(2.4), [
        "Le premier client vérifiera nos promesses. Autant les vérifier d'abord, "
        "chez nous, où une anomalie ne coûte qu'une correction.",
        "Un cahier de recette qui ne trouve rien n'est pas un bon cahier : c'est "
        "un cahier qui n'a pas cherché.",
        "Ce qui est présenté ici n'est pas un avis : c'est un compte rendu "
        "d'essais, avec des preuves, et des manques assumés.",
    ], taille=16)
    enonce(d, "La question posée n'est pas « est-ce que ça marche ? » mais "
              "« qu'est-ce qui ne marche pas, et est-ce que je peux vivre avec ? »")

    d = page(prs, "L'étendue de ce qui est mis à l'épreuve")
    chiffres(d, [(NB, "cas de test, écrits et joués un par un"),
                 (len(DOMAINES), "domaines fonctionnels couverts"),
                 ("80", "écrans de l'application"),
                 ("7", "rôles distincts, chacun testé pour lui-même"),
                 (len(SCENARIOS), "parcours bout-en-bout")])
    texte(d, Inches(0.85), Inches(3.65), Inches(11.6), Inches(1.6),
          "L'inventaire n'a pas été écrit de mémoire : il est tiré de la "
          "navigation réelle de l'application. Onze sections, environ quatre-vingts "
          "écrans. Rien n'a été écarté parce qu'on n'y pensait plus.",
          taille=15.5, couleur=C_DOUX, interligne=1.3)
    enonce(d, "Sur les %d cas, %d sont classés bloquants : ce sont ceux qui, "
              "s'ils échouent, interdisent de montrer le produit." % (NB, NB_BLOQ))

    d = page(prs, "Un fil rouge industriel, pas une liste de cases",
             "LA MÉTHODE")
    texte(d, Inches(0.85), Inches(1.65), Inches(11.6), Inches(0.6),
          "La pompe P-204 vibre anormalement. Nous suivons ce qu'il en advient, "
          "d'un bout à l'autre.", taille=16.5, couleur=C_DOUX)
    etapes = [("1", "Le capteur voit la dérive"),
              ("2", "La carte de contrôle lève une règle"),
              ("3", "Une non-conformité naît"),
              ("4", "L'Ishikawa trouve la cause"),
              ("5", "La CAPA la traite"),
              ("6", "L'efficacité est jugée"),
              ("7", "L'audit en fait une preuve")]
    larg, ecart = Inches(1.56), Inches(0.1)
    for i, (num, lib) in enumerate(etapes):
        x = Inches(0.85) + i * (larg + ecart)
        bloc(d, x, Inches(2.5), larg, Inches(1.85), C_FROID, C_FILET)
        bloc(d, x, Inches(2.5), larg, Pt(3), C_SIGNAL if i in (0, 6) else C_BLEU)
        texte(d, x + Inches(0.14), Inches(2.66), larg - Inches(0.28),
              Inches(0.4), num, taille=19, gras=True, couleur=C_BLEU)
        texte(d, x + Inches(0.14), Inches(3.12), larg - Inches(0.28),
              Inches(1.1), lib, taille=11.5, couleur=C_ENCRE, interligne=1.12)
    enonce(d, "Sept systèmes différents, un seul dossier. C'est précisément ce "
              "qu'aucun concurrent ne sait faire — donc c'est ce qu'il faut "
              "pouvoir démontrer sans trembler.", y=Inches(4.75))

    d = page(prs, "Ce que nous n'avons pas vérifié", "HONNÊTETÉ DU PÉRIMÈTRE")
    tableau(d, ["Hors périmètre", "Pourquoi", "Quand"], [
        ["Tenue en charge", "Relève des campagnes automatisées, pas d'une "
         "recette manuelle", "En continu, à chaque livraison"],
        ["Test d'intrusion", "Demande un prestataire et un mandat écrit", "À planifier"],
        ["Reprise après sinistre", "Se répète sur l'environnement d'exploitation",
         "Exercice semestriel"],
        ["Console super-administrateur", "Fait l'objet de sa propre campagne",
         "Lot suivant"],
    ], y=Inches(1.75), taille=12.5, haut_ligne=Inches(0.62))
    bloc(d, Inches(0.85), Inches(4.55), L - Inches(1.7), Inches(1.5), rgb("FDF0DE"))
    bloc(d, Inches(0.85), Inches(4.55), Pt(4), Inches(1.5), C_SIGNAL)
    texte(d, Inches(1.2), Inches(4.75), L - Inches(2.4), Inches(1.2),
          "Un manque identifié avant même la campagne : la console "
          "super-administrateur n'a pas de piste d'audit inviolable, alors "
          "qu'elle porte les actes les plus sensibles — créer ou suspendre un "
          "client, activer un module, changer un prix. C'est le premier point "
          "qu'un auditeur ANSSI regardera. Le chantier est ouvert.",
          taille=13.5, couleur=C_ENCRE, interligne=1.25)

    d = page(prs, "À quelle condition nous disons que c'est bon",
             "CRITÈRES DE SORTIE")
    puces(d, Inches(0.85), Inches(1.75), Inches(11.6), Inches(3.4), [
        "Aucune anomalie bloquante ouverte. Pas « en cours de correction » : fermée "
        "et contre-essayée.",
        "Cent pour cent des cas bloquants et majeurs joués — joués, pas réussis. "
        "Un cas non joué est une zone d'ombre, pas un succès.",
        "Au moins 95 % de cas conformes.",
        "Les %d scénarios bout-en-bout conformes." % len(SCENARIOS),
        "Chaque anomalie majeure encore ouverte porte une date de correction "
        "engagée, pas une intention.",
    ], taille=16)
    enonce(d, "Ces critères sont calculés automatiquement dans le classeur. "
              "La cellule affiche « PRÊT » ou « PAS ENCORE » — et c'est elle qui "
              "fait foi en comité, pas une opinion.")

    d = page(prs, "Ce que nous vous demandons")
    chiffres(d, [("8 j", "de campagne, 3 personnes"),
                 ("J0", "gel de la version recettée"),
                 ("J8", "comité de décision"),
                 ("1", "décision attendue de vous")], hauteur=Inches(1.5))
    texte(d, Inches(0.85), Inches(3.6), Inches(11.6), Inches(1.6),
          "Au terme de la campagne, la direction tranche une seule question : "
          "au vu des anomalies restantes et de leur gravité, présente-t-on le "
          "produit au premier client, ou corrige-t-on d'abord ?",
          taille=17, couleur=C_MARINE, interligne=1.3)
    enonce(d, "Nous ne demandons pas un blanc-seing. Nous demandons une décision "
              "éclairée, et nous fournissons de quoi l'éclairer.")
    return prs


# =====================================================================
# 2. MANAGERS QUALITÉ
# =====================================================================
def deck_managers():
    prs = nouvelle()
    couverture(prs, "CONDUITE DE LA CAMPAGNE",
               "Qui fait quoi, quand,\net comment on le suit",
               "QualitOS — recette complète. Présentation aux managers qualité "
               "et aux pilotes de la campagne.")

    d = page(prs, "La campagne en un coup d'œil")
    chiffres(d, [(NB, "cas à jouer"), (NB_BLOQ, "bloquants"), (NB_MAJ, "majeurs"),
                 (NB_MIN, "mineurs"), ("8 j", "ouvrés"), ("3", "personnes")])
    texte(d, Inches(0.85), Inches(3.6), Inches(11.6), Inches(1.4),
          "Le chiffre de huit jours suppose que les corrections des bloquants "
          "arrivent dans la journée. Sinon le contre-essai décale d'autant — "
          "c'est le seul aléa qui déplace vraiment la date de fin.",
          taille=15, couleur=C_DOUX, interligne=1.3)
    enonce(d, "Le classeur est l'outil de travail, pas un rapport. Les statuts "
              "sont des listes déroulantes, la synthèse se calcule seule.")

    d = page(prs, "Qui fait quoi", "RÔLES DANS LA CAMPAGNE")
    tableau(d, ["Rôle", "Responsabilité", "Ne fait pas"], [
        ["Pilote de recette", "Tient le classeur, arbitre les statuts et la "
         "gravité, prononce les critères de sortie", "Ne joue pas les cas lui-même"],
        ["Testeurs métier (2)", "Jouent les cas fonctionnels de leur domaine, "
         "avec le compte prescrit", "Ne décident pas de la gravité"],
        ["Testeur technique", "Sécurité, performance, intégrations, accessibilité",
         "Ne juge pas la pertinence métier"],
        ["Développeur de permanence", "Qualifie les anomalies, corrige les "
         "bloquants dans la journée", "Ne ferme jamais une anomalie lui-même"],
    ], y=Inches(1.75), taille=12, haut_ligne=Inches(0.72))
    enonce(d, "Une anomalie fermée par celui qui l'a corrigée n'est pas vérifiée, "
              "elle est espérée. Seul le testeur qui l'a ouverte la passe à "
              "« vérifiée », après avoir rejoué son cas.", y=Inches(5.05))

    d = page(prs, "Le déroulé, jour par jour")
    tableau(d, ["Jalon", "Durée", "Contenu"], [
        ["J0", "0,5 j", "Montage de l'environnement, chargement et vérification "
         "des jeux de données, gel de la version"],
        ["J1 – J2", "2 j", "Pilotage, méthodes qualité, SPC, APQP"],
        ["J3 – J4", "2 j", "Non-conformité, CAPA, fournisseurs, opérations"],
        ["J5", "1 j", "Référentiels, conformité GRC, administration"],
        ["J6", "1 j", "Transverses : sécurité, langues, accessibilité, "
         "performance, mobile et hors ligne"],
        ["J7", "1 j", "Scénarios bout-en-bout"],
        ["J8", "0,5 j", "Contre-essais, synthèse, comité de décision"],
    ], y=Inches(1.7), taille=12, haut_ligne=Inches(0.52))
    enonce(d, "Les scénarios bout-en-bout sont en dernier, à dessein : échouer à "
              "l'étape 7 parce que l'étape 2 était déjà cassée n'apprend rien.",
           y=Inches(5.85))

    d = page(prs, "Comment vous suivez l'avancement", "PILOTAGE QUOTIDIEN")
    puces(d, Inches(0.85), Inches(1.75), Inches(5.6), Inches(3.6), [
        "Un point de dix minutes chaque matin : ce qui a été joué la veille, "
        "ce qui bloque aujourd'hui.",
        "L'onglet « Synthèse » donne l'avancement, le taux de conformité et le "
        "nombre d'anomalies ouvertes par gravité.",
        "La matrice de couverture dit, par domaine, combien de cas de droits et "
        "de cas de refus ont été joués.",
    ], taille=15)
    bloc(d, Inches(6.9), Inches(1.75), Inches(5.55), Inches(3.6), C_FROID, C_FILET)
    texte(d, Inches(7.25), Inches(2.0), Inches(4.9), Inches(0.5),
          "Le signal à surveiller", taille=13, gras=True, couleur=C_BLEU)
    texte(d, Inches(7.25), Inches(2.55), Inches(4.9), Inches(2.6),
          "Un domaine qui affiche 100 % de conformes au premier passage, sans "
          "aucune anomalie, n'est pas un bon signe : c'est presque toujours le "
          "signe que seuls les chemins nominaux ont été joués, et que les cas de "
          "refus et de droits ont été sautés.\n\n"
          "Dans ce cas, redemandez les captures des cas de type « Négatif » et "
          "« Droits » de ce domaine.",
          taille=13, couleur=C_ENCRE, interligne=1.3)
    enonce(d, "Ce que vous cherchez n'est pas un tableau vert. C'est la preuve "
              "que l'on a vraiment essayé de casser le produit.")

    d = page(prs, "Une anomalie bien écrite se corrige dix fois plus vite",
             "GESTION DES ANOMALIES")
    bloc(d, Inches(0.85), Inches(1.75), Inches(5.6), Inches(2.5), rgb("FADBD8"))
    texte(d, Inches(1.15), Inches(1.95), Inches(5.0), Inches(0.4),
          "Ce qui ne sert à rien", taille=13, gras=True, couleur=rgb("B3261E"))
    texte(d, Inches(1.15), Inches(2.45), Inches(5.0), Inches(1.6),
          "« Le plan de travail des CAPA ne marche pas. »\n\n"
          "Sera refermé sans correction : personne ne sait quoi reproduire.",
          taille=13.5, couleur=C_ENCRE, interligne=1.3)
    bloc(d, Inches(6.9), Inches(1.75), Inches(5.55), Inches(2.5), rgb("DFF0E4"))
    texte(d, Inches(7.2), Inches(1.95), Inches(4.95), Inches(0.4),
          "Ce qui se corrige le jour même", taille=13, gras=True, couleur=rgb("1E7A4B"))
    texte(d, Inches(7.2), Inches(2.45), Inches(4.95), Inches(1.7),
          "« Depuis demo.resp-service, le plan de travail affiche 14 lignes "
          "alors que ce compte n'est concerné que par 1 CAPA. Capture jointe, "
          "version main-4078a994. »",
          taille=13.5, couleur=C_ENCRE, interligne=1.3)
    puces(d, Inches(0.85), Inches(4.5), Inches(11.6), Inches(1.2), [
        "Quatre champs obligatoires : étapes de reproduction, observé, attendu, preuve.",
        "Cycle : ouverte → en cours → corrigée → vérifiée. Seul l'ouvreur ferme.",
    ], taille=14.5)

    d = page(prs, "Ce qui peut faire dérailler la campagne", "RISQUES")
    tableau(d, ["Risque", "Effet", "Parade"], [
        ["La version bouge pendant la campagne", "Les cas joués ne disent plus rien",
         "Gel de l'étiquette d'image à J0"],
        ["Les jeux de données dérivent", "Un cas échoue à cause du précédent",
         "Rechargement entre deux sections"],
        ["Tout est joué en administrateur", "Les défauts de droits restent invisibles",
         "Chaque cas désigne son compte ; vérification par sondage"],
        ["Un service externe indisponible", "Des cas marqués « non conforme » à tort",
         "Statut « Bloqué », distinct, prévu au classeur"],
        ["La campagne cherche à se rassurer", "Un cahier vert qui ne protège de rien",
         "Cas de refus et de droits obligatoires"],
    ], y=Inches(1.72), taille=11.5, haut_ligne=Inches(0.66))
    enonce(d, "Le dernier risque est le plus probable, et c'est le seul qui ne se "
              "voie pas dans les chiffres.", y=Inches(5.75))
    return prs


# =====================================================================
# 3. INGÉNIEURS ET ACTEURS DE LA QUALITÉ
# =====================================================================
def deck_ingenieurs():
    prs = nouvelle()
    couverture(prs, "MODE D'EMPLOI DE LA CAMPAGNE",
               "Comment on recette,\net où sont les pièges",
               "QualitOS — recette complète. Présentation aux ingénieurs qualité, "
               "testeurs et acteurs du terrain.")

    d = page(prs, "Les deux documents, et ce que chacun contient")
    bloc(d, Inches(0.85), Inches(1.75), Inches(5.6), Inches(3.3), C_FROID, C_FILET)
    bloc(d, Inches(0.85), Inches(1.75), Inches(5.6), Pt(3), C_BLEU)
    texte(d, Inches(1.15), Inches(2.0), Inches(5.0), Inches(0.5),
          "Le classeur — votre outil de travail", taille=15, gras=True, couleur=C_MARINE)
    puces(d, Inches(1.15), Inches(2.6), Inches(5.0), Inches(2.2), [
        "Les %d cas, avec étapes et attendu" % NB,
        "Les jeux de données et leur état de départ",
        "Le journal des anomalies",
        "La synthèse, calculée seule",
    ], taille=13)
    bloc(d, Inches(6.9), Inches(1.75), Inches(5.55), Inches(3.3), C_FROID, C_FILET)
    bloc(d, Inches(6.9), Inches(1.75), Inches(5.55), Pt(3), C_SIGNAL)
    texte(d, Inches(7.2), Inches(2.0), Inches(4.95), Inches(0.5),
          "Le plan — la règle du jeu", taille=15, gras=True, couleur=C_MARINE)
    puces(d, Inches(7.2), Inches(2.6), Inches(4.95), Inches(2.2), [
        "Le périmètre, et ce qui en est exclu",
        "L'échelle de gravité",
        "Les critères d'entrée et de sortie",
        "Les risques de la campagne",
    ], taille=13)
    enonce(d, "On joue dans le classeur. On tranche avec le plan.")

    d = page(prs, "Les comptes : on recette des rôles, pas un compte",
             "AVANT DE COMMENCER")
    tableau(d, ["Compte", "Personne", "Rôle", "Sert à vérifier"],
            [[c[0], c[1], c[2], c[3][:58]] for c in COMPTES[:8]],
            y=Inches(1.72), taille=11, haut_ligne=Inches(0.48))
    bloc(d, Inches(0.85), Inches(5.95), L - Inches(1.7), Inches(0.95), rgb("FDF0DE"))
    bloc(d, Inches(0.85), Inches(5.95), Pt(4), Inches(0.95), C_SIGNAL)
    texte(d, Inches(1.2), Inches(6.1), L - Inches(2.4), Inches(0.7),
          "Une fenêtre de navigation privée par compte. La session Keycloak est "
          "partagée par le navigateur : deux comptes ouverts dans la même fenêtre "
          "donnent des résultats faux, et on perd une demi-journée à comprendre "
          "pourquoi.", taille=12.5, couleur=C_ENCRE, interligne=1.2)

    d = page(prs, "Le fil rouge : la pompe P-204", "JEUX DE DONNÉES")
    texte(d, Inches(0.85), Inches(1.68), Inches(11.6), Inches(1.0),
          "Les %d jeux ne sont pas indépendants. Ils forment une seule histoire "
          "industrielle, et leur état de départ fait partie du test." % len(JEUX),
          taille=15.5, couleur=C_DOUX, interligne=1.25)
    tableau(d, ["Code", "Objet", "État au départ — et pourquoi il compte"], [
        ["JD-CAPA-01", "CAPA liée à la fuite sur P-204",
         "Actions faites, efficacité NON jugée : c'est la condition qui ouvre l'évaluation"],
        ["JD-SPC-02", "Série de 8 points du même côté",
         "Construite exprès : la règle de Nelson 2 DOIT se déclencher"],
        ["JD-GRC-01", "Registre RoPA de 8 traitements",
         "Un traitement sans base légale, volontairement : le produit doit le dire"],
        ["JD-TEN-02", "Second client « Plasturgie du Centre »",
         "Ne sert QU'À prouver l'étanchéité. Aucun cas fonctionnel ne s'y joue"],
    ], y=Inches(2.85), taille=11.5, haut_ligne=Inches(0.68))
    enonce(d, "Si un jeu n'est pas dans l'état annoncé, on le recharge avant de "
              "jouer. Un cas joué sur des données approximatives ne prouve rien — "
              "et il occupe une ligne « conforme » trompeuse.", y=Inches(5.85))

    d = page(prs, "Jouer un cas, concrètement")
    etapes = [
        ("1", "Lire l'attendu AVANT d'agir", "Sinon on voit ce qu'on espère voir."),
        ("2", "Se connecter avec le compte prescrit", "Pas un autre. C'est la moitié du test."),
        ("3", "Dérouler les étapes dans l'ordre", "Sans raccourci, même si on connaît l'écran."),
        ("4", "Comparer à l'attendu, mot à mot", "« À peu près » n'existe pas."),
        ("5", "Capturer la preuve", "Conforme aussi : c'est ce qui rend le résultat opposable."),
        ("6", "Renseigner statut, testeur, date", "Un statut sans testeur n'engage personne."),
    ]
    for i, (num, titre_e, detail) in enumerate(etapes):
        col, lig = i % 3, i // 3
        x = Inches(0.85) + col * Inches(3.95)
        y = Inches(1.85) + lig * Inches(1.85)
        bloc(d, x, y, Inches(3.7), Inches(1.6), C_FROID, C_FILET)
        bloc(d, x, y, Pt(3), Inches(1.6), C_BLEU)
        texte(d, x + Inches(0.22), y + Inches(0.14), Inches(3.3), Inches(0.35),
              num, taille=16, gras=True, couleur=C_BLEU)
        texte(d, x + Inches(0.22), y + Inches(0.52), Inches(3.3), Inches(0.4),
              titre_e, taille=13, gras=True, couleur=C_MARINE)
        texte(d, x + Inches(0.22), y + Inches(0.95), Inches(3.3), Inches(0.55),
              detail, taille=11, couleur=C_DOUX, interligne=1.15)
    enonce(d, "Un cas « conforme » sans preuve jointe sera repassé. Ce n'est pas "
              "de la défiance : c'est ce qui distingue un compte rendu d'essai "
              "d'une déclaration.", y=Inches(5.7))

    d = page(prs, "Deux règles qui valent sur tous les écrans",
             "MÊME SI LE CAS NE LE DEMANDE PAS")
    bloc(d, Inches(0.85), Inches(1.8), Inches(5.6), Inches(2.9), C_FROID, C_FILET)
    bloc(d, Inches(0.85), Inches(1.8), Inches(5.6), Pt(3), C_SIGNAL)
    texte(d, Inches(1.15), Inches(2.05), Inches(5.0), Inches(0.5),
          "A. Jamais d'identifiant technique", taille=15, gras=True, couleur=C_MARINE)
    texte(d, Inches(1.15), Inches(2.6), Inches(5.0), Inches(2.0),
          "Une personne s'affiche par son nom, à défaut par son adresse. "
          "Un « 8fb8df9f-b44f-… » visible à l'écran est une anomalie majeure, "
          "sur n'importe quel écran.\n\n"
          "Le produit s'adresse à des gens qui se connaissent par leur nom.",
          taille=13, couleur=C_ENCRE, interligne=1.3)
    bloc(d, Inches(6.9), Inches(1.8), Inches(5.55), Inches(2.9), C_FROID, C_FILET)
    bloc(d, Inches(6.9), Inches(1.8), Inches(5.55), Pt(3), C_SIGNAL)
    texte(d, Inches(7.2), Inches(2.05), Inches(4.95), Inches(0.5),
          "B. Hors de portée = 404, jamais liste vide", taille=15, gras=True,
          couleur=C_MARINE)
    texte(d, Inches(7.2), Inches(2.6), Inches(4.95), Inches(2.0),
          "Une liste vide dit qu'il n'y a rien. Un 404 dit qu'il n'y a rien à voir "
          "pour vous.\n\n"
          "La différence se lit, et elle renseigne un attaquant sur ce qui existe "
          "ailleurs. C'est une anomalie bloquante.",
          taille=13, couleur=C_ENCRE, interligne=1.3)
    enonce(d, "Ces deux règles ne figurent pas dans chaque cas : elles "
              "s'appliquent à tous. Relevez-les où que vous les rencontriez.",
           y=Inches(5.0))

    d = page(prs, "Les pièges qui font perdre une demi-journée", "RETOURS D'EXPÉRIENCE")
    tableau(d, ["Symptôme", "Ce n'est pas un défaut du produit", "Ce qu'il faut faire"], [
        ["Résultats incohérents entre deux comptes",
         "Session Keycloak partagée par le navigateur",
         "Fenêtre privée par compte, ou déconnexion complète"],
        ["Second facteur demandé à l'improviste",
         "Politique du realm sur les rôles sensibles et les actes critiques",
         "Enrôler le code à usage unique une fois, puis continuer"],
        ["L'IA ne répond pas",
         "Fournisseur épinglé indisponible — pas de repli, c'est voulu",
         "Statut « Bloqué », pas « Non conforme »"],
        ["Un compteur ne bouge pas après une action",
         "Certains indicateurs se recalculent en différé",
         "Rafraîchir, attendre le cycle, puis seulement relever"],
        ["Un module absent du menu",
         "Il n'est pas souscrit par ce client",
         "Statut « Sans objet », et vérifier auprès du pilote"],
    ], y=Inches(1.72), taille=11, haut_ligne=Inches(0.66))

    d = page(prs, "Les cinq parcours qui comptent le plus", "BOUT-EN-BOUT")
    tableau(d, ["ID", "Parcours", "Ce qu'il prouve"],
            [[s[0], s[1], s[2]] for s in SCENARIOS],
            y=Inches(1.75), taille=12, haut_ligne=Inches(0.78))
    enonce(d, "Ils se jouent en dernier, quand les écrans sont sains. Ils ne "
              "testent pas des écrans : ils testent que les objets sont vraiment "
              "liés — ce qui est toute la valeur du produit.", y=Inches(5.9))
    return prs


# =====================================================================
if not os.path.isdir(SORTIE):
    os.makedirs(SORTIE)

for nom, fabrique in (("direction", deck_direction),
                      ("managers", deck_managers),
                      ("ingenieurs", deck_ingenieurs)):
    prs = fabrique()
    chemin = os.path.join(SORTIE, "QualitOS-recette-%s-v%s.pptx" % (nom, VERSION))
    prs.save(chemin)
    print("presentation ecrite : %s (%d diapositives)" % (chemin, len(prs.slides.__iter__.__self__._sldIdLst)))
