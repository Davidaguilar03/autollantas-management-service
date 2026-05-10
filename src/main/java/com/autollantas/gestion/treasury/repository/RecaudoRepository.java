package com.autollantas.gestion.treasury.repository;

import com.autollantas.gestion.treasury.model.Recaudo;
import com.autollantas.gestion.sales.model.Venta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface RecaudoRepository extends JpaRepository<Recaudo, Integer> {
    List<Recaudo> findByVenta(Venta venta);
}