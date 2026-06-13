package com.autollantas.gestion.sales.repository;

import com.autollantas.gestion.sales.model.Sale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface SaleRepository extends JpaRepository<Sale, Integer> {

    List<Sale> findBySaleDateBetween(LocalDate from, LocalDate to);

    List<Sale> findByStatus(String status);

    @Query(value = "SELECT * FROM ventas ORDER BY id_venta DESC LIMIT 1", nativeQuery = true)
    Optional<Sale> findTopByOrderByIdDesc();

    @Query("SELECT COALESCE(SUM(CASE WHEN s.pendingBalance IS NOT NULL THEN s.pendingBalance ELSE s.total END), 0) FROM Sale s WHERE s.status = 'PENDIENTE'")
    double sumPendingReceivable();

    @Query("SELECT COALESCE(SUM(s.total), 0) FROM Sale s WHERE s.saleDate BETWEEN :start AND :end")
    double sumTotalByDateBetween(LocalDate start, LocalDate end);
}
