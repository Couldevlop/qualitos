"""Limitation de débit par adresse d'appel.

Un formulaire public sans borne devient un relais de courriel pour qui le
trouve. Le compteur vit en mémoire : le service tourne en un exemplaire, et un
compteur partagé exigerait un Redis pour protéger une boîte aux lettres qui
reçoit quelques messages par jour. Un redémarrage remet les compteurs à zéro,
ce qui est acceptable — et c'est la raison pour laquelle on l'écrit ici.
"""

from __future__ import annotations

import threading
import time
from collections import deque


class Debit:
    """Fenêtre glissante : au plus N demandes par adresse et par période."""

    def __init__(self, maximum: int, periode_s: float, horloge=time.monotonic) -> None:
        if maximum < 1:
            raise ValueError("maximum doit valoir au moins 1")
        self._maximum = maximum
        self._periode = periode_s
        self._horloge = horloge
        self._par_adresse: dict[str, deque[float]] = {}
        self._verrou = threading.Lock()

    def accepte(self, adresse: str) -> bool:
        """Enregistre une tentative et dit si elle passe."""
        maintenant = self._horloge()
        with self._verrou:
            passages = self._par_adresse.setdefault(adresse, deque())
            while passages and maintenant - passages[0] >= self._periode:
                passages.popleft()

            # Les adresses devenues inactives sont oubliées. Sans ce ménage, un
            # balayage d'adresses ferait croître la table sans fin — la limite
            # de débit deviendrait elle-même la fuite de mémoire.
            if not passages:
                self._par_adresse.pop(adresse, None)
                passages = self._par_adresse.setdefault(adresse, deque())

            if len(passages) >= self._maximum:
                return False
            passages.append(maintenant)
            return True
