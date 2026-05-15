package com.autollantas.gestion.treasury.repository;

import com.autollantas.gestion.treasury.model.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AccountRepository extends JpaRepository<Account, Integer> {
}
