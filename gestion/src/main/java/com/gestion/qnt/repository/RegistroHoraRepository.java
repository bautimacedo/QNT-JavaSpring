package com.gestion.qnt.repository;

import com.gestion.qnt.model.RegistroHora;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface RegistroHoraRepository extends JpaRepository<RegistroHora, Long> {

    List<RegistroHora> findAllByOrderByFechaDescCreatedAtDesc();

    List<RegistroHora> findByFechaBetweenOrderByFechaDescCreatedAtDesc(LocalDate desde, LocalDate hasta);

    /** Resumen agregado de horas por autor, con filtro opcional de rango de fechas. */
    @Query("SELECT r.autor.id, r.autor.nombre, r.autor.apellido, SUM(r.horas), COUNT(r) " +
           "FROM RegistroHora r " +
           "WHERE (:desde IS NULL OR r.fecha >= :desde) AND (:hasta IS NULL OR r.fecha <= :hasta) " +
           "GROUP BY r.autor.id, r.autor.nombre, r.autor.apellido " +
           "ORDER BY r.autor.nombre")
    List<Object[]> resumenPorAutor(@Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);
}
