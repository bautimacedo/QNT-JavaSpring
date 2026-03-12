package com.gestion.qnt.repository;

import com.gestion.qnt.model.MantenimientoDron;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface MantenimientoDronRepository extends JpaRepository<MantenimientoDron, Long> {

    @Query("""
        SELECT m FROM MantenimientoDron m
        LEFT JOIN FETCH m.dron
        LEFT JOIN FETCH m.usuario
        LEFT JOIN FETCH m.bateriaVieja
        LEFT JOIN FETCH m.bateriaNueva
        ORDER BY m.fechaMantenimiento DESC
        """)
    List<MantenimientoDron> findAllWithDetails();

    @Query("""
        SELECT m FROM MantenimientoDron m
        LEFT JOIN FETCH m.dron
        LEFT JOIN FETCH m.usuario
        LEFT JOIN FETCH m.bateriaVieja
        LEFT JOIN FETCH m.bateriaNueva
        WHERE m.dron.id = :dronId
        ORDER BY m.fechaMantenimiento DESC
        """)
    List<MantenimientoDron> findByDronIdWithDetails(Long dronId);
}
