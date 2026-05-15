package com.autollantas.gestion.purchases.repository;

import com.autollantas.gestion.purchases.model.Purchase;
import com.autollantas.gestion.purchases.model.PurchaseDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PurchaseDetailRepository extends JpaRepository<PurchaseDetail, Integer> {
    List<PurchaseDetail> findByPurchase(Purchase purchase);
}
