package com.openlab.qualitos.core.edition;

import com.openlab.qualitos.core.tenant.Tenant;
import com.openlab.qualitos.core.tenant.TenantRepository;
import com.openlab.qualitos.licensing.application.Licensing;
import com.openlab.qualitos.licensing.domain.License;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.Clock;
import java.util.Locale;
import java.util.Optional;

/**
 * Le client unique d'une installation on-premise naît au démarrage (ADR 0082).
 *
 * <p>Il prend l'identifiant que fixe la licence : c'est celui que portent les
 * comptes dans leur jeton ({@code tenant_id}), et celui qui rattache les données
 * d'une licence à la suivante. Si la licence change de raison sociale, le nom
 * suit. Sans licence lisible, rien n'est créé : l'installation reste en lecture
 * seule et l'écran dit pourquoi.
 */
@Component
public class OnPremTenantBootstrap {

    private static final Logger log = LoggerFactory.getLogger(OnPremTenantBootstrap.class);

    private final Licensing licensing;
    private final TenantRepository tenants;
    private final Clock clock;

    public OnPremTenantBootstrap(Licensing licensing, TenantRepository tenants, Clock clock) {
        this.licensing = licensing;
        this.tenants = tenants;
        this.clock = clock;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void onReady() {
        ensureTenant();
    }

    /** @return le client de la licence, créé ou mis à jour ; vide en SaaS ou sans licence. */
    @Transactional
    public Optional<Tenant> ensureTenant() {
        if (!licensing.isOnPrem()) {
            return Optional.empty();
        }
        Optional<License> licence = licensing.state().licenseOpt();
        if (licence.isEmpty()) {
            log.warn("edition.onprem.no_license status={} reason=\"{}\"", licensing.state().status(),
                    licensing.state().reason());
            return Optional.empty();
        }
        License l = licence.get();
        Optional<Tenant> existant = tenants.findById(l.tenantId());
        if (existant.isPresent()) {
            Tenant t = existant.get();
            if (!l.customer().equals(t.getName()) || !planOf(l).equals(t.getPlan())) {
                t.setName(l.customer());
                t.setPlan(planOf(l));
                tenants.save(t);
            }
            return Optional.of(t);
        }
        String slug = slug(l.customer());
        if (tenants.existsBySlug(slug)) {
            slug = (slug.length() > 54 ? slug.substring(0, 54) : slug) + "-" + l.tenantId().toString().substring(0, 8);
        }
        tenants.insertWithId(l.tenantId(), slug, l.customer(), planOf(l).name(), clock.instant());
        log.info("edition.onprem.tenant_created tenant_id={} license_id={}", l.tenantId(), l.licenseId());
        return tenants.findById(l.tenantId());
    }

    static Tenant.Plan planOf(License l) {
        return switch (l.tier()) {
            case "PRO" -> Tenant.Plan.PRO;
            case "ENTERPRISE" -> Tenant.Plan.ENTERPRISE;
            default -> Tenant.Plan.STARTER;
        };
    }

    /** « Hôpital Saint-Louis » → « hopital-saint-louis » : un slug valable, 3 à 63 caractères. */
    static String slug(String customer) {
        String s = Normalizer.normalize(customer, Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-+|-+$)", "");
        if (s.length() > 63) {
            s = s.substring(0, 63).replaceAll("-+$", "");
        }
        return s.length() < 3 ? "client-" + (s.isEmpty() ? "local" : s) : s;
    }
}
