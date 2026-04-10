package com.gestion.qnt.repository;

import com.gestion.qnt.model.Seguro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SeguroRepository extends JpaRepository<Seguro, Long> {

    @Query("SELECT COUNT(s) FROM Seguro s WHERE s.compra.id = :compraId")
    long countByCompraId(@Param("compraId") Long compraId);
}
