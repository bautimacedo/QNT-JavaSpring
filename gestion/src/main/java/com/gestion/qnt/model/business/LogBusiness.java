package com.gestion.qnt.model.business;

import com.gestion.qnt.model.Log;
import com.gestion.qnt.model.business.exceptions.BusinessException;
import com.gestion.qnt.model.business.exceptions.NotFoundException;
import com.gestion.qnt.model.business.interfaces.ILogBusiness;
import com.gestion.qnt.repository.LogRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class LogBusiness implements ILogBusiness {

    @Autowired
    private LogRepository repository;

    @Override
    public List<Log> list() throws BusinessException {
        try {
            return repository.findAll();
        } catch (Exception e) {
            log.error("Error al listar logs", e);
            throw new BusinessException("Error al listar logs", e);
        }
    }

    @Override
    public Log load(Long id) throws NotFoundException, BusinessException {
        try {
            return repository.findById(id)
                    .orElseThrow(() -> new NotFoundException("No existe Log con id " + id));
        } catch (NotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error al cargar log con id {}", id, e);
            throw new BusinessException("Error al cargar log", e);
        }
    }

    @Override
    public Log add(Log entity) throws BusinessException {
        try {
            return repository.save(entity);
        } catch (Exception e) {
            log.error("Error al agregar log", e);
            throw new BusinessException("Error al agregar log", e);
        }
    }

    @Override
    public Log update(Log entity) throws NotFoundException, BusinessException {
        try {
            load(entity.getId());
            return repository.save(entity);
        } catch (NotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error al actualizar log con id {}", entity.getId(), e);
            throw new BusinessException("Error al actualizar log", e);
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
            log.error("Error al eliminar log con id {}", id, e);
            throw new BusinessException("Error al eliminar log", e);
        }
    }
}
