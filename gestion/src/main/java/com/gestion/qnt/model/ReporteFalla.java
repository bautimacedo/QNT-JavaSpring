package com.gestion.qnt.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "reportes_fallas")
public class ReporteFalla {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String titulo;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(name = "archivo_nombre", nullable = false)
    private String archivoNombre;

    @Column(nullable = false, columnDefinition = "bytea")
    @JsonIgnore
    private byte[] contenido;

    @Column(name = "fecha_subida")
    private Instant fechaSubida = Instant.now();

    public Long getId() { return id; }
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }
    public String getArchivoNombre() { return archivoNombre; }
    public void setArchivoNombre(String archivoNombre) { this.archivoNombre = archivoNombre; }
    public byte[] getContenido() { return contenido; }
    public void setContenido(byte[] contenido) { this.contenido = contenido; }
    public Instant getFechaSubida() { return fechaSubida; }
    public void setFechaSubida(Instant fechaSubida) { this.fechaSubida = fechaSubida; }
}
