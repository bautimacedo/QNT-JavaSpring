package com.gestion.qnt.repository;

import com.gestion.qnt.model.ReporteFalla;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReporteFallaRepository extends JpaRepository<ReporteFalla, Long> {
    List<ReporteFalla> findAllByOrderByFechaDesc();
}
