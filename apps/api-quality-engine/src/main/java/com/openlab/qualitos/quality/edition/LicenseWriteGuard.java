package com.openlab.qualitos.quality.edition;

import com.openlab.qualitos.licensing.application.Licensing;
import com.openlab.qualitos.licensing.domain.LicenseState;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.lang.NonNull;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Set;

/**
 * Lecture seule quand la licence on-premise ne couvre plus l'installation (ADR 0082).
 *
 * <p>Licence absente, mal signée, pas encore en vigueur ou échue au-delà du
 * délai de grâce : toute écriture est refusée. Les lectures et les exports, eux,
 * passent toujours : les enregistrements qualité du client sont des preuves, et
 * une licence échue ne les lui confisque pas. En SaaS, la garde ne fait rien.
 *
 * <p>Comme les autres gardes, elle décide AVANT la lecture du corps.
 */
public class LicenseWriteGuard implements HandlerInterceptor {

    private static final Set<String> READ_ONLY = Set.of("GET", "HEAD", "OPTIONS");

    /** Optionnel : les tranches {@code @WebMvcTest} ne l'instancient pas. */
    private final ObjectProvider<Licensing> licensing;

    public LicenseWriteGuard(ObjectProvider<Licensing> licensing) {
        this.licensing = licensing;
    }

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                             @NonNull Object handler) {
        if (!(handler instanceof HandlerMethod) || READ_ONLY.contains(request.getMethod())) {
            return true;
        }
        Licensing l = licensing.getIfAvailable();
        if (l == null || !l.isOnPrem()) {
            return true;
        }
        LicenseState state = l.state();
        if (!state.allowsWrites()) {
            throw new LicenseReadOnlyException(state.status(), state.reason());
        }
        return true;
    }
}
