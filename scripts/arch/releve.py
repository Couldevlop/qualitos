#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""Relevé d'architecture du moteur qualité.

Recalcule les chiffres de `docs/architecture/etat-des-lieux.md` pour qu'un relevé
ultérieur soit COMPARABLE plutôt que refait à la main. Chaque mesure répond à une
question d'architecture précise, et non à une curiosité :

- modules plats / en couches : où en est la convergence vers Clean ;
- entités hors infrastructure : mesure directe du couplage domaine / persistance ;
- couverture ArchUnit : quelle part de l'invariant est tenue par le `build` ;
- graphe et cycles : où le découplage coûte le plus cher.

À lancer depuis la racine du dépôt :

    python scripts/arch/releve.py [--archunit] [--graphe] [--tout]

Sans option, affiche la synthèse. Sort en code 0 ; il s'agit d'un relevé, pas d'une
porte de qualité — la porte, c'est `HexagonalArchitectureTest`.
"""

from __future__ import annotations

import argparse
import collections
import io
import os
import re
import sys

MOTEUR = os.path.join(
    "apps", "api-quality-engine", "src", "main", "java", "com", "openlab", "qualitos", "quality"
)
TEST_ARCH = os.path.join(
    "apps", "api-quality-engine", "src", "test", "java", "com", "openlab", "qualitos",
    "quality", "arch", "HexagonalArchitectureTest.java",
)

COUCHES = ("domain", "application", "infrastructure")

IMPORT_MODULE = re.compile(
    r"^import (?:static )?com[.]openlab[.]qualitos[.]quality[.]([a-z0-9]+)[.]", re.M
)
# Le motif capture la LIGNE ENTIERE, et non son prefixe : sans quoi on ne peut pas
# distinguer l'annotation transactionnelle du reste de Spring.
CADRES = re.compile(
    r"^import (?:static )?(?:org[.]springframework|jakarta[.]persistence|jakarta[.]validation"
    r"|org[.]hibernate|com[.]fasterxml[.]jackson)[a-zA-Z0-9_.*]*;", re.M
)

# Le piege qui a fait manquer vingt-deux couplages : une annotation ecrite en nom
# PLEINEMENT QUALIFIE dans le corps n'apparait dans aucun import, et echappe donc a
# toute mesure qui ne lit que les imports. ArchUnit la voit, lui, parce qu'il lit le
# bytecode ; ce releve doit la voir aussi, sans quoi il annonce une purete fausse.
CADRES_EN_CLAIR = re.compile(
    r"@(?:org[.]springframework|jakarta[.]persistence|org[.]hibernate)[.][a-zA-Z0-9_.]+"
)

# La frontiere transactionnelle est une decision du CAS D'USAGE, pas de la persistance :
# la regle d'architecture l'admet dans `application`, et jamais dans `domain`. Ce releve
# dit donc la meme chose que le build -- deux mesures qui divergent ne valent rien.
ADMIS_EN_APPLICATION = ("org.springframework.transaction.annotation",)


def lire(chemin: str) -> str:
    return io.open(chemin, encoding="utf-8", errors="replace").read()


def fichiers_java(racine: str):
    for dp, _dn, fn in os.walk(racine):
        for f in fn:
            if f.endswith(".java"):
                yield os.path.join(dp, f)


def modules() -> list[str]:
    return sorted(
        m for m in os.listdir(MOTEUR)
        if os.path.isdir(os.path.join(MOTEUR, m)) and not m.startswith(".")
    )


def style_des_modules():
    """Un module est EN COUCHES s'il porte au moins deux des trois couches."""
    plats, couches = [], []
    for m in modules():
        sous = set(os.listdir(os.path.join(MOTEUR, m)))
        (couches if len(set(COUCHES) & sous) >= 2 else plats).append(m)
    return plats, couches


def mesures_de_couplage():
    """Entités, ports, services annotés, injection par champ."""
    entites = entites_hors_infra = ports = services = autowired = 0
    for f in fichiers_java(MOTEUR):
        s = lire(f)
        rel = f.replace(os.sep, "/")
        if "@Entity" in s:
            entites += 1
            if "/infrastructure/" not in rel:
                entites_hors_infra += 1
        if "/domain/" in rel and re.search(r"^public interface ", s, re.M):
            ports += 1
        if "@Service" in s:
            services += 1
        if "@Autowired" in s:
            autowired += 1
    return entites, entites_hors_infra, ports, services, autowired


def couverture_archunit():
    """Paquets `domain` existants, vs ceux que nomme une règle ArchUnit."""
    existants = set()
    for dp, dn, _fn in os.walk(MOTEUR):
        for d in dn:
            if d == "domain":
                rel = os.path.relpath(dp, MOTEUR).replace(os.sep, "/")
                existants.add(rel)
    couverts, universelle = set(), False
    if os.path.exists(TEST_ARCH):
        src = lire(TEST_ARCH)
        # Une regle qui porte sur "..domain.." sans nommer de module couvre TOUT.
        # Sans ce test, le releve compterait 11 paquets couverts sur 54 alors que la
        # regle universelle les tient tous -- il mesurerait l'ancien etat.
        universelle = '.resideInAPackage("..domain..")' in src
        for frag in re.findall(r'"[.][.]([a-z0-9.]+)[.]domain[.][.]"', src):
            couverts.add(frag.replace(".", "/"))
    return existants, couverts, universelle


