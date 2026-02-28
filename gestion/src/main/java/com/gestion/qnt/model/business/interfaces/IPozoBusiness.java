package com.gestion.qnt.model.business.interfaces;

import com.gestion.qnt.model.Pozo;
import com.gestion.qnt.model.business.exceptions.BusinessException;
import com.gestion.qnt.model.business.exceptions.NotFoundException;

import java.util.List;

public interface IPozoBusiness {

    List<Pozo> list() throws BusinessException;

    Pozo load(Long id) throws NotFoundException, BusinessException;

    Pozo add(Pozo entity) throws BusinessException;

    Pozo update(Pozo entity) throws NotFoundException, BusinessException;

    void delete(Long id) throws NotFoundException, BusinessException;
}
