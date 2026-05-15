package com.autollantas.gestion.reporting.service;

import com.autollantas.gestion.purchases.model.Purchase;
import com.autollantas.gestion.purchases.service.PurchasesService;
import com.autollantas.gestion.treasury.model.OperationalExpense;
import com.autollantas.gestion.treasury.model.OccasionalIncome;
import com.autollantas.gestion.treasury.model.Movement;
import com.autollantas.gestion.treasury.service.TreasuryService;
import com.autollantas.gestion.sales.model.Sale;
import com.autollantas.gestion.sales.service.SalesService;
import com.autollantas.gestion.inventory.service.InventoryService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class DashboardService {

    private final SalesService salesService;
    private final PurchasesService purchasesService;
    private final TreasuryService treasuryService;
    private final InventoryService inventoryService;

    public DashboardService(SalesService salesService,
                            PurchasesService purchasesService,
                            TreasuryService treasuryService,
                            InventoryService inventoryService) {
        this.salesService = salesService;
        this.purchasesService = purchasesService;
        this.treasuryService = treasuryService;
        this.inventoryService = inventoryService;
    }

    @Transactional(readOnly = true)
    public List<Movement> getMovements(LocalDate start, LocalDate end) {
        List<Movement> movements = new ArrayList<>();

        for (Sale sale : salesService.findSalesByDateBetween(start, end)) {
            Movement movement = new Movement(
                    sale.getSaleDate(),
                    sale.getId(),
                    "Venta",
                    sale.getTotal(),
                    sale.getAccount()
            );
            movement.setSourceTable("VENTA");
            movements.add(movement);
        }

        for (Purchase purchase : purchasesService.findPurchasesByDateBetween(start, end)) {
            Movement movement = new Movement(
                    purchase.getPurchaseDate(),
                    purchase.getId(),
                    "Costo",
                    purchase.getTotal(),
                    purchase.getAccount()
            );
            movement.setSourceTable("COMPRA");
            movements.add(movement);
        }

        for (OperationalExpense expense : treasuryService.findExpensesByDateBetween(start, end)) {
            Movement movement = new Movement(
                    expense.getDate(),
                    expense.getId(),
                    "Gasto",
                    expense.getAmount(),
                    expense.getAccount()
            );
            movement.setSourceTable("GASTO: " + expense.getConcept());
            movements.add(movement);
        }

        for (OccasionalIncome income : treasuryService.findIncomesByDateBetween(start, end)) {
            Movement movement = new Movement(
                    income.getDate(),
                    income.getId(),
                    "Ingreso",
                    income.getAmount(),
                    income.getAccount()
            );
            movement.setSourceTable("OTRO: " + income.getConcept());
            movements.add(movement);
        }

        movements.sort((m1, m2) -> m2.getDate().compareTo(m1.getDate()));
        return movements;
    }

    @Transactional(readOnly = true)
    public DashboardKpis getGlobalKpis() {
        double totalReceivable = salesService.findAllSales().stream()
                .filter(s -> "PENDIENTE".equalsIgnoreCase(s.getStatus()))
                .mapToDouble(Sale::getTotal)
                .sum();

        double totalPayable = purchasesService.findAllPurchases().stream()
                .filter(p -> "PENDIENTE".equalsIgnoreCase(p.getStatus()))
                .mapToDouble(Purchase::getTotal)
                .sum();

        return new DashboardKpis(
                totalReceivable,
                totalPayable,
                treasuryService.getTotalAccountBalance(),
                inventoryService.countStockAlerts()
        );
    }

    public static class DashboardKpis {
        private final double totalReceivable;
        private final double totalPayable;
        private final double totalBalance;
        private final long alertCount;

        public DashboardKpis(double totalReceivable, double totalPayable, double totalBalance, long alertCount) {
            this.totalReceivable = totalReceivable;
            this.totalPayable = totalPayable;
            this.totalBalance = totalBalance;
            this.alertCount = alertCount;
        }

        public double getTotalReceivable() { return totalReceivable; }
        public double getTotalPayable() { return totalPayable; }
        public double getTotalBalance() { return totalBalance; }
        public long getAlertCount() { return alertCount; }
    }
}
