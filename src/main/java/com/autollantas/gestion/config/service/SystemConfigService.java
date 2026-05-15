package com.autollantas.gestion.config.service;

import com.autollantas.gestion.config.model.SystemConfig;
import com.autollantas.gestion.config.repository.SystemConfigRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class SystemConfigService {

    private final SystemConfigRepository configurationRepository;

    public SystemConfigService(SystemConfigRepository configurationRepository) {
        this.configurationRepository = configurationRepository;
    }

    @Transactional(readOnly = true)
    public Optional<SystemConfig> findByKey(String key) {
        return configurationRepository.findByKey(key);
    }

    @Transactional(readOnly = true)
    public boolean validateAdminPassword(String enteredPassword) {
        return configurationRepository.findByKey("admin_password")
                .map(SystemConfig::getValue)
                .filter(password -> password != null && password.equals(enteredPassword))
                .isPresent();
    }

    @Transactional
    public SystemConfig save(SystemConfig configuration) {
        return configurationRepository.save(configuration);
    }
}
