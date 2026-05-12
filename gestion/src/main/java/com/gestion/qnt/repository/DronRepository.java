package com.gestion.qnt.repository;

import com.gestion.qnt.model.Dron;
import com.gestion.qnt.model.enums.Estado;
import com.gestion.qnt.model.enums.Yacimiento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface DronRepository extends JpaRepository<Dron, Long> {

    Optional<Dron> findByNumeroSerie(String numeroSerie);

    Optional<Dron> findByNombre(String nombre);

    List<Dron> findByEstado(Estado estado);

    @Query("SELECT d.estado, COUNT(d) FROM Dron d GROUP BY d.estado")
    List<Object[]> countGroupByEstado();

    long countByBateriaTempCGreaterThan(BigDecimal temp);

    List<Dron> findByBateriaTempCGreaterThan(BigDecimal temp);

    List<Dron> findByYacimiento(Yacimiento yacimiento);

    Optional<Dron> findBySnMqtt(String snMqtt);

    Optional<Dron> findByDock_Id(Long dockId);
}
