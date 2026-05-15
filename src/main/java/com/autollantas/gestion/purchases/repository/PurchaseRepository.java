package com.autollantas.gestion.purchases.repository;

import com.autollantas.gestion.purchases.model.Purchase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface PurchaseRepository extends JpaRepository<Purchase, Integer> {
    List<Purchase> findByPurchaseDateBetween(LocalDate start, LocalDate end);
    List<Purchase> findByStatus(String status);
}
