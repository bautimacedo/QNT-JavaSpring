package com.gestion.qnt.model.business.interfaces;

import com.gestion.qnt.model.Log;
import com.gestion.qnt.model.business.exceptions.BusinessException;
import com.gestion.qnt.model.business.exceptions.NotFoundException;

import java.util.List;

public interface ILogBusiness {

    List<Log> list() throws BusinessException;

    Log load(Long id) throws NotFoundException, BusinessException;

    Log add(Log entity) throws BusinessException;

    Log update(Log entity) throws NotFoundException, BusinessException;

    void delete(Long id) throws NotFoundException, BusinessException;
}
