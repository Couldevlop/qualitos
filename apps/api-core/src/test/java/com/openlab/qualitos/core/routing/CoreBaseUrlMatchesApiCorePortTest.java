package com.openlab.qualitos.core.routing;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tout {@code CORE_BASE_URL} doit viser le port sur lequel api-core écoute.
 *
 * <p><b>Pourquoi ce banc existe.</b> api-core écoute sur <b>8081</b>
 * ({@code server.port} de son {@code application.yml}, et le port de son Service
 * dans le chart). Les cinq déclarations de {@code CORE_BASE_URL} disaient
 * <b>8080</b> — le port de Keycloak et du site, pas le sien.
 *
 * <p>Ce décalage ne se voit pas : un ClusterIP sur un port que le Service
 * n'expose pas ne REFUSE pas la connexion, il n'aboutit nulle part. L'appel
 * reste donc en attente jusqu'au délai de la passerelle, qui rend un <b>504</b>
 * au bout d'une minute. Constaté en préproduction le 23/11/2026 : « Validation
 * manager » sur une dérogation ne rendait jamais la main, et le dossier restait
 * en brouillon sans qu'un message dise pourquoi. Un port refusé aurait échoué
 * en une milliseconde et nommé la cause ; un port qui n'aboutit nulle part a
 * coûté une demi-journée de recette.
 *
 * <p>Le banc lit les FICHIERS, sans contexte Spring ni cluster : il est assez
 * rapide pour tourner à chaque build, et assez bête pour ne pas se tromper.
 */
class CoreBaseUrlMatchesApiCorePortTest {

    /** Le banc tourne depuis `apps/api-core` : la racine est deux crans plus haut. */
    private static final Path RACINE = Path.of("..", "..");

    /** La source de vérité : le port qu'api-core se donne. */
    private static final Path APPLICATION_YML =
            Path.of("src", "main", "resources", "application.yml");

    /**
     * Tout ce qui déclare où joindre api-core. Y ajouter un déploiement, c'est
     * l'ajouter ici : une déclaration oubliée est précisément le défaut couvert.
     */
    private static final List<Path> DECLARATIONS = List.of(
            RACINE.resolve(Path.of("infra", "k8s", "qualitos", "values.yaml")),
            RACINE.resolve(Path.of("infra", "k8s", "qualitos", "values-preprod.yaml")),
            RACINE.resolve(Path.of("infra", "k8s", "qualitos", "values-onprem.yaml")),
            RACINE.resolve(Path.of("infra", "onprem", "compose", "docker-compose.yml")),
            RACINE.resolve(Path.of("apps", "api-quality-engine", "src", "main",
                    "resources", "application.yml")));

    /** `CORE_BASE_URL: http://hote:PORT` ou `base-url: ${CORE_BASE_URL:http://hote:PORT}`. */
    private static final Pattern ADRESSE =
            Pattern.compile("CORE_BASE_URL[^\\n]*?://[^\\n:/]+:(\\d+)");

    /** `server:` puis, indenté, `port: NNNN`. */
    private static final Pattern PORT_SERVEUR =
            Pattern.compile("(?m)^server:\\s*$(?:\\n(?!\\S).*)*?\\n\\s+port:\\s*(\\d+)");

    @Test
    @DisplayName("api-core déclare bien un port d'écoute")
    void leBancSaitSurQuelPortApiCoreEcoute() throws IOException {
        assertThat(portDEcoute()).isBetween(1, 65535);
    }

    @Test
    @DisplayName("chaque CORE_BASE_URL vise le port d'écoute d'api-core")
    void aucuneDeclarationNeViseUnPortQuiNAboutitNullePart() throws IOException {
        int attendu = portDEcoute();
        List<String> fautives = new ArrayList<>();

        for (Path fichier : DECLARATIONS) {
            String contenu = lire(fichier);
            Matcher m = ADRESSE.matcher(contenu);
            boolean trouve = false;
            while (m.find()) {
                trouve = true;
                int port = Integer.parseInt(m.group(1));
                if (port != attendu) {
                    fautives.add(fichier + " vise :" + port);
                }
            }
            assertThat(trouve)
                    .as("%s ne déclare plus CORE_BASE_URL : si le déploiement a changé,"
                            + " c'est la liste du banc qu'il faut mettre à jour", fichier)
                    .isTrue();
        }

        assertThat(fautives)
                .as("api-core écoute sur :%d. Un CORE_BASE_URL qui vise un autre port"
                        + " n'aboutit nulle part et fait attendre l'appelant jusqu'au"
                        + " 504 de la passerelle.", attendu)
                .isEmpty();
    }

    /**
     * Le port du Service Helm suit celui de l'application.
     *
     * <p>Le Service peut exposer un port différent de celui du conteneur ; ici
     * il ne le fait pas, et {@code CORE_BASE_URL} vise le Service. Les trois
     * doivent donc concorder, sinon le banc précédent contrôle la mauvaise
     * référence.
     */
    @Test
    @DisplayName("le Service api-core du chart expose le port d'écoute")
    void leServiceDuChartExposeLeMemePort() throws IOException {
        int attendu = portDEcoute();
        String values = lire(RACINE.resolve(Path.of("infra", "k8s", "qualitos", "values.yaml")));

        // Ligne à ligne, et non une expression régulière qui embrasserait le
        // bloc : une clé voisine indentée pareillement la ferait déborder en
        // silence, et le banc contrôlerait alors le port d'un autre service.
        String[] lignes = values.split("\\R");
        int debut = -1;
        for (int i = 0; i < lignes.length && debut == -1; i++) {
            if (lignes[i].matches("\\s{2}api-core:\\s*")) {
                debut = i;
            }
        }
        assertThat(debut).as("le chart déclare un service api-core").isNotEqualTo(-1);

        Integer trouve = null;
        for (int i = debut + 1; i < lignes.length && trouve == null; i++) {
            // Une clé de même indentation ouvre le service suivant : on s'arrête.
            if (lignes[i].matches("\\s{2}\\S.*")) {
                break;
            }
            Matcher m = Pattern.compile("^\\s+port:\\s*(\\d+)\\s*$").matcher(lignes[i]);
            if (m.find()) {
                trouve = Integer.valueOf(m.group(1));
            }
        }
        assertThat(trouve).as("le service api-core déclare un port").isNotNull();
        assertThat(trouve.intValue())
                .as("le Service doit exposer le port sur lequel api-core écoute")
                .isEqualTo(attendu);
    }

    private static int portDEcoute() throws IOException {
        Matcher m = PORT_SERVEUR.matcher(lire(APPLICATION_YML));
        assertThat(m.find())
                .as("api-core doit déclarer server.port dans son application.yml")
                .isTrue();
        return Integer.parseInt(m.group(1));
    }

    private static String lire(Path p) throws IOException {
        assertThat(Files.exists(p)).as("fichier attendu : %s", p.toAbsolutePath()).isTrue();
        return Files.readString(p, StandardCharsets.UTF_8);
    }
}
