package com.autollantas.gestion.treasury.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Entity
@Data
@Table(name = "MOVIMIENTOS")
public class Movement {

    public Movement(LocalDate date, Integer sourceId, String type, Double amount, Account account) {
        this.date = date;
        this.sourceId = sourceId;
        this.type = type;
        this.amount = amount;
        this.account = account;
    }

    public Movement() {
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_movimiento")
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "id_cuenta")
    private Account account;

    @Column(name = "fecha_movimiento")
    private LocalDate date;

    @Column(name = "tipo_movimiento")
    private String type;

    @Column(name = "monto_movimiento")
    private Double amount;

    @Column(name = "id_origen_movimiento")
    private Integer sourceId;

    @Column(name = "tabla_origen_movimiento")
    private String sourceTable;
}
