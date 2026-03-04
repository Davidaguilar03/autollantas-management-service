package com.autollantas.gestion.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "CATEGORIA_PRODUCTOS")
public class CategoriaProducto {
    public CategoriaProducto(Integer idCategoriaProducto, String nombreCategoriaProducto, Integer stockMinAmarillo, Integer stockMinRojo) {
        this.idCategoriaProducto = idCategoriaProducto;
        this.nombreCategoriaProducto = nombreCategoriaProducto;
        this.stockMinAmarillo = stockMinAmarillo;
        this.stockMinRojo = stockMinRojo;
    }

    public CategoriaProducto() {
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_categoria_producto")
    private Integer idCategoriaProducto;

    @Column(name = "nombre_categoria_producto")
    private String nombreCategoriaProducto;

    @Column(name = "stock_min_amarillo")
    private Integer stockMinAmarillo;

    @Column(name = "stock_min_rojo")
    private Integer stockMinRojo;

    @Override
    public String toString() {
        return this.nombreCategoriaProducto;
    }
}
