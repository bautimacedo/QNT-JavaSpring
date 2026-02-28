package com.gestion.qnt.model.business.interfaces;

import com.gestion.qnt.model.MantenimientoDron;
import com.gestion.qnt.model.business.exceptions.BusinessException;
import com.gestion.qnt.model.business.exceptions.NotFoundException;

import java.util.List;

public interface IMantenimientoDronBusiness {

    List<MantenimientoDron> list() throws BusinessException;

    MantenimientoDron load(Long id) throws NotFoundException, BusinessException;

    MantenimientoDron add(MantenimientoDron entity) throws BusinessException;

    MantenimientoDron update(MantenimientoDron entity) throws NotFoundException, BusinessException;

    void delete(Long id) throws NotFoundException, BusinessException;
}
