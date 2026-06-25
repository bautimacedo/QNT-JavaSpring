package com.gestion.qnt.repository;

import com.gestion.qnt.model.RegistroHora;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface RegistroHoraRepository extends JpaRepository<RegistroHora, Long> {
    List<RegistroHora> findAllByOrderByFechaDescCreatedAtDesc();
    List<RegistroHora> findByFechaBetweenOrderByFechaDescCreatedAtDesc(LocalDate desde, LocalDate hasta);
}
