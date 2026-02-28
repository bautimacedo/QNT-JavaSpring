package com.gestion.qnt.model.business.interfaces;

import com.gestion.qnt.model.Seguro;
import com.gestion.qnt.model.business.exceptions.BusinessException;
import com.gestion.qnt.model.business.exceptions.NotFoundException;

import java.util.List;

public interface ISeguroBusiness {

    List<Seguro> list() throws BusinessException;

    Seguro load(Long id) throws NotFoundException, BusinessException;

    Seguro add(Seguro entity) throws BusinessException;

    Seguro update(Seguro entity) throws NotFoundException, BusinessException;

    void delete(Long id) throws NotFoundException, BusinessException;
}
