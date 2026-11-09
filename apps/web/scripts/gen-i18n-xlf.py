# -*- coding: utf-8 -*-
"""Génère src/locale/messages.<lang>.xlf à partir des tables scripts/i18n/*.py.

Usage : python scripts/gen-i18n-xlf.py            (depuis apps/web)
        python scripts/gen-i18n-xlf.py --check    (+ vérif croisée code <-> tables)

Pourquoi un générateur plutôt que des XLF édités à la main : une seule source
de vérité par ID, diffs lisibles, et l'ajout d'une langue = une colonne.
Les IDs sont explicites (@@nav.*, @@capa.*, …) — pas de dépendance à l'ordre
d'extraction. `ng extract-i18n` reste utilisable pour détecter les oublis.

Les tables sont éclatées PAR DOMAINE dans scripts/i18n/ (core, methods,
offline_queue, capa, grc_data, …) : chaque chantier i18n ajoute son fichier
sans toucher aux autres — pas de conflit de merge. Chaque fichier expose un
dict TRANSLATIONS = { id: (fr, en, es, ar, ja, zh) }. Un même id défini dans
deux fichiers avec un contenu différent est une erreur fatale.

Placeholder d'interpolation $localize : `${expr}:nom:` côté TS s'écrit `{$nom}`
dans la table et devient `<x id="nom"/>` dans le XLF généré.
"""
import glob
import importlib.util
import io
import os
import re
import sys

LANGS = ["en", "es", "ar", "ja", "zh"]
N_COLS = len(LANGS) + 1   # fr + 5 cibles

PLACEHOLDER_RE = re.compile(r"\{\$([A-Za-z0-9_]+)\}")

HEADER = (
    '<?xml version="1.0" encoding="UTF-8"?>\n'
    '<!-- Généré par scripts/gen-i18n-xlf.py — NE PAS éditer à la main :\n'
    '     modifier les tables scripts/i18n/*.py puis regénérer. -->\n'
    '<xliff version="1.2" xmlns="urn:oasis:names:tc:xliff:document:1.2">\n'
    '  <file source-language="fr" target-language="{lang}" datatype="plaintext" original="ng2.template">\n'
    "    <body>\n"
)
FOOTER = "    </body>\n  </file>\n</xliff>\n"

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
TABLES_DIR = os.path.join(BASE_DIR, "i18n")
SRC_APP = os.path.normpath(os.path.join(BASE_DIR, "..", "src", "app"))
OUT_DIR = os.path.normpath(os.path.join(BASE_DIR, "..", "src", "locale"))


def load_tables() -> dict:
    """Agrège les TRANSLATIONS de tous les fichiers scripts/i18n/*.py."""
    merged: dict = {}
    origin: dict = {}
    files = sorted(glob.glob(os.path.join(TABLES_DIR, "*.py")))
    if not files:
        sys.exit("ERREUR : aucune table dans %s" % TABLES_DIR)
    for path in files:
        name = os.path.splitext(os.path.basename(path))[0]
        spec = importlib.util.spec_from_file_location("i18n_" + name, path)
        mod = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(mod)  # type: ignore[union-attr]
        table = getattr(mod, "TRANSLATIONS", None)
        if not isinstance(table, dict):
            sys.exit("ERREUR : %s n'expose pas un dict TRANSLATIONS" % path)
        for key, row in table.items():
            if not isinstance(row, tuple) or len(row) != N_COLS:
                sys.exit("ERREUR : %s — '%s' doit être un tuple de %d chaînes (fr, %s)"
                         % (name, key, N_COLS, ", ".join(LANGS)))
            if key in merged and merged[key] != row:
                sys.exit("ERREUR : id '%s' défini différemment dans %s et %s"
                         % (key, origin[key], name))
            merged[key] = row
            origin[key] = name
    return merged


