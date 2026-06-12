package com.autollantas.gestion.inventory.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "PRODUCTOS")
public class Product {

    public Product(Integer quantity, ProductCategory category, String code, String description,
                   Integer id, Double taxAmount, Double basePrice, Double priceWithTax, String itemType,
                   Double purchaseCost, Double minSalePrice, Double suggestedPrice) {
        this.quantity = quantity;
        this.category = category;
        this.code = code;
        this.description = description;
        this.id = id;
        this.taxAmount = taxAmount;
        this.basePrice = basePrice;
        this.priceWithTax = priceWithTax;
        this.itemType = itemType;
        this.purchaseCost = purchaseCost;
        this.minSalePrice = minSalePrice;
        this.suggestedPrice = suggestedPrice;
    }

    public Product() {
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_producto")
    private Integer id;

    @Column(name = "codigo_producto")
    private String code;

    private String description;

    @Column(name = "precio_bruto_producto")
    private Double basePrice;

    @Column(name = "iva_producto")
    private Double taxAmount;

    @Column(name = "precio_iva_producto")
    private Double priceWithTax;

    private Integer quantity;

    @Column(name = "tipo_item")
    private String itemType;

    @ManyToOne
    @JoinColumn(name = "id_categoria_producto")
    private ProductCategory category;

    @Column(name = "costo_compra")
    private Double purchaseCost;

    @Column(name = "precio_minimo_venta")
    private Double minSalePrice;

    @Column(name = "precio_sugerido")
    private Double suggestedPrice;
}
