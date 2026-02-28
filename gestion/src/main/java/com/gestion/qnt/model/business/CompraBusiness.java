package com.gestion.qnt.model.business;

import com.gestion.qnt.model.Compra;
import com.gestion.qnt.model.business.exceptions.BusinessException;
import com.gestion.qnt.model.business.exceptions.NotFoundException;
import com.gestion.qnt.model.business.interfaces.ICompraBusiness;
import com.gestion.qnt.repository.CompraRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class CompraBusiness implements ICompraBusiness {

    @Autowired
    private CompraRepository repository;

    @Override
    public List<Compra> list() throws BusinessException {
        try {
            return repository.findAll();
        } catch (Exception e) {
            log.error("Error al listar compras", e);
            throw new BusinessException("Error al listar compras", e);
        }
    }

    @Override
    public Compra load(Long id) throws NotFoundException, BusinessException {
        try {
            return repository.findById(id)
                    .orElseThrow(() -> new NotFoundException("No existe Compra con id " + id));
        } catch (NotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error al cargar compra con id {}", id, e);
            throw new BusinessException("Error al cargar compra", e);
        }
    }

    @Override
    public Compra add(Compra entity) throws BusinessException {
        try {
            return repository.save(entity);
        } catch (Exception e) {
            log.error("Error al agregar compra", e);
            throw new BusinessException("Error al agregar compra", e);
        }
    }

    @Override
    public Compra update(Compra entity) throws NotFoundException, BusinessException {
        try {
            load(entity.getId());
            return repository.save(entity);
        } catch (NotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error al actualizar compra con id {}", entity.getId(), e);
            throw new BusinessException("Error al actualizar compra", e);
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
            log.error("Error al eliminar compra con id {}", id, e);
            throw new BusinessException("Error al eliminar compra", e);
        }
    }
}
