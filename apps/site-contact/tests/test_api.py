"""La route publique, de bout en bout, sans relais SMTP."""

import pytest
from fastapi.testclient import TestClient

from app.api import construis
from app.courriel import EnvoiImpossible
from app.debit import Debit, DebitGlobal
from app.jeton import DELAI_MINIMAL_S, Jetons


class ExpediteurEspion:
    def __init__(self, echoue=False):
        self.envoyees = []
        self.echoue = echoue

    def envoie(self, demande):
        if self.echoue:
            raise EnvoiImpossible("relais injoignable")
        self.envoyees.append(demande)


@pytest.fixture
def espion():
    return ExpediteurEspion()


class Horloge:
    """Une horloge qu'on avance à la main.

    Le service refuse un formulaire soumis en moins de trois secondes. Attendre
    réellement ferait des bancs qui durent, et des bancs qui durent finissent
    par être désactivés.
    """

    def __init__(self, depart=1_000_000.0):
        self.instant = depart

    def __call__(self):
        return self.instant

    def avance(self, secondes):
        self.instant += secondes


@pytest.fixture
def horloge():
    return Horloge()


@pytest.fixture
def jetons(horloge):
    return Jetons(horloge=horloge)


@pytest.fixture
def client(espion, jetons):
    return TestClient(construis(expediteur=espion, debit=Debit(100, 3600),
                                global_=DebitGlobal(100, 3600), jetons=jetons))


@pytest.fixture
def jeton(client, horloge):
    """Un jeton prêt à servir : émis, puis le temps d'un remplissage humain."""
    valeur = client.get("/api/contact/jeton").json()["jeton"]
    horloge.avance(DELAI_MINIMAL_S + 1)
    return valeur


DEMANDE = {
    "nom": "Claire Vidal",
    "fonction": "Directrice qualité",
    "organisation": "Fonderie du Nord",
    "courriel": "claire@fonderie.fr",
    "telephone": "0601020304",
    "secteur": "industrie",
    "besoin": "Nous visons l'ISO 9001 en mars.",
}


def test_la_vitalite_repond(client):
    assert client.get("/healthz").json() == {"etat": "ok"}


def test_une_demande_valide_est_transmise(client, espion, jeton):
    reponse = client.post("/api/contact", json={**DEMANDE, "jeton": jeton})
    assert reponse.status_code == 202
    assert len(espion.envoyees) == 1
    assert espion.envoyees[0].organisation == "Fonderie du Nord"


def test_un_champ_invalide_est_refuse_en_disant_lequel(client, espion, jeton):
    reponse = client.post("/api/contact", json={**DEMANDE, "jeton": jeton, "courriel": "pas-une-adresse"})
    assert reponse.status_code == 422
    assert reponse.json()["champ"] == "courriel"
    assert espion.envoyees == []


def test_un_corps_illisible_est_refuse(client):
    reponse = client.post(
        "/api/contact", content=b"{ceci n'est pas du json", headers={"content-type": "application/json"}
    )
    assert reponse.status_code == 400


def test_un_corps_qui_n_est_pas_un_objet_est_refuse(client):
    assert client.post("/api/contact", json=["Claire"]).status_code == 400


def test_un_corps_trop_gros_est_refuse_sans_etre_analyse(client, espion):
    reponse = client.post("/api/contact", content=b"x" * (33 * 1024),
                          headers={"content-type": "application/json"})
    assert reponse.status_code == 413
    assert espion.envoyees == []


def test_le_piege_a_robots_fait_semblant_d_accepter(client, espion, jeton):
    """Rempli, c'est un automate. On répond 202 sans rien envoyer."""
    reponse = client.post("/api/contact", json={**DEMANDE, "jeton": jeton, "piege": "http://spam"})
    assert reponse.status_code == 202
    assert espion.envoyees == []


def test_un_piege_vide_ne_gene_pas(client, espion, jeton):
    assert client.post("/api/contact", json={**DEMANDE, "jeton": jeton, "piege": "  "}).status_code == 202
    assert len(espion.envoyees) == 1


