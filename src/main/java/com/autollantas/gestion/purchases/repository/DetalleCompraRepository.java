package com.autollantas.gestion.purchases.repository;

import com.autollantas.gestion.purchases.model.Compra;
import com.autollantas.gestion.purchases.model.DetalleCompra;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DetalleCompraRepository extends JpaRepository<DetalleCompra, Long> {
    List<DetalleCompra> findByCompra(Compra compra);
}
