package com.gestion.qnt.repository;

import com.gestion.qnt.model.Dock;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DockRepository extends JpaRepository<Dock, Long> {
}
