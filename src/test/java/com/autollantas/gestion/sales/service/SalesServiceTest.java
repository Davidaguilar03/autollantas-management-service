package com.autollantas.gestion.sales.service;

import com.autollantas.gestion.inventory.model.Product;
import com.autollantas.gestion.inventory.repository.ProductRepository;
import com.autollantas.gestion.sales.model.Customer;
import com.autollantas.gestion.sales.model.Sale;
import com.autollantas.gestion.sales.model.SaleDetail;
import com.autollantas.gestion.sales.repository.CustomerRepository;
import com.autollantas.gestion.sales.repository.SaleDetailRepository;
import com.autollantas.gestion.sales.repository.SaleRepository;
import com.autollantas.gestion.treasury.model.Account;
import com.autollantas.gestion.treasury.repository.AccountRepository;
import com.autollantas.gestion.treasury.repository.CollectionRepository;
import com.autollantas.gestion.treasury.repository.MovementRepository;
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
class SalesServiceTest {

    @Mock SaleRepository saleRepository;
    @Mock SaleDetailRepository saleDetailRepository;
    @Mock CustomerRepository customerRepository;
    @Mock ProductRepository productRepository;
    @Mock CollectionRepository collectionRepository;
    @Mock AccountRepository accountRepository;
    @Mock MovementRepository movementRepository;

    @InjectMocks SalesService salesService;

    // ===== GRUPO 1: Creación de venta =====
    //
    // Nota: saveSaleWithDetails persiste el sale tal como llega; el caller es
    // responsable de fijar status y pendingBalance antes de invocar el servicio.
    // Los tests 1-4 verifican que esos valores se preservan en el save.
    // El test 5 se traslada a registerCollection, único lugar donde la cuenta
    // se actualiza dentro de SalesService.

    @Nested
    class CreacionDeVenta {

