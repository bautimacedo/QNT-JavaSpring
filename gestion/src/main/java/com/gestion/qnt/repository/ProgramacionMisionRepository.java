package com.gestion.qnt.repository;

import com.gestion.qnt.model.ProgramacionMision;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ProgramacionMisionRepository extends JpaRepository<ProgramacionMision, Long> {

    @Query("SELECT p FROM ProgramacionMision p LEFT JOIN FETCH p.misionPlantilla mp LEFT JOIN FETCH mp.dron LEFT JOIN FETCH mp.dock LEFT JOIN FETCH mp.piloto")
    List<ProgramacionMision> findAllWithDetails();

    @Query("SELECT p FROM ProgramacionMision p LEFT JOIN FETCH p.dron LEFT JOIN FETCH p.piloto LEFT JOIN FETCH p.dock WHERE p.activa = true")
    List<ProgramacionMision> findByActivaTrue();

    @Query("SELECT p FROM ProgramacionMision p LEFT JOIN FETCH p.dron LEFT JOIN FETCH p.piloto LEFT JOIN FETCH p.dock WHERE p.activa = true AND p.proxEjecucion < :threshold")
    List<ProgramacionMision> findByActivaTrueAndProxEjecucionBefore(@Param("threshold") LocalDateTime threshold);
}
