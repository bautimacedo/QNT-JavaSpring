package com.gestion.qnt.scheduler;

import com.gestion.qnt.model.Dock;
import com.gestion.qnt.model.Dron;
import com.gestion.qnt.model.Usuario;
import com.gestion.qnt.model.VueloLog;
import com.gestion.qnt.model.enums.TipoEventoVuelo;
import com.gestion.qnt.repository.DockRepository;
import com.gestion.qnt.repository.DronRepository;
import com.gestion.qnt.repository.UsuarioRepository;
import com.gestion.qnt.repository.VueloLogRepository;
import com.gestion.qnt.service.FlightHubService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Reemplaza el workflow n8n "GET FlightHub Tasks → INSERT vuelos_log".
 * Corre cada 15 minutos, consulta tareas de los últimos 60 min y las persiste.
 */
@Component
public class FlightHubVueloLogJob {

    private static final Logger log = LoggerFactory.getLogger(FlightHubVueloLogJob.class);

    private static final Map<Integer, TipoEventoVuelo> FALLA_MAP = Map.of(
            3, TipoEventoVuelo.FALLA_DESPEGUE,
            4, TipoEventoVuelo.DESPEGUE_FALLIDO,
            5, TipoEventoVuelo.DESPEGUE_FALLIDO
    );

    @Autowired private FlightHubService flightHubService;
    @Autowired private VueloLogRepository vueloLogRepository;
    @Autowired private DronRepository dronRepository;
    @Autowired private DockRepository dockRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    @Scheduled(fixedDelay = 3 * 60 * 1000)
    @Transactional
    public void sincronizar() {
        long now     = Instant.now().getEpochSecond();
        long beginAt = now - 60 * 60; // últimos 60 minutos

        List<Map<String, Object>> tasks;
        try {
            tasks = flightHubService.listarTareasV2(beginAt, now);
        } catch (Exception e) {
            log.warn("FlightHubVueloLogJob: no se pudo consultar FlightHub: {}", e.getMessage());
            return;
        }

        log.info("FlightHubVueloLogJob: {} tareas recibidas de FlightHub", tasks.size());
        if (tasks.isEmpty()) return;

        int insertados = 0, omitidos = 0;
        for (Map<String, Object> t : tasks) {
            try {
                procesarTarea(t);
                insertados++;
            } catch (DataIntegrityViolationException e) {
                omitidos++;
            } catch (Exception e) {
                log.error("FlightHubVueloLogJob: error procesando tarea {}: {}", t.get("flight_task_id"), e.getMessage());
            }
        }
        log.info("FlightHubVueloLogJob: insertados={}, omitidos={}", insertados, omitidos);
    }

    private void procesarTarea(Map<String, Object> t) {
        String eventId  = str(t.get("flight_task_id") != null ? t.get("flight_task_id") : t.get("uuid"));
        String droneSn  = str(t.get("drone_sn")); // puede venir vacío en v2
        String dockSn   = str(t.get("take_off_airport_sn") != null ? t.get("take_off_airport_sn")
                         : (t.get("sn") != null ? t.get("sn") : t.get("connect_device_sn")));
        int    status   = toInt(t.get("task_status"));
        Object endTime  = t.get("task_end_time");
        boolean isFailed = FALLA_MAP.containsKey(status);
        boolean isDone   = endTime != null;

        Dron dron = resolverDron(droneSn, dockSn);
        Dock dock = resolverDock(dockSn);

        String piloto = resolverPiloto(dron, str(t.get("user_name")));
        String detalle = str(t.get("task_name") != null ? t.get("task_name") : t.get("name"));

        VueloLog v = new VueloLog();
        v.setSite("CAM");
        v.setNombreDron(dron != null ? dron.getNombre() : null);
        v.setNombreDock(dock != null ? dock.getNombre() : dockSn);
        v.setPiloto(piloto);
        v.setDetalleVuelo(detalle);
        v.setDespegueFallido(isFailed);
        if (dron != null && dron.getBateriaPorc() != null)
            v.setBateria(dron.getBateriaPorc());

        if (isFailed) {
            v.setEvento(FALLA_MAP.get(status));
            v.setEventId(eventId);
            v.setTimestampFlytbase(toInstant(endTime != null ? endTime : t.get("task_start_time")));
        } else if (isDone) {
            v.setEvento(TipoEventoVuelo.VUELO);
            v.setEventId(eventId);
            v.setTimestampFlytbase(toInstant(endTime));

            // Duración: usar Instant para soportar tanto ISO strings como epoch seconds
            Instant end   = toInstant(endTime);
            Instant start = toInstant(t.get("task_start_time"));
            if (end != null && start != null) {
                long durSeg = end.getEpochSecond() - start.getEpochSecond();
                if (durSeg > 0) v.setDuracionMinutos((int) (durSeg / 60));
            }
        } else {
            // En curso
            v.setEvento(TipoEventoVuelo.VUELO);
            v.setEventId(eventId + "_DESPEGUE");
            v.setTimestampFlytbase(toInstant(t.get("task_start_time") != null
                    ? t.get("task_start_time") : t.get("run_at")));
        }

        vueloLogRepository.save(v);

        // Actualizar stats de dron y horas del piloto solo para vuelos completados (no fallas, no en curso)
        if (isDone && !isFailed && v.getDuracionMinutos() != null && v.getDuracionMinutos() > 0) {
            actualizarStatsDron(dron, v.getDuracionMinutos());
            actualizarHorasPiloto(dron != null ? dron.getNombre() : null, v.getDuracionMinutos());
        }
    }

