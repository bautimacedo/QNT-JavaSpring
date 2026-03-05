package com.gestion.qnt.model.business;

import com.gestion.qnt.model.Bateria;
import com.gestion.qnt.model.Compra;
import com.gestion.qnt.model.Dron;
import com.gestion.qnt.model.Helice;
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
import com.gestion.qnt.model.enums.MetodoPago;
import com.gestion.qnt.model.enums.TipoCompra;
import com.gestion.qnt.model.business.interfaces.IBateriaBusiness;
import com.gestion.qnt.model.business.interfaces.IDronBusiness;
import com.gestion.qnt.model.business.interfaces.IHeliceBusiness;
import com.gestion.qnt.model.business.interfaces.ILicenciaBusiness;
import com.gestion.qnt.model.business.interfaces.IProveedorBusiness;
import com.gestion.qnt.model.business.interfaces.ISiteBusiness;
import com.gestion.qnt.model.Licencia;
import com.gestion.qnt.model.enums.Estado;

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

    @Autowired
    private IDronBusiness dronBusiness;

    @Autowired
    private IBateriaBusiness bateriaBusiness;

    @Autowired
    private IHeliceBusiness heliceBusiness;

    @Autowired
    private ILicenciaBusiness licenciaBusiness;

    @Override
    public List<Compra> list() throws BusinessException {
        try {
            return repository.findAllWithProveedorAndSite();
        } catch (Exception e) {
            log.error("Error al listar compras", e);
            throw new BusinessException("Error al listar compras", e);
        }
    }

    @Override
    public List<Compra> listFiltered(TipoCompra tipoCompra, Long proveedorId) throws BusinessException {
        try {
            return repository.findFilteredWithProveedorAndSite(tipoCompra, proveedorId);
        } catch (Exception e) {
            log.error("Error al filtrar compras", e);
            throw new BusinessException("Error al filtrar compras", e);
        }
    }

    @Override
    public Compra load(Long id) throws NotFoundException, BusinessException {
        try {
            return repository.findByIdWithProveedorAndSite(id)
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
            if (!request.hasProveedor()) {
                throw new BusinessException("Se debe indicar proveedorId o proveedorNombre");
            }
            Proveedor proveedor = request.proveedorId() != null
                    ? proveedorBusiness.load(request.proveedorId())
                    : proveedorBusiness.loadOrCreate(request.proveedorNombre());

            Site site = null;
            if (request.siteId() != null) {
                site = siteBusiness.load(request.siteId());
            }

            Compra compra = new Compra();
            compra.setProveedor(proveedor);
            compra.setFechaCompra(request.fechaCompra());
            compra.setFechaFactura(request.fechaFactura());
            compra.setImporte(request.importe());
            compra.setMoneda(request.moneda() != null && !request.moneda().isBlank() ? request.moneda() : "ARS");
            applyIva(compra, request);
            compra.setTipoCompra(request.tipoCompra());
            applyMetodoPago(compra, request);
            compra.setDescripcion(request.descripcion());
            compra.setSite(site);
            compra.setObservaciones(request.observaciones());

            if (request.tipoCompra() == TipoCompra.EQUIPO) {
                if (request.tipoEquipo() == null) {
                    throw new BusinessException("Se debe indicar el tipoEquipo cuando el tipo de compra es EQUIPO");
                }
                compra.setTipoEquipo(request.tipoEquipo());
                compra.setDescripcionEquipo(request.descripcionEquipo());
                crearItemEnInventario(compra);
            } else {
                compra.setTipoEquipo(null);
                compra.setDescripcionEquipo(null);
            }

            Compra savedCompra = repository.save(compra);

            if (request.tipoCompra() == TipoCompra.LICENCIA_SW) {
                crearLicenciaSWDesdeCompra(savedCompra, request);
            }

            return savedCompra;
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

            if (!request.hasProveedor()) {
                throw new BusinessException("Se debe indicar proveedorId o proveedorNombre");
            }
            Proveedor proveedor = request.proveedorId() != null
                    ? proveedorBusiness.load(request.proveedorId())
                    : proveedorBusiness.loadOrCreate(request.proveedorNombre());

            existing.setProveedor(proveedor);
            existing.setFechaCompra(request.fechaCompra());
            existing.setFechaFactura(request.fechaFactura());
            existing.setImporte(request.importe());
            existing.setMoneda(request.moneda() != null && !request.moneda().isBlank() ? request.moneda() : "ARS");
            applyIva(existing, request);
            existing.setTipoCompra(request.tipoCompra());
            applyMetodoPago(existing, request);
            existing.setDescripcion(request.descripcion());
            if (request.siteId() != null) {
                existing.setSite(siteBusiness.load(request.siteId()));
            } else {
                existing.setSite(null);
            }
            existing.setObservaciones(request.observaciones());

            if (request.tipoCompra() == TipoCompra.EQUIPO) {
                if (request.tipoEquipo() == null) {
                    throw new BusinessException("Se debe indicar el tipoEquipo cuando el tipo de compra es EQUIPO");
                }
                existing.setTipoEquipo(request.tipoEquipo());
                existing.setDescripcionEquipo(request.descripcionEquipo());
            } else {
                existing.setTipoEquipo(null);
                existing.setDescripcionEquipo(null);
            }

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

    /**
     * Crea automáticamente una Licencia (SW) en stock al registrar una compra de tipo LICENCIA_SW.
     * La licencia queda vinculada a la compra mediante compra_id para trazabilidad completa.
     * El nombre inicial se toma de descripcion; si está vacío se usa "Licencia SW".
     */
    private void crearLicenciaSWDesdeCompra(Compra compra, CreateCompraRequest request)
            throws BusinessException {
        Licencia licencia = new Licencia();
        String nombre = (request.descripcion() != null && !request.descripcion().isBlank())
                ? request.descripcion().trim()
                : "Licencia SW";
        licencia.setNombre(nombre);
        licencia.setCompra(compra);
        licencia.setFechaCompra(compra.getFechaCompra());
        licencia.setActivo(true);
        licenciaBusiness.add(licencia);
        log.info("Licencia SW '{}' creada automáticamente a partir de compra id={}", nombre, compra.getId());
    }

    /**
     * Crea automáticamente el ítem en inventario cuando se registra una compra de tipo EQUIPO.
     * Solo se llama desde add(); en update() no se duplica el ítem.
     * DOCK, ANTENA_RTK y ANTENA_STARLINK requieren FKs (site_id, dock_id) que no están disponibles
     * en la compra, por lo que solo se loguea un aviso y no se crea nada.
     */
    private void crearItemEnInventario(Compra compra) throws BusinessException {
        String nombre = compra.getDescripcionEquipo();

        switch (compra.getTipoEquipo()) {
            case DRON -> {
                Dron dron = new Dron();
                dron.setEstado(Estado.NO_LLEGO);
                dron.setNombre(nombre);
                dron.setFechaCompra(compra.getFechaCompra());
                dronBusiness.add(dron);
            }
            case BATERIA -> {
                Bateria bateria = new Bateria();
                bateria.setEstado(Estado.NO_LLEGO);
                bateria.setNombre(nombre);
                bateria.setFechaCompra(compra.getFechaCompra());
                bateriaBusiness.add(bateria);
            }
            case HELICE -> {
                Helice helice = new Helice();
                helice.setEstado(Estado.NO_LLEGO);
                helice.setNombre(nombre);
                helice.setFechaCompra(compra.getFechaCompra());
                heliceBusiness.add(helice);
            }
            case DOCK -> {
                // Dock requiere site_id NOT NULL; no se puede crear automáticamente sin un site.
                // El admin debe crear el Dock manualmente desde la pantalla de inventario.
                log.info("Compra de DOCK registrada. El ítem debe crearse manualmente en inventario (requiere Site).");
            }
            case ANTENA_RTK, ANTENA_STARLINK -> {
                // AntenaRtk y AntenaStarlink requieren dock_id NOT NULL.
                // El admin las asocia a un Dock existente manualmente.
                log.info("Compra de {} registrada. El ítem debe asociarse a un Dock existente manualmente.", compra.getTipoEquipo());
            }
            case OTRO -> {
                // Sin entidad en inventario para tipo OTRO.
            }
        }
    }

    /**
     * Aplica la lógica de método de pago y datos de tarjeta sobre la entidad Compra.
     * Regla:
     * - metodoPago es obligatorio (se valida a nivel DTO y nuevamente aquí).
     * - Si metodoPago == TARJETA: companiaTarjeta obligatoria (no vacía) y ultimos4Tarjeta obligatorio,
     *   exactamente 4 dígitos. Si falla, lanza BusinessException.
     * - Si metodoPago != TARJETA: companiaTarjeta y ultimos4Tarjeta se guardan como null, ignorando
     *   cualquier valor enviado por el cliente.
     */
    private void applyMetodoPago(Compra compra, CreateCompraRequest request) throws BusinessException {
        MetodoPago metodoPago = request.metodoPago();
        if (metodoPago == null) {
            throw new BusinessException("Se debe indicar el metodoPago de la compra");
        }
        compra.setMetodoPago(metodoPago);

        if (metodoPago == MetodoPago.TARJETA) {
            String compania = request.companiaTarjeta();
            String ultimos4 = request.ultimos4Tarjeta();

            if (compania == null || compania.isBlank()) {
                throw new BusinessException("Cuando el metodoPago es TARJETA, companiaTarjeta es obligatorio");
            }
            if (ultimos4 == null || !ultimos4.matches("^[0-9]{4}$")) {
                throw new BusinessException("Cuando el metodoPago es TARJETA, ultimos4Tarjeta debe tener exactamente 4 dígitos");
            }

            compra.setCompaniaTarjeta(compania.trim());
            compra.setUltimos4Tarjeta(ultimos4);
        } else {
            // Para métodos de pago que no son tarjeta, siempre se guardan como null.
            compra.setCompaniaTarjeta(null);
            compra.setUltimos4Tarjeta(null);
        }
    }

    /**
     * Aplica IVA a la compra. Regla: importe es siempre el TOTAL.
     * Si tieneIva = true: ivaPorcentaje obligatorio, en rango (0, 100].
     * Si tieneIva = false o null: ivaPorcentaje se guarda como null.
     */
    private void applyIva(Compra compra, CreateCompraRequest request) throws BusinessException {
        Boolean tieneIva = Boolean.TRUE.equals(request.tieneIva());
        compra.setTieneIva(tieneIva);

        if (tieneIva) {
            java.math.BigDecimal pct = request.ivaPorcentaje();
            if (pct == null) {
                throw new BusinessException("Cuando la compra tiene IVA, ivaPorcentaje es obligatorio");
            }
            if (pct.compareTo(java.math.BigDecimal.ZERO) <= 0 || pct.compareTo(java.math.BigDecimal.valueOf(100)) > 0) {
                throw new BusinessException("ivaPorcentaje debe ser mayor que 0 y menor o igual a 100");
            }
            compra.setIvaPorcentaje(pct.setScale(2, java.math.RoundingMode.HALF_UP));
        } else {
            compra.setIvaPorcentaje(null);
        }
    }
}
