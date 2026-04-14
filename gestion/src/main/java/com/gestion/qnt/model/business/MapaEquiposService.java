package com.gestion.qnt.model.business;

import com.gestion.qnt.controller.dto.MapEquipoMarker;
import com.gestion.qnt.model.*;
import com.gestion.qnt.model.business.exceptions.BusinessException;
import com.gestion.qnt.model.business.interfaces.*;
import com.gestion.qnt.model.enums.Estado;
import com.gestion.qnt.model.enums.TipoEquipoMapa;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Servicio que agrega los equipos con coordenadas para el mapa.
 * Solo incluye equipos con latitud y longitud no nulas.
 */
@Service
public class MapaEquiposService {

    @Autowired
    private IDockBusiness dockBusiness;
    @Autowired
    private IDronBusiness dronBusiness;
    @Autowired
    private IHeliceBusiness heliceBusiness;
    @Autowired
    private IAntenaRtkBusiness antenaRtkBusiness;
    @Autowired
    private IAntenaStarlinkBusiness antenaStarlinkBusiness;

    /**
     * Lista todos los equipos que tienen latitud y longitud, listos para pintar en el mapa.
     */
    @Transactional(readOnly = true)
    public List<MapEquipoMarker> getEquiposParaMapa() throws BusinessException {
        List<MapEquipoMarker> out = new ArrayList<>();

        try {
            Stream<MapEquipoMarker> docks = dockBusiness.list().stream()
                    .filter(d -> d.getLatitud() != null && d.getLongitud() != null)
                    .map(this::dockToMarker);
            Stream<MapEquipoMarker> drones = dronBusiness.list().stream()
                    .filter(d -> d.getLatitud() != null && d.getLongitud() != null)
                    .map(this::dronToMarker);

            Stream<MapEquipoMarker> helices = heliceBusiness.list().stream()
                    .filter(h -> h.getLatitud() != null && h.getLongitud() != null)
                    .map(this::heliceToMarker);
            Stream<MapEquipoMarker> antenasRtk = antenaRtkBusiness.list().stream()
                    .filter(a -> a.getLatitud() != null && a.getLongitud() != null)
                    .map(this::antenaRtkToMarker);
            Stream<MapEquipoMarker> antenasStarlink = antenaStarlinkBusiness.list().stream()
                    .filter(a -> a.getLatitud() != null && a.getLongitud() != null)
                    .map(this::antenaStarlinkToMarker);

            Stream.of(docks, drones, helices, antenasRtk, antenasStarlink)
                    .flatMap(s -> s)
                    .forEach(out::add);
        } catch (Exception e) {
            throw new BusinessException("Error al obtener equipos para el mapa", e);
        }

        return out;
    }

    private static String nombreODerivado(String nombre, String marca, String modelo, Long id, String prefijo) {
        if (nombre != null && !nombre.isBlank()) return nombre;
        if (marca != null || modelo != null) return (marca != null ? marca : "").trim() + " " + (modelo != null ? modelo : "").trim();
        return prefijo + " " + id;
    }

    private MapEquipoMarker dockToMarker(Dock d) {
        LocalDate ultima = d.getUltimoMantenimiento() != null && d.getUltimoMantenimiento().getFechaMantenimiento() != null
                ? d.getUltimoMantenimiento().getFechaMantenimiento().toLocalDate()
                : null;
        String siteNombre = d.getSite() != null ? d.getSite().getNombre() : null;
        return new MapEquipoMarker(
                TipoEquipoMapa.DOCK, d.getId(),
                nombreODerivado(d.getNombre(), d.getMarca(), d.getModelo(), d.getId(), "Dock"),
                d.getLatitud(), d.getLongitud(), d.getAltitud(), d.getEstado(),
                ultima, siteNombre, d.getNumeroSerie(),
                null, null, null, null, null,
                d.getTemperaturaAmbiente(), d.getVelocidadViento(), d.getUltimaTelemetria()
        );
    }

    private MapEquipoMarker dronToMarker(Dron d) {
        LocalDate ultima = d.getUltimoMantenimiento() != null && d.getUltimoMantenimiento().getFechaMantenimiento() != null
                ? d.getUltimoMantenimiento().getFechaMantenimiento().toLocalDate()
                : null;
        Bateria bateriaActiva = d.getBaterias().stream()
                .filter(b -> b.getEstado() == Estado.STOCK_ACTIVO)
                .findFirst().orElse(null);
        String bateriaNombre = bateriaActiva != null ? bateriaActiva.getNombre() : null;
        Integer bateriaCiclos = bateriaActiva != null ? bateriaActiva.getCiclosCarga() : null;
        return new MapEquipoMarker(
                TipoEquipoMapa.DRON, d.getId(),
                nombreODerivado(d.getNombre(), d.getMarca(), d.getModelo(), d.getId(), "Dron"),
                d.getLatitud(), d.getLongitud(), d.getAltitud(), d.getEstado(),
                ultima, null, d.getNumeroSerie(),
                d.getBateriaPorc(), d.getBateriaTempC(), d.getDroneEnDock(),
                bateriaNombre, bateriaCiclos,
                null, null, d.getUltimaTelemetria()
        );
    }

    private MapEquipoMarker heliceToMarker(Helice h) {
        return new MapEquipoMarker(
                TipoEquipoMapa.HELICE, h.getId(),
                nombreODerivado(h.getNombre(), h.getMarca(), h.getModelo(), h.getId(), "Hélice"),
                h.getLatitud(), h.getLongitud(), h.getAltitud(), h.getEstado(),
                null, null, h.getNumeroSerie(),
                null, null, null, null, null, null, null, null
        );
    }

    private MapEquipoMarker antenaRtkToMarker(AntenaRtk a) {
        return new MapEquipoMarker(
                TipoEquipoMapa.ANTENA_RTK, a.getId(),
                nombreODerivado(a.getNombre(), a.getMarca(), a.getModelo(), a.getId(), "Antena RTK"),
                a.getLatitud(), a.getLongitud(), a.getAltitud(), a.getEstado(),
                null, null, a.getNumeroSerie(),
                null, null, null, null, null, null, null, null
        );
    }

    private MapEquipoMarker antenaStarlinkToMarker(AntenaStarlink a) {
        return new MapEquipoMarker(
                TipoEquipoMapa.ANTENA_STARLINK, a.getId(),
                nombreODerivado(a.getNombre(), a.getMarca(), a.getModelo(), a.getId(), "Antena Starlink"),
                a.getLatitud(), a.getLongitud(), a.getAltitud(), a.getEstado(),
                null, null, a.getNumeroSerie(),
                null, null, null, null, null, null, null, null
        );
    }
}
