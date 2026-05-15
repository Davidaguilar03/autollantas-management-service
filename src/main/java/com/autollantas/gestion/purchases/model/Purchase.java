package com.autollantas.gestion.purchases.model;

import com.autollantas.gestion.treasury.model.Account;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Entity
@Data
@Table(name = "COMPRAS")
public class Purchase {
    public Purchase(Integer id, Supplier supplier, Account account, String invoiceNumber,
                    LocalDate purchaseDate, String paymentType, LocalDate dueDate,
                    String paymentMethod, String notes, Double total, String status) {
        this.id = id;
        this.supplier = supplier;
        this.account = account;
        this.invoiceNumber = invoiceNumber;
        this.purchaseDate = purchaseDate;
        this.paymentType = paymentType;
        this.dueDate = dueDate;
        this.paymentMethod = paymentMethod;
        this.notes = notes;
        this.total = total;
        this.status = status;
    }
    public Purchase() {}

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_compra")
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "id_proveedor")
    private Supplier supplier;

    @ManyToOne
    @JoinColumn(name = "id_cuenta")
    private Account account;

    @Column(name = "numero_factura_compra")
    private String invoiceNumber;

    @Column(name = "fecha_compra")
    private LocalDate purchaseDate;

    @Column(name = "fecha_vencimiento_compra")
    private LocalDate dueDate;

    @Column(name = "forma_pago_compra")
    private String paymentType;

    @Column(name = "medio_pago_compra")
    private String paymentMethod;

    @Column(name = "notas_compra")
    private String notes;

    @Column(name = "total_compra")
    private Double total;

    @Column(name = "estado_compra")
    private String status;

    @Column(name = "saldo_pendiente")
    private Double pendingBalance;
}
