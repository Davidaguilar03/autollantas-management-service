package com.autollantas.gestion.repository;

import com.autollantas.gestion.model.Recaudo;
import com.autollantas.gestion.model.Venta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface RecaudoRepository extends JpaRepository<Recaudo, Integer> {
    List<Recaudo> findByVenta(Venta venta);
}