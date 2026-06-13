package com.openlab.qualitos.quality;

import com.openlab.qualitos.quality.ai.guard.TokenBucketAiGuard;
import com.openlab.qualitos.quality.erpconnector.DynamicsClient;
import com.openlab.qualitos.quality.erpconnector.OracleFusionClient;
import com.openlab.qualitos.quality.erpconnector.SapOdataClient;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Boot RÉEL du contexte Spring complet (component scan entier, aucun slice).
 *
 * <p>Classe de bug visée (déjà mordue 2 fois : TokenBucketAiGuard puis clients ERP) :
 * un {@code @Component} avec DEUX constructeurs (prod {@code @Value} + package-private
 * de test) SANS {@code @Autowired} sur celui de prod fait échouer le boot Spring
 * (« No default constructor found »). Les slices Mockito — pattern exclusif du module —
 * ne bootent jamais le contexte, donc la CI ne l'attrapait pas. Ce test comble
 * ce trou : un simple {@code mvn test} révèle désormais tout bug de câblage.
 *
 * <p>Idiome repris d'api-iot-hub ({@code DeviceControllerIntegrationTest}) : H2
 * in-memory MODE=PostgreSQL via {@code application-test.yml}, Flyway désactivé
 * (schéma généré par Hibernate create-drop — le schéma de prod reste validé par
 * les 86 migrations Flyway en environnement réel), JWT resource server pointé sur
 * un issuer factice (décodeur paresseux, jamais résolu au boot). Les beans gardés
 * par {@code @Profile} (fabric, tls…) ne se chargent pas : c'est voulu, ce test
 * cible les beans par défaut.
 */
@SpringBootTest
@ActiveProfiles("test")
@Tag("web")
class QualityEngineContextLoadsTest {

    @Autowired
    private ApplicationContext context;

    @Test
    void contextLoads() {
        assertThat(context).isNotNull();
    }

    /**
     * Témoin explicite : les beans historiquement cassés par le bug des
     * constructeurs multiples doivent s'instancier au boot par défaut.
     */
    @Test
    void beansHistoriquementFragilesSontInstancies() {
        assertThat(context.getBean(TokenBucketAiGuard.class)).isNotNull();
        assertThat(context.getBean(DynamicsClient.class)).isNotNull();
        assertThat(context.getBean(SapOdataClient.class)).isNotNull();
        assertThat(context.getBean(OracleFusionClient.class)).isNotNull();
    }
}
