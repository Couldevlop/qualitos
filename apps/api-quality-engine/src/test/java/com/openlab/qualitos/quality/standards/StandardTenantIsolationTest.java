package com.openlab.qualitos.quality.standards;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Le catalogue héberge désormais DEUX régimes de propriété : les normes de la
 * plateforme (owner_tenant_id NULL) et les référentiels d'un tenant. Une seule
 * méthode de lecture qui oublierait le filtre exposerait les procédures internes
 * d'une organisation à toutes les autres — et rien, dans le code appelant, ne le
 * signalerait.
 *
 * <p>Ce test interdit donc la FORME du bug plutôt que d'en attendre l'occurrence :
 * aucune méthode de lecture du dépôt ne doit exister sans paramètre de tenant.
 */
class StandardTenantIsolationTest {

    /** Méthodes héritées de JpaRepository qu'on n'utilise pas et qui ne filtrent rien. */
    private static final List<String> FORBIDDEN = List.of(
            "findAll", "findById", "findByCode", "findByStatus", "findByFamily", "getOne", "getById");

    @Test
    void noReadMethodOnTheRepositoryCanForgetTheTenant() {
        List<String> declared = Arrays.stream(StandardRepository.class.getDeclaredMethods())
                .map(Method::getName)
                .toList();

        assertThat(declared)
                .as("une lecture sans tenant rendrait les procédures d'un tenant visibles aux autres")
                .doesNotContainAnyElementsOf(FORBIDDEN);
    }

    @Test
    void everyDeclaredReadTakesATenantArgument() {
        for (Method m : StandardRepository.class.getDeclaredMethods()) {
            boolean carriesTenant = Arrays.stream(m.getParameters())
                    .anyMatch(p -> p.getName().toLowerCase().contains("tenant")
                            || p.getType().getSimpleName().equals("UUID"));
            assertThat(carriesTenant)
                    .as("%s doit porter le tenant", m.getName())
                    .isTrue();
        }
    }
}
