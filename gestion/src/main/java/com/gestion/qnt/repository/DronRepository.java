package com.gestion.qnt.repository;

import com.gestion.qnt.model.Dron;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DronRepository extends JpaRepository<Dron, Long> {
}
