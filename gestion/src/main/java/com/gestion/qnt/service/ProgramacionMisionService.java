package com.gestion.qnt.service;

import com.gestion.qnt.model.Alerta;
import com.gestion.qnt.model.Dock;
import com.gestion.qnt.model.Mision;
import com.gestion.qnt.model.ProgramacionMision;
import com.gestion.qnt.model.enums.EstadoMision;
import com.gestion.qnt.model.enums.NivelAlerta;
import com.gestion.qnt.model.enums.PrioridadMision;
import com.gestion.qnt.model.enums.TipoAlerta;
import com.gestion.qnt.model.enums.Yacimiento;
import com.gestion.qnt.model.Dron;
import com.gestion.qnt.repository.AlertaRepository;
import com.gestion.qnt.repository.MisionRepository;
import com.gestion.qnt.repository.ProgramacionMisionRepository;
import com.gestion.qnt.service.FlightHubService;
import com.gestion.qnt.scheduler.TempestPollingJob;
import com.gestion.qnt.service.WeatherEvaluator.Aptitud;
import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Service
public class ProgramacionMisionService {

    private static final Logger log = LoggerFactory.getLogger(ProgramacionMisionService.class);

    @Autowired
    private MisionRepository misionRepository;

    @Autowired
    private ProgramacionMisionRepository programacionRepository;

    @Autowired
    private FlytbaseService flytbaseService;

    @Autowired
    private FlightHubService flightHubService;

    @Autowired
    private TempestPollingJob tempestPollingJob;

    @Autowired
    private TelegramNotificationService telegramNotificationService;

    @Autowired
    private AlertaRepository alertaRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Value("${weather.gate.enabled:false}")
    private boolean weatherGateEnabled;

