package com.gestion.qnt.model.business.interfaces;

import com.gestion.qnt.model.AntenaRtk;
import com.gestion.qnt.model.business.exceptions.BusinessException;
import com.gestion.qnt.model.business.exceptions.NotFoundException;

import java.util.List;

public interface IAntenaRtkBusiness {

    List<AntenaRtk> list() throws BusinessException;

    AntenaRtk load(Long id) throws NotFoundException, BusinessException;

    AntenaRtk add(AntenaRtk entity) throws BusinessException;

    AntenaRtk update(AntenaRtk entity) throws NotFoundException, BusinessException;

    void delete(Long id) throws NotFoundException, BusinessException;
}
