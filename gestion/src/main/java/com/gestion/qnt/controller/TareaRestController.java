package com.gestion.qnt.controller;

import com.gestion.qnt.config.ApiConstants;
import com.gestion.qnt.controller.dto.TareaDTO;
import com.gestion.qnt.controller.dto.TareaRequest;
import com.gestion.qnt.model.Tarea;
import com.gestion.qnt.model.Usuario;
import com.gestion.qnt.model.business.exceptions.BusinessException;
import com.gestion.qnt.model.business.exceptions.NotFoundException;
import com.gestion.qnt.model.business.interfaces.ITareaBusiness;
import com.gestion.qnt.model.enums.EstadoTarea;
import com.gestion.qnt.repository.TareaRepository;
import com.gestion.qnt.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping(ApiConstants.URL_BASE + "/tareas")
public class TareaRestController {

    @Autowired
    private ITareaBusiness tareaBusiness;

    @Autowired
    private TareaRepository tareaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    // ─────────────────────────────────────────────
    // GET /tareas — lista completa con detalles
    // ─────────────────────────────────────────────
    @GetMapping("")
    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<TareaDTO>> list() {
        try {
            List<Tarea> tareas = tareaRepository.findAllWithDetails();
            return ResponseEntity.ok(tareas.stream().map(this::toDTO).collect(Collectors.toList()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ─────────────────────────────────────────────
    // GET /tareas?estado=PENDIENTE
    // ─────────────────────────────────────────────
    @GetMapping(params = "estado")
    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<TareaDTO>> listByEstado(@RequestParam EstadoTarea estado) {
        try {
            List<Tarea> tareas = tareaRepository.findByEstadoWithDetails(estado);
            return ResponseEntity.ok(tareas.stream().map(this::toDTO).collect(Collectors.toList()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ─────────────────────────────────────────────
    // GET /tareas/{id}
    // ─────────────────────────────────────────────
    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TareaDTO> load(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(toDTO(tareaBusiness.load(id)));
        } catch (NotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (BusinessException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ─────────────────────────────────────────────
    // POST /tareas
    // ─────────────────────────────────────────────
    @PostMapping("")
    @Transactional
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TareaDTO> add(@RequestBody TareaRequest req) {
        try {
            Tarea t = fromRequest(req, new Tarea());
            return ResponseEntity.status(HttpStatus.CREATED).body(toDTO(tareaBusiness.add(t)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (NotFoundException e) {
            return ResponseEntity.badRequest().build();
        } catch (BusinessException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ─────────────────────────────────────────────
    // PUT /tareas/{id}
    // ─────────────────────────────────────────────
    @PutMapping("/{id}")
    @Transactional
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TareaDTO> update(@PathVariable Long id, @RequestBody TareaRequest req) {
        try {
            Tarea existing = tareaBusiness.load(id);
            Tarea t = fromRequest(req, existing);
            t.setId(id);
            return ResponseEntity.ok(toDTO(tareaBusiness.update(t)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (NotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (BusinessException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ─────────────────────────────────────────────
    // PATCH /tareas/{id}/estado — mover columna Kanban
    // ─────────────────────────────────────────────
    @PatchMapping("/{id}/estado")
    @Transactional
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TareaDTO> cambiarEstado(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        try {
            EstadoTarea nuevoEstado = EstadoTarea.valueOf(body.get("estado"));
            Tarea t = tareaBusiness.load(id);
            t.setEstado(nuevoEstado);
            if (nuevoEstado == EstadoTarea.COMPLETADA) {
                t.setFechaCompletada(LocalDateTime.now());
            } else {
                t.setFechaCompletada(null);
            }
            return ResponseEntity.ok(toDTO(tareaBusiness.update(t)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (NotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (BusinessException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ─────────────────────────────────────────────
    // DELETE /tareas/{id}
    // ─────────────────────────────────────────────
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        try {
            tareaBusiness.delete(id);
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
    private TareaDTO toDTO(Tarea t) {
        TareaDTO dto = new TareaDTO();
        dto.id              = t.getId();
        dto.titulo          = t.getTitulo();
        dto.descripcion     = t.getDescripcion();
        dto.estado          = t.getEstado();
        dto.prioridad       = t.getPrioridad();
        dto.fechaVencimiento = t.getFechaVencimiento();
        dto.fechaCreacion   = t.getFechaCreacion();
        dto.fechaCompletada = t.getFechaCompletada();

        dto.vencida = t.getFechaVencimiento() != null
                && t.getEstado() != EstadoTarea.COMPLETADA
                && t.getFechaVencimiento().isBefore(LocalDate.now());

        if (t.getAsignadoA() != null) {
            dto.asignadoAId     = t.getAsignadoA().getId();
            dto.asignadoANombre = t.getAsignadoA().getNombre() + " " + t.getAsignadoA().getApellido();
        }
        if (t.getCreadoPor() != null) {
            dto.creadoPorId     = t.getCreadoPor().getId();
            dto.creadoPorNombre = t.getCreadoPor().getNombre() + " " + t.getCreadoPor().getApellido();
        }
        return dto;
    }

    // ─────────────────────────────────────────────
    // Mapeo request → entidad
    // ─────────────────────────────────────────────
    private Tarea fromRequest(TareaRequest req, Tarea t) throws NotFoundException {
        if (req.titulo == null || req.titulo.isBlank()) {
            throw new IllegalArgumentException("El título de la tarea es obligatorio");
        }
        t.setTitulo(req.titulo.trim());
        t.setDescripcion(req.descripcion);
        t.setFechaVencimiento(req.fechaVencimiento);

        if (req.prioridad != null) t.setPrioridad(req.prioridad);
        if (req.estado    != null) t.setEstado(req.estado);

        if (req.asignadoAId != null) {
            Usuario u = usuarioRepository.findById(req.asignadoAId)
                    .orElseThrow(() -> new NotFoundException("Usuario no encontrado: " + req.asignadoAId));
            t.setAsignadoA(u);
        } else {
            t.setAsignadoA(null);
        }

        if (req.creadoPorId != null) {
            Usuario u = usuarioRepository.findById(req.creadoPorId)
                    .orElseThrow(() -> new NotFoundException("Usuario no encontrado: " + req.creadoPorId));
            t.setCreadoPor(u);
        }

        return t;
    }
}
