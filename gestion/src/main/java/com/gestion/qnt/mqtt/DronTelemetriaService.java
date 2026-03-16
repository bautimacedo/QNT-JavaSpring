package com.gestion.qnt.mqtt;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Escucha mensajes MQTT y guarda el último snapshot de telemetría en memoria.
 * El TelemetriaScheduler se encarga de persistir a DB cada 5 minutos.
 */
@Service
public class DronTelemetriaService {

    private static final Logger log = LoggerFactory.getLogger(DronTelemetriaService.class);

    private final ObjectMapper objectMapper = new ObjectMapper();

    // Último snapshot por SN del dispositivo
    private final Map<String, DockSnapshot> dockSnapshots = new ConcurrentHashMap<>();
    private final Map<String, DronSnapshot> dronSnapshots = new ConcurrentHashMap<>();

    public void procesarMensaje(String topic, String payload) {
        // Solo procesamos OSD
        if (topic == null || !topic.endsWith("/osd")) return;

        // Extraemos el SN del topic: thing/product/{SN}/osd
        String[] parts = topic.split("/");
        if (parts.length < 4) return;
        String sn = parts[2];

        try {
            JsonNode root = objectMapper.readTree(payload);
            JsonNode host = root.path("data").path("host");

            // --- Drone volando: tiene latitude/longitude directamente en host ---
            if (host.has("latitude") && host.has("longitude") && !host.has("drone_in_dock")) {
                parsearDrone(sn, host);
                return;
            }

            // --- Dock: tiene drone_in_dock ---
            if (host.has("drone_in_dock")) {
                parsearDock(sn, host);
                return;
            }

            // --- Batería del drone (mensaje del dock con drone_battery_maintenance_info) ---
            if (host.has("drone_battery_maintenance_info")) {
                parsearBateriaDesdeDock(sn, host);
            }

        } catch (Exception e) {
            log.warn("Error procesando mensaje MQTT topic={}: {}", topic, e.getMessage());
        }
    }

    private void parsearDrone(String sn, JsonNode host) {
        DronSnapshot snap = dronSnapshots.computeIfAbsent(sn, k -> new DronSnapshot());
        snap.sn = sn;

        if (host.has("latitude")) snap.latitud = host.get("latitude").decimalValue();
        if (host.has("longitude")) snap.longitud = host.get("longitude").decimalValue();
        if (host.has("height")) snap.altitud = host.get("height").decimalValue();
        snap.enDock = false;

        JsonNode battery = host.path("battery");
        if (!battery.isMissingNode()) {
            if (battery.has("capacity_percent")) snap.bateriaPorc = battery.get("capacity_percent").asInt();
            JsonNode bats = battery.path("batteries");
            if (bats.isArray() && bats.size() > 0) {
                JsonNode b = bats.get(0);
                if (b.has("temperature")) snap.bateriaTempC = b.get("temperature").decimalValue();
                if (b.has("loop_times")) snap.bateriaCiclos = b.get("loop_times").asInt();
                if (b.has("sn")) snap.bateriaSn = b.get("sn").asText();
            }
        }

        log.debug("Telemetría drone {} en vuelo: lat={} lon={} bat={}%", sn, snap.latitud, snap.longitud, snap.bateriaPorc);
    }

    private void parsearDock(String sn, JsonNode host) {
        DockSnapshot snap = dockSnapshots.computeIfAbsent(sn, k -> new DockSnapshot());
        snap.sn = sn;

        if (host.has("latitude")) snap.latitud = host.get("latitude").decimalValue();
        if (host.has("longitude")) snap.longitud = host.get("longitude").decimalValue();
        if (host.has("height")) snap.altitud = host.get("height").decimalValue();
        if (host.has("environment_temperature")) snap.temperaturaAmbiente = host.get("environment_temperature").decimalValue();
        if (host.has("wind_speed")) snap.velocidadViento = host.get("wind_speed").decimalValue();

        // Batería del drone en dock
        JsonNode chargeState = host.path("drone_charge_state");
        if (!chargeState.isMissingNode()) {
            snap.droneBateriaPorc = chargeState.has("capacity_percent") ? chargeState.get("capacity_percent").asInt() : null;
            snap.droneEnDock = host.has("drone_in_dock") && host.get("drone_in_dock").asInt() == 1;
        }

        // SN del drone asociado
        JsonNode subDevice = host.path("sub_device");
        if (!subDevice.isMissingNode() && subDevice.has("device_sn")) {
            snap.droneSn = subDevice.get("device_sn").asText();
        }

        log.debug("Telemetría dock {}: lat={} lon={} tempAmb={}°C viento={} bat={}%",
                sn, snap.latitud, snap.longitud, snap.temperaturaAmbiente, snap.velocidadViento, snap.droneBateriaPorc);
    }

    private void parsearBateriaDesdeDock(String sn, JsonNode host) {
        DockSnapshot snap = dockSnapshots.computeIfAbsent(sn, k -> new DockSnapshot());
        JsonNode bats = host.path("drone_battery_maintenance_info").path("batteries");
        if (bats.isArray() && bats.size() > 0) {
            JsonNode b = bats.get(0);
            if (b.has("temperature")) snap.droneBateriaTempC = b.get("temperature").decimalValue();
            if (b.has("loop_times")) snap.droneBateriaCiclos = b.get("loop_times").asInt();
        }
    }

    public Map<String, DockSnapshot> getDockSnapshots() { return dockSnapshots; }
    public Map<String, DronSnapshot> getDronSnapshots() { return dronSnapshots; }
}
