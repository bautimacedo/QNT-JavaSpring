package com.gestion.qnt.model.business;

import com.gestion.qnt.model.AntenaRtk;
import com.gestion.qnt.model.business.exceptions.BusinessException;
import com.gestion.qnt.model.business.exceptions.NotFoundException;
import com.gestion.qnt.model.business.interfaces.IAntenaRtkBusiness;
import com.gestion.qnt.repository.AntenaRtkRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class AntenaRtkBusiness implements IAntenaRtkBusiness {

    @Autowired
    private AntenaRtkRepository repository;

    @Override
    public List<AntenaRtk> list() throws BusinessException {
        try {
            return repository.findAll();
        } catch (Exception e) {
            log.error("Error al listar antenas RTK", e);
            throw new BusinessException("Error al listar antenas RTK", e);
        }
    }

    @Override
    public AntenaRtk load(Long id) throws NotFoundException, BusinessException {
        try {
            return repository.findById(id)
                    .orElseThrow(() -> new NotFoundException("No existe AntenaRtk con id " + id));
        } catch (NotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error al cargar antena RTK con id {}", id, e);
            throw new BusinessException("Error al cargar antena RTK", e);
        }
    }

    @Override
    public AntenaRtk add(AntenaRtk entity) throws BusinessException {
        try {
            return repository.save(entity);
        } catch (Exception e) {
            log.error("Error al agregar antena RTK", e);
            throw new BusinessException("Error al agregar antena RTK", e);
        }
    }

    @Override
    public AntenaRtk update(AntenaRtk entity) throws NotFoundException, BusinessException {
        try {
            load(entity.getId());
            return repository.save(entity);
        } catch (NotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error al actualizar antena RTK con id {}", entity.getId(), e);
            throw new BusinessException("Error al actualizar antena RTK", e);
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
            log.error("Error al eliminar antena RTK con id {}", id, e);
            throw new BusinessException("Error al eliminar antena RTK", e);
        }
    }
}
