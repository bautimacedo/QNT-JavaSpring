package com.gestion.qnt.model.business;

import com.gestion.qnt.model.Helice;
import com.gestion.qnt.model.business.exceptions.BusinessException;
import com.gestion.qnt.model.business.exceptions.NotFoundException;
import com.gestion.qnt.model.business.interfaces.IHeliceBusiness;
import com.gestion.qnt.repository.HeliceRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class HeliceBusiness implements IHeliceBusiness {

    @Autowired
    private HeliceRepository repository;

    @Override
    public List<Helice> list() throws BusinessException {
        try {
            return repository.findAll();
        } catch (Exception e) {
            log.error("Error al listar helices", e);
            throw new BusinessException("Error al listar helices", e);
        }
    }

    @Override
    public Helice load(Long id) throws NotFoundException, BusinessException {
        try {
            return repository.findById(id)
                    .orElseThrow(() -> new NotFoundException("No existe Helice con id " + id));
        } catch (NotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error al cargar helice con id {}", id, e);
            throw new BusinessException("Error al cargar helice", e);
        }
    }

    @Override
    public Helice add(Helice entity) throws BusinessException {
        try {
            return repository.save(entity);
        } catch (Exception e) {
            log.error("Error al agregar helice", e);
            throw new BusinessException("Error al agregar helice", e);
        }
    }

    @Override
    public Helice update(Helice entity) throws NotFoundException, BusinessException {
        try {
            load(entity.getId());
            return repository.save(entity);
        } catch (NotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error al actualizar helice con id {}", entity.getId(), e);
            throw new BusinessException("Error al actualizar helice", e);
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
            log.error("Error al eliminar helice con id {}", id, e);
            throw new BusinessException("Error al eliminar helice", e);
        }
    }
}
