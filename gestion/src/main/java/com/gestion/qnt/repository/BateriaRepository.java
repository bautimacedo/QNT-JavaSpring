package com.gestion.qnt.repository;

import com.gestion.qnt.model.Bateria;
import com.gestion.qnt.model.enums.Estado;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BateriaRepository extends JpaRepository<Bateria, Long> {

    List<Bateria> findByEstado(Estado estado);
}
