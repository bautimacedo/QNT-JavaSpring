package com.gestion.qnt.model.business.interfaces;

import com.gestion.qnt.model.Site;
import com.gestion.qnt.model.business.exceptions.BusinessException;
import com.gestion.qnt.model.business.exceptions.NotFoundException;

import java.util.List;

public interface ISiteBusiness {

    List<Site> list() throws BusinessException;

    Site load(Long id) throws NotFoundException, BusinessException;

    Site add(Site entity) throws BusinessException;

    Site update(Site entity) throws NotFoundException, BusinessException;

    void delete(Long id) throws NotFoundException, BusinessException;
}
