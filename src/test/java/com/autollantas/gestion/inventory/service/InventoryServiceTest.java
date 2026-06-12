package com.autollantas.gestion.inventory.service;

import com.autollantas.gestion.inventory.model.Product;
import com.autollantas.gestion.inventory.model.ProductCategory;
import com.autollantas.gestion.inventory.model.TaxType;
import com.autollantas.gestion.inventory.repository.ProductCategoryRepository;
import com.autollantas.gestion.inventory.repository.ProductRepository;
import com.autollantas.gestion.inventory.repository.TaxTypeRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock ProductRepository productRepository;
    @Mock ProductCategoryRepository productCategoryRepository;
    @Mock TaxTypeRepository taxTypeRepository;

    @InjectMocks InventoryService inventoryService;

    @Nested
    class AlertasDeStock {

        @Test
        void countStockAlerts_deberia_contar_productosConStockBajoOIgualAUmbralAmarillo() {
            ProductCategory cat = new ProductCategory(null, "Llantas", 5, 2, null);

            Product bajoUmbral = new Product();
            bajoUmbral.setQuantity(3);
            bajoUmbral.setCategory(cat);

            Product sobreUmbral = new Product();
            sobreUmbral.setQuantity(8);
            sobreUmbral.setCategory(cat);

            Product sinCategoria = new Product();
            sinCategoria.setQuantity(1);

            when(productRepository.findAll()).thenReturn(List.of(bajoUmbral, sobreUmbral, sinCategoria));

            long alertas = inventoryService.countStockAlerts();

            assertThat(alertas).isEqualTo(1);
        }

        @Test
        void countStockAlerts_deberia_contar_multiples_alertas() {
            ProductCategory cat = new ProductCategory(null, "Aceites", 5, 2, null);

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

    @Nested
    class RecalcularPrecioMinimo {

        private TaxType ivaType() {
            TaxType iva = new TaxType();
            iva.setIsVat(true);
            iva.setRate(0.19);
            iva.setAppliesToTransaction(false);
            return iva;
        }

        @Test
        void soloIva_taxAmountEs19pctYMinimoIgualCosto() {
            ProductCategory cat = new ProductCategory(null, "LLANTAS", 5, 2, null);
            cat.setTaxTypes(new ArrayList<>(List.of(ivaType())));

            Product p = new Product();
            p.setPurchaseCost(100000.0);
            p.setCategory(cat);

            when(productRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            inventoryService.recalculateMinSalePrice(p);

            assertThat(p.getTaxAmount()).isCloseTo(19000.0, within(0.01));
            assertThat(p.getMinSalePrice()).isCloseTo(100000.0, within(0.01));
        }

        @Test
        void ivaYOtroImpuesto3pct_minimoIncluyeOtros() {
            TaxType otro = new TaxType();
            otro.setIsVat(false);
            otro.setRate(0.03);
            otro.setAppliesToTransaction(false);

            ProductCategory cat = new ProductCategory(null, "LLANTAS", 5, 2, null);
            cat.setTaxTypes(new ArrayList<>(List.of(ivaType(), otro)));

            Product p = new Product();
            p.setPurchaseCost(100000.0);
            p.setCategory(cat);

            when(productRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            inventoryService.recalculateMinSalePrice(p);

            assertThat(p.getTaxAmount()).isCloseTo(19000.0, within(0.01));
            assertThat(p.getMinSalePrice()).isCloseTo(103000.0, within(0.01));
        }

        @Test
        void conMargen30pct_suggestedPriceEsMinimoMas30pct() {
            ProductCategory cat = new ProductCategory(null, "LLANTAS", 5, 2, 0.30);
            cat.setTaxTypes(new ArrayList<>(List.of(ivaType())));

            Product p = new Product();
            p.setPurchaseCost(100000.0);
            p.setCategory(cat);

            when(productRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            inventoryService.recalculateMinSalePrice(p);

            assertThat(p.getSuggestedPrice()).isCloseTo(130000.0, within(0.01));
        }

        @Test
        void purchaseCostNull_noLanzaExcepcion_noModificaProducto() {
            Product p = new Product();
            p.setPurchaseCost(null);

            assertThatNoException().isThrownBy(() -> inventoryService.recalculateMinSalePrice(p));
            assertThat(p.getMinSalePrice()).isNull();
            assertThat(p.getTaxAmount()).isNull();
        }

        @Test
        void purchaseCostCero_noLanzaExcepcion_noModificaProducto() {
            Product p = new Product();
            p.setPurchaseCost(0.0);

            assertThatNoException().isThrownBy(() -> inventoryService.recalculateMinSalePrice(p));
            assertThat(p.getMinSalePrice()).isNull();
        }
    }

    @Nested
    class EnsureVatExists {

        @Test
        void noExisteIva_creaUnoNuevo() {
            when(taxTypeRepository.findAll()).thenReturn(List.of());
            when(taxTypeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            inventoryService.ensureVatExists();

            verify(taxTypeRepository).save(argThat((TaxType t) ->
                    Boolean.TRUE.equals(t.getIsVat()) && Double.compare(t.getRate(), 0.19) == 0));
        }

        @Test
        void yaExisteIva_noCreaDuplicado() {
            TaxType existente = new TaxType();
            existente.setIsVat(true);
            existente.setRate(0.19);
            when(taxTypeRepository.findAll()).thenReturn(List.of(existente));

            inventoryService.ensureVatExists();

            verify(taxTypeRepository, never()).save(any());
        }
    }

    @Nested
    class AssignVatToCategory {

        @Test
        void sinIvaAsignado_agregaIvaACategoria() {
            TaxType iva = new TaxType();
            iva.setId(1);
            iva.setIsVat(true);
            when(taxTypeRepository.findAll()).thenReturn(List.of(iva));

            ProductCategory cat = new ProductCategory(1, "LLANTAS", 5, 2, null);
            cat.setTaxTypes(new ArrayList<>());
            when(productCategoryRepository.findById(1)).thenReturn(Optional.of(cat));
            when(productCategoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            inventoryService.assignVatToCategory(cat);

            assertThat(cat.getTaxTypes()).contains(iva);
        }

        @Test
        void yaConIva_noAgregaDuplicado() {
            TaxType iva = new TaxType();
            iva.setId(1);
            iva.setIsVat(true);
            when(taxTypeRepository.findAll()).thenReturn(List.of(iva));

            ProductCategory cat = new ProductCategory(1, "LLANTAS", 5, 2, null);
            cat.setTaxTypes(new ArrayList<>(List.of(iva)));
            when(productCategoryRepository.findById(1)).thenReturn(Optional.of(cat));

            inventoryService.assignVatToCategory(cat);

            verify(productCategoryRepository, never()).save(any());
            assertThat(cat.getTaxTypes()).hasSize(1);
        }
    }

    @Nested
    class CreateCategory {

        @Test
        void nombreMinusculas_seGuardaEnMayusculas() {
            TaxType iva = new TaxType();
            iva.setId(1);
            iva.setIsVat(true);
            when(taxTypeRepository.findAll()).thenReturn(List.of(iva));

            ProductCategory savedCat = new ProductCategory(1, "LLANTAS", 5, 2, null);
            savedCat.setTaxTypes(new ArrayList<>());
            when(productCategoryRepository.save(any())).thenReturn(savedCat);
            when(productCategoryRepository.findById(1)).thenReturn(Optional.of(savedCat));

            inventoryService.createCategory("llantas", 5, 2);

            verify(productCategoryRepository, atLeastOnce()).save(argThat((ProductCategory c) -> "LLANTAS".equals(c.getName())));
        }
    }

    @Nested
    class DeleteCategory {

        @Test
        void conProductosAsociados_retornaFalseYNoElimina() {
            ProductCategory cat = new ProductCategory(1, "LLANTAS", 5, 2, null);
            Product p = new Product();
            p.setCategory(cat);
            when(productRepository.findByCategory(cat)).thenReturn(List.of(p));

            boolean result = inventoryService.deleteCategory(cat);

            assertThat(result).isFalse();
            verify(productCategoryRepository, never()).delete(any());
        }

        @Test
        void sinProductos_retornaTrueYElimina() {
            ProductCategory cat = new ProductCategory(1, "LLANTAS", 5, 2, null);
            when(productRepository.findByCategory(cat)).thenReturn(List.of());

            boolean result = inventoryService.deleteCategory(cat);

            assertThat(result).isTrue();
            verify(productCategoryRepository).delete(cat);
        }
    }

    @Nested
    class SaveCategory {

        @Test
        void nombreConEspaciosYMinusculas_normalizaAMayusculas() {
            ProductCategory cat = new ProductCategory(null, "llantas mixtas", 5, 2, null);
            when(productCategoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            ProductCategory result = inventoryService.saveCategory(cat);

            assertThat(result.getName()).isEqualTo("LLANTAS MIXTAS");
        }
    }
}
