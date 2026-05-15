package com.autollantas.gestion.treasury.repository;

import com.autollantas.gestion.treasury.model.OccasionalIncome;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface OccasionalIncomeRepository extends JpaRepository<OccasionalIncome, Integer> {
    List<OccasionalIncome> findByDateBetween(LocalDate start, LocalDate end);
}
