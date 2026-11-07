# -*- coding: utf-8 -*-
"""Socle commun des livrables de recette QualitOS.

Un seul endroit porte la charte, les comptes, les jeux de donnees et les
conventions : le classeur, le document et les trois presentations lisent tous
ici. Une couleur changee une fois change partout, et aucun des quatre fichiers
ne peut contredire les autres sur le nom d'un compte ou le code d'un jeu de
donnees -- ce qui arriverait fatalement si chacun portait sa propre copie.
"""

import os

# La racine se deduit de l'emplacement du script (scripts/recette/..), et non
# d'un chemin ecrit en dur : le depot est clone ailleurs sur chaque poste, et
# un chemin absurde ne se verrait qu'au moment d'ecrire les fichiers.
RACINE = os.environ.get(
    "QOS_RACINE",
    os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__)))))
SORTIE = os.path.join(RACINE, "docs", "recette")
LOGO = os.path.join(RACINE, "apps", "site", "assets", "qualitos-marque.png")

VERSION = "1.0"
DATE = "2026-11-07"
ENVIRONNEMENT = "Preproduction -- https://preprod.qualitos.openlabconsulting.com/fr/"

# ---------------------------------------------------------------- la charte
# Les trois couleurs du logo, et rien de plus : ce sont celles du site
# (apps/site/styles.css). Les reprendre ici garde les livrables et la vitrine
# dans la meme famille.
MARINE = "12305F"
MARINE_SOMBRE = "0B1E3F"
BLEU = "1F62C4"
BLEU_CLAIR = "E8EFFA"
SIGNAL = "F5901F"
SIGNAL_SOMBRE = "B9690A"
ENCRE = "0F1D33"
ENCRE_DOUCE = "42536E"
PAPIER = "FFFFFF"
PAPIER_FROID = "F2F5FA"
FILET = "C9D6E8"
FILET_FORT = "9DB4D4"

VERT = "1E7A4B"      # conforme
ROUGE = "B3261E"     # bloquant
AMBRE = "B9690A"     # reserve

POLICE_TITRE = "Archivo"
POLICE_TEXTE = "IBM Plex Sans"
# Archivo et IBM Plex Sans ne sont pas installees sur tous les postes. Calibri
# est le repli : il est present partout, et un document qui s'ouvre avec la
# police de secours reste lisible, alors qu'un document qui s'ouvre avec une
# police absente se recompose n'importe comment.
POLICE_REPLI = "Calibri"

# ------------------------------------------------------------- les personnes
# Reprend les comptes de la recette MOC/CAPA/audits/SMI deja jouee, pour que
# les deux campagnes parlent des memes personnes.
COMPTES = [
    ("demo.admin", "Claire Morel", "Administrateur client",
     "Parametrage du client, modules, roles, cles d'API, journal d'audit"),
    ("demo.dir", "Philippe Marchand", "Directeur qualite",
     "Vue 360, validation des plans strategiques, revue de direction, rapports"),
    ("demo.qualite", "Isabelle Fontaine", "Manager qualite",
     "Pilote les cycles, les CAPA, les audits, les analyses SPC et IA"),
    ("demo.auditeur", "Marc Delorme", "Auditeur",
     "Lecture etendue, conduite d'audit, constats, rapport"),
    ("demo.externe", "Agnes Rivet", "Auditeur externe",
     "Acces borne dans le temps, lecture des preuves, attestation"),
    ("demo.resp-service", "Karim Benali", "Utilisateur",
     "Responsable de service : approbations MOC, actions CAPA, audits 5S"),
    ("demo.hse", "Sophie Lambert", "Utilisateur",
     "HSE : incidents EHS, approbations MOC, proprietaire de risques"),
    ("demo.demandeur", "Julien Faure", "Utilisateur",
     "Terrain : saisit NC, idees, audits 5S au telephone, mode hors ligne"),
    ("demo.ingenieur", "Nadia Rousseau", "Utilisateur",
     "Ingenieur qualite : AMDEC, plans de surveillance, capabilite, Gage R&R"),
    ("superadmin", "Compte plateforme", "Super administrateur",
     "Console plateforme : clients, modules souscrits, cabinets, facturation"),
]

