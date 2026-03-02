package com.gestion.qnt.model.business.interfaces;

import com.gestion.qnt.model.Compra;
import com.gestion.qnt.model.enums.TipoCompra;
import com.gestion.qnt.model.business.exceptions.BusinessException;
import com.gestion.qnt.model.business.exceptions.NotFoundException;
import com.gestion.qnt.controller.dto.CreateCompraRequest;

import java.util.List;

public interface ICompraBusiness {

    List<Compra> list() throws BusinessException;

    List<Compra> listFiltered(TipoCompra tipoCompra, Long proveedorId) throws BusinessException;

    Compra load(Long id) throws NotFoundException, BusinessException;

    Compra add(CreateCompraRequest request) throws NotFoundException, BusinessException;

    Compra update(Compra entity) throws NotFoundException, BusinessException;

    Compra update(Long id, CreateCompraRequest request) throws NotFoundException, BusinessException;

    void delete(Long id) throws NotFoundException, BusinessException;
}
