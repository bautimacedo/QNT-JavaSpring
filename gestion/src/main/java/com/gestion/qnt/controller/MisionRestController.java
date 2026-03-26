package com.gestion.qnt.controller;

import com.gestion.qnt.config.ApiConstants;
import com.gestion.qnt.controller.dto.MisionDTO;
import com.gestion.qnt.controller.dto.MisionRequest;
import com.gestion.qnt.model.*;
import com.gestion.qnt.model.business.exceptions.BusinessException;
import com.gestion.qnt.model.business.exceptions.NotFoundException;
import com.gestion.qnt.model.business.interfaces.IMisionBusiness;
import com.gestion.qnt.model.enums.EstadoMision;
import com.gestion.qnt.repository.DockRepository;
import com.gestion.qnt.repository.DronRepository;
import com.gestion.qnt.repository.LogRepository;
import com.gestion.qnt.repository.MisionRepository;
import com.gestion.qnt.repository.PozoRepository;
import com.gestion.qnt.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping(ApiConstants.URL_BASE + "/misiones")
public class MisionRestController {

    @Autowired
    private IMisionBusiness misionBusiness;

    @Autowired
    private MisionRepository misionRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private DronRepository dronRepository;

    @Autowired
    private DockRepository dockRepository;

    @Autowired
    private PozoRepository pozoRepository;

    @Autowired
    private LogRepository logRepository;

