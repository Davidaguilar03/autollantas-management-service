package com.autollantas.gestion.sales.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "CLIENTES")
public class Customer {

    public Customer(Integer id, String name, String documentNumber, String email, String phone) {
        this.id = id;
        this.name = name;
        this.documentNumber = documentNumber;
        this.email = email;
        this.phone = phone;
    }

    public Customer() {
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_cliente")
    private Integer id;

    @Column(name = "nombre_cliente")
    private String name;

    @Column(name = "numero_documento_cliente")
    private String documentNumber;

    @Column(name = "correo_cliente")
    private String email;

    @Column(name = "celular_cliente")
    private String phone;

    @Column(name = "tipo_documento_cliente")
    private String documentType;
}
