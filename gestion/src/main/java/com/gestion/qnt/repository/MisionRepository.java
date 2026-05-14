package com.gestion.qnt.repository;

import com.gestion.qnt.model.Mision;
import com.gestion.qnt.model.enums.EstadoMision;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MisionRepository extends JpaRepository<Mision, Long> {

    List<Mision> findByEstado(EstadoMision estado);

    @Query("SELECT m.estado, COUNT(m) FROM Mision m GROUP BY m.estado")
    List<Object[]> countGroupByEstado();

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

    @Query("SELECT m FROM Mision m LEFT JOIN FETCH m.piloto LEFT JOIN FETCH m.dron LEFT JOIN FETCH m.dock WHERE m.programacion IS NOT NULL OR m.fechaInicio IS NOT NULL ORDER BY m.fechaInicio DESC NULLS LAST, m.fechaCreacion DESC")
    List<Mision> findHistorial();

    Optional<Mision> findByFlightHubWaylineUuid(String flightHubWaylineUuid);

    @Query("SELECT m FROM Mision m WHERE m.flightHubWaylineUuid IS NOT NULL AND m.estado = :estado")
    List<Mision> findCAMMisionesByEstado(@Param("estado") EstadoMision estado);

    @Query("SELECT m FROM Mision m LEFT JOIN FETCH m.piloto LEFT JOIN FETCH m.dron d LEFT JOIN FETCH d.baterias LEFT JOIN FETCH d.helices WHERE d.nombre = :dronNombre AND m.estado = :estado ORDER BY m.fechaInicio DESC NULLS LAST")
    java.util.List<Mision> findByDron_NombreAndEstado(@Param("dronNombre") String dronNombre, @Param("estado") EstadoMision estado);

    @Query("SELECT m FROM Mision m LEFT JOIN FETCH m.piloto LEFT JOIN FETCH m.dron LEFT JOIN FETCH m.dock WHERE m.estado = :estado AND m.fechaProgramada >= :start AND m.fechaProgramada < :end ORDER BY m.fechaProgramada ASC")
    List<Mision> findPlanificadasDelDia(@Param("estado") EstadoMision estado, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
