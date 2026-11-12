"""Le jeton qu'un formulaire reçoit en s'affichant, et rend en se soumettant.

Le piège à robots attrape l'automate qui remplit TOUT ce qu'il trouve. Il ne
voit pas celui qui a été réglé sur ce formulaire-ci, champ par champ : celui-là
laisse le piège vide et passe. Contre lui, la seule chose qu'un automate ne
peut pas imiter sans y perdre son intérêt économique, c'est le TEMPS — personne
ne lit un formulaire de six champs et n'écrit sa demande en moins de trois
secondes.

Le formulaire demande donc un jeton en s'affichant, et le rend en se soumettant.
Le jeton porte l'instant de son émission, signé : le service peut mesurer le
temps écoulé sans rien mémoriser entre les deux appels.

**Pourquoi signé.** Un simple horodatage écrit par le navigateur se réécrit : il
suffirait d'annoncer une minute d'ancienneté. La signature rend l'instant
infalsifiable sans connaître le secret.

**Pourquoi le secret n'est pas configuré.** Il est tiré au hasard au démarrage et
ne quitte pas la mémoire du processus. Il n'y a donc ni secret à déposer dans un
coffre, ni secret à faire tourner, ni secret à fuir. Le prix est qu'un
redémarrage périme les jetons déjà distribués : la page ouverte au mauvais
moment reçoit un refus explicite qui lui dit de recharger. Pour un service qui
reçoit quelques demandes par jour et se redéploie rarement, c'est le bon
échange — et il suppose un seul exemplaire du service, exactement comme le
compteur de débit.
"""

from __future__ import annotations

import base64
import hmac
import secrets
import time
from hashlib import sha256

# Trois secondes. Non pas le temps de remplir le formulaire, mais le temps
# minimal pour qu'un être humain ait pu le faire : le navigateur demande le
# jeton au chargement de la page, pas au premier clic dans un champ.
DELAI_MINIMAL_S = 3.0

# Deux heures. Au-delà, l'onglet est resté ouvert : la demande est sans doute
# sincère, mais on préfère un jeton frais à un jeton qui circule depuis la
# veille. Le visiteur n'a qu'à recharger, et on le lui dit.
PEREMPTION_S = 7200.0


class JetonRefuse(ValueError):
    """Le jeton manque, ne se vérifie pas, ou vient d'arriver."""

    def __init__(self, raison: str) -> None:
        super().__init__(raison)
        self.raison = raison


class Jetons:
    """Émet et vérifie les jetons d'un seul processus."""

    def __init__(self, horloge=time.time) -> None:
        self._secret = secrets.token_bytes(32)
        self._horloge = horloge

    def emets(self) -> str:
        """Un jeton tout neuf, à poser dans le formulaire.

        L'instant est porté en MILLISECONDES ENTIÈRES, et non en secondes
        décimales : un nombre à virgule contient un point, or le point sépare les
        deux parties du jeton. Écrit « 1000000.000 », l'instant faisait trois
        morceaux au lieu de deux et tout jeton était rejeté.
        """
        instant = str(int(self._horloge() * 1000))
        return f"{instant}.{self._signe(instant)}"

    def verifie(self, jeton: str | None) -> None:
        """Accepte, ou lève en disant ce qui n'allait pas."""
        morceaux = (jeton or "").split(".")
        if len(morceaux) != 2 or not morceaux[0] or not morceaux[1]:
            raise JetonRefuse("jeton absent ou illisible")

        instant, signature = morceaux
        # `compare_digest` et non `==` : une comparaison qui s'arrête au premier
        # octet different laisse mesurer, essai apres essai, combien d'octets
        # etaient justes.
        if not hmac.compare_digest(signature, self._signe(instant)):
            raise JetonRefuse("jeton non reconnu — rechargez la page")

        try:
            emis_le = int(instant) / 1000
        except ValueError:  # pragma: no cover - la signature l'interdit déjà
            raise JetonRefuse("jeton illisible") from None

        age = self._horloge() - emis_le
        if age < DELAI_MINIMAL_S:
            raise JetonRefuse("formulaire soumis trop vite")
        if age > PEREMPTION_S:
            raise JetonRefuse("jeton périmé — rechargez la page")

    def _signe(self, instant: str) -> str:
        brut = hmac.new(self._secret, instant.encode("ascii"), sha256).digest()
        return base64.urlsafe_b64encode(brut).decode("ascii").rstrip("=")
