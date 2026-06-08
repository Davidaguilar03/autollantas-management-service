package com.autollantas.gestion.inventory.service;

import com.autollantas.gestion.inventory.model.Product;
import com.autollantas.gestion.inventory.model.ProductCategory;
import com.autollantas.gestion.inventory.repository.ProductCategoryRepository;
import com.autollantas.gestion.inventory.repository.ProductRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock ProductRepository productRepository;
    @Mock ProductCategoryRepository productCategoryRepository;

    @InjectMocks InventoryService inventoryService;

    // InventoryService no expone getCriticalProducts() ni getWarningProducts();
    // los tests 1-2 se adaptan a los métodos reales del servicio.

    @Nested
    class AlertasDeStock {

        @Test
        void countStockAlerts_deberia_contar_productosConStockBajoOIgualAUmbralAmarillo() {
            // Test 1 adaptado: no existe getCriticalProducts(redStockMin).
            // countStockAlerts() usa yellowStockMin; un producto ≤ umbral suma 1 alerta.
            ProductCategory cat = new ProductCategory(null, "Llantas", 5, 2);

            Product bajoUmbral = new Product();  // qty=3 ≤ yellowStockMin=5 → alerta
            bajoUmbral.setQuantity(3);
            bajoUmbral.setCategory(cat);

            Product sobreUmbral = new Product(); // qty=8 > yellowStockMin=5 → sin alerta
            sobreUmbral.setQuantity(8);
            sobreUmbral.setCategory(cat);

            Product sinCategoria = new Product(); // category=null → ignorado
            sinCategoria.setQuantity(1);

            when(productRepository.findAll()).thenReturn(List.of(bajoUmbral, sobreUmbral, sinCategoria));

            long alertas = inventoryService.countStockAlerts();

            assertThat(alertas).isEqualTo(1);
        }

        @Test
        void countStockAlerts_deberia_contar_multiples_alertas() {
            // Variante: dos productos bajo umbral → 2 alertas.
            ProductCategory cat = new ProductCategory(null, "Aceites", 5, 2);

            Product p1 = new Product();
            p1.setQuantity(1);
            p1.setCategory(cat);

            Product p2 = new Product();
            p2.setQuantity(5); // igual al umbral también cuenta (<=)
            p2.setCategory(cat);

            Product p3 = new Product();
            p3.setQuantity(6);
            p3.setCategory(cat);

            when(productRepository.findAll()).thenReturn(List.of(p1, p2, p3));

            long alertas = inventoryService.countStockAlerts();

            assertThat(alertas).isEqualTo(2);
        }
    }

    @Nested
    class ProductosConStock {

        @Test
        void findProductsWithStock_deberia_excluir_productosConStockCeroONulo() {
            // Test 2 adaptado: no existe getWarningProducts().
            // findProductsWithStock() filtra quantity > 0.
            Product conStock = new Product();
            conStock.setQuantity(5);

            Product sinStock = new Product();
            sinStock.setQuantity(0);

            Product stockNulo = new Product();
            stockNulo.setQuantity(null);

            when(productRepository.findAll()).thenReturn(List.of(conStock, sinStock, stockNulo));

            List<Product> result = inventoryService.findProductsWithStock();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getQuantity()).isEqualTo(5);
        }
    }

    @Nested
    class ActualizacionDeStock {

        @Test
        void saveProduct_deberia_llamar_repositorioSave_conProductoModificado() {
            // Test 3: saveProduct delega en productRepository.save().
            Product product = new Product();
            product.setId(1);
            product.setQuantity(15);

            Product saved = new Product();
            saved.setId(1);
            saved.setQuantity(15);
            when(productRepository.save(any(Product.class))).thenReturn(saved);

            Product result = inventoryService.saveProduct(product);

            verify(productRepository).save(product);
            assertThat(result.getQuantity()).isEqualTo(15);
        }
    }
}
