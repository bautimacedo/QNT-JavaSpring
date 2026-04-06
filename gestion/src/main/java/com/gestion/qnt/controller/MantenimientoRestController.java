package com.gestion.qnt.controller;

import com.gestion.qnt.config.ApiConstants;
import com.gestion.qnt.controller.dto.MantenimientoDockDTO;
import com.gestion.qnt.controller.dto.MantenimientoDockRequest;
import com.gestion.qnt.controller.dto.MantenimientoDronDTO;
import com.gestion.qnt.controller.dto.MantenimientoDronRequest;
import com.gestion.qnt.model.*;
import com.gestion.qnt.model.business.exceptions.BusinessException;
import com.gestion.qnt.model.business.exceptions.NotFoundException;
import com.gestion.qnt.model.business.interfaces.IMantenimientoDockBusiness;
import com.gestion.qnt.model.business.interfaces.IMantenimientoDronBusiness;
import com.gestion.qnt.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Endpoints para gestión de mantenimientos de drones y docks.
 *
 * Drones:  GET|POST /mantenimientos/drones
 *          GET|PUT|DELETE /mantenimientos/drones/{id}
 *          GET /mantenimientos/drones?dronId=X
 *
 * Docks:   GET|POST /mantenimientos/docks
 *          GET|PUT|DELETE /mantenimientos/docks/{id}
 *          GET /mantenimientos/docks?dockId=X
 */
@RestController
@RequestMapping(ApiConstants.URL_BASE + "/mantenimientos")
public class MantenimientoRestController {

    @Autowired private IMantenimientoDronBusiness dronBusiness;
    @Autowired private IMantenimientoDockBusiness dockBusiness;

    @Autowired private MantenimientoDronRepository dronRepo;
    @Autowired private MantenimientoDockRepository dockRepo;
    @Autowired private DronRepository dronRepository;
    @Autowired private DockRepository dockRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private BateriaRepository bateriaRepository;
    @Autowired private HeliceRepository heliceRepository;

    // ════════════════════════════════════════════════════
    // MANTENIMIENTO DRONES
    // ════════════════════════════════════════════════════

