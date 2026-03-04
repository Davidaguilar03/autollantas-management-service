package com.autollantas.gestion.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Entity
@Data
@Table(name = "GASTOS_OPERATIVOS")
public class GastoOperativo {
    public GastoOperativo(String conceptoGasto, Cuenta cuenta, LocalDate fechaGasto, Integer idGasto, Double montoGasto, String observaciones) {
        this.conceptoGasto = conceptoGasto;
        this.cuenta = cuenta;
        this.fechaGasto = fechaGasto;
        this.idGasto = idGasto;
        this.montoGasto = montoGasto;
        this.observaciones = observaciones;
    }

    public GastoOperativo() {
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_gasto")
    private Integer idGasto;

    @ManyToOne
    @JoinColumn(name = "id_cuenta")
    private Cuenta cuenta;

    @Column(name = "concepto_gasto")
    private String conceptoGasto;

    @Column(name = "monto_gasto")
    private Double montoGasto;

    @Column(name = "fecha_gasto")
    private LocalDate fechaGasto;

    private String observaciones;
}
