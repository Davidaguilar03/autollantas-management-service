package com.autollantas.gestion.treasury.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Entity
@Data
@Table(name = "GASTOS_OPERATIVOS")
public class OperationalExpense {

    public OperationalExpense(String concept, Account account, LocalDate date, Integer id, Double amount, String notes) {
        this.concept = concept;
        this.account = account;
        this.date = date;
        this.id = id;
        this.amount = amount;
        this.notes = notes;
    }

    public OperationalExpense() {
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_gasto")
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "id_cuenta")
    private Account account;

    @Column(name = "concepto_gasto")
    private String concept;

    @Column(name = "monto_gasto")
    private Double amount;

    @Column(name = "fecha_gasto")
    private LocalDate date;

    private String notes;
}
