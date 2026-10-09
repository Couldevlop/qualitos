package com.openlab.qualitos.quality.authz.infrastructure;

import com.openlab.qualitos.quality.authz.application.AuthorizationService;
import com.openlab.qualitos.quality.authz.application.RecordScope;
import com.openlab.qualitos.quality.authz.domain.Permission;
import com.openlab.qualitos.quality.riskregister.infrastructure.JwtRegisterContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** La portée des registres : « voir tout », sinon l'utilisateur du jeton, sinon personne (ADR 0081). */
class RecordScopeBeanTest {

    final AuthorizationService authorization = mock(AuthorizationService.class);
    final RecordScope scope = new AuthzBeanConfiguration().recordScope(authorization);

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    static void connecte(String name) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(name, "n/a", List.of()));
    }

    @Test
    void voirToutNeRestreintRien() {
        when(authorization.has(Permission.NC_VIEW_ALL)).thenReturn(true);
        connecte(UUID.randomUUID().toString());
        assertThat(scope.restrictTo(Permission.NC_VIEW_ALL)).isEmpty();
    }

    @Test
    void sinonOnSeLimiteALUtilisateurDuJeton() {
        UUID moi = UUID.randomUUID();
        connecte(moi.toString());
        assertThat(scope.restrictTo(Permission.CAPA_VIEW_ALL)).contains(moi);
    }

    @Test
    void sansUtilisateurIdentifieLaPorteeNeDesignePersonne() {
        connecte("compte-de-service");
        assertThat(scope.restrictTo(Permission.RISK_VIEW_ALL)).contains(AuthzBeanConfiguration.NOBODY);
    }

    @Test
    void leRegistreDemandeVoirToutLeRegistre() {
        UUID moi = UUID.randomUUID();
        connecte(moi.toString());
        when(authorization.has(Permission.RISK_VIEW_ALL)).thenReturn(false);
        assertThat(new JwtRegisterContext(scope).visibleOnlyTo()).isEqualTo(Optional.of(moi));
        when(authorization.has(Permission.RISK_VIEW_ALL)).thenReturn(true);
        assertThat(new JwtRegisterContext(scope).visibleOnlyTo()).isEmpty();
    }
}
