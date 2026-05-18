package com.gestion.qnt.scheduler;

import com.gestion.qnt.model.Mision;
import com.gestion.qnt.model.ProgramacionMision;
import com.gestion.qnt.model.enums.EstadoMision;
import com.gestion.qnt.model.enums.Yacimiento;
import com.gestion.qnt.repository.MisionRepository;
import com.gestion.qnt.repository.ProgramacionMisionRepository;
import com.gestion.qnt.repository.VueloLogRepository;
import com.gestion.qnt.service.ProgramacionMisionService;
import com.gestion.qnt.service.TelegramNotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class MisionSchedulerJob {

    private static final Logger log = LoggerFactory.getLogger(MisionSchedulerJob.class);

    private static final int HORAS_TIMEOUT_MISION = 6;

    @Autowired
    private ProgramacionMisionService service;

    @Autowired
    private ProgramacionMisionRepository repo;

    @Autowired
    private MisionRepository misionRepository;

    @Autowired
    private VueloLogRepository vueloLogRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TelegramNotificationService telegramNotificationService;

    // Sin @Transactional aquí: cada llamada al service abre su propia tx (REQUIRES_NEW).
    // Así un fallo en una programación no afecta a las demás ni deja la tx outer en rollback-only.
    @Scheduled(fixedRate = 60000)
    public void ejecutar() {
        List<ProgramacionMision> pendientes =
                repo.findByActivaTrueAndProxEjecucionBefore(LocalDateTime.now().plusSeconds(30));
        if (pendientes.isEmpty()) return;

        log.info("Scheduler: {} programaciones listas para ejecutar", pendientes.size());


        for (ProgramacionMision p : pendientes) {
            LocalDate hoy = LocalDate.now();

            if (p.getFechaFinVigencia() != null && hoy.isAfter(p.getFechaFinVigencia())) {
                try { service.desactivar(p); } catch (Exception e) { log.error("Error desactivando {}: {}", p.getId(), e.getMessage()); }
                log.info("Programación {} desactivada por fin de vigencia", p.getId());
                continue;
            }

            if (p.getFechaInicioVigencia() != null && hoy.isBefore(p.getFechaInicioVigencia())) {
                continue;
            }

            try {
                service.generarYLanzar(p);
            } catch (Exception e) {
                log.error("Error ejecutando programación {}: {}", p.getId(), e.getMessage());
            }

            try {
                service.actualizarProxEjecucion(p);
            } catch (Exception e) {
                log.error("Error actualizando proxEjecucion para {}: {}", p.getId(), e.getMessage());
            }
        }
    }

    /**
     * Cada 3 minutos: detecta misiones EFO que se marcaron EN_CURSO pero no registraron
     * un DESPEGUE real en vuelos_log dentro de los 5 minutos posteriores al lanzamiento.
     * Si no hay DESPEGUE → FlytBase no lanzó efectivamente → FALLO_LANZAMIENTO + Telegram.
     */
    @Scheduled(fixedDelay = 3 * 60 * 1000)
    @Transactional
    public void verificarDespeguesEFO() {
        LocalDateTime ahora  = LocalDateTime.now();
        LocalDateTime hace5  = ahora.minusMinutes(5);
        LocalDateTime hace30 = ahora.minusMinutes(30);

        misionRepository.findByEstadoWithDetails(EstadoMision.EN_CURSO).stream()
                .filter(m -> m.getFechaInicio() != null
                          && m.getFechaInicio().isAfter(hace30)
                          && m.getFechaInicio().isBefore(hace5)
                          && m.getDron() != null
                          && m.getDron().getYacimiento() == Yacimiento.EFO)
                .forEach(m -> {
                    String dronNombre = m.getDron().getNombre();
                    boolean despego = vueloLogRepository.existsDespegueAfter(dronNombre, m.getFechaInicio());
                    if (!despego) {
                        m.setEstado(EstadoMision.FALLO_LANZAMIENTO);
                        misionRepository.save(m);
                        jdbcTemplate.update(
                                "UPDATE mision_pendiente SET procesado = true WHERE drone_nombre = ? AND procesado = false",
                                dronNombre);
                        String msg = "⚠️ Misión <b>'" + m.getNombre() + "'</b> lanzada pero sin despegue confirmado.\n" +
                                     "<i>Drone " + dronNombre + " no registró despegue en 5 minutos.</i>";
                        telegramNotificationService.notifyAll(msg);
                        log.warn("Watchdog EFO: misión {} '{}' sin DESPEGUE → FALLO_LANZAMIENTO", m.getId(), m.getNombre());
                    }
                });
    }

    /** Cada hora: cierra misiones que llevan más de HORAS_TIMEOUT_MISION horas EN_CURSO. */
    @Scheduled(fixedRate = 3600000)
    @Transactional
    public void cerrarMisionesAtascadas() {
        LocalDateTime corte = LocalDateTime.now().minusHours(HORAS_TIMEOUT_MISION);
        List<Mision> stuck = misionRepository.findHistorial().stream()
                .filter(m -> m.getEstado() == EstadoMision.EN_CURSO)
                .filter(m -> m.getFechaInicio() != null && m.getFechaInicio().isBefore(corte))
                .toList();
        if (stuck.isEmpty()) return;
        log.warn("Auto-cierre: {} misiones EN_CURSO llevan más de {} horas sin completarse", stuck.size(), HORAS_TIMEOUT_MISION);
        for (Mision m : stuck) {
            m.setEstado(EstadoMision.COMPLETADA);
            misionRepository.save(m);
            log.warn("  → Misión {} '{}' cerrada automáticamente (inicio: {})", m.getId(), m.getNombre(), m.getFechaInicio());
        }
    }
}
