#!/usr/bin/env python3
"""Validation syntaxique + structurelle des manifests Chaos Mesh QualitOS.

CLAUDE.md §14.3. Sans cluster : on valide (1) le YAML est parsable, (2) chaque
document est un CRD Chaos Mesh connu (apiVersion + kind), (3) les champs
obligatoires par kind sont présents, (4) tout sélecteur cible le namespace
`qualitos` et un label `app.kubernetes.io/name` réellement déployé.

Usage : python infra/chaos/validate.py
Sortie : code 0 si tout est valide, 1 sinon.
"""
from __future__ import annotations

import glob
import os
import sys

import yaml

API_VERSION = "chaos-mesh.org/v1alpha1"

# Kinds Chaos Mesh réels (https://chaos-mesh.org/docs/) utilisés ici.
KNOWN_KINDS = {
    "PodChaos",
    "NetworkChaos",
    "StressChaos",
    "IOChaos",
    "Workflow",
    "Schedule",
}

# Labels app.kubernetes.io/name réellement produits par le chart Helm
# (infra/k8s/qualitos/values.yaml -> services.<name>, posés en matchLabels par
#  infra/k8s/qualitos/templates/deployment.yaml).
KNOWN_SERVICE_NAMES = {
    "web",
    "api-core",
    "api-quality-engine",
    "api-iot-hub",
    "ai-service",
    "blockchain-service",
}

NAMESPACE = "qualitos"

HERE = os.path.dirname(os.path.abspath(__file__))


def _check_selector(sel: dict, ctx: str, errors: list[str]) -> None:
    """Vérifie qu'un selector cible le bon namespace et un label connu."""
    if not isinstance(sel, dict):
        return
    namespaces = sel.get("namespaces", [])
    if namespaces and NAMESPACE not in namespaces:
        errors.append(f"{ctx}: selector.namespaces={namespaces} ne contient pas '{NAMESPACE}'")
    labels = sel.get("labelSelectors", {})
    name = labels.get("app.kubernetes.io/name")
    if name is not None and name not in KNOWN_SERVICE_NAMES:
        errors.append(
            f"{ctx}: label app.kubernetes.io/name='{name}' inconnu "
            f"(attendu parmi {sorted(KNOWN_SERVICE_NAMES)})"
        )


def _walk_selectors(node, ctx: str, errors: list[str]) -> None:
    """Parcourt récursivement le doc pour valider tout 'selector' rencontré."""
    if isinstance(node, dict):
        if "selector" in node and isinstance(node["selector"], dict):
            _check_selector(node["selector"], ctx, errors)
        for k, v in node.items():
            _walk_selectors(v, f"{ctx}.{k}", errors)
    elif isinstance(node, list):
        for i, v in enumerate(node):
            _walk_selectors(v, f"{ctx}[{i}]", errors)


def _required_for(kind: str, spec: dict) -> list[str]:
    """Champs obligatoires minimaux par kind."""
    missing = []
    if kind == "PodChaos":
        for f in ("action", "mode", "selector"):
            if f not in spec:
                missing.append(f)
    elif kind == "NetworkChaos":
        for f in ("action", "mode", "selector"):
            if f not in spec:
                missing.append(f)
    elif kind == "StressChaos":
        for f in ("mode", "selector", "stressors"):
            if f not in spec:
                missing.append(f)
    elif kind == "IOChaos":
        for f in ("action", "mode", "selector", "volumePath"):
            if f not in spec:
                missing.append(f)
    elif kind == "Workflow":
        for f in ("entry", "templates"):
            if f not in spec:
                missing.append(f)
    elif kind == "Schedule":
        for f in ("schedule", "type"):
            if f not in spec:
                missing.append(f)
    return missing


def validate_file(path: str) -> list[str]:
    errors: list[str] = []
    rel = os.path.relpath(path, HERE)
    try:
        with open(path, "r", encoding="utf-8") as fh:
            docs = list(yaml.safe_load_all(fh))
    except yaml.YAMLError as exc:
        return [f"{rel}: YAML invalide — {exc}"]

    docs = [d for d in docs if d is not None]
    if not docs:
        return [f"{rel}: aucun document YAML"]

    for idx, doc in enumerate(docs):
        ctx = f"{rel}[doc {idx}]"
        if not isinstance(doc, dict):
            errors.append(f"{ctx}: document non mappé (attendu un objet K8s)")
            continue
        api = doc.get("apiVersion")
        kind = doc.get("kind")
        if api != API_VERSION:
            errors.append(f"{ctx}: apiVersion='{api}' (attendu '{API_VERSION}')")
        if kind not in KNOWN_KINDS:
            errors.append(f"{ctx}: kind='{kind}' inconnu (attendu parmi {sorted(KNOWN_KINDS)})")
            continue
        meta = doc.get("metadata", {})
        if meta.get("namespace") != NAMESPACE:
            errors.append(f"{ctx}: metadata.namespace='{meta.get('namespace')}' (attendu '{NAMESPACE}')")
        if not meta.get("name"):
            errors.append(f"{ctx}: metadata.name manquant")
        spec = doc.get("spec", {})
        if not isinstance(spec, dict):
            errors.append(f"{ctx}: spec absent ou non mappé")
            continue
        for f in _required_for(kind, spec):
            errors.append(f"{ctx} ({kind}): champ spec.{f} manquant")
        _walk_selectors(spec, ctx, errors)
    return errors


def main() -> int:
    patterns = [
        os.path.join(HERE, "experiments", "*.yaml"),
        os.path.join(HERE, "workflows", "*.yaml"),
        os.path.join(HERE, "schedules", "*.yaml"),
    ]
    files = sorted(f for p in patterns for f in glob.glob(p))
    if not files:
        print("Aucun manifest trouvé sous infra/chaos/.", file=sys.stderr)
        return 1

    all_errors: list[str] = []
    for f in files:
        errs = validate_file(f)
        status = "OK" if not errs else "ÉCHEC"
        print(f"[{status}] {os.path.relpath(f, HERE)}")
        all_errors.extend(errs)

    print("-" * 60)
    if all_errors:
        print(f"{len(all_errors)} erreur(s) :")
        for e in all_errors:
            print(f"  - {e}")
        return 1
    print(f"{len(files)} manifest(s) valides (apiVersion {API_VERSION}, "
          f"kinds {sorted(KNOWN_KINDS)}, selectors cohérents avec infra/k8s).")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
