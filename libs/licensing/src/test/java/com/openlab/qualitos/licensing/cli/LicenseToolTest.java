package com.openlab.qualitos.licensing.cli;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

/** L'outil de l'éditeur : générer ses clés, émettre une licence, la relire comme le client. */
class LicenseToolTest {

    final ByteArrayOutputStream out = new ByteArrayOutputStream();
    final ByteArrayOutputStream err = new ByteArrayOutputStream();
    final Clock clock = Clock.fixed(Instant.parse("2026-10-08T08:00:00Z"), ZoneOffset.UTC);

    int run(String... args) {
        return LicenseTool.run(args, new PrintStream(out, true, StandardCharsets.UTF_8),
                new PrintStream(err, true, StandardCharsets.UTF_8), clock);
    }

    @Test
    void genererEmettreInspecter(@TempDir Path dir) throws Exception {
        Path priv = dir.resolve("cles-privees.properties");
        Path pub = dir.resolve("cles-publiques.properties");
        assertThat(run("keygen", priv.toString(), pub.toString())).isZero();
        // Une clé existante ne s'écrase jamais.
        assertThat(run("keygen", priv.toString(), pub.toString())).isEqualTo(2);

        Path demande = dir.resolve("demande.json");
        Files.writeString(demande, """
                {"licenseId":"LIC-2026-007","customer":"Coopérative du Sud","tenantId":"0b5c8f7e-1111-4222-8333-444455556666",
                 "tier":"ENTERPRISE","modules":["*"],"maxUsers":0,"issuedAt":"2026-10-08T00:00:00Z",
                 "notBefore":"2026-10-08T00:00:00Z","expiresAt":"2027-10-08T00:00:00Z","graceDays":30}""");
        Path lic = dir.resolve("client.lic");
        assertThat(run("issue", priv.toString(), demande.toString(), lic.toString())).isZero();

        assertThat(run("inspect", lic.toString(), pub.toString())).isZero();
        String rapport = out.toString(StandardCharsets.UTF_8);
        assertThat(rapport).contains("LIC-2026-007", "Coopérative du Sud", "sans limite", "VALID");

        // Avec les clés épinglées de l'application, une licence d'une autre clé est refusée.
        assertThat(run("inspect", lic.toString())).isEqualTo(1);
        assertThat(err.toString(StandardCharsets.UTF_8)).contains("Licence refusée");
    }

    @Test
    void uneCommandeIncompleteAfficheLUsage(@TempDir Path dir) {
        assertThat(run()).isEqualTo(64);
        assertThat(run("keygen", "x")).isEqualTo(64);
        assertThat(run("issue", "a", "b")).isEqualTo(64);
        assertThat(run("inspect")).isEqualTo(64);
        assertThat(run("signer")).isEqualTo(64);
        assertThat(run("inspect", dir.resolve("absente.lic").toString())).isEqualTo(1);
        assertThat(err.toString(StandardCharsets.UTF_8)).contains("Usage", "Échec");
    }
}
