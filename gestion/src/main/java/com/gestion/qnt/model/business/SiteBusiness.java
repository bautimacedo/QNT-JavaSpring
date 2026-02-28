package com.gestion.qnt.model.business;

import com.gestion.qnt.model.Site;
import com.gestion.qnt.model.business.exceptions.BusinessException;
import com.gestion.qnt.model.business.exceptions.NotFoundException;
import com.gestion.qnt.model.business.interfaces.ISiteBusiness;
import com.gestion.qnt.repository.SiteRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class SiteBusiness implements ISiteBusiness {

    @Autowired
    private SiteRepository repository;

    @Override
    public List<Site> list() throws BusinessException {
        try {
            return repository.findAll();
        } catch (Exception e) {
            log.error("Error al listar sites", e);
            throw new BusinessException("Error al listar sites", e);
        }
    }

    @Override
    public Site load(Long id) throws NotFoundException, BusinessException {
        try {
            return repository.findById(id)
                    .orElseThrow(() -> new NotFoundException("No existe Site con id " + id));
        } catch (NotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error al cargar site con id {}", id, e);
            throw new BusinessException("Error al cargar site", e);
        }
    }

    @Override
    public Site add(Site entity) throws BusinessException {
        try {
            return repository.save(entity);
        } catch (Exception e) {
            log.error("Error al agregar site", e);
            throw new BusinessException("Error al agregar site", e);
        }
    }

    @Override
    public Site update(Site entity) throws NotFoundException, BusinessException {
        try {
            load(entity.getId());
            return repository.save(entity);
        } catch (NotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error al actualizar site con id {}", entity.getId(), e);
            throw new BusinessException("Error al actualizar site", e);
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
            log.error("Error al eliminar site con id {}", id, e);
            throw new BusinessException("Error al eliminar site", e);
        }
    }
}
