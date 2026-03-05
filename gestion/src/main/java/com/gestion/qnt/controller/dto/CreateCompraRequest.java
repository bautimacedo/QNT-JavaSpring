package com.gestion.qnt.controller.dto;

import com.gestion.qnt.model.enums.MetodoPago;
import com.gestion.qnt.model.enums.TipoCompra;
import com.gestion.qnt.model.enums.TipoEquipo;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO para crear o actualizar una Compra.
 * Proveedor: indicar proveedorId (existente) o proveedorNombre (se crea si no existe).
 * Site: siteId opcional.
 * tipoEquipo y descripcionEquipo: solo aplican cuando tipoCompra = EQUIPO.
 * metodoPago y datos de tarjeta: metodoPago obligatorio; datos de tarjeta solo válidos cuando metodoPago = TARJETA.
 * IVA: tieneIva opcional (default false). Si true, ivaPorcentaje es obligatorio (ej: 21.00). El importe es siempre el TOTAL.
 */
public record CreateCompraRequest(
        Long proveedorId,
        String proveedorNombre,

        @NotNull(message = "fechaCompra es obligatoria")
        LocalDate fechaCompra,

        LocalDate fechaFactura,

        @NotNull(message = "importe es obligatorio")
        @DecimalMin(value = "0", inclusive = false, message = "importe debe ser mayor que 0")
        BigDecimal importe,

        Boolean tieneIva,
        BigDecimal ivaPorcentaje,

        String moneda,

        @NotNull(message = "tipoCompra es obligatorio")
        TipoCompra tipoCompra,

        @NotNull(message = "metodoPago es obligatorio")
        MetodoPago metodoPago,
        String companiaTarjeta,
        String ultimos4Tarjeta,

        TipoEquipo tipoEquipo,
        String descripcionEquipo,

        String descripcion,
        Long siteId,
        String observaciones
) {
    public boolean hasProveedor() {
        return (proveedorId != null) || (proveedorNombre != null && !proveedorNombre.isBlank());
    }
}
