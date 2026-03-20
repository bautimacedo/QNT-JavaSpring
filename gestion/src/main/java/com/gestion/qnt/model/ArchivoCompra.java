package com.gestion.qnt.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.gestion.qnt.model.enums.TipoDocumentoCompra;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "archivos_compra")
@Getter
@Setter
public class ArchivoCompra {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "compra_id", nullable = false)
    @JsonIgnore
    private Compra compra;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_documento", nullable = false)
    private TipoDocumentoCompra tipoDocumento;

    @Column(name = "nombre_archivo", nullable = false)
    private String nombreArchivo;

    @Column(name = "content_type", length = 100)
    private String contentType;

    @Column(name = "fecha_subida", nullable = false)
    private LocalDateTime fechaSubida;

    @Column(name = "contenido", nullable = false, columnDefinition = "bytea")
    @Basic(fetch = FetchType.LAZY)
    @JsonIgnore
    private byte[] contenido;

    /** Expone el id de la compra en JSON sin serializar toda la entidad. */
    public Long getCompraId() {
        return compra != null ? compra.getId() : null;
    }
}
