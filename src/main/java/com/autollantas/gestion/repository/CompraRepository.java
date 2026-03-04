package com.autollantas.gestion.repository;

import com.autollantas.gestion.model.Compra;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface CompraRepository extends JpaRepository<Compra, Integer> {

    List<Compra> findByFechaCompraBetween(LocalDate inicio, LocalDate fin);

    List<Compra> findByEstadoCompra(String estadoCompra);
}