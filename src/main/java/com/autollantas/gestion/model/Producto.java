package com.autollantas.gestion.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "PRODUCTOS")
public class Producto {
    public Producto(Integer cantidad, CategoriaProducto categoria, String codigoProducto, String descripcion, Integer idProducto, Double ivaProducto, Double precioBrutoProducto, Double precioIvaProducto, String tipoItem) {
        this.cantidad = cantidad;
        this.categoria = categoria;
        this.codigoProducto = codigoProducto;
        this.descripcion = descripcion;
        this.idProducto = idProducto;
        this.ivaProducto = ivaProducto;
        this.precioBrutoProducto = precioBrutoProducto;
        this.precioIvaProducto = precioIvaProducto;
        this.tipoItem = tipoItem;
    }
    public Producto() {
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_producto")
    private Integer idProducto;

    @Column(name = "codigo_producto")
    private String codigoProducto;

    private String descripcion;

    @Column(name = "precio_bruto_producto")
    private Double precioBrutoProducto;

    @Column(name = "iva_producto")
    private Double ivaProducto;

    @Column(name = "precio_iva_producto")
    private Double precioIvaProducto;

    private Integer cantidad;

    @Column(name = "tipo_item")
    private String tipoItem;

    @ManyToOne
    @JoinColumn(name = "id_categoria_producto")
    private CategoriaProducto categoria;
}
