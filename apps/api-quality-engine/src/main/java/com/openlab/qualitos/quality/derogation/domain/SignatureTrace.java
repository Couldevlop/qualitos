package com.openlab.qualitos.quality.derogation.domain;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;

/**
 * Le tracé d'une signature, et ce qu'il doit être pour entrer dans le dossier.
 *
 * <p><b>Pourquoi vérifier ici et non à la frontière HTTP.</b> Ce tracé finit
 * dans un PDF qui part chez un client, à l'extérieur. Un en-tête PNG valide
 * suivi d'un corps illisible passerait la signature sans bruit et ne casserait
 * qu'au moment de fabriquer le document — c'est-à-dire pendant l'envoi, après
 * que le dossier a été déclaré signé. On décode donc l'image <b>maintenant</b> :
 * un refus au moment de signer se corrige, un refus au moment d'envoyer se
 * subit.
 *
 * <p>La règle est dans le domaine parce que c'est une règle du dossier, pas du
 * transport ; elle n'utilise que le JDK.
 */
public final class SignatureTrace {

    /** Les huit octets d'en-tête d'un PNG. */
    private static final byte[] PNG_MAGIC = {
            (byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};

    /** Un tracé de zone de dessin tient dans quelques dizaines de kilo-octets. */
    static final int MAX_OCTETS = 1_000_000;

    /**
     * La borne de dimensions, lue AVANT de décoder.
     *
     * <p>Un PNG de quelques kilo-octets peut déclarer 50 000 × 50 000 pixels :
     * le décoder réserverait des gigaoctets. La borne se vérifie donc sur
     * l'en-tête, pas sur l'image obtenue.
     */
    static final int MAX_COTE = 4000;

    private final byte[] octets;

    private SignatureTrace(byte[] octets) {
        this.octets = octets;
    }

    /**
     * Le tracé, vérifié.
     *
     * @throws DerogationValidationException si ce n'est pas un PNG lisible, ou
     *                                       s'il dépasse les bornes
     */
    public static SignatureTrace of(byte[] brut) {
        if (brut == null || brut.length == 0) {
            throw new DerogationValidationException("La signature est vide.");
        }
        if (brut.length > MAX_OCTETS) {
            throw new DerogationValidationException(
                    "La signature dépasse " + (MAX_OCTETS / 1000) + " ko.");
        }
        // On ne se fie ni au nom du fichier ni au type annonce : un client HTTP
        // dit ce qu'il veut.
        if (brut.length < PNG_MAGIC.length
                || !Arrays.equals(Arrays.copyOf(brut, PNG_MAGIC.length), PNG_MAGIC)) {
            throw new DerogationValidationException("La signature doit être une image PNG.");
        }
        verifierLesCotes(brut);
        verifierLisible(brut);
        return new SignatureTrace(brut.clone());
    }

    /**
     * Largeur et hauteur, lues dans le bloc {@code IHDR} du PNG.
     *
     * <p>Il commence au 17e octet : 8 d'en-tête, 4 de longueur de bloc, 4 de
     * type, puis deux entiers de 4 octets en gros-boutien.
     */
    private static void verifierLesCotes(byte[] png) {
        if (png.length < 24) {
            throw new DerogationValidationException("La signature est tronquée.");
        }
        long largeur = entier(png, 16);
        long hauteur = entier(png, 20);
        if (largeur <= 0 || hauteur <= 0) {
            throw new DerogationValidationException("La signature n'a pas de dimensions.");
        }
        if (largeur > MAX_COTE || hauteur > MAX_COTE) {
            throw new DerogationValidationException(
                    "La signature dépasse " + MAX_COTE + " pixels de côté.");
        }
    }

    private static long entier(byte[] o, int debut) {
        return ((long) (o[debut] & 0xFF) << 24) | ((o[debut + 1] & 0xFF) << 16)
                | ((o[debut + 2] & 0xFF) << 8) | (o[debut + 3] & 0xFF);
    }

    private static void verifierLisible(byte[] png) {
        BufferedImage image;
        try {
            image = ImageIO.read(new ByteArrayInputStream(png));
        } catch (IOException e) {
            throw new DerogationValidationException("La signature est illisible.");
        }
        if (image == null) {
            throw new DerogationValidationException("La signature est illisible.");
        }
    }

    /** Les octets du tracé. Une copie : le dossier ne se modifie pas de l'extérieur. */
    public byte[] bytes() {
        return octets.clone();
    }
}
