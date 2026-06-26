package com.gestion.qnt.controller.dto;

import java.time.LocalDate;

/** Borrador de actividad parseado por el asistente IA (el usuario lo revisa antes de guardar). */
public record BorradorHora(LocalDate fecha, String descripcion) {}
