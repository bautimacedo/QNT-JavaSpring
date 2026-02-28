package com.gestion.qnt.model.business.interfaces;

import com.gestion.qnt.model.Bateria;
import com.gestion.qnt.model.business.exceptions.BusinessException;
import com.gestion.qnt.model.business.exceptions.NotFoundException;

import java.util.List;

public interface IBateriaBusiness {

    List<Bateria> list() throws BusinessException;

    Bateria load(Long id) throws NotFoundException, BusinessException;

    Bateria add(Bateria entity) throws BusinessException;

    Bateria update(Bateria entity) throws NotFoundException, BusinessException;

    void delete(Long id) throws NotFoundException, BusinessException;
}
