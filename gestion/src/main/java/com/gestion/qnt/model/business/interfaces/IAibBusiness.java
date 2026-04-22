package com.gestion.qnt.model.business.interfaces;

import com.gestion.qnt.model.Aib;
import com.gestion.qnt.model.business.exceptions.BusinessException;
import com.gestion.qnt.model.business.exceptions.NotFoundException;

import java.util.List;

public interface IAibBusiness {

    List<Aib> list() throws BusinessException;

    Aib load(Long id) throws NotFoundException, BusinessException;

    Aib loadByAibId(String aibId) throws NotFoundException, BusinessException;

    Aib findOrCreate(String aibId) throws BusinessException;

    Aib update(Aib entity) throws NotFoundException, BusinessException;

    void delete(Long id) throws NotFoundException, BusinessException;
}
