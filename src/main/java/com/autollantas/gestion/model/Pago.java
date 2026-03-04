package com.autollantas.gestion.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Entity
@Data
@Table(name = "PAGOS")
public class Pago {
    public Pago(Compra compra, Cuenta cuenta, LocalDate fechaPago, Integer idPagos, String metodoPagoPago, Double valorPago) {
        this.compra = compra;
        this.cuenta = cuenta;
        this.fechaPago = fechaPago;
        this.idPagos = idPagos;
        this.metodoPagoPago = metodoPagoPago;
        this.valorPago = valorPago;
    }
    public Pago() {
    }
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_pagos")
    private Integer idPagos;

    @ManyToOne
    @JoinColumn(name = "id_compra")
    private Compra compra;

    @ManyToOne
    @JoinColumn(name = "id_cuenta")
    private Cuenta cuenta;

    @Column(name = "fecha_pago")
    private LocalDate fechaPago;

    @Column(name = "metodo_pago_pago")
    private String metodoPagoPago;

    @Column(name = "valor_pago")
    private Double valorPago;
}
