package com.autollantas.gestion.treasury.repository;

import com.autollantas.gestion.treasury.model.IngresoOcasional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface IngresoOcasionalRepository extends JpaRepository<IngresoOcasional, Integer> {
    List<IngresoOcasional> findByFechaIngresoBetween(LocalDate inicio, LocalDate fin);
}
