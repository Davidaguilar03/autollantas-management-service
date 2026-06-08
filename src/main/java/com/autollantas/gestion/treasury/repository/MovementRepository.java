package com.autollantas.gestion.treasury.repository;

import com.autollantas.gestion.treasury.model.Movement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MovementRepository extends JpaRepository<Movement, Integer> {
    List<Movement> findByAccount_IdOrderByDateDesc(Integer accountId);
    List<Movement> findBySourceIdAndSourceTable(Integer sourceId, String sourceTable);
}
