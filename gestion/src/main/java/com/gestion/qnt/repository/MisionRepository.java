package com.gestion.qnt.repository;

import com.gestion.qnt.model.Mision;
import com.gestion.qnt.model.enums.EstadoMision;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MisionRepository extends JpaRepository<Mision, Long> {

    List<Mision> findByEstado(EstadoMision estado);

    List<Mision> findByPilotoId(Long pilotoId);

    @Query("SELECT m FROM Mision m LEFT JOIN FETCH m.piloto LEFT JOIN FETCH m.dron LEFT JOIN FETCH m.dock ORDER BY m.fechaCreacion DESC")
    List<Mision> findAllWithDetails();

    @Query("SELECT m FROM Mision m LEFT JOIN FETCH m.piloto LEFT JOIN FETCH m.dron LEFT JOIN FETCH m.dock WHERE m.estado = :estado ORDER BY m.fechaCreacion DESC")
    List<Mision> findByEstadoWithDetails(EstadoMision estado);

    @Query("SELECT m FROM Mision m LEFT JOIN FETCH m.piloto LEFT JOIN FETCH m.dron LEFT JOIN FETCH m.dock WHERE m.piloto.id = :pilotoId ORDER BY m.fechaInicio DESC NULLS LAST")
    List<Mision> findByPilotoIdWithDetails(@Param("pilotoId") Long pilotoId);

    @Modifying
    @Query("DELETE FROM Mision m WHERE m.programacion.id = :programacionId")
    int deleteByProgramacionId(@Param("programacionId") Long programacionId);
}
