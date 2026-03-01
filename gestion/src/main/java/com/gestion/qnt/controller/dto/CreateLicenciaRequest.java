package com.gestion.qnt.controller.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

/**
 * DTO para crear o actualizar una Licencia.
 * compraId se resuelve en el controller a la entidad Compra.
 */
public record CreateLicenciaRequest(
        @NotNull(message = "nombre es obligatorio")
        String nombre,

        String numLicencia,
        Long compraId,
        LocalDate fechaCompra,
        LocalDate caducidad,
        String version,
        Boolean activo
) {}
