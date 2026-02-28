package com.gestion.qnt.model.business.interfaces;

import com.gestion.qnt.model.Proveedor;
import com.gestion.qnt.model.business.exceptions.BusinessException;
import com.gestion.qnt.model.business.exceptions.NotFoundException;

import java.util.List;

public interface IProveedorBusiness {

    List<Proveedor> list() throws BusinessException;

    Proveedor load(Long id) throws NotFoundException, BusinessException;

    Proveedor add(Proveedor entity) throws BusinessException;

    Proveedor update(Proveedor entity) throws NotFoundException, BusinessException;

    void delete(Long id) throws NotFoundException, BusinessException;
}
