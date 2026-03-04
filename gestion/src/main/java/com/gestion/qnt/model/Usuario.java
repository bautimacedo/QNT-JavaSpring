package com.gestion.qnt.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import com.gestion.qnt.model.enums.EstadoUsuario;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.authority.SimpleGrantedAuthority;


@Entity
@Table(name = "usuarios", uniqueConstraints = @UniqueConstraint(columnNames = "email"))
@Getter
@Setter
public class Usuario implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    @Column
    private String apellido;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    @JsonIgnore
    private String password;
    
    @Column(length = 100, unique = true)
	private String username;
    

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "usuario_roles",
            joinColumns = @JoinColumn(name = "usuario_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    
    
    private Set<Role> roles; 

    @Column
    private String dni;

    @Column(name = "cma_vencimiento")
    private LocalDate cmaVencimiento;

    @Column(name = "cma_imagenes")
    private String cmaImagenes;

    /** Imagen del Certificado Médico Aeronáutico. No se serializa en JSON. */
    @Lob
    @Column(name = "imagen_cma", nullable = true)
    @Basic(optional = true)
    @JsonIgnore
    private byte[] imagenCma;

    @Column(name = "horas_vuelo")
    private Integer horasVuelo;

    @Column(name = "cantidad_vuelos")
    private Integer cantidadVuelos;

    /** Clave/contraseña para misiones; relevante para pilotos. Máx. 30 caracteres. */
    @Column(name = "password_mission", length = 30, nullable = true)
    private String passwordMission;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 30)
    private EstadoUsuario estado = EstadoUsuario.ACTIVO;

    @Column(nullable = false)
    private Boolean activo = true;

    @Lob
    @Column(name = "imagen_perfil", nullable = true)
    @Basic(optional = true)
    @JsonIgnore
    private byte[] imagenPerfil;
    
    
 // le paso un rol y me dice si el usuario tiene el rol o no.
 	@Transient
 	public boolean isInRole(Role role) {
 		return isInRole(role.getCodigo());
 	}

 	@Transient
 	public boolean isInRole(String role) {
 		for (Role r : getRoles())
 			if (r.getCodigo().equals(role))
 				return true;
 		return false;
 	}

 	@Transient // transient dice que jpa no guarde esto en la bd. no lo trates ni guardar ni obtener de la bd.
 	@Override
 	public Collection<? extends GrantedAuthority> getAuthorities() {
 		List<GrantedAuthority> authorities = getRoles().stream().map(role -> new SimpleGrantedAuthority(role.getCodigo())) 
 				.collect(Collectors.toList()); // basicamente lo que hago aca es transformar mis roles en granted authorities para spring
 		return authorities;
 	}

 	@Transient //ibtenemos una lista de strings de autoridades.
 	public List<String> getAuthoritiesStr() {
 		List<String> authorities = getRoles().stream().map(role -> role.getCodigo()).collect(Collectors.toList());
 		return authorities;
 	}
    
    
    
    
}
