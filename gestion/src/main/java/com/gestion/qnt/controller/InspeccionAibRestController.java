package com.gestion.qnt.controller;

import com.gestion.qnt.config.ApiConstants;
import com.gestion.qnt.controller.dto.InspeccionAibDTO;
import com.gestion.qnt.model.InspeccionAib;
import com.gestion.qnt.model.business.exceptions.BusinessException;
import com.gestion.qnt.model.business.exceptions.NotFoundException;
import com.gestion.qnt.model.business.interfaces.IInspeccionAibBusiness;
import com.gestion.qnt.service.AwsS3Service;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@RestController
@RequestMapping(ApiConstants.URL_BASE + "/inspecciones/aib")
@Slf4j
public class InspeccionAibRestController {

    /** Mapa tipo→accesor de la S3 key correspondiente en la entity. Whitelist estricta. */
    private static final Map<String, Function<InspeccionAib, String>> TIPO_TO_KEY = Map.ofEntries(
            Map.entry("video",                          InspeccionAib::getS3KeyVideo),
            Map.entry("captura",                        InspeccionAib::getS3KeyCaptura),
            Map.entry("grafico_detecciones_raw",        InspeccionAib::getS3KeyGraficoDeteccionesRaw),
            Map.entry("grafico_tiempos_ciclo",          InspeccionAib::getS3KeyGraficoTiemposCiclo),
            Map.entry("grafico_posicion_pulgadas",      InspeccionAib::getS3KeyGraficoPosicionPulgadas),
            Map.entry("grafico_velocidad_pulgadas",     InspeccionAib::getS3KeyGraficoVelocidadPulgadas),
            Map.entry("grafico_aceleracion_pulgadas",   InspeccionAib::getS3KeyGraficoAceleracionPulgadas),
            Map.entry("detecciones",                    InspeccionAib::getS3KeyDetecciones),
            Map.entry("posiciones_pulgadas",            InspeccionAib::getS3KeyPosicionesPulgadas),
            Map.entry("posiciones_tam",                 InspeccionAib::getS3KeyPosicionesTam),
            Map.entry("velocidad_aceleracion_pulgadas", InspeccionAib::getS3KeyVelocidadAceleracionPulgadas)
    );

    @Autowired
    private IInspeccionAibBusiness business;

    @Autowired
    private com.gestion.qnt.repository.InspeccionAibRepository inspeccionAibRepository;

    @Autowired
    private AwsS3Service awsS3Service;

    @Value("${app.aib.api-key}")
    private String apiKey;

    // ─────────────────────────────────────────────
    // POST — recibe inspección del pipeline externo (X-API-Key, JSON puro)
    // ─────────────────────────────────────────────
    @PostMapping(value = "", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Transactional
    public ResponseEntity<?> receiveInspeccion(
            @RequestHeader(value = "X-API-Key", required = false) String headerKey,
            @RequestBody String datosJson) {

        boolean apiKeyValida = !apiKey.isBlank() && MessageDigest.isEqual(
                apiKey.getBytes(StandardCharsets.UTF_8),
                (headerKey != null ? headerKey : "").getBytes(StandardCharsets.UTF_8)
        );
        if (!apiKeyValida) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            InspeccionAib saved = business.receiveInspeccion(datosJson);
            return ResponseEntity.status(HttpStatus.CREATED).body(toDTO(saved));
        } catch (BusinessException e) {
            log.warn("Inspección AIB rechazada: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

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
    // GET /inspecciones/aib/{id}/archivo/{tipo}
    // 302 redirect a una presigned URL de S3 (TTL: configurado en AwsS3Service)
    // ─────────────────────────────────────────────
    @GetMapping("/{id}/archivo/{tipo}")
    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getArchivo(@PathVariable Long id, @PathVariable String tipo) {
        Function<InspeccionAib, String> accessor = TIPO_TO_KEY.get(tipo);
        if (accessor == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "tipo de archivo desconocido: " + tipo));
        }

        InspeccionAib insp;
        try {
            insp = business.load(id);
        } catch (NotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (BusinessException e) {
            return ResponseEntity.internalServerError().build();
        }

        String key = accessor.apply(insp);
        if (key == null || key.isBlank()) {
            return ResponseEntity.notFound().build();
        }

        try {
            URL url = awsS3Service.generatePresignedGetUrl(insp.getS3Bucket(), key);
            return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(url.toString())).build();
        } catch (Exception e) {
            log.error("Error generando presigned URL para inspección {} tipo {}", id, tipo, e);
            return ResponseEntity.internalServerError().body(Map.of("error", "No se pudo generar URL de descarga"));
        }
    }

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

    // ─────────────────────────────────────────────
    // Mapping entity → DTO
    // ─────────────────────────────────────────────
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
            dto.modelo = i.getAib().getModelo();
            dto.notas = i.getAib().getNotas();
            if (i.getAib().getPozo() != null) {
                dto.pozoId = i.getAib().getPozo().getId();
                dto.pozoNombre = i.getAib().getPozo().getNombre();
            }
        }

