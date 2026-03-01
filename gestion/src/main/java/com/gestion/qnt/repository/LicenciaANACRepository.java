package com.gestion.qnt.repository;

import com.gestion.qnt.model.LicenciaANAC;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LicenciaANACRepository extends JpaRepository<LicenciaANAC, Long> {

    List<LicenciaANAC> findByPilotoId(Long pilotoId);
}
