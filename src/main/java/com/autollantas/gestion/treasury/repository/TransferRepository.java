package com.autollantas.gestion.treasury.repository;

import com.autollantas.gestion.treasury.model.Transfer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TransferRepository extends JpaRepository<Transfer, Integer> {
    List<Transfer> findBySourceAccount_IdOrDestinationAccount_IdOrderByDateDesc(Integer sourceId, Integer destinationId);
}
