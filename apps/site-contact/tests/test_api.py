"""La route publique, de bout en bout, sans relais SMTP."""

import pytest
from fastapi.testclient import TestClient

from app.api import construis
from app.courriel import EnvoiImpossible
from app.debit import Debit


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


@pytest.fixture
def client(espion):
    return TestClient(construis(expediteur=espion, debit=Debit(100, 3600)))


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


def test_une_demande_valide_est_transmise(client, espion):
    reponse = client.post("/api/contact", json=DEMANDE)
    assert reponse.status_code == 202
    assert len(espion.envoyees) == 1
    assert espion.envoyees[0].organisation == "Fonderie du Nord"


def test_un_champ_invalide_est_refuse_en_disant_lequel(client, espion):
    reponse = client.post("/api/contact", json={**DEMANDE, "courriel": "pas-une-adresse"})
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


def test_le_piege_a_robots_fait_semblant_d_accepter(client, espion):
    """Rempli, c'est un automate. On répond 202 sans rien envoyer."""
    reponse = client.post("/api/contact", json={**DEMANDE, "piege": "http://spam"})
    assert reponse.status_code == 202
    assert espion.envoyees == []


def test_un_piege_vide_ne_gene_pas(client, espion):
    assert client.post("/api/contact", json={**DEMANDE, "piege": "  "}).status_code == 202
    assert len(espion.envoyees) == 1


def test_un_relais_en_panne_donne_une_issue_au_visiteur():
    espion = ExpediteurEspion(echoue=True)
    client = TestClient(construis(expediteur=espion, debit=Debit(100, 3600)))
    reponse = client.post("/api/contact", json=DEMANDE)
    assert reponse.status_code == 502
    assert "contact@qualitos.fr" in reponse.json()["message"]


def test_la_limite_de_debit_s_applique(espion):
    client = TestClient(construis(expediteur=espion, debit=Debit(2, 3600)))
    codes = [client.post("/api/contact", json=DEMANDE).status_code for _ in range(3)]
    assert codes == [202, 202, 429]
    assert len(espion.envoyees) == 2


def test_la_limite_distingue_les_adresses_constatees_par_le_proxy(espion):
    """Sans lire l'entête, tout le monde partagerait le quota du proxy."""
    client = TestClient(construis(expediteur=espion, debit=Debit(1, 3600)))
    premier = client.post("/api/contact", json=DEMANDE, headers={"x-forwarded-for": "1.2.3.4"})
    autre = client.post("/api/contact", json=DEMANDE, headers={"x-forwarded-for": "5.6.7.8"})
    rejoue = client.post("/api/contact", json=DEMANDE, headers={"x-forwarded-for": "1.2.3.4"})
    assert [premier.status_code, autre.status_code, rejoue.status_code] == [202, 202, 429]


def test_un_appelant_ne_peut_pas_se_renommer_en_prefixant_l_entete(espion):
    """La liste s'allonge PAR LA DROITE : la gauche est ce que l'appelant écrit.

    Lire la gauche laisserait un robot changer d'identité à chaque requête, et
    la limite de débit ne limiterait plus personne. C'est la dernière valeur,
    celle que l'ingress ajoute d'après la connexion reçue, qui fait foi.
    """
    client = TestClient(construis(expediteur=espion, debit=Debit(1, 3600)))
    vrai = client.post("/api/contact", json=DEMANDE, headers={"x-forwarded-for": "9.9.9.9"})
    # Même appelant (9.9.9.9 reste à droite), mais il se prétend quelqu'un d'autre.
    deguise = client.post(
        "/api/contact", json=DEMANDE, headers={"x-forwarded-for": "1.1.1.1, 9.9.9.9"}
    )
    assert vrai.status_code == 202
    assert deguise.status_code == 429


def test_une_entete_vide_retombe_sur_l_adresse_de_connexion(espion):
    client = TestClient(construis(expediteur=espion, debit=Debit(1, 3600)))
    assert client.post("/api/contact", json=DEMANDE, headers={"x-forwarded-for": " , "}).status_code == 202
    assert client.post("/api/contact", json=DEMANDE, headers={"x-forwarded-for": " , "}).status_code == 429
