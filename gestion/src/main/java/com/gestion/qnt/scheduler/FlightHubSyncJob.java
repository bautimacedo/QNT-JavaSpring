package com.gestion.qnt.scheduler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gestion.qnt.model.Dron;
import com.gestion.qnt.model.Mision;
import com.gestion.qnt.model.enums.EstadoMision;
import com.gestion.qnt.model.enums.Yacimiento;
import com.gestion.qnt.repository.DronRepository;
import com.gestion.qnt.repository.MisionRepository;
import com.gestion.qnt.service.FlightHubService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class FlightHubSyncJob {

    private static final Logger log = LoggerFactory.getLogger(FlightHubSyncJob.class);

    @Autowired private FlightHubService flightHubService;
    @Autowired private MisionRepository misionRepository;
    @Autowired private DronRepository dronRepository;

    private final ObjectMapper mapper = new ObjectMapper();

    @Scheduled(cron = "0 0 */6 * * *")
    @Transactional
    public void sync() {
        List<Map<String, Object>> waylines;
        try {
            waylines = flightHubService.listarWaylines();
        } catch (Exception e) {
            log.error("FlightHubSyncJob: no se pudo contactar FlightHub: {}", e.getMessage());
            return;
        }

        List<Map<String, Object>> filtradas = waylines.stream()
                .filter(w -> {
                    String name = (String) w.get("name");
                    return name != null && name.toLowerCase().contains("inspect");
                })
                .toList();

        Dron dronCam = dronRepository.findByYacimiento(Yacimiento.CAM).stream().findFirst().orElse(null);

        Set<String> uuidsFlightHub = filtradas.stream()
                .map(w -> (String) w.get("id"))
                .collect(Collectors.toSet());

        int creadas = 0, actualizadas = 0;
        for (Map<String, Object> w : filtradas) {
            String uuid = (String) w.get("id");
            String nombre = (String) w.get("name");
            if (uuid == null || uuid.isBlank()) continue;

            Long updateTime = w.get("update_time") instanceof Number n ? n.longValue() : null;

            Mision m = misionRepository.findByFlightHubWaylineUuid(uuid).orElse(null);
            boolean esNueva = (m == null);

            if (esNueva) {
                m = new Mision();
                m.setNombre(nombre);
                m.setFlightHubWaylineUuid(uuid);
                m.setEstado(EstadoMision.PLANIFICADA);
                m.setDron(dronCam);
            }

            boolean cambio = esNueva || (updateTime != null && !updateTime.equals(m.getWaylineUpdateTime()));
            if (cambio) {
                poblarDetalleWayline(m, uuid, updateTime);
                misionRepository.save(m);
                if (esNueva) creadas++; else actualizadas++;
                log.info("FlightHubSyncJob: {} '{}' ({})", esNueva ? "creada" : "actualizada", nombre, uuid);
            }
        }

        // Misiones CAM PLANIFICADA que ya no existen en FlightHub → CANCELADA
        int canceladas = 0;
        for (Mision m : misionRepository.findCAMMisionesByEstado(EstadoMision.PLANIFICADA)) {
            if (!uuidsFlightHub.contains(m.getFlightHubWaylineUuid())) {
                m.setEstado(EstadoMision.CANCELADA);
                misionRepository.save(m);
                canceladas++;
                log.warn("FlightHubSyncJob: cancelada '{}' (wayline ya no existe en FlightHub)", m.getNombre());
            }
        }

        log.info("FlightHubSyncJob: creadas={}, actualizadas={}, canceladas={}", creadas, actualizadas, canceladas);
    }

    private void poblarDetalleWayline(Mision m, String uuid, Long updateTime) {
        try {
            Map<String, Object> detalle = flightHubService.obtenerDetalleWayline(uuid);
            if (detalle.isEmpty()) return;

            m.setWaylineUpdateTime(updateTime);
            m.setDistanciaMetros(detalle.get("distance") instanceof Number n ? n.doubleValue() : null);
            m.setWaypointCount(detalle.get("wayline_point_nums") instanceof Number n ? n.intValue() : null);

            String downloadUrl = (String) detalle.get("download_url");
            if (downloadUrl != null && !downloadUrl.isBlank()) {
                List<double[]> coords = flightHubService.parsearCoordenadasKmz(downloadUrl);
                if (!coords.isEmpty()) {
                    m.setCoordenadasJson(mapper.writeValueAsString(
                            coords.stream().map(c -> Map.of("lat", c[0], "lon", c[1])).toList()
                    ));
                }
            }
        } catch (Exception e) {
            log.error("FlightHubSyncJob: error al poblar detalle de wayline {}: {}", uuid, e.getMessage());
        }
    }
}
