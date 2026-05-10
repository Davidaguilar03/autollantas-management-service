package com.autollantas.gestion.inventory.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "CATEGORIA_PRODUCTOS")
public class ProductCategory {

    public ProductCategory(Integer id, String name, Integer yellowStockMin, Integer redStockMin) {
        this.id = id;
        this.name = name;
        this.yellowStockMin = yellowStockMin;
        this.redStockMin = redStockMin;
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

    @Override
    public String toString() {
        return this.name;
    }
}
