package com.gestion.qnt.model.business.interfaces;

import com.gestion.qnt.model.Mision;
import com.gestion.qnt.model.business.exceptions.BusinessException;
import com.gestion.qnt.model.business.exceptions.NotFoundException;

import java.util.List;

public interface IMisionBusiness {

    List<Mision> list() throws BusinessException;

    Mision load(Long id) throws NotFoundException, BusinessException;

    Mision add(Mision entity) throws BusinessException;

    Mision update(Mision entity) throws NotFoundException, BusinessException;

    void delete(Long id) throws NotFoundException, BusinessException;
}
