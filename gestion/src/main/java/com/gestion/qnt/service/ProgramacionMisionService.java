package com.gestion.qnt.service;

import com.gestion.qnt.model.Dock;
import com.gestion.qnt.model.Mision;
import com.gestion.qnt.model.ProgramacionMision;
import com.gestion.qnt.model.enums.EstadoMision;
import com.gestion.qnt.model.enums.PrioridadMision;
import com.gestion.qnt.repository.MisionRepository;
import com.gestion.qnt.repository.ProgramacionMisionRepository;
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
    private JdbcTemplate jdbcTemplate;

    public LocalDateTime calcularProxEjecucion(ProgramacionMision p) {
        if (p.getTipoRecurrencia() == null || p.getHora() == null) return null;
        LocalDateTime ahora = LocalDateTime.now();
        LocalDate hoy = ahora.toLocalDate();
        LocalTime hora = p.getHora();

        switch (p.getTipoRecurrencia()) {
            case DIARIA: {
                int n = p.getIntervaloDias() != null ? p.getIntervaloDias() : 1;
                LocalDateTime c = LocalDateTime.of(hoy, hora);
                while (!c.isAfter(ahora)) c = c.plusDays(n);
                return c;
            }
            case SEMANAL: {
                if (p.getDiasSemana() == null || p.getDiasSemana().isEmpty()) return null;
                LocalDate c = hoy;
                for (int i = 0; i < 7; i++) {
                    if (p.getDiasSemana().contains(c.getDayOfWeek())) {
                        LocalDateTime dt = LocalDateTime.of(c, hora);
                        if (dt.isAfter(ahora)) return dt;
                    }
                    c = c.plusDays(1);
                }
                return null;
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
                return dt;
            }
            default:
                return null;
        }
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
                log.info("Programación {} lanzada exitosamente para drone {}", progId, dronNombre);
            } catch (Exception e) {
                log.error("Error llamando FlytBase para programación {}: {}", progId, e.getMessage());
            }
        } else {
            log.warn("Programación {} sin webhook — misión {} creada en PLANIFICADA sin lanzar", progId, saved.getId());
        }
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
}
