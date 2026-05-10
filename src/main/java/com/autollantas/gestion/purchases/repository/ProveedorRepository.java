package com.autollantas.gestion.purchases.repository;

import com.autollantas.gestion.purchases.model.Proveedor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;


@Repository
public interface ProveedorRepository extends JpaRepository<Proveedor, Long> {
    Optional<Proveedor> findByNumeroNitProveedor(String numeroDocumentoProveedor);
}
