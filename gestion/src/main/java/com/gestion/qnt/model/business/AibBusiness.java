package com.gestion.qnt.model.business;

import com.gestion.qnt.model.Aib;
import com.gestion.qnt.model.business.exceptions.BusinessException;
import com.gestion.qnt.model.business.exceptions.NotFoundException;
import com.gestion.qnt.model.business.interfaces.IAibBusiness;
import com.gestion.qnt.repository.AibRepository;
import com.gestion.qnt.repository.PozoRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class AibBusiness implements IAibBusiness {

    @Autowired
    private AibRepository repository;

    @Autowired
    private PozoRepository pozoRepository;

    @Override
    public List<Aib> list() throws BusinessException {
        try {
            return repository.findAllWithPozo();
        } catch (Exception e) {
            log.error("Error al listar AIBs", e);
            throw new BusinessException("Error al listar AIBs", e);
        }
    }

    @Override
    public Aib load(Long id) throws NotFoundException, BusinessException {
        try {
            return repository.findById(id)
                    .orElseThrow(() -> new NotFoundException("No existe AIB con id " + id));
        } catch (NotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error al cargar AIB con id {}", id, e);
            throw new BusinessException("Error al cargar AIB", e);
        }
    }

    @Override
    public Aib loadByAibId(String aibId) throws NotFoundException, BusinessException {
        try {
            return repository.findByAibId(aibId)
                    .orElseThrow(() -> new NotFoundException("No existe AIB con aibId " + aibId));
        } catch (NotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error al cargar AIB con aibId {}", aibId, e);
            throw new BusinessException("Error al cargar AIB", e);
        }
    }

    @Override
    public Aib findOrCreate(String aibId) throws BusinessException {
        try {
            return repository.findByAibId(aibId).orElseGet(() -> {
                Aib nuevo = new Aib();
                nuevo.setAibId(aibId);
                nuevo.setNombre(aibId);
                // Intentar vincular automáticamente al Pozo por nombre coincidente
                pozoRepository.findByNombreIgnoreCase(aibId).ifPresent(nuevo::setPozo);
                return repository.save(nuevo);
            });
        } catch (Exception e) {
            log.error("Error al buscar/crear AIB con aibId {}", aibId, e);
            throw new BusinessException("Error al buscar o crear AIB", e);
        }
    }

    @Override
    public Aib update(Aib entity) throws NotFoundException, BusinessException {
        try {
            Aib existing = load(entity.getId());
            if (entity.getNombre() != null) existing.setNombre(entity.getNombre());
            if (entity.getPozo() != null && entity.getPozo().getId() != null) {
                pozoRepository.findById(entity.getPozo().getId())
                        .ifPresent(existing::setPozo);
            }
            return repository.save(existing);
        } catch (NotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error al actualizar AIB con id {}", entity.getId(), e);
            throw new BusinessException("Error al actualizar AIB", e);
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
            log.error("Error al eliminar AIB con id {}", id, e);
            throw new BusinessException("Error al eliminar AIB", e);
        }
    }
}
