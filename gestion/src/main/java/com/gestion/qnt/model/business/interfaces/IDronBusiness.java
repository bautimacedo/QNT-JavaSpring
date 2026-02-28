package com.gestion.qnt.model.business.interfaces;

import com.gestion.qnt.model.Dron;
import com.gestion.qnt.model.business.exceptions.BusinessException;
import com.gestion.qnt.model.business.exceptions.NotFoundException;

import java.util.List;

public interface IDronBusiness {

    List<Dron> list() throws BusinessException;

    Dron load(Long id) throws NotFoundException, BusinessException;

    Dron add(Dron entity) throws BusinessException;

    Dron update(Dron entity) throws NotFoundException, BusinessException;

    void delete(Long id) throws NotFoundException, BusinessException;
}
