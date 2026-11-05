package com.openlab.qualitos.core.routing;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Toute surface d'api-core doit être nommée dans l'ingress.
 *
 * <p><b>Pourquoi ce banc existe.</b> L'ingress route par préfixe : quelques
 * chemins vont à api-core, et une règle fourre-tout {@code /api} envoie tout le
 * reste au moteur qualité. Un contrôleur ajouté dans api-core sans sa règle
 * tombe donc dans ce fourre-tout et le moteur répond <b>404</b> — pas 500, pas
 * de trace : l'écran affiche une erreur générique alors que le serveur appelé
 * n'a jamais eu le chemin. Le code est complet, testé, déployé, et muet.
 *
 * <p>Le défaut s'est produit trois fois : l'annuaire (ADR 0091), la surface
 * d'administration de la facturation, puis la marque du client et les cabinets
 * partenaires (ADR 0106). Chaque fois découvert à la main en préproduction.
 * Ce banc le fait découvrir à la compilation.
 *
 * <p>Il lit les <b>sources</b> et le <b>gabarit Helm</b>, sans contexte Spring
 * ni cluster : c'est ce qui le rend assez rapide pour tourner à chaque build, et
 * assez bête pour ne pas se tromper.
 */
class IngressCoversApiCoreRoutesTest {

    /** Le gabarit est hors du module : le banc tourne depuis `apps/api-core`. */
    private static final Path INGRESS =
            Path.of("..", "..", "infra", "k8s", "qualitos", "templates", "ingress.yaml");
    private static final Path SOURCES = Path.of("src", "main", "java");

    private static final String MARQUE = "@RequestMapping(" + '"';

    @Test
    @DisplayName("chaque @RequestMapping d'api-core est couvert par une règle d'ingress vers api-core")
    void toute_surface_est_routee() throws IOException {
        List<String> chemins = cheminsDeControleurs();
        List<String> regles = reglesVersApiCore();

        // Le banc ne vaut que s'il voit quelque chose : une expression régulière
        // devenue fausse rendrait sinon une liste vide et un test vert.
        assertThat(chemins).as("aucun @RequestMapping lu — l'extraction est cassée").isNotEmpty();
        assertThat(regles).as("aucune règle api-core lue — l'extraction est cassée").isNotEmpty();

        List<String> orphelins = chemins.stream().filter(chemin -> !estCouvert(chemin, regles)).toList();

        assertThat(orphelins)
                .as("Ces chemins d'api-core n'ont pas de règle d'ingress : ils partiraient "
                        + "au moteur qualité par la règle fourre-tout /api, qui répondrait 404. "
                        + "Ajouter une règle dans infra/k8s/qualitos/templates/ingress.yaml. "
                        + "Règles api-core actuelles : " + regles)
                .isEmpty();
    }

    /**
     * Un chemin est couvert si une règle lui est égale, ou en est un préfixe
     * <b>de segments</b> : {@code /api/v1/partner} couvre
     * {@code /api/v1/partner/portfolio} mais pas {@code /api/v1/partner-firms} —
     * c'est exactement la façon dont Kubernetes compare un {@code pathType:
     * Prefix}, et s'en écarter ici rendrait le banc rassurant et faux.
     */
    private static boolean estCouvert(String chemin, List<String> regles) {
        return regles.stream()
                .anyMatch(regle -> chemin.equals(regle) || chemin.startsWith(regle + "/"));
    }

    /**
     * Les chemins déclarés par les contrôleurs, lus dans les sources.
     *
     * <p>Lecture par découpage de chaîne et non par expression régulière : la
     * forme cherchée est littérale, et un motif échappé serait ici plus facile à
     * casser qu'à lire.
     */
    private static List<String> cheminsDeControleurs() throws IOException {
        List<String> chemins = new ArrayList<>();
        try (Stream<Path> fichiers = Files.walk(SOURCES)) {
            for (Path fichier : fichiers.filter(f -> f.toString().endsWith(".java")).toList()) {
                String source = Files.readString(fichier, StandardCharsets.UTF_8);
                int depuis = source.indexOf(MARQUE);
                while (depuis >= 0) {
                    int ouvre = depuis + MARQUE.length();
                    int ferme = source.indexOf('"', ouvre);
                    String chemin = source.substring(ouvre, ferme);
                    if (chemin.startsWith("/api/")) {
                        chemins.add(chemin);
                    }
                    depuis = source.indexOf(MARQUE, ferme);
                }
            }
        }
        return new ArrayList<>(new TreeSet<>(chemins));
    }

    /**
     * Les chemins que l'ingress envoie à api-core.
     *
     * <p>On retient un chemin quand le {@code name: api-core} de son bloc
     * {@code backend} arrive — donc le service réellement visé, et non le
     * voisinage dans le fichier.
     */
    private static List<String> reglesVersApiCore() throws IOException {
        List<String> regles = new ArrayList<>();
        String enCours = null;
        for (String ligne : Files.readAllLines(INGRESS, StandardCharsets.UTF_8)) {
            String nette = ligne.trim();
            if (nette.startsWith("- path: ")) {
                enCours = nette.substring("- path: ".length()).trim();
            } else if (nette.equals("name: api-core") && enCours != null) {
                regles.add(enCours);
                enCours = null;
            }
        }
        return regles;
    }
}
