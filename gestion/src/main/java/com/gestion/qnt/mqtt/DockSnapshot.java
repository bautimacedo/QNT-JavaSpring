package com.gestion.qnt.mqtt;

import java.math.BigDecimal;

public class DockSnapshot {
    public String sn;
    public BigDecimal latitud;
    public BigDecimal longitud;
    public BigDecimal altitud;
    public BigDecimal temperaturaAmbiente;
    public BigDecimal velocidadViento;

    // Drone asociado al dock
    public String droneSn;
    public volatile Boolean droneEnDock;
    public Integer droneBateriaPorc;
    public BigDecimal droneBateriaTempC;
    public Integer droneBateriaCiclos;

    // Debounce: evita disparar eventos por glitches MQTT transitorios
    // volatile: escrito por el thread MQTT, leído por el TelemetriaScheduler
    public volatile Boolean confirmedDroneEnDock = null;
    public int pendingConfirmations = 0;

    // Timestamp del despegue detectado, para calcular duración al aterrizar
    public volatile java.time.Instant despegueTimestamp = null;
}
