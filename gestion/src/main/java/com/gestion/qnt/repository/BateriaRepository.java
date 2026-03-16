package com.gestion.qnt.repository;

import com.gestion.qnt.model.Bateria;
import com.gestion.qnt.model.enums.Estado;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BateriaRepository extends JpaRepository<Bateria, Long> {

    List<Bateria> findByEstado(Estado estado);

    Optional<Bateria> findByNumeroSerie(String numeroSerie);

    List<Bateria> findByCiclosCargaGreaterThan(int ciclos);
}
