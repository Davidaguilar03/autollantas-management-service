package com.autollantas.gestion.repository;

import com.autollantas.gestion.model.GastoOperativo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime; // O LocalDate si usas solo fecha
import java.util.List;

@Repository
public interface GastoOperativoRepository extends JpaRepository<GastoOperativo, Long> {

    List<GastoOperativo> findByFechaGastoBetween(LocalDate inicio, LocalDate fin);
}
