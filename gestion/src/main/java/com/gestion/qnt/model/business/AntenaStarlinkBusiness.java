package com.gestion.qnt.model.business;

import com.gestion.qnt.model.AntenaStarlink;
import com.gestion.qnt.model.business.exceptions.BusinessException;
import com.gestion.qnt.model.business.exceptions.NotFoundException;
import com.gestion.qnt.model.business.interfaces.IAntenaStarlinkBusiness;
import com.gestion.qnt.repository.AntenaStarlinkRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class AntenaStarlinkBusiness implements IAntenaStarlinkBusiness {

    @Autowired
    private AntenaStarlinkRepository repository;

    @Override
    public List<AntenaStarlink> list() throws BusinessException {
        try {
            return repository.findAll();
        } catch (Exception e) {
            log.error("Error al listar antenas Starlink", e);
            throw new BusinessException("Error al listar antenas Starlink", e);
        }
    }

    @Override
    public AntenaStarlink load(Long id) throws NotFoundException, BusinessException {
        try {
            return repository.findById(id)
                    .orElseThrow(() -> new NotFoundException("No existe AntenaStarlink con id " + id));
        } catch (NotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error al cargar antena Starlink con id {}", id, e);
            throw new BusinessException("Error al cargar antena Starlink", e);
        }
    }

    @Override
    public AntenaStarlink add(AntenaStarlink entity) throws BusinessException {
        try {
            return repository.save(entity);
        } catch (Exception e) {
            log.error("Error al agregar antena Starlink", e);
            throw new BusinessException("Error al agregar antena Starlink", e);
        }
    }

    @Override
    public AntenaStarlink update(AntenaStarlink entity) throws NotFoundException, BusinessException {
        try {
            load(entity.getId());
            return repository.save(entity);
        } catch (NotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error al actualizar antena Starlink con id {}", entity.getId(), e);
            throw new BusinessException("Error al actualizar antena Starlink", e);
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
            log.error("Error al eliminar antena Starlink con id {}", id, e);
            throw new BusinessException("Error al eliminar antena Starlink", e);
        }
    }
}
