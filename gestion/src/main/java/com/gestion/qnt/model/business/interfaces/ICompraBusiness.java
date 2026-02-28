package com.gestion.qnt.model.business.interfaces;

import com.gestion.qnt.model.Compra;
import com.gestion.qnt.model.business.exceptions.BusinessException;
import com.gestion.qnt.model.business.exceptions.NotFoundException;

import java.util.List;

public interface ICompraBusiness {

    List<Compra> list() throws BusinessException;

    Compra load(Long id) throws NotFoundException, BusinessException;

    Compra add(Compra entity) throws BusinessException;

    Compra update(Compra entity) throws NotFoundException, BusinessException;

    void delete(Long id) throws NotFoundException, BusinessException;
}
