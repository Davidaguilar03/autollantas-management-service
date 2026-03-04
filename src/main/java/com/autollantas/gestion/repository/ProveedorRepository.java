package com.autollantas.gestion.repository;

import com.autollantas.gestion.model.Cliente;
import com.autollantas.gestion.model.Proveedor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;


@Repository
public interface ProveedorRepository extends JpaRepository<Proveedor, Long> {
    Optional<Proveedor> findByNumeroNitProveedor(String numeroDocumentoProveedor);
}