    // ─────────────────────────────────────────────
    // GET /misiones — lista con detalles
    // ─────────────────────────────────────────────
    @GetMapping("")
    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<MisionDTO>> list() {
        try {
            List<Mision> misiones = misionRepository.findAllWithDetails();
            return ResponseEntity.ok(misiones.stream().map(this::toDTO).collect(Collectors.toList()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ─────────────────────────────────────────────
    // GET /misiones/{id}
    // ─────────────────────────────────────────────
    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MisionDTO> load(@PathVariable Long id) {
        try {
            Mision m = misionBusiness.load(id);
            return ResponseEntity.ok(toDTO(m));
        } catch (NotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (BusinessException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ─────────────────────────────────────────────
    // GET /misiones?estado=PLANIFICADA
    // ─────────────────────────────────────────────
    @GetMapping(params = "estado")
    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<MisionDTO>> listByEstado(@RequestParam EstadoMision estado) {
        try {
            List<Mision> misiones = misionRepository.findByEstadoWithDetails(estado);
            return ResponseEntity.ok(misiones.stream().map(this::toDTO).collect(Collectors.toList()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ─────────────────────────────────────────────
    // POST /misiones
    // ─────────────────────────────────────────────
    @PostMapping("")
    @Transactional
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<MisionDTO> add(@RequestBody MisionRequest req) {
        try {
            Mision m = fromRequest(req, new Mision());
            Mision saved = misionBusiness.add(m);
            return ResponseEntity.status(HttpStatus.CREATED).body(toDTO(saved));
        } catch (NotFoundException e) {
            return ResponseEntity.badRequest().build();
        } catch (BusinessException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ─────────────────────────────────────────────
    // PUT /misiones/{id}
    // ─────────────────────────────────────────────
    @PutMapping("/{id}")
    @Transactional
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<MisionDTO> update(@PathVariable Long id, @RequestBody MisionRequest req) {
        try {
            Mision existing = misionBusiness.load(id);
            Mision m = fromRequest(req, existing);
            m.setId(id);
            Mision updated = misionBusiness.update(m);
            return ResponseEntity.ok(toDTO(updated));
        } catch (NotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (BusinessException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ─────────────────────────────────────────────
    // PATCH /misiones/{id}/estado — cambio de estado rápido
    // ─────────────────────────────────────────────
    @PatchMapping("/{id}/estado")
    @Transactional
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<MisionDTO> cambiarEstado(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        try {
            EstadoMision nuevoEstado = EstadoMision.valueOf(body.get("estado"));
            Mision m = misionBusiness.load(id);
            EstadoMision estadoAnterior = m.getEstado();
            LocalDateTime ahora = LocalDateTime.now();

            m.setEstado(nuevoEstado);

            if (nuevoEstado == EstadoMision.EN_CURSO) {
                if (m.getUltimaEjecucion() == null) m.setUltimaEjecucion(ahora);
                if (m.getFechaInicio() == null) m.setFechaInicio(ahora);
            }

            if (nuevoEstado == EstadoMision.COMPLETADA) {
                m.setFechaFin(ahora);

                // Calcular duración en minutos
                LocalDateTime inicio = m.getFechaInicio() != null ? m.getFechaInicio() : m.getUltimaEjecucion();
                if (inicio != null) {
                    long minutos = ChronoUnit.MINUTES.between(inicio, ahora);

                    // Actualizar drone
                    if (m.getDron() != null) {
                        Dron dron = m.getDron();
                        dron.setCantidadMinutosVolados((dron.getCantidadMinutosVolados() != null ? dron.getCantidadMinutosVolados() : 0) + (int) minutos);
                        dron.setCantidadVuelos((dron.getCantidadVuelos() != null ? dron.getCantidadVuelos() : 0) + 1);
                        dron.setUltimoVuelo(Instant.now());
                    }

                    // Actualizar piloto
                    Usuario piloto = m.getPiloto();
                    piloto.setHorasVuelo((piloto.getHorasVuelo() != null ? piloto.getHorasVuelo() : 0) + (int)(minutos / 60));
                    piloto.setCantidadVuelos((piloto.getCantidadVuelos() != null ? piloto.getCantidadVuelos() : 0) + 1);
                }
            }

            Mision updated = misionBusiness.update(m);

            // Log automático del cambio de estado
            registrarLog(updated, estadoAnterior, nuevoEstado);

            return ResponseEntity.ok(toDTO(updated));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (NotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (BusinessException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private void registrarLog(Mision m, EstadoMision estadoAnterior, EstadoMision estadoNuevo) {
        try {
            Usuario usuarioActual = (Usuario) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            Log log = new Log();
            log.setEntidadTipo("MISION");
            log.setEntidadId(m.getId());
            log.setTimestamp(Instant.now());
            log.setTipo("CAMBIO_ESTADO");
            log.setDetalle(String.format("Misión '%s' cambió de %s → %s. Piloto: %s %s. Drone: %s",
                    m.getNombre(),
                    estadoAnterior,
                    estadoNuevo,
                    m.getPiloto().getNombre(), m.getPiloto().getApellido(),
                    m.getDron() != null ? m.getDron().getNombre() : "sin drone"));
            log.setUsuario(usuarioActual);
            logRepository.save(log);
        } catch (Exception ignored) {}
    }

    // ─────────────────────────────────────────────
    // DELETE /misiones/{id}
    // ─────────────────────────────────────────────
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        try {
            misionBusiness.delete(id);
            return ResponseEntity.noContent().build();
        } catch (NotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (BusinessException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ─────────────────────────────────────────────
    // Mapeo entidad → DTO
    // ─────────────────────────────────────────────
    private MisionDTO toDTO(Mision m) {
        MisionDTO dto = new MisionDTO();
        dto.id              = m.getId();
        dto.nombre          = m.getNombre();
        dto.descripcion     = m.getDescripcion();
        dto.observaciones   = m.getObservaciones();
        dto.linkRtsp        = m.getLinkRtsp();
        dto.categoria       = m.getCategoria();
        dto.prioridad       = m.getPrioridad();
        dto.estado          = m.getEstado();
        dto.fechaCreacion   = m.getFechaCreacion();
        dto.ultimaEjecucion = m.getUltimaEjecucion();
        dto.fechaInicio     = m.getFechaInicio();
        dto.fechaFin        = m.getFechaFin();
        if (m.getFechaInicio() != null && m.getFechaFin() != null) {
            dto.duracionMinutos = ChronoUnit.MINUTES.between(m.getFechaInicio(), m.getFechaFin());
        }

        if (m.getPiloto() != null) {
            dto.pilotoId     = m.getPiloto().getId();
            dto.pilotoNombre = m.getPiloto().getNombre() + " " + m.getPiloto().getApellido();
        }
        if (m.getDron() != null) {
            dto.dronId     = m.getDron().getId();
            dto.dronNombre = m.getDron().getNombre();
        }
        if (m.getDock() != null) {
            dto.dockId     = m.getDock().getId();
            dto.dockNombre = m.getDock().getNombre();
        }
        return dto;
    }

    // ─────────────────────────────────────────────
    // Mapeo request → entidad
    // ─────────────────────────────────────────────
    private Mision fromRequest(MisionRequest req, Mision m) throws NotFoundException {
        m.setNombre(req.nombre);
        m.setDescripcion(req.descripcion);
        m.setObservaciones(req.observaciones);
        m.setLinkRtsp(req.linkRtsp);
        m.setCategoria(req.categoria);
        m.setPrioridad(req.prioridad);
        if (req.estado != null) {
            m.setEstado(req.estado);
        }

        if (req.pilotoId == null) {
            throw new NotFoundException("pilotoId es obligatorio");
        }
        Usuario piloto = usuarioRepository.findById(req.pilotoId)
                .orElseThrow(() -> new NotFoundException("Piloto no encontrado: " + req.pilotoId));
        m.setPiloto(piloto);

        if (req.dronId != null) {
            Dron dron = dronRepository.findById(req.dronId)
                    .orElseThrow(() -> new NotFoundException("Dron no encontrado: " + req.dronId));
            m.setDron(dron);
        } else {
            m.setDron(null);
        }

        if (req.dockId != null) {
            Dock dock = dockRepository.findById(req.dockId)
                    .orElseThrow(() -> new NotFoundException("Dock no encontrado: " + req.dockId));
            m.setDock(dock);
        } else {
            m.setDock(null);
        }

        if (req.pozoId != null) {
            Pozo pozo = pozoRepository.findById(req.pozoId)
                    .orElseThrow(() -> new NotFoundException("Pozo no encontrado: " + req.pozoId));
            m.setPozo(pozo);
        } else {
            m.setPozo(null);
        }

        return m;
    }
}