        @Test
        void crearVentaContado_deberia_setearEstadoPagada() {
            Sale sale = new Sale();
            sale.setPaymentType("Contado");
            sale.setTotal(500000.0);
            sale.setStatus("PAGADA");
            sale.setPendingBalance(0.0);
            when(saleRepository.save(any(Sale.class))).thenReturn(sale);

            salesService.saveSaleWithDetails(sale, Collections.emptyList(), false);

            ArgumentCaptor<Sale> captor = ArgumentCaptor.forClass(Sale.class);
            verify(saleRepository).save(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo("PAGADA");
            assertThat(captor.getValue().getPendingBalance()).isEqualTo(0.0);
        }

        @Test
        void crearVentaCredito_deberia_setearEstadoPendiente() {
            Sale sale = new Sale();
            sale.setPaymentType("Credito");
            sale.setTotal(1000000.0);
            sale.setStatus("PENDIENTE");
            sale.setPendingBalance(1000000.0);
            when(saleRepository.save(any(Sale.class))).thenReturn(sale);

            salesService.saveSaleWithDetails(sale, Collections.emptyList(), false);

            ArgumentCaptor<Sale> captor = ArgumentCaptor.forClass(Sale.class);
            verify(saleRepository).save(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo("PENDIENTE");
            assertThat(captor.getValue().getPendingBalance()).isEqualTo(1000000.0);
        }

        @Test
        void crearVenta_deberia_descontarStockDeProductos() {
            Sale savedSale = new Sale();
            savedSale.setId(1);
            when(saleRepository.save(any())).thenReturn(savedSale);

            // Producto A: 10 unidades, se venden 3 → quedan 7
            Product prodARef = new Product();
            prodARef.setId(1);
            Product realProdA = new Product();
            realProdA.setId(1);
            realProdA.setQuantity(10);

            // Producto B: 5 unidades, se vende 1 → quedan 4
            Product prodBRef = new Product();
            prodBRef.setId(2);
            Product realProdB = new Product();
            realProdB.setId(2);
            realProdB.setQuantity(5);

            when(productRepository.findById(1)).thenReturn(Optional.of(realProdA));
            when(productRepository.findById(2)).thenReturn(Optional.of(realProdB));

            SaleDetail detailA = new SaleDetail();
            detailA.setProduct(prodARef);
            detailA.setQuantity(3);

            SaleDetail detailB = new SaleDetail();
            detailB.setProduct(prodBRef);
            detailB.setQuantity(1);

            salesService.saveSaleWithDetails(new Sale(), List.of(detailA, detailB), false);

            ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
            verify(productRepository, times(2)).save(captor.capture());
            assertThat(captor.getAllValues())
                    .extracting(Product::getQuantity)
                    .containsExactlyInAnyOrder(7, 4);
        }

        @Test
        void crearVenta_deberia_guardarUnDetalleporCadaItem() {
            Sale savedSale = new Sale();
            savedSale.setId(1);
            when(saleRepository.save(any())).thenReturn(savedSale);

            // product=null para que el servicio saltee la reducción de stock
            SaleDetail detail1 = new SaleDetail();
            SaleDetail detail2 = new SaleDetail();

            salesService.saveSaleWithDetails(new Sale(), List.of(detail1, detail2), false);

            verify(saleDetailRepository, times(2)).save(any(SaleDetail.class));
        }

        @Test
        void crearVenta_deberia_actualizarSaldoDeLaCuenta() {
            // El balance de la cuenta se actualiza en registerCollection, no en
            // saveSaleWithDetails; este test lo verifica en ese método.
            Account account = new Account();
            account.setCurrentBalance(100000.0);

            Sale sale = new Sale();
            sale.setPendingBalance(50000.0);

            salesService.registerCollection(sale, account, LocalDate.now(), "Efectivo", 50000.0);

            ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
            verify(accountRepository).save(captor.capture());
            assertThat(captor.getValue().getCurrentBalance()).isEqualTo(150000.0);
        }
    }

    // ===== GRUPO 2: Recaudos (cobros de cartera) =====

    @Nested
    class Recaudos {

        @Test
        void registrarRecaudo_parcial_deberia_reducirSaldoPendiente() {
            Sale sale = new Sale();
            sale.setPendingBalance(1000000.0);
            Account account = new Account();
            account.setCurrentBalance(0.0);

            salesService.registerCollection(sale, account, LocalDate.now(), "Efectivo", 400000.0);

            assertThat(sale.getPendingBalance()).isEqualTo(600000.0);
            assertThat(sale.getStatus()).isEqualTo("PENDIENTE");
        }

        @Test
        void registrarRecaudo_total_deberia_marcarVentaPagada() {
            Sale sale = new Sale();
            sale.setPendingBalance(500000.0);
            Account account = new Account();
            account.setCurrentBalance(0.0);

            salesService.registerCollection(sale, account, LocalDate.now(), "Transferencia", 500000.0);

            assertThat(sale.getPendingBalance()).isEqualTo(0.0);
            assertThat(sale.getStatus()).isEqualTo("PAGADA");
        }

        @Test
        void registrarRecaudo_deberia_crearColeccionYActualizarCuenta() {
            // SalesService no gestiona movimientos de tesorería; sí persiste una
            // Collection y actualiza el saldo de la cuenta destino.
            Sale sale = new Sale();
            sale.setPendingBalance(300000.0);
            Account account = new Account();
            account.setCurrentBalance(200000.0);

            salesService.registerCollection(sale, account, LocalDate.now(), "Efectivo", 150000.0);

            verify(collectionRepository).save(any());

            ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
            verify(accountRepository).save(captor.capture());
            assertThat(captor.getValue().getCurrentBalance()).isEqualTo(350000.0);
        }
    }

    // ===== GRUPO 3: Validaciones =====
    //
    // Nota: saveSaleWithDetails no lanza excepción con lista vacía; simplemente
    // no guarda detalles ni toca el stock.  registerCollection no lanza excepción
    // si el monto supera el saldo; lo clampea a 0 y marca la venta PAGADA.

    @Nested
    class Validaciones {

        @Test
        void crearVenta_sinProductos_noDeberiaGuardarDetalles() {
            when(saleRepository.save(any())).thenReturn(new Sale());

            salesService.saveSaleWithDetails(new Sale(), Collections.emptyList(), false);

            verify(saleDetailRepository, never()).save(any());
            verify(productRepository, never()).save(any());
        }

        @Test
        void registrarRecaudo_mayorAlSaldo_deberiaClampearCeroYMarcarPagada() {
            Sale sale = new Sale();
            sale.setPendingBalance(200000.0);
            Account account = new Account();
            account.setCurrentBalance(0.0);

            salesService.registerCollection(sale, account, LocalDate.now(), "Efectivo", 300000.0);

            assertThat(sale.getPendingBalance()).isEqualTo(0.0);
            assertThat(sale.getStatus()).isEqualTo("PAGADA");
        }
    }

    // ===== GRUPO 4: Nuevos tests para fixes de integridad financiera =====

    @Nested
    class IntegridadFinanciera {

        @Test
        void ventaContado_deberia_acreditar_saldoDeLaCuenta() {
            Account account = new Account();
            account.setCurrentBalance(500000.0);

            Sale sale = new Sale();
            sale.setPaymentType("Contado");
            sale.setTotal(300000.0);
            sale.setStatus("PAGADA");
            sale.setAccount(account);
            when(saleRepository.save(any(Sale.class))).thenReturn(sale);

            salesService.saveSaleWithDetails(sale, Collections.emptyList(), false);

            ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
            verify(accountRepository).save(captor.capture());
            assertThat(captor.getValue().getCurrentBalance()).isEqualTo(800000.0);
        }

        @Test
        void ventaContado_deberia_crear_movimientoIngreso() {
            Account account = new Account();
            account.setCurrentBalance(0.0);

            Sale sale = new Sale();
            sale.setPaymentType("Contado");
            sale.setTotal(200000.0);
            sale.setStatus("PAGADA");
            sale.setAccount(account);
            when(saleRepository.save(any(Sale.class))).thenReturn(sale);

            salesService.saveSaleWithDetails(sale, Collections.emptyList(), false);

            verify(movementRepository).save(any());
        }

        @Test
        void ventaCredito_noDeberia_tocar_cuenta() {
            Sale sale = new Sale();
            sale.setPaymentType("Credito");
            sale.setTotal(400000.0);
            sale.setStatus("PENDIENTE");
            when(saleRepository.save(any(Sale.class))).thenReturn(sale);

            salesService.saveSaleWithDetails(sale, Collections.emptyList(), false);

            verify(accountRepository, never()).save(any());
            verify(movementRepository, never()).save(any());
        }

        @Test
        void cancelarVentaContadoPagada_deberia_revertir_saldoCuenta() {
            Account account = new Account();
            account.setCurrentBalance(800000.0);

            Sale sale = new Sale();
            sale.setId(1);
            sale.setPaymentType("Contado");
            sale.setStatus("PAGADA");
            sale.setTotal(300000.0);
            sale.setAccount(account);
            when(movementRepository.findBySourceIdAndSourceTable(1, "VENTAS"))
                    .thenReturn(Collections.emptyList());
            when(saleDetailRepository.findBySale(sale)).thenReturn(Collections.emptyList());

            salesService.cancelSale(sale);

            assertThat(account.getCurrentBalance()).isEqualTo(500000.0);
        }

        @Test
        void recaudo_deberia_crear_movimientoIngreso() {
            Sale sale = new Sale();
            sale.setPendingBalance(500000.0);
            Account account = new Account();
            account.setCurrentBalance(0.0);

            salesService.registerCollection(sale, account, LocalDate.now(), "Efectivo", 200000.0);

            verify(movementRepository).save(any());
        }
    }

    // ===== GRUPO 5: Edición de ventas =====

    @Nested
    class Edicion {

        @Test
        void editarVenta_deberia_revertirStockViejo_antesDeAplicarNuevo() {
            // Stock=10, old qty=3 → revert: 13; new qty=5 → apply: 8
            Product product = new Product();
            product.setId(1);
            product.setQuantity(10);

            SaleDetail oldDetail = new SaleDetail();
            oldDetail.setProduct(product);
            oldDetail.setQuantity(3);

            Product newProductRef = new Product();
            newProductRef.setId(1);
            SaleDetail newDetail = new SaleDetail();
            newDetail.setProduct(newProductRef);
            newDetail.setQuantity(5);

            Sale savedSale = new Sale();
            savedSale.setId(1);
            when(saleRepository.save(any())).thenReturn(savedSale);
            when(saleDetailRepository.findBySale(savedSale)).thenReturn(List.of(oldDetail));
            when(productRepository.findById(1)).thenReturn(Optional.of(product));

            salesService.saveSaleWithDetails(new Sale(), List.of(newDetail), true);

            // revert: 10+3=13, apply: 13-5=8
            assertThat(product.getQuantity()).isEqualTo(8);
            verify(productRepository, times(2)).save(product);
        }

        @Test
        void editarVenta_noDeberia_tocar_cuenta_niMovimiento() {
            Account account = new Account();
            account.setCurrentBalance(1000000.0);

            Sale sale = new Sale();
            sale.setPaymentType("Contado");
            sale.setAccount(account);

            Sale savedSale = new Sale();
            when(saleRepository.save(any())).thenReturn(savedSale);
            when(saleDetailRepository.findBySale(savedSale)).thenReturn(Collections.emptyList());

            salesService.saveSaleWithDetails(sale, Collections.emptyList(), true);

            verify(accountRepository, never()).save(any());
            verify(movementRepository, never()).save(any());
        }
    }

    // ===== GRUPO 6: Gestión de clientes =====

    @Nested
    class GestionClientes {

        @Test
        void saveOrUpdateCustomer_clienteNuevo_deberia_guardar() {
            when(customerRepository.findByDocumentNumber("123")).thenReturn(Optional.empty());

            salesService.saveOrUpdateCustomer(null, "Carlos", "123", "c@e.com", "3001", "CC");

            ArgumentCaptor<Customer> captor = ArgumentCaptor.forClass(Customer.class);
            verify(customerRepository).save(captor.capture());
            assertThat(captor.getValue().getName()).isEqualTo("Carlos");
            assertThat(captor.getValue().getDocumentNumber()).isEqualTo("123");
        }

        @Test
        void saveOrUpdateCustomer_clienteExistente_deberia_actualizar_noCrearNuevo() {
            Customer existente = new Customer();
            existente.setId(42);
            existente.setName("Ana");
            existente.setDocumentNumber("456");
            when(customerRepository.findByDocumentNumber("456")).thenReturn(Optional.of(existente));

            salesService.saveOrUpdateCustomer(null, "Ana Actualizada", "456", "a@e.com", "222", "CC");

            ArgumentCaptor<Customer> captor = ArgumentCaptor.forClass(Customer.class);
            verify(customerRepository).save(captor.capture());
            assertThat(captor.getValue().getId()).isEqualTo(42);
            assertThat(captor.getValue().getName()).isEqualTo("Ana Actualizada");
        }

        @Test
        void saveOrUpdateCustomer_mismoDocumento_diferenteNombre_deberia_actualizar() {
            Customer existente = new Customer();
            existente.setId(7);
            existente.setName("Carlos Perez");
            existente.setDocumentNumber("123");
            when(customerRepository.findByDocumentNumber("123")).thenReturn(Optional.of(existente));

            salesService.saveOrUpdateCustomer(null, "Carlos A. Perez", "123", "c@e.com", "300", "CC");

            ArgumentCaptor<Customer> captor = ArgumentCaptor.forClass(Customer.class);
            verify(customerRepository, times(1)).save(captor.capture());
            assertThat(captor.getValue().getName()).isEqualTo("Carlos A. Perez");
            assertThat(captor.getValue().getId()).isEqualTo(7);
        }
    }

    // ===== GRUPO 7: Numeración de facturas =====

    @Nested
    class NumeracionFacturas {

        @Test
        void generateNextInvoiceNumber_sinFacturasPrevias_deberia_retornar_VEN00001() {
            when(saleRepository.findAll()).thenReturn(Collections.emptyList());

            assertThat(salesService.generateNextInvoiceNumber()).isEqualTo("VEN-00001");
        }

        @Test
        void generateNextInvoiceNumber_conFacturaPrevia_deberia_incrementar() {
            Sale s = new Sale();
            s.setInvoiceNumber("VEN-00004");
            when(saleRepository.findAll()).thenReturn(List.of(s));

            assertThat(salesService.generateNextInvoiceNumber()).isEqualTo("VEN-00005");
        }

        @Test
        void generateNextInvoiceNumber_conFormatoInesperado_noDeberia_explotar() {
            Sale s = new Sale();
            s.setInvoiceNumber("FACTURA-VIEJA");
            when(saleRepository.findAll()).thenReturn(List.of(s));

            String result = salesService.generateNextInvoiceNumber();

            assertThat(result).startsWith("VEN-");
        }
    }

    // ===== GRUPO 8: Utilidad por ventas =====

    @Nested
    class CalculoUtilidad {

        @Test
        void unDetalle_retornaProfitAmount() {
            Sale sale = new Sale();
            SaleDetail d = new SaleDetail();
            d.setProfitAmount(84000.0);
            when(saleDetailRepository.findBySale(sale)).thenReturn(List.of(d));

            assertThat(salesService.calculateUtilidad(sale)).isCloseTo(84000.0, within(0.01));
        }

        @Test
        void dosDetalles_sumaAmbos() {
            Sale sale = new Sale();
            SaleDetail d1 = new SaleDetail();
            d1.setProfitAmount(84000.0);
            SaleDetail d2 = new SaleDetail();
            d2.setProfitAmount(50000.0);
            when(saleDetailRepository.findBySale(sale)).thenReturn(List.of(d1, d2));

            assertThat(salesService.calculateUtilidad(sale)).isCloseTo(134000.0, within(0.01));
        }

        @Test
        void profitAmountNegativo_refleja_perdida() {
            Sale sale = new Sale();
            SaleDetail d = new SaleDetail();
            d.setProfitAmount(-5000.0);
            when(saleDetailRepository.findBySale(sale)).thenReturn(List.of(d));

            assertThat(salesService.calculateUtilidad(sale)).isCloseTo(-5000.0, within(0.01));
        }

        @Test
        void profitAmountNull_tratadoComoCero() {
            Sale sale = new Sale();
            SaleDetail d = new SaleDetail();
            d.setProfitAmount(null);
            when(saleDetailRepository.findBySale(sale)).thenReturn(List.of(d));

            assertThat(salesService.calculateUtilidad(sale)).isEqualTo(0.0);
        }

        @Test
        void sinDetalles_retornaCero() {
            Sale sale = new Sale();
            when(saleDetailRepository.findBySale(sale)).thenReturn(Collections.emptyList());

            assertThat(salesService.calculateUtilidad(sale)).isEqualTo(0.0);
        }
    }

    // ===== GRUPO 9: IVA por pagar en ventas =====

    @Nested
    class CalculoDiferenciaIva {

        @Test
        void unDetalle_retornaIvaDifference() {
            Sale sale = new Sale();
            SaleDetail d = new SaleDetail();
            d.setIvaDifference(5700.0);
            when(saleDetailRepository.findBySale(sale)).thenReturn(List.of(d));

            assertThat(salesService.calculateDiferenciaIva(sale)).isCloseTo(5700.0, within(0.01));
        }

        @Test
        void dosDetalles_sumaAmbos() {
            Sale sale = new Sale();
            SaleDetail d1 = new SaleDetail();
            d1.setIvaDifference(5700.0);
            SaleDetail d2 = new SaleDetail();
            d2.setIvaDifference(3000.0);
            when(saleDetailRepository.findBySale(sale)).thenReturn(List.of(d1, d2));

            assertThat(salesService.calculateDiferenciaIva(sale)).isCloseTo(8700.0, within(0.01));
        }

        @Test
        void ivaDifferenceNegativa_ventaBajoMinimo() {
            Sale sale = new Sale();
            SaleDetail d = new SaleDetail();
            d.setIvaDifference(-1000.0);
            when(saleDetailRepository.findBySale(sale)).thenReturn(List.of(d));

            assertThat(salesService.calculateDiferenciaIva(sale)).isCloseTo(-1000.0, within(0.01));
        }

        @Test
        void ivaDifferenceNull_tratadoComoCero() {
            Sale sale = new Sale();
            SaleDetail d = new SaleDetail();
            d.setIvaDifference(null);
            when(saleDetailRepository.findBySale(sale)).thenReturn(List.of(d));

            assertThat(salesService.calculateDiferenciaIva(sale)).isEqualTo(0.0);
        }

        @Test
        void sinDetalles_retornaCero() {
            Sale sale = new Sale();
            when(saleDetailRepository.findBySale(sale)).thenReturn(Collections.emptyList());

            assertThat(salesService.calculateDiferenciaIva(sale)).isEqualTo(0.0);
        }
    }
}
