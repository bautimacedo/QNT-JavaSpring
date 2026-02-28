package com.gestion.qnt.model.business.interfaces;

import com.gestion.qnt.model.Licencia;
import com.gestion.qnt.model.business.exceptions.BusinessException;
import com.gestion.qnt.model.business.exceptions.NotFoundException;

import java.util.List;

public interface ILicenciaBusiness {

    List<Licencia> list() throws BusinessException;

    Licencia load(Long id) throws NotFoundException, BusinessException;

    Licencia add(Licencia entity) throws BusinessException;

    Licencia update(Licencia entity) throws NotFoundException, BusinessException;

    void delete(Long id) throws NotFoundException, BusinessException;
}
