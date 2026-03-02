package com.gestion.qnt.model.business;

import com.gestion.qnt.model.Proveedor;
import com.gestion.qnt.model.business.exceptions.BusinessException;
import com.gestion.qnt.model.business.exceptions.NotFoundException;
import com.gestion.qnt.model.business.interfaces.IProveedorBusiness;
import com.gestion.qnt.repository.ProveedorRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class ProveedorBusiness implements IProveedorBusiness {

    @Autowired
    private ProveedorRepository repository;

    @Override
    public List<Proveedor> list() throws BusinessException {
        try {
            return repository.findAll();
        } catch (Exception e) {
            log.error("Error al listar proveedores", e);
            throw new BusinessException("Error al listar proveedores", e);
        }
    }

    @Override
    public Proveedor load(Long id) throws NotFoundException, BusinessException {
        try {
            return repository.findById(id)
                    .orElseThrow(() -> new NotFoundException("No existe Proveedor con id " + id));
        } catch (NotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error al cargar proveedor con id {}", id, e);
            throw new BusinessException("Error al cargar proveedor", e);
        }
    }

    @Override
    public Proveedor loadOrCreate(String nombre) throws BusinessException {
        if (nombre == null || nombre.isBlank()) {
            throw new BusinessException("El nombre del proveedor no puede estar vacío");
        }
        try {
            return repository.findFirstByNombreIgnoreCase(nombre.trim())
                    .orElseGet(() -> {
                        Proveedor nuevo = new Proveedor();
                        nuevo.setNombre(nombre.trim());
                        return repository.save(nuevo);
                    });
        } catch (Exception e) {
            log.error("Error en loadOrCreate proveedor con nombre '{}'", nombre, e);
            throw new BusinessException("Error al obtener o crear proveedor", e);
        }
    }

    @Override
    public Proveedor add(Proveedor entity) throws BusinessException {
        try {
            return repository.save(entity);
        } catch (Exception e) {
            log.error("Error al agregar proveedor", e);
            throw new BusinessException("Error al agregar proveedor", e);
        }
    }

    @Override
    public Proveedor update(Proveedor entity) throws NotFoundException, BusinessException {
        try {
            load(entity.getId());
            return repository.save(entity);
        } catch (NotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error al actualizar proveedor con id {}", entity.getId(), e);
            throw new BusinessException("Error al actualizar proveedor", e);
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
            log.error("Error al eliminar proveedor con id {}", id, e);
            throw new BusinessException("Error al eliminar proveedor", e);
        }
    }
}
