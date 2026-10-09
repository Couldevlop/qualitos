#!/usr/bin/env python3
"""Extrait un script d'un manifeste Kubernetes (ADR 0084).

    extract-script.py <manifeste> <regex du document> <regex de la ligne marqueur>

Le mode léger rejoue les scripts des manifestes Kubernetes — initialisation de
PostgreSQL, du stockage objet, sauvegarde — plutôt que d'en garder une copie :
une copie finirait par diverger, et une installation cliente porterait alors un
comportement que personne n'éprouve plus.

On lit le bloc littéral YAML (« | ») qui suit la ligne marqueur, dans le premier
document du manifeste qui correspond, et on le rend dé-indenté. Lecture textuelle
volontaire : aucune dépendance (PyYAML) n'est exigée du serveur.
"""
import io
import re
import sys


def extract(text, doc_pattern, marker_pattern):
    for doc in re.split(r'(?m)^---\s*$', text):
        if not re.search(doc_pattern, doc, re.M):
            continue
        lines = doc.split('\n')
        for i, line in enumerate(lines):
            if re.search(marker_pattern, line) and line.rstrip().endswith('|'):
                base = len(line) - len(line.lstrip())
                out, indent = [], None
                for l in lines[i + 1:]:
                    if l.strip() == '':
                        out.append('')
                        continue
                    cur = len(l) - len(l.lstrip())
                    if cur <= base:
                        break
                    if indent is None:
                        indent = cur
                    out.append(l[indent:] if cur >= indent else l.lstrip())
                while out and out[-1] == '':
                    out.pop()
                if not out:
                    break
                return '\n'.join(out) + '\n'
    raise SystemExit('script introuvable dans le manifeste (%s / %s)' % (doc_pattern, marker_pattern))


def main(argv):
    if len(argv) != 4:
        raise SystemExit('usage: extract-script.py <manifeste> <regex document> <regex marqueur>')
    text = io.open(argv[1], encoding='utf-8').read().replace('\r\n', '\n')
    # En octets UTF-8 : la console Windows (cp1252) corromprait les accents.
    sys.stdout.buffer.write(extract(text, argv[2], argv[3]).encode('utf-8'))


if __name__ == '__main__':
    main(sys.argv)
