package com.gestion.qnt.repository;

import com.gestion.qnt.model.Aib;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface AibRepository extends JpaRepository<Aib, Long> {

    Optional<Aib> findByAibId(String aibId);

    @Query("SELECT a FROM Aib a LEFT JOIN FETCH a.pozo ORDER BY a.aibId ASC")
    List<Aib> findAllWithPozo();
}
