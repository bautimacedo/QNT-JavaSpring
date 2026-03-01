package com.gestion.qnt.model.business;

import com.gestion.qnt.model.Role;
import com.gestion.qnt.model.Usuario;
import com.gestion.qnt.model.enums.EstadoUsuario;
import com.gestion.qnt.model.business.exceptions.BusinessException;
import com.gestion.qnt.model.business.exceptions.FoundException;
import com.gestion.qnt.model.business.exceptions.NotFoundException;
import com.gestion.qnt.model.business.interfaces.IUsuarioBusiness;
import com.gestion.qnt.model.business.interfaces.IRoleBusiness;
import com.gestion.qnt.repository.UsuarioRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

@Service
@Slf4j
public class UsuarioBusiness implements IUsuarioBusiness {

    @Autowired
    private UsuarioRepository repository;

    @Autowired
    private IRoleBusiness roleBusiness;

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

    @Override
    public void changePassword(String email, String oldPassword, String newPassword, PasswordEncoder encoder) throws NotFoundException, BusinessException {
        try {
            Usuario user = load(email);
            if (!encoder.matches(oldPassword, user.getPassword())) {
                throw new BusinessException("Contraseña anterior incorrecta");
            }
            user.setPassword(encoder.encode(newPassword));
            update(user);
        } catch (NotFoundException | BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error al cambiar contraseña para email {}", email, e);
            throw new BusinessException("Error al cambiar contraseña", e);
        }
    }

    @Override
    public void disable(String email) throws NotFoundException, BusinessException {
        try {
            Usuario user = load(email);
            user.setActivo(false);
            user.setEstado(EstadoUsuario.DESACTIVADO);
            update(user);
        } catch (NotFoundException | BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error al desactivar usuario con email {}", email, e);
            throw new BusinessException("Error al desactivar usuario", e);
        }
    }

    @Override
    public void enable(String email) throws NotFoundException, BusinessException {
        try {
            Usuario user = load(email);
            user.setActivo(true);
            user.setEstado(EstadoUsuario.ACTIVO);
            update(user);
        } catch (NotFoundException | BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error al activar usuario con email {}", email, e);
            throw new BusinessException("Error al activar usuario", e);
        }
    }

    @Override
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public List<Usuario> listPendientes() throws BusinessException {
        try {
            return repository.findByEstado(EstadoUsuario.PENDIENTE_APROBACION);
        } catch (Exception e) {
            log.error("Error al listar usuarios pendientes", e);
            throw new BusinessException("Error al listar usuarios pendientes", e);
        }
    }

    @Override
    public Usuario aprobar(Long usuarioId, String roleCodigo) throws NotFoundException, BusinessException {
        try {
            Usuario usuario = load(usuarioId);
            if (usuario.getEstado() != EstadoUsuario.PENDIENTE_APROBACION) {
                throw new BusinessException("El usuario no está pendiente de aprobación");
            }
            Role role = roleBusiness.load(roleCodigo);
            usuario.setEstado(EstadoUsuario.ACTIVO);
            usuario.setActivo(true);
            return addRole(role, usuario);
        } catch (NotFoundException | BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error al aprobar usuario {}", usuarioId, e);
            throw new BusinessException("Error al aprobar usuario", e);
        }
    }

    @Override
    public Usuario addRole(Role role, Usuario user) throws NotFoundException, BusinessException {
        try {
            load(user.getId());
            if (user.getRoles() == null) {
                user.setRoles(new java.util.ArrayList<>());
            }
            if (user.getRoles().stream().noneMatch(r -> r.getId().equals(role.getId()))) {
                user.getRoles().add(role);
            }
            return update(user);
        } catch (NotFoundException | BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error al asignar rol al usuario {}", user.getId(), e);
            throw new BusinessException("Error al asignar rol", e);
        }
    }

    @Override
    public Usuario removeRole(Role role, Usuario user) throws NotFoundException, BusinessException {
        try {
            load(user.getId());
            if (user.getRoles() != null) {
                user.getRoles().removeIf(r -> r.getId().equals(role.getId()));
            }
            return update(user);
        } catch (NotFoundException | BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error al quitar rol del usuario {}", user.getId(), e);
            throw new BusinessException("Error al quitar rol", e);
        }
    }
}
