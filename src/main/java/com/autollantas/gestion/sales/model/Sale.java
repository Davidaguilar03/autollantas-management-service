package com.autollantas.gestion.sales.model;

import com.autollantas.gestion.treasury.model.Account;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Entity
@Data
@Table(name = "VENTAS")
public class Sale {

    public Sale(Customer customer, Account account, String status, LocalDate dueDate, LocalDate saleDate,
                String paymentType, Integer id, String paymentMethod, String notes, String invoiceNumber, Double total) {
        this.customer = customer;
        this.account = account;
        this.status = status;
        this.dueDate = dueDate;
        this.saleDate = saleDate;
        this.paymentType = paymentType;
        this.id = id;
        this.paymentMethod = paymentMethod;
        this.notes = notes;
        this.invoiceNumber = invoiceNumber;
        this.total = total;
        this.pendingBalance = total;
    }

    public Sale() {
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_venta")
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "id_cliente")
    private Customer customer;

    @ManyToOne
    @JoinColumn(name = "id_cuenta")
    private Account account;

    @Column(name = "numero_factura_venta")
    private String invoiceNumber;

    @Column(name = "fecha_venta")
    private LocalDate saleDate;

    @Column(name = "fecha_vencimiento_venta")
    private LocalDate dueDate;

    @Column(name = "forma_pago_venta")
    private String paymentType;

    @Column(name = "medio_pago_venta")
    private String paymentMethod;

    @Column(name = "notas_venta")
    private String notes;

    @Column(name = "total_venta")
    private Double total;

    @Column(name = "estado_venta")
    private String status;

    @Column(name = "saldo_pendiente")
    private Double pendingBalance;
}
