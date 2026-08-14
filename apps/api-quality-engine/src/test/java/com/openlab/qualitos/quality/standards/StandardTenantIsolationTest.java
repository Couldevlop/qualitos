package com.openlab.qualitos.quality.standards;

import org.junit.jupiter.api.Test;
import org.springframework.data.repository.CrudRepository;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Le catalogue héberge désormais DEUX régimes de propriété : les normes de la
 * plateforme (owner_tenant_id NULL) et les référentiels d'un tenant. Une seule
 * méthode de lecture qui oublierait le filtre exposerait les procédures internes
 * d'une organisation à toutes les autres — et rien, dans le code appelant, ne le
 * signalerait.
 *
 * <p><b>Une première version de ce test était insuffisante</b> : elle n'inspectait
 * que les méthodes DÉCLARÉES sur {@link StandardRepository} par
 * {@code getDeclaredMethods()}. Or {@code StandardRepository} étendait
 * {@code JpaRepository}, qui apporte {@code findById}/{@code existsById}/
 * {@code findAll} SANS tenant — hérités, donc absents de
 * {@code getDeclaredMethods()} et invisibles à ce test. Le bug que le test était
 * censé interdire a donc pu se glisser sans le faire échouer :
 * {@code StandardsService.adopt()} et {@code requireStandard()} appelaient bel et
 * bien ces méthodes héritées, permettant à un tenant d'adopter — puis de lire
 * clauses, preuves et score d'alignement — le référentiel d'un AUTRE tenant. Un
 * test qui n'attrape que les noms de méthodes ne protège rien tant que
 * l'interface continue d'HÉRITER des méthodes non filtrées : lister des noms
 * interdits est une liste noire, toujours contournable par héritage.
 *
 * <p>La garantie doit donc porter sur la STRUCTURE du type, pas sur ses seules
 * méthodes déclarées : {@code StandardRepository} ne doit descendre d'AUCUNE
 * interface Spring Data générique ({@code CrudRepository}, {@code JpaRepository},
 * {@code PagingAndSortingRepository}…) qui offrirait ces méthodes non filtrées.
 * Il se limite au marqueur minimal {@code org.springframework.data.repository.Repository}
 * et déclare explicitement chaque méthode dont l'application a besoin — chacune
 * porteuse d'un tenant. Le compilateur tient alors l'invariant à la place d'un
 * test qui pourrait être contourné.
 */
class StandardTenantIsolationTest {

    /** Méthodes d'écriture : elles ne "lisent" pas, donc pas de tenant à porter ici. */
    private static final Set<String> WRITE_METHODS = Set.of("save", "delete");

    @Test
    void repositoryDoesNotExtendAnyGenericCrudInterface() {
        assertThat(CrudRepository.class.isAssignableFrom(StandardRepository.class))
                .as("CrudRepository (et JpaRepository, qui en hérite) expose findById/"
                        + "existsById/findAll/deleteById SANS tenant : hérités, ils échappent à "
                        + "toute inspection des méthodes déclarées, et un seul appel distrait à "
                        + "l'un d'eux suffit à exposer le référentiel d'un tenant à un autre")
                .isFalse();

        assertThat(StandardRepository.class.getInterfaces())
                .as("StandardRepository doit étendre le marqueur minimal Repository<Standard, UUID>, "
                        + "rien de plus riche")
                .containsExactly(org.springframework.data.repository.Repository.class);
    }

    @Test
    void everyDeclaredReadMethodTakesATenantArgument() {
        for (Method m : StandardRepository.class.getDeclaredMethods()) {
            if (WRITE_METHODS.contains(m.getName())) continue;
            boolean carriesTenant = Arrays.stream(m.getParameters())
                    .anyMatch(p -> p.getName().toLowerCase().contains("tenant")
                            || p.getType().getSimpleName().equals("UUID"));
            assertThat(carriesTenant)
                    .as("%s doit porter le tenant", m.getName())
                    .isTrue();
        }
    }
}
