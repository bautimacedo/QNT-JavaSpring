package com.gestion.qnt.repository;

import com.gestion.qnt.model.ArchivoCompra;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ArchivoCompraRepository extends JpaRepository<ArchivoCompra, Long> {

    @Query("SELECT a FROM ArchivoCompra a WHERE a.compra.id = :compraId ORDER BY a.fechaSubida DESC")
    List<ArchivoCompra> findByCompraId(@Param("compraId") Long compraId);

    @Query("SELECT a FROM ArchivoCompra a WHERE a.id = :id AND a.compra.id = :compraId")
    Optional<ArchivoCompra> findByIdAndCompraId(@Param("id") Long id, @Param("compraId") Long compraId);
}
