"""La composition du message, et la lecture de la configuration."""

import smtplib

import pytest

from app.courriel import EnvoiImpossible, ExpediteurSmtp, ReglagesSmtp, compose
from app.domaine import Demande

REGLAGES = ReglagesSmtp(
    hote="ssl0.ovh.net",
    port=465,
    utilisateur="contact@qualitos.fr",
    mot_de_passe="x",
    expediteur="contact@qualitos.fr",
    destinataire="contact@qualitos.fr",
)


def demande():
    return Demande.depuis({"nom": "Claire Vidal", "courriel": "claire@fonderie.fr"})


def test_l_expediteur_est_notre_adresse_et_non_celle_du_visiteur():
    """SPF et DMARC rejettent un message envoyé au nom d'un autre domaine."""
    message = compose(demande(), REGLAGES)
    assert message["From"] == "contact@qualitos.fr"
    assert "claire@fonderie.fr" not in message["From"]


def test_repondre_tombe_chez_le_visiteur():
    message = compose(demande(), REGLAGES)
    assert message["Reply-To"] == "Claire Vidal <claire@fonderie.fr>"


def test_le_corps_est_du_texte_brut():
    message = compose(demande(), REGLAGES)
    assert message.get_content_type() == "text/plain"


def test_le_sujet_est_porte():
    assert compose(demande(), REGLAGES)["Subject"].startswith("Demande de démonstration")


def test_une_configuration_incomplete_refuse_de_demarrer(monkeypatch):
    for nom in ("SMTP_HOTE", "SMTP_UTILISATEUR", "SMTP_MOT_DE_PASSE", "SMTP_EXPEDITEUR"):
        monkeypatch.delenv(nom, raising=False)
    with pytest.raises(RuntimeError) as echec:
        ReglagesSmtp.depuis_environnement()
    assert "SMTP_HOTE" in str(echec.value)


def test_une_configuration_complete_est_lue(monkeypatch):
    monkeypatch.setenv("SMTP_HOTE", "ssl0.ovh.net")
    monkeypatch.setenv("SMTP_UTILISATEUR", "contact@qualitos.fr")
    monkeypatch.setenv("SMTP_MOT_DE_PASSE", "secret")
    monkeypatch.setenv("SMTP_EXPEDITEUR", "contact@qualitos.fr")
    monkeypatch.setenv("SMTP_PORT", "587")
    monkeypatch.setenv("SMTP_TLS_IMPLICITE", "0")
    reglages = ReglagesSmtp.depuis_environnement()
    assert reglages.port == 587
    assert reglages.tls_implicite is False
    assert reglages.destinataire == "contact@qualitos.fr"


class RelaisFactice:
    dernier = None

    def __init__(self, *args, **kwargs):
        RelaisFactice.dernier = self
        self.connecte = True
        self.messages = []
        self.starttls_appele = False

    def __enter__(self):
        return self

    def __exit__(self, *exc):
        return False

    def login(self, *a):
        pass

    def starttls(self, **k):
        self.starttls_appele = True

    def send_message(self, message):
        self.messages.append(message)


def test_envoi_en_tls_implicite(monkeypatch):
    monkeypatch.setattr(smtplib, "SMTP_SSL", RelaisFactice)
    ExpediteurSmtp(REGLAGES).envoie(demande())
    assert len(RelaisFactice.dernier.messages) == 1


def test_envoi_en_starttls(monkeypatch):
    monkeypatch.setattr(smtplib, "SMTP", RelaisFactice)
    reglages = ReglagesSmtp(**{**REGLAGES.__dict__, "port": 587, "tls_implicite": False})
    ExpediteurSmtp(reglages).envoie(demande())
    assert RelaisFactice.dernier.starttls_appele is True
    assert len(RelaisFactice.dernier.messages) == 1


@pytest.mark.parametrize("panne", [smtplib.SMTPAuthenticationError(535, b"non"), OSError("injoignable")])
def test_une_panne_du_relais_devient_une_erreur_du_service(monkeypatch, panne):
    def tombe(*a, **k):
        raise panne

    monkeypatch.setattr(smtplib, "SMTP_SSL", tombe)
    with pytest.raises(EnvoiImpossible):
        ExpediteurSmtp(REGLAGES).envoie(demande())
