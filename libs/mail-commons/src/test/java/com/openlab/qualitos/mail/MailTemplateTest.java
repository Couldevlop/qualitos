package com.openlab.qualitos.mail;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * La mise en forme des courriels sortants.
 *
 * <p>Ce qui se joue ici n'est pas l'apparence : c'est QUI signe le message, et ce
 * qu'un corps de message peut faire au balisage qui l'entoure.
 */
@DisplayName("La mise en forme des courriels")
class MailTemplateTest {

    @Nested
    @DisplayName("Qui signe le message")
    class QuiSigne {

        @Test
        @DisplayName("un courriel de la plateforme porte le logo QualitOS")
        void laPlateformePorteSonLogo() {
            String html = MailTemplate.plateforme("Bonjour,");

            assertThat(html).contains("cid:" + MailTemplate.LOGO_CID);
            assertThat(html).contains("alt=\"QualitOS\"");
        }

        @Test
        @DisplayName("un courriel d'organisation porte son nom, et PAS le logo QualitOS")
        void lOrganisationSigneDeSonNom() {
            String html = MailTemplate.organisation("Aerospatiale Normandie", "Bonjour,");

            assertThat(html).contains("Aerospatiale Normandie");
            // Le fond de l'affaire : ce courriel part vers le client d'un client,
            // sous une adresse que ce client a reglee. Un logo qui n'est pas le
            // sien tromperait le destinataire sur qui lui ecrit.
            assertThat(html).doesNotContain("cid:" + MailTemplate.LOGO_CID);
            assertThat(html).doesNotContain("alt=\"QualitOS\"");
        }

        @Test
        @DisplayName("sans nom d'organisation, l'en-tete se reduit au filet")
        void sansNomPasDeCadreVide() {
            String html = MailTemplate.organisation("   ", "Bonjour,");

            assertThat(html).doesNotContain("<div style=\"font:600 18px");
            assertThat(html).contains("Bonjour,");
        }

        @Test
        @DisplayName("un nom nul ne fait pas tomber le rendu")
        void unNomNulNeFaitPasTomberLeRendu() {
            assertThat(MailTemplate.organisation(null, "Bonjour,")).contains("Bonjour,");
        }
    }

    @Nested
    @DisplayName("Ce qui vient d'ailleurs")
    class CeQuiVientDAilleurs {

        @Test
        @DisplayName("un corps de message n'ouvre pas de balise")
        void unCorpsNOuvrePasDeBalise() {
            String html = MailTemplate.plateforme("<script>alert(1)</script>");

            assertThat(html).doesNotContain("<script>");
            assertThat(html).contains("&lt;script&gt;");
        }

        @Test
        @DisplayName("un nom d'organisation n'ouvre pas de balise non plus")
        void unNomNOuvrePasDeBalise() {
            String html = MailTemplate.organisation("<img src=x onerror=1>", "Bonjour,");

            assertThat(html).doesNotContain("<img src=x");
            assertThat(html).contains("&lt;img src=x onerror=1&gt;");
        }

        @Test
        @DisplayName("une apostrophe et un guillemet ne referment aucun attribut")
        void lesQuotesNeReferementRien() {
            String html = MailTemplate.organisation("L'Atelier \"Nord\"", "Bonjour,");

            assertThat(html).contains("L&#39;Atelier &quot;Nord&quot;");
        }

        @Test
        @DisplayName("une esperluette est echappee une seule fois")
        void uneEsperluetteEstEchappeeUneFois() {
            String html = MailTemplate.organisation("Dupont & Fils", "Bonjour,");

            assertThat(html).contains("Dupont &amp; Fils");
            assertThat(html).doesNotContain("&amp;amp;");
        }
    }

    @Nested
    @DisplayName("Le corps du message")
    class LeCorps {

        @Test
        @DisplayName("une ligne vide separe deux paragraphes")
        void uneLigneVideSepareDeuxParagraphes() {
            String html = MailTemplate.plateforme("Premier.\n\nSecond.");

            assertThat(html).contains("Premier.").contains("Second.");
            assertThat(html.split("<p style=", -1)).hasSize(3);
        }

