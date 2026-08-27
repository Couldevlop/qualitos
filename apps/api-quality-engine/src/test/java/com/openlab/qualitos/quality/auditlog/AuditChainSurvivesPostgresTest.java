package com.openlab.qualitos.quality.auditlog;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Ce que H2 et les doublures ne peuvent pas dire.
 *
 * <p>L'empreinte d'un événement est calculée sur l'objet EN MÉMOIRE, puis la
 * ligne part en base ; la vérification, elle, recalcule l'empreinte sur la ligne
 * RELUE. Les deux ne coïncident que si chaque champ haché traverse la base sans
 * changer d'un caractère. La suite du paquet {@code auditlog} tourne sur des
 * dépôts simulés qui rendent l'objet d'origine : elle ne peut pas voir un champ
 * que la base abîme, et elle ne l'a pas vu.
 *
 * <p>Le champ en cause est l'horodatage. {@code Instant} porte la nanoseconde,
 * {@code TIMESTAMP WITH TIME ZONE} n'en garde que la microseconde. Une empreinte
 * calculée avant l'écriture sur un instant à la nanoseconde n'est donc plus
 * recalculable après relecture, et la vérification de chaîne annonce
 * « Integrity hash mismatch (tamper) » sur un journal parfaitement intact —
 * constaté en préproduction sur les 19 événements du registre, le premier
 * compris.
 *
 * <p>Ce banc démarre un vrai PostgreSQL, y rejoue les migrations, écrit puis
 * relit, et compare les empreintes. Sans Docker il se saute plutôt qu'il
 * n'échoue — mais il dit pourquoi.
 */
@Tag("migration")
class AuditChainSurvivesPostgresTest {

    private static final String IMAGE = "postgres:17-alpine";

    private static PostgreSQLContainer<?> postgres;
    private static Connection connection;

    @BeforeAll
    static void migrateOnARealServer() throws SQLException {
        assumeTrue(DockerClientFactory.instance().isDockerAvailable(),
                "Docker indisponible : la traversée de la base reste non vérifiée sur cette machine");

        postgres = new PostgreSQLContainer<>(IMAGE);
        postgres.start();

        Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .locations("classpath:db/migration")
                .load()
                .migrate();

        connection = DriverManager.getConnection(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
    }

    @AfterAll
    static void stop() throws SQLException {
        if (connection != null) connection.close();
        if (postgres != null) postgres.stop();
    }

    // ---------- ce que la base fait de l'horodatage ----------

    @Test
    void postgresRoundsTheTimestampToTheMicrosecond() throws SQLException {
        // Il n'ampute pas, il ARRONDIT : .123456789 ressort en .123457. La nuance
        // compte pour le correctif — un instant déjà ramené à la microseconde n'a
        // rien à arrondir et ressort donc identique, ce que dit le banc suivant.
        UUID tenant = UUID.randomUUID();
        Instant withNanos = Instant.parse("2026-08-27T18:18:04.123456789Z");

        insert(event(tenant, 1L, withNanos, null, "hash-nanos"));

        assertThat(readOccurredAt(tenant, 1L))
                .isEqualTo(Instant.parse("2026-08-27T18:18:04.123457Z"));
    }

    // ---------- et ce que cela fait à l'empreinte ----------

    @Test
    void anEventTimestampedToTheNanosecondCannotBeVerifiedAfterItsRoundTrip() throws SQLException {
        // Le défaut lui-même : on hache avant l'écriture, la base rabote, et le
        // recalcul ne retombe plus jamais sur le même hexadécimal.
        UUID tenant = UUID.randomUUID();
        AuditEvent written = event(tenant, 1L, Instant.parse("2026-08-27T18:18:04.123456789Z"),
                null, null);
        written.setIntegrityHash(AuditEventHasher.hash(written));
        insert(written);

        AuditEvent reread = reread(tenant, 1L);

        assertThat(AuditEventHasher.hash(reread)).isNotEqualTo(written.getIntegrityHash());
    }

    @Test
    void anEventTruncatedBeforeHashingSurvivesItsRoundTrip() throws SQLException {
        // Le correctif : ne hacher que ce que la base sait rendre. L'instant est
        // ramené à la microseconde AVANT le calcul, donc l'aller-retour ne change
        // plus rien.
        UUID tenant = UUID.randomUUID();
        AuditEvent written = event(tenant, 1L,
                Instant.parse("2026-08-27T18:18:04.123456789Z")
                        .truncatedTo(java.time.temporal.ChronoUnit.MICROS),
                null, null);
        written.setIntegrityHash(AuditEventHasher.hash(written));
        insert(written);

        AuditEvent reread = reread(tenant, 1L);

        assertThat(AuditEventHasher.hash(reread)).isEqualTo(written.getIntegrityHash());
    }

    @Test
    void aWholeChainWrittenByTheServiceRuleVerifiesAfterItsRoundTrip() throws SQLException {
        // Trois événements chaînés, écrits avec la règle du service corrigé :
        // chacun doit se recalculer, et chacun doit pointer sur le précédent.
        UUID tenant = UUID.randomUUID();
        String previous = null;
        String[] hashes = new String[3];

        for (int i = 0; i < 3; i++) {
            AuditEvent e = event(tenant, i + 1L,
                    Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS),
                    previous, null);
            e.setIntegrityHash(AuditEventHasher.hash(e));
            insert(e);
            hashes[i] = e.getIntegrityHash();
            previous = e.getIntegrityHash();
        }

        String expectedPrevious = null;
        for (int i = 0; i < 3; i++) {
            AuditEvent reread = reread(tenant, i + 1L);
            assertThat(reread.getPreviousHash()).isEqualTo(expectedPrevious);
            assertThat(AuditEventHasher.hash(reread)).isEqualTo(hashes[i]);
            expectedPrevious = reread.getIntegrityHash();
        }
    }

