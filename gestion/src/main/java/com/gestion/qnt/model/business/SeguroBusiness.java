package com.gestion.qnt.model.business;

import com.gestion.qnt.model.Seguro;
import com.gestion.qnt.model.business.exceptions.BusinessException;
import com.gestion.qnt.model.business.exceptions.NotFoundException;
import com.gestion.qnt.model.business.interfaces.ISeguroBusiness;
import com.gestion.qnt.repository.SeguroRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class SeguroBusiness implements ISeguroBusiness {

    @Autowired
    private SeguroRepository repository;

    @Override
    public List<Seguro> list() throws BusinessException {
        try {
            return repository.findAll();
        } catch (Exception e) {
            log.error("Error al listar seguros", e);
            throw new BusinessException("Error al listar seguros", e);
        }
    }

    @Override
    public Seguro load(Long id) throws NotFoundException, BusinessException {
        try {
            return repository.findById(id)
                    .orElseThrow(() -> new NotFoundException("No existe Seguro con id " + id));
        } catch (NotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error al cargar seguro con id {}", id, e);
            throw new BusinessException("Error al cargar seguro", e);
        }
    }

    @Override
    public Seguro add(Seguro entity) throws BusinessException {
        try {
            return repository.save(entity);
        } catch (Exception e) {
            log.error("Error al agregar seguro", e);
            throw new BusinessException("Error al agregar seguro", e);
        }
    }

    @Override
    public Seguro update(Seguro entity) throws NotFoundException, BusinessException {
        try {
            load(entity.getId());
            return repository.save(entity);
        } catch (NotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error al actualizar seguro con id {}", entity.getId(), e);
            throw new BusinessException("Error al actualizar seguro", e);
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
            log.error("Error al eliminar seguro con id {}", id, e);
            throw new BusinessException("Error al eliminar seguro", e);
        }
    }
}
