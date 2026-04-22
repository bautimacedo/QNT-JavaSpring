package com.gestion.qnt.model.business.interfaces;

import com.gestion.qnt.model.InspeccionAib;
import com.gestion.qnt.model.business.exceptions.BusinessException;
import com.gestion.qnt.model.business.exceptions.NotFoundException;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface IInspeccionAibBusiness {

    List<InspeccionAib> list() throws BusinessException;

    InspeccionAib load(Long id) throws NotFoundException, BusinessException;

    List<InspeccionAib> listByAibId(String aibId) throws BusinessException;

    InspeccionAib receiveInspeccion(String datosJson, List<MultipartFile> graficos) throws BusinessException;

    void delete(Long id) throws NotFoundException, BusinessException;
}
