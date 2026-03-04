package com.autollantas.gestion.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Entity
@Data
@Table(name = "RECAUDOS")
public class Recaudo {
    public Recaudo(Cuenta cuenta, LocalDate fechaRecaudo, Integer idRecaudo, String metodoPagoRecaudo, Double valorRecaudo, Venta venta) {
        this.cuenta = cuenta;
        this.fechaRecaudo = fechaRecaudo;
        this.idRecaudo = idRecaudo;
        this.metodoPagoRecaudo = metodoPagoRecaudo;
        this.valorRecaudo = valorRecaudo;
        this.venta = venta;
    }
    public Recaudo() {
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_recaudo")
    private Integer idRecaudo;

    @ManyToOne
    @JoinColumn(name = "id_venta")
    private Venta venta;

    @ManyToOne
    @JoinColumn(name = "id_cuenta")
    private Cuenta cuenta;

    @Column(name = "fecha_recaudo")
    private LocalDate fechaRecaudo;

    @Column(name = "metodo_pago_recaudo")
    private String metodoPagoRecaudo;

    @Column(name = "valor_recaudo")
    private Double valorRecaudo;
}