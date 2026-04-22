package com.gestion.qnt.repository;

import com.gestion.qnt.model.InspeccionAib;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface InspeccionAibRepository extends JpaRepository<InspeccionAib, Long> {

    @Query("SELECT i FROM InspeccionAib i LEFT JOIN FETCH i.aib ORDER BY i.timestamp DESC")
    List<InspeccionAib> findAllWithAib();

    @Query("SELECT i FROM InspeccionAib i LEFT JOIN FETCH i.aib WHERE i.aib.aibId = :aibId ORDER BY i.timestamp DESC")
    List<InspeccionAib> findByAibIdStringOrderByTimestampDesc(@Param("aibId") String aibId);

    @Query("SELECT i FROM InspeccionAib i LEFT JOIN FETCH i.aib WHERE i.id = :id")
    Optional<InspeccionAib> findByIdWithAib(@Param("id") Long id);

    @Query("SELECT i FROM InspeccionAib i WHERE i.aib.aibId = :aibId ORDER BY i.timestamp DESC LIMIT 1")
    Optional<InspeccionAib> findLatestByAibId(@Param("aibId") String aibId);

    @Query("SELECT i FROM InspeccionAib i LEFT JOIN FETCH i.aib a WHERE a.pozo.id = :pozoId ORDER BY i.timestamp DESC")
    List<InspeccionAib> findByPozoId(@Param("pozoId") Long pozoId);
}
