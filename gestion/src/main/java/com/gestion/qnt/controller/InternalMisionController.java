package com.gestion.qnt.controller;

import com.gestion.qnt.config.ApiConstants;
import com.gestion.qnt.model.*;
import com.gestion.qnt.model.enums.Estado;
import com.gestion.qnt.model.enums.EstadoMision;
import com.gestion.qnt.repository.DronRepository;
import com.gestion.qnt.repository.MisionRepository;
import com.gestion.qnt.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Endpoints internos para integraciones sin JWT (n8n, scripts).
 * Protegidos por el header X-Internal-Secret.
 * Ruta base: /api/qnt/v1/internal/
 */
@RestController
@RequestMapping(ApiConstants.URL_BASE + "/internal")
public class InternalMisionController {

    @Value("${qnt.internal.secret}")
    private String internalSecret;

    @Autowired private MisionRepository misionRepository;
    @Autowired private DronRepository dronRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    /**
     * Llamado por n8n cuando FlytBase detecta ATERRIZAJE.
     * Completa la misión y suma stats a drone, batería y piloto.
     *
     * Body: { "dronNombre": "EFO-Q1", "duracionMinutos": 23 }
     * Header: X-Internal-Secret: <secret>
     */
    @PostMapping("/misiones/completar-por-drone")
    @Transactional
    public ResponseEntity<Map<String, Object>> completarPorDrone(
            @RequestHeader(value = "X-Internal-Secret", required = false) String secret,
            @RequestBody Map<String, Object> body) {

        if (!internalSecret.equals(secret)) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }

        String dronNombre = (String) body.get("dronNombre");

        if (dronNombre == null || dronNombre.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "dronNombre es requerido"));
        }

        // Leer duracion_minutos del último ATERRIZAJE en vuelos_log (ya lo calculó n8n)
        Integer duracionMinutos = 0;
        try {
            Integer dur = jdbcTemplate.queryForObject(
                    "SELECT duracion_minutos FROM vuelos_log " +
                    "WHERE nombre_dron = ? AND evento = 'ATERRIZAJE' AND duracion_minutos IS NOT NULL " +
                    "ORDER BY fecha_registro DESC LIMIT 1",
                    Integer.class, dronNombre);
            if (dur != null) duracionMinutos = dur;
        } catch (Exception ignored) {}

        // Buscar misión pendiente para este drone
        List<Map<String, Object>> filas = jdbcTemplate.queryForList(
                "SELECT id, mision_id, usuario_id FROM mision_pendiente " +
                "WHERE drone_nombre = ? AND procesado = false AND mision_id IS NOT NULL " +
                "ORDER BY timestamp_lanzamiento DESC LIMIT 1",
                dronNombre);

        if (filas.isEmpty()) {
            return ResponseEntity.ok(Map.of(
                "message", "No hay misión pendiente para " + dronNombre,
                "accion", "ignorado"));
        }

        Map<String, Object> fila = filas.get(0);
        long pendienteId = ((Number) fila.get("id")).longValue();
        long misionId    = ((Number) fila.get("mision_id")).longValue();

        // Marcar procesado de entrada para evitar doble ejecución
        jdbcTemplate.update("UPDATE mision_pendiente SET procesado = true WHERE id = ?", pendienteId);

        Mision m = misionRepository.findById(misionId).orElse(null);
        if (m == null || m.getEstado() != EstadoMision.EN_CURSO) {
            return ResponseEntity.ok(Map.of(
                "message", "Misión " + misionId + " no encontrada o no está EN_CURSO",
                "accion", "ignorado"));
        }

        // ── 1. Completar la misión ──────────────────────────────
        LocalDateTime ahora = LocalDateTime.now();
        m.setEstado(EstadoMision.COMPLETADA);
        m.setFechaFin(ahora);
        misionRepository.save(m);

        // ── 2. Stats del drone ──────────────────────────────────
        Dron dron = m.getDron();
        if (dron != null) {
            dron.setCantidadVuelos((dron.getCantidadVuelos() != null ? dron.getCantidadVuelos() : 0) + 1);
            dron.setCantidadMinutosVolados((dron.getCantidadMinutosVolados() != null ? dron.getCantidadMinutosVolados() : 0) + duracionMinutos);
            dron.setUltimoVuelo(Instant.now());
            dronRepository.save(dron);

            // ── 3. Stats de batería (la que esté activa en el drone) ──
            for (Bateria bateria : dron.getBaterias()) {
                if (bateria.getEstado() == Estado.STOCK_ACTIVO) {
                    bateria.setCantidadVuelos((bateria.getCantidadVuelos() != null ? bateria.getCantidadVuelos() : 0) + 1);
                    bateria.setCantidadMinutosVolados((bateria.getCantidadMinutosVolados() != null ? bateria.getCantidadMinutosVolados() : 0) + duracionMinutos);
                    bateria.setCiclosCarga((bateria.getCiclosCarga() != null ? bateria.getCiclosCarga() : 0) + 1);
                    // bateriaRepository.save() no hace falta: cascade ALL desde Dron
                }
            }

            // ── 4. Hélices ─────────────────────────────────────────
            for (Helice helice : dron.getHelices()) {
                if (helice.getEstado() == Estado.STOCK_ACTIVO) {
                    helice.setCantidadVuelos((helice.getCantidadVuelos() != null ? helice.getCantidadVuelos() : 0) + 1);
                    helice.setCantidadMinutosVolados((helice.getCantidadMinutosVolados() != null ? helice.getCantidadMinutosVolados() : 0) + duracionMinutos);
                }
            }
        }

        // ── 5. Stats del piloto ─────────────────────────────────
        Usuario piloto = m.getPiloto();
        String pilotoNombre = "sin piloto";
        if (piloto != null) {
            double horasActuales = piloto.getHorasVuelo() != null ? piloto.getHorasVuelo() : 0.0;
            piloto.setHorasVuelo(Math.round((horasActuales + duracionMinutos / 60.0) * 100.0) / 100.0);
            piloto.setCantidadVuelos((piloto.getCantidadVuelos() != null ? piloto.getCantidadVuelos() : 0) + 1);
            usuarioRepository.save(piloto);
            pilotoNombre = piloto.getNombre() + " " + (piloto.getApellido() != null ? piloto.getApellido() : "");
        }

        return ResponseEntity.ok(Map.of(
                "message",          "Misión completada correctamente",
                "misionId",         misionId,
                "drone",            dron != null ? dron.getNombre() : "sin drone",
                "piloto",           pilotoNombre.trim(),
                "duracionMinutos",  duracionMinutos));
    }
}
