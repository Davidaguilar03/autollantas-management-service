package com.autollantas.gestion.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Entity
@Data
@Table(name = "VENTAS")
public class Venta {

    public Venta(Cliente cliente, Cuenta cuenta, String estadoVenta, LocalDate fechaVencimientoVenta, LocalDate fechaVenta, String formaPagoVenta, Integer idVenta, String medioPagoVenta, String notasVenta, String numeroFacturaVenta, Double totalVenta) {
        this.cliente = cliente;
        this.cuenta = cuenta;
        this.estadoVenta = estadoVenta;
        this.fechaVencimientoVenta = fechaVencimientoVenta;
        this.fechaVenta = fechaVenta;
        this.formaPagoVenta = formaPagoVenta;
        this.idVenta = idVenta;
        this.medioPagoVenta = medioPagoVenta;
        this.notasVenta = notasVenta;
        this.numeroFacturaVenta = numeroFacturaVenta;
        this.totalVenta = totalVenta;

        this.saldoPendiente = totalVenta;
    }

    public Venta() {
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_venta")
    private Integer idVenta;

    @ManyToOne
    @JoinColumn(name = "id_cliente")
    private Cliente cliente;

    @ManyToOne
    @JoinColumn(name = "id_cuenta")
    private Cuenta cuenta;

    @Column(name = "numero_factura_venta")
    private String numeroFacturaVenta;

    @Column(name = "fecha_venta")
    private LocalDate fechaVenta;

    @Column(name = "fecha_vencimiento_venta")
    private LocalDate fechaVencimientoVenta;

    @Column(name = "forma_pago_venta")
    private String formaPagoVenta;

    @Column(name = "medio_pago_venta")
    private String medioPagoVenta;

    @Column(name = "notas_venta")
    private String notasVenta;

    @Column(name = "total_venta")
    private Double totalVenta;

    @Column(name = "estado_venta")
    private String estadoVenta;

    @Column(name = "saldo_pendiente")
    private Double saldoPendiente;

}