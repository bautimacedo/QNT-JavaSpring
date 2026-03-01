package com.gestion.qnt.model.business.interfaces;

import com.gestion.qnt.model.Role;
import com.gestion.qnt.model.Usuario;
import com.gestion.qnt.model.business.exceptions.BusinessException;
import com.gestion.qnt.model.business.exceptions.FoundException;
import com.gestion.qnt.model.business.exceptions.NotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

public interface IUsuarioBusiness {

    List<Usuario> list() throws BusinessException;

    Usuario load(Long id) throws NotFoundException, BusinessException;

    Usuario load(String email) throws NotFoundException, BusinessException;

    Usuario add(Usuario entity) throws FoundException, BusinessException;

    Usuario update(Usuario entity) throws FoundException, NotFoundException, BusinessException;

    void delete(Long id) throws NotFoundException, BusinessException;

    void changePassword(String email, String oldPassword, String newPassword, PasswordEncoder encoder) throws NotFoundException, BusinessException;

    void disable(String email) throws NotFoundException, BusinessException;

    void enable(String email) throws NotFoundException, BusinessException;

    Usuario addRole(Role role, Usuario user) throws NotFoundException, BusinessException;

    Usuario removeRole(Role role, Usuario user) throws NotFoundException, BusinessException;
}
