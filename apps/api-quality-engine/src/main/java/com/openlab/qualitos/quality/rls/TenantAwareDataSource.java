package com.openlab.qualitos.quality.rls;

import com.openlab.qualitos.quality.common.TenantContext;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.jdbc.datasource.DelegatingDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Passe le client courant à PostgreSQL à chaque emprunt d'une connexion
 * (ADR 0086) : {@code app.tenant_id}, que lisent les politiques.
 *
 * <p>Posé à CHAQUE emprunt, client ou non : une connexion rendue au pool garde
 * ses réglages, et le client de la requête précédente ne doit jamais déteindre
 * sur la suivante. Une transaction garde sa connexion du début à la fin, donc
 * son client.
 */
public class TenantAwareDataSource extends DelegatingDataSource {

    public TenantAwareDataSource(DataSource target) {
        super(target);
    }

    @Override
    public Connection getConnection() throws SQLException {
        return withTenant(super.getConnection());
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return withTenant(super.getConnection(username, password));
    }

    // Lus par Spring Boot quand il dérive de cette source la connexion des
    // migrations (rôle propriétaire, spring.flyway.user) : sans ces accesseurs,
    // le démarrage échoue sur « Unable to find suitable method for url ».
    public String getUrl() { return hikari() == null ? null : hikari().getJdbcUrl(); }
    public String getUsername() { return hikari() == null ? null : hikari().getUsername(); }
    public String getPassword() { return hikari() == null ? null : hikari().getPassword(); }
    public String getDriverClassName() { return hikari() == null ? null : hikari().getDriverClassName(); }

    private HikariDataSource hikari() {
        return getTargetDataSource() instanceof HikariDataSource h ? h : null;
    }

    private static Connection withTenant(Connection connection) throws SQLException {
        String tenant = TenantContext.hasTenant() ? TenantContext.getTenantId() : "";
        try (PreparedStatement ps = connection.prepareStatement("SELECT set_config('" + RlsSchema.SETTING + "', ?, false)")) {
            ps.setString(1, tenant);
            ps.execute();
        } catch (SQLException e) {
            connection.close();
            throw e;
        }
        return connection;
    }
}
