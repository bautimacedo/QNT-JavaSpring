package com.gestion.qnt.controller;

import com.gestion.qnt.config.ApiConstants;
import com.gestion.qnt.controller.dto.AibDTO;
import com.gestion.qnt.model.Aib;
import com.gestion.qnt.model.business.exceptions.BusinessException;
import com.gestion.qnt.model.business.exceptions.NotFoundException;
import com.gestion.qnt.model.business.interfaces.IAibBusiness;
import com.gestion.qnt.repository.InspeccionAibRepository;
import com.gestion.qnt.repository.PozoRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(ApiConstants.URL_BASE + "/aib")
@Slf4j
public class AibRestController {

    @Autowired
    private IAibBusiness aibBusiness;

    @Autowired
    private InspeccionAibRepository inspeccionAibRepository;

    @Autowired
    private PozoRepository pozoRepository;

    @GetMapping("")
    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<AibDTO>> list() {
        try {
            List<Aib> aibs = aibBusiness.list();
            return ResponseEntity.ok(aibs.stream().map(a -> {
                AibDTO dto = toDTO(a);
                inspeccionAibRepository.findLatestByAibId(a.getAibId()).ifPresent(i -> {
                    dto.ultimaInspeccion = i.getTimestamp();
                    dto.ultimoEstado = i.getEstado();
                    dto.ultimoGpm = i.getGpm();
                });
                return dto;
            }).toList());
        } catch (BusinessException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AibDTO> getById(@PathVariable Long id) {
        try {
            Aib aib = aibBusiness.load(id);
            AibDTO dto = toDTO(aib);
            inspeccionAibRepository.findLatestByAibId(aib.getAibId()).ifPresent(i -> {
                dto.ultimaInspeccion = i.getTimestamp();
                dto.ultimoEstado = i.getEstado();
                dto.ultimoGpm = i.getGpm();
            });
            return ResponseEntity.ok(dto);
        } catch (NotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (BusinessException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PutMapping("/{id}")
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AibDTO> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        try {
            Aib entity = new Aib();
            entity.setId(id);
            if (body.containsKey("nombre")) entity.setNombre((String) body.get("nombre"));
            if (body.containsKey("pozoId") && body.get("pozoId") != null) {
                Long pozoId = Long.valueOf(body.get("pozoId").toString());
                pozoRepository.findById(pozoId).ifPresent(p -> {
                    entity.setPozo(p);
                });
            }
            Aib updated = aibBusiness.update(entity);
            return ResponseEntity.ok(toDTO(updated));
        } catch (NotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (BusinessException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @DeleteMapping("/{id}")
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        try {
            aibBusiness.delete(id);
            return ResponseEntity.noContent().build();
        } catch (NotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (BusinessException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    private AibDTO toDTO(Aib a) {
        AibDTO dto = new AibDTO();
        dto.id = a.getId();
        dto.aibId = a.getAibId();
        dto.nombre = a.getNombre();
        dto.fechaCreacion = a.getFechaCreacion();
        if (a.getPozo() != null) {
            dto.pozoId = a.getPozo().getId();
            dto.pozoNombre = a.getPozo().getNombre();
        }
        return dto;
    }
}