def graphe():
    """Arêtes entre modules, fan-in, fan-out, cycles."""
    aretes = collections.Counter()
    sortant = collections.defaultdict(set)
    entrant = collections.defaultdict(set)
    for f in fichiers_java(MOTEUR):
        rel = os.path.relpath(f, MOTEUR).replace(os.sep, "/")
        source = rel.split("/")[0]
        for cible in set(IMPORT_MODULE.findall(lire(f))):
            if cible != source:
                aretes[(source, cible)] += 1
                sortant[source].add(cible)
                entrant[cible].add(source)
    cycles = sorted({tuple(sorted(c)) for c in aretes if (c[1], c[0]) in aretes})
    return aretes, entrant, sortant, cycles


def domaines_impurs():
    """Fichiers d'un `domain` ou d'une `application` qui nomment un cadre technique,
    par un import comme en nom pleinement qualifie."""
    impurs = []
    for f in fichiers_java(MOTEUR):
        rel = os.path.relpath(f, MOTEUR).replace(os.sep, "/")
        if "/domain/" not in rel and "/application/" not in rel:
            continue
        src = lire(f)
        trouves = [m.group(0) for m in CADRES.finditer(src)]
        trouves += [m.group(0) for m in CADRES_EN_CLAIR.finditer(src)]
        if "/application/" in rel:
            trouves = [t for t in trouves if not any(a in t for a in ADMIS_EN_APPLICATION)]
        if trouves:
            impurs.append(rel)
    return sorted(impurs)


def titre(t: str) -> None:
    print()
    print("=" * 72)
    print(t)
    print("=" * 72)


def main() -> int:
    ap = argparse.ArgumentParser(description="Relevé d'architecture du moteur qualité.")
    ap.add_argument("--archunit", action="store_true", help="couverture des règles ArchUnit")
    ap.add_argument("--graphe", action="store_true", help="graphe des dépendances et cycles")
    ap.add_argument("--tout", action="store_true", help="tout afficher")
    args = ap.parse_args()

    if not os.path.isdir(MOTEUR):
        print("À lancer depuis la racine du dépôt (moteur introuvable : %s)" % MOTEUR)
        return 2

    synthese = args.tout or not (args.archunit or args.graphe)

    if synthese:
        titre("STYLE DES MODULES")
        plats, couches = style_des_modules()
        total = len(plats) + len(couches)
        print("en couches : %d / %d" % (len(couches), total))
        print("plats      : %d / %d" % (len(plats), total))
        print()
        print("plats : " + ", ".join(plats))

        titre("COUPLAGE")
        ent, hors, ports, services, autow = mesures_de_couplage()
        print("entités JPA                      : %d" % ent)
        print("  dont hors d'infrastructure     : %d (%d %%)" % (hors, round(100 * hors / max(ent, 1))))
        print("ports (interfaces d'un domain)   : %d" % ports)
        print("services annotés @Service        : %d" % services)
        print("fichiers à @Autowired            : %d" % autow)

        titre("PURETÉ DES COUCHES domain / application")
        impurs = domaines_impurs()
        if impurs:
            print("%d fichier(s) important un cadre technique :" % len(impurs))
            for i in impurs:
                print("  " + i)
        else:
            print("aucun : les couches domain et application sont sans cadre technique.")

    if args.archunit or args.tout:
        titre("COUVERTURE ArchUnit DES PAQUETS domain")
        existants, couverts, universelle = couverture_archunit()
        print("paquets domain            : %d" % len(existants))
        if universelle:
            print("regle universelle         : OUI -- les %d sont tenus" % len(existants))
            print("paquets en plus nommes    : %d (regles PLUS STRICTES)"
                  % len(existants & couverts))
        else:
            manquants = sorted(existants - couverts)
            print("couverts par une regle    : %d" % len(existants & couverts))
            print("NON COUVERTS              : %d" % len(manquants))
            for m in manquants:
                print("  " + m)

    if args.graphe or args.tout:
        titre("GRAPHE DES DÉPENDANCES ENTRE MODULES")
        aretes, entrant, sortant, cycles = graphe()
        print("arêtes distinctes : %d" % len(aretes))
        print("dépendances       : %d" % sum(aretes.values()))
        print()
        print("-- les plus dépendus (fan-in) --")
        for m, s in sorted(entrant.items(), key=lambda x: -len(x[1]))[:10]:
            print("  %3d  %s" % (len(s), m))
        print()
        print("-- les plus dépendants (fan-out) --")
        for m, s in sorted(sortant.items(), key=lambda x: -len(x[1]))[:10]:
            print("  %3d  %s" % (len(s), m))
        print()
        print("-- cycles (%d) --" % len(cycles))
        for a, b in cycles:
            print("  %s <-> %s  (%d / %d)" % (a, b, aretes[(a, b)], aretes[(b, a)]))

    return 0


if __name__ == "__main__":
    sys.exit(main())
