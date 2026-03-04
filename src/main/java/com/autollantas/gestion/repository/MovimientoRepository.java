package com.autollantas.gestion.repository;

import com.autollantas.gestion.model.Movimiento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MovimientoRepository extends JpaRepository<Movimiento, Integer> {
    List<Movimiento> findByCuenta_IdCuentaOrderByFechaMovimientoDesc(Integer idCuenta);
}