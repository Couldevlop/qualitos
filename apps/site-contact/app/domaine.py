"""Le métier de la boîte aux lettres : une demande, et ce qui en fait une.

Aucune dépendance au web ni au courriel. Ce fichier sait ce qu'est une demande
recevable et comment elle se lit ; il ignore qui la reçoit et comment elle part.
"""

from __future__ import annotations

import re
from dataclasses import dataclass, field
from datetime import datetime, timezone

# Bornes de longueur. Elles ne protègent pas d'un attaquant — un formulaire
# public n'est pas un périmètre de confiance — mais elles bornent ce qui entre
# en mémoire et ce qui part dans un courriel.
LONGUEUR_MAX = {
    "nom": 120,
    "fonction": 120,
    "organisation": 160,
    "courriel": 254,  # longueur maximale d'une adresse, RFC 5321 §4.5.3.1
    "telephone": 40,
    "secteur": 60,
    "besoin": 4000,
}

# Validation d'adresse DÉLIBÉRÉMENT PERMISSIVE. La grammaire réelle d'une
# adresse (RFC 5322) accepte des formes qu'aucune expression régulière courte ne
# décrit, et toute tentative d'être strict finit par refuser des adresses
# valides — ce qui, sur un formulaire de prospection, écarte un client. La seule
# preuve qu'une adresse existe est qu'un message y arrive.
_ADRESSE = re.compile(r"^[^@\s]+@[^@\s.]+(\.[^@\s.]+)+$")

# Caractères de contrôle et retours à la ligne : ils n'ont rien à faire dans un
# en-tête de courriel. Les y laisser passer, c'est l'injection d'en-tête (un
# « \nBcc: » dans le champ « nom » ajoute un destinataire au message).
_INTERDITS = re.compile(r"[\r\n\x00-\x08\x0b\x0c\x0e-\x1f\x7f]")


class DemandeInvalide(ValueError):
    """La demande ne peut pas être traitée, et on sait dire pourquoi."""

    def __init__(self, champ: str, raison: str) -> None:
        super().__init__(f"{champ} : {raison}")
        self.champ = champ
        self.raison = raison


def _propre(champ: str, valeur: str | None) -> str:
    texte = (valeur or "").strip()
    if _INTERDITS.search(texte):
        raise DemandeInvalide(champ, "caractères de contrôle interdits")
    maximum = LONGUEUR_MAX[champ]
    if len(texte) > maximum:
        raise DemandeInvalide(champ, f"au plus {maximum} caractères")
    return texte


@dataclass(frozen=True)
class Demande:
    """Une demande de démonstration, déjà validée."""

    nom: str
    courriel: str
    fonction: str = ""
    organisation: str = ""
    telephone: str = ""
    secteur: str = ""
    besoin: str = ""
    recue_le: datetime = field(default_factory=lambda: datetime.now(timezone.utc))

    @staticmethod
    def depuis(brut: dict[str, object]) -> "Demande":
        """Construit une demande à partir de champs bruts, ou refuse.

        Les deux seuls champs exigés sont le nom et l'adresse : ce sont les
        seuls sans lesquels on ne peut pas répondre. Tout le reste aide la
        conversation sans la conditionner — un formulaire qui exige plus que le
        nécessaire perd des demandes qu'il aurait suffi d'accepter.
        """
        valeurs = {
            champ: _propre(champ, str(brut.get(champ, "") or ""))
            for champ in LONGUEUR_MAX
        }

        if not valeurs["nom"]:
            raise DemandeInvalide("nom", "obligatoire")
        if not valeurs["courriel"]:
            raise DemandeInvalide("courriel", "obligatoire")
        if not _ADRESSE.match(valeurs["courriel"]):
            raise DemandeInvalide("courriel", "adresse non reconnaissable")

        return Demande(**valeurs)

    # --- mise en forme du message -------------------------------------------

    def sujet(self) -> str:
        qui = self.organisation or self.nom
        return f"Demande de démonstration QualitOS — {qui}"

    def corps(self) -> str:
        """Texte brut, volontairement.

        Le corps reprend ce qu'un inconnu a saisi. En HTML, un oubli
        d'échappement passerait inaperçu jusqu'au jour où il servirait ; en
        texte brut il n'y a rien à échapper.
        """
        lignes = [
            ("Nom", self.nom),
            ("Fonction", self.fonction),
            ("Organisation", self.organisation),
            ("Courriel", self.courriel),
            ("Téléphone", self.telephone),
            ("Secteur", self.secteur),
        ]
        bloc = "\n".join(f"{intitule} : {valeur}" for intitule, valeur in lignes if valeur)
        besoin = f"\n\nCe qu'il ou elle cherche à régler :\n{self.besoin}" if self.besoin else ""
        horodatage = self.recue_le.strftime("%d/%m/%Y à %H:%M UTC")
        return f"{bloc}{besoin}\n\n—\nReçue le {horodatage} depuis qualitos.fr."
