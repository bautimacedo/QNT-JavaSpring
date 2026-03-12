package com.gestion.qnt.model.business.interfaces;

import com.gestion.qnt.model.Tarea;
import com.gestion.qnt.model.business.exceptions.BusinessException;
import com.gestion.qnt.model.business.exceptions.NotFoundException;

import java.util.List;

public interface ITareaBusiness {

    List<Tarea> list() throws BusinessException;

    Tarea load(Long id) throws NotFoundException, BusinessException;

    Tarea add(Tarea entity) throws BusinessException;

    Tarea update(Tarea entity) throws NotFoundException, BusinessException;

    void delete(Long id) throws NotFoundException, BusinessException;
}
