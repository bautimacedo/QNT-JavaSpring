package com.gestion.qnt.controller;

import com.gestion.qnt.config.ApiConstants;
import com.gestion.qnt.model.*;
import com.gestion.qnt.model.Dock;
import com.gestion.qnt.model.enums.Estado;
import com.gestion.qnt.model.enums.EstadoMision;
import com.gestion.qnt.model.enums.TipoEventoVuelo;
import com.gestion.qnt.model.enums.Yacimiento;
import com.gestion.qnt.model.VueloLog;
import com.gestion.qnt.repository.DronRepository;
import com.gestion.qnt.repository.MisionRepository;
import com.gestion.qnt.repository.UsuarioRepository;
import com.gestion.qnt.repository.VueloLogRepository;
import com.gestion.qnt.service.FlytbaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
    @Autowired private VueloLogRepository vueloLogRepository;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private FlytbaseService flytbaseService;

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
                    "WHERE nombre_dron = ? AND evento IN ('ATERRIZAJE','VUELO') AND duracion_minutos IS NOT NULL " +
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

    // ─────────────────────────────────────────────────────────────────────────
    // POST /internal/vuelo-log
    // n8n llama este endpoint en lugar de insertar directo en la DB.
    // CAM  → DESPEGUE/ATERRIZAJE se convierte a VUELO directamente.
    // EFO  → DESPEGUE se guarda tal cual (pendiente);
    //         ATERRIZAJE busca el DESPEGUE previo y crea un registro VUELO.
    // ─────────────────────────────────────────────────────────────────────────
    @PostMapping("/vuelo-log")
    @Transactional
    public ResponseEntity<Map<String, Object>> receiveVueloLog(
            @RequestHeader(value = "X-Internal-Secret", required = false) String secret,
            @RequestBody Map<String, Object> body) {

        if (!internalSecret.equals(secret)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized"));
        }

        String eventoStr = (String) body.get("evento");
        String site      = (String) body.get("site");

        TipoEventoVuelo evento;
        try {
            evento = TipoEventoVuelo.valueOf(eventoStr);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "evento inválido: " + eventoStr));
        }

        // ── CAM: cualquier DESPEGUE/ATERRIZAJE → VUELO directo ───────────────
        if ("CAM".equals(site) &&
                (evento == TipoEventoVuelo.DESPEGUE || evento == TipoEventoVuelo.ATERRIZAJE)) {
            evento = TipoEventoVuelo.VUELO;
        }

        VueloLog registro = buildVueloLog(body, evento);
        try {
            vueloLogRepository.save(registro);
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "Evento ya registrado (eventId duplicado)", "eventId", registro.getEventId() != null ? registro.getEventId() : ""));
        }

        // ── EFO: actualizar droneEnDock y ultimoVuelo según el evento ────────
        final TipoEventoVuelo eventoFinal = evento;
        if ("EFO".equals(site) && registro.getNombreDron() != null
                && (eventoFinal == TipoEventoVuelo.DESPEGUE || eventoFinal == TipoEventoVuelo.ATERRIZAJE)) {
            dronRepository.findByNombre(registro.getNombreDron()).ifPresent(d -> {
                d.setDroneEnDock(eventoFinal == TipoEventoVuelo.ATERRIZAJE);
                if (eventoFinal == TipoEventoVuelo.ATERRIZAJE) {
                    d.setUltimoVuelo(Instant.now());
                }
                dronRepository.save(d);
            });
        }

        // ── EFO ATERRIZAJE: crear VUELO combinando con el DESPEGUE previo ────
        if ("EFO".equals(site) && evento == TipoEventoVuelo.ATERRIZAJE) {
            String dronNombre = registro.getNombreDron();
            Optional<VueloLog> pendingDespegue = dronNombre != null
                    ? vueloLogRepository.findLastDespegueByDron(dronNombre)
                    : vueloLogRepository.findLastDespegueBySite(site);

            if (pendingDespegue.isPresent()) {
                VueloLog desp = pendingDespegue.get();
                VueloLog vuelo = new VueloLog();
                vuelo.setEvento(TipoEventoVuelo.VUELO);
                vuelo.setSite(desp.getSite());
                vuelo.setNombreDron(desp.getNombreDron() != null ? desp.getNombreDron() : registro.getNombreDron());
                vuelo.setNombreDock(desp.getNombreDock() != null ? desp.getNombreDock() : registro.getNombreDock());
                vuelo.setPiloto(desp.getPiloto() != null ? desp.getPiloto() : registro.getPiloto());
                vuelo.setDetalleVuelo(desp.getDetalleVuelo() != null ? desp.getDetalleVuelo() : registro.getDetalleVuelo());
                vuelo.setTimestampFlytbase(desp.getTimestampFlytbase());
                vuelo.setBateria(registro.getBateria());
                // Duración: primero usamos la del ATERRIZAJE (calculada por n8n), si no, la calculamos
                if (registro.getDuracionMinutos() != null) {
                    vuelo.setDuracionMinutos(registro.getDuracionMinutos());
                } else if (desp.getTimestampFlytbase() != null && registro.getTimestampFlytbase() != null) {
                    long segundos = registro.getTimestampFlytbase().getEpochSecond()
                            - desp.getTimestampFlytbase().getEpochSecond();
                    vuelo.setDuracionMinutos(Math.max(0, (int) (segundos / 60)));
                }
                vuelo.setDespegueFallido(false);
                vueloLogRepository.save(vuelo);
            }
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "id",     registro.getId(),
                "evento", registro.getEvento().name(),
                "site",   site != null ? site : ""));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /internal/bot/misiones — misiones EFO lanzables (para el bot Telegram)
    // ─────────────────────────────────────────────────────────────────────────
    @GetMapping("/bot/misiones")
    @Transactional(readOnly = true)
    public ResponseEntity<?> botMisiones(
            @RequestHeader(value = "X-Internal-Secret", required = false) String secret) {
        if (!internalSecret.equals(secret))
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));

        List<Map<String, Object>> result = misionRepository.findAllWithDetails().stream()
                .filter(m -> (m.getEstado() == EstadoMision.PLANIFICADA || m.getEstado() == EstadoMision.COMPLETADA)
                        && m.getDron() != null && m.getDron().getYacimiento() == Yacimiento.EFO)
                .map(m -> {
                    Map<String, Object> dto = new java.util.LinkedHashMap<>();
                    dto.put("id",          m.getId());
                    dto.put("nombre",      m.getNombre());
                    dto.put("descripcion", m.getDescripcion());
                    dto.put("estado",      m.getEstado().name());
                    dto.put("dronNombre",  m.getDron().getNombre());
                    Dock dock = m.getDock() != null ? m.getDock()
                            : (m.getDron().getDock() != null ? m.getDron().getDock() : null);
                    dto.put("dockNombre",  dock != null ? dock.getNombre() : null);
                    return dto;
                })
                .collect(java.util.stream.Collectors.toList());
        return ResponseEntity.ok(result);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // POST /internal/bot/misiones/{id}/lanzar — lanza misión desde el bot
    // Body: { "telegramUserId": 123, "telegramNombre": "Juan Pérez" }
    // ─────────────────────────────────────────────────────────────────────────
    @PostMapping("/bot/misiones/{id}/lanzar")
    @Transactional
    public ResponseEntity<Map<String, Object>> botLanzarMision(
            @RequestHeader(value = "X-Internal-Secret", required = false) String secret,
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {

        if (!internalSecret.equals(secret))
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));

        Mision m = misionRepository.findById(id).orElse(null);
        if (m == null) return ResponseEntity.notFound().build();

        if (m.getEstado() != EstadoMision.PLANIFICADA && m.getEstado() != EstadoMision.COMPLETADA)
            return ResponseEntity.badRequest().body(Map.of("error", "La misión debe estar PLANIFICADA o COMPLETADA"));

        Dron dron = m.getDron();
        if (dron == null || dron.getYacimiento() != Yacimiento.EFO)
            return ResponseEntity.badRequest().body(Map.of("error", "Solo misiones EFO"));

        if (m.getWebhookUrl() == null || m.getWebhookUrl().isBlank()
                || m.getWebhookBearer() == null || m.getWebhookBearer().isBlank())
            return ResponseEntity.badRequest().body(Map.of("error", "Webhook FlytBase no configurado"));

        if (vueloLogRepository.hayVueloActivo(dron.getNombre()))
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "El dron '" + dron.getNombre() + "' ya tiene un vuelo activo"));

        // Resolver piloto por telegramUserId o dejar sin piloto
        Long telegramUserId = body.get("telegramUserId") instanceof Number n ? n.longValue() : null;
        String telegramNombre = (String) body.get("telegramNombre");
        Usuario piloto = telegramUserId != null
                ? usuarioRepository.findByTelegramUserId(telegramUserId).orElse(null)
                : null;

        try {
            flytbaseService.lanzarMision(
                    m.getWebhookUrl(), m.getWebhookBearer(), m.getNombre(),
                    m.getDock() != null ? m.getDock().getLatitud() : null,
                    m.getDock() != null ? m.getDock().getLongitud() : null);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(Map.of("error", "No se pudo contactar a FlytBase: " + e.getMessage()));
        }

        m.setPiloto(piloto);
        if (piloto == null && telegramNombre != null) {
            String obs = (m.getObservaciones() != null ? m.getObservaciones() + "\n" : "")
                    + "Lanzado por Telegram: " + telegramNombre;
            m.setObservaciones(obs);
        }
        m.setEstado(EstadoMision.EN_CURSO);
        m.setUltimaEjecucion(LocalDateTime.now());
        m.setFechaInicio(LocalDateTime.now());
        m.setFechaFin(null);
        misionRepository.save(m);

        String pilotoNombre = piloto != null
                ? (piloto.getNombre() + " " + (piloto.getApellido() != null ? piloto.getApellido() : "")).trim()
                : (telegramNombre != null ? telegramNombre + " (no mapeado)" : "sin piloto");

        jdbcTemplate.update(
                "INSERT INTO mision_pendiente (drone_nombre, piloto_nombre, usuario_id, mision_id) VALUES (?, ?, ?, ?)",
                dron.getNombre(), pilotoNombre, piloto != null ? piloto.getId() : null, m.getId());

        return ResponseEntity.ok(Map.of(
                "success",      true,
                "misionId",     m.getId(),
                "misionNombre", m.getNombre(),
                "pilotoNombre", pilotoNombre,
                "mensaje",      "Misión '" + m.getNombre() + "' lanzada exitosamente"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /internal/bot/drones — drones EFO con estado inferido de vuelos_log
    // ─────────────────────────────────────────────────────────────────────────
    @GetMapping("/bot/drones")
    @Transactional(readOnly = true)
    public ResponseEntity<?> botDrones(
            @RequestHeader(value = "X-Internal-Secret", required = false) String secret) {
        if (!internalSecret.equals(secret))
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));

        List<Map<String, Object>> result = dronRepository.findAll().stream()
                .filter(d -> d.getYacimiento() == Yacimiento.EFO)
                .map(d -> {
                    boolean volando = vueloLogRepository.hayVueloActivo(d.getNombre());
                    // Último evento registrado para este dron
                    List<VueloLog> ultimos = vueloLogRepository.findFiltered(
                            d.getNombre(), "EFO", null, null, null);
                    VueloLog ultimoEvento = ultimos.isEmpty() ? null : ultimos.get(0);

                    Map<String, Object> dto = new java.util.LinkedHashMap<>();
                    dto.put("id",            d.getId());
                    dto.put("nombre",        d.getNombre());
                    dto.put("estado",        d.getEstado() != null ? d.getEstado().name() : null);
                    dto.put("cantidadVuelos", d.getCantidadVuelos());
                    dto.put("ultimoVuelo",   d.getUltimoVuelo() != null ? d.getUltimoVuelo().toString() : null);
                    dto.put("volandoAhora",  volando);
                    dto.put("droneEnDock",   d.getDroneEnDock());
                    if (ultimoEvento != null) {
                        Map<String, String> ue = new java.util.LinkedHashMap<>();
                        ue.put("tipo",      ultimoEvento.getEvento().name());
                        ue.put("timestamp", ultimoEvento.getFechaRegistro() != null
                                ? ultimoEvento.getFechaRegistro().toString() : null);
                        dto.put("ultimoEvento", ue);
                    } else {
                        dto.put("ultimoEvento", null);
                    }
                    return dto;
                })
                .collect(java.util.stream.Collectors.toList());
        return ResponseEntity.ok(result);
    }

    private VueloLog buildVueloLog(Map<String, Object> body, TipoEventoVuelo evento) {
        VueloLog v = new VueloLog();
        v.setEvento(evento);
        v.setEventId((String) body.get("eventId"));
        v.setSeveridad((String) body.get("severidad"));
        v.setNombreDron((String) body.get("nombreDron"));
        v.setNombreDock((String) body.get("nombreDock"));
        v.setSite((String) body.get("site"));
        v.setPiloto((String) body.get("piloto"));
        v.setDetalleVuelo((String) body.get("detalleVuelo"));
        if (body.get("bateria") instanceof Number n) v.setBateria(n.intValue());
        if (body.get("duracionMinutos") instanceof Number n) v.setDuracionMinutos(n.intValue());
        if (body.get("despegueFallido") instanceof Boolean b) v.setDespegueFallido(b);
        if (body.get("latitud")  instanceof Number n) v.setLatitud(BigDecimal.valueOf(n.doubleValue()));
        if (body.get("longitud") instanceof Number n) v.setLongitud(BigDecimal.valueOf(n.doubleValue()));
        if (body.get("altitud")  instanceof Number n) v.setAltitud(BigDecimal.valueOf(n.doubleValue()));
        String ts = (String) body.get("timestampFlytbase");
        if (ts != null) {
            try { v.setTimestampFlytbase(Instant.parse(ts)); } catch (Exception ignored) {}
        }
        return v;
    }
}
