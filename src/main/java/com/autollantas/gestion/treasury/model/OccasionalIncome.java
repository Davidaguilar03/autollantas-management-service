package com.autollantas.gestion.treasury.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Entity
@Data
@Table(name = "INGRESOS_OCASIONALES")
public class OccasionalIncome {

    public OccasionalIncome(String concept, Account account, LocalDate date, Integer id, Double amount, String notes) {
        this.concept = concept;
        this.account = account;
        this.date = date;
        this.id = id;
        this.amount = amount;
        this.notes = notes;
    }

    public OccasionalIncome() {
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_ingreso")
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "id_cuenta")
    private Account account;

    @Column(name = "concepto_ingreso")
    private String concept;

    @Column(name = "monto_ingreso")
    private Double amount;

    @Column(name = "fecha_ingreso")
    private LocalDate date;

    private String notes;
}
