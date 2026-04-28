package com.gestion.qnt.controller;

import com.gestion.qnt.config.ApiConstants;
import com.gestion.qnt.controller.dto.MisionDTO;
import com.gestion.qnt.controller.dto.MisionRequest;
import com.gestion.qnt.model.*;
import com.gestion.qnt.model.business.exceptions.BusinessException;
import com.gestion.qnt.model.business.exceptions.NotFoundException;
import com.gestion.qnt.model.business.interfaces.IMisionBusiness;
import com.gestion.qnt.model.enums.EstadoMision;
import com.gestion.qnt.model.enums.Yacimiento;
import com.gestion.qnt.repository.DockRepository;
import com.gestion.qnt.repository.DronRepository;
import com.gestion.qnt.repository.LogRepository;
import com.gestion.qnt.repository.MisionRepository;
import com.gestion.qnt.repository.PozoRepository;
import com.gestion.qnt.repository.UsuarioRepository;
import com.gestion.qnt.repository.VueloLogRepository;
import com.gestion.qnt.security.AuthUser;
import com.gestion.qnt.service.FlytbaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import org.springframework.format.annotation.DateTimeFormat;
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

    @Autowired
    private VueloLogRepository vueloLogRepository;

    @Autowired
    private FlytbaseService flytbaseService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // ─────────────────────────────────────────────
    // GET /misiones — lista con detalles
    // ─────────────────────────────────────────────
    @GetMapping("")
    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<MisionDTO>> list() {
        try {
            List<Mision> misiones = misionRepository.findAllWithDetails().stream()
                    .filter(m -> m.getProgramacion() == null)
                    .collect(Collectors.toList());
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
    // GET /misiones/historial — misiones lanzadas desde el sistema
    // ─────────────────────────────────────────────
    @GetMapping("/historial")
    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<MisionDTO>> historial(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        try {
            LocalDateTime desdeTs = desde != null ? desde.atStartOfDay() : null;
            LocalDateTime hastaTs = hasta != null ? hasta.plusDays(1).atStartOfDay().minusNanos(1) : null;
            List<Mision> misiones = misionRepository.findHistorial();
            return ResponseEntity.ok(misiones.stream()
                    .filter(m -> desdeTs == null || (m.getFechaInicio() != null && !m.getFechaInicio().isBefore(desdeTs)))
                    .filter(m -> hastaTs == null || (m.getFechaInicio() != null && !m.getFechaInicio().isAfter(hastaTs)))
                    .map(this::toDTO)
                    .collect(Collectors.toList()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ─────────────────────────────────────────────
    // GET /misiones/piloto/{pilotoId} — historial de vuelos del piloto
    // ─────────────────────────────────────────────
    @GetMapping("/piloto/{pilotoId}")
    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<MisionDTO>> listByPiloto(@PathVariable Long pilotoId) {
        try {
            List<Mision> misiones = misionRepository.findByPilotoIdWithDetails(pilotoId);
            return ResponseEntity.ok(misiones.stream().map(this::toDTO).collect(Collectors.toList()));
        } catch (Exception e) {
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
            List<Mision> misiones = misionRepository.findByEstadoWithDetails(estado).stream()
                    .filter(m -> m.getProgramacion() == null)
                    .collect(Collectors.toList());
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

                    // Actualizar piloto (puede ser null si la misión nunca fue lanzada)
                    Usuario piloto = m.getPiloto();
                    if (piloto != null) {
                        double horasActualesPiloto = piloto.getHorasVuelo() != null ? piloto.getHorasVuelo() : 0.0;
                        piloto.setHorasVuelo(Math.round((horasActualesPiloto + minutos / 60.0) * 100.0) / 100.0);
                        piloto.setCantidadVuelos((piloto.getCantidadVuelos() != null ? piloto.getCantidadVuelos() : 0) + 1);
                    }
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
            AuthUser authUser = (AuthUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            Usuario usuarioActual = usuarioRepository.findById(authUser.getId()).orElse(null);
            if (usuarioActual == null) return;
            Log log = new Log();
            log.setEntidadTipo("MISION");
            log.setEntidadId(m.getId());
            log.setTimestamp(Instant.now());
            log.setTipo("CAMBIO_ESTADO");
            String pilotoStr = m.getPiloto() != null
                    ? m.getPiloto().getNombre() + " " + (m.getPiloto().getApellido() != null ? m.getPiloto().getApellido() : "")
                    : "sin piloto";
            log.setDetalle(String.format("Misión '%s' cambió de %s → %s. Piloto: %s. Drone: %s",
                    m.getNombre(),
                    estadoAnterior,
                    estadoNuevo,
                    pilotoStr.trim(),
                    m.getDron() != null ? m.getDron().getNombre() : "sin drone"));
            log.setUsuario(usuarioActual);
            logRepository.save(log);
        } catch (Exception ignored) {}
    }

    // ─────────────────────────────────────────────
    // POST /misiones/{id}/lanzar — lanza la misión vía FlytBase (EFO)
    // Solo PILOTO y ADMIN. Valida que el drone no esté volando.
    // ─────────────────────────────────────────────
    @PostMapping("/{id}/lanzar")
    @Transactional
    @PreAuthorize("hasRole('PILOTO') or hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> lanzar(@PathVariable Long id) {
        Mision m;
        try {
            m = misionBusiness.load(id);
        } catch (NotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (BusinessException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }

        // Estado debe ser PLANIFICADA o COMPLETADA (misiones reutilizables)
        if (m.getEstado() != EstadoMision.PLANIFICADA && m.getEstado() != EstadoMision.COMPLETADA) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "La misión debe estar en estado PLANIFICADA o COMPLETADA para lanzarse."));
        }

        // Debe tener dron asignado con yacimiento EFO
        Dron dron = m.getDron();
        if (dron == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "La misión no tiene un drone asignado."));
        }
        if (dron.getYacimiento() != Yacimiento.EFO) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Solo se pueden lanzar misiones con drones del yacimiento EFO."));
        }

        // Debe tener webhook configurado
        if (m.getWebhookUrl() == null || m.getWebhookUrl().isBlank()
                || m.getWebhookBearer() == null || m.getWebhookBearer().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "La misión no tiene configurado el webhook de FlytBase. Pedile a un administrador que lo configure."));
        }

        // Verificar que no haya un vuelo activo para este drone en vuelos_log
        if (vueloLogRepository.hayVueloActivo(dron.getNombre())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "El drone '" + dron.getNombre() + "' ya tiene un vuelo activo en curso. Esperá a que aterrice antes de lanzar otra misión."));
        }

        // Llamar webhook FlytBase
        try {
            flytbaseService.lanzarMision(
                    m.getWebhookUrl(),
                    m.getWebhookBearer(),
                    m.getNombre(),
                    m.getDock() != null ? m.getDock().getLatitud() : null,
                    m.getDock() != null ? m.getDock().getLongitud() : null
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(Map.of("error", "No se pudo contactar a FlytBase: " + e.getMessage()));
        }

        // Asignar piloto al usuario que ejecuta la misión (no al creador)
        AuthUser authUser = (AuthUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Usuario usuarioActual = usuarioRepository.findById(authUser.getId())
                .orElseThrow(() -> new RuntimeException("Usuario autenticado no encontrado"));
        m.setPiloto(usuarioActual);

        // Registrar en mision_pendiente para atribución de piloto en n8n
        String pilotoNombre = usuarioActual.getNombre() + " " + (usuarioActual.getApellido() != null ? usuarioActual.getApellido() : "");
        jdbcTemplate.update(
                "INSERT INTO mision_pendiente (drone_nombre, piloto_nombre, usuario_id, mision_id) VALUES (?, ?, ?, ?)",
                dron.getNombre(), pilotoNombre.trim(), usuarioActual.getId(), m.getId()
        );

        // Cambiar estado de la misión a EN_CURSO
        EstadoMision estadoAnteriorLanzar = m.getEstado();
        m.setEstado(EstadoMision.EN_CURSO);
        m.setUltimaEjecucion(LocalDateTime.now());
        m.setFechaInicio(LocalDateTime.now());
        m.setFechaFin(null);  // limpiar fechaFin de ejecuciones previas
        try {
            misionBusiness.update(m);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Misión lanzada en FlytBase pero error al actualizar estado: " + e.getMessage()));
        }

        registrarLog(m, estadoAnteriorLanzar, EstadoMision.EN_CURSO);

        return ResponseEntity.ok(Map.of("message", "Misión '" + m.getNombre() + "' lanzada exitosamente en FlytBase."));
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
        dto.webhookUrl = m.getWebhookUrl();
        dto.fechaProgramada = m.getFechaProgramada();
        dto.programacionId = m.getProgramacion() != null ? m.getProgramacion().getId() : null;

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

        if (req.pilotoId != null) {
            Usuario piloto = usuarioRepository.findById(req.pilotoId)
                    .orElseThrow(() -> new NotFoundException("Piloto no encontrado: " + req.pilotoId));
            m.setPiloto(piloto);
        } else {
            // El piloto se asigna al momento de lanzar la misión
            m.setPiloto(null);
        }

        if (req.dronId != null) {
            Dron dron = dronRepository.findById(req.dronId)
                    .orElseThrow(() -> new NotFoundException("Dron no encontrado: " + req.dronId));
            m.setDron(dron);
            // El dock se obtiene automáticamente del dron asignado
            m.setDock(dron.getDock());
        } else {
            m.setDron(null);
            m.setDock(null);
        }

        if (req.pozoId != null) {
            Pozo pozo = pozoRepository.findById(req.pozoId)
                    .orElseThrow(() -> new NotFoundException("Pozo no encontrado: " + req.pozoId));
            m.setPozo(pozo);
        } else {
            m.setPozo(null);
        }

        m.setWebhookUrl(req.webhookUrl);
        m.setWebhookBearer(req.webhookBearer);

        return m;
    }
}
