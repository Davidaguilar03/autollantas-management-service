package com.autollantas.gestion.treasury.repository;

import com.autollantas.gestion.purchases.model.Purchase;
import com.autollantas.gestion.treasury.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Integer> {
    List<Payment> findByPurchase(Purchase purchase);
}
