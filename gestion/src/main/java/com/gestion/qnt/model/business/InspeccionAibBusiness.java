package com.gestion.qnt.model.business;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.gestion.qnt.model.Aib;
import com.gestion.qnt.model.InspeccionAib;
import com.gestion.qnt.model.business.exceptions.BusinessException;
import com.gestion.qnt.model.business.exceptions.NotFoundException;
import com.gestion.qnt.model.business.interfaces.IAibBusiness;
import com.gestion.qnt.model.business.interfaces.IInspeccionAibBusiness;
import com.gestion.qnt.repository.AibRepository;
import com.gestion.qnt.repository.InspeccionAibRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
@Slf4j
public class InspeccionAibBusiness implements IInspeccionAibBusiness {

    private static final Set<String> ESTADOS_VALIDOS = Set.of("ON", "OFF", "INDETERMINADO");

    @Autowired
    private InspeccionAibRepository repository;

    @Autowired
    private IAibBusiness aibBusiness;

    @Autowired
    private AibRepository aibRepository;

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    @Override
    public List<InspeccionAib> list() throws BusinessException {
        try {
            return repository.findAllWithAib();
        } catch (Exception e) {
            log.error("Error al listar inspecciones AIB", e);
            throw new BusinessException("Error al listar inspecciones AIB", e);
        }
    }

    @Override
    public InspeccionAib load(Long id) throws NotFoundException, BusinessException {
        try {
            return repository.findByIdWithAib(id)
                    .orElseThrow(() -> new NotFoundException("No existe inspección AIB con id " + id));
        } catch (NotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error al cargar inspección AIB con id {}", id, e);
            throw new BusinessException("Error al cargar inspección AIB", e);
        }
    }

    @Override
    public List<InspeccionAib> listByAibId(String aibId) throws BusinessException {
        try {
            return repository.findByAibIdStringOrderByTimestampDesc(aibId);
        } catch (Exception e) {
            log.error("Error al listar inspecciones del AIB {}", aibId, e);
            throw new BusinessException("Error al listar inspecciones del AIB", e);
        }
    }

