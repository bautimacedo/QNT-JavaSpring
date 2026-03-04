package com.gestion.qnt.model;

import com.gestion.qnt.model.enums.Estado;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;

@Entity
@Table(name = "accesorios")
@Getter
@Setter
public class Accesorio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre; // Ej: Cable Ethernet Cat 6

    @Column
    private String marca; // Ej: Hikvision

    @Column
    private String modelo; // Ej: Prd551

    @Column(name = "numero_serie")
    private String numeroSerie; // Puede ser null para tornillos

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Estado estado;

    @Column(name = "fecha_compra")
    private LocalDate fechaCompra;

    @Column
    private Double cantidad; // 3.0 para metros, 50 para tornillos

    @Column(name = "unidad_medida")
    private String unidadMedida; // mts, un, kg, etc.

    @Lob
    @Column
    private String descripcion; // Aca va el detalle: "Tripolar 3 X 2,5 Mm"

    @Column (nullable = true)
    private String ubicacion; // Ej: Deposito A, Caja de herramientas 1

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id")
    private Site site;
}