package com.gestion.qnt.controller.dto;

import com.gestion.qnt.model.enums.TipoCompra;
import com.gestion.qnt.model.enums.TipoEquipo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * DTO para cada ítem de una compra.
 * descripcion: nombre del ítem (ej: "DJI Matrice 3D", "Terra Pro 1 año", "Allianz drone seguro").
 * tipoEquipo: solo aplica cuando tipoCompra = EQUIPO.
 * importe: monto parcial del ítem (informativo; el total es el importe de la cabecera Compra).
 */
public record CompraItemRequest(

        @NotNull(message = "tipoCompra del ítem es obligatorio")
        TipoCompra tipoCompra,

        TipoEquipo tipoEquipo,

        @NotBlank(message = "descripcion del ítem es obligatoria")
        String descripcion,

        BigDecimal importe
) {}
