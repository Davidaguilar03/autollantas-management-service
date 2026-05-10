package com.autollantas.gestion.treasury.repository;

import com.autollantas.gestion.purchases.model.Compra;
import com.autollantas.gestion.treasury.model.Pago;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PagoRepository extends JpaRepository<Pago, Integer> {
    List<Pago> findByCompra(Compra compra);
}