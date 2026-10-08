package com.openlab.qualitos.licensing.cli;

import com.openlab.qualitos.licensing.application.LicenseCodec;
import com.openlab.qualitos.licensing.application.LicenseIssuer;
import com.openlab.qualitos.licensing.application.LicenseVerifier;
import com.openlab.qualitos.licensing.domain.License;
import com.openlab.qualitos.licensing.domain.LicenseException;
import com.openlab.qualitos.licensing.infrastructure.EditorKeys;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;

/**
 * L'outil de l'éditeur pour les licences on-premise (ADR 0082).
 *
 * <pre>
 *   keygen  &lt;cles-privees.properties&gt; &lt;cles-publiques.properties&gt;
 *   issue   &lt;cles-privees.properties&gt; &lt;demande.json&gt; &lt;licence.lic&gt;
 *   inspect &lt;licence.lic&gt; [cles-publiques.properties]
 * </pre>
 *
 * <p>{@code inspect} sans fichier de clés vérifie avec les clés épinglées dans
 * l'application : c'est exactement ce que fera l'installation du client.
 */
public final class LicenseTool {

    private LicenseTool() {}

    public static void main(String[] args) {
        System.exit(run(args, System.out, System.err, Clock.systemUTC()));
    }

    static int run(String[] args, PrintStream out, PrintStream err, Clock clock) {
        if (args.length == 0) {
            return usage(err);
        }
        try {
            switch (args[0]) {
                case "keygen" -> {
                    if (args.length != 3) return usage(err);
                    Path priv = Path.of(args[1]);
                    if (Files.exists(priv)) {
                        err.println("Refusé : " + priv + " existe déjà (une clé ne s'écrase pas).");
                        return 2;
                    }
                    EditorKeys.generate(priv, Path.of(args[2]));
                    out.println("Clés générées. Gardez " + priv + " hors ligne ; épinglez " + args[2] + ".");
                    return 0;
                }
                case "issue" -> {
                    if (args.length != 4) return usage(err);
                    License demande = LicenseCodec.fromRequest(Files.readString(Path.of(args[2]),
                            StandardCharsets.UTF_8));
                    String fichier = new LicenseIssuer(EditorKeys.readPrivate(Path.of(args[1])), clock)
                            .issue(demande);
                    Files.writeString(Path.of(args[3]), fichier, StandardCharsets.UTF_8);
                    out.println("Licence " + demande.licenseId() + " émise pour " + demande.customer()
                            + " jusqu'au " + demande.expiresAt() + ".");
                    return 0;
                }
                case "inspect" -> {
                    if (args.length != 2 && args.length != 3) return usage(err);
                    LicenseVerifier v = new LicenseVerifier(args.length == 3
                            ? EditorKeys.readPublic(Path.of(args[2])) : EditorKeys.pinned());
                    License l = v.verify(Files.readString(Path.of(args[1]), StandardCharsets.UTF_8));
                    Instant now = clock.instant();
                    out.println("Licence " + l.licenseId() + " — " + l.customer());
                    out.println("  client (tenant) : " + l.tenantId());
                    out.println("  palier          : " + l.tier());
                    out.println("  modules         : " + String.join(", ", new java.util.TreeSet<>(l.modules())));
                    out.println("  utilisateurs    : " + (l.unlimitedUsers() ? "sans limite" : l.maxUsers()));
                    out.println("  validité        : " + l.notBefore() + " → " + l.expiresAt()
                            + " (+" + l.graceDays() + " j de grâce)");
                    out.println("  état            : " + l.statusAt(now));
                    return 0;
                }
                default -> {
                    return usage(err);
                }
            }
        } catch (LicenseException e) {
            err.println("Licence refusée : " + e.getMessage());
            return 1;
        } catch (IOException | RuntimeException e) {
            err.println("Échec : " + e.getMessage());
            return 1;
        }
    }

    private static int usage(PrintStream err) {
        err.println("""
                Usage :
                  keygen  <cles-privees.properties> <cles-publiques.properties>
                  issue   <cles-privees.properties> <demande.json> <licence.lic>
                  inspect <licence.lic> [cles-publiques.properties]""");
        return 64;
    }
}
