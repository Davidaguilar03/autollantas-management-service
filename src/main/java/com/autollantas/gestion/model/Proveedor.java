package com.autollantas.gestion.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "PROVEEDORES")
public class Proveedor {
    public Proveedor(String celularProveedor, String correoProveedor, Integer idProveedor, String nombreProveedor, String numeroNitProveedor) {
        this.celularProveedor = celularProveedor;
        this.correoProveedor = correoProveedor;
        this.idProveedor = idProveedor;
        this.nombreProveedor = nombreProveedor;
        this.numeroNitProveedor = numeroNitProveedor;
    }
    public Proveedor() {
    }
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_proveedor")
    private Integer idProveedor;

    @Column(name = "nombre_proveedor")
    private String nombreProveedor;

    @Column(name = "numero_nit_proveedor")
    private String numeroNitProveedor;

    @Column(name = "correo_proveedor")
    private String correoProveedor;

    @Column(name = "celular_proveedor")
    private String celularProveedor;
}