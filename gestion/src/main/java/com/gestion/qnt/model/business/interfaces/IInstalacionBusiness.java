package com.gestion.qnt.model.business.interfaces;

import com.gestion.qnt.model.Instalacion;
import com.gestion.qnt.model.business.exceptions.BusinessException;
import com.gestion.qnt.model.business.exceptions.NotFoundException;

import java.util.List;

public interface IInstalacionBusiness {

    List<Instalacion> list() throws BusinessException;

    Instalacion load(Long id) throws NotFoundException, BusinessException;

    Instalacion add(Instalacion entity) throws BusinessException;

    Instalacion update(Instalacion entity) throws NotFoundException, BusinessException;

    void delete(Long id) throws NotFoundException, BusinessException;
}
