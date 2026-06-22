package com.openlab.qualitos.quality.academy.domain;

import java.util.List;

/**
 * Construit l'{@code imsmanifest.xml} d'un package SCORM 2004 4th Edition (§19.3,
 * « intégration SCORM 2004 / xAPI vers LMS externe »).
 *
 * <p>Fonction pure : à partir d'un identifiant de cours, d'un titre et de la
 * liste ordonnée des ressources (une par leçon), produit un manifeste conforme
 * (organisation + items + resources). Aucun effet de bord, testable
 * unitairement. Tout texte injecté est échappé XML.</p>
 */
public final class ScormManifestBuilder {

    private ScormManifestBuilder() {}

    /** Une ressource SCO/asset : identifiant, titre, fichier HTML href. */
    public record Item(String identifier, String title, String href) {}

    public static String build(String courseIdentifier, String courseTitle, List<Item> items) {
        StringBuilder b = new StringBuilder(2048);
        b.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        b.append("<manifest identifier=\"").append(xml(courseIdentifier)).append("\" version=\"1.0\"\n");
        b.append("  xmlns=\"http://www.imsglobal.org/xsd/imscp_v1p1\"\n");
        b.append("  xmlns:adlcp=\"http://www.adlnet.org/xsd/adlcp_v1p3\"\n");
        b.append("  xmlns:imsss=\"http://www.imsglobal.org/xsd/imsss\"\n");
        b.append("  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n");
        b.append("  xsi:schemaLocation=\"http://www.imsglobal.org/xsd/imscp_v1p1 imscp_v1p1.xsd\">\n");
        b.append("  <metadata>\n");
        b.append("    <schema>ADL SCORM</schema>\n");
        b.append("    <schemaversion>2004 4th Edition</schemaversion>\n");
        b.append("  </metadata>\n");
        b.append("  <organizations default=\"ORG-").append(xml(courseIdentifier)).append("\">\n");
        b.append("    <organization identifier=\"ORG-").append(xml(courseIdentifier)).append("\">\n");
        b.append("      <title>").append(xml(courseTitle)).append("</title>\n");
        for (Item it : items) {
            b.append("      <item identifier=\"ITEM-").append(xml(it.identifier()))
             .append("\" identifierref=\"RES-").append(xml(it.identifier())).append("\">\n");
            b.append("        <title>").append(xml(it.title())).append("</title>\n");
            b.append("      </item>\n");
        }
        b.append("    </organization>\n");
        b.append("  </organizations>\n");
        b.append("  <resources>\n");
        for (Item it : items) {
            b.append("    <resource identifier=\"RES-").append(xml(it.identifier()))
             .append("\" type=\"webcontent\" adlcp:scormType=\"sco\" href=\"")
             .append(xml(it.href())).append("\">\n");
            b.append("      <file href=\"").append(xml(it.href())).append("\"/>\n");
            b.append("    </resource>\n");
        }
        b.append("  </resources>\n");
        b.append("</manifest>\n");
        return b.toString();
    }

    static String xml(String s) {
        if (s == null) return "";
        StringBuilder out = new StringBuilder(s.length() + 16);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '&' -> out.append("&amp;");
                case '<' -> out.append("&lt;");
                case '>' -> out.append("&gt;");
                case '"' -> out.append("&quot;");
                case '\'' -> out.append("&apos;");
                default -> out.append(c);
            }
        }
        return out.toString();
    }
}
