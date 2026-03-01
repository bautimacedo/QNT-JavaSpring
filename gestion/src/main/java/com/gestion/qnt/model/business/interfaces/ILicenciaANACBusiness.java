package com.gestion.qnt.model.business.interfaces;

import com.gestion.qnt.model.LicenciaANAC;
import com.gestion.qnt.model.business.exceptions.BusinessException;
import com.gestion.qnt.model.business.exceptions.NotFoundException;

import java.util.List;

public interface ILicenciaANACBusiness {

    List<LicenciaANAC> list() throws BusinessException;

    List<LicenciaANAC> listByPiloto(Long pilotoId) throws BusinessException;

    LicenciaANAC load(Long id) throws NotFoundException, BusinessException;

    LicenciaANAC add(LicenciaANAC entity) throws BusinessException;

    LicenciaANAC update(LicenciaANAC entity) throws NotFoundException, BusinessException;

    void delete(Long id) throws NotFoundException, BusinessException;
}
