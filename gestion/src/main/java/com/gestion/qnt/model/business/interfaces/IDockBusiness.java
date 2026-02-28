package com.gestion.qnt.model.business.interfaces;

import com.gestion.qnt.model.Dock;
import com.gestion.qnt.model.business.exceptions.BusinessException;
import com.gestion.qnt.model.business.exceptions.NotFoundException;

import java.util.List;

public interface IDockBusiness {

    List<Dock> list() throws BusinessException;

    Dock load(Long id) throws NotFoundException, BusinessException;

    Dock add(Dock entity) throws BusinessException;

    Dock update(Dock entity) throws NotFoundException, BusinessException;

    void delete(Long id) throws NotFoundException, BusinessException;
}
