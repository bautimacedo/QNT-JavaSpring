package com.gestion.qnt.model.business.interfaces;

import com.gestion.qnt.model.MantenimientoDock;
import com.gestion.qnt.model.business.exceptions.BusinessException;
import com.gestion.qnt.model.business.exceptions.NotFoundException;

import java.util.List;

public interface IMantenimientoDockBusiness {

    List<MantenimientoDock> list() throws BusinessException;

    MantenimientoDock load(Long id) throws NotFoundException, BusinessException;

    MantenimientoDock add(MantenimientoDock entity) throws BusinessException;

    MantenimientoDock update(MantenimientoDock entity) throws NotFoundException, BusinessException;

    void delete(Long id) throws NotFoundException, BusinessException;
}
