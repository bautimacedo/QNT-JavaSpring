package com.gestion.qnt.controller.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Borrador de registro de horas parseado por el asistente IA (el usuario lo revisa antes de guardar). */
public record BorradorHora(LocalDate fecha, BigDecimal horas, String descripcion) {}
