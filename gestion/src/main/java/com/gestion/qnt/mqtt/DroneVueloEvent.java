package com.gestion.qnt.mqtt;

import org.springframework.context.ApplicationEvent;

import java.time.Instant;

public class DroneVueloEvent extends ApplicationEvent {

    public enum Tipo { DESPEGUE, ATERRIZAJE }

    public final String dockSn;
    public final String droneSn;
    public final Tipo tipo;
    public final Instant timestamp;
    public final Integer duracionMinutos;

    public DroneVueloEvent(Object source, String dockSn, String droneSn,
                           Tipo tipo, Instant timestamp, Integer duracionMinutos) {
        super(source);
        this.dockSn = dockSn;
        this.droneSn = droneSn;
        this.tipo = tipo;
        this.timestamp = timestamp;
        this.duracionMinutos = duracionMinutos;
    }
}
