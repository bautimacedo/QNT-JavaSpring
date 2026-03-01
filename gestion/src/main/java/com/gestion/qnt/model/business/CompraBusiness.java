package com.gestion.qnt.model.business;

import com.gestion.qnt.model.Compra;
import com.gestion.qnt.model.business.exceptions.BusinessException;
import com.gestion.qnt.model.business.exceptions.NotFoundException;
import com.gestion.qnt.model.business.interfaces.ICompraBusiness;
import com.gestion.qnt.repository.CompraRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.gestion.qnt.controller.dto.CreateCompraRequest;
import com.gestion.qnt.model.Proveedor;
import com.gestion.qnt.model.Site;
import com.gestion.qnt.model.business.interfaces.IProveedorBusiness;
import com.gestion.qnt.model.business.interfaces.ISiteBusiness;

import java.util.List;

@Service
@Slf4j
public class CompraBusiness implements ICompraBusiness {

    @Autowired
    private CompraRepository repository;

    @Autowired
    private IProveedorBusiness proveedorBusiness;

    @Autowired
    private ISiteBusiness siteBusiness;

    @Override
    public List<Compra> list() throws BusinessException {
        try {
            return repository.findAll();
        } catch (Exception e) {
            log.error("Error al listar compras", e);
            throw new BusinessException("Error al listar compras", e);
        }
    }

    @Override
    public Compra load(Long id) throws NotFoundException, BusinessException {
        try {
            return repository.findById(id)
                    .orElseThrow(() -> new NotFoundException("No existe Compra con id " + id));
        } catch (NotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error al cargar compra con id {}", id, e);
            throw new BusinessException("Error al cargar compra", e);
        }
    }

    @Override
    public Compra add(CreateCompraRequest request) throws NotFoundException, BusinessException {
        try {
            Proveedor proveedor = proveedorBusiness.load(request.proveedorId());
            Site site = null;
            if (request.siteId() != null) {
                site = siteBusiness.load(request.siteId());
            }

            Compra compra = new Compra();
            compra.setProveedor(proveedor);
            compra.setFechaCompra(request.fechaCompra());
            compra.setFechaFactura(request.fechaFactura());
            compra.setNumeroFactura(request.numeroFactura());
            compra.setImporte(request.importe());
            compra.setMoneda(request.moneda() != null && !request.moneda().isBlank() ? request.moneda() : "ARS");
            compra.setTipoCompra(request.tipoCompra());
            compra.setDescripcion(request.descripcion());
            compra.setSite(site);
            compra.setObservaciones(request.observaciones());

            return repository.save(compra);
        } catch (NotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error al agregar compra", e);
            throw new BusinessException("Error al agregar compra", e);
        }
    }

    @Override
    public Compra update(Compra entity) throws NotFoundException, BusinessException {
        try {
            load(entity.getId());
            return repository.save(entity);
        } catch (NotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error al actualizar compra con id {}", entity.getId(), e);
            throw new BusinessException("Error al actualizar compra", e);
        }
    }

    @Override
    public Compra update(Long id, CreateCompraRequest request) throws NotFoundException, BusinessException {
        try {
            Compra existing = load(id);

            Proveedor proveedor = proveedorBusiness.load(request.proveedorId());
            existing.setProveedor(proveedor);
            existing.setFechaCompra(request.fechaCompra());
            existing.setFechaFactura(request.fechaFactura());
            existing.setNumeroFactura(request.numeroFactura());
            existing.setImporte(request.importe());
            existing.setMoneda(request.moneda() != null && !request.moneda().isBlank() ? request.moneda() : "ARS");
            existing.setTipoCompra(request.tipoCompra());
            existing.setDescripcion(request.descripcion());
            if (request.siteId() != null) {
                existing.setSite(siteBusiness.load(request.siteId()));
            } else {
                existing.setSite(null);
            }
            existing.setObservaciones(request.observaciones());

            return repository.save(existing);
        } catch (NotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error al actualizar compra con id {}", id, e);
            throw new BusinessException("Error al actualizar compra", e);
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
            log.error("Error al eliminar compra con id {}", id, e);
            throw new BusinessException("Error al eliminar compra", e);
        }
    }
}
