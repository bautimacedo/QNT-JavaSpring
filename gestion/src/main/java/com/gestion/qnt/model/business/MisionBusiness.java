package com.gestion.qnt.model.business;

import com.gestion.qnt.model.Mision;
import com.gestion.qnt.model.business.exceptions.BusinessException;
import com.gestion.qnt.model.business.exceptions.NotFoundException;
import com.gestion.qnt.model.business.interfaces.IMisionBusiness;
import com.gestion.qnt.repository.MisionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class MisionBusiness implements IMisionBusiness {

    @Autowired
    private MisionRepository repository;

    @Override
    public List<Mision> list() throws BusinessException {
        try {
            return repository.findAll();
        } catch (Exception e) {
            log.error("Error al listar misiones", e);
            throw new BusinessException("Error al listar misiones", e);
        }
    }

    @Override
    public Mision load(Long id) throws NotFoundException, BusinessException {
        try {
            return repository.findById(id)
                    .orElseThrow(() -> new NotFoundException("No existe Mision con id " + id));
        } catch (NotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error al cargar mision con id {}", id, e);
            throw new BusinessException("Error al cargar mision", e);
        }
    }

    @Override
    public Mision add(Mision entity) throws BusinessException {
        try {
            return repository.save(entity);
        } catch (Exception e) {
            log.error("Error al agregar mision", e);
            throw new BusinessException("Error al agregar mision", e);
        }
    }

    @Override
    public Mision update(Mision entity) throws NotFoundException, BusinessException {
        try {
            load(entity.getId());
            return repository.save(entity);
        } catch (NotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error al actualizar mision con id {}", entity.getId(), e);
            throw new BusinessException("Error al actualizar mision", e);
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
            log.error("Error al eliminar mision con id {}", id, e);
            throw new BusinessException("Error al eliminar mision", e);
        }
    }
}
