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
from .debit import Debit, DebitGlobal
from .domaine import Demande, DemandeInvalide
from .jeton import JetonRefuse, Jetons

journal = logging.getLogger("site-contact")

# Cinq demandes par heure et par adresse. Un prospect en envoie une ; cinq
# laissent la place à une erreur de saisie et à un bureau derrière une même
# sortie internet, sans faire du formulaire un relais ouvert.
DEBIT_MAX = int(os.environ.get("CONTACT_DEBIT_MAX", "5"))
DEBIT_PERIODE_S = float(os.environ.get("CONTACT_DEBIT_PERIODE_S", "3600"))

# Le plafond du service entier, toutes adresses confondues. La borne par adresse
# arrête celui qui insiste ; elle n'arrête pas un réseau réparti sur mille
# adresses, chacune restant sous sa limite. Soixante par heure est très haut
# devant le trafic réel — quelques demandes par jour : ce plafond ne se voit
# jamais en exploitation normale, et il borne le pire.
DEBIT_GLOBAL_MAX = int(os.environ.get("CONTACT_DEBIT_GLOBAL_MAX", "60"))

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


def construis(expediteur: Expediteur | None = None, debit: Debit | None = None,
               global_: DebitGlobal | None = None, jetons: Jetons | None = None) -> FastAPI:
    """Assemble l'application. L'injection sert d'abord aux tests."""
    envoi = expediteur if expediteur is not None else ExpediteurSmtp(ReglagesSmtp.depuis_environnement())
    compteur = debit if debit is not None else Debit(DEBIT_MAX, DEBIT_PERIODE_S)
    plafond = global_ if global_ is not None else DebitGlobal(DEBIT_GLOBAL_MAX, DEBIT_PERIODE_S)
    sceau = jetons if jetons is not None else Jetons()

    app = FastAPI(title="QualitOS — formulaire de contact", docs_url=None, redoc_url=None, openapi_url=None)
    routes = APIRouter()

    @routes.get("/healthz")
    def vitalite() -> dict[str, str]:
        return {"etat": "ok"}

    @routes.get("/api/contact/jeton")
    def jeton() -> dict[str, str]:
        """Le jeton que le formulaire demande en s'affichant.

        Il porte l'instant de son émission, signé : la soumission peut être
        datee sans que le service mémorise quoi que ce soit entre les deux
        appels. Voir `jeton.py` pour la raison d'être — le temps est la seule
        chose qu'un automate ne peut pas imiter sans y perdre son intérêt.

        Cette route n'est pas comptée dans la limite de débit : elle n'envoie
        rien et ne coûte rien. La compter ferait tomber le visiteur qui recharge
        la page trois fois avant de se décider.
        """
        return {"jeton": sceau.emets()}

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
                status_code=status.HTTP_413_CONTENT_TOO_LARGE,
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

        # Le jeton, et avec lui le temps de remplissage. Le piège à robots
        # attrape l'automate qui remplit tout ce qu'il trouve ; il ne voit pas
        # celui qui a été réglé sur ce formulaire-ci, champ par champ. Ici, on
        # répond en revanche FRANCHEMENT : contrairement au piège, un refus
        # légitime existe — l'onglet resté ouvert depuis la veille — et le
        # visiteur doit savoir qu'il lui suffit de recharger.
        try:
            sceau.verifie(str(donnees.get("jeton", "") or ""))
        except JetonRefuse as refus:
            # La raison précise est JOURNALISÉE, pas répondue — sauf quand elle
            # apprend au visiteur ce qu'il doit faire. « Jeton périmé » mérite
            # d'être dit : l'onglet est resté ouvert, il suffit de recharger, et
            # le taire laisserait une personne sincère devant un refus muet.
            # « Soumis trop vite » ne mérite pas de l'être : aucun être humain
            # ne le déclenche, et le nommer dit à l'automate exactement quoi
            # corriger — attendre trois secondes.
            journal.info("demande refusée : %s", refus.raison)
            message = (f"Demande non acceptée ({refus.raison}). Rechargez la page et "
                       "réessayez, ou écrivez à contact@qualitos.fr."
                       if refus.rechargeable else
                       "Demande non acceptée. Réessayez dans un instant, "
                       "ou écrivez à contact@qualitos.fr.")
            return JSONResponse(
                {"message": message},
                status_code=status.HTTP_400_BAD_REQUEST,
            )

        try:
            demande = Demande.depuis(donnees)
        except DemandeInvalide as refus:
            return JSONResponse(
                {"champ": refus.champ, "message": f"Champ « {refus.champ} » : {refus.raison}."},
                status_code=status.HTTP_422_UNPROCESSABLE_CONTENT,
            )

        # Le plafond du service, compté ICI et non à l'entrée : il dit ce que la
        # BOÎTE AUX LETTRES accepte en une heure, et seule une demande arrivée
        # jusqu'ici lui coûtera quelque chose. Placé en tête, il se consommait
        # sur des requêtes qui n'envoyaient rien — corps malformé, piège rempli,
        # jeton absent : douze adresses et soixante requêtes de rebut, chacune
        # sous la borne par adresse, fermaient le formulaire pour tout le monde
        # pendant une heure. Une protection devenue le moyen le moins cher de
        # faire taire le service.
        if not plafond.accepte():
            # Le visiteur n'y est pour rien et reçoit la même porte de sortie :
            # une adresse à laquelle écrire.
            journal.warning("plafond global atteint : demande refusée")
            return JSONResponse(
                {"message": "Le formulaire reçoit trop de demandes en ce moment. "
                            "Écrivez directement à contact@qualitos.fr."},
                status_code=status.HTTP_429_TOO_MANY_REQUESTS,
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
        # (§22-9). On note qu'une demande est passée, pas qui l'a envoyée. Le
        # NOMBRE de soupçons est un compte, pas un contenu : il dit si le filtre
        # travaille sans révéler ce qu'on a reçu.
        if demande.soupcons:
            journal.info("demande transmise, %d point(s) à vérifier", len(demande.soupcons))
        else:
            journal.info("demande transmise")
        return JSONResponse({"message": "Demande reçue."}, status_code=status.HTTP_202_ACCEPTED)

    app.include_router(routes)
    return app
