package com.autollantas.gestion.treasury.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Entity
@Data
@Table(name = "TRANSFERENCIAS")
public class Transfer {

    public Transfer(Account destinationAccount, Account sourceAccount, LocalDate date, Integer id, Double amount) {
        this.destinationAccount = destinationAccount;
        this.sourceAccount = sourceAccount;
        this.date = date;
        this.id = id;
        this.amount = amount;
    }

    public Transfer() {
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_transferencia")
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "id_cuenta_origen")
    private Account sourceAccount;

    @ManyToOne
    @JoinColumn(name = "id_cuenta_destino")
    private Account destinationAccount;

    @Column(name = "fecha_transferencia")
    private LocalDate date;

    @Column(name = "monto_transferencia")
    private Double amount;
}
