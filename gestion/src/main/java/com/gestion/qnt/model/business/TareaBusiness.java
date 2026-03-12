package com.gestion.qnt.model.business;

import com.gestion.qnt.model.Tarea;
import com.gestion.qnt.model.business.exceptions.BusinessException;
import com.gestion.qnt.model.business.exceptions.NotFoundException;
import com.gestion.qnt.model.business.interfaces.ITareaBusiness;
import com.gestion.qnt.repository.TareaRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class TareaBusiness implements ITareaBusiness {

    @Autowired
    private TareaRepository repository;

    @Override
    public List<Tarea> list() throws BusinessException {
        try {
            return repository.findAll();
        } catch (Exception e) {
            log.error("Error al listar tareas", e);
            throw new BusinessException("Error al listar tareas", e);
        }
    }

    @Override
    public Tarea load(Long id) throws NotFoundException, BusinessException {
        try {
            return repository.findById(id)
                    .orElseThrow(() -> new NotFoundException("No existe Tarea con id " + id));
        } catch (NotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error al cargar tarea con id {}", id, e);
            throw new BusinessException("Error al cargar tarea", e);
        }
    }

    @Override
    public Tarea add(Tarea entity) throws BusinessException {
        try {
            return repository.save(entity);
        } catch (Exception e) {
            log.error("Error al agregar tarea", e);
            throw new BusinessException("Error al agregar tarea", e);
        }
    }

    @Override
    public Tarea update(Tarea entity) throws NotFoundException, BusinessException {
        try {
            load(entity.getId());
            return repository.save(entity);
        } catch (NotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error al actualizar tarea con id {}", entity.getId(), e);
            throw new BusinessException("Error al actualizar tarea", e);
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
            log.error("Error al eliminar tarea con id {}", id, e);
            throw new BusinessException("Error al eliminar tarea", e);
        }
    }
}
