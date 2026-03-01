package com.gestion.qnt.model.business;

import com.gestion.qnt.model.LicenciaANAC;
import com.gestion.qnt.model.business.exceptions.BusinessException;
import com.gestion.qnt.model.business.exceptions.NotFoundException;
import com.gestion.qnt.model.business.interfaces.ILicenciaANACBusiness;
import com.gestion.qnt.repository.LicenciaANACRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class LicenciaANACBusiness implements ILicenciaANACBusiness {

    @Autowired
    private LicenciaANACRepository repository;

    @Override
    public List<LicenciaANAC> list() throws BusinessException {
        try {
            return repository.findAll();
        } catch (Exception e) {
            log.error("Error al listar licencias ANAC", e);
            throw new BusinessException("Error al listar licencias ANAC", e);
        }
    }

    @Override
    public List<LicenciaANAC> listByPiloto(Long pilotoId) throws BusinessException {
        try {
            return repository.findByPilotoId(pilotoId);
        } catch (Exception e) {
            log.error("Error al listar licencias ANAC del piloto {}", pilotoId, e);
            throw new BusinessException("Error al listar licencias ANAC del piloto", e);
        }
    }

    @Override
    public LicenciaANAC load(Long id) throws NotFoundException, BusinessException {
        try {
            return repository.findById(id)
                    .orElseThrow(() -> new NotFoundException("No existe LicenciaANAC con id " + id));
        } catch (NotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error al cargar licencia ANAC con id {}", id, e);
            throw new BusinessException("Error al cargar licencia ANAC", e);
        }
    }

    @Override
    public LicenciaANAC add(LicenciaANAC entity) throws BusinessException {
        try {
            return repository.save(entity);
        } catch (Exception e) {
            log.error("Error al agregar licencia ANAC", e);
            throw new BusinessException("Error al agregar licencia ANAC", e);
        }
    }

    @Override
    public LicenciaANAC update(LicenciaANAC entity) throws NotFoundException, BusinessException {
        try {
            load(entity.getId());
            return repository.save(entity);
        } catch (NotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error al actualizar licencia ANAC con id {}", entity.getId(), e);
            throw new BusinessException("Error al actualizar licencia ANAC", e);
        }
    }

    @Override
    public void delete(Long id) throws NotFoundException, BusinessException {
        try {
            load(id);
            repository.deleteById(id);
        } catch (NotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error al eliminar licencia ANAC con id {}", id, e);
            throw new BusinessException("Error al eliminar licencia ANAC", e);
        }
    }
}
