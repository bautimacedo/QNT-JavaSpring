package com.gestion.qnt.repository;

import com.gestion.qnt.model.Empresa;
import org.springframework.data.jpa.repository.JpaRepository;


public interface EmpresaRepository extends JpaRepository<Empresa, Long> {

}
