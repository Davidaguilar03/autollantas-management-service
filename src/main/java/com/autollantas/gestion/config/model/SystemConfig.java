package com.autollantas.gestion.config.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "CONFIGURACION_SISTEMA")
public class SystemConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "CLAVE_CONFIG", unique = true)
    private String key;

    @Column(name = "VALOR_CONFIG")
    private String value;
}
