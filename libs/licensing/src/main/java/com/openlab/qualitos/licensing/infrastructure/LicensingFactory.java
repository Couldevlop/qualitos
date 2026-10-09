package com.openlab.qualitos.licensing.infrastructure;

import com.openlab.qualitos.licensing.application.LicenseFile;
import com.openlab.qualitos.licensing.application.LicenseVerifier;
import com.openlab.qualitos.licensing.application.Licensing;
import com.openlab.qualitos.licensing.domain.Edition;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.util.Optional;

/**
 * Construit {@link Licensing} depuis la configuration d'un service :
 * {@code QUALITOS_EDITION} et {@code QUALITOS_LICENSE_FILE} (un secret monté).
 * Les clés publiques de l'éditeur sont celles épinglées dans l'application.
 */
public final class LicensingFactory {

    private LicensingFactory() {}

    public static Licensing create(String edition, String licenseFile, Clock clock) {
        if (Edition.parse(edition) == Edition.SAAS) {
            return Licensing.saas(clock);
        }
        return Licensing.onPrem(fileSource(licenseFile), new LicenseVerifier(EditorKeys.pinned()), clock);
    }

    /** Le contenu du fichier, vide s'il n'existe pas ; une lecture qui échoue remonte. */
    static java.util.function.Supplier<Optional<String>> fileSource(String licenseFile) {
        return () -> {
            if (licenseFile == null || licenseFile.isBlank()) {
                return Optional.empty();
            }
            Path p = Path.of(licenseFile.strip());
            if (!Files.isRegularFile(p)) {
                return Optional.empty();
            }
            try {
                if (Files.size(p) > LicenseFile.MAX_BYTES) {
                    throw new IllegalStateException("Fichier de licence trop volumineux.");
                }
                return Optional.of(Files.readString(p, StandardCharsets.UTF_8));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        };
    }
}
