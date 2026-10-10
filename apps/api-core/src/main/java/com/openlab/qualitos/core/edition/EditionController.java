package com.openlab.qualitos.core.edition;

import com.openlab.qualitos.core.security.TenantContext;
import com.openlab.qualitos.core.user.UserRepository;
import com.openlab.qualitos.licensing.application.Licensing;
import com.openlab.qualitos.licensing.domain.License;
import com.openlab.qualitos.licensing.domain.LicenseState;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.TreeSet;
import java.util.UUID;

/**
 * Ce que l'écran doit savoir de l'installation (ADR 0082) : son édition, et en
 * on-premise l'état de sa licence — pour cacher la console éditeur, prévenir
 * avant l'échéance, et dire pourquoi l'installation serait en lecture seule.
 *
 * <p>Ouvert à tout utilisateur connecté : la bannière d'échéance s'adresse à
 * tous. Rien de secret : ni clé, ni signature.
 */
@RestController
@RequestMapping("/api/v1/edition")
@PreAuthorize("isAuthenticated()")
@Tag(name = "Edition", description = "Édition de l'installation et état de la licence")
public class EditionController {

    private final Licensing licensing;
    private final UserRepository users;

    public EditionController(Licensing licensing, UserRepository users) {
        this.licensing = licensing;
        this.users = users;
    }

    /**
     * @param daysLeft jours avant l'échéance (négatif une fois passée), absent en SaaS
     * @param activeUsers membres actifs du client, comparés au plafond de la licence
     */
    public record EditionView(String edition, String licenseStatus, boolean writable, String reason,
                              String customer, String licenseId, String tier, List<String> modules,
                              Integer maxUsers, Long activeUsers, Instant expiresAt, Instant graceEndsAt,
                              Long daysLeft) {}

    @GetMapping
    public EditionView edition() {
        LicenseState s = licensing.state();
        License l = s.license();
        if (l == null) {
            return new EditionView(s.edition().name(), s.status().name(), s.allowsWrites(), s.reason(),
                    null, null, null, List.of(), null, null, null, null, null);
        }
        return new EditionView(s.edition().name(), s.status().name(), s.allowsWrites(), s.reason(),
                l.customer(), l.licenseId(), l.tier(), List.copyOf(new TreeSet<>(l.modules())),
                l.maxUsers(), activeUsers(), l.expiresAt(), l.graceEndsAt(),
                Duration.between(s.evaluatedAt(), l.expiresAt()).toDays());
    }

    private Long activeUsers() {
        String tenant = TenantContext.getTenantId();
        return tenant == null ? null : users.countByTenantIdAndActiveTrue(UUID.fromString(tenant));
    }
}
