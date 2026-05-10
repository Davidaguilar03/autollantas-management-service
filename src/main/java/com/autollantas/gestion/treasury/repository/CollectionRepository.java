package com.autollantas.gestion.treasury.repository;

import com.autollantas.gestion.treasury.model.Collection;
import com.autollantas.gestion.sales.model.Sale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CollectionRepository extends JpaRepository<Collection, Integer> {
    List<Collection> findBySale(Sale sale);
}
