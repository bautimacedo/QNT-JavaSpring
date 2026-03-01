package com.gestion.qnt.repository;

import com.gestion.qnt.model.Usuario;
import com.gestion.qnt.model.enums.EstadoUsuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmail(String email);

    Optional<Usuario> findByEmailAndIdNot(String email, Long id);

    List<Usuario> findByEstado(EstadoUsuario estado);
}
