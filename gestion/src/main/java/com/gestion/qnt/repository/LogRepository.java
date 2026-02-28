package com.gestion.qnt.repository;

import com.gestion.qnt.model.Log;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LogRepository extends JpaRepository<Log, Long> {
}
