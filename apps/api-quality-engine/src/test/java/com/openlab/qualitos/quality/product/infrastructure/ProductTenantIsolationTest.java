package com.openlab.qualitos.quality.product.infrastructure;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Le dépôt JPA ne doit exposer AUCUNE lecture sans tenant. On interdit la forme du
 * bug plutôt que d'en attendre l'occurrence : une méthode oubliée ne se signale pas
 * au moment où elle fuit, elle se signale le jour où un client s'en aperçoit.
 */
class ProductTenantIsolationTest {

    private static final List<String> FORBIDDEN =
            List.of("findAll", "findByCode", "findByStatus", "getOne", "getById");

    @Test
    void noJpaReadMethodCanForgetTheTenant() {
        List<String> declared = Arrays.stream(ProductJpaRepository.class.getDeclaredMethods())
                .map(Method::getName)
                .toList();

        assertThat(declared).doesNotContainAnyElementsOf(FORBIDDEN);
        assertThat(declared)
                .as("toute lecture déclarée porte le tenant dans son nom")
                .allMatch(name -> name.contains("TenantId") || name.equals("save")
                        || name.equals("saveAll") || name.equals("delete")
                        || name.equals("deleteById") || name.equals("flush"));
    }
}
