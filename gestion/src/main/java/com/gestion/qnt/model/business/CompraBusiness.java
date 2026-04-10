package com.gestion.qnt.model.business;

import com.gestion.qnt.controller.dto.CompraItemRequest;
import com.gestion.qnt.model.AntenaRtk;
import com.gestion.qnt.model.AntenaStarlink;
import com.gestion.qnt.model.Bateria;
import com.gestion.qnt.model.Compra;
import com.gestion.qnt.model.CompraItem;
import com.gestion.qnt.model.Dock;
import com.gestion.qnt.model.Dron;
import com.gestion.qnt.model.Helice;
import com.gestion.qnt.model.Seguro;
import com.gestion.qnt.model.business.exceptions.BusinessException;
import com.gestion.qnt.model.business.exceptions.NotFoundException;
import com.gestion.qnt.model.business.interfaces.ICompraBusiness;
import com.gestion.qnt.repository.CompraRepository;
import com.gestion.qnt.repository.LicenciaRepository;
import com.gestion.qnt.repository.SeguroRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.gestion.qnt.controller.dto.CreateCompraRequest;
import com.gestion.qnt.model.Proveedor;
import com.gestion.qnt.model.Site;
import com.gestion.qnt.model.enums.MetodoPago;
import com.gestion.qnt.model.enums.TipoCompra;
import com.gestion.qnt.model.business.interfaces.IAntenaRtkBusiness;
import com.gestion.qnt.model.business.interfaces.IAntenaStarlinkBusiness;
import com.gestion.qnt.model.business.interfaces.IBateriaBusiness;
import com.gestion.qnt.model.business.interfaces.IDockBusiness;
import com.gestion.qnt.model.business.interfaces.IDronBusiness;
import com.gestion.qnt.model.business.interfaces.IHeliceBusiness;
import com.gestion.qnt.model.business.interfaces.ILicenciaBusiness;
import com.gestion.qnt.model.business.interfaces.IProveedorBusiness;
import com.gestion.qnt.model.business.interfaces.ISeguroBusiness;
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
    private IDockBusiness dockBusiness;

    @Autowired
    private IAntenaRtkBusiness antenaRtkBusiness;

    @Autowired
    private IAntenaStarlinkBusiness antenaStarlinkBusiness;

    @Autowired
    private ILicenciaBusiness licenciaBusiness;

    @Autowired
    private ISeguroBusiness seguroBusiness;

    @Autowired
    private LicenciaRepository licenciaRepository;

    @Autowired
    private SeguroRepository seguroRepository;

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
            applyMetodoPago(compra, request);
            compra.setDescripcion(request.descripcion());
            compra.setSite(site);
            compra.setObservaciones(request.observaciones());

            if (request.hasItems()) {
                // Nuevo flujo: múltiples ítems
                derivarTipoCompraDesdeItems(compra, request.items());
                compra.setTipoEquipo(null);
                compra.setDescripcionEquipo(null);
                for (CompraItemRequest itemReq : request.items()) {
                    validarItem(itemReq);
                    CompraItem item = new CompraItem();
                    item.setCompra(compra);
                    item.setTipoCompra(itemReq.tipoCompra());
                    item.setTipoEquipo(itemReq.tipoEquipo());
                    item.setDescripcion(itemReq.descripcion());
                    item.setCantidad(itemReq.cantidad() != null && itemReq.cantidad() > 0 ? itemReq.cantidad() : 1);
                    item.setImporte(itemReq.importe());
                    compra.getItems().add(item);
                }
                Compra savedCompra = repository.save(compra);
                // Auto-crear entidades por ítem
                for (CompraItem item : savedCompra.getItems()) {
                    crearEntidadDesdeItem(savedCompra, item);
                }
                return savedCompra;
            } else {
                // Flujo legacy: un solo tipo en cabecera
                if (request.tipoCompra() == null) {
                    throw new BusinessException("Se debe indicar tipoCompra o al menos un ítem");
                }
                compra.setTipoCompra(request.tipoCompra());
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
            }
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
            applyMetodoPago(existing, request);
            existing.setDescripcion(request.descripcion());
            if (request.siteId() != null) {
                existing.setSite(siteBusiness.load(request.siteId()));
            } else {
                existing.setSite(null);
            }
            existing.setObservaciones(request.observaciones());

            if (request.hasItems()) {
                // Actualizar ítems: orphanRemoval elimina los anteriores, se reemplazan
                existing.getItems().clear();
                derivarTipoCompraDesdeItems(existing, request.items());
                existing.setTipoEquipo(null);
                existing.setDescripcionEquipo(null);
                for (CompraItemRequest itemReq : request.items()) {
                    validarItem(itemReq);
                    CompraItem item = new CompraItem();
                    item.setCompra(existing);
                    item.setTipoCompra(itemReq.tipoCompra());
                    item.setTipoEquipo(itemReq.tipoEquipo());
                    item.setDescripcion(itemReq.descripcion());
                    item.setImporte(itemReq.importe());
                    existing.getItems().add(item);
                }
            } else if (request.tipoCompra() == TipoCompra.EQUIPO) {
                if (request.tipoEquipo() == null) {
                    throw new BusinessException("Se debe indicar el tipoEquipo cuando el tipo de compra es EQUIPO");
                }
                existing.setTipoCompra(request.tipoCompra());
                existing.setTipoEquipo(request.tipoEquipo());
                existing.setDescripcionEquipo(request.descripcionEquipo());
            } else {
                if (request.tipoCompra() != null) existing.setTipoCompra(request.tipoCompra());
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
            long licencias = licenciaRepository.countByCompraId(id);
            if (licencias > 0) {
                throw new BusinessException(
                    "No se puede eliminar la compra porque tiene " + licencias +
                    " licencia(s) asociada(s). Eliminá las licencias primero.");
            }
            long seguros = seguroRepository.countByCompraId(id);
            if (seguros > 0) {
                throw new BusinessException(
                    "No se puede eliminar la compra porque tiene " + seguros +
                    " seguro(s) asociado(s). Eliminá los seguros primero.");
            }
            repository.deleteById(id);
        } catch (NotFoundException e) {
            throw e;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error al eliminar compra con id {}", id, e);
            throw new BusinessException("Error al eliminar compra", e);
        }
    }

    /**
     * Valida que un CompraItemRequest tenga los campos necesarios según su tipo.
     */
    private void validarItem(CompraItemRequest item) throws BusinessException {
        if (item.descripcion() == null || item.descripcion().isBlank()) {
            throw new BusinessException("La descripción de cada ítem es obligatoria");
        }
        if (item.tipoCompra() == TipoCompra.EQUIPO && item.tipoEquipo() == null) {
            throw new BusinessException("Se debe indicar tipoEquipo para ítems de tipo EQUIPO");
        }
    }

    /**
     * Deriva el tipoCompra de la cabecera Compra a partir de los ítems.
     * Si todos los ítems son del mismo tipo → ese tipo. Si hay mezcla → OTRO.
     */
    private void derivarTipoCompraDesdeItems(Compra compra, java.util.List<CompraItemRequest> items) {
        TipoCompra primero = items.get(0).tipoCompra();
        boolean todosMismo = items.stream().allMatch(i -> i.tipoCompra() == primero);
        compra.setTipoCompra(todosMismo ? primero : TipoCompra.OTRO);
    }

    /**
     * Auto-crea la entidad en el sistema según el tipo de ítem.
     * EQUIPO → inventario, LICENCIA_SW → Licencia, SEGURO → Seguro.
     * El resto (VIATICO, FLETES, etc.) solo registra el gasto, sin entidad adicional.
     */
    private void crearEntidadDesdeItem(Compra compra, CompraItem item) throws BusinessException {
        int cantidad = item.getCantidad() != null && item.getCantidad() > 0 ? item.getCantidad() : 1;
        for (int i = 0; i < cantidad; i++) {
            switch (item.getTipoCompra()) {
                case EQUIPO -> {
                    if (item.getTipoEquipo() != null) {
                        crearItemEnInventarioPorTipo(item.getTipoEquipo(), item.getDescripcion(), compra.getFechaCompra());
                    }
                }
                case LICENCIA_SW -> {
                    Licencia licencia = new Licencia();
                    licencia.setNombre(item.getDescripcion());
                    licencia.setCompra(compra);
                    licencia.setFechaCompra(compra.getFechaCompra());
                    licencia.setActivo(true);
                    licenciaBusiness.add(licencia);
                    log.info("Licencia SW '{}' creada desde ítem de compra id={}", item.getDescripcion(), compra.getId());
                }
                case SEGURO -> {
                    Seguro seguro = new Seguro();
                    seguro.setAseguradora(item.getDescripcion());
                    seguro.setCompra(compra);
                    seguroBusiness.add(seguro);
                    log.info("Seguro '{}' creado desde ítem de compra id={}", item.getDescripcion(), compra.getId());
                }
                default -> {
                    // REPUESTO, VIATICO, FLETES, MOVILIZACION, SERVICIOS, OTRO: solo gasto, sin entidad.
                }
            }
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
     * Crea automáticamente el ítem en inventario cuando se registra una compra de tipo EQUIPO (flujo legacy).
     */
    private void crearItemEnInventario(Compra compra) throws BusinessException {
        crearItemEnInventarioPorTipo(compra.getTipoEquipo(), compra.getDescripcionEquipo(), compra.getFechaCompra());
    }

    /**
     * Crea la entidad de inventario correspondiente al tipo de equipo dado.
     */
    private void crearItemEnInventarioPorTipo(com.gestion.qnt.model.enums.TipoEquipo tipoEquipo,
                                               String nombre,
                                               java.time.LocalDate fechaCompra) throws BusinessException {
        switch (tipoEquipo) {
            case DRON -> {
                Dron dron = new Dron();
                dron.setEstado(Estado.NO_LLEGO);
                dron.setNombre(nombre);
                dron.setFechaCompra(fechaCompra);
                dronBusiness.add(dron);
            }
            case BATERIA -> {
                Bateria bateria = new Bateria();
                bateria.setEstado(Estado.NO_LLEGO);
                bateria.setNombre(nombre);
                bateria.setFechaCompra(fechaCompra);
                bateriaBusiness.add(bateria);
            }
            case HELICE -> {
                Helice helice = new Helice();
                helice.setEstado(Estado.NO_LLEGO);
                helice.setNombre(nombre);
                helice.setFechaCompra(fechaCompra);
                heliceBusiness.add(helice);
            }
            case DOCK -> {
                Dock dock = new Dock();
                dock.setEstado(Estado.NO_LLEGO);
                dock.setNombre(nombre);
                dock.setFechaCompra(fechaCompra);
                dockBusiness.add(dock);
            }
            case ANTENA_RTK -> {
                AntenaRtk antenaRtk = new AntenaRtk();
                antenaRtk.setEstado(Estado.NO_LLEGO);
                antenaRtk.setNombre(nombre);
                antenaRtk.setFechaCompra(fechaCompra);
                antenaRtkBusiness.add(antenaRtk);
            }
            case ANTENA_STARLINK -> {
                AntenaStarlink antenaStarlink = new AntenaStarlink();
                antenaStarlink.setEstado(Estado.NO_LLEGO);
                antenaStarlink.setNombre(nombre);
                antenaStarlink.setFechaCompra(fechaCompra);
                antenaStarlinkBusiness.add(antenaStarlink);
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
