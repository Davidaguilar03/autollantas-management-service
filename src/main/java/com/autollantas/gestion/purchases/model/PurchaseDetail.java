package com.autollantas.gestion.purchases.model;

import com.autollantas.gestion.inventory.model.Product;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "DETALLE_COMPRAS")
public class PurchaseDetail {
    public PurchaseDetail(Integer quantity, Purchase purchase, Double discount, Integer id,
                          Double tax, Double unitPrice, Product product, Double subtotal) {
        this.quantity = quantity;
        this.purchase = purchase;
        this.discount = discount;
        this.id = id;
        this.tax = tax;
        this.unitPrice = unitPrice;
        this.product = product;
        this.subtotal = subtotal;
    }
    public PurchaseDetail() {}

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_detalle_compra")
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "id_producto")
    private Product product;

    @ManyToOne
    @JoinColumn(name = "id_compra")
    private Purchase purchase;

    @Column(name = "precio_compra")
    private Double unitPrice;

    @Column(name = "descuento_compra")
    private Double discount;

    @Column(name = "impuesto_compra")
    private Double tax;

    @Column(name = "cantidad_compra")
    private Integer quantity;

    @Column(name = "subtotal_compra")
    private Double subtotal;
}
