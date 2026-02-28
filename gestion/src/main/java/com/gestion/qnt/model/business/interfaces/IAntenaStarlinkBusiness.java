package com.gestion.qnt.model.business.interfaces;

import com.gestion.qnt.model.AntenaStarlink;
import com.gestion.qnt.model.business.exceptions.BusinessException;
import com.gestion.qnt.model.business.exceptions.NotFoundException;

import java.util.List;

public interface IAntenaStarlinkBusiness {

    List<AntenaStarlink> list() throws BusinessException;

    AntenaStarlink load(Long id) throws NotFoundException, BusinessException;

    AntenaStarlink add(AntenaStarlink entity) throws BusinessException;

    AntenaStarlink update(AntenaStarlink entity) throws NotFoundException, BusinessException;

    void delete(Long id) throws NotFoundException, BusinessException;
}