def test_un_relais_en_panne_donne_une_issue_au_visiteur(jetons, jeton):
    espion = ExpediteurEspion(echoue=True)
    client = TestClient(construis(expediteur=espion, debit=Debit(100, 3600), jetons=jetons))
    reponse = client.post("/api/contact", json={**DEMANDE, "jeton": jeton})
    assert reponse.status_code == 502
    assert "contact@qualitos.fr" in reponse.json()["message"]


def test_la_limite_de_debit_s_applique(jetons, espion, jeton):
    client = TestClient(construis(expediteur=espion, debit=Debit(2, 3600), jetons=jetons))
    codes = [client.post("/api/contact", json={**DEMANDE, "jeton": jeton}).status_code for _ in range(3)]
    assert codes == [202, 202, 429]
    assert len(espion.envoyees) == 2


def test_la_limite_distingue_les_adresses_constatees_par_le_proxy(jetons, espion, jeton):
    """Sans lire l'entête, tout le monde partagerait le quota du proxy."""
    client = TestClient(construis(expediteur=espion, debit=Debit(1, 3600), jetons=jetons))
    premier = client.post("/api/contact", json={**DEMANDE, "jeton": jeton}, headers={"x-forwarded-for": "1.2.3.4"})
    autre = client.post("/api/contact", json={**DEMANDE, "jeton": jeton}, headers={"x-forwarded-for": "5.6.7.8"})
    rejoue = client.post("/api/contact", json={**DEMANDE, "jeton": jeton}, headers={"x-forwarded-for": "1.2.3.4"})
    assert [premier.status_code, autre.status_code, rejoue.status_code] == [202, 202, 429]


def test_un_appelant_ne_peut_pas_se_renommer_en_prefixant_l_entete(jetons, espion, jeton):
    """La liste s'allonge PAR LA DROITE : la gauche est ce que l'appelant écrit.

    Lire la gauche laisserait un robot changer d'identité à chaque requête, et
    la limite de débit ne limiterait plus personne. C'est la dernière valeur,
    celle que l'ingress ajoute d'après la connexion reçue, qui fait foi.
    """
    client = TestClient(construis(expediteur=espion, debit=Debit(1, 3600), jetons=jetons))
    vrai = client.post("/api/contact", json={**DEMANDE, "jeton": jeton}, headers={"x-forwarded-for": "9.9.9.9"})
    # Même appelant (9.9.9.9 reste à droite), mais il se prétend quelqu'un d'autre.
    deguise = client.post(
        "/api/contact", json={**DEMANDE, "jeton": jeton}, headers={"x-forwarded-for": "1.1.1.1, 9.9.9.9"}
    )
    assert vrai.status_code == 202
    assert deguise.status_code == 429


def test_une_entete_vide_retombe_sur_l_adresse_de_connexion(jetons, espion, jeton):
    client = TestClient(construis(expediteur=espion, debit=Debit(1, 3600), jetons=jetons))
    assert client.post("/api/contact", json={**DEMANDE, "jeton": jeton}, headers={"x-forwarded-for": " , "}).status_code == 202
    assert client.post("/api/contact", json={**DEMANDE, "jeton": jeton}, headers={"x-forwarded-for": " , "}).status_code == 429


# --- le jeton, vu de la route ------------------------------------------------


def test_le_formulaire_obtient_un_jeton(client):
    corps = client.get("/api/contact/jeton").json()
    assert corps["jeton"].count(".") == 1


def test_une_demande_sans_jeton_est_refusee_en_le_disant(client, espion):
    """Le piège à robots fait SEMBLANT d'accepter ; le jeton, non.

    La différence est voulue. Un piège rempli ne peut venir que d'un automate :
    lui dire qu'il a été repéré ne lui apprendrait qu'à mieux se cacher. Un
    jeton manquant, lui, a une cause légitime — l'onglet resté ouvert, le
    script qui n'a pas chargé — et le visiteur doit savoir qu'il suffit de
    recharger.
    """
    reponse = client.post("/api/contact", json=DEMANDE)
    assert reponse.status_code == 400
    assert "Rechargez" in reponse.json()["message"]
    assert espion.envoyees == []


