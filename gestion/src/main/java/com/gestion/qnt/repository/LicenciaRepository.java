package com.gestion.qnt.repository;

import com.gestion.qnt.model.Licencia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LicenciaRepository extends JpaRepository<Licencia, Long> {

    List<Licencia> findByPilotoId(Long pilotoId);
}
