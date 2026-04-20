package com.gestion.qnt.controller.dto;

import com.gestion.qnt.model.enums.TipoRecurrencia;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

public class ProgramacionMisionRequest {

    public Long misionPlantillaId;
    public TipoRecurrencia tipoRecurrencia;
    public Set<DayOfWeek> diasSemana;
    public Integer diaMes;
    public Integer intervaloDias;
    public LocalTime hora;
    public Boolean activa;
    public LocalDate fechaInicioVigencia;
    public LocalDate fechaFinVigencia;
}
