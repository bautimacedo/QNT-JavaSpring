package com.gestion.qnt.controller;

import com.gestion.qnt.config.ApiConstants;
import com.gestion.qnt.controller.dto.LogDTO;
import com.gestion.qnt.controller.dto.LogRequest;
import com.gestion.qnt.model.Log;
import com.gestion.qnt.model.business.exceptions.BusinessException;
import com.gestion.qnt.model.business.exceptions.NotFoundException;
import com.gestion.qnt.model.business.interfaces.ILogBusiness;
import com.gestion.qnt.repository.LogRepository;
import com.gestion.qnt.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Endpoints para Libros de Vuelo (logs de eventos del sistema).
 *
 * GET  /logs                         → todos los logs
 * GET  /logs?entidadTipo=DRON        → filtrar por tipo de entidad
 * GET  /logs?entidadTipo=DRON&entidadId=5 → logs de un equipo específico
 * GET  /logs/{id}
 * POST /logs
 * DELETE /logs/{id}   (solo ADMIN)
 */
@RestController
@RequestMapping(ApiConstants.URL_BASE + "/logs")
public class LogRestController {

    @Autowired private ILogBusiness logBusiness;
    @Autowired private LogRepository logRepository;
    @Autowired private UsuarioRepository usuarioRepository;

    // ─────────────────────────────────────────────
    // GET /logs
    // ─────────────────────────────────────────────
    @GetMapping("")
    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<LogDTO>> list(
            @RequestParam(required = false) String entidadTipo,
            @RequestParam(required = false) Long entidadId) {
        try {
            List<Log> logs;
            if (entidadTipo != null && entidadId != null) {
                logs = logRepository.findByEntidadWithDetails(entidadTipo.toUpperCase(), entidadId);
            } else if (entidadTipo != null) {
                logs = logRepository.findByEntidadTipoWithDetails(entidadTipo.toUpperCase());
            } else {
                logs = logRepository.findAllWithDetails();
            }
            return ResponseEntity.ok(logs.stream().map(this::toDTO).collect(Collectors.toList()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ─────────────────────────────────────────────
    // GET /logs/{id}
    // ─────────────────────────────────────────────
    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<LogDTO> load(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(toDTO(logBusiness.load(id)));
        } catch (NotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (BusinessException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ─────────────────────────────────────────────
    // POST /logs
    // ─────────────────────────────────────────────
    @PostMapping("")
    @Transactional
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<LogDTO> add(@RequestBody LogRequest req) {
        try {
            Log log = fromRequest(req, new Log());
            return ResponseEntity.status(HttpStatus.CREATED).body(toDTO(logBusiness.add(log)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (NotFoundException e) {
            return ResponseEntity.badRequest().build();
        } catch (BusinessException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ─────────────────────────────────────────────
    // DELETE /logs/{id}
    // ─────────────────────────────────────────────
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        try {
            logBusiness.delete(id);
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
    private LogDTO toDTO(Log l) {
        LogDTO dto = new LogDTO();
        dto.id = l.getId();
        dto.entidadTipo = l.getEntidadTipo();
        dto.entidadId = l.getEntidadId();
        dto.timestamp = l.getTimestamp();
        dto.tipo = l.getTipo();
        dto.detalle = l.getDetalle();

        if (l.getUsuario() != null) {
            dto.usuarioId = l.getUsuario().getId();
            dto.usuarioNombre = l.getUsuario().getNombre() + " " + l.getUsuario().getApellido();
        }
        return dto;
    }

    // ─────────────────────────────────────────────
    // Mapeo request → entidad
    // ─────────────────────────────────────────────
    private Log fromRequest(LogRequest req, Log l) throws NotFoundException {
        if (req.entidadTipo == null || req.entidadTipo.isBlank())
            throw new IllegalArgumentException("entidadTipo es obligatorio");
        if (req.entidadId == null)
            throw new IllegalArgumentException("entidadId es obligatorio");

        l.setEntidadTipo(req.entidadTipo.toUpperCase());
        l.setEntidadId(req.entidadId);
        l.setTimestamp(req.timestamp != null ? req.timestamp : Instant.now());
        l.setTipo(req.tipo);
        l.setDetalle(req.detalle);

        if (req.usuarioId != null) {
            l.setUsuario(usuarioRepository.findById(req.usuarioId)
                    .orElseThrow(() -> new NotFoundException("Usuario no encontrado: " + req.usuarioId)));
        }
        return l;
    }
}
