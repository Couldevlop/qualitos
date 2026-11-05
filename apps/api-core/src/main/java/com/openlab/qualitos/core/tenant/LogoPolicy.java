package com.openlab.qualitos.core.tenant;

import java.util.Locale;
import java.util.Map;

/**
 * Ce qu'un client peut poser comme logo.
 *
 * <p>Le logo est le seul fichier téléversé par un client que la plateforme sert
 * ensuite depuis SON domaine, à tous les membres de ce client, sur chaque écran.
 * Un fichier accepté ici s'affiche donc dans le contexte de l'application.
 *
 * <p>Trois contrôles, dans cet ordre, et le dernier est celui qu'on oublie :
 *
 * <ol>
 *   <li><b>le type est dans la liste</b> — allow-list : un format inconnu est
 *       refusé par défaut. Le SVG en est exclu non par méfiance de principe mais
 *       parce qu'il porte {@code <script>} et s'exécuterait avec la session du
 *       membre qui l'affiche ;</li>
 *   <li><b>le poids tient</b> — 512 Kio. Un logo est une marque, pas une
 *       photographie ; au-delà, c'est un fichier qui s'est trompé de route ;</li>
 *   <li><b>le contenu confirme le type</b> — le type annoncé par le client est
 *       une déclaration, les premiers octets sont une preuve. Sans ce
 *       troisième contrôle, renommer un SVG en PNG suffirait.</li>
 * </ol>
 *
 * <p>Classe sans état : la politique est la même pour tous les clients, c'est
 * une règle de plateforme et non un paramètre de client.
 */
public final class LogoPolicy {

    /** 512 Kio. Les octets sont gardés en base : la borne protège aussi la ligne. */
    public static final int MAX_BYTES = 512 * 1024;

    /** Signature attendue par type accepté, sous forme (décalage, octets). */
    private static final Map<String, Signature[]> SIGNATURES = Map.of(
            "image/png", new Signature[]{
                    new Signature(0, new int[]{0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A})},
            // Toute variante JFIF/Exif commence par FF D8 FF ; le quatrième
            // octet distingue les variantes et ne nous regarde pas.
            "image/jpeg", new Signature[]{new Signature(0, new int[]{0xFF, 0xD8, 0xFF})},
            // « RIFF » désigne aussi du WAV et de l'AVI : c'est le marqueur de
            // forme, en position 8, qui fait un WebP.
            "image/webp", new Signature[]{
                    new Signature(0, new int[]{0x52, 0x49, 0x46, 0x46}),
                    new Signature(8, new int[]{0x57, 0x45, 0x42, 0x50})});

    private LogoPolicy() {
    }

    /**
     * Vérifie le logo proposé et rend le type normalisé à enregistrer.
     *
     * @param declaredContentType type annoncé par le client, paramètres et casse
     *                            compris ({@code IMAGE/PNG; charset=binary})
     * @param content             octets du fichier
     * @return le type normalisé ({@code image/png}, {@code image/jpeg},
     *         {@code image/webp})
     * @throws LogoRejectedException si le type, le poids ou le contenu ne passent pas
     */
    public static String check(String declaredContentType, byte[] content) {
        String type = normalize(declaredContentType);
        Signature[] expected = SIGNATURES.get(type);
        if (expected == null) {
            throw new LogoRejectedException(LogoRejectedException.Reason.TYPE,
                    "type de fichier refusé : " + type + " — attendus image/png, image/jpeg, image/webp");
        }
        if (content == null || content.length == 0) {
            throw new LogoRejectedException(LogoRejectedException.Reason.SIZE, "fichier vide");
        }
        if (content.length > MAX_BYTES) {
            throw new LogoRejectedException(LogoRejectedException.Reason.SIZE,
                    "fichier trop volumineux : " + content.length + " octets, limite 512 Kio");
        }
        for (Signature signature : expected) {
            if (!signature.matches(content)) {
                throw new LogoRejectedException(LogoRejectedException.Reason.CONTENT,
                        "le contenu du fichier ne correspond pas au type annoncé (" + type + ")");
            }
        }
        return type;
    }

    private static String normalize(String declaredContentType) {
        if (declaredContentType == null || declaredContentType.isBlank()) {
            throw new LogoRejectedException(LogoRejectedException.Reason.TYPE, "type de fichier absent");
        }
        int parametre = declaredContentType.indexOf(';');
        String brut = parametre < 0 ? declaredContentType : declaredContentType.substring(0, parametre);
        return brut.trim().toLowerCase(Locale.ROOT);
    }

    /** Une suite d'octets attendue à un décalage donné. */
    private record Signature(int offset, int[] bytes) {

        boolean matches(byte[] content) {
            if (content.length < offset + bytes.length) {
                return false;
            }
            for (int i = 0; i < bytes.length; i++) {
                if ((content[offset + i] & 0xFF) != bytes[i]) {
                    return false;
                }
            }
            return true;
        }
    }
}
