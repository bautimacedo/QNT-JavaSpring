package com.gestion.qnt.repository;

import com.gestion.qnt.model.MantenimientoDock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface MantenimientoDockRepository extends JpaRepository<MantenimientoDock, Long> {

    @Query("""
        SELECT m FROM MantenimientoDock m
        LEFT JOIN FETCH m.dock d
        LEFT JOIN FETCH d.site
        LEFT JOIN FETCH m.usuario
        ORDER BY m.fechaMantenimiento DESC
        """)
    List<MantenimientoDock> findAllWithDetails();

    @Query("""
        SELECT m FROM MantenimientoDock m
        LEFT JOIN FETCH m.dock d
        LEFT JOIN FETCH d.site
        LEFT JOIN FETCH m.usuario
        WHERE m.dock.id = :dockId
        ORDER BY m.fechaMantenimiento DESC
        """)
    List<MantenimientoDock> findByDockIdWithDetails(Long dockId);
}
