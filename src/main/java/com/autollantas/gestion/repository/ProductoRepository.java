package com.autollantas.gestion.repository;

import com.autollantas.gestion.model.Producto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductoRepository extends JpaRepository<Producto, Integer> {
    Producto findByCodigoProducto(String s);
}