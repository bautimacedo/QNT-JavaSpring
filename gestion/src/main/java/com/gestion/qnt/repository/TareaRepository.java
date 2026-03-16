package com.gestion.qnt.repository;

import com.gestion.qnt.model.Tarea;
import com.gestion.qnt.model.enums.EstadoTarea;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface TareaRepository extends JpaRepository<Tarea, Long> {

    @Query("SELECT t FROM Tarea t LEFT JOIN FETCH t.asignadoA LEFT JOIN FETCH t.creadoPor ORDER BY CASE t.prioridad WHEN 'CRITICA' THEN 0 WHEN 'ALTA' THEN 1 WHEN 'MEDIA' THEN 2 WHEN 'BAJA' THEN 3 ELSE 4 END ASC, t.fechaVencimiento ASC NULLS LAST, t.fechaCreacion DESC")
    List<Tarea> findAllWithDetails();

    @Query("SELECT t FROM Tarea t LEFT JOIN FETCH t.asignadoA LEFT JOIN FETCH t.creadoPor WHERE t.estado = :estado ORDER BY CASE t.prioridad WHEN 'CRITICA' THEN 0 WHEN 'ALTA' THEN 1 WHEN 'MEDIA' THEN 2 WHEN 'BAJA' THEN 3 ELSE 4 END ASC, t.fechaVencimiento ASC NULLS LAST")
    List<Tarea> findByEstadoWithDetails(EstadoTarea estado);

    @Query("SELECT t FROM Tarea t LEFT JOIN FETCH t.asignadoA LEFT JOIN FETCH t.creadoPor WHERE t.asignadoA.id = :userId ORDER BY CASE t.prioridad WHEN 'CRITICA' THEN 0 WHEN 'ALTA' THEN 1 WHEN 'MEDIA' THEN 2 WHEN 'BAJA' THEN 3 ELSE 4 END ASC")
    List<Tarea> findByAsignadoAId(Long userId);

    boolean existsByTituloAndEstadoNot(String titulo, EstadoTarea estado);
}
