package com.autollantas.gestion.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Entity
@Data
@Table(name = "INGRESOS_OCASIONALES")
public class IngresoOcasional {
    public IngresoOcasional(String conceptoIngreso, Cuenta cuenta, LocalDate fechaIngreso, Integer idIngreso, Double montoIngreso, String observaciones) {
        this.conceptoIngreso = conceptoIngreso;
        this.cuenta = cuenta;
        this.fechaIngreso = fechaIngreso;
        this.idIngreso = idIngreso;
        this.montoIngreso = montoIngreso;
        this.observaciones = observaciones;
    }
    public IngresoOcasional() {
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_ingreso")
    private Integer idIngreso;

    @ManyToOne
    @JoinColumn(name = "id_cuenta")
    private Cuenta cuenta;

    @Column(name = "concepto_ingreso")
    private String conceptoIngreso;

    @Column(name = "monto_ingreso")
    private Double montoIngreso;

    @Column(name = "fecha_ingreso")
    private LocalDate fechaIngreso;

    private String observaciones;
}