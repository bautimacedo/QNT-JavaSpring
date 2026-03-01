package com.gestion.qnt.controller.dto;

import com.gestion.qnt.model.enums.TipoCompra;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO para crear o actualizar una Compra.
 * Los IDs (proveedorId, siteId) se resuelven en el controller a entidades.
 */
public record CreateCompraRequest(
        @NotNull(message = "proveedorId es obligatorio")
        Long proveedorId,

        @NotNull(message = "fechaCompra es obligatoria")
        LocalDate fechaCompra,

        LocalDate fechaFactura,
        String numeroFactura,

        @NotNull(message = "importe es obligatorio")
        @DecimalMin(value = "0", inclusive = false, message = "importe debe ser mayor que 0")
        BigDecimal importe,

        String moneda,

        @NotNull(message = "tipoCompra es obligatorio")
        TipoCompra tipoCompra,

        String descripcion,
        Long siteId,
        String observaciones
) {}
