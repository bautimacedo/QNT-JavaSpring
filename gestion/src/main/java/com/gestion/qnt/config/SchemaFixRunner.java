package com.gestion.qnt.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Aplica correcciones de schema que Hibernate no puede manejar solo y migraciones
 * de datos idempotentes. Corre después de que Hibernate termina su ddl-auto=update.
 */
@Component
public class SchemaFixRunner {

    private static final Logger log = LoggerFactory.getLogger(SchemaFixRunner.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @EventListener(ApplicationReadyEvent.class)
    public void fixSchema() {
        // piloto_id es nullable: el piloto se asigna al lanzar la misión, no al crearla
        jdbcTemplate.execute("ALTER TABLE misiones ALTER COLUMN piloto_id DROP NOT NULL");

        migrateVuelosLog();
    }

    /**
     * Idempotente: convierte eventos DESPEGUE/ATERRIZAJE históricos al formato correcto.
     *
     * 1. EFO: crea un VUELO por cada par DESPEGUE+ATERRIZAJE que no tenga VUELO cercano.
     * 2. Elimina DESPEGUE/ATERRIZAJE con más de 2 horas (completarPorDrone ya los procesó).
     * 3. CAM: convierte los DESPEGUE/ATERRIZAJE restantes a VUELO (FlightHub no emite VUELO).
     */
    private void migrateVuelosLog() {
        try {
            // ── 1. EFO: crear VUELOs de pares DESPEGUE→ATERRIZAJE sin VUELO existente ──
            int efoCreados = jdbcTemplate.update("""
                INSERT INTO vuelos_log
                    (evento, event_id, severidad, nombre_dron, nombre_dock, site,
                     latitud, longitud, altitud, bateria, piloto, detalle_vuelo,
                     timestamp_flytbase, despegue_fallido, duracion_minutos, fecha_registro)
                SELECT
                    'VUELO',
                    'migration-efo-' || d.id::text,
                    COALESCE(d.severidad, 'INFO'),
                    d.nombre_dron, d.nombre_dock, d.site,
                    d.latitud, d.longitud, d.altitud,
                    COALESCE(a.bateria, d.bateria),
                    COALESCE(d.piloto, a.piloto),
                    COALESCE(d.detalle_vuelo, a.detalle_vuelo),
                    d.timestamp_flytbase,
                    false,
                    GREATEST(0, ROUND(EXTRACT(EPOCH FROM (a.timestamp_flytbase - d.timestamp_flytbase)) / 60)::int),
                    d.fecha_registro
                FROM vuelos_log d
                JOIN LATERAL (
                    SELECT * FROM vuelos_log a2
                    WHERE a2.site = d.site
                      AND (d.nombre_dron IS NULL OR a2.nombre_dron IS NULL OR a2.nombre_dron = d.nombre_dron)
                      AND a2.evento = 'ATERRIZAJE'
                      AND a2.timestamp_flytbase > d.timestamp_flytbase
                      AND a2.timestamp_flytbase < d.timestamp_flytbase + INTERVAL '8 hours'
                    ORDER BY a2.timestamp_flytbase ASC LIMIT 1
                ) a ON true
                WHERE d.site = 'EFO'
                  AND d.evento = 'DESPEGUE'
                  AND (d.despegue_fallido IS NULL OR d.despegue_fallido = false)
                  AND NOT EXISTS (
                    SELECT 1 FROM vuelos_log v
                    WHERE v.site = d.site
                      AND v.evento = 'VUELO'
                      AND v.timestamp_flytbase >= d.timestamp_flytbase - INTERVAL '30 minutes'
                      AND v.timestamp_flytbase <= a.timestamp_flytbase + INTERVAL '30 minutes'
                      AND (d.nombre_dron IS NULL OR v.nombre_dron IS NULL OR v.nombre_dron = d.nombre_dron)
                  )
                ON CONFLICT (event_id) DO NOTHING
                """);
            if (efoCreados > 0) log.info("[vuelos-migration] EFO: {} VUELOs creados de pares DESPEGUE-ATERRIZAJE", efoCreados);

            // ── 2. Eliminar DESPEGUE/ATERRIZAJE antiguos (> 2 h; completarPorDrone ya los usó) ──
            int eliminados = jdbcTemplate.update("""
                DELETE FROM vuelos_log
                WHERE evento IN ('DESPEGUE', 'ATERRIZAJE')
                  AND fecha_registro < NOW() - INTERVAL '2 hours'
                """);
            if (eliminados > 0) log.info("[vuelos-migration] Eliminados {} registros DESPEGUE/ATERRIZAJE antiguos", eliminados);

            // ── 3. CAM: convertir DESPEGUE/ATERRIZAJE recientes → VUELO ─────────────
            int camConvertidos = jdbcTemplate.update("""
                UPDATE vuelos_log SET evento = 'VUELO'
                WHERE site = 'CAM' AND evento IN ('DESPEGUE', 'ATERRIZAJE')
                """);
            if (camConvertidos > 0) log.info("[vuelos-migration] CAM: {} registros convertidos a VUELO", camConvertidos);

        } catch (Exception e) {
            log.warn("[vuelos-migration] Falló: {}", e.getMessage());
        }
    }
}
