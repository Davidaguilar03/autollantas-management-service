package com.autollantas.gestion.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "CONFIGURACION_SISTEMA")
public class Configuracion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "CLAVE_CONFIG", unique = true)
    private String clave;

    @Column(name = "VALOR_CONFIG")
    private String valor;
}