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


def esc(s: str) -> str:
    s = s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
    # Après échappement, restaure les placeholders {$nom} en éléments <x/> XLIFF.
    return PLACEHOLDER_RE.sub(r'<x id="\1"/>', s)


def write_xlf(translations: dict) -> None:
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
