"""La surface HTTP du formulaire de contact : une seule route utile.

Ce service ne stocke rien, ne lit aucune base, n'a aucun compte. Il reçoit une
demande, en fait un courriel, et l'oublie. C'est délibérément le service le plus
ennuyeux du dépôt : il est public et sans authentification, donc tout ce qu'il
ne fait pas est une surface qu'il n'offre pas.
"""

from __future__ import annotations

import json
import logging
import os

from fastapi import APIRouter, FastAPI, Request, Response, status
from fastapi.responses import JSONResponse

from .courriel import EnvoiImpossible, Expediteur, ExpediteurSmtp, ReglagesSmtp
from .debit import Debit
from .domaine import Demande, DemandeInvalide

journal = logging.getLogger("site-contact")

# Cinq demandes par heure et par adresse. Un prospect en envoie une ; cinq
# laissent la place à une erreur de saisie et à un bureau derrière une même
# sortie internet, sans faire du formulaire un relais ouvert.
DEBIT_MAX = int(os.environ.get("CONTACT_DEBIT_MAX", "5"))
DEBIT_PERIODE_S = float(os.environ.get("CONTACT_DEBIT_PERIODE_S", "3600"))

# Au-delà, on ne lit même pas le corps. Le champ le plus long est borné à
# 4 000 caractères ; 32 ko laissent toute la marge utile.
TAILLE_MAX_OCTETS = 32 * 1024


def adresse_appelante(requete: Request) -> str:
    """L'adresse du visiteur, telle que l'ingress l'a CONSTATÉE.

    Derrière un proxy, l'adresse de connexion est toujours celle du proxy :
    sans lire l'entête transmise, la limite de débit s'appliquerait au proxy,
    donc à tout le monde à la fois.

    Mais l'entête n'est pas une source sûre. `X-Forwarded-For` est une LISTE que
    chaque maillon ALLONGE PAR LA DROITE : le maillon de gauche est celui que
    l'appelant a écrit lui-même, et il écrit ce qu'il veut. Lire la gauche, c'est
    laisser un robot changer d'identité à chaque requête et ne plus être limité
    du tout — la borne existerait encore, elle ne bornerait plus personne.

    On lit donc la DROITE : la dernière valeur est celle qu'ajoute l'ingress, à
    partir de l'adresse de la connexion qu'il a réellement reçue. Elle n'est pas
    falsifiable par l'appelant.

    Si un jour un second proxy s'intercale, cette valeur devient l'adresse du
    proxy interne et tout le monde partage à nouveau un seul compteur : la
    limite se resserre au lieu de s'ouvrir. C'est le bon sens de l'erreur.
    """
    transmise = requete.headers.get("x-forwarded-for", "")
    if transmise:
        maillons = [m.strip() for m in transmise.split(",") if m.strip()]
        if maillons:
            return maillons[-1]
    return requete.client.host if requete.client else "inconnue"


def construis(expediteur: Expediteur | None = None, debit: Debit | None = None) -> FastAPI:
    """Assemble l'application. L'injection sert d'abord aux tests."""
    envoi = expediteur if expediteur is not None else ExpediteurSmtp(ReglagesSmtp.depuis_environnement())
    compteur = debit if debit is not None else Debit(DEBIT_MAX, DEBIT_PERIODE_S)

    app = FastAPI(title="QualitOS — formulaire de contact", docs_url=None, redoc_url=None, openapi_url=None)
    routes = APIRouter()

    @routes.get("/healthz")
    def vitalite() -> dict[str, str]:
        return {"etat": "ok"}

    @routes.post("/api/contact")
    async def recevoir(requete: Request) -> Response:
        if not compteur.accepte(adresse_appelante(requete)):
            return JSONResponse(
                {"message": "Trop de demandes depuis cette connexion. Réessayez dans une heure, "
                            "ou écrivez directement à contact@qualitos.fr."},
                status_code=status.HTTP_429_TOO_MANY_REQUESTS,
            )

        brut = await requete.body()
        if len(brut) > TAILLE_MAX_OCTETS:
            return JSONResponse(
                {"message": "Demande trop volumineuse."},
                status_code=status.HTTP_413_REQUEST_ENTITY_TOO_LARGE,
            )

        try:
            donnees = json.loads(brut or b"{}")
        except ValueError:
            return JSONResponse({"message": "Demande illisible."}, status_code=status.HTTP_400_BAD_REQUEST)
        if not isinstance(donnees, dict):
            return JSONResponse({"message": "Demande illisible."}, status_code=status.HTTP_400_BAD_REQUEST)

        # Piège à robots : un champ que le formulaire garde vide et qu'aucun
        # humain ne voit. Rempli, c'est un automate — on répond comme si tout
        # allait bien, sans rien envoyer. Lui dire qu'il a été repéré ne ferait
        # que lui apprendre à mieux se cacher.
        if str(donnees.get("piege", "") or "").strip():
            journal.info("demande ignorée : piège à robots rempli")
            return JSONResponse({"message": "Demande reçue."}, status_code=status.HTTP_202_ACCEPTED)

        try:
            demande = Demande.depuis(donnees)
        except DemandeInvalide as refus:
            return JSONResponse(
                {"champ": refus.champ, "message": f"Champ « {refus.champ} » : {refus.raison}."},
                status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
            )

        try:
            envoi.envoie(demande)
        except EnvoiImpossible as echec:
            # Le visiteur n'a pas à connaître l'état de notre relais ; mais il
            # doit savoir que son message n'est pas parti, et par où passer.
            journal.error("envoi SMTP impossible : %s", echec)
            return JSONResponse(
                {"message": "L'envoi n'a pas abouti. Écrivez directement à contact@qualitos.fr."},
                status_code=status.HTTP_502_BAD_GATEWAY,
            )

        # Rien du contenu n'est journalisé : ce sont les coordonnées d'un tiers
        # (§22-9). On note qu'une demande est passée, pas qui l'a envoyée.
        journal.info("demande transmise")
        return JSONResponse({"message": "Demande reçue."}, status_code=status.HTTP_202_ACCEPTED)

    app.include_router(routes)
    return app
