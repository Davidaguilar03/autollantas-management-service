package com.autollantas.gestion.treasury.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "CUENTAS")
public class Account {

    public Account(Double initialBalance, Integer id, String name, Double currentBalance) {
        this.initialBalance = initialBalance;
        this.id = id;
        this.name = name;
        this.currentBalance = currentBalance;
    }

    public Account() {
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_cuenta")
    private Integer id;

    @Column(name = "nombre_cuenta")
    private String name;

    @Column(name = "balance_inicial")
    private Double initialBalance;

    @Column(name = "saldo_actual")
    private Double currentBalance;
}
