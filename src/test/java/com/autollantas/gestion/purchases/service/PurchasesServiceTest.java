package com.autollantas.gestion.purchases.service;

import com.autollantas.gestion.inventory.model.Product;
import com.autollantas.gestion.inventory.repository.ProductRepository;
import com.autollantas.gestion.purchases.model.Purchase;
import com.autollantas.gestion.purchases.model.PurchaseDetail;
import com.autollantas.gestion.purchases.model.Supplier;
import com.autollantas.gestion.purchases.repository.PurchaseDetailRepository;
import com.autollantas.gestion.purchases.repository.PurchaseRepository;
import com.autollantas.gestion.purchases.repository.SupplierRepository;
import com.autollantas.gestion.treasury.model.Account;
import com.autollantas.gestion.treasury.repository.AccountRepository;
import com.autollantas.gestion.treasury.repository.MovementRepository;
import com.autollantas.gestion.treasury.repository.PaymentRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PurchasesServiceTest {

    @Mock PurchaseRepository purchaseRepository;
    @Mock PurchaseDetailRepository purchaseDetailRepository;
    @Mock SupplierRepository supplierRepository;
    @Mock ProductRepository productRepository;
    @Mock PaymentRepository paymentRepository;
    @Mock AccountRepository accountRepository;
    @Mock MovementRepository movementRepository;

    @InjectMocks PurchasesService purchasesService;

    // ===== GRUPO 1: Creación de compra =====
    //
    // savePurchaseWithDetails persiste la compra tal como llega; el caller fija
    // status y pendingBalance antes de invocar. Tests 1-4 verifican esos valores.
    // Test 5 se traslada a registerPayment (único lugar donde la cuenta se toca).
    // PurchasesService no tiene MovementRepository; test 4 se adapta a verificar
    // que purchaseDetailRepository.save() se llama una vez por detalle.

    @Nested
    class CreacionDeCompra {

        @Test
        void crearCompraContado_deberia_setearEstadoPagada() {
            Purchase purchase = new Purchase();
            purchase.setPaymentType("Contado");
            purchase.setTotal(500000.0);
            purchase.setStatus("PAGADA");
            purchase.setPendingBalance(0.0);
            when(purchaseRepository.save(any(Purchase.class))).thenReturn(purchase);

            purchasesService.savePurchaseWithDetails(purchase, Collections.emptyList(), false);

            ArgumentCaptor<Purchase> captor = ArgumentCaptor.forClass(Purchase.class);
            verify(purchaseRepository).save(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo("PAGADA");
            assertThat(captor.getValue().getPendingBalance()).isEqualTo(0.0);
        }

        @Test
        void crearCompraCredito_deberia_setearEstadoPendiente_conSaldoCompleto() {
            Purchase purchase = new Purchase();
            purchase.setPaymentType("Credito");
            purchase.setTotal(1000000.0);
            purchase.setStatus("PENDIENTE");
            purchase.setPendingBalance(1000000.0);
            when(purchaseRepository.save(any(Purchase.class))).thenReturn(purchase);

            purchasesService.savePurchaseWithDetails(purchase, Collections.emptyList(), false);

            ArgumentCaptor<Purchase> captor = ArgumentCaptor.forClass(Purchase.class);
            verify(purchaseRepository).save(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo("PENDIENTE");
            assertThat(captor.getValue().getPendingBalance()).isEqualTo(1000000.0);
        }

        @Test
        void crearCompra_deberia_incrementarStockDeProductos() {
            Purchase savedPurchase = new Purchase();
            savedPurchase.setId(1);
            when(purchaseRepository.save(any())).thenReturn(savedPurchase);

            // Producto A: 10 unidades existentes, se compran 5 → quedan 15
            Product prodARef = new Product();
            prodARef.setId(1);
            Product realProdA = new Product();
            realProdA.setId(1);
            realProdA.setQuantity(10);

            // Producto B: 3 unidades existentes, se compran 7 → quedan 10
            Product prodBRef = new Product();
            prodBRef.setId(2);
            Product realProdB = new Product();
            realProdB.setId(2);
            realProdB.setQuantity(3);

            when(productRepository.findById(1)).thenReturn(Optional.of(realProdA));
            when(productRepository.findById(2)).thenReturn(Optional.of(realProdB));

            PurchaseDetail detailA = new PurchaseDetail();
            detailA.setProduct(prodARef);
            detailA.setQuantity(5);

            PurchaseDetail detailB = new PurchaseDetail();
            detailB.setProduct(prodBRef);
            detailB.setQuantity(7);

            purchasesService.savePurchaseWithDetails(new Purchase(), List.of(detailA, detailB), false);

            ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
            verify(productRepository, times(2)).save(captor.capture());
            assertThat(captor.getAllValues())
                    .extracting(Product::getQuantity)
                    .containsExactlyInAnyOrder(15, 10);
        }

        @Test
        void crearCompra_deberia_guardarUnDetallePorCadaItem() {
            // PurchasesService no tiene MovementRepository; se verifica que
            // purchaseDetailRepository.save() se invoca una vez por cada detalle.
            Purchase savedPurchase = new Purchase();
            savedPurchase.setId(1);
            when(purchaseRepository.save(any())).thenReturn(savedPurchase);

            // product=null para saltear la actualización de stock
            PurchaseDetail detail1 = new PurchaseDetail();
            PurchaseDetail detail2 = new PurchaseDetail();

            purchasesService.savePurchaseWithDetails(new Purchase(), List.of(detail1, detail2), false);

            verify(purchaseDetailRepository, times(2)).save(any(PurchaseDetail.class));
        }

        @Test
        void crearCompra_deberia_reducirSaldoDeLaCuenta() {
            // savePurchaseWithDetails no toca la cuenta; este test usa registerPayment,
            // único método del servicio que actualiza el saldo.
            Account account = new Account();
            account.setCurrentBalance(5000000.0);

            Purchase purchase = new Purchase();
            purchase.setPendingBalance(2380000.0);

            purchasesService.registerPayment(purchase, account, LocalDate.now(), "Transferencia", 2380000.0);

            ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
            verify(accountRepository).save(captor.capture());
            assertThat(captor.getValue().getCurrentBalance()).isEqualTo(2620000.0);
        }
    }

    // ===== GRUPO 2: Pagos a proveedores =====

    @Nested
    class PagosAProveedores {

        @Test
        void registrarPago_parcial_deberia_reducirSaldoPendiente() {
            Purchase purchase = new Purchase();
            purchase.setPendingBalance(1000000.0);
            Account account = new Account();
            account.setCurrentBalance(2000000.0);

            purchasesService.registerPayment(purchase, account, LocalDate.now(), "Efectivo", 400000.0);

            assertThat(purchase.getPendingBalance()).isEqualTo(600000.0);
            assertThat(purchase.getStatus()).isEqualTo("PENDIENTE");
        }

        @Test
        void registrarPago_total_deberia_marcarCompraPagada() {
            Purchase purchase = new Purchase();
            purchase.setPendingBalance(500000.0);
            Account account = new Account();
            account.setCurrentBalance(1000000.0);

            purchasesService.registerPayment(purchase, account, LocalDate.now(), "Transferencia", 500000.0);

            assertThat(purchase.getPendingBalance()).isEqualTo(0.0);
            assertThat(purchase.getStatus()).isEqualTo("PAGADA");
        }

        @Test
        void registrarPago_deberia_crearPagoYActualizarCuenta() {
            // PurchasesService no tiene MovementRepository; se verifica que
            // paymentRepository.save() es llamado y que la cuenta se actualiza.
            Purchase purchase = new Purchase();
            purchase.setPendingBalance(300000.0);
            Account account = new Account();
            account.setCurrentBalance(800000.0);

            purchasesService.registerPayment(purchase, account, LocalDate.now(), "Efectivo", 150000.0);

            verify(paymentRepository).save(any());

            ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
            verify(accountRepository).save(captor.capture());
            assertThat(captor.getValue().getCurrentBalance()).isEqualTo(650000.0);
        }
    }

    // ===== GRUPO 3: Validaciones =====
    //
    // savePurchaseWithDetails no lanza excepción con lista vacía: simplemente
    // no persiste detalles ni toca el stock.
    // registerPayment no lanza excepción con monto mayor al saldo: lo clampea a 0.

    @Nested
    class Validaciones {

        @Test
        void crearCompra_sinDetalles_noDeberiaGuardarDetalles() {
            when(purchaseRepository.save(any())).thenReturn(new Purchase());

            purchasesService.savePurchaseWithDetails(new Purchase(), Collections.emptyList(), false);

            verify(purchaseDetailRepository, never()).save(any());
            verify(productRepository, never()).save(any());
        }

        @Test
        void registrarPago_mayorAlSaldo_deberiaClampearCeroYMarcarPagada() {
            Purchase purchase = new Purchase();
            purchase.setPendingBalance(200000.0);
            Account account = new Account();
            account.setCurrentBalance(500000.0);

            purchasesService.registerPayment(purchase, account, LocalDate.now(), "Efectivo", 300000.0);

            assertThat(purchase.getPendingBalance()).isEqualTo(0.0);
            assertThat(purchase.getStatus()).isEqualTo("PAGADA");
        }
    }

    // ===== GRUPO 4: Nuevos tests para fixes de integridad financiera =====

    @Nested
    class IntegridadFinanciera {

        @Test
        void compraContado_deberia_reducir_saldoDeLaCuenta() {
            Account account = new Account();
            account.setCurrentBalance(2000000.0);

            Purchase purchase = new Purchase();
            purchase.setPaymentType("Contado");
            purchase.setTotal(500000.0);
            purchase.setStatus("PAGADA");
            purchase.setAccount(account);
            when(purchaseRepository.save(any(Purchase.class))).thenReturn(purchase);

            purchasesService.savePurchaseWithDetails(purchase, Collections.emptyList(), false);

            ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
            verify(accountRepository).save(captor.capture());
            assertThat(captor.getValue().getCurrentBalance()).isEqualTo(1500000.0);
        }

        @Test
        void compraContado_deberia_crear_movimientoEgreso() {
            Account account = new Account();
            account.setCurrentBalance(1000000.0);

            Purchase purchase = new Purchase();
            purchase.setPaymentType("Contado");
            purchase.setTotal(200000.0);
            purchase.setStatus("PAGADA");
            purchase.setAccount(account);
            when(purchaseRepository.save(any(Purchase.class))).thenReturn(purchase);

            purchasesService.savePurchaseWithDetails(purchase, Collections.emptyList(), false);

            verify(movementRepository).save(any());
        }

        @Test
        void compraCredito_noDeberia_tocar_cuenta() {
            Purchase purchase = new Purchase();
            purchase.setPaymentType("Credito");
            purchase.setTotal(400000.0);
            purchase.setStatus("PENDIENTE");
            when(purchaseRepository.save(any(Purchase.class))).thenReturn(purchase);

            purchasesService.savePurchaseWithDetails(purchase, Collections.emptyList(), false);

            verify(accountRepository, never()).save(any());
            verify(movementRepository, never()).save(any());
        }

        @Test
        void cancelarCompraContadoPagada_deberia_revertir_saldoCuenta() {
            Account account = new Account();
            account.setCurrentBalance(1500000.0);

            Purchase purchase = new Purchase();
            purchase.setId(1);
            purchase.setPaymentType("Contado");
            purchase.setStatus("PAGADA");
            purchase.setTotal(500000.0);
            purchase.setAccount(account);
            when(movementRepository.findBySourceIdAndSourceTable(1, "COMPRAS"))
                    .thenReturn(Collections.emptyList());
            when(purchaseDetailRepository.findByPurchase(purchase)).thenReturn(Collections.emptyList());

            purchasesService.cancelPurchase(purchase);

            assertThat(account.getCurrentBalance()).isEqualTo(2000000.0);
        }

        @Test
        void pago_deberia_crear_movimientoEgreso() {
            Purchase purchase = new Purchase();
            purchase.setPendingBalance(500000.0);
            Account account = new Account();
            account.setCurrentBalance(1000000.0);

            purchasesService.registerPayment(purchase, account, LocalDate.now(), "Transferencia", 200000.0);

            verify(movementRepository).save(any());
        }
    }

    // ===== GRUPO 5: Edición de compras =====

    @Nested
    class Edicion {

        @Test
        void editarCompra_deberia_revertirStockViejo_antesDeAplicarNuevo() {
            // Stock=20, old qty=4 → revert: 16; new qty=2 → apply: 18
            Product product = new Product();
            product.setId(1);
            product.setQuantity(20);

            PurchaseDetail oldDetail = new PurchaseDetail();
            oldDetail.setProduct(product);
            oldDetail.setQuantity(4);

            Product newProductRef = new Product();
            newProductRef.setId(1);
            PurchaseDetail newDetail = new PurchaseDetail();
            newDetail.setProduct(newProductRef);
            newDetail.setQuantity(2);

            Purchase savedPurchase = new Purchase();
            savedPurchase.setId(1);
            when(purchaseRepository.save(any())).thenReturn(savedPurchase);
            when(purchaseDetailRepository.findByPurchase(savedPurchase)).thenReturn(List.of(oldDetail));
            when(productRepository.findById(1)).thenReturn(Optional.of(product));

            purchasesService.savePurchaseWithDetails(new Purchase(), List.of(newDetail), true);

            // revert: 20-4=16, apply: 16+2=18
            assertThat(product.getQuantity()).isEqualTo(18);
            verify(productRepository, times(2)).save(product);
        }

        @Test
        void editarCompra_noDeberia_tocar_cuenta_niMovimiento() {
            Account account = new Account();
            account.setCurrentBalance(2000000.0);

            Purchase purchase = new Purchase();
            purchase.setPaymentType("Contado");
            purchase.setAccount(account);

            Purchase savedPurchase = new Purchase();
            when(purchaseRepository.save(any())).thenReturn(savedPurchase);
            when(purchaseDetailRepository.findByPurchase(savedPurchase)).thenReturn(Collections.emptyList());

            purchasesService.savePurchaseWithDetails(purchase, Collections.emptyList(), true);

            verify(accountRepository, never()).save(any());
            verify(movementRepository, never()).save(any());
        }
    }

    // ===== GRUPO 6: Gestión de proveedores =====

    @Nested
    class GestionProveedores {

        @Test
        void saveOrUpdateSupplier_proveedorNuevo_deberia_guardar() {
            when(supplierRepository.findByNitNumber("900123")).thenReturn(Optional.empty());

            purchasesService.saveOrUpdateSupplier(null, "Importadora XYZ", "900123", "x@e.com", "600");

            ArgumentCaptor<Supplier> captor = ArgumentCaptor.forClass(Supplier.class);
            verify(supplierRepository).save(captor.capture());
            assertThat(captor.getValue().getName()).isEqualTo("Importadora XYZ");
            assertThat(captor.getValue().getNitNumber()).isEqualTo("900123");
        }

        @Test
        void saveOrUpdateSupplier_proveedorExistente_deberia_actualizar_noCrearNuevo() {
            Supplier existente = new Supplier();
            existente.setId(99);
            existente.setName("Distribuidora A");
            existente.setNitNumber("800456");
            when(supplierRepository.findByNitNumber("800456")).thenReturn(Optional.of(existente));

            purchasesService.saveOrUpdateSupplier(null, "Distribuidora A Plus", "800456", "d@e.com", "700");

            ArgumentCaptor<Supplier> captor = ArgumentCaptor.forClass(Supplier.class);
            verify(supplierRepository).save(captor.capture());
            assertThat(captor.getValue().getId()).isEqualTo(99);
            assertThat(captor.getValue().getName()).isEqualTo("Distribuidora A Plus");
        }

        @Test
        void saveOrUpdateSupplier_mismoNit_diferenteNombre_deberia_actualizar() {
            Supplier existente = new Supplier();
            existente.setId(12);
            existente.setName("Proveedor Viejo");
            existente.setNitNumber("700789");
            when(supplierRepository.findByNitNumber("700789")).thenReturn(Optional.of(existente));

            purchasesService.saveOrUpdateSupplier(null, "Proveedor Nuevo", "700789", "p@e.com", "800");

            ArgumentCaptor<Supplier> captor = ArgumentCaptor.forClass(Supplier.class);
            verify(supplierRepository, times(1)).save(captor.capture());
            assertThat(captor.getValue().getName()).isEqualTo("Proveedor Nuevo");
            assertThat(captor.getValue().getId()).isEqualTo(12);
        }
    }

    // ===== GRUPO 7: Numeración de facturas =====

    @Nested
    class NumeracionFacturas {

        @Test
        void generateNextInvoiceNumber_sinComprasPrevias_deberia_retornar_primeraSecuencia() {
            when(purchaseRepository.findAll()).thenReturn(Collections.emptyList());

            assertThat(purchasesService.generateNextInvoiceNumber()).isEqualTo("FAC-00001");
        }

        @Test
        void generateNextInvoiceNumber_conCompraPrevia_deberia_incrementar() {
            Purchase p = new Purchase();
            p.setInvoiceNumber("FAC-00007");
            when(purchaseRepository.findAll()).thenReturn(List.of(p));

            assertThat(purchasesService.generateNextInvoiceNumber()).isEqualTo("FAC-00008");
        }

        @Test
        void generateNextInvoiceNumber_conFormatoInesperado_noDeberia_explotar() {
            Purchase p = new Purchase();
            p.setInvoiceNumber("FORMATO-RARO");
            when(purchaseRepository.findAll()).thenReturn(List.of(p));

            String result = purchasesService.generateNextInvoiceNumber();

            assertThat(result).startsWith("FAC-");
        }
    }

    // ===== GRUPO 8: IVA descontable en compras =====

    @Nested
    class CalculoIvaDescontable {

        @Test
        void unDetalle_retornaTaxPorQuantity() {
            Purchase purchase = new Purchase();
            PurchaseDetail d = new PurchaseDetail();
            d.setTax(1900.0);
            d.setQuantity(10);
            when(purchaseDetailRepository.findByPurchase(purchase)).thenReturn(List.of(d));

            assertThat(purchasesService.calculateIvaFavor(purchase)).isCloseTo(19000.0, within(0.01));
        }

        @Test
        void dosDetalles_sumaTaxDeCadaUno() {
            Purchase purchase = new Purchase();
            PurchaseDetail d1 = new PurchaseDetail();
            d1.setTax(1900.0);
            d1.setQuantity(10);
            PurchaseDetail d2 = new PurchaseDetail();
            d2.setTax(950.0);
            d2.setQuantity(4);
            when(purchaseDetailRepository.findByPurchase(purchase)).thenReturn(List.of(d1, d2));

            assertThat(purchasesService.calculateIvaFavor(purchase)).isCloseTo(22800.0, within(0.01));
        }

        @Test
        void sinDetalles_retornaCero() {
            Purchase purchase = new Purchase();
            when(purchaseDetailRepository.findByPurchase(purchase)).thenReturn(Collections.emptyList());

            assertThat(purchasesService.calculateIvaFavor(purchase)).isEqualTo(0.0);
        }

        @Test
        void taxNull_tratadoComoCero_noLanzaNPE() {
            Purchase purchase = new Purchase();
            PurchaseDetail d = new PurchaseDetail();
            d.setTax(null);
            d.setQuantity(5);
            when(purchaseDetailRepository.findByPurchase(purchase)).thenReturn(List.of(d));

            assertThat(purchasesService.calculateIvaFavor(purchase)).isEqualTo(0.0);
        }
    }
}
