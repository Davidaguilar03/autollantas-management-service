package com.autollantas.gestion.sales.repository;

import com.autollantas.gestion.sales.model.DetalleVenta;
import com.autollantas.gestion.sales.model.Venta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DetalleVentaRepository extends JpaRepository<DetalleVenta, Integer> {
    List<DetalleVenta> findByVenta(Venta venta);
}