package com.autollantas.gestion.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Entity
@Data
@Table(name = "MOVIMIENTOS")
public class Movimiento {
    public Movimiento(LocalDate fecha, Integer concepto, String tipo, Double monto, Cuenta cuenta) {
        this.fechaMovimiento = fecha;
        this.idOrigenMovimiento = concepto;
        this.tipoMovimiento = tipo;
        this.montoMovimiento = monto;
        this.cuenta = cuenta;
    }
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_movimiento")
    private Integer idMovimiento;

    @ManyToOne
    @JoinColumn(name = "id_cuenta")
    private Cuenta cuenta;

    @Column(name = "fecha_movimiento")
    private LocalDate fechaMovimiento;

    @Column(name = "tipo_movimiento")
    private String tipoMovimiento;

    @Column(name = "monto_movimiento")
    private Double montoMovimiento;

    @Column(name = "id_origen_movimiento")
    private Integer idOrigenMovimiento;

    @Column(name = "tabla_origen_movimiento")
    private String tablaOrigenMovimiento;

    public Movimiento() {

    }
}
