package com.gestion.qnt.model.business;

import com.gestion.qnt.model.Accesorio;
import com.gestion.qnt.model.business.interfaces.IAccesorioBusiness;
import com.gestion.qnt.model.business.exceptions.BusinessException;
import com.gestion.qnt.model.business.exceptions.NotFoundException;
import com.gestion.qnt.repository.AccesorioRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@Slf4j
public class AccesorioBusiness implements IAccesorioBusiness {

    @Autowired
    private AccesorioRepository repository;

    @Override
    public List<Accesorio> list() throws BusinessException {
        try {
            return repository.findAll();
        } catch (Exception e) {
            log.error("Error al listar accesorios", e);
            throw new BusinessException("Error al listar accesorios", e);
        }
    }

    @Override
    public Accesorio load(Long id) throws NotFoundException, BusinessException {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("No existe accesorio con id " + id));
    }

    @Override
    public Accesorio add(Accesorio entity) throws BusinessException {
        try {
            return repository.save(entity);
        } catch (Exception e) {
            log.error("Error al agregar accesorio", e);
            throw new BusinessException("Error al agregar accesorio", e);
        }
    }

    @Override
    public Accesorio update(Accesorio entity) throws NotFoundException, BusinessException {
        load(entity.getId());
        try {
            return repository.save(entity);
        } catch (Exception e) {
            log.error("Error al actualizar accesorio", e);
            throw new BusinessException("Error al actualizar accesorio", e);
        }
    }

    @Override
    public void delete(Long id) throws NotFoundException, BusinessException {
        load(id);
        try {
            repository.deleteById(id);
        } catch (Exception e) {
            log.error("Error al eliminar accesorio", e);
            throw new BusinessException("Error al eliminar accesorio", e);
        }
    }
}