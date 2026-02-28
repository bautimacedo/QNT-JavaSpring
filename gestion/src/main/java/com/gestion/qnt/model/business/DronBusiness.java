package com.gestion.qnt.model.business;

import com.gestion.qnt.model.Dron;
import com.gestion.qnt.model.business.exceptions.BusinessException;
import com.gestion.qnt.model.business.exceptions.NotFoundException;
import com.gestion.qnt.model.business.interfaces.IDronBusiness;
import com.gestion.qnt.repository.DronRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class DronBusiness implements IDronBusiness {

    @Autowired
    private DronRepository repository;

    @Override
    public List<Dron> list() throws BusinessException {
        try {
            return repository.findAll();
        } catch (Exception e) {
            log.error("Error al listar drones", e);
            throw new BusinessException("Error al listar drones", e);
        }
    }

    @Override
    public Dron load(Long id) throws NotFoundException, BusinessException {
        try {
            return repository.findById(id)
                    .orElseThrow(() -> new NotFoundException("No existe Dron con id " + id));
        } catch (NotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error al cargar dron con id {}", id, e);
            throw new BusinessException("Error al cargar dron", e);
        }
    }

    @Override
    public Dron add(Dron entity) throws BusinessException {
        try {
            return repository.save(entity);
        } catch (Exception e) {
            log.error("Error al agregar dron", e);
            throw new BusinessException("Error al agregar dron", e);
        }
    }

    @Override
    public Dron update(Dron entity) throws NotFoundException, BusinessException {
        try {
            load(entity.getId());
            return repository.save(entity);
        } catch (NotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error al actualizar dron con id {}", entity.getId(), e);
            throw new BusinessException("Error al actualizar dron", e);
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
            log.error("Error al eliminar dron con id {}", id, e);
            throw new BusinessException("Error al eliminar dron", e);
        }
    }
}
