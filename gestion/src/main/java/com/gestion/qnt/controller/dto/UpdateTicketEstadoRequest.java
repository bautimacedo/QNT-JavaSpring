package com.gestion.qnt.controller.dto;

import com.gestion.qnt.model.enums.EstadoTicket;

public record UpdateTicketEstadoRequest(
        EstadoTicket estado,
        String notaResolucion
) {}
