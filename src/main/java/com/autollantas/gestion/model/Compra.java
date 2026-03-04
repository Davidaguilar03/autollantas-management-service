package com.autollantas.gestion.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Entity
@Data
@Table(name = "COMPRAS")
public class Compra {
    public Compra(Integer idCompra, Proveedor proveedor, Cuenta cuenta, String numeroFacturaCompra, LocalDate fechaCompra, String formaPagoCompra, LocalDate fechaVencimientoCompra, String medioPagoCompra, String notasCompra, Double totalCompra, String estadoCompra) {
        this.idCompra = idCompra;
        this.proveedor = proveedor;
        this.cuenta = cuenta;
        this.numeroFacturaCompra = numeroFacturaCompra;
        this.fechaCompra = fechaCompra;
        this.formaPagoCompra = formaPagoCompra;
        this.fechaVencimientoCompra = fechaVencimientoCompra;
        this.medioPagoCompra = medioPagoCompra;
        this.notasCompra = notasCompra;
        this.totalCompra = totalCompra;
        this.estadoCompra = estadoCompra;
    }

    public Compra() {
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_compra")
    private Integer idCompra;

    @ManyToOne
    @JoinColumn(name = "id_proveedor")
    private Proveedor proveedor;

    @ManyToOne
    @JoinColumn(name = "id_cuenta")
    private Cuenta cuenta;

    @Column(name = "numero_factura_compra")
    private String numeroFacturaCompra;

    @Column(name = "fecha_compra")
    private LocalDate fechaCompra;

    @Column(name = "fecha_vencimiento_compra")
    private LocalDate fechaVencimientoCompra;

    @Column(name = "forma_pago_compra")
    private String formaPagoCompra;

    @Column(name = "medio_pago_compra")
    private String medioPagoCompra;

    @Column(name = "notas_compra")
    private String notasCompra;

    @Column(name = "total_compra")
    private Double totalCompra;

    @Column(name = "estado_compra")
    private String estadoCompra;

    @Column(name = "saldo_pendiente")
    private Double saldoPendiente;
}
