package com.gestion.qnt.model.business.interfaces;

import com.gestion.qnt.model.Helice;
import com.gestion.qnt.model.business.exceptions.BusinessException;
import com.gestion.qnt.model.business.exceptions.NotFoundException;

import java.util.List;

public interface IHeliceBusiness {

    List<Helice> list() throws BusinessException;

    Helice load(Long id) throws NotFoundException, BusinessException;

    Helice add(Helice entity) throws BusinessException;

    Helice update(Helice entity) throws NotFoundException, BusinessException;

    void delete(Long id) throws NotFoundException, BusinessException;
}
