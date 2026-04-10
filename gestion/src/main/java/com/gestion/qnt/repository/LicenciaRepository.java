package com.gestion.qnt.repository;

import com.gestion.qnt.model.Licencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LicenciaRepository extends JpaRepository<Licencia, Long> {

    @Query("SELECT COUNT(l) FROM Licencia l WHERE l.compra.id = :compraId")
    long countByCompraId(@Param("compraId") Long compraId);
}
