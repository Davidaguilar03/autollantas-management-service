package com.autollantas.gestion.service;

import com.autollantas.gestion.model.CategoriaProducto;
import com.autollantas.gestion.model.Producto;
import com.autollantas.gestion.repository.CategoriaProductoRepository;
import com.autollantas.gestion.repository.ProductoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class InventarioService {

    private final ProductoRepository productoRepository;
    private final CategoriaProductoRepository categoriaProductoRepository;

    public InventarioService(ProductoRepository productoRepository,
                             CategoriaProductoRepository categoriaProductoRepository) {
        this.productoRepository = productoRepository;
        this.categoriaProductoRepository = categoriaProductoRepository;
    }

    @Transactional(readOnly = true)
    public List<Producto> findAllProductos() {
        return productoRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Producto> findProductosConStock() {
        return productoRepository.findAll().stream()
                .filter(p -> p.getCantidad() != null && p.getCantidad() > 0)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Optional<Producto> findProductoById(Integer idProducto) {
        return productoRepository.findById(idProducto);
    }

    @Transactional
    public Producto saveProducto(Producto producto) {
        return productoRepository.save(producto);
    }

    @Transactional
    public void deleteProducto(Producto producto) {
        productoRepository.delete(producto);
    }

    @Transactional(readOnly = true)
    public List<CategoriaProducto> findAllCategorias() {
        return categoriaProductoRepository.findAll();
    }

    @Transactional(readOnly = true)
    public long contarAlertasStock() {
        return productoRepository.findAll().stream()
                .filter(p -> p.getCategoria() != null
                        && p.getCantidad() != null
                        && p.getCategoria().getStockMinAmarillo() != null
                        && p.getCantidad() <= p.getCategoria().getStockMinAmarillo())
                .count();
    }
}

