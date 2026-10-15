package com.openlab.qualitos.core.user;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Après l'authentification du jeton : provisionne le membre de l'utilisateur
 * (ADR 0093). Une panne de provision ne fait jamais échouer la requête — elle
 * est journalisée, et la requête suivante réessaiera.
 *
 * <p>Ni composant ni bean : instancié par {@code SecurityConfig} dans la chaîne
 * de sécurité, pour ne pas être enregistré une seconde fois comme filtre de
 * servlet. Sans {@link MemberProvisioning} dans le contexte (tranches de test
 * Web), il ne fait rien.
 */
public class MemberProvisioningFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(MemberProvisioningFilter.class);

    private final ObjectProvider<MemberProvisioning> provisioning;

    public MemberProvisioningFilter(ObjectProvider<MemberProvisioning> provisioning) {
        this.provisioning = provisioning;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws ServletException, IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthenticationToken jwt && auth.isAuthenticated()) {
            MemberProvisioning p = provisioning.getIfAvailable();
            if (p != null) {
                try {
                    p.provision(jwt.getToken());
                } catch (RuntimeException e) {
                    log.warn("directory.provisioning.failed error={}", e.getClass().getSimpleName());
                }
            }
        }
        chain.doFilter(request, response);
    }
}
