package com.autollantas.gestion.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "DETALLE_COMPRAS")
public class DetalleCompra {
    public DetalleCompra(Integer cantidadCompra, Compra compra, Double descuentoCompra, Integer idDetalleCompra, Double impuestoCompra, Double precioCompra, Producto producto, Double subtotalCompra) {
        this.cantidadCompra = cantidadCompra;
        this.compra = compra;
        this.descuentoCompra = descuentoCompra;
        this.idDetalleCompra = idDetalleCompra;
        this.impuestoCompra = impuestoCompra;
        this.precioCompra = precioCompra;
        this.producto = producto;
        this.subtotalCompra = subtotalCompra;
    }

    public DetalleCompra() {
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_detalle_compra")
    private Integer idDetalleCompra;

    @ManyToOne
    @JoinColumn(name = "id_producto")
    private Producto producto;

    @ManyToOne
    @JoinColumn(name = "id_compra")
    private Compra compra;

    @Column(name = "precio_compra")
    private Double precioCompra;

    @Column(name = "descuento_compra")
    private Double descuentoCompra;

    @Column(name = "impuesto_compra")
    private Double impuestoCompra;

    @Column(name = "cantidad_compra")
    private Integer cantidadCompra;

    @Column(name = "subtotal_compra")
    private Double subtotalCompra;
}
