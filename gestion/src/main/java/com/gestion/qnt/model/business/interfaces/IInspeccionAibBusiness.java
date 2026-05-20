package com.gestion.qnt.model.business.interfaces;

import com.gestion.qnt.model.InspeccionAib;
import com.gestion.qnt.model.business.exceptions.BusinessException;
import com.gestion.qnt.model.business.exceptions.NotFoundException;

import java.util.List;

public interface IInspeccionAibBusiness {

    List<InspeccionAib> list() throws BusinessException;

    InspeccionAib load(Long id) throws NotFoundException, BusinessException;

    List<InspeccionAib> listByAibId(String aibId) throws BusinessException;

    /**
     * Procesa el JSON enviado por el pipeline externo: actualiza modelo/notas del AIB,
     * crea la entidad InspeccionAib con todas las métricas y S3 keys, y la persiste.
     * Los archivos no se almacenan en disco — viven en S3 y se sirven on-demand vía presigned URLs.
     */
    InspeccionAib receiveInspeccion(String datosJson) throws BusinessException;

    void delete(Long id) throws NotFoundException, BusinessException;
}
