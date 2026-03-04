package com.autollantas.gestion.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "DETALLE_VENTAS")
public class DetalleVenta {
    public DetalleVenta(Integer cantidadVenta, Double descuentoVenta, Integer idDetalleVenta, Double impuestoVenta, Double precioVenta, Producto producto, Double subtotalVenta, Venta venta) {
        this.cantidadVenta = cantidadVenta;
        this.descuentoVenta = descuentoVenta;
        this.idDetalleVenta = idDetalleVenta;
        this.impuestoVenta = impuestoVenta;
        this.precioVenta = precioVenta;
        this.producto = producto;
        this.subtotalVenta = subtotalVenta;
        this.venta = venta;
    }

    public DetalleVenta() {
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_detalle_venta")
    private Integer idDetalleVenta;

    @ManyToOne
    @JoinColumn(name = "id_producto")
    private Producto producto;

    @ManyToOne
    @JoinColumn(name = "id_venta")
    private Venta venta;

    @Column(name = "precio_venta")
    private Double precioVenta;

    @Column(name = "descuento_venta")
    private Double descuentoVenta;

    @Column(name = "impuesto_venta")
    private Double impuestoVenta;

    @Column(name = "cantidad_venta")
    private Integer cantidadVenta;

    @Column(name = "subtotal_venta")
    private Double subtotalVenta;
}
