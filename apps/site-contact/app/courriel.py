"""Expédition SMTP, et la configuration qui la rend possible.

Le reste du service ne connaît que le protocole `Expediteur` : une demande
entre, un message part. Ce fichier est le seul à savoir qu'il y a un relais
SMTP derrière, ce qui permet de l'échanger — ou de ne rien envoyer du tout
pendant un test — sans toucher au métier.
"""

from __future__ import annotations

import os
import smtplib
import ssl
from dataclasses import dataclass
from email.message import EmailMessage
from typing import Protocol

from .domaine import Demande


class Expediteur(Protocol):
    def envoie(self, demande: Demande) -> None: ...


class EnvoiImpossible(RuntimeError):
    """Le relais n'a pas accepté le message."""


@dataclass(frozen=True)
class ReglagesSmtp:
    hote: str
    port: int
    utilisateur: str
    mot_de_passe: str
    expediteur: str
    destinataire: str
    # OVH, comme la plupart des relais, écoute en 465 (TLS implicite) et en 587
    # (STARTTLS). Les deux sont chiffrés ; ils ne se négocient simplement pas au
    # même moment, et se tromper donne une erreur illisible côté client.
    tls_implicite: bool = True
    delai_s: float = 15.0

    @staticmethod
    def depuis_environnement() -> "ReglagesSmtp":
        """Lit la configuration, et refuse de démarrer si elle manque.

        Échouer AU DÉMARRAGE et non au premier envoi : un service qui démarre
        sans pouvoir expédier répondrait « merci » à des visiteurs dont le
        message ne partirait jamais. C'est le pire des deux mondes — la panne
        est invisible précisément là où elle coûte le plus.
        """
        manquantes = [
            nom
            for nom in ("SMTP_HOTE", "SMTP_UTILISATEUR", "SMTP_MOT_DE_PASSE", "SMTP_EXPEDITEUR")
            if not os.environ.get(nom)
        ]
        if manquantes:
            raise RuntimeError(
                "configuration SMTP incomplète : " + ", ".join(manquantes)
            )

        port = int(os.environ.get("SMTP_PORT", "465"))
        return ReglagesSmtp(
            hote=os.environ["SMTP_HOTE"],
            port=port,
            utilisateur=os.environ["SMTP_UTILISATEUR"],
            mot_de_passe=os.environ["SMTP_MOT_DE_PASSE"],
            expediteur=os.environ["SMTP_EXPEDITEUR"],
            destinataire=os.environ.get("SMTP_DESTINATAIRE", "contact@qualitos.fr"),
            tls_implicite=os.environ.get("SMTP_TLS_IMPLICITE", "1") != "0",
        )


def compose(demande: Demande, reglages: ReglagesSmtp) -> EmailMessage:
    message = EmailMessage()
    message["Subject"] = demande.sujet()

    # L'EXPÉDITEUR est notre propre adresse, jamais celle du visiteur. L'y mettre
    # ferait envoyer un message au nom d'un domaine qui ne nous a pas autorisés :
    # SPF et DMARC le rejettent, et la boîte ne reçoit rien. C'est Reply-To qui
    # porte l'adresse du visiteur — répondre tombe donc bien chez lui.
    message["From"] = reglages.expediteur
    message["To"] = reglages.destinataire
    message["Reply-To"] = f"{demande.nom} <{demande.courriel}>"
    message.set_content(demande.corps())
    return message


class ExpediteurSmtp:
    def __init__(self, reglages: ReglagesSmtp) -> None:
        self._reglages = reglages

    def envoie(self, demande: Demande) -> None:
        r = self._reglages
        message = compose(demande, r)
        contexte = ssl.create_default_context()
        try:
            if r.tls_implicite:
                with smtplib.SMTP_SSL(r.hote, r.port, timeout=r.delai_s, context=contexte) as relais:
                    relais.login(r.utilisateur, r.mot_de_passe)
                    relais.send_message(message)
            else:
                with smtplib.SMTP(r.hote, r.port, timeout=r.delai_s) as relais:
                    relais.starttls(context=contexte)
                    relais.login(r.utilisateur, r.mot_de_passe)
                    relais.send_message(message)
        except (smtplib.SMTPException, OSError) as echec:
            # Le détail reste dans la trace du service ; l'appelant reçoit une
            # phrase, pas l'adresse du relais ni le nom du compte (§22-9).
            raise EnvoiImpossible(str(echec)) from echec
