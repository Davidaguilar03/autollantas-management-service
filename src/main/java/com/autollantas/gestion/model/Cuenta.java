package com.autollantas.gestion.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "CUENTAS")
public class Cuenta {
    public Cuenta(Double balanceInicial, Integer idCuenta, String nombreCuenta, Double saldoActual) {
        this.balanceInicial = balanceInicial;
        this.idCuenta = idCuenta;
        this.nombreCuenta = nombreCuenta;
        this.saldoActual = saldoActual;
    }
    public Cuenta() {
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_cuenta")
    private Integer idCuenta;

    @Column(name = "nombre_cuenta")
    private String nombreCuenta;

    @Column(name = "balance_inicial")
    private Double balanceInicial;

    @Column(name = "saldo_actual")
    private Double saldoActual;
}
