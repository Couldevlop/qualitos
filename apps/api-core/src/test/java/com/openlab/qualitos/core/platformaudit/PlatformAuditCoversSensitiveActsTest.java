package com.openlab.qualitos.core.platformaudit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Un acte déclaré mais jamais consigné.
 *
 * <p><b>La faiblesse que ce banc couvre.</b> La chaîne d'empreintes prouve
 * qu'un journal n'a pas été retouché. Elle ne prouve pas qu'il est COMPLET :
 * un acte jamais consigné ne laisse aucun vide, aucune empreinte qui jure,
 * aucun trou de séquence. La complétude ne tient qu'au branchement du journal
 * dans chaque acte sensible — une discipline de code, que rien d'automatique
 * ne vérifiait.
 *
 * <p>Le scénario concret : quelqu'un ajoute une valeur à
 * {@link PlatformAuditAction} en préparant une fonctionnalité, oublie l'appel
 * à {@code record(...)}, et la plateforme expose un pouvoir sur le dossier
 * d'un client que sa piste d'audit ignore. Tout est vert : le code compile,
 * les bancs passent, la vérification de chaîne répond « intact ». Ce banc
 * transforme cet oubli en échec de compilation.
 *
 * <p>Il lit les <b>sources</b>, sans contexte Spring : c'est ce qui le rend
 * assez rapide pour tourner à chaque construction, et assez bête pour ne pas
 * se tromper. Même parti que {@code IngressCoversApiCoreRoutesTest}, qui
 * attrape la même famille de défaut — du code complet, déployé, et muet.
 */
@DisplayName("La piste d'audit couvre les actes sensibles")
class PlatformAuditCoversSensitiveActsTest {

    private static final Path SOURCES = Path.of("src", "main", "java");
    private static final Path DOMAINE_AUDIT =
            Path.of("src", "main", "java", "com", "openlab", "qualitos", "core", "platformaudit");

    @Test
    @DisplayName("chaque acte declare est consigne par le code qui l'execute")
    void chaque_acte_est_branche() throws IOException {
        String codeMetier = sourcesHorsPisteDAudit();

        // Le banc ne vaut que s'il voit quelque chose : une lecture devenue
        // fausse rendrait sinon un texte vide et un test vert.
        assertThat(codeMetier).as("aucune source metier lue — l'extraction est cassee").isNotEmpty();

        List<String> orphelins = new ArrayList<>();
        for (PlatformAuditAction acte : PlatformAuditAction.values()) {
            if (acte == PlatformAuditAction.AUDIT_EXPORTED) {
                // Le seul acte émis par la piste elle-même : l'export se
                // consigne dans PlatformAuditService, donc hors du code métier
                // que ce banc parcourt. Il est couvert par
                // PlatformAuditServiceTest.Export#export_consigne.
                continue;
            }
            if (!codeMetier.contains("PlatformAuditAction." + acte.name())) {
                orphelins.add(acte.name());
            }
        }

        assertThat(orphelins)
                .as("Ces actes sont declares dans PlatformAuditAction mais aucun code ne les "
                        + "consigne : la plateforme exposerait un pouvoir que sa piste d'audit "
                        + "ignore, sans qu'aucune verification de chaine ne s'en apercoive. "
                        + "Brancher l'appel a PlatformAuditService.record(...) dans le service "
                        + "qui execute l'acte, ou retirer la valeur de l'enumeration.")
                .isEmpty();
    }

    @Test
    @DisplayName("les actes les plus sensibles sont nommement couverts")
    void les_actes_critiques() throws IOException {
        // Liste écrite à la main, et c'est voulu : le banc ci-dessus vérifie
        // que TOUT acte déclaré est branché, mais il ne dirait rien si
        // quelqu'un retirait la valeur de l'énumération EN MÊME TEMPS que
        // l'appel. Ces quatre-là ne doivent pas pouvoir disparaître en silence
        // — un tiers qui obtient accès aux données d'un client, un client
        // désactivé, un module ouvert, un tarif changé.
        String codeMetier = sourcesHorsPisteDAudit();

        assertThat(codeMetier)
                .contains("PlatformAuditAction.PARTNER_ENGAGEMENT_GRANTED")
                .contains("PlatformAuditAction.PARTNER_ENGAGEMENT_REVOKED")
                .contains("PlatformAuditAction.TENANT_DEACTIVATED")
                .contains("PlatformAuditAction.MODULE_SUBSCRIBED");
    }

    /** Tout le code d'api-core, sauf le domaine de la piste d'audit lui-même. */
    private static String sourcesHorsPisteDAudit() throws IOException {
        StringBuilder tout = new StringBuilder();
        try (Stream<Path> fichiers = Files.walk(SOURCES)) {
            for (Path fichier : fichiers.filter(f -> f.toString().endsWith(".java")).toList()) {
                if (fichier.startsWith(DOMAINE_AUDIT)) {
                    continue;
                }
                tout.append(Files.readString(fichier, StandardCharsets.UTF_8)).append('\n');
            }
        }
        return tout.toString();
    }

    @Test
    @DisplayName("l'enumeration ne porte pas de valeur en double")
    void pas_de_doublon() {
        // Deux valeurs pour le même acte rendraient la lecture du journal
        // ambigue : « TENANT_DEACTIVATED » et « TENANT_DISABLED » obligeraient
        // l'auditeur a deviner s'il s'agit du meme geste.
        List<String> noms = Arrays.stream(PlatformAuditAction.values())
                .map(PlatformAuditAction::name)
                .toList();

        assertThat(noms).doesNotHaveDuplicates();
    }
}