    @Override
    public InspeccionAib receiveInspeccion(String datosJson) throws BusinessException {
        try {
            JsonNode root = MAPPER.readTree(datosJson);

            // ── 1. Campos obligatorios al nivel raíz ──────────────────────
            String aibId = root.path("aib_id").asText();
            if (aibId.isBlank()) throw new BusinessException("aib_id es requerido");

            String estado = root.path("estado").asText();
            if (estado.isBlank()) throw new BusinessException("estado es requerido");
            if (!ESTADOS_VALIDOS.contains(estado))
                throw new BusinessException("estado inválido: " + estado + " (esperado ON|OFF|INDETERMINADO)");

            String tsText = root.path("timestamp").asText(null);
            if (tsText == null || tsText.isBlank())
                throw new BusinessException("timestamp es requerido");
            LocalDateTime timestamp = parseTimestamp(tsText);

            // s3_bucket: si viene null, AwsS3Service usa el bucket por defecto al firmar URLs
            String s3Bucket = nodeToString(root, "s3_bucket");

            JsonNode archivos = root.path("archivos");
            if (archivos.isMissingNode() || !archivos.isObject())
                throw new BusinessException("archivos es requerido");

            // Archivos obligatorios dentro del dict
            requireFile(archivos, "video");
            requireFile(archivos, "captura");
            requireFile(archivos, "detecciones");
            requireFile(archivos, "grafico_detecciones_raw");

            JsonNode videoNode = root.path("video");
            if (videoNode.isMissingNode() || videoNode.isNull())
                throw new BusinessException("video es requerido");
            String videoNombre = nodeToString(videoNode, "nombre");
            if (videoNombre == null)
                throw new BusinessException("video.nombre es requerido");

            // ── 2. AIB: buscar o crear; actualizar modelo/notas si llegaron ──
            Aib aib = aibBusiness.findOrCreate(aibId);
            String modelo = nodeToString(root, "modelo");
            String notas  = nodeToString(root, "notas");
            boolean aibChanged = false;
            if (modelo != null && !Objects.equals(modelo, aib.getModelo())) {
                aib.setModelo(modelo);
                aibChanged = true;
            }
            if (notas != null && !Objects.equals(notas, aib.getNotas())) {
                aib.setNotas(notas);
                aibChanged = true;
            }
            if (aibChanged) aibRepository.save(aib);

            // ── 3. Construir y persistir InspeccionAib ─────────────────
            InspeccionAib insp = new InspeccionAib();
            insp.setAib(aib);
            insp.setTimestamp(timestamp);
            insp.setEstado(estado);
            insp.setGpm(nodeToDouble(root, "gpm"));
            insp.setS3Bucket(s3Bucket);

            JsonNode tc = root.path("tiempos_ciclo");
            insp.setTcSubidaS(nodeToDouble(tc, "subida_s"));
            insp.setTcBajadaS(nodeToDouble(tc, "bajada_s"));
            insp.setTcSubidaInS(nodeToDouble(tc, "subida_in_s"));
            insp.setTcBajadaInS(nodeToDouble(tc, "bajada_in_s"));
            insp.setTcRatio(nodeToDouble(tc, "ratio"));
            insp.setTcConfianza(nodeToDouble(tc, "confianza"));

            JsonNode vel = root.path("velocidad");
            insp.setVelMaxInS(nodeToDouble(vel, "vel_max_in_s"));
            insp.setVelRmsInS(nodeToDouble(vel, "vel_rms_in_s"));
            insp.setVelConfianza(nodeToDouble(vel, "confianza"));

            JsonNode acel = root.path("aceleracion");
            insp.setAcelMaxInS2(nodeToDouble(acel, "acel_max_in_s2"));

            JsonNode conv = root.path("conversion");
            insp.setConvCarreraIn(nodeToDouble(conv, "carrera_in"));
            insp.setConvCarreraPx(nodeToDouble(conv, "carrera_px"));
            insp.setConvScaleInPerPx(nodeToDouble(conv, "scale_in_per_px"));
            insp.setConvConfianza(nodeToDouble(conv, "confianza"));

            insp.setVideoNombre(videoNombre);
            insp.setVideoFps(nodeToDouble(videoNode, "fps"));
            insp.setVideoDuracionSegundos(nodeToDouble(videoNode, "duracion_segundos"));
            insp.setVideoFramesConDeteccion(nodeToInt(videoNode, "frames_con_deteccion"));
            insp.setVideoFramesTotales(nodeToInt(videoNode, "frames_totales"));
            insp.setVideoCoberturaPorcentaje(nodeToDouble(videoNode, "cobertura_porcentaje"));

            insp.setS3KeyVideo(nodeToString(archivos, "video"));
            insp.setS3KeyCaptura(nodeToString(archivos, "captura"));
            insp.setS3KeyGraficoDeteccionesRaw(nodeToString(archivos, "grafico_detecciones_raw"));
            insp.setS3KeyDetecciones(nodeToString(archivos, "detecciones"));
            insp.setS3KeyGraficoTiemposCiclo(nodeToString(archivos, "grafico_tiempos_ciclo"));
            insp.setS3KeyGraficoPosicionPulgadas(nodeToString(archivos, "grafico_posicion_pulgadas"));
            insp.setS3KeyGraficoVelocidadPulgadas(nodeToString(archivos, "grafico_velocidad_pulgadas"));
            insp.setS3KeyGraficoAceleracionPulgadas(nodeToString(archivos, "grafico_aceleracion_pulgadas"));
            insp.setS3KeyPosicionesPulgadas(nodeToString(archivos, "posiciones_pulgadas"));
            insp.setS3KeyPosicionesTam(nodeToString(archivos, "posiciones_tam"));
            insp.setS3KeyVelocidadAceleracionPulgadas(nodeToString(archivos, "velocidad_aceleracion_pulgadas"));

            return repository.save(insp);

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error procesando inspección AIB", e);
            throw new BusinessException("Error al procesar inspección AIB: " + e.getMessage(), e);
        }
    }

    @Override
    public void delete(Long id) throws NotFoundException, BusinessException {
        try {
            load(id);
            repository.deleteById(id);
        } catch (NotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error al eliminar inspección AIB con id {}", id, e);
            throw new BusinessException("Error al eliminar inspección AIB", e);
        }
    }

    // ── helpers de parseo ──────────────────────────────────────────────

    private static void requireFile(JsonNode archivos, String key) throws BusinessException {
        String val = nodeToString(archivos, key);
        if (val == null)
            throw new BusinessException("archivos." + key + " es requerido");
    }

    private static String nodeToString(JsonNode node, String field) {
        JsonNode n = node.path(field);
        if (n.isMissingNode() || n.isNull()) return null;
        String s = n.asText();
        return (s == null || s.isBlank()) ? null : s;
    }

    private static Double nodeToDouble(JsonNode node, String field) {
        JsonNode n = node.path(field);
        return (n.isMissingNode() || n.isNull()) ? null : n.asDouble();
    }

    private static Integer nodeToInt(JsonNode node, String field) {
        JsonNode n = node.path(field);
        return (n.isMissingNode() || n.isNull()) ? null : n.asInt();
    }

    /**
     * Acepta formatos comunes: LocalDateTime ISO sin offset, con offset, o Instant.
     */
    private static LocalDateTime parseTimestamp(String text) {
        try {
            return LocalDateTime.parse(text);
        } catch (Exception ignored) {
            try {
                return java.time.OffsetDateTime.parse(text).toLocalDateTime();
            } catch (Exception e2) {
                try {
                    return java.time.Instant.parse(text).atZone(java.time.ZoneId.systemDefault()).toLocalDateTime();
                } catch (Exception e3) {
                    throw new IllegalArgumentException("timestamp inválido: " + text);
                }
            }
        }
    }
}
