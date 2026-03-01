package com.gestion.qnt.controller.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

/**
 * DTO para crear o actualizar un Seguro.
 * compraId se resuelve en el controller a la entidad Compra.
 */
public record CreateSeguroRequest(
        @NotNull(message = "aseguradora es obligatoria")
        String aseguradora,

        String numeroPoliza,
        LocalDate vigenciaDesde,
        LocalDate vigenciaHasta,
        String observaciones,
        Long compraId
) {}
