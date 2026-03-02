package com.gestion.qnt.repository;

import com.gestion.qnt.model.Compra;
import com.gestion.qnt.model.enums.TipoCompra;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CompraRepository extends JpaRepository<Compra, Long> {

    /** Evita LazyInitializationException al serializar (proveedor y site). */
    @Query("SELECT c FROM Compra c JOIN FETCH c.proveedor p LEFT JOIN FETCH c.site s")
    List<Compra> findAllWithProveedorAndSite();

    /** Evita LazyInitializationException al serializar (proveedor y site). */
    @Query("SELECT c FROM Compra c JOIN FETCH c.proveedor p LEFT JOIN FETCH c.site s WHERE c.id = :id")
    Optional<Compra> findByIdWithProveedorAndSite(@Param("id") Long id);

    /** Filtra por tipoCompra y proveedorId (cualquiera puede ser null). */
    @Query("""
            SELECT c
            FROM Compra c
            JOIN FETCH c.proveedor p
            LEFT JOIN FETCH c.site s
            WHERE (:tipoCompra IS NULL OR c.tipoCompra = :tipoCompra)
              AND (:proveedorId IS NULL OR p.id = :proveedorId)
            """)
    List<Compra> findFilteredWithProveedorAndSite(@Param("tipoCompra") TipoCompra tipoCompra,
                                                  @Param("proveedorId") Long proveedorId);

    long countByProveedorId(Long proveedorId);
}
