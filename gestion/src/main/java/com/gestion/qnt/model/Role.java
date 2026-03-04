package com.gestion.qnt.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

import java.io.Serializable;

@Entity
@Table(name = "roles", uniqueConstraints = @UniqueConstraint(columnNames = "codigo"))
@Getter
@Setter
public class Role implements Serializable{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String codigo; //ROLE_ADMIN

    @Column
    private String nombre; // Descripcion

}
