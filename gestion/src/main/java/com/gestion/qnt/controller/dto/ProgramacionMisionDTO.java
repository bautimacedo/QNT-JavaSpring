package com.gestion.qnt.controller.dto;

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
    public LocalDate fechaInicioVigencia;
    public LocalDate fechaFinVigencia;
    public LocalDateTime ultimaEjecucion;
    public LocalDateTime proxEjecucion;
    public LocalDateTime fechaCreacion;

    // plantilla
    public Long misionPlantillaId;
    public String misionPlantillaNombre;
    public String misionPlantillaDronNombre;
    public boolean misionPlantillaTieneWebhook;
}
