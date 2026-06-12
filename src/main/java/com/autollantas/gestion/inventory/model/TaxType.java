package com.autollantas.gestion.inventory.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "TIPOS_IMPUESTO")
public class TaxType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "nombre_impuesto")
    private String name;

    @Column(name = "tasa_impuesto")
    private Double rate;

    @Column(name = "descripcion_impuesto")
    private String description;

    @Column(name = "aplica_transaccion")
    private Boolean appliesToTransaction;

    @Column(name = "es_iva")
    private Boolean isVat = false;

    @Override
    public String toString() {
        return name;
    }
}
