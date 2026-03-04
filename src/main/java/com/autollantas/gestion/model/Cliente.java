package com.autollantas.gestion.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "CLIENTES")
public class Cliente {
    public Cliente(Integer idCliente, String nombreCliente, String numeroDocumentoCliente, String celularCliente, String correoCliente) {
        this.idCliente = idCliente;
        this.nombreCliente = nombreCliente;
        this.numeroDocumentoCliente = numeroDocumentoCliente;
        this.celularCliente = celularCliente;
        this.correoCliente = correoCliente;
    }

    public Cliente() {
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_cliente")
    private Integer idCliente;

    @Column(name = "nombre_cliente")
    private String nombreCliente;

    @Column(name = "numero_documento_cliente")
    private String numeroDocumentoCliente;

    @Column(name = "correo_cliente")
    private String correoCliente;

    @Column(name = "celular_cliente")
    private String celularCliente;
}
