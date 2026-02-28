package com.gestion.qnt.model.business;

import com.gestion.qnt.model.Role;
import com.gestion.qnt.model.business.exceptions.BusinessException;
import com.gestion.qnt.model.business.exceptions.FoundException;
import com.gestion.qnt.model.business.exceptions.NotFoundException;
import com.gestion.qnt.model.business.interfaces.IRoleBusiness;
import com.gestion.qnt.repository.RoleRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class RoleBusiness implements IRoleBusiness {

    @Autowired
    private RoleRepository repository;

    @Override
    public List<Role> list() throws BusinessException {
        try {
            return repository.findAll();
        } catch (Exception e) {
            log.error("Error al listar roles", e);
            throw new BusinessException("Error al listar roles", e);
        }
    }

    @Override
    public Role load(Long id) throws NotFoundException, BusinessException {
        try {
            return repository.findById(id)
                    .orElseThrow(() -> new NotFoundException("No existe Role con id " + id));
        } catch (NotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error al cargar role con id {}", id, e);
            throw new BusinessException("Error al cargar role", e);
        }
    }

    @Override
    public Role load(String codigo) throws NotFoundException, BusinessException {
        try {
            return repository.findByCodigo(codigo)
                    .orElseThrow(() -> new NotFoundException("No existe Role con codigo " + codigo));
        } catch (NotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error al cargar role con codigo {}", codigo, e);
            throw new BusinessException("Error al cargar role", e);
        }
    }

    @Override
    public Role add(Role entity) throws FoundException, BusinessException {
        try {
            if (entity.getCodigo() != null && repository.findByCodigo(entity.getCodigo()).isPresent()) {
                throw new FoundException("Ya existe un Role con codigo " + entity.getCodigo());
            }
            return repository.save(entity);
        } catch (FoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error al agregar role", e);
            throw new BusinessException("Error al agregar role", e);
        }
    }

    @Override
    public Role update(Role entity) throws FoundException, NotFoundException, BusinessException {
        try {
            load(entity.getId());
            if (entity.getCodigo() != null && repository.findByCodigoAndIdNot(entity.getCodigo(), entity.getId()).isPresent()) {
                throw new FoundException("Ya existe otro Role con codigo " + entity.getCodigo());
            }
            return repository.save(entity);
        } catch (FoundException | NotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error al actualizar role con id {}", entity.getId(), e);
            throw new BusinessException("Error al actualizar role", e);
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
            log.error("Error al eliminar role con id {}", id, e);
            throw new BusinessException("Error al eliminar role", e);
        }
    }
}
