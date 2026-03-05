package com.gestion.qnt.controller.dto;

import com.gestion.qnt.model.enums.EstadoUsuario;

import java.time.LocalDate;
import java.util.List;

public record PilotoResumenResponse(
        Long id,
        String nombre,
        String apellido,
        String email,
        Integer horasVuelo,
        Integer cantidadVuelos,
        LocalDate cmaVencimiento,
        EstadoUsuario estado,
        boolean tieneFotoPerfil,
        List<LicenciaANACResumen> licencias
) {
    public record LicenciaANACResumen(
            Long id,
            LocalDate fechaVencimientoCma,
            LocalDate fechaEmision,
            LocalDate caducidad,
            boolean tieneImagenCma,
            boolean tieneImagenCertificadoIdoneidad,
            Boolean activo
    ) {}
}
