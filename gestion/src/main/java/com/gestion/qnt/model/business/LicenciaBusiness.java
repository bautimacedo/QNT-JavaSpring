package com.gestion.qnt.model.business;

import com.gestion.qnt.model.Licencia;
import com.gestion.qnt.model.business.exceptions.BusinessException;
import com.gestion.qnt.model.business.exceptions.NotFoundException;
import com.gestion.qnt.model.business.interfaces.ILicenciaBusiness;
import com.gestion.qnt.repository.LicenciaRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class LicenciaBusiness implements ILicenciaBusiness {

    @Autowired
    private LicenciaRepository repository;

    @Override
    public List<Licencia> list() throws BusinessException {
        try {
            return repository.findAll();
        } catch (Exception e) {
            log.error("Error al listar licencias", e);
            throw new BusinessException("Error al listar licencias", e);
        }
    }

    @Override
    public Licencia load(Long id) throws NotFoundException, BusinessException {
        try {
            return repository.findById(id)
                    .orElseThrow(() -> new NotFoundException("No existe Licencia con id " + id));
        } catch (NotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error al cargar licencia con id {}", id, e);
            throw new BusinessException("Error al cargar licencia", e);
        }
    }

    @Override
    public Licencia add(Licencia entity) throws BusinessException {
        try {
            return repository.save(entity);
        } catch (Exception e) {
            log.error("Error al agregar licencia", e);
            throw new BusinessException("Error al agregar licencia", e);
        }
    }

    @Override
    public Licencia update(Licencia entity) throws NotFoundException, BusinessException {
        try {
            load(entity.getId());
            return repository.save(entity);
        } catch (NotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error al actualizar licencia con id {}", entity.getId(), e);
            throw new BusinessException("Error al actualizar licencia", e);
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
            log.error("Error al eliminar licencia con id {}", id, e);
            throw new BusinessException("Error al eliminar licencia", e);
        }
    }
}
