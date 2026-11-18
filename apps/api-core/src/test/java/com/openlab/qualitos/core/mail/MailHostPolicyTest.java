package com.openlab.qualitos.core.mail;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Quels serveurs sortants une organisation peut designer")
class MailHostPolicyTest {

    private static MailHostPolicy saas() {
        return new MailHostPolicy("saas", null);
    }

    @Test
    @DisplayName("Le bouclage est refuse : sinon la plateforme se scanne elle-meme")
    void bouclageRefuse() {
        // Sans cette regle, la fonctionnalite serait un scanner de reseau
        // interne offert a tout administrateur de client : il suffirait de
        // pointer la messagerie sur un port et de lire le motif d'echec.
        assertThat(saas().refus("127.0.0.1")).contains("adresses publiques");
        assertThat(saas().refus("localhost")).isNotNull();
        assertThat(saas().refus("[::1]")).isNotNull();
    }

    @Test
    @DisplayName("Les plages privees sont refusees")
    void plagesPriveesRefusees() {
        assertThat(saas().refus("10.0.0.7")).isNotNull();
        assertThat(saas().refus("192.168.1.10")).isNotNull();
        assertThat(saas().refus("172.16.5.5")).isNotNull();
    }

    @Test
    @DisplayName("L'adresse de metadonnees du nuage est refusee, et c'est le point")
    void metadonneesRefusees() {
        // 169.254.169.254 sert les identifiants d'instance chez la plupart des
        // fournisseurs. C'est la cible classique d'une SSRF, et elle tombe ici
        // sous la regle du lien-local.
        assertThat(saas().refus("169.254.169.254")).isNotNull();
    }

    @Test
    @DisplayName("Un hote public est accepte")
    void hotePublicAccepte() {
        // Une adresse publique litterale : on ne depend pas d'une resolution
        // DNS, qui ferait du banc un test du reseau de la machine.
        assertThat(saas().refus("8.8.8.8")).isNull();
    }

    @Test
    @DisplayName("Un nom introuvable est dit tel quel : c'est une faute de frappe, pas un secret")
    void nomIntrouvable() {
        String motif = saas().refus("serveur-qui-n-existe-pas.invalid");

        assertThat(motif).contains("introuvable");
    }

    @Test
    @DisplayName("Un hote vide est refuse")
    void hoteVide() {
        assertThat(saas().refus(null)).isNotNull();
        assertThat(saas().refus("   ")).isNotNull();
    }

    @Test
    @DisplayName("En ON-PREMISE, le relais interne est le cas NORMAL")
    void onPremiseAutorise() {
        MailHostPolicy onprem = new MailHostPolicy("onprem", null);

        // Refuser smtp.intranet.local sur une installation qui vit entierement
        // sur le reseau de l'organisation rendrait la fonctionnalite
        // inutilisable la ou elle sert le plus -- et l'exploitant a deja acces
        // a ce reseau par construction.
        assertThat(onprem.refus("127.0.0.1")).isNull();
        assertThat(onprem.refus("10.0.0.7")).isNull();
    }

    @Test
    @DisplayName("La surcharge explicite l'emporte sur l'edition, dans les deux sens")
    void surcharge() {
        assertThat(new MailHostPolicy("saas", "true").refus("10.0.0.7")).isNull();
        assertThat(new MailHostPolicy("onprem", "false").refus("10.0.0.7")).isNotNull();
        // Une surcharge vide ne vaut pas « false » : elle n'est pas renseignee,
        // et c'est l'edition qui decide.
        assertThat(new MailHostPolicy("onprem", "  ").refus("10.0.0.7")).isNull();
    }

    @Test
    @DisplayName("La FABRIQUE refuse aussi : c'est le point par ou passe tout envoi")
    void laFabriqueRefuseAussi() {
        MailSenderFactory fabrique = new MailSenderFactory(saas());
        MailAccount interne = new MailAccount("a@b.fr", null, null, "10.0.0.7", 25,
                MailSecurity.NONE, null, null, true);

        // La garde dans la seule sonde laissait un contournement evident :
        // enregistrer un hote interne, activer la messagerie, et ne jamais
        // appuyer sur « essai » -- le premier courriel emis faisait alors la
        // connexion qu'on voulait interdire. La fabrique est le SEUL point par
        // lequel passent l'essai ET l'envoi reel.
        assertThatThrownBy(() -> fabrique.creer(interne, null))
                .isInstanceOf(MailHostRefusedException.class);
    }

    @Test
    @DisplayName("Un hote public laisse bien construire l'envoyeur")
    void hotePublicConstruit() {
        MailSenderFactory fabrique = new MailSenderFactory(saas());
        MailAccount public_ = new MailAccount("a@b.fr", null, null, "8.8.8.8", 587,
                MailSecurity.STARTTLS, "u", null, true);

        assertThat(fabrique.creer(public_, "mdp")).isNotNull();
    }

    @Test
    @DisplayName("Le motif de refus ne dit PAS vers quoi l'hote a resolu")
    void leMotifNeRenseignePas() {
        String motif = saas().refus("10.0.0.7");

        // Rendre l'adresse resolue par le message, ce serait rendre par la
        // reponse ce que le refus vient d'interdire.
        assertThat(motif).doesNotContain("10.0.0.7");
    }
}
