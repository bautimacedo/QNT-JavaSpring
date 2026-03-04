package com.gestion.qnt.model.business;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import com.gestion.qnt.model.Empresa;
import com.gestion.qnt.model.Site;
import com.gestion.qnt.model.business.exceptions.BusinessException;
import com.gestion.qnt.model.business.exceptions.NotFoundException;
import com.gestion.qnt.model.business.interfaces.IEmpresaBusiness;
import com.gestion.qnt.repository.EmpresaRepository;

public class EmpresaBusiness implements IEmpresaBusiness {

	@Autowired
    private EmpresaRepository empresaRepository;

    @Override
    public List<Empresa> list() throws BusinessException {
        try {
            return empresaRepository.findAll();
        } catch (Exception e) {
            throw new BusinessException("Error al listar las empresas", e);
        }
    }

    @Override
    public Empresa load(Long id) throws NotFoundException, BusinessException {
        return empresaRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("No se encontró la empresa con ID: " + id));
    }

    @Override
    public Empresa add(Empresa entity) throws BusinessException {
        try {
            return empresaRepository.save(entity);
        } catch (Exception e) {
            throw new BusinessException("No se pudo guardar la empresa", e);
        }
    }

    @Override
    public Empresa update(Empresa entity) throws NotFoundException, BusinessException {
        // Verificamos que exista antes de intentar actualizar
        load(entity.getId()); 
        try {
            return empresaRepository.save(entity);
        } catch (Exception e) {
            throw new BusinessException("Error al actualizar la empresa", e);
        }
    }

    @Override
    public Empresa delete(Long id) throws NotFoundException, BusinessException {
        Empresa empresa = load(id);
        try {
            empresaRepository.deleteById(id);
            return empresa;
        } catch (Exception e) {
            throw new BusinessException("No se pudo eliminar la empresa", e);
        }
    }

    @Override
    public Empresa addSite(Site site, Long id) throws NotFoundException, BusinessException {
        Empresa empresa = load(id); // Reutilizamos load para validar existencia
        try {
            empresa.getSites().add(site);
            return empresaRepository.save(empresa);
        } catch (Exception e) {
            throw new BusinessException("No se pudo agregar el site a la empresa", e);
        }
    }

}
