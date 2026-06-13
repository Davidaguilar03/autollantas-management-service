package com.autollantas.gestion.reporting.service;

import com.autollantas.gestion.inventory.service.InventoryService;
import com.autollantas.gestion.purchases.model.Purchase;
import com.autollantas.gestion.purchases.repository.PurchaseRepository;
import com.autollantas.gestion.purchases.service.PurchasesService;
import com.autollantas.gestion.reporting.model.MovementDto;
import com.autollantas.gestion.sales.model.Sale;
import com.autollantas.gestion.sales.repository.SaleRepository;
import com.autollantas.gestion.sales.service.SalesService;
import com.autollantas.gestion.treasury.model.OccasionalIncome;
import com.autollantas.gestion.treasury.model.OperationalExpense;
import com.autollantas.gestion.treasury.repository.OccasionalIncomeRepository;
import com.autollantas.gestion.treasury.repository.OperationalExpenseRepository;
import com.autollantas.gestion.treasury.service.TreasuryService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock SalesService salesService;
    @Mock SaleRepository saleRepository;
    @Mock PurchasesService purchasesService;
    @Mock PurchaseRepository purchaseRepository;
    @Mock TreasuryService treasuryService;
    @Mock InventoryService inventoryService;
    @Mock OperationalExpenseRepository expenseRepository;
    @Mock OccasionalIncomeRepository incomeRepository;

    @InjectMocks DashboardService dashboardService;

    private static final LocalDate HOY        = LocalDate.now();
    private static final LocalDate HACE_7_DIAS = HOY.minusDays(7);

    // ===== PARTE A: getMovements =====

    @Nested
    class GetMovements {

        @Test
        void deberia_incluir_ventasDelPeriodo_comoMovimientosTipoVenta() {
            Sale venta = new Sale();
            venta.setId(1);
            venta.setSaleDate(HOY);
            venta.setTotal(500000.0);

            when(salesService.findSalesByDateBetween(HACE_7_DIAS, HOY)).thenReturn(List.of(venta));

            List<MovementDto> result = dashboardService.getMovements(HACE_7_DIAS, HOY);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getType()).isEqualTo("Venta");
            assertThat(result.get(0).getAmount()).isEqualTo(500000.0);
        }

        @Test
        void deberia_combinar_ventasYComprasDelPeriodo_enLaMismaLista() {
            Sale venta = new Sale();
            venta.setSaleDate(HOY);
            venta.setTotal(200000.0);

            Purchase compra = new Purchase();
            compra.setPurchaseDate(HOY);
            compra.setTotal(100000.0);

            when(salesService.findSalesByDateBetween(any(), any())).thenReturn(List.of(venta));
            when(purchasesService.findPurchasesByDateBetween(any(), any())).thenReturn(List.of(compra));

            List<MovementDto> result = dashboardService.getMovements(HACE_7_DIAS, HOY);

            assertThat(result).hasSize(2);
            assertThat(result).extracting(MovementDto::getType)
                    .containsExactlyInAnyOrder("Venta", "Costo");
        }

        @Test
        void deberia_limitar_a_MAX_MOVEMENTS_cuando_hay_muchos_registros() {
            int total = DashboardService.MAX_MOVEMENTS + 20;
            List<Sale> ventas = new ArrayList<>();
            for (int i = 0; i < total; i++) {
                Sale s = new Sale();
                s.setSaleDate(HOY.minusDays(i));
                s.setTotal(10000.0);
                ventas.add(s);
            }
            when(salesService.findSalesByDateBetween(any(), any())).thenReturn(ventas);

            List<MovementDto> result = dashboardService.getMovements(HOY.minusDays(total), HOY);

            assertThat(result).hasSize(DashboardService.MAX_MOVEMENTS);
        }
    }

    // ===== PARTE B: getGlobalKpis =====

    @Nested
    class GetGlobalKpis {

        @Test
        void totalReceivable_deberia_usar_query_SUM_directa() {
            when(saleRepository.sumPendingReceivable()).thenReturn(1000000.0);

            DashboardService.DashboardKpis kpis = dashboardService.getGlobalKpis();

            assertThat(kpis.getTotalReceivable()).isEqualTo(1000000.0);
        }

        @Test
        void totalPayable_deberia_usar_query_SUM_directa() {
            when(purchaseRepository.sumPendingPayable()).thenReturn(2380000.0);

            DashboardService.DashboardKpis kpis = dashboardService.getGlobalKpis();

            assertThat(kpis.getTotalPayable()).isEqualTo(2380000.0);
        }

        @Test
        void deberia_retornar_cerosCuandoNoHayPendientes() {
            when(saleRepository.sumPendingReceivable()).thenReturn(0.0);
            when(purchaseRepository.sumPendingPayable()).thenReturn(0.0);

            DashboardService.DashboardKpis kpis = dashboardService.getGlobalKpis();

            assertThat(kpis.getTotalReceivable()).isEqualTo(0.0);
            assertThat(kpis.getTotalPayable()).isEqualTo(0.0);
        }
    }

    // ===== PARTE C: getPeriodKpis =====

    @Nested
    class GetPeriodKpis {

        @Test
        void deberia_sumar_ventas_e_ingresos_ocasionales_como_totalIncome() {
            when(saleRepository.sumTotalByDateBetween(HACE_7_DIAS, HOY)).thenReturn(500000.0);
            when(incomeRepository.sumAmountByDateBetween(HACE_7_DIAS, HOY)).thenReturn(100000.0);

            DashboardService.PeriodKpis kpis = dashboardService.getPeriodKpis(HACE_7_DIAS, HOY);

            assertThat(kpis.getTotalIncome()).isEqualTo(600000.0);
        }

        @Test
        void deberia_reportar_totalCosts_desde_compras() {
            when(purchaseRepository.sumTotalByDateBetween(HACE_7_DIAS, HOY)).thenReturn(300000.0);

            DashboardService.PeriodKpis kpis = dashboardService.getPeriodKpis(HACE_7_DIAS, HOY);

            assertThat(kpis.getTotalCosts()).isEqualTo(300000.0);
        }

        @Test
        void deberia_reportar_totalExpenses_desde_gastos_operativos() {
            when(expenseRepository.sumAmountByDateBetween(HACE_7_DIAS, HOY)).thenReturn(80000.0);

            DashboardService.PeriodKpis kpis = dashboardService.getPeriodKpis(HACE_7_DIAS, HOY);

            assertThat(kpis.getTotalExpenses()).isEqualTo(80000.0);
        }

        @Test
        void netResult_deberia_ser_ingresos_menos_costos_menos_gastos() {
            when(saleRepository.sumTotalByDateBetween(any(), any())).thenReturn(1000000.0);
            when(incomeRepository.sumAmountByDateBetween(any(), any())).thenReturn(0.0);
            when(purchaseRepository.sumTotalByDateBetween(any(), any())).thenReturn(400000.0);
            when(expenseRepository.sumAmountByDateBetween(any(), any())).thenReturn(100000.0);

            DashboardService.PeriodKpis kpis = dashboardService.getPeriodKpis(HACE_7_DIAS, HOY);

            assertThat(kpis.getNetResult()).isEqualTo(500000.0);
        }

        @Test
        void netResult_negativo_cuando_costos_superan_ingresos() {
            when(saleRepository.sumTotalByDateBetween(any(), any())).thenReturn(100000.0);
            when(incomeRepository.sumAmountByDateBetween(any(), any())).thenReturn(0.0);
            when(purchaseRepository.sumTotalByDateBetween(any(), any())).thenReturn(300000.0);
            when(expenseRepository.sumAmountByDateBetween(any(), any())).thenReturn(50000.0);

            DashboardService.PeriodKpis kpis = dashboardService.getPeriodKpis(HACE_7_DIAS, HOY);

            assertThat(kpis.getNetResult()).isNegative();
            assertThat(kpis.getNetResult()).isEqualTo(-250000.0);
        }
    }

    // ===== PARTE D: getMovements — Tesorería =====

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

            when(treasuryService.findExpensesByDateBetween(any(), any())).thenReturn(List.of(gasto1, gasto2));

            List<MovementDto> result = dashboardService.getMovements(HACE_7_DIAS, HOY);

            assertThat(result).hasSize(2);
            assertThat(result).allMatch(m -> "Gasto".equals(m.getType()));
            assertThat(result).extracting(MovementDto::getAmount)
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

            when(treasuryService.findIncomesByDateBetween(any(), any())).thenReturn(List.of(ingreso1, ingreso2));

            List<MovementDto> result = dashboardService.getMovements(HACE_7_DIAS, HOY);

            assertThat(result).hasSize(2);
            assertThat(result).allMatch(m -> "Ingreso".equals(m.getType()));
            assertThat(result).extracting(MovementDto::getAmount)
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

            List<MovementDto> result = dashboardService.getMovements(HACE_7_DIAS, HOY);

            assertThat(result).hasSize(4);
            assertThat(result).extracting(MovementDto::getType)
                    .containsExactlyInAnyOrder("Venta", "Costo", "Gasto", "Ingreso");
            // orden descendente por fecha: HOY, HOY-1, HOY-2, HOY-3
            assertThat(result.get(0).getType()).isEqualTo("Venta");
            assertThat(result.get(1).getType()).isEqualTo("Gasto");
            assertThat(result.get(2).getType()).isEqualTo("Costo");
            assertThat(result.get(3).getType()).isEqualTo("Ingreso");
        }
    }
}