# Une interpolation Angular écrite telle quelle dans une table.
#
# Elle ne se voit ni à la génération ni au build : le XLF sort bien formé, et le
# défaut n'éclate qu'à l'EXÉCUTION, dans la langue traduite, sous la forme
# « Unable to parse ICU expression » — parce que `$localize` prend l'accolade
# pour une expression ICU. La vue lève alors, et l'écran reste vide.
#
# Le français échappe au piège (il vient du gabarit, pas de la table), donc rien
# ne se voit tant qu'on développe en français. D'où ce refus à la source.
# Une entite XML deja echappee dans une table.
#
# Meme famille que ci-dessus : `esc()` echappe deja `&`, `<` et `>`. Une table
# qui ecrit `&amp;` ressort donc en `&amp;amp;`, et l'ecran affiche l'entite EN
# CLAIR — « Corrective &amp; Preventive » au lieu de « Corrective & Preventive ».
# Le XLF reste bien forme, le build ne dit rien : cela ne se voit qu'a l'oeil.
ESCAPED_ENTITY_RE = re.compile(r"&(amp|lt|gt|quot|apos|#\d+);")


def reject_escaped_entities(translations: dict) -> None:
    faulty = sorted(k for k, row in translations.items()
                    if any(ESCAPED_ENTITY_RE.search(v) for v in row))
    if faulty:
        details = "\n".join("  - " + k for k in faulty)
        sys.exit(
            "ERREUR : entite XML deja echappee dans "
            + str(len(faulty)) + " unite(s).\n"
            "Ecrire le caractere lui-meme (& < >) : le generateur l'echappe.\n"
            "Pre-echappee, elle ressort doublee et s'affiche en clair.\n"
            + details)


LITERAL_INTERPOLATION_RE = re.compile(r"\{\{.*?\}\}", re.S)


def reject_literal_interpolations(translations: dict) -> None:
    faulty = sorted(k for k, row in translations.items()
                    if any(LITERAL_INTERPOLATION_RE.search(v) for v in row))
    if faulty:
        details = "\n".join("  - " + k for k in faulty)
        sys.exit(
            "ERREUR : interpolation Angular ecrite litteralement dans "
            + str(len(faulty)) + " unite(s).\n"
            "Ecrire {$INTERPOLATION} : le generateur le rend en "
            "<x id=\"INTERPOLATION\"/>.\n"
            "Laissee telle quelle, l'accolade est lue comme une expression "
            "ICU et l'ecran traduit leve a l'execution.\n"
            + details)

NAMED_PLACEHOLDER_RE = re.compile(r"\}:([A-Za-z_][A-Za-z0-9_]*):")
# La MEME chose dans un gabarit : `{{ x // i18n(ph="count") }}`. Angular met
# le nom en capitales dans ce cas, et le laisse tel quel dans le cas TS.
TEMPLATE_PH_RE = re.compile(r'i18n\(ph="([A-Za-z0-9_]+)"\)')
# Le marqueur d'un message de gabarit, attribut compris : i18n="@@id",
# i18n-title="@@id", i18n-matTooltip="@@id".
TEMPLATE_MSG_ID_RE = re.compile(r'i18n(?:-[a-zA-Z]+)?="@@([A-Za-z0-9._-]+)"')


# Les balises en ligne qu'Angular ne nomme PAS d'apres leur nom de balise.
# Ecrire {$START_TAG_EM} pour un <em> passe la generation et casse le build
# localise : Angular attend {$START_EMPHASISED_TEXT}. Mesure du 2026-11-09 :
# deux unites ainsi ecrites ont bloque la mise en production.
BALISES_NOMMEES = {
    "em": ("START_EMPHASISED_TEXT", "CLOSE_EMPHASISED_TEXT"),
    "b": ("START_BOLD_TEXT", "CLOSE_BOLD_TEXT"),
    "i": ("START_ITALIC_TEXT", "CLOSE_ITALIC_TEXT"),
    "u": ("START_UNDERLINED_TEXT", "CLOSE_UNDERLINED_TEXT"),
    "s": ("START_STRIKETHROUGH_TEXT", "CLOSE_STRIKETHROUGH_TEXT"),
    "a": ("START_LINK", "CLOSE_LINK"),
    "br": ("LINE_BREAK", None),
}
BALISE_OUVRANTE_RE = re.compile(r"<([a-zA-Z][a-zA-Z0-9-]*)(?=[\s>])")


