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
import com.gestion.qnt.repository.InspeccionAibRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class InspeccionAibBusiness implements IInspeccionAibBusiness {

    @Autowired
    private InspeccionAibRepository repository;

    @Autowired
    private IAibBusiness aibBusiness;

    @Value("${app.aib.upload-dir:/var/lib/qnt/inspecciones}")
    private String uploadDir;

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
    public InspeccionAib receiveInspeccion(String datosJson, List<MultipartFile> graficos) throws BusinessException {
        try {
            JsonNode root = MAPPER.readTree(datosJson);

            String aibId = root.path("aib_id").asText();
            if (aibId.isBlank()) throw new BusinessException("aib_id es requerido en el JSON");

            Aib aib = aibBusiness.findOrCreate(aibId);

            InspeccionAib inspeccion = new InspeccionAib();
            inspeccion.setAib(aib);
            inspeccion.setEstado(root.path("estado").asText("OFF"));
            inspeccion.setGpm(nodeToDouble(root, "gpm"));

            String tsText = root.path("timestamp").asText(null);
            inspeccion.setTimestamp(tsText != null ? LocalDateTime.parse(tsText) : LocalDateTime.now());

            JsonNode vel = root.path("velocidad");
            inspeccion.setVelSubidaS(nodeToDouble(vel, "subida_s"));
            inspeccion.setVelBajadaS(nodeToDouble(vel, "bajada_s"));
            inspeccion.setVelSubidaInS(nodeToDouble(vel, "subida_in_s"));
            inspeccion.setVelBajadaInS(nodeToDouble(vel, "bajada_in_s"));
            inspeccion.setVelRatio(nodeToDouble(vel, "ratio"));
            inspeccion.setVelConfianza(nodeToDouble(vel, "confianza"));

            JsonNode dPx = root.path("derivada_px");
            inspeccion.setDerivadaVelMaxPxS(nodeToDouble(dPx, "vel_max_px_s"));
            inspeccion.setDerivadaVelRmsPxS(nodeToDouble(dPx, "vel_rms_px_s"));
            inspeccion.setDerivadaAcelMaxPxS2(nodeToDouble(dPx, "acel_max_px_s2"));
            inspeccion.setDerivadaConfianza(nodeToDouble(dPx, "confianza"));

            JsonNode conv = root.path("conversion");
            inspeccion.setConvCarreraIn(nodeToDouble(conv, "carrera_in"));
            inspeccion.setConvCarreraPx(nodeToDouble(conv, "carrera_px"));
            inspeccion.setConvScaleInPerPx(nodeToDouble(conv, "scale_in_per_px"));
            inspeccion.setConvConfianza(nodeToDouble(conv, "confianza"));

            JsonNode dIn = root.path("derivada_in");
            inspeccion.setDerivadaInVelMaxInS(nodeToDouble(dIn, "vel_max_in_s"));
            inspeccion.setDerivadaInVelRmsInS(nodeToDouble(dIn, "vel_rms_in_s"));
            inspeccion.setDerivadaInAcelMaxInS2(nodeToDouble(dIn, "acel_max_in_s2"));

            InspeccionAib saved = repository.save(inspeccion);

            if (graficos != null && !graficos.isEmpty()) {
                guardarGraficos(saved, graficos);
                saved = repository.save(saved);
            }

            return saved;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error al recibir inspección AIB", e);
            throw new BusinessException("Error al procesar inspección AIB", e);
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

    private void guardarGraficos(InspeccionAib inspeccion, List<MultipartFile> graficos) throws IOException {
        String aibId = inspeccion.getAib().getAibId();
        Long id = inspeccion.getId();
        Path dir = Paths.get(uploadDir, aibId, String.valueOf(id));
        Files.createDirectories(dir);

        for (MultipartFile file : graficos) {
            String name = file.getOriginalFilename();
            if (name == null || name.isBlank()) continue;
            Path dest = dir.resolve(name);
            file.transferTo(dest.toFile());

            String relativePath = aibId + "/" + id + "/" + name;
            if (name.startsWith("captura_anotada")) inspeccion.setCapturaAnotadaPath(relativePath);
            else if (name.startsWith("grafico_posicion_in")) inspeccion.setGraficoPosicionInPath(relativePath);
            else if (name.startsWith("grafico_procesada")) inspeccion.setGraficoProcesadaPath(relativePath);
            else if (name.startsWith("grafico_velocidad")) inspeccion.setGraficoVelocidadPath(relativePath);
            else if (name.startsWith("grafico_derivada_in")) inspeccion.setGraficoDerivadaInPath(relativePath);
            else if (name.startsWith("grafico_aceleracion_in")) inspeccion.setGraficoAceleracionInPath(relativePath);
        }
    }

    private Double nodeToDouble(JsonNode node, String field) {
        JsonNode n = node.path(field);
        return n.isMissingNode() || n.isNull() ? null : n.asDouble();
    }
}
