package com.gestion.qnt.repository;

import com.gestion.qnt.model.Alerta;
import com.gestion.qnt.model.enums.TipoAlerta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AlertaRepository extends JpaRepository<Alerta, Long> {

    List<Alerta> findByResueltaFalseOrderByNivelAscFechaCreacionDesc();

    Optional<Alerta> findByClaveDedup(String claveDedup);

    boolean existsByClaveDedup(String claveDedup);

    List<Alerta> findByEntidadTipoAndEntidadIdAndResueltaFalse(String entidadTipo, Long entidadId);

    List<Alerta> findByTipoAndResueltaFalse(TipoAlerta tipo);
}
