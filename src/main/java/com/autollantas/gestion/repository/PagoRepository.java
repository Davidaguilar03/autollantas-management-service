package com.autollantas.gestion.repository;

import com.autollantas.gestion.model.Compra;
import com.autollantas.gestion.model.Pago;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PagoRepository extends JpaRepository<Pago, Integer> {
    List<Pago> findByCompra(Compra compra);
}