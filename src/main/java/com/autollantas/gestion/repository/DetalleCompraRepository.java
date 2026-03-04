package com.autollantas.gestion.repository;

import com.autollantas.gestion.model.Compra;
import com.autollantas.gestion.model.DetalleCompra;
import com.autollantas.gestion.model.DetalleVenta;
import com.autollantas.gestion.model.Venta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DetalleCompraRepository extends JpaRepository<DetalleCompra, Long> {
    List<DetalleCompra> findByCompra(Compra compra);
}
