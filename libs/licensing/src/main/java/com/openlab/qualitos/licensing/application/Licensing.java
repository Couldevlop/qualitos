package com.openlab.qualitos.licensing.application;

import com.openlab.qualitos.licensing.domain.Edition;
import com.openlab.qualitos.licensing.domain.LicenseException;
import com.openlab.qualitos.licensing.domain.LicenseState;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * L'édition de l'installation, et ce qu'elle sait de sa licence (ADR 0082).
 *
 * <p>En SaaS, la licence ne s'applique pas. En on-premise, le fichier est relu
 * régulièrement : renouveler une licence, c'est remplacer le fichier, sans
 * redémarrer. Une licence illisible ou mal signée n'empêche pas l'application de
 * démarrer : elle la met en lecture seule et dit pourquoi — un serveur qui
 * redémarre en boucle chez un client n'aide personne à corriger.
 */
public final class Licensing {

    /** Intervalle de relecture du fichier : un renouvellement est pris en compte vite, sans relire à chaque requête. */
    public static final Duration RECHECK = Duration.ofMinutes(1);

    private final Edition edition;
    private final Supplier<Optional<String>> source;
    private final LicenseVerifier verifier;
    private final Clock clock;

    private volatile Cached cached;

    private Licensing(Edition edition, Supplier<Optional<String>> source, LicenseVerifier verifier, Clock clock) {
        this.edition = edition;
        this.source = source;
        this.verifier = verifier;
        this.clock = clock;
    }

    public static Licensing saas(Clock clock) {
        return new Licensing(Edition.SAAS, Optional::empty, null, clock);
    }

    /**
     * @param source le contenu du fichier de licence, vide s'il n'y en a pas ;
     *               relu au plus une fois par {@link #RECHECK}
     */
    public static Licensing onPrem(Supplier<Optional<String>> source, LicenseVerifier verifier, Clock clock) {
        return new Licensing(Edition.ONPREM, source, verifier, clock);
    }

    public Edition edition() {
        return edition;
    }

    public boolean isOnPrem() {
        return edition == Edition.ONPREM;
    }

    /** L'état de la licence maintenant. */
    public LicenseState state() {
        Instant now = clock.instant();
        if (edition == Edition.SAAS) {
            return LicenseState.notRequired(now);
        }
        Cached c = cached;
        if (c == null || !now.isBefore(c.readAt().plus(RECHECK))) {
            c = new Cached(read(now), now);
            cached = c;
        }
        // Le statut suit l'horloge même entre deux lectures : l'échéance tombe à l'heure.
        LicenseState lu = c.state();
        return lu.license() == null ? lu : LicenseState.of(lu.license(), now);
    }

    private LicenseState read(Instant now) {
        Optional<String> contenu;
        try {
            contenu = source.get();
        } catch (RuntimeException e) {
            return LicenseState.invalid("Fichier de licence inaccessible.", now);
        }
        if (contenu.isEmpty() || contenu.get().isBlank()) {
            return LicenseState.missing(now);
        }
        try {
            return LicenseState.of(verifier.verify(contenu.get()), now);
        } catch (LicenseException e) {
            return LicenseState.invalid(e.getMessage(), now);
        }
    }

    private record Cached(LicenseState state, Instant readAt) {}
}
