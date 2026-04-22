package com.gestion.qnt.controller;

import com.gestion.qnt.config.ApiConstants;
import com.gestion.qnt.controller.dto.InspeccionAibDTO;
import com.gestion.qnt.model.InspeccionAib;
import com.gestion.qnt.model.business.exceptions.BusinessException;
import com.gestion.qnt.model.business.exceptions.NotFoundException;
import com.gestion.qnt.model.business.interfaces.IInspeccionAibBusiness;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@RestController
@RequestMapping(ApiConstants.URL_BASE + "/inspecciones/aib")
@Slf4j
public class InspeccionAibRestController {

    @Autowired
    private IInspeccionAibBusiness business;

    @Autowired
    private com.gestion.qnt.repository.InspeccionAibRepository inspeccionAibRepository;

    @Value("${app.aib.api-key:aib-default-key}")
    private String apiKey;

    @Value("${app.aib.upload-dir:/var/lib/qnt/inspecciones}")
    private String uploadDir;

    // ─────────────────────────────────────────────
    // POST — recibe inspección desde pipeline externo (API Key)
    // ─────────────────────────────────────────────
    @PostMapping(value = "", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Transactional
    public ResponseEntity<InspeccionAibDTO> receiveInspeccion(
            @RequestHeader(value = "X-API-Key", required = false) String headerKey,
            @RequestPart("datos") String datosJson,
            @RequestPart(value = "graficos", required = false) List<MultipartFile> graficos) {

        if (apiKey.isBlank() || headerKey == null || !headerKey.equals(apiKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            InspeccionAib saved = business.receiveInspeccion(datosJson, graficos);
            return ResponseEntity.status(HttpStatus.CREATED).body(toDTO(saved));
        } catch (BusinessException e) {
            log.error("Error al recibir inspección AIB", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    // ─────────────────────────────────────────────
    // GET /inspecciones/aib — lista todas
    // ─────────────────────────────────────────────
    @GetMapping("")
    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<InspeccionAibDTO>> list() {
        try {
            return ResponseEntity.ok(business.list().stream().map(this::toDTO).toList());
        } catch (BusinessException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // ─────────────────────────────────────────────
    // GET /inspecciones/aib/{id}
    // ─────────────────────────────────────────────
    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<InspeccionAibDTO> getById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(toDTO(business.load(id)));
        } catch (NotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (BusinessException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // ─────────────────────────────────────────────
    // GET /inspecciones/aib/equipo/{aibId} — historial de un AIB
    // ─────────────────────────────────────────────
    @GetMapping("/equipo/{aibId}")
    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<InspeccionAibDTO>> listByAibId(@PathVariable String aibId) {
        try {
            return ResponseEntity.ok(business.listByAibId(aibId).stream().map(this::toDTO).toList());
        } catch (BusinessException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // ─────────────────────────────────────────────
    // GET /inspecciones/aib/{id}/graficos/{filename} — sirve imagen
    // ─────────────────────────────────────────────
    @GetMapping("/{id}/graficos/{filename}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<InputStreamResource> getGrafico(
            @PathVariable Long id,
            @PathVariable String filename) {

        if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            return ResponseEntity.badRequest().build();
        }

        try {
            InspeccionAib inspeccion = business.load(id);
            Path filePath = Paths.get(uploadDir, inspeccion.getAib().getAibId(), String.valueOf(id), filename);

            if (!Files.exists(filePath)) return ResponseEntity.notFound().build();

            String contentType = Files.probeContentType(filePath);
            if (contentType == null) contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(new InputStreamResource(Files.newInputStream(filePath)));
        } catch (NotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (BusinessException | IOException e) {
            log.error("Error al servir gráfico id={} filename={}", id, filename, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    // ─────────────────────────────────────────────
    // GET /inspecciones/aib/pozo/{pozoId} — historial por pozo
    // ─────────────────────────────────────────────
    @GetMapping("/pozo/{pozoId}")
    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<InspeccionAibDTO>> listByPozo(@PathVariable Long pozoId) {
        try {
            return ResponseEntity.ok(
                    inspeccionAibRepository.findByPozoId(pozoId).stream().map(this::toDTO).toList());
        } catch (Exception e) {
            log.error("Error al listar inspecciones del pozo {}", pozoId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    // ─────────────────────────────────────────────
    // DELETE /inspecciones/aib/{id}
    // ─────────────────────────────────────────────
    @DeleteMapping("/{id}")
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        try {
            business.delete(id);
            return ResponseEntity.noContent().build();
        } catch (NotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (BusinessException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    private InspeccionAibDTO toDTO(InspeccionAib i) {
        InspeccionAibDTO dto = new InspeccionAibDTO();
        dto.id = i.getId();
        dto.timestamp = i.getTimestamp();
        dto.fechaRegistro = i.getFechaRegistro();
        dto.estado = i.getEstado();
        dto.gpm = i.getGpm();
        if (i.getAib() != null) {
            dto.aibId = i.getAib().getAibId();
            dto.aibNombre = i.getAib().getNombre();
            if (i.getAib().getPozo() != null) {
                dto.pozoId = i.getAib().getPozo().getId();
                dto.pozoNombre = i.getAib().getPozo().getNombre();
            }
        }
        dto.velSubidaS = i.getVelSubidaS();
        dto.velBajadaS = i.getVelBajadaS();
        dto.velSubidaInS = i.getVelSubidaInS();
        dto.velBajadaInS = i.getVelBajadaInS();
        dto.velRatio = i.getVelRatio();
        dto.velConfianza = i.getVelConfianza();
        dto.derivadaVelMaxPxS = i.getDerivadaVelMaxPxS();
        dto.derivadaVelRmsPxS = i.getDerivadaVelRmsPxS();
        dto.derivadaAcelMaxPxS2 = i.getDerivadaAcelMaxPxS2();
        dto.derivadaConfianza = i.getDerivadaConfianza();
        dto.convCarreraIn = i.getConvCarreraIn();
        dto.convCarreraPx = i.getConvCarreraPx();
        dto.convScaleInPerPx = i.getConvScaleInPerPx();
        dto.convConfianza = i.getConvConfianza();
        dto.derivadaInVelMaxInS = i.getDerivadaInVelMaxInS();
        dto.derivadaInVelRmsInS = i.getDerivadaInVelRmsInS();
        dto.derivadaInAcelMaxInS2 = i.getDerivadaInAcelMaxInS2();
        dto.capturaAnotadaUrl = toUrl(i.getCapturaAnotadaPath(), i.getId());
        dto.graficoPosicionInUrl = toUrl(i.getGraficoPosicionInPath(), i.getId());
        dto.graficoProcesadaUrl = toUrl(i.getGraficoProcesadaPath(), i.getId());
        dto.graficoVelocidadUrl = toUrl(i.getGraficoVelocidadPath(), i.getId());
        dto.graficoDerivadaInUrl = toUrl(i.getGraficoDerivadaInPath(), i.getId());
        dto.graficoAceleracionInUrl = toUrl(i.getGraficoAceleracionInPath(), i.getId());
        return dto;
    }

    private String toUrl(String relativePath, Long id) {
        if (relativePath == null) return null;
        String filename = Paths.get(relativePath).getFileName().toString();
        return ApiConstants.URL_BASE + "/inspecciones/aib/" + id + "/graficos/" + filename;
    }
}
