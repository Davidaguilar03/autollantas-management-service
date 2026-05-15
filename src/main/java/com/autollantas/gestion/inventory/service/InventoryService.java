package com.autollantas.gestion.inventory.service;

import com.autollantas.gestion.inventory.model.Product;
import com.autollantas.gestion.inventory.model.ProductCategory;
import com.autollantas.gestion.inventory.repository.ProductCategoryRepository;
import com.autollantas.gestion.inventory.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class InventoryService {

    private final ProductRepository productRepository;
    private final ProductCategoryRepository productCategoryRepository;

    public InventoryService(ProductRepository productRepository,
                            ProductCategoryRepository productCategoryRepository) {
        this.productRepository = productRepository;
        this.productCategoryRepository = productCategoryRepository;
    }

    @Transactional(readOnly = true)
    public List<Product> findAllProducts() {
        return productRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Product> findProductsWithStock() {
        return productRepository.findAll().stream()
                .filter(p -> p.getQuantity() != null && p.getQuantity() > 0)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Optional<Product> findProductById(Integer id) {
        return productRepository.findById(id);
    }

    @Transactional
    public Product saveProduct(Product product) {
        return productRepository.save(product);
    }

    @Transactional
    public void deleteProduct(Product product) {
        productRepository.delete(product);
    }

    @Transactional(readOnly = true)
    public List<ProductCategory> findAllCategories() {
        return productCategoryRepository.findAll();
    }

    @Transactional(readOnly = true)
    public long countStockAlerts() {
        return productRepository.findAll().stream()
                .filter(p -> p.getCategory() != null
                        && p.getQuantity() != null
                        && p.getCategory().getYellowStockMin() != null
                        && p.getQuantity() <= p.getCategory().getYellowStockMin())
                .count();
    }
}
