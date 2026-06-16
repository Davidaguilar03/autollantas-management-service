package com.autollantas.gestion.inventory.model;

import jakarta.persistence.*;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@Table(name = "CATEGORIA_PRODUCTOS")
public class ProductCategory {

    public ProductCategory(Integer id, String name, Integer yellowStockMin, Integer redStockMin, Double targetMargin) {
        this.id = id;
        this.name = name;
        this.yellowStockMin = yellowStockMin;
        this.redStockMin = redStockMin;
        this.targetMargin = targetMargin;
    }

    public ProductCategory() {
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_categoria_producto")
    private Integer id;

    @Column(name = "nombre_categoria_producto")
    private String name;

    @Column(name = "stock_min_amarillo")
    private Integer yellowStockMin;

    @Column(name = "stock_min_rojo")
    private Integer redStockMin;

    @Column(name = "margen_utilidad")
    private Double targetMargin;

    @Column(name = "color_categoria")
    private String color;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "CATEGORIA_IMPUESTOS",
        joinColumns = @JoinColumn(name = "id_categoria_producto"),
        inverseJoinColumns = @JoinColumn(name = "id_impuesto")
    )
    private List<TaxType> taxTypes = new ArrayList<>();

    @Override
    public String toString() {
        return this.name;
    }
}
