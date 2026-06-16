-- =====================================================================
-- api-iot-hub — V2: TimescaleDB rollups (continuous aggregate + policies)
--   CLAUDE.md §9.3 (TimescaleDB) + §6.4 (KPI temps réel).
--
-- INOFFENSIF sur PostgreSQL simple ET H2 (tests) : TOUT le contenu vit dans
-- un bloc `DO $$ IF EXISTS(timescaledb) ... END IF; $$`. Sans l'extension
-- TimescaleDB, le bloc ne s'exécute pas → aucune fonction Timescale
-- (create_hypertable / add_*_policy / CREATE MATERIALIZED VIEW ... WITH
-- (timescaledb.continuous)) n'est jamais référencée hors du IF.
--
-- L'endpoint REST de rollup (`/telemetry/rollup`) calcule, lui, les agrégats
-- en SQL standard portable (date_trunc) : il fonctionne SANS cette migration.
-- La continuous aggregate ci-dessous n'est qu'un ACCÉLÉRATEUR sur le runtime
-- TimescaleDB de production ; le résultat fonctionnel est identique.
-- =====================================================================

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_extension WHERE extname = 'timescaledb') THEN

        -- Continuous aggregate horaire : avg/min/max/count par
        -- tenant + device + métrique, sur des buckets d'1 heure.
        EXECUTE $cagg$
            CREATE MATERIALIZED VIEW IF NOT EXISTS iot_telemetry_hourly
            WITH (timescaledb.continuous) AS
            SELECT time_bucket(INTERVAL '1 hour', recorded_at) AS bucket,
                   tenant_id,
                   device_id,
                   metric,
                   AVG(value_double) AS avg_value,
                   MIN(value_double) AS min_value,
                   MAX(value_double) AS max_value,
                   COUNT(value_double) AS sample_count
              FROM iot_telemetry
             WHERE value_double IS NOT NULL
             GROUP BY bucket, tenant_id, device_id, metric
            WITH NO DATA
        $cagg$;

        -- Rafraîchissement automatique de la continuous aggregate.
        PERFORM add_continuous_aggregate_policy('iot_telemetry_hourly',
            start_offset      => INTERVAL '3 hours',
            end_offset        => INTERVAL '1 hour',
            schedule_interval => INTERVAL '1 hour',
            if_not_exists     => TRUE);

        -- Rétention : on purge les chunks de télémétrie brute > 90 jours
        -- (les agrégats horaires restent disponibles via la continuous aggregate).
        PERFORM add_retention_policy('iot_telemetry',
            drop_after    => INTERVAL '90 days',
            if_not_exists => TRUE);

        -- Compression : compresse les chunks bruts > 7 jours.
        BEGIN
            ALTER TABLE iot_telemetry SET (
                timescaledb.compress,
                timescaledb.compress_segmentby = 'tenant_id, device_id, metric'
            );
        EXCEPTION WHEN others THEN
            -- ALTER ... SET (compress) est idempotent côté intention ; on tolère
            -- l'erreur si la compression est déjà configurée.
            NULL;
        END;

        PERFORM add_compression_policy('iot_telemetry',
            compress_after => INTERVAL '7 days',
            if_not_exists  => TRUE);

    END IF;
END $$;
