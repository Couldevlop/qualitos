package com.openlab.qualitos.quality.rls;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Pose, sous le rôle propriétaire, ce que l'isolation par la base exige
 * (ADR 0086) : le rôle applicatif et ses droits, puis une politique sur CHAQUE
 * table portant une colonne {@code tenant_id}.
 *
 * <p>Les tables sont découvertes à chaque passage, pas listées : une table créée
 * par une migration future est couverte sans que personne n'y pense. Tout est
 * rejouable — c'est appelé après chaque migration, donc à chaque démarrage.
 *
 * <p>La politique contraint quand un client est fixé dans la session
 * ({@code app.tenant_id}) : on ne voit alors que ses lignes, et les lignes
 * partagées ({@code tenant_id} NULL). Sans client fixé — travaux de fond,
 * relais, chargements au démarrage — rien ne change : ces chemins parcourent
 * légitimement tous les clients. La protection vise l'oubli d'un filtre dans le
 * traitement d'une requête, qui est le risque réel d'une application multi-client.
 */
public final class RlsSchema {

    public static final String POLICY = "qualitos_tenant_isolation";
    public static final String SETTING = "app.tenant_id";

    private static final Pattern ROLE_NAME = Pattern.compile("[a-z_][a-z0-9_]{0,62}");
    private static final String CURRENT = "NULLIF(current_setting('" + SETTING + "', true), '')";

    private RlsSchema() {
    }

    /** Crée le rôle applicatif s'il manque, (re)pose son mot de passe et ses droits. */
    public static void applyRole(Connection owner, String role, String password) throws SQLException {
        if (!ROLE_NAME.matcher(role).matches()) {
            throw new IllegalArgumentException("nom de rôle applicatif invalide : " + role);
        }
        if (password == null || password.length() < 16) {
            throw new IllegalArgumentException("mot de passe du rôle applicatif absent ou trop court (16 caractères minimum)");
        }
        boolean exists;
        try (PreparedStatement ps = owner.prepareStatement("SELECT 1 FROM pg_roles WHERE rolname = ?")) {
            ps.setString(1, role);
            try (ResultSet rs = ps.executeQuery()) {
                exists = rs.next();
            }
        }
        // Le mot de passe passe par format(%L) côté serveur : jamais concaténé ici.
        String verb = exists ? "ALTER" : "CREATE";
        String ddl;
        try (PreparedStatement ps = owner.prepareStatement(
                "SELECT format('" + verb + " ROLE %I WITH LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOBYPASSRLS PASSWORD %L', ?, ?)")) {
            ps.setString(1, role);
            ps.setString(2, password);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                ddl = rs.getString(1);
            }
        }
        try (Statement st = owner.createStatement()) {
            st.execute(ddl);
            String r = quoteIdent(owner, role);
            st.execute("GRANT CONNECT ON DATABASE " + quoteIdent(owner, currentDatabase(owner)) + " TO " + r);
            st.execute("GRANT USAGE ON SCHEMA public TO " + r);
            st.execute("GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO " + r);
            st.execute("GRANT USAGE, SELECT, UPDATE ON ALL SEQUENCES IN SCHEMA public TO " + r);
            st.execute("GRANT EXECUTE ON ALL FUNCTIONS IN SCHEMA public TO " + r);
            // L'historique des migrations n'appartient qu'au propriétaire.
            try (ResultSet rs = st.executeQuery("SELECT to_regclass('public.flyway_schema_history') IS NOT NULL")) {
                rs.next();
                if (rs.getBoolean(1)) {
                    st.execute("REVOKE ALL ON public.flyway_schema_history FROM " + r);
                }
            }
        }
    }

    /**
     * Pose la politique sur chaque table à {@code tenant_id}, et l'active ou la
     * désactive selon {@code enabled}. Rend les tables traitées.
     */
    public static List<String> applyPolicies(Connection owner, boolean enabled) throws SQLException {
        List<String[]> tables = new ArrayList<>();
        try (PreparedStatement ps = owner.prepareStatement("""
                SELECT c.relname, format_type(a.atttypid, a.atttypmod)
                  FROM pg_class c
                  JOIN pg_namespace n ON n.oid = c.relnamespace
                  JOIN pg_attribute a ON a.attrelid = c.oid AND a.attname = 'tenant_id' AND NOT a.attisdropped
                 WHERE n.nspname = 'public' AND c.relkind IN ('r', 'p')
                 ORDER BY c.relname""");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                tables.add(new String[] {rs.getString(1), rs.getString(2)});
            }
        }
        List<String> done = new ArrayList<>();
        try (Statement st = owner.createStatement()) {
            for (String[] t : tables) {
                String cast = castFor(t[1]);
                if (cast == null) {
                    continue; // type inattendu : on ne devine pas
                }
                String table = quoteIdent(owner, t[0]);
                String predicate = CURRENT + " IS NULL OR tenant_id IS NULL OR tenant_id = " + CURRENT + cast;
                st.execute("DROP POLICY IF EXISTS " + POLICY + " ON " + table);
                st.execute("CREATE POLICY " + POLICY + " ON " + table
                        + " USING (" + predicate + ") WITH CHECK (" + predicate + ")");
                st.execute("ALTER TABLE " + table + (enabled ? " ENABLE" : " DISABLE") + " ROW LEVEL SECURITY");
                done.add(t[0]);
            }
        }
        return done;
    }

    static String castFor(String type) {
        return switch (type) {
            case "uuid" -> "::uuid";
            case "text" -> "";
            default -> type.startsWith("character varying") ? "" : null;
        };
    }

    private static String currentDatabase(Connection c) throws SQLException {
        try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery("SELECT current_database()")) {
            rs.next();
            return rs.getString(1);
        }
    }

    private static String quoteIdent(Connection c, String ident) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT quote_ident(?)")) {
            ps.setString(1, ident);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getString(1);
            }
        }
    }
}