        @Test
        @DisplayName("un simple retour reste un retour, pas une fusion de lignes")
        void unSimpleRetourResteUnRetour() {
            String html = MailTemplate.plateforme("Ligne une\nLigne deux");

            assertThat(html).contains("Ligne une<br>Ligne deux");
        }

        @Test
        @DisplayName("les fins de ligne Windows ne doublent pas les paragraphes")
        void lesFinsDeLigneWindowsSontNormalisees() {
            String html = MailTemplate.plateforme("Premier.\r\n\r\nSecond.");

            assertThat(html.split("<p style=", -1)).hasSize(3);
        }

        @Test
        @DisplayName("une adresse web devient cliquable")
        void uneAdresseWebDevientCliquable() {
            String html = MailTemplate.plateforme(
                    "Le dossier : https://qualitos.example/fr/derogations/42");

            assertThat(html).contains(
                    "<a href=\"https://qualitos.example/fr/derogations/42\"");
        }

        @Test
        @DisplayName("un lien a parametres reste entier, et son esperluette echappee")
        void unLienAParametresResteEntier() {
            String html = MailTemplate.plateforme("https://x.test/a?b=1&c=2");

            assertThat(html).contains("href=\"https://x.test/a?b=1&amp;c=2\"");
        }

        @Test
        @DisplayName("un faux lien ne referme pas l'attribut href")
        void unFauxLienNeRefermePasLAttribut() {
            String html = MailTemplate.plateforme("https://x.test/\" onmouseover=\"alert(1)");

            // Le guillemet a ete echappe AVANT la mise en lien : il ne peut plus
            // refermer href pour ouvrir un autre attribut.
            assertThat(html).doesNotContain("onmouseover=\"alert(1)\"");
            assertThat(html).contains("&quot;");
        }

        @Test
        @DisplayName("un corps vide rend un document, pas une exception")
        void unCorpsVideRendUnDocument() {
            assertThat(MailTemplate.plateforme("")).contains("<!DOCTYPE html>");
            assertThat(MailTemplate.plateforme(null)).contains("<!DOCTYPE html>");
        }
    }

    @Nested
    @DisplayName("Le document")
    class LeDocument {

        @Test
        @DisplayName("le rendu est un document HTML complet et francais")
        void leRenduEstUnDocumentComplet() {
            String html = MailTemplate.plateforme("Bonjour,");

            assertThat(html).startsWith("<!DOCTYPE html>");
            assertThat(html).contains("lang=\"fr\"").contains("charset=\"utf-8\"");
            assertThat(html).endsWith("</html>");
        }

        @Test
        @DisplayName("la mise en page tient en tableaux et styles en ligne")
        void laMiseEnPageTientEnTableaux() {
            String html = MailTemplate.plateforme("Bonjour,");

            // Les messageries ignorent les feuilles de style externes et une
            // bonne part de la mise en page moderne ; le tableau et le style en
            // ligne sont ce qui survit partout.
            assertThat(html).contains("role=\"presentation\"");
            assertThat(html).doesNotContain("<link").doesNotContain("display:flex");
        }

        @Test
        @DisplayName("la couleur d'accent de la charte est celle du produit")
        void lAccentEstCeluiDeLaCharte() {
            assertThat(MailTemplate.plateforme("Bonjour,")).contains("#2563d6");
        }

        @Test
        @DisplayName("le logo se lit, et c'est bien une image PNG")
        void leLogoSeLitEtEstUnPng() {
            byte[] octets = MailTemplate.logo();

            assertThat(octets).hasSizeGreaterThan(1024);
            // La signature PNG : un fichier renomme ou tronque arriverait en
            // cadre vide chez le destinataire, sans rien dire au journal.
            assertThat(new byte[] { octets[0], octets[1], octets[2], octets[3] })
                    .containsExactly((byte) 0x89, (byte) 'P', (byte) 'N', (byte) 'G');
        }

        @Test
        @DisplayName("la ressource du logo existe la ou le gabarit la designe")
        void laRessourceDuLogoExiste() {
            assertThat(MailTemplate.class.getResource(MailTemplate.LOGO_RESOURCE))
                    .as("le logo doit etre embarque dans le jar, sinon l'en-tete "
                            + "de la plateforme arrive vide")
                    .isNotNull();
        }
    }
}
