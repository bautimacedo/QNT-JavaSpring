package com.gestion.qnt.model.business;

import com.gestion.qnt.model.MantenimientoDron;
import com.gestion.qnt.model.business.exceptions.BusinessException;
import com.gestion.qnt.model.business.exceptions.NotFoundException;
import com.gestion.qnt.model.business.interfaces.IMantenimientoDronBusiness;
import com.gestion.qnt.repository.MantenimientoDronRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class MantenimientoDronBusiness implements IMantenimientoDronBusiness {

    @Autowired
    private MantenimientoDronRepository repository;

    @Override
    public List<MantenimientoDron> list() throws BusinessException {
        try {
            return repository.findAll();
        } catch (Exception e) {
            log.error("Error al listar mantenimientos dron", e);
            throw new BusinessException("Error al listar mantenimientos dron", e);
        }
    }

    @Override
    public MantenimientoDron load(Long id) throws NotFoundException, BusinessException {
        try {
            return repository.findById(id)
                    .orElseThrow(() -> new NotFoundException("No existe MantenimientoDron con id " + id));
        } catch (NotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error al cargar mantenimiento dron con id {}", id, e);
            throw new BusinessException("Error al cargar mantenimiento dron", e);
        }
    }

    @Override
    public MantenimientoDron add(MantenimientoDron entity) throws BusinessException {
        try {
            return repository.save(entity);
        } catch (Exception e) {
            log.error("Error al agregar mantenimiento dron", e);
            throw new BusinessException("Error al agregar mantenimiento dron", e);
        }
    }

    @Override
    public MantenimientoDron update(MantenimientoDron entity) throws NotFoundException, BusinessException {
        try {
            load(entity.getId());
            return repository.save(entity);
        } catch (NotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error al actualizar mantenimiento dron con id {}", entity.getId(), e);
            throw new BusinessException("Error al actualizar mantenimiento dron", e);
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
            log.error("Error al eliminar mantenimiento dron con id {}", id, e);
            throw new BusinessException("Error al eliminar mantenimiento dron", e);
        }
    }
}
