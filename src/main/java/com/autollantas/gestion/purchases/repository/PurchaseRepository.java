package com.autollantas.gestion.purchases.repository;

import com.autollantas.gestion.purchases.model.Purchase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface PurchaseRepository extends JpaRepository<Purchase, Integer> {
    List<Purchase> findByPurchaseDateBetween(LocalDate start, LocalDate end);
    List<Purchase> findByStatus(String status);

    @Query("SELECT COALESCE(SUM(CASE WHEN p.pendingBalance IS NOT NULL THEN p.pendingBalance ELSE p.total END), 0) FROM Purchase p WHERE p.status = 'PENDIENTE'")
    double sumPendingPayable();

    @Query("SELECT COALESCE(SUM(p.total), 0) FROM Purchase p WHERE p.purchaseDate BETWEEN :start AND :end")
    double sumTotalByDateBetween(LocalDate start, LocalDate end);
}
