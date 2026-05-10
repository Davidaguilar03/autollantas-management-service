package com.autollantas.gestion.treasury.repository;

import com.autollantas.gestion.treasury.model.OperationalExpense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface OperationalExpenseRepository extends JpaRepository<OperationalExpense, Long> {
    List<OperationalExpense> findByDateBetween(LocalDate start, LocalDate end);
}
