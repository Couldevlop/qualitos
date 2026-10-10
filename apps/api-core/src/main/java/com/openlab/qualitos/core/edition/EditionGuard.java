package com.openlab.qualitos.core.edition;

import com.openlab.qualitos.licensing.application.Licensing;
import com.openlab.qualitos.licensing.domain.LicenseState;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.lang.NonNull;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Set;

/**
 * Ce que l'édition permet, décidé avant la lecture du corps (ADR 0082).
 *
 * <ul>
 *   <li>En on-premise, la console éditeur ({@link SaasOnly}) n'existe pas : 404.</li>
 *   <li>Sans licence valable, toute écriture est refusée (403) ; les lectures et
 *       les exports passent toujours.</li>
 * </ul>
 * En SaaS, la garde ne fait rien.
 */
public class EditionGuard implements HandlerInterceptor {

    private static final Set<String> READ_ONLY = Set.of("GET", "HEAD", "OPTIONS");

    private final ObjectProvider<Licensing> licensing;

    public EditionGuard(ObjectProvider<Licensing> licensing) {
        this.licensing = licensing;
    }

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                             @NonNull Object handler) {
        Licensing l = licensing.getIfAvailable();
        if (!(handler instanceof HandlerMethod method) || l == null || !l.isOnPrem()) {
            return true;
        }
        if (AnnotatedElementUtils.hasAnnotation(method.getMethod(), SaasOnly.class)
                || AnnotatedElementUtils.hasAnnotation(method.getBeanType(), SaasOnly.class)) {
            throw new EditionExceptions.NotInThisEdition();
        }
        if (READ_ONLY.contains(request.getMethod())) {
            return true;
        }
        LicenseState state = l.state();
        if (!state.allowsWrites()) {
            throw new EditionExceptions.LicenseReadOnly(state.status(), state.reason());
        }
        return true;
    }
}