def test_une_demande_soumise_instantanement_est_refusee(client, espion):
    jeton = client.get("/api/contact/jeton").json()["jeton"]
    # Aucune avance d'horloge : la soumission suit l'affichage d'un seul souffle.
    reponse = client.post("/api/contact", json={**DEMANDE, "jeton": jeton})
    assert reponse.status_code == 400
    assert "trop vite" in reponse.json()["message"]
    assert espion.envoyees == []


def test_demander_un_jeton_ne_consomme_pas_le_quota(espion, jetons, horloge):
    """Recharger la page trois fois avant de se décider ne doit rien coûter."""
    client = TestClient(construis(expediteur=espion, debit=Debit(1, 3600), jetons=jetons))
    for _ in range(5):
        assert client.get("/api/contact/jeton").status_code == 200
    jeton = client.get("/api/contact/jeton").json()["jeton"]
    horloge.avance(DELAI_MINIMAL_S + 1)
    assert client.post("/api/contact", json={**DEMANDE, "jeton": jeton}).status_code == 202


# --- le contenu --------------------------------------------------------------


def test_le_message_recu_le_12_novembre_2026_est_refuse(client, espion, jeton):
    """Le cas réel, celui qui a motivé le filtre.

    Une demande de démonstration dont le texte libre n'était qu'un lien vers un
    domaine sans rapport, écrit « hhtps:// » — avec une faute, pour traverser
    les filtres tout en restant cliquable pour un lecteur humain.
    """
    reponse = client.post("/api/contact", json={
        **DEMANDE, "jeton": jeton, "besoin": "hhtps://goparatge.online/fr/sw/qualitos"})
    assert reponse.status_code == 422
    assert reponse.json()["champ"] == "besoin"
    assert espion.envoyees == []


def test_un_lien_dans_le_nom_est_refuse(client, espion, jeton):
    reponse = client.post("/api/contact", json={
        **DEMANDE, "jeton": jeton, "nom": "Claire https://promo.example/offre"})
    assert reponse.status_code == 422
    assert reponse.json()["champ"] == "nom"
    assert espion.envoyees == []


def test_un_lien_dans_le_texte_libre_passe_mais_le_message_avertit(client, espion, jeton):
    reponse = client.post("/api/contact", json={
        **DEMANDE, "jeton": jeton,
        "besoin": "Nous visons l'ISO 9001, notre site est https://fonderie.fr/qualite"})
    assert reponse.status_code == 202

    demande = espion.envoyees[0]
    # Le soupçon se lit AVANT d'ouvrir, dans la liste des messages : écrit
    # seulement dans le corps, il serait lu après le lien qu'il vise à signaler.
    assert demande.sujet().startswith("[à vérifier]")
    corps = demande.corps()
    assert "À vérifier avant de répondre" in corps
    assert "N'ouvrez aucun lien" in corps


def test_une_demande_ordinaire_n_est_pas_marquee(client, espion, jeton):
    client.post("/api/contact", json={**DEMANDE, "jeton": jeton})
    demande = espion.envoyees[0]
    assert demande.soupcons == ()
    assert not demande.sujet().startswith("[")
    assert "À vérifier" not in demande.corps()


# --- le plafond du service ---------------------------------------------------


def test_le_plafond_global_tient_quand_les_adresses_changent(espion, jetons, horloge):
    """La borne par adresse n'arrête pas un réseau réparti.

    Chacun reste sous SA limite ; c'est la boîte aux lettres qui déborde. Le
    plafond global est le seul à voir ce total.
    """
    client = TestClient(construis(expediteur=espion, debit=Debit(5, 3600),
                                  global_=DebitGlobal(2, 3600), jetons=jetons))
    jeton = client.get("/api/contact/jeton").json()["jeton"]
    horloge.avance(DELAI_MINIMAL_S + 1)

    codes = [
        client.post("/api/contact", json={**DEMANDE, "jeton": jeton},
                    headers={"x-forwarded-for": f"10.0.0.{n}"}).status_code
        for n in range(3)
    ]
    assert codes == [202, 202, 429]
    assert len(espion.envoyees) == 2
