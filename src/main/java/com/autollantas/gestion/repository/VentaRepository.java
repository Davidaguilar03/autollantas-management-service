package com.autollantas.gestion.repository;

import com.autollantas.gestion.model.Venta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface VentaRepository extends JpaRepository<Venta, Integer> {

    List<Venta> findByFechaVentaBetween(LocalDate inicio, LocalDate fin);

    List<Venta> findByEstadoVenta(String estadoVenta);

    @Query(value = "SELECT * FROM ventas ORDER BY id_venta DESC LIMIT 1", nativeQuery = true)
    Optional<Venta> findTopByOrderByIdVentaDesc();
}
