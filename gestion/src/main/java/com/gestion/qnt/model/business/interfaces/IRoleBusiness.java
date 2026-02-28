package com.gestion.qnt.model.business.interfaces;

import com.gestion.qnt.model.Role;
import com.gestion.qnt.model.business.exceptions.BusinessException;
import com.gestion.qnt.model.business.exceptions.FoundException;
import com.gestion.qnt.model.business.exceptions.NotFoundException;

import java.util.List;

public interface IRoleBusiness {

    List<Role> list() throws BusinessException;

    Role load(Long id) throws NotFoundException, BusinessException;

    Role load(String codigo) throws NotFoundException, BusinessException;

    Role add(Role entity) throws FoundException, BusinessException;

    Role update(Role entity) throws FoundException, NotFoundException, BusinessException;

    void delete(Long id) throws NotFoundException, BusinessException;
}
