package com.gestion.qnt.mqtt;

import com.gestion.qnt.model.Dock;
import com.gestion.qnt.model.Dron;
import com.gestion.qnt.repository.BateriaRepository;
import com.gestion.qnt.repository.DockRepository;
import com.gestion.qnt.repository.DronRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Component
public class TelemetriaScheduler {

    private static final Logger log = LoggerFactory.getLogger(TelemetriaScheduler.class);

    private final DronTelemetriaService telemetriaService;
    private final DockRepository dockRepository;
    private final DronRepository dronRepository;
    private final BateriaRepository bateriaRepository;

    public TelemetriaScheduler(DronTelemetriaService telemetriaService,
                                DockRepository dockRepository,
                                DronRepository dronRepository,
                                BateriaRepository bateriaRepository) {
        this.telemetriaService = telemetriaService;
        this.dockRepository = dockRepository;
        this.dronRepository = dronRepository;
        this.bateriaRepository = bateriaRepository;
    }

    @Scheduled(fixedRateString = "${mqtt.flush-interval-ms:300000}") // 5 minutos por defecto
    @Transactional
    public void persistirTelemetria() {
        persistirDocks();
        persistirDrones();
    }

    private void persistirDocks() {
        telemetriaService.getDockSnapshots().values().forEach(snap -> {
            if (snap.sn == null) return;

            dockRepository.findByNumeroSerie(snap.sn).ifPresentOrElse(dock -> {
                actualizarDock(dock, snap);
                dockRepository.save(dock);

                // Actualizar drone asociado en dock
                if (snap.droneSn != null) {
                    dronRepository.findByNumeroSerie(snap.droneSn).ifPresent(dron -> {
                        actualizarDronDesdeDock(dron, snap);
                        dronRepository.save(dron);

                        // Actualizar batería si tenemos ciclos
                        if (snap.droneBateriaCiclos != null) {
                            actualizarBateriaDesdeDock(dron, snap);
                        }
                    });
                }

                log.info("Dock {} actualizado: tempAmb={}°C viento={} m/s", snap.sn, snap.temperaturaAmbiente, snap.velocidadViento);
            }, () -> log.warn("Dock con SN {} no encontrado en BD", snap.sn));
        });
    }

    private void persistirDrones() {
        telemetriaService.getDronSnapshots().values().forEach(snap -> {
            if (snap.sn == null) return;

            dronRepository.findByNumeroSerie(snap.sn).ifPresentOrElse(dron -> {
                if (snap.latitud != null) dron.setLatitud(snap.latitud);
                if (snap.longitud != null) dron.setLongitud(snap.longitud);
                if (snap.altitud != null) dron.setAltitud(snap.altitud);
                if (snap.bateriaPorc != null) dron.setBateriaPorc(snap.bateriaPorc);
                if (snap.bateriaTempC != null) dron.setBateriaTempC(snap.bateriaTempC);
                dron.setDroneEnDock(false);
                dron.setUltimaTelemetria(Instant.now());
                dronRepository.save(dron);

                // Actualizar batería por SN si la tenemos
                if (snap.bateriaSn != null && snap.bateriaCiclos != null) {
                    bateriaRepository.findByNumeroSerie(snap.bateriaSn).ifPresent(bat -> {
                        bat.setCiclosCarga(snap.bateriaCiclos);
                        bateriaRepository.save(bat);
                    });
                }

                log.info("Dron {} actualizado en vuelo: lat={} lon={} bat={}%", snap.sn, snap.latitud, snap.longitud, snap.bateriaPorc);
            }, () -> log.warn("Dron con SN {} no encontrado en BD", snap.sn));
        });
    }

    private void actualizarDock(Dock dock, DockSnapshot snap) {
        if (snap.latitud != null) dock.setLatitud(snap.latitud);
        if (snap.longitud != null) dock.setLongitud(snap.longitud);
        if (snap.altitud != null) dock.setAltitud(snap.altitud);
        if (snap.temperaturaAmbiente != null) dock.setTemperaturaAmbiente(snap.temperaturaAmbiente);
        if (snap.velocidadViento != null) dock.setVelocidadViento(snap.velocidadViento);
        dock.setUltimaTelemetria(Instant.now());
    }

    private void actualizarDronDesdeDock(Dron dron, DockSnapshot snap) {
        if (snap.droneBateriaPorc != null) dron.setBateriaPorc(snap.droneBateriaPorc);
        if (snap.droneBateriaTempC != null) dron.setBateriaTempC(snap.droneBateriaTempC);
        dron.setDroneEnDock(snap.droneEnDock != null ? snap.droneEnDock : true);
        dron.setUltimaTelemetria(Instant.now());
    }

    private void actualizarBateriaDesdeDock(Dron dron, DockSnapshot snap) {
        // Busca la batería activa del drone por el dron_id
        dron.getBaterias().stream()
                .filter(b -> b.getCiclosCarga() != null || snap.droneBateriaCiclos != null)
                .findFirst()
                .ifPresent(bat -> {
                    if (snap.droneBateriaCiclos != null) bat.setCiclosCarga(snap.droneBateriaCiclos);
                    bateriaRepository.save(bat);
                });
    }
}
