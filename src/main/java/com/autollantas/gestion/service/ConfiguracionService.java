package com.autollantas.gestion.service;

import com.autollantas.gestion.model.Configuracion;
import com.autollantas.gestion.repository.ConfiguracionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class ConfiguracionService {

    private final ConfiguracionRepository configuracionRepository;

    public ConfiguracionService(ConfiguracionRepository configuracionRepository) {
        this.configuracionRepository = configuracionRepository;
    }

    @Transactional(readOnly = true)
    public Optional<Configuracion> findByClave(String clave) {
        return configuracionRepository.findByClave(clave);
    }

    @Transactional(readOnly = true)
    public boolean validarPasswordAdmin(String passwordIngresada) {
        return configuracionRepository.findByClave("admin_password")
                .map(Configuracion::getValor)
                .filter(password -> password != null && password.equals(passwordIngresada))
                .isPresent();
    }

    @Transactional
    public Configuracion guardar(Configuracion configuracion) {
        return configuracionRepository.save(configuracion);
    }
}

