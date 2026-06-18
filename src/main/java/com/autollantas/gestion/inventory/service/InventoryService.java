package com.autollantas.gestion.inventory.service;

import com.autollantas.gestion.inventory.model.Product;
import com.autollantas.gestion.inventory.model.ProductCategory;
import com.autollantas.gestion.inventory.model.TaxType;
import com.autollantas.gestion.inventory.repository.ProductCategoryRepository;
import com.autollantas.gestion.inventory.repository.ProductRepository;
import com.autollantas.gestion.inventory.repository.TaxTypeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class InventoryService {

    private final ProductRepository productRepository;
    private final ProductCategoryRepository productCategoryRepository;
    private final TaxTypeRepository taxTypeRepository;

    public InventoryService(ProductRepository productRepository,
                            ProductCategoryRepository productCategoryRepository,
                            TaxTypeRepository taxTypeRepository) {
        this.productRepository = productRepository;
        this.productCategoryRepository = productCategoryRepository;
        this.taxTypeRepository = taxTypeRepository;
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

    @Transactional(readOnly = true)
    public List<TaxType> findAllTaxTypes() {
        return taxTypeRepository.findAll();
    }

    @Transactional
    public TaxType saveTaxType(TaxType taxType) {
        return taxTypeRepository.save(taxType);
    }

    @Transactional
    public void deleteTaxType(TaxType taxType) {
        taxTypeRepository.delete(taxType);
    }

    @Transactional(readOnly = true)
    public List<TaxType> findProductTaxTypes() {
        return taxTypeRepository.findByAppliesToTransaction(false);
    }

    @Transactional(readOnly = true)
    public List<TaxType> findTransactionTaxTypes() {
        return taxTypeRepository.findByAppliesToTransaction(true);
    }

    @Transactional
    public ProductCategory saveCategory(ProductCategory category) {
        if (category.getName() != null) {
            category.setName(category.getName().trim().toUpperCase());
        }
        return productCategoryRepository.save(category);
    }

    @Transactional(readOnly = true)
    public List<Product> findProductsByCategory(ProductCategory category) {
        return productRepository.findByCategory(category);
    }

    @Transactional
    public void recalculateMinSalePrice(Product product) {
        if (product.getPurchaseCost() == null || product.getPurchaseCost() == 0) return;

        double purchaseCost = product.getPurchaseCost();

        // IVA siempre 19%
        double ivaFavor = purchaseCost * 0.19;
        product.setTaxAmount(ivaFavor);

        // minSalePrice = purchaseCost + suma de todos los impuestos asignados a la categoría
        double totalTaxRate = 0.0;
        if (product.getCategory() != null && product.getCategory().getTaxTypes() != null) {
            for (TaxType t : product.getCategory().getTaxTypes()) {
                if (t.getRate() != null
                        && !Boolean.TRUE.equals(t.getIsVat())) {
                    totalTaxRate += t.getRate();
                }
            }
        }
        double minSalePrice = purchaseCost + (purchaseCost * totalTaxRate);
        product.setMinSalePrice(minSalePrice);

        double margin = (product.getCategory() != null
                && product.getCategory().getTargetMargin() != null)
                ? product.getCategory().getTargetMargin() : 0.0;
        product.setSuggestedPrice(minSalePrice * (1 + margin));

        productRepository.save(product);
    }

    @Transactional
    public void ensureVatExists() {
        boolean exists = taxTypeRepository.findAll().stream()
                .anyMatch(t -> Boolean.TRUE.equals(t.getIsVat()));
        if (!exists) {
            TaxType iva = new TaxType();
            iva.setName("IVA");
            iva.setRate(0.19);
            iva.setDescription("Impuesto al Valor Agregado");
            iva.setAppliesToTransaction(false);
            iva.setIsVat(true);
            taxTypeRepository.save(iva);
        }
    }

    @Transactional
    public void assignVatToCategory(ProductCategory category) {
        TaxType iva = taxTypeRepository.findAll().stream()
                .filter(t -> Boolean.TRUE.equals(t.getIsVat()))
                .findFirst().orElse(null);
        if (iva == null) return;
        ProductCategory fresh = productCategoryRepository
                .findById(category.getId()).orElse(category);
        boolean alreadyHas = fresh.getTaxTypes().stream()
                .anyMatch(t -> Boolean.TRUE.equals(t.getIsVat()));
        if (!alreadyHas) {
            fresh.getTaxTypes().add(iva);
            productCategoryRepository.save(fresh);
            category.setTaxTypes(fresh.getTaxTypes());
        }
    }

    @Transactional
    public ProductCategory createCategory(String name, String color) {
        name = name.trim().toUpperCase();
        ProductCategory cat = new ProductCategory();
        cat.setName(name);
        cat.setColor(color);
        cat.setTaxTypes(new ArrayList<>());
        cat = productCategoryRepository.save(cat);
        assignVatToCategory(cat);
        return cat;
    }

    @Transactional
    public void addTaxToCategory(ProductCategory cat, TaxType tax) {
        ProductCategory fresh = productCategoryRepository.findById(cat.getId()).orElse(cat);
        boolean alreadyHas = fresh.getTaxTypes().stream().anyMatch(t -> t.getId().equals(tax.getId()));
        if (!alreadyHas) {
            fresh.getTaxTypes().add(tax);
            productCategoryRepository.save(fresh);
            findProductsByCategory(fresh).forEach(this::recalculateMinSalePrice);
        }
    }

    @Transactional
    public void removeTaxFromCategory(ProductCategory cat, TaxType tax) {
        ProductCategory fresh = productCategoryRepository.findById(cat.getId()).orElse(cat);
        fresh.getTaxTypes().removeIf(t -> t.getId().equals(tax.getId()));
        productCategoryRepository.save(fresh);
        findProductsByCategory(fresh).forEach(this::recalculateMinSalePrice);
    }

    @Transactional
    public boolean deleteCategory(ProductCategory cat) {
        List<Product> productos = findProductsByCategory(cat);
        if (!productos.isEmpty()) return false;
        productCategoryRepository.delete(cat);
        return true;
    }

    @Transactional
    public void assignVatToAllCategories() {
        TaxType iva = taxTypeRepository.findAll().stream()
                .filter(t -> Boolean.TRUE.equals(t.getIsVat()))
                .findFirst().orElse(null);
        if (iva == null) return;

        List<ProductCategory> categories = productCategoryRepository.findAll();
        for (ProductCategory cat : categories) {
            if (cat.getTaxTypes() == null) cat.setTaxTypes(new ArrayList<>());
            boolean alreadyHas = cat.getTaxTypes().stream()
                    .anyMatch(t -> Boolean.TRUE.equals(t.getIsVat()));
            if (!alreadyHas) {
                cat.getTaxTypes().add(iva);
                productCategoryRepository.save(cat);
            }
        }
    }
}