# --------------------------------------------------------- jeux transversaux
# Le principe : un seul fil rouge industriel traverse toute la campagne. La
# pompe P-204 casse, la NC nait, la CAPA la traite, l'audit la verifie, la
# carte SPC la voit venir, le MOC la remplace. Des donnees eparpillees
# testeraient les ecrans ; un fil rouge teste le PRODUIT, qui promet justement
# que ces objets soient lies.
JEUX = [
    # (code, objet, etat de depart, module(s) concernes)
    ("JD-ORG-01", "Client de recette " + chr(171) + " Fonderie Atlantique " + chr(187) +
     " -- 2 sites (Nantes, Vitre), 6 services, 42 personnes",
     "Cree, modules actifs : tous sauf Marketplace", "Administration"),
    ("JD-ORG-02", "Matrice des roles : les 7 roles de la plateforme attribues aux 10 comptes",
     "Attribuee", "Administration"),
    ("JD-REF-01", "SMI : ISO 9001 adoptee ; ISO 14001 a ajouter pendant la recette",
     "ISO 9001 seule", "Referentiels"),
    ("JD-REF-02", "Pack sectoriel Industrie / Manufacturing",
     "Disponible, non active", "Referentiels"),
    ("JD-EQP-01", "Equipement P-204 (pompe centrifuge, Nantes, ligne 2), criticite haute",
     "En service", "Operations"),
    ("JD-EQP-02", "Instrument CAL-118 (manometre), echeance d'etalonnage dans 10 jours",
     "Conforme, echeance proche", "Operations"),
    ("JD-EQP-03", "Capteur IoT VIB-204 (vibration) sur P-204, 1 mesure / minute",
     "Connecte, flux nominal", "Operations"),
    ("JD-PRD-01", "Produit " + chr(171) + " Corps de pompe CP-55 " + chr(187) +
     ", 3 caracteristiques speciales", "Actif, AMDEC et plan de surveillance lies", "APQP"),
    ("JD-FRS-01", "Fournisseur " + chr(171) + " Metallurgie du Ponant " + chr(187) +
     ", score 72/100, 2 NC ouvertes", "Actif, sous surveillance", "Non-conformite"),
    ("JD-DOC-01", "Procedure PR-QUA-012 " + chr(171) + " Traitement des non-conformites " +
     chr(187) + " v3, 2 signataires", "Publiee, lecture obligatoire pour 12 personnes", "GED"),
    ("JD-DOC-02", "Mode operatoire MO-MNT-004, version brouillon en attente d'approbation",
     "Brouillon", "GED"),
    ("JD-NC-01", "NC-2026-0006 " + chr(171) + " Fuite d'huile au palier de la pompe P-204 " +
     chr(187), "Ouverte, majeure, non traitee", "Non-conformite"),
    ("JD-NC-02", "NC-2026-0011 (externe) " + chr(171) + " Reclamation client : fuite constatee " +
     "a la reception " + chr(187), "Ouverte, criticite haute", "Non-conformite"),
    ("JD-NC-03", "Lot de 40 NC des 18 derniers mois, dont 7 recurrentes sur P-204",
     "Historique, cloturees", "Analyses IA"),
    ("JD-CAPA-01", "CAPA-2026-0014 liee a NC-2026-0006, 2 actions, critere d'efficacite pose",
     "Actions faites, efficacite non jugee", "CAPA"),
    ("JD-CAPA-02", "CAPA-2026-0009, cloturee, verdict " + chr(171) + " partiellement efficace " +
     chr(187) + " il y a 6 mois", "Cloturee, recidive constatee", "CAPA"),
    ("JD-AUD-01", "AUD-2026-0002 audit interne magasin, checklist de 3 questions",
     "Planifie, non demarre", "Audits"),
    ("JD-AUD-02", "AUD-2026-0004 audit maintenance, 2 constats, rapport en brouillon",
     "Cloture", "Audits"),
    ("JD-SPC-01", "Carte X-barre/R sur le diametre d'alesage du CP-55, 25 sous-groupes de 5",
     "Sous controle, 1 point proche de la limite", "SPC"),
    ("JD-SPC-02", "Serie SPC avec 8 points consecutifs du meme cote de la moyenne",
     "Regle de Nelson 2 a declencher", "SPC"),
    ("JD-SPC-03", "Etude Gage R&R : 3 operateurs, 10 pieces, 2 essais",
     "Donnees saisies, non calculee", "SPC"),
    ("JD-MOC-01", "MOC-2026-001 remplacement de la pompe P-204, 54/100 Critique",
     "En validation, 1 approbation sur 4", "MOC"),
    ("JD-RSQ-01", "R-005 rupture du circuit de refroidissement, proprietaire Sophie Lambert",
     "A traiter", "Risques"),
    ("JD-FOR-01", "Matrice de competences : 6 competences x 42 personnes, 4 echeances depassees",
     "A jour sauf 4 ecarts", "Formation"),
    ("JD-GRC-01", "RoPA : 8 traitements, dont 1 sans base legale renseignee",
     "Incomplet (volontairement)", "Conformite GRC"),
    ("JD-GRC-02", "Systeme d'IA " + chr(171) + " Detection d'anomalies SPC " + chr(187) +
     ", classe haut risque", "Declare, FRIA non faite", "Conformite GRC"),
    ("JD-KPI-01", "12 KPI strategiques alimentes sur 18 mois",
     "Historises", "Pilotage"),
    ("JD-TEN-02", "Second client " + chr(171) + " Plasturgie du Centre " + chr(187) +
     " avec ses propres NC et documents",
     "Cree -- sert UNIQUEMENT a prouver l'etancheite entre clients", "Securite"),
]

CRITICITES = ["Bloquant", "Majeur", "Mineur"]
STATUTS = ["A jouer", "Conforme", "Non conforme", "Bloque", "Sans objet"]
TYPES = ["Fonctionnel", "Droits", "Donnees", "Negatif", "Securite",
         "Integration", "i18n", "Accessibilite", "Performance", "Mobile/Hors ligne"]