    public LocalDateTime calcularProxEjecucion(ProgramacionMision p) {
        if (p.getTipoRecurrencia() == null || p.getHora() == null) return null;
        LocalDateTime ahora = LocalDateTime.now();
        LocalDate hoy = ahora.toLocalDate();
        LocalTime hora = p.getHora();

        LocalDateTime resultado = null;
        switch (p.getTipoRecurrencia()) {
            case DIARIA: {
                int n = p.getIntervaloDias() != null ? p.getIntervaloDias() : 1;
                LocalDateTime c = LocalDateTime.of(hoy, hora);
                while (!c.isAfter(ahora)) c = c.plusDays(n);
                resultado = c;
                break;
            }
            case SEMANAL: {
                if (p.getDiasSemana() == null || p.getDiasSemana().isEmpty()) return null;
                LocalDate c = hoy;
                for (int i = 0; i < 7; i++) {
                    if (p.getDiasSemana().contains(c.getDayOfWeek())) {
                        LocalDateTime dt = LocalDateTime.of(c, hora);
                        if (dt.isAfter(ahora)) { resultado = dt; break; }
                    }
                    c = c.plusDays(1);
                }
                if (resultado == null) return null;
                break;
            }
            case MENSUAL: {
                if (p.getDiaMes() == null) return null;
                int dia = p.getDiaMes();
                LocalDate c = hoy.withDayOfMonth(Math.min(dia, hoy.lengthOfMonth()));
                LocalDateTime dt = LocalDateTime.of(c, hora);
                if (!dt.isAfter(ahora)) {
                    LocalDate nm = hoy.plusMonths(1);
                    c = nm.withDayOfMonth(Math.min(dia, nm.lengthOfMonth()));
                    dt = LocalDateTime.of(c, hora);
                }
                resultado = dt;
                break;
            }
            default:
                return null;
        }

        // Clamp dentro de vigencia
        if (p.getFechaInicioVigencia() != null && resultado.toLocalDate().isBefore(p.getFechaInicioVigencia())) {
            resultado = p.getFechaInicioVigencia().atTime(hora);
        }
        if (p.getFechaFinVigencia() != null && resultado.toLocalDate().isAfter(p.getFechaFinVigencia())) {
            return null;
        }
        return resultado;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void generarYLanzar(ProgramacionMision p) {
        // Reload in new tx so lazy associations (misionPlantilla and its dron/dock/piloto) are accessible.
        Long progId = p.getId();
        ProgramacionMision prog = programacionRepository.findById(progId)
                .orElseThrow(() -> new RuntimeException("Programación no encontrada: " + progId));

        Mision src = prog.getMisionPlantilla(); // lazy-loaded safely within this tx

        Mision m = new Mision();
        m.setNombre(src != null ? src.getNombre() : prog.getNombre());
        m.setDescripcion(src != null ? src.getDescripcion() : prog.getDescripcion());
        m.setCategoria(src != null ? src.getCategoria() : prog.getCategoria());
        m.setPrioridad(src != null
                ? (src.getPrioridad() != null ? src.getPrioridad() : PrioridadMision.MEDIA)
                : (prog.getPrioridad() != null ? prog.getPrioridad() : PrioridadMision.MEDIA));
        m.setDron(src != null ? src.getDron() : prog.getDron());
        Dock dock = src != null
                ? (src.getDock() != null ? src.getDock() : (src.getDron() != null ? src.getDron().getDock() : null))
                : (prog.getDock() != null ? prog.getDock() : (prog.getDron() != null ? prog.getDron().getDock() : null));
        m.setDock(dock);
        m.setPiloto(src != null ? src.getPiloto() : prog.getPiloto());
        String webhookUrl    = src != null ? src.getWebhookUrl()    : prog.getWebhookUrl();
        String webhookBearer = src != null ? src.getWebhookBearer() : prog.getWebhookBearer();
        m.setWebhookUrl(webhookUrl);
        m.setWebhookBearer(webhookBearer);
        m.setEstado(EstadoMision.PLANIFICADA);
        m.setFechaProgramada(prog.getProxEjecucion());
        m.setProgramacion(prog);
        Mision saved = misionRepository.save(m);

        String dronNombre = m.getDron() != null ? m.getDron().getNombre() : "desconocido";
        String pilotoNombre = m.getPiloto() != null
                ? (m.getPiloto().getNombre() + " " + (m.getPiloto().getApellido() != null ? m.getPiloto().getApellido() : "")).trim()
                : "sistema";
        Long pilotoId = m.getPiloto() != null ? m.getPiloto().getId() : null;
        jdbcTemplate.update(
                "INSERT INTO mision_pendiente (drone_nombre, piloto_nombre, usuario_id, mision_id) VALUES (?, ?, ?, ?)",
                dronNombre, pilotoNombre, pilotoId, saved.getId()
        );

        // ── Weather gate (solo Cañadón León) ────────────────────────────
        boolean esCL = weatherGateEnabled && esSitioConTempest(dock, saved.getDron());
        Aptitud aptitud = esCL ? tempestPollingJob.getAptitudActual() : null;
        if (aptitud == Aptitud.NO_VOLAR || aptitud == Aptitud.PRECAUCION) {
            saved.setEstado(EstadoMision.CANCELADA);
            misionRepository.save(saved);
            WeatherEvaluator.Evaluacion eval = tempestPollingJob.getEvaluacionActual();
            String detalle = eval != null ? String.join(", ", eval.razones()) : aptitud.name();
            String emoji = aptitud == Aptitud.NO_VOLAR ? "🔴" : "🟡";
            String estado = aptitud == Aptitud.NO_VOLAR ? "NO VOLAR" : "PRECAUCIÓN";
            log.warn("Programación {}: misión {} cancelada por MAL_TIEMPO ({})", progId, saved.getId(), estado);
            String msg = emoji + " Misión cronogramada <b>'" + saved.getNombre() + "'</b> cancelada automáticamente.\n" +
                    "<i>Estado climático: " + estado + "</i>\n" + detalle;
            telegramNotificationService.notifyAll(msg);
            Alerta a = new Alerta(TipoAlerta.MAL_TIEMPO, NivelAlerta.CRITICA,
                    "Misión cancelada por mal tiempo (" + estado + ")", "Drone: " + dronNombre, "MISION", saved.getId());
            a.setClaveDedup("MAL_TIEMPO_MISION_" + saved.getId());
            alertaRepository.save(a);
            return;
        }
        // ────────────────────────────────────────────────────────────────

        boolean esCam = m.getDron() != null && m.getDron().getYacimiento() == Yacimiento.CAM;

        if (esCam) {
            // CAM → DJI FlightHub 2
            String waylineUuid = src != null ? src.getFlightHubWaylineUuid() : null;
            if (waylineUuid != null && !waylineUuid.isBlank()) {
                try {
                    flightHubService.lanzarMision(saved.getNombre(), waylineUuid);
                    saved.setEstado(EstadoMision.EN_CURSO);
                    saved.setFechaInicio(LocalDateTime.now());
                    saved.setUltimaEjecucion(LocalDateTime.now());
                    misionRepository.save(saved);
                    log.info("Programación {} lanzada en FlightHub para drone CAM {}", progId, dronNombre);
                } catch (Exception e) {
                    marcarFalloLanzamiento(saved, dronNombre, progId, "FlightHub: " + e.getMessage());
                }
            } else {
                marcarFalloLanzamiento(saved, dronNombre, progId, "waylineUuid no configurado");
            }
        } else {
            // EFO → FlytBase
            if (webhookUrl != null && !webhookUrl.isBlank() && webhookBearer != null && !webhookBearer.isBlank()) {
                try {
                    flytbaseService.lanzarMision(
                            webhookUrl, webhookBearer, m.getNombre(),
                            dock != null ? dock.getLatitud() : null,
                            dock != null ? dock.getLongitud() : null
                    );
                    saved.setEstado(EstadoMision.EN_CURSO);
                    saved.setFechaInicio(LocalDateTime.now());
                    saved.setUltimaEjecucion(LocalDateTime.now());
                    misionRepository.save(saved);
                    log.info("Programación {} lanzada en FlytBase para drone EFO {}", progId, dronNombre);
                } catch (Exception e) {
                    marcarFalloLanzamiento(saved, dronNombre, progId, "FlytBase webhook error: " + e.getMessage());
                }
            } else {
                marcarFalloLanzamiento(saved, dronNombre, progId, "webhook FlytBase no configurado");
            }
        }
    }

    private void marcarFalloLanzamiento(Mision saved, String dronNombre, Long progId, String motivo) {
        saved.setEstado(EstadoMision.FALLO_LANZAMIENTO);
        misionRepository.save(saved);
        jdbcTemplate.update(
                "UPDATE mision_pendiente SET procesado = true WHERE drone_nombre = ? AND procesado = false",
                dronNombre);
        String msg = "⚠️ Misión cronogramada <b>'" + saved.getNombre() + "'</b> no se pudo lanzar.\n" +
                     "<i>Motivo: " + motivo + "</i>";
        telegramNotificationService.notifyAll(msg);
        log.warn("Programación {}: misión {} con FALLO_LANZAMIENTO — {}", progId, saved.getId(), motivo);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void actualizarProxEjecucion(ProgramacionMision p) {
        p.setUltimaEjecucion(LocalDateTime.now());
        p.setProxEjecucion(calcularProxEjecucion(p));
        programacionRepository.save(p);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void desactivar(ProgramacionMision p) {
        p.setActiva(false);
        programacionRepository.save(p);
    }

    private boolean esSitioConTempest(Dock dock, Dron dron) {
        if (dock != null) {
            if (dock.getYacimiento() == Yacimiento.CANADON_LEON) return true;
            if (dock.getSite() != null && "CL".equals(dock.getSite().getCodigo())) return true;
        }
        if (dron != null && dron.getYacimiento() == Yacimiento.CANADON_LEON) return true;
        return false;
    }
}
