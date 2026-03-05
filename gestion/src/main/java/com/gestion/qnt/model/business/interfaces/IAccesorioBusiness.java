package com.gestion.qnt.model.business.interfaces;

import com.gestion.qnt.model.Accesorio;
import com.gestion.qnt.model.business.exceptions.BusinessException;
import com.gestion.qnt.model.business.exceptions.NotFoundException;
import java.util.List;

public interface IAccesorioBusiness {
    List<Accesorio> list() throws BusinessException;
    Accesorio load(Long id) throws NotFoundException, BusinessException;
    Accesorio add(Accesorio entity) throws BusinessException;
    Accesorio update(Accesorio entity) throws NotFoundException, BusinessException;
    void delete(Long id) throws NotFoundException, BusinessException;
}