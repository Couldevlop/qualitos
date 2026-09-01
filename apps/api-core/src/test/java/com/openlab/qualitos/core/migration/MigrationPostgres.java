package com.openlab.qualitos.core.migration;

import org.flywaydb.core.Flyway;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Le serveur PostgreSQL PARTAGÉ par les bancs de migration, démarré une seule
 * fois pour toute la JVM.
 *
 * <p><b>Pourquoi partagé.</b> Chaque banc de migration démarrait son propre
 * conteneur. Surefire exécute les classes de test en CONCURRENCE
 * ({@code junit.jupiter.execution.parallel.mode.classes.default=concurrent},
 * pom parent), si bien qu'à quatre bancs, quatre PostgreSQL se lançaient
 * ensemble. Sur une machine à 8 Go alloués à Docker, le démarrage échoue —
 * « Container startup failed » — et l'échec est INTERMITTENT, donc lu comme un
 * banc instable plutôt que comme une contrainte de ressources. Un serveur, un
 * jeu de migrations, quatre bancs : le coût ne grandit plus avec le nombre de
 * bancs.
 *
 * <p><b>Une connexion par banc, pas une connexion partagée.</b>
 * {@link Connection} n'est pas sûre entre fils d'exécution, et les classes
 * tournent en parallèle. Le conteneur se partage, la connexion non.
 *
 * <p><b>Les bancs ne se marchent pas dessus</b> parce que chacun crée ses
 * propres lignes, sous des identifiants et des clés naturelles qui lui sont
 * propres (slug de client, code de module, numéro de facture). Aucun ne compte
 * le contenu global d'une table — un décompte global serait le seul geste que
 * le partage rendrait faux.
 */
final class MigrationPostgres {

    private static final String IMAGE = "postgres:17-alpine";

    private static PostgreSQLContainer<?> container;

    private MigrationPostgres() {}

    static boolean dockerAvailable() {
        return DockerClientFactory.instance().isDockerAvailable();
    }

    /**
     * Une connexion neuve vers le serveur partagé, dont les migrations d'api-core
     * ont déjà été rejouées — les VRAIES, dans l'ordre, pas une recréation
     * Hibernate : c'est exactement le SQL qui partira en production.
     */
    static Connection connect() throws SQLException {
        ensureStarted();
        return DriverManager.getConnection(
                container.getJdbcUrl(), container.getUsername(), container.getPassword());
    }

    private static synchronized void ensureStarted() {
        if (container != null) {
            return;
        }
        PostgreSQLContainer<?> started = new PostgreSQLContainer<>(IMAGE);
        started.start();
        Flyway.configure()
                .dataSource(started.getJdbcUrl(), started.getUsername(), started.getPassword())
                .locations("classpath:db/migration")
                .load()
                .migrate();
        // Arrêt à la fin de la JVM plutôt qu'en @AfterAll : le conteneur ne
        // sait pas quel banc est le dernier. Sans ce crochet, un Ryuk désactivé
        // (le cas sur cette machine) laisserait le conteneur tourner après la
        // suite.
        Runtime.getRuntime().addShutdownHook(new Thread(started::stop));
        container = started;
    }
}
