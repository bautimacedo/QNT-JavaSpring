package com.gestion.qnt.model.business.interfaces;


import com.gestion.qnt.model.Empresa;
import com.gestion.qnt.model.Site;
import com.gestion.qnt.model.business.exceptions.BusinessException;
import com.gestion.qnt.model.business.exceptions.NotFoundException;

import java.util.List;

public interface IEmpresaBusiness {

	List<Empresa> list() throws BusinessException; 
	
	Empresa load(Long id) throws NotFoundException, BusinessException;
	
	Empresa add(Empresa entity) throws BusinessException;
	
	Empresa update(Empresa entity) throws NotFoundException, BusinessException;
	
	Empresa delete(Long id) throws NotFoundException, BusinessException;
	
	Empresa addSite(Site site, Long id) throws NotFoundException, BusinessException;
	
	
}
