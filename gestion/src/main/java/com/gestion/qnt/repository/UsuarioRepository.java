package com.gestion.qnt.repository;

import com.gestion.qnt.model.Usuario;
import com.gestion.qnt.model.enums.EstadoUsuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmail(String email);

    Optional<Usuario> findByEmailAndIdNot(String email, Long id);

    List<Usuario> findByEstado(EstadoUsuario estado);

    /** Lista todos con roles cargados (evita LazyInitializationException al serializar a JSON). */
    @Query("SELECT DISTINCT u FROM Usuario u LEFT JOIN FETCH u.roles")
    List<Usuario> findAllWithRoles();

    /** Lista por estado con roles cargados. */
    @Query("SELECT DISTINCT u FROM Usuario u LEFT JOIN FETCH u.roles WHERE u.estado = :estado")
    List<Usuario> findByEstadoWithRoles(@Param("estado") EstadoUsuario estado);

    /** Busca por id con roles cargados. */
    @Query("SELECT u FROM Usuario u LEFT JOIN FETCH u.roles WHERE u.id = :id")
    Optional<Usuario> findByIdWithRoles(@Param("id") Long id);

    /** Busca por email con roles cargados. */
    @Query("SELECT u FROM Usuario u LEFT JOIN FETCH u.roles WHERE u.email = :email")
    Optional<Usuario> findByEmailWithRoles(@Param("email") String email);

    /** Usuarios que tienen el rol con el código dado, con roles cargados. */
    @Query("SELECT DISTINCT u FROM Usuario u LEFT JOIN FETCH u.roles r WHERE r.codigo = :codigo")
    List<Usuario> findByRoleCodigoWithRoles(@Param("codigo") String codigo);
}
