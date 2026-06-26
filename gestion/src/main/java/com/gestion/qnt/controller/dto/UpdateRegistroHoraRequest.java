package com.gestion.qnt.controller.dto;

import java.time.LocalDate;

public record UpdateRegistroHoraRequest(
        LocalDate fecha,
        String descripcion
) {}
