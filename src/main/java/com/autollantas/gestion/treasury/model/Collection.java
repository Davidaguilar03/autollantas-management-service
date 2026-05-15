package com.autollantas.gestion.treasury.model;

import com.autollantas.gestion.sales.model.Sale;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Entity
@Data
@Table(name = "RECAUDOS")
public class Collection {

    public Collection(Account account, LocalDate date, Integer id, String paymentMethod, Double amount, Sale sale) {
        this.account = account;
        this.date = date;
        this.id = id;
        this.paymentMethod = paymentMethod;
        this.amount = amount;
        this.sale = sale;
    }

    public Collection() {
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_recaudo")
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "id_venta")
    private Sale sale;

    @ManyToOne
    @JoinColumn(name = "id_cuenta")
    private Account account;

    @Column(name = "fecha_recaudo")
    private LocalDate date;

    @Column(name = "metodo_pago_recaudo")
    private String paymentMethod;

    @Column(name = "valor_recaudo")
    private Double amount;
}
