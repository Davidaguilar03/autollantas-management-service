package com.autollantas.gestion.reporting.service;

import com.autollantas.gestion.inventory.service.InventoryService;
import com.autollantas.gestion.purchases.model.Purchase;
import com.autollantas.gestion.purchases.service.PurchasesService;
import com.autollantas.gestion.sales.model.Sale;
import com.autollantas.gestion.sales.service.SalesService;
import com.autollantas.gestion.treasury.model.Movement;
import com.autollantas.gestion.treasury.model.OccasionalIncome;
import com.autollantas.gestion.treasury.model.OperationalExpense;
import com.autollantas.gestion.treasury.service.TreasuryService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock SalesService salesService;
    @Mock PurchasesService purchasesService;
    @Mock TreasuryService treasuryService;
    @Mock InventoryService inventoryService;

    @InjectMocks DashboardService dashboardService;

    private static final LocalDate HOY = LocalDate.now();
    private static final LocalDate HACE_7_DIAS = HOY.minusDays(7);

    // ===== PARTE A: getMovements =====
    //
    // DashboardService no tiene "calcularResultadoNeto"; los tests 1-2 verifican
    // getMovements(), que es donde se construyen y clasifican los movimientos.

    @Nested
    class GetMovements {

        @Test
        void deberia_incluir_ventasDelPeriodo_comoMovimientosTipoVenta() {
            // Test 1 adaptado: no existe "calcularIngresosPeriodo".
            // getMovements() envuelve cada Sale en un Movement con type="Venta".
            Sale venta = new Sale();
            venta.setId(1);
            venta.setSaleDate(HOY);
            venta.setTotal(500000.0);

            when(salesService.findSalesByDateBetween(HACE_7_DIAS, HOY))
                    .thenReturn(List.of(venta));

            List<Movement> result = dashboardService.getMovements(HACE_7_DIAS, HOY);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getType()).isEqualTo("Venta");
            assertThat(result.get(0).getAmount()).isEqualTo(500000.0);
        }

        @Test
        void deberia_combinar_ventasYComprasDelPeriodo_enLaMismaLista() {
            // Test 2 adaptado: no existe "calcularResultadoNeto".
            // getMovements() consolida ventas (Venta) y compras (Costo) en una lista.
            Sale venta = new Sale();
            venta.setSaleDate(HOY);
            venta.setTotal(200000.0);

            Purchase compra = new Purchase();
            compra.setPurchaseDate(HOY);
            compra.setTotal(100000.0);

            when(salesService.findSalesByDateBetween(any(), any()))
                    .thenReturn(List.of(venta));
            when(purchasesService.findPurchasesByDateBetween(any(), any()))
                    .thenReturn(List.of(compra));

            List<Movement> result = dashboardService.getMovements(HACE_7_DIAS, HOY);

            assertThat(result).hasSize(2);
            assertThat(result).extracting(Movement::getType)
                    .containsExactlyInAnyOrder("Venta", "Costo");
        }
    }

    // ===== PARTE B: getGlobalKpis =====
    //
    // getGlobalKpis() usa Sale::getTotal (NO pendingBalance) para ventas PENDIENTE,
    // y Purchase::getTotal para compras PENDIENTE.

    @Nested
    class GetGlobalKpis {

        @Test
        void totalReceivable_deberia_sumar_totalDeVentasPendientes() {
            // Test 3: solo ventas con status PENDIENTE contribuyen a totalReceivable.
            Sale pendiente1 = new Sale();
            pendiente1.setStatus("PENDIENTE");
            pendiente1.setTotal(400000.0);

            Sale pendiente2 = new Sale();
            pendiente2.setStatus("PENDIENTE");
            pendiente2.setTotal(600000.0);

            Sale pagada = new Sale();
            pagada.setStatus("PAGADA");
            pagada.setTotal(999000.0); // no debe sumarse

            when(salesService.findAllSales())
                    .thenReturn(List.of(pendiente1, pendiente2, pagada));

            DashboardService.DashboardKpis kpis = dashboardService.getGlobalKpis();

            assertThat(kpis.getTotalReceivable()).isEqualTo(1000000.0);
        }

        @Test
        void totalPayable_deberia_sumar_totalDeComprasPendientes() {
            // Test 4: solo compras con status PENDIENTE contribuyen a totalPayable.
            Purchase pendiente = new Purchase();
            pendiente.setStatus("PENDIENTE");
            pendiente.setTotal(2380000.0);

            Purchase pagada = new Purchase();
            pagada.setStatus("PAGADA");
            pagada.setTotal(999000.0); // no debe sumarse

            when(purchasesService.findAllPurchases())
                    .thenReturn(List.of(pendiente, pagada));

            DashboardService.DashboardKpis kpis = dashboardService.getGlobalKpis();

            assertThat(kpis.getTotalPayable()).isEqualTo(2380000.0);
        }

        @Test
        void deberia_retornar_cerosCuandoNoHayPendientes() {
            when(salesService.findAllSales()).thenReturn(Collections.emptyList());
            when(purchasesService.findAllPurchases()).thenReturn(Collections.emptyList());

            DashboardService.DashboardKpis kpis = dashboardService.getGlobalKpis();

            assertThat(kpis.getTotalReceivable()).isEqualTo(0.0);
            assertThat(kpis.getTotalPayable()).isEqualTo(0.0);
        }
    }

    // ===== PARTE C: Gastos e Ingresos en getMovements =====

    @Nested
    class GetMovementsTesoreria {

        @Test
        void getMovements_conGastosOperativos_deberia_incluirlos_comoGastos() {
            OperationalExpense gasto1 = new OperationalExpense();
            gasto1.setDate(HOY);
            gasto1.setAmount(80000.0);
            gasto1.setConcept("Electricidad");

            OperationalExpense gasto2 = new OperationalExpense();
            gasto2.setDate(HOY.minusDays(1));
            gasto2.setAmount(50000.0);
            gasto2.setConcept("Arrendamiento");

            when(treasuryService.findExpensesByDateBetween(any(), any()))
                    .thenReturn(List.of(gasto1, gasto2));

            List<Movement> result = dashboardService.getMovements(HACE_7_DIAS, HOY);

            assertThat(result).hasSize(2);
            assertThat(result).allMatch(m -> "Gasto".equals(m.getType()));
            assertThat(result).extracting(Movement::getAmount)
                    .containsExactlyInAnyOrder(80000.0, 50000.0);
        }

        @Test
        void getMovements_conIngresosOcasionales_deberia_incluirlos_comoIngresos() {
            OccasionalIncome ingreso1 = new OccasionalIncome();
            ingreso1.setDate(HOY);
            ingreso1.setAmount(200000.0);
            ingreso1.setConcept("Arriendo local");

            OccasionalIncome ingreso2 = new OccasionalIncome();
            ingreso2.setDate(HOY.minusDays(2));
            ingreso2.setAmount(150000.0);
            ingreso2.setConcept("Venta activo");

            when(treasuryService.findIncomesByDateBetween(any(), any()))
                    .thenReturn(List.of(ingreso1, ingreso2));

            List<Movement> result = dashboardService.getMovements(HACE_7_DIAS, HOY);

            assertThat(result).hasSize(2);
            assertThat(result).allMatch(m -> "Ingreso".equals(m.getType()));
            assertThat(result).extracting(Movement::getAmount)
                    .containsExactlyInAnyOrder(200000.0, 150000.0);
        }

        @Test
        void getMovements_mixto_deberia_combinar_todas_las_fuentes_ordenadoPorFecha() {
            Sale venta = new Sale();
            venta.setSaleDate(HOY);
            venta.setTotal(500000.0);

            Purchase compra = new Purchase();
            compra.setPurchaseDate(HOY.minusDays(2));
            compra.setTotal(300000.0);

            OperationalExpense gasto = new OperationalExpense();
            gasto.setDate(HOY.minusDays(1));
            gasto.setAmount(80000.0);
            gasto.setConcept("Electricidad");

            OccasionalIncome ingreso = new OccasionalIncome();
            ingreso.setDate(HOY.minusDays(3));
            ingreso.setAmount(120000.0);
            ingreso.setConcept("Arriendo");

            when(salesService.findSalesByDateBetween(any(), any())).thenReturn(List.of(venta));
            when(purchasesService.findPurchasesByDateBetween(any(), any())).thenReturn(List.of(compra));
            when(treasuryService.findExpensesByDateBetween(any(), any())).thenReturn(List.of(gasto));
            when(treasuryService.findIncomesByDateBetween(any(), any())).thenReturn(List.of(ingreso));

            List<Movement> result = dashboardService.getMovements(HACE_7_DIAS, HOY);

            assertThat(result).hasSize(4);
            assertThat(result).extracting(Movement::getType)
                    .containsExactlyInAnyOrder("Venta", "Costo", "Gasto", "Ingreso");
            // sorted descending by date: HOY, HOY-1, HOY-2, HOY-3
            assertThat(result.get(0).getType()).isEqualTo("Venta");
            assertThat(result.get(1).getType()).isEqualTo("Gasto");
            assertThat(result.get(2).getType()).isEqualTo("Costo");
            assertThat(result.get(3).getType()).isEqualTo("Ingreso");
        }
    }
}
