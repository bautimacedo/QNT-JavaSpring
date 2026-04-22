package com.gestion.qnt.repository;

import com.gestion.qnt.model.Pozo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PozoRepository extends JpaRepository<Pozo, Long> {

    java.util.Optional<Pozo> findByNombreIgnoreCase(String nombre);
}
