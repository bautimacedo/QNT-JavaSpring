package com.gestion.qnt.mqtt;

import com.gestion.qnt.model.*;
import com.gestion.qnt.model.enums.Estado;
import com.gestion.qnt.model.enums.EstadoMision;
import com.gestion.qnt.model.enums.TipoEventoVuelo;
import com.gestion.qnt.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;

@Component
public class DroneVueloEventListener {

    private static final Logger log = LoggerFactory.getLogger(DroneVueloEventListener.class);

    private final DronRepository dronRepository;
    private final DockRepository dockRepository;
    private final MisionRepository misionRepository;
    private final UsuarioRepository usuarioRepository;
    private final VueloLogRepository vueloLogRepository;

    public DroneVueloEventListener(DronRepository dronRepository,
                                   DockRepository dockRepository,
                                   MisionRepository misionRepository,
                                   UsuarioRepository usuarioRepository,
                                   VueloLogRepository vueloLogRepository) {
        this.dronRepository = dronRepository;
        this.dockRepository = dockRepository;
        this.misionRepository = misionRepository;
        this.usuarioRepository = usuarioRepository;
        this.vueloLogRepository = vueloLogRepository;
    }

    @Async
    @EventListener
    @Transactional
    public void onVueloEvent(DroneVueloEvent event) {
        if (event.droneSn == null) {
            log.warn("DroneVueloEvent ignorado: droneSn null (dock {} aún no recibió sub_device)", event.dockSn);
            return;
        }
        Dron dron = resolverDron(event.droneSn);
        String dockNombre = dockRepository.findByNumeroSerie(event.dockSn)
                .map(Dock::getNombre).orElse(event.dockSn);

        if (event.tipo == DroneVueloEvent.Tipo.DESPEGUE) {
            manejarDespegue(event, dron, dockNombre);
        } else {
            manejarAterrizaje(event, dron, dockNombre);
        }
    }

    private void manejarDespegue(DroneVueloEvent event, Dron dron, String dockNombre) {
        if (dron != null) {
            dron.setDroneEnDock(false);
            dronRepository.save(dron);
        }

        VueloLog log = new VueloLog();
        log.setEvento(TipoEventoVuelo.DESPEGUE);
        log.setSite("CAM");
        log.setNombreDron(dron != null ? dron.getNombre() : event.droneSn);
        log.setNombreDock(dockNombre);
        log.setTimestampFlytbase(event.timestamp);
        log.setDespegueFallido(false);
        log.setEventId("MQTT_DESP_" + event.droneSn + "_" + event.timestamp.getEpochSecond());
        vueloLogRepository.save(log);

        DroneVueloEventListener.log.info("DESPEGUE CAM procesado — drone {} dock {}", event.droneSn, event.dockSn);
    }

    private void manejarAterrizaje(DroneVueloEvent event, Dron dron, String dockNombre) {
        Integer duracion = event.duracionMinutos != null ? event.duracionMinutos : 0;
        String dronNombre = dron != null ? dron.getNombre() : event.droneSn;

        if (dron != null) {
            dron.setDroneEnDock(true);
            dron.setUltimoVuelo(event.timestamp);
            dronRepository.save(dron);
        }

        VueloLog vuelo = new VueloLog();
        vuelo.setEvento(TipoEventoVuelo.VUELO);
        vuelo.setSite("CAM");
        vuelo.setNombreDron(dronNombre);
        vuelo.setNombreDock(dockNombre);
        vuelo.setTimestampFlytbase(event.timestamp);
        vuelo.setDuracionMinutos(duracion);
        vuelo.setDespegueFallido(false);
        vuelo.setEventId("MQTT_VUELO_" + event.droneSn + "_" + event.timestamp.getEpochSecond());
        vueloLogRepository.save(vuelo);

        completarMisionEnCurso(dronNombre, duracion);

        DroneVueloEventListener.log.info("ATERRIZAJE CAM procesado — drone {} dock {} duración {}min",
                event.droneSn, event.dockSn, duracion);
    }

    private void completarMisionEnCurso(String dronNombre, int duracion) {
        misionRepository.findByDron_NombreAndEstado(dronNombre, EstadoMision.EN_CURSO)
                .stream().findFirst().ifPresent(m -> {
                    m.setEstado(EstadoMision.PLANIFICADA);
                    m.setFechaFin(LocalDateTime.now());
                    misionRepository.save(m);

                    Dron dron = m.getDron();
                    if (dron != null) {
                        dron.setCantidadVuelos((dron.getCantidadVuelos() != null ? dron.getCantidadVuelos() : 0) + 1);
                        dron.setCantidadMinutosVolados((dron.getCantidadMinutosVolados() != null ? dron.getCantidadMinutosVolados() : 0) + duracion);
                        dronRepository.save(dron);

                        for (Bateria bat : dron.getBaterias()) {
                            if (bat.getEstado() == Estado.STOCK_ACTIVO) {
                                bat.setCantidadVuelos((bat.getCantidadVuelos() != null ? bat.getCantidadVuelos() : 0) + 1);
                                bat.setCantidadMinutosVolados((bat.getCantidadMinutosVolados() != null ? bat.getCantidadMinutosVolados() : 0) + duracion);
                                bat.setCiclosCarga((bat.getCiclosCarga() != null ? bat.getCiclosCarga() : 0) + 1);
                            }
                        }

                        for (Helice helice : dron.getHelices()) {
                            if (helice.getEstado() == Estado.STOCK_ACTIVO) {
                                helice.setCantidadVuelos((helice.getCantidadVuelos() != null ? helice.getCantidadVuelos() : 0) + 1);
                                helice.setCantidadMinutosVolados((helice.getCantidadMinutosVolados() != null ? helice.getCantidadMinutosVolados() : 0) + duracion);
                            }
                        }
                    }

                    Usuario piloto = m.getPiloto();
                    if (piloto != null) {
                        double horas = piloto.getHorasVuelo() != null ? piloto.getHorasVuelo() : 0.0;
                        piloto.setHorasVuelo(Math.round((horas + duracion / 60.0) * 100.0) / 100.0);
                        piloto.setCantidadVuelos((piloto.getCantidadVuelos() != null ? piloto.getCantidadVuelos() : 0) + 1);
                        usuarioRepository.save(piloto);
                    }

                    DroneVueloEventListener.log.info("Misión {} completada tras aterrizaje MQTT ({}min)", m.getNombre(), duracion);
                });
    }

    private Dron resolverDron(String droneSn) {
        if (droneSn == null) return null;
        java.util.Optional<Dron> opt = dronRepository.findByNumeroSerie(droneSn);
        if (opt.isEmpty()) opt = dronRepository.findBySnMqtt(droneSn);
        return opt.orElse(null);
    }
}
