package com.autollantas.gestion.treasury.model;

import com.autollantas.gestion.purchases.model.Compra;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Entity
@Data
@Table(name = "PAGOS")
public class Payment {

    public Payment(Compra purchase, Account account, LocalDate date, Integer id, String paymentMethod, Double amount) {
        this.purchase = purchase;
        this.account = account;
        this.date = date;
        this.id = id;
        this.paymentMethod = paymentMethod;
        this.amount = amount;
    }

    public Payment() {
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_pagos")
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "id_compra")
    private Compra purchase;

    @ManyToOne
    @JoinColumn(name = "id_cuenta")
    private Account account;

    @Column(name = "fecha_pago")
    private LocalDate date;

    @Column(name = "metodo_pago_pago")
    private String paymentMethod;

    @Column(name = "valor_pago")
    private Double amount;
}
