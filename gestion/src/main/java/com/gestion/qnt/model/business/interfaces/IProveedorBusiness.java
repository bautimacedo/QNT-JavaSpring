package com.gestion.qnt.model.business.interfaces;

import com.gestion.qnt.model.Proveedor;
import com.gestion.qnt.model.business.exceptions.BusinessException;
import com.gestion.qnt.model.business.exceptions.NotFoundException;

import java.util.List;

public interface IProveedorBusiness {

    List<Proveedor> list() throws BusinessException;

    Proveedor load(Long id) throws NotFoundException, BusinessException;

    /**
     * Obtiene un proveedor por nombre; si no existe, lo crea con ese nombre y lo devuelve.
     * Útil al cargar una compra con un proveedor aún no registrado.
     */
    Proveedor loadOrCreate(String nombre) throws BusinessException;

    Proveedor add(Proveedor entity) throws BusinessException;

    Proveedor update(Proveedor entity) throws NotFoundException, BusinessException;

    void delete(Long id) throws NotFoundException, BusinessException;
}
