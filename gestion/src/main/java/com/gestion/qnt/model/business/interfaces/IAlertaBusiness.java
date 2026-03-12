package com.gestion.qnt.model.business.interfaces;

import com.gestion.qnt.model.Alerta;
import com.gestion.qnt.model.business.exceptions.BusinessException;
import com.gestion.qnt.model.business.exceptions.NotFoundException;

import java.util.List;

public interface IAlertaBusiness {

    /** Devuelve todas las alertas no resueltas, ordenadas por nivel y fecha. */
    List<Alerta> listActivas() throws BusinessException;

    /** Marca una alerta como resuelta. */
    Alerta resolver(Long id) throws NotFoundException, BusinessException;

    /** Ejecuta la generación automática de alertas (llamado por el scheduler). */
    void generarAlertas() throws BusinessException;
}
