package com.autollantas.gestion.purchases.repository;

import com.autollantas.gestion.purchases.model.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SupplierRepository extends JpaRepository<Supplier, Integer> {
    Optional<Supplier> findByNitNumber(String nitNumber);
}
