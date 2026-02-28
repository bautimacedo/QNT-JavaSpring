package com.gestion.qnt.model.business;

import com.gestion.qnt.model.Usuario;
import com.gestion.qnt.model.business.exceptions.BusinessException;
import com.gestion.qnt.model.business.exceptions.FoundException;
import com.gestion.qnt.model.business.exceptions.NotFoundException;
import com.gestion.qnt.model.business.interfaces.IUsuarioBusiness;
import com.gestion.qnt.repository.UsuarioRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class UsuarioBusiness implements IUsuarioBusiness {

    @Autowired
    private UsuarioRepository repository;

    @Override
    public List<Usuario> list() throws BusinessException {
        try {
            return repository.findAll();
        } catch (Exception e) {
            log.error("Error al listar usuarios", e);
            throw new BusinessException("Error al listar usuarios", e);
        }
    }

    @Override
    public Usuario load(Long id) throws NotFoundException, BusinessException {
        try {
            return repository.findById(id)
                    .orElseThrow(() -> new NotFoundException("No existe Usuario con id " + id));
        } catch (NotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error al cargar usuario con id {}", id, e);
            throw new BusinessException("Error al cargar usuario", e);
        }
    }

    @Override
    public Usuario load(String email) throws NotFoundException, BusinessException {
        try {
            return repository.findByEmail(email)
                    .orElseThrow(() -> new NotFoundException("No existe Usuario con email " + email));
        } catch (NotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error al cargar usuario con email {}", email, e);
            throw new BusinessException("Error al cargar usuario", e);
        }
    }

    @Override
    public Usuario add(Usuario entity) throws FoundException, BusinessException {
        try {
            if (entity.getEmail() != null && repository.findByEmail(entity.getEmail()).isPresent()) {
                throw new FoundException("Ya existe un Usuario con email " + entity.getEmail());
            }
            return repository.save(entity);
        } catch (FoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error al agregar usuario", e);
            throw new BusinessException("Error al agregar usuario", e);
        }
    }

    @Override
    public Usuario update(Usuario entity) throws FoundException, NotFoundException, BusinessException {
        try {
            load(entity.getId());
            if (entity.getEmail() != null && repository.findByEmailAndIdNot(entity.getEmail(), entity.getId()).isPresent()) {
                throw new FoundException("Ya existe otro Usuario con email " + entity.getEmail());
            }
            return repository.save(entity);
        } catch (FoundException | NotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error al actualizar usuario con id {}", entity.getId(), e);
            throw new BusinessException("Error al actualizar usuario", e);
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
            log.error("Error al eliminar usuario con id {}", id, e);
            throw new BusinessException("Error al eliminar usuario", e);
        }
    }
}
