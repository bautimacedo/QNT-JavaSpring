package com.gestion.qnt.controller;

import com.gestion.qnt.config.ApiConstants;
import com.gestion.qnt.controller.dto.ProgramacionMisionDTO;
import com.gestion.qnt.controller.dto.ProgramacionMisionRequest;
import com.gestion.qnt.model.Mision;
import com.gestion.qnt.model.ProgramacionMision;
import com.gestion.qnt.repository.MisionRepository;
import com.gestion.qnt.repository.ProgramacionMisionRepository;
import com.gestion.qnt.service.ProgramacionMisionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.DayOfWeek;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping(ApiConstants.URL_BASE + "/programaciones")
public class ProgramacionMisionRestController {

    @Autowired
    private ProgramacionMisionRepository repo;

    @Autowired
    private MisionRepository misionRepository;

    @Autowired
    private ProgramacionMisionService service;

    @GetMapping("")
    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ProgramacionMisionDTO>> list() {
        return ResponseEntity.ok(
                repo.findAllWithDetails().stream().map(this::toDTO).collect(Collectors.toList())
        );
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProgramacionMisionDTO> load(@PathVariable Long id) {
        return repo.findById(id)
                .map(p -> ResponseEntity.ok(toDTO(p)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("")
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProgramacionMisionDTO> create(@RequestBody ProgramacionMisionRequest req) {
        ProgramacionMision p = fromRequest(req, new ProgramacionMision());
        if (p.getNombre() == null) return ResponseEntity.badRequest().build();
        p.setProxEjecucion(service.calcularProxEjecucion(p));
        return ResponseEntity.status(HttpStatus.CREATED).body(toDTO(repo.save(p)));
    }

    @PutMapping("/{id}")
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProgramacionMisionDTO> update(@PathVariable Long id, @RequestBody ProgramacionMisionRequest req) {
        return repo.findById(id).map(p -> {
            fromRequest(req, p);
            p.setProxEjecucion(service.calcularProxEjecucion(p));
            return ResponseEntity.ok(toDTO(repo.save(p)));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!repo.existsById(id)) return ResponseEntity.notFound().build();
        // Borrar primero las misiones generadas automáticamente por esta programación
        // (FK constraint: misiones.programacion_id apunta a programacion_misiones.id).
        // La misionPlantilla no se toca: su FK va de programación → misión, no al revés.
        misionRepository.deleteByProgramacionId(id);
        repo.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/activar")
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> activar(@PathVariable Long id) {
        return repo.findById(id).map(p -> {
            p.setActiva(true);
            p.setProxEjecucion(service.calcularProxEjecucion(p));
            repo.save(p);
            return ResponseEntity.ok(Map.<String, Object>of("activa", true));
        }).orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}/desactivar")
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> desactivar(@PathVariable Long id) {
        return repo.findById(id).map(p -> {
            p.setActiva(false);
            repo.save(p);
            return ResponseEntity.ok(Map.<String, Object>of("activa", false));
        }).orElse(ResponseEntity.notFound().build());
    }

    // ─── helpers ──────────────────────────────────────────────────────────────

    private ProgramacionMision fromRequest(ProgramacionMisionRequest req, ProgramacionMision p) {
        if (req.misionPlantillaId != null) {
            Mision plantilla = misionRepository.findById(req.misionPlantillaId).orElse(null);
            p.setMisionPlantilla(plantilla);
            if (plantilla != null) p.setNombre(plantilla.getNombre());
        }

        p.setTipoRecurrencia(req.tipoRecurrencia);
        p.setDiasSemana(req.diasSemana != null ? req.diasSemana : new HashSet<>());
        p.setDiaMes(req.diaMes);
        p.setIntervaloDias(req.intervaloDias);
        p.setHora(req.hora);
        p.setFechaInicioVigencia(req.fechaInicioVigencia);
        p.setFechaFinVigencia(req.fechaFinVigencia);
        if (req.activa != null) p.setActiva(req.activa);

        return p;
    }

    private ProgramacionMisionDTO toDTO(ProgramacionMision p) {
        ProgramacionMisionDTO dto = new ProgramacionMisionDTO();
        dto.id = p.getId();
        dto.nombre = p.getNombre();
        dto.tipoRecurrencia = p.getTipoRecurrencia();
        dto.diasSemana = p.getDiasSemana() != null
                ? p.getDiasSemana().stream().map(DayOfWeek::name).collect(Collectors.toList())
                : List.of();
        dto.diaMes = p.getDiaMes();
        dto.intervaloDias = p.getIntervaloDias();
        dto.hora = p.getHora() != null ? p.getHora().toString() : null;
        dto.activa = p.isActiva();
        dto.fechaInicioVigencia = p.getFechaInicioVigencia();
        dto.fechaFinVigencia = p.getFechaFinVigencia();
        dto.ultimaEjecucion = p.getUltimaEjecucion();
        dto.proxEjecucion = p.getProxEjecucion();
        dto.fechaCreacion = p.getFechaCreacion();

        Mision plantilla = p.getMisionPlantilla();
        if (plantilla != null) {
            dto.misionPlantillaId = plantilla.getId();
            dto.misionPlantillaNombre = plantilla.getNombre();
            dto.misionPlantillaDronNombre = plantilla.getDron() != null ? plantilla.getDron().getNombre() : null;
            dto.misionPlantillaTieneWebhook = plantilla.getWebhookUrl() != null && !plantilla.getWebhookUrl().isBlank()
                    && plantilla.getWebhookBearer() != null && !plantilla.getWebhookBearer().isBlank();
        }

        return dto;
    }
}
