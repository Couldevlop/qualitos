package com.openlab.qualitos.quality.authz.web;

import com.openlab.qualitos.quality.authz.application.AuthorizationService;
import com.openlab.qualitos.quality.authz.domain.Permission;
import com.openlab.qualitos.quality.authz.domain.TenantRoles;
import com.openlab.qualitos.quality.authz.infrastructure.AuthzAdapters;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.lang.NonNull;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Refuse, avant toute lecture du corps, l'appel d'un point d'entrée dont
 * l'utilisateur n'a pas l'action ({@link RequiresPermission}).
 *
 * <p><b>Sans service de droits</b> — les tranches {@code @WebMvcTest}, qui
 * n'instancient pas les services — la décision suit les droits LIVRÉS par la
 * plateforme pour les rôles du jeton : exactement ce qu'obtient un client qui
 * n'a rien réglé. Jamais « tout est permis » : l'absence du service ne peut
 * pas ouvrir une porte que le catalogue ferme.
 *
 * <p>Sans authentification, il s'abstient : c'est à la chaîne de filtres de
 * répondre 401 (« je ne sais pas qui tu es » n'est pas « tu n'as pas le droit »).
 */
public class PermissionInterceptor implements HandlerInterceptor {

    private final ObjectProvider<AuthorizationService> service;
    private final Map<HandlerMethod, Optional<Permission>> declared = new ConcurrentHashMap<>();

    public PermissionInterceptor(ObjectProvider<AuthorizationService> service) {
        this.service = service;
    }

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                             @NonNull Object handler) {
        if (!(handler instanceof HandlerMethod method)) {
            return true;
        }
        Optional<Permission> exigee = declared.computeIfAbsent(method, PermissionInterceptor::declaredOn);
        if (exigee.isEmpty()) {
            return true;
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return true;
        }
        AuthorizationService droits = service.getIfAvailable();
        boolean accorde = droits != null
                ? droits.has(exigee.get())
                : TenantRoles.of(List.of())
                        .effective(AuthzAdapters.JwtContext.systemRoles(auth.getAuthorities()))
                        .contains(exigee.get());
        if (!accorde) {
            throw new AccessDeniedException("Access denied");
        }
        return true;
    }

    static Optional<Permission> declaredOn(HandlerMethod method) {
        RequiresPermission annotation = AnnotatedElementUtils.findMergedAnnotation(
                method.getMethod(), RequiresPermission.class);
        if (annotation == null) {
            annotation = AnnotatedElementUtils.findMergedAnnotation(method.getBeanType(), RequiresPermission.class);
        }
        return annotation == null ? Optional.empty() : Optional.of(annotation.value());
    }
}
