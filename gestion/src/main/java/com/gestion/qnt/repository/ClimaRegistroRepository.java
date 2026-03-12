package com.gestion.qnt.repository;

import com.gestion.qnt.model.ClimaRegistro;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ClimaRegistroRepository extends JpaRepository<ClimaRegistro, Long> {

    /** Último registro por site — agrupa por site y toma el MAX id */
    @Query("""
        SELECT c FROM ClimaRegistro c
        JOIN FETCH c.site s
        WHERE c.id IN (
            SELECT MAX(c2.id) FROM ClimaRegistro c2 GROUP BY c2.site
        )
        ORDER BY s.codigo
        """)
    List<ClimaRegistro> findLatestPerSite();

    /** Último registro para un site específico por código */
    @Query("""
        SELECT c FROM ClimaRegistro c
        JOIN FETCH c.site s
        WHERE s.codigo = :codigo
        ORDER BY c.recordedAt DESC
        """)
    List<ClimaRegistro> findLatestByCodigo(@Param("codigo") String codigo, Pageable pageable);

    default Optional<ClimaRegistro> findLatestByCodigo(String codigo) {
        List<ClimaRegistro> results = findLatestByCodigo(codigo, Pageable.ofSize(1));
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    /** Historial de un site */
    @Query("""
        SELECT c FROM ClimaRegistro c
        JOIN FETCH c.site s
        WHERE s.codigo = :codigo
        ORDER BY c.recordedAt DESC
        """)
    List<ClimaRegistro> findHistorialByCodigo(@Param("codigo") String codigo, Pageable pageable);

    default List<ClimaRegistro> findHistorialByCodigo(String codigo, int limit) {
        return findHistorialByCodigo(codigo, Pageable.ofSize(limit));
    }
}
