package com.gestion.qnt.repository;

import com.gestion.qnt.model.TempestRegistro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

public interface TempestRegistroRepository extends JpaRepository<TempestRegistro, Long> {

    List<TempestRegistro> findByTimestampAfterOrderByTimestampAsc(Instant from);

    @Modifying
    @Transactional
    @Query("DELETE FROM TempestRegistro t WHERE t.timestamp < :before")
    void deleteOlderThan(Instant before);
}
