package com.gestion.qnt.model.business.interfaces;

import com.gestion.qnt.model.Usuario;
import com.gestion.qnt.model.business.exceptions.BusinessException;
import com.gestion.qnt.model.business.exceptions.FoundException;
import com.gestion.qnt.model.business.exceptions.NotFoundException;

import java.util.List;

public interface IUsuarioBusiness {

    List<Usuario> list() throws BusinessException;

    Usuario load(Long id) throws NotFoundException, BusinessException;

    Usuario load(String email) throws NotFoundException, BusinessException;

    Usuario add(Usuario entity) throws FoundException, BusinessException;

    Usuario update(Usuario entity) throws FoundException, NotFoundException, BusinessException;

    void delete(Long id) throws NotFoundException, BusinessException;
}
