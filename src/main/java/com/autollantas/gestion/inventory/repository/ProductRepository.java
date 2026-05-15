package com.autollantas.gestion.inventory.repository;

import com.autollantas.gestion.inventory.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository extends JpaRepository<Product, Integer> {
    Product findByCode(String code);
}