    @GetMapping("/drones")
    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<MantenimientoDronDTO>> listDrones(
            @RequestParam(required = false) Long dronId) {
        try {
            List<MantenimientoDron> list = dronId != null
                    ? dronRepo.findByDronIdWithDetails(dronId)
                    : dronRepo.findAllWithDetails();
            return ResponseEntity.ok(list.stream().map(this::toDronDTO).collect(Collectors.toList()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/drones/{id}")
    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MantenimientoDronDTO> loadDron(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(toDronDTO(dronBusiness.load(id)));
        } catch (NotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (BusinessException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/drones")
    @Transactional
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MantenimientoDronDTO> addDron(@RequestBody MantenimientoDronRequest req) {
        try {
            MantenimientoDron m = fromDronRequest(req, new MantenimientoDron());
            return ResponseEntity.status(HttpStatus.CREATED).body(toDronDTO(dronBusiness.add(m)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (NotFoundException e) {
            return ResponseEntity.badRequest().build();
        } catch (BusinessException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping("/drones/{id}")
    @Transactional
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MantenimientoDronDTO> updateDron(
            @PathVariable Long id,
            @RequestBody MantenimientoDronRequest req) {
        try {
            MantenimientoDron existing = dronBusiness.load(id);
            MantenimientoDron m = fromDronRequest(req, existing);
            m.setId(id);
            return ResponseEntity.ok(toDronDTO(dronBusiness.update(m)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (NotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (BusinessException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/drones/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteDron(@PathVariable Long id) {
        try {
            dronBusiness.delete(id);
            return ResponseEntity.noContent().build();
        } catch (NotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (BusinessException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ════════════════════════════════════════════════════
    // MANTENIMIENTO DOCKS
    // ════════════════════════════════════════════════════

    @GetMapping("/docks")
    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<MantenimientoDockDTO>> listDocks(
            @RequestParam(required = false) Long dockId) {
        try {
            List<MantenimientoDock> list = dockId != null
                    ? dockRepo.findByDockIdWithDetails(dockId)
                    : dockRepo.findAllWithDetails();
            return ResponseEntity.ok(list.stream().map(this::toDockDTO).collect(Collectors.toList()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/docks/{id}")
    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MantenimientoDockDTO> loadDock(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(toDockDTO(dockBusiness.load(id)));
        } catch (NotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (BusinessException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/docks")
    @Transactional
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MantenimientoDockDTO> addDock(@RequestBody MantenimientoDockRequest req) {
        try {
            MantenimientoDock m = fromDockRequest(req, new MantenimientoDock());
            return ResponseEntity.status(HttpStatus.CREATED).body(toDockDTO(dockBusiness.add(m)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (NotFoundException e) {
            return ResponseEntity.badRequest().build();
        } catch (BusinessException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping("/docks/{id}")
    @Transactional
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MantenimientoDockDTO> updateDock(
            @PathVariable Long id,
            @RequestBody MantenimientoDockRequest req) {
        try {
            MantenimientoDock existing = dockBusiness.load(id);
            MantenimientoDock m = fromDockRequest(req, existing);
            m.setId(id);
            return ResponseEntity.ok(toDockDTO(dockBusiness.update(m)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (NotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (BusinessException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/docks/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteDock(@PathVariable Long id) {
        try {
            dockBusiness.delete(id);
            return ResponseEntity.noContent().build();
        } catch (NotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (BusinessException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ════════════════════════════════════════════════════
    // Mapeos: entidad → DTO
    // ════════════════════════════════════════════════════

    private MantenimientoDronDTO toDronDTO(MantenimientoDron m) {
        MantenimientoDronDTO dto = new MantenimientoDronDTO();
        dto.id = m.getId();
        dto.fechaMantenimiento = m.getFechaMantenimiento();
        dto.tipoMantenimiento = m.getTipoMantenimiento();
        dto.checklist = m.getChecklist();
        dto.observaciones = m.getObservaciones();
        dto.fotos = m.getFotos();

        if (m.getDron() != null) {
            dto.dronId = m.getDron().getId();
            dto.dronNombre = m.getDron().getNombre();
            dto.dronModelo = m.getDron().getModelo();
        }
        if (m.getUsuario() != null) {
            dto.usuarioId = m.getUsuario().getId();
            dto.usuarioNombre = m.getUsuario().getNombre() + " " + m.getUsuario().getApellido();
        }
        if (m.getBateriaVieja() != null) {
            dto.bateriaViejaId = m.getBateriaVieja().getId();
            dto.bateriaViejaNombre = m.getBateriaVieja().getNombre();
        }
        if (m.getBateriaNueva() != null) {
            dto.bateriaNuevaId = m.getBateriaNueva().getId();
            dto.bateriaNuevaNombre = m.getBateriaNueva().getNombre();
        }
        dto.helicesViejasIds = m.getHelicesViejas().stream()
                .map(Helice::getId).collect(Collectors.toList());
        dto.helicesNuevasIds = m.getHelicesNuevas().stream()
                .map(Helice::getId).collect(Collectors.toList());
        return dto;
    }

    private MantenimientoDockDTO toDockDTO(MantenimientoDock m) {
        MantenimientoDockDTO dto = new MantenimientoDockDTO();
        dto.id = m.getId();
        dto.fechaMantenimiento = m.getFechaMantenimiento();
        dto.tipoMantenimiento = m.getTipoMantenimiento();
        dto.checklist = m.getChecklist();
        dto.observaciones = m.getObservaciones();
        dto.fotos = m.getFotos();

        if (m.getDock() != null) {
            dto.dockId = m.getDock().getId();
            dto.dockNombre = m.getDock().getNombre();
            dto.dockModelo = m.getDock().getModelo();
        }
        if (m.getUsuario() != null) {
            dto.usuarioId = m.getUsuario().getId();
            dto.usuarioNombre = m.getUsuario().getNombre() + " " + m.getUsuario().getApellido();
        }
        return dto;
    }

    // ════════════════════════════════════════════════════
    // Mapeos: request → entidad
    // ════════════════════════════════════════════════════

    private MantenimientoDron fromDronRequest(MantenimientoDronRequest req, MantenimientoDron m)
            throws NotFoundException {
        if (req.dronId == null) throw new IllegalArgumentException("dronId es obligatorio");
        if (req.usuarioId == null) throw new IllegalArgumentException("usuarioId es obligatorio");
        if (req.fechaMantenimiento == null) throw new IllegalArgumentException("fechaMantenimiento es obligatorio");

        m.setDron(dronRepository.findById(req.dronId)
                .orElseThrow(() -> new NotFoundException("Dron no encontrado: " + req.dronId)));
        m.setUsuario(usuarioRepository.findById(req.usuarioId)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado: " + req.usuarioId)));
        m.setFechaMantenimiento(req.fechaMantenimiento);
        m.setTipoMantenimiento(req.tipoMantenimiento);
        m.setChecklist(req.checklist);
        m.setObservaciones(req.observaciones);
        m.setFotos(req.fotos);

        m.setBateriaVieja(req.bateriaViejaId != null
                ? bateriaRepository.findById(req.bateriaViejaId).orElse(null) : null);
        m.setBateriaNueva(req.bateriaNuevaId != null
                ? bateriaRepository.findById(req.bateriaNuevaId).orElse(null) : null);

        if (req.helicesViejasIds != null) {
            List<Helice> viejas = new ArrayList<>();
            for (Long hId : req.helicesViejasIds) {
                heliceRepository.findById(hId).ifPresent(viejas::add);
            }
            m.setHelicesViejas(viejas);
        }
        if (req.helicesNuevasIds != null) {
            List<Helice> nuevas = new ArrayList<>();
            for (Long hId : req.helicesNuevasIds) {
                heliceRepository.findById(hId).ifPresent(nuevas::add);
            }
            m.setHelicesNuevas(nuevas);
        }
        return m;
    }

    private MantenimientoDock fromDockRequest(MantenimientoDockRequest req, MantenimientoDock m)
            throws NotFoundException {
        if (req.dockId == null) throw new IllegalArgumentException("dockId es obligatorio");
        if (req.usuarioId == null) throw new IllegalArgumentException("usuarioId es obligatorio");
        if (req.fechaMantenimiento == null) throw new IllegalArgumentException("fechaMantenimiento es obligatorio");

        m.setDock(dockRepository.findById(req.dockId)
                .orElseThrow(() -> new NotFoundException("Dock no encontrado: " + req.dockId)));
        m.setUsuario(usuarioRepository.findById(req.usuarioId)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado: " + req.usuarioId)));
        m.setFechaMantenimiento(req.fechaMantenimiento);
        m.setTipoMantenimiento(req.tipoMantenimiento);
        m.setChecklist(req.checklist);
        m.setObservaciones(req.observaciones);
        m.setFotos(req.fotos);

        return m;
    }
}
