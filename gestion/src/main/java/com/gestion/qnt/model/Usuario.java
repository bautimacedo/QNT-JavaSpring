package com.gestion.qnt.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import com.gestion.qnt.model.enums.EstadoUsuario;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "usuarios", uniqueConstraints = @UniqueConstraint(columnNames = "email"))
@Getter
@Setter
public class Usuario {

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

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "usuario_roles",
            joinColumns = @JoinColumn(name = "usuario_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private List<Role> roles = new ArrayList<>();

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
}
