package com.gestion.qnt.model.business;

import com.gestion.qnt.model.MantenimientoDock;
import com.gestion.qnt.model.business.exceptions.BusinessException;
import com.gestion.qnt.model.business.exceptions.NotFoundException;
import com.gestion.qnt.model.business.interfaces.IMantenimientoDockBusiness;
import com.gestion.qnt.repository.MantenimientoDockRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class MantenimientoDockBusiness implements IMantenimientoDockBusiness {

    @Autowired
    private MantenimientoDockRepository repository;

    @Override
    public List<MantenimientoDock> list() throws BusinessException {
        try {
            return repository.findAll();
        } catch (Exception e) {
            log.error("Error al listar mantenimientos dock", e);
            throw new BusinessException("Error al listar mantenimientos dock", e);
        }
    }

    @Override
    public MantenimientoDock load(Long id) throws NotFoundException, BusinessException {
        try {
            return repository.findById(id)
                    .orElseThrow(() -> new NotFoundException("No existe MantenimientoDock con id " + id));
        } catch (NotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error al cargar mantenimiento dock con id {}", id, e);
            throw new BusinessException("Error al cargar mantenimiento dock", e);
        }
    }

    @Override
    public MantenimientoDock add(MantenimientoDock entity) throws BusinessException {
        try {
            return repository.save(entity);
        } catch (Exception e) {
            log.error("Error al agregar mantenimiento dock", e);
            throw new BusinessException("Error al agregar mantenimiento dock", e);
        }
    }

    @Override
    public MantenimientoDock update(MantenimientoDock entity) throws NotFoundException, BusinessException {
        try {
            load(entity.getId());
            return repository.save(entity);
        } catch (NotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error al actualizar mantenimiento dock con id {}", entity.getId(), e);
            throw new BusinessException("Error al actualizar mantenimiento dock", e);
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
            log.error("Error al eliminar mantenimiento dock con id {}", id, e);
            throw new BusinessException("Error al eliminar mantenimiento dock", e);
        }
    }
}