    // ---------- plomberie ----------

    private static AuditEvent event(UUID tenant, long seq, Instant occurredAt,
                                    String previousHash, String integrityHash) {
        AuditEvent e = new AuditEvent();
        e.setId(UUID.randomUUID());
        e.setTenantId(tenant);
        e.setSequenceNo(seq);
        e.setOccurredAt(occurredAt);
        e.setRecordedAt(occurredAt);
        e.setActorType(ActorType.USER);
        e.setActorUserId(UUID.randomUUID());
        e.setAction("pdca.step-evidence.uploaded");
        e.setResourceType("pdca_step_evidence");
        e.setResourceId(UUID.randomUUID());
        e.setSummary("Preuve versée à l'étape " + seq);
        e.setPayloadJson("{\"sizeBytes\":1024}");
        e.setPreviousHash(previousHash);
        e.setIntegrityHash(integrityHash == null ? "x".repeat(64) : pad(integrityHash));
        return e;
    }

    /** La colonne est un CHAR(64) : les libellés lisibles des bancs y sont complétés. */
    private static String pad(String value) {
        return value.length() >= 64 ? value.substring(0, 64)
                : value + "0".repeat(64 - value.length());
    }

    private static void insert(AuditEvent e) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("""
                insert into audit_events (id, tenant_id, sequence_no, occurred_at, recorded_at,
                                          actor_type, actor_user_id, action, resource_type,
                                          resource_id, summary, payload_json, integrity_hash,
                                          previous_hash)
                values (?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                """)) {
            ps.setObject(1, e.getId());
            ps.setObject(2, e.getTenantId());
            ps.setLong(3, e.getSequenceNo());
            ps.setTimestamp(4, Timestamp.from(e.getOccurredAt()));
            ps.setTimestamp(5, Timestamp.from(e.getRecordedAt()));
            ps.setString(6, e.getActorType().name());
            ps.setObject(7, e.getActorUserId());
            ps.setString(8, e.getAction());
            ps.setString(9, e.getResourceType());
            ps.setObject(10, e.getResourceId());
            ps.setString(11, e.getSummary());
            ps.setString(12, e.getPayloadJson());
            ps.setString(13, e.getIntegrityHash());
            ps.setString(14, e.getPreviousHash());
            ps.executeUpdate();
        }
    }

    private static Instant readOccurredAt(UUID tenant, long seq) throws SQLException {
        return reread(tenant, seq).getOccurredAt();
    }

    private static AuditEvent reread(UUID tenant, long seq) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("""
                select id, tenant_id, sequence_no, occurred_at, recorded_at, actor_type,
                       actor_user_id, action, resource_type, resource_id, summary,
                       payload_json, integrity_hash, previous_hash
                  from audit_events
                 where tenant_id = ? and sequence_no = ?
                """)) {
            ps.setObject(1, tenant);
            ps.setLong(2, seq);
            try (ResultSet rs = ps.executeQuery()) {
                assertThat(rs.next()).as("événement %d du tenant %s", seq, tenant).isTrue();
                AuditEvent e = new AuditEvent();
                e.setId(rs.getObject(1, UUID.class));
                e.setTenantId(rs.getObject(2, UUID.class));
                e.setSequenceNo(rs.getLong(3));
                e.setOccurredAt(rs.getTimestamp(4).toInstant());
                e.setRecordedAt(rs.getTimestamp(5).toInstant());
                e.setActorType(ActorType.valueOf(rs.getString(6)));
                e.setActorUserId(rs.getObject(7, UUID.class));
                e.setAction(rs.getString(8));
                e.setResourceType(rs.getString(9));
                e.setResourceId(rs.getObject(10, UUID.class));
                e.setSummary(rs.getString(11));
                e.setPayloadJson(rs.getString(12));
                e.setIntegrityHash(rs.getString(13));
                e.setPreviousHash(rs.getString(14));
                return e;
            }
        }
    }
}
