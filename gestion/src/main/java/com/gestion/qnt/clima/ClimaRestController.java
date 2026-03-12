package com.gestion.qnt.clima;

import com.gestion.qnt.model.ClimaRegistro;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/qnt/v1/clima")
public class ClimaRestController {

    private final ClimaService service;

    public ClimaRestController(ClimaService service) {
        this.service = service;
    }

    /** GET /api/qnt/v1/clima — último registro de todos los sites */
    @GetMapping
    public ResponseEntity<List<ClimaDTO>> getAll() {
        return ResponseEntity.ok(service.getLatestAll().stream().map(this::toDTO).toList());
    }

    /** GET /api/qnt/v1/clima/{codigo} — último registro de un site */
    @GetMapping("/{codigo}")
    public ResponseEntity<ClimaDTO> getBySite(@PathVariable String codigo) {
        return service.getLatestBySite(codigo)
            .map(r -> ResponseEntity.ok(toDTO(r)))
            .orElse(ResponseEntity.notFound().build());
    }

    /** GET /api/qnt/v1/clima/{codigo}/historial?limit=24 */
    @GetMapping("/{codigo}/historial")
    public ResponseEntity<List<ClimaDTO>> getHistorial(
            @PathVariable String codigo,
            @RequestParam(defaultValue = "24") int limit) {
        return ResponseEntity.ok(service.getHistorial(codigo, limit).stream().map(this::toDTO).toList());
    }

    // ── DTO ────────────────────────────────────────────────────────────────────
    public record ClimaDTO(
        Long    id,
        String  codigo,
        String  siteName,
        String  cityName,
        Double  tempCelsius,
        Double  windSpeedMs,
        Double  windGustMs,
        Integer visibilityMeters,
        String  conditionMain,
        String  conditionDesc,
        Boolean isFlyable,
        Instant recordedAt
    ) {}

    private ClimaDTO toDTO(ClimaRegistro r) {
        return new ClimaDTO(
            r.getId(),
            r.getSite().getCodigo(),
            r.getSite().getNombre(),
            r.getCityName(),
            r.getTempCelsius(),
            r.getWindSpeedMs(),
            r.getWindGustMs(),
            r.getVisibilityMeters(),
            r.getConditionMain(),
            r.getConditionDesc(),
            r.getIsFlyable(),
            r.getRecordedAt()
        );
    }
}
