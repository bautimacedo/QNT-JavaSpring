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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
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

    // Proyectos FlightHub a sincronizar. Cada uno mapea a un site distinto.
    @Value("${flighthub.project-uuid:${FLIGHTHUB_PROJECT_UUID:}}")
    private String camProjectUuid;
    @Value("${flighthub.sn:${FLIGHTHUB_SN:}}")
    private String camSn;
    @Value("${flighthub.cl-project-uuid:${FLIGHTHUB_CL_PROJECT_UUID:}}")
    private String clProjectUuid;
    @Value("${flighthub.cl-sn:${FLIGHTHUB_CL_SN:}}")
    private String clSn;

    /** (projectUuid, snFiltro, site) — snFiltro null trae todas las tareas del proyecto. */
    private record FhProyecto(String uuid, String sn, String site) {}

    // Estados FlightHub a registrar: 2=completada, 3/4/5=falla.
    // El estado 1 (en ejecución) NO se registra acá: lo usa isDroneFlying() (weather gate) con otro propósito.
    private static final int[] ESTADOS_A_REGISTRAR = {2, 3, 4, 5};

    @Scheduled(fixedDelay = 10 * 60 * 1000)
    @Transactional
    public void sincronizar() {
        long now     = Instant.now().getEpochSecond();
        long beginAt = now - 7L * 24 * 60 * 60; // últimos 7 días (dedup por event_id hace barato re-consultar)

        List<FhProyecto> proyectos = new ArrayList<>();
        if (camProjectUuid != null && !camProjectUuid.isBlank())
            proyectos.add(new FhProyecto(camProjectUuid, camSn, "CAM"));
        if (clProjectUuid != null && !clProjectUuid.isBlank())
            proyectos.add(new FhProyecto(clProjectUuid, clSn, "CL")); // FlightHub exige sn[] para devolver tareas

        for (FhProyecto p : proyectos) {
            int insertados = 0, actualizados = 0, omitidos = 0;
            for (int estado : ESTADOS_A_REGISTRAR) {
                List<Map<String, Object>> tasks;
                try {
                    tasks = flightHubService.listarTareasV2(p.uuid(), p.sn(), beginAt, now, estado);
                } catch (Exception e) {
                    log.warn("FlightHubVueloLogJob[{}]: no se pudo consultar FlightHub (status={}): {}",
                            p.site(), estado, e.getMessage());
                    continue;
                }
                for (Map<String, Object> t : tasks) {
                    try {
                        switch (procesarTarea(t, p.site(), estado)) {
                            case INSERTADO   -> insertados++;
                            case ACTUALIZADO -> actualizados++;
                            case OMITIDO     -> omitidos++;
                        }
                    } catch (Exception e) {
                        log.error("FlightHubVueloLogJob[{}]: error procesando tarea {}: {}",
                                p.site(), t.get("flight_task_id"), e.getMessage());
                    }
                }
            }
            log.info("FlightHubVueloLogJob[{}]: insertados={}, actualizados={}, omitidos={}",
                    p.site(), insertados, actualizados, omitidos);
        }
    }

    private enum Resultado { INSERTADO, ACTUALIZADO, OMITIDO }

    /**
     * Registra una tarea finalizada. El tipo de evento se decide por {@code flightTaskStatus}
     * (el estado consultado: 2=completada, 3/4/5=falla), NO por el campo interno {@code task_status}
     * (que puede valer 5 en un vuelo exitoso).
     * Si el vuelo ya existe pero sin duración (ej. lo había cargado n8n a medias), la completa.
     */
    private Resultado procesarTarea(Map<String, Object> t, String site, int flightTaskStatus) {
        String eventId = str(t.get("flight_task_id") != null ? t.get("flight_task_id") : t.get("uuid"));
        if (eventId == null || eventId.isBlank()) return Resultado.OMITIDO;

        boolean isFailed = FALLA_MAP.containsKey(flightTaskStatus);
        Object endTime   = t.get("task_end_time");
        Integer duracion = isFailed ? null : calcularDuracion(t.get("task_start_time"), endTime);

        // ¿Ya existe? Completar la duración si la tenemos y le falta; si no, omitir.
        var existente = vueloLogRepository.findByEventId(eventId);
        if (existente.isPresent()) {
            VueloLog v = existente.get();
            if (duracion != null && v.getDuracionMinutos() == null) {
                v.setDuracionMinutos(duracion);
                vueloLogRepository.save(v);
                return Resultado.ACTUALIZADO;
            }
            return Resultado.OMITIDO;
        }

        String droneSn  = str(t.get("drone_sn")); // puede venir vacío en v2
        String dockSn   = str(t.get("take_off_airport_sn") != null ? t.get("take_off_airport_sn")
                         : (t.get("sn") != null ? t.get("sn") : t.get("connect_device_sn")));

        Dron dron = resolverDron(droneSn, dockSn);
        Dock dock = resolverDock(dockSn);

        String piloto = resolverPiloto(dron, str(t.get("user_name")));
        String detalle = str(t.get("task_name") != null ? t.get("task_name") : t.get("name"));

        VueloLog v = new VueloLog();
        v.setSite(site);
        v.setNombreDron(dron != null ? dron.getNombre() : null);
        v.setNombreDock(dock != null ? dock.getNombre() : dockSn);
        v.setPiloto(piloto);
        v.setDetalleVuelo(detalle);
        v.setDespegueFallido(isFailed);
        v.setEventId(eventId);
        if (dron != null && dron.getBateriaPorc() != null)
            v.setBateria(dron.getBateriaPorc());

        if (isFailed) {
            v.setEvento(FALLA_MAP.get(flightTaskStatus));
            v.setTimestampFlytbase(toInstant(endTime != null ? endTime : t.get("task_start_time")));
        } else {
            // Completada (flight_task_status=2): VUELO con duración real (end − start).
            v.setEvento(TipoEventoVuelo.VUELO);
            v.setTimestampFlytbase(toInstant(endTime));
            v.setDuracionMinutos(duracion);
        }

        vueloLogRepository.save(v);

        // Stats de dron y horas del piloto solo para vuelos completados con duración.
        if (!isFailed && v.getDuracionMinutos() != null && v.getDuracionMinutos() > 0) {
            actualizarStatsDron(dron, v.getDuracionMinutos());
            actualizarHorasPiloto(dron != null ? dron.getNombre() : null, v.getDuracionMinutos());
        }
        return Resultado.INSERTADO;
    }

    /** Duración en minutos entre dos timestamps FlightHub (ISO o epoch), o null si no se puede. */
    private static Integer calcularDuracion(Object start, Object end) {
        Instant ini = toInstant(start);
        Instant fin = toInstant(end);
        if (ini == null || fin == null) return null;
        long durSeg = fin.getEpochSecond() - ini.getEpochSecond();
        return durSeg > 0 ? (int) (durSeg / 60) : null;
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

    private static Instant toInstant(Object v) {
        if (v == null) return null;
        String s = v.toString().trim();
        if (s.isEmpty()) return null;
        try { return Instant.parse(s); } catch (Exception ignored) {}
        try { return Instant.ofEpochSecond(Long.parseLong(s)); } catch (Exception ignored) {}
        return null;
    }
}
