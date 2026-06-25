package com.gestion.qnt.controller.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record UpdateRegistroHoraRequest(
        LocalDate fecha,
        BigDecimal horas,
        String descripcion
) {}
