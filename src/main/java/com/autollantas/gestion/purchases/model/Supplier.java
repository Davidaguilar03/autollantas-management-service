package com.autollantas.gestion.purchases.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "PROVEEDORES")
public class Supplier {
    public Supplier(String phone, String email, Integer id, String name, String nitNumber) {
        this.phone = phone;
        this.email = email;
        this.id = id;
        this.name = name;
        this.nitNumber = nitNumber;
    }
    public Supplier() {}

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_proveedor")
    private Integer id;

    @Column(name = "nombre_proveedor")
    private String name;

    @Column(name = "numero_nit_proveedor")
    private String nitNumber;

    @Column(name = "correo_proveedor")
    private String email;

    @Column(name = "celular_proveedor")
    private String phone;
}
