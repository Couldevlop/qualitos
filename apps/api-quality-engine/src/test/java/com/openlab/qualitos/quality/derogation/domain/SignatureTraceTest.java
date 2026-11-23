package com.openlab.qualitos.quality.derogation.domain;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Le tracé d'une signature, et ce qu'on refuse d'écrire dans un dossier.
 *
 * <p>Ces bancs couvrent le cas qui compte vraiment : un PNG à l'<b>en-tête
 * valide mais au corps illisible</b>. Sans décodage à la signature, il passerait
 * ici et n'échouerait qu'au moment de fabriquer le PDF — c'est-à-dire pendant
 * l'envoi au client, sur un dossier déjà déclaré signé.
 */
class SignatureTraceTest {

    @Test
    void unPngValideEstAccepteEtRenduParCopie() {
        byte[] png = pngMinimal();
        SignatureTrace trace = SignatureTrace.of(png);
        assertThat(trace.bytes()).isEqualTo(png);
        // Une copie : le dossier ne se modifie pas de l'exterieur.
        byte[] sorti = trace.bytes();
        Arrays.fill(sorti, (byte) 0);
        assertThat(trace.bytes()).isEqualTo(png);
    }

    @Test
    void unTraceAbsentOuVideEstRefuse() {
        assertThatThrownBy(() -> SignatureTrace.of(null))
                .isInstanceOf(DerogationValidationException.class)
                .hasMessageContaining("vide");
        assertThatThrownBy(() -> SignatureTrace.of(new byte[0]))
                .isInstanceOf(DerogationValidationException.class);
    }

    @Test
    void ceQuiNEstPasUnPngEstRefuse() {
        assertThatThrownBy(() -> SignatureTrace.of("%PDF-1.7 une fausse signature".getBytes()))
                .isInstanceOf(DerogationValidationException.class)
                .hasMessageContaining("PNG");
        // Trop court pour porter meme un en-tete.
        assertThatThrownBy(() -> SignatureTrace.of(new byte[]{(byte) 0x89, 'P'}))
                .isInstanceOf(DerogationValidationException.class)
                .hasMessageContaining("PNG");
    }

    @Test
    void unPngTronqueApresSonEnteteEstRefuse() {
        byte[] png = pngMinimal();
        assertThatThrownBy(() -> SignatureTrace.of(Arrays.copyOf(png, 12)))
                .isInstanceOf(DerogationValidationException.class)
                .hasMessageContaining("tronquée");
    }

    @Test
    void unEnTetePngSuiviDUnCorpsIllisibleEstRefuseDesLaSignature() {
        // Le cas qui justifie le decodage : en-tete parfait, IHDR credible,
        // contenu absent. Sans ce controle, il echouerait a l'envoi.
        byte[] faux = new byte[64];
        System.arraycopy(new byte[]{(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A},
                0, faux, 0, 8);
        // IHDR annonce 10 x 10 : les cotes passent, le decodage non.
        faux[16] = 0;
        faux[17] = 0;
        faux[18] = 0;
        faux[19] = 10;
        faux[20] = 0;
        faux[21] = 0;
        faux[22] = 0;
        faux[23] = 10;
        assertThatThrownBy(() -> SignatureTrace.of(faux))
                .isInstanceOf(DerogationValidationException.class)
                .hasMessageContaining("illisible");
    }

    @Test
    void unPngSansDimensionsEstRefuse() {
        byte[] faux = new byte[64];
        System.arraycopy(new byte[]{(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A},
                0, faux, 0, 8);
        // Largeur et hauteur a zero.
        assertThatThrownBy(() -> SignatureTrace.of(faux))
                .isInstanceOf(DerogationValidationException.class)
                .hasMessageContaining("dimensions");
    }

    @Test
    void unPngQuiAnnonceDesCotesEnormesEstRefuseAvantDEtreDecode() {
        // Quelques octets peuvent annoncer 50 000 pixels de cote : les decoder
        // reserverait des gigaoctets. La borne se lit sur l'en-tete.
        byte[] faux = new byte[64];
        System.arraycopy(new byte[]{(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A},
                0, faux, 0, 8);
        faux[18] = (byte) 0xC3;
        faux[19] = (byte) 0x50;
        faux[22] = (byte) 0xC3;
        faux[23] = (byte) 0x50;
        assertThatThrownBy(() -> SignatureTrace.of(faux))
                .isInstanceOf(DerogationValidationException.class)
                .hasMessageContaining("pixels de côté");
    }

    @Test
    void unTraceTropLourdEstRefuse() {
        byte[] enorme = new byte[SignatureTrace.MAX_OCTETS + 1];
        enorme[0] = (byte) 0x89;
        assertThatThrownBy(() -> SignatureTrace.of(enorme))
                .isInstanceOf(DerogationValidationException.class)
                .hasMessageContaining("ko");
    }

    /** Un PNG réel et minuscule, celui qu'une zone de dessin produirait. */
    static byte[] pngMinimal() {
        BufferedImage image = new BufferedImage(120, 40, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setColor(Color.BLACK);
        g.drawLine(4, 30, 110, 8);
        g.dispose();
        try (ByteArrayOutputStream sortie = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", sortie);
            return sortie.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
