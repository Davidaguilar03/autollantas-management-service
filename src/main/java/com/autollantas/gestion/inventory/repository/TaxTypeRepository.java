package com.autollantas.gestion.inventory.repository;

import com.autollantas.gestion.inventory.model.TaxType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaxTypeRepository extends JpaRepository<TaxType, Integer> {
    List<TaxType> findByAppliesToTransaction(Boolean appliesToTransaction);
}
