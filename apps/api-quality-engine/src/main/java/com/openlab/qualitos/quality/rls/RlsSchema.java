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

    private static final String CREATE_ROLE =
            "CREATE ROLE %I WITH LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOBYPASSRLS PASSWORD %L";
    private static final String ALTER_ROLE =
            "ALTER ROLE %I WITH LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOBYPASSRLS PASSWORD %L";
    private static final String ENABLE_RLS = "ALTER TABLE %I ENABLE ROW LEVEL SECURITY";
    private static final String DISABLE_RLS = "ALTER TABLE %I DISABLE ROW LEVEL SECURITY";
    private static final String[] FORMAT = {
        "SELECT format(?)", "SELECT format(?, ?)", "SELECT format(?, ?, ?)"};

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
        // Chaque instruction est construite PAR POSTGRESQL (format, %I pour les
        // noms, %L pour les valeurs) à partir d'un gabarit constant : aucun nom
        // ni mot de passe n'est jamais concaténé dans du SQL ici.
        ddl(owner, exists ? ALTER_ROLE : CREATE_ROLE, role, password);
        ddl(owner, "GRANT CONNECT ON DATABASE %I TO %I", currentDatabase(owner), role);
        ddl(owner, "GRANT USAGE ON SCHEMA public TO %I", role);
        ddl(owner, "GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO %I", role);
        ddl(owner, "GRANT USAGE, SELECT, UPDATE ON ALL SEQUENCES IN SCHEMA public TO %I", role);
        ddl(owner, "GRANT EXECUTE ON ALL FUNCTIONS IN SCHEMA public TO %I", role);
        // L'historique des migrations n'appartient qu'au propriétaire.
        try (Statement st = owner.createStatement();
             ResultSet rs = st.executeQuery("SELECT to_regclass('public.flyway_schema_history') IS NOT NULL")) {
            rs.next();
            if (rs.getBoolean(1)) {
                ddl(owner, "REVOKE ALL ON public.flyway_schema_history FROM %I", role);
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
        for (String[] t : tables) {
            String create = policyFor(t[1]);
            if (create == null) {
                continue; // type inattendu : on ne devine pas
            }
            ddl(owner, "DROP POLICY IF EXISTS " + POLICY + " ON %I", t[0]);
            ddl(owner, create, t[0]);
            ddl(owner, enabled ? ENABLE_RLS : DISABLE_RLS, t[0]);
            done.add(t[0]);
        }
        return done;
    }

    /** Le gabarit de création de la politique selon le type de {@code tenant_id}. */
    static String policyFor(String type) {
        String cast = castFor(type);
        if (cast == null) {
            return null;
        }
        String predicate = CURRENT + " IS NULL OR tenant_id IS NULL OR tenant_id = " + CURRENT + cast;
        return "CREATE POLICY " + POLICY + " ON %I USING (" + predicate + ") WITH CHECK (" + predicate + ")";
    }

    /**
     * Exécute une instruction que PostgreSQL construit lui-même depuis un gabarit
     * constant ({@code format}) et ses arguments liés : noms échappés par %I,
     * valeurs par %L.
     */
    private static void ddl(Connection c, String template, String... args) throws SQLException {
        String sql;
        try (PreparedStatement ps = c.prepareStatement(FORMAT[args.length])) {
            ps.setString(1, template);
            for (int i = 0; i < args.length; i++) {
                ps.setString(i + 2, args[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                sql = rs.getString(1);
            }
        }
        try (Statement st = c.createStatement()) {
            st.execute(sql);
        }
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
}
