package com.gestion.qnt.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "roles", uniqueConstraints = @UniqueConstraint(columnNames = "codigo"))
@Getter
@Setter
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String codigo;

    @Column
    private String nombre;

    @ManyToMany(mappedBy = "roles", fetch = FetchType.LAZY)
    private List<Usuario> usuarios = new ArrayList<>();
}