def _corps_du_message(texte: str, debut_attribut: int) -> str:
    """Le contenu de l'element qui porte l'attribut i18n, balise fermante comprise.

    On remonte au `<` de son nom de balise, puis on avance en comptant les
    ouvertures et les fermetures du MEME nom : un <em> qui vit dans l'element
    suivant n'appartient pas a ce message et ne doit pas etre exige ici.
    """
    ouvrant = texte.rfind("<", 0, debut_attribut)
    if ouvrant < 0:
        return ""
    nom = BALISE_OUVRANTE_RE.match(texte, ouvrant)
    if not nom:
        return ""
    nom = nom.group(1)
    fin_ouvrante = texte.find(">", debut_attribut)
    if fin_ouvrante < 0:
        return ""
    profondeur, i = 1, fin_ouvrante + 1
    ouvre, ferme = "<" + nom, "</" + nom
    while profondeur and i < len(texte):
        suivant_o, suivant_f = texte.find(ouvre, i), texte.find(ferme, i)
        if suivant_f < 0:
            # Element sans fermeture (<input>, <img>, attribut i18n-placeholder) :
            # on ne sait pas ou s'arrete le message, donc on n'exige rien. Mieux
            # vaut ne rien dire que reclamer les balises du reste du fichier.
            return ""
        if 0 <= suivant_o < suivant_f:
            profondeur, i = profondeur + 1, suivant_o + len(ouvre)
        else:
            profondeur, i = profondeur - 1, suivant_f + len(ferme)
    return "" if profondeur else texte[fin_ouvrante + 1:i]


def balises_attendues(corps: str) -> set:
    """Les espaces reserves qu'Angular produira pour les balises en ligne du corps."""
    attendus = set()
    for balise in BALISE_OUVRANTE_RE.findall(corps) + re.findall(r"<(br)\s*/?>", corps):
        noms = BALISES_NOMMEES.get(balise.lower())
        if not noms:
            continue
        attendus.add(noms[0])
        if noms[1] and ("</%s" % balise.lower()) in corps.lower():
            attendus.add(noms[1])
    return attendus


def reject_placeholder_mismatch(translations: dict) -> None:
    """Les noms d'espaces reserves des tables doivent etre ceux du code.

    POURQUOI CE CONTROLE. Un message ecrit
    ``$localize`:@@id:Pas plus de ${n}:max: natures.``` nomme son espace
    reserve ``max`` ; la table doit donc ecrire ``{$max}``. Ecrire
    ``{$INTERPOLATION}`` passe ici, passe ``--check``, passe ``build:prod``
    (qui ne construit PAS les locales) -- et echoue seulement au build
    localise, c'est-a-dire dans le job d'image de la CD, apres la fusion :
    « There is a placeholder name mismatch with the translation provided ».
    Mesure du 2026-11-05 : deux unites ainsi ecrites ont bloque le
    deploiement de preproduction, et le defaut n'etait visible nulle part
    plus tot. Ce controle le ramene a une seconde de generation.

    DEUX ELARGISSEMENTS, apres une seconde panne le meme jour.

    1. Les GABARITS nomment autrement : `{{ x // i18n(ph="count") }}`, et
       Angular met alors le nom en capitales (COUNT). La premiere version de ce
       controle ne lisait que la forme TypeScript ``${x}:nom:``, de sorte que
       `spc.study.subgroup-count` -- un gabarit -- a passe la generation, passe
       --check, et casse l'image web en CD.
    2. Une table qui n'ecrit AUCUN espace reserve etait exemptee. Or le defaut
       est le meme en pire : la valeur n'apparait nulle part dans la langue
       traduite, et le texte brut de l'expression (``${a.name}``) s'affiche a
       l'ecran. Quatre unites etaient dans ce cas.
    """
    used: dict = {}
    id_re = re.compile(r"@@([A-Za-z0-9._-]+):(.*?)`", re.S)
    for root, _dirs, files in os.walk(SRC_APP):
        for fn in files:
            if not fn.endswith((".html", ".ts")):
                continue
            text = io.open(os.path.join(root, fn), encoding="utf-8", errors="replace").read()
            for match in id_re.finditer(text):
                names = set(NAMED_PLACEHOLDER_RE.findall(match.group(2)))
                if names:
                    used.setdefault(match.group(1), set()).update(names)
            if not fn.endswith(".html"):
                continue
            # Forme gabarit. Le message s'arrete au marqueur suivant : au-dela,
            # les espaces reserves appartiennent a un autre message.
            for match in TEMPLATE_MSG_ID_RE.finditer(text):
                suite = text[match.end():]
                suivant = TEMPLATE_MSG_ID_RE.search(suite)
                if suivant:
                    suite = suite[:suivant.start()]
                names = {n.upper() for n in TEMPLATE_PH_RE.findall(suite)}
                if names:
                    used.setdefault(match.group(1), set()).update(names)
            # Troisieme elargissement : les balises EN LIGNE du message
            # (<em>, <a>, <br>...) sont elles aussi des espaces reserves, et
            # Angular ne les nomme pas toutes d'apres leur nom de balise.
            # Seulement la forme i18n="@@id" : avec i18n-title / i18n-label, le
            # message est la VALEUR de l'attribut, pas le contenu de l'element.
            for match in re.finditer(r'i18n="@@([A-Za-z0-9._-]+)"', text):
                names = balises_attendues(_corps_du_message(text, match.start()))
                if names:
                    used.setdefault(match.group(1), set()).update(names)
    faulty = []
    for unit_id, names in sorted(used.items()):
        row = translations.get(unit_id)
        if not row:
            continue
        present = set()
        for value in row:
            present.update(PLACEHOLDER_RE.findall(value))
        # Pas de `present and ...` : une table SANS aucun espace reserve est
        # le cas le plus grave, pas une exemption (cf. elargissement 2).
        if not names.issubset(present):
            faulty.append("  - %s : le code nomme %s, la table ecrit %s"
                          % (unit_id, sorted(names), sorted(present) or ["aucun"]))
    if faulty:
        sys.exit(
            "ERREUR : nom d'espace reserve divergent dans "
            + str(len(faulty)) + " unite(s).\n"
            "Le build localise refuse la traduction ; le build de production,"
            " lui, ne le voit pas. Ecrire {$nom}, avec le nom du code.\n"
            + "\n".join(faulty))


