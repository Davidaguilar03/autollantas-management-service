package com.autollantas.gestion.repository;

import com.autollantas.gestion.model.Transferencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TransferenciaRepository extends JpaRepository<Transferencia, Integer> {
    List<Transferencia> findByCuentaOrigen_IdCuentaOrCuentaDestino_IdCuentaOrderByFechaTransferenciaDesc(Integer idOrigen, Integer idDestino);
}