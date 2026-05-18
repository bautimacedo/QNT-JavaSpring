package com.gestion.qnt.controller.dto;

public record DashboardStatsResponse(
        // Flota drones
        long dronesTotal,
        long dronesOperativos,
        long dronesEnMantenimiento,
        long dronesEnDesuso,

        // Misiones
        long misionesTotal,
        long misionesPlanificadas,
        long misionesEnCurso,
        long misionesCompletadas,
        long misionesCanceladas,
        long misionesFalloLanzamiento,

        // Alertas
        long alertasActivas,

        // Advertencias de ciclos y temperatura (datos live)
        long bateriasCiclosExcedidos,
        long dronesConTempAlta
) {}
