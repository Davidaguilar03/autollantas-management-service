package com.autollantas.gestion.sales.model;

import com.autollantas.gestion.inventory.model.Product;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "DETALLE_VENTAS")
public class SaleDetail {

    public SaleDetail(Integer quantity, Double discount, Integer id, Double tax, Double salePrice, Product product, Double subtotal, Sale sale) {
        this.quantity = quantity;
        this.discount = discount;
        this.id = id;
        this.tax = tax;
        this.salePrice = salePrice;
        this.product = product;
        this.subtotal = subtotal;
        this.sale = sale;
    }

    public SaleDetail() {
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_detalle_venta")
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "id_producto")
    private Product product;

    @ManyToOne
    @JoinColumn(name = "id_venta")
    private Sale sale;

    @Column(name = "precio_venta")
    private Double salePrice;

    @Column(name = "descuento_venta")
    private Double discount;

    @Column(name = "impuesto_venta")
    private Double tax;

    @Column(name = "cantidad_venta")
    private Integer quantity;

    @Column(name = "subtotal_venta")
    private Double subtotal;

    @Column(name = "utilidad_linea")
    private Double profitAmount;

    @Column(name = "diferencia_iva_linea")
    private Double ivaDifference;
}
