package com.gestion.qnt.repository;

import com.gestion.qnt.model.Proveedor;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProveedorRepository extends JpaRepository<Proveedor, Long> {

    /** Busca el primer proveedor cuyo nombre coincide (ignorando mayúsculas). */
    java.util.Optional<Proveedor> findFirstByNombreIgnoreCase(String nombre);
}
