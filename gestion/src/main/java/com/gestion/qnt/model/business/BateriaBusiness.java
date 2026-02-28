package com.gestion.qnt.model.business;

import com.gestion.qnt.model.Bateria;
import com.gestion.qnt.model.business.exceptions.BusinessException;
import com.gestion.qnt.model.business.exceptions.NotFoundException;
import com.gestion.qnt.model.business.interfaces.IBateriaBusiness;
import com.gestion.qnt.repository.BateriaRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class BateriaBusiness implements IBateriaBusiness {

    @Autowired
    private BateriaRepository repository;

    @Override
    public List<Bateria> list() throws BusinessException {
        try {
            return repository.findAll();
        } catch (Exception e) {
            log.error("Error al listar baterias", e);
            throw new BusinessException("Error al listar baterias", e);
        }
    }

    @Override
    public Bateria load(Long id) throws NotFoundException, BusinessException {
        try {
            return repository.findById(id)
                    .orElseThrow(() -> new NotFoundException("No existe Bateria con id " + id));
        } catch (NotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error al cargar bateria con id {}", id, e);
            throw new BusinessException("Error al cargar bateria", e);
        }
    }

    @Override
    public Bateria add(Bateria entity) throws BusinessException {
        try {
            return repository.save(entity);
        } catch (Exception e) {
            log.error("Error al agregar bateria", e);
            throw new BusinessException("Error al agregar bateria", e);
        }
    }

    @Override
    public Bateria update(Bateria entity) throws NotFoundException, BusinessException {
        try {
            load(entity.getId());
            return repository.save(entity);
        } catch (NotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error al actualizar bateria con id {}", entity.getId(), e);
            throw new BusinessException("Error al actualizar bateria", e);
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
            log.error("Error al eliminar bateria con id {}", id, e);
            throw new BusinessException("Error al eliminar bateria", e);
        }
    }
}
