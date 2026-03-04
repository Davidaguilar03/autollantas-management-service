package com.autollantas.gestion.repository;

import com.autollantas.gestion.model.Configuracion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface ConfiguracionRepository extends JpaRepository<Configuracion, Long> {
    Optional<Configuracion> findByClave(String clave);
}
