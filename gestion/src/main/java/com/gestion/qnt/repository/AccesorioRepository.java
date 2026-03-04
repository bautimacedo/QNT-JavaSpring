package com.gestion.qnt.repository;

import com.gestion.qnt.model.Accesorio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AccesorioRepository extends JpaRepository<Accesorio, Long> {
}