        if (anyNotNull(i.getTcSubidaS(), i.getTcBajadaS(), i.getTcSubidaInS(),
                       i.getTcBajadaInS(), i.getTcRatio(), i.getTcConfianza())) {
            InspeccionAibDTO.TiemposCicloDTO tc = new InspeccionAibDTO.TiemposCicloDTO();
            tc.subidaS    = i.getTcSubidaS();
            tc.bajadaS    = i.getTcBajadaS();
            tc.subidaInS  = i.getTcSubidaInS();
            tc.bajadaInS  = i.getTcBajadaInS();
            tc.ratio      = i.getTcRatio();
            tc.confianza  = i.getTcConfianza();
            dto.tiemposCiclo = tc;
        }

        if (anyNotNull(i.getVelMaxInS(), i.getVelRmsInS(), i.getVelConfianza())) {
            InspeccionAibDTO.VelocidadDTO v = new InspeccionAibDTO.VelocidadDTO();
            v.velMaxInS = i.getVelMaxInS();
            v.velRmsInS = i.getVelRmsInS();
            v.confianza = i.getVelConfianza();
            dto.velocidad = v;
        }

        if (i.getAcelMaxInS2() != null) {
            InspeccionAibDTO.AceleracionDTO a = new InspeccionAibDTO.AceleracionDTO();
            a.acelMaxInS2 = i.getAcelMaxInS2();
            dto.aceleracion = a;
        }

        if (anyNotNull(i.getConvCarreraIn(), i.getConvCarreraPx(),
                       i.getConvScaleInPerPx(), i.getConvConfianza())) {
            InspeccionAibDTO.ConversionDTO c = new InspeccionAibDTO.ConversionDTO();
            c.carreraIn      = i.getConvCarreraIn();
            c.carreraPx      = i.getConvCarreraPx();
            c.scaleInPerPx   = i.getConvScaleInPerPx();
            c.confianza      = i.getConvConfianza();
            dto.conversion = c;
        }

        if (i.getVideoNombre() != null) {
            InspeccionAibDTO.VideoDTO v = new InspeccionAibDTO.VideoDTO();
            v.nombre              = i.getVideoNombre();
            v.fps                 = i.getVideoFps();
            v.duracionSegundos    = i.getVideoDuracionSegundos();
            v.framesConDeteccion  = i.getVideoFramesConDeteccion();
            v.framesTotales       = i.getVideoFramesTotales();
            v.coberturaPorcentaje = i.getVideoCoberturaPorcentaje();
            dto.video = v;
        }

        // Mapa archivos: tipo → URL relativa del backend (que hace 302 a S3)
        Map<String, String> archivos = new LinkedHashMap<>();
        for (Map.Entry<String, Function<InspeccionAib, String>> entry : TIPO_TO_KEY.entrySet()) {
            String key = entry.getValue().apply(i);
            if (key != null && !key.isBlank()) {
                archivos.put(entry.getKey(),
                        ApiConstants.URL_BASE + "/inspecciones/aib/" + i.getId() + "/archivo/" + entry.getKey());
            }
        }
        dto.archivos = archivos;

        return dto;
    }

    private static boolean anyNotNull(Object... values) {
        for (Object v : values) if (v != null) return true;
        return false;
    }
}
