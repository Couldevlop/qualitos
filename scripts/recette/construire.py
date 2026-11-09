# -*- coding: utf-8 -*-
"""Produit les cinq livrables de recette en une commande.

    python scripts/recette/construire.py

Les documents ne sont pas versionnes : ils se regenerent. Ce qui est versionne,
c'est leur contenu, sous forme de code lisible en revue -- un tableau Excel ne
se relit pas dans une pull request, une liste de cas en Python, si.
"""

import os
import runpy
import sys

ICI = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, ICI)

for module in ("recette_xlsx", "recette_docx", "recette_pptx"):
    runpy.run_path(os.path.join(ICI, module + ".py"), run_name="__main__")
