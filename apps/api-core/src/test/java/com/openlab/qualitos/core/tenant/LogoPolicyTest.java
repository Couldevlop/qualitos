package com.openlab.qualitos.core.tenant;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Ce qu'un client peut poser comme logo, et ce que la plateforme refuse.
 *
 * <p>Le logo est le seul fichier qu'un client téléverse et que la plateforme
 * sert ensuite depuis SON domaine, à tous ses membres, sur chaque écran. Un
 * fichier accepté ici est donc exécuté dans le contexte de l'application si le
 * navigateur décide qu'il est du code — c'est tout l'enjeu du refus du SVG.
 *
 * <p>Trois refus, et le troisième est celui qu'on oublie :
 *
 * <ul>
 *   <li><b>le type</b> — allow-list, jamais deny-list : un format inconnu est
 *       refusé par défaut plutôt qu'accepté faute d'être listé ;</li>
 *   <li><b>le poids</b> — un logo est une marque, pas une photographie ;</li>
 *   <li><b>la cohérence</b> — le type ANNONCÉ par le client ne prouve rien. Ce
 *       qui compte est ce que disent les premiers octets.</li>
 * </ul>
 */
class LogoPolicyTest {

    /** Signature PNG (RFC 2083 §3.1), suivie d'un remplissage quelconque. */
    private static byte[] png(int taille) {
        byte[] octets = new byte[taille];
        byte[] signature = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};
        System.arraycopy(signature, 0, octets, 0, Math.min(signature.length, taille));
        return octets;
    }

    private static byte[] jpeg() {
        byte[] octets = new byte[64];
        octets[0] = (byte) 0xFF;
        octets[1] = (byte) 0xD8;
        octets[2] = (byte) 0xFF;
        return octets;
    }

    private static byte[] webp() {
        byte[] octets = new byte[64];
        System.arraycopy("RIFF".getBytes(StandardCharsets.US_ASCII), 0, octets, 0, 4);
        System.arraycopy("WEBP".getBytes(StandardCharsets.US_ASCII), 0, octets, 8, 4);
        return octets;
    }

    @Test
    void pngAccepte() {
        assertThat(LogoPolicy.check("image/png", png(2048))).isEqualTo("image/png");
    }

    @Test
    void jpegAccepte() {
        assertThat(LogoPolicy.check("image/jpeg", jpeg())).isEqualTo("image/jpeg");
    }

    @Test
    void webpAccepte() {
        assertThat(LogoPolicy.check("image/webp", webp())).isEqualTo("image/webp");
    }

    @Test
    void leTypeEstNormaliseEtLeParametreDEncodageIgnore() {
        // Un navigateur peut envoyer « image/png; charset=binary », et la casse
        // n'est pas significative : refuser là-dessus serait un refus de forme.
        assertThat(LogoPolicy.check("IMAGE/PNG; charset=binary", png(1024))).isEqualTo("image/png");
    }

    @Test
    void svgRefuse() {
        // Un SVG est un document : il porte <script>. Servi depuis le domaine
        // applicatif, il s'exécute avec la session du membre qui l'affiche.
        byte[] contenu = "<svg xmlns=\"http://www.w3.org/2000/svg\"><script>1</script></svg>"
                .getBytes(StandardCharsets.UTF_8);

        assertThatThrownBy(() -> LogoPolicy.check("image/svg+xml", contenu))
                .isInstanceOf(LogoRejectedException.class)
                .hasMessageContaining("image/svg+xml");
    }

    @Test
    void gifRefuseFauteDEtreDansLaListe() {
        assertThatThrownBy(() -> LogoPolicy.check("image/gif", png(512)))
                .isInstanceOf(LogoRejectedException.class);
    }

    @Test
    void typeAbsentRefuse() {
        assertThatThrownBy(() -> LogoPolicy.check(null, png(512)))
                .isInstanceOf(LogoRejectedException.class);
        assertThatThrownBy(() -> LogoPolicy.check("   ", png(512)))
                .isInstanceOf(LogoRejectedException.class);
    }

    @Test
    void auDelaDeLaLimiteRefuse() {
        assertThatThrownBy(() -> LogoPolicy.check("image/png", png(LogoPolicy.MAX_BYTES + 1)))
                .isInstanceOf(LogoRejectedException.class)
                .hasMessageContaining("512");
    }

    @Test
    void exactementALaLimiteAccepte() {
        // La borne est inclusive : un fichier de 512 Kio pile n'est pas « trop
        // gros », et un refus à la borne est un refus que personne ne comprend.
        assertThat(LogoPolicy.check("image/png", png(LogoPolicy.MAX_BYTES))).isEqualTo("image/png");
    }

    @Test
    void fichierVideRefuse() {
        assertThatThrownBy(() -> LogoPolicy.check("image/png", new byte[0]))
                .isInstanceOf(LogoRejectedException.class)
                .hasMessageContaining("vide");
        assertThatThrownBy(() -> LogoPolicy.check("image/png", null))
                .isInstanceOf(LogoRejectedException.class);
    }

    @Test
    void typeAnnonceDementiParLesPremiersOctetsRefuse() {
        // Le cas qui compte : un client annonce « image/png » et envoie une
        // archive ZIP (ou un SVG renommé). Le type annoncé est une déclaration,
        // les premiers octets sont une preuve.
        byte[] zip = {'P', 'K', 0x03, 0x04, 0, 0, 0, 0, 0, 0, 0, 0};

        assertThatThrownBy(() -> LogoPolicy.check("image/png", zip))
                .isInstanceOf(LogoRejectedException.class)
                .hasMessageContaining("contenu");
    }

    @Test
    void svgRenommeEnPngRefuse() {
        byte[] svg = "<svg><script>alert(1)</script></svg>".getBytes(StandardCharsets.UTF_8);

        assertThatThrownBy(() -> LogoPolicy.check("image/png", svg))
                .isInstanceOf(LogoRejectedException.class);
    }

    @Test
    void unPngTropCourtPourPorterSaSignatureRefuse() {
        assertThatThrownBy(() -> LogoPolicy.check("image/png", new byte[]{(byte) 0x89, 'P'}))
                .isInstanceOf(LogoRejectedException.class);
    }

    @Test
    void unWebpSansLeMarqueurWebpRefuse() {
        // « RIFF » seul désigne aussi du WAV ou de l'AVI : les quatre octets de
        // forme, en position 8, sont ce qui fait un WebP.
        byte[] riffWave = new byte[64];
        System.arraycopy("RIFF".getBytes(StandardCharsets.US_ASCII), 0, riffWave, 0, 4);
        System.arraycopy("WAVE".getBytes(StandardCharsets.US_ASCII), 0, riffWave, 8, 4);

        assertThatThrownBy(() -> LogoPolicy.check("image/webp", riffWave))
                .isInstanceOf(LogoRejectedException.class);
    }
}
