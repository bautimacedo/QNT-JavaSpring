package com.gestion.qnt.model.business;

import com.gestion.qnt.model.Dock;
import com.gestion.qnt.model.business.exceptions.BusinessException;
import com.gestion.qnt.model.business.exceptions.NotFoundException;
import com.gestion.qnt.model.business.interfaces.IDockBusiness;
import com.gestion.qnt.repository.DockRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class DockBusiness implements IDockBusiness {

    @Autowired
    private DockRepository repository;

    @Override
    public List<Dock> list() throws BusinessException {
        try {
            return repository.findAll();
        } catch (Exception e) {
            log.error("Error al listar docks", e);
            throw new BusinessException("Error al listar docks", e);
        }
    }

    @Override
    public Dock load(Long id) throws NotFoundException, BusinessException {
        try {
            return repository.findById(id)
                    .orElseThrow(() -> new NotFoundException("No existe Dock con id " + id));
        } catch (NotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error al cargar dock con id {}", id, e);
            throw new BusinessException("Error al cargar dock", e);
        }
    }

    @Override
    public Dock add(Dock entity) throws BusinessException {
        try {
            return repository.save(entity);
        } catch (Exception e) {
            log.error("Error al agregar dock", e);
            throw new BusinessException("Error al agregar dock", e);
        }
    }

    @Override
    public Dock update(Dock entity) throws NotFoundException, BusinessException {
        try {
            load(entity.getId());
            return repository.save(entity);
        } catch (NotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error al actualizar dock con id {}", entity.getId(), e);
            throw new BusinessException("Error al actualizar dock", e);
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
            log.error("Error al eliminar dock con id {}", id, e);
            throw new BusinessException("Error al eliminar dock", e);
        }
    }
}