    private void actualizarStatsDron(Dron dron, int duracionMinutos) {
        if (dron == null) return;
        dron.setCantidadVuelos((dron.getCantidadVuelos() != null ? dron.getCantidadVuelos() : 0) + 1);
        dron.setCantidadMinutosVolados((dron.getCantidadMinutosVolados() != null ? dron.getCantidadMinutosVolados() : 0) + duracionMinutos);
        dronRepository.save(dron);
    }

    private void actualizarHorasPiloto(String dronNombre, int duracionMinutos) {
        if (dronNombre == null || dronNombre.isBlank()) return;
        List<Long> usuarioIds = jdbcTemplate.queryForList(
                "SELECT usuario_id FROM mision_pendiente WHERE drone_nombre = ? AND procesado = false AND usuario_id IS NOT NULL LIMIT 1",
                Long.class, dronNombre);
        if (usuarioIds.isEmpty() || usuarioIds.get(0) == null) return;
        usuarioRepository.findById(usuarioIds.get(0)).ifPresent(u -> {
            double horas = (u.getHorasVuelo() != null ? u.getHorasVuelo() : 0.0) + duracionMinutos / 60.0;
            u.setHorasVuelo(horas);
            usuarioRepository.save(u);
            log.info("FlightHubVueloLogJob: horas actualizadas para piloto {} → {:.2f}h", u.getNombre(), horas);
        });
        // Marcar como procesado
        jdbcTemplate.update(
                "UPDATE mision_pendiente SET procesado = true WHERE drone_nombre = ? AND procesado = false",
                dronNombre);
    }

    private Dron resolverDron(String droneSn, String dockSn) {
        if (droneSn != null && !droneSn.isBlank())
            return dronRepository.findByNumeroSerie(droneSn).orElse(null);
        if (dockSn != null && !dockSn.isBlank())
            return dockRepository.findByNumeroSerie(dockSn)
                    .map(d -> dronRepository.findByDock_Id(d.getId()).orElse(null))
                    .orElse(null);
        return null;
    }

    private Dock resolverDock(String dockSn) {
        if (dockSn == null || dockSn.isBlank()) return null;
        return dockRepository.findByNumeroSerie(dockSn).orElse(null);
    }

    private String resolverPiloto(Dron dron, String fallback) {
        if (dron == null) return fallback;
        List<String> pilotos = jdbcTemplate.queryForList(
                "SELECT piloto_nombre FROM mision_pendiente WHERE drone_nombre = ? AND procesado = false LIMIT 1",
                String.class, dron.getNombre());
        return (!pilotos.isEmpty() && pilotos.get(0) != null) ? pilotos.get(0) : fallback;
    }

    private static String str(Object v) {
        return v != null ? v.toString().trim() : null;
    }

    private static int toInt(Object v) {
        return v instanceof Number n ? n.intValue() : 0;
    }

    private static Instant toInstant(Object v) {
        if (v == null) return null;
        String s = v.toString().trim();
        if (s.isEmpty()) return null;
        try { return Instant.parse(s); } catch (Exception ignored) {}
        try { return Instant.ofEpochSecond(Long.parseLong(s)); } catch (Exception ignored) {}
        return null;
    }
}
