package com.gestion.qnt.repository;

import com.gestion.qnt.model.Dron;
import com.gestion.qnt.model.enums.Estado;
import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface DronRepository extends JpaRepository<Dron, Long> {

    Optional<Dron> findByNumeroSerie(String numeroSerie);

    List<Dron> findByEstado(Estado estado);

    List<Dron> findByBateriaTempCGreaterThan(BigDecimal temp);
}
