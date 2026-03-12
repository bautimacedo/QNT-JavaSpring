package com.gestion.qnt.repository;

import com.gestion.qnt.model.Log;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface LogRepository extends JpaRepository<Log, Long> {

    @Query("""
        SELECT l FROM Log l
        LEFT JOIN FETCH l.usuario
        ORDER BY l.timestamp DESC
        """)
    List<Log> findAllWithDetails();

    @Query("""
        SELECT l FROM Log l
        LEFT JOIN FETCH l.usuario
        WHERE l.entidadTipo = :entidadTipo
        ORDER BY l.timestamp DESC
        """)
    List<Log> findByEntidadTipoWithDetails(String entidadTipo);

    @Query("""
        SELECT l FROM Log l
        LEFT JOIN FETCH l.usuario
        WHERE l.entidadTipo = :entidadTipo AND l.entidadId = :entidadId
        ORDER BY l.timestamp DESC
        """)
    List<Log> findByEntidadWithDetails(String entidadTipo, Long entidadId);
}
