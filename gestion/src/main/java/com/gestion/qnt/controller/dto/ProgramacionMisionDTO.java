package com.gestion.qnt.controller.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.gestion.qnt.model.enums.TipoRecurrencia;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class ProgramacionMisionDTO {

    public Long id;
    public String nombre;
    public TipoRecurrencia tipoRecurrencia;
    public List<String> diasSemana;
    public Integer diaMes;
    public Integer intervaloDias;
    public String hora;
    public boolean activa;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    public LocalDate fechaInicioVigencia;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    public LocalDate fechaFinVigencia;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    public LocalDateTime ultimaEjecucion;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    public LocalDateTime proxEjecucion;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    public LocalDateTime fechaCreacion;

    // plantilla
    public Long misionPlantillaId;
    public String misionPlantillaNombre;
    public String misionPlantillaDronNombre;
    public boolean misionPlantillaTieneWebhook;
    public String misionPlantillaSite;
    public String misionPlantillaWaylineUuid;
}
