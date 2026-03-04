package com.autollantas.gestion.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Entity
@Data
@Table(name = "TRANSFERENCIAS")
public class Transferencia {
    public Transferencia(Cuenta cuentaDestino, Cuenta cuentaOrigen, LocalDate fechaTransferencia, Integer idTransferencia, Double montoTransferencia) {
        this.cuentaDestino = cuentaDestino;
        this.cuentaOrigen = cuentaOrigen;
        this.fechaTransferencia = fechaTransferencia;
        this.idTransferencia = idTransferencia;
        this.montoTransferencia = montoTransferencia;
    }
    public Transferencia() {
    }
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_transferencia")
    private Integer idTransferencia;

    @ManyToOne
    @JoinColumn(name = "id_cuenta_origen")
    private Cuenta cuentaOrigen;

    @ManyToOne
    @JoinColumn(name = "id_cuenta_destino")
    private Cuenta cuentaDestino;

    @Column(name = "fecha_transferencia")
    private LocalDate fechaTransferencia;

    @Column(name = "monto_transferencia")
    private Double montoTransferencia;
}