package com.gestion.qnt.controller;

import com.gestion.qnt.config.ApiConstants;
import com.gestion.qnt.controller.dto.MapEquipoMarker;
import com.gestion.qnt.model.business.MapaEquiposService;
import com.gestion.qnt.model.business.exceptions.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Endpoints para el mapa de equipos (coordenadas).
 */
@RestController
@RequestMapping(ApiConstants.URL_BASE + "/mapa")
public class MapaRestController {

    private final MapaEquiposService mapaEquiposService;

    public MapaRestController(MapaEquiposService mapaEquiposService) {
        this.mapaEquiposService = mapaEquiposService;
    }

    /**
     * Devuelve todos los equipos que tienen latitud y longitud, listos para pintar como marcadores.
     * Solo incluye equipos con coordenadas no nulas.
     */
    @GetMapping("/equipos")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<MapEquipoMarker>> getEquipos() {
        try {
            return ResponseEntity.ok(mapaEquiposService.getEquiposParaMapa());
        } catch (BusinessException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
