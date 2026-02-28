package com.gestion.qnt.model.business;

import com.gestion.qnt.model.Instalacion;
import com.gestion.qnt.model.business.exceptions.BusinessException;
import com.gestion.qnt.model.business.exceptions.NotFoundException;
import com.gestion.qnt.model.business.interfaces.IInstalacionBusiness;
import com.gestion.qnt.repository.InstalacionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class InstalacionBusiness implements IInstalacionBusiness {

    @Autowired
    private InstalacionRepository repository;

    @Override
    public List<Instalacion> list() throws BusinessException {
        try {
            return repository.findAll();
        } catch (Exception e) {
            log.error("Error al listar instalaciones", e);
            throw new BusinessException("Error al listar instalaciones", e);
        }
    }

    @Override
    public Instalacion load(Long id) throws NotFoundException, BusinessException {
        try {
            return repository.findById(id)
                    .orElseThrow(() -> new NotFoundException("No existe Instalacion con id " + id));
        } catch (NotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error al cargar instalacion con id {}", id, e);
            throw new BusinessException("Error al cargar instalacion", e);
        }
    }

    @Override
    public Instalacion add(Instalacion entity) throws BusinessException {
        try {
            return repository.save(entity);
        } catch (Exception e) {
            log.error("Error al agregar instalacion", e);
            throw new BusinessException("Error al agregar instalacion", e);
        }
    }

    @Override
    public Instalacion update(Instalacion entity) throws NotFoundException, BusinessException {
        try {
            load(entity.getId());
            return repository.save(entity);
        } catch (NotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error al actualizar instalacion con id {}", entity.getId(), e);
            throw new BusinessException("Error al actualizar instalacion", e);
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
            log.error("Error al eliminar instalacion con id {}", id, e);
            throw new BusinessException("Error al eliminar instalacion", e);
        }
    }
}
