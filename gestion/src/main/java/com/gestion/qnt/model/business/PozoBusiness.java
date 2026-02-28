package com.gestion.qnt.model.business;

import com.gestion.qnt.model.Pozo;
import com.gestion.qnt.model.business.exceptions.BusinessException;
import com.gestion.qnt.model.business.exceptions.NotFoundException;
import com.gestion.qnt.model.business.interfaces.IPozoBusiness;
import com.gestion.qnt.repository.PozoRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class PozoBusiness implements IPozoBusiness {

    @Autowired
    private PozoRepository repository;

    @Override
    public List<Pozo> list() throws BusinessException {
        try {
            return repository.findAll();
        } catch (Exception e) {
            log.error("Error al listar pozos", e);
            throw new BusinessException("Error al listar pozos", e);
        }
    }

    @Override
    public Pozo load(Long id) throws NotFoundException, BusinessException {
        try {
            return repository.findById(id)
                    .orElseThrow(() -> new NotFoundException("No existe Pozo con id " + id));
        } catch (NotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error al cargar pozo con id {}", id, e);
            throw new BusinessException("Error al cargar pozo", e);
        }
    }

    @Override
    public Pozo add(Pozo entity) throws BusinessException {
        try {
            return repository.save(entity);
        } catch (Exception e) {
            log.error("Error al agregar pozo", e);
            throw new BusinessException("Error al agregar pozo", e);
        }
    }

    @Override
    public Pozo update(Pozo entity) throws NotFoundException, BusinessException {
        try {
            load(entity.getId());
            return repository.save(entity);
        } catch (NotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error al actualizar pozo con id {}", entity.getId(), e);
            throw new BusinessException("Error al actualizar pozo", e);
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
            log.error("Error al eliminar pozo con id {}", id, e);
            throw new BusinessException("Error al eliminar pozo", e);
        }
    }
}
