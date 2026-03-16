package com.gestion.qnt.repository;

import com.gestion.qnt.model.Dock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DockRepository extends JpaRepository<Dock, Long> {

    Optional<Dock> findByNumeroSerie(String numeroSerie);
}