def esc(s: str) -> str:
    s = s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
    # Après échappement, restaure les placeholders {$nom} en éléments <x/> XLIFF.
    return PLACEHOLDER_RE.sub(r'<x id="\1"/>', s)


def write_xlf(translations: dict) -> None:
    reject_literal_interpolations(translations)
    reject_escaped_entities(translations)
    reject_placeholder_mismatch(translations)
    os.makedirs(OUT_DIR, exist_ok=True)
    for idx, lang in enumerate(LANGS, start=1):
        path = os.path.join(OUT_DIR, "messages.%s.xlf" % lang)
        with io.open(path, "w", encoding="utf-8", newline="\n") as f:
            f.write(HEADER.format(lang=lang))
            for unit_id, row in translations.items():
                source, target = row[0], row[idx]
                f.write('      <trans-unit id="%s" datatype="html">\n' % unit_id)
                f.write("        <source>%s</source>\n" % esc(source))
                f.write('        <target state="translated">%s</target>\n' % esc(target))
                f.write("      </trans-unit>\n")
            f.write(FOOTER)
        print("OK %s (%d unités)" % (path, len(translations)))


def check(translations: dict) -> int:
    """Vérif croisée : tout @@id du code existe en table (et l'inverse, en info)."""
    used: set = set()
    id_re = re.compile(r"@@([A-Za-z0-9._-]+)")
    for root, _dirs, files in os.walk(SRC_APP):
        for fn in files:
            if fn.endswith((".html", ".ts")):
                text = io.open(os.path.join(root, fn), encoding="utf-8").read()
                used.update(id_re.findall(text))
    missing = sorted(used - set(translations))
    unused = sorted(set(translations) - used)
    print("ids utilisés dans le code :", len(used))
    if missing:
        print("MANQUANTS dans les tables (build i18n incomplet) :")
        for m in missing:
            print("  -", m)
    if unused:
        print("Dans les tables mais inutilisés (%d) — toléré (vocabulaire de réserve) :" % len(unused))
        for u in unused:
            print("  -", u)
    return 1 if missing else 0


def main() -> None:
    translations = load_tables()
    write_xlf(translations)
    if "--check" in sys.argv:
        sys.exit(check(translations))


if __name__ == "__main__":
    main()
