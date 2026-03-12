package com.gestion.qnt.repository;

import com.gestion.qnt.model.Site;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SiteRepository extends JpaRepository<Site, Long> {
    Optional<Site> findByCodigo(String codigo);
}
